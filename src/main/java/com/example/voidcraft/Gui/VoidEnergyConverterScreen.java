package com.example.voidcraft.Gui;

import com.example.voidcraft.Custom.Behavior.Energy.BoundVoidPosition;
import com.example.voidcraft.Network.ModNetworking;
import com.example.voidcraft.Network.RequestCoordinateBindingsPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

public class VoidEnergyConverterScreen extends AbstractContainerScreen<VoidEnergyConverterMenu> {
    private static final int LEFT_X = 12;
    private static final int RIGHT_X = 150;
    private static final int SECTION_Y = 48;
    private static final int COLUMN_WIDTH = 114;
    private static final int CACHE_BAR_X = LEFT_X;
    private static final int CACHE_BAR_Y = 94;
    private static final int CACHE_BAR_WIDTH = 114;
    private static final int CACHE_BAR_HEIGHT = 6;
    private static final int CONNECTION_TAG_X = RIGHT_X;
    private static final int CONNECTION_TAG_Y = 118;
    private static final int CONNECTION_TAG_WIDTH = COLUMN_WIDTH;
    private static final int CONNECTION_TAG_HEIGHT = 18;
    private static final int DONE_TAG_WIDTH = 38;
    private static final int DONE_TAG_HEIGHT = 18;

    public VoidEnergyConverterScreen(VoidEnergyConverterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = VoidEnergyConverterMenu.IMAGE_WIDTH;
        this.imageHeight = VoidEnergyConverterMenu.IMAGE_HEIGHT;
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
        GuiDraw.drawPanel(guiGraphics, x, y, VoidEnergyConverterMenu.PANEL_WIDTH, VoidEnergyConverterMenu.PANEL_HEIGHT);
        GuiDraw.drawPanelLine(guiGraphics, x, y, VoidEnergyConverterMenu.PANEL_WIDTH, 39);
        GuiDraw.drawBox(guiGraphics, x + LEFT_X, y + SECTION_Y, COLUMN_WIDTH, 92);
        GuiDraw.drawBox(guiGraphics, x + RIGHT_X, y + SECTION_Y, COLUMN_WIDTH, 92);
        renderCacheBar(guiGraphics);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int localMouseX = mouseX - this.leftPos;
        int localMouseY = mouseY - this.topPos;

        guiGraphics.drawString(this.font, this.title, 8, 9, GuiStyle.TEXT_TITLE, false);
        GuiDraw.drawLine(guiGraphics, this.font, 8, 26, Component.literal(posText(this.menu.getBlockPos())), GuiStyle.TEXT_MID, this.imageWidth - 16);

        renderHeaderStatus(guiGraphics);
        renderCache(guiGraphics);
        renderRun(guiGraphics);
        renderConnectionTag(guiGraphics, localMouseX, localMouseY);
        renderDoneTag(guiGraphics, localMouseX, localMouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        int localX = (int) event.x() - this.leftPos;
        int localY = (int) event.y() - this.topPos;

        if (GuiDraw.inRect(localX, localY, CONNECTION_TAG_X, CONNECTION_TAG_Y, CONNECTION_TAG_WIDTH, CONNECTION_TAG_HEIGHT)) {
            openConnections();
            AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
            return true;
        }

        if (GuiDraw.inRect(localX, localY, doneTagX(), doneTagY(), DONE_TAG_WIDTH, DONE_TAG_HEIGHT)) {
            AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
            this.onClose();
            return true;
        }

        return super.mouseClicked(event, isDoubleClick);
    }

    private void renderCache(GuiGraphics guiGraphics) {
        GuiDraw.drawSection(guiGraphics, this.font, LEFT_X + 6, 58, COLUMN_WIDTH - 12, Component.translatable("screen.void_craft.void_energy_converter.cache"));
        GuiDraw.drawLine(
                guiGraphics,
                this.font,
                LEFT_X + 6,
                76,
                Component.literal(this.menu.getEnergyStored() + " / " + this.menu.getEnergyCapacity()),
                COLUMN_WIDTH - 12
        );

        int percent = this.menu.getEnergyStored() * 100 / Math.max(1, this.menu.getEnergyCapacity());
        GuiDraw.drawLine(
                guiGraphics,
                this.font,
                LEFT_X + 6,
                106,
                Component.translatable("screen.void_craft.void_energy_converter.percent", percent),
                GuiStyle.TEXT_MID,
                COLUMN_WIDTH - 12
        );
    }

    private void renderRun(GuiGraphics guiGraphics) {
        GuiDraw.drawSection(guiGraphics, this.font, RIGHT_X + 6, 58, COLUMN_WIDTH - 12, Component.translatable("screen.void_craft.void_energy_converter.status"));
        GuiDraw.drawLine(
                guiGraphics,
                this.font,
                RIGHT_X + 6,
                76,
                Component.translatable("screen.void_craft.void_energy_converter.input_count", this.menu.getInputCount(), this.menu.getMaxInputCount()),
                GuiStyle.TEXT_MID,
                COLUMN_WIDTH - 12
        );
        GuiDraw.drawLine(
                guiGraphics,
                this.font,
                RIGHT_X + 6,
                92,
                Component.translatable("screen.void_craft.void_energy_converter.output_count", this.menu.getOutputCount(), this.menu.getMaxOutputCount()),
                GuiStyle.TEXT_MID,
                COLUMN_WIDTH - 12
        );
    }

    private void renderHeaderStatus(GuiGraphics guiGraphics) {
        Component status = Component.translatable(this.menu.isRunning()
                ? "screen.void_craft.void_energy_converter.running"
                : "screen.void_craft.void_energy_converter.idle");
        GuiDraw.drawStatus(
                guiGraphics,
                this.font,
                status,
                this.imageWidth - 10,
                9,
                this.menu.isRunning() ? GuiStyle.TEXT_OK : GuiStyle.TEXT_MUTED
        );
    }

    private void renderConnectionTag(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        boolean hovered = GuiDraw.inRect(mouseX, mouseY, CONNECTION_TAG_X, CONNECTION_TAG_Y, CONNECTION_TAG_WIDTH, CONNECTION_TAG_HEIGHT);
        GuiDraw.drawTab(
                guiGraphics,
                this.font,
                CONNECTION_TAG_X,
                CONNECTION_TAG_Y,
                CONNECTION_TAG_WIDTH,
                CONNECTION_TAG_HEIGHT,
                Component.translatable("screen.void_craft.void_energy_converter.open_connections"),
                false,
                hovered
        );
    }

    private void renderDoneTag(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        boolean hovered = GuiDraw.inRect(mouseX, mouseY, doneTagX(), doneTagY(), DONE_TAG_WIDTH, DONE_TAG_HEIGHT);
        GuiDraw.drawTab(
                guiGraphics,
                this.font,
                doneTagX(),
                doneTagY(),
                DONE_TAG_WIDTH,
                DONE_TAG_HEIGHT,
                Component.translatable("gui.done"),
                false,
                hovered,
                GuiStyle.TEXT_MUTED
        );
    }

    private void renderCacheBar(GuiGraphics guiGraphics) {
        int x = this.leftPos + CACHE_BAR_X;
        int y = this.topPos + CACHE_BAR_Y;
        int stored = Math.max(0, this.menu.getEnergyStored());
        int capacity = Math.max(1, this.menu.getEnergyCapacity());
        int filled = Math.min(CACHE_BAR_WIDTH, stored * CACHE_BAR_WIDTH / capacity);

        guiGraphics.fill(x, y, x + CACHE_BAR_WIDTH, y + CACHE_BAR_HEIGHT, 0xFF070B0F);
        guiGraphics.renderOutline(x, y, CACHE_BAR_WIDTH, CACHE_BAR_HEIGHT, 0x554B6577);
        if (filled > 1) {
            guiGraphics.fill(x + 1, y + 1, x + filled, y + CACHE_BAR_HEIGHT - 1, GuiStyle.ACCENT);
        }
    }

    private void openConnections() {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        ModNetworking.sendToServer(new RequestCoordinateBindingsPayload(BoundVoidPosition.of(level, this.menu.getBlockPos())));
        this.onClose();
    }

    private String posText(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private int doneTagX() {
        return this.imageWidth - 12 - DONE_TAG_WIDTH;
    }

    private int doneTagY() {
        return this.imageHeight - 30;
    }
}
