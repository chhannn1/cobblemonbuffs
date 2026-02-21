package com.chan1.cobblemonbuffs.network

import com.chan1.cobblemonbuffs.client.ClientConfigState
import dev.architectury.networking.NetworkManager
import dev.architectury.networking.simple.BaseS2CMessage
import dev.architectury.networking.simple.MessageType
import net.minecraft.network.RegistryFriendlyByteBuf

class ConfigSyncPacket : BaseS2CMessage {
    val t1Threshold: Int
    val t2Threshold: Int
    val t3Threshold: Int
    val levelMaxPoints: Int
    val friendshipMaxPoints: Int

    constructor(t1: Int, t2: Int, t3: Int, levelMax: Int, friendshipMax: Int) {
        this.t1Threshold = t1
        this.t2Threshold = t2
        this.t3Threshold = t3
        this.levelMaxPoints = levelMax
        this.friendshipMaxPoints = friendshipMax
    }

    constructor(buf: RegistryFriendlyByteBuf) {
        this.t1Threshold = buf.readInt()
        this.t2Threshold = buf.readInt()
        this.t3Threshold = buf.readInt()
        this.levelMaxPoints = buf.readInt()
        this.friendshipMaxPoints = buf.readInt()
    }

    override fun getType(): MessageType = CobblemonBuffsNetwork.CONFIG_SYNC_TYPE

    override fun write(buf: RegistryFriendlyByteBuf) {
        buf.writeInt(t1Threshold)
        buf.writeInt(t2Threshold)
        buf.writeInt(t3Threshold)
        buf.writeInt(levelMaxPoints)
        buf.writeInt(friendshipMaxPoints)
    }

    override fun handle(context: NetworkManager.PacketContext) {
        context.queue(Runnable {
            ClientConfigState.update(t1Threshold, t2Threshold, t3Threshold, levelMaxPoints, friendshipMaxPoints)
        })
    }
}
