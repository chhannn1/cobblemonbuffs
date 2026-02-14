package com.chan1.cobblemonbuffs.buff

import com.chan1.cobblemonbuffs.config.CobblemonBuffsConfig
import com.chan1.cobblemonbuffs.network.AuraSyncPacket
import com.chan1.cobblemonbuffs.network.CobblemonBuffsNetwork
import com.cobblemon.mod.common.Cobblemon
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffect
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

// a mess currently

object BuffManager {
    private val previousActiveAuras = ConcurrentHashMap<UUID, List<ActiveAura>>()
    private val previousPartySlots = ConcurrentHashMap<UUID, List<PartySlotInfo?>>()
    private val previousAwakened = ConcurrentHashMap<UUID, Set<UUID>>()
    private val dirtyPlayers: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    val playerTiers = ConcurrentHashMap<UUID, Map<String, AuraTier>>()
    val cooldowns = ConcurrentHashMap<UUID, ConcurrentHashMap<String, Long>>()
    val playerActiveTypes = ConcurrentHashMap<UUID, Set<String>>()

    data class RecalcResult(
        val gained: List<String>,
        val lost: List<String>,
        val newlyAwakened: List<String>,
        val tierChanges: List<TierChange>,
        val providers: Map<String, String>
    )

    data class TierChange(
        val typeName: String,
        val oldTier: Int,
        val newTier: Int
    )

    fun recalculate(player: ServerPlayer, forceSync: Boolean = false): RecalcResult {
        val config = CobblemonBuffsConfig.data
        val party = Cobblemon.storage.getParty(player)
        val gappyList = party.toGappyList()
        val slottedPokemon = gappyList.mapIndexedNotNull { slot, pokemon ->
            pokemon?.let { slot to it }
        }

        val computeResult = AuraPowerCalculator.computeScores(slottedPokemon, config)
        val scores = computeResult.scores
        val candidates = computeResult.candidates

        val sortedTypes = scores.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, AuraPowerScore>> { it.value.score }
                    .thenBy { candidates[it.key]?.partyOrder ?: Int.MAX_VALUE }
            )
            .map { it.key }

        val limitedTypes = sortedTypes.take(config.maxActiveBuffs)

        val awakenedPokemon = slottedPokemon
            .map { it.second }
            .filter { it.friendship >= config.friendshipMaxThreshold }
        val allAwakenedIds = awakenedPokemon.map { it.uuid }.toSet()
        val oldAwakenedIds = previousAwakened.getOrDefault(player.uuid, emptySet())
        val newlyAwakenedIds = allAwakenedIds - oldAwakenedIds
        val newlyAwakened = awakenedPokemon.filter { it.uuid in newlyAwakenedIds }
            .map { it.species.name }

        val tierMap = mutableMapOf<String, AuraTier>()
        for (typeName in limitedTypes) {
            val score = scores[typeName] ?: continue
            tierMap[typeName] = score.tier
        }
        playerTiers[player.uuid] = tierMap
        playerActiveTypes[player.uuid] = limitedTypes.toSet()

        val partySlotInfos: List<PartySlotInfo?> = gappyList.map { pokemon ->
            pokemon?.let {
                PartySlotInfo(
                    speciesName = it.species.name.replaceFirstChar { c -> c.uppercase() },
                    level = it.level,
                    primaryType = it.primaryType.name.lowercase(),
                    secondaryType = it.secondaryType?.name?.lowercase() ?: ""
                )
            }
        }

        val activeAuras = mutableListOf<ActiveAura>()
        for (typeName in limitedTypes) {
            val score = scores[typeName] ?: continue
            val candidate = candidates[typeName]
            activeAuras.add(ActiveAura(
                id = typeName,
                displayName = typeName.replaceFirstChar { it.uppercase() },
                tier = score.tier.level,
                score = score.score,
                providerSlot = candidate?.partyOrder ?: -1
            ))
        }

        val oldActiveAuras = previousActiveAuras.getOrDefault(player.uuid, emptyList())
        val oldAuraIds = oldActiveAuras.map { it.id }.toSet()
        val currentAuraIds = activeAuras.map { it.id }.toSet()

        val gained = (currentAuraIds - oldAuraIds).toList()
        val lost = (oldAuraIds - currentAuraIds).toList()

        val oldTierByType = oldActiveAuras.associate { it.id to it.tier }
        val tierChanges = mutableListOf<TierChange>()
        for (aura in activeAuras) {
            val oldTier = oldTierByType[aura.id] ?: continue
            if (aura.tier != oldTier) {
                tierChanges.add(TierChange(aura.id, oldTier, aura.tier))
            }
        }

        val oldPartySlots = previousPartySlots.getOrDefault(player.uuid, emptyList())
        val aurasChanged = activeAuras != oldActiveAuras
        val partySlotsChanged = partySlotInfos != oldPartySlots
        if (forceSync || aurasChanged || partySlotsChanged) {
            CobblemonBuffsNetwork.sendAuraSync(player, AuraSyncPacket(activeAuras, partySlotInfos))
        }

        previousActiveAuras[player.uuid] = activeAuras
        previousPartySlots[player.uuid] = partySlotInfos
        previousAwakened[player.uuid] = allAwakenedIds

        dirtyPlayers.remove(player.uuid)

        val providers = candidates.mapValues { it.value.bestPokemonName }
        return RecalcResult(gained, lost, newlyAwakened, tierChanges, providers)
    }

    fun markDirty(playerUUID: UUID) {
        dirtyPlayers.add(playerUUID)
    }

    fun isDirty(playerUUID: UUID): Boolean = playerUUID in dirtyPlayers

    fun clearAll(player: ServerPlayer) {
        // remove aura-applied effects
        val tiers = playerTiers[player.uuid]
        if (tiers != null) {
            for ((typeName, tier) in tiers) {
                for (triggerType in TriggerType.entries) {
                    val triggered = BuffRegistry.getTriggeredEffects(typeName, tier, triggerType)
                    if (triggered != null) {
                        for (effect in triggered.tierEffects.effects) {
                            val holder = resolveEffect(effect.effectId) ?: continue
                            player.removeEffect(holder)
                        }
                    }
                }
            }
        }
        previousActiveAuras.remove(player.uuid)
        previousPartySlots.remove(player.uuid)
        previousAwakened.remove(player.uuid)
        dirtyPlayers.remove(player.uuid)
        playerTiers.remove(player.uuid)
        playerActiveTypes.remove(player.uuid)
        cooldowns.remove(player.uuid)
        CobblemonBuffsNetwork.sendAuraSync(player, AuraSyncPacket(emptyList()))
    }

    fun clearTracking(playerUUID: UUID) {
        previousActiveAuras.remove(playerUUID)
        previousPartySlots.remove(playerUUID)
        previousAwakened.remove(playerUUID)
        dirtyPlayers.remove(playerUUID)
        playerTiers.remove(playerUUID)
        playerActiveTypes.remove(playerUUID)
        cooldowns.remove(playerUUID)
    }

    fun getPlayerTiers(uuid: UUID): Map<String, AuraTier> {
        return playerTiers[uuid] ?: emptyMap()
    }

    var currentTick: Long = 0L
        private set

    fun tickUpdate() {
        currentTick++
    }

    fun isOnCooldown(playerUUID: UUID, key: String): Boolean {
        val playerCooldowns = cooldowns[playerUUID] ?: return false
        val expiry = playerCooldowns[key] ?: return false
        if (currentTick >= expiry) {
            playerCooldowns.remove(key)
            return false
        }
        return true
    }

    fun setCooldown(playerUUID: UUID, key: String, durationTicks: Int) {
        val playerCooldowns = cooldowns.getOrPut(playerUUID) { ConcurrentHashMap() }
        playerCooldowns[key] = currentTick + durationTicks
    }

    fun resolveEffect(effectId: ResourceLocation): Holder<MobEffect>? {
        return BuiltInRegistries.MOB_EFFECT.getHolder(effectId).orElse(null)
    }
}
