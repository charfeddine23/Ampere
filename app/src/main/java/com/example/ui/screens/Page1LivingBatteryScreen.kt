package com.example.ui.screens

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.audio.CyberHapticsEngine
import com.example.data.BatteryStatus
import com.example.data.BatteryTelemetry
import com.example.data.DeviceHardwareProfile
import com.example.data.GlassThemeConfig
import com.example.data.LivingBatteryState
import com.example.data.MetricType
import com.example.data.TemperatureUnit
import com.example.data.telemetry.AppBatteryConsumption
import com.example.data.telemetry.AppBatteryUsageTracker
import com.example.data.telemetry.AppUsageSnapshot
import com.example.data.telemetry.BatteryScienceClassifier
import com.example.ui.components.LiquidBatteryRing
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.PulsingNeonDot
import com.example.ui.components.VoltMetricTile
import com.example.ui.components.VoltSectionHeader
import com.example.ui.theme.LocalAmpereTheme
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.TactileAmber
import com.example.ui.theme.TactileCoralRed
import com.example.ui.theme.TactileCyan
import com.example.ui.theme.TactileGreen
import com.example.ui.theme.TextCharcoal
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs

private val appIconCache = LruCache<String, ImageBitmap>(48)

/**
 * PAGE 1: Precision Live Battery Monitor
 * Redesigned around minimalist Swiss Precision Telemetry:
 * 1. Compact single-row status & sensor pill
 * 2. Hero circular battery ring + 3-stat quick telemetry bar
 * 3. Conditional JEITA / IEEE 1625 thermal stress notice
 * 4. Unified 2x2 scientific telemetry matrix
 * 5. Ranked per-app battery consumption panel
 */
@Composable
fun Page1LivingBatteryScreen(
    telemetry: BatteryTelemetry,
    themeConfig: GlassThemeConfig,
    deviceProfile: DeviceHardwareProfile = DeviceHardwareProfile(),
    appUsageSnapshot: AppUsageSnapshot = AppUsageSnapshot(),
    onRefreshAppUsage: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val isDark = themeConfig.isDarkMode
    val isArabic = themeConfig.appLanguage.isArabic
    val state = telemetry.livingState
    val resolvedTheme = LocalAmpereTheme.current
    val customTokens = resolvedTheme.themeId != null || resolvedTheme.isSimpleMode || resolvedTheme.isProMode

    val primaryColor = when {
        resolvedTheme.isSimpleMode -> resolvedTheme.accent
        resolvedTheme.isProMode -> lerp(state.neonPrimary, resolvedTheme.accent, 0.55f)
        else -> state.neonPrimary
    }
    val glow = when {
        resolvedTheme.isSimpleMode -> 0f
        resolvedTheme.isProMode -> 0.35f
        else -> themeConfig.glowIntensity
    }
    val autoSensorsColor = when {
        resolvedTheme.isSimpleMode || resolvedTheme.isProMode -> resolvedTheme.accent
        else -> TactileGreen
    }
    val textColor = if (customTokens) resolvedTheme.textPrimary else if (isDark) TextWhite else TextCharcoal
    val subTextColor = if (customTokens) resolvedTheme.textSecondary else if (isDark) Color(0xFF94A3B8) else TextSlate
    val insetBg = if (customTokens) resolvedTheme.surfaceInset else if (isDark) Color(0xFF151F30) else Color(0xFFDCE4EE)

    val scienceProfile = remember(telemetry, deviceProfile, isArabic, themeConfig.temperatureUnit) {
        BatteryScienceClassifier.buildScienceProfile(
            telemetry = telemetry,
            deviceProfile = deviceProfile,
            isArabic = isArabic,
            temperatureUnit = themeConfig.temperatureUnit
        )
    }

    val displayTemp = if (themeConfig.temperatureUnit == TemperatureUnit.FAHRENHEIT) {
        (telemetry.temperatureC * 9f / 5f) + 32f
    } else {
        telemetry.temperatureC
    }
    val formattedTemp = "${String.format(Locale.US, "%.1f", displayTemp)}${themeConfig.temperatureUnit.symbol}"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
            .graphicsLayer { clip = false }
    ) {
        // 1. Compact Status & Auto-Sensors Strip Pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentColor = primaryColor,
                isDarkMode = isDark,
                shape = RoundedCornerShape(16.dp),
                glowIntensity = glow
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(primaryColor.copy(alpha = if (isDark) 0.18f else 0.12f))
                                .border(1.dp, primaryColor.copy(alpha = 0.40f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (state) {
                                    LivingBatteryState.OVERDRIVE_HEATING -> Icons.Default.LocalFireDepartment
                                    LivingBatteryState.COOLING -> Icons.Default.AcUnit
                                    LivingBatteryState.ENERGY_STORING -> Icons.Default.Bolt
                                    LivingBatteryState.DEEP_SLEEP -> Icons.Default.Bedtime
                                },
                                contentDescription = state.localizedTitle(isArabic),
                                tint = primaryColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val titleText = state.localizedTitle(isArabic).let {
                                    if (isArabic) it else it.uppercase()
                                }
                                Text(
                                    text = titleText,
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.3.sp,
                                    color = textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                PulsingNeonDot(color = primaryColor, sizeDp = 5.dp)
                            }
                            val syncTag = if (themeConfig.isProMode) {
                                if (isArabic) " • 10ms" else " • 10ms PRO"
                            } else ""
                            Text(
                                text = "${scienceProfile.phaseLabelAr} • $formattedTemp • ${String.format(Locale.US, "%.2f", scienceProfile.cRate)}C$syncTag",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 10.sp,
                                color = subTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(autoSensorsColor.copy(alpha = if (isDark) 0.18f else 0.12f))
                            .border(1.dp, autoSensorsColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                            .testTag("auto_sensors_status_badge"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = if (isArabic) "مستشعرات تلقائية نشطة" else "Active Auto Sensors",
                                tint = autoSensorsColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isArabic) "تلقائي" else "AUTO",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp,
                                letterSpacing = 0.4.sp,
                                color = autoSensorsColor
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Hero Precision Battery Ring
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            LiquidBatteryRing(
                percentage = telemetry.percentage,
                status = telemetry.status,
                plugType = telemetry.plugType,
                wattageW = telemetry.wattageW,
                livingState = state,
                isDarkMode = isDark,
                sizeDp = 216.dp,
                glowIntensity = glow,
                ambientLiquidWaves = themeConfig.ambientLiquidWaves
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2b. Compact 3-Stat Quick Summary Strip under the Ring
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentColor = primaryColor,
                isDarkMode = isDark,
                shape = RoundedCornerShape(16.dp),
                glowIntensity = glow * 0.65f
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val voltageLabel = if (telemetry.voltageMv > 0) {
                        "${String.format(Locale.US, "%.2f", telemetry.voltageMv / 1000f)} V"
                    } else "--"
                    val currentLabel = "${if (telemetry.currentMa >= 0) "+" else ""}${telemetry.currentMa} mA"
                    val estLabel = formatRemainingTime(
                        minutes = telemetry.estimatedMinutesRemaining,
                        status = telemetry.status,
                        percent = telemetry.percentage,
                        isArabic = isArabic
                    )

                    QuickTelemetryStatItem(
                        label = if (isArabic) "الجهد" else "Voltage",
                        value = voltageLabel,
                        accent = primaryColor,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        modifier = Modifier.weight(1f)
                    )
                    QuickStripDivider(color = subTextColor.copy(alpha = 0.22f))
                    QuickTelemetryStatItem(
                        label = if (isArabic) "التيار" else "Current",
                        value = currentLabel,
                        accent = primaryColor,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        modifier = Modifier.weight(1f)
                    )
                    QuickStripDivider(color = subTextColor.copy(alpha = 0.22f))
                    QuickTelemetryStatItem(
                        label = if (isArabic) "المتبقي" else "Est. Time",
                        value = estLabel,
                        accent = primaryColor,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Conditional JEITA / IEEE 1625 Thermal Stress Notice
        if (state == LivingBatteryState.OVERDRIVE_HEATING) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = TactileCoralRed,
                    isDarkMode = isDark,
                    shape = RoundedCornerShape(18.dp),
                    glowIntensity = glow,
                    testTag = "thermal_stress_warning_card"
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = if (isArabic) "تحذير إجهاد حراري" else "Thermal Stress Warning",
                                tint = TactileCoralRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isArabic) {
                                    "إجهاد حراري: سقف JEITA (≥${deviceProfile.safeThermalCeilingC}°C)"
                                } else {
                                    "THERMAL STRESS: JEITA CEILING (≥${deviceProfile.safeThermalCeilingC}°C)"
                                },
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 0.3.sp,
                                color = TactileCoralRed
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isArabic) {
                                "${scienceProfile.thermalAssessment.detailAr} القراءة الحالية: $formattedTemp."
                            } else {
                                "${scienceProfile.thermalAssessment.detailAr} Current reading: $formattedTemp."
                            },
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.sp,
                            color = subTextColor,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 4. Unified 2x2 Scientific Telemetry Matrix
        VoltSectionHeader(
            title = if (isArabic) "القياسات الهندسية الحية" else "Live Hardware Telemetry",
            isDark = isDark
        )
        Spacer(modifier = Modifier.height(8.dp))

        val metrics = scienceProfile.orderedMetrics
        val firstRow = metrics.take(2)
        val secondRow = metrics.drop(2).take(2)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("scientific_telemetry_matrix"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(firstRow to 0, secondRow to 2).forEach { (rowMetrics, baseIndex) ->
                if (rowMetrics.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowMetrics.forEachIndexed { localIdx, metric ->
                            val priorityIdx = baseIndex + localIdx
                            val metricColor = when {
                                metric.isPriorityWarning -> TactileCoralRed
                                metric.type == MetricType.TEMPERATURE -> when (scienceProfile.thermalAssessment.severityLevel) {
                                    3 -> TactileCoralRed
                                    2 -> TactileAmber
                                    0 -> TactileCyan
                                    else -> TactileGreen
                                }
                                priorityIdx == 0 -> primaryColor
                                else -> primaryColor.copy(alpha = 0.90f)
                            }
                            val displayValue = if (metric.type == MetricType.TEMPERATURE) {
                                formattedTemp
                            } else {
                                metric.formattedValue
                            }
                            val displaySubtext = if (metric.type == MetricType.TEMPERATURE) {
                                scienceProfile.thermalAssessment.zoneTitleAr
                            } else {
                                metric.standardRangeAr
                            }

                            VoltMetricTile(
                                modifier = Modifier.weight(1f),
                                title = metric.titleAr,
                                value = displayValue,
                                subtext = displaySubtext,
                                accentColor = metricColor,
                                isDark = isDark,
                                highlightBadge = if (priorityIdx == 0) {
                                    if (isArabic) "أولوية" else "PRIMARY"
                                } else null,
                                testTag = "scientific_metric_${metric.type.name.lowercase(Locale.US)}"
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Per-App Battery Consumption Panel
        AppBatteryConsumptionSection(
            snapshot = appUsageSnapshot,
            accentColor = primaryColor,
            isDarkMode = isDark,
            glowIntensity = glow,
            insetBg = insetBg,
            textColor = textColor,
            subTextColor = subTextColor,
            isArabic = isArabic,
            onRefresh = onRefreshAppUsage
        )
    }
}

@Composable
private fun QuickTelemetryStatItem(
    label: String,
    value: String,
    accent: Color,
    textColor: Color,
    subTextColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontFamily = PlusJakartaSansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            color = subTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun QuickStripDivider(color: Color) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(color)
    )
}

private fun formatRemainingTime(
    minutes: Int,
    status: BatteryStatus,
    percent: Int,
    isArabic: Boolean
): String {
    if (status == BatteryStatus.FULL && percent >= 100) {
        return if (isArabic) "مكتمل" else "Full"
    }
    if (minutes <= 0) {
        val isCharging = status == BatteryStatus.CHARGING || status == BatteryStatus.FULL
        return when {
            isCharging && percent >= 95 -> if (isArabic) "قريب الامتلاء" else "Soon"
            isCharging -> if (isArabic) "جاري الحساب" else "Calculating"
            else -> "--"
        }
    }
    val hrs = minutes / 60
    val mins = minutes % 60
    return if (isArabic) {
        if (hrs > 0) "${hrs}س ${mins}د" else "${mins}د"
    } else {
        if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
    }
}

@Composable
private fun AppBatteryConsumptionSection(
    snapshot: AppUsageSnapshot,
    accentColor: Color,
    isDarkMode: Boolean,
    glowIntensity: Float,
    insetBg: Color,
    textColor: Color,
    subTextColor: Color,
    isArabic: Boolean,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    var showAllApps by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = 0.88f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
            accentColor = accentColor,
            isDarkMode = isDarkMode,
            shape = RoundedCornerShape(20.dp),
            glowIntensity = glowIntensity,
            testTag = "app_battery_consumption_card"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(accentColor.copy(alpha = if (isDarkMode) 0.18f else 0.12f))
                                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isArabic) "استهلاك التطبيقات للبطارية" else "App Battery Usage",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = textColor
                            )
                            Text(
                                text = if (snapshot.hasPermission) {
                                    if (isArabic) {
                                        "الأعلى استهلاكاً • آخر ${snapshot.scanWindowHours} ساعة (${snapshot.totalTrackedMah} mAh)"
                                    } else {
                                        "Top drain first • Last ${snapshot.scanWindowHours}h (${snapshot.totalTrackedMah} mAh)"
                                    }
                                } else {
                                    if (isArabic) "يتطلب صلاحية مراقبة استخدام التطبيقات" else "Requires App Usage Access"
                                },
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 10.5.sp,
                                color = subTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(insetBg.copy(alpha = 0.7f))
                            .clickable {
                                CyberHapticsEngine.triggerTactileTick(context)
                                onRefresh()
                            }
                            .testTag("refresh_app_usage_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = if (isArabic) "تحديث قائمة الاستهلاك" else "Refresh App Usage",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                when {
                    snapshot.isLoading -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = accentColor,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isArabic) "جاري تحليل استهلاك التطبيقات..." else "Analyzing app battery consumption...",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.sp,
                                color = subTextColor
                            )
                        }
                    }

                    !snapshot.hasPermission -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(insetBg.copy(alpha = 0.72f))
                                .border(1.dp, TactileAmber.copy(alpha = 0.42f), RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isArabic) "منح صلاحية الوصول إلى بيانات الاستخدام" else "Grant Usage Access Permission",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = textColor
                            )
                            Text(
                                text = if (isArabic) {
                                    "لمعرفة التطبيقات التي تستهلك البطارية بشكل حقيقي وترتيبها من الأعلى استهلاكاً، يرجى تفعيل صلاحية مراقبة الاستخدام لتطبيق Volt Core."
                                } else {
                                    "To inspect real app battery drain and rank the highest consumers, please grant Usage Access permission to Volt Core."
                                },
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 11.sp,
                                color = subTextColor,
                                lineHeight = 16.sp
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(accentColor)
                                    .clickable {
                                        CyberHapticsEngine.triggerTactileTick(context)
                                        AppBatteryUsageTracker.openUsageAccessSettings(context)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("grant_usage_access_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LockOpen,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isArabic) "منح الصلاحية الآن" else "Grant Permission Now",
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    snapshot.apps.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(insetBg.copy(alpha = 0.65f))
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArabic) {
                                    "لا توجد تطبيقات نشطة مسجلة خلال آخر 24 ساعة حتى الآن."
                                } else {
                                    "No active app battery usage recorded in the last 24 hours yet."
                                },
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 11.5.sp,
                                color = subTextColor
                            )
                        }
                    }

                    else -> {
                        val visibleApps = if (showAllApps) snapshot.apps else snapshot.apps.take(5)
                        val maxPercent = snapshot.apps.firstOrNull()?.consumptionPercent?.coerceAtLeast(1f) ?: 100f

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            visibleApps.forEach { app ->
                                key(app.packageName) {
                                    AppConsumptionRowItem(
                                        app = app,
                                        maxPercent = maxPercent,
                                        accentColor = accentColor,
                                        insetBg = insetBg,
                                        textColor = textColor,
                                        subTextColor = subTextColor,
                                        isDarkMode = isDarkMode,
                                        isArabic = isArabic,
                                        onClick = {
                                            CyberHapticsEngine.triggerTactileTick(context)
                                            AppBatteryUsageTracker.openAppBatteryDetailsSettings(context, app.packageName)
                                        }
                                    )
                                }
                            }

                            if (snapshot.apps.size > 5) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(insetBg.copy(alpha = 0.70f))
                                        .border(1.dp, accentColor.copy(alpha = 0.32f), RoundedCornerShape(12.dp))
                                        .clickable {
                                            CyberHapticsEngine.triggerTactileTick(context)
                                            showAllApps = !showAllApps
                                        }
                                        .testTag("toggle_all_app_usage_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isArabic) {
                                            if (showAllApps) "عرض أعلى 5 تطبيقات فقط" else "عرض جميع التطبيقات (${snapshot.apps.size})"
                                        } else {
                                            if (showAllApps) "Show Top 5 Apps Only" else "Show All Apps (${snapshot.apps.size})"
                                        },
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = accentColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppConsumptionRowItem(
    app: AppBatteryConsumption,
    maxPercent: Float,
    accentColor: Color,
    insetBg: Color,
    textColor: Color,
    subTextColor: Color,
    isDarkMode: Boolean,
    isArabic: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val cachedIcon = remember(app.packageName) { appIconCache.get(app.packageName) }
    val appIconBitmap by produceState<ImageBitmap?>(initialValue = cachedIcon, key1 = app.packageName) {
        if (value == null) {
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    val drawable = context.packageManager.getApplicationIcon(app.packageName)
                    drawable.toBitmap(width = 72, height = 72, config = Bitmap.Config.ARGB_8888).asImageBitmap()
                }.getOrNull()
            }
            if (loaded != null) {
                appIconCache.put(app.packageName, loaded)
                value = loaded
            }
        }
    }

    val rankColor = when (app.rank) {
        1 -> TactileCoralRed
        2 -> TactileAmber
        3 -> TactileCyan
        else -> accentColor
    }
    val relativeFill = (app.consumptionPercent / maxPercent).coerceIn(0.06f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(insetBg.copy(alpha = if (isDarkMode) 0.52f else 0.62f))
            .border(
                width = if (app.rank == 1) 1.dp else 0.7.dp,
                color = if (app.rank == 1) rankColor.copy(alpha = 0.45f) else rankColor.copy(alpha = 0.16f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp)
            .testTag("app_usage_row_${app.rank}"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(rankColor.copy(alpha = if (isDarkMode) 0.22f else 0.14f))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${app.rank}",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        color = rankColor
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = if (isDarkMode) 0.08f else 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    val resolvedIcon = appIconBitmap
                    if (resolvedIcon != null) {
                        Image(
                            bitmap = resolvedIcon,
                            contentDescription = app.appName,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = app.appName,
                            tint = rankColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(9.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = app.appName,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (app.isHighDrain) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(TactileCoralRed.copy(alpha = 0.16f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "استهلاك عالٍ" else "High Drain",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.5.sp,
                                    color = TactileCoralRed
                                )
                            }
                        }
                    }
                    Text(
                        text = if (isArabic) {
                            "نشط ${app.formattedDuration(true)} • ≈ ${app.estimatedMah} mAh"
                        } else {
                            "Active ${app.formattedDuration(false)} • ≈ ${app.estimatedMah} mAh"
                        },
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 10.sp,
                        color = subTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${String.format(Locale.US, "%.1f", app.consumptionPercent)}%",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = rankColor
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(if (isDarkMode) Color(0x33475569) else Color(0x4D94A3B8))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(relativeFill)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(rankColor, accentColor)
                        )
                    )
            )
        }
    }
}
