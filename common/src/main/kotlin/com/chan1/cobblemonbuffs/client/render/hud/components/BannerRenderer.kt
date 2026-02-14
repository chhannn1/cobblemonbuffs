package com.chan1.cobblemonbuffs.client.render.hud.components

import com.chan1.cobblemonbuffs.client.render.hud.BANNER_BG
import com.chan1.cobblemonbuffs.client.render.hud.BANNER_BORDER
import com.chan1.cobblemonbuffs.client.render.hud.BANNER_BOTTOM_ROUNDING
import com.chan1.cobblemonbuffs.client.render.hud.BANNER_INSET_L
import com.chan1.cobblemonbuffs.client.render.hud.BANNER_INSET_R
import com.chan1.cobblemonbuffs.client.render.hud.BANNER_PADDING_V
import com.chan1.cobblemonbuffs.client.render.hud.BANNER_TEXT_SCALE
import com.chan1.cobblemonbuffs.client.render.hud.withAlpha
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics

internal object BannerRenderer {

    fun renderBanner(
        g: GuiGraphics,
        mc: Minecraft,
        panelX: Int,
        panelBottom: Int,
        panelWidth: Int,
        bannerProgress: Float,
        notificationText: String,
        alpha: Float
    ) {
        val scaledLineHeight = (mc.font.lineHeight * BANNER_TEXT_SCALE).toInt()
        val bannerHeight = scaledLineHeight + BANNER_PADDING_V * 2 + BANNER_BOTTOM_ROUNDING

        val bannerY = panelBottom - bannerHeight + (bannerHeight * bannerProgress).toInt()

        val window = mc.window
        g.enableScissor(0, panelBottom, window.guiScaledWidth, window.guiScaledHeight)

        val bx1 = panelX + BANNER_INSET_L
        val bx2 = panelX + panelWidth - BANNER_INSET_R
        val by1 = bannerY
        val by2 = bannerY + bannerHeight

        g.fill(bx1, by1, bx2, by2 - 2, withAlpha(BANNER_BORDER, alpha))
        g.fill(bx1 + 1, by2 - 2, bx2 - 1, by2 - 1, withAlpha(BANNER_BORDER, alpha))
        g.fill(bx1 + 2, by2 - 1, bx2 - 2, by2, withAlpha(BANNER_BORDER, alpha))

        g.fill(bx1 + 1, by1, bx2 - 1, by2 - 2, withAlpha(BANNER_BG, alpha))
        g.fill(bx1 + 2, by2 - 2, bx2 - 2, by2 - 1, withAlpha(BANNER_BG, alpha))

        val bannerWidth = bx2 - bx1
        val textWidth = (mc.font.width(notificationText) * BANNER_TEXT_SCALE).toInt()
        val textX = bx1 + (bannerWidth - textWidth) / 2
        val textY = bannerY + BANNER_PADDING_V

        g.pose().pushPose()
        g.pose().translate(textX.toFloat(), textY.toFloat(), 0f)
        g.pose().scale(BANNER_TEXT_SCALE, BANNER_TEXT_SCALE, 1f)
        g.drawString(mc.font, notificationText, 0, 0, withAlpha(0xFFFFFFFF.toInt(), alpha), true)
        g.pose().popPose()

        g.disableScissor()
    }
}
