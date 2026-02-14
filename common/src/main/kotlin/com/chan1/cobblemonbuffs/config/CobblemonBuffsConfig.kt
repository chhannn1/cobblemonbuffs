package com.chan1.cobblemonbuffs.config

import com.chan1.cobblemonbuffs.CobblemonBuffs
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.nio.file.Files
import java.nio.file.Path

data class CobblemonBuffsConfigData(
    val maxActiveBuffs: Int = 8,
    val refreshIntervalTicks: Int = 100,
    val showChatNotifications: Boolean = true,
    val enabledTypes: Map<String, Boolean> = emptyMap(),
    val friendshipMaxThreshold: Int = 255,
    val enableAuraParticles: Boolean = true,
    val levelMaxPoints: Int = 60,
    val friendshipMaxPoints: Int = 40,
    val t1Threshold: Int = 10,
    val t2Threshold: Int = 35,
    val t3Threshold: Int = 75,
    val conditionalCheckTicks: Int = 20,
    val tierOverrides: Map<String, Int> = emptyMap(),
    val enableTier2: Boolean = true,
    val enableTier3: Boolean = true
) {
    fun validated(): CobblemonBuffsConfigData {
        val t1 = t1Threshold.coerceAtLeast(0)
        val t2 = t2Threshold.coerceAtLeast(t1)
        val t3 = t3Threshold.coerceAtLeast(t2)
        return copy(
            maxActiveBuffs = maxActiveBuffs.coerceAtLeast(0),
            refreshIntervalTicks = refreshIntervalTicks.coerceAtLeast(1),
            conditionalCheckTicks = conditionalCheckTicks.coerceAtLeast(1),
            levelMaxPoints = levelMaxPoints.coerceAtLeast(0),
            friendshipMaxPoints = friendshipMaxPoints.coerceAtLeast(0),
            friendshipMaxThreshold = friendshipMaxThreshold.coerceIn(1, 255),
            t1Threshold = t1,
            t2Threshold = t2,
            t3Threshold = t3
        )
    }
}

object CobblemonBuffsConfig {
    private val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
    private lateinit var configPath: Path

    var data: CobblemonBuffsConfigData = CobblemonBuffsConfigData()
        private set

    fun init(configDir: Path) {
        configPath = configDir.resolve("cobblemonbuffs.json")
        load()
    }

    fun load() {
        if (!::configPath.isInitialized) return
        if (Files.exists(configPath)) {
            try {
                val json = Files.readString(configPath)
                data = (GSON.fromJson(json, CobblemonBuffsConfigData::class.java) ?: CobblemonBuffsConfigData()).validated()
            } catch (e: Exception) {
                CobblemonBuffs.LOGGER.warn("Failed to load config, using defaults: {}", e.message)
                data = CobblemonBuffsConfigData()
                save()
            }
        } else {
            data = CobblemonBuffsConfigData()
            save()
        }
    }

    fun save() {
        if (!::configPath.isInitialized) return
        try {
            Files.createDirectories(configPath.parent)
            Files.writeString(configPath, GSON.toJson(data))
        } catch (e: Exception) {
            CobblemonBuffs.LOGGER.warn("Failed to save config: {}", e.message)
        }
    }

    fun isTypeEnabled(typeName: String): Boolean {
        return data.enabledTypes.getOrDefault(typeName.lowercase(), true)
    }

    fun getTierOverride(typeName: String): Int? {
        return data.tierOverrides[typeName.lowercase()]
    }
}
