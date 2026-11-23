package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.example.ui.theme.LocalAmpereTheme
import com.example.ui.theme.NeumorphBgDark
import com.example.ui.theme.NeumorphBgLight

/**
 * Calm, GPU-Light Thermal Ambient Background Modifier.
 * Smoothly shifts subtle radial atmospheric bloom between:
 * - Cool Cryo Blue (<= 34°C)
 * - Thermal Stress Amber/Red (>= 40°C)
 * Zero continuous per-frame coordinate drift so 120Hz scrolling stays lock-smooth.
 */
fun Modifier.thermalLiquidGlassBackground(
    temperatureC: Float,
    isDarkMode: Boolean,
    glowIntensity: Float = 0.85f,
    criticalTempThreshold: Float = 40.0f,
    normalTempThreshold: Float = 34.0f
): Modifier = composed {
    val theme = LocalAmpereTheme.current
    val rootBg = if (theme.themeId != null) theme.background else if (isDarkMode) NeumorphBgDark else NeumorphBgLight

    if (theme.isSimpleMode || glowIntensity <= 0.01f) {
        return@composed this.drawBehind {
            drawRect(color = rootBg)
        }
    }

    val coolBluePrimary = Color(0xFF00B0FF)
    val coolBlueSecondary = Color(0xFF2962FF)
    val hotOrangePrimary = Color(0xFFFF3D00)
    val hotCrimsonSecondary = Color(0xFFFF9100)

    val rawRatio = ((temperatureC - normalTempThreshold) / (criticalTempThreshold - normalTempThreshold))
        .coerceIn(0f, 1f)

    val targetPrimary = lerp(coolBluePrimary, hotOrangePrimary, rawRatio)
    val targetSecondary = lerp(coolBlueSecondary, hotCrimsonSecondary, rawRatio)

    val animatedPrimary by animateColorAsState(
        targetValue = targetPrimary,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "thermalPrimaryColor"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = targetSecondary,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "thermalSecondaryColor"
    )

    val alphaScale = (if (isDarkMode) 1.0f else 0.70f) * (if (theme.isProMode) 0.35f else 0.85f)
    val effectiveGlow = glowIntensity * alphaScale

    this.drawBehind {
        drawRect(color = rootBg)

        // Subtle centered top bloom behind Hero Gauge
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    animatedPrimary.copy(alpha = (if (isDarkMode) 0.13f else 0.09f) * effectiveGlow),
                    animatedSecondary.copy(alpha = (if (isDarkMode) 0.04f else 0.02f) * effectiveGlow),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.5f, size.height * 0.22f),
                radius = size.width * 0.88f
            )
        )

        // Delicate perimeter warning tint only during active thermal stress
        if (rawRatio > 0.65f) {
            val warningAlpha = ((rawRatio - 0.65f) / 0.35f) * (0.07f * effectiveGlow)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        hotOrangePrimary.copy(alpha = warningAlpha),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = size.height * 0.45f
                )
            )
        }
    }
}
