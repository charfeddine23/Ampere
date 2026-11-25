package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAmpereTheme
import com.example.ui.theme.NeumorphBorderDark
import com.example.ui.theme.NeumorphBorderLight
import com.example.ui.theme.NeumorphShadowDark
import com.example.ui.theme.NeumorphShadowLight
import com.example.ui.theme.NeumorphSurfaceDark
import com.example.ui.theme.NeumorphSurfaceLight
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.TactileRoyalBlue
import com.example.ui.theme.TextCharcoal
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite

/**
 * Refined Executive Surface Card
 * Balances clean hairline borders, calm tonal contrast, and whisper-soft depth
 * without noisy multi-pass shadow clutter.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    accentColor: Color = TactileRoyalBlue,
    isDarkMode: Boolean = false,
    glowIntensity: Float = 0.75f,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    val tagModifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
    val theme = LocalAmpereTheme.current
    val customThemeActive = theme.themeId != null

    val isSimple = theme.isSimpleMode
    val isPro = theme.isProMode
    val surfaceColor = if (customThemeActive) theme.surface else if (isDarkMode) NeumorphSurfaceDark else NeumorphSurfaceLight
    val shadowColor = when {
        isSimple -> Color.Transparent
        isPro -> if (isDarkMode) Color(0x28000000) else Color(0x187589A2)
        else -> if (isDarkMode) NeumorphShadowDark.copy(alpha = 0.42f) else NeumorphShadowLight.copy(alpha = 0.65f)
    }
    val borderColor = if (customThemeActive) {
        theme.border
    } else if (isDarkMode) {
        NeumorphBorderDark
    } else {
        NeumorphBorderLight
    }
    val effectiveAccent = if (isSimple) theme.accent else accentColor

    Box(
        modifier = modifier
            .then(tagModifier)
            .drawBehind {
                if (!isSimple) {
                    val shadowOffset = if (isPro) 2.dp.toPx() else 3.5.dp.toPx()
                    val cornerPx = (shape as? CornerBasedShape)?.topStart?.toPx(size, this) ?: 20.dp.toPx()
                    drawRoundRect(
                        color = shadowColor,
                        topLeft = Offset(0f, shadowOffset),
                        size = size,
                        cornerRadius = CornerRadius(cornerPx, cornerPx)
                    )
                }
            }
            .clip(shape)
            .background(surfaceColor)
            .drawBehind {
                if (!isSimple && glowIntensity > 0.05f) {
                    val sheenAlpha = when {
                        isPro -> if (isDarkMode) 0.02f else 0.07f
                        theme.isGlassStyle -> if (isDarkMode) 0.05f else 0.14f
                        else -> if (isDarkMode) 0.03f else 0.10f
                    }
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = sheenAlpha),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.45f
                        )
                    )

                    val tintAlpha = (if (isDarkMode) 0.06f else 0.035f) * glowIntensity * (if (isPro) 0.4f else 1f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                effectiveAccent.copy(alpha = tintAlpha),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.18f, 0f),
                            radius = size.width * 0.65f
                        )
                    )
                }
            }
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shape
            )
            .then(clickableModifier),
        content = content
    )
}

/**
 * Unified Precision Metric Tile shared cleanly across Battery, Cell SOH, and Cable DNA pages
 * for a cohesive Swiss-engineering layout.
 */
@Composable
fun VoltMetricTile(
    title: String,
    value: String,
    subtext: String,
    accentColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    highlightBadge: String? = null,
    testTag: String? = null
) {
    val theme = LocalAmpereTheme.current
    val customTheme = theme.themeId != null
    val textColor = if (customTheme) theme.textPrimary else if (isDark) TextWhite else TextCharcoal
    val subColor = if (customTheme) theme.textSecondary else if (isDark) Color(0xFF94A3B8) else TextSlate
    val effectiveAccent = if (theme.isSimpleMode) theme.accent else accentColor

    LiquidGlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        accentColor = effectiveAccent,
        isDarkMode = isDark,
        glowIntensity = if (theme.isSimpleMode) 0f else 0.55f,
        testTag = testTag
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 12.dp)
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
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(effectiveAccent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.5.sp,
                        color = subColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!highlightBadge.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(effectiveAccent.copy(alpha = if (isDark) 0.18f else 0.12f))
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = highlightBadge,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            color = effectiveAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.5.sp,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtext,
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                color = effectiveAccent.copy(alpha = 0.90f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Clean Minimalist Section Header for scannable vertical rhythm.
 */
@Composable
fun VoltSectionHeader(
    title: String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    val theme = LocalAmpereTheme.current
    val textColor = if (theme.themeId != null) theme.textPrimary else if (isDark) TextWhite else TextCharcoal
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = 0.2.sp,
            color = textColor
        )
        if (trailing != null) {
            trailing()
        }
    }
}

/**
 * Minimal Live Status Dot
 */
@Composable
fun PulsingNeonDot(
    modifier: Modifier = Modifier,
    color: Color = TactileRoyalBlue,
    sizeDp: Dp = 8.dp
) {
    val theme = LocalAmpereTheme.current
    if (theme.isSimpleMode) {
        Box(
            modifier = modifier.size(sizeDp * 1.4f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(sizeDp)
                    .clip(CircleShape)
                    .background(theme.accent)
            )
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotScale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.80f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    Box(
        modifier = modifier.size(sizeDp * 1.8f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(sizeDp * scale)
                .clip(CircleShape)
                .background(color.copy(alpha = alpha * 0.32f))
        )
        Box(
            modifier = Modifier
                .size(sizeDp)
                .clip(CircleShape)
                .background(color)
        )
    }
}
