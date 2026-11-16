package com.example.data

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.TactileAmber
import com.example.ui.theme.TactileCoralRed
import com.example.ui.theme.TactileCyan
import com.example.ui.theme.TactileGreen
import com.example.ui.theme.TactileRoyalBlue
import com.example.ui.theme.TactileViolet

enum class AppLanguage(
    val code: String,
    val isRtl: Boolean,
    val nativeName: String
) {
    ARABIC("ar", true, "العربية"),
    ENGLISH("en", false, "English");

    val isArabic: Boolean
        get() = this == ARABIC

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.find {
                it.code.equals(code, ignoreCase = true) || it.name.equals(code, ignoreCase = true)
            } ?: ARABIC
        }
    }
}

enum class BatteryStatus(val label: String, val labelAr: String) {
    CHARGING("Charging", "قيد الشحن"),
    DISCHARGING("Discharging", "تفريغ الطاقة"),
    FULL("Fully Charged", "مشحون بالكامل"),
    NOT_CHARGING("Idle (Not Charging)", "خامل (لا يشحن)"),
    UNKNOWN("Analyzing", "قيد التحليل");

    fun localizedLabel(isArabic: Boolean): String = if (isArabic) labelAr else label
}

enum class BatteryHealth(val label: String, val labelAr: String) {
    GOOD("Good / Optimal", "جيدة / مثالية"),
    OVERHEAT("Overheat Warning", "تحذير ارتفاع الحرارة"),
    DEAD("Exhausted", "مستهلكة"),
    OVER_VOLTAGE("Over Voltage", "تجاوز الجهد الآمن"),
    COLD("Cold Sub-zero", "برودة منخفضة"),
    UNKNOWN("Nominal", "طبيعية");

    fun localizedLabel(isArabic: Boolean): String = if (isArabic) labelAr else label
}

enum class ChargePlug(val label: String, val labelAr: String) {
    AC("Ultra AC Wall Charger", "شاحن جداري AC سريع"),
    USB("USB-C Fast Data Port", "منفذ بيانات USB"),
    WIRELESS("Wireless Qi Induction Pad", "منصة شحن لاسلكي Qi"),
    DOCK("Smart Docking Station", "قاعدة شحن ذكية"),
    NONE("Battery Power (Unplugged)", "طاقة البطارية (غير موصول)");

    fun localizedLabel(isArabic: Boolean): String = if (isArabic) labelAr else label
}

enum class TemperatureUnit(val symbol: String) {
    CELSIUS("°C"),
    FAHRENHEIT("°F")
}

enum class MetricType(val title: String, val titleAr: String, val unit: String) {
    VOLTAGE("Voltage", "الجهد", "mV"),
    CURRENT("Current", "التيار", "mA"),
    WATTAGE("Wattage", "القدرة", "W"),
    TEMPERATURE("Temperature", "الحرارة", "°C");

    fun localizedTitle(isArabic: Boolean): String = if (isArabic) titleAr else title
}

/**
 * 4 Scientific Operating States mapped to IEEE 1625 / JEITA / BU-808 electrochemistry
 */
enum class LivingBatteryState(
    val title: String,
    val titleAr: String,
    val neonPrimary: Color,
    val neonSecondary: Color,
    val auraDescription: String,
    val auraDescriptionAr: String
) {
    OVERDRIVE_HEATING(
        title = "Thermal Stress",
        titleAr = "إجهاد حراري",
        neonPrimary = TactileCoralRed, // #FF3D00
        neonSecondary = Color(0xFFFF6E40),
        auraDescription = "Cell temperature or voltage exceeded IEEE 1625 / JEITA safe ceiling.",
        auraDescriptionAr = "تجاوزت حرارة الخلية أو جهدها السقف الآمن لمعايير IEEE 1625 / JEITA."
    ),
    COOLING(
        title = "Thermal Recovery",
        titleAr = "استعادة حرارية",
        neonPrimary = TactileCyan, // #00B0FF
        neonSecondary = Color(0xFF40C4FF),
        auraDescription = "Active heat dissipation or low-temperature protection window.",
        auraDescriptionAr = "تبديد حراري نشط أو نافذة حماية من البرودة المنخفضة."
    ),
    ENERGY_STORING(
        title = "Active Charging",
        titleAr = "شحن نشط",
        neonPrimary = TactileGreen, // #00C853
        neonSecondary = Color(0xFF69F0AE),
        auraDescription = "Controlled CC/CV lithium intercalation within JEITA safe temperature.",
        auraDescriptionAr = "شحن منضبط بنمط CC/CV ضمن النطاق الحراري الآمن JEITA."
    ),
    DEEP_SLEEP(
        title = "Quiescent Standby",
        titleAr = "سكون كهربائي",
        neonPrimary = TactileViolet, // #7C4DFF
        neonSecondary = Color(0xFFB388FF),
        auraDescription = "Low quiescent C-rate draw preserving cathode structural stability.",
        auraDescriptionAr = "سحب خامل منخفض يحافظ على استقرار بنية الكاثود الكيميائية."
    );

    fun localizedTitle(isArabic: Boolean): String = if (isArabic) titleAr else title
    fun localizedDescription(isArabic: Boolean): String = if (isArabic) auraDescriptionAr else auraDescription
}

data class TelemetryPoint(
    val timestamp: Long,
    val value: Float
)

data class BatteryTelemetry(
    val percentage: Int = 0,
    val status: BatteryStatus = BatteryStatus.UNKNOWN,
    val plugType: ChargePlug = ChargePlug.NONE,
    val health: BatteryHealth = BatteryHealth.GOOD,
    val voltageMv: Int = 0,
    val currentMa: Int = 0,
    val wattageW: Float = 0.0f,
    val temperatureC: Float = 25.0f,
    val technology: String = "Li-Ion / Li-Poly",
    val capacityMah: Int = 5000,
    val chargeCounterUah: Long = 0L,
    val cycleCount: Int = 0,
    val estimatedMinutesRemaining: Int = 0,
    val isFastCharging: Boolean = false,
    val dischargeRatePerHour: Float = 0.0f,
    val livingState: LivingBatteryState = LivingBatteryState.DEEP_SLEEP,
    val lastUpdated: Long = System.currentTimeMillis()
)

enum class AppTier(
    val label: String,
    val labelAr: String,
    val color: Color,
    val description: String,
    val descriptionAr: String
) {
    FREE("FREE", "مجاني", Color(0xFF64748B), "Standard Telemetry & Real-Time Metrics", "قياسات أساسية دقيقة ومراقبة حية"),
    VIP("VIP", "VIP", Color(0xFFFF9100), "VIP High-Flow Telemetry & Hologram Unlocked", "تدفق قياسات عالي الدقة وفتح التأثيرات الزجاجية"),
    PREMIUM("PREMIUM", "بريميوم", Color(0xFFFFD700), "Ultra-Precision Telemetry & Full Customization Active", "قياسات فائقة الدقة وتخصيص شامل مفعل بالكامل");

    fun localizedLabel(isArabic: Boolean): String = if (isArabic) labelAr else label
    fun localizedDescription(isArabic: Boolean): String = if (isArabic) descriptionAr else description

    fun displayColor(isDarkMode: Boolean): Color {
        if (isDarkMode) return color
        return when (this) {
            FREE -> Color(0xFF4C5E75)
            VIP -> Color(0xFFD46A00)
            PREMIUM -> Color(0xFFB77900)
        }
    }
}

enum class GraphicsMode(val key: String, val titleAr: String, val titleEn: String) {
    STANDARD("standard", "الوضع القياسي", "Standard Mode"),
    SIMPLE("simple", "الوضع البسيط", "Simple Mode"),
    PRO("pro", "وضع المحترفين", "Pro Mode");

    fun localizedTitle(isArabic: Boolean): String = if (isArabic) titleAr else titleEn

    companion object {
        fun fromKey(key: String?): GraphicsMode {
            return entries.find { it.key.equals(key, ignoreCase = true) || it.name.equals(key, ignoreCase = true) } ?: STANDARD
        }
    }
}

data class GlassThemeConfig(
    val isDarkMode: Boolean = false, // Defaults to Soft Stone Light Neumorphism
    val glowIntensity: Float = 0.75f,
    val accentColorIndex: Int = 0, // 0 = Royal Blue (#2962FF), 1 = Vibrant Green (#00C853), 2 = Coral Red (#FF3D00), 3 = Violet (#7C4DFF)
    val activeSkinId: String = "obsidian_cyber",
    val customAccentHex: Long? = null,
    val appTier: AppTier = AppTier.FREE,
    val isPremiumUnlocked: Boolean = false,
    val glassBlurAmount: Float = 0.85f,
    val ambientLiquidWaves: Boolean = true,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val samplingIntervalMs: Long = 1000L,
    val soundEffectsEnabled: Boolean = true,
    val backgroundServiceEnabled: Boolean = true,
    val alertOn80Percent: Boolean = true,
    val alertOnHighTemp: Boolean = true,
    val highTempThresholdC: Float = 42.0f,
    val hapticFeedbackEnabled: Boolean = true,
    val target120FpsMode: Boolean = true,
    val showFpsOverlay: Boolean = true,
    val showLiquidDroplet: Boolean = true,
    val autoUpdateEnabled: Boolean = true,
    val currentMultiplier: Float = 1.0f,
    val currentZeroOffsetMa: Float = 0.0f,
    val activeThemeId: String? = null,
    val graphicsMode: GraphicsMode = GraphicsMode.STANDARD,
    val dropletDragNavigationEnabled: Boolean = false,
    val appLanguage: AppLanguage = AppLanguage.ARABIC
) {
    val isUsingThemeEngine: Boolean
        get() = !activeThemeId.isNullOrBlank()

    val isSimpleMode: Boolean
        get() = graphicsMode == GraphicsMode.SIMPLE

    val isProMode: Boolean
        get() = graphicsMode == GraphicsMode.PRO

    // Backward compatibility alias
    val target90FpsMode: Boolean
        get() = target120FpsMode
    val accentColor: Color
        get() = if (customAccentHex != null) {
            Color(customAccentHex)
        } else {
            when (accentColorIndex) {
                0 -> TactileRoyalBlue // #2962FF
                1 -> TactileGreen     // #00C853
                2 -> TactileCoralRed  // #FF3D00
                3 -> TactileViolet    // #7C4DFF
                else -> TactileRoyalBlue
            }
        }

    val accentName: String
        get() = localizedAccentName(isArabic = false)

    fun localizedAccentName(isArabic: Boolean): String = if (isArabic) {
        when (accentColorIndex) {
            0 -> "الأزرق الملكي (#2962FF)"
            1 -> "الأخضر الحيوي (#00C853)"
            2 -> "الأحمر المرجاني (#FF3D00)"
            3 -> "البنفسجي الفلكي (#7C4DFF)"
            else -> "لون مظهر مخصص"
        }
    } else {
        when (accentColorIndex) {
            0 -> "Royal Blue (#2962FF)"
            1 -> "Tactile Green (#00C853)"
            2 -> "Coral Red (#FF3D00)"
            3 -> "Cosmic Violet (#7C4DFF)"
            else -> "Custom Cyber Skin"
        }
    }
}

data class SessionLog(
    val id: String,
    val title: String,
    val timestampFormatted: String,
    val description: String,
    val statTag: String,
    val type: String
)

data class UserAccount(
    val ownerName: String = "Charfeddine",
    val email: String = "charfeddineka02@gmail.com",
    val avatarInitials: String = "CK",
    val accountTier: String = "Pulse Pioneer",
    val isLocalHardwareActive: Boolean = true,
    val isCloudSyncActive: Boolean = false
) {
    fun localizedAccountTier(isArabic: Boolean): String =
        if (isArabic && accountTier == "Pulse Pioneer") "رائد النبض" else accountTier
}

enum class CableDnaTier(
    val title: String,
    val titleAr: String,
    val grade: String,
    val gradeAr: String,
    val badgeColor: Color,
    val description: String,
    val descriptionAr: String,
    val isDangerous: Boolean
) {
    LEGENDARY_ORIGINAL(
        title = "OEM High-Conductivity",
        titleAr = "كابل أصلي عالي التوصيل",
        grade = "TIER S+",
        gradeAr = "الفئة S+",
        badgeColor = TactileGreen,
        description = "Ultra-low loop resistance (<125mΩ). Minimal I²R drop compliant with IEC 62680 & USB-IF 5A PD.",
        descriptionAr = "مقاومة حلقة فائقة الانخفاض (<125mΩ) وهبوط جهد ضئيل مطابق لمعايير IEC 62680 و USB-IF 5A PD.",
        isDangerous = false
    ),
    CERTIFIED_FAST(
        title = "Certified Fast PD/PPS",
        titleAr = "كابل شحن سريع معتمد PD/PPS",
        grade = "TIER A",
        gradeAr = "الفئة A",
        badgeColor = TactileCyan,
        description = "Nominal cable resistance (126–230mΩ). Stable power delivery under sustained charging load.",
        descriptionAr = "مقاومة كابل طبيعية (126–230mΩ) مع استقرار ممتاز لنقل الطاقة تحت الحمل المستمر.",
        isDangerous = false
    ),
    DEGRADED_STANDARD(
        title = "Degraded Standard Line",
        titleAr = "كابل قياسي متراجع الأداء",
        grade = "TIER C",
        gradeAr = "الفئة C",
        badgeColor = TactileAmber,
        description = "Elevated loop resistance (231–390mΩ). Moderate voltage sag limits maximum charging wattage.",
        descriptionAr = "مقاومة حلقة مرتفعة (231–390mΩ) تسبب هبوط جهد متوسط وتحد من قدرة الشحن القصوى.",
        isDangerous = false
    ),
    DANGEROUS_COUNTERFEIT(
        title = "High-Loss / Non-Compliant",
        titleAr = "عالي الفقد / غير مطابق",
        grade = "CRITICAL",
        gradeAr = "حرج",
        badgeColor = TactileCoralRed,
        description = "Excessive loop resistance (>390mΩ) or voltage drop >750mV. Significant conductor Joule heating.",
        descriptionAr = "مقاومة مرتفعة جداً (>390mΩ) أو هبوط جهد >750mV مع تسخين ملحوظ في الموصلات.",
        isDangerous = true
    );

    fun localizedTitle(isArabic: Boolean): String = if (isArabic) titleAr else title
    fun localizedGrade(isArabic: Boolean): String = if (isArabic) gradeAr else grade
    fun localizedDescription(isArabic: Boolean): String = if (isArabic) descriptionAr else description
}

data class CableDiagnosticResult(
    val resistanceMilliOhms: Int = 0,
    val voltageDropMv: Int = 0,
    val measuredVoltageMv: Int = 0,
    val measuredCurrentMa: Int = 0,
    val maxWattageW: Float = 0.0f,
    val qualityScore: Int = 100,
    val tier: CableDnaTier = CableDnaTier.LEGENDARY_ORIGINAL,
    val usbProtocol: String = "Standby (Unplugged)",
    val pinIntegrityPercent: Int = 100,
    val thermalDissipationMilliWatts: Int = 0,
    val estimatedAwg: String = "20 AWG (5A E-Marked)",
    val lineEfficiencyPercent: Float = 100.0f,
    val standardCompliance: String = "IEC 62680 • USB-IF Ready",
    val chargingPhase: String = "Standby",
    val recommendedCableSpec: String = "USB-IF 3A/5A Type-C",
    val cellTopology: String = "1S Single-Cell",
    val tailoredDeviceNote: String = "",
    val scanTimestamp: Long = System.currentTimeMillis()
)

enum class DeviceFormFactor(val labelAr: String, val labelEn: String) {
    PHONE("هاتف ذكي", "Smartphone"),
    FOLDABLE("هاتف قابل للطي", "Foldable"),
    TABLET("جهاز لوحي", "Tablet");

    fun localizedLabel(isArabic: Boolean): String = if (isArabic) labelAr else labelEn
}

data class DeviceHardwareProfile(
    val manufacturer: String = "Android",
    val brand: String = "Generic",
    val model: String = "Device",
    val codeName: String = "unknown",
    val chipset: String = "ARM64 SoC",
    val androidVersion: String = "Android",
    val sdkInt: Int = 24,
    val totalRamGb: Float = 8.0f,
    val cpuCores: Int = 8,
    val primaryAbi: String = "arm64-v8a",
    val formFactor: DeviceFormFactor = DeviceFormFactor.PHONE,
    val chargingEcosystemAr: String = "USB-IF PD 3.0 / PPS القياسي",
    val chargingEcosystemEn: String = "USB-IF PD 3.0 / PPS",
    val recommendedCableAr: String = "كابل Type-C 3A/5A متوافق مع USB-IF (مقاومة < 180 mΩ)",
    val recommendedCableEn: String = "USB-IF certified Type-C 3A/5A cable (resistance < 180 mΩ)",
    val recommendedPowerWatts: Int = 25,
    val safeThermalCeilingC: Float = 41.0f,
    val nominalDesignCapacityMah: Int = 5000,
    val isDualCellSuspected: Boolean = false
) {
    fun localizedEcosystem(isArabic: Boolean): String = if (isArabic) chargingEcosystemAr else chargingEcosystemEn
    fun localizedRecommendedCable(isArabic: Boolean): String = if (isArabic) recommendedCableAr else recommendedCableEn
}

enum class CellHealthStatus(
    val label: String,
    val labelAr: String,
    val color: Color,
    val description: String,
    val descriptionAr: String
) {
    PRISTINE(
        "Pristine Medical Grade",
        "حالة نقية ممتازة",
        TactileGreen,
        "Solid crystal cathode lattice. Zero measurable dendrite formation.",
        "بنية كاثود بلورية سليمة مع انعدام ترسبات التشعب الليثيومي."
    ),
    OPTIMAL(
        "Optimal Operating Matrix",
        "مصفوفة تشغيل مثالية",
        TactileCyan,
        "Nominal cycle degradation within manufacturer design specifications.",
        "تآكل دورات طبيعي ضمن المواصفات التصميمية القياسية للشركة المصنعة."
    ),
    MODERATE(
        "Aging Cell Matrix",
        "تراجع تدريجي في الخلية",
        TactileAmber,
        "Slight capacity fade detected. Recommend 20%-80% charging habits.",
        "رصد تراجع طفيف في السعة الكيميائية؛ يُنصح بالشحن بين 20% و 80%."
    ),
    CRITICAL(
        "Severe Cell Degradation",
        "تآكل حرج في الخلية",
        TactileCoralRed,
        "Noticeable internal resistance buildup. Replacement recommended.",
        "ارتفاع ملحوظ في المقاومة الداخلية للخلية؛ يُوصى بفحص أو استبدال البطارية."
    );

    fun localizedLabel(isArabic: Boolean): String = if (isArabic) labelAr else label
    fun localizedDescription(isArabic: Boolean): String = if (isArabic) descriptionAr else description
}

data class BatterySohPrediction(
    val healthPercentage: Float = 100.0f,
    val originalCapacityMah: Int = 5000,
    val estimatedCurrentCapacityMah: Int = 5000,
    val totalCyclesCompleted: Int = 0,
    val estimatedRemainingCycles: Int = 1000,
    val estimatedLifespanDays: Int = 1000,
    val degradationRatePer100Cycles: Float = 2.0f,
    val coulombicEfficiencyPercent: Float = 99.5f,
    val internalResistanceEstimateMilliOhms: Int = 45,
    val cellStatus: CellHealthStatus = CellHealthStatus.PRISTINE,
    val projectedReplacementDate: String = "--",
    val chemistryType: String = "Li-Ion / Li-Poly (NMC)"
)

/**
 * Cyber Skin Definition for Liquid Glass Vault.
 */
data class CyberSkin(
    val id: String,
    val name: String,
    val nameAr: String,
    val codename: String,
    val themeDescription: String,
    val themeDescriptionAr: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val ambientAura: Color,
    val glowIntensity: Float = 0.85f,
    val isDarkRecommended: Boolean = true,
    val tag: String = "VAULT EXCLUSIVE",
    val tagAr: String = "حصري",
    val holographicBorder: List<Color>
) {
    fun localizedName(isArabic: Boolean): String = if (isArabic) nameAr else name
    fun localizedDescription(isArabic: Boolean): String = if (isArabic) themeDescriptionAr else themeDescription
    fun localizedTag(isArabic: Boolean): String = if (isArabic) tagAr else tag
}

object CyberSkinVaultCatalog {
    val allSkins: List<CyberSkin> = listOf(
        CyberSkin(
            id = "obsidian_cyber",
            name = "Obsidian Liquid Void",
            nameAr = "فراغ الأوبسيديان السائل",
            codename = "SKIN // 01-VOID",
            themeDescription = "Deep obsidian glass with quantum neon cyan photon aura and sub-zero highlights.",
            themeDescriptionAr = "زجاج أوبسيديان عميق مع هالة سماوية باردة وإبرازات ضوئية متوازنة.",
            primaryColor = Color(0xFF00E5FF),
            secondaryColor = Color(0xFF00B0FF),
            ambientAura = Color(0xFF0091EA),
            glowIntensity = 0.90f,
            isDarkRecommended = true,
            tag = "QUANTUM",
            tagAr = "كوانتم",
            holographicBorder = listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFF00E5FF))
        ),
        CyberSkin(
            id = "overdrive_plasma",
            name = "Overdrive Plasma 4.0",
            nameAr = "بلازما الطاقة القصوى 4.0",
            codename = "SKIN // 02-OVERDRIVE",
            themeDescription = "High-energy thermonuclear orange-crimson glow with molten amber core currents.",
            themeDescriptionAr = "توهج كهرماني وقرمزي عالي الطاقة مع خطوط إضاءة دافئة.",
            primaryColor = Color(0xFFFF3D00),
            secondaryColor = Color(0xFFFF9100),
            ambientAura = Color(0xFFFF1744),
            glowIntensity = 1.0f,
            isDarkRecommended = true,
            tag = "OVERDRIVE",
            tagAr = "اندفاع",
            holographicBorder = listOf(Color(0xFFFF3D00), Color(0xFFFF9100), Color(0xFFFF1744))
        ),
        CyberSkin(
            id = "cryo_glacier",
            name = "Cryo Glacier Frost",
            nameAr = "صقيع الأنهار الجليدية",
            codename = "SKIN // 03-CRYO",
            themeDescription = "Sub-zero crystalline ice glass with arctic chill luminescence and glacial refraction.",
            themeDescriptionAr = "زجاج بلوري بارد مستوحى من الجليد القطبي بانعكاسات سماوية هادئة.",
            primaryColor = Color(0xFF00B0FF),
            secondaryColor = Color(0xFF80D8FF),
            ambientAura = Color(0xFF40C4FF),
            glowIntensity = 0.85f,
            isDarkRecommended = false,
            tag = "SUB-ZERO",
            tagAr = "بارد",
            holographicBorder = listOf(Color(0xFF80D8FF), Color(0xFF00B0FF), Color(0xFFFFFFFF))
        ),
        CyberSkin(
            id = "bio_emerald",
            name = "Bioluminescent Matrix",
            nameAr = "المصفوفة الزمردية الحيوية",
            codename = "SKIN // 04-BIO",
            themeDescription = "Toxic radioactive neon emerald radiance with bio-electrochemical pulse waves.",
            themeDescriptionAr = "إشعاع زمردي حيوي مستلهم من نبضات الطاقة الكهروكيميائية النقية.",
            primaryColor = Color(0xFF00E676),
            secondaryColor = Color(0xFF69F0AE),
            ambientAura = Color(0xFF00C853),
            glowIntensity = 0.90f,
            isDarkRecommended = true,
            tag = "BIO-RAD",
            tagAr = "حيوي",
            holographicBorder = listOf(Color(0xFF00E676), Color(0xFF76FF03), Color(0xFF00C853))
        ),
        CyberSkin(
            id = "solar_auric",
            name = "Solar Flare 24K Auric",
            nameAr = "التوهج الشمسي الذهبي",
            codename = "SKIN // 05-SOLAR",
            themeDescription = "Ultra-bright coronal solar gold and molten amber rays for prestige power tracking.",
            themeDescriptionAr = "ذهب شمسي متوهج مع لمسات كهرمانية فاخرة لمراقبة الطاقة بأناقة.",
            primaryColor = Color(0xFFFFD700),
            secondaryColor = Color(0xFFFFA000),
            ambientAura = Color(0xFFFF8F00),
            glowIntensity = 0.92f,
            isDarkRecommended = true,
            tag = "LEGENDARY",
            tagAr = "ذهبي",
            holographicBorder = listOf(Color(0xFFFFD700), Color(0xFFFFAB00), Color(0xFFFFE082))
        ),
        CyberSkin(
            id = "vapor_sunset",
            name = "Vaporwave Neon Sunset",
            nameAr = "غسق النيون البنفسجي",
            codename = "SKIN // 06-VAPOR",
            themeDescription = "Retro-futuristic electric magenta and cosmic violet neon twilight waves.",
            themeDescriptionAr = "أمواج غسق بنفسجية وأرجوانية حديثة بتباين مريح للعين.",
            primaryColor = Color(0xFFE040FB),
            secondaryColor = Color(0xFF7C4DFF),
            ambientAura = Color(0xFF651FFF),
            glowIntensity = 0.88f,
            isDarkRecommended = true,
            tag = "TWILIGHT",
            tagAr = "غسقي",
            holographicBorder = listOf(Color(0xFFE040FB), Color(0xFF7C4DFF), Color(0xFF00E5FF))
        )
    )

    fun getSkinById(id: String): CyberSkin {
        return allSkins.find { it.id == id } ?: allSkins.first()
    }
}

