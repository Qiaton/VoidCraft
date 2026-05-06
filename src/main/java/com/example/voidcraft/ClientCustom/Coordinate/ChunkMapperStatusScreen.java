package com.example.voidcraft.ClientCustom.Coordinate;

import com.example.voidcraft.Block.ChunkMapperBlock;
import com.example.voidcraft.network.ChunkMapperStatusPayload;
import com.example.voidcraft.network.ModNetworking;
import com.example.voidcraft.network.SetChunkMapperTierPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ChunkMapperStatusScreen extends Screen {
    private static final int PANEL_WIDTH = 460;
    private static final int PANEL_HEIGHT = 220;
    private static final int LEFT_X = 12;
    private static final int RIGHT_X = 250;

    private final ChunkMapperStatusPayload payload;

    private ChunkMapperStatusScreen(ChunkMapperStatusPayload payload) {
        super(Component.translatable("screen.void_craft.chunk_mapper_status"));
        this.payload = payload;
    }

    public static void open(ChunkMapperStatusPayload payload) {
        Minecraft.getInstance().setScreen(new ChunkMapperStatusScreen(payload));
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int top = panelTop();

        for (int tier = 0; tier <= ChunkMapperBlock.MAX_TIER; tier++) {
            int targetTier = tier;
            Button tierButton = Button.builder(
                    Component.translatable("button.void_craft.chunk_mapper_status.tier", ChunkMapperBlock.getTierDisplayName(tier)),
                    button -> setTier(targetTier)
            ).bounds(left + LEFT_X + tier * 54, top + 166, 50, 20).build();
            tierButton.active = tier != this.payload.tier();
            this.addRenderableWidget(tierButton);
        }

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
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, top + 12, 0xFFFFFFFF);
        guiGraphics.drawString(this.font, Component.literal(this.payload.owner().shortText()), left + LEFT_X, top + 30, 0xFFA7D7FF, false);

        drawLine(guiGraphics, left + LEFT_X, top + 56, Component.translatable("screen.void_craft.chunk_mapper_status.tier", tierName()));
        drawLine(guiGraphics, left + LEFT_X, top + 72, Component.translatable("screen.void_craft.chunk_mapper_status.radius", this.payload.radius()));
        drawLine(guiGraphics, left + LEFT_X, top + 88, Component.translatable("screen.void_craft.chunk_mapper_status.coverage", this.payload.coverageSize(), this.payload.coverageSize()));
        drawLine(guiGraphics, left + LEFT_X, top + 104, Component.translatable("screen.void_craft.chunk_mapper_status.cost", this.payload.energyCostPerTick()));
        drawLine(guiGraphics, left + LEFT_X, top + 120, Component.translatable("screen.void_craft.chunk_mapper_status.energy", this.payload.energyStored(), this.payload.energyCapacity()));

        int statusColor = this.payload.running() ? 0xFF79FF72 : 0xFFFF6565;
        guiGraphics.drawString(
                this.font,
                Component.translatable(this.payload.running()
                        ? "screen.void_craft.chunk_mapper_status.running"
                        : "screen.void_craft.chunk_mapper_status.stopped"),
                left + LEFT_X,
                top + 136,
                statusColor,
                false
        );
        drawLine(guiGraphics, left + LEFT_X, top + 152, Component.translatable("screen.void_craft.chunk_mapper_status.tier_select"));

        drawLine(guiGraphics, left + RIGHT_X, top + 56, Component.translatable("screen.void_craft.chunk_mapper_status.input"));
        if (this.payload.inputSource() == null) {
            drawLine(guiGraphics, left + RIGHT_X, top + 76, Component.translatable("screen.void_craft.chunk_mapper_status.input_empty"), 0xFFA0A0A0);
        } else {
            drawLine(guiGraphics, left + RIGHT_X, top + 76, Component.translatable("screen.void_craft.chunk_mapper_status.input_name", this.payload.inputName()));
            drawLine(guiGraphics, left + RIGHT_X, top + 92, Component.translatable("screen.void_craft.chunk_mapper_status.input_dimension", this.payload.inputSource().dimension().toString()));
            drawLine(guiGraphics, left + RIGHT_X, top + 108, Component.translatable(
                    "screen.void_craft.chunk_mapper_status.input_pos",
                    this.payload.inputSource().pos().getX(),
                    this.payload.inputSource().pos().getY(),
                    this.payload.inputSource().pos().getZ()
            ));
            drawLine(guiGraphics, left + RIGHT_X, top + 124, Component.translatable(this.payload.inputStatus().translationKey()));
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawLine(GuiGraphics guiGraphics, int x, int y, Component text) {
        drawLine(guiGraphics, x, y, text, 0xFFDADADA);
    }

    private void drawLine(GuiGraphics guiGraphics, int x, int y, Component text, int color) {
        guiGraphics.drawString(this.font, text, x, y, color, false);
    }

    private String tierName() {
        return ChunkMapperBlock.getTierDisplayName(this.payload.tier());
    }

    private void setTier(int tier) {
        if (tier == this.payload.tier()) {
            return;
        }
        ModNetworking.sendToServer(new SetChunkMapperTierPayload(this.payload.owner(), tier));
    }

    private int panelLeft() {
        return (this.width - PANEL_WIDTH) / 2;
    }

    private int panelTop() {
        return Math.max(24, (this.height - PANEL_HEIGHT) / 2);
    }
}
