package com.example.data.telemetry

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import kotlin.math.roundToInt

data class RawAppUsageMetric(
    val packageName: String,
    val appName: String,
    val foregroundTimeMs: Long,
    val wakeEventsCount: Int = 0,
    val powerMultiplier: Float = 1.0f,
    val isSystemApp: Boolean = false
)

data class AppBatteryConsumption(
    val rank: Int,
    val packageName: String,
    val appName: String,
    val foregroundTimeMs: Long,
    val consumptionPercent: Float,
    val estimatedMah: Int,
    val wakeEventsCount: Int,
    val isHighDrain: Boolean,
    val isSystemApp: Boolean
) {
    fun formattedDurationAr(): String = formattedDuration(isArabic = true)

    fun formattedDuration(isArabic: Boolean): String {
        val totalMinutes = (foregroundTimeMs / 60_000L).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (isArabic) {
            when {
                hours > 0 && minutes > 0 -> "${hours} س ${minutes} د"
                hours > 0 -> "${hours} ساعة"
                minutes > 0 -> "${minutes} دقيقة"
                foregroundTimeMs >= 1_000L -> "${(foregroundTimeMs / 1000L).toInt()} ثانية"
                else -> "< 1 ثانية"
            }
        } else {
            when {
                hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
                hours > 0 -> "${hours} hr"
                minutes > 0 -> "${minutes} min"
                foregroundTimeMs >= 1_000L -> "${(foregroundTimeMs / 1000L).toInt()}s"
                else -> "< 1s"
            }
        }
    }
}

data class AppUsageSnapshot(
    val hasPermission: Boolean = false,
    val isLoading: Boolean = false,
    val apps: List<AppBatteryConsumption> = emptyList(),
    val totalTrackedMah: Int = 0,
    val scanWindowHours: Int = 24,
    val lastUpdatedMs: Long = System.currentTimeMillis()
)

object AppBatteryUsageTracker {

    fun hasUsageStatsPermission(context: Context): Boolean {
        return runCatching {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            if (mode == AppOpsManager.MODE_ALLOWED) {
                return@runCatching true
            }
            if (mode == AppOpsManager.MODE_DEFAULT) {
                return@runCatching context.checkCallingOrSelfPermission("android.permission.PACKAGE_USAGE_STATS") == PackageManager.PERMISSION_GRANTED
            }
            false
        }.getOrDefault(false)
    }

    fun openUsageAccessSettings(context: Context) {
        val packageSpecificIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val genericIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(packageSpecificIntent)
        }.onFailure {
            runCatching { context.startActivity(genericIntent) }
        }
    }

    fun openAppBatteryDetailsSettings(context: Context, packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun collectUsageSnapshot(context: Context, batteryCapacityMah: Int = 5000): AppUsageSnapshot {
        val granted = hasUsageStatsPermission(context)
        if (!granted) {
            return AppUsageSnapshot(
                hasPermission = false,
                isLoading = false,
                apps = emptyList(),
                totalTrackedMah = 0
            )
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return AppUsageSnapshot(hasPermission = true, apps = emptyList(), totalTrackedMah = 0)

        val pm = context.packageManager
        val endTime = System.currentTimeMillis()
        val windowMs = 24L * 60L * 60L * 1000L
        val startTime = endTime - windowMs

        val aggregatedStats = runCatching {
            usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
        }.getOrDefault(emptyMap())

        // Count foreground transition wake events over window
        val wakeCounts = mutableMapOf<String, Int>()
        runCatching {
            val events = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                @Suppress("DEPRECATION")
                val isForegroundEvent = event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && event.eventType == UsageEvents.Event.ACTIVITY_RESUMED)
                if (isForegroundEvent && !event.packageName.isNullOrBlank()) {
                    wakeCounts[event.packageName] = (wakeCounts[event.packageName] ?: 0) + 1
                }
            }
        }

        val rawMetrics = mutableListOf<RawAppUsageMetric>()
        aggregatedStats.values.forEach { stats ->
            val pkg = stats.packageName ?: return@forEach
            val fgMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                maxOf(stats.totalTimeInForeground, stats.totalTimeVisible)
            } else {
                stats.totalTimeInForeground
            }
            val wakes = wakeCounts[pkg] ?: 0
            if (fgMs < 15_000L && wakes < 2) return@forEach

            val appInfo = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull() ?: return@forEach
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0

            // Exclude pure headless system daemons unless they actually hold launcher UI or notable time
            val hasLauncher = pm.getLaunchIntentForPackage(pkg) != null
            if (!hasLauncher && fgMs < 60_000L) return@forEach

            val appName = runCatching {
                pm.getApplicationLabel(appInfo).toString()
            }.getOrDefault(pkg.substringAfterLast('.'))

            val multiplier = estimateCategoryPowerMultiplier(appInfo, pkg)
            rawMetrics += RawAppUsageMetric(
                packageName = pkg,
                appName = appName,
                foregroundTimeMs = fgMs,
                wakeEventsCount = wakes,
                powerMultiplier = multiplier,
                isSystemApp = isSystem
            )
        }

        val sorted = calculateAndSortConsumption(rawMetrics, batteryCapacityMah)
        val totalMah = sorted.sumOf { it.estimatedMah }

        return AppUsageSnapshot(
            hasPermission = true,
            isLoading = false,
            apps = sorted,
            totalTrackedMah = totalMah,
            scanWindowHours = 24,
            lastUpdatedMs = System.currentTimeMillis()
        )
    }

    /**
     * Pure deterministic calculator & strict descending sorter for per-app battery drain.
     * Always ranks highest battery consumer first (Rank #1).
     */
    fun calculateAndSortConsumption(
        metrics: List<RawAppUsageMetric>,
        batteryCapacityMah: Int = 5000
    ): List<AppBatteryConsumption> {
        if (metrics.isEmpty()) return emptyList()

        // Weighted power score combines active duration (minutes), wake churn, and workload multiplier
        val scored = metrics.mapNotNull { item ->
            val activeMinutes = item.foregroundTimeMs / 60_000.0
            val wakePenalty = item.wakeEventsCount * 0.35
            val rawScore = (activeMinutes * item.powerMultiplier) + wakePenalty
            if (rawScore <= 0.0) null else item to rawScore
        }

        if (scored.isEmpty()) return emptyList()
        val totalScore = scored.sumOf { it.second }.coerceAtLeast(0.001)

        // Estimate realistic 24h battery energy spent proportional to active runtime & capacity
        val totalActiveMinutes = metrics.sumOf { it.foregroundTimeMs } / 60_000.0
        val estimatedTotalMah = ((totalActiveMinutes / 360.0).coerceIn(0.08, 1.35) * batteryCapacityMah)
            .roundToInt()
            .coerceAtLeast(1)

        return scored
            .sortedByDescending { it.second }
            .mapIndexed { index, (item, score) ->
                val share = (score / totalScore).toFloat().coerceIn(0f, 1f)
                val percent = ((share * 1000f).roundToInt() / 10f)
                val mah = (estimatedTotalMah * share).roundToInt().coerceAtLeast(1)
                AppBatteryConsumption(
                    rank = index + 1,
                    packageName = item.packageName,
                    appName = item.appName,
                    foregroundTimeMs = item.foregroundTimeMs,
                    consumptionPercent = percent,
                    estimatedMah = mah,
                    wakeEventsCount = item.wakeEventsCount,
                    isHighDrain = percent >= 18.0f || index == 0 && percent >= 12.0f,
                    isSystemApp = item.isSystemApp
                )
            }
            .sortedWith(
                compareByDescending<AppBatteryConsumption> { it.consumptionPercent }
                    .thenByDescending { it.estimatedMah }
                    .thenByDescending { it.foregroundTimeMs }
            )
            .mapIndexed { idx, app -> app.copy(rank = idx + 1) }
    }

    private fun estimateCategoryPowerMultiplier(info: ApplicationInfo, pkg: String): Float {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return when (info.category) {
                ApplicationInfo.CATEGORY_GAME -> 1.65f
                ApplicationInfo.CATEGORY_VIDEO -> 1.40f
                ApplicationInfo.CATEGORY_MAPS -> 1.38f
                ApplicationInfo.CATEGORY_IMAGE -> 1.25f
                ApplicationInfo.CATEGORY_SOCIAL -> 1.18f
                ApplicationInfo.CATEGORY_AUDIO -> 0.90f
                ApplicationInfo.CATEGORY_NEWS -> 0.95f
                else -> 1.0f
            }
        }
        val lower = pkg.lowercase()
        return when {
            lower.contains("camera") || lower.contains("maps") || lower.contains("game") -> 1.45f
            lower.contains("youtube") || lower.contains("tiktok") || lower.contains("instagram") -> 1.30f
            else -> 1.0f
        }
    }
}
