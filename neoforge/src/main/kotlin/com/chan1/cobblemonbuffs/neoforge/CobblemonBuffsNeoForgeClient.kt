package com.chan1.cobblemonbuffs.neoforge

import com.chan1.cobblemonbuffs.CobblemonBuffs
import com.chan1.cobblemonbuffs.client.render.hud.AuraHudOverlay
import com.chan1.cobblemonbuffs.client.AuraParticleSystem
import dev.architectury.event.events.client.ClientGuiEvent
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent

@EventBusSubscriber(modid = CobblemonBuffs.MOD_ID, value = [Dist.CLIENT], bus = EventBusSubscriber.Bus.MOD)
object CobblemonBuffsNeoForgeClient {
    @SubscribeEvent
    fun onClientSetup(event: FMLClientSetupEvent) {
        event.enqueueWork {
            ClientGuiEvent.RENDER_HUD.register { guiGraphics, deltaTracker ->
                AuraHudOverlay.render(guiGraphics, deltaTracker)
            }
            AuraParticleSystem.init()
        }
    }
}
