package com.example.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.data.AppLanguage

/**
 * Provides the active user-selected [AppLanguage] down the entire Compose hierarchy.
 */
val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.ARABIC }

/**
 * Convenience bilingual selector used cleanly across Composables and engines.
 */
fun localizedText(isArabic: Boolean, ar: String, en: String): String = if (isArabic) ar else en
