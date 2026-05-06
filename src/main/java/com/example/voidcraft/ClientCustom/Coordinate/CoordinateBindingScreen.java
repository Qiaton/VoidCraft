package com.example.voidcraft.ClientCustom.Coordinate;

import com.example.voidcraft.Block.entity.VoidEnergyBindingType;
import com.example.voidcraft.network.CoordinateBindingsPayload;
import com.example.voidcraft.network.ModNetworking;
import com.example.voidcraft.network.RemoveCoordinateBindingPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CoordinateBindingScreen extends Screen {
    private static final int PANEL_WIDTH = 460;
    private static final int PANEL_HEIGHT = 220;
    private static final int LIST_WIDTH = 286;
    private static final int ROW_HEIGHT = 24;

    private final CoordinateBindingsPayload payload;
    private int selectedIndex = -1;
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
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = Math.max(24, (this.height - PANEL_HEIGHT) / 2);
        int y = top + 52;
        int index = 0;

        for (CoordinateBindingsPayload.Entry entry : this.payload.entries()) {
            int entryIndex = index;
            this.addRenderableWidget(Button.builder(
                    entryLabel(entry),
                    button -> {
                        this.selectedIndex = entryIndex;
                        updateRemoveButton();
                    }
            ).bounds(left + 12, y - 4, LIST_WIDTH, 20).build());
            y += ROW_HEIGHT;
            index++;
        }

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
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = Math.max(24, (this.height - PANEL_HEIGHT) / 2);
        guiGraphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xE0101010);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, top + 12, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.literal(this.payload.owner().shortText()), left + 12, top + 30, 0xA7D7FF);
        guiGraphics.drawString(this.font, Component.translatable("screen.void_craft.coordinate_bindings.entries"), left + 12, top + 42, 0xDADADA);

        int y = top + 52;
        if (this.payload.entries().isEmpty()) {
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("screen.void_craft.coordinate_bindings.empty"),
                    left + 12,
                    y,
                    0xDADADA
            );
        }

        CoordinateBindingsPayload.Entry selected = selectedEntry();
        renderSelectedEntry(guiGraphics, selected, left + LIST_WIDTH + 28, top + 42);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
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
        guiGraphics.drawString(this.font, Component.translatable("screen.void_craft.coordinate_bindings.selected"), x, y, 0xDADADA);
        if (entry == null) {
            guiGraphics.drawString(this.font, Component.translatable("screen.void_craft.coordinate_bindings.select_hint"), x, y + 18, 0xA0A0A0);
            return;
        }

        int color = entry.type() == VoidEnergyBindingType.OUTPUT ? 0x79FF72 : 0xFF6565;
        Component side = Component.translatable(entry.outputList()
                ? "screen.void_craft.coordinate_bindings.output"
                : "screen.void_craft.coordinate_bindings.input");
        guiGraphics.drawString(this.font, Component.translatable("screen.void_craft.coordinate_bindings.target_name", entry.targetName()), x, y + 18, color);
        guiGraphics.drawString(this.font, Component.translatable("screen.void_craft.coordinate_bindings.target_dimension", entry.target().dimension().toString()), x, y + 32, 0xD0D0D0);
        guiGraphics.drawString(this.font, Component.translatable(
                "screen.void_craft.coordinate_bindings.target_pos",
                entry.target().pos().getX(),
                entry.target().pos().getY(),
                entry.target().pos().getZ()
        ), x, y + 46, 0xD0D0D0);
        guiGraphics.drawString(this.font, Component.translatable("screen.void_craft.coordinate_bindings.target_port", side, entry.type().getDisplayName()), x, y + 60, 0xD0D0D0);
        guiGraphics.drawString(this.font, Component.translatable(entry.status().translationKey()), x, y + 74, 0xD0D0D0);
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
