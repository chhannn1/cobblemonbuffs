package com.chan1.cobblemonbuffs.fabric.client

import com.chan1.cobblemonbuffs.client.render.hud.AuraHudOverlay
import com.chan1.cobblemonbuffs.client.AuraParticleSystem
import dev.architectury.event.events.client.ClientGuiEvent
import net.fabricmc.api.ClientModInitializer

class CobblemonBuffsFabricClient : ClientModInitializer {
    override fun onInitializeClient() {
        ClientGuiEvent.RENDER_HUD.register { guiGraphics, deltaTracker ->
            AuraHudOverlay.render(guiGraphics, deltaTracker)
        }
        AuraParticleSystem.init()
    }
}
