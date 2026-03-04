package com.chan1.cobblemonbuffs.network

import com.chan1.cobblemonbuffs.CobblemonBuffs
import dev.architectury.networking.NetworkManager
import dev.architectury.networking.simple.MessageType
import dev.architectury.networking.simple.SimpleNetworkManager
import dev.architectury.utils.Env
import dev.architectury.utils.EnvExecutor
import net.minecraft.server.level.ServerPlayer

object CobblemonBuffsNetwork {
    private val NETWORK: SimpleNetworkManager = SimpleNetworkManager.create(CobblemonBuffs.MOD_ID)

    lateinit var AURA_SYNC_TYPE: MessageType
        private set

    lateinit var CONFIG_SYNC_TYPE: MessageType
        private set

    fun init() {
        AURA_SYNC_TYPE = NETWORK.registerS2C("aura_sync", ::AuraSyncPacket)
        CONFIG_SYNC_TYPE = NETWORK.registerS2C("config_sync", ::ConfigSyncPacket)

        EnvExecutor.runInEnv(Env.SERVER) {
            Runnable {
                NetworkManager.registerS2CPayloadType(AURA_SYNC_TYPE.id)
                NetworkManager.registerS2CPayloadType(CONFIG_SYNC_TYPE.id)
            }
        }
    }

    fun sendAuraSync(player: ServerPlayer, packet: AuraSyncPacket) {
        packet.sendTo(player)
    }

    fun sendConfigSync(player: ServerPlayer, packet: ConfigSyncPacket) {
        packet.sendTo(player)
    }
}
