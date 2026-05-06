package com.example.voidcraft.ClientCustom.Void;

import com.example.voidcraft.VoidCraft;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public final class PhaseWorldTransitionOverlay {
    private static final int PHASE_CORE_RGB = 0x00E6F3FF;
    private static final int PHASE_EDGE_RGB = 0x009FC4FF;
    private static final int PHASE_HOLD_RGB = 0x00DCEBFA;
    private static final int PHASE_CORE_ALPHA = 230;
    private static final int PHASE_HOLD_COLOR = 0xFF000000 | PHASE_HOLD_RGB;
    private static final int MASK_TEXTURE_SIZE = 512;
    private static final float MASK_INNER_RATIO = 0.52F;
    private static final Identifier ENTER_MASK_TEXTURE =
            Identifier.fromNamespaceAndPath(VoidCraft.MODID, "textures/effect/phase_world_enter_mask.png");
    private static final Identifier EXIT_MASK_TEXTURE =
            Identifier.fromNamespaceAndPath(VoidCraft.MODID, "textures/effect/phase_world_exit_mask.png");

    private static DynamicTexture enterMaskTexture;
    private static DynamicTexture exitMaskTexture;

    private PhaseWorldTransitionOverlay() {
    }

    public static void render(GuiGraphics guiGraphics) {
        render(guiGraphics, false);
    }

    public static void prepare(Minecraft mc) {
        if (mc != null) {
            ensureMaskTextures(mc);
        }
    }

    public static void render(GuiGraphics guiGraphics, boolean screenLayer) {
        Minecraft mc = Minecraft.getInstance();
        PhaseWorldTransitionClient.markOverlayFrameRendered(screenLayer);
        PhaseWorldTransitionClient.OverlayState state = PhaseWorldTransitionClient.overlayState(mc);
        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        ensureMaskTextures(mc);
        switch (state.shape()) {
            case FULL_WHITE -> fillWhite(guiGraphics, width, height);
            case ENTER_WHITE -> drawEnterMask(guiGraphics, width, height, state.progress());
            case EXIT_WINDOW -> drawExitMask(guiGraphics, width, height, state.progress());
            case NONE -> {
            }
        }
    }

    private static void fillWhite(GuiGraphics guiGraphics, int width, int height) {
        guiGraphics.fill(0, 0, width, height, PHASE_HOLD_COLOR);
    }

    private static void drawEnterMask(GuiGraphics guiGraphics, int width, int height, float progress) {
        EllipseBounds bounds = EllipseBounds.forOuterRadius(width, height, transitionCoverRadius(width, height) * progress);
        if (bounds.isEmpty()) {
            return;
        }

        blitMask(guiGraphics, ENTER_MASK_TEXTURE, bounds);
    }

    private static void drawExitMask(GuiGraphics guiGraphics, int width, int height, float progress) {
        float desiredWindowRadius = transitionCoverRadius(width, height) * progress;
        EllipseBounds bounds = EllipseBounds.forOuterRadius(width, height, desiredWindowRadius / MASK_INNER_RATIO);
        if (bounds.isEmpty()) {
            fillWhite(guiGraphics, width, height);
            return;
        }

        fillOutsideBounds(guiGraphics, width, height, bounds);
        blitMask(guiGraphics, EXIT_MASK_TEXTURE, bounds);
    }

    private static void ensureMaskTextures(Minecraft mc) {
        if (enterMaskTexture != null && exitMaskTexture != null) {
            return;
        }

        enterMaskTexture = createMaskTexture("phase world enter mask", true);
        exitMaskTexture = createMaskTexture("phase world exit mask", false);
        mc.getTextureManager().register(ENTER_MASK_TEXTURE, enterMaskTexture);
        mc.getTextureManager().register(EXIT_MASK_TEXTURE, exitMaskTexture);
    }

    private static DynamicTexture createMaskTexture(String label, boolean enterMask) {
        DynamicTexture texture = new DynamicTexture(
                () -> VoidCraft.MODID + " " + label,
                MASK_TEXTURE_SIZE,
                MASK_TEXTURE_SIZE,
                true
        );
        NativeImage pixels = texture.getPixels();
        if (pixels == null) {
            return texture;
        }

        for (int y = 0; y < MASK_TEXTURE_SIZE; y++) {
            for (int x = 0; x < MASK_TEXTURE_SIZE; x++) {
                float px = ((float) x + 0.5F) / (float) MASK_TEXTURE_SIZE * 2.0F - 1.0F;
                float py = ((float) y + 0.5F) / (float) MASK_TEXTURE_SIZE * 2.0F - 1.0F;
                float dist = (float) Math.sqrt(px * px + py * py);
                pixels.setPixel(x, y, enterMask ? enterMaskPixel(dist) : exitMaskPixel(dist));
            }
        }

        texture.upload();
        return texture;
    }

    private static int enterMaskPixel(float dist) {
        if (dist >= 1.0F) {
            return 0;
        }

        float edge = smoothStep(MASK_INNER_RATIO, 1.0F, dist);
        int alpha = Mth.clamp(Math.round(Mth.lerp(edge, PHASE_CORE_ALPHA, 0.0F)), 0, 255);
        int rgb = lerpRgb(edge, PHASE_CORE_RGB, PHASE_EDGE_RGB);
        return alpha << 24 | rgb;
    }

    private static int exitMaskPixel(float dist) {
        if (dist <= MASK_INNER_RATIO) {
            return 0;
        }

        if (dist >= 1.0F) {
            return PHASE_HOLD_COLOR;
        }

        float edge = smoothStep(MASK_INNER_RATIO, 1.0F, dist);
        int alpha = Mth.clamp(Math.round(edge * 255.0F), 0, 255);
        int rgb = lerpRgb(edge, PHASE_EDGE_RGB, PHASE_HOLD_RGB);
        return alpha << 24 | rgb;
    }

    private static void fillOutsideBounds(GuiGraphics guiGraphics, int width, int height, EllipseBounds bounds) {
        int left = Mth.clamp(bounds.x0(), 0, width);
        int right = Mth.clamp(bounds.x1(), 0, width);
        int top = Mth.clamp(bounds.y0(), 0, height);
        int bottom = Mth.clamp(bounds.y1(), 0, height);

        fillClamped(guiGraphics, 0, 0, width, top, PHASE_HOLD_COLOR);
        fillClamped(guiGraphics, 0, bottom, width, height, PHASE_HOLD_COLOR);
        fillClamped(guiGraphics, 0, top, left, bottom, PHASE_HOLD_COLOR);
        fillClamped(guiGraphics, right, top, width, bottom, PHASE_HOLD_COLOR);
    }

    private static void blitMask(GuiGraphics guiGraphics, Identifier texture, EllipseBounds bounds) {
        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture,
                bounds.x0(),
                bounds.y0(),
                0.0F,
                0.0F,
                bounds.width(),
                bounds.height(),
                MASK_TEXTURE_SIZE,
                MASK_TEXTURE_SIZE,
                MASK_TEXTURE_SIZE,
                MASK_TEXTURE_SIZE
        );
    }

    private static void fillClamped(GuiGraphics guiGraphics, int startX, int startY, int endX, int endY, int color) {
        int x0 = Math.min(startX, endX);
        int x1 = Math.max(startX, endX);
        int y0 = Math.min(startY, endY);
        int y1 = Math.max(startY, endY);
        if (x0 < x1 && y0 < y1) {
            guiGraphics.fill(x0, y0, x1, y1, color);
        }
    }

    private static float transitionCoverRadius(int width, int height) {
        float aspect = (float) width / Math.max(1.0F, (float) height);
        float cornerX = 0.5F * aspect;
        float cornerY = 0.5F;
        return (float) Math.sqrt(cornerX * cornerX + cornerY * cornerY) + 0.08F;
    }

    private static float smoothStep(float edge0, float edge1, float value) {
        if (edge1 <= edge0) {
            return value >= edge1 ? 1.0F : 0.0F;
        }

        float t = Mth.clamp((value - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    private static int lerpRgb(float amount, int fromRgb, int toRgb) {
        return ARGB.srgbLerp(amount, 0xFF000000 | fromRgb, 0xFF000000 | toRgb) & 0x00FFFFFF;
    }

    private record EllipseBounds(int x0, int y0, int x1, int y1) {
        private static EllipseBounds forOuterRadius(int width, int height, float outerRadius) {
            if (outerRadius <= 0.0F) {
                return new EllipseBounds(width / 2, height / 2, width / 2, height / 2);
            }

            float aspect = (float) width / Math.max(1.0F, (float) height);
            int halfWidth = Mth.ceil(outerRadius / Math.max(aspect, 0.0001F) * width);
            int halfHeight = Mth.ceil(outerRadius * height);
            int centerX = width / 2;
            int centerY = height / 2;
            return new EllipseBounds(centerX - halfWidth, centerY - halfHeight, centerX + halfWidth, centerY + halfHeight);
        }

        private int width() {
            return Math.max(0, x1 - x0);
        }

        private int height() {
            return Math.max(0, y1 - y0);
        }

        private boolean isEmpty() {
            return width() <= 0 || height() <= 0;
        }
    }
}
