package com.example.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.CyberSkin
import com.example.data.CyberSkinVaultCatalog
import com.example.data.GlassThemeConfig
import com.example.ui.theme.LocalAmpereTheme
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.TactileGreen
import com.example.ui.theme.TactileRoyalBlue
import com.example.ui.theme.TextCharcoal
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite

/**
 * Cyber Skin (Liquid Glass Vault) - متجر الأقنعة والثيمات السيبرانية
 * A futuristic cyber vault dialog allowing users to browse, preview, and equip
 * high-fidelity holographic themes and liquid glass skins with 90 FPS animated auras.
 */
@Composable
fun CyberSkinVaultDialog(
    isOpen: Boolean,
    onClose: () -> Unit,
    themeConfig: GlassThemeConfig,
    onEquipSkin: (CyberSkin) -> Unit
) {
    if (!isOpen) return

    val resolvedTheme = LocalAmpereTheme.current
    val isDark = resolvedTheme.isDark
    val isArabic = themeConfig.appLanguage.isArabic
    val textColor = if (resolvedTheme.themeId != null) resolvedTheme.textPrimary else if (isDark) TextWhite else TextCharcoal
    val subTextColor = if (resolvedTheme.themeId != null) resolvedTheme.textSecondary else if (isDark) Color(0xFF94A3B8) else TextSlate
    val dialogBg = if (resolvedTheme.themeId != null) resolvedTheme.background else if (isDark) Color(0xF20F172A) else Color(0xF8E1E8F1)
    val closeBtnBg = if (resolvedTheme.themeId != null) resolvedTheme.surface else if (isDark) Color(0xFF1E293B) else Color(0xFFD2DBE6)
    val activeSkin = CyberSkinVaultCatalog.getSkinById(themeConfig.activeSkinId)

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 520.dp)
                .fillMaxHeight(0.87f)
                .testTag("cyber_skin_vault_dialog"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(28.dp))
                    .background(dialogBg)
                    .border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                activeSkin.primaryColor.copy(alpha = 0.8f),
                                activeSkin.secondaryColor.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    // Header with Vault Badge & Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(activeSkin.primaryColor.copy(alpha = 0.18f))
                                    .border(1.2.dp, activeSkin.primaryColor.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = activeSkin.primaryColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isArabic) "خزينة الثيمات الزجاجية" else "CYBER SKIN VAULT",
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        letterSpacing = (-0.2).sp,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    PulsingNeonDot(color = activeSkin.primaryColor, sizeDp = 6.dp)
                                }
                                Text(
                                    text = if (isArabic) "مظاهر زجاجية وتدرجات نيون فورية" else "Holographic Liquid Glass Skins",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 11.sp,
                                    color = activeSkin.primaryColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Close Button
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(closeBtnBg)
                                .clickable { onClose() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isArabic) "إغلاق" else "Close",
                                tint = textColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Active Equipped Skin Status Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(activeSkin.primaryColor.copy(alpha = if (isDark) 0.16f else 0.10f))
                            .border(1.dp, activeSkin.primaryColor.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = activeSkin.primaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isArabic) "المظهر النشط حالياً:" else "Currently Equipped:",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 10.sp,
                                        color = subTextColor
                                    )
                                    Text(
                                        text = activeSkin.localizedName(isArabic),
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = activeSkin.primaryColor
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(activeSkin.primaryColor.copy(alpha = 0.22f))
                                    .border(1.dp, activeSkin.primaryColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "مفعل" else "EQUIPPED",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = activeSkin.primaryColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Catalog of Cyber Skins
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(CyberSkinVaultCatalog.allSkins) { skin ->
                            val isEquipped = skin.id == themeConfig.activeSkinId
                            CyberSkinItemCard(
                                skin = skin,
                                isEquipped = isEquipped,
                                isDarkMode = isDark,
                                isArabic = isArabic,
                                onEquip = {
                                    onEquipSkin(skin)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Vault Bottom Tip
                    Text(
                        text = if (isArabic) {
                            "كل مظهر يضبط لون الهالة المحيطة، شدة التوهج، وشفافية الزجاج تلقائياً."
                        } else {
                            "Each skin tunes ambient aura tint, glow intensity, and translucent glass response automatically."
                        },
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 10.5.sp,
                        color = subTextColor,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CyberSkinItemCard(
    skin: CyberSkin,
    isEquipped: Boolean,
    isDarkMode: Boolean,
    isArabic: Boolean,
    onEquip: () -> Unit
) {
    val resolvedTheme = LocalAmpereTheme.current
    val cardBg = if (resolvedTheme.themeId != null) resolvedTheme.surface else if (isDarkMode) Color(0xFF1E293B) else Color(0xFFECF0F6)
    val textColor = if (resolvedTheme.themeId != null) resolvedTheme.textPrimary else if (isDarkMode) TextWhite else TextCharcoal
    val subTextColor = if (resolvedTheme.themeId != null) resolvedTheme.textSecondary else if (isDarkMode) Color(0xFF94A3B8) else TextSlate

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(
                width = if (isEquipped) 1.8.dp else 1.dp,
                brush = if (isEquipped) {
                    Brush.sweepGradient(
                        colors = skin.holographicBorder + skin.holographicBorder.first(),
                        center = Offset(200f, 60f)
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            skin.primaryColor.copy(alpha = 0.35f),
                            Color(0x33475569)
                        )
                    )
                },
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onEquip() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Holographic Color Orb & Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Dual-Ring Color Lens
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    skin.primaryColor.copy(alpha = 0.85f),
                                    skin.secondaryColor.copy(alpha = 0.50f),
                                    skin.ambientAura.copy(alpha = 0.15f)
                                )
                            )
                        )
                        .border(1.5.dp, skin.primaryColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f))
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = skin.localizedName(isArabic),
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(skin.primaryColor.copy(alpha = 0.18f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = skin.localizedTag(isArabic),
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp,
                                color = skin.primaryColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = skin.localizedDescription(isArabic),
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 10.sp,
                        color = subTextColor,
                        lineHeight = 13.sp,
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right: Equip Button / Equipped Status
            if (isEquipped) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(TactileGreen.copy(alpha = 0.2f))
                        .border(1.dp, TactileGreen.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = TactileGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isArabic) "مفعل" else "Equipped",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = TactileGreen
                        )
                    }
                }
            } else {
                Button(
                    onClick = onEquip,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = skin.primaryColor
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isArabic) "تفعيل" else "Equip",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (skin.primaryColor.luminance() > 0.52f) Color.Black else Color.White
                    )
                }
            }
        }
    }
}
