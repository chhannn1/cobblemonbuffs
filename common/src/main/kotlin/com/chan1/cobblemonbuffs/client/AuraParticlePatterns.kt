package com.chan1.cobblemonbuffs.client

import kotlin.math.cos
import kotlin.math.sin

// for now this is a placeholder
object AuraParticlePatterns {

    private const val TWO_PI = (Math.PI * 2).toFloat()

    fun burstPositions(
        particleCount: Int = 12,
        radius: Double = 1.2
    ): List<Triple<Double, Double, Double>> {
        return (0 until particleCount).map { i ->
            val angle = TWO_PI * i / particleCount
            val y = 0.4 + (i % 3) * 0.4
            Triple(cos(angle) * radius, y, sin(angle) * radius)
        }
    }
}
