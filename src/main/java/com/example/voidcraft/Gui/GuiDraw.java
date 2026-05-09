package com.example.voidcraft.Gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

import java.util.List;

public final class GuiDraw {
    public static final int SLOT_SIZE = 18;
    public static final int DOT_SIZE = 5;

    private GuiDraw() {
    }

    /*
     * 最常用的机器 GUI 画法：
     * 1. renderBg 里先 drawBg，再 drawPanel。
     * 2. 如果顶部要分出标题栏，就 drawPanelLine。
     * 3. 有玩家背包就 drawInv。
     * 4. 有槽位就 drawSlot 或 drawSlots。
     * 5. renderLabels 里画标题、状态点、标签页和文字。
     *
     * 坐标规则：
     * - ContainerScreen 里的槽位坐标要加 leftPos/topPos。
     * - renderLabels 里的文字坐标不用加 leftPos/topPos，因为它已经是 GUI 内部坐标。
     * - 普通 Screen 没有 leftPos/topPos，就直接用自己算出来的 panelLeft/panelTop。
     */

    // 画整个 GUI 的暗色底板。一般在 renderBg 的第一步调用。
    public static void drawBg(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, GuiStyle.BG);
    }

    // 画机器主面板。x/y 是面板左上角，width/height 是面板尺寸。
    public static void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, GuiStyle.PANEL);
        guiGraphics.renderOutline(x, y, width, height, GuiStyle.LINE);
    }

    // 给面板画一条横向分割线。lineY 是相对面板顶部的 y，不是屏幕绝对坐标。
    public static void drawPanelLine(GuiGraphics guiGraphics, int panelX, int panelY, int width, int lineY) {
        guiGraphics.fill(panelX, panelY + lineY, panelX + width, panelY + lineY + 1, GuiStyle.LINE_SOFT);
    }

    // 画玩家背包背后的暗色区域。x/y 是区域左上角，width/height 是区域尺寸。
    public static void drawInv(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, GuiStyle.INV);
        guiGraphics.renderOutline(x, y, width, height, GuiStyle.LINE_BOX);
    }

    // 画普通小区域，比如连接列表、状态列表、说明列表。
    public static void drawBox(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, GuiStyle.BOX);
        guiGraphics.renderOutline(x, y, width, height, GuiStyle.LINE_BOX);
    }

    // 画一个 18x18 的槽位底框。slotX/slotY 直接传 Slot 的 x/y 加上 leftPos/topPos。
    public static void drawSlot(GuiGraphics guiGraphics, int slotX, int slotY) {
        drawSlot(guiGraphics, slotX, slotY, GuiStyle.LINE_SLOT);
    }

    // 画一个带自定义描边颜色的槽位底框。结果槽、核心槽这些可以传不同颜色。
    public static void drawSlot(GuiGraphics guiGraphics, int slotX, int slotY, int lineColor) {
        guiGraphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, GuiStyle.SLOT);
        guiGraphics.renderOutline(slotX - 1, slotY - 1, SLOT_SIZE, SLOT_SIZE, lineColor);
    }

    // 一次画完整菜单里的所有可见槽位。left/top 传 Screen 的 leftPos/topPos。
    public static void drawSlots(GuiGraphics guiGraphics, List<Slot> slots, int left, int top) {
        for (Slot slot : slots) {
            if (slot.isActive()) {
                drawSlot(guiGraphics, left + slot.x, top + slot.y);
            }
        }
    }

    // 画状态点。x/y 是状态点左上角，color 一般用 GuiStyle.TEXT_OK 或 TEXT_BAD。
    public static void drawDot(GuiGraphics guiGraphics, int x, int y, int color) {
        guiGraphics.fill(x, y, x + DOT_SIZE, y + DOT_SIZE, color);
    }

    // 画右对齐状态文字，文字左边自动带一个状态点。rightX 是文字区域右边界。
    public static void drawStatus(GuiGraphics guiGraphics, Font font, Component text, int rightX, int y, int color) {
        int textWidth = font.width(text);
        int textX = rightX - textWidth;
        drawDot(guiGraphics, textX - 8, y + 3, color);
        guiGraphics.drawString(font, text, textX, y, color, false);
    }

    // 画扁平标签页。selected 是当前页，hovered 是鼠标是否在这个标签上。
    public static void drawTab(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height, Component text, boolean selected, boolean hovered) {
        drawTab(guiGraphics, font, x, y, width, height, text, selected, hovered, GuiStyle.ACCENT);
    }

    // 画扁平标签页，并允许传入自己的强调色。
    public static void drawTab(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height, Component text, boolean selected, boolean hovered, int accentColor) {
        int color = selected ? 0xFFB9F4FF : hovered ? 0xFFD8E8F6 : 0xFF8290A0;
        int background = selected ? GuiStyle.TAB_ON : hovered ? GuiStyle.TAB_HOVER : GuiStyle.TAB;
        guiGraphics.fill(x, y, x + width, y + height, background);
        guiGraphics.drawCenteredString(font, text, x + width / 2, y + Math.max(3, (height - 8) / 2), color);
        if (selected) {
            guiGraphics.fill(x + 5, y + height - 2, x + width - 5, y + height, accentColor);
        }
    }

    // 画一个分组标题，下面带一条细线。适合“状态”“输入”“缓存”这类小标题。
    public static void drawSection(GuiGraphics guiGraphics, Font font, int x, int y, int width, Component text) {
        guiGraphics.drawString(font, text, x, y, GuiStyle.TEXT_HEAD, false);
        guiGraphics.fill(x, y + 11, x + width, y + 12, 0x33283844);
    }

    // 画单行文字，超出 width 会自动截断成 ...。
    public static void drawLine(GuiGraphics guiGraphics, Font font, int x, int y, Component text, int width) {
        drawLine(guiGraphics, font, x, y, text, GuiStyle.TEXT, width);
    }

    // 画带自定义颜色的单行文字，超出 width 会自动截断成 ...。
    public static void drawLine(GuiGraphics guiGraphics, Font font, int x, int y, Component text, int color, int width) {
        guiGraphics.drawString(font, Component.literal(clip(font, text.getString(), width)), x, y, color, false);
    }

    // 画一个向右的箭头。x/y 是箭头左边中心线的位置。
    public static void drawArrow(GuiGraphics guiGraphics, int x, int y, int color) {
        guiGraphics.fill(x, y, x + 22, y + 2, color);
        guiGraphics.fill(x + 21, y - 2, x + 26, y + 4, color);
        guiGraphics.fill(x + 24, y - 4, x + 29, y + 6, color);
    }

    // 判断鼠标是否在一个矩形里。x/y/width/height 和 fill 的坐标习惯一致。
    public static boolean inRect(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x
                && mouseY >= y
                && mouseX < x + width
                && mouseY < y + height;
    }

    // 把太长的文本截短，避免小 GUI 里文字顶出面板。
    public static String clip(Font font, String text, int width) {
        if (font.width(text) <= width) {
            return text;
        }
        return font.plainSubstrByWidth(text, width - font.width("...")) + "...";
    }
}
