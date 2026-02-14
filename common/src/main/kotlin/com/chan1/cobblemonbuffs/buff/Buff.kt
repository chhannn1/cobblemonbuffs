package com.chan1.cobblemonbuffs.buff

import net.minecraft.resources.ResourceLocation

enum class TriggerType {
    ON_KILL,
    ON_HIT_RECEIVED,
    ON_ATTACK,
    CONDITIONAL_TICK
}

enum class EffectTarget {
    SELF,
    OTHER
}

data class EffectEntry(
    val effectId: ResourceLocation,
    val amplifier: Int,
    val durationTicks: Int
)

data class TierEffects(
    val effects: List<EffectEntry>,
    val triggerType: TriggerType,
    val target: EffectTarget = EffectTarget.SELF,
    val cooldownTicks: Int = 0,
    val displayLabel: String? = null,
    val triggerDescription: String? = null
)

data class TriggeredTierEffect(
    val tier: AuraTier,
    val tierEffects: TierEffects
)


data class TypeBuff(
    val typeName: String,
    val t1: TierEffects,
    val t2: TierEffects? = null,
    val t3: TierEffects? = null,
    val description: String? = null,
    val maintained: Boolean = false,
    val alwaysMaintainEffects: List<String> = emptyList()
)


data class PartySlotInfo(
    val speciesName: String,
    val level: Int,
    val primaryType: String,
    val secondaryType: String
)


data class ActiveAura(
    val id: String,
    val displayName: String,
    val tier: Int,
    val score: Int,
    val providerSlot: Int = -1
)
