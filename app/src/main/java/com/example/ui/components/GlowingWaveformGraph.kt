package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.TelemetryPoint
import com.example.ui.theme.LocalAmpereTheme
import com.example.ui.theme.TactileRoyalBlue
import kotlin.math.abs
import kotlin.math.exp

/**
 * Pure Signal Conditioning & Anti-Jitter Spline Math for Telemetry Curves.
 * Eliminates high-frequency micro-oscillations via double-pass Gaussian smoothing,
 * bucket resampling into stable control knots, and physically-scaled minimum visual span.
 */
object FluidCurveMath {
    const val DEFAULT_KNOT_COUNT = 12

    fun computeNormalizedKnots(
        points: List<TelemetryPoint>,
        minVisualSpan: Float = 1.0f,
        knotCount: Int = DEFAULT_KNOT_COUNT
    ): FloatArray {
        val count = knotCount.coerceAtLeast(4)
        if (points.isEmpty()) return FloatArray(count) { 0.5f }
        if (points.size == 1) return FloatArray(count) { 0.5f }

        // 1. Double-pass weighted Gaussian smoothing kernel [0.12, 0.22, 0.32, 0.22, 0.12]
        // Completely eliminates high-frequency sensor chatter while keeping genuine trends intact.
        var smoothed = FloatArray(points.size) { points[it].value }
        repeat(2) {
            val next = FloatArray(smoothed.size)
            for (i in smoothed.indices) {
                val vM2 = smoothed[(i - 2).coerceAtLeast(0)]
                val vM1 = smoothed[(i - 1).coerceAtLeast(0)]
                val v0 = smoothed[i]
                val vP1 = smoothed[(i + 1).coerceAtMost(smoothed.lastIndex)]
                val vP2 = smoothed[(i + 2).coerceAtMost(smoothed.lastIndex)]
                next[i] = vM2 * 0.12f + vM1 * 0.22f + v0 * 0.32f + vP1 * 0.22f + vP2 * 0.12f
            }
            smoothed = next
        }

        // 2. Resample smoothed series into evenly-spaced cosine/linear knots
        val knots = FloatArray(count)
        for (k in 0 until count) {
            val pos = (k.toFloat() / (count - 1).toFloat()) * (smoothed.size - 1).toFloat()
            val idx0 = pos.toInt().coerceIn(0, smoothed.lastIndex)
            val idx1 = (idx0 + 1).coerceAtMost(smoothed.lastIndex)
            val frac = pos - idx0
            knots[k] = smoothed[idx0] + (smoothed[idx1] - smoothed[idx0]) * frac
        }

        // 3. Enforce physically meaningful minimum visual span so 1-unit fluctuations stay calm
        var minVal = Float.MAX_VALUE
        var maxVal = -Float.MAX_VALUE
        for (v in knots) {
            if (v < minVal) minVal = v
            if (v > maxVal) maxVal = v
        }
        val rawSpan = maxVal - minVal
        val effectiveSpan = maxOf(rawSpan * 1.40f, minVisualSpan.coerceAtLeast(0.001f))
        val mid = (maxVal + minVal) * 0.5f
        val low = mid - effectiveSpan * 0.5f

        return FloatArray(count) { i ->
            ((knots[i] - low) / effectiveSpan).coerceIn(0.08f, 0.92f)
        }
    }
}

/**
 * Ultra-Fluid Silk Sparkline for My Space Metric Cards.
 * Features:
 * - Gaussian signal conditioning + physical minimum span (zero tiny jittery oscillations)
 * - Frame-rate-independent exponential spline morphing via withFrameNanos (60/90/120Hz)
 * - C1-continuous Catmull-Rom cubic Bezier spline geometry
 * - Dual-layer neon ribbon glow, subtle traveling specular sheen, and softly breathing tip
 */
@Composable
fun MetricSparkline(
    points: List<TelemetryPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = TactileRoyalBlue,
    isDarkMode: Boolean = false,
    glowIntensity: Float = 0.75f,
    heightDp: Dp = 44.dp,
    minVisualSpan: Float = 1.0f
) {
    val resolvedTheme = LocalAmpereTheme.current
    val isSimple = resolvedTheme.isSimpleMode

    val targetKnots = remember(points, minVisualSpan) {
        FluidCurveMath.computeNormalizedKnots(
            points = points,
            minVisualSpan = minVisualSpan,
            knotCount = FluidCurveMath.DEFAULT_KNOT_COUNT
        )
    }

    // Rendered knot heights interpolated smoothly at display refresh rate
    val renderedKnots = remember { FloatArray(FluidCurveMath.DEFAULT_KNOT_COUNT) { targetKnots[it] } }
    val latestTarget = rememberUpdatedState(targetKnots)
    var isMorphing by remember { mutableStateOf(false) }
    var animTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(targetKnots) {
        if (!targetKnots.contentEquals(renderedKnots)) {
            isMorphing = true
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { isMorphing }.collect { active ->
            if (!active) return@collect
            var lastFrameNs = 0L
            while (isMorphing) {
                withFrameNanos { frameTimeNs ->
                    val target = latestTarget.value
                    val dtSec = if (lastFrameNs == 0L) {
                        0.016f
                    } else {
                        ((frameTimeNs - lastFrameNs) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    }
                    lastFrameNs = frameTimeNs

                    val alpha = (1f - exp(-dtSec * 5.8f)).coerceIn(0.02f, 0.45f)
                    var maxDelta = 0f
                    for (i in renderedKnots.indices) {
                        val diff = target[i] - renderedKnots[i]
                        if (abs(diff) > maxDelta) maxDelta = abs(diff)
                        renderedKnots[i] = renderedKnots[i] + diff * alpha
                    }

                    if (maxDelta < 0.0005f) {
                        for (i in renderedKnots.indices) {
                            renderedKnots[i] = target[i]
                        }
                        isMorphing = false
                    }
                    animTick++
                }
            }
        }
    }

    val pulseAlpha: Float
    val sheenPhase: Float
    if (isSimple) {
        pulseAlpha = 1.0f
        sheenPhase = 0f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "sparklineSilkVfx")
        val animPulse by infiniteTransition.animateFloat(
            initialValue = 0.55f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "sparklineAlpha"
        )
        val animSheen by infiniteTransition.animateFloat(
            initialValue = -0.25f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(3200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "sparklineSheen"
        )
        pulseAlpha = animPulse
        sheenPhase = animSheen
    }

    // Pre-allocated paths, dash effect & coordinate buffers to prevent GC churn
    val strokePath = remember { Path() }
    val fillPath = remember { Path() }
    val baselineDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(5f, 7f), 0f) }
    val coordBuffer = remember { Array(FluidCurveMath.DEFAULT_KNOT_COUNT) { Offset.Zero } }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp)
            .graphicsLayer { clip = false }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Read animTick to re-draw cleanly on active frame morphing ticks
            @Suppress("UNUSED_EXPRESSION")
            animTick

            val w = size.width
            val h = size.height
            if (w <= 1f || h <= 1f) return@Canvas

            val topMargin = h * 0.14f
            val usableH = h * 0.72f
            val minYBound = h * 0.05f
            val maxYBound = h * 0.95f

            val count = renderedKnots.size
            val stepX = w / (count - 1).coerceAtLeast(1)
            for (i in 0 until count) {
                val x = i * stepX
                val y = (h - (topMargin + renderedKnots[i] * usableH)).coerceIn(minYBound, maxYBound)
                coordBuffer[i] = Offset(x, y)
            }

            // Build smooth C1-continuous Catmull-Rom spline path
            strokePath.reset()
            fillPath.reset()

            val first = coordBuffer[0]
            strokePath.moveTo(first.x, first.y)
            fillPath.moveTo(first.x, h)
            fillPath.lineTo(first.x, first.y)

            val tension = 0.45f
            for (i in 0 until count - 1) {
                val p0 = coordBuffer[(i - 1).coerceAtLeast(0)]
                val p1 = coordBuffer[i]
                val p2 = coordBuffer[i + 1]
                val p3 = coordBuffer[(i + 2).coerceAtMost(count - 1)]

                val c1x = p1.x + (p2.x - p0.x) * tension / 3f
                val c1y = (p1.y + (p2.y - p0.y) * tension / 3f).coerceIn(minYBound, maxYBound)
                val c2x = p2.x - (p3.x - p1.x) * tension / 3f
                val c2y = (p2.y - (p3.y - p1.y) * tension / 3f).coerceIn(minYBound, maxYBound)

                strokePath.cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
                fillPath.cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
            }

            val last = coordBuffer[count - 1]
            fillPath.lineTo(last.x, h)
            fillPath.close()

            // Subtle faint reference midpoint hairline
            drawLine(
                color = lineColor.copy(alpha = if (isDarkMode) 0.12f else 0.10f),
                start = Offset(0f, h * 0.5f),
                end = Offset(w, h * 0.5f),
                strokeWidth = 0.8.dp.toPx(),
                pathEffect = baselineDashEffect
            )

            // 1. Silky vertical gradient underfill
            val fillPeakAlpha = if (isSimple) 0.12f else (if (isDarkMode) 0.30f else 0.20f) * glowIntensity.coerceAtLeast(0.45f)
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = fillPeakAlpha),
                        lineColor.copy(alpha = fillPeakAlpha * 0.25f),
                        Color.Transparent
                    ),
                    startY = topMargin * 0.5f,
                    endY = h
                )
            )

            // 2. Soft diffused ribbon bloom (skipped in Simple Mode)
            if (!isSimple) {
                drawPath(
                    path = strokePath,
                    color = lineColor.copy(alpha = if (isDarkMode) 0.28f * glowIntensity else 0.16f * glowIntensity),
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // 3. Primary crisp gradient silk stroke
            val curveBrush = if (isSimple) {
                Brush.horizontalGradient(colors = listOf(lineColor.copy(alpha = 0.85f), lineColor))
            } else {
                Brush.horizontalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.45f),
                        lineColor.copy(alpha = 0.85f),
                        lineColor
                    )
                )
            }
            drawPath(
                path = strokePath,
                brush = curveBrush,
                style = Stroke(width = if (isSimple) 1.8.dp.toPx() else 2.3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // 4. Serene traveling specular light wave along spline (skipped in Simple Mode)
            if (!isSimple && glowIntensity > 0.1f) {
                val sheenStart = (sheenPhase - 0.18f) * w
                val sheenEnd = (sheenPhase + 0.18f) * w
                drawPath(
                    path = strokePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = if (isDarkMode) 0.55f else 0.65f),
                            Color.Transparent
                        ),
                        startX = sheenStart,
                        endX = sheenEnd
                    ),
                    style = Stroke(width = 2.1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // 5. Real-time leading tracking beacon
            if (!isSimple) {
                drawCircle(
                    color = lineColor.copy(alpha = pulseAlpha * 0.28f),
                    radius = 7.dp.toPx(),
                    center = last
                )
                drawCircle(
                    color = lineColor.copy(alpha = 0.90f),
                    radius = 3.5.dp.toPx(),
                    center = last
                )
            }
            drawCircle(
                color = if (isSimple) lineColor else Color.White,
                radius = 2.dp.toPx(),
                center = last
            )
        }
    }
}
