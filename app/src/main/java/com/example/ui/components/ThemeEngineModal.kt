package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.CyberHapticsEngine
import com.example.data.GlassThemeConfig
import com.example.ui.theme.AppThemePreset
import com.example.ui.theme.LocalAmpereTheme
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.ThemeEngineCatalog
import com.example.ui.theme.ThemePresetCategory

private enum class ThemeFilterTab(val labelAr: String, val labelEn: String) {
    ALL("الكل", "All"),
    CLASSIC_IDE("برمجي", "Code IDE"),
    LIQUID_GLASS("زجاج سائل", "Liquid Glass"),
    LIGHT_COMFORT("نهاري مريح", "Light Comfort");

    fun localizedLabel(isArabic: Boolean): String = if (isArabic) labelAr else labelEn
}

@Composable
fun ThemeEngineDialog(
    isOpen: Boolean,
    onClose: () -> Unit,
    themeConfig: GlassThemeConfig,
    onSelectPreset: (AppThemePreset) -> Unit,
    onResetDefault: () -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    val resolvedTheme = LocalAmpereTheme.current
    val isDark = resolvedTheme.isDark
    val activeAccent = resolvedTheme.accent
    val isArabic = themeConfig.appLanguage.isArabic

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ThemeFilterTab.ALL) }

    val allPresets = ThemeEngineCatalog.presets
    val filteredPresets = remember(searchQuery, selectedFilter) {
        allPresets.filter { preset ->
            val matchesFilter = when (selectedFilter) {
                ThemeFilterTab.ALL -> true
                ThemeFilterTab.CLASSIC_IDE -> preset.category == ThemePresetCategory.CLASSIC_IDE
                ThemeFilterTab.LIQUID_GLASS -> preset.category == ThemePresetCategory.LIQUID_GLASS || preset.isGlassStyle
                ThemeFilterTab.LIGHT_COMFORT -> !preset.isDark
            }
            val matchesQuery = searchQuery.isBlank() ||
                preset.name.contains(searchQuery.trim(), ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.87f)
                .clip(RoundedCornerShape(28.dp))
                .background(resolvedTheme.background)
                .border(1.2.dp, resolvedTheme.border, RoundedCornerShape(28.dp))
                .padding(16.dp)
                .testTag("theme_engine_dialog")
        ) {
            // 1. Header Bar
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
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(activeAccent.copy(alpha = if (isDark) 0.22f else 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = activeAccent,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isArabic) "محرك الثيمات" else "Theme Engine",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = resolvedTheme.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isArabic) {
                                "النشط: ${resolvedTheme.themeName} (${allPresets.size} ثيم)"
                            } else {
                                "Active: ${resolvedTheme.themeName} (${allPresets.size} themes)"
                            },
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.sp,
                            color = activeAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (themeConfig.isUsingThemeEngine) {
                        Box(
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(resolvedTheme.surface)
                                .border(1.dp, resolvedTheme.border, RoundedCornerShape(14.dp))
                                .clickable {
                                    CyberHapticsEngine.triggerTactileTick(context)
                                    onResetDefault()
                                }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = if (isArabic) "استعادة الافتراضي" else "Reset to default",
                                    tint = activeAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isArabic) "افتراضي" else "Default",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = resolvedTheme.textPrimary
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(resolvedTheme.surface)
                            .border(1.dp, resolvedTheme.border, CircleShape)
                            .clickable {
                                CyberHapticsEngine.triggerTactileTick(context)
                                onClose()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isArabic) "إغلاق محرك الثيمات" else "Close Theme Engine",
                            tint = resolvedTheme.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Live Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine = true,
                placeholder = {
                    Text(
                        text = if (isArabic) "ابحث عن ثيم... (Monokai, Dracula, Glass)" else "Search themes... (Monokai, Dracula, Glass)",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 12.sp,
                        color = resolvedTheme.textSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = activeAccent,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { searchQuery = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isArabic) "مسح البحث" else "Clear search",
                                tint = resolvedTheme.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = resolvedTheme.surface,
                    unfocusedContainerColor = resolvedTheme.surface,
                    focusedBorderColor = activeAccent,
                    unfocusedBorderColor = resolvedTheme.border,
                    focusedTextColor = resolvedTheme.textPrimary,
                    unfocusedTextColor = resolvedTheme.textPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("theme_engine_search")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Category Filter Pills
            val classicCount = remember { allPresets.count { it.category == ThemePresetCategory.CLASSIC_IDE } }
            val glassCount = remember { allPresets.count { it.category == ThemePresetCategory.LIQUID_GLASS || it.isGlassStyle } }
            val lightCount = remember { allPresets.count { !it.isDark } }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ThemeFilterTab.entries) { tab ->
                    val count = when (tab) {
                        ThemeFilterTab.ALL -> allPresets.size
                        ThemeFilterTab.CLASSIC_IDE -> classicCount
                        ThemeFilterTab.LIQUID_GLASS -> glassCount
                        ThemeFilterTab.LIGHT_COMFORT -> lightCount
                    }
                    val selected = selectedFilter == tab
                    Box(
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) activeAccent.copy(alpha = if (isDark) 0.24f else 0.16f)
                                else resolvedTheme.surface
                            )
                            .border(
                                1.dp,
                                if (selected) activeAccent else resolvedTheme.border,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                CyberHapticsEngine.triggerTactileTick(context)
                                selectedFilter = tab
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${tab.localizedLabel(isArabic)} ($count)",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.5.sp,
                            color = if (selected) activeAccent else resolvedTheme.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. 2-Column Live Preview Theme Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(filteredPresets, key = { it.id }) { preset ->
                    ThemePresetGridCard(
                        preset = preset,
                        isSelected = themeConfig.activeThemeId == preset.id,
                        isArabic = isArabic,
                        onClick = { onSelectPreset(preset) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemePresetGridCard(
    preset: AppThemePreset,
    isSelected: Boolean,
    isArabic: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) preset.accentColor else preset.borderColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "presetCardBorder_${preset.id}"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 118.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(preset.backgroundColor)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
            .testTag("theme_preset_${preset.id}"),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Mini App Preview Card inside tile
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(preset.surfaceColor)
                .border(0.8.dp, preset.borderColor, RoundedCornerShape(11.dp))
                .padding(7.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(preset.accentColor)
                        )
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(preset.secondaryAccentColor)
                        )
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(preset.textPrimaryColor.copy(alpha = 0.7f))
                        )
                    }

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(preset.accentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = if (isArabic) "مفعّل" else "Active",
                                tint = if (preset.isDark) Color(0xFF0B101B) else Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (preset.isGlassStyle) Icons.Default.AutoAwesome
                            else if (preset.isDark) Icons.Default.DarkMode
                            else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = preset.accentColor.copy(alpha = 0.85f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                // Simulated mini telemetry bar in preset accent colors
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(preset.surfaceInsetColor)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(preset.accentColor, preset.secondaryAccentColor)
                                )
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        Column {
            Text(
                text = preset.localizedName(isArabic),
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                color = preset.textPrimaryColor,
                maxLines = 2,
                lineHeight = 14.sp,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = when {
                    preset.isGlassStyle -> if (isArabic) "زجاج سائل" else "Liquid Glass"
                    preset.isDark -> if (isArabic) "داكن" else "Dark"
                    else -> if (isArabic) "نهاري" else "Light"
                },
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 9.5.sp,
                color = preset.textSecondaryColor
            )
        }
    }
}
