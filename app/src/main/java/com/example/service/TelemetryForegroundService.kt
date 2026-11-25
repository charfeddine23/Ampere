package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.data.BatteryMonitorRepository
import com.example.data.GlassThemeConfig
import com.example.data.local.AmpereDatabase
import com.example.data.local.HourlyBatteryStatEntity
import com.example.data.local.SettingsPreferencesManager
import com.example.data.local.TelemetryLogEntity
import com.example.data.telemetry.CurrentCalibrationEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

/**
 * Persistent Foreground Service ensuring background telemetry tracking
 * and Room Database logging strictly when enabled by the user.
 */
class TelemetryForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: AmpereDatabase
    private lateinit var prefsManager: SettingsPreferencesManager
    private lateinit var calibrationEngine: CurrentCalibrationEngine

    private var lastNotifiedPct: Int = -1
    private var lastNotifiedState: String = ""
    private var lastNotificationUpdateMs: Long = 0L
    private var insertCountSinceTrim: Int = 0
    @Volatile
    private var cachedConfig: GlassThemeConfig = GlassThemeConfig()
    private var lastConfigReloadMs: Long = 0L

    private var activeHourKey: Long = -1L
    private var hourSamples: Int = 0
    private var hourVoltageSum: Long = 0L
    private var hourMinVoltage: Int = Int.MAX_VALUE
    private var hourMaxVoltage: Int = 0
    private var hourCurrentSum: Long = 0L
    private var hourMinCurrent: Int = Int.MAX_VALUE
    private var hourMaxCurrent: Int = Int.MIN_VALUE
    private var hourTempSum: Double = 0.0
    private var hourMaxTemp: Float = -99f
    private var hourWattSum: Double = 0.0
    private var hourStartPct: Int = -1
    private var hourLastSaveMs: Long = 0L

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        database = AmpereDatabase.getInstance(applicationContext)
        prefsManager = SettingsPreferencesManager(applicationContext)
        val initialConfig = prefsManager.loadThemeConfig()
        cachedConfig = initialConfig
        lastConfigReloadMs = System.currentTimeMillis()
        calibrationEngine = CurrentCalibrationEngine(
            context = applicationContext,
            initialMultiplier = initialConfig.currentMultiplier,
            initialOffsetMa = initialConfig.currentZeroOffsetMa
        )

        val isAr = initialConfig.appLanguage.isArabic
        createNotificationChannel(isAr)
        promoteToForeground(
            title = if (isAr) "فولت كور نشط" else "Volt Core Active",
            text = if (isAr) "المراقبة الصامتة للعتاد نشطة" else "Silent Hardware Telemetry Active"
        )

        if (!initialConfig.backgroundServiceEnabled) {
            stopSelf()
            return
        }

        startTelemetrySamplingLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val config = prefsManager.loadThemeConfig()
        cachedConfig = config
        lastConfigReloadMs = System.currentTimeMillis()
        if (!config.backgroundServiceEnabled) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val isAr = config.appLanguage.isArabic
        createNotificationChannel(isAr)
        calibrationEngine.updateCalibration(
            newMultiplier = config.currentMultiplier,
            newOffsetMa = config.currentZeroOffsetMa
        )
        promoteToForeground(
            title = if (isAr) "فولت كور نشط" else "Volt Core Active",
            text = if (isAr) "المراقبة الصامتة للعتاد نشطة" else "Silent Hardware Telemetry Active"
        )
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf(startId)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        serviceScope.cancel()
    }

    private fun promoteToForeground(title: String, text: String) {
        try {
            val notification = createNotification(title, text)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, 0)
            }
        } catch (_: Exception) {
            stopSelf()
        }
    }

    private fun startTelemetrySamplingLoop() {
        serviceScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                if (now - lastConfigReloadMs >= 60_000L) {
                    cachedConfig = prefsManager.loadThemeConfig()
                    lastConfigReloadMs = now
                }
                val config = cachedConfig
                if (!config.backgroundServiceEnabled) {
                    stopSelf()
                    break
                }
                calibrationEngine.updateCalibration(
                    newMultiplier = config.currentMultiplier,
                    newOffsetMa = config.currentZeroOffsetMa
                )
                sampleAndPersistTelemetry(config)
                // Save to Room every 20 seconds to minimize background wakeups and DB churn
                delay(20_000L)
            }
        }
    }

    private suspend fun sampleAndPersistTelemetry(config: GlassThemeConfig) {
        try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val bStatus = registerReceiver(null, ifilter) ?: return

            val rawLevel = bStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = bStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (rawLevel < 0 || scale <= 0) return
            val pct = ((rawLevel / scale.toFloat()) * 100).toInt().coerceIn(0, 100)

            val rawVoltage = bStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
            val voltageMv = BatteryMonitorRepository.normalizeVoltageMv(rawVoltage.toLong())
            if (voltageMv <= 0) return

            val rawTemp = bStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            if (rawTemp == Int.MIN_VALUE) return
            val tempC = (rawTemp / 10.0f).coerceIn(-30.0f, 90.0f)

            val bm = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val statusInt = bStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isFull = statusInt == BatteryManager.BATTERY_STATUS_FULL
            val isCharging = statusInt == BatteryManager.BATTERY_STATUS_CHARGING || isFull

            val currentMa = calibrationEngine.sampleCurrentMa(
                batteryManager = bm,
                isCharging = isCharging,
                voltageMv = voltageMv,
                isFull = isFull
            )

            val stateString = when {
                tempC >= config.highTempThresholdC -> "HEATING"
                isCharging -> "STORING"
                tempC < 15.0f -> "COOLING"
                else -> "SLEEP"
            }

            val entity = TelemetryLogEntity(
                voltageMv = voltageMv,
                currentMa = currentMa,
                temperatureC = tempC,
                batteryPct = pct,
                state = stateString
            )
            database.telemetryLogDao().insertLog(entity)
            insertCountSinceTrim++
            if (insertCountSinceTrim >= 20) {
                insertCountSinceTrim = 0
                database.telemetryLogDao().trimOldLogs(2000)
            }

            recordHourlyStat(
                pct = pct,
                voltageMv = voltageMv,
                currentMa = currentMa,
                tempC = tempC,
                stateString = stateString,
                isCharging = isCharging
            )

            // Throttle notification updates to prevent Android notification rate-limit warnings
            val now = System.currentTimeMillis()
            val shouldUpdateNotification = pct != lastNotifiedPct ||
                stateString != lastNotifiedState ||
                (now - lastNotificationUpdateMs >= 60_000L)

            if (shouldUpdateNotification) {
                lastNotifiedPct = pct
                lastNotifiedState = stateString
                lastNotificationUpdateMs = now
                val isAr = config.appLanguage.isArabic
                val currentText = if (currentMa >= 0) "+$currentMa" else "$currentMa"
                val notificationText = "${pct}% • ${voltageMv}mV • ${currentText}mA • ${String.format(Locale.US, "%.1f", tempC)}°C"
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.notify(
                    NOTIFICATION_ID,
                    createNotification(
                        if (isAr) "قياسات Volt Core" else "Volt Core Telemetry",
                        notificationText
                    )
                )
            }
        } catch (_: Exception) {
            // Guard against background receiver quirks
        }
    }

    private suspend fun recordHourlyStat(
        pct: Int,
        voltageMv: Int,
        currentMa: Int,
        tempC: Float,
        stateString: String,
        isCharging: Boolean
    ) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val hourStart = cal.timeInMillis
        if (activeHourKey != hourStart) {
            activeHourKey = hourStart
            hourSamples = 0
            hourVoltageSum = 0L
            hourMinVoltage = Int.MAX_VALUE
            hourMaxVoltage = 0
            hourCurrentSum = 0L
            hourMinCurrent = Int.MAX_VALUE
            hourMaxCurrent = Int.MIN_VALUE
            hourTempSum = 0.0
            hourMaxTemp = -99f
            hourWattSum = 0.0
            hourStartPct = pct
            hourLastSaveMs = 0L
        }

        hourSamples++
        hourVoltageSum += voltageMv
        if (voltageMv < hourMinVoltage) hourMinVoltage = voltageMv
        if (voltageMv > hourMaxVoltage) hourMaxVoltage = voltageMv
        hourCurrentSum += currentMa
        if (currentMa < hourMinCurrent) hourMinCurrent = currentMa
        if (currentMa > hourMaxCurrent) hourMaxCurrent = currentMa
        hourTempSum += tempC
        if (tempC > hourMaxTemp) hourMaxTemp = tempC
        hourWattSum += (voltageMv / 1000.0) * (abs(currentMa) / 1000.0)

        val now = System.currentTimeMillis()
        if (hourLastSaveMs == 0L || (now - hourLastSaveMs) >= 180_000L) {
            hourLastSaveMs = now
            val timeFmt = SimpleDateFormat("HH:mm", Locale.US).format(cal.time)
            val dateFmt = SimpleDateFormat("MMM d", Locale.US).format(cal.time)
            val avgI = (hourCurrentSum / hourSamples).toInt()
            val netMah = ((avgI * hourSamples * 20.0) / 3600.0).toInt()
            database.hourlyBatteryStatDao().insertHourlyStat(
                HourlyBatteryStatEntity(
                    id = hourStart,
                    hourTimestamp = hourStart,
                    formattedTime = timeFmt,
                    formattedDate = dateFmt,
                    avgVoltageMv = (hourVoltageSum / hourSamples).toInt(),
                    minVoltageMv = hourMinVoltage,
                    maxVoltageMv = hourMaxVoltage,
                    avgCurrentMa = avgI,
                    minCurrentMa = hourMinCurrent,
                    maxCurrentMa = hourMaxCurrent,
                    avgTemperatureC = (hourTempSum / hourSamples).toFloat(),
                    maxTemperatureC = hourMaxTemp,
                    avgWattageW = (hourWattSum / hourSamples).toFloat(),
                    startBatteryPct = hourStartPct,
                    endBatteryPct = pct,
                    netMahFlow = netMah,
                    dominantState = stateString,
                    dominantStateLabel = stateString,
                    sampleCount = hourSamples,
                    isChargingHour = isCharging,
                    screenOnDurationMinutes = 0
                )
            )
        }
    }

    private fun createNotificationChannel(isArabic: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                if (isArabic) "خدمة قياسات Volt Core" else "Volt Core Telemetry Service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = if (isArabic) {
                    "مؤشر صامت عند تفعيل مراقبة البطارية في الخلفية."
                } else {
                    "Silent foreground indicator when background battery monitoring is enabled."
                }
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, text: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "ampere_pulse_telemetry_channel_v2"
        const val NOTIFICATION_ID = 1001
        @Volatile
        var isServiceRunning: Boolean = false
            private set

        fun startService(context: Context) {
            try {
                val appContext = context.applicationContext
                val enabled = SettingsPreferencesManager(appContext).loadThemeConfig().backgroundServiceEnabled
                if (!enabled) return

                val intent = Intent(appContext, TelemetryForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appContext.startForegroundService(intent)
                } else {
                    appContext.startService(intent)
                }
            } catch (_: Exception) {
                // Safeguard against ForegroundServiceStartNotAllowedException on Android 12+
            }
        }

        fun stopService(context: Context) {
            try {
                val appContext = context.applicationContext
                val intent = Intent(appContext, TelemetryForegroundService::class.java)
                appContext.stopService(intent)
            } catch (_: Exception) {}
        }
    }
}
