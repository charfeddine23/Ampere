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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BatteryTelemetry
import com.example.data.CableDiagnosticResult
import com.example.data.CableDnaTier
import com.example.data.ChargePlug
import com.example.data.DeviceFormFactor
import com.example.data.DeviceHardwareProfile
import com.example.data.GlassThemeConfig
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.PulsingNeonDot
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

/**
 * PAGE 4: Cable DNA & Charger Analyzer (IEC 62680 / USB-IF Rev 2.3 International Standards)
 * Continuous hardware-calibrated cable diagnostics + real device hardware profile detection.
 */
@Composable
fun Page4CableDnaScreen(
    telemetry: BatteryTelemetry,
    themeConfig: GlassThemeConfig,
    cableResult: CableDiagnosticResult,
    deviceProfile: DeviceHardwareProfile = DeviceHardwareProfile(),
    onRunDiagnosticSweep: (CableDnaTier?) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val resolvedTheme = LocalAmpereTheme.current
    val isDark = themeConfig.isDarkMode
    val isArabic = themeConfig.appLanguage.isArabic
    val glow = when {
        resolvedTheme.isSimpleMode -> 0f
        resolvedTheme.isProMode -> 0.35f
        else -> themeConfig.glowIntensity
    }
    val textColor = if (resolvedTheme.themeId != null) resolvedTheme.textPrimary else if (isDark) TextWhite else TextCharcoal
    val subTextColor = if (resolvedTheme.themeId != null) resolvedTheme.textSecondary else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val surfaceInset = if (resolvedTheme.themeId != null) resolvedTheme.surfaceInset else if (isDark) Color(0xFF151F30) else Color(0xFFD5DFEA)

    val activeResult = cableResult
    val tier = activeResult.tier
    val tierColor = if (resolvedTheme.isSimpleMode) resolvedTheme.accent else tier.badgeColor

    val targetResistance = activeResult.resistanceMilliOhms.toFloat()
    val animatedResistance by animateFloatAsState(
        targetValue = targetResistance,
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        ),
        label = "resistance500msGauge"
    )

    val targetVoltageDrop = activeResult.voltageDropMv.toFloat()
    val animatedVoltageDrop by animateFloatAsState(
        targetValue = targetVoltageDrop,
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        ),
        label = "voltageDrop500ms"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "cableDnaGlow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (tier.isDangerous) 600 else 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
            .graphicsLayer { clip = false }
    ) {
        // 1. Header Banner Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentColor = tierColor,
                isDarkMode = isDark,
                glowIntensity = glow
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(tierColor.copy(alpha = if (isDark) 0.22f else 0.12f))
                                    .border(1.2.dp, tierColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cable,
                                    contentDescription = null,
                                    tint = tierColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isArabic) "محلل الكابل والشاحن • IEC 62680" else "CABLE DNA & IEC 62680 ANALYZER",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    letterSpacing = 0.4.sp,
                                    color = textColor
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
                        PulsingNeonDot(color = tierColor, sizeDp = 7.dp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 2. Main Cable Resistance & International Standards Dial Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentColor = tierColor,
                isDarkMode = isDark,
                glowIntensity = glow
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isArabic) "مقاومة حلقة التوصيل (VBUS + GND)" else "USB-IF LOOP RESISTANCE (VBUS + GND)",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        letterSpacing = 0.9.sp,
                        color = subTextColor
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Circular Resistance Gauge (mΩ Meter calibrated to 600 mΩ IEC boundary)
                    Box(
                        modifier = Modifier.size(190.dp),
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
                                    width = strokeWidth + 2.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            )

                            if (animatedResistance > 1.5f) {
                                val currentAngle = 150f + activeSweep
                                val angleRad = Math.toRadians(currentAngle.toDouble())
                                val needleX = center.x + radius * Math.cos(angleRad).toFloat()
                                val needleY = center.y + radius * Math.sin(angleRad).toFloat()

                                drawCircle(
                                    color = tierColor.copy(alpha = 0.50f * pulseAlpha),
                                    radius = strokeWidth * 0.95f,
                                    center = Offset(needleX, needleY)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = strokeWidth * 0.55f,
                                    center = Offset(needleX, needleY)
                                )
                                drawCircle(
                                    color = tierColor,
                                    radius = strokeWidth * 0.35f,
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
                                    fontSize = 42.sp,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "mΩ",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = tierColor,
                                    modifier = Modifier.padding(bottom = 6.dp)
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
                                fontSize = 11.5.sp,
                                color = if (tier.isDangerous) TactileCoralRed else subTextColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // International Compliance & Tier Badge
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(tierColor.copy(alpha = if (isDark) 0.20f else 0.12f))
                            .border(1.2.dp, tierColor.copy(alpha = pulseAlpha), RoundedCornerShape(14.dp))
                            .padding(14.dp)
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
                                    modifier = Modifier.size(22.dp)
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
                                        fontSize = 13.5.sp,
                                        color = textColor
                                    )
                                    Text(
                                        text = if (telemetry.plugType == ChargePlug.NONE) {
                                            if (isArabic) {
                                                "وصّل الكابل للقياس التلقائي وفق IEC 62680"
                                            } else {
                                                "Connect cable for automatic IEC 62680 diagnostics"
                                            }
                                        } else {
                                            "${tier.localizedGrade(isArabic)} • ${activeResult.estimatedAwg}"
                                        },
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.5.sp,
                                        color = tierColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

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
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = activeResult.tailoredDeviceNote.ifBlank {
                            if (isArabic) {
                                "المستشعرات في وضع المراقبة المستمرة وفق معايير USB-IF Rev 2.3 و IEC 62680 (الحد الأقصى لهبوط الجهد ≤ 750 mV)."
                            } else {
                                "Sensors continuously monitor per USB-IF Rev 2.3 & IEC 62680 (maximum loop voltage drop ≤ 750 mV)."
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

        Spacer(modifier = Modifier.height(12.dp))

        // 3. International Standards Metrics Grid (Adaptive Layout by Container Width & Device Class)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            val useExpandedColumns = maxWidth >= 560.dp || deviceProfile.formFactor == DeviceFormFactor.TABLET
            if (useExpandedColumns) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CableMetricCard(
                            modifier = Modifier.weight(1f),
                            title = if (isArabic) "سُمك الموصل (AWG)" else "WIRE GAUGE (AWG)",
                            value = activeResult.estimatedAwg,
                            subtext = if (isArabic) "مواصفة الموصل IEC 62680" else "IEC 62680 Conductor Spec",
                            accentColor = TactileCyan,
                            isDark = isDark
                        )
                        CableMetricCard(
                            modifier = Modifier.weight(1f),
                            title = if (isArabic) "كفاءة الخط" else "LINE EFFICIENCY",
                            value = "${String.format(Locale.US, "%.1f", activeResult.lineEfficiencyPercent)}%",
                            subtext = if (isArabic) "فقد حراري: ${activeResult.thermalDissipationMilliWatts} mW" else "I²R Loss: ${activeResult.thermalDissipationMilliWatts} mW",
                            accentColor = TactileGreen,
                            isDark = isDark
                        )
                        CableMetricCard(
                            modifier = Modifier.weight(1f),
                            title = if (isArabic) "هبوط جهد USB-IF" else "USB-IF LOOP DROP",
                            value = "-${animatedVoltageDrop.toInt()} mV",
                            subtext = if (isArabic) "الحد ≤ 750 mV @ 3A-5A" else "Limit ≤ 750 mV @ 3A-5A",
                            accentColor = if (animatedVoltageDrop > 750f) TactileCoralRed else TactileRoyalBlue,
                            isDark = isDark
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CableMetricCard(
                            modifier = Modifier.weight(1f),
                            title = if (isArabic) "مرحلة الشحن" else "CHARGING PHASE",
                            value = activeResult.chargingPhase,
                            subtext = activeResult.cellTopology,
                            accentColor = TactileAmber,
                            isDark = isDark
                        )
                        CableMetricCard(
                            modifier = Modifier.weight(1f),
                            title = if (isArabic) "بروتوكول التفاوض" else "PROTOCOL MATRIX",
                            value = activeResult.usbProtocol,
                            subtext = if (isArabic) "استقرار التوصيل ${activeResult.pinIntegrityPercent}%" else "Handshake & Stability ${activeResult.pinIntegrityPercent}%",
                            accentColor = TactileCyan,
                            isDark = isDark
                        )
                        CableMetricCard(
                            modifier = Modifier.weight(1f),
                            title = if (isArabic) "درجة جودة الكابل" else "CABLE SCORE",
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
                        CableMetricCard(
                            modifier = Modifier.weight(1f),
                            title = if (isArabic) "سُمك الموصل (AWG)" else "WIRE GAUGE (AWG)",
                            value = activeResult.estimatedAwg,
                            subtext = if (isArabic) "معيار المقطع العالمي" else "IEC 62680 Conductor",
                            accentColor = TactileCyan,
                            isDark = isDark
                        )
                        CableMetricCard(
                            modifier = Modifier.weight(1f),
                            title = if (isArabic) "كفاءة الخط" else "LINE EFFICIENCY",
                            value = "${String.format(Locale.US, "%.1f", activeResult.lineEfficiencyPercent)}%",
                            subtext = if (isArabic) "فقد حراري ${activeResult.thermalDissipationMilliWatts} mW" else "Loss ${activeResult.thermalDissipationMilliWatts} mW",
                            accentColor = TactileGreen,
                            isDark = isDark
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CableMetricCard(
                            modifier = Modifier.weight(1f),
                            title = if (isArabic) "هبوط جهد USB-IF" else "USB-IF LOOP DROP",
                            value = if (animatedVoltageDrop.toInt() > 0) "-${animatedVoltageDrop.toInt()} mV" else "0 mV",
                            subtext = if (isArabic) "حد USB-IF ≤ 750 mV" else "USB-IF Limit ≤ 750 mV",
                            accentColor = if (animatedVoltageDrop > 750f) TactileCoralRed else TactileRoyalBlue,
                            isDark = isDark
                        )
                        CableMetricCard(
                            modifier = Modifier.weight(1f),
                            title = if (isArabic) "البروتوكول والمرحلة" else "PROTOCOL & PHASE",
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
                        CableMetricCard(
                            modifier = Modifier.weight(1f),
                            title = if (isArabic) "سلامة الأقطاب" else "PIN INTEGRITY",
                            value = "${activeResult.pinIntegrityPercent}%",
                            subtext = activeResult.cellTopology,
                            accentColor = TactileRoyalBlue,
                            isDark = isDark
                        )
                        CableMetricCard(
                            modifier = Modifier.weight(1f),
                            title = if (isArabic) "تقييم الكابل" else "CABLE SCORE",
                            value = if (telemetry.plugType == ChargePlug.NONE) "-- / 100" else "${activeResult.qualityScore} / 100",
                            subtext = activeResult.standardCompliance,
                            accentColor = tierColor,
                            isDark = isDark
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Detected Device Hardware & Adaptive Spec Card (Formatted by Device Type)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentColor = TactileRoyalBlue,
                isDarkMode = isDark,
                glowIntensity = glow,
                testTag = "device_hardware_profile_card"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(TactileRoyalBlue.copy(alpha = if (isDark) 0.22f else 0.12f))
                                    .border(1.dp, TactileRoyalBlue.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Devices,
                                    contentDescription = null,
                                    tint = TactileRoyalBlue,
                                    modifier = Modifier.size(19.dp)
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

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(surfaceInset)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
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

                    // Device Specs Structured Matrix (adapts to Phone vs Tablet/Foldable width)
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
                                                .background(surfaceInset.copy(alpha = if (isDark) 0.55f else 0.72f))
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

                    // Recommended Cable Spec Banner Tailored to Device OEM
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

@Composable
private fun CableMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtext: String,
    accentColor: Color,
    isDark: Boolean
) {
    val resolvedTheme = LocalAmpereTheme.current
    val bgColor = if (resolvedTheme.themeId != null) resolvedTheme.surface else if (isDark) Color(0xFF1E293B) else Color(0xFFE7ECF3)
    val borderColor = if (resolvedTheme.themeId != null) resolvedTheme.border else if (isDark) Color(0xFF334155) else Color(0x99BECAD8)
    val subColor = if (resolvedTheme.themeId != null) resolvedTheme.textSecondary else if (isDark) Color(0xFF94A3B8) else TextSlate

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = title,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 9.5.sp,
                letterSpacing = 0.4.sp,
                color = subColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 9.5.sp,
                color = subColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
