package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BatteryMonitorRepository
import com.example.data.BatteryTelemetry
import com.example.data.CyberEggData
import com.example.data.GlassThemeConfig
import com.example.data.HatchedCompanion
import com.example.data.LivingBatteryState
import com.example.data.PrizeRoadData
import com.example.data.SessionLog
import com.example.data.StreakData
import com.example.data.TechCrewMember
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import android.content.Context
import com.example.data.TelemetryPoint
import com.example.data.telemetry.AppBatteryUsageTracker
import com.example.data.telemetry.AppUsageSnapshot
import com.example.data.update.InAppUpdateEngine
import com.example.data.update.InAppUpdateUiState
import com.example.ui.components.NavigationTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AmperePulseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BatteryMonitorRepository(
        context = application.applicationContext,
        scope = viewModelScope
    )

    val telemetry: StateFlow<BatteryTelemetry> = repository.telemetry
    val voltageHistory: StateFlow<List<TelemetryPoint>> = repository.voltageHistory
    val currentHistory: StateFlow<List<TelemetryPoint>> = repository.currentHistory
    val wattageHistory: StateFlow<List<TelemetryPoint>> = repository.wattageHistory
    val temperatureHistory: StateFlow<List<TelemetryPoint>> = repository.temperatureHistory
    val sessionLogs: StateFlow<List<SessionLog>> = repository.sessionLogs

    // 5-Page Architecture Flows
    val streakData: StateFlow<StreakData> = repository.streakData
    val eggData: StateFlow<CyberEggData> = repository.eggData
    val prizeRoadData: StateFlow<PrizeRoadData> = repository.prizeRoadData
    val techCrew: StateFlow<List<TechCrewMember>> = repository.techCrew
    val isAutoSensorMode: StateFlow<Boolean> = repository.isAutoSensorMode
    val userAccount: StateFlow<com.example.data.UserAccount> = repository.userAccount
    val cableResult: StateFlow<com.example.data.CableDiagnosticResult> = repository.cableResult
    val sohPrediction: StateFlow<com.example.data.BatterySohPrediction> = repository.sohPrediction
    val hourlyStats: StateFlow<List<com.example.data.local.HourlyBatteryStatEntity>> = repository.hourlyStats

    private val _selectedTab = MutableStateFlow(NavigationTab.BATTERY)
    val selectedTab: StateFlow<NavigationTab> = _selectedTab.asStateFlow()

    private val prefsManager = com.example.data.local.SettingsPreferencesManager(application.applicationContext)

    private val _themeConfig = MutableStateFlow(prefsManager.loadThemeConfig())
    val themeConfig: StateFlow<GlassThemeConfig> = _themeConfig.asStateFlow()

    private val _isSettingsMenuOpen = MutableStateFlow(false)
    val isSettingsMenuOpen: StateFlow<Boolean> = _isSettingsMenuOpen.asStateFlow()

    private val _isSkinVaultOpen = MutableStateFlow(false)
    val isSkinVaultOpen: StateFlow<Boolean> = _isSkinVaultOpen.asStateFlow()

    private val _isRedeemDialogOpen = MutableStateFlow(false)
    val isRedeemDialogOpen: StateFlow<Boolean> = _isRedeemDialogOpen.asStateFlow()

    private val _isThemeEngineOpen = MutableStateFlow(false)
    val isThemeEngineOpen: StateFlow<Boolean> = _isThemeEngineOpen.asStateFlow()

    private val _appUsageSnapshot = MutableStateFlow(AppUsageSnapshot(isLoading = true))
    val appUsageSnapshot: StateFlow<AppUsageSnapshot> = _appUsageSnapshot.asStateFlow()

    private val updateEngine = InAppUpdateEngine(application.applicationContext)
    val updateUiState: StateFlow<InAppUpdateUiState> = updateEngine.uiState

    fun selectTab(tab: NavigationTab) {
        _selectedTab.value = tab
    }

    init {
        // Apply persisted sound & haptic preferences on startup
        com.example.audio.CyberSoundscapeEngine.isEnabled = _themeConfig.value.soundEffectsEnabled
        com.example.audio.CyberHapticsEngine.isEnabled = _themeConfig.value.hapticFeedbackEnabled
        repository.updateCurrentCalibration(
            multiplier = _themeConfig.value.currentMultiplier,
            zeroOffsetMa = _themeConfig.value.currentZeroOffsetMa
        )
        refreshAppBatteryUsage()
        if (_themeConfig.value.autoUpdateEnabled) {
            updateEngine.checkForUpdates(
                autoShowOnAvailable = true,
                autoDownloadIfFound = true
            )
        }
    }

    fun refreshAppBatteryUsage() {
        viewModelScope.launch {
            val capacityMah = telemetry.value.capacityMah
            val snapshot = withContext(Dispatchers.IO) {
                AppBatteryUsageTracker.collectUsageSnapshot(
                    context = getApplication(),
                    batteryCapacityMah = capacityMah
                )
            }
            _appUsageSnapshot.value = snapshot
        }
    }

    fun attachUpdateLauncher(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        updateEngine.attachLauncher(launcher)
    }

    fun checkForAppUpdates(
        openDialogImmediatelyOnManual: Boolean = false,
        autoDownloadIfFound: Boolean = true
    ) {
        updateEngine.checkForUpdates(
            autoShowOnAvailable = true,
            openDialogImmediatelyOnManual = openDialogImmediatelyOnManual,
            autoDownloadIfFound = autoDownloadIfFound
        )
    }

    fun cancelAppUpdateDownload() {
        updateEngine.cancelInAppDownload()
    }

    fun openUpdatePreviewDialog() {
        updateEngine.openUpdateDialogPreview()
    }

    fun dismissUpdateDialog() {
        updateEngine.dismissDialog()
    }

    fun triggerAppUpdate(hostContext: Context) {
        updateEngine.startOrCompleteUpdate(hostContext)
    }

    fun onUpdateFlowResult(resultCode: Int) {
        updateEngine.onUpdateFlowResult(resultCode)
    }

    fun onResumeUpdateCheck() {
        if (_themeConfig.value.autoUpdateEnabled) {
            updateEngine.checkForUpdates(
                autoShowOnAvailable = true,
                autoDownloadIfFound = true
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        updateEngine.unregisterListener()
    }

    fun setAppInForeground(inForeground: Boolean) {
        repository.setAppInForeground(inForeground)
        com.example.audio.CyberSoundscapeEngine.isAppInForeground = inForeground
        com.example.audio.CyberHapticsEngine.isAppInForeground = inForeground
    }

    fun setSohPageActive(active: Boolean) {
        repository.setSohPageActive(active)
    }

    fun updateThemeConfig(config: GlassThemeConfig) {
        val prev = _themeConfig.value
        val activePreset = com.example.ui.theme.ThemeEngineCatalog.findById(config.activeThemeId)
        val sanitized = if (activePreset != null && prev.isDarkMode != config.isDarkMode && activePreset.isDark != config.isDarkMode) {
            config.copy(activeThemeId = null)
        } else {
            config
        }
        _themeConfig.value = sanitized
        prefsManager.saveThemeConfig(sanitized)
        com.example.audio.CyberSoundscapeEngine.isEnabled = sanitized.soundEffectsEnabled
        com.example.audio.CyberHapticsEngine.isEnabled = sanitized.hapticFeedbackEnabled
        repository.updateCurrentCalibration(
            multiplier = sanitized.currentMultiplier,
            zeroOffsetMa = sanitized.currentZeroOffsetMa
        )
        if (sanitized.backgroundServiceEnabled) {
            com.example.service.TelemetryForegroundService.startService(getApplication())
            com.example.service.AlarmScheduler.scheduleNextAlarm(getApplication())
        } else {
            com.example.service.TelemetryForegroundService.stopService(getApplication())
            com.example.service.AlarmScheduler.cancelAlarm(getApplication())
        }
    }

    fun toggleThemeEngine(open: Boolean) {
        _isThemeEngineOpen.value = open
    }

    fun equipThemePreset(preset: com.example.ui.theme.AppThemePreset) {
        val argb = (preset.accentColor.value shr 32).toLong()
        val updated = _themeConfig.value.copy(
            activeThemeId = preset.id,
            isDarkMode = preset.isDark,
            customAccentHex = argb,
            glowIntensity = preset.glowIntensity
        )
        _themeConfig.value = updated
        prefsManager.saveThemeConfig(updated)
        com.example.audio.CyberHapticsEngine.triggerTactileTick(getApplication())
        com.example.audio.CyberSoundscapeEngine.playCryoChime()
        repository.addManualSessionLog(
            title = "Theme Engine: ${preset.name}",
            desc = "Applied ${preset.name} (${preset.category.labelAr}) across Ampere Pulse.",
            statTag = "THEME",
            type = "check"
        )
    }

    fun resetThemeEngineToDefault() {
        val updated = _themeConfig.value.copy(
            activeThemeId = null,
            customAccentHex = null,
            glowIntensity = 0.75f
        )
        _themeConfig.value = updated
        prefsManager.saveThemeConfig(updated)
        com.example.audio.CyberHapticsEngine.triggerTactileTick(getApplication())
        repository.addManualSessionLog(
            title = "Theme Reset",
            desc = "Restored default adaptive Eye-Comfort theme.",
            statTag = "THEME",
            type = "check"
        )
    }

    fun updateLauncherIcon(variant: com.example.data.LauncherIconVariant) {
        val current = _themeConfig.value
        if (current.launcherIconKey == variant.key) {
            com.example.data.AppIconManager.syncLauncherIcon(getApplication(), variant)
            return
        }
        val updated = current.copy(launcherIconKey = variant.key)
        _themeConfig.value = updated
        prefsManager.saveThemeConfig(updated)
        com.example.data.AppIconManager.applyLauncherIcon(getApplication(), variant)
        com.example.audio.CyberHapticsEngine.triggerTactileTick(getApplication())
        com.example.audio.CyberSoundscapeEngine.playCryoChime()
        repository.addManualSessionLog(
            title = "Launcher Icon Updated: ${variant.titleAr}",
            desc = "External home launcher icon color switched to ${variant.titleAr} (${variant.key}).",
            statTag = "ICON",
            type = "check"
        )
    }

    fun toggleSettingsMenu(open: Boolean) {
        _isSettingsMenuOpen.value = open
    }

    fun toggleSkinVault(open: Boolean) {
        _isSkinVaultOpen.value = open
    }

    fun toggleRedeemDialog(open: Boolean) {
        _isRedeemDialogOpen.value = open
    }

    fun redeemPremiumCode(rawCode: String): Boolean {
        val cleaned = rawCode.trim()
        if (cleaned == PREMIUM_REDEEM_CODE) {
            val updated = _themeConfig.value.copy(
                isPremiumUnlocked = true,
                appTier = com.example.data.AppTier.PREMIUM
            )
            _themeConfig.value = updated
            prefsManager.saveThemeConfig(updated)
            com.example.audio.CyberHapticsEngine.triggerBenchmarkSuccess(getApplication())
            com.example.audio.CyberSoundscapeEngine.playCryoChime()
            repository.addManualSessionLog(
                title = "Premium Tier Unlocked",
                desc = "Activated lifetime PREMIUM access via Redeem Code ($PREMIUM_REDEEM_CODE).",
                statTag = "PREMIUM",
                type = "check"
            )
            return true
        }
        return false
    }

    fun equipCyberSkin(skin: com.example.data.CyberSkin) {
        val argb = (skin.primaryColor.value shr 32).toLong()
        // Retain the current user's dark/light mode preference unchanged
        val updatedConfig = _themeConfig.value.copy(
            activeSkinId = skin.id,
            customAccentHex = argb,
            glowIntensity = skin.glowIntensity
            // isDarkMode remains completely unchanged as requested
        )
        _themeConfig.value = updatedConfig
        prefsManager.saveThemeConfig(updatedConfig)

        repository.addManualSessionLog(
            title = "Cyber Skin Equipped: ${skin.name}",
            desc = "Liquid Glass holographic aesthetic applied. Aura: ${skin.tag} (${skin.codename}).",
            statTag = skin.tag,
            type = "check"
        )
    }

    fun seedOrResetHourlyStats() {
        repository.seedOrResetHourlyStats()
    }

    fun selectLivingState(state: LivingBatteryState?) {
        repository.setLivingBatteryState(state)
    }

    fun setAutoSensorMode(auto: Boolean) {
        repository.setAutoSensorMode(auto)
    }

    fun triggerCrewAbility(crewId: String) {
        repository.triggerCrewAbility(crewId)
    }

    fun hatchEgg(): HatchedCompanion {
        return repository.hatchEgg()
    }

    fun addCumulativeMah(addedMah: Long) {
        repository.addCumulativeMah(addedMah)
    }

    fun claimMilestone(level: Int) {
        repository.claimMilestone(level)
    }

    fun repairShield() {
        repository.repairShield()
    }

    fun addManualLog(title: String, desc: String, statTag: String, type: String) {
        repository.addManualSessionLog(title, desc, statTag, type)
    }

    fun updateOwnerName(name: String) {
        repository.updateOwnerName(name)
    }

    fun toggleCloudSync(enabled: Boolean) {
        repository.toggleCloudSync(enabled)
    }

    fun runCableDiagnosticSweep(tierOverride: com.example.data.CableDnaTier? = null) {
        repository.runCableDiagnosticSweep(tierOverride)
    }

    fun recalibrateSohPrediction(addLog: Boolean = true) {
        repository.recalibrateSohPrediction(addLog = addLog)
    }

    fun toggleThermalSimulation(forceOverheat: Boolean? = null) {
        repository.toggleThermalSimulation(forceOverheat)
    }

    companion object {
        const val PREMIUM_REDEEM_CODE = "charfeddine23a"
    }
}
