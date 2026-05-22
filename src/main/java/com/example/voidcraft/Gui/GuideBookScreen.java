package com.example.voidcraft.Gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class GuideBookScreen extends AbstractContainerScreen<GuideBookMenu> {
    private static final int PANEL_WIDTH = 330;
    private static final int PANEL_HEIGHT = 220;
    private static final int LEFT_WIDTH = 94;
    private static final int PADDING = 10;
    private static final int HEADER_HEIGHT = 35;
    private static final int ROW_HEIGHT = 18;
    private static final int CONTENT_TOP = 46;
    private static final int CONTENT_WIDTH = PANEL_WIDTH - LEFT_WIDTH - PADDING * 3;
    private static final int CONTENT_HEIGHT = PANEL_HEIGHT - CONTENT_TOP - 30;

    private static final List<Page> PAGES = List.of(
            new Page("screen.void_craft.guide_book.page.start", List.of(
                    "screen.void_craft.guide_book.start.1",
                    "screen.void_craft.guide_book.start.2",
                    "screen.void_craft.guide_book.start.3",
                    "screen.void_craft.guide_book.start.4",
                    "screen.void_craft.guide_book.start.5",
                    "screen.void_craft.guide_book.start.6",
                    "screen.void_craft.guide_book.start.7",
                    "screen.void_craft.guide_book.start.8"
            )),
            new Page("screen.void_craft.guide_book.page.material", List.of(
                    "screen.void_craft.guide_book.material.1",
                    "screen.void_craft.guide_book.material.2",
                    "screen.void_craft.guide_book.material.3",
                    "screen.void_craft.guide_book.material.4",
                    "screen.void_craft.guide_book.material.5",
                    "screen.void_craft.guide_book.material.6",
                    "screen.void_craft.guide_book.material.7"
            )),
            new Page("screen.void_craft.guide_book.page.machine", List.of(
                    "screen.void_craft.guide_book.machine.1",
                    "screen.void_craft.guide_book.machine.2",
                    "screen.void_craft.guide_book.machine.3",
                    "screen.void_craft.guide_book.machine.4",
                    "screen.void_craft.guide_book.machine.5",
                    "screen.void_craft.guide_book.machine.6",
                    "screen.void_craft.guide_book.machine.7",
                    "screen.void_craft.guide_book.machine.8",
                    "screen.void_craft.guide_book.machine.9"
            )),
            new Page("screen.void_craft.guide_book.page.designator", List.of(
                    "screen.void_craft.guide_book.designator.1",
                    "screen.void_craft.guide_book.designator.2",
                    "screen.void_craft.guide_book.designator.3",
                    "screen.void_craft.guide_book.designator.4",
                    "screen.void_craft.guide_book.designator.5",
                    "screen.void_craft.guide_book.designator.6"
            )),
            new Page("screen.void_craft.guide_book.page.watch", List.of(
                    "screen.void_craft.guide_book.watch.1",
                    "screen.void_craft.guide_book.watch.2",
                    "screen.void_craft.guide_book.watch.3",
                    "screen.void_craft.guide_book.watch.4",
                    "screen.void_craft.guide_book.watch.5",
                    "screen.void_craft.guide_book.watch.6",
                    "screen.void_craft.guide_book.watch.7",
                    "screen.void_craft.guide_book.watch.8"
            )),
            new Page("screen.void_craft.guide_book.page.module", List.of(
                    "screen.void_craft.guide_book.module.1",
                    "screen.void_craft.guide_book.module.2",
                    "screen.void_craft.guide_book.module.3",
                    "screen.void_craft.guide_book.module.4",
                    "screen.void_craft.guide_book.module.5",
                    "screen.void_craft.guide_book.module.6",
                    "screen.void_craft.guide_book.module.7",
                    "screen.void_craft.guide_book.module.8",
                    "screen.void_craft.guide_book.module.9"
            )),
            new Page("screen.void_craft.guide_book.page.boost", List.of(
                    "screen.void_craft.guide_book.boost.1",
                    "screen.void_craft.guide_book.boost.2",
                    "screen.void_craft.guide_book.boost.3",
                    "screen.void_craft.guide_book.boost.4",
                    "screen.void_craft.guide_book.boost.5"
            )),
            new Page("screen.void_craft.guide_book.page.phase", List.of(
                    "screen.void_craft.guide_book.phase.1",
                    "screen.void_craft.guide_book.phase.2",
                    "screen.void_craft.guide_book.phase.3",
                    "screen.void_craft.guide_book.phase.4"
            )),
            new Page("screen.void_craft.guide_book.page.other", List.of(
                    "screen.void_craft.guide_book.other.1",
                    "screen.void_craft.guide_book.other.2",
                    "screen.void_craft.guide_book.other.3",
                    "screen.void_craft.guide_book.other.4",
                    "screen.void_craft.guide_book.other.5"
            )),
            new Page("screen.void_craft.guide_book.page.faq", List.of(
                    "screen.void_craft.guide_book.faq.1",
                    "screen.void_craft.guide_book.faq.2",
                    "screen.void_craft.guide_book.faq.3",
                    "screen.void_craft.guide_book.faq.4",
                    "screen.void_craft.guide_book.faq.5",
                    "screen.void_craft.guide_book.faq.6"
            ))
    );

    private int pageIndex = 0;
    private int scrollLine = 0;
    private int listScrollIndex = 0;

    public GuideBookScreen(GuideBookMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = PANEL_WIDTH;
        this.imageHeight = PANEL_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        clampScrollLine();
        clampListScroll();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        GuiDraw.drawBg(guiGraphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
        GuiDraw.drawPanel(guiGraphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
        GuiDraw.drawPanelLine(guiGraphics, this.leftPos, this.topPos, this.imageWidth, HEADER_HEIGHT);
        GuiDraw.drawBox(guiGraphics, this.leftPos + PADDING, this.topPos + CONTENT_TOP, LEFT_WIDTH - PADDING, CONTENT_HEIGHT);
        GuiDraw.drawBox(guiGraphics, contentLeft(), this.topPos + CONTENT_TOP, CONTENT_WIDTH, CONTENT_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int localX = mouseX - this.leftPos;
        int localY = mouseY - this.topPos;

        guiGraphics.drawString(this.font, this.title, PADDING, 9, GuiStyle.TEXT_TITLE, false);
        GuiDraw.drawLine(guiGraphics, this.font, PADDING, 24, Component.translatable("screen.void_craft.guide_book.tip"), GuiStyle.TEXT_MID, PANEL_WIDTH - PADDING * 2);
        renderPageList(guiGraphics, localX, localY);
        renderPage(guiGraphics);
        renderPageButtons(guiGraphics, localX, localY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        int localX = (int) event.x() - this.leftPos;
        int localY = (int) event.y() - this.topPos;

        int clickedPage = getPageAt(localX, localY);
        if (clickedPage >= 0) {
            setPage(clickedPage);
            return true;
        }

        if (GuiDraw.inRect(localX, localY, prevX(), bottomButtonY(), 54, 18)) {
            setPage(this.pageIndex - 1);
            return true;
        }

        if (GuiDraw.inRect(localX, localY, nextX(), bottomButtonY(), 54, 18)) {
            setPage(this.pageIndex + 1);
            return true;
        }

        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (GuiDraw.inRect(mouseX, mouseY, this.leftPos + PADDING, this.topPos + CONTENT_TOP, LEFT_WIDTH - PADDING, CONTENT_HEIGHT)) {
            if (scrollY < 0.0D) {
                listScrollIndex++;
            } else if (scrollY > 0.0D) {
                listScrollIndex--;
            }
            clampListScroll();
            return true;
        }

        if (GuiDraw.inRect(mouseX, mouseY, contentLeft(), this.topPos + CONTENT_TOP, CONTENT_WIDTH, CONTENT_HEIGHT)) {
            if (scrollY < 0.0D) {
                scrollLine++;
            } else if (scrollY > 0.0D) {
                scrollLine--;
            }
            clampScrollLine();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return switch (event.key()) {
            case 262 -> {
                setPage(this.pageIndex + 1);
                yield true;
            }
            case 263 -> {
                setPage(this.pageIndex - 1);
                yield true;
            }
            case 264 -> {
                scrollLine++;
                clampScrollLine();
                yield true;
            }
            case 265 -> {
                scrollLine--;
                clampScrollLine();
                yield true;
            }
            default -> super.keyPressed(event);
        };
    }

    private void renderPageList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = PADDING + 4;
        int y = CONTENT_TOP + 5;
        int width = LEFT_WIDTH - PADDING - 8;
        int rows = listVisibleRows();

        guiGraphics.enableScissor(PADDING + 1, CONTENT_TOP + 1, PADDING + LEFT_WIDTH - PADDING - 1, CONTENT_TOP + CONTENT_HEIGHT - 1);
        for (int row = 0; row < rows; row++) {
            int page = this.listScrollIndex + row;
            if (page >= PAGES.size()) {
                break;
            }

            int rowY = y + row * ROW_HEIGHT - 2;
            boolean selected = page == this.pageIndex;
            boolean hovered = GuiDraw.inRect(mouseX, mouseY, x, rowY, width, ROW_HEIGHT);
            GuiDraw.drawTab(
                    guiGraphics,
                    this.font,
                    x,
                    rowY,
                    width,
                    ROW_HEIGHT,
                    Component.translatable(PAGES.get(page).titleKey()),
                    selected,
                    hovered
            );
        }
        guiGraphics.disableScissor();

        renderPageListScrollbar(guiGraphics);
    }

    private void renderPage(GuiGraphics guiGraphics) {
        Page page = PAGES.get(this.pageIndex);
        int x = LEFT_WIDTH + PADDING * 2;
        int y = CONTENT_TOP + 8;
        int width = CONTENT_WIDTH - 16;

        guiGraphics.drawString(this.font, Component.translatable(page.titleKey()), x, y, GuiStyle.TEXT_HEAD, false);
        guiGraphics.fill(x, y + 12, x + width, y + 13, GuiStyle.LINE_SOFT);

        int textY = y + 20;
        int line = 0;
        guiGraphics.enableScissor(x, textY, x + width, CONTENT_TOP + CONTENT_HEIGHT - 8);
        for (String lineKey : page.lineKeys()) {
            List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(Component.translatable(lineKey), width);
            for (net.minecraft.util.FormattedCharSequence text : lines) {
                if (line >= this.scrollLine) {
                    int drawY = textY + (line - this.scrollLine) * 10;
                    if (drawY < CONTENT_TOP + CONTENT_HEIGHT - 10) {
                        guiGraphics.drawString(this.font, text, x, drawY, GuiStyle.TEXT, false);
                    }
                }
                line++;
            }
            line++;
        }
        guiGraphics.disableScissor();
        renderScrollbar(guiGraphics, line, x + width + 4, textY);
    }

    private void renderScrollbar(GuiGraphics guiGraphics, int lineCount, int x, int y) {
        int visible = visibleLines();
        if (lineCount <= visible) {
            return;
        }

        int trackHeight = CONTENT_HEIGHT - 34;
        int thumbHeight = Math.max(16, trackHeight * visible / lineCount);
        int maxScroll = maxScrollLine();
        int thumbY = y + (trackHeight - thumbHeight) * this.scrollLine / Math.max(1, maxScroll);
        guiGraphics.fill(x, y, x + 3, y + trackHeight, 0x66000000);
        guiGraphics.fill(x, thumbY, x + 3, thumbY + thumbHeight, GuiStyle.ACCENT);
    }

    private void renderPageListScrollbar(GuiGraphics guiGraphics) {
        int visible = listVisibleRows();
        if (PAGES.size() <= visible) {
            return;
        }

        int trackX = PADDING + LEFT_WIDTH - PADDING - 5;
        int trackY = CONTENT_TOP + 5;
        int trackHeight = CONTENT_HEIGHT - 10;
        int thumbHeight = Math.max(16, trackHeight * visible / PAGES.size());
        int maxScroll = maxListScroll();
        int thumbY = trackY + (trackHeight - thumbHeight) * this.listScrollIndex / Math.max(1, maxScroll);
        guiGraphics.fill(trackX, trackY, trackX + 3, trackY + trackHeight, 0x66000000);
        guiGraphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, GuiStyle.ACCENT);
    }

    private void renderPageButtons(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        GuiDraw.drawTab(
                guiGraphics,
                this.font,
                prevX(),
                bottomButtonY(),
                54,
                18,
                Component.translatable("screen.void_craft.guide_book.prev"),
                false,
                GuiDraw.inRect(mouseX, mouseY, prevX(), bottomButtonY(), 54, 18),
                GuiStyle.TEXT_MUTED
        );
        GuiDraw.drawTab(
                guiGraphics,
                this.font,
                nextX(),
                bottomButtonY(),
                54,
                18,
                Component.translatable("screen.void_craft.guide_book.next"),
                false,
                GuiDraw.inRect(mouseX, mouseY, nextX(), bottomButtonY(), 54, 18),
                GuiStyle.TEXT_MUTED
        );
    }

    private int getPageAt(int mouseX, int mouseY) {
        int x = PADDING + 4;
        int y = CONTENT_TOP + 5;
        int width = LEFT_WIDTH - PADDING - 8;
        int rows = listVisibleRows();
        for (int row = 0; row < rows; row++) {
            int page = this.listScrollIndex + row;
            if (page >= PAGES.size()) {
                break;
            }
            if (GuiDraw.inRect(mouseX, mouseY, x, y + row * ROW_HEIGHT - 2, width, ROW_HEIGHT)) {
                return page;
            }
        }
        return -1;
    }

    private void setPage(int index) {
        this.pageIndex = Mth.clamp(index, 0, PAGES.size() - 1);
        this.scrollLine = 0;
        keepPageVisible();
        clampScrollLine();
    }

    private void clampScrollLine() {
        this.scrollLine = Mth.clamp(this.scrollLine, 0, maxScrollLine());
    }

    private int maxScrollLine() {
        int lineCount = getLineCount(PAGES.get(this.pageIndex));
        return Math.max(0, lineCount - visibleLines());
    }

    private int getLineCount(Page page) {
        int width = CONTENT_WIDTH - 16;
        int lineCount = 0;
        for (String lineKey : page.lineKeys()) {
            lineCount += this.font.split(Component.translatable(lineKey), width).size();
            lineCount++;
        }
        return lineCount;
    }

    private int visibleLines() {
        return Math.max(1, (CONTENT_HEIGHT - 34) / 10);
    }

    private int listVisibleRows() {
        return Math.max(1, (CONTENT_HEIGHT - 10) / ROW_HEIGHT);
    }

    private void keepPageVisible() {
        int visible = listVisibleRows();
        if (this.pageIndex < this.listScrollIndex) {
            this.listScrollIndex = this.pageIndex;
        } else if (this.pageIndex >= this.listScrollIndex + visible) {
            this.listScrollIndex = this.pageIndex - visible + 1;
        }
        clampListScroll();
    }

    private void clampListScroll() {
        this.listScrollIndex = Mth.clamp(this.listScrollIndex, 0, maxListScroll());
    }

    private int maxListScroll() {
        return Math.max(0, PAGES.size() - listVisibleRows());
    }

    private int contentLeft() {
        return this.leftPos + LEFT_WIDTH + PADDING;
    }

    private int prevX() {
        return LEFT_WIDTH + PADDING * 2;
    }

    private int nextX() {
        return PANEL_WIDTH - PADDING - 54;
    }

    private int bottomButtonY() {
        return PANEL_HEIGHT - 24;
    }

    private record Page(String titleKey, List<String> lineKeys) {
    }
}
