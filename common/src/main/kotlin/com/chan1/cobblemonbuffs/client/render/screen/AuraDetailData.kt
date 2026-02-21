package com.chan1.cobblemonbuffs.client.render.screen

import com.chan1.cobblemonbuffs.buff.EffectEntry
import com.chan1.cobblemonbuffs.buff.TierEffects
import com.chan1.cobblemonbuffs.buff.TriggerType
import com.chan1.cobblemonbuffs.buff.TypeBuff

internal val BUFF_DESCRIPTIONS = mapOf(
    "fire" to "Grants fire immunity after taking fire damage.",
    "ice" to "Chills attackers with slowness when they hit you.",
    "poison" to "Poisons attackers when they hit you. Withers undead mobs instead.",
    "steel" to "Reduces incoming damage when struck.",
    "rock" to "Grants an absorption shield when struck.",
    "electric" to "Grants a burst of speed when attacking.",
    "fighting" to "Grants strength and speed when attacking. Higher tiers cycle faster.",
    "ghost" to "Phase out when struck, turning invisible to escape. Higher tiers add speed.",
    "dragon" to "Grants strength and resistance when attacking.",
    "bug" to "Weakens enemies when you attack them.",
    "water" to "Grants water breathing while submerged.",
    "grass" to "Regenerates health while on natural ground.",
    "ground" to "Grants haste while deep underground.",
    "dark" to "Grants night vision at night. Higher tiers add strength.",
    "fairy" to "Regenerates health when low on HP.",
    "flying" to "Grants a burst of speed when hit. Higher tiers add jump boost.",
    "normal" to "Restores hunger over time when food is low.",
    "psychic" to "Lifts hostile enemies into the air when you strike them. Only affects hostile mobs or mobs targeting you."
)

internal val EFFECT_NAMES = mapOf(
    "minecraft:fire_resistance" to "Fire Resistance",
    "minecraft:water_breathing" to "Water Breathing",
    "minecraft:dolphins_grace" to "Dolphin's Grace",
    "minecraft:conduit_power" to "Conduit Power",
    "minecraft:saturation" to "Saturation",
    "minecraft:regeneration" to "Regeneration",
    "minecraft:speed" to "Speed",
    "minecraft:weakness" to "Weakness",
    "minecraft:resistance" to "Resistance",
    "minecraft:slowness" to "Slowness",
    "minecraft:absorption" to "Absorption",
    "minecraft:strength" to "Strength",
    "minecraft:haste" to "Haste",
    "minecraft:slow_falling" to "Slow Falling",
    "minecraft:jump_boost" to "Jump Boost",
    "minecraft:night_vision" to "Night Vision",
    "minecraft:invisibility" to "Invisibility",
    "minecraft:health_boost" to "Health Boost",
    "minecraft:luck" to "Luck",
    "minecraft:mining_fatigue" to "Mining Fatigue",
    "minecraft:poison" to "Poison",
    "minecraft:levitation" to "Levitation",
    "minecraft:glowing" to "Glowing",
    "minecraft:instant_health" to "Instant Health"
)

private val ROMAN_NUMERALS = arrayOf("", "II", "III", "IV", "V")

internal fun effectLabel(effect: EffectEntry): String {
    val id = effect.effectId.toString()
    val base = EFFECT_NAMES[id] ?: id.substringAfter(':').replace('_', ' ')
        .split(' ').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    if (effect.amplifier == 0) return base
    val suffix = ROMAN_NUMERALS.getOrElse(effect.amplifier) { "${effect.amplifier + 1}" }
    return "$base $suffix"
}

internal fun tierEffectsLabel(tierEffects: TierEffects): String {
    if (tierEffects.displayLabel != null) return tierEffects.displayLabel
    return tierEffects.effects.joinToString(" + ") { effectLabel(it) }
}

internal fun triggerLabel(type: TriggerType): String = when (type) {
    TriggerType.ON_KILL -> "On Kill"
    TriggerType.ON_HIT_RECEIVED -> "When Hit"
    TriggerType.ON_ATTACK -> "On Attack"
    TriggerType.CONDITIONAL_TICK -> "Conditional"
}

internal fun tierTriggerLabel(tierEffects: TierEffects): String =
    tierEffects.triggerDescription ?: triggerLabel(tierEffects.triggerType)

internal fun buffDescription(buff: TypeBuff): String? =
    buff.description ?: BUFF_DESCRIPTIONS[buff.typeName]

internal fun currentTierEffects(buff: TypeBuff, tier: Int): TierEffects = when (tier) {
    3 -> buff.t3 ?: buff.t2 ?: buff.t1
    2 -> buff.t2 ?: buff.t1
    else -> buff.t1
}
