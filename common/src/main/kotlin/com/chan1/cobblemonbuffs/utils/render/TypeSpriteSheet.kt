package com.chan1.cobblemonbuffs.utils.render

import net.minecraft.resources.ResourceLocation

object TypeSpriteSheet {
    val TEXTURE: ResourceLocation = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/types.png")
    const val ICON_SRC_SIZE = 36
    const val TYPE_COUNT = 18

    val TYPE_ICON_INDEX: Map<String, Int> = mapOf(
        "normal" to 0, "fire" to 1, "water" to 2, "grass" to 3,
        "electric" to 4, "ice" to 5, "fighting" to 6, "poison" to 7,
        "ground" to 8, "flying" to 9, "psychic" to 10, "bug" to 11,
        "rock" to 12, "ghost" to 13, "dragon" to 14, "dark" to 15,
        "steel" to 16, "fairy" to 17
    )
}
