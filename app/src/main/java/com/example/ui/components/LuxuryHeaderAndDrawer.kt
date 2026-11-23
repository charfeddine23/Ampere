package com.example.ui.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
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
import com.example.BuildConfig
import com.example.data.AppLanguage
import com.example.data.DeviceHardwareProfile
import com.example.data.GraphicsMode
import com.example.ui.AmperePulseViewModel.UnsupportedModeAlert
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

private const val VOLT_RIBBON_OUTER =
    "M3.6,14.8 C3.1,13.5 4.2,12.3 6.2,12.3 L12.0,12.3 C14.6,12.3 16.5,13.7 18.0,16.4 L28.4,34.4 C29.3,35.9 31.2,35.7 32.7,34.0 L42.2,21.5 C43.2,20.3 44.3,21.0 43.2,22.8 L30.8,38.9 C27.2,43.1 23.0,44.7 19.6,43.8 C18.1,43.4 16.9,42.1 15.8,40.0 L3.9,16.2 Z"

private const val VOLT_RIBBON_HIGHLIGHT =
    "M4.2,13.3 C6.5,12.4 9.0,13.2 10.7,15.8 L21.8,35.5 C23.3,38.1 25.7,38.4 28.0,36.2 L32.2,32.0 L28.5,35.4 C25.9,37.7 23.5,37.1 22.0,34.5 L11.4,15.8 C9.8,13.3 7.2,12.6 4.2,13.3 Z"

private const val VOLT_CORE_BOLT =
    "M40.2,9.0 L21.8,25.2 L29.4,25.2 L19.6,44.4 L44.4,20.8 L35.0,20.8 Z"

/**
 * Precision-crafted, theme- & size-adaptive Volt Core emblem.
 * Features the sculptural 3D V-ribbon and electric lightning bolt, scaling smoothly
 * to any Dp dimension and adapting across Light, Dark, Simple, Pro, and all 60 themes.
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
            parser.parsePathString(VOLT_RIBBON_OUTER).toPath(),
            parser.parsePathString(VOLT_RIBBON_HIGHLIGHT).toPath(),
            parser.parsePathString(VOLT_CORE_BOLT).toPath()
        )
    }

    val ribbonStartColor = when {
        isSimple -> if (isDark) Color(0xFF94A3B8) else Color(0xFF1E293B)
        isDark -> lerp(Color(0xFF1C2742), resolvedTheme.surface, 0.30f)
        else -> Color(0xFF0A0F1D)
    }
    val ribbonEndColor = when {
        isSimple -> if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
        isDark -> lerp(Color(0xFF34476E), resolvedTheme.textPrimary, 0.22f)
        else -> Color(0xFF1D2842)
    }
    val ribbonBorderTint = when {
        isDark -> resolvedTheme.textPrimary.copy(alpha = 0.28f)
        else -> Color(0xFF3B4B6B).copy(alpha = 0.25f)
    }
    val boltTopColor = when {
        isSimple -> resolvedTheme.accent
        isPro -> lerp(resolvedTheme.secondaryAccent, Color(0xFF00C2FF), 0.45f)
        else -> lerp(Color(0xFF00D4FF), resolvedTheme.secondaryAccent, if (resolvedTheme.themeId != null) 0.45f else 0.15f)
    }
    val boltBottomColor = when {
        isSimple -> resolvedTheme.accent
        isPro -> resolvedTheme.accent
        else -> lerp(Color(0xFF005CFF), resolvedTheme.accent, if (resolvedTheme.themeId != null) 0.55f else 0.20f)
    }

    val semanticsMod = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    Canvas(modifier = semanticsMod.size(size)) {
        val scaleFactor = minOf(this.size.width, this.size.height) / 48f
        val ribbonBrush = Brush.linearGradient(
            colors = listOf(ribbonStartColor, ribbonEndColor, ribbonStartColor),
            start = Offset(4f, 12f),
            end = Offset(34f, 44f)
        )
        val boltBrush = Brush.linearGradient(
            colors = listOf(boltTopColor, lerp(boltTopColor, boltBottomColor, 0.55f), boltBottomColor),
            start = Offset(40f, 9f),
            end = Offset(20f, 44f)
        )

        withTransform({
            scale(scaleFactor, scaleFactor, Offset.Zero)
        }) {
            // 1. Sculptural V-ribbon base
            drawPath(path = parsedPaths.first, brush = ribbonBrush, style = Fill)
            if (isDark && !isSimple) {
                drawPath(
                    path = parsedPaths.first,
                    color = ribbonBorderTint,
                    style = Stroke(width = 0.85f, join = StrokeJoin.Round)
                )
            }
            if (!isSimple) {
                drawPath(
                    path = parsedPaths.second,
                    color = Color.White.copy(alpha = if (isDark) 0.22f else 0.14f),
                    style = Fill
                )
            }

            // 2. Electric Cyan-to-Blue Lightning Bolt
            drawPath(path = parsedPaths.third, brush = boltBrush, style = Fill)
            if (!isSimple) {
                drawPath(
                    path = parsedPaths.third,
                    color = Color.White.copy(alpha = 0.28f),
                    style = Stroke(width = 0.55f, join = StrokeJoin.Miter)
                )
            }
        }
    }
}

/**
 * Top Header with Luxury 3-line frosted glass hamburger menu
 * and quick 3-state Graphics Mode switcher (Normal / Simple / Pro).
 */
@Composable
fun LuxuryTopBar(
    title: String,
    subtitle: String,
    isMenuOpen: Boolean,
    onMenuToggle: () -> Unit,
    themeConfig: GlassThemeConfig,
    isGraphicsLockedLowEnd: Boolean = false,
    onCycleGraphicsMode: () -> Unit,
    onOpenSkinVault: () -> Unit = {},
    onOpenRedeemDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = themeConfig.isDarkMode
    val isArabic = themeConfig.appLanguage.isArabic
    val resolvedTheme = LocalAmpereTheme.current
    val accentColor = if (resolvedTheme.themeId != null || resolvedTheme.isSimpleMode || resolvedTheme.isProMode) {
        resolvedTheme.accent
    } else {
        themeConfig.accentColor
    }
    val titleColor = if (resolvedTheme.themeId != null) resolvedTheme.textPrimary else if (isDark) TextWhite else TextCharcoal
    val subColor = if (resolvedTheme.themeId != null) resolvedTheme.textSecondary else TextSlate

    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .graphicsLayer { clip = false },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Brand Emblem, Crisp Title, Subtle Tier Pill & Concise Subtitle
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AmpereBrandEmblem(
                    size = 25.dp,
                    contentDescription = if (isArabic) "شعار Volt Core" else "Volt Core Emblem",
                    modifier = Modifier.testTag("in_app_brand_icon")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.5.sp,
                    letterSpacing = (-0.2).sp,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Clean Tier Pill (FREE / VIP / PREMIUM)
                Spacer(modifier = Modifier.width(7.dp))
                val tier = themeConfig.appTier
                val tierColor = tier.displayColor(isDark)
                val pulseGlow = if (resolvedTheme.isSimpleMode) {
                    0.75f
                } else {
                    val infiniteTransition = rememberInfiniteTransition(label = "tierGlow")
                    val animatedPulse by infiniteTransition.animateFloat(
                        initialValue = 0.65f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1600, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "tierPulse"
                    )
                    animatedPulse
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(tierColor.copy(alpha = if (isDark) 0.16f else 0.10f))
                        .border(
                            width = 0.9.dp,
                            color = tierColor.copy(alpha = 0.50f * pulseGlow),
                            shape = RoundedCornerShape(7.dp)
                        )
                        .clickable {
                            if (!themeConfig.isPremiumUnlocked) onOpenRedeemDialog() else onMenuToggle()
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag("app_tier_badge")
                ) {
                    Text(
                        text = tier.localizedLabel(isArabic),
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp,
                        letterSpacing = 0.4.sp,
                        color = tierColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(1.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = subtitle,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = subColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Quick 3-Mode Switcher (Normal / Simple / Pro) + Minimal Menu Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val modeTint = when (themeConfig.graphicsMode) {
                GraphicsMode.STANDARD -> accentColor
                GraphicsMode.SIMPLE -> TactileGreen
                GraphicsMode.PRO -> if (isDark) Color(0xFF6C9BCF) else Color(0xFF2E5B88)
            }
            val modeIcon = when (themeConfig.graphicsMode) {
                GraphicsMode.STANDARD -> Icons.Default.AutoAwesome
                GraphicsMode.SIMPLE -> Icons.Default.Layers
                GraphicsMode.PRO -> Icons.Default.Bolt
            }
            val modeShortLabel = when (themeConfig.graphicsMode) {
                GraphicsMode.STANDARD -> if (isArabic) "عادي" else "Normal"
                GraphicsMode.SIMPLE -> if (isArabic) "بسيط" else "Simple"
                GraphicsMode.PRO -> if (isArabic) "محترف" else "Pro"
            }

            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .heightIn(min = 38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (resolvedTheme.themeId != null) resolvedTheme.surface
                        else if (isDark) Color(0xFF1E293B) else Color(0xFFE7ECF3)
                    )
                    .border(
                        1.dp,
                        modeTint.copy(alpha = 0.42f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onCycleGraphicsMode() }
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                    .testTag("top_bar_graphics_mode_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = modeIcon,
                        contentDescription = if (isArabic) "تبديل وضع الرسوميات ($modeShortLabel)" else "Cycle Mode ($modeShortLabel)",
                        tint = modeTint,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = modeShortLabel,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = modeTint,
                        maxLines = 1
                    )
                    if (isGraphicsLockedLowEnd) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = if (isArabic) "مقفل على الوضع البسيط" else "Locked to Simple Mode",
                            tint = modeTint,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

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
            .minimumInteractiveComponentSize()
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
    deviceProfile: DeviceHardwareProfile = DeviceHardwareProfile(),
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
    val accentColor = if (resolvedTheme.themeId != null || resolvedTheme.isSimpleMode || resolvedTheme.isProMode) {
        resolvedTheme.accent
    } else {
        themeConfig.accentColor
    }

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
                                contentDescription = if (isArabic) "شعار Volt Core" else "Volt Core Logo"
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
                                        "تخصيص شامل لتجربة Volt Core"
                                    } else {
                                        "Precision control for Volt Core"
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
                            // Hardware Capability Identification Card
                            val isLowEndDevice = deviceProfile.isLowEndDevice
                            val hwAccent = if (isLowEndDevice) TactileAmber else TactileGreen
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(15.dp))
                                    .background(hwAccent.copy(alpha = if (isDark) 0.14f else 0.09f))
                                    .border(1.dp, hwAccent.copy(alpha = 0.42f), RoundedCornerShape(15.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("device_hardware_tier_card"),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${deviceProfile.brand} ${deviceProfile.model}",
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = textColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(hwAccent.copy(alpha = 0.22f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = deviceProfile.performanceTier.localizedLabel(isArabic),
                                            fontFamily = OutfitFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            color = hwAccent
                                        )
                                    }
                                }
                                Text(
                                    text = "${deviceProfile.chipset} • ${String.format(Locale.US, "%.1f", deviceProfile.totalRamGb)} GB RAM • ${deviceProfile.cpuCores} ${if (isArabic) "أنوية" else "Cores"}",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 10.sp,
                                    color = subTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isLowEndDevice) {
                                    Text(
                                        text = if (isArabic) {
                                            "مواصفات الهاتف محدودة: تم فرض الوضع البسيط وتعطيل الوضع العادي والاحترافي."
                                        } else {
                                            "Entry hardware detected: Simple Mode is enforced; Normal & Pro modes are disabled."
                                        },
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 10.sp,
                                        color = hwAccent,
                                        lineHeight = 14.sp
                                    )
                                }
                            }

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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (isArabic) "الوضع البسيط" else "Simple Mode",
                                            fontFamily = OutfitFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = textColor
                                        )
                                        if (isLowEndDevice) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(TactileAmber.copy(alpha = 0.22f))
                                                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                            ) {
                                                Text(
                                                    text = if (isArabic) "إجباري لهذا الجهاز" else "REQUIRED",
                                                    fontFamily = OutfitFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp,
                                                    color = TactileAmber
                                                )
                                            }
                                        }
                                    }
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

                            // 3b. Pro Mode Toggle Card (Classic Luxury Eye-Comfort + 10ms Ultra-Fast Sync)
                            val isProSelected = themeConfig.graphicsMode == GraphicsMode.PRO
                            val proGoldAccent = if (isDark) Color(0xFF6C9BCF) else Color(0xFF2E5B88)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isProSelected) proGoldAccent.copy(alpha = if (isDark) 0.22f else 0.13f)
                                        else softTileColor.copy(alpha = if (isLowEndDevice) 0.40f else 0.65f)
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
                                                samplingIntervalMs = if (nextMode == GraphicsMode.PRO) 10L else 1000L
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
                                            color = if (isLowEndDevice) subTextColor else textColor
                                        )
                                        if (isLowEndDevice) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(TactileCoralRed.copy(alpha = 0.18f))
                                                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                            ) {
                                                Text(
                                                    text = if (isArabic) "غير مدعوم" else "UNSUPPORTED",
                                                    fontFamily = OutfitFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp,
                                                    color = TactileCoralRed
                                                )
                                            }
                                        } else if (isProSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(proGoldAccent.copy(alpha = 0.22f))
                                                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                            ) {
                                                Text(
                                                    text = if (isArabic) "تزامن 10ms" else "10ms SYNC",
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
                                            "مظهر كلاسيكي فخم ناعم ومريح للعين في كل الأوضاع + رفع تزامن القياسات إلى 10 أجزاء من الثانية"
                                        } else {
                                            "Classic executive eye-comfort finish + 10ms high-frequency sensor synchronization"
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
                                                samplingIntervalMs = if (checked) 10L else 1000L
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

                    // 8. Developer Info, App Version & Pulse Ecosystem Showcase
                    SettingsDeveloperFooterCard(
                        isDark = isDark,
                        isArabic = isArabic,
                        accentColor = accentColor,
                        surfaceColor = surfaceCardColor,
                        softColor = softTileColor,
                        borderColor = subtleBorder,
                        textColor = textColor,
                        subTextColor = subTextColor
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
    }
}

private const val PULSE_TELEGRAM_URL = "https://t.me/pulseprojectdz"

@Composable
private fun SettingsDeveloperFooterCard(
    isDark: Boolean,
    isArabic: Boolean,
    accentColor: Color,
    surfaceColor: Color,
    softColor: Color,
    borderColor: Color,
    textColor: Color,
    subTextColor: Color
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_developer_footer")
            .clip(RoundedCornerShape(20.dp))
            .background(surfaceColor)
            .border(1.1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Developer Signature Row
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
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(accentColor.copy(alpha = if (isDark) 0.20f else 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isArabic) "معلومات المطور" else "Developer Info",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        color = subTextColor
                    )
                    Text(
                        text = "Developed by Charfeddine Ka",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Version, Release Number & Last Update Metadata Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DeveloperMetaChip(
                label = if (isArabic) "رقم النسخة" else "Version",
                value = "v${BuildConfig.VERSION_NAME}",
                accentColor = accentColor,
                softColor = softColor,
                textColor = textColor,
                subTextColor = subTextColor,
                modifier = Modifier.weight(1f)
            )
            DeveloperMetaChip(
                label = if (isArabic) "رقم الإصدار" else "Build",
                value = "#${BuildConfig.VERSION_CODE}",
                accentColor = accentColor,
                softColor = softColor,
                textColor = textColor,
                subTextColor = subTextColor,
                modifier = Modifier.weight(0.85f)
            )
            DeveloperMetaChip(
                label = if (isArabic) "آخر تحديث" else "Last Update",
                value = BuildConfig.LAST_UPDATE_DATE,
                accentColor = accentColor,
                softColor = softColor,
                textColor = textColor,
                subTextColor = subTextColor,
                modifier = Modifier.weight(1.25f)
            )
        }

        // More Apps & Telegram Showcase Box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(15.dp))
                .background(softColor.copy(alpha = if (isDark) 0.72f else 0.65f))
                .border(1.dp, borderColor, RoundedCornerShape(15.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isArabic) "للمزيد من التطبيقات" else "More Apps",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = accentColor
            )
            Text(
                text = "🚀 Welcome to the Pulse Apps Ecosystem | Crafting Android apps with a stunning Pulse identity. 💎",
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 11.sp,
                color = textColor,
                lineHeight = 16.sp
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = if (isDark) 0.22f else 0.13f))
                    .border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                    .clickable {
                        CyberHapticsEngine.triggerTactileTick(context)
                        runCatching {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PULSE_TELEGRAM_URL)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("developer_telegram_button"),
                contentAlignment = Alignment.Center
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
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isArabic) "قناة تيليغرام Pulse Apps" else "Telegram • Pulse Apps",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = textColor
                            )
                            Text(
                                text = PULSE_TELEGRAM_URL,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 10.sp,
                                color = accentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = if (isArabic) "فتح رابط تيليغرام" else "Open Telegram link",
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DeveloperMetaChip(
    label: String,
    value: String,
    accentColor: Color,
    softColor: Color,
    textColor: Color,
    subTextColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(softColor.copy(alpha = 0.72f))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            fontFamily = PlusJakartaSansFontFamily,
            fontSize = 9.5.sp,
            color = subTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = accentColor.takeIf { it != Color.Unspecified } ?: textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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

    val isSimpleMode = LocalAmpereTheme.current.isSimpleMode
    val breathPhase: Float
    val sheenSweep: Float
    if (isSimpleMode) {
        breathPhase = 0.5f
        sheenSweep = -1f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "luxuriousUpdateCardAnim")
        breathPhase = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "cardBreathPhase"
        ).value
        sheenSweep = infiniteTransition.animateFloat(
            initialValue = -0.35f,
            targetValue = 1.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "cardSheenSweep"
        ).value
    }
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

/**
 * Hardware Compatibility Alert Dialog displayed when low-end devices attempt to
 * enable Normal (Standard) or Pro Mode. Enforces Simple Mode cleanly.
 */
@Composable
fun UnsupportedModeDialog(
    alert: UnsupportedModeAlert?,
    deviceProfile: DeviceHardwareProfile,
    themeConfig: GlassThemeConfig,
    onDismiss: () -> Unit
) {
    if (alert == null) return

    val context = LocalContext.current
    val resolvedTheme = LocalAmpereTheme.current
    val isDark = resolvedTheme.isDark
    val isArabic = themeConfig.appLanguage.isArabic
    val cardBg = if (resolvedTheme.themeId != null) resolvedTheme.surface else if (isDark) Color(0xFF172133) else Color(0xFFECF1F7)
    val softInset = if (resolvedTheme.themeId != null) resolvedTheme.surfaceInset else if (isDark) Color(0xFF1E2A3F) else Color(0xFFD8E1EC)
    val textColor = if (resolvedTheme.themeId != null) resolvedTheme.textPrimary else if (isDark) TextWhite else TextCharcoal
    val subTextColor = if (resolvedTheme.themeId != null) resolvedTheme.textSecondary else TextSlate

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(cardBg)
                .border(1.2.dp, TactileAmber.copy(alpha = 0.65f), RoundedCornerShape(26.dp))
                .padding(22.dp)
                .testTag("unsupported_mode_dialog")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(TactileAmber.copy(alpha = if (isDark) 0.20f else 0.14f))
                        .border(1.2.dp, TactileAmber.copy(alpha = 0.65f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = TactileAmber,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = if (isArabic) "هاتفك لايدعم هذا الوضع" else "Your phone does not support this mode",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = textColor,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isArabic) {
                        "لا يمكن تفعيل ${alert.attemptedMode.localizedTitle(true)} على هذا الجهاز. تم فرض الوضع البسيط تلقائياً لضمان أفضل أداء واستقرار للبطارية."
                    } else {
                        "${alert.attemptedMode.localizedTitle(false)} is disabled on this device. Simple Mode is enforced automatically for stability and power efficiency."
                    },
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.5.sp,
                    color = subTextColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(softInset)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${deviceProfile.brand} ${deviceProfile.model}",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = textColor
                    )
                    Text(
                        text = "${deviceProfile.chipset} • ${String.format(Locale.US, "%.1f", deviceProfile.totalRamGb)} GB RAM • ${deviceProfile.cpuCores} ${if (isArabic) "أنوية" else "Cores"} • ${deviceProfile.androidVersion}",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 10.5.sp,
                        color = subTextColor
                    )
                    val reasonText = deviceProfile.localizedLowEndReason(isArabic)
                    if (reasonText.isNotBlank()) {
                        Text(
                            text = reasonText,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.5.sp,
                            color = TactileAmber
                        )
                    }
                }

                Button(
                    onClick = {
                        CyberHapticsEngine.triggerTactileTick(context)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("unsupported_mode_dismiss_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TactileAmber,
                        contentColor = Color(0xFF101827)
                    )
                ) {
                    Text(
                        text = if (isArabic) "حسناً، المتابعة بالوضع البسيط" else "OK, Keep Simple Mode",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }
            }
        }
    }
}
