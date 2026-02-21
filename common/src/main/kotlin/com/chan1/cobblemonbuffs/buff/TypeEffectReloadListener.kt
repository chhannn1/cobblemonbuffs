package com.chan1.cobblemonbuffs.buff

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.architectury.registry.ReloadListenerRegistry
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

object TypeEffectReloadListener : SimpleJsonResourceReloadListener(Gson(), "type_effects") {
    private val logger = LoggerFactory.getLogger("CobblemonBuffs")
    private var configDir: Path? = null

    fun init(configDir: Path) {
        this.configDir = configDir
    }

    fun register() {
        ReloadListenerRegistry.register(PackType.SERVER_DATA, this)
    }

    private fun typeEffectsConfigDir(): Path? =
        configDir?.resolve("cobblemonbuffs")?.resolve("type_effects")

    private fun exportDefaults(resourceManager: ResourceManager) {
        val dir = typeEffectsConfigDir() ?: return
        if (Files.exists(dir)) return

        Files.createDirectories(dir)
        val gson = GsonBuilder().setPrettyPrinting().create()

        val resources = resourceManager.listResources("type_effects") { it.path.endsWith(".json") }
        for ((id, resource) in resources) {
            if (id.namespace != "cobblemonbuffs") continue
            val typeName = id.path.substringAfterLast('/').removeSuffix(".json")
            try {
                val json = resource.openAsReader().use { JsonParser.parseReader(it) }
                val outPath = dir.resolve("$typeName.json")
                Files.writeString(outPath, gson.toJson(json))
                logger.debug("Exported default type effect: {}", typeName)
            } catch (e: Exception) {
                logger.warn("Failed to export default type effect '{}': {}", typeName, e.message)
            }
        }
        logger.info("Exported default type effect configs to {}", dir)
    }

    override fun apply(
        entries: Map<ResourceLocation, JsonElement>,
        resourceManager: ResourceManager,
        profiler: ProfilerFiller
    ) {
        exportDefaults(resourceManager)

        val types = mutableMapOf<String, TypeBuff>()

        for ((id, element) in entries) {
            try {
                val typeName = id.path.substringAfterLast('/')
                val json = element.asJsonObject
                types[typeName] = parseTypeBuff(typeName, json)
            } catch (e: Exception) {
                logger.warn("Failed to load type effect definition '{}': {}", id, e.message)
            }
        }

        val dir = typeEffectsConfigDir()
        if (dir != null && Files.isDirectory(dir)) {
            try {
                Files.list(dir).use { stream ->
                    stream.filter { it.toString().endsWith(".json") }.forEach { path ->
                        try {
                            val typeName = path.fileName.toString().removeSuffix(".json")
                            val jsonStr = Files.readString(path)
                            val json = JsonParser.parseString(jsonStr).asJsonObject
                            types[typeName] = parseTypeBuff(typeName, json)
                        } catch (e: Exception) {
                            logger.warn("Failed to load config type effect '{}': {}", path.fileName, e.message)
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Failed to read type_effects config directory: {}", e.message)
            }
        }

        BuffRegistry.reload(types)
        logger.info("Loaded {} type effect definitions", types.size)
    }

    private fun parseTypeBuff(typeName: String, json: JsonObject): TypeBuff {
        val t1 = parseTierEffects(json.getAsJsonObject("t1")
            ?: throw IllegalArgumentException("Missing required 't1' tier"))
        val t2 = json.getAsJsonObject("t2")?.let { parseTierEffects(it) }
        val t3 = json.getAsJsonObject("t3")?.let { parseTierEffects(it) }
        val description = json.get("description")?.asString
        val maintained = json.get("maintained")?.asBoolean ?: false
        val alwaysMaintainEffects = json.getAsJsonArray("alwaysMaintainEffects")
            ?.map { it.asString }
            ?: emptyList()

        return TypeBuff(
            typeName = typeName,
            t1 = t1,
            t2 = t2,
            t3 = t3,
            description = description,
            maintained = maintained,
            alwaysMaintainEffects = alwaysMaintainEffects
        )
    }

    private fun parseTierEffects(json: JsonObject): TierEffects {
        val effects = json.getAsJsonArray("effects")?.map { effectElement ->
            val effectObj = effectElement.asJsonObject
            EffectEntry(
                effectId = ResourceLocation.parse(effectObj.get("effectId").asString),
                amplifier = effectObj.get("amplifier").asInt,
                durationTicks = effectObj.get("durationTicks").asInt
            )
        } ?: emptyList()

        val triggerType = TriggerType.valueOf(json.get("triggerType").asString)
        val target = json.get("target")?.asString?.let { EffectTarget.valueOf(it) } ?: EffectTarget.SELF
        val cooldownTicks = json.get("cooldownTicks")?.asInt ?: 0
        val displayLabel = json.get("displayLabel")?.asString
        val triggerDescription = json.get("triggerDescription")?.asString

        return TierEffects(
            effects = effects,
            triggerType = triggerType,
            target = target,
            cooldownTicks = cooldownTicks,
            displayLabel = displayLabel,
            triggerDescription = triggerDescription
        )
    }
}
