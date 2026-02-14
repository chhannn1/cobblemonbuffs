package com.chan1.cobblemonbuffs.client.render.hud.components

import com.chan1.cobblemonbuffs.client.render.hud.INNER_FILL
import com.chan1.cobblemonbuffs.client.render.hud.MID_BORDER
import com.chan1.cobblemonbuffs.client.render.hud.OUTER_BORDER
import com.chan1.cobblemonbuffs.client.render.hud.TOPBAR_BORDER
import com.chan1.cobblemonbuffs.client.render.hud.TOPBAR_HEIGHT
import com.chan1.cobblemonbuffs.client.render.hud.TOPBAR_SHINE
import com.chan1.cobblemonbuffs.client.render.hud.withAlpha
import net.minecraft.client.gui.GuiGraphics

internal object PanelRenderer {


    fun fillPanelShape(g: GuiGraphics, x: Int, y: Int, w: Int, h: Int, color: Int, tr: Int, br: Int) {
        // Top-right corner rows
        if (tr >= 4) {
            g.fill(x, y, x + w - 4, y + 1, color)
            g.fill(x, y + 1, x + w - 2, y + 2, color)
            g.fill(x, y + 2, x + w - 1, y + 3, color)
        } else if (tr >= 3) {
            g.fill(x, y, x + w - 3, y + 1, color)
            g.fill(x, y + 1, x + w - 1, y + 2, color)
        } else if (tr >= 2) {
            g.fill(x, y, x + w - 2, y + 1, color)
            g.fill(x, y + 1, x + w - 1, y + 2, color)
        } else if (tr >= 1) {
            g.fill(x, y, x + w - 1, y + 1, color)
        }

        val bodyTop = when {
            tr >= 4 -> y + 3
            tr >= 2 -> y + 2
            tr >= 1 -> y + 1
            else -> y
        }
        val bodyBottom = when {
            br >= 2 -> y + h - 2
            br >= 1 -> y + h - 1
            else -> y + h
        }
        if (bodyTop < bodyBottom) {
            g.fill(x, bodyTop, x + w, bodyBottom, color)
        }

        if (br >= 2) {
            g.fill(x, y + h - 2, x + w - 1, y + h - 1, color)
            g.fill(x, y + h - 1, x + w - 2, y + h, color)
        } else if (br >= 1) {
            g.fill(x, y + h - 1, x + w - 1, y + h, color)
        }
    }

    fun drawRoundedPanel(g: GuiGraphics, x: Int, y: Int, w: Int, h: Int, alpha: Float) {
        fillPanelShape(g, x, y, w, h, withAlpha(OUTER_BORDER, alpha), tr = 4, br = 2)
        fillPanelShape(g, x, y + 1, w - 1, h - 2, withAlpha(MID_BORDER, alpha), tr = 3, br = 1)
        fillPanelShape(g, x + 1, y + 2, w - 3, h - 4, withAlpha(INNER_FILL, alpha), tr = 2, br = 0)

        fillPanelShape(g, x, y + 1, w - 1, TOPBAR_HEIGHT, withAlpha(TOPBAR_BORDER, alpha), tr = 3, br = 0)
        fillPanelShape(g, x + 1, y + 2, w - 3, TOPBAR_HEIGHT - 1, withAlpha(TOPBAR_SHINE, alpha), tr = 2, br = 0)
    }
}
