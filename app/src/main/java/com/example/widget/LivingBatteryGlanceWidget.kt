package com.example.widget

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.MainActivity
import com.example.data.BatteryMonitorRepository
import com.example.data.telemetry.CurrentCalibrationEngine
import java.util.Locale
import kotlin.math.roundToInt

/**
 * High-Fidelity Liquid Glass Home Screen Widget for Volt Core.
 * Built with Jetpack Glance, mirroring the Living Battery state (Storing, Heating, Cooling, Sleep)
 * with dynamic ambient glowing colors and real-time battery telemetry.
 */
class LivingBatteryGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("ampere_pulse_widget_prefs", Context.MODE_PRIVATE)
        val hasStoredData = prefs.contains("battery_pct")
        var pct = if (hasStoredData) prefs.getInt("battery_pct", -1) else -1
        var tempC = if (hasStoredData) prefs.getFloat("battery_temp", Float.NaN) else Float.NaN
        var voltageMv = if (hasStoredData) prefs.getInt("battery_voltage", 0) else 0
        var currentMa = if (hasStoredData) prefs.getInt("battery_current", 0) else 0
        var stateName = prefs.getString("battery_state", null)

        if (!hasStoredData || pct < 0 || voltageMv <= 0 || tempC.isNaN()) {
            val sticky = runCatching {
                context.applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            }.getOrNull()
            if (sticky != null) {
                val lvl = sticky.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = sticky.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (lvl >= 0 && scale > 0) {
                    pct = ((lvl / scale.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
                }
                val rawV = sticky.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
                voltageMv = BatteryMonitorRepository.normalizeVoltageMv(rawV.toLong())
                val rawT = sticky.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
                if (rawT != Int.MIN_VALUE) tempC = (rawT / 10f).coerceIn(-30f, 90f)
                val statusInt = sticky.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isFull = statusInt == BatteryManager.BATTERY_STATUS_FULL
                val isCharging = statusInt == BatteryManager.BATTERY_STATUS_CHARGING || isFull
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val cfg = com.example.data.local.SettingsPreferencesManager(context).loadThemeConfig()
                currentMa = CurrentCalibrationEngine(
                    context = context,
                    initialMultiplier = cfg.currentMultiplier,
                    initialOffsetMa = cfg.currentZeroOffsetMa
                ).sampleCurrentMa(bm, isCharging, voltageMv, isFull)
                stateName = when {
                    !tempC.isNaN() && tempC >= cfg.highTempThresholdC -> "OVERDRIVE_HEATING"
                    isCharging -> "ENERGY_STORING"
                    !tempC.isNaN() && tempC < 15.0f -> "COOLING"
                    else -> "DEEP_SLEEP"
                }
            }
        }
        val resolvedStateName = stateName ?: "DEEP_SLEEP"
        val isArabic = if (prefs.contains("is_arabic")) {
            prefs.getBoolean("is_arabic", true)
        } else {
            com.example.data.local.SettingsPreferencesManager(context).loadThemeConfig().appLanguage.isArabic
        }

        val (accentColor, stateTitle, stateSubtitle) = when (resolvedStateName) {
            "OVERDRIVE_HEATING" -> Triple(
                Color(0xFFFF3D00),
                if (isArabic) "إجهاد حراري" else "THERMAL STRESS",
                if (isArabic) "سقف JEITA ≥ 42°C" else "JEITA Ceiling ≥ 42°C"
            )
            "COOLING" -> Triple(
                Color(0xFF00B0FF),
                if (isArabic) "تبريد حراري" else "THERMAL RECOVERY",
                if (isArabic) "تبديد حراري نشط" else "Active Dissipation"
            )
            "ENERGY_STORING" -> Triple(
                Color(0xFF00C853),
                if (isArabic) "شحن نشط" else "ACTIVE CHARGING",
                if (isArabic) "نطاق CC/CV آمن" else "Safe CC/CV Window"
            )
            else -> Triple(
                Color(0xFF7C4DFF),
                if (isArabic) "وضع السكون" else "QUIESCENT STANDBY",
                if (isArabic) "استهلاك منخفض مستقر" else "Low C-Rate Preservation"
            )
        }

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(24.dp)
                        .background(Color(0xFF0F172A))
                        .clickable(actionStartActivity<MainActivity>())
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.Start
                    ) {
                        // Header: App Label & Status Beacon
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = GlanceModifier
                                    .size(8.dp)
                                    .cornerRadius(4.dp)
                                    .background(accentColor)
                            ) {}
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            Text(
                                text = if (isArabic) "فولت كور" else "VOLT CORE",
                                style = TextStyle(
                                    color = androidx.glance.unit.ColorProvider(Color(0xFF94A3B8)),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(6.dp))

                        // Percentage & Live Stats
                        val displayPct = pct.coerceIn(0, 100)
                        val pctText = if (pct >= 0) "$displayPct%" else "--"
                        val tempLabel = if (!tempC.isNaN()) "${String.format(Locale.US, "%.1f", tempC)}°C" else "--°C"
                        val voltLabel = if (voltageMv > 0) "${voltageMv}mV" else "--mV"
                        val currLabel = "${if (currentMa >= 0) "+$currentMa" else "$currentMa"}mA"

                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = pctText,
                                style = TextStyle(
                                    color = androidx.glance.unit.ColorProvider(Color.White),
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(10.dp))
                            Column {
                                Text(
                                    text = stateTitle,
                                    style = TextStyle(
                                        color = androidx.glance.unit.ColorProvider(accentColor),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "$tempLabel • $voltLabel • $currLabel",
                                    style = TextStyle(
                                        color = androidx.glance.unit.ColorProvider(Color(0xFFCBD5E1)),
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.height(8.dp))

                        // Ambient glowing state track bar scaled to live battery percentage
                        val litSegments = if (pct < 0) 0 else ((displayPct + 5) / 10).coerceIn(0, 10)
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .height(4.dp)
                        ) {
                            for (segmentIndex in 0 until 10) {
                                Box(
                                    modifier = GlanceModifier
                                        .defaultWeight()
                                        .height(4.dp)
                                        .cornerRadius(2.dp)
                                        .background(
                                            if (segmentIndex < litSegments) accentColor else Color(0xFF334155)
                                        )
                                ) {}
                                if (segmentIndex < 9) {
                                    Spacer(modifier = GlanceModifier.width(2.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class LivingBatteryGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LivingBatteryGlanceWidget()
}

object WidgetTelemetryUpdater {
    @Volatile
    private var lastUpdateTimeMs: Long = 0L

    @Volatile
    private var lastPct: Int = -1

    @Volatile
    private var lastStateName: String = ""

    suspend fun update(context: Context, pct: Int, tempC: Float, voltageMv: Int, currentMa: Int, stateName: String) {
        val now = System.currentTimeMillis()
        val stateChanged = pct != lastPct || stateName != lastStateName
        val minCooldownElapsed = (now - lastUpdateTimeMs) >= 3_000L
        val periodicIntervalElapsed = (now - lastUpdateTimeMs) >= 15_000L

        if (!(periodicIntervalElapsed || (stateChanged && minCooldownElapsed))) {
            return
        }

        val appContext = context.applicationContext
        val isArabic = com.example.data.local.SettingsPreferencesManager(appContext).loadThemeConfig().appLanguage.isArabic
        val prefs = appContext.getSharedPreferences("ampere_pulse_widget_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("battery_pct", pct)
            .putFloat("battery_temp", tempC)
            .putInt("battery_voltage", voltageMv)
            .putInt("battery_current", currentMa)
            .putString("battery_state", stateName)
            .putBoolean("is_arabic", isArabic)
            .apply()

        try {
            val glanceIds = GlanceAppWidgetManager(appContext).getGlanceIds(LivingBatteryGlanceWidget::class.java)
            if (glanceIds.isEmpty()) {
                lastUpdateTimeMs = now
                lastPct = pct
                lastStateName = stateName
                return
            }

            lastUpdateTimeMs = now
            lastPct = pct
            lastStateName = stateName
            LivingBatteryGlanceWidget().updateAll(appContext)
        } catch (_: Exception) {}
    }

    suspend fun forceRefresh(context: Context, isArabic: Boolean) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences("ampere_pulse_widget_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_arabic", isArabic).apply()
        try {
            val glanceIds = GlanceAppWidgetManager(appContext).getGlanceIds(LivingBatteryGlanceWidget::class.java)
            if (glanceIds.isNotEmpty()) {
                LivingBatteryGlanceWidget().updateAll(appContext)
            }
        } catch (_: Exception) {}
    }
}
