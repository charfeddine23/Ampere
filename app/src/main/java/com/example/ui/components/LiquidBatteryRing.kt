package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BatteryStatus
import com.example.data.ChargePlug
import com.example.data.LivingBatteryState
import com.example.ui.theme.LocalAmpereTheme
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.TextCharcoal
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Precision Instrument Battery Ring
 * Minimalist Swiss-inspired circular dial with crisp graduation ticks,
 * calm liquid depth, and clear digital hierarchy.
 */
@Composable
fun LiquidBatteryRing(
    percentage: Int,
    status: BatteryStatus,
    plugType: ChargePlug,
    wattageW: Float,
    livingState: LivingBatteryState,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false,
    sizeDp: Dp = 216.dp,
    glowIntensity: Float = 0.75f,
    ambientLiquidWaves: Boolean = true
) {
    val theme = LocalAmpereTheme.current
    val isSimple = theme.isSimpleMode
    val isPro = theme.isProMode

    val animatedPercent by animateFloatAsState(
        targetValue = percentage.toFloat(),
        animationSpec = if (isSimple) {
            tween(160)
        } else {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
        },
        label = "animatedPercent"
    )

    val waveDuration = when (livingState) {
        LivingBatteryState.OVERDRIVE_HEATING -> 1600
        LivingBatteryState.COOLING -> 2600
        LivingBatteryState.ENERGY_STORING -> 2200
        LivingBatteryState.DEEP_SLEEP -> 4000
    }

    val wavePhase: Float
    val auraPulse: Float
    if (isSimple || !ambientLiquidWaves) {
        wavePhase = 0f
        auraPulse = 0.72f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "liquidWaves")
        val animWave by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(waveDuration, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wavePhase"
        )
        val animAura by infiniteTransition.animateFloat(
            initialValue = 0.72f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "auraPulse"
        )
        wavePhase = animWave
        auraPulse = animAura
    }

    val targetPrimaryColor = when {
        isSimple -> theme.accent
        isPro -> lerp(livingState.neonPrimary, theme.accent, 0.55f)
        else -> livingState.neonPrimary
    }
    val targetSecondaryColor = when {
        isSimple -> theme.secondaryAccent
        isPro -> theme.secondaryAccent
        else -> livingState.neonSecondary
    }

    val activePrimary by animateColorAsState(
        targetValue = targetPrimaryColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "neonPrimary"
    )
    val activeSecondary by animateColorAsState(
        targetValue = targetSecondaryColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "neonSecondary"
    )

    val wavePath = remember { Path() }
    val clipPath = remember { Path() }

    Box(
        modifier = modifier
            .size(sizeDp)
            .testTag("circular_liquid_progress_ring")
            .graphicsLayer { clip = false },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(sizeDp)) {
            val canvasSize = size.minDimension
            val strokeWidth = 13.dp.toPx()
            val radius = (canvasSize - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // 1. Subtle Elevation Ring Shadow
            if (!isSimple) {
                val shadowOffset = if (isPro) 3.dp.toPx() else 4.5.dp.toPx()
                val shadowColor = if (isDarkMode) Color(0x44000000) else Color(0x287589A2)
                drawCircle(
                    color = shadowColor,
                    radius = radius,
                    center = Offset(center.x, center.y + shadowOffset)
                )
            }

            // 2. Dial Disc Surface
            if (isSimple) {
                drawCircle(
                    color = theme.surface,
                    radius = radius,
                    center = center
                )
            } else {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = if (isPro) {
                            listOf(theme.surface, theme.surfaceInset)
                        } else if (isDarkMode) {
                            listOf(Color(0xFF223045), Color(0xFF182232))
                        } else {
                            listOf(Color(0xFFF0F4F9), Color(0xFFE5ECF4), Color(0xFFDCE4EE))
                        },
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
            }

            // 3. Calm Liquid Wave inside Dial
            if (ambientLiquidWaves && !isSimple) {
                drawNeumorphicLiquidWave(
                    center = center,
                    radius = radius * 0.87f,
                    fillRatio = (animatedPercent / 100f).coerceIn(0.05f, 0.96f),
                    wavePhase = wavePhase,
                    primaryColor = activePrimary,
                    secondaryColor = activeSecondary,
                    isDarkMode = isDarkMode,
                    wavePath = wavePath,
                    clipPath = clipPath
                )
            }

            // 4. Crisp Inner Hairline Bevel
            drawCircle(
                color = if (isSimple) theme.border else if (isDarkMode) Color(0x2B475569) else Color(0x70B4C2D3),
                radius = radius * 0.88f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // 5. Recessed Track Arc
            val trackColor = if (isSimple || isPro) theme.surfaceInset else if (isDarkMode) Color(0xFF28364B) else Color(0xFFCFD9E6)
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 6. Active Precision Arc
            val activeSweep = (animatedPercent / 100f).coerceIn(0f, 1f) * 270f
            if (activeSweep > 0f) {
                if (!isSimple && glowIntensity > 0.05f) {
                    val proHaloScale = if (isPro) 0.35f else 0.75f
                    drawArc(
                        brush = Brush.sweepGradient(
                            0.0f to activeSecondary.copy(alpha = 0.16f * auraPulse * proHaloScale),
                            0.5f to activePrimary.copy(alpha = 0.25f * auraPulse * proHaloScale),
                            1.0f to activePrimary.copy(alpha = 0.32f * auraPulse * proHaloScale),
                            center = center
                        ),
                        startAngle = 135f,
                        sweepAngle = activeSweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth * 1.45f, cap = StrokeCap.Round)
                    )
                }

                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to activeSecondary,
                        0.6f to activePrimary,
                        1.0f to activePrimary,
                        center = center
                    ),
                    startAngle = 135f,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                val endAngleRad = Math.toRadians((135f + activeSweep).toDouble())
                val headX = center.x + radius * cos(endAngleRad).toFloat()
                val headY = center.y + radius * sin(endAngleRad).toFloat()

                drawCircle(
                    color = Color.White,
                    radius = strokeWidth * 0.42f,
                    center = Offset(headX, headY)
                )
                drawCircle(
                    color = activePrimary,
                    radius = strokeWidth * 0.24f,
                    center = Offset(headX, headY)
                )
            }

            // 7. Minimal Clean Graduation Markers (11 crisp ticks instead of 36 noisy ticks)
            if (!isSimple) {
                drawDialTicks(
                    center = center,
                    radius = radius + 9.dp.toPx(),
                    accentColor = activePrimary,
                    isDarkMode = isDarkMode
                )
            }
        }

        // Center Digital Telemetry Readout
        val isArabic = LocalAppLanguage.current.isArabic
        val textColor = if (theme.themeId != null) theme.textPrimary else if (isDarkMode) TextWhite else TextCharcoal
        val subColor = if (theme.themeId != null) theme.textSecondary else TextSlate

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val stateIcon = when (livingState) {
                LivingBatteryState.OVERDRIVE_HEATING -> Icons.Default.LocalFireDepartment
                LivingBatteryState.COOLING -> Icons.Default.AcUnit
                LivingBatteryState.ENERGY_STORING -> Icons.Default.Bolt
                LivingBatteryState.DEEP_SLEEP -> Icons.Default.Bedtime
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = stateIcon,
                    contentDescription = livingState.localizedTitle(isArabic),
                    tint = activePrimary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                val topStatusLabel = when {
                    status == BatteryStatus.FULL -> if (isArabic) "مكتمل الشحن" else "FULLY CHARGED"
                    status == BatteryStatus.CHARGING -> if (isArabic) "شحن نشط" else "CHARGING"
                    status == BatteryStatus.NOT_CHARGING && plugType != ChargePlug.NONE ->
                        if (isArabic) "موصول • متوقف" else "PLUGGED • PAUSED"
                    else -> livingState.localizedTitle(isArabic).uppercase()
                }
                Text(
                    text = topStatusLabel,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    letterSpacing = 0.6.sp,
                    color = activePrimary
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$percentage",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 54.sp,
                    lineHeight = 54.sp,
                    letterSpacing = (-1.2).sp,
                    color = textColor
                )
                Text(
                    text = "%",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 21.sp,
                    color = activePrimary,
                    modifier = Modifier.padding(bottom = 7.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            val isPluggedActive = status == BatteryStatus.CHARGING || status == BatteryStatus.FULL
            val pillText = when {
                status == BatteryStatus.FULL ->
                    "${if (isArabic) "مكتمل" else "FULL"} • ${plugType.localizedLabel(isArabic)}"
                status == BatteryStatus.CHARGING ->
                    "${String.format(Locale.US, "%.1f", wattageW)}W • ${plugType.localizedLabel(isArabic)}"
                status == BatteryStatus.NOT_CHARGING && plugType != ChargePlug.NONE ->
                    "${String.format(Locale.US, "%.1f", wattageW)}W • ${if (isArabic) "متوقف مؤقتاً" else "PAUSED"}"
                else ->
                    "${String.format(Locale.US, "%.1f", wattageW)}W • ${if (isArabic) "تفريغ" else "ACTIVE"}"
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(activePrimary.copy(alpha = if (isDarkMode) 0.16f else 0.11f))
                    .border(0.8.dp, activePrimary.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = pillText,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    color = if (isPluggedActive) activePrimary else subColor
                )
            }
        }
    }
}

private fun DrawScope.drawNeumorphicLiquidWave(
    center: Offset,
    radius: Float,
    fillRatio: Float,
    wavePhase: Float,
    primaryColor: Color,
    secondaryColor: Color,
    isDarkMode: Boolean,
    wavePath: Path,
    clipPath: Path
) {
    val waterLevelY = (center.y + radius) - (radius * 2f * fillRatio)
    val waveHeight = 4.5.dp.toPx()

    wavePath.reset()
    wavePath.moveTo(center.x - radius, center.y + radius)

    val segments = 20
    val twoPi = (2.0 * PI).toFloat()
    for (i in 0..segments) {
        val x = (center.x - radius) + (radius * 2f * i / segments)
        val normalizedX = (i.toFloat() / segments) * twoPi
        val y = waterLevelY + sin(normalizedX + wavePhase) * waveHeight
        wavePath.lineTo(x, y)
    }

    wavePath.lineTo(center.x + radius, center.y + radius)
    wavePath.close()

    clipPath.reset()
    clipPath.addOval(
        Rect(
            center.x - radius,
            center.y - radius,
            center.x + radius,
            center.y + radius
        )
    )

    drawContext.canvas.save()
    drawContext.canvas.clipPath(clipPath)

    drawPath(
        path = wavePath,
        brush = Brush.verticalGradient(
            colors = listOf(
                primaryColor.copy(alpha = if (isDarkMode) 0.24f else 0.16f),
                secondaryColor.copy(alpha = if (isDarkMode) 0.10f else 0.05f),
                Color.Transparent
            ),
            startY = waterLevelY - waveHeight,
            endY = center.y + radius
        )
    )

    drawPath(
        path = wavePath,
        color = primaryColor.copy(alpha = 0.35f),
        style = Stroke(width = 1.2.dp.toPx())
    )

    drawContext.canvas.restore()
}

private fun DrawScope.drawDialTicks(
    center: Offset,
    radius: Float,
    accentColor: Color,
    isDarkMode: Boolean
) {
    val totalTicks = 11
    val tickColor = if (isDarkMode) Color(0x4494A3B8) else Color(0x66B0BFCF)

    for (i in 0 until totalTicks) {
        val angleDeg = 135f + (i * (270f / (totalTicks - 1)))
        val angleRad = Math.toRadians(angleDeg.toDouble())
        val isMajor = i % 5 == 0
        val tickLen = if (isMajor) 5.dp.toPx() else 3.dp.toPx()

        val startX = center.x + (radius - tickLen) * cos(angleRad).toFloat()
        val startY = center.y + (radius - tickLen) * sin(angleRad).toFloat()
        val endX = center.x + radius * cos(angleRad).toFloat()
        val endY = center.y + radius * sin(angleRad).toFloat()

        drawLine(
            color = if (isMajor) accentColor.copy(alpha = 0.60f) else tickColor,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = if (isMajor) 1.6.dp.toPx() else 1.1.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
