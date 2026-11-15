package com.example.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R
import kotlin.system.exitProcess

data class IconSwitchDiagnostics(
    val activeAliasClassName: String,
    val launcherName: String,
    val launcherPackage: String,
    val isPinShortcutSupported: Boolean,
    val applySucceeded: Boolean = true,
    val statusMessage: String = ""
)

/**
 * Dynamically switches the Home Launcher external app icon via manifest <activity-alias> entries.
 * Supports both safe standard switching and an aggressive multi-layer "Force Change" engine
 * (alias state-cycling, dynamic ShortcutManager sync, Home launcher flush, flags=0 hard commit,
 * and direct Home Screen shortcut pinning) to break stubborn OEM launcher icon caches.
 */
object AppIconManager {

    private const val SHORTCUT_ID_FORCE_LAUNCH = "ampere_pulse_forced_icon_shortcut"

    fun applyLauncherIcon(context: Context, variant: LauncherIconVariant): IconSwitchDiagnostics {
        val appContext = context.applicationContext
        val pm = appContext.packageManager
        val pkgName = appContext.packageName

        val bridge = pickBridgeVariant(variant)
        val success = runCatching {
            val bridgeComponent = ComponentName(pkgName, bridge.aliasClassName)
            val targetComponent = ComponentName(pkgName, variant.aliasClassName)

            // Guarantee there is always an enabled launcher component during transitions
            pm.setComponentEnabledSetting(
                bridgeComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            // Cycle target off->on if it was already enabled so Android emits a fresh PACKAGE_CHANGED
            pm.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            LauncherIconVariant.entries.forEach { entry ->
                if (entry != variant) {
                    val comp = ComponentName(pkgName, entry.aliasClassName)
                    pm.setComponentEnabledSetting(
                        comp,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }
            syncDynamicShortcut(appContext, variant)
        }.isSuccess

        return getDiagnostics(appContext).copy(
            applySucceeded = success,
            statusMessage = if (success) {
                "تم إرسال أمر التحديث للنظام (${variant.titleAr})"
            } else {
                "تعذر تحديث مكون النظام مباشرة — جرّب التثبيت المباشر"
            }
        )
    }

    /**
     * Aggressive Force Change ("التغيير الإجباري"):
     * 1. Enables a temporary bridge alias so the app entry can never disappear.
     * 2. Cycles target alias DISABLED -> ENABLED to force a genuine PackageManager state transition.
     * 3. Disables all remaining aliases except the bridge.
     * 4. Syncs dynamic ShortcutManager icon bitmap.
     * 5. Navigates to the Home Launcher so the launcher comes to foreground, then disables the bridge
     *    with flags = 0 (omitting DONT_KILL_APP) to force synchronous ACTION_PACKAGE_CHANGED delivery
     *    and immediate cache purge across Android / OneUI / HyperOS / Pixel Launchers.
     */
    fun forceApplyLauncherIcon(
        context: Context,
        variant: LauncherIconVariant,
        launchHomeScreen: Boolean = true
    ): IconSwitchDiagnostics {
        val appContext = context.applicationContext
        val pm = appContext.packageManager
        val pkgName = appContext.packageName
        val bridge = pickBridgeVariant(variant)
        val bridgeComponent = ComponentName(pkgName, bridge.aliasClassName)
        val targetComponent = ComponentName(pkgName, variant.aliasClassName)

        val prepSuccess = runCatching {
            // Stage 1: Enable bridge first
            pm.setComponentEnabledSetting(
                bridgeComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            // Stage 2: Force clean target cycle (DISABLED -> ENABLED)
            pm.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            // Stage 3: Disable all other aliases except target and bridge
            LauncherIconVariant.entries.forEach { entry ->
                if (entry != variant && entry != bridge) {
                    pm.setComponentEnabledSetting(
                        ComponentName(pkgName, entry.aliasClassName),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }

            // Stage 4: Update dynamic shortcut icons
            syncDynamicShortcut(appContext, variant)

            // Stage 5: Bring Home Launcher to foreground before flags=0 hard commit
            if (launchHomeScreen) {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                appContext.startActivity(homeIntent)
            }
        }.isSuccess

        // Stage 6: Schedule final flags=0 bridge disable after Home Screen transitions into foreground.
        // Because bridge was explicitly ENABLED above, transitioning it to DISABLED with flags = 0
        // guarantees PackageManagerService flushes ACTION_PACKAGE_CHANGED synchronously and restarts/kills
        // the cached app process.
        Handler(Looper.getMainLooper()).postDelayed({
            runCatching {
                pm.setComponentEnabledSetting(
                    bridgeComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    0
                )
            }
            if (launchHomeScreen) {
                runCatching {
                    exitProcess(0)
                }
            }
        }, if (launchHomeScreen) 280L else 120L)

        return getDiagnostics(appContext).copy(
            applySucceeded = prepSuccess,
            statusMessage = "جاري تنفيذ التغيير الإجباري وتفريغ كاش المشغّل..."
        )
    }

    /**
     * Pins a direct Home Screen shortcut rendered with the exact chosen icon bitmap.
     * Bypasses OEM launcher icon-cache locks and Material You monochrome overrides.
     */
    fun pinForcedHomeShortcut(context: Context, variant: LauncherIconVariant): Boolean {
        val appContext = context.applicationContext
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(appContext)) {
            return false
        }
        return runCatching {
            val bitmap = renderVariantBitmap(appContext, variant, sizePx = 192)
            val iconCompat = if (bitmap != null) {
                IconCompat.createWithBitmap(bitmap)
            } else {
                IconCompat.createWithResource(appContext, variant.mipmapIconRes())
            }

            val launchIntent = Intent(appContext, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("forced_icon_variant", variant.key)
            }

            val shortcutInfo = ShortcutInfoCompat.Builder(
                appContext,
                "${SHORTCUT_ID_FORCE_LAUNCH}_${variant.key}"
            )
                .setShortLabel(appContext.getString(R.string.app_name))
                .setLongLabel("${appContext.getString(R.string.app_name)} (${variant.titleAr})")
                .setIcon(iconCompat)
                .setIntent(launchIntent)
                .build()

            ShortcutManagerCompat.requestPinShortcut(appContext, shortcutInfo, null)
        }.getOrDefault(false)
    }

    fun syncLauncherIcon(context: Context, variant: LauncherIconVariant) {
        val appContext = context.applicationContext
        val pm = appContext.packageManager
        val pkgName = appContext.packageName

        runCatching {
            val targetComponent = ComponentName(pkgName, variant.aliasClassName)
            val targetEnabled = pm.getComponentEnabledSetting(targetComponent) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED

            val otherMismatch = LauncherIconVariant.entries.any { entry ->
                entry != variant && pm.getComponentEnabledSetting(
                    ComponentName(pkgName, entry.aliasClassName)
                ) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }

            if (!targetEnabled || otherMismatch) {
                if (!targetEnabled) {
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
                syncDynamicShortcut(appContext, variant)
            }
        }
    }

    fun getDiagnostics(context: Context): IconSwitchDiagnostics {
        val appContext = context.applicationContext
        val pm = appContext.packageManager
        val pkgName = appContext.packageName

        val activeAlias = runCatching {
            LauncherIconVariant.entries.firstOrNull { entry ->
                val state = pm.getComponentEnabledSetting(ComponentName(pkgName, entry.aliasClassName))
                state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                    (entry == LauncherIconVariant.SAPPHIRE &&
                        state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
            }?.aliasClassName ?: LauncherIconVariant.SAPPHIRE.aliasClassName
        }.getOrDefault(LauncherIconVariant.SAPPHIRE.aliasClassName)

        var launcherLabel = "System Launcher"
        var launcherPkg = "default"
        runCatching {
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolveInfo = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            val activityInfo = resolveInfo?.activityInfo
            if (activityInfo != null) {
                launcherPkg = activityInfo.packageName ?: "default"
                launcherLabel = activityInfo.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() }
                    ?: launcherPkg
            }
        }

        val pinSupported = runCatching {
            ShortcutManagerCompat.isRequestPinShortcutSupported(appContext)
        }.getOrDefault(false)

        return IconSwitchDiagnostics(
            activeAliasClassName = activeAlias,
            launcherName = launcherLabel,
            launcherPackage = launcherPkg,
            isPinShortcutSupported = pinSupported
        )
    }

    private fun syncDynamicShortcut(context: Context, variant: LauncherIconVariant) {
        runCatching {
            val bitmap = renderVariantBitmap(context, variant, sizePx = 192)
            val iconCompat = if (bitmap != null) {
                IconCompat.createWithBitmap(bitmap)
            } else {
                IconCompat.createWithResource(context, variant.mipmapIconRes())
            }

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            val shortcut = ShortcutInfoCompat.Builder(context, "ampere_pulse_quick_open")
                .setShortLabel(context.getString(R.string.app_name))
                .setLongLabel("${context.getString(R.string.app_name)} • ${variant.titleAr}")
                .setIcon(iconCompat)
                .setIntent(launchIntent)
                .build()

            ShortcutManagerCompat.setDynamicShortcuts(context, listOf(shortcut))
        }
    }

    private fun renderVariantBitmap(context: Context, variant: LauncherIconVariant, sizePx: Int): Bitmap? {
        return runCatching {
            val drawable = ContextCompat.getDrawable(context, variant.foregroundDrawableRes())
                ?: ContextCompat.getDrawable(context, variant.mipmapIconRes())
                ?: return null
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            // Adaptive icon foreground vectors use a 108dp canvas with safe zone inside 72dp;
            // slightly over-draw bounds (-22%) so the emblem fills the shortcut cleanly.
            val bleed = (sizePx * 0.22f).toInt()
            drawable.setBounds(-bleed, -bleed, sizePx + bleed, sizePx + bleed)
            drawable.draw(canvas)
            bitmap
        }.getOrNull()
    }

    private fun pickBridgeVariant(target: LauncherIconVariant): LauncherIconVariant {
        return if (target == LauncherIconVariant.SAPPHIRE) {
            LauncherIconVariant.EMERALD
        } else {
            LauncherIconVariant.SAPPHIRE
        }
    }

    private fun LauncherIconVariant.mipmapIconRes(): Int = when (this) {
        LauncherIconVariant.SAPPHIRE -> R.mipmap.ic_launcher
        LauncherIconVariant.EMERALD -> R.mipmap.ic_launcher_emerald
        LauncherIconVariant.CRIMSON -> R.mipmap.ic_launcher_crimson
        LauncherIconVariant.VIOLET -> R.mipmap.ic_launcher_violet
        LauncherIconVariant.AURIC -> R.mipmap.ic_launcher_auric
        LauncherIconVariant.WHITE -> R.mipmap.ic_launcher_white
        LauncherIconVariant.BLACK -> R.mipmap.ic_launcher_black
    }

    private fun LauncherIconVariant.foregroundDrawableRes(): Int = when (this) {
        LauncherIconVariant.SAPPHIRE -> R.drawable.ic_launcher_foreground
        LauncherIconVariant.EMERALD -> R.drawable.ic_launcher_fg_emerald
        LauncherIconVariant.CRIMSON -> R.drawable.ic_launcher_fg_crimson
        LauncherIconVariant.VIOLET -> R.drawable.ic_launcher_fg_violet
        LauncherIconVariant.AURIC -> R.drawable.ic_launcher_fg_auric
        LauncherIconVariant.WHITE -> R.drawable.ic_launcher_fg_white
        LauncherIconVariant.BLACK -> R.drawable.ic_launcher_fg_black
    }
}
