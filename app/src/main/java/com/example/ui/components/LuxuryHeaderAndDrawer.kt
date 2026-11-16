package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.WorkspacePremium
import com.example.data.AppLanguage
import com.example.data.GraphicsMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlin.math.abs
import com.example.audio.CyberHapticsEngine
import com.example.data.GlassThemeConfig
import com.example.data.TemperatureUnit
import com.example.data.update.InAppUpdateUiState
import com.example.ui.theme.LocalAmpereTheme
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.TactileAmber
import com.example.ui.theme.TactileCoralRed
import com.example.ui.theme.TactileCyan
import com.example.ui.theme.TactileGreen
import com.example.ui.theme.TactileViolet
import com.example.ui.theme.TextCharcoal
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite
import com.example.ui.theme.ThemeEngineCatalog
import java.util.Locale

private val EMP_RING_SEGMENTS = listOf(
    "M26.12,2.30 A21.80,21.80 0 0,1 32.91,4.10 Q33.39,4.32 33.16,4.80 L32.46,6.26 Q32.23,6.74 31.76,6.52 A19.13,19.13 0 0,0 25.93,4.97 Q25.40,4.92 25.44,4.40 L25.56,2.78 Q25.60,2.26 26.12,2.30 Z",
    "M38.13,7.40 A21.80,21.80 0 0,1 41.21,10.61 Q41.52,11.03 41.10,11.35 L39.80,12.31 Q39.38,12.62 39.06,12.20 A19.13,19.13 0 0,0 36.44,9.47 Q36.04,9.13 36.37,8.73 L37.39,7.47 Q37.72,7.06 38.13,7.40 Z",
    "M42.95,13.23 A21.80,21.80 0 0,1 45.70,21.88 Q45.74,22.40 45.22,22.44 L43.60,22.56 Q43.08,22.60 43.03,22.07 A19.13,19.13 0 0,0 40.66,14.60 Q40.40,14.15 40.85,13.88 L42.23,13.04 Q42.69,12.77 42.95,13.23 Z",
    "M45.70,26.12 A21.80,21.80 0 0,1 26.12,45.70 Q25.60,45.74 25.56,45.22 L25.44,43.60 Q25.40,43.08 25.93,43.03 A19.13,19.13 0 0,0 43.03,25.93 Q43.08,25.40 43.60,25.44 L45.22,25.56 Q45.74,25.60 45.70,26.12 Z",
    "M21.88,45.70 A21.80,21.80 0 0,1 15.09,43.90 Q14.61,43.68 14.84,43.20 L15.54,41.74 Q15.77,41.26 16.24,41.48 A19.13,19.13 0 0,0 22.07,43.03 Q22.60,43.08 22.56,43.60 L22.44,45.22 Q22.40,45.74 21.88,45.70 Z",
    "M9.87,40.60 A21.80,21.80 0 0,1 6.56,37.08 Q6.25,36.66 6.68,36.35 L8.00,35.41 Q8.43,35.11 8.74,35.53 A19.13,19.13 0 0,0 11.56,38.53 Q11.96,38.87 11.63,39.27 L10.61,40.53 Q10.28,40.94 9.87,40.60 Z",
    "M4.86,34.44 A21.80,21.80 0 0,1 2.30,26.12 Q2.26,25.60 2.78,25.56 L4.40,25.44 Q4.92,25.40 4.97,25.93 A19.13,19.13 0 0,0 7.18,33.10 Q7.43,33.56 6.98,33.83 L5.58,34.64 Q5.12,34.90 4.86,34.44 Z",
    "M2.30,21.88 A21.80,21.80 0 0,1 21.88,2.30 Q22.40,2.26 22.44,2.78 L22.56,4.40 Q22.60,4.92 22.07,4.97 A19.13,19.13 0 0,0 4.97,22.07 Q4.92,22.60 4.40,22.56 L2.78,22.44 Q2.26,22.40 2.30,21.88 Z",
    "M26.05,7.25 A16.88,16.88 0 0,1 29.13,7.92 Q29.63,8.09 29.46,8.59 L28.92,10.11 Q28.74,10.61 28.24,10.44 A14.21,14.21 0 0,0 25.81,9.91 Q25.29,9.85 25.34,9.33 L25.48,7.72 Q25.53,7.19 26.05,7.25 Z",
    "M35.79,11.92 A16.88,16.88 0 0,1 40.75,21.95 Q40.81,22.47 40.28,22.52 L38.67,22.66 Q38.15,22.71 38.09,22.19 A14.21,14.21 0 0,0 33.98,13.89 Q33.60,13.53 33.95,13.14 L35.05,11.95 Q35.40,11.56 35.79,11.92 Z",
    "M21.95,40.75 A16.88,16.88 0 0,1 18.59,39.99 Q18.09,39.81 18.27,39.31 L18.84,37.80 Q19.03,37.31 19.52,37.48 A14.21,14.21 0 0,0 22.19,38.09 Q22.71,38.15 22.66,38.67 L22.52,40.28 Q22.47,40.81 21.95,40.75 Z",
    "M12.01,35.87 A16.88,16.88 0 0,1 10.66,34.34 Q10.35,33.92 10.77,33.61 L12.08,32.66 Q12.51,32.35 12.83,32.77 A14.21,14.21 0 0,0 13.85,33.93 Q14.22,34.30 13.86,34.69 L12.75,35.86 Q12.38,36.24 12.01,35.87 Z",
    "M9.06,31.85 A16.88,16.88 0 0,1 7.25,26.05 Q7.19,25.53 7.72,25.48 L9.33,25.34 Q9.85,25.29 9.91,25.81 A14.21,14.21 0 0,0 11.39,30.53 Q11.64,30.99 11.18,31.25 L9.77,32.05 Q9.31,32.31 9.06,31.85 Z",
    "M7.25,21.95 A16.88,16.88 0 0,1 21.95,7.25 Q22.47,7.19 22.52,7.72 L22.66,9.33 Q22.71,9.85 22.19,9.91 A14.21,14.21 0 0,0 9.91,22.19 Q9.85,22.71 9.33,22.66 L7.72,22.52 Q7.19,22.47 7.25,21.95 Z"
)

private const val EMP_ACCENT_ARC =
    "M40.75,26.05 A16.88,16.88 0 0,1 26.05,40.75 Q25.53,40.81 25.48,40.28 L25.34,38.67 Q25.29,38.15 25.81,38.09 A14.21,14.21 0 0,0 38.09,25.81 Q38.15,25.29 38.67,25.34 L40.28,25.48 Q40.81,25.53 40.75,26.05 Z"

private const val EMP_LIGHTNING_BOLT =
    "M36.10,4.87 L11.48,25.55 L21.33,25.55 L11.90,43.13 L36.52,22.45 L26.67,22.45 Z"

/**
 * Precision-crafted, theme- & size-adaptive Ampere Pulse emblem.
 * Scales smoothly to any Dp dimension and adapts colors seamlessly across Light, Dark,
 * Simple, Pro, and all 60 Theme Engine studio presets.
 */
@Composable
fun AmpereBrandEmblem(
    size: Dp,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    val resolvedTheme = LocalAmpereTheme.current
    val isDark = resolvedTheme.isDark
    val isSimple = resolvedTheme.isSimpleMode
    val isPro = resolvedTheme.isProMode

    val parsedPaths = remember {
        val parser = PathParser()
        Triple(
            EMP_RING_SEGMENTS.map { parser.parsePathString(it).toPath() },
            parser.parsePathString(EMP_ACCENT_ARC).toPath(),
            parser.parsePathString(EMP_LIGHTNING_BOLT).toPath()
        )
    }

    val ringFillColor = when {
        isSimple -> if (isDark) Color(0xFFDDE3EC) else Color(0xFFF9FBFD)
        isPro -> if (isDark) Color(0xFFEAF0F7) else Color(0xFFFAFCFF)
        isDark -> lerp(Color(0xFFF3F8FF), resolvedTheme.surface, 0.14f)
        else -> Color(0xFFFAFCFF)
    }
    val strokeTint = when {
        isSimple -> resolvedTheme.accent.copy(alpha = 0.82f)
        isPro -> resolvedTheme.accent
        else -> lerp(resolvedTheme.accent, Color(0xFF005CFF), if (resolvedTheme.themeId != null) 0.25f else 0.72f)
    }
    val arcStartColor = when {
        isSimple -> resolvedTheme.accent.copy(alpha = 0.38f)
        isPro -> lerp(resolvedTheme.secondaryAccent, resolvedTheme.accent, 0.35f)
        else -> lerp(TactileCyan, resolvedTheme.secondaryAccent, 0.35f)
    }
    val arcEndColor = when {
        isSimple -> resolvedTheme.accent.copy(alpha = 0.55f)
        isPro -> resolvedTheme.accent
        else -> strokeTint
    }
    val boltTintEnd = when {
        isSimple -> ringFillColor
        isPro -> lerp(ringFillColor, resolvedTheme.accent, 0.14f)
        else -> lerp(Color(0xFFD8EAFF), resolvedTheme.accent, 0.18f)
    }

    val semanticsMod = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    Canvas(modifier = semanticsMod.size(size)) {
        val scaleFactor = minOf(this.size.width, this.size.height) / 48f
        val arcBrush = Brush.linearGradient(
            colors = listOf(arcStartColor, lerp(arcStartColor, arcEndColor, 0.5f), arcEndColor),
            start = Offset(41f, 25f),
            end = Offset(25f, 41f)
        )
        val boltFillBrush = Brush.linearGradient(
            colors = listOf(Color.White, lerp(Color.White, boltTintEnd, 0.55f), boltTintEnd),
            start = Offset(13f, 10f),
            end = Offset(35f, 38f)
        )
        val boltStrokeBrush = Brush.linearGradient(
            colors = listOf(arcStartColor, strokeTint, arcEndColor),
            start = Offset(34f, 6f),
            end = Offset(14f, 42f)
        )

        withTransform({
            scale(scaleFactor, scaleFactor, Offset.Zero)
        }) {
            val segStroke = Stroke(width = 0.74f, join = StrokeJoin.Round)
            parsedPaths.first.forEach { segPath ->
                drawPath(path = segPath, color = ringFillColor, style = Fill)
                drawPath(path = segPath, color = strokeTint, style = segStroke)
            }

            drawPath(path = parsedPaths.second, brush = arcBrush, style = Fill)
            drawPath(path = parsedPaths.second, color = strokeTint, style = segStroke)

            drawPath(path = parsedPaths.third, brush = boltFillBrush, style = Fill)
            drawPath(
                path = parsedPaths.third,
                brush = boltStrokeBrush,
                style = Stroke(width = 0.92f, join = StrokeJoin.Miter)
            )
        }
    }
}

/**
 * Top Header with Luxury 3-line frosted glass hamburger menu,
 * Quick Dark Mode / Light Mode toggle, and 90 FPS status HUD.
 */
@Composable
fun LuxuryTopBar(
    title: String,
    subtitle: String,
    isMenuOpen: Boolean,
    onMenuToggle: () -> Unit,
    themeConfig: GlassThemeConfig,
    onToggleDarkMode: () -> Unit,
    onOpenSkinVault: () -> Unit = {},
    onOpenRedeemDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = themeConfig.isDarkMode
    val isArabic = themeConfig.appLanguage.isArabic
    val accentColor = themeConfig.accentColor
    val resolvedTheme = LocalAmpereTheme.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .graphicsLayer { clip = false },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // App Title, Theme-Adaptive Emblem & Status Subtitle
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AmpereBrandEmblem(
                    size = 24.dp,
                    contentDescription = if (isArabic) "شعار Ampere Pulse" else "Ampere Pulse Emblem",
                    modifier = Modifier.testTag("in_app_brand_icon")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp,
                    letterSpacing = (-0.3).sp,
                    color = if (resolvedTheme.themeId != null) resolvedTheme.textPrimary else if (isDark) TextWhite else TextCharcoal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                PulsingNeonDot(color = accentColor, sizeDp = 5.5.dp)

                // Dynamic glowing AppTier badge (FREE / VIP / PREMIUM)
                Spacer(modifier = Modifier.width(7.dp))
                val tier = themeConfig.appTier
                val tierColor = tier.displayColor(isDark)
                val infiniteTransition = rememberInfiniteTransition(label = "tierGlow")
                val pulseGlow by infiniteTransition.animateFloat(
                    initialValue = 0.55f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "tierPulse"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    tierColor.copy(alpha = if (isDark) 0.28f else 0.16f),
                                    tierColor.copy(alpha = if (isDark) 0.14f else 0.08f)
                                )
                            )
                        )
                        .border(
                            width = 1.1.dp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    tierColor.copy(alpha = 0.85f * pulseGlow),
                                    tierColor.copy(alpha = 0.40f)
                                )
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            if (!themeConfig.isPremiumUnlocked) onOpenRedeemDialog() else onMenuToggle()
                        }
                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                        .testTag("app_tier_badge")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(tierColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tier.localizedLabel(isArabic),
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            letterSpacing = 0.4.sp,
                            color = tierColor
                        )
                    }
                }
            }
            Text(
                text = subtitle,
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = TextSlate,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Actions: Quick Dark/Light Toggle + Luxury 3-Line Button
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Dark Mode Quick Toggle Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (resolvedTheme.themeId != null) resolvedTheme.surface else if (isDark) Color(0xFF1E293B) else Color(0xFFE7ECF3))
                    .border(
                        1.dp,
                        if (resolvedTheme.themeId != null) resolvedTheme.border else if (isDark) Color(0x33475569) else Color(0x99BECAD8),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onToggleDarkMode() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = if (isArabic) "تبديل المظهر" else "Toggle Dark Mode",
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Luxury 3-Line Animated Hamburger Button
            LuxuryHamburgerButton(
                isOpen = isMenuOpen,
                onClick = onMenuToggle,
                accentColor = accentColor,
                isDarkMode = isDark
            )
        }
    }
}

/**
 * Animated 3-line luxury hamburger button using Spring physics
 */
@Composable
fun LuxuryHamburgerButton(
    isOpen: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val line1Rotate by animateFloatAsState(
        targetValue = if (isOpen) 45f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "line1Rotate"
    )
    val line2Alpha by animateFloatAsState(
        targetValue = if (isOpen) 0f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "line2Alpha"
    )
    val line3Rotate by animateFloatAsState(
        targetValue = if (isOpen) -45f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "line3Rotate"
    )
    val line1TranslateY by animateFloatAsState(
        targetValue = if (isOpen) 6.5f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "line1Y"
    )
    val line3TranslateY by animateFloatAsState(
        targetValue = if (isOpen) -6.5f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "line3Y"
    )

    val buttonBg = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE7ECF3)
    val borderColor = if (isDarkMode) Color(0x33475569) else Color(0x99BECAD8)
    val lineColor = if (isDarkMode) TextWhite else TextCharcoal

    Box(
        modifier = modifier
            .testTag("luxury_hamburger_button")
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(buttonBg)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(9.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.size(22.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.2.dp)
                    .graphicsLayer {
                        rotationZ = line1Rotate
                        translationY = line1TranslateY * density
                    }
                    .clip(CircleShape)
                    .background(if (isOpen) accentColor else lineColor)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(2.2.dp)
                    .graphicsLayer { alpha = line2Alpha }
                    .clip(CircleShape)
                    .background(lineColor)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.2.dp)
                    .graphicsLayer {
                        rotationZ = line3Rotate
                        translationY = line3TranslateY * density
                    }
                    .clip(CircleShape)
                    .background(if (isOpen) accentColor else lineColor)
            )
        }
    }
}

/**
 * Modern, Simple, Dynamic & Eye-Friendly Slide-Over Luxury Settings Drawer
 */
@Composable
fun LuxurySettingsDrawer(
    isOpen: Boolean,
    onClose: () -> Unit,
    themeConfig: GlassThemeConfig,
    onConfigChange: (GlassThemeConfig) -> Unit,
    onOpenThemeEngine: () -> Unit = {},
    onOpenSkinVault: () -> Unit = {},
    onOpenRedeemDialog: () -> Unit = {},
    updateUiState: InAppUpdateUiState = InAppUpdateUiState(),
    onCheckUpdates: () -> Unit = {},
    onOpenUpdateDialog: () -> Unit = {},
    onTriggerUpdate: () -> Unit = {},
    onCancelDownload: () -> Unit = {}
) {
    if (isOpen) {
        BackHandler { onClose() }
    }

    val context = LocalContext.current
    val resolvedTheme = LocalAmpereTheme.current
    val isDark = themeConfig.isDarkMode
    val isArabic = themeConfig.appLanguage.isArabic
    val isRtl = themeConfig.appLanguage.isRtl
    val accentColor = themeConfig.accentColor

    // Respects Theme Engine preset if active, otherwise uses Soft Nordic twilight / Stone Mist palette
    val drawerBgBrush = if (resolvedTheme.themeId != null) {
        Brush.verticalGradient(
            colors = listOf(
                resolvedTheme.background.copy(alpha = 0.98f),
                resolvedTheme.surface.copy(alpha = 0.98f)
            )
        )
    } else if (isDark) {
        Brush.verticalGradient(colors = listOf(Color(0xF8151D2C), Color(0xFA101622)))
    } else {
        Brush.verticalGradient(colors = listOf(Color(0xFCE5ECF4), Color(0xFCD9E1EC)))
    }
    val surfaceCardColor = if (resolvedTheme.themeId != null) resolvedTheme.surface else if (isDark) Color(0xFF1B2436) else Color(0xFFECF0F6)
    val softTileColor = if (resolvedTheme.themeId != null) resolvedTheme.surfaceInset else if (isDark) Color(0xFF212C42) else Color(0xFFD2DBE6)
    val subtleBorder = if (resolvedTheme.themeId != null) resolvedTheme.border else if (isDark) Color(0xFF2A364F) else Color(0xFFC0CDDC)
    val textColor = if (resolvedTheme.themeId != null) resolvedTheme.textPrimary else if (isDark) Color(0xFFE9EFF8) else TextCharcoal
    val subTextColor = if (resolvedTheme.themeId != null) resolvedTheme.textSecondary else if (isDark) Color(0xFF93A5BF) else TextSlate

    var expandAppearance by remember { mutableStateOf(false) }
    var expandColorsIdentity by remember { mutableStateOf(false) }
    var expandGraphics by remember { mutableStateOf(false) }
    var expandNotifications by remember { mutableStateOf(false) }
    var expandAutoUpdate by remember { mutableStateOf(false) }
    var expandCalibration by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(tween(220)) + slideInHorizontally(
            initialOffsetX = { if (isRtl) -it else it },
            animationSpec = spring(dampingRatio = 0.84f, stiffness = Spring.StiffnessMediumLow)
        ),
        exit = fadeOut(tween(180)) + slideOutHorizontally(
            targetOffsetX = { if (isRtl) -it else it },
            animationSpec = spring(stiffness = Spring.StiffnessMedium)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x66060B14))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onClose() },
            contentAlignment = Alignment.CenterEnd
        ) {
            val drawerDensity = LocalDensity.current
            val closeSwipeTriggerPx = with(drawerDensity) { 38.dp.toPx() }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.86f)
                    .widthIn(max = 420.dp)
                    .pointerInput(isOpen, isRtl) {
                        val touchSlop = viewConfiguration.touchSlop
                        coroutineScope {
                            while (isActive) {
                                awaitPointerEventScope {
                                    val down = awaitFirstDown(
                                        requireUnconsumed = false,
                                        pass = PointerEventPass.Main
                                    )
                                    val startX = down.position.x
                                    val startY = down.position.y
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Main)
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!change.pressed || change.isConsumed) break

                                        val totalDx = change.position.x - startX
                                        val totalDy = change.position.y - startY
                                        if (abs(totalDy) > touchSlop && abs(totalDy) >= abs(totalDx)) break

                                        // Swipe toward edge edge-end to dismiss Settings Drawer cleanly in RTL or LTR
                                        val closeSwiped = if (isRtl) totalDx < -closeSwipeTriggerPx else totalDx > closeSwipeTriggerPx
                                        if (closeSwiped && abs(totalDx) > abs(totalDy) * 1.35f) {
                                            change.consume()
                                            CyberHapticsEngine.triggerTactileTick(context)
                                            onClose()
                                            break
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* consume touches inside drawer */ }
                    .clip(RoundedCornerShape(topStart = 30.dp, bottomStart = 30.dp))
                    .background(drawerBgBrush)
                    .border(
                        1.dp,
                        subtleBorder,
                        RoundedCornerShape(topStart = 30.dp, bottomStart = 30.dp)
                    )
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Minimal Luxury Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            AmpereBrandEmblem(
                                size = 30.dp,
                                contentDescription = if (isArabic) "شعار Ampere Pulse" else "Ampere Pulse Logo"
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArabic) "الإعدادات" else "Settings",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 21.sp,
                                    color = textColor
                                )
                                Text(
                                    text = if (isArabic) {
                                        "تخصيص شامل لتجربة Ampere Pulse"
                                    } else {
                                        "Precision control for Ampere Pulse"
                                    },
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 10.5.sp,
                                    color = subTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Quick Language Switch Pill (AR / EN)
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(surfaceCardColor)
                                    .border(1.dp, subtleBorder, CircleShape)
                                    .clickable {
                                        CyberHapticsEngine.triggerTactileTick(context)
                                        val nextLang = if (isArabic) AppLanguage.ENGLISH else AppLanguage.ARABIC
                                        onConfigChange(themeConfig.copy(appLanguage = nextLang))
                                    }
                                    .testTag("drawer_quick_language_toggle"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isArabic) "EN" else "ع",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = accentColor
                                )
                            }

                            // Quick Eye-Comfort Theme Switch Pill
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(surfaceCardColor)
                                    .border(1.dp, subtleBorder, CircleShape)
                                    .clickable {
                                        CyberHapticsEngine.triggerTactileTick(context)
                                        onConfigChange(themeConfig.copy(isDarkMode = !isDark))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = if (isArabic) "تبديل المظهر" else "Toggle Theme",
                                    tint = accentColor,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            // Accessible Close Button
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(surfaceCardColor)
                                    .border(1.dp, subtleBorder, CircleShape)
                                    .clickable {
                                        CyberHapticsEngine.triggerTactileTick(context)
                                        onClose()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = if (isArabic) "إغلاق الإعدادات" else "Close Settings",
                                    tint = subTextColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // 2. TOP SECTION: Appearance (Language, Theme Engine, Skin Vault, Colors & Identity)
                    SettingsAccordionCard(
                        title = if (isArabic) "المظهر واللغة" else "Appearance & Language",
                        summary = "${resolvedTheme.themeName} • ${if (isDark) (if (isArabic) "ليلي" else "Dark") else (if (isArabic) "نهاري" else "Light")} • ${themeConfig.appLanguage.nativeName}",
                        icon = Icons.Default.Palette,
                        expanded = expandAppearance,
                        onToggle = {
                            CyberHapticsEngine.triggerTactileTick(context)
                            expandAppearance = !expandAppearance
                        },
                        accentColor = accentColor,
                        surfaceColor = surfaceCardColor,
                        borderColor = subtleBorder,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        testTag = "settings_appearance_section"
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // 2a. Theme Engine Studio Showcase Banner (60 Curated IDE & Liquid Glass Themes)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 64.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                accentColor.copy(alpha = if (isDark) 0.24f else 0.14f),
                                                resolvedTheme.secondaryAccent.copy(alpha = if (isDark) 0.12f else 0.08f),
                                                softTileColor
                                            )
                                        )
                                    )
                                    .border(
                                        1.1.dp,
                                        accentColor.copy(alpha = if (isDark) 0.48f else 0.34f),
                                        RoundedCornerShape(18.dp)
                                    )
                                    .clickable {
                                        CyberHapticsEngine.triggerTactileTick(context)
                                        onClose()
                                        onOpenThemeEngine()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("drawer_theme_engine_banner")
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
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(11.dp))
                                                .background(accentColor.copy(alpha = if (isDark) 0.25f else 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Palette,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = if (isArabic) "محرك الثيمات" else "Theme Engine",
                                                    fontFamily = OutfitFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.5.sp,
                                                    color = textColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(accentColor.copy(alpha = 0.20f))
                                                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                                ) {
                                                    Text(
                                                        text = "${ThemeEngineCatalog.presets.size}",
                                                        fontFamily = OutfitFontFamily,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 9.5.sp,
                                                        color = accentColor
                                                    )
                                                }
                                            }
                                            Text(
                                                text = if (isArabic) "النشط: ${resolvedTheme.themeName}" else "Active: ${resolvedTheme.themeName}",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 10.sp,
                                                color = subTextColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // 2b. Cyber Skin Vault Showcase Banner
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 60.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                accentColor.copy(alpha = if (isDark) 0.20f else 0.11f),
                                                softTileColor
                                            )
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        accentColor.copy(alpha = if (isDark) 0.40f else 0.28f),
                                        RoundedCornerShape(18.dp)
                                    )
                                    .clickable {
                                        CyberHapticsEngine.triggerTactileTick(context)
                                        onClose()
                                        onOpenSkinVault()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("drawer_cyber_vault_banner")
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
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(11.dp))
                                                .background(accentColor.copy(alpha = if (isDark) 0.24f else 0.14f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (isArabic) "متجر الثيمات الزجاجية" else "Glass Skin Vault",
                                                fontFamily = OutfitFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.8.sp,
                                                color = textColor
                                            )
                                            Text(
                                                text = if (isArabic) "أقنعة ومؤثرات بصرية مريحة للعين" else "Eye-friendly liquid glass overlays",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 10.sp,
                                                color = subTextColor
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // 2c. Full App Language Selector (العربية / English)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(softTileColor.copy(alpha = 0.62f))
                                    .border(1.dp, subtleBorder, RoundedCornerShape(16.dp))
                                    .padding(12.dp)
                                    .testTag("settings_language_selector"),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        text = if (isArabic) "لغة التطبيق" else "App Language",
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = textColor
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AppLanguage.entries.forEach { langOption ->
                                        val selected = themeConfig.appLanguage == langOption
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (selected) accentColor.copy(alpha = if (isDark) 0.24f else 0.14f)
                                                    else surfaceCardColor
                                                )
                                                .border(
                                                    1.1.dp,
                                                    if (selected) accentColor.copy(alpha = 0.75f) else subtleBorder,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    CyberHapticsEngine.triggerTactileTick(context)
                                                    onConfigChange(themeConfig.copy(appLanguage = langOption))
                                                }
                                                .testTag("language_option_${langOption.code}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                if (selected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = accentColor,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                }
                                                Text(
                                                    text = langOption.nativeName,
                                                    fontFamily = OutfitFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = if (selected) accentColor else subTextColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 2d. Colors & Identity (moved inside Appearance)
                            SettingsAccordionCard(
                                title = if (isArabic) "الألوان والهوية" else "Colors & Identity",
                                summary = "${if (isDark) (if (isArabic) "ليلي" else "Dark") else (if (isArabic) "نهاري" else "Light")} • ${themeConfig.appTier.localizedLabel(isArabic)} • ${themeConfig.temperatureUnit.symbol}",
                                icon = Icons.Default.Palette,
                                expanded = expandColorsIdentity,
                                onToggle = {
                                    CyberHapticsEngine.triggerTactileTick(context)
                                    expandColorsIdentity = !expandColorsIdentity
                                },
                                accentColor = accentColor,
                                surfaceColor = softTileColor.copy(alpha = 0.62f),
                                borderColor = subtleBorder,
                                textColor = textColor,
                                subTextColor = subTextColor
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    // Day / Night Mode Row
                                    DrawerToggleRow(
                                        title = if (isArabic) {
                                            if (isDark) "الوضع الليلي" else "الوضع النهاري"
                                        } else {
                                            if (isDark) "Dark Mode" else "Light Mode"
                                        },
                                        subtitle = if (isArabic) {
                                            if (isDark) "تبديل إلى المظهر النهاري المريح" else "تبديل إلى المظهر الليلي الهادئ"
                                        } else {
                                            if (isDark) "Switch to soft daylight surface" else "Switch to quiet dark surface"
                                        },
                                        checked = isDark,
                                        onCheckedChange = { onConfigChange(themeConfig.copy(isDarkMode = it)) },
                                        accentColor = accentColor,
                                        textColor = textColor,
                                        subTextColor = subTextColor
                                    )

                                    // Soft Eye-Friendly Color Orbs
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = if (isArabic) "لون التمييز المريح للعين" else "Eye-Comfort Accent Color",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = subTextColor
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            val orbItems = if (isArabic) {
                                                listOf(
                                                    Triple("أزرق هادئ", Color(0xFF4F7DF3), 0),
                                                    Triple("زمردي", Color(0xFF1BBF75), 1),
                                                    Triple("مرجاني", Color(0xFFF06449), 2),
                                                    Triple("بنفسجي", Color(0xFF8B6EF3), 3)
                                                )
                                            } else {
                                                listOf(
                                                    Triple("Cobalt", Color(0xFF4F7DF3), 0),
                                                    Triple("Emerald", Color(0xFF1BBF75), 1),
                                                    Triple("Coral", Color(0xFFF06449), 2),
                                                    Triple("Violet", Color(0xFF8B6EF3), 3)
                                                )
                                            }
                                            orbItems.forEach { (label, color, idx) ->
                                                val isSelected = themeConfig.accentColorIndex == idx && themeConfig.customAccentHex == null
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .clickable {
                                                            CyberHapticsEngine.triggerTactileTick(context)
                                                            onConfigChange(
                                                                themeConfig.copy(
                                                                    accentColorIndex = idx,
                                                                    customAccentHex = null
                                                                )
                                                            )
                                                        }
                                                        .padding(vertical = 6.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .clip(CircleShape)
                                                            .background(color)
                                                            .border(
                                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                                color = if (isSelected) textColor else Color.White.copy(alpha = 0.25f),
                                                                shape = CircleShape
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isSelected) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = label,
                                                                tint = Color.White,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = label,
                                                        fontFamily = PlusJakartaSansFontFamily,
                                                        fontSize = 10.sp,
                                                        color = if (isSelected) textColor else subTextColor,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Tier Pills
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = if (isArabic) "فئة الحساب والشارة" else "Account Tier & Badge",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = subTextColor
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            com.example.data.AppTier.values().forEach { tierOption ->
                                                val isLockedPremium = tierOption == com.example.data.AppTier.PREMIUM && !themeConfig.isPremiumUnlocked
                                                val isSelected = themeConfig.appTier == tierOption && !isLockedPremium
                                                val tColor = tierOption.displayColor(isDark)
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 48.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(
                                                            if (isSelected) tColor.copy(alpha = if (isDark) 0.22f else 0.14f)
                                                            else surfaceCardColor
                                                        )
                                                        .border(
                                                            1.dp,
                                                            if (isSelected) tColor.copy(alpha = 0.7f) else subtleBorder,
                                                            RoundedCornerShape(12.dp)
                                                        )
                                                        .clickable {
                                                            CyberHapticsEngine.triggerTactileTick(context)
                                                            if (isLockedPremium) {
                                                                onClose()
                                                                onOpenRedeemDialog()
                                                            } else {
                                                                onConfigChange(themeConfig.copy(appTier = tierOption))
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center
                                                    ) {
                                                        if (isLockedPremium) {
                                                            Icon(
                                                                imageVector = Icons.Default.Lock,
                                                                contentDescription = null,
                                                                tint = tColor,
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                        }
                                                        Text(
                                                            text = tierOption.localizedLabel(isArabic),
                                                            fontFamily = OutfitFontFamily,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.5.sp,
                                                            color = if (isSelected || isLockedPremium) tColor else subTextColor
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        if (themeConfig.isPremiumUnlocked) {
                                            val goldTint = com.example.data.AppTier.PREMIUM.displayColor(isDark)
                                            Text(
                                                text = if (isArabic) "وضع البريميوم مفعّل بالكامل ✓" else "PREMIUM Mode Unlocked ✓",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 10.5.sp,
                                                color = goldTint
                                            )
                                        }
                                    }

                                    // Temperature Scale Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isArabic) "وحدة الحرارة" else "Temperature Unit",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.5.sp,
                                            color = textColor
                                        )
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(surfaceCardColor)
                                                .padding(3.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            listOf(
                                                TemperatureUnit.CELSIUS to "°C",
                                                TemperatureUnit.FAHRENHEIT to "°F"
                                            ).forEach { (unit, label) ->
                                                val selected = themeConfig.temperatureUnit == unit
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (selected) accentColor.copy(alpha = 0.22f) else Color.Transparent)
                                                        .clickable {
                                                            CyberHapticsEngine.triggerTactileTick(context)
                                                            onConfigChange(themeConfig.copy(temperatureUnit = unit))
                                                        }
                                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = label,
                                                        fontFamily = OutfitFontFamily,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = if (selected) accentColor else subTextColor
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Liquid Droplet Visibility Toggle
                                    DrawerToggleRow(
                                        title = if (isArabic) "قطرة الزجاج السائل" else "Liquid Glass Droplet",
                                        subtitle = if (isArabic) "إظهار المؤشر العائم في شريط التنقل السفلي" else "Show floating indicator in bottom nav bar",
                                        checked = themeConfig.showLiquidDroplet,
                                        onCheckedChange = { onConfigChange(themeConfig.copy(showLiquidDroplet = it)) },
                                        accentColor = accentColor,
                                        textColor = textColor,
                                        subTextColor = subTextColor
                                    )

                                    // Droplet Drag Navigation Behavior Toggle (Disabled by default)
                                    DrawerToggleRow(
                                        title = if (isArabic) "التنقل بسحب القطرة" else "Droplet Drag Navigation",
                                        subtitle = if (isArabic) {
                                            if (themeConfig.dropletDragNavigationEnabled) {
                                                "مفعّل: سحب القطرة يميناً ويساراً يبدّل الصفحات"
                                            } else {
                                                "مقفول: سحب القطرة في البانر لا يبدّل الصفحات (النقر فقط)"
                                            }
                                        } else {
                                            if (themeConfig.dropletDragNavigationEnabled) {
                                                "Enabled: Dragging droplet left/right switches tabs"
                                            } else {
                                                "Locked: Dragging droplet stays in place (tap only)"
                                            }
                                        },
                                        checked = themeConfig.dropletDragNavigationEnabled,
                                        onCheckedChange = {
                                            onConfigChange(themeConfig.copy(dropletDragNavigationEnabled = it))
                                        },
                                        accentColor = accentColor,
                                        textColor = textColor,
                                        subTextColor = subTextColor,
                                        testTag = "droplet_drag_nav_switch"
                                    )
                                }
                            }
                        }
                    }

                    // 3. GRAPHICS SECTION: Simple Mode, Pro Mode (1ms Sync + Classic Comfort), and 120Hz Refresh
                    SettingsAccordionCard(
                        title = if (isArabic) "الرسوميات والأداء" else "Graphics & Engine",
                        summary = "${themeConfig.graphicsMode.localizedTitle(isArabic)} • ${if (themeConfig.target120FpsMode) "120Hz" else (if (isArabic) "قياسي" else "Standard")}",
                        icon = Icons.Default.DisplaySettings,
                        expanded = expandGraphics,
                        onToggle = {
                            CyberHapticsEngine.triggerTactileTick(context)
                            expandGraphics = !expandGraphics
                        },
                        accentColor = TactileCyan,
                        surfaceColor = surfaceCardColor,
                        borderColor = subtleBorder,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        testTag = "settings_graphics_section"
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // 3a. Simple Mode Toggle Card
                            val isSimpleSelected = themeConfig.graphicsMode == GraphicsMode.SIMPLE
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 58.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSimpleSelected) accentColor.copy(alpha = if (isDark) 0.18f else 0.12f)
                                        else softTileColor.copy(alpha = 0.65f)
                                    )
                                    .border(
                                        1.1.dp,
                                        if (isSimpleSelected) accentColor.copy(alpha = 0.65f) else subtleBorder,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        CyberHapticsEngine.triggerTactileTick(context)
                                        val nextMode = if (isSimpleSelected) GraphicsMode.STANDARD else GraphicsMode.SIMPLE
                                        onConfigChange(
                                            themeConfig.copy(
                                                graphicsMode = nextMode,
                                                samplingIntervalMs = 1000L
                                            )
                                        )
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("simple_mode_card"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isArabic) "الوضع البسيط" else "Simple Mode",
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = textColor
                                    )
                                    Text(
                                        text = if (isArabic) {
                                            "مظهر عادي وهادئ يميل إلى البساطة بدون ألوان زاهية أو حركات لامعة"
                                        } else {
                                            "Clean minimalist surface with muted tones and zero distracting animations"
                                        },
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 10.sp,
                                        color = subTextColor,
                                        lineHeight = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = isSimpleSelected,
                                    onCheckedChange = { checked ->
                                        CyberHapticsEngine.triggerTactileTick(context)
                                        onConfigChange(
                                            themeConfig.copy(
                                                graphicsMode = if (checked) GraphicsMode.SIMPLE else GraphicsMode.STANDARD,
                                                samplingIntervalMs = 1000L
                                            )
                                        )
                                    },
                                    modifier = Modifier.testTag("simple_mode_switch"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = accentColor,
                                        uncheckedThumbColor = subTextColor,
                                        uncheckedTrackColor = Color(0x3394A3B8)
                                    )
                                )
                            }

                            // 3b. Pro Mode Toggle Card (Classic Luxury Eye-Comfort + 1ms Ultra-Fast Sync)
                            val isProSelected = themeConfig.graphicsMode == GraphicsMode.PRO
                            val proGoldAccent = if (isDark) Color(0xFF6C9BCF) else Color(0xFF2E5B88)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isProSelected) proGoldAccent.copy(alpha = if (isDark) 0.22f else 0.13f)
                                        else softTileColor.copy(alpha = 0.65f)
                                    )
                                    .border(
                                        1.1.dp,
                                        if (isProSelected) proGoldAccent.copy(alpha = 0.72f) else subtleBorder,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        CyberHapticsEngine.triggerTactileTick(context)
                                        val nextMode = if (isProSelected) GraphicsMode.STANDARD else GraphicsMode.PRO
                                        onConfigChange(
                                            themeConfig.copy(
                                                graphicsMode = nextMode,
                                                samplingIntervalMs = if (nextMode == GraphicsMode.PRO) 1L else 1000L
                                            )
                                        )
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("pro_mode_card"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (isArabic) "وضع المحترفين" else "Pro Mode",
                                            fontFamily = OutfitFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = textColor
                                        )
                                        if (isProSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(proGoldAccent.copy(alpha = 0.22f))
                                                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                            ) {
                                                Text(
                                                    text = if (isArabic) "تزامن 1ms" else "1ms SYNC",
                                                    fontFamily = OutfitFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp,
                                                    color = proGoldAccent
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = if (isArabic) {
                                            "مظهر كلاسيكي فخم ناعم ومريح للعين في كل الأوضاع + رفع تزامن القياسات إلى 1 جزء من الثانية"
                                        } else {
                                            "Classic executive eye-comfort finish + 1ms high-frequency sensor synchronization"
                                        },
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 10.sp,
                                        color = subTextColor,
                                        lineHeight = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = isProSelected,
                                    onCheckedChange = { checked ->
                                        CyberHapticsEngine.triggerTactileTick(context)
                                        val nextMode = if (checked) GraphicsMode.PRO else GraphicsMode.STANDARD
                                        onConfigChange(
                                            themeConfig.copy(
                                                graphicsMode = nextMode,
                                                samplingIntervalMs = if (checked) 1L else 1000L
                                            )
                                        )
                                    },
                                    modifier = Modifier.testTag("pro_mode_switch"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = proGoldAccent,
                                        uncheckedThumbColor = subTextColor,
                                        uncheckedTrackColor = Color(0x3394A3B8)
                                    )
                                )
                            }

                            // 3c. 120Hz Smoothness moved underneath Simple & Pro inside Graphics
                            DrawerToggleRow(
                                title = if (isArabic) "سلاسة 120Hz" else "120Hz Smoothness",
                                subtitle = if (isArabic) {
                                    if (themeConfig.target120FpsMode) "مفعّل: سلاسة حركة فائقة 120 إطار" else "قياسي: استجابة موفرة للطاقة"
                                } else {
                                    if (themeConfig.target120FpsMode) "Enabled: Ultra-fluid 120 FPS motion" else "Standard: Power-saving frame rate"
                                },
                                checked = themeConfig.target120FpsMode,
                                onCheckedChange = {
                                    CyberHapticsEngine.triggerTactileTick(context)
                                    onConfigChange(themeConfig.copy(target120FpsMode = it))
                                },
                                accentColor = TactileCyan,
                                textColor = textColor,
                                subTextColor = subTextColor,
                                testTag = "fps_120_toggle_row"
                            )
                        }
                    }

                    // 4. NOTIFICATIONS SECTION: Sounds, Vibration & Battery Protection Alerts
                    val activeNotifCount = listOf(
                        themeConfig.soundEffectsEnabled,
                        themeConfig.hapticFeedbackEnabled,
                        themeConfig.backgroundServiceEnabled,
                        themeConfig.alertOn80Percent,
                        themeConfig.alertOnHighTemp
                    ).count { it }

                    SettingsAccordionCard(
                        title = if (isArabic) "التنبيهات" else "Notifications",
                        summary = if (isArabic) "$activeNotifCount من 5 مفعّلة" else "$activeNotifCount of 5 active",
                        icon = Icons.Default.Notifications,
                        expanded = expandNotifications,
                        onToggle = {
                            CyberHapticsEngine.triggerTactileTick(context)
                            expandNotifications = !expandNotifications
                        },
                        accentColor = TactileGreen,
                        surfaceColor = surfaceCardColor,
                        borderColor = subtleBorder,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        testTag = "settings_notifications_section"
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            DrawerToggleRow(
                                title = if (isArabic) "الاهتزاز" else "Haptic Vibration",
                                subtitle = if (isArabic) {
                                    if (themeConfig.hapticFeedbackEnabled) "استجابة لمسية تفاعلية مفعّلة" else "الاهتزاز معطّل"
                                } else {
                                    if (themeConfig.hapticFeedbackEnabled) "Tactile haptic feedback enabled" else "Vibration muted"
                                },
                                checked = themeConfig.hapticFeedbackEnabled,
                                onCheckedChange = {
                                    onConfigChange(themeConfig.copy(hapticFeedbackEnabled = it))
                                    if (it) CyberHapticsEngine.triggerTactileTick(context)
                                },
                                accentColor = TactileGreen,
                                textColor = textColor,
                                subTextColor = subTextColor,
                                testTag = "notifications_vibration_switch"
                            )
                            DrawerToggleRow(
                                title = if (isArabic) "الأصوات" else "Sound Effects",
                                subtitle = if (isArabic) {
                                    if (themeConfig.soundEffectsEnabled) "مؤثرات صوتية تفاعلية مفعّلة" else "الوضع الصامت مفعّل"
                                } else {
                                    if (themeConfig.soundEffectsEnabled) "Interactive acoustic feedback enabled" else "Silent mode enabled"
                                },
                                checked = themeConfig.soundEffectsEnabled,
                                onCheckedChange = {
                                    CyberHapticsEngine.triggerTactileTick(context)
                                    onConfigChange(themeConfig.copy(soundEffectsEnabled = it))
                                },
                                accentColor = TactileViolet,
                                textColor = textColor,
                                subTextColor = subTextColor,
                                testTag = "notifications_sound_switch"
                            )
                            DrawerToggleRow(
                                title = if (isArabic) "خدمة القياس في الخلفية 24/7" else "24/7 Background Telemetry",
                                subtitle = if (isArabic) "رصد استقرار الشحن والبطارية باستمرار" else "Continuous charge stability & battery tracking",
                                checked = themeConfig.backgroundServiceEnabled,
                                onCheckedChange = { onConfigChange(themeConfig.copy(backgroundServiceEnabled = it)) },
                                accentColor = accentColor,
                                textColor = textColor,
                                subTextColor = subTextColor
                            )
                            DrawerToggleRow(
                                title = if (isArabic) "تنبيه حماية البطارية 80%" else "80% Cell Protection Alert",
                                subtitle = if (isArabic) "إشعار لطيف لإطالة عمر خلايا الليثيوم" else "Gentle chime to extend lithium cell longevity",
                                checked = themeConfig.alertOn80Percent,
                                onCheckedChange = { onConfigChange(themeConfig.copy(alertOn80Percent = it)) },
                                accentColor = accentColor,
                                textColor = textColor,
                                subTextColor = subTextColor
                            )
                            DrawerToggleRow(
                                title = if (isArabic) "تحذير الحرارة المرتفعة (>42°C)" else "Thermal Stress Warning (>42°C)",
                                subtitle = if (isArabic) "تنبيه فوري عند ارتفاع حرارة المستشعر" else "Immediate alert when cell exceeds JEITA ceiling",
                                checked = themeConfig.alertOnHighTemp,
                                onCheckedChange = { onConfigChange(themeConfig.copy(alertOnHighTemp = it)) },
                                accentColor = accentColor,
                                textColor = textColor,
                                subTextColor = subTextColor
                            )
                        }
                    }

                    // 5. Quick Premium Redeem Banner (shown cleanly when Premium is locked)
                    if (!themeConfig.isPremiumUnlocked) {
                        val goldColor = if (isDark) Color(0xFFFFD700) else Color(0xFFB77900)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 54.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            goldColor.copy(alpha = if (isDark) 0.20f else 0.13f),
                                            surfaceCardColor
                                        )
                                    )
                                )
                                .border(1.1.dp, goldColor.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                                .clickable {
                                    CyberHapticsEngine.triggerTactileTick(context)
                                    onClose()
                                    onOpenRedeemDialog()
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("drawer_redeem_premium_button")
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
                                            .background(goldColor.copy(alpha = if (isDark) 0.22f else 0.14f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VpnKey,
                                            contentDescription = null,
                                            tint = goldColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (isArabic) "تفعيل وضع البريميوم برمز التفعيل" else "Unlock PREMIUM via Redeem Code",
                                            fontFamily = OutfitFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = textColor
                                        )
                                        Text(
                                            text = if (isArabic) "أدخل رمز التفعيل لفتح المزايا الذهبية" else "Enter activation code to unlock gold features",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 10.sp,
                                            color = goldColor
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = goldColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // 6. Expandable Accordion: Auto-Update (collapsed by default)
                    SettingsAccordionCard(
                        title = if (isArabic) "التحديث التلقائي" else "Auto-Update",
                        summary = "v${updateUiState.currentVersionName} • ${if (themeConfig.autoUpdateEnabled) (if (isArabic) "تلقائي" else "Auto") else (if (isArabic) "يدوي" else "Manual")}",
                        icon = Icons.Default.SystemUpdate,
                        expanded = expandAutoUpdate,
                        onToggle = {
                            CyberHapticsEngine.triggerTactileTick(context)
                            expandAutoUpdate = !expandAutoUpdate
                        },
                        accentColor = accentColor,
                        surfaceColor = surfaceCardColor,
                        borderColor = subtleBorder,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        testTag = "settings_auto_update_section"
                    ) {
                        LuxuriousAutoUpdateStatusCard(
                            updateUiState = updateUiState,
                            autoUpdateEnabled = themeConfig.autoUpdateEnabled,
                            isArabic = isArabic,
                            onToggleAutoUpdate = { enabled ->
                                CyberHapticsEngine.triggerTactileTick(context)
                                onConfigChange(themeConfig.copy(autoUpdateEnabled = enabled))
                            },
                            accentColor = accentColor,
                            isDark = isDark,
                            textColor = textColor,
                            onCheckUpdates = onCheckUpdates,
                            onTriggerUpdate = onTriggerUpdate,
                            onCancelDownload = onCancelDownload,
                            onOpenUpdateDialog = {
                                onClose()
                                onOpenUpdateDialog()
                            }
                        )
                    }

                    // 7. Expandable Accordion: Sensor Calibration
                    SettingsAccordionCard(
                        title = if (isArabic) "معايرة مقياس التيار" else "Current Calibration",
                        summary = "${String.format(Locale.US, "%.2fx", themeConfig.currentMultiplier)} • ${themeConfig.currentZeroOffsetMa.toInt()} mA",
                        icon = Icons.Default.Tune,
                        expanded = expandCalibration,
                        onToggle = {
                            CyberHapticsEngine.triggerTactileTick(context)
                            expandCalibration = !expandCalibration
                        },
                        accentColor = TactileCyan,
                        surfaceColor = surfaceCardColor,
                        borderColor = subtleBorder,
                        textColor = textColor,
                        subTextColor = subTextColor
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isArabic) "معامل الضبط (Gain)" else "Gain Multiplier",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = textColor
                                )
                                Text(
                                    text = String.format(Locale.US, "%.2fx", themeConfig.currentMultiplier),
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = accentColor
                                )
                            }
                            Slider(
                                value = themeConfig.currentMultiplier,
                                onValueChange = {
                                    onConfigChange(themeConfig.copy(currentMultiplier = ((it * 100).toInt() / 100f)))
                                },
                                valueRange = 0.70f..1.40f,
                                steps = 14,
                                colors = SliderDefaults.colors(
                                    thumbColor = accentColor,
                                    activeTrackColor = accentColor,
                                    inactiveTrackColor = softTileColor
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isArabic) "معايرة الصفر (Offset)" else "Zero Offset",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = textColor
                                )
                                Text(
                                    text = "${if (themeConfig.currentZeroOffsetMa >= 0) "+" else ""}${themeConfig.currentZeroOffsetMa.toInt()} mA",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = accentColor
                                )
                            }
                            Slider(
                                value = themeConfig.currentZeroOffsetMa,
                                onValueChange = {
                                    onConfigChange(themeConfig.copy(currentZeroOffsetMa = it.toInt().toFloat()))
                                },
                                valueRange = -150f..150f,
                                steps = 15,
                                colors = SliderDefaults.colors(
                                    thumbColor = accentColor,
                                    activeTrackColor = accentColor,
                                    inactiveTrackColor = softTileColor
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Box(
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(accentColor.copy(alpha = if (isDark) 0.18f else 0.10f))
                                        .border(1.dp, accentColor.copy(alpha = 0.40f), RoundedCornerShape(12.dp))
                                        .clickable {
                                            CyberHapticsEngine.triggerTactileTick(context)
                                            onConfigChange(
                                                themeConfig.copy(
                                                    currentMultiplier = 1.0f,
                                                    currentZeroOffsetMa = 0.0f
                                                )
                                            )
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isArabic) "إعادة ضبط المصنع" else "Factory Reset",
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = accentColor
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
    }
}


@Composable
private fun QuickControlTile(
    title: String,
    status: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    accentColor: Color,
    surfaceColor: Color,
    softColor: Color,
    borderColor: Color,
    textColor: Color,
    subTextColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (active) accentColor.copy(alpha = 0.14f) else surfaceColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "quickTileBg"
    )
    val strokeColor by animateColorAsState(
        targetValue = if (active) accentColor.copy(alpha = 0.55f) else borderColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "quickTileBorder"
    )

    Box(
        modifier = modifier
            .heightIn(min = 68.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .border(1.1.dp, strokeColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (active) accentColor.copy(alpha = 0.20f) else softColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (active) accentColor else subTextColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (active) accentColor else subTextColor.copy(alpha = 0.35f))
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = status,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 10.sp,
                color = if (active) accentColor else subTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingsAccordionCard(
    title: String,
    summary: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    accentColor: Color,
    surfaceColor: Color,
    borderColor: Color,
    textColor: Color,
    subTextColor: Color,
    testTag: String? = null,
    content: @Composable () -> Unit
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "accordionArrow"
    )
    val activeBorder by animateColorAsState(
        targetValue = if (expanded) accentColor.copy(alpha = 0.45f) else borderColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "accordionBorder"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .clip(RoundedCornerShape(20.dp))
            .background(surfaceColor)
            .border(1.1.dp, activeBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(onClick = onToggle),
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
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = summary,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 10.5.sp,
                        color = subTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = subTextColor,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = arrowRotation }
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun DrawerToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color,
    textColor: Color,
    subTextColor: Color = TextSlate,
    testTag: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable { onCheckedChange(!checked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.5.sp,
                color = textColor
            )
            Text(
                text = subtitle,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 10.sp,
                color = subTextColor,
                lineHeight = 14.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = subTextColor,
                uncheckedTrackColor = Color(0x3394A3B8)
            )
        )
    }
}

/**
 * Luxurious, breathing, smooth & dynamic In-App Auto-Update Status container.
 * Features breathing glass aura, sweeping specular sheen, live status orb,
 * version pill, spring progress indicator, and direct in-app download/install actions.
 */
@Composable
private fun LuxuriousAutoUpdateStatusCard(
    updateUiState: InAppUpdateUiState,
    autoUpdateEnabled: Boolean,
    isArabic: Boolean,
    onToggleAutoUpdate: (Boolean) -> Unit,
    accentColor: Color,
    isDark: Boolean,
    textColor: Color,
    onCheckUpdates: () -> Unit,
    onTriggerUpdate: () -> Unit,
    onCancelDownload: () -> Unit,
    onOpenUpdateDialog: () -> Unit
) {
    val context = LocalContext.current

    val targetStatusColor = when {
        updateUiState.isDownloadedReadyToInstall && !updateUiState.needsInstallPermission -> TactileGreen
        updateUiState.needsInstallPermission -> TactileAmber
        updateUiState.isDownloading -> accentColor
        updateUiState.isChecking -> TactileCyan
        updateUiState.updateAvailable -> TactileAmber
        else -> TactileGreen
    }
    val statusColor by animateColorAsState(
        targetValue = targetStatusColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "updateCardStatusColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "luxuriousUpdateCardAnim")
    val breathPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cardBreathPhase"
    )
    val sheenSweep by infiniteTransition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cardSheenSweep"
    )
    val animatedDownloadProgress by animateFloatAsState(
        targetValue = updateUiState.downloadProgress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "drawerDownloadProgress"
    )

    val cardBaseBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF1B2436),
                Color(0xFF141C2B)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFECF0F6),
                Color(0xFFE0E7F0)
            )
        )
    }

    val breathingBorderBrush = Brush.linearGradient(
        colors = listOf(
            statusColor.copy(alpha = 0.28f + 0.42f * breathPhase),
            accentColor.copy(alpha = 0.16f + 0.28f * (1f - breathPhase)),
            statusColor.copy(alpha = 0.22f + 0.34f * breathPhase)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("auto_update_status_card")
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.80f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .clip(RoundedCornerShape(20.dp))
            .background(cardBaseBrush)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            statusColor.copy(alpha = (if (isDark) 0.22f else 0.12f) * (0.55f + 0.45f * breathPhase)),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.86f, size.height * 0.20f),
                        radius = size.width * (0.44f + 0.14f * breathPhase)
                    ),
                    radius = size.width * 0.6f,
                    center = Offset(size.width * 0.86f, size.height * 0.20f)
                )

                val sweepStart = Offset(size.width * sheenSweep - size.width * 0.3f, 0f)
                val sweepEnd = Offset(size.width * sheenSweep + size.width * 0.25f, size.height)
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFF3F6FA).copy(alpha = if (isDark) 0.045f else 0.20f),
                            Color.Transparent
                        ),
                        start = sweepStart,
                        end = sweepEnd
                    ),
                    cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                )
            }
            .border(1.35.dp, breathingBorderBrush, RoundedCornerShape(20.dp))
            .padding(15.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Header: Breathing Status Orb + Title + Version Capsule
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
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .graphicsLayer {
                                    val s = 0.88f + 0.24f * breathPhase
                                    scaleX = s
                                    scaleY = s
                                    alpha = 0.55f - 0.30f * breathPhase
                                }
                                .clip(CircleShape)
                                .background(statusColor.copy(alpha = 0.35f))
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(statusColor.copy(alpha = if (isDark) 0.24f else 0.15f))
                                .border(1.dp, statusColor.copy(alpha = 0.65f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (updateUiState.isDownloadedReadyToInstall) {
                                    Icons.Default.DownloadDone
                                } else {
                                    Icons.Default.SystemUpdate
                                },
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "حالة التحديث التلقائي" else "Auto-Update Status",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isArabic) {
                                when {
                                    updateUiState.isDownloading -> "تنزيل مباشر داخل التطبيق"
                                    updateUiState.isDownloadedReadyToInstall -> "الحزمة جاهزة للتثبيت الفوري"
                                    updateUiState.isChecking -> "جاري الفحص السحابي..."
                                    updateUiState.updateAvailable -> "تحديث جديد متوفر للتحميل"
                                    else -> "تحديث سحابي آمن ومباشر"
                                }
                            } else {
                                when {
                                    updateUiState.isDownloading -> "Direct in-app download"
                                    updateUiState.isDownloadedReadyToInstall -> "Package ready to install"
                                    updateUiState.isChecking -> "Checking cloud release..."
                                    updateUiState.updateAvailable -> "New update ready to download"
                                    else -> "Secure direct OTA engine"
                                }
                            },
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 10.sp,
                            color = statusColor,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Compact Auto-Update Toggle Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (autoUpdateEnabled) {
                                    accentColor.copy(alpha = if (isDark) 0.22f else 0.12f)
                                } else {
                                    if (isDark) Color(0xFF232F45) else Color(0xFFD2DBE6)
                                }
                            )
                            .border(
                                1.dp,
                                if (autoUpdateEnabled) accentColor.copy(alpha = 0.50f) else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onToggleAutoUpdate(!autoUpdateEnabled) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (autoUpdateEnabled) (if (isArabic) "تلقائي" else "Auto") else (if (isArabic) "يدوي" else "Manual"),
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = if (autoUpdateEnabled) accentColor else TextSlate
                        )
                    }

                    // Dynamic Version Pill
                    val versionLabel = if (updateUiState.updateAvailable && !updateUiState.availableVersionName.isNullOrBlank()) {
                        "v${updateUiState.currentVersionName} → v${updateUiState.availableVersionName}"
                    } else {
                        "v${updateUiState.currentVersionName}"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(statusColor.copy(alpha = if (isDark) 0.18f else 0.12f))
                            .border(1.dp, statusColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = versionLabel,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            AnimatedContent(
                targetState = updateUiState.lastCheckSummary,
                transitionSpec = { fadeIn(tween(220)).togetherWith(fadeOut(tween(180))) },
                label = "updateSummaryTransition"
            ) { summary ->
                Text(
                    text = summary,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 11.sp,
                    color = if (isDark) Color(0xFFCBD5E1) else TextSlate,
                    lineHeight = 16.sp
                )
            }

            // Live In-App Download Progress Section inside the Drawer Card
            AnimatedVisibility(
                visible = updateUiState.isDownloading,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { animatedDownloadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = statusColor,
                        trackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFCED8E5),
                        strokeCap = StrokeCap.Round
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val pct = (animatedDownloadProgress * 100).toInt()
                        Text(
                            text = if (updateUiState.totalMb > 0f) {
                                String.format(
                                    Locale.US,
                                    "%.1f / %.1f MB",
                                    updateUiState.downloadedMb,
                                    updateUiState.totalMb
                                )
                            } else {
                                String.format(Locale.US, "%.1f MB", updateUiState.downloadedMb)
                            },
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 10.5.sp,
                            color = TextSlate
                        )
                        Text(
                            text = "$pct%",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Row with >= 48dp accessible hit targets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val primaryLabel = if (isArabic) {
                    when {
                        updateUiState.needsInstallPermission -> "منح الإذن والتثبيت"
                        updateUiState.isDownloadedReadyToInstall -> "تثبيت الآن"
                        updateUiState.isDownloading -> "إلغاء التنزيل"
                        updateUiState.isChecking -> "جاري الفحص..."
                        updateUiState.updateAvailable -> "تحديث داخلي"
                        else -> "فحص التحديثات"
                    }
                } else {
                    when {
                        updateUiState.needsInstallPermission -> "Allow & Install"
                        updateUiState.isDownloadedReadyToInstall -> "Install Now"
                        updateUiState.isDownloading -> "Cancel Download"
                        updateUiState.isChecking -> "Checking..."
                        updateUiState.updateAvailable -> "Update Now"
                        else -> "Check Updates"
                    }
                }

                val primaryBg = when {
                    updateUiState.isDownloading -> if (isDark) Color(0xFF334155) else Color(0xFFD2DBE6)
                    updateUiState.isDownloadedReadyToInstall -> TactileGreen
                    updateUiState.updateAvailable -> statusColor
                    else -> accentColor
                }

                val primaryTextColor = if (updateUiState.isDownloading) {
                    if (isDark) TextWhite else TextCharcoal
                } else {
                    Color(0xFFF2F6FA)
                }

                Box(
                    modifier = Modifier
                        .weight(1.15f)
                        .heightIn(min = 48.dp)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(primaryBg)
                        .clickable(enabled = !updateUiState.isChecking) {
                            CyberHapticsEngine.triggerTactileTick(context)
                            when {
                                updateUiState.isDownloading -> onCancelDownload()
                                updateUiState.isDownloadedReadyToInstall || updateUiState.updateAvailable -> onTriggerUpdate()
                                else -> onCheckUpdates()
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 11.dp)
                        .testTag("check_updates_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = primaryLabel,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = primaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(0.85f)
                        .heightIn(min = 48.dp)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor.copy(alpha = if (isDark) 0.16f else 0.10f))
                        .border(1.dp, statusColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .clickable {
                            CyberHapticsEngine.triggerTactileTick(context)
                            onOpenUpdateDialog()
                        }
                        .padding(horizontal = 8.dp, vertical = 11.dp)
                        .testTag("open_update_dialog_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isArabic) "نافذة التحديث" else "Update Details",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = statusColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Centered Luxurious Kinetic Modal Dialog for unlocking PREMIUM tier via Redeem Code.
 */
@Composable
fun PremiumRedeemDialog(
    isOpen: Boolean,
    themeConfig: GlassThemeConfig,
    onDismiss: () -> Unit,
    onRedeemCode: (String) -> Boolean
) {
    if (!isOpen) return

    BackHandler { onDismiss() }

    val context = LocalContext.current
    val isDark = themeConfig.isDarkMode
    val isArabic = themeConfig.appLanguage.isArabic
    val goldAccent = com.example.data.AppTier.PREMIUM.displayColor(isDark)
    val textColor = if (isDark) Color(0xFFE9EFF8) else TextCharcoal
    val subTextColor = if (isDark) Color(0xFF9FB0C7) else TextSlate
    val cardBrush = if (isDark) {
        Brush.verticalGradient(colors = listOf(Color(0xF7182235), Color(0xFA101726)))
    } else {
        Brush.verticalGradient(colors = listOf(Color(0xFCEDF1F7), Color(0xFCE0E7F0)))
    }

    var codeInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var isUnlockedJustNow by remember { mutableStateOf(themeConfig.isPremiumUnlocked) }

    LaunchedEffect(isOpen) {
        if (isOpen) {
            isError = false
            isUnlockedJustNow = themeConfig.isPremiumUnlocked
            if (!themeConfig.isPremiumUnlocked) {
                codeInput = ""
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(cardBrush)
                    .border(
                        1.4.dp,
                        goldAccent.copy(alpha = 0.55f),
                        RoundedCornerShape(28.dp)
                    )
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(
                            (if (isUnlockedJustNow) TactileGreen else goldAccent)
                                .copy(alpha = if (isDark) 0.22f else 0.14f)
                        )
                        .border(
                            1.5.dp,
                            (if (isUnlockedJustNow) TactileGreen else goldAccent).copy(alpha = 0.7f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isUnlockedJustNow) Icons.Default.Check else Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = if (isUnlockedJustNow) TactileGreen else goldAccent,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isArabic) {
                        if (isUnlockedJustNow) "تم تفعيل وضع البريميوم!" else "تفعيل وضع البريميوم"
                    } else {
                        if (isUnlockedJustNow) "PREMIUM Unlocked!" else "Unlock PREMIUM"
                    },
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = textColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isArabic) {
                        if (isUnlockedJustNow) {
                            "أصبحت تتمتع الآن بشارة البريميوم الذهبية ومحرك الطاقة الدقيق بشكل دائم."
                        } else {
                            "أدخل رمز التفعيل الخاص بك لفتح وضع البريميوم الذهبي."
                        }
                    } else {
                        if (isUnlockedJustNow) {
                            "You now have permanent access to the gold PREMIUM badge and precision engine."
                        } else {
                            "Enter your activation redeem code to unlock the gold PREMIUM tier."
                        }
                    },
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.5.sp,
                    color = subTextColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                if (!isUnlockedJustNow) {
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = {
                            codeInput = it
                            if (isError) isError = false
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("redeem_code_input"),
                        placeholder = {
                            Text(
                                text = if (isArabic) "أدخل رمز التفعيل هنا..." else "Enter activation code...",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 13.sp,
                                color = subTextColor.copy(alpha = 0.75f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = if (isError) TactileCoralRed else goldAccent
                            )
                        },
                        isError = isError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isError) TactileCoralRed else goldAccent,
                            unfocusedBorderColor = if (isError) TactileCoralRed else if (isDark) Color(0xFF334155) else Color(0x99BECAD8),
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedContainerColor = if (isDark) Color(0xFF131B2B) else Color(0xFFDCE3EC),
                            unfocusedContainerColor = if (isDark) Color(0xFF131B2B) else Color(0xFFDCE3EC)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    if (isError) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isArabic) {
                                "الرمز غير صحيح، يرجى التحقق من الكتابة والمحاولة مجدداً."
                            } else {
                                "Invalid redeem code. Please check spelling and try again."
                            },
                            fontFamily = PlusJakartaSansFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.5.sp,
                            color = TactileCoralRed,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(0.42f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFD2DBE6))
                                .clickable {
                                    CyberHapticsEngine.triggerTactileTick(context)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArabic) "إلغاء" else "Cancel",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = subTextColor
                            )
                        }

                        Button(
                            onClick = {
                                val ok = onRedeemCode(codeInput)
                                if (ok) {
                                    isUnlockedJustNow = true
                                    isError = false
                                } else {
                                    CyberHapticsEngine.triggerThermalWarning(context)
                                    isError = true
                                }
                            },
                            modifier = Modifier
                                .weight(0.58f)
                                .heightIn(min = 48.dp)
                                .testTag("redeem_code_submit_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = goldAccent,
                                contentColor = Color(0xFFF2F6FA)
                            )
                        ) {
                            Text(
                                text = if (isArabic) "تفعيل البريميوم" else "Activate Premium",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFF2F6FA)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = {
                            CyberHapticsEngine.triggerTactileTick(context)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TactileGreen,
                            contentColor = Color(0xFFF2F6FA)
                        )
                    ) {
                        Text(
                            text = if (isArabic) "رائع! متابعة" else "Awesome! Continue",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFF2F6FA)
                        )
                    }
                }
            }
        }
    }
}
