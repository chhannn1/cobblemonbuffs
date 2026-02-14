package com.chan1.cobblemonbuffs.client.render.hud.components

import com.chan1.cobblemonbuffs.buff.ActiveAura
import com.chan1.cobblemonbuffs.utils.render.AnimationUtil
import com.chan1.cobblemonbuffs.utils.render.TypeSpriteSheet
import com.chan1.cobblemonbuffs.client.render.hud.*
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics

internal object IconRenderer {

    fun renderAuraIcon(g: GuiGraphics, x: Int, y: Int, aura: ActiveAura, alpha: Float) {
        renderTypeSprite(g, x, y, aura.id.lowercase(), alpha)
    }

    fun renderTierUpSprite(g: GuiGraphics, x: Int, y: Int, aura: ActiveAura, alpha: Float, progress: Float) {
        renderScaledPulse(g, x, y, alpha, progress) { effectiveAlpha ->
            renderTypeSprite(g, x, y, aura.id.lowercase(), effectiveAlpha, additive = true)
        }
    }

    fun renderTierUpGlow(g: GuiGraphics, x: Int, y: Int, aura: ActiveAura, alpha: Float, progress: Float) {
        renderGlowEffect(g, x, y, alpha, progress) { glowAlpha ->
            renderTypeSprite(g, x, y, aura.id.lowercase(), glowAlpha, additive = true, whiteTint = true)
        }
    }

    fun renderOverflowIcon(g: GuiGraphics, x: Int, y: Int, overflowCount: Int, alpha: Float) {
        renderOverflowBase(g, x, y, alpha)
        drawOverflowText(g, x, y, overflowCount, alpha)
    }

    fun renderOverflowTierUpSprite(g: GuiGraphics, x: Int, y: Int, alpha: Float, progress: Float) {
        renderScaledPulse(g, x, y, alpha, progress) { effectiveAlpha ->
            renderOverflowSprite(g, x, y, effectiveAlpha, additive = true)
        }
    }

    fun renderOverflowTierUpGlow(g: GuiGraphics, x: Int, y: Int, alpha: Float, progress: Float) {
        renderGlowEffect(g, x, y, alpha, progress) { glowAlpha ->
            renderOverflowSprite(g, x, y, glowAlpha, additive = true, whiteTint = true)
        }
    }

    private inline fun withCenteredScale(g: GuiGraphics, x: Int, y: Int, scale: Float, block: () -> Unit) {
        val cx = x + ICON_SIZE / 2f
        val cy = y + ICON_SIZE / 2f
        g.pose().pushPose()
        g.pose().translate(cx, cy, 0f)
        g.pose().scale(scale, scale, 1f)
        g.pose().translate(-cx, -cy, 0f)
        block()
        g.pose().popPose()
    }

    private fun fadeAlpha(progress: Float): Float =
        if (progress > FADE_START_PROGRESS) 1f - (progress - FADE_START_PROGRESS) / (1f - FADE_START_PROGRESS) else 1f

    private inline fun renderScaledPulse(
        g: GuiGraphics, x: Int, y: Int, alpha: Float, progress: Float,
        draw: (Float) -> Unit
    ) {
        val effectiveAlpha = alpha * fadeAlpha(progress)
        if (effectiveAlpha <= 0.01f) return
        withCenteredScale(g, x, y, AnimationUtil.scalePulse(progress)) { draw(effectiveAlpha) }
    }

    private inline fun renderGlowEffect(
        g: GuiGraphics, x: Int, y: Int, alpha: Float, progress: Float,
        draw: (Float) -> Unit
    ) {
        val glowAlpha = (1f - progress) * GLOW_MAX_ALPHA * alpha
        if (glowAlpha <= 0.01f) return
        withCenteredScale(g, x, y, AnimationUtil.scalePulse(progress) * GLOW_OVERSCALE) { draw(glowAlpha) }
    }

    private fun blitScaled(g: GuiGraphics, x: Int, y: Int, u: Float, texWidth: Int) {
        val scale = ICON_SIZE.toFloat() / TypeSpriteSheet.ICON_SRC_SIZE.toFloat()
        g.pose().pushPose()
        g.pose().translate(x.toFloat(), y.toFloat(), 0f)
        g.pose().scale(scale, scale, 1f)
        g.blit(TypeSpriteSheet.TEXTURE, 0, 0, u, 0f,
            TypeSpriteSheet.ICON_SRC_SIZE, TypeSpriteSheet.ICON_SRC_SIZE,
            texWidth, TypeSpriteSheet.ICON_SRC_SIZE)
        g.pose().popPose()
    }

    private fun renderTypeSprite(g: GuiGraphics, x: Int, y: Int, typeName: String, alpha: Float, additive: Boolean = false, whiteTint: Boolean = false) {
        val index = TypeSpriteSheet.TYPE_ICON_INDEX[typeName] ?: return
        val u = (index * TypeSpriteSheet.ICON_SRC_SIZE).toFloat()
        val texWidth = TypeSpriteSheet.TYPE_COUNT * TypeSpriteSheet.ICON_SRC_SIZE
        val brightness = if (whiteTint) WHITE_TINT_BRIGHTNESS else 1f

        RenderSystem.enableBlend()
        if (additive) RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE)
        RenderSystem.setShaderColor(brightness, brightness, brightness, alpha)
        try {
            blitScaled(g, x, y, u, texWidth)
        } finally {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
            if (additive) RenderSystem.defaultBlendFunc()
            RenderSystem.disableBlend()
        }
    }

    private fun renderOverflowBase(g: GuiGraphics, x: Int, y: Int, alpha: Float) {
        val index = TypeSpriteSheet.TYPE_ICON_INDEX["normal"] ?: return
        val u = (index * TypeSpriteSheet.ICON_SRC_SIZE).toFloat()
        val texWidth = TypeSpriteSheet.TYPE_COUNT * TypeSpriteSheet.ICON_SRC_SIZE

        RenderSystem.enableBlend()
        try {
            RenderSystem.setShaderColor(OVERFLOW_BASE_DARKEN, OVERFLOW_BASE_DARKEN, OVERFLOW_BASE_DARKEN, alpha)
            blitScaled(g, x, y, u, texWidth)

            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE)
            RenderSystem.setShaderColor(OVERFLOW_TINT_BRIGHTNESS, OVERFLOW_TINT_BRIGHTNESS, OVERFLOW_TINT_BRIGHTNESS, alpha * OVERFLOW_EDGE_ALPHA)
            blitScaled(g, x, y, u, texWidth)
        } finally {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
            RenderSystem.defaultBlendFunc()
            RenderSystem.disableBlend()
        }
    }

    private fun renderOverflowSprite(g: GuiGraphics, x: Int, y: Int, alpha: Float, additive: Boolean = false, whiteTint: Boolean = false) {
        val index = TypeSpriteSheet.TYPE_ICON_INDEX["normal"] ?: return
        val u = (index * TypeSpriteSheet.ICON_SRC_SIZE).toFloat()
        val texWidth = TypeSpriteSheet.TYPE_COUNT * TypeSpriteSheet.ICON_SRC_SIZE

        RenderSystem.enableBlend()
        if (additive) RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE)
        if (whiteTint) {
            RenderSystem.setShaderColor(WHITE_TINT_BRIGHTNESS, WHITE_TINT_BRIGHTNESS, WHITE_TINT_BRIGHTNESS, alpha)
        } else {
            RenderSystem.setShaderColor(OVERFLOW_TINT_BRIGHTNESS, OVERFLOW_TINT_BRIGHTNESS, OVERFLOW_TINT_BRIGHTNESS, alpha * OVERFLOW_SPRITE_ALPHA)
        }
        try {
            blitScaled(g, x, y, u, texWidth)
        } finally {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
            if (additive) RenderSystem.defaultBlendFunc()
            RenderSystem.disableBlend()
        }
    }

    private fun drawOverflowText(g: GuiGraphics, x: Int, y: Int, count: Int, alpha: Float) {
        val mc = Minecraft.getInstance()
        val font = mc.font
        val text = "+$count"

        withCenteredScale(g, x, y, OVERFLOW_TEXT_SCALE) {
            val textX = x + (ICON_SIZE - font.width(text)) / 2
            val textY = y + (ICON_SIZE - font.lineHeight) / 2
            g.drawString(font, text, textX, textY, withAlpha(0xFFFFFFFF.toInt(), alpha), true)
        }
    }
}
