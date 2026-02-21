package com.chan1.cobblemonbuffs.client.render.screen

import com.chan1.cobblemonbuffs.buff.BuffRegistry
import com.chan1.cobblemonbuffs.client.ClientAuraState
import com.chan1.cobblemonbuffs.client.render.screen.components.ContentRenderer
import com.chan1.cobblemonbuffs.client.render.screen.components.InfoButtonHitArea
import com.chan1.cobblemonbuffs.client.render.screen.components.PopupBounds
import com.chan1.cobblemonbuffs.client.render.screen.components.SidebarRenderer
import com.chan1.cobblemonbuffs.client.render.screen.components.TypeInfoPopupRenderer
import com.chan1.cobblemonbuffs.client.render.screen.helpers.fillCobblemonFrame
import com.chan1.cobblemonbuffs.client.render.screen.helpers.fillRoundedRect
import com.chan1.cobblemonbuffs.client.ClientConfigState
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class AuraDetailScreen : Screen(Component.literal("Aura Overview")) {

    private var selectedSlot = 0
    private var scrollOffset = 0f
    private var maxScroll = 0f
    private var panelLeft = 0
    private var panelTop = 0
    private var computedPanelHeight = 0
    private val slotStates = Array(PARTY_SIZE) { FloatingState() }
    private var infoPopupType: String? = null
    private val infoButtonHitAreas = mutableListOf<InfoButtonHitArea>()
    private var cachedPopupBounds: PopupBounds? = null

    override fun init() {
        super.init()
        selectedSlot = ClientAuraState.partySlots.indexOfFirst { it != null }.coerceAtLeast(0)
    }

    override fun renderBackground(g: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {}

    override fun render(g: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(g, mouseX, mouseY, partialTick)

        val partySlots = ClientAuraState.partySlots
        val auras = ClientAuraState.auras
        val t2 = ClientConfigState.t2Threshold
        val t3 = ClientConfigState.t3Threshold

        val selectedAuras = auras.filter { it.providerSlot == selectedSlot }
        val selectedSlotInfo = partySlots.getOrNull(selectedSlot)

        val contentH = ContentRenderer.measureMainContent(selectedSlotInfo, selectedAuras)
        val headerH = ROW_HEIGHT * 2
        val sidebarH = SIDEBAR_HEADER_H + GRID_PAD + GRID_ROWS * (SLOT_H + SLOT_ROW_GAP) - SLOT_ROW_GAP + GRID_PAD + 2
        val totalH = PANEL_PADDING + headerH + maxOf(contentH, sidebarH) + PANEL_PADDING
        computedPanelHeight = totalH.coerceAtMost(height - 16)

        val scrollableH = computedPanelHeight - PANEL_PADDING - headerH - PANEL_PADDING
        maxScroll = (contentH - scrollableH).coerceAtLeast(0).toFloat()
        scrollOffset = scrollOffset.coerceIn(0f, maxScroll)

        panelLeft = (width - PANEL_WIDTH) / 2
        panelTop = (height - computedPanelHeight) / 2

        fillCobblemonFrame(g, panelLeft, panelTop, PANEL_WIDTH, computedPanelHeight, PANEL_OUTER, PANEL_BORDER, PANEL_BG, SIDEBAR_CONTAINER_RADIUS)

        var y = panelTop + PANEL_PADDING

        val titleStr = title.string
        g.drawString(font, titleStr, panelLeft + (PANEL_WIDTH - font.width(titleStr)) / 2,
            y + (ROW_HEIGHT - font.lineHeight) / 2, TITLE_COLOR, true)

        val closeX = panelLeft + PANEL_WIDTH - PANEL_PADDING - CLOSE_SIZE
        val closeY = y + (ROW_HEIGHT - CLOSE_SIZE) / 2
        val hoveringClose = mouseX in closeX..(closeX + CLOSE_SIZE) && mouseY in closeY..(closeY + CLOSE_SIZE)
        fillRoundedRect(g, closeX - 1, closeY - 1, CLOSE_SIZE + 2, CLOSE_SIZE + 2,
            if (hoveringClose) CLOSE_HOVER_BG else CLOSE_BG, tl = 2, tr = 2, bl = 2, br = 2)
        g.drawString(font, "x",
            closeX + (CLOSE_SIZE - font.width("x")) / 2,
            closeY + (CLOSE_SIZE - font.lineHeight) / 2,
            if (hoveringClose) CLOSE_HOVER_ICON_COLOR else CLOSE_ICON_COLOR, true)
        y += ROW_HEIGHT

        g.fill(panelLeft + PANEL_PADDING, y + ROW_HEIGHT / 2,
            panelLeft + PANEL_WIDTH - PANEL_PADDING, y + ROW_HEIGHT / 2 + 1, HEADER_LINE)
        y += ROW_HEIGHT

        val bodyTop = y
        val bodyBottom = panelTop + computedPanelHeight - PANEL_PADDING

        val sidebarX = panelLeft + PANEL_PADDING
        val slotTiers = mutableMapOf<Int, Int>()
        for (a in auras) {
            if (a.providerSlot >= 0) {
                slotTiers[a.providerSlot] = maxOf(slotTiers[a.providerSlot] ?: 0, a.tier)
            }
        }
        SidebarRenderer.render(g, font, sidebarX, bodyTop, partySlots, selectedSlot, slotTiers, slotStates, mouseX, mouseY, partialTick)

        val mainX = sidebarX + SIDEBAR_WIDTH + CONTENT_GAP
        infoButtonHitAreas.clear()
        g.enableScissor(mainX - 2, bodyTop, panelLeft + PANEL_WIDTH - PANEL_PADDING, bodyBottom)
        y = bodyTop - scrollOffset.toInt()
        y = ContentRenderer.renderMainContent(g, font, mainX, y, selectedSlotInfo, selectedAuras, t2, t3,
            infoButtonHitAreas, mouseX, mouseY)
        g.disableScissor()

        if (maxScroll > 0) {
            val sbX = panelLeft + PANEL_WIDTH - PANEL_PADDING - 3
            val trackH = bodyBottom - bodyTop
            val thumbH = ((trackH.toFloat() * trackH / (trackH + maxScroll)).toInt()).coerceAtLeast(12)
            val thumbY = bodyTop + ((scrollOffset / maxScroll) * (trackH - thumbH)).toInt()
            fillRoundedRect(g, sbX, bodyTop, 3, trackH, SCROLLBAR_TRACK, tl = 1, tr = 1, bl = 1, br = 1)
            fillRoundedRect(g, sbX, thumbY, 3, thumbH, SCROLLBAR_THUMB, tl = 1, tr = 1, bl = 1, br = 1)
        }

        val popupType = infoPopupType
        if (popupType != null) {
            val typeBuff = BuffRegistry.getForType(popupType)
            if (typeBuff != null) {
                val t1 = ClientConfigState.t1Threshold
                val bounds = TypeInfoPopupRenderer.computeBounds(font, width, height, typeBuff, t1, t2, t3)
                cachedPopupBounds = bounds
                TypeInfoPopupRenderer.render(g, font, bounds, width, height,
                    popupType, typeBuff, t1, t2, t3, mouseX, mouseY)
            } else {
                infoPopupType = null
                cachedPopupBounds = null
            }
        } else {
            cachedPopupBounds = null
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            val mx = mouseX.toInt()
            val my = mouseY.toInt()

            val popupType = infoPopupType
            if (popupType != null) {
                val bounds = cachedPopupBounds
                if (bounds != null) {
                    if (mx in bounds.closeX..(bounds.closeX + CLOSE_SIZE) &&
                        my in bounds.closeY..(bounds.closeY + CLOSE_SIZE)) {
                        infoPopupType = null
                        cachedPopupBounds = null
                        return true
                    }

                    if (mx !in bounds.x..(bounds.x + bounds.w) || my !in bounds.y..(bounds.y + bounds.h)) {
                        infoPopupType = null
                        cachedPopupBounds = null
                        return true
                    }

                    return true
                } else {
                    infoPopupType = null
                }
            }

            val titleY = panelTop + PANEL_PADDING
            val closeX = panelLeft + PANEL_WIDTH - PANEL_PADDING - CLOSE_SIZE
            val closeY = titleY + (ROW_HEIGHT - CLOSE_SIZE) / 2
            if (mx in closeX..(closeX + CLOSE_SIZE) && my in closeY..(closeY + CLOSE_SIZE)) {
                onClose()
                return true
            }

            val bodyTop = panelTop + PANEL_PADDING + ROW_HEIGHT * 2
            val bodyBottom = panelTop + computedPanelHeight - PANEL_PADDING
            if (my in bodyTop..bodyBottom) {
                for (btn in infoButtonHitAreas) {
                    if (mx in btn.x..(btn.x + btn.size) && my in btn.y..(btn.y + btn.size)) {
                        infoPopupType = btn.auraId
                        return true
                    }
                }
            }

            val sidebarX = panelLeft + PANEL_PADDING
            val gridStartY = bodyTop + SIDEBAR_HEADER_H + GRID_PAD
            val gridLeft = sidebarX + GRID_PAD
            val slots = ClientAuraState.partySlots
            for (i in 0 until PARTY_SIZE) {
                val col = i % GRID_COLS
                val row = i / GRID_COLS
                val sx = gridLeft + col * (SLOT_W + SLOT_COL_GAP)
                val sy = gridStartY + row * (SLOT_H + SLOT_ROW_GAP)
                if (mx in sx..(sx + SLOT_W) && my in sy..(sy + SLOT_H)) {
                    if (slots.getOrNull(i) != null && i != selectedSlot) {
                        selectedSlot = i
                        scrollOffset = 0f
                        infoPopupType = null
                        cachedPopupBounds = null
                    }
                    return true
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == 256 && infoPopupType != null) {
            infoPopupType = null
            cachedPopupBounds = null
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (infoPopupType != null) return true
        if (maxScroll > 0) {
            scrollOffset = (scrollOffset - scrollY.toFloat() * 10f).coerceIn(0f, maxScroll)
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    override fun isPauseScreen(): Boolean = false
}
