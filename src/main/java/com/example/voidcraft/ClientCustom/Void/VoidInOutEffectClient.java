package com.example.voidcraft.ClientCustom.Void;

import net.minecraft.util.Mth;
import net.minecraft.util.Util;

public final class VoidInOutEffectClient {
    private static final long WARP_MS = 200L;
    private static final long MASK_MS = 200L;

    private static boolean active;
    private static long startMs;

    private VoidInOutEffectClient() {
    }

    public static void start() {
        active = true;
        startMs = Util.getMillis();
    }

    public static void tick() {
        if (active && getElapsedMs() >= WARP_MS + MASK_MS) {
            active = false;
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static float warpProgress() {
        if (!active) {
            return 0.0F;
        }

        return Mth.clamp((float) getElapsedMs() / (float) WARP_MS, 0.0F, 1.0F);
    }

    public static float maskProgress() {
        if (!active) {
            return 0.0F;
        }

        long elapsed = getElapsedMs() - WARP_MS;
        return Mth.clamp((float) elapsed / (float) MASK_MS, 0.0F, 1.0F);
    }

    private static long getElapsedMs() {
        return Math.max(0L, Util.getMillis() - startMs);
    }
}
