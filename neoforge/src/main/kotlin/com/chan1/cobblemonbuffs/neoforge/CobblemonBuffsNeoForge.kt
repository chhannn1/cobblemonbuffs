package com.chan1.cobblemonbuffs.neoforge

import com.chan1.cobblemonbuffs.CobblemonBuffs
import net.neoforged.fml.common.Mod
import net.neoforged.fml.loading.FMLPaths

@Mod(CobblemonBuffs.MOD_ID)
class CobblemonBuffsNeoForge {
    init {
        CobblemonBuffs.init(FMLPaths.CONFIGDIR.get())
    }
}
