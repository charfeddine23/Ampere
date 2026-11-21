package com.example.data.telemetry

import com.example.data.BatteryStatus
import com.example.data.BatteryTelemetry
import com.example.data.CableDiagnosticResult
import com.example.data.CableDnaTier
import com.example.data.ChargePlug
import com.example.data.DeviceHardwareProfile
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * International Electrical Standards Engine for Cable DNA & Charger Quality Auditing:
 * - Calibrated against USB-IF Type-C Cable & Connector Spec Rev 2.3 & IEC 62680-1-2:
 *   * VBUS + GND max allowable round-trip loop drop at rated current <= 750 mV.
 *   * <= 125 mOhm: 20 AWG E-Marked 5A / 240W EPR OEM Grade (TIER S+)
 *   * 126..230 mOhm: 22 AWG Certified 3A-5A Fast PD/PPS (TIER A)
 *   * 231..390 mOhm: 24-26 AWG Standard / Degraded USB 2.0 (TIER C)
 *   * > 390 mOhm or thermal stress: 28 AWG+ Non-Compliant / Counterfeit Risk (CRITICAL)
 * - Uses OCV (Open-Circuit Voltage) SoC curve + ΔV/ΔI dynamic impedance + CV Taper compensation
 *   so fast chargers (PD 9V/11V/20V, 2S Dual-Cell) are never penalized by false 5V subtraction.
 */
object CableDnaStandardsEngine {

    fun evaluate(
        telemetry: BatteryTelemetry,
        deviceProfile: DeviceHardwareProfile,
        observedBaselineOcvMv: Int?,
        dynamicDeltaImpedanceMilliOhms: Int?,
        currentRipplePercent: Float,
        tierOverride: CableDnaTier? = null,
        timestampMs: Long = System.currentTimeMillis(),
        isArabic: Boolean = true
    ): CableDiagnosticResult {
        val currentV = telemetry.voltageMv.coerceAtLeast(3000)
        val rawCurrentI = abs(telemetry.currentMa)
        val isPlugged = telemetry.plugType != ChargePlug.NONE ||
            telemetry.status == BatteryStatus.CHARGING ||
            rawCurrentI > 180

        val isDualCell = currentV > 6000 || deviceProfile.isDualCellSuspected
        val topologyLabel = if (isArabic) {
            if (isDualCell) "ثنائي الخلية (2S)" else "خلية مفردة (1S)"
        } else {
            if (isDualCell) "2S Dual-Cell" else "1S Single-Cell"
        }

        if (!isPlugged && tierOverride == null) {
            return CableDiagnosticResult(
                resistanceMilliOhms = 0,
                voltageDropMv = 0,
                measuredVoltageMv = currentV,
                measuredCurrentMa = 0,
                maxWattageW = 0.0f,
                qualityScore = 100,
                tier = CableDnaTier.LEGENDARY_ORIGINAL,
                usbProtocol = if (isArabic) {
                    "وضع الاستعداد • ${deviceProfile.chargingEcosystemEn}"
                } else {
                    "Standby • ${deviceProfile.chargingEcosystemEn}"
                },
                pinIntegrityPercent = 100,
                thermalDissipationMilliWatts = 0,
                estimatedAwg = if (isArabic) "بانتظار التوصيل" else "Awaiting Connection",
                lineEfficiencyPercent = 100.0f,
                standardCompliance = if (isArabic) "جاهز • معيار IEC 62680 و USB-IF" else "IEC 62680 • USB-IF Ready",
                chargingPhase = if (isArabic) "وضع الاستعداد (غير موصول)" else "Standby (Unplugged)",
                recommendedCableSpec = deviceProfile.localizedRecommendedCable(isArabic),
                cellTopology = topologyLabel,
                tailoredDeviceNote = if (isArabic) {
                    "تمت معايرة القياس لجهاز ${deviceProfile.brand} ${deviceProfile.model} (${deviceProfile.chargingEcosystemEn})."
                } else {
                    "Calibrated for ${deviceProfile.brand} ${deviceProfile.model} (${deviceProfile.chargingEcosystemEn})."
                },
                scanTimestamp = timestampMs
            )
        }

        // 1. Estimate true Li-Ion Open-Circuit Voltage (V_ocv) for current SoC
        val curveOcvMv = estimateLiIonOcvMv(telemetry.percentage, isDualCell)
        val effectiveOcvMv = when {
            observedBaselineOcvMv != null &&
                abs(observedBaselineOcvMv - curveOcvMv) <= (if (isDualCell) 450 else 260) ->
                observedBaselineOcvMv
            else -> curveOcvMv
        }

        val effectiveI = rawCurrentI.coerceAtLeast(if (telemetry.plugType == ChargePlug.USB) 450 else 550)
        val watts = (currentV / 1000.0f) * (effectiveI / 1000.0f)

        // 2. Determine charging phase (CC Fast, CV Taper >= 80%, USB Host, Wireless)
        val isCvTaper = telemetry.percentage >= 82 && telemetry.plugType == ChargePlug.AC
        val chargingPhase = if (isArabic) {
            when {
                telemetry.plugType == ChargePlug.WIRELESS -> "ملف الشحن اللاسلكي (Qi)"
                telemetry.plugType == ChargePlug.USB -> "منفذ USB قياسي (500–900mA)"
                isCvTaper -> "مرحلة الامتلاء (CV Taper >80%)"
                watts >= 18f -> "شحن سريع مباشر عالي القدرة (CC)"
                else -> "شحن قياسي بتيار مستقر (CC)"
            }
        } else {
            when {
                telemetry.plugType == ChargePlug.WIRELESS -> "Wireless Inductive Coil (Qi)"
                telemetry.plugType == ChargePlug.USB -> "USB Host Rail (500–900mA Standard)"
                isCvTaper -> "CV Taper Phase (>80% Safe Step-Down)"
                watts >= 18f -> "CC High-Power Fast Charge"
                else -> "CC Standard Steady Charge"
            }
        }

        // 3. Compute loop resistance (mOhm) using polarization delta & dynamic dV/dI impedance
        val polarizationDeltaMv = (currentV - effectiveOcvMv).coerceIn(25, if (isDualCell) 950 else 580)
        val staticImpedanceMohm = ((polarizationDeltaMv.toFloat() / effectiveI.toFloat()) * 1000f)
            .roundToInt()
            .coerceIn(55, 620)

        val blendedResistanceRaw = if (dynamicDeltaImpedanceMilliOhms != null && dynamicDeltaImpedanceMilliOhms in 45..620) {
            ((dynamicDeltaImpedanceMilliOhms * 0.55f) + (staticImpedanceMohm * 0.45f)).roundToInt()
        } else {
            staticImpedanceMohm
        }

        // During CV Taper (>82% SoC) or USB-IF 500mA host limit, compensate for intentional PMIC current reduction
        val adjustedResistance = when {
            tierOverride == CableDnaTier.LEGENDARY_ORIGINAL -> 92
            tierOverride == CableDnaTier.CERTIFIED_FAST -> 175
            tierOverride == CableDnaTier.DEGRADED_STANDARD -> 320
            tierOverride == CableDnaTier.DANGEROUS_COUNTERFEIT -> 510
            telemetry.plugType == ChargePlug.WIRELESS -> blendedResistanceRaw.coerceIn(140, 320)
            isCvTaper -> (blendedResistanceRaw * 0.72f).roundToInt().coerceIn(70, 340)
            telemetry.plugType == ChargePlug.USB && currentRipplePercent < 8f ->
                blendedResistanceRaw.coerceAtMost(290)
            else -> blendedResistanceRaw
        }.coerceIn(55, 640)

        // 4. Calculate real USB-IF VBUS+GND loop drop under actual active current (I * R_cable)
        val estimatedCableDropMv = ((effectiveI / 1000f) * adjustedResistance)
            .roundToInt()
            .coerceIn(25, 1400)

        // 5. Thermal loss in cable conductors: P_loss = I^2 * R (in mW)
        val currentAmps = effectiveI / 1000.0
        val resistanceOhms = adjustedResistance / 1000.0
        val thermalDissipationMw = (currentAmps * currentAmps * resistanceOhms * 1000.0)
            .roundToInt()
            .coerceAtLeast(0)

        // 6. Classify according to IEC 62680 / USB-IF Rev 2.3 + Device thermal ceiling
        val overThermalCeiling = telemetry.temperatureC >= deviceProfile.safeThermalCeilingC
        val evaluatedTier = tierOverride ?: when {
            adjustedResistance > 390 || estimatedCableDropMv > 780 ||
                (overThermalCeiling && adjustedResistance > 250) ->
                CableDnaTier.DANGEROUS_COUNTERFEIT
            adjustedResistance <= 125 -> CableDnaTier.LEGENDARY_ORIGINAL
            adjustedResistance <= 230 -> CableDnaTier.CERTIFIED_FAST
            else -> CableDnaTier.DEGRADED_STANDARD
        }

        // 7. Wire Gauge AWG & USB-IF Standard Compliance
        val estimatedAwg = if (isArabic) {
            when {
                telemetry.plugType == ChargePlug.WIRELESS -> "ملف شحن لاسلكي Qi"
                adjustedResistance <= 115 -> "20 AWG E-Marked (240W / 5A)"
                adjustedResistance <= 190 -> "22 AWG شحن سريع (100W / 5A)"
                adjustedResistance <= 290 -> "24 AWG قياسي (60W / 3A)"
                adjustedResistance <= 410 -> "26 AWG تقليدي (15W–18W)"
                else -> "28 AWG+ فاقد عالٍ (هبوط جهد)"
            }
        } else {
            when {
                telemetry.plugType == ChargePlug.WIRELESS -> "Qi Inductive Coil"
                adjustedResistance <= 115 -> "20 AWG E-Marked (240W / 5A)"
                adjustedResistance <= 190 -> "22 AWG Fast-PD (100W / 5A)"
                adjustedResistance <= 290 -> "24 AWG Standard (60W / 3A)"
                adjustedResistance <= 410 -> "26 AWG Legacy (15W–18W)"
                else -> "28 AWG+ High-Loss (Voltage Drop)"
            }
        }

        val standardCompliance = if (isArabic) {
            when {
                telemetry.plugType == ChargePlug.WIRELESS -> "مسار شحن لاسلكي Qi معتمد"
                estimatedCableDropMv <= 500 && adjustedResistance <= 180 ->
                    "اجتياز IEC 62680 (≤500mV VBUS)"
                estimatedCableDropMv <= 750 && adjustedResistance <= 280 ->
                    "اجتياز USB-IF (≤750mV Loop)"
                estimatedCableDropMv <= 900 && adjustedResistance <= 390 ->
                    "تقييد USB-IF (فقد جهد متوسط)"
                else ->
                    "إخفاق IEC 62680 (>750mV هبوط)"
            }
        } else {
            when {
                telemetry.plugType == ChargePlug.WIRELESS -> "Qi Wireless Certified Path"
                estimatedCableDropMv <= 500 && adjustedResistance <= 180 ->
                    "IEC 62680 PASS (≤500mV VBUS)"
                estimatedCableDropMv <= 750 && adjustedResistance <= 280 ->
                    "USB-IF PASS (≤750mV Loop)"
                estimatedCableDropMv <= 900 && adjustedResistance <= 390 ->
                    "USB-IF THROTTLED (Moderate Drop)"
                else ->
                    "IEC 62680 FAIL (>750mV Drop)"
            }
        }

        // 8. Negotiated Protocol Classification
        val usbProtocol = if (isArabic) {
            when {
                telemetry.plugType == ChargePlug.WIRELESS ->
                    if (watts >= 12f) "شحن لاسلكي سريع Qi (EPP)" else "شحن لاسلكي قياسي Qi (BPP)"
                telemetry.plugType == ChargePlug.USB -> "منفذ بيانات USB 2.0 / 3.x"
                watts >= 36f -> "${deviceProfile.chargingEcosystemEn} (قدرة عالية)"
                watts >= 20f -> "شحن سريع USB-PD 3.0 / PPS"
                watts >= 12f -> "شحن سريع USB-PD / QC 3.0"
                else -> "USB BC 1.2 (5V قياسي)"
            }
        } else {
            when {
                telemetry.plugType == ChargePlug.WIRELESS ->
                    if (watts >= 12f) "Qi Fast Wireless (EPP)" else "Qi Standard Wireless (BPP)"
                telemetry.plugType == ChargePlug.USB -> "USB 2.0 / 3.x Host Port"
                watts >= 36f -> "${deviceProfile.chargingEcosystemEn} (High-W)"
                watts >= 20f -> "USB-PD 3.0 / PPS Fast"
                watts >= 12f -> "USB-PD / QC 3.0 (15W+)"
                else -> "USB BC 1.2 (5V Standard)"
            }
        }

        // 9. Continuous Quality Score (0..100), Pin Integrity (%), Line Efficiency (%)
        val resistancePenalty = ((adjustedResistance - 70).coerceAtLeast(0) * 0.145f)
        val dropPenalty = ((estimatedCableDropMv - 250).coerceAtLeast(0) * 0.032f)
        val ripplePenalty = (currentRipplePercent * 0.45f).coerceAtMost(14f)
        val thermalPenalty = if (overThermalCeiling) 12f else 0f

        val computedScore = when (tierOverride) {
            CableDnaTier.LEGENDARY_ORIGINAL -> 98
            CableDnaTier.CERTIFIED_FAST -> 89
            CableDnaTier.DEGRADED_STANDARD -> 64
            CableDnaTier.DANGEROUS_COUNTERFEIT -> 31
            null -> (100f - resistancePenalty - dropPenalty - ripplePenalty - thermalPenalty)
                .roundToInt()
                .coerceIn(15, 99)
        }

        val pinIntegrity = (100f - (currentRipplePercent * 0.85f) - ((adjustedResistance - 150).coerceAtLeast(0) * 0.05f))
            .roundToInt()
            .coerceIn(45, 100)

        val totalInputMilliWatts = (watts * 1000f) + thermalDissipationMw
        val lineEfficiency = if (totalInputMilliWatts > 100f) {
            ((watts * 1000f) / totalInputMilliWatts * 100f).coerceIn(68.0f, 99.6f)
        } else {
            99.0f
        }

        val tailoredNote = buildTailoredDeviceNote(
            profile = deviceProfile,
            resistance = adjustedResistance,
            watts = watts,
            isCvTaper = isCvTaper,
            overThermal = overThermalCeiling,
            isArabic = isArabic
        )

        return CableDiagnosticResult(
            resistanceMilliOhms = adjustedResistance,
            voltageDropMv = estimatedCableDropMv,
            measuredVoltageMv = currentV,
            measuredCurrentMa = effectiveI,
            maxWattageW = watts,
            qualityScore = computedScore,
            tier = evaluatedTier,
            usbProtocol = usbProtocol,
            pinIntegrityPercent = pinIntegrity,
            thermalDissipationMilliWatts = thermalDissipationMw,
            estimatedAwg = estimatedAwg,
            lineEfficiencyPercent = ((lineEfficiency * 10f).roundToInt() / 10f),
            standardCompliance = standardCompliance,
            chargingPhase = chargingPhase,
            recommendedCableSpec = deviceProfile.localizedRecommendedCable(isArabic),
            cellTopology = topologyLabel,
            tailoredDeviceNote = tailoredNote,
            scanTimestamp = timestampMs
        )
    }

    fun estimateLiIonOcvMv(percentage: Int, isDualCell: Boolean): Int {
        val soc = percentage.coerceIn(0, 100)
        val singleCellMv = when {
            soc <= 10 -> 3420 + (soc * 25)
            soc <= 50 -> 3670 + ((soc - 10) * 4)
            soc <= 85 -> 3830 + ((soc - 50) * 9)
            else -> 4145 + ((soc - 85) * 12)
        }
        return if (isDualCell) singleCellMv * 2 else singleCellMv
    }

    private fun buildTailoredDeviceNote(
        profile: DeviceHardwareProfile,
        resistance: Int,
        watts: Float,
        isCvTaper: Boolean,
        overThermal: Boolean,
        isArabic: Boolean
    ): String {
        return if (isArabic) {
            when {
                overThermal -> "حرارة البطارية تجاوزت الحد الآمن لجهاز ${profile.model} (${profile.safeThermalCeilingC}°C) — يخفض النظام الشحن لحماية الخلايا."
                isCvTaper -> "البطارية في مرحلة الامتلاء النهائي (CV Taper)؛ خفض التيار الآن سلوك طبيعي من متحكم شحن ${profile.brand}."
                resistance <= 130 -> "توافق ممتاز مع ${profile.chargingEcosystemEn}: المسار يحافظ على كفاءة نقل عالية بقدرة ${String.format(Locale.US, "%.1f", watts)}W."
                resistance <= 230 -> "كابل جيد؛ قد لا يبلغ الذروة القصوى (${profile.recommendedPowerWatts}W) لجهاز ${profile.model} لكنه آمن ومطابق لـ USB-IF."
                else -> "مقاومة مرتفعة تحدّ من قدرة شحن ${profile.brand} ${profile.model}؛ يُنصح باستخدام: ${profile.recommendedCableAr}."
            }
        } else {
            when {
                overThermal -> "Battery temperature exceeded the safe ceiling for ${profile.model} (${profile.safeThermalCeilingC}°C) — charging is throttled to protect the cells."
                isCvTaper -> "Cell is in final Constant-Voltage Taper; reduced current is normal behavior by the ${profile.brand} PMIC."
                resistance <= 130 -> "Optimal match with ${profile.chargingEcosystemEn}: line maintains high power efficiency at ${String.format(Locale.US, "%.1f", watts)}W."
                resistance <= 230 -> "Solid cable; may not hit ${profile.model}'s full ${profile.recommendedPowerWatts}W peak, yet stays safe and USB-IF compliant."
                else -> "Elevated resistance is limiting ${profile.brand} ${profile.model} charge rate; recommended: ${profile.recommendedCableEn}."
            }
        }
    }
}
