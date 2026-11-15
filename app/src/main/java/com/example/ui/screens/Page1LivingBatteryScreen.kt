package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.graphics.drawable.toBitmap
import com.example.audio.CyberHapticsEngine
import com.example.data.BatteryTelemetry
import com.example.data.GlassThemeConfig
import com.example.data.LivingBatteryState
import com.example.data.PrizeRoadData
import com.example.data.TelemetryPoint
import com.example.data.telemetry.AppBatteryConsumption
import com.example.data.telemetry.AppBatteryUsageTracker
import com.example.data.telemetry.AppUsageSnapshot
import com.example.ui.components.LiquidBatteryRing
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.PulsingNeonDot
import com.example.ui.components.thermalLiquidGlassBackground
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
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

/**
 * PAGE 1: Living Dynamic Battery
 * Auto-triggered via real-time BatteryManager sensors data across 4 states:
 * 1. ENERGY STORING (Green #00C853): Charging (Current > 0) + Temp 20-37°C + stable V.
 * 2. OVERDRIVE / HEATING (Warning Red #FF3D00): Temp > 38°C OR high internal resistance/voltage drop.
 * 3. CRYOGENIC COOLING (Ice Blue #00B0FF): Rapid thermal drop (<35°C) OR Cryo skill deployed.
 * 4. DEEP SLEEP / HIBERNATION (Night Purple #7C4DFF): Screen OFF + Low idle draw (<=300mA) OR Overnight 80-85% trickling.
 */
@Composable
fun Page1LivingBatteryScreen(
    telemetry: BatteryTelemetry,
    voltageHistory: List<TelemetryPoint>,
    currentHistory: List<TelemetryPoint>,
    themeConfig: GlassThemeConfig,
    prizeRoadData: PrizeRoadData,
    isAutoSensorMode: Boolean = true,
    appUsageSnapshot: AppUsageSnapshot = AppUsageSnapshot(),
    onRefreshAppUsage: () -> Unit = {},
    onSelectLivingState: (LivingBatteryState?) -> Unit,
    onNavigateToPrizeRoad: () -> Unit = {},
    onToggleThermalTest: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val isDark = themeConfig.isDarkMode
    val state = telemetry.livingState
    val primaryColor = state.neonPrimary
    val glow = themeConfig.glowIntensity
    val resolvedTheme = LocalAmpereTheme.current
    val textColor = if (resolvedTheme.themeId != null) resolvedTheme.textPrimary else if (isDark) TextWhite else TextCharcoal

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 20.dp)
            .graphicsLayer { clip = false }
    ) {
        // 1. Compact & Sleek Living State Sensor Banner (Reduced container height, no XP text)
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
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Compact Icon & Real-Time Sensor State
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
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
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(16.dp)
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
                            Text(
                                text = when (state) {
                                    LivingBatteryState.OVERDRIVE_HEATING -> "Temp ${telemetry.temperatureC}°C > 38°C • Thermal Stress"
                                    LivingBatteryState.COOLING -> "Rapid Core Dissipation • <35°C Normalizing"
                                    LivingBatteryState.ENERGY_STORING -> "Charging Flow • ${telemetry.temperatureC}°C (20-37°C) • Stable ${telemetry.voltageMv}mV"
                                    LivingBatteryState.DEEP_SLEEP -> "Screen Idle • Sub-Milliamp Draw (${telemetry.currentMa}mA)"
                                },
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 10.sp,
                                color = TextSlate,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Right: Real-time Auto Sensors Indicator
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(TactileGreen.copy(alpha = if (isDark) 0.22f else 0.14f))
                            .border(1.dp, TactileGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = null,
                                tint = TactileGreen,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "100% AUTO SENSORS",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                letterSpacing = 0.3.sp,
                                color = TactileGreen
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 2. Center Prominent Circular Liquid Progress Ring (Zero XP labels)
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

        Spacer(modifier = Modifier.height(8.dp))

        Spacer(modifier = Modifier.height(10.dp))

        // Overdrive Warning Card (if heating condition is active)
        if (state == LivingBatteryState.OVERDRIVE_HEATING) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFFFF3D00),
                    isDarkMode = isDark,
                    glowIntensity = glow
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Thermal Warning",
                                tint = Color(0xFFFF3D00),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "THERMAL STRESS: ELEVATED CORE TEMPERATURE (>38°C)",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 0.4.sp,
                                color = Color(0xFFFF3D00)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Battery cell thermistor is reading ${telemetry.temperatureC}°C. Unplug the charger or deploy Tech Crew Cryo Coolant (Page 4) to restore electrochemical stability.",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.sp,
                            color = TextSlate,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Live Vital Metrics Row - Harmonized with uniform height and styling
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickMetricNeumorph(
                modifier = Modifier.weight(1f),
                label = "VOLTAGE",
                value = "${telemetry.voltageMv} mV",
                color = primaryColor,
                isDarkMode = isDark
            )
            QuickMetricNeumorph(
                modifier = Modifier.weight(1f),
                label = "CURRENT FLOW",
                value = "${if (telemetry.currentMa >= 0) "+" else ""}${telemetry.currentMa} mA",
                color = primaryColor,
                isDarkMode = isDark
            )
            QuickMetricNeumorph(
                modifier = Modifier.weight(1f),
                label = "CORE TEMP",
                value = "${telemetry.temperatureC} °C",
                color = when {
                    telemetry.temperatureC >= 40f -> Color(0xFFFF3D00)
                    telemetry.temperatureC >= 38f -> Color(0xFFFF9100)
                    telemetry.temperatureC <= 34f -> Color(0xFF00B0FF)
                    else -> primaryColor
                },
                isDarkMode = isDark,
                onClick = onToggleThermalTest,
                subtext = null
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // App Battery Consumption Ranking Section (Sorted Highest Consumer First)
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
                // Header
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

                    // Manual Refresh Action (>= 48dp touch target)
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
                        // Permission Request Card for PACKAGE_USAGE_STATS
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
                // Rank Pill (#1 Highest)
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

                // Real App Icon
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

            // Drain Percentage
            Text(
                text = "${String.format(Locale.US, "%.1f", app.consumptionPercent)}%",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = rankColor
            )
        }

        // Consumption Bar
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

/**
 * Harmonized Prize Road Level Card with Dynamic Fluid Fill Effects
 * Coordinates container proportions with font sizes and adds liquid fullness animation.
 */
@Composable
fun PrizeRoadLevelProgressCard(
    prizeRoadData: PrizeRoadData,
    accentColor: Color,
    isDarkMode: Boolean,
    glowIntensity: Float,
    onClick: () -> Unit
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    // Each level represents 10,000 mAh
    val currentLevelMah = (prizeRoadData.cumulativeMah % 10_000L).toFloat()
    val levelProgress = (currentLevelMah / 10_000f).coerceIn(0.04f, 1f)
    val nextMilestone = ((prizeRoadData.currentLevel / 5) + 1) * 5
    val levelsLeft = nextMilestone - prizeRoadData.currentLevel

    val infiniteTransition = rememberInfiniteTransition(label = "fillWaves")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fillWavePhase"
    )

    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fillShimmer"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    val textColor = if (isDarkMode) TextWhite else TextCharcoal

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        accentColor = accentColor,
        isDarkMode = isDarkMode,
        shape = RoundedCornerShape(20.dp),
        glowIntensity = glowIntensity,
        onClick = onClick,
        testTag = "prize_road_level_card"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Header Row: Harmonized Title & Level Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = if (isDarkMode) 0.22f else 0.12f))
                            .border(1.2.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MilitaryTech,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "PRIZE ROAD PROGRESSION",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp,
                            color = TextSlate
                        )
                        Text(
                            text = "Rank: ${prizeRoadData.activeTitle}",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = (-0.2).sp,
                            color = textColor
                        )
                    }
                }

                // Extruded Level Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDarkMode) Color(0xFF0F172A) else Color(0xFFD2DBE6))
                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "LVL ${prizeRoadData.currentLevel}",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            lineHeight = 15.sp,
                            color = accentColor
                        )
                        Text(
                            text = "OF 1,000",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 8.sp,
                            lineHeight = 9.sp,
                            color = TextSlate
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dynamic Fill Level Fluid Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDarkMode) Color(0xFF28364B) else Color(0xFFCED8E5))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val fillWidth = w * levelProgress

                    // 1. Fluid Underglow
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = if (isDarkMode) 0.5f else 0.35f),
                                accentColor.copy(alpha = 0.85f * pulseGlow)
                            ),
                            startX = 0f,
                            endX = fillWidth
                        ),
                        topLeft = Offset(0f, 0f),
                        size = Size(fillWidth, h),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )

                    // 2. Sinusoidal Fluid Wave Surface inside the filled portion
                    val wavePath = Path()
                    wavePath.moveTo(0f, h)
                    val segments = 20
                    for (i in 0..segments) {
                        val x = (fillWidth * i) / segments
                        val normalized = (i.toFloat() / segments) * 2 * PI
                        val waveAmp = 2.5.dp.toPx()
                        val y = (h * 0.45f) + (sin(normalized + wavePhase) * waveAmp).toFloat()
                        wavePath.lineTo(x, y)
                    }
                    wavePath.lineTo(fillWidth, h)
                    wavePath.close()

                    drawPath(
                        path = wavePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDarkMode) 0.28f else 0.4f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = h
                        )
                    )

                    // 3. Shimmer Traveling Light Streak across fullness
                    val shimmerX = w * shimmerOffset
                    if (shimmerX in 0f..fillWidth + 40.dp.toPx()) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.45f),
                                    Color.Transparent
                                ),
                                startX = shimmerX - 30.dp.toPx(),
                                endX = shimmerX + 30.dp.toPx()
                            ),
                            topLeft = Offset(0f, 0f),
                            size = Size(fillWidth, h)
                        )
                    }

                    // 4. Leading Edge Glow Pulse Dot
                    if (fillWidth > 6.dp.toPx()) {
                        drawCircle(
                            color = accentColor.copy(alpha = 0.4f * pulseGlow),
                            radius = 6.dp.toPx(),
                            center = Offset(fillWidth, h / 2f)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.5.dp.toPx(),
                            center = Offset(fillWidth, h / 2f)
                        )
                    }

                    // Subtle inner border for tactile recessed slot
                    drawRoundRect(
                        color = if (isDarkMode) Color(0x33475569) else Color(0x99B4C2D3),
                        size = size,
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metrics and Next Milestone Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Exact Level Fullness
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${numberFormat.format(currentLevelMah.toLong())} / 10,000 mAh",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentColor.copy(alpha = if (isDarkMode) 0.22f else 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${(levelProgress * 100).toInt()}% FULL",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = accentColor
                        )
                    }
                }

                // Next Milestone Navigation Teaser
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onClick() }
                ) {
                    Text(
                        text = "Next: Tier #$nextMilestone ($levelsLeft lvls)",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Go to Prize Road",
                        tint = accentColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickMetricNeumorph(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color,
    isDarkMode: Boolean,
    onClick: (() -> Unit)? = null,
    subtext: String? = null
) {
    LiquidGlassCard(
        modifier = modifier,
        accentColor = color,
        isDarkMode = isDarkMode,
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
            Text(
                text = label,
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 9.sp,
                color = TextSlate
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (isDarkMode) TextWhite else TextCharcoal
            )
            if (subtext != null) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtext,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 7.5.sp,
                    color = color,
                    maxLines = 1
                )
            }
        }
    }
}
