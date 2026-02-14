package com.chan1.cobblemonbuffs.client.render.screen.helpers

import net.minecraft.client.gui.GuiGraphics

internal fun fillRoundedRect(
    g: GuiGraphics, x: Int, y: Int, w: Int, h: Int, color: Int,
    tl: Int = 0, tr: Int = 0, bl: Int = 0, br: Int = 0
) {
    val topRows = maxOf(tl, tr)
    val bottomRows = maxOf(bl, br)
    for (row in 0 until topRows.coerceAtMost(h)) {
        val li = if (row < tl) cornerInset(tl, row) else 0
        val ri = if (row < tr) cornerInset(tr, row) else 0
        g.fill(x + li, y + row, x + w - ri, y + row + 1, color)
    }
    val bodyTop = topRows.coerceAtMost(h)
    val bodyBottom = (h - bottomRows).coerceAtLeast(bodyTop)
    if (bodyTop < bodyBottom) g.fill(x, y + bodyTop, x + w, y + bodyBottom, color)
    for (row in bodyBottom until h) {
        val li = if (row >= h - bl) cornerInset(bl, h - 1 - row) else 0
        val ri = if (row >= h - br) cornerInset(br, h - 1 - row) else 0
        g.fill(x + li, y + row, x + w - ri, y + row + 1, color)
    }
}

internal fun fillCobblemonFrame(
    g: GuiGraphics, x: Int, y: Int, w: Int, h: Int,
    outer: Int, border: Int, fill: Int, cornerRadius: Int
) {
    val cr = cornerRadius
    fillRoundedRect(g, x, y, w, h, outer, tl = cr + 2, tr = cr + 2, bl = cr + 2, br = cr + 2)
    fillRoundedRect(g, x + 1, y + 1, w - 2, h - 2, border, tl = cr + 1, tr = cr + 1, bl = cr + 1, br = cr + 1)
    fillRoundedRect(g, x + 2, y + 2, w - 4, h - 4, fill, tl = cr, tr = cr, bl = cr, br = cr)
}

private fun cornerInset(radius: Int, dist: Int): Int {
    if (radius <= 0 || dist >= radius) return 0
    return when (radius) {
        1 -> 1
        2 -> if (dist == 0) 2 else 1
        3 -> if (dist == 0) 3 else 1
        else -> when (dist) { 0 -> 4; 1 -> 2; else -> 1 }
    }
}
