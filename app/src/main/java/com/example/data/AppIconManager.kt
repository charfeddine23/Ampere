package com.example.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Dynamically switches the Home Launcher external app icon via manifest <activity-alias> entries.
 * Always enables the newly chosen alias before disabling the remaining aliases to keep the
 * launcher shortcut uninterrupted and never touches MainActivity's own component state.
 */
object AppIconManager {

    fun applyLauncherIcon(context: Context, variant: LauncherIconVariant) {
        val appContext = context.applicationContext
        val pm = appContext.packageManager
        val pkgName = appContext.packageName

        runCatching {
            val targetComponent = ComponentName(pkgName, variant.aliasClassName)
            if (pm.getComponentEnabledSetting(targetComponent) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                pm.setComponentEnabledSetting(
                    targetComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            }

            LauncherIconVariant.entries.forEach { entry ->
                if (entry != variant) {
                    val comp = ComponentName(pkgName, entry.aliasClassName)
                    if (pm.getComponentEnabledSetting(comp) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                        pm.setComponentEnabledSetting(
                            comp,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        )
                    }
                }
            }
        }
    }

    fun syncLauncherIcon(context: Context, variant: LauncherIconVariant) {
        applyLauncherIcon(context, variant)
    }
}
