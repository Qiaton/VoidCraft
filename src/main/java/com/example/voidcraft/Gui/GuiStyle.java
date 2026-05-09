package com.example.voidcraft.Gui;

public final class GuiStyle {
    // 整个 GUI 的底色，先画它，再画面板。
    public static final int BG = 0xE00B0E12;
    // 机器主面板颜色。
    public static final int PANEL = 0xF014181D;
    // 普通列表或小区域的底色。
    public static final int BOX = 0x66090D11;
    // 玩家背包区域底色。
    public static final int INV = 0xAA11161B;
    // 槽位底色。
    public static final int SLOT = 0xFF0A0D10;

    // 面板描边和分割线。
    public static final int LINE = 0x663A5368;
    public static final int LINE_SOFT = 0x44334757;
    public static final int LINE_BOX = 0x333A5368;
    public static final int LINE_SLOT = 0x774B6577;

    // 标签页颜色。
    public static final int TAB = 0x22181F26;
    public static final int TAB_HOVER = 0x33202B33;
    public static final int TAB_ON = 0x552A4855;

    // 文字颜色。
    public static final int TEXT_TITLE = 0xFFEAF4FF;
    public static final int TEXT = 0xFFEAF4FF;
    public static final int TEXT_HEAD = 0xFFDDEEFF;
    public static final int TEXT_DIM = 0xFF8F9BA8;
    public static final int TEXT_MID = 0xFF9FC7D6;
    public static final int TEXT_MUTED = 0xFF7C8792;
    public static final int TEXT_OK = 0xFF89F6B4;
    public static final int TEXT_BAD = 0xFFFF8A8A;

    // 青蓝色强调线，常用于选中标签页。
    public static final int ACCENT = 0xFF62D6E8;

    private GuiStyle() {
    }
}
