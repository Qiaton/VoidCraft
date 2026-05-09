package com.example.voidcraft.Gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class ModuleBoostScreen extends AbstractContainerScreen<ModuleBoostMenu> {
    private static final int PANEL_WIDTH = ModuleBoostMenu.IMAGE_WIDTH;
    private static final int PANEL_HEIGHT = ModuleBoostMenu.TOP_HEIGHT;
    private static final int ARROW_X = 89;
    private static final int ARROW_Y = 39;

    public ModuleBoostScreen(ModuleBoostMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = ModuleBoostMenu.IMAGE_WIDTH;
        this.imageHeight = ModuleBoostMenu.IMAGE_HEIGHT;
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

        GuiDraw.drawBg(guiGraphics, x, y, this.imageWidth, this.imageHeight);
        GuiDraw.drawPanel(guiGraphics, x, y, PANEL_WIDTH, PANEL_HEIGHT);
        GuiDraw.drawPanelLine(guiGraphics, x, y, PANEL_WIDTH, PANEL_HEIGHT - 1);
        GuiDraw.drawInv(guiGraphics, x, y + ModuleBoostMenu.PLAYER_INV_Y - 6, this.imageWidth, this.imageHeight - (ModuleBoostMenu.PLAYER_INV_Y - 6));

        renderArrow(guiGraphics);
        renderSlotBack(guiGraphics);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderStatus(guiGraphics);

        guiGraphics.drawString(this.font, Component.translatable("screen.void_craft.module_boost.part"), 18, 12, 0xFF8F9BA8, false);
        guiGraphics.drawCenteredString(this.font, Component.translatable("screen.void_craft.module_boost.module"), ModuleBoostMenu.MODULE_SLOT_X + 8, 68, 0xFF9B85D6);
        guiGraphics.drawCenteredString(this.font, Component.translatable("screen.void_craft.module_boost.result"), ModuleBoostMenu.RESULT_SLOT_X + 8, 12, 0xFF92D9CB);
    }

    private void renderStatus(GuiGraphics guiGraphics) {
        if (!this.menu.hasResult()) {
            return;
        }

        int dotRight = this.imageWidth - 10;
        GuiDraw.drawDot(guiGraphics, dotRight - GuiDraw.DOT_SIZE, 10, GuiStyle.TEXT_OK);
    }

    private void renderArrow(GuiGraphics guiGraphics) {
        GuiDraw.drawArrow(guiGraphics, this.leftPos + ARROW_X, this.topPos + ARROW_Y, 0xFFCBD5E1);
    }

    private void renderSlotBack(GuiGraphics guiGraphics) {
        for (int i = 0; i < this.menu.slots.size(); i++) {
            Slot slot = this.menu.slots.get(i);
            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y;

            int outline = 0x554B6577;
            if (i >= ModuleBoostMenu.PART_SLOT_START && i < ModuleBoostMenu.PART_SLOT_START + ModuleBoostMenu.PART_SLOT_COUNT) {
                outline = 0x885A9FD3;
            } else if (i == ModuleBoostMenu.MODULE_SLOT) {
                outline = 0x886E47D6;
            } else if (i == ModuleBoostMenu.RESULT_SLOT) {
                outline = this.menu.hasResult() ? 0x8876E1C7 : 0x55647C79;
            }

            GuiDraw.drawSlot(guiGraphics, x, y, outline);
        }
    }
}
