package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.example.audio.CyberHapticsEngine
import com.example.data.GraphicsMode
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlin.math.abs
import com.example.ui.components.CyberSkinVaultDialog
import com.example.ui.components.FloatingGlassNavBar
import com.example.ui.components.InAppUpdateDialog
import com.example.ui.components.LuxurySettingsDrawer
import com.example.ui.components.LuxuryTopBar
import com.example.ui.components.NavigationTab
import com.example.ui.components.PremiumRedeemDialog
import com.example.ui.components.ThemeEngineDialog
import com.example.ui.components.UnsupportedModeDialog
import com.example.ui.components.thermalLiquidGlassBackground
import com.example.ui.screens.Page1LivingBatteryScreen
import com.example.ui.screens.Page3BatterySohScreen
import com.example.ui.screens.Page4CableDnaScreen
import com.example.ui.screens.Page5MySpaceScreen
import com.example.ui.theme.LocalAmpereTheme
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeumorphBgDark
import com.example.ui.theme.NeumorphBgLight

@Composable
fun AmperePulseApp(viewModel: AmperePulseViewModel) {
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val voltageHistory by viewModel.voltageHistory.collectAsStateWithLifecycle()
    val currentHistory by viewModel.currentHistory.collectAsStateWithLifecycle()
    val wattageHistory by viewModel.wattageHistory.collectAsStateWithLifecycle()
    val temperatureHistory by viewModel.temperatureHistory.collectAsStateWithLifecycle()
    val sessionLogs by viewModel.sessionLogs.collectAsStateWithLifecycle()

    val cableResult by viewModel.cableResult.collectAsStateWithLifecycle()
    val sohPrediction by viewModel.sohPrediction.collectAsStateWithLifecycle()
    val deviceProfile by viewModel.deviceProfile.collectAsStateWithLifecycle()

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val themeConfig by viewModel.themeConfig.collectAsStateWithLifecycle()
    val isSettingsOpen by viewModel.isSettingsMenuOpen.collectAsStateWithLifecycle()
    val isSkinVaultOpen by viewModel.isSkinVaultOpen.collectAsStateWithLifecycle()
    val isRedeemDialogOpen by viewModel.isRedeemDialogOpen.collectAsStateWithLifecycle()
    val isThemeEngineOpen by viewModel.isThemeEngineOpen.collectAsStateWithLifecycle()
    val appUsageSnapshot by viewModel.appUsageSnapshot.collectAsStateWithLifecycle()
    val userAccount by viewModel.userAccount.collectAsStateWithLifecycle()
    val updateUiState by viewModel.updateUiState.collectAsStateWithLifecycle()
    val unsupportedModeAlert by viewModel.unsupportedModeAlert.collectAsStateWithLifecycle()
    val hostContext = LocalContext.current

    val isDark = themeConfig.isDarkMode

    LaunchedEffect(selectedTab) {
        viewModel.setSohPageActive(selectedTab == NavigationTab.BATTERY_SOH)
        viewModel.setMySpacePageActive(selectedTab == NavigationTab.MY_SPACE)
    }

    // Handle back button: first close settings drawer, then return to BATTERY page
    BackHandler(enabled = isSettingsOpen) {
        viewModel.toggleSettingsMenu(false)
    }

    if (!isSettingsOpen && selectedTab != NavigationTab.BATTERY) {
        BackHandler {
            viewModel.selectTab(NavigationTab.BATTERY)
        }
    }

    // Dynamic state-based primary accent color (calm in Simple Mode, classic executive in Pro Mode)
    val rawActiveAccent = when (selectedTab) {
        NavigationTab.BATTERY -> {
            when {
                telemetry.temperatureC >= 42f -> Color(0xFFFF3D00)
                telemetry.temperatureC >= 39.5f -> Color(0xFFFF9100)
                themeConfig.isUsingThemeEngine -> themeConfig.accentColor
                telemetry.temperatureC <= 34f -> Color(0xFF00B0FF)
                else -> telemetry.livingState.neonPrimary
            }
        }
        else -> themeConfig.accentColor
    }
    val activeAccent = when {
        themeConfig.isSimpleMode -> if (isDark) Color(0xFF7B8FA6) else Color(0xFF475569)
        themeConfig.isProMode -> if (isDark) Color(0xFF6C9BCF) else Color(0xFF2E5B88)
        else -> rawActiveAccent
    }

    MyApplicationTheme(
        isDarkMode = isDark,
        accentColor = activeAccent,
        activeThemeId = themeConfig.activeThemeId,
        graphicsMode = themeConfig.graphicsMode,
        appLanguage = themeConfig.appLanguage
    ) {
        val resolvedTheme = LocalAmpereTheme.current
        val rootBg = if (resolvedTheme.themeId != null) resolvedTheme.background
        else if (isDark) NeumorphBgDark else NeumorphBgLight

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (selectedTab == NavigationTab.BATTERY) {
                        Modifier.thermalLiquidGlassBackground(
                            temperatureC = telemetry.temperatureC,
                            isDarkMode = isDark,
                            glowIntensity = when {
                                themeConfig.isSimpleMode -> 0f
                                themeConfig.isProMode -> 0.35f
                                else -> themeConfig.glowIntensity
                            },
                            criticalTempThreshold = 42.0f,
                            normalTempThreshold = 34.0f
                        )
                    } else if (themeConfig.isSimpleMode) {
                        Modifier.background(rootBg)
                    } else {
                        Modifier
                            .background(rootBg)
                            .graphicsLayer { clip = false }
                            .drawBehind {
                                val auraAlphaScale = if (themeConfig.isProMode) 0.35f else 1f
                                val auraColor = activeAccent
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = if (isDark) {
                                            listOf(
                                                auraColor.copy(alpha = 0.08f * themeConfig.glowIntensity * auraAlphaScale),
                                                auraColor.copy(alpha = 0.02f * auraAlphaScale),
                                                Color.Transparent
                                            )
                                        } else {
                                            listOf(
                                                auraColor.copy(alpha = 0.05f * themeConfig.glowIntensity * auraAlphaScale),
                                                Color.Transparent
                                            )
                                        },
                                        center = Offset(size.width * 0.5f, size.height * 0.16f),
                                        radius = size.width * 0.85f
                                    )
                                )
                            }
                    }
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Luxury 3-Line Top Settings Header Bar with Dark Mode Toggle & Cyber Vault Button
                val isArabic = themeConfig.appLanguage.isArabic
                LuxuryTopBar(
                    title = "VOLT CORE",
                    subtitle = when (selectedTab) {
                        NavigationTab.BATTERY -> if (isArabic) "مراقبة الطاقة اللحظية" else "Real-Time Power Telemetry"
                        NavigationTab.BATTERY_SOH -> if (isArabic) "صحة الخلية ودورات الشحن" else "Cell Health & Cycle Analytics"
                        NavigationTab.CABLE_DNA -> if (isArabic) "فحص الكابل وبروتوكول الشحن" else "Cable & Protocol Diagnostics"
                        NavigationTab.MY_SPACE -> if (isArabic) "وحدة القياس الحية والسجل" else "Live Oscilloscope & Logs"
                    },
                    isMenuOpen = isSettingsOpen,
                    onMenuToggle = { viewModel.toggleSettingsMenu(!isSettingsOpen) },
                    themeConfig = themeConfig,
                    isGraphicsLockedLowEnd = deviceProfile.isLowEndDevice,
                    onCycleGraphicsMode = {
                        CyberHapticsEngine.triggerTactileTick(hostContext)
                        val nextMode = when (themeConfig.graphicsMode) {
                            GraphicsMode.STANDARD -> GraphicsMode.SIMPLE
                            GraphicsMode.SIMPLE -> GraphicsMode.PRO
                            GraphicsMode.PRO -> GraphicsMode.STANDARD
                        }
                        viewModel.updateThemeConfig(
                            themeConfig.copy(
                                graphicsMode = nextMode,
                                samplingIntervalMs = if (nextMode == GraphicsMode.PRO) 10L else 1000L
                            )
                        )
                    },
                    onOpenRedeemDialog = { viewModel.toggleRedeemDialog(true) }
                )

                // 5-Page Content with High-Performance Spring Transitions
                // Right-half swipe-left sensor (excludes bottom FloatingGlassNavBar & nav insets cleanly)
                val density = LocalDensity.current
                val navBarsBottomPx = WindowInsets.navigationBars.getBottom(density).toFloat()
                val bottomBannerExclusionPx = with(density) { 44.dp.toPx() } + navBarsBottomPx
                val swipeTriggerPx = with(density) { 30.dp.toPx() }

                val isRtlLayout = themeConfig.appLanguage.isArabic
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 680.dp)
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth()
                        .padding(bottom = 74.dp)
                        .pointerInput(isSettingsOpen, bottomBannerExclusionPx, isRtlLayout) {
                            if (isSettingsOpen) return@pointerInput
                            val touchSlop = viewConfiguration.touchSlop
                            coroutineScope {
                                while (isActive) {
                                    awaitPointerEventScope {
                                        val down = awaitFirstDown(
                                            requireUnconsumed = false,
                                            pass = PointerEventPass.Main
                                        )
                                        // Match Alignment.CenterEnd edge: Left half in RTL Arabic, Right half in LTR English
                                        val inDrawerEdgeHalf = if (isRtlLayout) {
                                            down.position.x <= size.width * 0.5f
                                        } else {
                                            down.position.x >= size.width * 0.5f
                                        }
                                        val aboveBottomBanner = down.position.y <= (size.height - bottomBannerExclusionPx)
                                        if (!inDrawerEdgeHalf || !aboveBottomBanner) {
                                            return@awaitPointerEventScope
                                        }

                                        val startX = down.position.x
                                        val startY = down.position.y

                                        while (true) {
                                            val event = awaitPointerEvent(PointerEventPass.Main)
                                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                            if (!change.pressed || change.isConsumed) break

                                            val totalDx = change.position.x - startX
                                            val totalDy = change.position.y - startY

                                            // Yield cleanly if user is scrolling vertically
                                            if (abs(totalDy) > touchSlop && abs(totalDy) >= abs(totalDx)) {
                                                break
                                            }

                                            // Open Settings drawer on deliberate inward edge swipe
                                            val swipeTriggered = if (isRtlLayout) totalDx > swipeTriggerPx else totalDx < -swipeTriggerPx
                                            if (swipeTriggered && abs(totalDx) > abs(totalDy) * 1.35f) {
                                                change.consume()
                                                CyberHapticsEngine.triggerTactileTick(hostContext)
                                                viewModel.toggleSettingsMenu(true)
                                                break
                                            }
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                if (themeConfig.isSimpleMode) {
                                    fadeIn(androidx.compose.animation.core.tween(120)) togetherWith
                                        fadeOut(androidx.compose.animation.core.tween(90))
                                } else {
                                    val isMovingForward = targetState.ordinal > initialState.ordinal
                                    val dirSign = if (isRtlLayout) -1 else 1
                                    (slideInHorizontally(
                                        initialOffsetX = {
                                            (if (isMovingForward) 1 else -1) * dirSign * (it * 0.14f).toInt()
                                        },
                                        animationSpec = spring(
                                            dampingRatio = 0.94f,
                                            stiffness = 500f
                                        )
                                    ) + fadeIn(
                                        animationSpec = androidx.compose.animation.core.tween(200)
                                    )).togetherWith(
                                        slideOutHorizontally(
                                            targetOffsetX = {
                                                (if (isMovingForward) -1 else 1) * dirSign * (it * 0.10f).toInt()
                                            },
                                            animationSpec = spring(
                                                dampingRatio = 0.94f,
                                                stiffness = 500f
                                            )
                                        ) + fadeOut(
                                            animationSpec = androidx.compose.animation.core.tween(150)
                                        )
                                    )
                                }
                            },
                            label = "120HzUltraFluidPageTransition"
                        ) { tab ->
                            when (tab) {
                                NavigationTab.BATTERY -> Page1LivingBatteryScreen(
                                    telemetry = telemetry,
                                    themeConfig = themeConfig,
                                    deviceProfile = deviceProfile,
                                    appUsageSnapshot = appUsageSnapshot,
                                    onRefreshAppUsage = { viewModel.refreshAppBatteryUsage(force = true) }
                                )
                                NavigationTab.BATTERY_SOH -> Page3BatterySohScreen(
                                    telemetry = telemetry,
                                    themeConfig = themeConfig,
                                    sohPrediction = sohPrediction,
                                    onRecalibrateSoh = { addLog -> viewModel.recalibrateSohPrediction(addLog) }
                                )
                                NavigationTab.CABLE_DNA -> Page4CableDnaScreen(
                                    telemetry = telemetry,
                                    themeConfig = themeConfig,
                                    cableResult = cableResult,
                                    deviceProfile = deviceProfile,
                                    onRunDiagnosticSweep = { viewModel.runCableDiagnosticSweep(it) }
                                )
                                NavigationTab.MY_SPACE -> Page5MySpaceScreen(
                                    telemetry = telemetry,
                                    voltageHistory = voltageHistory,
                                    currentHistory = currentHistory,
                                    wattageHistory = wattageHistory,
                                    temperatureHistory = temperatureHistory,
                                    sessionLogs = sessionLogs,
                                    themeConfig = themeConfig,
                                    userAccount = userAccount,
                                    deviceProfile = deviceProfile,
                                    onUpdateOwnerName = { viewModel.updateOwnerName(it) },
                                    onOpenRedeemDialog = { viewModel.toggleRedeemDialog(true) },
                                    onAddVitalLog = { title, desc, tag, type ->
                                        viewModel.addManualLog(title, desc, tag, type)
                                    }
                                )
                        }
                    }
                }
            }

            // Sleek Floating Navigation Bar anchored at bottom
            FloatingGlassNavBar(
                selectedTab = selectedTab,
                onTabSelected = { viewModel.selectTab(it) },
                accentColor = activeAccent,
                isDarkMode = isDark,
                showLiquidDroplet = themeConfig.showLiquidDroplet,
                dropletDragNavigationEnabled = themeConfig.dropletDragNavigationEnabled,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // Luxury Slide-Over Settings Drawer
            LuxurySettingsDrawer(
                isOpen = isSettingsOpen,
                onClose = { viewModel.toggleSettingsMenu(false) },
                themeConfig = themeConfig,
                deviceProfile = deviceProfile,
                onConfigChange = { viewModel.updateThemeConfig(it) },
                onOpenThemeEngine = { viewModel.toggleThemeEngine(true) },
                onOpenSkinVault = { viewModel.toggleSkinVault(true) },
                onOpenRedeemDialog = { viewModel.toggleRedeemDialog(true) },
                updateUiState = updateUiState,
                onCheckUpdates = {
                    viewModel.checkForAppUpdates(
                        openDialogImmediatelyOnManual = true,
                        autoDownloadIfFound = true
                    )
                },
                onOpenUpdateDialog = { viewModel.openUpdatePreviewDialog() },
                onTriggerUpdate = { viewModel.triggerAppUpdate(hostContext) },
                onCancelDownload = { viewModel.cancelAppUpdateDownload() }
            )

            // Compact In-App Auto-Update Modal Dialog
            InAppUpdateDialog(
                state = updateUiState,
                themeConfig = themeConfig,
                accentColor = activeAccent,
                onDismiss = { viewModel.dismissUpdateDialog() },
                onUpdateClick = { viewModel.triggerAppUpdate(hostContext) },
                onCancelDownload = { viewModel.cancelAppUpdateDownload() }
            )

            // Premium Redeem Code Activation Dialog
            PremiumRedeemDialog(
                isOpen = isRedeemDialogOpen,
                themeConfig = themeConfig,
                onDismiss = { viewModel.toggleRedeemDialog(false) },
                onRedeemCode = { code -> viewModel.redeemPremiumCode(code) }
            )

            // Theme Engine Studio Modal (60 Curated IDE & Liquid Glass Themes)
            ThemeEngineDialog(
                isOpen = isThemeEngineOpen,
                onClose = { viewModel.toggleThemeEngine(false) },
                themeConfig = themeConfig,
                onSelectPreset = { preset -> viewModel.equipThemePreset(preset) },
                onResetDefault = { viewModel.resetThemeEngineToDefault() }
            )

            // Cyber Skin (Liquid Glass Vault) Modal Dialog - متجر الأقنعة والثيمات السيبرانية
            CyberSkinVaultDialog(
                isOpen = isSkinVaultOpen,
                onClose = { viewModel.toggleSkinVault(false) },
                themeConfig = themeConfig,
                onEquipSkin = { skin ->
                    viewModel.equipCyberSkin(skin)
                }
            )

            // Unsupported Hardware Graphics Mode Modal Alert
            UnsupportedModeDialog(
                alert = unsupportedModeAlert,
                deviceProfile = deviceProfile,
                themeConfig = themeConfig,
                onDismiss = { viewModel.dismissUnsupportedModeAlert() }
            )
        }
    }
}
