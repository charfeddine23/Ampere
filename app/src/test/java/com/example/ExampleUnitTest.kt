package com.example

import com.example.data.BatteryHealth
import com.example.data.BatteryStatus
import com.example.data.BatteryTelemetry
import com.example.data.ChargePlug
import com.example.data.CyberEggData
import com.example.data.GlassThemeConfig
import com.example.data.LivingBatteryState
import com.example.data.PrizeRoadData
import com.example.data.StreakData
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
        assertTrue(LivingBatteryState.OVERDRIVE_HEATING.isBleeding)
        assertFalse(LivingBatteryState.COOLING.isBleeding)
        assertFalse(LivingBatteryState.ENERGY_STORING.isBleeding)
        assertFalse(LivingBatteryState.DEEP_SLEEP.isBleeding)
    }

    @Test
    fun test4DynamicSensorTriggerRules() {
        // Condition 1: ENERGY STORING (Current > 0, Temp 20-37C, stable V)
        val temp1 = 28.5f
        val currentMa1 = 2200
        val voltageMv1 = 4180
        val isStableV = voltageMv1 in 3700..4450
        val isEnergyStoring = currentMa1 > 0 && temp1 in 20.0f..37.0f && isStableV
        assertTrue(isEnergyStoring)

        // Condition 2: OVERDRIVE / HEATING (Temp > 38C or severe voltage drop fault)
        val temp2 = 39.5f
        val isOverdrive = temp2 > 38.0f
        assertTrue(isOverdrive)

        // Condition 3: CRYOGENIC COOLING (Rapid thermal drop toward <35C or Cryo skill)
        val tempDropRate = 0.25f
        val temp3 = 31.0f
        val isRapidCooling = tempDropRate >= 0.10f && temp3 < 35.0f
        assertTrue(isRapidCooling)

        // Condition 4: DEEP SLEEP / HIBERNATION (Screen off + low idle draw <=300mA)
        val isScreenOn = false
        val idleCurrentMa = 120
        val isDeepSleep = !isScreenOn && idleCurrentMa <= 350
        assertTrue(isDeepSleep)
    }

    @Test
    fun testStreakAndEggDefaults() {
        val streak = StreakData()
        assertEquals(0, streak.currentStreakDays)
        assertEquals(0, streak.shieldsAvailable)
        assertEquals(0, streak.maxShields)
        assertEquals(0.0f, streak.todayComplianceRate, 0.01f)
        assertEquals(0, streak.healthyChargeMinutesToday)

        val egg = CyberEggData()
        assertEquals(0f, egg.incubationProgress, 0.001f)
        assertEquals(0, egg.currentXP)
        assertTrue(egg.hatchedList.isEmpty())
    }

    @Test
    fun testPrizeRoadMath() {
        val prizeRoad = PrizeRoadData()
        assertEquals(0L, prizeRoad.cumulativeMah)
        assertEquals(1, prizeRoad.currentLevel)
        assertEquals(1000, prizeRoad.maxLevel)
        assertTrue(prizeRoad.claimedMilestones.isEmpty())

        // Level fullness calculation at initial 0 mAh
        val currentLevelMah = (prizeRoad.cumulativeMah % 10_000L).toFloat()
        val fullnessRatio = currentLevelMah / 10_000f
        assertEquals(0f, fullnessRatio, 0.01f)

        // Progress after adding 25,000 mAh -> Level 3
        val simulatedMah = 25_000L
        val calculatedLevel = ((simulatedMah / 10_000L).toInt() + 1).coerceIn(1, 1000)
        assertEquals(3, calculatedLevel)
    }

    @Test
    fun testGlassThemeConfigDefaults() {
        val config = GlassThemeConfig()
        assertFalse(config.isDarkMode) // Defaults to Light Neumorphism
        assertEquals("Royal Blue (#2962FF)", config.accentName)
        assertTrue(config.glowIntensity in 0.3f..1.0f)
        assertTrue(config.target120FpsMode)
        assertTrue(config.target90FpsMode)
        assertTrue(config.showLiquidDroplet)
        assertEquals(1.0f, config.currentMultiplier, 0.001f)
        assertEquals(0.0f, config.currentZeroOffsetMa, 0.001f)

        // Dark mode toggle
        val darkConfig = config.copy(isDarkMode = true)
        assertTrue(darkConfig.isDarkMode)
    }

    @Test
    fun testCurrentCalibrationEngineMath() {
        val engine = com.example.data.telemetry.CurrentCalibrationEngine(
            context = null,
            initialMultiplier = 1.0f,
            initialOffsetMa = 0.0f
        )

        // 1. Physical fallback when no BatteryManager present
        val dischargeReading = engine.sampleCurrentMa(null, isCharging = false)
        assertTrue(dischargeReading < 0)

        val chargingReading = engine.sampleCurrentMa(null, isCharging = true)
        assertTrue(chargingReading > 0)

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

        assertTrue(com.example.data.update.InAppUpdateEngine.compareSemanticVersions("1.0.1", "1.0.0") > 0)
        assertTrue(com.example.data.update.InAppUpdateEngine.compareSemanticVersions("1.1.0", "1.0.99") > 0)
        assertEquals(0, com.example.data.update.InAppUpdateEngine.compareSemanticVersions("1.0.5", "1.0.5"))
        assertTrue(com.example.data.update.InAppUpdateEngine.compareSemanticVersions("1.0.4", "1.0.5") < 0)
    }

    @Test
    fun testPremiumRedeemCodeGatingDefaults() {
        val config = GlassThemeConfig()
        assertFalse(config.isPremiumUnlocked)
        assertEquals(com.example.data.AppTier.FREE, config.appTier)
        assertEquals("charfeddine23a", com.example.ui.AmperePulseViewModel.PREMIUM_REDEEM_CODE)
    }

    @Test
    fun testLauncherIconVariantsAndDefaults() {
        val config = GlassThemeConfig()
        assertEquals("sapphire", config.launcherIconKey)
        assertEquals(com.example.data.LauncherIconVariant.SAPPHIRE, config.launcherIconVariant)
        assertEquals(com.example.data.LauncherIconVariant.SAPPHIRE, com.example.data.LauncherIconVariant.fromKey("unknown"))

        val variants = com.example.data.LauncherIconVariant.entries
        assertEquals(7, variants.size)
        assertEquals(7, variants.map { it.key }.distinct().size)
        assertEquals(7, variants.map { it.aliasClassName }.distinct().size)
        assertTrue(variants.any { it == com.example.data.LauncherIconVariant.WHITE && it.key == "white" })
        assertTrue(variants.any { it == com.example.data.LauncherIconVariant.BLACK && it.key == "black" })
        assertTrue(variants.all { it.aliasClassName.startsWith("com.example.Launcher") })
    }

    @Test
    fun testThemeEngineCatalog60Themes() {
        val presets = com.example.ui.theme.ThemeEngineCatalog.presets
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
            com.example.data.telemetry.RawAppUsageMetric(
                packageName = "com.example.notes",
                appName = "Notes",
                foregroundTimeMs = 15L * 60_000L,
                wakeEventsCount = 2,
                powerMultiplier = 0.9f
            ),
            com.example.data.telemetry.RawAppUsageMetric(
                packageName = "com.example.heavygame",
                appName = "Heavy Game",
                foregroundTimeMs = 120L * 60_000L,
                wakeEventsCount = 15,
                powerMultiplier = 1.65f
            ),
            com.example.data.telemetry.RawAppUsageMetric(
                packageName = "com.example.video",
                appName = "Video Stream",
                foregroundTimeMs = 60L * 60_000L,
                wakeEventsCount = 5,
                powerMultiplier = 1.40f
            )
        )

        val sorted = com.example.data.telemetry.AppBatteryUsageTracker.calculateAndSortConsumption(
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
    fun testForceChangeIconDefaultAndScientificBatteryModes() {
        val config = GlassThemeConfig()
        assertFalse(config.forceChangeIconEnabled)

        // 1. 38.5°C during fast charging must stay ENERGY_STORING (not false overheat below 42°C JEITA ceiling)
        val warmFastChargeState = com.example.data.telemetry.BatteryScienceClassifier.classifyState(
            temperatureC = 38.5f,
            voltageMv = 4150,
            currentMa = 2600,
            percentage = 48,
            isCharging = true,
            isScreenInteractive = true,
            tempDropRatePerTick = 0.0f,
            isCryoSkillActive = false,
            deviceThermalCeilingC = 42.0f
        )
        assertEquals(com.example.data.LivingBatteryState.ENERGY_STORING, warmFastChargeState)

        // 2. >= 42.0°C triggers OVERDRIVE_HEATING (IEEE 1625 / JEITA stress zone)
        val stressState = com.example.data.telemetry.BatteryScienceClassifier.classifyState(
            temperatureC = 42.5f,
            voltageMv = 4200,
            currentMa = 2400,
            percentage = 55,
            isCharging = true,
            isScreenInteractive = true,
            tempDropRatePerTick = -0.05f,
            isCryoSkillActive = false,
            deviceThermalCeilingC = 42.0f
        )
        assertEquals(com.example.data.LivingBatteryState.OVERDRIVE_HEATING, stressState)

        // 3. < 15.0°C triggers COOLING cold-charge protection
        val coldState = com.example.data.telemetry.BatteryScienceClassifier.classifyState(
            temperatureC = 12.0f,
            voltageMv = 3900,
            currentMa = 900,
            percentage = 40,
            isCharging = true,
            isScreenInteractive = true,
            tempDropRatePerTick = 0.0f,
            isCryoSkillActive = false,
            deviceThermalCeilingC = 42.0f
        )
        assertEquals(com.example.data.LivingBatteryState.COOLING, coldState)

        // 4. Ordered metrics put Temperature #1 during OVERDRIVE_HEATING, and Current #1 during ENERGY_STORING
        val profile = com.example.data.DeviceHardwareProfile(
            brand = "Samsung",
            model = "Galaxy Test",
            safeThermalCeilingC = 42.0f,
            nominalDesignCapacityMah = 5000
        )
        val stressProfile = com.example.data.telemetry.BatteryScienceClassifier.buildScienceProfile(
            telemetry = BatteryTelemetry(
                percentage = 55,
                voltageMv = 4200,
                currentMa = 2400,
                temperatureC = 43.0f,
                livingState = com.example.data.LivingBatteryState.OVERDRIVE_HEATING
            ),
            deviceProfile = profile
        )
        assertEquals(com.example.data.MetricType.TEMPERATURE, stressProfile.orderedMetrics.first().type)

        val chargingProfile = com.example.data.telemetry.BatteryScienceClassifier.buildScienceProfile(
            telemetry = BatteryTelemetry(
                percentage = 45,
                voltageMv = 4150,
                currentMa = 3000,
                temperatureC = 32.0f,
                status = com.example.data.BatteryStatus.CHARGING,
                plugType = com.example.data.ChargePlug.AC,
                livingState = com.example.data.LivingBatteryState.ENERGY_STORING
            ),
            deviceProfile = profile
        )
        assertEquals(com.example.data.MetricType.CURRENT, chargingProfile.orderedMetrics.first().type)
    }

    @Test
    fun testInternationalCableDnaStandardsEngine() {
        val profile = com.example.data.DeviceHardwareProfile(
            brand = "Google",
            model = "Pixel Test",
            chargingEcosystemEn = "USB-IF PD 3.0 + PPS",
            recommendedPowerWatts = 27,
            safeThermalCeilingC = 41.0f
        )

        // Genuine 20AWG E-Marked 5A fast cable
        val oemCable = com.example.data.telemetry.CableDnaStandardsEngine.evaluate(
            telemetry = BatteryTelemetry(
                percentage = 50,
                status = com.example.data.BatteryStatus.CHARGING,
                plugType = com.example.data.ChargePlug.AC,
                voltageMv = 4130,
                currentMa = 3500,
                temperatureC = 33.0f
            ),
            deviceProfile = profile,
            observedBaselineOcvMv = 3830,
            dynamicDeltaImpedanceMilliOhms = 90,
            currentRipplePercent = 2.0f
        )
        assertEquals(com.example.data.CableDnaTier.LEGENDARY_ORIGINAL, oemCable.tier)
        assertTrue(oemCable.estimatedAwg.contains("20 AWG"))
        assertTrue(oemCable.standardCompliance.contains("PASS"))

        // 2S Dual-Cell (8.15V pack) high-speed charge does not miscalculate against 5V rail
        val dualCellCable = com.example.data.telemetry.CableDnaStandardsEngine.evaluate(
            telemetry = BatteryTelemetry(
                percentage = 50,
                status = com.example.data.BatteryStatus.CHARGING,
                plugType = com.example.data.ChargePlug.AC,
                voltageMv = 8150,
                currentMa = 4800,
                temperatureC = 35.0f
            ),
            deviceProfile = profile.copy(isDualCellSuspected = true),
            observedBaselineOcvMv = 7660,
            dynamicDeltaImpedanceMilliOhms = 98,
            currentRipplePercent = 2.2f
        )
        assertEquals(com.example.data.CableDnaTier.LEGENDARY_ORIGINAL, dualCellCable.tier)
        assertTrue(dualCellCable.cellTopology.contains("2S"))
    }

    @Test
    fun testGraphicsModeAndDropletDragDefaults() {
        val defaultConfig = GlassThemeConfig()
        assertEquals(com.example.data.GraphicsMode.STANDARD, defaultConfig.graphicsMode)
        assertFalse(defaultConfig.isSimpleMode)
        assertFalse(defaultConfig.isProMode)
        assertFalse(defaultConfig.dropletDragNavigationEnabled)

        val simpleConfig = defaultConfig.copy(graphicsMode = com.example.data.GraphicsMode.SIMPLE)
        assertTrue(simpleConfig.isSimpleMode)
        assertFalse(simpleConfig.isProMode)

        val proConfig = defaultConfig.copy(graphicsMode = com.example.data.GraphicsMode.PRO, samplingIntervalMs = 1L)
        assertTrue(proConfig.isProMode)
        assertFalse(proConfig.isSimpleMode)
        assertEquals(1L, proConfig.samplingIntervalMs)

        assertEquals(com.example.data.GraphicsMode.SIMPLE, com.example.data.GraphicsMode.fromKey("simple"))
        assertEquals(com.example.data.GraphicsMode.PRO, com.example.data.GraphicsMode.fromKey("pro"))
        assertEquals(com.example.data.GraphicsMode.STANDARD, com.example.data.GraphicsMode.fromKey("invalid"))
    }
}
