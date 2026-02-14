package com.chan1.cobblemonbuffs.buff

object BuffRegistry {
    // immutable reference swap
    @Volatile private var typeBuffs: Map<String, TypeBuff> = emptyMap()
    @Volatile var dirty = false
        private set

    fun init() {
        typeBuffs = emptyMap()
    }

    fun reload(types: Map<String, TypeBuff>) {
        typeBuffs = types.toMap()
        dirty = true
    }

    fun clearDirty() { dirty = false }

    fun getForType(name: String): TypeBuff? = typeBuffs[name.lowercase()]

    fun getAll(): Collection<TypeBuff> = typeBuffs.values


    fun getTriggeredEffects(
        typeName: String,
        tier: AuraTier,
        triggerType: TriggerType
    ): TriggeredTierEffect? {
        val buff = getForType(typeName) ?: return null

        if (tier >= AuraTier.T3) {
            val t3 = buff.t3
            if (t3 != null && t3.triggerType == triggerType) {
                return TriggeredTierEffect(AuraTier.T3, t3)
            }
        }
        if (tier >= AuraTier.T2) {
            val t2 = buff.t2
            if (t2 != null && t2.triggerType == triggerType) {
                return TriggeredTierEffect(AuraTier.T2, t2)
            }
        }
        if (tier >= AuraTier.T1 && buff.t1.triggerType == triggerType) {
            return TriggeredTierEffect(AuraTier.T1, buff.t1)
        }

        return null
    }
}
