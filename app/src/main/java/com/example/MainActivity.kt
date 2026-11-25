package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.SettingsPreferencesManager
import com.example.service.AlarmScheduler
import com.example.service.TelemetryForegroundService
import com.example.ui.AmperePulseApp
import com.example.ui.AmperePulseViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: AmperePulseViewModel by viewModels()

    private val inAppUpdateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onUpdateFlowResult(result.resultCode)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled gracefully; alerts and foreground notifications respect OS state */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel.attachUpdateLauncher(inAppUpdateLauncher)
        requestNotificationPermissionIfNeeded()

        setContent {
            val themeConfig by viewModel.themeConfig.collectAsStateWithLifecycle()
            LaunchedEffect(themeConfig.target120FpsMode) {
                applyDisplayRefreshRatePreference(enableHighRefresh = themeConfig.target120FpsMode)
            }
            AmperePulseApp(viewModel = viewModel)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val config = SettingsPreferencesManager(applicationContext).loadThemeConfig()
            val needsNotifications = config.backgroundServiceEnabled ||
                config.alertOn80Percent ||
                config.alertOnHighTemp
            if (needsNotifications &&
                androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun applyDisplayRefreshRatePreference(enableHighRefresh: Boolean) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            runCatching {
                val supportedModes = display?.supportedModes ?: emptyArray()
                val chosenMode = if (enableHighRefresh) {
                    supportedModes.maxByOrNull { it.refreshRate }?.takeIf { it.refreshRate >= 90f }
                } else {
                    supportedModes.firstOrNull { it.refreshRate in 58f..62f }
                        ?: supportedModes.minByOrNull { it.refreshRate }
                }
                if (chosenMode != null && window.attributes.preferredDisplayModeId != chosenMode.modeId) {
                    window.attributes = window.attributes.apply {
                        preferredDisplayModeId = chosenMode.modeId
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.setAppInForeground(true)
        val bgEnabled = SettingsPreferencesManager(applicationContext)
            .loadThemeConfig()
            .backgroundServiceEnabled
        if (bgEnabled) {
            TelemetryForegroundService.startService(applicationContext)
            AlarmScheduler.scheduleNextAlarm(applicationContext)
        } else {
            TelemetryForegroundService.stopService(applicationContext)
            AlarmScheduler.cancelAlarm(applicationContext)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResumeUpdateCheck()
        viewModel.refreshAppBatteryUsage(force = false)
    }

    override fun onStop() {
        super.onStop()
        viewModel.setAppInForeground(false)
    }
}
