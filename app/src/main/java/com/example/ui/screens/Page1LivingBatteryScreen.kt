package com.example.ui.screens

import android.graphics.Bitmap
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import java.util.Locale

/**
 * PAGE 1: Scientific Real-Time Battery Monitor
 * Structured around IEEE 1625 / JEITA / BU-808 electrochemistry:
 * 1. Compact live hardware state banner & 100% Auto Sensors indicator
 * 2. Live liquid ring gauge (SoC %, W, plug rail)
 * 3. Conditional IEEE 1625 / JEITA Thermal Stress warning (>= device ceiling)
 * 4. 2x2 Scientific Telemetry Matrix dynamically prioritized by active state
 * 5. Per-App Battery Consumption ranking (sorted highest consumer first)
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
    val state = telemetry.livingState
    val resolvedTheme = LocalAmpereTheme.current
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
        resolvedTheme.isSimpleMode -> resolvedTheme.accent
        resolvedTheme.isProMode -> resolvedTheme.accent
        else -> TactileGreen
    }
    val textColor = if (resolvedTheme.themeId != null) resolvedTheme.textPrimary else if (isDark) TextWhite else TextCharcoal
    val subTextColor = if (resolvedTheme.themeId != null) resolvedTheme.textSecondary else TextSlate

    val scienceProfile = remember(telemetry, deviceProfile) {
        BatteryScienceClassifier.buildScienceProfile(telemetry, deviceProfile)
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
        // 1. Compact Real-Time Sensor State Banner
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
                        .padding(horizontal = 12.dp, vertical = 9.dp),
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
                                .background(primaryColor.copy(alpha = if (isDark) 0.22f else 0.12f))
                                .border(1.dp, primaryColor.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (state) {
                                    LivingBatteryState.OVERDRIVE_HEATING -> Icons.Default.LocalFireDepartment
                                    LivingBatteryState.COOLING -> Icons.Default.AcUnit
                                    LivingBatteryState.ENERGY_STORING -> Icons.Default.Bolt
                                    LivingBatteryState.DEEP_SLEEP -> Icons.Default.Bedtime
                                },
                                contentDescription = state.title,
                                tint = primaryColor,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = state.title.uppercase(),
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.3.sp,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                PulsingNeonDot(color = primaryColor, sizeDp = 5.dp)
                            }
                            val syncTag = if (themeConfig.isProMode) " • 1ms PRO" else ""
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
                            .background(autoSensorsColor.copy(alpha = if (isDark) 0.22f else 0.14f))
                            .border(1.dp, autoSensorsColor.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("auto_sensors_status_badge"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = "مستشعرات تلقائية نشطة",
                                tint = autoSensorsColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "100% AUTO SENSORS",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                letterSpacing = 0.3.sp,
                                color = autoSensorsColor
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Center Liquid Battery Ring
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            LiquidBatteryRing(
                percentage = telemetry.percentage,
                status = telemetry.status,
                plugType = telemetry.plugType,
                wattageW = telemetry.wattageW,
                livingState = state,
                isDarkMode = isDark,
                sizeDp = 224.dp,
                glowIntensity = glow,
                ambientLiquidWaves = themeConfig.ambientLiquidWaves
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

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
                    glowIntensity = glow,
                    testTag = "thermal_stress_warning_card"
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "تحذير إجهاد حراري",
                                tint = TactileCoralRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "THERMAL STRESS: JEITA CEILING (≥${deviceProfile.safeThermalCeilingC}°C)",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 0.4.sp,
                                color = TactileCoralRed
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${scienceProfile.thermalAssessment.detailAr} القراءة الحالية: $formattedTemp.",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.sp,
                            color = subTextColor,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 4. Restructured 2x2 Scientific Telemetry Matrix (All 4 hardware sensors prioritized by IEEE/JEITA/BU-808)
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

                            ScientificMetricTile(
                                modifier = Modifier.weight(1f),
                                label = "#${priorityIdx + 1} ${metric.titleAr}",
                                value = displayValue,
                                color = metricColor,
                                isDarkMode = isDark,
                                textColor = textColor,
                                subTextColor = subTextColor,
                                subtext = displaySubtext,
                                testTag = "scientific_metric_${metric.type.name.lowercase(Locale.US)}"
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Per-App Battery Consumption Section (Directly below telemetry matrix)
        AppBatteryConsumptionSection(
            snapshot = appUsageSnapshot,
            accentColor = primaryColor,
            isDarkMode = isDark,
            glowIntensity = glow,
            onRefresh = onRefreshAppUsage
        )
    }
}

@Composable
private fun ScientificMetricTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color,
    isDarkMode: Boolean,
    textColor: Color,
    subTextColor: Color,
    subtext: String,
    testTag: String
) {
    LiquidGlassCard(
        modifier = modifier,
        accentColor = color,
        isDarkMode = isDarkMode,
        shape = RoundedCornerShape(16.dp),
        testTag = testTag
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    color = subTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.5.sp,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtext,
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 9.sp,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AppBatteryConsumptionSection(
    snapshot: AppUsageSnapshot,
    accentColor: Color,
    isDarkMode: Boolean,
    glowIntensity: Float,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val resolvedTheme = LocalAmpereTheme.current
    val textColor = if (resolvedTheme.themeId != null) resolvedTheme.textPrimary else if (isDarkMode) TextWhite else TextCharcoal
    val subTextColor = if (resolvedTheme.themeId != null) resolvedTheme.textSecondary else TextSlate
    val insetBg = if (resolvedTheme.themeId != null) resolvedTheme.surfaceInset else if (isDarkMode) Color(0xFF151F30) else Color(0xFFD2DBE6)

    var showAllApps by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            accentColor = accentColor,
            isDarkMode = isDarkMode,
            shape = RoundedCornerShape(22.dp),
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
                                .size(36.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(accentColor.copy(alpha = if (isDarkMode) 0.22f else 0.13f))
                                .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(11.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "استهلاك التطبيقات للبطارية",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = textColor
                            )
                            Text(
                                text = if (snapshot.hasPermission) {
                                    "مرتبة من الأعلى استهلاكاً • آخر ${snapshot.scanWindowHours} ساعة (${snapshot.totalTrackedMah} mAh)"
                                } else {
                                    "يتطلب صلاحية مراقبة استخدام التطبيقات"
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
                            contentDescription = "تحديث قائمة الاستهلاك",
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
                                text = "جاري تحليل استهلاك التطبيقات...",
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
                                .background(insetBg.copy(alpha = 0.75f))
                                .border(1.dp, TactileAmber.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "منح صلاحية الوصول إلى بيانات الاستخدام",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = textColor
                            )
                            Text(
                                text = "لمعرفة التطبيقات التي تستهلك البطارية بشكل حقيقي وترتيبها من الأعلى استهلاكاً، يرجى تفعيل صلاحية مراقبة الاستخدام لتطبيق Ampere Pulse.",
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
                                        text = "منح الصلاحية الآن",
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
                                text = "لا توجد تطبيقات نشطة مسجلة خلال آخر 24 ساعة حتى الآن.",
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
                                AppConsumptionRowItem(
                                    app = app,
                                    maxPercent = maxPercent,
                                    accentColor = accentColor,
                                    insetBg = insetBg,
                                    textColor = textColor,
                                    subTextColor = subTextColor,
                                    isDarkMode = isDarkMode,
                                    onClick = {
                                        CyberHapticsEngine.triggerTactileTick(context)
                                        AppBatteryUsageTracker.openAppBatteryDetailsSettings(context, app.packageName)
                                    }
                                )
                            }

                            if (snapshot.apps.size > 5) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(insetBg.copy(alpha = 0.72f))
                                        .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                        .clickable {
                                            CyberHapticsEngine.triggerTactileTick(context)
                                            showAllApps = !showAllApps
                                        }
                                        .testTag("toggle_all_app_usage_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (showAllApps) "عرض أعلى 5 تطبيقات فقط" else "عرض جميع التطبيقات (${snapshot.apps.size})",
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
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val appIconBitmap: ImageBitmap? = remember(app.packageName) {
        runCatching {
            val drawable = context.packageManager.getApplicationIcon(app.packageName)
            drawable.toBitmap(width = 72, height = 72, config = Bitmap.Config.ARGB_8888).asImageBitmap()
        }.getOrNull()
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
            .clip(RoundedCornerShape(15.dp))
            .background(insetBg.copy(alpha = if (isDarkMode) 0.58f else 0.68f))
            .border(
                width = if (app.rank == 1) 1.1.dp else 0.7.dp,
                color = if (app.rank == 1) rankColor.copy(alpha = 0.55f) else rankColor.copy(alpha = 0.20f),
                shape = RoundedCornerShape(15.dp)
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
                        .background(rankColor.copy(alpha = if (isDarkMode) 0.24f else 0.14f))
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
                    if (appIconBitmap != null) {
                        Image(
                            bitmap = appIconBitmap,
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
                                    text = "استهلاك عالٍ",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.5.sp,
                                    color = TactileCoralRed
                                )
                            }
                        }
                    }
                    Text(
                        text = "نشط ${app.formattedDurationAr()} • ≈ ${app.estimatedMah} mAh",
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
                .height(5.dp)
                .clip(CircleShape)
                .background(if (isDarkMode) Color(0x33475569) else Color(0x4D94A3B8))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(relativeFill)
                    .height(5.dp)
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
