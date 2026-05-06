package com.example.voidcraft.ClientCustom.Void;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.multiplayer.LevelLoadTracker;

public class PhaseWorldTransitionScreen extends LevelLoadingScreen {
    private LevelLoadTracker loadTracker;

    public PhaseWorldTransitionScreen(LevelLoadTracker loadTracker, Reason reason) {
        super(loadTracker, reason);
        this.loadTracker = loadTracker;
        if (PhaseWorldTransitionClient.isIdle()) {
            PhaseWorldTransitionClient.beginLoadingHoldTransition();
        }
        PhaseWorldTransitionClient.markLoadingScreenState(loadTracker.isLevelReady());
    }

    @Override
    public void update(LevelLoadTracker loadTracker, Reason reason) {
        super.update(loadTracker, reason);
        this.loadTracker = loadTracker;
        if (PhaseWorldTransitionClient.isIdle()) {
            PhaseWorldTransitionClient.beginLoadingHoldTransition();
        }
        PhaseWorldTransitionClient.markLoadingScreenState(loadTracker.isLevelReady());
    }

    @Override
    public void tick() {
        PhaseWorldTransitionClient.markLoadingScreenState(this.loadTracker.isLevelReady());
        super.tick();
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PhaseWorldTransitionClient.markLoadingScreenState(this.loadTracker.isLevelReady());
        PhaseWorldTransitionOverlay.render(guiGraphics, true);
    }
}
