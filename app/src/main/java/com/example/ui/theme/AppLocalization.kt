package com.example.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.data.AppLanguage

/**
 * Provides the active user-selected [AppLanguage] down the entire Compose hierarchy.
 */
val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.ARABIC }
