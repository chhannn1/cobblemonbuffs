package com.chan1.cobblemonbuffs.client

import com.chan1.cobblemonbuffs.config.CobblemonBuffsConfig
import dev.architectury.event.events.client.ClientTickEvent
import net.minecraft.client.Minecraft
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.world.level.Level


object AuraParticleSystem {

    private var initialized = false

    fun init() {
        if (initialized) return
        initialized = true
        ClientTickEvent.CLIENT_POST.register { onClientTick() }
    }

    private fun onClientTick() {
        if (!CobblemonBuffsConfig.data.enableAuraParticles) {
            if (ClientAuraState.gainedAuras.isNotEmpty() || ClientAuraState.lostAuraIds.isNotEmpty()) {
                ClientAuraState.acknowledgeChanges()
            }
            return
        }

        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val level = mc.level ?: return

        val gained = ClientAuraState.gainedAuras
        val lost = ClientAuraState.lostAuraIds

        if (gained.isEmpty() && lost.isEmpty()) return

        val px = player.x
        val py = player.y
        val pz = player.z

        for (aura in gained) {
            val color = AuraColors.TYPE_COLORS[aura.id.lowercase()] ?: 0xFFFFFFFF.toInt()
            val positions = AuraParticlePatterns.burstPositions(particleCount = 12)
            for ((bx, by, bz) in positions) {
                spawnDust(level, px + bx, py + by, pz + bz, color, 1.2f)
            }
        }

        for (id in lost) {
            val positions = AuraParticlePatterns.burstPositions(particleCount = 8, radius = 0.8)
            for ((bx, by, bz) in positions) {
                spawnDust(level, px + bx, py + by, pz + bz, AuraColors.LOST_AURA_COLOR, 0.8f)
            }
        }

        ClientAuraState.acknowledgeChanges()
    }

    private fun spawnDust(level: Level, x: Double, y: Double, z: Double, argbColor: Int, size: Float) {
        val rgb = AuraColors.toVector3f(argbColor)
        val particle = DustParticleOptions(rgb, size)
        level.addParticle(particle, x, y, z, 0.0, 0.0, 0.0)
    }
}
