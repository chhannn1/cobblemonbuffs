package com.chan1.cobblemonbuffs.buff

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import dev.architectury.registry.ReloadListenerRegistry
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import org.slf4j.LoggerFactory

object TypeEffectReloadListener : SimpleJsonResourceReloadListener(Gson(), "type_effects") {
    private val logger = LoggerFactory.getLogger("CobblemonBuffs")

    fun register() {
        ReloadListenerRegistry.register(PackType.SERVER_DATA, this)
    }

    override fun apply(
        entries: Map<ResourceLocation, JsonElement>,
        resourceManager: ResourceManager,
        profiler: ProfilerFiller
    ) {
        val types = mutableMapOf<String, TypeBuff>()

        for ((id, element) in entries) {
            try {
                val typeName = id.path.substringAfterLast('/')
                val json = element.asJsonObject
                val typeBuff = parseTypeBuff(typeName, json)
                types[typeName] = typeBuff
            } catch (e: Exception) {
                logger.warn("Failed to load type effect definition '{}': {}", id, e.message)
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
