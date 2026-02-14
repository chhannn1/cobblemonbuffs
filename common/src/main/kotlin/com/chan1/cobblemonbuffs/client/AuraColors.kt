package com.chan1.cobblemonbuffs.client

import org.joml.Vector3f

object AuraColors {
    val TYPE_COLORS: Map<String, Int> = mapOf(
        "fire" to 0xFFEE8130.toInt(),
        "water" to 0xFF6390F0.toInt(),
        "grass" to 0xFF7AC74C.toInt(),
        "electric" to 0xFFF7D02C.toInt(),
        "ground" to 0xFFE2BF65.toInt(),
        "rock" to 0xFFB6A136.toInt(),
        "flying" to 0xFFA98FF3.toInt(),
        "dark" to 0xFF705746.toInt(),
        "ice" to 0xFF96D9D6.toInt(),
        "ghost" to 0xFF735797.toInt(),
        "fighting" to 0xFFC22E28.toInt(),
        "steel" to 0xFFB7B7CE.toInt(),
        "fairy" to 0xFFD685AD.toInt(),
        "psychic" to 0xFFF95587.toInt(),
        "bug" to 0xFFA6B91A.toInt(),
        "dragon" to 0xFF6F35FC.toInt(),
        "normal" to 0xFFA8A77A.toInt(),
        "poison" to 0xFFA33EA1.toInt()
    )

    const val LOST_AURA_COLOR = 0xFF888888.toInt()


    fun toVector3f(argb: Int): Vector3f {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        return Vector3f(r, g, b)
    }
}
