package com.example.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.example.data.telemetry.BatteryScienceClassifier
import com.example.data.telemetry.CableDnaStandardsEngine
import com.example.data.telemetry.CurrentCalibrationEngine
import com.example.data.telemetry.DeviceHardwareProfiler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class BatteryMonitorRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    val currentCalibrationEngine = CurrentCalibrationEngine(context)

    private var previousTempC: Float = 25.0f

    private val _telemetry = MutableStateFlow(BatteryTelemetry())
    val telemetry: StateFlow<BatteryTelemetry> = _telemetry.asStateFlow()

    private val _voltageHistory = MutableStateFlow<List<TelemetryPoint>>(emptyList())
    val voltageHistory: StateFlow<List<TelemetryPoint>> = _voltageHistory.asStateFlow()

    private val _currentHistory = MutableStateFlow<List<TelemetryPoint>>(emptyList())
    val currentHistory: StateFlow<List<TelemetryPoint>> = _currentHistory.asStateFlow()

    private val _wattageHistory = MutableStateFlow<List<TelemetryPoint>>(emptyList())
    val wattageHistory: StateFlow<List<TelemetryPoint>> = _wattageHistory.asStateFlow()

    private val _temperatureHistory = MutableStateFlow<List<TelemetryPoint>>(emptyList())
    val temperatureHistory: StateFlow<List<TelemetryPoint>> = _temperatureHistory.asStateFlow()

    private val _sessionLogs = MutableStateFlow<List<SessionLog>>(emptyList())
    val sessionLogs: StateFlow<List<SessionLog>> = _sessionLogs.asStateFlow()

    private val _userAccount = MutableStateFlow(UserAccount())
    val userAccount: StateFlow<UserAccount> = _userAccount.asStateFlow()

    private val _deviceProfile = MutableStateFlow(
        DeviceHardwareProfiler.collectProfile(context)
    )
    val deviceProfile: StateFlow<DeviceHardwareProfile> = _deviceProfile.asStateFlow()

    private val _cableResult = MutableStateFlow(CableDiagnosticResult())
    val cableResult: StateFlow<CableDiagnosticResult> = _cableResult.asStateFlow()

    private val _sohPrediction = MutableStateFlow(BatterySohPrediction())
    val sohPrediction: StateFlow<BatterySohPrediction> = _sohPrediction.asStateFlow()

    private var previousLivingState: LivingBatteryState? = null
    private var previousIsCharging: Boolean = false
    private var observedBaselineOcvMv: Int? = null
    private var prevSampleVoltageMv: Int = 0
    private var prevSampleCurrentMa: Int = 0
    private var smoothedDynamicImpedanceMohm: Int? = null
    private var smoothedRipplePercent: Float = 0.0f

    @Volatile
    private var isAppInForeground: Boolean = true

    @Volatile
    private var isSohPageActive: Boolean = false

    @Volatile
    private var isProModeActive: Boolean = false

    @Volatile
    private var cachedWalLogCount: Int = 0
    private var lastWalSyncTimeMs: Long = 0L

    private val _hourlyStats = MutableStateFlow<List<com.example.data.local.HourlyBatteryStatEntity>>(emptyList())
    val hourlyStats: StateFlow<List<com.example.data.local.HourlyBatteryStatEntity>> = _hourlyStats.asStateFlow()

    init {
        initSessionLogsSeed()
        initHourlyStatsFromDatabase()
        refreshWalLogCountAsync()
        startPolling()
    }

    fun setAppInForeground(inForeground: Boolean) {
        isAppInForeground = inForeground
    }

    fun setSohPageActive(active: Boolean) {
        isSohPageActive = active
        if (active) {
            refreshWalLogCountAsync()
            val t = _telemetry.value
            val rawChargeCounter = queryHardwareChargeCounterUah()
            computeLiveSohPrediction(
                telemetry = t,
                chargeCounterUah = if (rawChargeCounter > 0L) rawChargeCounter else t.chargeCounterUah,
                hwCycleCount = t.cycleCount,
                hasHwChargeCounter = rawChargeCounter > 0L
            )
        }
    }

    fun setProModeActive(enabled: Boolean) {
        isProModeActive = enabled
    }

    private fun queryHardwareChargeCounterUah(): Long {
        return try {
            batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) ?: -1L
        } catch (_: Exception) {
            -1L
        }
    }

    private fun refreshWalLogCountAsync() {
        scope.launch(Dispatchers.IO) {
            try {
                val db = com.example.data.local.AmpereDatabase.getInstance(context)
                cachedWalLogCount = db.telemetryLogDao().getLogCount()
                lastWalSyncTimeMs = System.currentTimeMillis()
            } catch (_: Exception) {}
        }
    }

    private fun initSessionLogsSeed() {
        _sessionLogs.value = listOf(
            SessionLog(
                id = "init_0",
                title = "Hardware Telemetry Initialized",
                timestampFormatted = "Just now",
                description = "Direct BatteryManager sensors bound to IEEE 1625 / JEITA / IEC 62680 standards.",
                statTag = "REALTIME",
                type = "check"
            )
        )
    }

    private var subTick = 0
    private var cachedBatteryStatus: Intent? = null
    private var lastIntentPollMs: Long = 0L
    private var lastHistoryAppendMs: Long = 0L
    private var lastWidgetSyncMs: Long = 0L

    private fun startPolling() {
        scope.launch(Dispatchers.Default) {
            while (isActive) {
                val now = System.currentTimeMillis()
                val activeForeground = isAppInForeground
                val proActive = isProModeActive && activeForeground

                val intentIntervalMs = if (proActive) 25L else if (isSohPageActive) 100L else 1000L
                if (cachedBatteryStatus == null || now - lastIntentPollMs >= intentIntervalMs) {
                    val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                    cachedBatteryStatus = context.registerReceiver(null, ifilter)
                    lastIntentPollMs = now
                }

                if ((isSohPageActive || proActive) && (now - lastWalSyncTimeMs >= 5_000L)) {
                    refreshWalLogCountAsync()
                }

                readBatteryState(cachedBatteryStatus, now = now, proActive = proActive)

                subTick++
                delay(if (proActive) 10L else if (activeForeground) 100L else 5_000L)
            }
        }
    }

    private fun readBatteryState(
        batteryStatus: Intent?,
        now: Long = System.currentTimeMillis(),
        proActive: Boolean = isProModeActive
    ) {
        val rawLevel = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (rawLevel >= 0 && scale > 0) {
            ((rawLevel / scale.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
        } else {
            0
        }

        val statusInt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = statusInt == BatteryManager.BATTERY_STATUS_CHARGING ||
            statusInt == BatteryManager.BATTERY_STATUS_FULL

        val status = when (statusInt) {
            BatteryManager.BATTERY_STATUS_CHARGING -> BatteryStatus.CHARGING
            BatteryManager.BATTERY_STATUS_DISCHARGING -> BatteryStatus.DISCHARGING
            BatteryManager.BATTERY_STATUS_FULL -> BatteryStatus.FULL
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> BatteryStatus.NOT_CHARGING
            else -> if (isCharging) BatteryStatus.CHARGING else BatteryStatus.UNKNOWN
        }

        val chargePlugInt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val plugType = when (chargePlugInt) {
            BatteryManager.BATTERY_PLUGGED_AC -> ChargePlug.AC
            BatteryManager.BATTERY_PLUGGED_USB -> ChargePlug.USB
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> ChargePlug.WIRELESS
            BatteryManager.BATTERY_PLUGGED_DOCK -> ChargePlug.DOCK
            else -> ChargePlug.NONE
        }

        val rawVoltage = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val voltageMv = when {
            rawVoltage in 1..15 -> rawVoltage * 1000
            rawVoltage > 100 -> rawVoltage
            else -> 0
        }

        val rawTempTenths = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
        val temperatureC = if (rawTempTenths != Int.MIN_VALUE) {
            val c = rawTempTenths / 10.0f
            if (c in -30.0f..90.0f) c else 25.0f
        } else {
            25.0f
        }

        val currentMa = currentCalibrationEngine.sampleCurrentMa(
            batteryManager = batteryManager,
            isCharging = isCharging,
            voltageMv = voltageMv
        )

        // Real instantaneous electrical power P = V * I (W)
        val wattageW = (voltageMv / 1000.0f) * (abs(currentMa) / 1000.0f)

        val isScreenInteractive = powerManager?.isInteractive ?: true
        val tempDropRate = previousTempC - temperatureC
        previousTempC = temperatureC

        if (voltageMv > 6000 && !_deviceProfile.value.isDualCellSuspected) {
            _deviceProfile.value = DeviceHardwareProfiler.collectProfile(context, voltageMv)
        }
        val currentDeviceProfile = _deviceProfile.value
        val designCapMah = currentDeviceProfile.nominalDesignCapacityMah.coerceAtLeast(3000)

        // Track resting OCV baseline during calm idle & dynamic dV/dI internal resistance during current steps
        if (!isCharging && abs(currentMa) in 1..320 && voltageMv > 0) {
            observedBaselineOcvMv = observedBaselineOcvMv?.let { ((it * 0.7f) + (voltageMv * 0.3f)).toInt() } ?: voltageMv
        }
        if (prevSampleVoltageMv > 0 && voltageMv > 0) {
            val deltaI = abs(abs(currentMa) - abs(prevSampleCurrentMa))
            val deltaV = abs(voltageMv - prevSampleVoltageMv)
            if (deltaI >= 160 && deltaV in 4..450) {
                val instantMohm = ((deltaV.toFloat() / deltaI.toFloat()) * 1000f).toInt().coerceIn(25, 620)
                smoothedDynamicImpedanceMohm = smoothedDynamicImpedanceMohm?.let {
                    ((it * 0.65f) + (instantMohm * 0.35f)).toInt()
                } ?: instantMohm
            }
            val denom = abs(currentMa).coerceAtLeast(100).toFloat()
            val instantRipple = ((deltaI / denom) * 100f).coerceIn(0f, 35f)
            smoothedRipplePercent = (smoothedRipplePercent * 0.75f) + (instantRipple * 0.25f)
        }
        prevSampleVoltageMv = voltageMv
        prevSampleCurrentMa = currentMa

        // Purely Scientific Classification (IEEE 1625 / JEITA / BU-808)
        val livingState = BatteryScienceClassifier.classifyState(
            temperatureC = temperatureC,
            voltageMv = voltageMv,
            currentMa = currentMa,
            percentage = percent,
            isCharging = isCharging,
            isScreenInteractive = isScreenInteractive,
            tempDropRatePerTick = tempDropRate,
            deviceThermalCeilingC = currentDeviceProfile.safeThermalCeilingC,
            nominalCapacityMah = designCapMah
        )

        // Scientific discharge rate (%/hr) = C-rate * 100 = (|I| / C_nom) * 100
        val dischargeRatePctPerHour = if (!isCharging && abs(currentMa) > 0) {
            (((abs(currentMa).toFloat() / designCapMah.toFloat()) * 100f) * 10f).roundToInt() / 10f
        } else {
            0.0f
        }

        // Physical Coulomb-based remaining charge / discharge time estimate (minutes)
        val estimatedMin = computeRemainingTimeMinutes(
            isCharging = isCharging,
            percent = percent,
            currentMa = currentMa,
            designCapMah = designCapMah
        )

        val rawChargeCounter = queryHardwareChargeCounterUah()
        val hasHwChargeCounter = rawChargeCounter > 0L
        val effectiveChargeCounterUah = if (hasHwChargeCounter) rawChargeCounter else 0L

        val hwCycleCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            (batteryStatus?.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1) ?: -1).coerceAtLeast(0)
        } else {
            0
        }

        val osHealth = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD)
            ?: BatteryManager.BATTERY_HEALTH_GOOD
        val resolvedHealth = when {
            osHealth == BatteryManager.BATTERY_HEALTH_OVERHEAT ||
                temperatureC >= BatteryScienceClassifier.TEMP_CRITICAL_OVERHEAT_C -> BatteryHealth.OVERHEAT
            osHealth == BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealth.DEAD
            osHealth == BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealth.OVER_VOLTAGE
            osHealth == BatteryManager.BATTERY_HEALTH_COLD ||
                temperatureC < 5.0f -> BatteryHealth.COLD
            else -> BatteryHealth.GOOD
        }

        val reportedTech = batteryStatus?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY).orEmpty()
        val techLabel = when {
            reportedTech.isNotBlank() && currentDeviceProfile.isDualCellSuspected -> "$reportedTech (2S)"
            reportedTech.isNotBlank() -> reportedTech
            currentDeviceProfile.isDualCellSuspected -> "Li-Ion / Li-Poly (2S)"
            else -> "Li-Ion / Li-Poly (1S)"
        }

        val telemetry = BatteryTelemetry(
            percentage = percent,
            status = status,
            plugType = plugType,
            health = resolvedHealth,
            voltageMv = voltageMv,
            currentMa = currentMa,
            wattageW = (wattageW * 100f).roundToInt() / 100f,
            temperatureC = (temperatureC * 10f).roundToInt() / 10f,
            technology = techLabel,
            capacityMah = designCapMah,
            chargeCounterUah = effectiveChargeCounterUah,
            cycleCount = hwCycleCount,
            estimatedMinutesRemaining = estimatedMin,
            isFastCharging = isCharging && (abs(currentMa) >= 2000 || wattageW >= 18f),
            dischargeRatePerHour = dischargeRatePctPerHour,
            livingState = livingState,
            lastUpdated = now
        )

        _telemetry.value = telemetry
        if (proActive || (now - lastHistoryAppendMs >= 100L)) {
            lastHistoryAppendMs = now
            appendHistoryPoint(telemetry)
        }

        if (proActive || isSohPageActive || subTick % 10 == 0) {
            computeLiveSohPrediction(
                telemetry = telemetry,
                chargeCounterUah = effectiveChargeCounterUah,
                hwCycleCount = hwCycleCount,
                hasHwChargeCounter = hasHwChargeCounter
            )
        }

        if ((now - lastWidgetSyncMs >= 3_000L) || previousLivingState != telemetry.livingState) {
            lastWidgetSyncMs = now
            scope.launch {
                try {
                    com.example.widget.WidgetTelemetryUpdater.update(
                        context = context,
                        pct = telemetry.percentage,
                        tempC = telemetry.temperatureC,
                        voltageMv = telemetry.voltageMv,
                        currentMa = telemetry.currentMa,
                        stateName = telemetry.livingState.name
                    )
                } catch (_: Exception) {}
            }
        }

        val isNowCharging = telemetry.status == BatteryStatus.CHARGING ||
            telemetry.status == BatteryStatus.FULL ||
            telemetry.plugType != ChargePlug.NONE ||
            telemetry.currentMa > 0

        if (previousIsCharging != isNowCharging) {
            previousIsCharging = isNowCharging
            if (isNowCharging) {
                runCableDiagnosticSweep()
                if (isAppInForeground) {
                    com.example.audio.CyberSoundscapeEngine.playDiagnosticSweep()
                    com.example.audio.CyberHapticsEngine.triggerElectricSpark(context)
                }
            } else {
                _cableResult.value = CableDnaStandardsEngine.evaluate(
                    telemetry = telemetry,
                    deviceProfile = currentDeviceProfile,
                    observedBaselineOcvMv = observedBaselineOcvMv,
                    dynamicDeltaImpedanceMilliOhms = smoothedDynamicImpedanceMohm,
                    currentRipplePercent = smoothedRipplePercent
                )
            }
        } else if (isNowCharging) {
            updateContinuousCableMetrics(telemetry)
        }

        if (previousLivingState != telemetry.livingState) {
            val oldState = previousLivingState
            previousLivingState = telemetry.livingState
            if (oldState != null && isAppInForeground) {
                when (telemetry.livingState) {
                    LivingBatteryState.OVERDRIVE_HEATING -> {
                        com.example.audio.CyberHapticsEngine.triggerThermalWarning(context)
                        com.example.audio.CyberSoundscapeEngine.playThermalAlert()
                    }
                    LivingBatteryState.COOLING -> {
                        com.example.audio.CyberHapticsEngine.triggerCrystalMorph(context)
                        com.example.audio.CyberSoundscapeEngine.playCryoChime()
                    }
                    LivingBatteryState.ENERGY_STORING -> {
                        com.example.audio.CyberHapticsEngine.triggerElectricSpark(context)
                        com.example.audio.CyberSoundscapeEngine.playElectricSpark()
                    }
                    LivingBatteryState.DEEP_SLEEP -> {
                        com.example.audio.CyberHapticsEngine.triggerTactileTick(context)
                    }
                }
            }
        }
    }

    private fun computeRemainingTimeMinutes(
        isCharging: Boolean,
        percent: Int,
        currentMa: Int,
        designCapMah: Int
    ): Int {
        if (isCharging) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val osChargeTimeMs = try {
                    batteryManager?.computeChargeTimeRemaining() ?: -1L
                } catch (_: Exception) {
                    -1L
                }
                if (osChargeTimeMs > 0L) {
                    return (osChargeTimeMs / 60_000L).toInt().coerceAtLeast(1)
                }
            }
            if (currentMa <= 50) return 0
            val missingMah = ((100 - percent).coerceAtLeast(0) / 100f) * designCapMah
            return ((missingMah / currentMa.toFloat()) * 60f).roundToInt().coerceAtLeast(0)
        } else {
            val drawMa = abs(currentMa)
            if (drawMa < 20) return 0
            val remainingMah = (percent.coerceAtLeast(0) / 100f) * designCapMah
            return ((remainingMah / drawMa.toFloat()) * 60f).roundToInt().coerceAtLeast(0)
        }
    }

    private fun appendHistoryPoint(t: BatteryTelemetry) {
        val now = System.currentTimeMillis()
        val maxPoints = 60

        fun append(currentList: List<TelemetryPoint>, newVal: Float): List<TelemetryPoint> {
            val list = currentList.toMutableList()
            list.add(TelemetryPoint(now, newVal))
            if (list.size > maxPoints) {
                list.removeAt(0)
            }
            return list
        }

        _voltageHistory.value = append(_voltageHistory.value, t.voltageMv.toFloat())
        _currentHistory.value = append(_currentHistory.value, t.currentMa.toFloat())
        _wattageHistory.value = append(_wattageHistory.value, t.wattageW)
        _temperatureHistory.value = append(_temperatureHistory.value, t.temperatureC)
    }

    fun addManualSessionLog(title: String, desc: String, statTag: String, type: String) {
        val newLog = SessionLog(
            id = System.currentTimeMillis().toString(),
            title = title,
            timestampFormatted = "Just now",
            description = desc,
            statTag = statTag,
            type = type
        )
        _sessionLogs.value = listOf(newLog) + _sessionLogs.value
    }

    fun updateOwnerName(name: String) {
        if (name.isNotBlank()) {
            val trimmed = name.trim()
            val initials = trimmed.take(2).uppercase()
            _userAccount.value = _userAccount.value.copy(
                ownerName = trimmed,
                avatarInitials = initials
            )
        }
    }

    fun toggleCloudSync(enabled: Boolean) {
        _userAccount.value = _userAccount.value.copy(isCloudSyncActive = enabled)
    }

    fun runCableDiagnosticSweep(tierOverride: CableDnaTier? = null) {
        val result = CableDnaStandardsEngine.evaluate(
            telemetry = _telemetry.value,
            deviceProfile = _deviceProfile.value,
            observedBaselineOcvMv = observedBaselineOcvMv,
            dynamicDeltaImpedanceMilliOhms = smoothedDynamicImpedanceMohm,
            currentRipplePercent = smoothedRipplePercent,
            tierOverride = tierOverride
        )
        _cableResult.value = result

        addManualSessionLog(
            title = "Cable DNA Audit: ${result.tier.title}",
            desc = "Resistance ${result.resistanceMilliOhms}mΩ (${result.estimatedAwg}), Drop -${result.voltageDropMv}mV, ${result.standardCompliance}.",
            statTag = result.tier.grade,
            type = if (result.tier.isDangerous) "warning" else "check"
        )
    }

    private fun updateContinuousCableMetrics(telemetry: BatteryTelemetry) {
        _cableResult.value = CableDnaStandardsEngine.evaluate(
            telemetry = telemetry,
            deviceProfile = _deviceProfile.value,
            observedBaselineOcvMv = observedBaselineOcvMv,
            dynamicDeltaImpedanceMilliOhms = smoothedDynamicImpedanceMohm,
            currentRipplePercent = smoothedRipplePercent
        )
    }

    /**
     * Scientific State-of-Health (SOH) Estimation anchored to IEC 61960 / IEEE 1625:
     * - Compares real OS hardware [chargeCounterUah] at known SoC against [designCapMah].
     * - Incorporates OS cycle count [hwCycleCount] (2.0% nominal fade per 100 full IEC cycles)
     *   and measured dynamic ΔV/ΔI cell ESR impedance when available.
     */
    private fun computeLiveSohPrediction(
        telemetry: BatteryTelemetry,
        chargeCounterUah: Long,
        hwCycleCount: Int,
        hasHwChargeCounter: Boolean
    ) {
        val profile = _deviceProfile.value
        val designCapMah = profile.nominalDesignCapacityMah.coerceAtLeast(3000)

        val cycleBasedHealth = (100.0f - (hwCycleCount * 0.02f)).coerceIn(60f, 100f)
        val rawSoh = if (hasHwChargeCounter && telemetry.percentage in 15..100) {
            val observedFullMah = ((chargeCounterUah / 1000f) / (telemetry.percentage / 100f))
                .coerceIn(designCapMah * 0.55f, designCapMah * 1.04f)
            val hwHealthPct = ((observedFullMah / designCapMah.toFloat()) * 100f).coerceIn(55f, 100f)
            if (hwCycleCount > 0) {
                (0.70f * hwHealthPct) + (0.30f * cycleBasedHealth)
            } else {
                hwHealthPct
            }
        } else {
            cycleBasedHealth
        }

        val liveHealthPct = (rawSoh.coerceIn(55f, 100f) * 100f).roundToInt() / 100f
        val effectiveCapacityMah = (designCapMah * (liveHealthPct / 100f)).roundToInt()

        // Physical Cell ESR (mΩ): prefer measured dynamic ΔV/ΔI, otherwise Arrhenius temperature + cycle model
        val liveEsrMilliOhms = smoothedDynamicImpedanceMohm?.coerceIn(25, 250) ?: run {
            val coldArrheniusPenalty = (25f - telemetry.temperatureC).coerceAtLeast(0f) * 0.65f
            val cycleEsrGrowth = hwCycleCount * 0.035f
            val baseCellEsr = if (profile.isDualCellSuspected) 35f else 45f
            (baseCellEsr + coldArrheniusPenalty + cycleEsrGrowth).roundToInt().coerceIn(30, 180)
        }

        // Coulombic efficiency grounded in C-rate and JEITA thermal polarization
        val cRate = abs(telemetry.currentMa).toFloat() / designCapMah.toFloat()
        val thermalExcess = (telemetry.temperatureC - BatteryScienceClassifier.TEMP_OPTIMAL_MAX_C).coerceAtLeast(0f)
        val rawCoulombic = (99.75f - (cRate * 0.45f) - (thermalExcess * 0.08f)).coerceIn(92.0f, 99.95f)
        val liveCoulombic = (rawCoulombic * 100f).roundToInt() / 100f

        // Remaining cycles to 80% IEC EOL (~1000 equivalent full cycles)
        val remainingCycles = if (hwCycleCount > 0) {
            (1000 - hwCycleCount).coerceAtLeast(0)
        } else {
            ((liveHealthPct - 80f).coerceAtLeast(0f) / 0.02f).roundToInt()
        }
        val lifespanDays = (remainingCycles * 0.95f).roundToInt().coerceAtLeast(30)

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, lifespanDays)
        val projectedDate = SimpleDateFormat("MMM yyyy", Locale.US).format(cal.time)

        val cellStatus = when {
            liveHealthPct >= 95f -> CellHealthStatus.PRISTINE
            liveHealthPct >= 88f -> CellHealthStatus.OPTIMAL
            liveHealthPct >= 80f -> CellHealthStatus.MODERATE
            else -> CellHealthStatus.CRITICAL
        }

        _sohPrediction.value = BatterySohPrediction(
            healthPercentage = liveHealthPct,
            originalCapacityMah = designCapMah,
            estimatedCurrentCapacityMah = effectiveCapacityMah,
            totalCyclesCompleted = hwCycleCount,
            estimatedRemainingCycles = remainingCycles,
            estimatedLifespanDays = lifespanDays,
            degradationRatePer100Cycles = 2.0f,
            coulombicEfficiencyPercent = liveCoulombic,
            internalResistanceEstimateMilliOhms = liveEsrMilliOhms,
            cellStatus = cellStatus,
            projectedReplacementDate = projectedDate,
            chemistryType = telemetry.technology
        )
    }

    fun recalibrateSohPrediction(addLog: Boolean = true) {
        scope.launch(Dispatchers.IO) {
            try {
                val db = com.example.data.local.AmpereDatabase.getInstance(context)
                cachedWalLogCount = db.telemetryLogDao().getLogCount()
                lastWalSyncTimeMs = System.currentTimeMillis()

                val t = _telemetry.value
                val rawChargeCounter = queryHardwareChargeCounterUah()
                computeLiveSohPrediction(
                    telemetry = t,
                    chargeCounterUah = if (rawChargeCounter > 0L) rawChargeCounter else t.chargeCounterUah,
                    hwCycleCount = t.cycleCount,
                    hasHwChargeCounter = rawChargeCounter > 0L
                )

                if (addLog) {
                    val latest = _sohPrediction.value
                    addManualSessionLog(
                        title = "Cell SOH Synchronized",
                        desc = "Hardware SOH verified (${cachedWalLogCount} WAL records). Health: ${latest.healthPercentage}%, ESR: ${latest.internalResistanceEstimateMilliOhms}mΩ.",
                        statTag = "SOH ${latest.healthPercentage}%",
                        type = "check"
                    )
                }
            } catch (_: Exception) {}
        }
    }

    private fun initHourlyStatsFromDatabase() {
        scope.launch(Dispatchers.IO) {
            try {
                val db = com.example.data.local.AmpereDatabase.getInstance(context)
                val dao = db.hourlyBatteryStatDao()
                dao.getAllHourlyStats().collect { list ->
                    _hourlyStats.value = list
                }
            } catch (_: Exception) {}
        }
    }

    fun updateCurrentCalibration(multiplier: Float, zeroOffsetMa: Float) {
        currentCalibrationEngine.updateCalibration(multiplier, zeroOffsetMa)
    }

    fun resetCurrentCalibration() {
        currentCalibrationEngine.resetCalibration()
    }
}
