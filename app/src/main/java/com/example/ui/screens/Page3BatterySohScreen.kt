package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.CyberHapticsEngine
import com.example.data.BatterySohPrediction
import com.example.data.BatteryTelemetry
import com.example.data.GlassThemeConfig
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.VoltMetricTile
import com.example.ui.components.VoltSectionHeader
import com.example.ui.theme.LocalAmpereTheme
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PlusJakartaSansFontFamily
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
 * PAGE 3: Cell Health (SOH) & Lifespan Predictor
 * Unified single hero card + interactive Sync calibration + consistent VoltMetricTile 2x2 grid
 * + lifespan trajectory chart.
 */
@Composable
fun Page3BatterySohScreen(
    telemetry: BatteryTelemetry,
    themeConfig: GlassThemeConfig,
    sohPrediction: BatterySohPrediction,
    onRecalibrateSoh: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val isDark = themeConfig.isDarkMode
    val isArabic = themeConfig.appLanguage.isArabic
    val resolvedTheme = LocalAmpereTheme.current
    val customTokens = resolvedTheme.themeId != null || resolvedTheme.isSimpleMode || resolvedTheme.isProMode

    val glow = when {
        resolvedTheme.isSimpleMode -> 0f
        resolvedTheme.isProMode -> 0.35f
        else -> themeConfig.glowIntensity
    }
    val textColor = if (customTokens) resolvedTheme.textPrimary else if (isDark) TextWhite else TextCharcoal
    val subTextColor = if (customTokens) resolvedTheme.textSecondary else if (isDark) Color(0xFF94A3B8) else TextSlate

    // Automatically recalibrate SOH against Room WAL & hardware registers on page entry
    LaunchedEffect(Unit) {
        onRecalibrateSoh(false)
    }

    val statusColor = if (resolvedTheme.isSimpleMode) resolvedTheme.accent else sohPrediction.cellStatus.color
    val animatedHealthFraction by animateFloatAsState(
        targetValue = (sohPrediction.healthPercentage / 100f).coerceIn(0f, 1f),
        animationSpec = if (resolvedTheme.isSimpleMode) {
            tween(180)
        } else {
            spring(
                dampingRatio = 0.88f,
                stiffness = Spring.StiffnessMediumLow
            )
        },
        label = "sohGaugeFraction"
    )

    val pulseAlpha: Float = if (resolvedTheme.isSimpleMode) {
        0.75f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "sohPulse")
        val animAlpha by infiniteTransition.animateFloat(
            initialValue = 0.45f,
            targetValue = 0.88f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "sohPulseAlpha"
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
        // 1. Unified Hero SOH Gauge Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentColor = statusColor,
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
                    // Header row inside Hero Card: Icon + Title + Chemistry + Sync Action
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
                                    .background(statusColor.copy(alpha = if (isDark) 0.18f else 0.12f))
                                    .border(1.dp, statusColor.copy(alpha = 0.45f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HealthAndSafety,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isArabic) "صحة الخلية (SOH)" else "Cell Health (SOH)",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${telemetry.technology} • Li-Ion",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 10.5.sp,
                                    color = subTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Interactive Recalibrate / Sync pill with accessible touch target
                        Box(
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(statusColor.copy(alpha = if (isDark) 0.18f else 0.12f))
                                .border(1.dp, statusColor.copy(alpha = 0.45f), RoundedCornerShape(11.dp))
                                .clickable {
                                    CyberHapticsEngine.triggerTactileTick(context)
                                    onRecalibrateSoh(true)
                                }
                                .padding(horizontal = 11.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = if (isArabic) "تحديث المعايرة" else "Sync Calibration",
                                    tint = statusColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isArabic) "تحديث" else "Sync",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = statusColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Precision Circular Medical SOH Gauge
                    Box(
                        modifier = Modifier.size(186.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val strokeWidth = 13.dp.toPx()
                            val center = Offset(w / 2f, size.height / 2f)
                            val radius = (w - strokeWidth) / 2f

                            drawArc(
                                color = if (isDark) Color(0xFF1E293B) else Color(0xFFCFD9E6),
                                startAngle = 140f,
                                sweepAngle = 260f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )

                            val activeSweep = 260f * animatedHealthFraction
                            if (activeSweep > 0f) {
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        colors = listOf(
                                            TactileGreen,
                                            TactileCyan,
                                            statusColor,
                                            TactileGreen
                                        ),
                                        center = center
                                    ),
                                    startAngle = 140f,
                                    sweepAngle = activeSweep,
                                    useCenter = false,
                                    style = Stroke(
                                        width = strokeWidth,
                                        cap = StrokeCap.Round
                                    )
                                )

                                val angleRad = Math.toRadians((140f + activeSweep).toDouble())
                                val needleX = center.x + radius * cos(angleRad).toFloat()
                                val needleY = center.y + radius * sin(angleRad).toFloat()

                                drawCircle(
                                    color = Color.White,
                                    radius = 5.5.dp.toPx(),
                                    center = Offset(needleX, needleY)
                                )
                                drawCircle(
                                    color = statusColor,
                                    radius = 3.dp.toPx(),
                                    center = Offset(needleX, needleY)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format(Locale.US, "%.2f%%", sohPrediction.healthPercentage),
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 34.sp,
                                color = textColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isArabic) {
                                    "متبقي ~${sohPrediction.estimatedLifespanDays} يوماً"
                                } else {
                                    "~${sohPrediction.estimatedLifespanDays} DAYS LEFT"
                                },
                                fontFamily = PlusJakartaSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                                color = statusColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Crisp Verdict Strip
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(statusColor.copy(alpha = if (isDark) 0.16f else 0.10f))
                            .border(1.dp, statusColor.copy(alpha = pulseAlpha * 0.65f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 13.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sohPrediction.cellStatus.localizedLabel(isArabic),
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = textColor
                                )
                                Text(
                                    text = sohPrediction.cellStatus.localizedDescription(isArabic),
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 10.5.sp,
                                    color = subTextColor,
                                    lineHeight = 15.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Unified 2x2 Cell Metrics Grid
        VoltSectionHeader(
            title = if (isArabic) "مؤشرات كفاءة الخلية" else "Cell Telemetry Breakdown",
            isDark = isDark
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VoltMetricTile(
                    modifier = Modifier.weight(1f),
                    title = if (isArabic) "السعة الفعلية" else "Effective Capacity",
                    value = "${sohPrediction.estimatedCurrentCapacityMah} mAh",
                    subtext = if (isArabic) "المصنعية ${sohPrediction.originalCapacityMah} mAh" else "Design ${sohPrediction.originalCapacityMah} mAh",
                    accentColor = statusColor,
                    isDark = isDark
                )
                VoltMetricTile(
                    modifier = Modifier.weight(1f),
                    title = if (isArabic) "دورات الشحن" else "Charge Cycles",
                    value = if (isArabic) "${sohPrediction.totalCyclesCompleted} دورة" else "${sohPrediction.totalCyclesCompleted} Cycles",
                    subtext = if (isArabic) "متبقٍ ${sohPrediction.estimatedRemainingCycles} دورة إلى 80%" else "${sohPrediction.estimatedRemainingCycles} cycles to 80%",
                    accentColor = TactileRoyalBlue,
                    isDark = isDark
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VoltMetricTile(
                    modifier = Modifier.weight(1f),
                    title = if (isArabic) "المقاومة الداخلية (ESR)" else "Internal ESR",
                    value = "${sohPrediction.internalResistanceEstimateMilliOhms} mΩ",
                    subtext = if (isArabic) "المثالي < 65 mΩ" else "Optimal < 65 mΩ",
                    accentColor = TactileCyan,
                    isDark = isDark
                )
                VoltMetricTile(
                    modifier = Modifier.weight(1f),
                    title = if (isArabic) "الكفاءة الكولومبية" else "Coulombic Ratio",
                    value = String.format(Locale.US, "%.2f%%", sohPrediction.coulombicEfficiencyPercent),
                    subtext = if (isArabic) "معدل الاحتفاظ بالشحنة" else "Charge retention",
                    accentColor = TactileGreen,
                    isDark = isDark
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Lifespan Forecast & Degradation Trajectory Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentColor = TactileCyan,
                isDarkMode = isDark,
                shape = RoundedCornerShape(20.dp),
                glowIntensity = glow
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "منحنى التوقع العمراني للخلية" else "Lifespan Trajectory Forecast",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(TactileCyan.copy(alpha = if (isDark) 0.16f else 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isArabic) "متوقع: ${sohPrediction.projectedReplacementDate}" else "Est. ${sohPrediction.projectedReplacementDate}",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = TactileCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val trajectoryPath = remember { Path() }
                    val trajectoryFillPath = remember { Path() }
                    val trajectoryDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(116.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            val path = trajectoryPath.apply { reset() }
                            val fillPath = trajectoryFillPath.apply { reset() }

                            val points = listOf(
                                Offset(0f, h * 0.15f),
                                Offset(w * 0.20f, h * 0.22f),
                                Offset(w * 0.45f, h * 0.38f),
                                Offset(w * 0.70f, h * 0.58f),
                                Offset(w, h * 0.82f)
                            )

                            path.moveTo(points[0].x, points[0].y)
                            fillPath.moveTo(points[0].x, h)
                            fillPath.lineTo(points[0].x, points[0].y)

                            for (i in 1 until points.size) {
                                val prev = points[i - 1]
                                val curr = points[i]
                                val cx1 = prev.x + (curr.x - prev.x) / 2f
                                val cy1 = prev.y
                                val cx2 = prev.x + (curr.x - prev.x) / 2f
                                val cy2 = curr.y
                                path.cubicTo(cx1, cy1, cx2, cy2, curr.x, curr.y)
                                fillPath.cubicTo(cx1, cy1, cx2, cy2, curr.x, curr.y)
                            }

                            fillPath.lineTo(w, h)
                            fillPath.close()

                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        TactileCyan.copy(alpha = if (isDark) 0.22f else 0.14f),
                                        Color.Transparent
                                    ),
                                    startY = 0f,
                                    endY = h
                                )
                            )

                            drawLine(
                                color = TactileCoralRed.copy(alpha = 0.45f),
                                start = Offset(0f, h * 0.82f),
                                end = Offset(w, h * 0.82f),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = trajectoryDashEffect
                            )

                            drawPath(
                                path = path,
                                color = TactileCyan,
                                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                            )

                            val healthDropFraction = ((100f - sohPrediction.healthPercentage) / 20f).coerceIn(0f, 1f)
                            val cycleFraction = if (sohPrediction.totalCyclesCompleted > 0) {
                                (sohPrediction.totalCyclesCompleted / 1000f).coerceIn(0f, 1f)
                            } else {
                                healthDropFraction
                            }
                            val curX = (w * cycleFraction).coerceIn(0f, w)
                            val curY = run {
                                var segIdx = 0
                                for (i in 1 until points.size) {
                                    if (curX <= points[i].x || i == points.lastIndex) {
                                        segIdx = i
                                        break
                                    }
                                }
                                val p0 = points[segIdx - 1]
                                val p1 = points[segIdx]
                                val midX = p0.x + (p1.x - p0.x) / 2f
                                var lo = 0f
                                var hi = 1f
                                repeat(10) {
                                    val t = (lo + hi) * 0.5f
                                    val mt = 1f - t
                                    val bx = (mt * mt * mt) * p0.x +
                                        (3f * mt * mt * t) * midX +
                                        (3f * mt * t * t) * midX +
                                        (t * t * t) * p1.x
                                    if (bx < curX) lo = t else hi = t
                                }
                                val t = (lo + hi) * 0.5f
                                val mt = 1f - t
                                (mt * mt * mt) * p0.y +
                                    (3f * mt * mt * t) * p0.y +
                                    (3f * mt * t * t) * p1.y +
                                    (t * t * t) * p1.y
                            }

                            drawCircle(
                                color = statusColor.copy(alpha = 0.28f),
                                radius = 7.dp.toPx(),
                                center = Offset(curX, curY)
                            )
                            drawCircle(
                                color = statusColor,
                                radius = 4.dp.toPx(),
                                center = Offset(curX, curY)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 2.dp.toPx(),
                                center = Offset(curX, curY)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isArabic) "الدورة 0 (100%)" else "Cycle 0 (100%)",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 10.sp,
                            color = subTextColor
                        )
                        Text(
                            text = if (isArabic) "الدورة 1000 (حد 80%)" else "Cycle 1000 (80% EOL)",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            color = TactileCoralRed
                        )
                    }
                }
            }
        }
    }
}
