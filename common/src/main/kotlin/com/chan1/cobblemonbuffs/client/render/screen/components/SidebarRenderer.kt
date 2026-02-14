package com.chan1.cobblemonbuffs.client.render.screen.components

import com.chan1.cobblemonbuffs.CobblemonBuffs
import com.chan1.cobblemonbuffs.buff.PartySlotInfo
import com.chan1.cobblemonbuffs.client.render.screen.*
import com.chan1.cobblemonbuffs.client.render.screen.helpers.fillCobblemonFrame
import com.chan1.cobblemonbuffs.client.render.screen.helpers.fillRoundedRect
import com.cobblemon.mod.common.api.gui.drawPosablePortrait
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics

internal object SidebarRenderer {

    fun render(
        g: GuiGraphics, font: Font, x: Int, topY: Int,
        slots: List<PartySlotInfo?>, selectedSlot: Int,
        slotTiers: Map<Int, Int>,
        slotStates: Array<FloatingState>,
        mouseX: Int, mouseY: Int, partialTick: Float
    ) {
        val gridH = GRID_ROWS * (SLOT_H + SLOT_ROW_GAP) - SLOT_ROW_GAP
        val containerH = SIDEBAR_HEADER_H + GRID_PAD + gridH + GRID_PAD

        fillCobblemonFrame(g, x, topY, SIDEBAR_WIDTH, containerH, SIDEBAR_OUTER, SIDEBAR_MID, SIDEBAR_FILL, SIDEBAR_CONTAINER_RADIUS)

        val headerText = "Party"
        g.drawString(font, headerText, x + (SIDEBAR_WIDTH - font.width(headerText)) / 2, topY + (SIDEBAR_HEADER_H - font.lineHeight) / 2, SIDEBAR_HEADER_COL, true)
        g.fill(x + GRID_PAD, topY + SIDEBAR_HEADER_H - 1, x + SIDEBAR_WIDTH - GRID_PAD, topY + SIDEBAR_HEADER_H, PANEL_BG)

        val gridTop = topY + SIDEBAR_HEADER_H + GRID_PAD
        val gridLeft = x + GRID_PAD
        for (i in 0 until PARTY_SIZE) {
            val col = i % GRID_COLS
            val row = i / GRID_COLS
            val sx = gridLeft + col * (SLOT_W + SLOT_COL_GAP)
            val sy = gridTop + row * (SLOT_H + SLOT_ROW_GAP)

            val slot = slots.getOrNull(i)
            val isSelected = i == selectedSlot && slot != null
            val isHovering = !isSelected && slot != null && mouseX in sx..(sx + SLOT_W) && mouseY in sy..(sy + SLOT_H)

            if (slot != null) {
                val tier = slotTiers[i] ?: 1
                renderFilledSlot(g, font, sx, sy, slot, tier, isSelected, isHovering, slotStates[i], partialTick)
            } else {
                renderEmptySlot(g, sx, sy)
            }
        }
    }

    private fun renderFilledSlot(
        g: GuiGraphics, font: Font, sx: Int, sy: Int,
        slot: PartySlotInfo, tier: Int, isSelected: Boolean, isHovering: Boolean,
        state: FloatingState, partialTick: Float
    ) {
        val bg = when {
            isSelected -> SLOT_SELECTED_BG
            isHovering -> SLOT_HOVER_BG
            else -> SLOT_FILLED_BG
        }
        val border = if (isSelected) SLOT_SELECTED_BORDER else SLOT_FILLED_BORDER
        val r = SLOT_RADIUS

        fillRoundedRect(g, sx - 1, sy - 1, SLOT_W + 2, SLOT_H + 2, border, tl = r + 1, tr = r + 1, bl = r + 1, br = r + 1)
        fillRoundedRect(g, sx, sy, SLOT_W, SLOT_H, bg, tl = r, tr = r, bl = r, br = r)

        val modelClipBot = sy + SLOT_H - PORTRAIT_CLIP_INSET
        try {
            val identifier = cobblemonResource(slot.speciesName.lowercase())
            state.currentAspects = setOf()

            g.enableScissor(sx, sy + 1, sx + SLOT_W, modelClipBot)
            g.pose().pushPose()
            g.pose().translate((sx + SLOT_W / 2.0).toFloat(), (sy + PORTRAIT_Y_OFFSET).toFloat(), 0f)

            drawPosablePortrait(
                identifier = identifier,
                matrixStack = g.pose(),
                scale = PORTRAIT_SCALE,
                state = state,
                partialTicks = partialTick
            )

            g.pose().popPose()
            g.disableScissor()
        } catch (e: Exception) {
            logModelWarning(slot.speciesName, e)
        }

        val nameBarY = sy + SLOT_H - NAME_BAR_OFFSET
        g.fill(sx + 1, nameBarY, sx + SLOT_W - 1, sy + SLOT_H - 1, SLOT_NAME_BG)

        g.pose().pushPose()
        g.pose().scale(SLOT_TEXT_SCALE, SLOT_TEXT_SCALE, 1f)
        val name = slot.speciesName
        val nameW = (font.width(name) * SLOT_TEXT_SCALE).toInt()
        val nameDrawX = ((sx + (SLOT_W - nameW) / 2) / SLOT_TEXT_SCALE).toInt()
        val nameDrawY = ((nameBarY + 1) / SLOT_TEXT_SCALE).toInt()
        g.drawString(font, name, nameDrawX, nameDrawY, SLOT_TEXT_COLOR, true)
        g.pose().popPose()

        val tierStr = "T$tier"
        val tierColor = when (tier) { 3 -> TIER_T3_COLOR; 2 -> TIER_T2_COLOR; else -> TIER_T1_COLOR }
        val tierW = (font.width(tierStr) * SLOT_TEXT_SCALE).toInt()
        g.fill(sx + SLOT_W - tierW - 3, sy + 1, sx + SLOT_W - 1, sy + 8, OVERLAY_BG)
        g.pose().pushPose()
        g.pose().scale(SLOT_TEXT_SCALE, SLOT_TEXT_SCALE, 1f)
        val tierDrawX = ((sx + SLOT_W - tierW - 2) / SLOT_TEXT_SCALE).toInt()
        val tierDrawY = ((sy + 2) / SLOT_TEXT_SCALE).toInt()
        g.drawString(font, tierStr, tierDrawX, tierDrawY, tierColor, true)
        g.pose().popPose()
    }

    private val warnedSpecies = mutableSetOf<String>()
    private fun logModelWarning(speciesName: String, e: Exception) {
        if (warnedSpecies.add(speciesName)) {
            CobblemonBuffs.LOGGER.debug("Could not render model for species '{}': {}", speciesName, e.message)
        }
    }

    private fun renderEmptySlot(g: GuiGraphics, sx: Int, sy: Int) {
        val r = SLOT_RADIUS
        fillRoundedRect(g, sx - 1, sy - 1, SLOT_W + 2, SLOT_H + 2, SLOT_EMPTY_OUTER, tl = r + 1, tr = r + 1, bl = r + 1, br = r + 1)
        fillRoundedRect(g, sx, sy, SLOT_W, SLOT_H, SLOT_EMPTY_BG, tl = r, tr = r, bl = r, br = r)
        g.fill(sx + r, sy, sx + SLOT_W - r, sy + 1, SLOT_EMPTY_SHADOW)
        g.fill(sx + r, sy + SLOT_H - 1, sx + SLOT_W - r, sy + SLOT_H, SLOT_EMPTY_HIGHLIGHT)
    }
}
