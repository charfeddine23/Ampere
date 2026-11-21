package com.example.data.telemetry

import com.example.data.BatteryHealth
import com.example.data.BatteryStatus
import com.example.data.BatteryTelemetry
import com.example.data.ChargePlug
import com.example.data.DeviceHardwareProfile
import com.example.data.LivingBatteryState
import com.example.data.MetricType
import java.util.Locale
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

    fun assessThermalZone(
        temperatureC: Float,
        isCharging: Boolean,
        isArabic: Boolean = true
    ): ThermalZoneAssessment {
        return when {
            temperatureC >= TEMP_CRITICAL_OVERHEAT_C -> ThermalZoneAssessment(
                zoneTitleAr = if (isArabic) "خطر حراري حرج (≥45°C)" else "Critical Thermal Danger (≥45°C)",
                scientificRange = if (isArabic) "≥ 45.0°C (حد القطع JEITA)" else "≥ 45.0°C (JEITA Cutoff)",
                detailAr = if (isArabic) {
                    "تسارع تحلل طبقة SEI وأكسدة الإلكتروليت — يجب إيقاف الشحن أو تبريد الجهاز فوراً."
                } else {
                    "Accelerated SEI breakdown & electrolyte oxidation — halt charging or cool device immediately."
                },
                severityLevel = 3
            )
            temperatureC >= TEMP_THERMAL_STRESS_C -> ThermalZoneAssessment(
                zoneTitleAr = if (isArabic) "إجهاد حراري زائد (42–44.9°C)" else "Thermal Stress Zone (42–44.9°C)",
                scientificRange = if (isArabic) "42.0–44.9°C (إجهاد IEEE 1625)" else "42.0–44.9°C (IEEE 1625 Stress)",
                detailAr = if (isArabic) {
                    "فوق 42°C تتضاعف وتيرة فقدان السعة وفق معادلة أرهينيوس؛ يُنصح بتقليل الحمل الحراري."
                } else {
                    "Above 42°C capacity fade accelerates exponentially per Arrhenius kinetics; reduce thermal load."
                },
                severityLevel = 3
            )
            temperatureC > TEMP_OPTIMAL_MAX_C -> ThermalZoneAssessment(
                zoneTitleAr = if (isArabic) "دافئ نشط (35.1–41.9°C)" else "Active Warm Window (35.1–41.9°C)",
                scientificRange = if (isArabic) "35.1–41.9°C (ضمن هامش الشحن السريع)" else "35.1–41.9°C (Fast-Charge Margin)",
                detailAr = if (isArabic) {
                    if (isCharging) {
                        "حرارة متوقعة أثناء الشحن السريع PD/PPS ولا تُعد خطراً ما دامت دون 42°C."
                    } else {
                        "حمل معالج متوسط أثناء التفريغ — لا يزال دون عتبة الإجهاد الحراري (42°C)."
                    }
                } else {
                    if (isCharging) {
                        "Expected thermal rise during PD/PPS fast charging; safe while remaining below 42°C."
                    } else {
                        "Moderate processor discharge load — still safely below the 42°C thermal stress threshold."
                    }
                },
                severityLevel = 2
            )
            temperatureC < TEMP_COLD_LIMIT_C -> ThermalZoneAssessment(
                zoneTitleAr = if (isArabic) "برودة منخفضة (<15°C)" else "Cold Sub-Optimal (<15°C)",
                scientificRange = if (isArabic) "< 15.0°C (حماية الطلاء الليثيومي)" else "< 15.0°C (Li-Plating Guard)",
                detailAr = if (isArabic) {
                    "الشحن السريع تحت 15°C يزيد خطر ترسّب الليثيوم المعدني (Li Plating) على الأنود."
                } else {
                    "Fast charging below 15°C increases metallic lithium plating risk on the graphite anode."
                },
                severityLevel = 0
            )
            else -> ThermalZoneAssessment(
                zoneTitleAr = if (isArabic) "النطاق الحراري المثالي (15–35°C)" else "Optimal Thermal Window (15–35°C)",
                scientificRange = if (isArabic) "20.0–35.0°C (المعيار الذهبي)" else "20.0–35.0°C (Gold Standard)",
                detailAr = if (isArabic) {
                    "أعلى موصلية أيونية مع أقل معدل تآكل كيميائي للخلية حسب دراسات BU-808."
                } else {
                    "Peak ionic conductivity with minimal chemical cell degradation per BU-808 studies."
                },
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
        deviceProfile: DeviceHardwareProfile,
        isArabic: Boolean = true
    ): LivingStateScienceProfile {
        val isCharging = telemetry.status == BatteryStatus.CHARGING || telemetry.plugType != ChargePlug.NONE
        val thermal = assessThermalZone(telemetry.temperatureC, isCharging, isArabic)
        val designMah = deviceProfile.nominalDesignCapacityMah.coerceAtLeast(3000)
        val cRate = (abs(telemetry.currentMa).toFloat() / designMah.toFloat())
        val isDualCell = telemetry.voltageMv > 6000 || deviceProfile.isDualCellSuspected
        val safeVoltageWindow = if (isDualCell) "6.60–8.90 V (2S)" else "3.40–4.48 V (1S)"

        val voltageMetric = ModeOrderedMetric(
            type = MetricType.VOLTAGE,
            titleAr = if (isArabic) "جهد الخلية" else "Cell Voltage",
            formattedValue = "${telemetry.voltageMv} mV",
            standardRangeAr = if (isArabic) "النطاق القياسي: $safeVoltageWindow" else "Nominal Window: $safeVoltageWindow",
            isPriorityWarning = telemetry.voltageMv > (if (isDualCell) 9050 else 4520)
        )
        val currentMetric = ModeOrderedMetric(
            type = MetricType.CURRENT,
            titleAr = if (isArabic) {
                if (isCharging) "التيار الداخل (C-Rate)" else "التيار المسحوب"
            } else {
                if (isCharging) "Charge Current (C-Rate)" else "Discharge Current (Load)"
            },
            formattedValue = "${if (telemetry.currentMa >= 0) "+" else ""}${telemetry.currentMa} mA (${String.format(Locale.US, "%.2f", cRate)}C)",
            standardRangeAr = if (isArabic) {
                if (isCharging) "الحد الآمن المستدام ≤ 1.20C" else "الخمول المثالي < 0.08C"
            } else {
                if (isCharging) "Sustained Safe ≤ 1.20C" else "Optimal Idle < 0.08C"
            },
            isPriorityWarning = cRate > 1.35f
        )
        val tempMetric = ModeOrderedMetric(
            type = MetricType.TEMPERATURE,
            titleAr = if (isArabic) "حرارة القلب" else "Core Temperature",
            formattedValue = "${telemetry.temperatureC} °C",
            standardRangeAr = if (isArabic) "مثالي 20–35°C • إجهاد ≥42°C" else "Optimal 20–35°C • Stress ≥42°C",
            isPriorityWarning = thermal.severityLevel >= 3
        )
        val wattageMetric = ModeOrderedMetric(
            type = MetricType.WATTAGE,
            titleAr = if (isArabic) "القدرة اللحظية" else "Instant Power",
            formattedValue = "${String.format(Locale.US, "%.1f", telemetry.wattageW)} W",
            standardRangeAr = if (isArabic) {
                "هدف ${deviceProfile.brand}: ${deviceProfile.recommendedPowerWatts}W"
            } else {
                "${deviceProfile.brand} Target: ${deviceProfile.recommendedPowerWatts}W"
            },
            isPriorityWarning = false
        )

        val (reasonAr, phaseAr, ordered, reference) = when (telemetry.livingState) {
            LivingBatteryState.OVERDRIVE_HEATING -> QuadData(
                reason = if (isArabic) {
                    "تجاوز الحرارة العتبة العلمية للإجهاد (${deviceProfile.safeThermalCeilingC}°C) أو حمل C-Rate حراري مرتفع."
                } else {
                    "Temperature exceeded the scientific stress threshold (${deviceProfile.safeThermalCeilingC}°C) or high thermal C-rate load."
                },
                phase = if (isArabic) "الإجهاد الحراري (حماية JEITA)" else "Thermal Stress (JEITA Guard)",
                ordered = listOf(tempMetric, currentMetric, wattageMetric, voltageMetric),
                reference = if (isArabic) {
                    "IEEE 1625 & JEITA: فوق 42°C يتسارع تحلل SEI ويلزم خفض التيار"
                } else {
                    "IEEE 1625 & JEITA: Above 42°C SEI breakdown accelerates and current throttling applies"
                }
            )
            LivingBatteryState.ENERGY_STORING -> QuadData(
                reason = if (isArabic) {
                    if (telemetry.percentage >= 80) {
                        "مرحلة الجهد الثابت (CV Taper): تيار مضبوط آلياً لحماية الكاثود قرب الامتلاء."
                    } else {
                        "شحن نشط ضمن نافذة JEITA الآمنة (${telemetry.temperatureC}°C) بتدفق تيار مستقر."
                    }
                } else {
                    if (telemetry.percentage >= 80) {
                        "Constant-Voltage Taper (CV): current is stepped down cleanly to protect the cathode near full charge."
                    } else {
                        "Active charging inside JEITA safe thermal window (${telemetry.temperatureC}°C) with steady current."
                    }
                },
                phase = if (isArabic) {
                    if (telemetry.percentage >= 80) "مرحلة تشبّع الجهد الثابت (≥80%)" else "الشحن السريع بالتيار الثابت (<80%)"
                } else {
                    if (telemetry.percentage >= 80) "CV Saturation Taper (≥80%)" else "CC Fast Charge (<80%)"
                },
                ordered = listOf(currentMetric, wattageMetric, voltageMetric, tempMetric),
                reference = if (isArabic) {
                    "BU-409 CC/CV: الذروة 0.5C–1.0C عند 20–35°C ثم خفض تدريجي بعد 80%"
                } else {
                    "BU-409 CC/CV: Peak 0.5C–1.0C at 20–35°C followed by progressive taper above 80%"
                }
            )
            LivingBatteryState.COOLING -> QuadData(
                reason = if (isArabic) {
                    if (telemetry.temperatureC < TEMP_COLD_LIMIT_C) {
                        "حرارة منخفضة (<15°C): يُنصح بالشحن الهادئ لتجنب طلاء الليثيوم (Li Plating)."
                    } else {
                        "انخفاض حراري نشط واستقرار في درجة حرارة الخلية نحو النطاق الذهبي (20–35°C)."
                    }
                } else {
                    if (telemetry.temperatureC < TEMP_COLD_LIMIT_C) {
                        "Low temperature (<15°C): gentle charging is advised to prevent metallic Li plating."
                    } else {
                        "Active thermal recovery stabilizing cell temperature toward the 20–35°C golden window."
                    }
                },
                phase = if (isArabic) "استقرار حراري (تبديد نشط)" else "Thermal Recovery (Stabilizing)",
                ordered = listOf(tempMetric, voltageMetric, currentMetric, wattageMetric),
                reference = if (isArabic) {
                    "JEITA Thermal Window: التبريد نحو 25°C يضاعف كفاءة دورة الشحن"
                } else {
                    "JEITA Thermal Window: Cooling toward 25°C preserves optimal cycle longevity"
                }
            )
            LivingBatteryState.DEEP_SLEEP -> QuadData(
                reason = if (isArabic) {
                    "سحب خامل منخفض جداً (${abs(telemetry.currentMa)}mA = ${String.format(Locale.US, "%.2f", cRate)}C) وحرارة هادئة لإطالة العمر الافتراضي."
                } else {
                    "Ultra-low quiescent draw (${abs(telemetry.currentMa)}mA = ${String.format(Locale.US, "%.2f", cRate)}C) and calm temperature preserving cell longevity."
                },
                phase = if (isArabic) "سكون كهربائي (استقرار الجهد)" else "Quiescent Longevity (Resting OCV)",
                ordered = listOf(currentMetric, voltageMetric, tempMetric, wattageMetric),
                reference = if (isArabic) {
                    "BU-808 Shelf Life: التيار الخامل <0.08C عند 18–30°C يقلل التآكل السنوي"
                } else {
                    "BU-808 Shelf Life: Quiescent draw <0.08C at 18–30°C minimizes annual fade"
                }
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
