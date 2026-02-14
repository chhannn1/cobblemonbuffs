package com.chan1.cobblemonbuffs.event

import com.chan1.cobblemonbuffs.buff.AuraTier
import com.chan1.cobblemonbuffs.buff.BuffManager
import com.chan1.cobblemonbuffs.buff.BuffRegistry
import com.chan1.cobblemonbuffs.buff.EffectEntry
import com.chan1.cobblemonbuffs.buff.TriggerType
import com.chan1.cobblemonbuffs.config.CobblemonBuffsConfig
import net.minecraft.core.BlockPos
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.BlockTags
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.LightLayer
import java.util.concurrent.ConcurrentHashMap

object ConditionalTickHandler {
    private var tickCounter = 0

    private val cooldownKeys = ConcurrentHashMap<String, String>()
    private val alwaysMaintainCache = ConcurrentHashMap<String, Set<String>>()

    fun onServerTick(server: MinecraftServer) {
        tickCounter++
        if (tickCounter < CobblemonBuffsConfig.data.conditionalCheckTicks) return
        tickCounter = 0

        for (player in server.playerList.players) {
            val tiers = BuffManager.getPlayerTiers(player.uuid)
            if (tiers.isEmpty()) continue

            for ((typeName, tier) in tiers) {
                if (tier >= AuraTier.T1) {
                    handleConditional(player, typeName, tier)
                }
            }
        }
    }

    private fun handleConditional(player: ServerPlayer, typeName: String, tier: AuraTier) {
        val triggered = BuffRegistry.getTriggeredEffects(typeName, tier, TriggerType.CONDITIONAL_TICK) ?: return

        if (!isConditionalStateValid(player, typeName, triggered.tier)) {
            // condition no longer met
            for (effect in triggered.tierEffects.effects) {
                val holder = BuffManager.resolveEffect(effect.effectId) ?: continue
                val existing = player.getEffect(holder)
                if (existing != null && existing.isAmbient) {
                    player.removeEffect(holder)
                }
            }
            return
        }

        // directly add hunger bar so no sat padding (which would make it op)
        if (typeName == "normal") {
            val cooldownKey = cooldownKey(typeName)
            if (triggered.tierEffects.cooldownTicks > 0 && BuffManager.isOnCooldown(player.uuid, cooldownKey)) return

            val hungerToRestore = if (triggered.tier >= AuraTier.T2) 2 else 1
            player.foodData.foodLevel = (player.foodData.foodLevel + hungerToRestore).coerceAtMost(20)

            if (triggered.tier == AuraTier.T3) {
                for (effect in triggered.tierEffects.effects) {
                    if (effect.effectId.toString() == "minecraft:regeneration") {
                        applyIfAbsent(player, effect)
                    }
                }
            }

            if (triggered.tierEffects.cooldownTicks > 0) {
                BuffManager.setCooldown(player.uuid, cooldownKey, triggered.tierEffects.cooldownTicks)
            }
            return
        }

        // weird approach, fuck it

        val typeBuff = BuffRegistry.getForType(typeName)
        val isMaintained = typeBuff?.maintained ?: false
        val alwaysMaintain = typeBuff?.let {
            alwaysMaintainCache.getOrPut(typeName) { it.alwaysMaintainEffects.toSet() }
        } ?: emptySet()

        if (isMaintained) {
            val tierEffects = triggered.tierEffects
            if (tierEffects.cooldownTicks > 0) {
                val cooldownKey = cooldownKey(typeName)
                val onCooldown = BuffManager.isOnCooldown(player.uuid, cooldownKey)
                var appliedGated = false

                for (effect in tierEffects.effects) {
                    val key = effect.effectId.toString()
                    if (key in alwaysMaintain) {
                        applyShortDuration(player, effect)
                    } else if (!onCooldown) {
                        applyShortDuration(player, effect)
                        appliedGated = true
                    }
                }

                if (appliedGated) {
                    BuffManager.setCooldown(player.uuid, cooldownKey, tierEffects.cooldownTicks)
                }
            } else {
                for (effect in tierEffects.effects) {
                    applyShortDuration(player, effect)
                }
            }
        } else {
            val cooldownKey = cooldownKey(typeName)
            if (triggered.tierEffects.cooldownTicks > 0 && BuffManager.isOnCooldown(player.uuid, cooldownKey)) return

            var applied = false
            for (effect in triggered.tierEffects.effects) {
                if (applyIfAbsent(player, effect)) applied = true
            }
            if (applied && triggered.tierEffects.cooldownTicks > 0) {
                BuffManager.setCooldown(player.uuid, cooldownKey, triggered.tierEffects.cooldownTicks)
            }
        }
    }

    private fun cooldownKey(typeName: String): String =
        cooldownKeys.getOrPut(typeName) { "conditional_$typeName" }

    fun clearCaches() {
        alwaysMaintainCache.clear()
    }

    private fun isConditionalStateValid(player: ServerPlayer, typeName: String, effectTier: AuraTier): Boolean {
        val isT3 = effectTier == AuraTier.T3
        return when (typeName) {
            "water" -> player.isInWater
            "ground" -> if (isT3) player.blockY < 62 else player.blockY < 50
            "grass" -> player.health < player.maxHealth && (if (isT3) isOnAnySolidBlock(player) else isOnNaturalBlock(player))
            "dark" -> if (isT3) isNighttime(player) || getLightLevel(player) <= 7 else isNighttime(player)
            "fairy" -> {
                val healthPercent = player.health / player.maxHealth
                when (effectTier) {
                    AuraTier.T3 -> healthPercent < 0.50f
                    AuraTier.T2 -> healthPercent < 0.40f
                    else -> healthPercent < 0.30f
                }
            }
            "normal" -> {
                val threshold = when (effectTier) {
                    AuraTier.T3 -> 17
                    AuraTier.T2 -> 14
                    else -> 10
                }
                player.foodData.foodLevel < threshold
            }
            else -> true
        }
    }

    private fun isOnNaturalBlock(player: ServerPlayer): Boolean {
        val belowPos = player.blockPosition().below()
        val blockState = player.level().getBlockState(belowPos)
        // just making it more balanced by making the specific blocks make it more conditional
        return blockState.`is`(BlockTags.DIRT) ||
            blockState.`is`(BlockTags.SAND) ||
            blockState.`is`(BlockTags.BASE_STONE_OVERWORLD) ||
            blockState.`is`(BlockTags.LEAVES) ||
            blockState.`is`(BlockTags.LOGS)
    }

    private fun isOnAnySolidBlock(player: ServerPlayer): Boolean {
        val belowPos = player.blockPosition().below()
        val blockState = player.level().getBlockState(belowPos)
        return !blockState.isAir
    }

    private fun isNighttime(player: ServerPlayer): Boolean {
        val dayTime = player.level().dayTime % 24000
        return dayTime >= 13000 && dayTime < 23000
    }

    private fun getLightLevel(player: ServerPlayer): Int {
        val pos = player.blockPosition()
        return player.level().getBrightness(LightLayer.BLOCK, pos)
    }

    private fun applyIfAbsent(entity: LivingEntity, effect: EffectEntry): Boolean {
        val holder = BuffManager.resolveEffect(effect.effectId) ?: return false
        val existing = entity.getEffect(holder)
        if (existing != null) return false
        val instance = MobEffectInstance(
            holder,
            effect.durationTicks,
            effect.amplifier,
            true,
            false,
            true
        )
        entity.addEffect(instance)
        return true
    }

    private fun applyShortDuration(entity: LivingEntity, effect: EffectEntry) {
        val holder = BuffManager.resolveEffect(effect.effectId) ?: return
        val existing = entity.getEffect(holder)
        if (existing != null && !existing.isAmbient) return
        if (existing != null && existing.amplifier > effect.amplifier) return
        if (existing != null && existing.amplifier >= effect.amplifier && existing.duration > effect.durationTicks) {
            return
        }
        val instance = MobEffectInstance(
            holder,
            effect.durationTicks,
            effect.amplifier,
            true,
            false,
            true
        )
        entity.addEffect(instance)
    }
}
