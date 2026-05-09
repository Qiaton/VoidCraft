package com.example.voidcraft.Gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class VoidChargerScreen extends AbstractContainerScreen<VoidChargerMenu> {
    private static final int PANEL_LEFT = 0;
    private static final int CACHE_BAR_X = 36;
    private static final int CACHE_BAR_Y = 91;
    private static final int CACHE_BAR_WIDTH = 104;
    private static final int CACHE_BAR_HEIGHT = 5;

    public VoidChargerScreen(VoidChargerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = VoidChargerMenu.IMAGE_WIDTH;
        this.imageHeight = VoidChargerMenu.IMAGE_HEIGHT;
        this.inventoryLabelY = 1000;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        int panelX = x + PANEL_LEFT;

        GuiDraw.drawBg(guiGraphics, x, y, this.imageWidth, this.imageHeight);
        GuiDraw.drawPanel(guiGraphics, panelX, y, VoidChargerMenu.PANEL_WIDTH, VoidChargerMenu.PANEL_HEIGHT);
        GuiDraw.drawPanelLine(guiGraphics, panelX, y, VoidChargerMenu.PANEL_WIDTH, 39);
        GuiDraw.drawInv(guiGraphics, x + 6, y + VoidChargerMenu.PLAYER_INVENTORY_Y - 5, this.imageWidth - 12, this.imageHeight - VoidChargerMenu.PLAYER_INVENTORY_Y - 1);

        renderSlotBacks(guiGraphics);
        renderCacheBar(guiGraphics);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, PANEL_LEFT + 8, 9, GuiStyle.TEXT_TITLE, false);

        Component status = Component.translatable(this.menu.isRunning()
                ? "screen.void_craft.void_charger.running"
                : "screen.void_craft.void_charger.idle");
        GuiDraw.drawStatus(
                guiGraphics,
                this.font,
                status,
                PANEL_LEFT + VoidChargerMenu.PANEL_WIDTH - 8,
                9,
                this.menu.isRunning() ? GuiStyle.TEXT_OK : GuiStyle.TEXT_MUTED
        );

        guiGraphics.drawCenteredString(this.font, Component.translatable("screen.void_craft.void_charger.slots"), VoidChargerMenu.SLOT_GRID_CENTER_X, 30, GuiStyle.TEXT_DIM);
        guiGraphics.drawCenteredString(this.font, this.menu.getEnergyStored() + " / " + this.menu.getEnergyCapacity(), VoidChargerMenu.SLOT_GRID_CENTER_X, 98, GuiStyle.TEXT);
    }

    private void renderSlotBacks(GuiGraphics guiGraphics) {
        for (int i = 0; i < this.menu.getChargerSlotCount(); i++) {
            Slot slot = this.menu.slots.get(i);
            GuiDraw.drawSlot(guiGraphics, this.leftPos + slot.x, this.topPos + slot.y, this.menu.isRunning() ? 0x8876E1C7 : GuiStyle.LINE_SLOT);
        }
    }

    private void renderCacheBar(GuiGraphics guiGraphics) {
        int x = this.leftPos + CACHE_BAR_X;
        int y = this.topPos + CACHE_BAR_Y;
        int stored = Math.max(0, this.menu.getEnergyStored());
        int capacity = Math.max(1, this.menu.getEnergyCapacity());
        int filled = Math.min(CACHE_BAR_WIDTH, stored * CACHE_BAR_WIDTH / capacity);

        guiGraphics.fill(x, y, x + CACHE_BAR_WIDTH, y + CACHE_BAR_HEIGHT, 0xFF070B0F);
        guiGraphics.renderOutline(x, y, CACHE_BAR_WIDTH, CACHE_BAR_HEIGHT, 0x554B6577);
        int innerFilled = Math.min(CACHE_BAR_WIDTH - 2, filled);
        if (innerFilled > 0) {
            guiGraphics.fill(x + 1, y + 1, x + 1 + innerFilled, y + CACHE_BAR_HEIGHT - 1, GuiStyle.ACCENT);
        }
    }
}
