package com.chan1.cobblemonbuffs.client

import com.chan1.cobblemonbuffs.buff.ActiveAura
import com.chan1.cobblemonbuffs.buff.PartySlotInfo

object ClientAuraState {
    var auras: List<ActiveAura> = emptyList()
        private set
    var partySlots: List<PartySlotInfo?> = emptyList()
        private set

    var gainedAuras: List<ActiveAura> = emptyList()
        private set
    var lostAuraIds: List<String> = emptyList()
        private set
    var tierChanged: Boolean = false
        private set

    var tieredUpAuraIds: Set<String> = emptySet()
        private set

    var hudEventPending: Boolean = false
    var hudEventText: String = ""
        private set
    var hudEventTierUpIds: Set<String> = emptySet()
        private set

    fun update(auras: List<ActiveAura>, partySlots: List<PartySlotInfo?> = emptyList()) {
        val oldIds = this.auras.map { it.id }.toSet()
        val newIds = auras.map { it.id }.toSet()

        gainedAuras = auras.filter { it.id !in oldIds }
        lostAuraIds = oldIds.filter { it !in newIds }

        val oldTiers = this.auras.associate { it.id to it.tier }
        val tierUpTo3 = auras.any { it.id in oldTiers && oldTiers[it.id]!! < 3 && it.tier >= 3 }
        val tierUpTo2 = auras.any { it.id in oldTiers && oldTiers[it.id]!! < 2 && it.tier >= 2 }
        val tierDown = auras.any { it.id in oldTiers && it.tier < oldTiers[it.id]!! }
        tierChanged = auras.any { it.id in oldTiers && oldTiers[it.id] != it.tier }

        tieredUpAuraIds = auras
            .filter { it.id in oldTiers && it.tier > oldTiers[it.id]!! }
            .map { it.id }
            .toSet()

        if (gainedAuras.isNotEmpty() || lostAuraIds.isNotEmpty() || tierChanged) {
            hudEventPending = true
            hudEventText = buildEventText(tierUpTo3, tierUpTo2, tierDown)
            hudEventTierUpIds = tieredUpAuraIds
        }

        this.auras = auras
        this.partySlots = partySlots
    }

    private fun buildEventText(tierUpTo3: Boolean, tierUpTo2: Boolean, tierDown: Boolean): String {
        if (tierUpTo3) return "Tier III!"
        if (tierUpTo2) return "Tier II!"
        if (gainedAuras.isNotEmpty()) return "New Aura!"
        if (tierDown) return "Tier Down"
        if (lostAuraIds.isNotEmpty()) return "Aura Lost"
        return ""
    }

    fun acknowledgeChanges() {
        gainedAuras = emptyList()
        lostAuraIds = emptyList()
        tierChanged = false
        tieredUpAuraIds = emptySet()
    }
    // todo: later
    fun clear() {
        auras = emptyList()
        partySlots = emptyList()
        gainedAuras = emptyList()
        lostAuraIds = emptyList()
        tierChanged = false
        tieredUpAuraIds = emptySet()
        hudEventPending = false
        hudEventText = ""
        hudEventTierUpIds = emptySet()
    }
}
