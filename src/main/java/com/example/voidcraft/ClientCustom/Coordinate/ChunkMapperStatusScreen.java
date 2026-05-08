package com.example.voidcraft.ClientCustom.Coordinate;

import com.example.voidcraft.Block.ChunkMapperBlock;
import com.example.voidcraft.network.ChunkMapperStatusPayload;
import com.example.voidcraft.network.ModNetworking;
import com.example.voidcraft.network.SetChunkMapperTierPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class ChunkMapperStatusScreen extends Screen {
    private static final int PANEL_WIDTH = 460;
    private static final int PANEL_HEIGHT = 220;
    private static final int PANEL_PADDING = 12;
    private static final int LEFT_X = 16;
    private static final int RIGHT_X = 248;
    private static final int COLUMN_WIDTH = 196;
    private static final int TIER_TAG_Y = 164;
    private static final int TIER_TAG_WIDTH = 50;
    private static final int TIER_TAG_HEIGHT = 18;
    private static final int TIER_TAG_GAP = 6;
    private static final int CLOSE_TAG_WIDTH = 48;
    private static final int CLOSE_TAG_HEIGHT = 18;

    private final ChunkMapperStatusPayload payload;

    private ChunkMapperStatusScreen(ChunkMapperStatusPayload payload) {
        super(Component.translatable("screen.void_craft.chunk_mapper_status"));
        this.payload = payload;
    }

    public static void open(ChunkMapperStatusPayload payload) {
        Minecraft.getInstance().setScreen(new ChunkMapperStatusScreen(payload));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 这个面板展示服务端快照；档位变化后等服务端回包刷新。
        int left = panelLeft();
        int top = panelTop();

        guiGraphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xE00B0E12);
        guiGraphics.fill(left, top, left + PANEL_WIDTH, top + 40, 0xF014181D);
        guiGraphics.renderOutline(left, top, PANEL_WIDTH, PANEL_HEIGHT, 0x663A5368);
        guiGraphics.fill(left, top + 39, left + PANEL_WIDTH, top + 40, 0x44283844);

        renderHeader(guiGraphics, left, top);
        renderMapperStats(guiGraphics, left, top);
        renderInputInfo(guiGraphics, left, top);
        renderTierTags(guiGraphics, left, top, mouseX, mouseY);
        renderCloseTag(guiGraphics, left, top, mouseX, mouseY);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        int left = panelLeft();
        int top = panelTop();

        // 档位标签是自绘按钮，点击后只发请求，真正修改由服务端完成。
        for (int tier = 0; tier <= ChunkMapperBlock.MAX_TIER; tier++) {
            int x = tierTagX(left, tier);
            int y = top + TIER_TAG_Y;
            if (isInRect(event.x(), event.y(), x, y, TIER_TAG_WIDTH, TIER_TAG_HEIGHT)) {
                if (tier != this.payload.tier()) {
                    setTier(tier);
                    AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
                }
                return true;
            }
        }

        if (isInRect(event.x(), event.y(), closeTagX(left), closeTagY(top), CLOSE_TAG_WIDTH, CLOSE_TAG_HEIGHT)) {
            AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
            this.onClose();
            return true;
        }

        return super.mouseClicked(event, isDoubleClick);
    }

    private void renderHeader(GuiGraphics guiGraphics, int left, int top) {
        guiGraphics.drawString(this.font, this.title, left + PANEL_PADDING, top + 9, 0xFFEAF4FF, false);
        drawClippedLine(guiGraphics, left + PANEL_PADDING, top + 27, Component.literal(this.payload.owner().shortText()), 0xFF9FC7D6, PANEL_WIDTH - PANEL_PADDING * 2);

        Component status = Component.translatable(this.payload.running()
                ? "screen.void_craft.chunk_mapper_status.running"
                : "screen.void_craft.chunk_mapper_status.stopped");
        int statusColor = this.payload.running() ? 0xFF89F6B4 : 0xFFFF8A8A;
        int textWidth = this.font.width(status);
        int textX = left + PANEL_WIDTH - PANEL_PADDING - textWidth;
        guiGraphics.fill(textX - 8, top + 12, textX - 4, top + 16, statusColor);
        guiGraphics.drawString(this.font, status, textX, top + 9, statusColor, false);
    }

    private void renderMapperStats(GuiGraphics guiGraphics, int left, int top) {
        drawSectionTitle(guiGraphics, left + LEFT_X, top + 54, Component.translatable("screen.void_craft.chunk_mapper_status.stats"));
        drawClippedLine(guiGraphics, left + LEFT_X, top + 72, Component.translatable("screen.void_craft.chunk_mapper_status.tier", tierName()), COLUMN_WIDTH);
        drawClippedLine(guiGraphics, left + LEFT_X, top + 88, Component.translatable("screen.void_craft.chunk_mapper_status.radius", this.payload.radius()), COLUMN_WIDTH);
        drawClippedLine(guiGraphics, left + LEFT_X, top + 104, Component.translatable("screen.void_craft.chunk_mapper_status.coverage", this.payload.coverageSize(), this.payload.coverageSize()), COLUMN_WIDTH);
        drawClippedLine(guiGraphics, left + LEFT_X, top + 120, Component.translatable("screen.void_craft.chunk_mapper_status.cost", this.payload.energyCostPerTick()), COLUMN_WIDTH);
        drawClippedLine(guiGraphics, left + LEFT_X, top + 136, Component.translatable("screen.void_craft.chunk_mapper_status.energy", this.payload.energyStored(), this.payload.energyCapacity()), COLUMN_WIDTH);

        drawSectionTitle(guiGraphics, left + LEFT_X, top + 152, Component.translatable("screen.void_craft.chunk_mapper_status.tier_select"));
    }

    private void renderInputInfo(GuiGraphics guiGraphics, int left, int top) {
        // 输入源为空时显示空状态；有输入源时显示方块名、维度、坐标和绑定状态。
        drawSectionTitle(guiGraphics, left + RIGHT_X, top + 54, Component.translatable("screen.void_craft.chunk_mapper_status.input"));
        if (this.payload.inputSource() == null) {
            drawClippedLine(guiGraphics, left + RIGHT_X, top + 76, Component.translatable("screen.void_craft.chunk_mapper_status.input_empty"), 0xFF7C8792, COLUMN_WIDTH);
        } else {
            drawClippedLine(guiGraphics, left + RIGHT_X, top + 76, Component.translatable("screen.void_craft.chunk_mapper_status.input_name", this.payload.inputName()), COLUMN_WIDTH);
            drawClippedLine(guiGraphics, left + RIGHT_X, top + 92, Component.translatable("screen.void_craft.chunk_mapper_status.input_dimension", this.payload.inputSource().dimension().toString()), COLUMN_WIDTH);
            drawClippedLine(guiGraphics, left + RIGHT_X, top + 108, Component.translatable(
                    "screen.void_craft.chunk_mapper_status.input_pos",
                    this.payload.inputSource().pos().getX(),
                    this.payload.inputSource().pos().getY(),
                    this.payload.inputSource().pos().getZ()
            ), COLUMN_WIDTH);
            drawClippedLine(guiGraphics, left + RIGHT_X, top + 124, Component.translatable(this.payload.inputStatus().translationKey()), COLUMN_WIDTH);
        }
    }

    private void renderTierTags(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        for (int tier = 0; tier <= ChunkMapperBlock.MAX_TIER; tier++) {
            int x = tierTagX(left, tier);
            int y = top + TIER_TAG_Y;
            boolean selected = tier == this.payload.tier();
            boolean hovered = isInRect(mouseX, mouseY, x, y, TIER_TAG_WIDTH, TIER_TAG_HEIGHT);
            Component text = Component.translatable("button.void_craft.chunk_mapper_status.tier", ChunkMapperBlock.getTierDisplayName(tier));
            renderTag(guiGraphics, x, y, TIER_TAG_WIDTH, TIER_TAG_HEIGHT, text, selected, hovered, 0xFF62D6E8);
        }
    }

    private void renderCloseTag(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        int x = closeTagX(left);
        int y = closeTagY(top);
        boolean hovered = isInRect(mouseX, mouseY, x, y, CLOSE_TAG_WIDTH, CLOSE_TAG_HEIGHT);
        renderTag(guiGraphics, x, y, CLOSE_TAG_WIDTH, CLOSE_TAG_HEIGHT, Component.translatable("gui.done"), false, hovered, 0xFF7C8792);
    }

    private void renderTag(GuiGraphics guiGraphics, int x, int y, int width, int height, Component text, boolean selected, boolean hovered, int accentColor) {
        int color = selected ? 0xFFB9F4FF : hovered ? 0xFFD8E8F6 : 0xFF8290A0;
        int background = selected ? 0x552A4855 : hovered ? 0x33202B33 : 0x22181F26;
        guiGraphics.fill(x, y, x + width, y + height, background);
        guiGraphics.drawCenteredString(this.font, text, x + width / 2, y + 5, color);
        if (selected) {
            guiGraphics.fill(x + 6, y + height - 2, x + width - 6, y + height, accentColor);
        }
    }

    private void drawSectionTitle(GuiGraphics guiGraphics, int x, int y, Component text) {
        guiGraphics.drawString(this.font, text, x, y, 0xFFDDEEFF, false);
        guiGraphics.fill(x, y + 11, x + COLUMN_WIDTH, y + 12, 0x33283844);
    }

    private void drawClippedLine(GuiGraphics guiGraphics, int x, int y, Component text, int width) {
        drawClippedLine(guiGraphics, x, y, text, 0xFFEAF4FF, width);
    }

    private void drawClippedLine(GuiGraphics guiGraphics, int x, int y, Component text, int color, int width) {
        String line = clip(text.getString(), width);
        guiGraphics.drawString(this.font, Component.literal(line), x, y, color, false);
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

    private int tierTagX(int left, int tier) {
        return left + LEFT_X + tier * (TIER_TAG_WIDTH + TIER_TAG_GAP);
    }

    private int closeTagX(int left) {
        return left + PANEL_WIDTH - PANEL_PADDING - CLOSE_TAG_WIDTH;
    }

    private int closeTagY(int top) {
        return top + PANEL_HEIGHT - 30;
    }

    private boolean isInRect(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x
                && mouseY >= y
                && mouseX < x + width
                && mouseY < y + height;
    }

    private String clip(String text, int width) {
        // 维度 id 或坐标太长时截断，避免压到右侧内容。
        if (this.font.width(text) <= width) {
            return text;
        }
        return this.font.plainSubstrByWidth(text, width - this.font.width("...")) + "...";
    }
}
