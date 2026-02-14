package com.chan1.cobblemonbuffs.network

import com.chan1.cobblemonbuffs.buff.ActiveAura
import com.chan1.cobblemonbuffs.buff.PartySlotInfo
import com.chan1.cobblemonbuffs.client.ClientAuraState
import dev.architectury.networking.NetworkManager
import dev.architectury.networking.simple.BaseS2CMessage
import dev.architectury.networking.simple.MessageType
import net.minecraft.network.RegistryFriendlyByteBuf

class AuraSyncPacket : BaseS2CMessage {
    val auras: List<ActiveAura>
    val partySlots: List<PartySlotInfo?>

    constructor(auras: List<ActiveAura>, partySlots: List<PartySlotInfo?> = emptyList()) {
        this.auras = auras
        this.partySlots = partySlots
    }

    constructor(buf: RegistryFriendlyByteBuf) {
        val slotCount = buf.readInt().coerceIn(0, MAX_PARTY_SLOTS)
        val slots = mutableListOf<PartySlotInfo?>()
        repeat(slotCount) {
            if (buf.readBoolean()) {
                slots.add(PartySlotInfo(
                    speciesName = buf.readUtf(),
                    level = buf.readInt(),
                    primaryType = buf.readUtf(),
                    secondaryType = buf.readUtf()
                ))
            } else {
                slots.add(null)
            }
        }
        this.partySlots = slots

        val auraCount = buf.readInt().coerceIn(0, MAX_AURAS)
        val list = mutableListOf<ActiveAura>()
        repeat(auraCount) {
            list.add(ActiveAura(
                id = buf.readUtf(),
                displayName = buf.readUtf(),
                tier = buf.readInt(),
                score = buf.readInt(),
                providerSlot = buf.readInt()
            ))
        }
        this.auras = list
    }

    override fun getType(): MessageType = CobblemonBuffsNetwork.AURA_SYNC_TYPE

    override fun write(buf: RegistryFriendlyByteBuf) {
        buf.writeInt(partySlots.size)
        for (slot in partySlots) {
            if (slot != null) {
                buf.writeBoolean(true)
                buf.writeUtf(slot.speciesName)
                buf.writeInt(slot.level)
                buf.writeUtf(slot.primaryType)
                buf.writeUtf(slot.secondaryType)
            } else {
                buf.writeBoolean(false)
            }
        }

        buf.writeInt(auras.size)
        for (aura in auras) {
            buf.writeUtf(aura.id)
            buf.writeUtf(aura.displayName)
            buf.writeInt(aura.tier)
            buf.writeInt(aura.score)
            buf.writeInt(aura.providerSlot)
        }
    }

    override fun handle(context: NetworkManager.PacketContext) {
        ClientAuraState.update(auras, partySlots)
    }

    companion object {
        private const val MAX_PARTY_SLOTS = 6
        private const val MAX_AURAS = 18
    }
}
