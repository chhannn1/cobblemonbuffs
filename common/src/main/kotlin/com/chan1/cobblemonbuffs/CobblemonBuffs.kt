package com.chan1.cobblemonbuffs

import com.chan1.cobblemonbuffs.buff.BuffRegistry
import com.chan1.cobblemonbuffs.buff.TypeEffectReloadListener
import com.chan1.cobblemonbuffs.config.CobblemonBuffsConfig
import com.chan1.cobblemonbuffs.event.BuffEventHandler
import com.chan1.cobblemonbuffs.network.CobblemonBuffsNetwork
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path

object CobblemonBuffs {
    const val MOD_ID = "cobblemonbuffs"
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

    fun init(configDir: Path) {
        CobblemonBuffsConfig.init(configDir)
        BuffRegistry.init()
        TypeEffectReloadListener.init(configDir)
        TypeEffectReloadListener.register()
        CobblemonBuffsNetwork.init()
        BuffEventHandler.init()
    }
}
