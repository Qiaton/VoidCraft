package com.example.voidcraft.Gui;

import com.example.voidcraft.Custom.Behavior.Energy.VoidEnergyBindingType;
import com.example.voidcraft.Network.CoordinateBindingsPayload;
import com.example.voidcraft.Network.ModNetworking;
import com.example.voidcraft.Network.RemoveCoordinateBindingPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class CoordinateBindingScreen extends Screen {
    private static final int PANEL_WIDTH = 460;
    private static final int PANEL_HEIGHT = 220;

    private static final int LIST_WIDTH = 286;
    private static final int LIST_TOP_OFFSET = 52;
    private static final int LIST_HEIGHT = 132;

    private static final int ROW_HEIGHT = 24;
    private static final int ENTRY_HEIGHT = 20;

    private static final int SCROLLBAR_WIDTH = 4;

    private final CoordinateBindingsPayload payload;

    private int selectedIndex = -1;
    private int scrollIndex = 0;

    private Button removeButton;

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

        int left = panelLeft();
        int top = panelTop();

        this.removeButton = Button.builder(
                Component.translatable("button.void_craft.coordinate_bindings.remove"),
                button -> removeSelected()
        ).bounds(left + PANEL_WIDTH - 164, top + PANEL_HEIGHT - 32, 72, 20).build();

        this.addRenderableWidget(this.removeButton);
        updateRemoveButton();

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                button -> this.onClose()
        ).bounds(left + PANEL_WIDTH - 84, top + PANEL_HEIGHT - 32, 72, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int left = panelLeft();
        int top = panelTop();

        guiGraphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xE0101010);

        guiGraphics.drawCenteredString(
                this.font,
                this.title,
                this.width / 2,
                top + 12,
                0xFFFFFFFF
        );

        guiGraphics.drawString(
                this.font,
                Component.literal(this.payload.owner().shortText()),
                left + 12,
                top + 30,
                0xFFA7D7FF,
                false
        );

        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.void_craft.coordinate_bindings.entries"),
                left + 12,
                top + 42,
                0xFFDADADA,
                false
        );

        if (this.payload.entries().isEmpty()) {
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("screen.void_craft.coordinate_bindings.empty"),
                    listX() + 4,
                    listY() + 6,
                    0xFFDADADA,
                    false
            );
        } else {
            renderEntryList(guiGraphics, mouseX, mouseY);
        }

        CoordinateBindingsPayload.Entry selected = selectedEntry();
        renderSelectedEntry(guiGraphics, selected, left + LIST_WIDTH + 28, top + 42);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        int entryIndex = entryIndexAt(mouseX, mouseY);

        if (entryIndex != -1) {
            this.selectedIndex = entryIndex;
            updateRemoveButton();

            GuiDraw.playClick();
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (GuiDraw.inRect(mouseX, mouseY, listX(), listY(), LIST_WIDTH, LIST_HEIGHT)) {
            if (scrollY < 0.0D) {
                scrollBy(1);
            } else if (scrollY > 0.0D) {
                scrollBy(-1);
            }

            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void renderEntryList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = listX();
        int y = listY();

        guiGraphics.fill(
                x - 1,
                y - 1,
                x + LIST_WIDTH + 1,
                y + LIST_HEIGHT + 1,
                0x55000000
        );

        guiGraphics.enableScissor(x, y, x + LIST_WIDTH, y + LIST_HEIGHT);

        int rows = visibleRows();

        for (int row = 0; row < rows; row++) {
            int entryIndex = this.scrollIndex + row;

            if (entryIndex >= this.payload.entries().size()) {
                break;
            }

            CoordinateBindingsPayload.Entry entry = this.payload.entries().get(entryIndex);

            int rowX = x;
            int rowY = y + row * ROW_HEIGHT;

            boolean hovered = GuiDraw.inRect(mouseX, mouseY, rowX, rowY, LIST_WIDTH, ENTRY_HEIGHT);
            boolean selected = this.selectedIndex == entryIndex;

            if (selected) {
                guiGraphics.fill(
                        rowX,
                        rowY,
                        rowX + LIST_WIDTH,
                        rowY + ENTRY_HEIGHT,
                        0x663A8DFF
                );
            } else if (hovered) {
                guiGraphics.fill(
                        rowX,
                        rowY,
                        rowX + LIST_WIDTH,
                        rowY + ENTRY_HEIGHT,
                        0x332A6CBA
                );
            }

            int textColor = selected ? 0xFFFFFFFF : hovered ? 0xFFDDEEFF : 0xFFBFC7D5;

            guiGraphics.drawString(
                    this.font,
                    entryLabel(entry),
                    rowX + 4,
                    rowY + 6,
                    textColor,
                    false
            );
        }

        guiGraphics.disableScissor();

        renderScrollbar(guiGraphics);
    }

    private void renderScrollbar(GuiGraphics guiGraphics) {
        int total = this.payload.entries().size();
        int visible = visibleRows();

        if (total <= visible) {
            return;
        }

        int trackX = listX() + LIST_WIDTH + 4;
        int trackY = listY();
        int trackHeight = LIST_HEIGHT;

        guiGraphics.fill(
                trackX,
                trackY,
                trackX + SCROLLBAR_WIDTH,
                trackY + trackHeight,
                0x66000000
        );

        int maxScroll = maxScrollIndex();

        int thumbHeight = Math.max(16, trackHeight * visible / total);
        int availableHeight = trackHeight - thumbHeight;

        int thumbY = trackY;

        if (maxScroll > 0) {
            thumbY = trackY + availableHeight * this.scrollIndex / maxScroll;
        }

        guiGraphics.fill(
                trackX,
                thumbY,
                trackX + SCROLLBAR_WIDTH,
                thumbY + thumbHeight,
                0xFF8AB4FF
        );
    }

    private int entryIndexAt(double mouseX, double mouseY) {
        int x = listX();
        int y = listY();

        if (!GuiDraw.inRect(mouseX, mouseY, x, y, LIST_WIDTH, LIST_HEIGHT)) {
            return -1;
        }

        int row = (int) ((mouseY - y) / ROW_HEIGHT);

        if (row < 0 || row >= visibleRows()) {
            return -1;
        }

        int entryIndex = this.scrollIndex + row;

        if (entryIndex < 0 || entryIndex >= this.payload.entries().size()) {
            return -1;
        }

        return entryIndex;
    }

    private void scrollBy(int rows) {
        this.scrollIndex = Mth.clamp(
                this.scrollIndex + rows,
                0,
                maxScrollIndex()
        );
    }

    private void clampScrollIndex() {
        this.scrollIndex = Mth.clamp(
                this.scrollIndex,
                0,
                maxScrollIndex()
        );
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

    private int listX() {
        return panelLeft() + 12;
    }

    private int listY() {
        return panelTop() + LIST_TOP_OFFSET;
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

    private void updateRemoveButton() {
        if (this.removeButton != null) {
            this.removeButton.active = selectedEntry() != null;
        }
    }

    private CoordinateBindingsPayload.Entry selectedEntry() {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.payload.entries().size()) {
            return null;
        }

        return this.payload.entries().get(this.selectedIndex);
    }

    private void renderSelectedEntry(GuiGraphics guiGraphics, CoordinateBindingsPayload.Entry entry, int x, int y) {
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.void_craft.coordinate_bindings.selected"),
                x,
                y,
                0xFFDADADA,
                false
        );

        if (entry == null) {
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("screen.void_craft.coordinate_bindings.select_hint"),
                    x,
                    y + 18,
                    0xFFA0A0A0,
                    false
            );
            return;
        }

        int color = entry.type() == VoidEnergyBindingType.OUTPUT ? 0xFF79FF72 : 0xFFFF6565;

        Component side = Component.translatable(entry.outputList()
                ? "screen.void_craft.coordinate_bindings.output"
                : "screen.void_craft.coordinate_bindings.input");

        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.void_craft.coordinate_bindings.target_name", entry.targetName()),
                x,
                y + 18,
                color,
                false
        );

        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.void_craft.coordinate_bindings.target_dimension", entry.target().dimension().toString()),
                x,
                y + 32,
                0xFFD0D0D0,
                false
        );

        guiGraphics.drawString(
                this.font,
                Component.translatable(
                        "screen.void_craft.coordinate_bindings.target_pos",
                        entry.target().pos().getX(),
                        entry.target().pos().getY(),
                        entry.target().pos().getZ()
                ),
                x,
                y + 46,
                0xFFD0D0D0,
                false
        );

        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.void_craft.coordinate_bindings.target_port", side, entry.type().getDisplayName()),
                x,
                y + 60,
                0xFFD0D0D0,
                false
        );

        guiGraphics.drawString(
                this.font,
                Component.translatable(entry.status().translationKey()),
                x,
                y + 74,
                0xFFD0D0D0,
                false
        );
    }

    private Component entryLabel(CoordinateBindingsPayload.Entry entry) {
        Component side = Component.translatable(entry.outputList()
                ? "screen.void_craft.coordinate_bindings.output"
                : "screen.void_craft.coordinate_bindings.input");

        return Component.translatable(
                "screen.void_craft.coordinate_bindings.entry_label",
                side,
                entry.targetName(),
                entry.target().dimension().toString(),
                entry.target().pos().getX() + ", " + entry.target().pos().getY() + ", " + entry.target().pos().getZ()
        );
    }
}
