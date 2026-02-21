package com.chan1.cobblemonbuffs.event

import com.chan1.cobblemonbuffs.buff.BuffManager
import com.chan1.cobblemonbuffs.buff.BuffRegistry
import com.chan1.cobblemonbuffs.config.CobblemonBuffsConfig
import com.chan1.cobblemonbuffs.network.CobblemonBuffsNetwork
import com.chan1.cobblemonbuffs.network.ConfigSyncPacket
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.reactive.ObservableSubscription
import dev.architectury.event.events.common.PlayerEvent
import dev.architectury.event.events.common.TickEvent
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object BuffEventHandler {
    private var tickCounter = 0
    private var safetyNetCounter = 0
    private var currentServer: MinecraftServer? = null

    // observable subs approach
    private val partySubscriptions = ConcurrentHashMap<UUID, ObservableSubscription<Unit>>()

    fun init() {
        CombatEventDispatcher.init()
        registerCobblemonEvents()
        registerLifecycleEvents()
        registerTickRefresh()
    }

    private fun registerCobblemonEvents() {
        CobblemonEvents.POKEMON_GAINED.subscribe { event ->
            val server = currentServer ?: return@subscribe
            val player = server.playerList.getPlayer(event.playerId) ?: return@subscribe
            BuffManager.markDirty(player.uuid)
            recalculateAndNotify(player)
        }

        CobblemonEvents.POKEMON_RELEASED_EVENT_POST.subscribe { event ->
            BuffManager.markDirty(event.player.uuid)
            recalculateAndNotify(event.player)
        }

        CobblemonEvents.POKEMON_SENT_POST.subscribe { event ->
            val entity = event.pokemonEntity
            val level = entity.level()
            val server = level.server ?: return@subscribe
            val ownerUUID = event.pokemon.storeCoordinates.get()?.store?.uuid ?: return@subscribe
            val player = server.playerList.getPlayer(ownerUUID) ?: return@subscribe
            BuffManager.markDirty(player.uuid)
            recalculateAndNotify(player)
        }

        CobblemonEvents.FRIENDSHIP_UPDATED.subscribe { event ->
            val pokemon = event.pokemon
            val storeCoords = pokemon.storeCoordinates.get() ?: return@subscribe
            val server = currentServer ?: return@subscribe
            val player = server.playerList.getPlayer(storeCoords.store.uuid) ?: return@subscribe
            BuffManager.markDirty(player.uuid)
            recalculateAndNotify(player)
        }

        CobblemonEvents.LEVEL_UP_EVENT.subscribe { event ->
            val pokemon = event.pokemon
            val storeCoords = pokemon.storeCoordinates.get() ?: return@subscribe
            val server = currentServer ?: return@subscribe
            val player = server.playerList.getPlayer(storeCoords.store.uuid) ?: return@subscribe
            BuffManager.markDirty(player.uuid)
            recalculateAndNotify(player)
        }
    }

    private fun registerLifecycleEvents() {
        PlayerEvent.PLAYER_JOIN.register { player ->
            if (player is ServerPlayer) {
                val config = CobblemonBuffsConfig.data
                CobblemonBuffsNetwork.sendConfigSync(player, ConfigSyncPacket(
                    config.t1Threshold, config.t2Threshold, config.t3Threshold,
                    config.levelMaxPoints, config.friendshipMaxPoints
                ))

                BuffManager.markDirty(player.uuid)
                BuffManager.recalculate(player, forceSync = true)

                val party = Cobblemon.storage.getParty(player)
                partySubscriptions.remove(player.uuid)?.unsubscribe()
                partySubscriptions[player.uuid] = party.getAnyChangeObservable()
                    .subscribe { BuffManager.markDirty(player.uuid) }
            }
        }

        PlayerEvent.PLAYER_RESPAWN.register { player, _, _ ->
            if (player is ServerPlayer) {
                BuffManager.markDirty(player.uuid)
                recalculateAndNotify(player)
            }
        }

        PlayerEvent.PLAYER_QUIT.register { player ->
            if (player is ServerPlayer) {
                partySubscriptions.remove(player.uuid)?.unsubscribe()
                BuffManager.clearTracking(player.uuid)
            }
        }

        PlayerEvent.CHANGE_DIMENSION.register { player, _, _ ->
            if (player is ServerPlayer) {
                BuffManager.markDirty(player.uuid)
                recalculateAndNotify(player)
            }
        }
    }

    private fun registerTickRefresh() {
        TickEvent.SERVER_POST.register { server ->
            currentServer = server

            if (BuffRegistry.dirty) {
                BuffRegistry.clearDirty()
                ConditionalTickHandler.clearCaches()

                val reloadConfig = CobblemonBuffsConfig.data
                val configPacket = ConfigSyncPacket(
                    reloadConfig.t1Threshold, reloadConfig.t2Threshold, reloadConfig.t3Threshold,
                    reloadConfig.levelMaxPoints, reloadConfig.friendshipMaxPoints
                )
                for (player in server.playerList.players) {
                    CobblemonBuffsNetwork.sendConfigSync(player, configPacket)
                    BuffManager.markDirty(player.uuid)
                    recalculateAndNotify(player)
                }
            }

            BuffManager.tickUpdate()
            tickCounter++

            ConditionalTickHandler.onServerTick(server)

            safetyNetCounter++
            if (safetyNetCounter >= SAFETY_NET_INTERVAL_TICKS) {
                safetyNetCounter = 0
                for (player in server.playerList.players) {
                    BuffManager.markDirty(player.uuid)
                }
            }

            if (tickCounter >= CobblemonBuffsConfig.data.refreshIntervalTicks) {
                tickCounter = 0
                for (player in server.playerList.players) {
                    if (BuffManager.isDirty(player.uuid)) {
                        BuffManager.recalculate(player)
                    }
                }
            }
        }
    }

    private const val SAFETY_NET_INTERVAL_TICKS = 1200

    private fun recalculateAndNotify(player: ServerPlayer) {
        val result = BuffManager.recalculate(player)
        if (!CobblemonBuffsConfig.data.showChatNotifications) return

        for (typeName in result.gained) {
            val displayName = typeName.replaceFirstChar { it.uppercase() }
            val pokemonName = result.providers[typeName] ?: "Pokemon"
            player.sendSystemMessage(
                Component.translatable("cobblemonbuffs.aura.gained", pokemonName, displayName)
                    .withStyle(ChatFormatting.GREEN)
            )
        }

        for (typeName in result.lost) {
            val displayName = typeName.replaceFirstChar { it.uppercase() }
            player.sendSystemMessage(
                Component.translatable("cobblemonbuffs.aura.lost", displayName)
                    .withStyle(ChatFormatting.RED)
            )
        }

        for (tierChange in result.tierChanges) {
            val displayName = tierChange.typeName.replaceFirstChar { it.uppercase() }
            val tierLabel = when (tierChange.newTier) {
                3 -> "III"
                2 -> "II"
                1 -> "I"
                else -> "None"
            }
            if (tierChange.newTier > tierChange.oldTier) {
                player.sendSystemMessage(
                    Component.translatable("cobblemonbuffs.aura.tier_up", displayName, tierLabel)
                        .withStyle(if (tierChange.newTier == 3) ChatFormatting.GOLD else ChatFormatting.GRAY)
                )
            } else {
                player.sendSystemMessage(
                    Component.translatable("cobblemonbuffs.aura.tier_down", displayName, tierLabel)
                        .withStyle(ChatFormatting.RED)
                )
            }
        }

        for (pokemonName in result.newlyAwakened) {
            player.sendSystemMessage(
                Component.translatable("cobblemonbuffs.aura.awakened", pokemonName)
                    .withStyle(ChatFormatting.LIGHT_PURPLE)
            )
        }
    }
}
