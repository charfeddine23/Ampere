package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.AppLanguage
import com.example.data.GraphicsMode

@Immutable
data class AmpereResolvedTheme(
    val themeId: String? = null,
    val themeName: String = "الافتراضي الذكي",
    val isDark: Boolean = false,
    val isGlassStyle: Boolean = false,
    val graphicsMode: GraphicsMode = GraphicsMode.STANDARD,
    val background: Color = NeumorphBgLight,
    val surface: Color = NeumorphSurfaceLight,
    val surfaceInset: Color = NeumorphInsetLight,
    val border: Color = NeumorphBorderLight,
    val textPrimary: Color = TextCharcoal,
    val textSecondary: Color = TextSlate,
    val accent: Color = TactileRoyalBlue,
    val secondaryAccent: Color = TactileCyan
) {
    val isSimpleMode: Boolean get() = graphicsMode == GraphicsMode.SIMPLE
    val isProMode: Boolean get() = graphicsMode == GraphicsMode.PRO
}

val LocalAmpereTheme = staticCompositionLocalOf { AmpereResolvedTheme() }

private val LightNeumorphicColorScheme = lightColorScheme(
    primary = TactileRoyalBlue,
    onPrimary = OnAccentLight,
    primaryContainer = Color(0x222962FF),
    onPrimaryContainer = TactileRoyalBlue,
    secondary = TactileGreen,
    onSecondary = OnAccentLight,
    secondaryContainer = Color(0x2200C853),
    onSecondaryContainer = TactileGreen,
    tertiary = TactileCoralRed,
    onTertiary = OnAccentLight,
    background = NeumorphBgLight,
    onBackground = TextCharcoal,
    surface = NeumorphSurfaceLight,
    onSurface = TextCharcoal,
    surfaceVariant = NeumorphInsetLight,
    onSurfaceVariant = TextSlate,
    outline = NeumorphBorderLight,
    outlineVariant = Color(0x1A2962FF)
)

private val DarkNeumorphicColorScheme = darkColorScheme(
    primary = Color(0xFF448AFF),
    onPrimary = Color.Black,
    primaryContainer = Color(0x33448AFF),
    onPrimaryContainer = Color(0xFF82B1FF),
    secondary = Color(0xFF00E676),
    onSecondary = Color.Black,
    secondaryContainer = Color(0x3300E676),
    onSecondaryContainer = Color(0xFF69F0AE),
    tertiary = Color(0xFFFF5722),
    onTertiary = Color.Black,
    background = NeumorphBgDark,
    onBackground = TextWhite,
    surface = NeumorphSurfaceDark,
    onSurface = TextWhite,
    surfaceVariant = Color(0xFF27354A),
    onSurfaceVariant = TextMutedDark,
    outline = NeumorphBorderDark,
    outlineVariant = Color(0x33448AFF)
)

@Composable
fun MyApplicationTheme(
    isDarkMode: Boolean = false,
    accentColor: Color = TactileRoyalBlue,
    activeThemeId: String? = null,
    graphicsMode: GraphicsMode = GraphicsMode.STANDARD,
    appLanguage: AppLanguage = AppLanguage.ARABIC,
    content: @Composable () -> Unit
) {
    val preset = ThemeEngineCatalog.findById(activeThemeId)
    val resolvedIsDark = when (graphicsMode) {
        GraphicsMode.SIMPLE, GraphicsMode.PRO -> isDarkMode
        GraphicsMode.STANDARD -> preset?.isDark ?: isDarkMode
    }

    val (targetBg, targetSurface, targetInset, targetBorder, targetTextPrimary, targetTextSecondary, targetAccent, targetSecondaryAccent) =
        when (graphicsMode) {
            GraphicsMode.SIMPLE -> {
                if (resolvedIsDark) {
                    ThemePalette8(
                        bg = Color(0xFF17191C),
                        surface = Color(0xFF202327),
                        inset = Color(0xFF2A2E34),
                        border = Color(0xFF343942),
                        textPrimary = Color(0xFFE6E8EB),
                        textSecondary = Color(0xFF9AA2AD),
                        accent = Color(0xFF7B8FA6),
                        secondaryAccent = Color(0xFF6D8299)
                    )
                } else {
                    ThemePalette8(
                        bg = Color(0xFFF1F3F5),
                        surface = Color(0xFFFFFFFF),
                        inset = Color(0xFFE6E9ED),
                        border = Color(0xFFD0D7DF),
                        textPrimary = Color(0xFF1F242B),
                        textSecondary = Color(0xFF5D6673),
                        accent = Color(0xFF475569),
                        secondaryAccent = Color(0xFF64748B)
                    )
                }
            }
            GraphicsMode.PRO -> {
                // Classic Luxury Eye-Comfort Palette across both Light & Dark states
                if (resolvedIsDark) {
                    ThemePalette8(
                        bg = Color(0xFF10151C),
                        surface = Color(0xFF171E28),
                        inset = Color(0xFF1E2733),
                        border = Color(0xFF2B3646),
                        textPrimary = Color(0xFFE4E8EE),
                        textSecondary = Color(0xFF92A0B4),
                        accent = Color(0xFF6C9BCF),
                        secondaryAccent = Color(0xFFBFA67A)
                    )
                } else {
                    ThemePalette8(
                        bg = Color(0xFFE6EAEF),
                        surface = Color(0xFFF2F5F8),
                        inset = Color(0xFFDCE2EA),
                        border = Color(0xFFBCC7D6),
                        textPrimary = Color(0xFF1B2432),
                        textSecondary = Color(0xFF526074),
                        accent = Color(0xFF2E5B88),
                        secondaryAccent = Color(0xFF8C7351)
                    )
                }
            }
            GraphicsMode.STANDARD -> {
                ThemePalette8(
                    bg = preset?.backgroundColor ?: if (resolvedIsDark) NeumorphBgDark else NeumorphBgLight,
                    surface = preset?.surfaceColor ?: if (resolvedIsDark) NeumorphSurfaceDark else NeumorphSurfaceLight,
                    inset = preset?.surfaceInsetColor ?: if (resolvedIsDark) Color(0xFF243045) else NeumorphInsetLight,
                    border = preset?.borderColor ?: if (resolvedIsDark) NeumorphBorderDark else NeumorphBorderLight,
                    textPrimary = preset?.textPrimaryColor ?: if (resolvedIsDark) TextWhite else TextCharcoal,
                    textSecondary = preset?.textSecondaryColor ?: if (resolvedIsDark) Color(0xFF93A5BF) else TextSlate,
                    accent = preset?.accentColor ?: accentColor,
                    secondaryAccent = preset?.secondaryAccentColor ?: accentColor
                )
            }
        }

    val animSpec = tween<Color>(
        durationMillis = if (graphicsMode == GraphicsMode.SIMPLE) 140 else 360,
        easing = FastOutSlowInEasing
    )
    val animBg by animateColorAsState(targetBg, animSpec, label = "themeBg")
    val animSurface by animateColorAsState(targetSurface, animSpec, label = "themeSurface")
    val animInset by animateColorAsState(targetInset, animSpec, label = "themeInset")
    val animBorder by animateColorAsState(targetBorder, animSpec, label = "themeBorder")
    val animTextPrimary by animateColorAsState(targetTextPrimary, animSpec, label = "themeTextPrimary")
    val animTextSecondary by animateColorAsState(targetTextSecondary, animSpec, label = "themeTextSecondary")
    val animAccent by animateColorAsState(targetAccent, animSpec, label = "themeAccent")
    val animSecondaryAccent by animateColorAsState(targetSecondaryAccent, animSpec, label = "themeSecondaryAccent")

    val resolvedTheme = AmpereResolvedTheme(
        themeId = when (graphicsMode) {
            GraphicsMode.SIMPLE -> "mode_simple"
            GraphicsMode.PRO -> "mode_pro"
            GraphicsMode.STANDARD -> preset?.id
        },
        themeName = when (graphicsMode) {
            GraphicsMode.SIMPLE -> if (appLanguage.isArabic) "الوضع البسيط" else "Simple Mode"
            GraphicsMode.PRO -> if (appLanguage.isArabic) "وضع المحترفين الكلاسيكي" else "Classic Pro Mode"
            GraphicsMode.STANDARD -> preset?.name ?: if (appLanguage.isArabic) {
                if (resolvedIsDark) "الليلي الهادئ" else "النهاري المريح"
            } else {
                if (resolvedIsDark) "Quiet Dark" else "Soft Light"
            }
        },
        isDark = resolvedIsDark,
        isGlassStyle = if (graphicsMode == GraphicsMode.STANDARD) (preset?.isGlassStyle ?: false) else false,
        graphicsMode = graphicsMode,
        background = animBg,
        surface = animSurface,
        surfaceInset = animInset,
        border = animBorder,
        textPrimary = animTextPrimary,
        textSecondary = animTextSecondary,
        accent = animAccent,
        secondaryAccent = animSecondaryAccent
    )

    val baseScheme = if (resolvedIsDark) DarkNeumorphicColorScheme else LightNeumorphicColorScheme
    val colorScheme = baseScheme.copy(
        primary = animAccent,
        onPrimaryContainer = animAccent,
        primaryContainer = animAccent.copy(alpha = 0.16f),
        secondary = animSecondaryAccent,
        background = animBg,
        onBackground = animTextPrimary,
        surface = animSurface,
        onSurface = animTextPrimary,
        surfaceVariant = animInset,
        onSurfaceVariant = animTextSecondary,
        outline = animBorder
    )

    val layoutDirection = if (appLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalAmpereTheme provides resolvedTheme,
        LocalAppLanguage provides appLanguage,
        LocalLayoutDirection provides layoutDirection
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

private data class ThemePalette8(
    val bg: Color,
    val surface: Color,
    val inset: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val secondaryAccent: Color
)
