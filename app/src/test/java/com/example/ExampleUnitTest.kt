package com.example

import com.example.data.AppLanguage
import com.example.data.AppTier
import com.example.data.BatteryHealth
import com.example.data.BatteryStatus
import com.example.data.BatteryTelemetry
import com.example.data.CableDnaTier
import com.example.data.CellHealthStatus
import com.example.data.ChargePlug
import com.example.data.DeviceFormFactor
import com.example.data.DeviceHardwareProfile
import com.example.data.DevicePerformanceTier
import com.example.data.GlassThemeConfig
import com.example.data.GraphicsMode
import com.example.data.LivingBatteryState
import com.example.data.MetricType
import com.example.data.telemetry.AppBatteryUsageTracker
import com.example.data.telemetry.BatteryScienceClassifier
import com.example.data.telemetry.CableDnaStandardsEngine
import com.example.data.telemetry.CurrentCalibrationEngine
import com.example.data.telemetry.DeviceHardwareProfiler
import com.example.data.telemetry.RawAppUsageMetric
import com.example.data.update.InAppUpdateEngine
import com.example.ui.AmperePulseViewModel
import com.example.ui.theme.ThemeEngineCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testBatteryTelemetryModel() {
        val telemetry = BatteryTelemetry(
            percentage = 84,
            status = BatteryStatus.CHARGING,
            plugType = ChargePlug.AC,
            health = BatteryHealth.GOOD,
            voltageMv = 4182,
            currentMa = 2340,
            wattageW = 9.78f,
            livingState = LivingBatteryState.ENERGY_STORING
        )
        assertEquals(84, telemetry.percentage)
        assertEquals(BatteryStatus.CHARGING, telemetry.status)
        assertEquals(ChargePlug.AC, telemetry.plugType)
        assertEquals(LivingBatteryState.ENERGY_STORING, telemetry.livingState)
        assertTrue(telemetry.wattageW > 0f)
    }

    @Test
    fun testLivingBatteryStates() {
        assertEquals(4, LivingBatteryState.entries.size)
        assertEquals("Thermal Stress", LivingBatteryState.OVERDRIVE_HEATING.title)
        assertEquals("Thermal Recovery", LivingBatteryState.COOLING.title)
        assertEquals("Active Charging", LivingBatteryState.ENERGY_STORING.title)
        assertEquals("Quiescent Standby", LivingBatteryState.DEEP_SLEEP.title)
    }

    @Test
    fun testPureScientificBatteryClassificationAndOrdering() {
        val profile = DeviceHardwareProfile(
            brand = "Samsung",
            model = "Galaxy Test",
            safeThermalCeilingC = 42.0f,
            nominalDesignCapacityMah = 5000
        )

        // 1. 38.5°C during fast charging must stay ENERGY_STORING (not false overheat below 42°C JEITA ceiling)
        val warmFastChargeState = BatteryScienceClassifier.classifyState(
            temperatureC = 38.5f,
            voltageMv = 4150,
            currentMa = 2600,
            percentage = 48,
            isCharging = true,
            isScreenInteractive = true,
            tempDropRatePerTick = 0.0f,
            deviceThermalCeilingC = profile.safeThermalCeilingC,
            nominalCapacityMah = profile.nominalDesignCapacityMah
        )
        assertEquals(LivingBatteryState.ENERGY_STORING, warmFastChargeState)

        // 2. >= 42.0°C triggers OVERDRIVE_HEATING (IEEE 1625 / JEITA stress zone)
        val stressState = BatteryScienceClassifier.classifyState(
            temperatureC = 42.5f,
            voltageMv = 4200,
            currentMa = 2400,
            percentage = 55,
            isCharging = true,
            isScreenInteractive = true,
            tempDropRatePerTick = -0.05f,
            deviceThermalCeilingC = profile.safeThermalCeilingC,
            nominalCapacityMah = profile.nominalDesignCapacityMah
        )
        assertEquals(LivingBatteryState.OVERDRIVE_HEATING, stressState)

        // 3. < 15.0°C triggers COOLING cold-charge protection (Li-plating prevention)
        val coldState = BatteryScienceClassifier.classifyState(
            temperatureC = 12.0f,
            voltageMv = 3900,
            currentMa = 900,
            percentage = 40,
            isCharging = true,
            isScreenInteractive = true,
            tempDropRatePerTick = 0.0f,
            deviceThermalCeilingC = profile.safeThermalCeilingC,
            nominalCapacityMah = profile.nominalDesignCapacityMah
        )
        assertEquals(LivingBatteryState.COOLING, coldState)

        // 4. Screen off + low quiescent draw (<= 0.08C = 400mA for 5000mAh) triggers DEEP_SLEEP
        val idleState = BatteryScienceClassifier.classifyState(
            temperatureC = 26.0f,
            voltageMv = 3890,
            currentMa = -180,
            percentage = 72,
            isCharging = false,
            isScreenInteractive = false,
            tempDropRatePerTick = 0.0f,
            deviceThermalCeilingC = profile.safeThermalCeilingC,
            nominalCapacityMah = profile.nominalDesignCapacityMah
        )
        assertEquals(LivingBatteryState.DEEP_SLEEP, idleState)

        // 5. Ordered 4-metric matrix places Temperature #1 in OVERDRIVE_HEATING and Current #1 in ENERGY_STORING
        val stressProfile = BatteryScienceClassifier.buildScienceProfile(
            telemetry = BatteryTelemetry(
                percentage = 55,
                voltageMv = 4200,
                currentMa = 2400,
                temperatureC = 43.0f,
                livingState = LivingBatteryState.OVERDRIVE_HEATING
            ),
            deviceProfile = profile
        )
        assertEquals(4, stressProfile.orderedMetrics.size)
        assertEquals(MetricType.TEMPERATURE, stressProfile.orderedMetrics.first().type)

        val chargingProfile = BatteryScienceClassifier.buildScienceProfile(
            telemetry = BatteryTelemetry(
                percentage = 45,
                voltageMv = 4150,
                currentMa = 3000,
                temperatureC = 32.0f,
                status = BatteryStatus.CHARGING,
                plugType = ChargePlug.AC,
                livingState = LivingBatteryState.ENERGY_STORING
            ),
            deviceProfile = profile
        )
        assertEquals(4, chargingProfile.orderedMetrics.size)
        assertEquals(MetricType.CURRENT, chargingProfile.orderedMetrics.first().type)
        assertEquals(0.60f, chargingProfile.cRate, 0.001f) // 3000mA / 5000mAh = 0.60C
    }

    @Test
    fun testGlassThemeConfigDefaults() {
        val config = GlassThemeConfig()
        assertFalse(config.isDarkMode) // Defaults to Soft Stone Light Neumorphism
        assertEquals("Royal Blue (#2962FF)", config.accentName)
        assertTrue(config.glowIntensity in 0.3f..1.0f)
        assertTrue(config.target120FpsMode)
        assertTrue(config.target90FpsMode)
        assertTrue(config.showLiquidDroplet)
        assertEquals(1.0f, config.currentMultiplier, 0.001f)
        assertEquals(0.0f, config.currentZeroOffsetMa, 0.001f)
        assertEquals(42.0f, config.highTempThresholdC, 0.001f)

        val darkConfig = config.copy(isDarkMode = true)
        assertTrue(darkConfig.isDarkMode)
    }

    @Test
    fun testCurrentCalibrationEngineMath() {
        val engine = CurrentCalibrationEngine(
            context = null,
            initialMultiplier = 1.0f,
            initialOffsetMa = 0.0f
        )

        // 1. Pure hardware policy: returns 0 (never fabricates fake/sinusoidal current) when BatteryManager is absent
        assertEquals(0, engine.sampleCurrentMa(null, isCharging = false))
        assertEquals(0, engine.sampleCurrentMa(null, isCharging = true))

        // 2. Calibration update (Multiplier & Zero Offset)
        engine.updateCalibration(newMultiplier = 1.10f, newOffsetMa = 20.0f)
        assertEquals(1.10f, engine.multiplier, 0.01f)
        assertEquals(20.0f, engine.zeroOffsetMa, 0.01f)

        // 3. Reset calibration
        engine.resetCalibration()
        assertEquals(1.0f, engine.multiplier, 0.001f)
        assertEquals(0.0f, engine.zeroOffsetMa, 0.001f)

        // 4. Manual Zero Calibration
        engine.performManualZeroCalibration(currentReadingMa = -140f, expectedNominalMa = -120f)
        assertEquals(-20.0f, engine.zeroOffsetMa, 0.01f)

        val status = engine.getTelemetryEngineStatus()
        assertEquals(1.0f, status.multiplier, 0.001f)
        assertEquals(-20.0f, status.zeroOffsetMa, 0.01f)
    }

    @Test
    fun testAutoUpdateVersionComparison() {
        val config = GlassThemeConfig()
        assertTrue(config.autoUpdateEnabled)

        assertTrue(InAppUpdateEngine.compareSemanticVersions("1.0.1", "1.0.0") > 0)
        assertTrue(InAppUpdateEngine.compareSemanticVersions("1.1.0", "1.0.99") > 0)
        assertEquals(0, InAppUpdateEngine.compareSemanticVersions("1.0.5", "1.0.5"))
        assertTrue(InAppUpdateEngine.compareSemanticVersions("1.0.4", "1.0.5") < 0)
    }

    @Test
    fun testPremiumRedeemCodeGatingDefaults() {
        val config = GlassThemeConfig()
        assertFalse(config.isPremiumUnlocked)
        assertEquals(AppTier.FREE, config.appTier)
        assertEquals("charfeddine23a", AmperePulseViewModel.PREMIUM_REDEEM_CODE)
    }

    @Test
    fun testThemeEngineCatalog60Themes() {
        val presets = ThemeEngineCatalog.presets
        assertEquals(60, presets.size)
        assertEquals(60, presets.map { it.id }.distinct().size)
        assertEquals(60, presets.map { it.name }.distinct().size)

        val expectedNames = listOf(
            "Monokai", "Monokai Pro", "Solarized Dark", "Solarized Light",
            "One Dark Pro", "One Light", "Dracula", "Tomorrow Night Eighties",
            "Nord", "Night Owl", "Bluloco Dark", "Ayu Dark", "Ayu Mirage", "Ayu Light",
            "Winter is Coming", "Zenburn", "SynthWave '84", "Shades of Purple",
            "LaserWave", "Abyss", "Cobalt2", "Red", "Poimandres", "Hopscotch",
            "Gruvbox Dark", "Gruvbox Light", "Oceanic Next", "Base16 Eighties",
            "Tomorrow Night Blue", "Material Theme (Oceanic)", "Material Theme (Palenight)",
            "Material Theme (Darker)", "Catppuccin (Mocha)", "Kanagawa", "Rose Pine",
            "GitHub Dark", "GitHub Light", "Panda Theme", "Atom One Dark", "Tokyo Night",
            "Obsidian Liquid Glass", "Frosted Arctic Glass", "Cyber Neon Liquid",
            "Smoked Quartz Glass", "Midnight Velvet Glass", "Aurora Borealis Glass",
            "Crystal Prism Liquid", "Deep Ocean Glass", "Amber Mercury Glass",
            "Titanium Mist Glass", "Holographic Liquid Glass", "Carbon Blur Glass",
            "Emerald Translucent Glass", "Ultraviolet Frost Glass", "Sapphire Liquid Glass",
            "Pearlescent Dark Glass", "Ruby Refractive Glass", "Chromatic Aberration Glass",
            "Liquid Mercury Dark", "Bioluminescent Glass"
        )
        expectedNames.forEach { expectedName ->
            assertTrue("Missing theme: $expectedName", presets.any { it.name == expectedName })
        }
        assertEquals(null, GlassThemeConfig().activeThemeId)
    }

    @Test
    fun testAppBatteryUsageStrictDescendingSort() {
        val raw = listOf(
            RawAppUsageMetric(
                packageName = "com.example.notes",
                appName = "Notes",
                foregroundTimeMs = 15L * 60_000L,
                wakeEventsCount = 2,
                powerMultiplier = 0.9f
            ),
            RawAppUsageMetric(
                packageName = "com.example.heavygame",
                appName = "Heavy Game",
                foregroundTimeMs = 120L * 60_000L,
                wakeEventsCount = 15,
                powerMultiplier = 1.65f
            ),
            RawAppUsageMetric(
                packageName = "com.example.video",
                appName = "Video Stream",
                foregroundTimeMs = 60L * 60_000L,
                wakeEventsCount = 5,
                powerMultiplier = 1.40f
            )
        )

        val sorted = AppBatteryUsageTracker.calculateAndSortConsumption(
            metrics = raw,
            batteryCapacityMah = 5000
        )
        assertEquals(3, sorted.size)
        assertEquals(1, sorted[0].rank)
        assertEquals("Heavy Game", sorted[0].appName)
        assertEquals(2, sorted[1].rank)
        assertEquals("Video Stream", sorted[1].appName)
        assertEquals(3, sorted[2].rank)
        assertEquals("Notes", sorted[2].appName)
        assertTrue(sorted[0].consumptionPercent > sorted[1].consumptionPercent)
        assertTrue(sorted[1].consumptionPercent > sorted[2].consumptionPercent)
        assertTrue(sorted.all { it.estimatedMah > 0 })
    }

    @Test
    fun testInternationalCableDnaStandardsEngine() {
        val profile = DeviceHardwareProfile(
            brand = "Google",
            model = "Pixel Test",
            chargingEcosystemEn = "USB-IF PD 3.0 + PPS",
            recommendedPowerWatts = 27,
            safeThermalCeilingC = 41.0f
        )

        // Genuine 20AWG E-Marked 5A fast cable (English telemetry)
        val oemCable = CableDnaStandardsEngine.evaluate(
            telemetry = BatteryTelemetry(
                percentage = 50,
                status = BatteryStatus.CHARGING,
                plugType = ChargePlug.AC,
                voltageMv = 4130,
                currentMa = 3500,
                temperatureC = 33.0f
            ),
            deviceProfile = profile,
            observedBaselineOcvMv = 3830,
            dynamicDeltaImpedanceMilliOhms = 90,
            currentRipplePercent = 2.0f,
            isArabic = false
        )
        assertEquals(CableDnaTier.LEGENDARY_ORIGINAL, oemCable.tier)
        assertTrue(oemCable.estimatedAwg.contains("20 AWG"))
        assertTrue(oemCable.standardCompliance.contains("PASS"))

        // Same OEM cable evaluated in Arabic mode emits pure Arabic standard compliance
        val oemCableAr = CableDnaStandardsEngine.evaluate(
            telemetry = BatteryTelemetry(
                percentage = 50,
                status = BatteryStatus.CHARGING,
                plugType = ChargePlug.AC,
                voltageMv = 4130,
                currentMa = 3500,
                temperatureC = 33.0f
            ),
            deviceProfile = profile,
            observedBaselineOcvMv = 3830,
            dynamicDeltaImpedanceMilliOhms = 90,
            currentRipplePercent = 2.0f,
            isArabic = true
        )
        assertEquals(CableDnaTier.LEGENDARY_ORIGINAL, oemCableAr.tier)
        assertTrue(oemCableAr.standardCompliance.contains("اجتياز"))
        assertFalse(oemCableAr.standardCompliance.contains("PASS"))

        // 2S Dual-Cell (8.15V pack) high-speed charge does not miscalculate against 5V rail
        val dualCellCable = CableDnaStandardsEngine.evaluate(
            telemetry = BatteryTelemetry(
                percentage = 50,
                status = BatteryStatus.CHARGING,
                plugType = ChargePlug.AC,
                voltageMv = 8150,
                currentMa = 4800,
                temperatureC = 35.0f
            ),
            deviceProfile = profile.copy(isDualCellSuspected = true),
            observedBaselineOcvMv = 7660,
            dynamicDeltaImpedanceMilliOhms = 98,
            currentRipplePercent = 2.2f,
            isArabic = false
        )
        assertEquals(CableDnaTier.LEGENDARY_ORIGINAL, dualCellCable.tier)
        assertTrue(dualCellCable.cellTopology.contains("2S"))
    }

    @Test
    fun testGraphicsModeAndDropletDragDefaults() {
        val defaultConfig = GlassThemeConfig()
        assertEquals(GraphicsMode.STANDARD, defaultConfig.graphicsMode)
        assertFalse(defaultConfig.isSimpleMode)
        assertFalse(defaultConfig.isProMode)
        assertFalse(defaultConfig.dropletDragNavigationEnabled)

        val simpleConfig = defaultConfig.copy(graphicsMode = GraphicsMode.SIMPLE)
        assertTrue(simpleConfig.isSimpleMode)
        assertFalse(simpleConfig.isProMode)

        val proConfig = defaultConfig.copy(graphicsMode = GraphicsMode.PRO, samplingIntervalMs = 10L)
        assertTrue(proConfig.isProMode)
        assertFalse(proConfig.isSimpleMode)
        assertEquals(10L, proConfig.samplingIntervalMs)

        assertEquals(GraphicsMode.SIMPLE, GraphicsMode.fromKey("simple"))
        assertEquals(GraphicsMode.PRO, GraphicsMode.fromKey("pro"))
        assertEquals(GraphicsMode.STANDARD, GraphicsMode.fromKey("invalid"))
    }

    @Test
    fun testFluidCurveMathSuppressesMicroOscillations() {
        // Tiny +-1 mV chatter over 24 samples must NOT stretch full height (80mV minVisualSpan keeps it calm near center)
        val tinyJitter = (0 until 24).map { idx ->
            com.example.data.TelemetryPoint(
                timestamp = idx * 1000L,
                value = if (idx % 2 == 0) 4180f else 4181f
            )
        }
        val calmKnots = com.example.ui.components.FluidCurveMath.computeNormalizedKnots(
            points = tinyJitter,
            minVisualSpan = 80f,
            knotCount = 12
        )
        assertEquals(12, calmKnots.size)
        val calmSpread = calmKnots.max() - calmKnots.min()
        assertTrue("Tiny 1mV jitter should compress to < 0.03 visual amplitude, got $calmSpread", calmSpread < 0.03f)

        // Genuine rising charging trend over 24 samples should progress smoothly upward
        val risingTrend = (0 until 24).map { idx ->
            com.example.data.TelemetryPoint(
                timestamp = idx * 1000L,
                value = 3800f + idx * 15f
            )
        }
        val trendKnots = com.example.ui.components.FluidCurveMath.computeNormalizedKnots(
            points = risingTrend,
            minVisualSpan = 80f,
            knotCount = 12
        )
        assertEquals(12, trendKnots.size)
        assertTrue(trendKnots.last() > trendKnots.first() + 0.45f)
        for (i in 1 until trendKnots.size) {
            assertTrue(trendKnots[i] >= trendKnots[i - 1])
        }
    }

    @Test
    fun testBilingualLocalizationSupport() {
        assertEquals("ar", AppLanguage.ARABIC.code)
        assertTrue(AppLanguage.ARABIC.isRtl)
        assertTrue(AppLanguage.ARABIC.isArabic)
        assertEquals("en", AppLanguage.ENGLISH.code)
        assertFalse(AppLanguage.ENGLISH.isRtl)
        assertFalse(AppLanguage.ENGLISH.isArabic)
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("en"))
        assertEquals(AppLanguage.ARABIC, AppLanguage.fromCode("ar"))
        assertEquals(AppLanguage.ARABIC, GlassThemeConfig().appLanguage)

        val telemetry = BatteryTelemetry(
            percentage = 52,
            voltageMv = 4150,
            currentMa = 2500,
            temperatureC = 33.5f,
            status = BatteryStatus.CHARGING,
            plugType = ChargePlug.AC,
            livingState = LivingBatteryState.ENERGY_STORING
        )
        val profile = DeviceHardwareProfile(
            brand = "Samsung",
            model = "Galaxy Ultra",
            safeThermalCeilingC = 42.0f,
            nominalDesignCapacityMah = 5000
        )

        val scienceAr = BatteryScienceClassifier.buildScienceProfile(telemetry, profile, isArabic = true)
        val scienceEn = BatteryScienceClassifier.buildScienceProfile(telemetry, profile, isArabic = false)
        assertTrue(scienceAr.reasonAr != scienceEn.reasonAr)
        assertTrue(scienceAr.phaseLabelAr != scienceEn.phaseLabelAr)
        assertTrue(scienceAr.scienceReference != scienceEn.scienceReference)
        assertTrue(scienceAr.thermalAssessment.zoneTitleAr != scienceEn.thermalAssessment.zoneTitleAr)

        assertTrue(LivingBatteryState.ENERGY_STORING.localizedTitle(true) != LivingBatteryState.ENERGY_STORING.localizedTitle(false))
        assertTrue(AppTier.FREE.localizedLabel(true) != AppTier.FREE.localizedLabel(false))
        assertTrue(GraphicsMode.PRO.localizedTitle(true) != GraphicsMode.PRO.localizedTitle(false))
        assertTrue(CableDnaTier.LEGENDARY_ORIGINAL.localizedTitle(true) != CableDnaTier.LEGENDARY_ORIGINAL.localizedTitle(false))
        assertTrue(CellHealthStatus.PRISTINE.localizedLabel(true) != CellHealthStatus.PRISTINE.localizedLabel(false))
        assertTrue(DeviceFormFactor.PHONE.localizedLabel(true) != DeviceFormFactor.PHONE.localizedLabel(false))
    }

    @Test
    fun testVoltageNormalizationUnits() {
        assertEquals(4182, com.example.data.BatteryMonitorRepository.normalizeVoltageMv(4182L))
        assertEquals(4185, com.example.data.BatteryMonitorRepository.normalizeVoltageMv(4_185_000L))
        assertEquals(8420, com.example.data.BatteryMonitorRepository.normalizeVoltageMv(8_420_000L))
        assertEquals(4000, com.example.data.BatteryMonitorRepository.normalizeVoltageMv(4L))
        assertEquals(4200, com.example.data.BatteryMonitorRepository.normalizeVoltageMv(42L))
        assertEquals(0, com.example.data.BatteryMonitorRepository.normalizeVoltageMv(-1L))
        assertEquals(0, com.example.data.BatteryMonitorRepository.normalizeVoltageMv(0L))
    }

    @Test
    fun testInAppUpdateUrlAndFilenameSecurity() {
        assertTrue(InAppUpdateEngine.isTrustedUpdateUrl("https://api.github.com/repos/charfeddine23/Ampere/releases/latest"))
        assertTrue(InAppUpdateEngine.isTrustedUpdateUrl("https://github.com/charfeddine23/Ampere/releases/download/v1.0.1/VoltCore.apk"))
        assertTrue(InAppUpdateEngine.isTrustedUpdateUrl("https://objects.githubusercontent.com/github-production-release-asset-2e65be/app.apk"))

        assertFalse(InAppUpdateEngine.isTrustedUpdateUrl("http://github.com/charfeddine23/Ampere/releases/latest"))
        assertFalse(InAppUpdateEngine.isTrustedUpdateUrl("https://evil.example.com/VoltCore.apk"))
        assertFalse(InAppUpdateEngine.isTrustedUpdateUrl("https://github.com@evil.example.com/VoltCore.apk"))
        assertFalse(InAppUpdateEngine.isTrustedUpdateUrl("file:///sdcard/Download/evil.apk"))
        assertFalse(InAppUpdateEngine.isTrustedUpdateUrl(""))
        assertFalse(InAppUpdateEngine.isTrustedUpdateUrl(null))

        val traversed = InAppUpdateEngine.sanitizeApkFileName("../../etc/evil payload?.apk")
        assertFalse(traversed.contains(".."))
        assertFalse(traversed.contains("/"))
        assertFalse(traversed.contains("\\"))
        assertTrue(traversed.endsWith(".apk"))

        val fallback = InAppUpdateEngine.sanitizeApkFileName("...")
        assertEquals("Volt.Core.update.apk", fallback)
    }

    @Test
    fun testGraphicsModeCycleAndCooldowns() {
        val next = { mode: GraphicsMode ->
            when (mode) {
                GraphicsMode.STANDARD -> GraphicsMode.SIMPLE
                GraphicsMode.SIMPLE -> GraphicsMode.PRO
                GraphicsMode.PRO -> GraphicsMode.STANDARD
            }
        }
        assertEquals(GraphicsMode.SIMPLE, next(GraphicsMode.STANDARD))
        assertEquals(GraphicsMode.PRO, next(GraphicsMode.SIMPLE))
        assertEquals(GraphicsMode.STANDARD, next(GraphicsMode.PRO))
        assertEquals(6L * 60L * 60L * 1000L, AmperePulseViewModel.AUTO_UPDATE_COOLDOWN_MS)
        assertEquals(45_000L, AmperePulseViewModel.APP_USAGE_REFRESH_COOLDOWN_MS)
    }

    @Test
    fun testDeviceHardwarePerformanceTierDetection() {
        val flagshipEval = DeviceHardwareProfiler.evaluatePerformanceTier(
            isLowRamDevice = false,
            totalRamGb = 8.0f,
            cpuCores = 8,
            sdkInt = 34,
            primaryAbi = "arm64-v8a",
            chipset = "Snapdragon 8 Gen 3",
            model = "Galaxy S24"
        )
        assertEquals(DevicePerformanceTier.FLAGSHIP, flagshipEval.tier)
        assertFalse(DeviceHardwareProfile(performanceTier = flagshipEval.tier).isLowEndDevice)

        val midEval = DeviceHardwareProfiler.evaluatePerformanceTier(
            isLowRamDevice = false,
            totalRamGb = 4.0f,
            cpuCores = 8,
            sdkInt = 33,
            primaryAbi = "arm64-v8a",
            chipset = "Snapdragon 680",
            model = "Redmi Note"
        )
        assertEquals(DevicePerformanceTier.MID_RANGE, midEval.tier)
        assertFalse(DeviceHardwareProfile(performanceTier = midEval.tier).isLowEndDevice)

        val lowRamEval = DeviceHardwareProfiler.evaluatePerformanceTier(
            isLowRamDevice = true,
            totalRamGb = 2.0f,
            cpuCores = 4,
            sdkInt = 31,
            primaryAbi = "armeabi-v7a",
            chipset = "MT6739",
            model = "Galaxy A01 Core"
        )
        assertEquals(DevicePerformanceTier.LOW_END, lowRamEval.tier)
        assertTrue(lowRamEval.reasonAr.isNotBlank())
        assertTrue(lowRamEval.reasonEn.isNotBlank())
        assertTrue(DeviceHardwareProfile(performanceTier = lowRamEval.tier).isLowEndDevice)

        val entryChipEval = DeviceHardwareProfiler.evaluatePerformanceTier(
            isLowRamDevice = false,
            totalRamGb = 4.0f,
            cpuCores = 8,
            sdkInt = 31,
            primaryAbi = "arm64-v8a",
            chipset = "UNISOC SC9863A",
            model = "Entry Phone"
        )
        assertEquals(DevicePerformanceTier.LOW_END, entryChipEval.tier)
    }
}
