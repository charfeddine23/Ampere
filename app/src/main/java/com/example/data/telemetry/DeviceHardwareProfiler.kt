package com.example.data.telemetry

import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.example.data.DeviceFormFactor
import com.example.data.DeviceHardwareProfile
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Inspects real Android hardware, SoC, memory, form factor, and OEM charging ecosystem
 * so Cable DNA diagnostics and UI telemetry adapt cleanly to the user's actual device.
 */
object DeviceHardwareProfiler {

    fun collectProfile(context: Context, observedVoltageMv: Int = 4000): DeviceHardwareProfile {
        val rawManufacturer = (Build.MANUFACTURER ?: "Android").trim()
        val rawBrand = (Build.BRAND ?: rawManufacturer).trim()
        val rawModel = (Build.MODEL ?: "Device").trim()
        val codeName = (Build.DEVICE ?: "unknown").trim()

        val displayManufacturer = formatTitleCase(rawManufacturer)
        val displayBrand = formatTitleCase(rawBrand)

        val chipset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val soc = Build.SOC_MODEL.orEmpty().trim()
            if (soc.isNotEmpty() && !soc.equals("unknown", ignoreCase = true)) {
                soc
            } else {
                (Build.HARDWARE ?: Build.BOARD ?: "ARM64 SoC").trim()
            }
        } else {
            (Build.HARDWARE ?: Build.BOARD ?: "ARM64 SoC").trim()
        }

        val androidVersion = "Android ${Build.VERSION.RELEASE ?: "OS"} (API ${Build.VERSION.SDK_INT})"
        val totalRamGb = readTotalRamGb(context)
        val cpuCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val primaryAbi = Build.SUPPORTED_ABIS?.firstOrNull() ?: "arm64-v8a"
        val isDualCell = observedVoltageMv > 6000 || isKnownDualCellModel(rawManufacturer, rawModel)
        val formFactor = detectFormFactor(context, rawModel)

        val oemKey = "${rawManufacturer.lowercase(Locale.ROOT)} ${rawBrand.lowercase(Locale.ROOT)} ${rawModel.lowercase(Locale.ROOT)}"
        val ecosystem = resolveOemChargingProfile(oemKey, formFactor, isDualCell)

        val designCapacity = estimateDesignCapacityMah(context, formFactor, isDualCell)

        return DeviceHardwareProfile(
            manufacturer = displayManufacturer,
            brand = displayBrand,
            model = cleanModelName(displayManufacturer, rawModel),
            codeName = codeName,
            chipset = chipset,
            androidVersion = androidVersion,
            sdkInt = Build.VERSION.SDK_INT,
            totalRamGb = totalRamGb,
            cpuCores = cpuCores,
            primaryAbi = primaryAbi,
            formFactor = formFactor,
            chargingEcosystemAr = ecosystem.ecosystemAr,
            chargingEcosystemEn = ecosystem.ecosystemEn,
            recommendedCableAr = ecosystem.recommendedCableAr,
            recommendedPowerWatts = ecosystem.targetWatts,
            safeThermalCeilingC = ecosystem.safeThermalCeilingC,
            nominalDesignCapacityMah = designCapacity,
            isDualCellSuspected = isDualCell
        )
    }

    private data class OemChargingSpec(
        val ecosystemAr: String,
        val ecosystemEn: String,
        val recommendedCableAr: String,
        val targetWatts: Int,
        val safeThermalCeilingC: Float
    )

    private fun resolveOemChargingProfile(
        oemKey: String,
        formFactor: DeviceFormFactor,
        isDualCell: Boolean
    ): OemChargingSpec {
        return when {
            "samsung" in oemKey -> {
                val watts = if (formFactor == DeviceFormFactor.TABLET) 45 else 45
                OemChargingSpec(
                    ecosystemAr = "Samsung Super Fast Charging 2.0 (USB-PD 3.0 + PPS)",
                    ecosystemEn = "Samsung SFC 2.0 • USB-PD PPS",
                    recommendedCableAr = "كابل Type-C 5A E-Marked أصلي (مقاومة < 130 mΩ) لدعم 45W PPS",
                    targetWatts = watts,
                    safeThermalCeilingC = 41.0f
                )
            }
            "xiaomi" in oemKey || "redmi" in oemKey || "poco" in oemKey -> OemChargingSpec(
                ecosystemAr = if (isDualCell) {
                    "Xiaomi HyperCharge ثنائي الخلية (PD + Mi Turbo)"
                } else {
                    "Xiaomi Mi Turbo Charge (USB-PD + PPS)"
                },
                ecosystemEn = "Xiaomi HyperCharge / Mi Turbo",
                recommendedCableAr = "كابل Xiaomi 6A/5A منخفض المقاومة (< 120 mΩ • 20AWG)",
                targetWatts = if (isDualCell) 67 else 33,
                safeThermalCeilingC = 42.5f
            )
            "oneplus" in oemKey || "oppo" in oemKey || "realme" in oemKey -> OemChargingSpec(
                ecosystemAr = if (isDualCell) {
                    "SUPERVOOC Direct Charge ثنائي الخلية (2S)"
                } else {
                    "VOOC / SUPERVOOC + USB-PD PPS"
                },
                ecosystemEn = "SUPERVOOC / USB-PD",
                recommendedCableAr = "كابل SUPERVOOC 8A/10A سميك (مقاومة < 110 mΩ)",
                targetWatts = if (isDualCell) 80 else 45,
                safeThermalCeilingC = 42.5f
            )
            "google" in oemKey || "pixel" in oemKey -> OemChargingSpec(
                ecosystemAr = "Google USB-IF PD 3.0 + PPS القياسي الدقيق",
                ecosystemEn = "USB-IF PD 3.0 + PPS",
                recommendedCableAr = "كابل USB-IF معتمد 3A/5A (هبوط جهد ≤ 750 mV وفق IEC 62680)",
                targetWatts = if (formFactor == DeviceFormFactor.TABLET) 30 else 27,
                safeThermalCeilingC = 40.5f
            )
            "vivo" in oemKey || "iqoo" in oemKey -> OemChargingSpec(
                ecosystemAr = "Vivo FlashCharge + USB-PD PPS",
                ecosystemEn = "Vivo FlashCharge",
                recommendedCableAr = "كابل FlashCharge 6A (20AWG • مقاومة < 130 mΩ)",
                targetWatts = if (isDualCell) 80 else 44,
                safeThermalCeilingC = 42.0f
            )
            "huawei" in oemKey || "honor" in oemKey -> OemChargingSpec(
                ecosystemAr = "SuperCharge (SCP / FCP) + PD",
                ecosystemEn = "SuperCharge SCP / FCP",
                recommendedCableAr = "كابل SuperCharge 5A/6A أصلي (مقاومة < 130 mΩ)",
                targetWatts = 66,
                safeThermalCeilingC = 41.5f
            )
            "motorola" in oemKey -> OemChargingSpec(
                ecosystemAr = "Motorola TurboPower (USB-PD 3.0 / PPS)",
                ecosystemEn = "TurboPower USB-PD PPS",
                recommendedCableAr = "كابل USB-C 5A 20AWG متوافق مع TurboPower",
                targetWatts = 45,
                safeThermalCeilingC = 41.0f
            )
            "nothing" in oemKey || "sony" in oemKey || "asus" in oemKey -> OemChargingSpec(
                ecosystemAr = "USB-IF PD 3.0 + PPS عالية الكفاءة",
                ecosystemEn = "USB-IF PD 3.0 PPS",
                recommendedCableAr = "كابل USB-C 3A/5A معتمد من USB-IF (< 160 mΩ)",
                targetWatts = 30,
                safeThermalCeilingC = 40.5f
            )
            formFactor == DeviceFormFactor.TABLET -> OemChargingSpec(
                ecosystemAr = "بروتوكول شحن الأجهزة اللوحية عالي القدرة (USB-PD 3.0)",
                ecosystemEn = "Tablet High-Power USB-PD",
                recommendedCableAr = "كابل USB-PD 5A E-Marker 100W (< 130 mΩ)",
                targetWatts = 45,
                safeThermalCeilingC = 40.0f
            )
            else -> OemChargingSpec(
                ecosystemAr = "بروتوكول الشحن القياسي USB-IF PD 3.0 / BC 1.2",
                ecosystemEn = "USB-IF PD 3.0 / BC 1.2",
                recommendedCableAr = "كابل Type-C 3A/5A معتمد (مقاومة < 180 mΩ)",
                targetWatts = 25,
                safeThermalCeilingC = 41.0f
            )
        }
    }

    private fun detectFormFactor(context: Context, rawModel: String): DeviceFormFactor {
        val modelLower = rawModel.lowercase(Locale.ROOT)
        if ("fold" in modelLower || "flip" in modelLower || "razr" in modelLower || "find n" in modelLower) {
            return DeviceFormFactor.FOLDABLE
        }
        val config: Configuration = context.resources.configuration
        val swDp = config.smallestScreenWidthDp
        return if (swDp >= 600 || "tab" in modelLower || "pad" in modelLower) {
            DeviceFormFactor.TABLET
        } else {
            DeviceFormFactor.PHONE
        }
    }

    private fun readTotalRamGb(context: Context): Float {
        return runCatching {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: return 8.0f
            val info = ActivityManager.MemoryInfo()
            am.getMemoryInfo(info)
            val gb = info.totalMem / (1024.0 * 1024.0 * 1024.0)
            ((gb * 10.0).roundToInt() / 10.0f).coerceAtLeast(1.0f)
        }.getOrDefault(8.0f)
    }

    private fun estimateDesignCapacityMah(
        context: Context,
        formFactor: DeviceFormFactor,
        isDualCell: Boolean
    ): Int {
        // Attempt private PowerProfile reflection gracefully before falling back to hardware class heuristics
        val reflected = runCatching {
            val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
            val ctor = powerProfileClass.getConstructor(Context::class.java)
            val instance = ctor.newInstance(context.applicationContext)
            val method = powerProfileClass.getMethod("getBatteryCapacity")
            val cap = (method.invoke(instance) as? Double)?.roundToInt() ?: 0
            if (cap in 1800..15000) cap else 0
        }.getOrDefault(0)

        if (reflected > 0) return reflected

        return when {
            formFactor == DeviceFormFactor.TABLET -> 8000
            isDualCell -> 4800
            else -> 5000
        }
    }

    private fun isKnownDualCellModel(manufacturer: String, model: String): Boolean {
        val combined = "$manufacturer $model".lowercase(Locale.ROOT)
        return "find x" in combined || "gt neo" in combined || "gt " in combined ||
            "120w" in combined || "150w" in combined || "oneplus 1" in combined ||
            "fold" in combined
    }

    private fun cleanModelName(manufacturer: String, rawModel: String): String {
        return if (rawModel.startsWith(manufacturer, ignoreCase = true)) {
            rawModel
        } else {
            rawModel
        }
    }

    private fun formatTitleCase(value: String): String {
        return value.lowercase(Locale.ROOT).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }
    }
}
