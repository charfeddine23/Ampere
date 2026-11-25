package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.CyberHapticsEngine
import com.example.data.BatteryTelemetry
import com.example.data.CableDiagnosticResult
import com.example.data.CableDnaTier
import com.example.data.ChargePlug
import com.example.data.DeviceFormFactor
import com.example.data.DeviceHardwareProfile
import com.example.data.GlassThemeConfig
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.VoltMetricTile
import com.example.ui.components.VoltSectionHeader
import com.example.ui.theme.LocalAmpereTheme
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.TactileAmber
import com.example.ui.theme.TactileCoralRed
import com.example.ui.theme.TactileCyan
import com.example.ui.theme.TactileGreen
import com.example.ui.theme.TactileRoyalBlue
import com.example.ui.theme.TextCharcoal
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * PAGE 4: Cable DNA & Charger Analyzer (IEC 62680 / USB-IF Rev 2.3)
 * Unified single Hero Cable Gauge card + one-tap Scan button + VoltMetricTile grid
 * + Device Hardware Profile specification matrix.
 */
@Composable
fun Page4CableDnaScreen(
    telemetry: BatteryTelemetry,
    themeConfig: GlassThemeConfig,
    cableResult: CableDiagnosticResult,
    deviceProfile: DeviceHardwareProfile = DeviceHardwareProfile(),
    onRunDiagnosticSweep: (CableDnaTier?) -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val resolvedTheme = LocalAmpereTheme.current
    val customTokens = resolvedTheme.themeId != null || resolvedTheme.isSimpleMode || resolvedTheme.isProMode

    val isDark = themeConfig.isDarkMode
    val isArabic = themeConfig.appLanguage.isArabic
    val glow = when {
        resolvedTheme.isSimpleMode -> 0f
        resolvedTheme.isProMode -> 0.35f
        else -> themeConfig.glowIntensity
    }
    val textColor = if (customTokens) resolvedTheme.textPrimary else if (isDark) TextWhite else TextCharcoal
    val subTextColor = if (customTokens) resolvedTheme.textSecondary else if (isDark) Color(0xFF94A3B8) else TextSlate
    val surfaceInset = if (customTokens) resolvedTheme.surfaceInset else if (isDark) Color(0xFF151F30) else Color(0xFFDCE4EE)

    val activeResult = cableResult
    val tier = activeResult.tier
    val tierColor = if (resolvedTheme.isSimpleMode) resolvedTheme.accent else tier.badgeColor

    val targetResistance = activeResult.resistanceMilliOhms.toFloat()
    val animatedResistance by animateFloatAsState(
        targetValue = targetResistance,
        animationSpec = tween(
            durationMillis = if (resolvedTheme.isSimpleMode) 180 else 420,
            easing = FastOutSlowInEasing
        ),
        label = "resistanceGauge"
    )

    val targetVoltageDrop = activeResult.voltageDropMv.toFloat()
    val animatedVoltageDrop by animateFloatAsState(
        targetValue = targetVoltageDrop,
        animationSpec = tween(
            durationMillis = if (resolvedTheme.isSimpleMode) 180 else 420,
            easing = FastOutSlowInEasing
        ),
        label = "voltageDrop"
    )

    val pulseAlpha: Float = if (resolvedTheme.isSimpleMode) {
        0.75f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "cableDnaGlow")
        val animAlpha by infiniteTransition.animateFloat(
            initialValue = 0.45f,
            targetValue = 0.90f,
            animationSpec = infiniteRepeatable(
                animation = tween(if (tier.isDangerous) 750 else 1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
        animAlpha
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
            .graphicsLayer { clip = false }
    ) {
        // 1. Unified Hero Cable Resistance & Verdict Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentColor = tierColor,
                isDarkMode = isDark,
                shape = RoundedCornerShape(22.dp),
                glowIntensity = glow
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header inside Hero Card: Cable icon + Title + One-tap Scan action
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
                                    .clip(CircleShape)
                                    .background(tierColor.copy(alpha = if (isDark) 0.18f else 0.12f))
                                    .border(1.dp, tierColor.copy(alpha = 0.45f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cable,
                                    contentDescription = null,
                                    tint = tierColor,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isArabic) "فحص الكابل والشاحن" else "Cable & Charger DNA",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${telemetry.plugType.localizedLabel(isArabic)} • ${activeResult.standardCompliance}",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 10.5.sp,
                                    color = subTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(tierColor.copy(alpha = if (isDark) 0.18f else 0.12f))
                                .border(1.dp, tierColor.copy(alpha = 0.45f), RoundedCornerShape(11.dp))
                                .clickable {
                                    CyberHapticsEngine.triggerTactileTick(context)
                                    onRunDiagnosticSweep(null)
                                }
                                .padding(horizontal = 11.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Radar,
                                    contentDescription = if (isArabic) "فحص الكابل الآن" else "Scan Cable",
                                    tint = tierColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isArabic) "فحص" else "Scan",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = tierColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Precision Circular Loop Resistance Dial (calibrated to 600 mΩ IEC boundary)
                    Box(
                        modifier = Modifier.size(182.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val strokeWidth = 12.dp.toPx()
                            val center = Offset(w / 2f, size.height / 2f)
                            val radius = (w - strokeWidth) / 2f

                            drawArc(
                                color = if (isDark) Color(0xFF1E293B) else Color(0xFFCFD9E6),
                                startAngle = 150f,
                                sweepAngle = 240f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )

                            val resistanceFraction = (animatedResistance / 600f).coerceIn(0f, 1f)
                            val activeSweep = 240f * resistanceFraction

                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        TactileGreen,
                                        TactileCyan,
                                        TactileAmber,
                                        TactileCoralRed
                                    ),
                                    center = center
                                ),
                                startAngle = 150f,
                                sweepAngle = activeSweep,
                                useCenter = false,
                                style = Stroke(
                                    width = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            )

                            if (animatedResistance > 1.5f) {
                                val currentAngle = 150f + activeSweep
                                val angleRad = Math.toRadians(currentAngle.toDouble())
                                val needleX = center.x + radius * cos(angleRad).toFloat()
                                val needleY = center.y + radius * sin(angleRad).toFloat()

                                drawCircle(
                                    color = tierColor.copy(alpha = 0.42f * pulseAlpha),
                                    radius = strokeWidth * 0.80f,
                                    center = Offset(needleX, needleY)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = strokeWidth * 0.45f,
                                    center = Offset(needleX, needleY)
                                )
                                drawCircle(
                                    color = tierColor,
                                    radius = strokeWidth * 0.25f,
                                    center = Offset(needleX, needleY)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${animatedResistance.toInt()}",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 40.sp,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "mΩ",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = tierColor,
                                    modifier = Modifier.padding(bottom = 5.dp)
                                )
                            }
                            Text(
                                text = if (isArabic) {
                                    "هبوط الجهد: -${animatedVoltageDrop.toInt()} mV"
                                } else {
                                    "ΔV Loop: -${animatedVoltageDrop.toInt()} mV"
                                },
                                fontFamily = PlusJakartaSansFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = if (tier.isDangerous) TactileCoralRed else subTextColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Crisp Verdict Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(tierColor.copy(alpha = if (isDark) 0.16f else 0.10f))
                            .border(1.dp, tierColor.copy(alpha = pulseAlpha * 0.65f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 13.dp, vertical = 11.dp)
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
                                Icon(
                                    imageVector = if (tier.isDangerous) Icons.Default.Warning else Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = tierColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (telemetry.plugType == ChargePlug.NONE) {
                                            if (isArabic) "شاحن غير موصول (0 mΩ)" else "Charger Unplugged (0 mΩ)"
                                        } else {
                                            tier.localizedTitle(isArabic)
                                        },
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = textColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (telemetry.plugType == ChargePlug.NONE) {
                                            if (isArabic) {
                                                "وصّل الكابل للقياس التلقائي وفق IEC 62680"
                                            } else {
                                                "Connect cable for automatic IEC 62680 check"
                                            }
                                        } else {
                                            "${tier.localizedGrade(isArabic)} • ${activeResult.estimatedAwg}"
                                        },
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.5.sp,
                                        color = tierColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(tierColor)
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = if (telemetry.plugType == ChargePlug.NONE) {
                                        if (isArabic) "استعداد" else "STANDBY"
                                    } else {
                                        "${activeResult.qualityScore}/100"
                                    },
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = activeResult.tailoredDeviceNote.ifBlank {
                            if (isArabic) {
                                "المراقبة المستمرة نشطة وفق معايير USB-IF Rev 2.3 و IEC 62680 (الحد الآمن ≤ 750 mV)."
                            } else {
                                "Continuous monitoring per USB-IF Rev 2.3 & IEC 62680 (safe drop limit ≤ 750 mV)."
                            }
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

        // 2. Unified Metrics Grid (Adaptive 2-column or 3-column via BoxWithConstraints)
        VoltSectionHeader(
            title = if (isArabic) "مؤشرات الموصل والشحن" else "Conductor & Delivery Metrics",
            isDark = isDark
        )
        Spacer(modifier = Modifier.height(8.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            val useExpandedColumns = maxWidth >= 560.dp || deviceProfile.formFactor == DeviceFormFactor.TABLET
            if (useExpandedColumns) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        VoltMetricTile(
                            modifier = Modifier.weight(1f),
                            title = if (isArabic) "سُمك الموصل (AWG)" else "Wire Gauge (AWG)",
                            value = activeResult.estimatedAwg,
                            subtext = if (isArabic) "مواصفة IEC 62680" else "IEC 62680 Conductor",
                            accentColor = TactileCyan,
                            isDark = isDark
                        )
                        VoltMetricTile(
                            modifier = Modifier.weight(1f),
                            title = if (isArabic) "كفاءة الخط" else "Line Efficiency",
                            value = "${String.format(Locale.US, "%.1f", activeResult.lineEfficiencyPercent)}%",
                            subtext = if (isArabic) "فقد ${activeResult.thermalDissipationMilliWatts} mW" else "I²R Loss ${activeResult.thermalDissipationMilliWatts} mW",
                            accentColor = TactileGreen,
                            isDark = isDark
                        )
                        VoltMetricTile(
                            modifier = Modifier.weight(1f),
                            title = if (isArabic) "هبوط جهد USB-IF" else "USB-IF Loop Drop",
                            value = if (animatedVoltageDrop.toInt() > 0) "-${animatedVoltageDrop.toInt()} mV" else "0 mV",
                            subtext = if (isArabic) "الحد ≤ 750 mV @ 3A-5A" else "Limit ≤ 750 mV @ 3A-5A",
                            accentColor = if (animatedVoltageDrop > 750f) TactileCoralRed else TactileRoyalBlue,
                            isDark = isDark
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        VoltMetricTile(
                            modifier = Modifier.weight(1f),
                            title = if (isArabic) "مرحلة الشحن" else "Charging Phase",
                            value = activeResult.chargingPhase,
                            subtext = activeResult.cellTopology,
                            accentColor = TactileAmber,
                            isDark = isDark
                        )
                        VoltMetricTile(
                            modifier = Modifier.weight(1f),
                            title = if (isArabic) "بروتوكول التفاوض" else "Protocol Matrix",
                            value = activeResult.usbProtocol,
                            subtext = if (isArabic) "استقرار ${activeResult.pinIntegrityPercent}%" else "Stability ${activeResult.pinIntegrityPercent}%",
                            accentColor = TactileCyan,
                            isDark = isDark
                        )
                        VoltMetricTile(
                            modifier = Modifier.weight(1f),
                            title = if (isArabic) "تقييم الكابل" else "Cable Score",
                            value = if (telemetry.plugType == ChargePlug.NONE) "-- / 100" else "${activeResult.qualityScore} / 100",
                            subtext = activeResult.standardCompliance,
                            accentColor = tierColor,
                            isDark = isDark
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VoltMetricTile(
                            modifier = Modifier.weight(1f),
                            title = if (isArabic) "سُمك الموصل (AWG)" else "Wire Gauge (AWG)",
                            value = activeResult.estimatedAwg,
                            subtext = if (isArabic) "مواصفة IEC 62680" else "IEC 62680 Spec",
                            accentColor = TactileCyan,
                            isDark = isDark
                        )
                        VoltMetricTile(
                            modifier = Modifier.weight(1f),
                            title = if (isArabic) "كفاءة الخط" else "Line Efficiency",
                            value = "${String.format(Locale.US, "%.1f", activeResult.lineEfficiencyPercent)}%",
                            subtext = if (isArabic) "فقد ${activeResult.thermalDissipationMilliWatts} mW" else "Loss ${activeResult.thermalDissipationMilliWatts} mW",
                            accentColor = TactileGreen,
                            isDark = isDark
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VoltMetricTile(
                            modifier = Modifier.weight(1f),
                            title = if (isArabic) "هبوط جهد USB-IF" else "USB-IF Loop Drop",
                            value = if (animatedVoltageDrop.toInt() > 0) "-${animatedVoltageDrop.toInt()} mV" else "0 mV",
                            subtext = if (isArabic) "حد ≤ 750 mV" else "Limit ≤ 750 mV",
                            accentColor = if (animatedVoltageDrop > 750f) TactileCoralRed else TactileRoyalBlue,
                            isDark = isDark
                        )
                        VoltMetricTile(
                            modifier = Modifier.weight(1f),
                            title = if (isArabic) "البروتوكول والمرحلة" else "Protocol & Phase",
                            value = activeResult.usbProtocol,
                            subtext = activeResult.chargingPhase,
                            accentColor = TactileAmber,
                            isDark = isDark
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VoltMetricTile(
                            modifier = Modifier.weight(1f),
                            title = if (isArabic) "سلامة الأقطاب" else "Pin Integrity",
                            value = "${activeResult.pinIntegrityPercent}%",
                            subtext = activeResult.cellTopology,
                            accentColor = TactileRoyalBlue,
                            isDark = isDark
                        )
                        VoltMetricTile(
                            modifier = Modifier.weight(1f),
                            title = if (isArabic) "تقييم الكابل" else "Cable Score",
                            value = if (telemetry.plugType == ChargePlug.NONE) "-- / 100" else "${activeResult.qualityScore} / 100",
                            subtext = activeResult.standardCompliance,
                            accentColor = tierColor,
                            isDark = isDark
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Device Hardware Profile Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentColor = TactileRoyalBlue,
                isDarkMode = isDark,
                shape = RoundedCornerShape(20.dp),
                glowIntensity = glow,
                testTag = "device_hardware_profile_card"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                    .background(TactileRoyalBlue.copy(alpha = if (isDark) 0.18f else 0.12f))
                                    .border(1.dp, TactileRoyalBlue.copy(alpha = 0.40f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Devices,
                                    contentDescription = null,
                                    tint = TactileRoyalBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isArabic) "ملف العتاد المخصص لجهازك" else "Device Hardware Profile",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = textColor
                                )
                                Text(
                                    text = "${deviceProfile.brand} ${deviceProfile.model} • ${deviceProfile.formFactor.localizedLabel(isArabic)}",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = TactileRoyalBlue,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(surfaceInset)
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (isArabic) "هدف ${deviceProfile.recommendedPowerWatts}W" else "${deviceProfile.recommendedPowerWatts}W Target",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = textColor
                            )
                        }
                    }

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val wideDevice = maxWidth >= 520.dp || deviceProfile.formFactor != DeviceFormFactor.PHONE
                        val specs = if (isArabic) {
                            listOf(
                                "المعالج (SoC)" to deviceProfile.chipset,
                                "النظام والمعمارية" to "${deviceProfile.androidVersion} • ${deviceProfile.primaryAbi}",
                                "الذاكرة والأنوية" to "${String.format(Locale.US, "%.1f", deviceProfile.totalRamGb)} GB RAM • ${deviceProfile.cpuCores} أنوية",
                                "منظومة الشحن" to deviceProfile.localizedEcosystem(true),
                                "السعة والخلية" to "${deviceProfile.nominalDesignCapacityMah} mAh • ${activeResult.cellTopology}",
                                "الحد الحراري الآمن" to "≤ ${deviceProfile.safeThermalCeilingC}°C (معيار المصنّع)"
                            )
                        } else {
                            listOf(
                                "Processor (SoC)" to deviceProfile.chipset,
                                "OS & Architecture" to "${deviceProfile.androidVersion} • ${deviceProfile.primaryAbi}",
                                "Memory & Cores" to "${String.format(Locale.US, "%.1f", deviceProfile.totalRamGb)} GB RAM • ${deviceProfile.cpuCores} cores",
                                "Charging Ecosystem" to deviceProfile.localizedEcosystem(false),
                                "Design Capacity" to "${deviceProfile.nominalDesignCapacityMah} mAh • ${activeResult.cellTopology}",
                                "Safe Thermal Limit" to "≤ ${deviceProfile.safeThermalCeilingC}°C (OEM Spec)"
                            )
                        }
                        val chunkCount = if (wideDevice) 3 else 2
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            specs.chunked(chunkCount).forEach { rowSpecs ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowSpecs.forEach { (k, v) ->
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(surfaceInset.copy(alpha = if (isDark) 0.50f else 0.65f))
                                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = k,
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 9.5.sp,
                                                color = subTextColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = v,
                                                fontFamily = OutfitFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = textColor,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    repeat(chunkCount - rowSpecs.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(TactileGreen.copy(alpha = if (isDark) 0.14f else 0.10f))
                            .border(1.dp, TactileGreen.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricMeter,
                            contentDescription = null,
                            tint = TactileGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) {
                                "الكابل الموصى به لجهازك: ${deviceProfile.localizedRecommendedCable(true)}"
                            } else {
                                "Recommended Cable: ${deviceProfile.localizedRecommendedCable(false)}"
                            },
                            fontFamily = PlusJakartaSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.5.sp,
                            color = textColor,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}
