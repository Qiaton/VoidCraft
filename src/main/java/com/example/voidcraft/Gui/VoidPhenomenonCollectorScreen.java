package com.example.voidcraft.Gui;

import com.example.voidcraft.Custom.Behavior.Energy.VoidEnergyBinding;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

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

        // 背景、主面板、背包区域都走 GuiDraw，后面新机器 GUI 可以照这个顺序画。
        GuiDraw.drawBg(guiGraphics, x, y, this.imageWidth, this.imageHeight);
        GuiDraw.drawPanel(guiGraphics, panelX, y, VoidPhenomenonCollectorMenu.PANEL_WIDTH, VoidPhenomenonCollectorMenu.PANEL_HEIGHT);
        GuiDraw.drawPanelLine(guiGraphics, panelX, y, VoidPhenomenonCollectorMenu.PANEL_WIDTH, 39);
        GuiDraw.drawInv(guiGraphics, x + 6, y + VoidPhenomenonCollectorMenu.PLAYER_INVENTORY_Y - 5, this.imageWidth - 12, this.imageHeight - VoidPhenomenonCollectorMenu.PLAYER_INVENTORY_Y - 1);

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
        if (GuiDraw.inRect(localX, localY, TAB_OVERVIEW_X, TAB_Y, TAB_WIDTH, TAB_HEIGHT)) {
            setSelectedTab(TAB_OVERVIEW);
            return true;
        }
        if (GuiDraw.inRect(localX, localY, TAB_CONNECTIONS_X, TAB_Y, TAB_WIDTH, TAB_HEIGHT)) {
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
                GuiDraw.inRect(mouseX, mouseY, TAB_OVERVIEW_X, TAB_Y, TAB_WIDTH, TAB_HEIGHT)
        );
        renderTab(
                guiGraphics,
                TAB_CONNECTIONS_X,
                Component.translatable("screen.void_craft.void_phenomenon_collector.connections"),
                this.selectedTab == TAB_CONNECTIONS,
                GuiDraw.inRect(mouseX, mouseY, TAB_CONNECTIONS_X, TAB_Y, TAB_WIDTH, TAB_HEIGHT)
        );
    }

    private void renderTab(GuiGraphics guiGraphics, int x, Component text, boolean selected, boolean hovered) {
        GuiDraw.drawTab(guiGraphics, this.font, x, TAB_Y, TAB_WIDTH, TAB_HEIGHT, text, selected, hovered);
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
            String clippedPos = GuiDraw.clip(this.font, posText, 62);
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
        GuiDraw.drawSlots(guiGraphics, this.menu.slots, this.leftPos, this.topPos);
    }

    private void renderConnectionListBackground(GuiGraphics guiGraphics) {
        int x = this.leftPos + PANEL_LEFT + 8;
        int y = this.topPos + 56;
        GuiDraw.drawBox(guiGraphics, x, y, CONNECTION_LIST_WIDTH, 45);
    }

    private boolean isOverviewPage() {
        return this.selectedTab == TAB_OVERVIEW;
    }

    private boolean isConnectionsPage() {
        return this.selectedTab == TAB_CONNECTIONS;
    }

}
