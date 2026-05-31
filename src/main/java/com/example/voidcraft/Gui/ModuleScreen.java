package com.example.voidcraft.Gui;

import com.example.voidcraft.ClientCustom.Key.ModKeyMappings;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleItem;
import com.example.voidcraft.Network.ModNetworking;
import com.example.voidcraft.Network.SwitchModuleFormPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class ModuleScreen extends AbstractContainerScreen<ModuleMenu> {
    private static final int WATCH_PANEL_LEFT = 0;
    private static final int WATCH_PANEL_WIDTH = 176;
    private static final int WATCH_PANEL_HEIGHT = 47;

    public ModuleScreen(ModuleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 133;
        this.inventoryLabelY = 43;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        int panelX = x + WATCH_PANEL_LEFT;

        GuiDraw.drawBg(guiGraphics, x, y, this.imageWidth, this.imageHeight);
        GuiDraw.drawPanel(guiGraphics, panelX, y, WATCH_PANEL_WIDTH, WATCH_PANEL_HEIGHT);
        GuiDraw.drawPanelLine(guiGraphics, panelX, y, WATCH_PANEL_WIDTH, WATCH_PANEL_HEIGHT - 1);
        GuiDraw.drawInv(guiGraphics, x + 6, y + 48, this.imageWidth - 12, this.imageHeight - 54);

        renderSlotBackgrounds(guiGraphics);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, WATCH_PANEL_LEFT + 8, 7, 0xFFEAF4FF, false);

        drawSlotLabel(guiGraphics, ModuleMenu.WATCH_SLOT_START_X, Component.translatable("screen.void_craft.phase_watch.module_1"));
        drawSlotLabel(guiGraphics, ModuleMenu.WATCH_SLOT_START_X + ModuleMenu.WATCH_SLOT_SPACING, Component.translatable("screen.void_craft.phase_watch.module_2"));
        drawSlotLabel(guiGraphics, ModuleMenu.WATCH_SLOT_START_X + ModuleMenu.WATCH_SLOT_SPACING * 2, Component.translatable("screen.void_craft.phase_watch.core"));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (ModKeyMappings.OPEN_PHASE_WATCH_KEY.matches(event)) {
            if (turnHoveredModuleForm()) {
                return true;
            }

            this.onClose();
            return true;
        }

        return super.keyPressed(event);
    }

    private boolean turnHoveredModuleForm() {
        Slot slot = this.getSlotUnderMouse();
        if (slot == null || !slot.hasItem()) {
            return false;
        }

        if (!ModuleItem.canTurnForm(slot.getItem())) {
            return false;
        }

        ModNetworking.sendToServer(new SwitchModuleFormPayload(slot.index));
        return true;
    }

    private void renderSlotBackgrounds(GuiGraphics guiGraphics) {
        for (Slot slot : this.menu.slots) {
            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y;
            boolean watchSlot = slot.index < 3;
            int outline = watchSlot ? 0x885B7A8C : 0x554B6577;
            GuiDraw.drawSlot(guiGraphics, x, y, outline);
        }
    }

    private void drawSlotLabel(GuiGraphics guiGraphics, int slotX, Component text) {
        guiGraphics.drawCenteredString(this.font, text, slotX + 8, 39, 0xFF8F9BA8);
    }
}
