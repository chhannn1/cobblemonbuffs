package com.chan1.cobblemonbuffs.client.render.screen.components

import com.chan1.cobblemonbuffs.buff.ActiveAura
import com.chan1.cobblemonbuffs.buff.BuffRegistry
import com.chan1.cobblemonbuffs.buff.PartySlotInfo
import com.chan1.cobblemonbuffs.client.AuraColors
import com.chan1.cobblemonbuffs.client.render.screen.*
import com.chan1.cobblemonbuffs.client.ClientConfigState
import com.chan1.cobblemonbuffs.client.render.screen.helpers.fillCobblemonFrame
import com.chan1.cobblemonbuffs.client.render.screen.helpers.fillRoundedRect
import com.chan1.cobblemonbuffs.utils.render.TypeSpriteSheet
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics

data class InfoButtonHitArea(val auraId: String, val x: Int, val y: Int, val size: Int)

internal object ContentRenderer {

    fun measureMainContent(slot: PartySlotInfo?, auras: List<ActiveAura>): Int {
        if (slot == null) return ROW_HEIGHT
        var h = POKEMON_HEADER_H + SECTION_GAP
        h += if (auras.isNotEmpty()) {
            ROW_HEIGHT + auras.size * (AURA_CARD_HEIGHT + CARD_GAP)
        } else {
            ROW_HEIGHT
        }
        return h
    }

    fun renderMainContent(
        g: GuiGraphics, font: Font, x: Int, startY: Int,
        slot: PartySlotInfo?, auras: List<ActiveAura>,
        t2: Int, t3: Int,
        infoButtons: MutableList<InfoButtonHitArea>,
        mouseX: Int, mouseY: Int
    ): Int {
        var y = startY

        if (slot == null) {
            g.drawString(font, "Select a Pokemon", x, y + (ROW_HEIGHT - font.lineHeight) / 2, DIM_COLOR, true)
            return y + ROW_HEIGHT
        }

        y = renderPokemonHeader(g, font, x, y, slot)
        y += SECTION_GAP

        val w = CONTENT_WIDTH
        if (auras.isNotEmpty()) {
            g.fill(x, y + 3, x + 2, y + ROW_HEIGHT - 3, SECTION_COLOR)
            g.drawString(font, "Provided Auras", x + 5, y + (ROW_HEIGHT - font.lineHeight) / 2, SECTION_COLOR, true)
            y += ROW_HEIGHT

            for (aura in auras) {
                renderAuraCard(g, font, aura, x, y, w, t2, t3, infoButtons, mouseX, mouseY)
                y += AURA_CARD_HEIGHT + CARD_GAP
            }
        } else {
            g.drawString(font, "No auras from this Pokemon", x + 4, y + (ROW_HEIGHT - font.lineHeight) / 2, DIM_COLOR, true)
            y += ROW_HEIGHT
        }

        return y
    }

    private fun renderPokemonHeader(g: GuiGraphics, font: Font, x: Int, y: Int, slot: PartySlotInfo): Int {
        val w = CONTENT_WIDTH
        val h = POKEMON_HEADER_H
        val primaryColor = AuraColors.TYPE_COLORS[slot.primaryType] ?: FALLBACK_TYPE_COLOR
        val cr = 3

        fillCobblemonFrame(g, x, y, w, h, PANEL_OUTER, CARD_BORDER, CARD_BG, cr)

        g.fill(x + 3, y + cr + 2, x + 5, y + h - cr - 2, primaryColor)

        val iy = y + (h - ICON_RENDER_SIZE) / 2
        var ix = x + 8

        ix = renderTypeIcon(g, slot.primaryType, ix, iy)
        if (slot.secondaryType.isNotEmpty()) {
            ix = renderTypeIcon(g, slot.secondaryType, ix + 2, iy)
        }

        val textY = y + (h - font.lineHeight) / 2
        g.drawString(font, slot.speciesName, ix + 4, textY, TITLE_COLOR, true)

        val lvlStr = "Lv. ${slot.level}"
        g.drawString(font, lvlStr, x + w - 6 - font.width(lvlStr), textY, LABEL_COLOR, true)

        return y + h
    }

    private fun renderTypeIcon(g: GuiGraphics, typeName: String, ix: Int, iy: Int): Int {
        val index = TypeSpriteSheet.TYPE_ICON_INDEX[typeName.lowercase()] ?: return ix
        val u = index * TypeSpriteSheet.ICON_SRC_SIZE
        val texWidth = TypeSpriteSheet.TYPE_COUNT * TypeSpriteSheet.ICON_SRC_SIZE

        RenderSystem.enableBlend()
        g.pose().pushPose()
        val scale = ICON_RENDER_SIZE.toFloat() / TypeSpriteSheet.ICON_SRC_SIZE.toFloat()
        g.pose().translate(ix.toFloat(), iy.toFloat(), 0f)
        g.pose().scale(scale, scale, 1f)
        g.blit(TypeSpriteSheet.TEXTURE, 0, 0, u.toFloat(), 0f, TypeSpriteSheet.ICON_SRC_SIZE, TypeSpriteSheet.ICON_SRC_SIZE, texWidth, TypeSpriteSheet.ICON_SRC_SIZE)
        g.pose().popPose()
        RenderSystem.disableBlend()

        return ix + ICON_RENDER_SIZE
    }

    private fun renderAuraCard(
        g: GuiGraphics, font: Font, aura: ActiveAura, x: Int, y: Int, w: Int,
        t2: Int, t3: Int,
        infoButtons: MutableList<InfoButtonHitArea>,
        mouseX: Int, mouseY: Int
    ) {
        val typeColor = AuraColors.TYPE_COLORS[aura.id.lowercase()] ?: FALLBACK_AURA_COLOR
        val typeBuff = BuffRegistry.getForType(aura.id)
        val cr = 3

        fillCobblemonFrame(g, x, y, w, AURA_CARD_HEIGHT, PANEL_OUTER, CARD_BORDER, CARD_BG, cr)

        g.fill(x + 3, y + cr + 2, x + 5, y + AURA_CARD_HEIGHT - cr - 2, typeColor)

        val cx = x + 8
        val cw = w - 16
        var iy = y + 3

        g.drawString(font, aura.displayName, cx, iy, TITLE_COLOR, true)

        val btnX = cx + font.width(aura.displayName) + INFO_BTN_GAP
        val btnY = iy - (INFO_BTN_SIZE - font.lineHeight) / 2
        val hoveringInfo = mouseX in btnX..(btnX + INFO_BTN_SIZE) && mouseY in btnY..(btnY + INFO_BTN_SIZE)
        g.pose().pushPose()
        g.pose().translate(0f, -0.5f, 0f)
        fillRoundedRect(g, btnX, btnY, INFO_BTN_SIZE, INFO_BTN_SIZE,
            if (hoveringInfo) INFO_BTN_HOVER_BG else INFO_BTN_BG, tl = 3, tr = 3, bl = 3, br = 3)
        val qStr = "?"
        g.drawString(font, qStr,
            btnX + (INFO_BTN_SIZE - font.width(qStr)) / 2,
            btnY + (INFO_BTN_SIZE - font.lineHeight) / 2 + 1,
            INFO_BTN_TEXT_COLOR, true)
        g.pose().popPose()
        infoButtons.add(InfoButtonHitArea(aura.id, btnX, btnY, INFO_BTN_SIZE))

        val tierText = "Tier ${aura.tier}"
        val tierColor = when (aura.tier) { 3 -> TIER_T3_COLOR; 2 -> TIER_T2_COLOR; else -> TIER_T1_COLOR }
        g.drawString(font, tierText, cx + cw - font.width(tierText), iy, tierColor, true)
        iy += font.lineHeight + 1

        g.fill(cx, iy, cx + cw, iy + 1, ROW_SEPARATOR)
        iy += 3

        if (typeBuff != null) {
            val tierEffects = currentTierEffects(typeBuff, aura.tier)
            g.drawString(font, tierEffectsLabel(tierEffects), cx, iy, EFFECT_COLOR, true)
            val trigStr = tierTriggerLabel(tierEffects)
            g.drawString(font, trigStr, cx + cw - font.width(trigStr), iy, DIM_COLOR, true)
        }
        iy += font.lineHeight + 2

        val score = aura.score
        val maxScore = (ClientConfigState.levelMaxPoints + ClientConfigState.friendshipMaxPoints).coerceAtLeast(1)
        val fillFrac = (score / maxScore.toFloat()).coerceIn(0f, 1f)
        val barColor = when (aura.tier) { 3 -> BAR_T3_COLOR; 2 -> BAR_T2_COLOR; else -> BAR_T1_COLOR }
        g.fill(cx - 1, iy - 1, cx + cw + 1, iy + BAR_HEIGHT + 1, BAR_INSET)
        g.fill(cx, iy, cx + cw, iy + BAR_HEIGHT, BAR_BG)
        val fillW = (cw * fillFrac).toInt()
        if (fillW > 0) g.fill(cx, iy, cx + fillW, iy + BAR_HEIGHT, barColor)

        if (t2 in 1..99) { val mx = cx + (cw * t2 / 100f).toInt(); g.fill(mx, iy, mx + 1, iy + BAR_HEIGHT, BAR_MARKER) }
        if (t3 in 1..99) { val mx = cx + (cw * t3 / 100f).toInt(); g.fill(mx, iy, mx + 1, iy + BAR_HEIGHT, BAR_MARKER) }
        iy += BAR_HEIGHT + 3

        val powerStr = "Power: $score"
        g.drawString(font, powerStr, cx, iy, DIM_COLOR, true)

        if (aura.tier < 3 && typeBuff != null) {
            val nextThresh = if (aura.tier < 2) t2 else t3
            val nextTierEffects = if (aura.tier < 2) typeBuff.t2 else typeBuff.t3
            if (nextTierEffects != null) {
                val nextStr = "Next ($nextThresh): ${tierEffectsLabel(nextTierEffects)}"
                val truncated = truncate(font, nextStr, cw - font.width(powerStr) - 4)
                g.drawString(font, truncated, cx + cw - font.width(truncated), iy, NEXT_COLOR, true)
            }
        } else if (aura.tier >= 3) {
            val s = "MAXED"
            g.drawString(font, s, cx + cw - font.width(s), iy, TIER_T3_COLOR, true)
        }
    }

    private fun truncate(font: Font, text: String, maxWidth: Int): String {
        if (font.width(text) <= maxWidth) return text
        var lo = 0
        var hi = text.length
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (font.width(text.substring(0, mid) + "..") <= maxWidth) lo = mid else hi = mid - 1
        }
        return text.substring(0, lo) + ".."
    }
}
