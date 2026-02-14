package com.chan1.cobblemonbuffs.fabric

import com.chan1.cobblemonbuffs.CobblemonBuffs
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader

class CobblemonBuffsFabric : ModInitializer {
    override fun onInitialize() {
        CobblemonBuffs.init(FabricLoader.getInstance().configDir)
    }
}
