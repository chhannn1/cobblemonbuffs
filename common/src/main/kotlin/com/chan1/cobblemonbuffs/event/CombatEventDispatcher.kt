package com.chan1.cobblemonbuffs.event

import com.chan1.cobblemonbuffs.buff.AuraTier
import com.chan1.cobblemonbuffs.buff.BuffManager
import dev.architectury.event.EventResult
import dev.architectury.event.events.common.EntityEvent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.LivingEntity

object CombatEventDispatcher {

    fun init() {
        registerDeathEvent()
        registerHurtEvent()
    }

    private fun registerDeathEvent() {
        EntityEvent.LIVING_DEATH.register { entity, source ->
            val killer = source.entity
            if (killer is ServerPlayer) {
                val tiers = BuffManager.getPlayerTiers(killer.uuid)
                for ((typeName, tier) in tiers) {
                    if (tier >= AuraTier.T1) {
                        TypeEffectHandlers.handleKill(killer, entity, typeName, tier)
                    }
                }
            }
            EventResult.pass()
        }
    }

    private fun registerHurtEvent() {
        EntityEvent.LIVING_HURT.register { entity, source, _ ->
            val isTargetPlayer = entity is ServerPlayer
            val sourceEntity = source.entity
            val isAttackerPlayer = sourceEntity is ServerPlayer
            if (!isTargetPlayer && !isAttackerPlayer) {
                return@register EventResult.pass()
            }

            if (isTargetPlayer) {
                val player = entity as ServerPlayer
                val attacker = sourceEntity as? LivingEntity
                val tiers = BuffManager.getPlayerTiers(player.uuid)
                for ((typeName, tier) in tiers) {
                    if (tier >= AuraTier.T1) {
                        if (attacker != null || typeName == "fire") {
                            TypeEffectHandlers.handleHitReceived(player, attacker, source, typeName, tier)
                        }
                    }
                }
            }

            if (isAttackerPlayer && entity is LivingEntity) {
                val attacker = sourceEntity as ServerPlayer
                val tiers = BuffManager.getPlayerTiers(attacker.uuid)
                for ((typeName, tier) in tiers) {
                    if (tier >= AuraTier.T1) {
                        TypeEffectHandlers.handleAttack(attacker, entity, typeName, tier)
                    }
                }
            }

            EventResult.pass()
        }
    }
}
