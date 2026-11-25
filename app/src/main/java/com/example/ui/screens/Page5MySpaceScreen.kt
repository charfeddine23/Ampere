package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.CyberHapticsEngine
import com.example.data.BatteryStatus
import com.example.data.BatteryTelemetry
import com.example.data.DeviceHardwareProfile
import com.example.data.GlassThemeConfig
import com.example.data.LivingBatteryState
import com.example.data.MetricType
import com.example.data.SessionLog
import com.example.data.TelemetryPoint
import com.example.data.TemperatureUnit
import com.example.data.UserAccount
import com.example.data.telemetry.BatteryScienceClassifier
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.MetricSparkline
import com.example.ui.components.PulsingNeonDot
import com.example.ui.components.VoltSectionHeader
import com.example.ui.theme.LocalAmpereTheme
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.TactileCoralRed
import com.example.ui.theme.TactileGreen
import com.example.ui.theme.TactileRoyalBlue
import com.example.ui.theme.TextCharcoal
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/**
 * PAGE 5: My Space Telemetry Workspace & Account Center
 * Clean executive console featuring:
 * 1. Crisp Workspace Profile card with adaptive charging-state ring
 * 2. Minimalist Live Flow velocity bar
 * 3. Interactive 2x2 real-time sparkline telemetry cards
 * 4. Expandable Live Telemetry Inspector console
 * 5. Vital Pulse Hardware Log with instant Benchmark trigger
 */
@Composable
fun Page5MySpaceScreen(
    telemetry: BatteryTelemetry,
    voltageHistory: List<TelemetryPoint>,
    currentHistory: List<TelemetryPoint>,
    wattageHistory: List<TelemetryPoint>,
    temperatureHistory: List<TelemetryPoint>,
    sessionLogs: List<SessionLog>,
    themeConfig: GlassThemeConfig,
    userAccount: UserAccount = UserAccount(),
    deviceProfile: DeviceHardwareProfile = DeviceHardwareProfile(),
    onUpdateOwnerName: (String) -> Unit = {},
    onOpenRedeemDialog: () -> Unit = {},
    onAddVitalLog: (String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val isDark = themeConfig.isDarkMode
    val isArabic = themeConfig.appLanguage.isArabic
    val resolvedTheme = LocalAmpereTheme.current
    val customTokens = resolvedTheme.themeId != null || resolvedTheme.isSimpleMode || resolvedTheme.isProMode

    val baseAccent = if (customTokens) resolvedTheme.accent else themeConfig.accentColor
    val glow = when {
        resolvedTheme.isSimpleMode -> 0f
        resolvedTheme.isProMode -> 0.35f
        else -> themeConfig.glowIntensity
    }
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    val textColor = if (customTokens) resolvedTheme.textPrimary else if (isDark) TextWhite else TextCharcoal
    val subTextColor = if (customTokens) resolvedTheme.textSecondary else if (isDark) Color(0xFF94A3B8) else TextSlate

    var selectedMetric by remember { mutableStateOf(MetricType.VOLTAGE) }
    var showVoltageInVolts by remember { mutableStateOf(false) }
    var showProfileEditDialog by remember { mutableStateOf(false) }
    val activeCurrentMa = telemetry.currentMa

    val isHighStress = activeCurrentMa > 2500 || telemetry.temperatureC >= BatteryScienceClassifier.TEMP_THERMAL_STRESS_C
    val isSafeFlow = activeCurrentMa in 400..2500 && !isHighStress

    val targetAmbientColor = when {
        resolvedTheme.isSimpleMode -> resolvedTheme.accent
        isHighStress -> TactileCoralRed
        isSafeFlow -> TactileGreen
        activeCurrentMa in 0..399 -> TactileRoyalBlue
        activeCurrentMa < -600 -> Color(0xFFA855F7)
        else -> Color(0xFF00B0FF)
    }

    val animatedAmbientColor by animateColorAsState(
        targetValue = targetAmbientColor,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "ambientAccentColor"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { clip = false }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 24.dp)
                .graphicsLayer { clip = false }
        ) {
            // 1. Clean Executive Workspace Profile Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = animatedAmbientColor,
                    isDarkMode = isDark,
                    shape = RoundedCornerShape(20.dp),
                    glowIntensity = glow,
                    onClick = { showProfileEditDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            ChargingStateNeonProfileAvatar(
                                userAccount = userAccount,
                                telemetry = telemetry,
                                isDarkMode = isDark,
                                onClick = { showProfileEditDialog = true }
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = userAccount.ownerName,
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.5.sp,
                                        color = textColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = if (isArabic) "حساب موثّق" else "Verified Account",
                                        tint = themeConfig.appTier.displayColor(isDark),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                val tierColor = themeConfig.appTier.displayColor(isDark)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(tierColor)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Volt Core • ${themeConfig.appTier.localizedLabel(isArabic)}",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = tierColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val badgeColor = themeConfig.appTier.displayColor(isDark)
                            if (!themeConfig.isPremiumUnlocked) {
                                val goldTint = if (isDark) Color(0xFFFFD700) else Color(0xFFB77900)
                                Box(
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .heightIn(min = 36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(goldTint.copy(alpha = if (isDark) 0.16f else 0.12f))
                                        .border(1.dp, goldTint.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            CyberHapticsEngine.triggerTactileTick(context)
                                            onOpenRedeemDialog()
                                        }
                                        .padding(horizontal = 9.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.VpnKey,
                                            contentDescription = null,
                                            tint = goldTint,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isArabic) "تفعيل" else "Redeem",
                                            fontFamily = OutfitFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp,
                                            color = goldTint
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .heightIn(min = 36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(badgeColor.copy(alpha = if (isDark) 0.18f else 0.12f))
                                    .border(1.dp, badgeColor.copy(alpha = 0.42f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        CyberHapticsEngine.triggerTactileTick(context)
                                        showProfileEditDialog = true
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isArabic) "تعديل" else "Edit",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = badgeColor
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Minimalist Live Flow Velocity Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                CurrentFlowVfxBar(
                    currentMa = activeCurrentMa,
                    ambientColor = animatedAmbientColor,
                    isDarkMode = isDark,
                    isArabic = isArabic,
                    subTextColor = subTextColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Interactive 2x2 Sparkline Metric Selector Grid + Live Inspector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isDualCell = telemetry.voltageMv > 6000 || deviceProfile.isDualCellSuspected
                    val hasValidVoltage = telemetry.voltageMv > 0
                    val voltageValueText = when {
                        !hasValidVoltage -> "--"
                        showVoltageInVolts -> String.format(Locale.US, "%.3f", telemetry.voltageMv / 1000f)
                        else -> numberFormat.format(telemetry.voltageMv)
                    }
                    val voltageUnitText = if (showVoltageInVolts) "V" else "mV"
                    val voltageSubtext = when {
                        !hasValidVoltage -> if (isArabic) "بانتظار القراءة..." else "Awaiting sensor..."
                        isDualCell -> if (isArabic) "القياسي 6.8V–8.9V (2S)" else "Nominal 6.8–8.9V (2S)"
                        else -> if (isArabic) "القياسي 3.4V–4.48V (1S)" else "Nominal 3.4–4.48V (1S)"
                    }
                    MySpaceMetricCard(
                        modifier = Modifier.weight(1f),
                        title = if (isArabic) "الجهد الكهربائي" else "VOLTAGE",
                        value = voltageValueText,
                        unit = voltageUnitText,
                        subtext = voltageSubtext,
                        points = voltageHistory,
                        minVisualSpan = 20f,
                        accentColor = if (selectedMetric == MetricType.VOLTAGE) animatedAmbientColor else baseAccent,
                        isDarkMode = isDark,
                        glowIntensity = glow,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        isSelected = selectedMetric == MetricType.VOLTAGE,
                        onClick = {
                            CyberHapticsEngine.triggerTactileTick(context)
                            if (selectedMetric == MetricType.VOLTAGE) {
                                showVoltageInVolts = !showVoltageInVolts
                            } else {
                                selectedMetric = MetricType.VOLTAGE
                            }
                        }
                    )

                    val currentSign = if (activeCurrentMa >= 0) "+" else ""
                    MySpaceMetricCard(
                        modifier = Modifier.weight(1f),
                        title = if (isArabic) "شدة التيار" else "CURRENT",
                        value = "$currentSign${numberFormat.format(activeCurrentMa)}",
                        unit = "mA",
                        subtext = if (isArabic) {
                            if (activeCurrentMa >= 0) "تدفق شحن داخل" else "سحب تفريغ الطاقة"
                        } else {
                            if (activeCurrentMa >= 0) "Inflow charge" else "Discharge draw"
                        },
                        points = currentHistory,
                        minVisualSpan = 180f,
                        accentColor = if (selectedMetric == MetricType.CURRENT) animatedAmbientColor else baseAccent,
                        isDarkMode = isDark,
                        glowIntensity = glow,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        isSelected = selectedMetric == MetricType.CURRENT,
                        onClick = {
                            CyberHapticsEngine.triggerTactileTick(context)
                            selectedMetric = MetricType.CURRENT
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val instantaneousWattage = (telemetry.voltageMv / 1000f) * (abs(activeCurrentMa) / 1000f)
                    MySpaceMetricCard(
                        modifier = Modifier.weight(1f),
                        title = if (isArabic) "القدرة الكهربائية" else "WATTAGE",
                        value = String.format(Locale.US, "%.2f", instantaneousWattage),
                        unit = "W",
                        subtext = if (isArabic) "P = V × I قياس حي" else "P = V × I Real-time",
                        points = wattageHistory,
                        minVisualSpan = 1.2f,
                        accentColor = if (selectedMetric == MetricType.WATTAGE) animatedAmbientColor else baseAccent,
                        isDarkMode = isDark,
                        glowIntensity = glow,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        isSelected = selectedMetric == MetricType.WATTAGE,
                        onClick = {
                            CyberHapticsEngine.triggerTactileTick(context)
                            selectedMetric = MetricType.WATTAGE
                        }
                    )

                    val displayTemp = if (themeConfig.temperatureUnit == TemperatureUnit.FAHRENHEIT) {
                        (telemetry.temperatureC * 9f / 5f) + 32f
                    } else {
                        telemetry.temperatureC
                    }
                    val isThermalStress = telemetry.temperatureC >= BatteryScienceClassifier.TEMP_THERMAL_STRESS_C
                    val tempAccent = if (isThermalStress) TactileCoralRed else baseAccent
                    MySpaceMetricCard(
                        modifier = Modifier.weight(1f),
                        title = if (isArabic) "درجة الحرارة" else "TEMPERATURE",
                        value = String.format(Locale.US, "%.1f", displayTemp),
                        unit = themeConfig.temperatureUnit.symbol,
                        subtext = if (isArabic) {
                            if (isThermalStress) "إجهاد حراري (≥42°C)" else "نطاق JEITA المثالي"
                        } else {
                            if (isThermalStress) "Thermal Stress (≥42°C)" else "JEITA Nominal Zone"
                        },
                        points = temperatureHistory,
                        minVisualSpan = 2.0f,
                        accentColor = if (selectedMetric == MetricType.TEMPERATURE) animatedAmbientColor else tempAccent,
                        isDarkMode = isDark,
                        glowIntensity = glow,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        isSelected = selectedMetric == MetricType.TEMPERATURE,
                        onClick = {
                            CyberHapticsEngine.triggerTactileTick(context)
                            selectedMetric = MetricType.TEMPERATURE
                        }
                    )
                }

                // 4. Live Telemetry Inspector Console
                SelectedMetricTelemetryConsole(
                    selectedMetric = selectedMetric,
                    telemetry = telemetry,
                    deviceProfile = deviceProfile,
                    temperatureUnit = themeConfig.temperatureUnit,
                    voltageHistory = voltageHistory,
                    currentHistory = currentHistory,
                    wattageHistory = wattageHistory,
                    temperatureHistory = temperatureHistory,
                    showVoltageInVolts = showVoltageInVolts,
                    onToggleVoltageUnit = {
                        CyberHapticsEngine.triggerTactileTick(context)
                        showVoltageInVolts = !showVoltageInVolts
                    },
                    accentColor = animatedAmbientColor,
                    isDarkMode = isDark,
                    glowIntensity = glow,
                    textColor = textColor,
                    subTextColor = subTextColor,
                    isArabic = isArabic,
                    numberFormat = numberFormat
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Vital Pulse Hardware Log Section
            VoltSectionHeader(
                title = if (isArabic) "سجل نبضات العتاد الحيوي" else "Vital Pulse Hardware Log",
                isDark = isDark,
                trailing = {
                    Box(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(animatedAmbientColor.copy(alpha = if (isDark) 0.18f else 0.12f))
                            .border(1.dp, animatedAmbientColor.copy(alpha = 0.50f), RoundedCornerShape(10.dp))
                            .clickable {
                                CyberHapticsEngine.triggerTactileTick(context)
                                if (isArabic) {
                                    onAddVitalLog(
                                        "فحص تشخيصي فوري للبطارية",
                                        "تم فحص مسجلات الطاقة بنجاح: ${telemetry.voltageMv}mV، ${activeCurrentMa}mA، ${String.format(Locale.US, "%.1f", telemetry.temperatureC)}°C.",
                                        "ناجح",
                                        "check"
                                    )
                                } else {
                                    onAddVitalLog(
                                        "Vital Pulse Diagnostics Sweep",
                                        "Battery hardware registers verified: ${telemetry.voltageMv}mV, ${activeCurrentMa}mA, ${String.format(Locale.US, "%.1f", telemetry.temperatureC)}°C.",
                                        "PASS OK",
                                        "check"
                                    )
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AddCircleOutline,
                                contentDescription = null,
                                tint = animatedAmbientColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isArabic) "فحص الآن" else "Benchmark",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = animatedAmbientColor
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sessionLogs.forEach { log ->
                    VitalLogItem(
                        log = log,
                        accentColor = animatedAmbientColor,
                        isDarkMode = isDark,
                        glow = glow,
                        textColor = textColor,
                        subTextColor = subTextColor
                    )
                }
            }
        }

        if (showProfileEditDialog) {
            UserProfileEditDialog(
                userAccount = userAccount,
                isDarkMode = isDark,
                isArabic = isArabic,
                onDismiss = { showProfileEditDialog = false },
                onUpdateOwnerName = { name ->
                    onUpdateOwnerName(name)
                }
            )
        }
    }
}

/**
 * Minimalist Live Flow Velocity Bar without jittery particle overload.
 */
@Composable
private fun CurrentFlowVfxBar(
    currentMa: Int,
    ambientColor: Color,
    isDarkMode: Boolean,
    isArabic: Boolean,
    subTextColor: Color
) {
    val isCharging = currentMa >= 0
    val absCurrent = abs(currentMa)

    val stateTitle = if (isArabic) {
        when {
            currentMa > 2500 -> "اندفاع شحن عالٍ (+${currentMa} mA)"
            currentMa in 400..2500 -> "شحن آمن ومستقر (+${currentMa} mA)"
            currentMa in 0..399 -> "شحن هادئ منخفض (+${currentMa} mA)"
            currentMa < -600 -> "استهلاك حمل مرتفع (${currentMa} mA)"
            else -> "سحب نظام خامل (${currentMa} mA)"
        }
    } else {
        when {
            currentMa > 2500 -> "High Inflow Surge (+${currentMa} mA)"
            currentMa in 400..2500 -> "Stable Safe Charging (+${currentMa} mA)"
            currentMa in 0..399 -> "Gentle Trickle Flow (+${currentMa} mA)"
            currentMa < -600 -> "Heavy Load Drain (${currentMa} mA)"
            else -> "Idle System Draw (${currentMa} mA)"
        }
    }

    val isSimpleMode = LocalAmpereTheme.current.isSimpleMode
    val flowPhase = if (isSimpleMode) {
        0f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "electronFlow")
        val animatedFlow by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(if (absCurrent > 2000) 1100 else 2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "flowPhase"
        )
        animatedFlow
    }

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        accentColor = ambientColor,
        isDarkMode = isDarkMode,
        glowIntensity = if (isSimpleMode) 0f else 0.45f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Waves,
                        contentDescription = null,
                        tint = ambientColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stateTitle,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = ambientColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (isArabic) {
                        if (isCharging) "تدفق داخل →" else "← تدفق خارج"
                    } else {
                        if (isCharging) "INFLOW →" else "← OUTFLOW"
                    },
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = subTextColor
                )
            }

            Spacer(modifier = Modifier.height(7.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
            ) {
                val w = size.width
                val h = size.height
                val radius = CornerRadius(h / 2f, h / 2f)

                drawRoundRect(
                    color = if (isDarkMode) Color(0xFF28364B) else Color(0xFFCFD9E6),
                    topLeft = Offset.Zero,
                    size = Size(w, h),
                    cornerRadius = radius
                )

                val loadRatio = (absCurrent / 4500f).coerceIn(0.18f, 0.96f)
                val barW = w * loadRatio
                val startX = if (isCharging) 0f else w - barW
                drawRoundRect(
                    color = ambientColor.copy(alpha = 0.78f),
                    topLeft = Offset(startX, 0f),
                    size = Size(barW, h),
                    cornerRadius = radius
                )

                if (!isSimpleMode) {
                    val pulseFrac = if (isCharging) flowPhase else (1f - flowPhase)
                    val pulseW = (barW * 0.22f).coerceAtLeast(18.dp.toPx())
                    val pulseX = startX + (barW - pulseW) * pulseFrac
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.65f),
                        topLeft = Offset(pulseX, 0f),
                        size = Size(pulseW, h),
                        cornerRadius = radius
                    )
                }
            }
        }
    }
}

@Composable
fun MySpaceMetricCard(
    title: String,
    value: String,
    unit: String,
    subtext: String,
    points: List<TelemetryPoint>,
    modifier: Modifier = Modifier,
    minVisualSpan: Float = 1.0f,
    accentColor: Color,
    isDarkMode: Boolean,
    glowIntensity: Float,
    textColor: Color = if (isDarkMode) TextWhite else TextCharcoal,
    subTextColor: Color = TextSlate,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    LiquidGlassCard(
        modifier = modifier
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 1.4.dp,
                        color = accentColor.copy(alpha = if (isDarkMode) 0.72f else 0.58f),
                        shape = RoundedCornerShape(18.dp)
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(18.dp),
        accentColor = accentColor,
        isDarkMode = isDarkMode,
        glowIntensity = if (isSelected) glowIntensity.coerceAtLeast(0.55f) else glowIntensity * 0.6f,
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    letterSpacing = 0.4.sp,
                    color = if (isSelected) accentColor else subTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                PulsingNeonDot(color = accentColor, sizeDp = if (isSelected) 5.5.dp else 4.5.dp)
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = unit,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp,
                    color = accentColor,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Text(
                text = subtext,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 9.5.sp,
                color = subTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            MetricSparkline(
                points = points,
                lineColor = accentColor,
                isDarkMode = isDarkMode,
                glowIntensity = glowIntensity,
                heightDp = 42.dp,
                minVisualSpan = minVisualSpan
            )
        }
    }
}

@Composable
private fun SelectedMetricTelemetryConsole(
    selectedMetric: MetricType,
    telemetry: BatteryTelemetry,
    deviceProfile: DeviceHardwareProfile,
    temperatureUnit: TemperatureUnit,
    voltageHistory: List<TelemetryPoint>,
    currentHistory: List<TelemetryPoint>,
    wattageHistory: List<TelemetryPoint>,
    temperatureHistory: List<TelemetryPoint>,
    showVoltageInVolts: Boolean,
    onToggleVoltageUnit: () -> Unit,
    accentColor: Color,
    isDarkMode: Boolean,
    glowIntensity: Float,
    textColor: Color,
    subTextColor: Color,
    isArabic: Boolean,
    numberFormat: NumberFormat
) {
    val historyPoints = when (selectedMetric) {
        MetricType.VOLTAGE -> voltageHistory
        MetricType.CURRENT -> currentHistory
        MetricType.WATTAGE -> wattageHistory
        MetricType.TEMPERATURE -> temperatureHistory
    }
    val span = when (selectedMetric) {
        MetricType.VOLTAGE -> 20f
        MetricType.CURRENT -> 180f
        MetricType.WATTAGE -> 1.2f
        MetricType.TEMPERATURE -> 2.0f
    }

    val isDualCell = telemetry.voltageMv > 6000 || deviceProfile.isDualCellSuspected
    val rawValues = historyPoints.map {
        if (selectedMetric == MetricType.TEMPERATURE && temperatureUnit == TemperatureUnit.FAHRENHEIT) {
            (it.value * 9f / 5f) + 32f
        } else {
            it.value
        }
    }
    val minVal = rawValues.minOrNull()
    val maxVal = rawValues.maxOrNull()
    val deltaVal = if (minVal != null && maxVal != null) maxVal - minVal else 0f

    fun formatValue(v: Float): String = when (selectedMetric) {
        MetricType.VOLTAGE -> if (showVoltageInVolts) {
            String.format(Locale.US, "%.3f V", v / 1000f)
        } else {
            "${numberFormat.format(v.toInt())} mV"
        }
        MetricType.CURRENT -> "${numberFormat.format(v.toInt())} mA"
        MetricType.WATTAGE -> String.format(Locale.US, "%.2f W", v)
        MetricType.TEMPERATURE -> String.format(Locale.US, "%.1f %s", v, temperatureUnit.symbol)
    }

    val headlineValue = when (selectedMetric) {
        MetricType.VOLTAGE -> if (telemetry.voltageMv <= 0) "--" else {
            if (showVoltageInVolts) {
                "${String.format(Locale.US, "%.3f", telemetry.voltageMv / 1000f)} V  (${numberFormat.format(telemetry.voltageMv)} mV)"
            } else {
                "${numberFormat.format(telemetry.voltageMv)} mV  (${String.format(Locale.US, "%.3f", telemetry.voltageMv / 1000f)} V)"
            }
        }
        MetricType.CURRENT -> "${if (telemetry.currentMa >= 0) "+" else ""}${numberFormat.format(telemetry.currentMa)} mA"
        MetricType.WATTAGE -> String.format(Locale.US, "%.2f W", telemetry.wattageW)
        MetricType.TEMPERATURE -> {
            val dispT = if (temperatureUnit == TemperatureUnit.FAHRENHEIT) (telemetry.temperatureC * 9f / 5f) + 32f else telemetry.temperatureC
            String.format(Locale.US, "%.1f %s", dispT, temperatureUnit.symbol)
        }
    }

    val statusText = when (selectedMetric) {
        MetricType.VOLTAGE -> when {
            telemetry.voltageMv <= 0 -> if (isArabic) "بانتظار إشارة المستشعر" else "Waiting for sensor"
            telemetry.voltageMv > (if (isDualCell) 9050 else 4520) -> if (isArabic) "تحذير: جهد مرتفع" else "Warning: Over-Voltage"
            telemetry.voltageMv < (if (isDualCell) 6400 else 3250) -> if (isArabic) "جهد منخفض" else "Low Cell Voltage"
            else -> if (isArabic) "ضمن النطاق القياسي الآمن" else "Within IEEE Safe Window"
        }
        MetricType.CURRENT -> if (telemetry.currentMa >= 0) {
            if (isArabic) "شحن نشط • +${telemetry.currentMa} mA" else "Active Inflow • +${telemetry.currentMa} mA"
        } else {
            if (isArabic) "سحب تفريغ • ${telemetry.currentMa} mA" else "Discharge Draw • ${telemetry.currentMa} mA"
        }
        MetricType.WATTAGE -> if (isArabic) "كفاءة الطاقة اللحظية P = V × I" else "Instant Power P = V × I"
        MetricType.TEMPERATURE -> if (telemetry.temperatureC >= BatteryScienceClassifier.TEMP_THERMAL_STRESS_C) {
            if (isArabic) "إجهاد حراري مرتفع" else "Thermal Stress"
        } else {
            if (isArabic) "استقرار حراري مثالي" else "Optimal Thermal Zone"
        }
    }

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.88f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
        shape = RoundedCornerShape(20.dp),
        accentColor = accentColor,
        isDarkMode = isDarkMode,
        glowIntensity = glowIntensity
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) {
                            "المحلل الحي: ${selectedMetric.localizedTitle(true)}"
                        } else {
                            "LIVE INSPECTOR • ${selectedMetric.localizedTitle(false).uppercase()}"
                        },
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        letterSpacing = 0.5.sp,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = headlineValue,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (selectedMetric == MetricType.VOLTAGE) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor.copy(alpha = if (isDarkMode) 0.18f else 0.12f))
                            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                            .clickable { onToggleVoltageUnit() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (showVoltageInVolts) "mV ⇄ V" else "V ⇄ mV",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            color = accentColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            MetricSparkline(
                points = historyPoints,
                lineColor = accentColor,
                isDarkMode = isDarkMode,
                glowIntensity = glowIntensity,
                heightDp = 68.dp,
                minVisualSpan = span
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (minVal != null && maxVal != null) {
                        if (isArabic) {
                            "الأدنى ${formatValue(minVal)} • الأعلى ${formatValue(maxVal)} • التغيّر ${formatValue(deltaVal)}"
                        } else {
                            "Min ${formatValue(minVal)} • Max ${formatValue(maxVal)} • Δ ${formatValue(deltaVal)}"
                        }
                    } else {
                        statusText
                    },
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 10.sp,
                    color = subTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusText,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    color = accentColor
                )
            }
        }
    }
}

@Composable
fun VitalLogItem(
    log: SessionLog,
    accentColor: Color,
    isDarkMode: Boolean,
    glow: Float,
    textColor: Color = if (isDarkMode) TextWhite else TextCharcoal,
    subTextColor: Color = if (isDarkMode) Color(0xFF94A3B8) else TextSlate
) {
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        accentColor = accentColor,
        isDarkMode = isDarkMode,
        glowIntensity = glow * 0.5f
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = if (isDarkMode) 0.18f else 0.12f))
                    .border(1.dp, accentColor.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (log.type) {
                        "charge" -> Icons.Default.Bolt
                        "temp" -> Icons.Default.Thermostat
                        "discharge" -> Icons.Default.Timeline
                        else -> Icons.Default.History
                    },
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(17.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.title,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = log.timestampFormatted,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 9.5.sp,
                        color = subTextColor
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = log.description,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 10.5.sp,
                    color = subTextColor,
                    lineHeight = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = if (isDarkMode) 0.16f else 0.10f))
                    .border(0.8.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = log.statTag,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = accentColor
                )
            }
        }
    }
}

@Composable
fun UserProfileEditDialog(
    userAccount: UserAccount,
    isDarkMode: Boolean,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onUpdateOwnerName: (String) -> Unit
) {
    var ownerNameInput by remember { mutableStateOf(userAccount.ownerName) }
    val resolvedTheme = LocalAmpereTheme.current
    val customTokens = resolvedTheme.themeId != null || resolvedTheme.isSimpleMode || resolvedTheme.isProMode

    val cardBg = if (customTokens) resolvedTheme.surface else if (isDarkMode) Color(0xFF1E293B) else Color(0xFFECF1F7)
    val textColor = if (customTokens) resolvedTheme.textPrimary else if (isDarkMode) TextWhite else TextCharcoal
    val subTextColor = if (customTokens) resolvedTheme.textSecondary else if (isDarkMode) Color(0xFF94A3B8) else TextSlate
    val insetBg = if (customTokens) resolvedTheme.surfaceInset else if (isDarkMode) Color(0xFF0F172A) else Color(0xFFD8E1EC)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardBg,
        titleContentColor = textColor,
        textContentColor = subTextColor,
        shape = RoundedCornerShape(22.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(TactileRoyalBlue.copy(alpha = if (isDarkMode) 0.22f else 0.12f))
                        .border(1.dp, TactileRoyalBlue.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = TactileRoyalBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isArabic) "ملف المستخدم" else "User Profile",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = textColor
                    )
                    Text(
                        text = if (isArabic) "النواة المحلية لـ Volt Core" else "Volt Core Local Profile",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 10.5.sp,
                        color = subTextColor
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(TactileGreen.copy(alpha = if (isDarkMode) 0.16f else 0.10f))
                        .border(1.dp, TactileGreen.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = TactileGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isArabic) "حساب عتاد محلي نشط 100%" else "100% Local Hardware Profile",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = TactileGreen
                            )
                            Text(
                                text = if (isArabic) {
                                    "محفوظة محلياً داخل مساحة التطبيق الخاصة (Room WAL)"
                                } else {
                                    "Stored privately on your device (Room WAL)"
                                },
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 10.5.sp,
                                color = subTextColor
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = if (isArabic) "اسم صاحب الحساب" else "Owner Display Name",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = ownerNameInput,
                        onValueChange = { ownerNameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            Text(if (isArabic) "أدخل اسم صاحب الحساب..." else "Enter display name...")
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = TactileRoyalBlue)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TactileRoyalBlue,
                            unfocusedBorderColor = if (isDarkMode) Color(0xFF475569) else Color(0xFFCBD5E1),
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(insetBg)
                        .border(1.dp, if (isDarkMode) Color(0xFF334155) else Color(0x99BECAD8), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isArabic) "رتبة الحساب" else "Account Tier",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 11.sp,
                                color = subTextColor
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(TactileRoyalBlue.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = userAccount.localizedAccountTier(isArabic),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TactileRoyalBlue
                                )
                            }
                        }

                        Text(
                            text = if (isArabic) {
                                "الأحرف الأولى: ${userAccount.avatarInitials}"
                            } else {
                                "Initials: ${userAccount.avatarInitials}"
                            },
                            fontFamily = PlusJakartaSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = textColor
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpdateOwnerName(ownerNameInput.ifBlank { "Charfeddine" })
                    onDismiss()
                },
                modifier = Modifier.heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TactileRoyalBlue)
            ) {
                Text(
                    text = if (isArabic) "حفظ الاسم" else "Save Name",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(
                    text = if (isArabic) "إلغاء" else "Cancel",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = subTextColor
                )
            }
        }
    )
}

private data class AvatarChargingStateConfig(
    val primaryNeon: Color,
    val stateBadgeIcon: androidx.compose.ui.graphics.vector.ImageVector
)

/**
 * Clean Precision Profile Avatar with state-coded ring (zero blurry halo bloat).
 */
@Composable
fun ChargingStateNeonProfileAvatar(
    userAccount: UserAccount,
    telemetry: BatteryTelemetry,
    isDarkMode: Boolean,
    glowIntensity: Float = 0.75f,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val resolvedTheme = LocalAmpereTheme.current
    val livingState = telemetry.livingState
    val isCharging = telemetry.status == BatteryStatus.CHARGING || telemetry.status == BatteryStatus.FULL || telemetry.currentMa > 0
    val isOverheating = telemetry.temperatureC >= 38.5f || livingState == LivingBatteryState.OVERDRIVE_HEATING

    val config = when {
        resolvedTheme.isSimpleMode -> AvatarChargingStateConfig(
            primaryNeon = resolvedTheme.accent,
            stateBadgeIcon = Icons.Default.Bolt
        )
        isOverheating -> AvatarChargingStateConfig(
            primaryNeon = TactileCoralRed,
            stateBadgeIcon = Icons.Default.LocalFireDepartment
        )
        isCharging -> AvatarChargingStateConfig(
            primaryNeon = TactileGreen,
            stateBadgeIcon = Icons.Default.Bolt
        )
        livingState == LivingBatteryState.COOLING -> AvatarChargingStateConfig(
            primaryNeon = Color(0xFF00B0FF),
            stateBadgeIcon = Icons.Default.AcUnit
        )
        livingState == LivingBatteryState.DEEP_SLEEP -> AvatarChargingStateConfig(
            primaryNeon = Color(0xFF7C4DFF),
            stateBadgeIcon = Icons.Default.Bedtime
        )
        else -> AvatarChargingStateConfig(
            primaryNeon = TactileRoyalBlue,
            stateBadgeIcon = Icons.Default.Bolt
        )
    }

    val primaryNeon = config.primaryNeon
    val surfaceBg = if (resolvedTheme.themeId != null || resolvedTheme.isSimpleMode || resolvedTheme.isProMode) {
        resolvedTheme.surfaceInset
    } else if (isDarkMode) {
        Color(0xFF1E293B)
    } else {
        Color(0xFFE7EEF6)
    }
    val badgeBg = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFDDE5EF)

    Box(
        modifier = modifier
            .size(50.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(surfaceBg)
                .border(2.dp, primaryNeon.copy(alpha = if (glowIntensity > 0f) 0.85f else 0.65f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = userAccount.avatarInitials,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.5.sp,
                color = primaryNeon
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(17.dp)
                .clip(CircleShape)
                .background(badgeBg)
                .border(1.2.dp, primaryNeon, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = config.stateBadgeIcon,
                contentDescription = null,
                tint = primaryNeon,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}
