package com.example.voidcraft.ClientCustom.Void;

import com.example.voidcraft.Network.ModNetworking;
import com.example.voidcraft.Network.PhaseWorldTransitionReadyPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.Level;

public final class PhaseWorldTransitionClient {
    private static final long TRANSITION_MS = 250L;
    private static final float ENTER_WHITE_START_PROGRESS = 0.5F;
    private static final float TELEPORT_REQUEST_WHITE_PROGRESS = 0.5F;
    private static final int MIN_RENDERED_ENTER_FRAMES = 2;
    private static final int SHADER_COMPAT_FULL_WHITE_HOLD_FRAMES = 1;
    private static final long TARGET_VISIBLE_FRAME_MS = 17L;

    private static Stage stage = Stage.IDLE;
    private static long lastVisibleFrameMarkedAtMs;
    private static long lastFullWhiteFrameMarkedAtMs;
    private static long visibleElapsedMs;
    private static int visibleFrames;
    private static int fullWhiteVisibleFrames;
    private static boolean teleportRequested;
    private static boolean visibleClockStarted;
    private static boolean loadingScreenObserved;
    private static boolean loadingScreenVisible;
    private static boolean targetLevelReady;
    private static boolean restoreHeldKeysPending;
    private static ResourceKey<Level> sourceDimension;
    private static ResourceKey<Level> targetDimension;

    private PhaseWorldTransitionClient() {
    }

    public static void beginLoadingTransition() {
        beginLoadingTransition(null, null);
    }

    public static void beginLoadingTransition(Identifier sourceDimensionId, Identifier targetDimensionId) {
        stage = Stage.ENTER;
        resetVisibleClock();
        teleportRequested = false;
        loadingScreenObserved = false;
        loadingScreenVisible = false;
        targetLevelReady = false;
        restoreHeldKeysPending = true;
        sourceDimension = toDimensionKey(sourceDimensionId, currentDimension());
        targetDimension = toDimensionKey(targetDimensionId, null);
    }

    public static void beginLoadingHoldTransition() {
        stage = Stage.HOLD_WHITE;
        resetVisibleClock();
        teleportRequested = true;
        loadingScreenObserved = true;
        loadingScreenVisible = true;
        targetLevelReady = false;
        restoreHeldKeysPending = true;
        sourceDimension = currentDimension();
        targetDimension = null;
    }

    public static void markOverlayFrameRendered(boolean screenLayer) {
        if (stage == Stage.IDLE) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (screenLayer || isLoadingScreenOpen(mc)) {
            loadingScreenVisible = true;
        } else {
            loadingScreenVisible = false;
        }

        markFullWhiteFrameIfNeeded(mc);

        if (!shouldAdvanceVisibleClock(mc, screenLayer)) {
            visibleClockStarted = false;
            return;
        }

        long now = Util.getMillis();
        if (!visibleClockStarted) {
            visibleClockStarted = true;
            lastVisibleFrameMarkedAtMs = now;
            visibleFrames++;
            return;
        }

        if (lastVisibleFrameMarkedAtMs == now) {
            return;
        }

        long delta = Math.max(1L, now - lastVisibleFrameMarkedAtMs);
        // 光影掉帧时不要让 0.25s 的动画一帧跳太远，宁愿按可见帧稍微变慢。
        visibleElapsedMs += Math.min(delta, TARGET_VISIBLE_FRAME_MS);
        lastVisibleFrameMarkedAtMs = now;
        visibleFrames++;
    }

    public static void markPostEffectFrameRendered() {
        // 后处理只负责扭曲，不能再驱动关键转场时间；维度切换时它会被 GameRenderer 清掉。
    }

    public static void markLoadingScreenState(boolean levelReady) {
        if (stage == Stage.IDLE) {
            return;
        }

        loadingScreenObserved = true;
        loadingScreenVisible = true;
        if (levelReady) {
            targetLevelReady = true;
        }
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        loadingScreenVisible = isLoadingScreenOpen(mc);

        if (stage == Stage.ENTER) {
            requestTeleportIfEnterReady();
            if (canStartExit()) {
                beginExit();
                return;
            }

            if (getStageProgress() >= 1.0F) {
                stage = Stage.HOLD_WHITE;
                resetVisibleClock();
            }
            return;
        }

        if (stage == Stage.HOLD_WHITE) {
            requestTeleportIfFullWhiteHeld();
            if (canStartExit()) {
                beginExit();
            }
            return;
        }

        if (stage == Stage.EXIT && getStageProgress() >= 1.0F) {
            finishTransition();
        }
    }

    public static boolean isActive() {
        return stage != Stage.IDLE;
    }

    public static boolean isIdle() {
        return stage == Stage.IDLE;
    }

    public static boolean isEntering() {
        return stage == Stage.ENTER;
    }

    public static boolean isHoldingWhite() {
        return stage == Stage.HOLD_WHITE;
    }

    public static boolean isExiting() {
        return stage == Stage.EXIT;
    }

    public static float enterProgress() {
        return stage == Stage.ENTER ? getStageProgress() : 0.0F;
    }

    public static float enterWhiteProgress() {
        return stage == Stage.ENTER ? getEnterWhiteProgress() : 0.0F;
    }

    public static float exitProgress() {
        return stage == Stage.EXIT ? getStageProgress() : 0.0F;
    }

    public static float holdWhite() {
        return stage == Stage.HOLD_WHITE ? 1.0F : 0.0F;
    }

    public static OverlayState overlayState(Minecraft mc) {
        if (stage == Stage.IDLE) {
            return OverlayState.hidden();
        }

        if (shouldForceSolidWhite(mc)) {
            return OverlayState.fullWhite();
        }

        if (stage == Stage.ENTER) {
            float whiteProgress = getEnterWhiteProgress();
            if (whiteProgress <= 0.0F) {
                return OverlayState.hidden();
            }

            return OverlayState.enterWhite(whiteProgress);
        }

        if (stage == Stage.HOLD_WHITE) {
            return OverlayState.fullWhite();
        }

        if (stage == Stage.EXIT) {
            float progress = getStageProgress();
            if (progress >= 0.5F) {
                return OverlayState.hidden();
            }

            return OverlayState.exitWindow(easeInOut(progress * 2.0F));
        }

        return OverlayState.hidden();
    }

    public static float stageCode() {
        if (stage == Stage.ENTER) {
            return 0.33F;
        }
        if (stage == Stage.HOLD_WHITE) {
            return 0.66F;
        }
        if (stage == Stage.EXIT) {
            return 1.0F;
        }
        return 0.0F;
    }

    private static float getStageProgress() {
        if (usesVisibleClock(stage) && !visibleClockStarted) {
            return 0.0F;
        }

        if (usesVisibleClock(stage)) {
            return Mth.clamp((float) visibleElapsedMs / (float) TRANSITION_MS, 0.0F, 1.0F);
        }

        return 0.0F;
    }

    private static void requestTeleportIfEnterReady() {
        if (shouldDelayTeleportUntilFullWhite()) {
            return;
        }

        if (teleportRequested
                || visibleFrames < MIN_RENDERED_ENTER_FRAMES
                || getEnterWhiteProgress() < TELEPORT_REQUEST_WHITE_PROGRESS) {
            return;
        }

        requestTeleport();
    }

    private static void requestTeleportIfFullWhiteHeld() {
        if (teleportRequested || !shouldDelayTeleportUntilFullWhite()) {
            return;
        }

        if (fullWhiteVisibleFrames < SHADER_COMPAT_FULL_WHITE_HOLD_FRAMES) {
            return;
        }

        requestTeleport();
    }

    private static void requestTeleport() {
        if (!teleportRequested) {
            teleportRequested = true;
            ModNetworking.sendToServer(new PhaseWorldTransitionReadyPayload());
        }
    }

    private static float getEnterWhiteProgress() {
        float progress = getStageProgress();
        if (progress <= ENTER_WHITE_START_PROGRESS) {
            return 0.0F;
        }

        return easeInOut((progress - ENTER_WHITE_START_PROGRESS) / (1.0F - ENTER_WHITE_START_PROGRESS));
    }

    private static float easeInOut(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static boolean hasArrivedInTargetWorld() {
        ResourceKey<Level> currentDimension = currentDimension();
        if (currentDimension == null) {
            return false;
        }

        if (targetDimension != null) {
            return targetDimension.equals(currentDimension);
        }

        return sourceDimension != null && !sourceDimension.equals(currentDimension);
    }

    private static boolean canStartExit() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null || isLoadingScreenOpen(mc)) {
            return false;
        }

        return hasArrivedInTargetWorld() && (!loadingScreenObserved || targetLevelReady);
    }

    private static void beginExit() {
        stage = Stage.EXIT;
        resetVisibleClock();
        restoreHeldKeysAfterLoading();
    }

    private static ResourceKey<Level> currentDimension() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) {
            return null;
        }

        return mc.level.dimension();
    }

    private static void finishTransition() {
        stage = Stage.IDLE;
        sourceDimension = null;
        targetDimension = null;
        resetVisibleClock();
        loadingScreenObserved = false;
        loadingScreenVisible = false;
        targetLevelReady = false;
        restoreHeldKeysPending = false;
    }

    private static void resetVisibleClock() {
        lastVisibleFrameMarkedAtMs = 0L;
        visibleElapsedMs = 0L;
        visibleFrames = 0;
        visibleClockStarted = false;
        resetFullWhiteFrameCounter();
    }

    private static void resetFullWhiteFrameCounter() {
        lastFullWhiteFrameMarkedAtMs = 0L;
        fullWhiteVisibleFrames = 0;
    }

    private static boolean usesVisibleClock(Stage stage) {
        return stage == Stage.ENTER || stage == Stage.EXIT;
    }

    private static boolean shouldAdvanceVisibleClock(Minecraft mc, boolean screenLayer) {
        if (!usesVisibleClock(stage)) {
            return false;
        }

        if (stage == Stage.ENTER) {
            return !loadingScreenVisible && hasPlayableWorld(mc);
        }

        return !screenLayer && !loadingScreenVisible && hasPlayableWorld(mc);
    }

    private static boolean shouldForceSolidWhite(Minecraft mc) {
        return stage == Stage.HOLD_WHITE
                || loadingScreenVisible
                || ((mc == null || mc.level == null || mc.player == null) && stage != Stage.IDLE)
                || (teleportRequested && stage == Stage.ENTER && !hasPlayableWorld(mc));
    }

    private static boolean hasPlayableWorld(Minecraft mc) {
        return mc != null && mc.level != null && mc.player != null;
    }

    private static boolean shouldDelayTeleportUntilFullWhite() {
        return VoidEffect.isShaderCompatMode();
    }

    private static void markFullWhiteFrameIfNeeded(Minecraft mc) {
        if (stage != Stage.HOLD_WHITE || teleportRequested || loadingScreenVisible || !hasPlayableWorld(mc)) {
            return;
        }

        long now = Util.getMillis();
        if (lastFullWhiteFrameMarkedAtMs == now) {
            return;
        }

        lastFullWhiteFrameMarkedAtMs = now;
        fullWhiteVisibleFrames++;
    }

    private static boolean isLoadingScreenOpen(Minecraft mc) {
        return mc != null && mc.screen instanceof LevelLoadingScreen;
    }

    private static void restoreHeldKeysAfterLoading() {
        if (!restoreHeldKeysPending) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (!hasPlayableWorld(mc) || isLoadingScreenOpen(mc)) {
            return;
        }

        KeyMapping.setAll();
        restoreHeldKeysPending = false;
    }

    private static ResourceKey<Level> toDimensionKey(Identifier dimensionId, ResourceKey<Level> fallback) {
        if (dimensionId == null) {
            return fallback;
        }

        return ResourceKey.create(Registries.DIMENSION, dimensionId);
    }

    private enum Stage {
        IDLE,
        ENTER,
        HOLD_WHITE,
        EXIT
    }

    public enum OverlayShape {
        NONE,
        FULL_WHITE,
        ENTER_WHITE,
        EXIT_WINDOW
    }

    public record OverlayState(OverlayShape shape, float progress) {
        private static OverlayState hidden() {
            return new OverlayState(OverlayShape.NONE, 0.0F);
        }

        private static OverlayState fullWhite() {
            return new OverlayState(OverlayShape.FULL_WHITE, 1.0F);
        }

        private static OverlayState enterWhite(float progress) {
            return new OverlayState(OverlayShape.ENTER_WHITE, Mth.clamp(progress, 0.0F, 1.0F));
        }

        private static OverlayState exitWindow(float progress) {
            return new OverlayState(OverlayShape.EXIT_WINDOW, Mth.clamp(progress, 0.0F, 1.0F));
        }
    }
}
