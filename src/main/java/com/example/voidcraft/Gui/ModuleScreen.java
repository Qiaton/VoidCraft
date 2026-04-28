package com.example.voidcraft.Gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ModuleScreen extends AbstractContainerScreen<ModuleMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("void_craft", "textures/gui/module.png");

    public ModuleScreen(ModuleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 133;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float v, int i, int i1) {
    int x = this.leftPos;
    int y = this.topPos;
        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                x,
                y,
                0.0F,
                0.0F,
                this.imageWidth,
                this.imageHeight,
                this.imageWidth,
                this.imageHeight
        );
    }
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
