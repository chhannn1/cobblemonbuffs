package com.chan1.cobblemonbuffs.client.render.screen.components

import com.chan1.cobblemonbuffs.buff.EffectTarget
import com.chan1.cobblemonbuffs.buff.TierEffects
import com.chan1.cobblemonbuffs.buff.TypeBuff
import com.chan1.cobblemonbuffs.client.render.screen.*
import com.chan1.cobblemonbuffs.client.render.screen.helpers.fillCobblemonFrame
import com.chan1.cobblemonbuffs.client.render.screen.helpers.fillRoundedRect
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component

data class PopupBounds(
    val x: Int, val y: Int, val w: Int, val h: Int,
    val closeX: Int, val closeY: Int
)

internal object TypeInfoPopupRenderer {

    fun computeBounds(font: Font, screenW: Int, screenH: Int, typeBuff: TypeBuff, t1: Int, t2: Int, t3: Int): PopupBounds {
        val lh = font.lineHeight
        val cw = POPUP_WIDTH - POPUP_PADDING * 2
        var h = POPUP_PADDING
        h += lh + POPUP_LINE_GAP
        h += 1 + POPUP_SECTION_GAP
        h += lh + POPUP_LINE_GAP

        val desc = buffDescription(typeBuff)
        if (desc != null) {
            val descLines = font.split(Component.literal(desc), cw)
            h += descLines.size * (lh + 1)
        }
        h += POPUP_SECTION_GAP

        val t1TriggerDesc = typeBuff.t1.triggerDescription
        var firstTier = true
        for ((tierNum, tier) in tiers(typeBuff)) {
            if (tier == null) continue
            if (!firstTier) h += POPUP_TIER_CARD_GAP
            firstTier = false
            h += tierCardHeight(tierNum, tier, t1TriggerDesc, font)
        }
        h += POPUP_PADDING

        val w = POPUP_WIDTH
        val x = (screenW - w) / 2
        val y = (screenH - h) / 2
        val closeX = x + w - POPUP_PADDING - CLOSE_SIZE
        val closeY = y + POPUP_PADDING + (lh - CLOSE_SIZE) / 2
        return PopupBounds(x, y, w, h, closeX, closeY)
    }

    fun render(
        g: GuiGraphics, font: Font, bounds: PopupBounds,
        screenW: Int, screenH: Int,
        typeName: String, typeBuff: TypeBuff,
        t1: Int, t2: Int, t3: Int,
        mouseX: Int, mouseY: Int
    ) {
        val lh = font.lineHeight

        g.pose().pushPose()
        g.pose().translate(0f, 0f, 400f)

        g.fill(0, 0, screenW, screenH, POPUP_OVERLAY_BG)

        fillCobblemonFrame(g, bounds.x, bounds.y, bounds.w, bounds.h, PANEL_OUTER, PANEL_BORDER, POPUP_BG, 4)

        val cx = bounds.x + POPUP_PADDING
        val cw = bounds.w - POPUP_PADDING * 2
        var y = bounds.y + POPUP_PADDING

        val headerStr = "${typeName.replaceFirstChar { it.uppercase() }} Aura"
        g.drawString(font, headerStr, cx, y, TITLE_COLOR, true)

        val hoveringClose = mouseX in bounds.closeX..(bounds.closeX + CLOSE_SIZE) &&
                mouseY in bounds.closeY..(bounds.closeY + CLOSE_SIZE)
        fillRoundedRect(g, bounds.closeX - 1, bounds.closeY - 1, CLOSE_SIZE + 2, CLOSE_SIZE + 2,
            if (hoveringClose) CLOSE_HOVER_BG else CLOSE_BG, tl = 2, tr = 2, bl = 2, br = 2)
        g.drawString(font, "x",
            bounds.closeX + (CLOSE_SIZE - font.width("x")) / 2,
            bounds.closeY + (CLOSE_SIZE - lh) / 2,
            if (hoveringClose) CLOSE_HOVER_ICON_COLOR else CLOSE_ICON_COLOR, true)
        y += lh + POPUP_LINE_GAP

        g.fill(cx, y, cx + cw, y + 1, ROW_SEPARATOR)
        y += 1 + POPUP_SECTION_GAP

        val triggerPrefix = "Trigger: "
        g.drawString(font, triggerPrefix, cx, y, POPUP_TRIGGER_COLOR, true)
        g.drawString(font, tierTriggerLabel(typeBuff.t1), cx + font.width(triggerPrefix), y, DIM_COLOR, true)
        y += lh + POPUP_LINE_GAP

        val desc = buffDescription(typeBuff)
        if (desc != null) {
            val descLines = font.split(Component.literal(desc), cw)
            for (line in descLines) {
                g.drawString(font, line, cx, y, POPUP_DESC_COLOR, true)
                y += lh + 1
            }
        }
        y += POPUP_SECTION_GAP

        val t1TriggerDesc = typeBuff.t1.triggerDescription

        var firstTier = true
        for ((tierNum, tier) in tiers(typeBuff)) {
            if (tier == null) continue
            if (!firstTier) y += POPUP_TIER_CARD_GAP
            firstTier = false

            val tierColor = when (tierNum) { 3 -> TIER_T3_COLOR; 2 -> TIER_T2_COLOR; else -> TIER_T1_COLOR }
            val cardH = tierCardHeight(tierNum, tier, t1TriggerDesc, font)

            fillCobblemonFrame(g, cx, y, cw, cardH, POPUP_TIER_CARD_OUTER, POPUP_TIER_CARD_BORDER, POPUP_TIER_CARD_BG, 2)

            val accentInset = 3
            fillRoundedRect(g, cx + 2, y + 2 + accentInset, POPUP_TIER_ACCENT_W, cardH - 4 - accentInset * 2,
                tierColor, tl = 1, tr = 1, bl = 1, br = 1)

            val contentX = cx + 2 + POPUP_TIER_ACCENT_W + POPUP_TIER_CARD_PAD
            val contentW = cw - 4 - POPUP_TIER_ACCENT_W - POPUP_TIER_CARD_PAD * 2
            var cy = y + 2 + POPUP_TIER_CARD_PAD

            val tierLabel = "Tier $tierNum"
            g.drawString(font, tierLabel, contentX, cy, tierColor, true)
            if (tierNum >= 2) {
                val thresh = when (tierNum) { 3 -> t3; else -> t2 }
                g.drawString(font, " ($thresh+)", contentX + font.width(tierLabel), cy, POPUP_THRESH_COLOR, true)
            }
            cy += lh + POPUP_LINE_GAP

            if (tierNum > 1 && tier.triggerDescription != null && tier.triggerDescription != t1TriggerDesc) {
                g.drawString(font, tier.triggerDescription, contentX, cy, POPUP_THRESH_COLOR, true)
                cy += lh + 1
            }

            if (tier.displayLabel != null) {
                g.drawString(font, tier.displayLabel, contentX, cy, EFFECT_COLOR, true)
                cy += lh + 1
            } else {
                for (effect in tier.effects) {
                    val name = effectLabel(effect)
                    val dur = ticksToSec(effect.durationTicks)
                    g.drawString(font, name, contentX, cy, EFFECT_COLOR, true)
                    g.drawString(font, dur, contentX + contentW - font.width(dur), cy, POPUP_DURATION_COLOR, true)
                    cy += lh + 1
                }
            }

            if (tier.target == EffectTarget.OTHER) {
                g.drawString(font, "Applies to attacker", contentX, cy, POPUP_TARGET_COLOR, true)
                cy += lh + 1
            }

            if (tier.cooldownTicks > 0) {
                g.drawString(font, "Cooldown: ${ticksToSec(tier.cooldownTicks)}", contentX, cy, DIM_COLOR, true)
            }

            y += cardH
        }

        g.pose().popPose()
    }

    private fun tiers(buff: TypeBuff): List<Pair<Int, TierEffects?>> =
        listOf(1 to buff.t1, 2 to buff.t2, 3 to buff.t3)

    private fun tierCardHeight(tierNum: Int, tier: TierEffects, t1TriggerDesc: String?, font: Font): Int {
        val lh = font.lineHeight
        var h = POPUP_TIER_CARD_PAD
        h += lh + POPUP_LINE_GAP
        if (tierNum > 1 && tier.triggerDescription != null && tier.triggerDescription != t1TriggerDesc) {
            h += lh + 1
        }
        h += tierContentHeight(tier, font)
        if (tier.cooldownTicks > 0) h += lh + 1
        h += POPUP_TIER_CARD_PAD
        return h + POPUP_TIER_CARD_FRAME_OVERHEAD
    }

    private fun tierContentHeight(tier: TierEffects, font: Font): Int {
        val lh = font.lineHeight
        var h = 0
        h += if (tier.displayLabel != null) lh + 1 else tier.effects.size * (lh + 1)
        if (tier.target == EffectTarget.OTHER) h += lh + 1
        return h
    }

    private fun ticksToSec(ticks: Int): String {
        val s = ticks / 20.0
        return if (s % 1.0 == 0.0) "${s.toInt()}s" else "${"%.1f".format(s)}s"
    }
}
