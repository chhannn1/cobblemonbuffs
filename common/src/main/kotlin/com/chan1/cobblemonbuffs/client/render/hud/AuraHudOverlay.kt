package com.chan1.cobblemonbuffs.client.render.hud

import com.chan1.cobblemonbuffs.buff.ActiveAura
import com.chan1.cobblemonbuffs.utils.render.AnimationUtil
import com.chan1.cobblemonbuffs.client.render.screen.AuraDetailScreen
import com.chan1.cobblemonbuffs.client.ClientAuraState
import com.chan1.cobblemonbuffs.client.render.hud.components.BannerRenderer
import com.chan1.cobblemonbuffs.client.render.hud.components.IconRenderer
import com.chan1.cobblemonbuffs.client.render.hud.components.PanelRenderer
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.ChatScreen
import org.lwjgl.glfw.GLFW

object AuraHudOverlay {

    private enum class HudState {
        HIDDEN,
        SLIDING_IN,
        BANNER_IN,
        VISIBLE,
        BANNER_OUT,
        PAUSE,
        SLIDING_OUT,
        IDLE
    }

    private var state = HudState.HIDDEN
    private var stateStartTime = 0L
    private var notificationText = ""

    private var wasChatOpen = false
    private var showBannerAfterSlideIn = false

    private val tierUpAnimStartTimes = mutableMapOf<String, Long>()
    private val pendingTierUpAnims = mutableSetOf<String>()

    private val stableBaseOrder = mutableListOf<String>()

    private var cachedAuraIds: List<String>? = null
    private var cachedStabilizedResult: List<ActiveAura> = emptyList()

    private var wasMouseDown = false
    private var lastPanelX = 0
    private var lastPanelTop = 0
    private var lastPanelWidth = PANEL_WIDTH
    private var lastPanelHeight = 0

    fun render(g: GuiGraphics, deltaTracker: DeltaTracker) {
        val mc = Minecraft.getInstance()
        if (mc.player == null || mc.options.hideGui) return

        if (mc.screen is AuraDetailScreen) {
            enterHidden()
            return
        }

        val auras = ClientAuraState.auras
        val isChatOpen = mc.screen is ChatScreen
        val now = System.currentTimeMillis()

        processEvents(auras.isNotEmpty(), now)
        processChatTransitions(isChatOpen, auras.isNotEmpty(), now)
        processClickDetection(mc, isChatOpen)

        if (state == HudState.HIDDEN) return
        if (auras.isEmpty()) {
            enterHidden()
            return
        }

        advanceStateMachine(isChatOpen, now)

        val elapsed = now - stateStartTime

        val baseAuras = stabilizeOrder(auras, stableBaseOrder)
        val visibleAuras = baseAuras.take(MAX_VISIBLE_AURAS)
        val overflowAuras = baseAuras.drop(MAX_VISIBLE_AURAS)
        val hasOverflow = overflowAuras.isNotEmpty()
        val displayCount = visibleAuras.size + if (hasOverflow) 1 else 0

        val panelHeight = calculatePanelHeight(displayCount)

        val offsetX = calculateOffsetX(elapsed) ?: return
        val renderAlpha = calculateAlpha(elapsed)

        val panelX = PANEL_LEFT + offsetX
        val partyTopY = mc.window.guiScaledHeight / 2 - 100
        val panelTop = partyTopY - PARTY_HUD_MARGIN - panelHeight

        lastPanelX = panelX
        lastPanelTop = panelTop
        lastPanelWidth = PANEL_WIDTH
        lastPanelHeight = panelHeight

        PanelRenderer.drawRoundedPanel(g, panelX, panelTop, PANEL_WIDTH, panelHeight, renderAlpha)

        val iconStartX = panelX + PANEL_PADDING
        val iconY = panelTop + PANEL_PADDING

        tierUpAnimStartTimes.entries.removeIf { now - it.value >= TIER_UP_ANIM_DURATION_MS }

        for ((i, aura) in visibleAuras.withIndex()) {
            val iconX = iconStartX + i * STACK_OFFSET
            IconRenderer.renderAuraIcon(g, iconX, iconY, aura, renderAlpha)
        }
        if (hasOverflow) {
            val overflowX = iconStartX + visibleAuras.size * STACK_OFFSET
            IconRenderer.renderOverflowIcon(g, overflowX, iconY, overflowAuras.size, renderAlpha)
        }

        for ((i, aura) in visibleAuras.withIndex()) {
            val iconX = iconStartX + i * STACK_OFFSET
            val tierUpProgress = tierUpAnimProgress(aura.id, now)
            if (tierUpProgress >= 0f) {
                IconRenderer.renderTierUpSprite(g, iconX, iconY, aura, renderAlpha, tierUpProgress)
                IconRenderer.renderTierUpGlow(g, iconX, iconY, aura, renderAlpha, tierUpProgress)
            }
        }
        if (hasOverflow) {
            val overflowX = iconStartX + visibleAuras.size * STACK_OFFSET
            var bestProgress = -1f
            for (aura in overflowAuras) {
                val p = tierUpAnimProgress(aura.id, now)
                if (p >= 0f && (bestProgress < 0f || p < bestProgress)) {
                    bestProgress = p
                }
            }
            if (bestProgress >= 0f) {
                IconRenderer.renderOverflowTierUpSprite(g, overflowX, iconY, renderAlpha, bestProgress)
                IconRenderer.renderOverflowTierUpGlow(g, overflowX, iconY, renderAlpha, bestProgress)
            }
        }

        val showBanner = state == HudState.BANNER_IN || state == HudState.VISIBLE || state == HudState.BANNER_OUT
        if (showBanner && notificationText.isNotEmpty()) {
            val bannerProgress = calculateBannerProgress(elapsed)
            BannerRenderer.renderBanner(
                g, mc, panelX, panelTop + panelHeight,
                PANEL_WIDTH, bannerProgress, notificationText, renderAlpha
            )
        }
    }

    private fun processEvents(hasContent: Boolean, now: Long) {
        if (!ClientAuraState.hudEventPending) return
        ClientAuraState.hudEventPending = false
        if (!hasContent) return

        val tierUps = ClientAuraState.hudEventTierUpIds

        notificationText = ClientAuraState.hudEventText
        when (state) {
            HudState.HIDDEN -> {
                pendingTierUpAnims.addAll(tierUps)
                state = HudState.SLIDING_IN
                stateStartTime = now
                showBannerAfterSlideIn = true
            }
            HudState.SLIDING_OUT -> {
                pendingTierUpAnims.addAll(tierUps)
                val outT = ((now - stateStartTime).toFloat() / SLIDE_DURATION_MS).coerceIn(0f, 1f)
                state = HudState.SLIDING_IN
                stateStartTime = now - ((1f - outT) * SLIDE_DURATION_MS).toLong()
                showBannerAfterSlideIn = true
            }
            HudState.PAUSE -> {
                for (auraId in tierUps) { tierUpAnimStartTimes[auraId] = now }
                state = HudState.BANNER_IN
                stateStartTime = now
            }
            HudState.SLIDING_IN -> {
                pendingTierUpAnims.addAll(tierUps)
            }
            HudState.BANNER_OUT, HudState.IDLE -> {
                for (auraId in tierUps) { tierUpAnimStartTimes[auraId] = now }
                state = HudState.BANNER_IN
                stateStartTime = now
            }
            else -> {
                for (auraId in tierUps) { tierUpAnimStartTimes[auraId] = now }
                state = HudState.VISIBLE
                stateStartTime = now
            }
        }
    }

    private fun processChatTransitions(isChatOpen: Boolean, hasContent: Boolean, now: Long) {
        if (isChatOpen && !wasChatOpen) {
            when (state) {
                HudState.HIDDEN -> {
                    if (hasContent) {
                        state = HudState.SLIDING_IN
                        stateStartTime = now
                        showBannerAfterSlideIn = false
                    }
                }
                HudState.SLIDING_OUT -> {
                    val outT = ((now - stateStartTime).toFloat() / SLIDE_DURATION_MS).coerceIn(0f, 1f)
                    state = HudState.SLIDING_IN
                    stateStartTime = now - ((1f - outT) * SLIDE_DURATION_MS).toLong()
                    showBannerAfterSlideIn = false
                }
                HudState.PAUSE -> {
                    state = HudState.IDLE
                    stateStartTime = now
                }
                else -> {}
            }
        }
        if (!isChatOpen && wasChatOpen) {
            if (state == HudState.IDLE) {
                state = HudState.SLIDING_OUT
                stateStartTime = now
            }
        }
        wasChatOpen = isChatOpen
    }

    private fun processClickDetection(mc: Minecraft, isChatOpen: Boolean) {
        if (isChatOpen && state != HudState.HIDDEN && state != HudState.SLIDING_IN) {
            val mouseX = (mc.mouseHandler.xpos() * mc.window.guiScaledWidth / mc.window.screenWidth).toInt()
            val mouseY = (mc.mouseHandler.ypos() * mc.window.guiScaledHeight / mc.window.screenHeight).toInt()
            val mouseDown = GLFW.glfwGetMouseButton(mc.window.window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS

            if (mouseDown && !wasMouseDown) {
                if (mouseX in lastPanelX..(lastPanelX + lastPanelWidth) &&
                    mouseY in lastPanelTop..(lastPanelTop + lastPanelHeight)
                ) {
                    mc.tell { mc.setScreen(AuraDetailScreen()) }
                }
            }
            wasMouseDown = mouseDown
        } else {
            wasMouseDown = false
        }
    }

    private fun advanceStateMachine(isChatOpen: Boolean, now: Long) {
        when (state) {
            HudState.SLIDING_IN -> {
                if (now - stateStartTime >= SLIDE_DURATION_MS) {
                    for (auraId in pendingTierUpAnims) {
                        tierUpAnimStartTimes[auraId] = now
                    }
                    pendingTierUpAnims.clear()
                    state = if (showBannerAfterSlideIn) HudState.BANNER_IN else HudState.IDLE
                    stateStartTime = now
                }
            }
            HudState.BANNER_IN -> {
                if (now - stateStartTime >= BANNER_SLIDE_DURATION_MS) {
                    state = HudState.VISIBLE
                    stateStartTime = now
                }
            }
            HudState.VISIBLE -> {
                if (now - stateStartTime >= DISPLAY_DURATION_MS) {
                    state = HudState.BANNER_OUT
                    stateStartTime = now
                }
            }
            HudState.BANNER_OUT -> {
                if (now - stateStartTime >= BANNER_SLIDE_DURATION_MS) {
                    state = if (isChatOpen) HudState.IDLE else HudState.PAUSE
                    stateStartTime = now
                }
            }
            HudState.PAUSE -> {
                if (now - stateStartTime >= BANNER_PAUSE_MS) {
                    state = HudState.SLIDING_OUT
                    stateStartTime = now
                }
            }
            HudState.SLIDING_OUT -> {
                if (now - stateStartTime >= SLIDE_DURATION_MS) {
                    enterHidden()
                }
            }
            HudState.IDLE -> {
                if (!isChatOpen) {
                    state = HudState.SLIDING_OUT
                    stateStartTime = now
                }
            }
            HudState.HIDDEN -> {}
        }
    }

    private fun enterHidden() {
        state = HudState.HIDDEN
        wasChatOpen = false
        wasMouseDown = false
        tierUpAnimStartTimes.clear()
        pendingTierUpAnims.clear()
        cachedAuraIds = null
        cachedStabilizedResult = emptyList()
    }

    private fun calculateOffsetX(elapsed: Long): Int? {
        return when (state) {
            HudState.SLIDING_IN -> {
                val t = (elapsed.toFloat() / SLIDE_DURATION_MS).coerceIn(0f, 1f)
                AnimationUtil.lerp(-PANEL_WIDTH.toFloat(), 0f, AnimationUtil.easeOutCubic(t)).toInt()
            }
            HudState.SLIDING_OUT -> {
                val t = (elapsed.toFloat() / SLIDE_DURATION_MS).coerceIn(0f, 1f)
                AnimationUtil.lerp(0f, -PANEL_WIDTH.toFloat(), AnimationUtil.easeInCubic(t)).toInt()
            }
            HudState.HIDDEN -> null
            else -> 0
        }
    }

    private fun calculateAlpha(elapsed: Long): Float {
        return when (state) {
            HudState.SLIDING_IN -> {
                val t = (elapsed.toFloat() / SLIDE_DURATION_MS).coerceIn(0f, 1f)
                AnimationUtil.easeOutCubic(t)
            }
            HudState.SLIDING_OUT -> {
                val t = (elapsed.toFloat() / SLIDE_DURATION_MS).coerceIn(0f, 1f)
                1f - AnimationUtil.easeInCubic(t)
            }
            else -> 1f
        }
    }

    private fun calculateBannerProgress(elapsed: Long): Float {
        return when (state) {
            HudState.BANNER_IN -> {
                val t = (elapsed.toFloat() / BANNER_SLIDE_DURATION_MS).coerceIn(0f, 1f)
                AnimationUtil.easeOutCubic(t)
            }
            HudState.VISIBLE -> 1f
            HudState.BANNER_OUT -> {
                val t = (elapsed.toFloat() / BANNER_SLIDE_DURATION_MS).coerceIn(0f, 1f)
                1f - AnimationUtil.easeInCubic(t)
            }
            else -> 0f
        }
    }

    private fun tierUpAnimProgress(auraId: String, now: Long): Float {
        val startTime = tierUpAnimStartTimes[auraId] ?: return -1f
        val elapsed = now - startTime
        if (elapsed >= TIER_UP_ANIM_DURATION_MS) return -1f
        return (elapsed.toFloat() / TIER_UP_ANIM_DURATION_MS).coerceIn(0f, 1f)
    }

    private fun calculatePanelHeight(displayCount: Int): Int =
        ICON_SIZE + PANEL_PADDING * 2

    private fun stabilizeOrder(auras: List<ActiveAura>, order: MutableList<String>): List<ActiveAura> {
        val currentIds = auras.map { it.id }
        if (currentIds == cachedAuraIds) return cachedStabilizedResult

        val currentIdSet = currentIds.toSet()
        order.removeAll { it !in currentIdSet }
        for (aura in auras) {
            if (aura.id !in order) order.add(aura.id)
        }
        val positionOf = order.withIndex().associate { (i, id) -> id to i }
        val result = auras.sortedBy { positionOf[it.id] ?: Int.MAX_VALUE }

        cachedAuraIds = currentIds
        cachedStabilizedResult = result
        return result
    }
}
