package com.example.data.telemetry

import com.example.data.BatteryHealth
import com.example.data.BatteryStatus
import com.example.data.BatteryTelemetry
import com.example.data.ChargePlug
import com.example.data.DeviceHardwareProfile
import com.example.data.LivingBatteryState
import com.example.data.MetricType
import kotlin.math.abs

/**
 * Scientific Battery Classification & Mode-Ordered Telemetry Engine
 * Grounded in IEEE 1625, JEITA (Japan Electronics & Information Technology Industries Assoc.),
 * IEC 62133, and Battery University (BU-409 / BU-410 / BU-808) Li-Ion electrochemistry studies.
 */
data class ThermalZoneAssessment(
    val zoneTitleAr: String,
    val scientificRange: String,
    val detailAr: String,
    val severityLevel: Int // 0 = Cold (<15°C), 1 = Optimal (15..35°C), 2 = Warm Active (35..41.9°C), 3 = Thermal Stress (>=42°C)
)

data class ModeOrderedMetric(
    val type: MetricType,
    val titleAr: String,
    val formattedValue: String,
    val standardRangeAr: String,
    val isPriorityWarning: Boolean = false
)

data class LivingStateScienceProfile(
    val evaluatedState: LivingBatteryState,
    val reasonAr: String,
    val phaseLabelAr: String,
    val thermalAssessment: ThermalZoneAssessment,
    val cRate: Float,
    val orderedMetrics: List<ModeOrderedMetric>,
    val scienceReference: String
)

object BatteryScienceClassifier {

    // JEITA / IEEE 1625 Li-Ion Scientific Thermal Thresholds (°C)
    const val TEMP_COLD_LIMIT_C = 15.0f
    const val TEMP_OPTIMAL_MIN_C = 20.0f
    const val TEMP_OPTIMAL_MAX_C = 35.0f
    const val TEMP_THERMAL_STRESS_C = 42.0f   // Acceleration of SEI decomposition & electrolyte oxidation (Arrhenius law)
    const val TEMP_CRITICAL_OVERHEAT_C = 45.0f // Hardware JEITA safety cutoff zone

    fun assessThermalZone(temperatureC: Float, isCharging: Boolean): ThermalZoneAssessment {
        return when {
            temperatureC >= TEMP_CRITICAL_OVERHEAT_C -> ThermalZoneAssessment(
                zoneTitleAr = "خطر حراري حرج (≥45°C)",
                scientificRange = "≥ 45.0°C (حد القطع JEITA)",
                detailAr = "تسارع تحلل طبقة SEI وأكسدة الإلكتروليت — يجب إيقاف الشحن أو تبريد الجهاز فوراً.",
                severityLevel = 3
            )
            temperatureC >= TEMP_THERMAL_STRESS_C -> ThermalZoneAssessment(
                zoneTitleAr = "إجهاد حراري زائد (42–44.9°C)",
                scientificRange = "42.0–44.9°C (إجهاد IEEE 1625)",
                detailAr = "فوق 42°C تتضاعف وتيرة فقدان السعة وفق معادلة أرهينيوس؛ يُنصح بتقليل الحمل الحراري.",
                severityLevel = 3
            )
            temperatureC > TEMP_OPTIMAL_MAX_C -> ThermalZoneAssessment(
                zoneTitleAr = "دافئ نشط (35.1–41.9°C)",
                scientificRange = "35.1–41.9°C (ضمن هامش الشحن السريع)",
                detailAr = if (isCharging) {
                    "حرارة متوقعة أثناء الشحن السريع PD/PPS ولا تُعد خطراً ما دامت دون 42°C."
                } else {
                    "حمل معالج متوسط أثناء التفريغ — لا يزال دون عتبة الإجهاد الحراري (42°C)."
                },
                severityLevel = 2
            )
            temperatureC < TEMP_COLD_LIMIT_C -> ThermalZoneAssessment(
                zoneTitleAr = "برودة منخفضة (<15°C)",
                scientificRange = "< 15.0°C (حماية الطلاء الليثيومي)",
                detailAr = "الشحن السريع تحت 15°C يزيد خطر ترسّب الليثيوم المعدني (Li Plating) على الأنود.",
                severityLevel = 0
            )
            else -> ThermalZoneAssessment(
                zoneTitleAr = "النطاق الحراري المثالي (15–35°C)",
                scientificRange = "20.0–35.0°C (المعيار الذهبي)",
                detailAr = "أعلى موصلية أيونية مع أقل معدل تآكل كيميائي للخلية حسب دراسات BU-808.",
                severityLevel = 1
            )
        }
    }

    fun classifyState(
        temperatureC: Float,
        voltageMv: Int,
        currentMa: Int,
        percentage: Int,
        isCharging: Boolean,
        isScreenInteractive: Boolean,
        tempDropRatePerTick: Float,
        deviceThermalCeilingC: Float = TEMP_THERMAL_STRESS_C,
        nominalCapacityMah: Int = 5000
    ): LivingBatteryState {
        val isDualCell = voltageMv > 6000
        val maxSafeCellMv = if (isDualCell) 9100 else 4550
        val minSafeCellMv = if (isDualCell) 6200 else 3100
        val safeCap = nominalCapacityMah.coerceAtLeast(3000).toFloat()
        val cRate = abs(currentMa).toFloat() / safeCap

        // 1. THERMAL STRESS (IEEE 1625 / JEITA >= 42°C or cell ceiling, abnormal voltage excursion, or rapid heating under >= 0.60C Joule load)
        val effectiveStressCeiling = deviceThermalCeilingC.coerceIn(40.5f, 43.0f)
        val isThermalOverdrive = temperatureC >= effectiveStressCeiling ||
            (temperatureC >= 40.5f && cRate >= 0.60f && tempDropRatePerTick < -0.04f)
        val isAbnormalVoltageFault = voltageMv > maxSafeCellMv || (voltageMv in 1..minSafeCellMv)
        if (isThermalOverdrive || isAbnormalVoltageFault) {
            return LivingBatteryState.OVERDRIVE_HEATING
        }

        // 2. THERMAL RECOVERY / COLD PROTECTION (JEITA <15°C Li-plating limit OR measurable physical cooling drop)
        val isRapidThermalDrop = tempDropRatePerTick >= 0.08f && temperatureC <= 39.0f
        if (temperatureC < TEMP_COLD_LIMIT_C || isRapidThermalDrop) {
            return LivingBatteryState.COOLING
        }

        // 3. ACTIVE CHARGING (CC/CV charging above final trickle termination threshold)
        val isFinalTrickleHold = isCharging && percentage >= 92 && abs(currentMa) <= 150
        if ((isCharging || currentMa > 200) && !isFinalTrickleHold) {
            return LivingBatteryState.ENERGY_STORING
        }

        // 4. QUIESCENT STANDBY (Low C-rate <= 0.08C screen-off/idle, OR final CV trickle hold)
        val isQuiescentIdle = (!isScreenInteractive && cRate <= 0.08f) ||
            (!isCharging && abs(currentMa) <= 380 && temperatureC < 36.0f) ||
            isFinalTrickleHold
        if (isQuiescentIdle) {
            return LivingBatteryState.DEEP_SLEEP
        }

        // 5. Active normal discharge below thermal stress ceiling
        return if (temperatureC <= TEMP_OPTIMAL_MAX_C && cRate <= 0.30f) {
            LivingBatteryState.COOLING
        } else {
            LivingBatteryState.DEEP_SLEEP
        }
    }

    /**
     * Builds the full scientific breakdown & orders telemetry measurements dynamically
     * according to engineering priority for the currently active LivingBatteryState.
     */
    fun buildScienceProfile(
        telemetry: BatteryTelemetry,
        deviceProfile: DeviceHardwareProfile
    ): LivingStateScienceProfile {
        val isCharging = telemetry.status == BatteryStatus.CHARGING || telemetry.plugType != ChargePlug.NONE
        val thermal = assessThermalZone(telemetry.temperatureC, isCharging)
        val designMah = deviceProfile.nominalDesignCapacityMah.coerceAtLeast(3000)
        val cRate = (abs(telemetry.currentMa).toFloat() / designMah.toFloat())
        val isDualCell = telemetry.voltageMv > 6000 || deviceProfile.isDualCellSuspected
        val safeVoltageWindow = if (isDualCell) "6.60–8.90 V (2S)" else "3.40–4.48 V (1S)"

        val voltageMetric = ModeOrderedMetric(
            type = MetricType.VOLTAGE,
            titleAr = "جهد الخلية (Voltage)",
            formattedValue = "${telemetry.voltageMv} mV",
            standardRangeAr = "النطاق القياسي: $safeVoltageWindow",
            isPriorityWarning = telemetry.voltageMv > (if (isDualCell) 9050 else 4520)
        )
        val currentMetric = ModeOrderedMetric(
            type = MetricType.CURRENT,
            titleAr = if (isCharging) "التيار الداخل (C-Rate)" else "التيار المسحوب (Load)",
            formattedValue = "${if (telemetry.currentMa >= 0) "+" else ""}${telemetry.currentMa} mA (${String.format("%.2f", cRate)}C)",
            standardRangeAr = if (isCharging) "الحد الآمن المستدام ≤ 1.20C" else "الخمول المثالي < 0.08C",
            isPriorityWarning = cRate > 1.35f
        )
        val tempMetric = ModeOrderedMetric(
            type = MetricType.TEMPERATURE,
            titleAr = "حرارة القلب (Core Temp)",
            formattedValue = "${telemetry.temperatureC} °C",
            standardRangeAr = "مثالي 20–35°C • إجهاد ≥42°C",
            isPriorityWarning = thermal.severityLevel >= 3
        )
        val wattageMetric = ModeOrderedMetric(
            type = MetricType.WATTAGE,
            titleAr = "القدرة اللحظية (Power)",
            formattedValue = "${String.format("%.1f", telemetry.wattageW)} W",
            standardRangeAr = "هدف ${deviceProfile.brand}: ${deviceProfile.recommendedPowerWatts}W",
            isPriorityWarning = false
        )

        val (reasonAr, phaseAr, ordered, reference) = when (telemetry.livingState) {
            LivingBatteryState.OVERDRIVE_HEATING -> QuadData(
                reason = "تجاوز الحرارة العتبة العلمية للإجهاد (${deviceProfile.safeThermalCeilingC}°C) أو حمل C-Rate حراري مرتفع.",
                phase = "الإجهاد الحراري (JEITA Protection)",
                ordered = listOf(tempMetric, currentMetric, wattageMetric, voltageMetric), // Temp #1
                reference = "IEEE 1625 & JEITA: فوق 42°C يتسارع تحلل SEI ويلزم خفض التيار"
            )
            LivingBatteryState.ENERGY_STORING -> QuadData(
                reason = if (telemetry.percentage >= 80) {
                    "مرحلة الجهد الثابت (CV Taper): تيار مضبوط آلياً لحماية الكاثود قرب الامتلاء."
                } else {
                    "شحن نشط ضمن نافذة JEITA الآمنة (${telemetry.temperatureC}°C) بتدفق تيار مستقر."
                },
                phase = if (telemetry.percentage >= 80) "CV Saturation Taper (≥80%)" else "CC Fast Charge (<80%)",
                ordered = listOf(currentMetric, wattageMetric, voltageMetric, tempMetric), // Current & Wattage #1
                reference = "BU-409 CC/CV: الذروة 0.5C–1.0C عند 20–35°C ثم خفض تدريجي بعد 80%"
            )
            LivingBatteryState.COOLING -> QuadData(
                reason = if (telemetry.temperatureC < TEMP_COLD_LIMIT_C) {
                    "حرارة منخفضة (<15°C): يُنصح بالشحن الهادئ لتجنب طلاء الليثيوم (Li Plating)."
                } else {
                    "انخفاض حراري نشط واستقرار في درجة حرارة الخلية نحو النطاق الذهبي (20–35°C)."
                },
                phase = "استقرار حراري (Thermal Recovery)",
                ordered = listOf(tempMetric, voltageMetric, currentMetric, wattageMetric), // Temp & Voltage stability #1
                reference = "JEITA Thermal Window: التبريد نحو 25°C يضاعف كفاءة دورة الشحن"
            )
            LivingBatteryState.DEEP_SLEEP -> QuadData(
                reason = "سحب خامل منخفض جداً (${abs(telemetry.currentMa)}mA = ${String.format("%.2f", cRate)}C) وحرارة هادئة لإطالة العمر الافتراضي.",
                phase = "سكون كهربائي (Quiescent Longevity)",
                ordered = listOf(currentMetric, voltageMetric, tempMetric, wattageMetric), // Quiescent current & resting OCV #1
                reference = "BU-808 Shelf Life: التيار الخامل <0.08C عند 18–30°C يقلل التآكل السنوي"
            )
        }

        return LivingStateScienceProfile(
            evaluatedState = telemetry.livingState,
            reasonAr = reasonAr,
            phaseLabelAr = phaseAr,
            thermalAssessment = thermal,
            cRate = cRate,
            orderedMetrics = ordered,
            scienceReference = reference
        )
    }

    private data class QuadData(
        val reason: String,
        val phase: String,
        val ordered: List<ModeOrderedMetric>,
        val reference: String
    )
}
