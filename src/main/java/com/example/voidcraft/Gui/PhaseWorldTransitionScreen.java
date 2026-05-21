package com.example.voidcraft.Gui;

import com.example.voidcraft.ClientCustom.Void.PhaseWorldTransitionClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;

import java.util.function.BooleanSupplier;

public class PhaseWorldTransitionScreen extends ReceivingLevelScreen {
    private final BooleanSupplier levelReady;

    public PhaseWorldTransitionScreen(BooleanSupplier levelReady, Reason reason) {
        super(levelReady, reason);
        this.levelReady = levelReady;
        if (PhaseWorldTransitionClient.isIdle()) {
            PhaseWorldTransitionClient.beginLoadingHoldTransition();
        }
        PhaseWorldTransitionClient.markLoadingScreenState(levelReady.getAsBoolean());
    }

    @Override
    public void tick() {
        PhaseWorldTransitionClient.markLoadingScreenState(this.levelReady.getAsBoolean());
        super.tick();
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PhaseWorldTransitionClient.markLoadingScreenState(this.levelReady.getAsBoolean());
        PhaseWorldTransitionOverlay.render(guiGraphics, true);
    }
}
