package com.chan1.cobblemonbuffs.event

import com.chan1.cobblemonbuffs.buff.AuraTier
import com.chan1.cobblemonbuffs.buff.BuffManager
import com.chan1.cobblemonbuffs.buff.BuffRegistry
import com.chan1.cobblemonbuffs.buff.EffectEntry
import com.chan1.cobblemonbuffs.buff.EffectTarget
import com.chan1.cobblemonbuffs.buff.TriggerType
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.DamageTypeTags
import net.minecraft.tags.EntityTypeTags
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.monster.Enemy

object TypeEffectHandlers {

    fun handleKill(player: ServerPlayer, killed: LivingEntity, typeName: String, tier: AuraTier) {
        applyTriggerEffects(player, killed, typeName, tier, TriggerType.ON_KILL)
    }

    fun handleHitReceived(
        player: ServerPlayer,
        attacker: LivingEntity?,
        source: DamageSource,
        typeName: String,
        tier: AuraTier
    ) {
        if (typeName == "fire" && !source.`is`(DamageTypeTags.IS_FIRE)) return
        applyTriggerEffects(player, attacker, typeName, tier, TriggerType.ON_HIT_RECEIVED)
    }

    fun handleAttack(player: ServerPlayer, target: LivingEntity, typeName: String, tier: AuraTier) {
        if (typeName == "psychic" && target !is Enemy && !((target as? Mob)?.target == player)) return
        applyTriggerEffects(player, target, typeName, tier, TriggerType.ON_ATTACK)
    }

    private fun applyTriggerEffects(
        actor: ServerPlayer,
        other: LivingEntity?,
        typeName: String,
        tier: AuraTier,
        triggerType: TriggerType
    ) {
        val triggered = BuffRegistry.getTriggeredEffects(typeName, tier, triggerType) ?: return
        val tierEffects = triggered.tierEffects

        val target: LivingEntity = when (tierEffects.target) {
            EffectTarget.SELF -> actor
            EffectTarget.OTHER -> other ?: return
        }

        val cooldownKey = if (tierEffects.cooldownTicks > 0) {
            val key = cooldownKey(typeName, triggerType)
            if (BuffManager.isOnCooldown(actor.uuid, key)) return
            key
        } else null

        var applied = false
        for (effect in tierEffects.effects) {
            if (applyEffect(target, effect)) applied = true
        }

        // sync fix
        if (applied && cooldownKey != null) {
            BuffManager.setCooldown(actor.uuid, cooldownKey, tierEffects.cooldownTicks)
        }
    }

    private fun cooldownKey(typeName: String, triggerType: TriggerType): String {
        return "trigger_${typeName}_${triggerType.name.lowercase()}"
    }

    private val POISON_ID: ResourceLocation = ResourceLocation.parse("minecraft:poison")
    private val WITHER_ID: ResourceLocation = ResourceLocation.parse("minecraft:wither")

    private fun applyEffect(entity: LivingEntity, effect: EffectEntry): Boolean {
        // undead mobs are immune to poison, so best approach is to apply wither instead
        val effectId = if (effect.effectId == POISON_ID && entity.type.`is`(EntityTypeTags.UNDEAD)) {
            WITHER_ID
        } else {
            effect.effectId
        }
        val holder = BuffManager.resolveEffect(effectId) ?: return false
        val instance = MobEffectInstance(
            holder,
            effect.durationTicks,
            effect.amplifier,
            false,
            true,
            true
        )
        entity.addEffect(instance)
        return true
    }
}
