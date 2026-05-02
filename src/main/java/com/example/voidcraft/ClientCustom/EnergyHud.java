package com.example.voidcraft.ClientCustom;

import com.example.voidcraft.Config;
import com.example.voidcraft.VoidCraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.Locale;

public final class EnergyHud {
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 128;
    private static final int HUD_WIDTH = 128;
    private static final int HUD_HEIGHT = 64;
    private static final long STALE_MILLIS = 1500L;
    private static final Identifier[] ENERGY_TEXTURES = new Identifier[101];

    private static int percent;
    private static boolean visible;
    private static long lastUpdateMillis;

    static {
        for (int i = 0; i < ENERGY_TEXTURES.length; i++) {
            ENERGY_TEXTURES[i] = Identifier.fromNamespaceAndPath(
                    VoidCraft.MODID,
                    String.format(Locale.ROOT, "textures/gui/energy/energy_%03d.png", i)
            );
        }
    }

    private EnergyHud() {
    }

    public static void update(int newPercent, boolean newVisible) {
        percent = clamp(newPercent, 0, 100);
        visible = newVisible;
        lastUpdateMillis = System.currentTimeMillis();
    }

    public static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) {
            return;
        }
        if (!visible || System.currentTimeMillis() - lastUpdateMillis > STALE_MILLIS) {
            return;
        }

        int offsetX = Config.ENERGY_HUD_OFFSET_X.get();
        int offsetY = Config.ENERGY_HUD_OFFSET_Y.get();
        int x;
        int y;

        switch (Config.ENERGY_HUD_POSITION.get()) {
            case TOP_LEFT -> {
                x = offsetX;
                y = offsetY;
            }
            case TOP_RIGHT -> {
                x = guiGraphics.guiWidth() - HUD_WIDTH - offsetX;
                y = offsetY;
            }
            case BOTTOM_LEFT -> {
                x = offsetX;
                y = guiGraphics.guiHeight() - HUD_HEIGHT - offsetY;
            }
            case BOTTOM_RIGHT -> {
                x = guiGraphics.guiWidth() - HUD_WIDTH - offsetX;
                y = guiGraphics.guiHeight() - HUD_HEIGHT - offsetY;
            }
            default -> {
                x = guiGraphics.guiWidth() - HUD_WIDTH - offsetX;
                y = guiGraphics.guiHeight() - HUD_HEIGHT - offsetY;
            }
        }

        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                ENERGY_TEXTURES[percent],
                x,
                y,
                0.0F,
                0.0F,
                HUD_WIDTH,
                HUD_HEIGHT,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT
        );
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
