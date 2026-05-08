package com.example.voidcraft.Gui;

import com.example.voidcraft.Block.entity.VoidEnergyBinding;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;

public class VoidPhenomenonCollectorScreen extends AbstractContainerScreen<VoidPhenomenonCollectorMenu> {
    private static final int IMAGE_HEIGHT = 194;
    private static final int PANEL_LEFT = (VoidPhenomenonCollectorMenu.IMAGE_WIDTH - VoidPhenomenonCollectorMenu.PANEL_WIDTH) / 2;
    private static final int TAB_OVERVIEW = 0;
    private static final int TAB_CONNECTIONS = 1;
    private static final int TAB_Y = 23;
    private static final int TAB_HEIGHT = 15;
    private static final int TAB_WIDTH = 36;
    private static final int TAB_OVERVIEW_X = PANEL_LEFT + 8;
    private static final int TAB_CONNECTIONS_X = TAB_OVERVIEW_X + TAB_WIDTH + 4;
    private static final int OVERVIEW_STATS_X = 86;
    private static final int CONNECTION_LIST_WIDTH = VoidPhenomenonCollectorMenu.PANEL_WIDTH - 16;
    private static final int CONNECTION_STATUS_X = PANEL_LEFT + VoidPhenomenonCollectorMenu.PANEL_WIDTH - 38;

    private int selectedTab = TAB_OVERVIEW;

    public VoidPhenomenonCollectorScreen(VoidPhenomenonCollectorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = VoidPhenomenonCollectorMenu.IMAGE_WIDTH;
        this.imageHeight = IMAGE_HEIGHT;
        this.inventoryLabelY = VoidPhenomenonCollectorMenu.PLAYER_INVENTORY_Y - 11;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 每帧把当前页签同步给菜单，让 Slot 的 isActive 跟着变。
        this.menu.setOverviewPage(isOverviewPage());
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        int panelX = x + PANEL_LEFT;

        // 背景全部用简单 fill 画，避免额外维护 GUI 贴图。
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xE00B0E12);
        guiGraphics.fill(panelX, y, panelX + VoidPhenomenonCollectorMenu.PANEL_WIDTH, y + VoidPhenomenonCollectorMenu.PANEL_HEIGHT, 0xF014181D);
        guiGraphics.renderOutline(panelX, y, VoidPhenomenonCollectorMenu.PANEL_WIDTH, VoidPhenomenonCollectorMenu.PANEL_HEIGHT, 0x663A5368);
        guiGraphics.fill(panelX, y + 39, panelX + VoidPhenomenonCollectorMenu.PANEL_WIDTH, y + 40, 0x44283844);
        guiGraphics.fill(x + 6, y + VoidPhenomenonCollectorMenu.PLAYER_INVENTORY_Y - 5, x + this.imageWidth - 6, y + this.imageHeight - 6, 0xAA11161B);

        renderSlotBackgrounds(guiGraphics);
        if (isConnectionsPage()) {
            // 连接页没有结晶槽，单独画输出列表背景。
            renderConnectionListBackground(guiGraphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int localMouseX = mouseX - this.leftPos;
        int localMouseY = mouseY - this.topPos;

        guiGraphics.drawString(this.font, this.title, PANEL_LEFT + 8, 9, 0xFFEAF4FF, false);
        renderStatus(guiGraphics);
        renderTabs(guiGraphics, localMouseX, localMouseY);

        if (isOverviewPage()) {
            renderOverview(guiGraphics);
        } else {
            renderConnections(guiGraphics);
        }

    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        int localX = (int) event.x() - this.leftPos;
        int localY = (int) event.y() - this.topPos;

        // 页签是纯客户端切换，不需要发包。
        if (isInRect(localX, localY, TAB_OVERVIEW_X, TAB_Y, TAB_WIDTH, TAB_HEIGHT)) {
            setSelectedTab(TAB_OVERVIEW);
            return true;
        }
        if (isInRect(localX, localY, TAB_CONNECTIONS_X, TAB_Y, TAB_WIDTH, TAB_HEIGHT)) {
            setSelectedTab(TAB_CONNECTIONS);
            return true;
        }

        return super.mouseClicked(event, isDoubleClick);
    }

    private void setSelectedTab(int tab) {
        if (this.selectedTab != tab) {
            this.selectedTab = tab;
            this.menu.setOverviewPage(isOverviewPage());
            AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
        }
    }

    private void renderStatus(GuiGraphics guiGraphics) {
        // 状态点跟随服务端同步的 running 值。
        Component status = Component.translatable(this.menu.isRunning()
                ? "screen.void_craft.void_phenomenon_collector.running"
                : "screen.void_craft.void_phenomenon_collector.stopped");
        int textWidth = this.font.width(status);
        int textX = PANEL_LEFT + VoidPhenomenonCollectorMenu.PANEL_WIDTH - 8 - textWidth;
        int color = this.menu.isRunning() ? 0xFF89F6B4 : 0xFF7C8792;
        guiGraphics.fill(textX - 8, 12, textX - 4, 16, color);
        guiGraphics.drawString(this.font, status, textX, 9, color, false);
    }

    private void renderTabs(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTab(
                guiGraphics,
                TAB_OVERVIEW_X,
                Component.translatable("screen.void_craft.void_phenomenon_collector.overview"),
                this.selectedTab == TAB_OVERVIEW,
                isInRect(mouseX, mouseY, TAB_OVERVIEW_X, TAB_Y, TAB_WIDTH, TAB_HEIGHT)
        );
        renderTab(
                guiGraphics,
                TAB_CONNECTIONS_X,
                Component.translatable("screen.void_craft.void_phenomenon_collector.connections"),
                this.selectedTab == TAB_CONNECTIONS,
                isInRect(mouseX, mouseY, TAB_CONNECTIONS_X, TAB_Y, TAB_WIDTH, TAB_HEIGHT)
        );
    }

    private void renderTab(GuiGraphics guiGraphics, int x, Component text, boolean selected, boolean hovered) {
        int color = selected ? 0xFFB9F4FF : hovered ? 0xFFD8E8F6 : 0xFF8290A0;
        int background = selected ? 0x552A4855 : hovered ? 0x33202B33 : 0x22181F26;
        guiGraphics.fill(x, TAB_Y, x + TAB_WIDTH, TAB_Y + TAB_HEIGHT, background);
        guiGraphics.drawCenteredString(this.font, text, x + TAB_WIDTH / 2, TAB_Y + 4, color);
        if (selected) {
            guiGraphics.fill(x + 5, TAB_Y + TAB_HEIGHT - 2, x + TAB_WIDTH - 5, TAB_Y + TAB_HEIGHT, 0xFF62D6E8);
        }
    }

    private void renderOverview(GuiGraphics guiGraphics) {
        // 概览页显示结晶槽、产能倍率、缓存电量和输出数量。
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("screen.void_craft.void_phenomenon_collector.crystal_slot"),
                VoidPhenomenonCollectorMenu.CRYSTAL_GRID_CENTER_X,
                43,
                0xFF8F9BA8
        );

        int statsX = OVERVIEW_STATS_X;
        guiGraphics.drawString(this.font, Component.translatable("screen.void_craft.void_phenomenon_collector.efficiency"), statsX, 46, 0xFF8F9BA8, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.void_craft.void_phenomenon_collector.efficiency_value", this.menu.getEnergyPerTick()), statsX, 56, 0xFFEAF4FF, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.void_craft.void_phenomenon_collector.tier", this.menu.getTierDisplayName()), statsX, 68, 0xFFEAF4FF, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.void_craft.void_phenomenon_collector.energy"), statsX, 80, 0xFF8F9BA8, false);
        guiGraphics.drawString(this.font, this.menu.getEnergyStored() + " / " + this.menu.getEnergyCapacity(), statsX, 90, 0xFFEAF4FF, false);
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.void_craft.void_phenomenon_collector.output_count", this.menu.getOutputCount(), this.menu.getMaxOutputCount()),
                statsX,
                100,
                0xFF9FC7D6,
                false
        );
    }

    private void renderConnections(GuiGraphics guiGraphics) {
        // 连接页只展示当前输出目标；新增/删除连接仍然走坐标制定器那套面板。
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.void_craft.void_phenomenon_collector.output_connections"),
                PANEL_LEFT + 8,
                45,
                0xFFDDEEFF,
                false
        );

        List<VoidEnergyBinding> outputs = this.menu.getOutputTargets();
        if (outputs.isEmpty()) {
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("screen.void_craft.void_phenomenon_collector.output_empty"),
                    PANEL_LEFT + 12,
                    62,
                    0xFF7C8792,
                    false
            );
            return;
        }

        int visibleRows = Math.min(3, outputs.size());
        for (int row = 0; row < visibleRows; row++) {
            VoidEnergyBinding binding = outputs.get(row);
            int y = 60 + row * 14;
            String posText = binding.target().pos().getX() + ", " + binding.target().pos().getY() + ", " + binding.target().pos().getZ();
            String clippedPos = clip(posText, 62);
            guiGraphics.drawString(this.font, Component.translatable("screen.void_craft.void_phenomenon_collector.output_short"), PANEL_LEFT + 12, y, 0xFF9FC7D6, false);
            guiGraphics.drawString(this.font, clippedPos, PANEL_LEFT + 40, y, 0xFFEAF4FF, false);
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("screen.void_craft.void_phenomenon_collector.bound"),
                    CONNECTION_STATUS_X,
                    y,
                    0xFF89F6B4,
                    false
            );
        }

        if (outputs.size() > visibleRows) {
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("screen.void_craft.void_phenomenon_collector.more_outputs", outputs.size() - visibleRows),
                    PANEL_LEFT + 12,
                    101,
                    0xFF7C8792,
                    false
            );
        }
    }

    private void renderSlotBackgrounds(GuiGraphics guiGraphics) {
        // 只给当前 active 的槽画背景，连接页隐藏结晶槽时这里也不会画出来。
        for (Slot slot : this.menu.slots) {
            if (!slot.isActive()) {
                continue;
            }
            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y;
            guiGraphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF0A0D10);
            guiGraphics.renderOutline(x - 1, y - 1, 18, 18, 0x774B6577);
        }
    }

    private void renderConnectionListBackground(GuiGraphics guiGraphics) {
        int x = this.leftPos + PANEL_LEFT + 8;
        int y = this.topPos + 56;
        guiGraphics.fill(x, y, x + CONNECTION_LIST_WIDTH, y + 45, 0x66090D11);
        guiGraphics.renderOutline(x, y, CONNECTION_LIST_WIDTH, 45, 0x333A5368);
    }

    private boolean isOverviewPage() {
        return this.selectedTab == TAB_OVERVIEW;
    }

    private boolean isConnectionsPage() {
        return this.selectedTab == TAB_CONNECTIONS;
    }

    private boolean isInRect(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x
                && mouseY >= y
                && mouseX < x + width
                && mouseY < y + height;
    }

    private String clip(String text, int width) {
        // 坐标过长时截断，防止文字顶出面板。
        if (this.font.width(text) <= width) {
            return text;
        }
        return this.font.plainSubstrByWidth(text, width - this.font.width("...")) + "...";
    }
}
