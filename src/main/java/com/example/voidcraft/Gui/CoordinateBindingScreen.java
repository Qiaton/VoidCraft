package com.example.voidcraft.Gui;

import com.example.voidcraft.Custom.Behavior.Energy.VoidEnergyBindingType;
import com.example.voidcraft.Network.CoordinateBindingsPayload;
import com.example.voidcraft.Network.ModNetworking;
import com.example.voidcraft.Network.RemoveCoordinateBindingPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class CoordinateBindingScreen extends Screen {
    private static final int PANEL_WIDTH = 460;
    private static final int PANEL_HEIGHT = 220;
    private static final int PADDING = 12;
    private static final int LIST_X = 12;
    private static final int LIST_Y = 58;
    private static final int LIST_WIDTH = 258;
    private static final int LIST_HEIGHT = 114;
    private static final int DETAIL_X = 286;
    private static final int DETAIL_Y = 58;
    private static final int DETAIL_WIDTH = 162;
    private static final int DETAIL_HEIGHT = 114;
    private static final int ROW_HEIGHT = 24;
    private static final int ROW_INNER_HEIGHT = 21;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int REMOVE_TAG_WIDTH = 72;
    private static final int DONE_TAG_WIDTH = 54;
    private static final int TAG_HEIGHT = 18;

    private final CoordinateBindingsPayload payload;

    private int selectedIndex = -1;
    private int scrollIndex = 0;

    private CoordinateBindingScreen(CoordinateBindingsPayload payload) {
        super(Component.translatable("screen.void_craft.coordinate_bindings"));
        this.payload = payload;
    }

    public static void open(CoordinateBindingsPayload payload) {
        Minecraft.getInstance().setScreen(new CoordinateBindingScreen(payload));
    }

    @Override
    protected void init() {
        clampScrollIndex();
        if (this.selectedIndex >= this.payload.entries().size()) {
            this.selectedIndex = -1;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int left = panelLeft();
        int top = panelTop();
        int localMouseX = mouseX - left;
        int localMouseY = mouseY - top;

        GuiDraw.drawBg(guiGraphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);
        GuiDraw.drawPanel(guiGraphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);
        GuiDraw.drawPanelLine(guiGraphics, left, top, PANEL_WIDTH, 39);
        GuiDraw.drawBox(guiGraphics, left + LIST_X, top + LIST_Y, LIST_WIDTH, LIST_HEIGHT);
        GuiDraw.drawBox(guiGraphics, left + DETAIL_X, top + DETAIL_Y, DETAIL_WIDTH, DETAIL_HEIGHT);

        renderHeader(guiGraphics, left, top);
        renderListTitle(guiGraphics, left, top);
        renderList(guiGraphics, left, top, localMouseX, localMouseY);
        renderDetail(guiGraphics, left, top);
        renderRemoveTag(guiGraphics, left, top, localMouseX, localMouseY);
        renderDoneTag(guiGraphics, left, top, localMouseX, localMouseY);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        int left = panelLeft();
        int top = panelTop();
        int localX = (int) event.x() - left;
        int localY = (int) event.y() - top;

        int entryIndex = entryIndexAt(localX, localY);
        if (entryIndex != -1) {
            this.selectedIndex = entryIndex;
            AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
            return true;
        }

        if (GuiDraw.inRect(localX, localY, removeTagX(), tagY(), REMOVE_TAG_WIDTH, TAG_HEIGHT)) {
            if (selectedEntry() != null) {
                removeSelected();
                AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
            }
            return true;
        }

        if (GuiDraw.inRect(localX, localY, doneTagX(), tagY(), DONE_TAG_WIDTH, TAG_HEIGHT)) {
            AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
            this.onClose();
            return true;
        }

        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int left = panelLeft();
        int top = panelTop();
        if (GuiDraw.inRect(mouseX, mouseY, left + LIST_X, top + LIST_Y, LIST_WIDTH, LIST_HEIGHT)) {
            if (scrollY < 0.0D) {
                scrollBy(1);
            } else if (scrollY > 0.0D) {
                scrollBy(-1);
            }
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void renderHeader(GuiGraphics guiGraphics, int left, int top) {
        guiGraphics.drawString(this.font, this.title, left + PADDING, top + 9, GuiStyle.TEXT_TITLE, false);
        GuiDraw.drawLine(guiGraphics, this.font, left + PADDING, top + 27, Component.literal(this.payload.owner().shortText()), GuiStyle.TEXT_MID, PANEL_WIDTH - PADDING * 2);
    }

    private void renderListTitle(GuiGraphics guiGraphics, int left, int top) {
        GuiDraw.drawSection(
                guiGraphics,
                this.font,
                left + LIST_X,
                top + 45,
                LIST_WIDTH,
                Component.translatable("screen.void_craft.coordinate_bindings.entries")
        );
    }

    private void renderList(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        if (this.payload.entries().isEmpty()) {
            GuiDraw.drawLine(
                    guiGraphics,
                    this.font,
                    left + LIST_X + 8,
                    top + LIST_Y + 9,
                    Component.translatable("screen.void_craft.coordinate_bindings.empty"),
                    GuiStyle.TEXT_MUTED,
                    LIST_WIDTH - 16
            );
            return;
        }

        guiGraphics.enableScissor(left + LIST_X, top + LIST_Y, left + LIST_X + LIST_WIDTH, top + LIST_Y + LIST_HEIGHT);

        int rows = visibleRows();
        for (int row = 0; row < rows; row++) {
            int entryIndex = this.scrollIndex + row;
            if (entryIndex >= this.payload.entries().size()) {
                break;
            }

            CoordinateBindingsPayload.Entry entry = this.payload.entries().get(entryIndex);
            int rowX = left + LIST_X + 3;
            int rowY = top + LIST_Y + 2 + row * ROW_HEIGHT;
            boolean selected = this.selectedIndex == entryIndex;
            boolean hovered = GuiDraw.inRect(mouseX, mouseY, LIST_X, rowY - top - 2, LIST_WIDTH, ROW_INNER_HEIGHT);

            if (selected) {
                guiGraphics.fill(rowX, rowY - 1, rowX + LIST_WIDTH - 10, rowY + ROW_INNER_HEIGHT - 1, GuiStyle.TAB_ON);
            } else if (hovered) {
                guiGraphics.fill(rowX, rowY - 1, rowX + LIST_WIDTH - 10, rowY + ROW_INNER_HEIGHT - 1, GuiStyle.TAB_HOVER);
            }

            int sideColor = entry.outputList() ? GuiStyle.ACCENT : GuiStyle.TEXT_BAD;
            guiGraphics.drawString(this.font, sideText(entry), rowX + 4, rowY + 1, sideColor, false);
            GuiDraw.drawLine(guiGraphics, this.font, rowX + 44, rowY + 1, entry.targetName(), selected ? GuiStyle.TEXT_TITLE : GuiStyle.TEXT, LIST_WIDTH - 60);
            GuiDraw.drawLine(guiGraphics, this.font, rowX + 4, rowY + 12, entryPos(entry), GuiStyle.TEXT_MUTED, LIST_WIDTH - 20);
        }

        guiGraphics.disableScissor();
        renderScrollbar(guiGraphics, left, top);
    }

    private void renderScrollbar(GuiGraphics guiGraphics, int left, int top) {
        int total = this.payload.entries().size();
        int visible = visibleRows();
        if (total <= visible) {
            return;
        }

        int trackX = left + LIST_X + LIST_WIDTH - SCROLLBAR_WIDTH - 3;
        int trackY = top + LIST_Y;
        int thumbHeight = Math.max(16, LIST_HEIGHT * visible / total);
        int moveHeight = LIST_HEIGHT - thumbHeight;
        int thumbY = trackY;
        int maxScroll = maxScrollIndex();
        if (maxScroll > 0) {
            thumbY = trackY + moveHeight * this.scrollIndex / maxScroll;
        }

        guiGraphics.fill(trackX, trackY + 2, trackX + SCROLLBAR_WIDTH, trackY + LIST_HEIGHT - 2, 0x66000000);
        guiGraphics.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbHeight, GuiStyle.ACCENT);
    }

    private void renderDetail(GuiGraphics guiGraphics, int left, int top) {
        GuiDraw.drawSection(
                guiGraphics,
                this.font,
                left + DETAIL_X + 6,
                top + 45,
                DETAIL_WIDTH - 12,
                Component.translatable("screen.void_craft.coordinate_bindings.selected")
        );

        CoordinateBindingsPayload.Entry entry = selectedEntry();
        if (entry == null) {
            GuiDraw.drawLine(
                    guiGraphics,
                    this.font,
                    left + DETAIL_X + 8,
                    top + DETAIL_Y + 10,
                    Component.translatable("screen.void_craft.coordinate_bindings.select_hint"),
                    GuiStyle.TEXT_MUTED,
                    DETAIL_WIDTH - 16
            );
            return;
        }

        int x = left + DETAIL_X + 8;
        int y = top + DETAIL_Y + 9;
        int width = DETAIL_WIDTH - 16;
        GuiDraw.drawLine(guiGraphics, this.font, x, y, Component.translatable("screen.void_craft.coordinate_bindings.target_name", entry.targetName()), getEntryColor(entry), width);
        GuiDraw.drawLine(guiGraphics, this.font, x, y + 16, Component.translatable("screen.void_craft.coordinate_bindings.target_dimension", entry.target().dimension().toString()), GuiStyle.TEXT_MID, width);
        GuiDraw.drawLine(guiGraphics, this.font, x, y + 32, Component.translatable(
                "screen.void_craft.coordinate_bindings.target_pos",
                entry.target().pos().getX(),
                entry.target().pos().getY(),
                entry.target().pos().getZ()
        ), GuiStyle.TEXT, width);
        GuiDraw.drawLine(guiGraphics, this.font, x, y + 48, Component.translatable("screen.void_craft.coordinate_bindings.target_port", sideText(entry), entry.type().getDisplayName()), GuiStyle.TEXT, width);
        GuiDraw.drawLine(guiGraphics, this.font, x, y + 64, Component.translatable(entry.status().translationKey()), GuiStyle.TEXT_MUTED, width);
    }

    private void renderRemoveTag(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        boolean active = selectedEntry() != null;
        boolean hovered = active && GuiDraw.inRect(mouseX, mouseY, removeTagX(), tagY(), REMOVE_TAG_WIDTH, TAG_HEIGHT);
        GuiDraw.drawTab(
                guiGraphics,
                this.font,
                left + removeTagX(),
                top + tagY(),
                REMOVE_TAG_WIDTH,
                TAG_HEIGHT,
                Component.translatable("button.void_craft.coordinate_bindings.remove"),
                false,
                hovered,
                active ? GuiStyle.TEXT_BAD : GuiStyle.TEXT_MUTED
        );
        if (!active) {
            guiGraphics.fill(left + removeTagX(), top + tagY(), left + removeTagX() + REMOVE_TAG_WIDTH, top + tagY() + TAG_HEIGHT, 0x33000000);
        }
    }

    private void renderDoneTag(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        boolean hovered = GuiDraw.inRect(mouseX, mouseY, doneTagX(), tagY(), DONE_TAG_WIDTH, TAG_HEIGHT);
        GuiDraw.drawTab(
                guiGraphics,
                this.font,
                left + doneTagX(),
                top + tagY(),
                DONE_TAG_WIDTH,
                TAG_HEIGHT,
                Component.translatable("gui.done"),
                false,
                hovered,
                GuiStyle.TEXT_MUTED
        );
    }

    private int entryIndexAt(double mouseX, double mouseY) {
        if (!GuiDraw.inRect(mouseX, mouseY, LIST_X, LIST_Y, LIST_WIDTH, LIST_HEIGHT)) {
            return -1;
        }

        int row = (int) ((mouseY - LIST_Y) / ROW_HEIGHT);
        if (row < 0 || row >= visibleRows()) {
            return -1;
        }

        int entryIndex = this.scrollIndex + row;
        if (entryIndex < 0 || entryIndex >= this.payload.entries().size()) {
            return -1;
        }

        return entryIndex;
    }

    private void removeSelected() {
        CoordinateBindingsPayload.Entry selected = selectedEntry();
        if (selected == null) {
            return;
        }

        ModNetworking.sendToServer(new RemoveCoordinateBindingPayload(
                this.payload.owner(),
                selected.outputList(),
                selected.target()
        ));
    }

    private CoordinateBindingsPayload.Entry selectedEntry() {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.payload.entries().size()) {
            return null;
        }
        return this.payload.entries().get(this.selectedIndex);
    }

    private Component sideText(CoordinateBindingsPayload.Entry entry) {
        return Component.translatable(entry.outputList()
                ? "screen.void_craft.coordinate_bindings.output"
                : "screen.void_craft.coordinate_bindings.input");
    }

    private Component entryPos(CoordinateBindingsPayload.Entry entry) {
        return Component.literal(entry.target().pos().getX() + ", " + entry.target().pos().getY() + ", " + entry.target().pos().getZ());
    }

    private int getEntryColor(CoordinateBindingsPayload.Entry entry) {
        return entry.type() == VoidEnergyBindingType.OUTPUT ? GuiStyle.TEXT_OK : GuiStyle.TEXT_BAD;
    }

    private void scrollBy(int rows) {
        this.scrollIndex = Mth.clamp(this.scrollIndex + rows, 0, maxScrollIndex());
    }

    private void clampScrollIndex() {
        this.scrollIndex = Mth.clamp(this.scrollIndex, 0, maxScrollIndex());
    }

    private int maxScrollIndex() {
        return Math.max(0, this.payload.entries().size() - visibleRows());
    }

    private int visibleRows() {
        return Math.max(1, LIST_HEIGHT / ROW_HEIGHT);
    }

    private int panelLeft() {
        return (this.width - PANEL_WIDTH) / 2;
    }

    private int panelTop() {
        return Math.max(24, (this.height - PANEL_HEIGHT) / 2);
    }

    private int removeTagX() {
        return PANEL_WIDTH - PADDING - DONE_TAG_WIDTH - 8 - REMOVE_TAG_WIDTH;
    }

    private int doneTagX() {
        return PANEL_WIDTH - PADDING - DONE_TAG_WIDTH;
    }

    private int tagY() {
        return PANEL_HEIGHT - 30;
    }
}
