package com.chan1.cobblemonbuffs.client.render.hud

// globals for extendibility/future use

internal const val ICON_SIZE = 16
internal const val ICON_SPACING = 3
internal const val PANEL_LEFT = 0
internal const val PARTY_HUD_MARGIN = 21
internal const val PANEL_WIDTH = 62
internal const val PANEL_PADDING = 6
internal const val SECTION_GAP = 4
internal const val STACK_OFFSET = 6
internal const val MAX_VISIBLE_AURAS = 6
internal const val OUTER_BORDER = 0xFF383838.toInt()
internal const val MID_BORDER = 0xFF9A9A9A.toInt()
internal const val INNER_FILL = 0xFF7A7A7A.toInt()
internal const val TOPBAR_BORDER = 0xFFE0E0E0.toInt()
internal const val TOPBAR_SHINE = 0xFFB8B8B8.toInt()
internal const val TOPBAR_HEIGHT = 4

internal const val BANNER_BG = 0xFF2A2A2A.toInt()
internal const val BANNER_BORDER = 0xFF4A4A4A.toInt()
internal const val BANNER_PADDING_V = 1
internal const val BANNER_INSET_L = 1
internal const val BANNER_INSET_R = 5
internal const val BANNER_TEXT_SCALE = 0.7f

internal const val SLIDE_DURATION_MS = 300L
internal const val BANNER_SLIDE_DURATION_MS = 200L
internal const val BANNER_PAUSE_MS = 150L
internal const val DISPLAY_DURATION_MS = 5000L
internal const val TIER_UP_ANIM_DURATION_MS = 500L

internal const val FADE_START_PROGRESS = 0.7f
internal const val GLOW_MAX_ALPHA = 0.35f
internal const val GLOW_OVERSCALE = 1.15f
internal const val WHITE_TINT_BRIGHTNESS = 10f
internal const val OVERFLOW_TINT_BRIGHTNESS = 5f
internal const val OVERFLOW_BASE_DARKEN = 0.05f
internal const val OVERFLOW_EDGE_ALPHA = 0.12f
internal const val OVERFLOW_SPRITE_ALPHA = 0.15f
internal const val OVERFLOW_TEXT_SCALE = 0.65f
internal const val BANNER_BOTTOM_ROUNDING = 2

internal fun withAlpha(color: Int, alpha: Float): Int {
    if (alpha >= 1f) return color
    val a = ((color ushr 24 and 0xFF) * alpha).toInt().coerceIn(0, 255)
    return (a shl 24) or (color and 0x00FFFFFF)
}
