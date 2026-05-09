package com.example.voidcraft.Gui;

import com.example.voidcraft.Block.Block.ChunkMapperBlock;
import com.example.voidcraft.Network.ChunkMapperStatusPayload;
import com.example.voidcraft.Network.ModNetworking;
import com.example.voidcraft.Network.SetChunkMapperTierPayload;
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

        GuiDraw.drawBg(guiGraphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);
        GuiDraw.drawPanel(guiGraphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);
        GuiDraw.drawPanelLine(guiGraphics, left, top, PANEL_WIDTH, 39);

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
            if (GuiDraw.inRect(event.x(), event.y(), x, y, TIER_TAG_WIDTH, TIER_TAG_HEIGHT)) {
                if (tier != this.payload.tier()) {
                    setTier(tier);
                    AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
                }
                return true;
            }
        }

        if (GuiDraw.inRect(event.x(), event.y(), closeTagX(left), closeTagY(top), CLOSE_TAG_WIDTH, CLOSE_TAG_HEIGHT)) {
            AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
            this.onClose();
            return true;
        }

        return super.mouseClicked(event, isDoubleClick);
    }

    private void renderHeader(GuiGraphics guiGraphics, int left, int top) {
        guiGraphics.drawString(this.font, this.title, left + PANEL_PADDING, top + 9, GuiStyle.TEXT_TITLE, false);
        GuiDraw.drawLine(guiGraphics, this.font, left + PANEL_PADDING, top + 27, Component.literal(this.payload.owner().shortText()), GuiStyle.TEXT_MID, PANEL_WIDTH - PANEL_PADDING * 2);

        Component status = Component.translatable(this.payload.running()
                ? "screen.void_craft.chunk_mapper_status.running"
                : "screen.void_craft.chunk_mapper_status.stopped");
        int statusColor = this.payload.running() ? GuiStyle.TEXT_OK : GuiStyle.TEXT_BAD;
        GuiDraw.drawStatus(guiGraphics, this.font, status, left + PANEL_WIDTH - PANEL_PADDING, top + 9, statusColor);
    }

    private void renderMapperStats(GuiGraphics guiGraphics, int left, int top) {
        GuiDraw.drawSection(guiGraphics, this.font, left + LEFT_X, top + 54, COLUMN_WIDTH, Component.translatable("screen.void_craft.chunk_mapper_status.stats"));
        GuiDraw.drawLine(guiGraphics, this.font, left + LEFT_X, top + 72, Component.translatable("screen.void_craft.chunk_mapper_status.tier", tierName()), COLUMN_WIDTH);
        GuiDraw.drawLine(guiGraphics, this.font, left + LEFT_X, top + 88, Component.translatable("screen.void_craft.chunk_mapper_status.radius", this.payload.radius()), COLUMN_WIDTH);
        GuiDraw.drawLine(guiGraphics, this.font, left + LEFT_X, top + 104, Component.translatable("screen.void_craft.chunk_mapper_status.coverage", this.payload.coverageSize(), this.payload.coverageSize()), COLUMN_WIDTH);
        GuiDraw.drawLine(guiGraphics, this.font, left + LEFT_X, top + 120, Component.translatable("screen.void_craft.chunk_mapper_status.cost", this.payload.energyCostPerTick()), COLUMN_WIDTH);
        GuiDraw.drawLine(guiGraphics, this.font, left + LEFT_X, top + 136, Component.translatable("screen.void_craft.chunk_mapper_status.energy", this.payload.energyStored(), this.payload.energyCapacity()), COLUMN_WIDTH);

        GuiDraw.drawSection(guiGraphics, this.font, left + LEFT_X, top + 152, COLUMN_WIDTH, Component.translatable("screen.void_craft.chunk_mapper_status.tier_select"));
    }

    private void renderInputInfo(GuiGraphics guiGraphics, int left, int top) {
        // 输入源为空时显示空状态；有输入源时显示方块名、维度、坐标和绑定状态。
        GuiDraw.drawSection(guiGraphics, this.font, left + RIGHT_X, top + 54, COLUMN_WIDTH, Component.translatable("screen.void_craft.chunk_mapper_status.input"));
        if (this.payload.inputSource() == null) {
            GuiDraw.drawLine(guiGraphics, this.font, left + RIGHT_X, top + 76, Component.translatable("screen.void_craft.chunk_mapper_status.input_empty"), GuiStyle.TEXT_MUTED, COLUMN_WIDTH);
        } else {
            GuiDraw.drawLine(guiGraphics, this.font, left + RIGHT_X, top + 76, Component.translatable("screen.void_craft.chunk_mapper_status.input_name", this.payload.inputName()), COLUMN_WIDTH);
            GuiDraw.drawLine(guiGraphics, this.font, left + RIGHT_X, top + 92, Component.translatable("screen.void_craft.chunk_mapper_status.input_dimension", this.payload.inputSource().dimension().toString()), COLUMN_WIDTH);
            GuiDraw.drawLine(guiGraphics, this.font, left + RIGHT_X, top + 108, Component.translatable(
                    "screen.void_craft.chunk_mapper_status.input_pos",
                    this.payload.inputSource().pos().getX(),
                    this.payload.inputSource().pos().getY(),
                    this.payload.inputSource().pos().getZ()
            ), COLUMN_WIDTH);
            GuiDraw.drawLine(guiGraphics, this.font, left + RIGHT_X, top + 124, Component.translatable(this.payload.inputStatus().translationKey()), COLUMN_WIDTH);
        }
    }

    private void renderTierTags(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        for (int tier = 0; tier <= ChunkMapperBlock.MAX_TIER; tier++) {
            int x = tierTagX(left, tier);
            int y = top + TIER_TAG_Y;
            boolean selected = tier == this.payload.tier();
            boolean hovered = GuiDraw.inRect(mouseX, mouseY, x, y, TIER_TAG_WIDTH, TIER_TAG_HEIGHT);
            Component text = Component.translatable("button.void_craft.chunk_mapper_status.tier", ChunkMapperBlock.getTierDisplayName(tier));
            GuiDraw.drawTab(guiGraphics, this.font, x, y, TIER_TAG_WIDTH, TIER_TAG_HEIGHT, text, selected, hovered);
        }
    }

    private void renderCloseTag(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        int x = closeTagX(left);
        int y = closeTagY(top);
        boolean hovered = GuiDraw.inRect(mouseX, mouseY, x, y, CLOSE_TAG_WIDTH, CLOSE_TAG_HEIGHT);
        GuiDraw.drawTab(guiGraphics, this.font, x, y, CLOSE_TAG_WIDTH, CLOSE_TAG_HEIGHT, Component.translatable("gui.done"), false, hovered, GuiStyle.TEXT_MUTED);
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

}
