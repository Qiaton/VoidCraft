package com.example.voidcraft.ClientCustom.Event;

import com.example.voidcraft.Effect.VoidBlackHoleInstance;
import com.example.voidcraft.Effect.VoidBlackHoleManager;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.BlackHoleModule;
import com.example.voidcraft.Item.custom.PhaseWatch;
import com.example.voidcraft.Network.ReleaseBlackHoleModulePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class BlackHoleAimClientController {
    private static final double DISTANCE_STEP = 1.0D;
    private static final double DISTANCE_PER_TICK = 1.0D;
    private static final boolean[] active = new boolean[PhaseWatch.WATCH_MODULE_SLOT_COUNT];
    private static final Vec3[] previewTarget = new Vec3[PhaseWatch.WATCH_MODULE_SLOT_COUNT];
    private static final double[] targetDistance = new double[PhaseWatch.WATCH_MODULE_SLOT_COUNT];
    private static final double[] distanceLimit = new double[PhaseWatch.WATCH_MODULE_SLOT_COUNT];

    private BlackHoleAimClientController() {
    }

    public static void onPress(Minecraft mc, int slot, BlackHoleModule.Stats stats, VoidBlackHoleInstance.Config previewBlackHole) {
        if (mc.player == null || stats == null) {
            return;
        }

        active[slot] = true;
        targetDistance[slot] = BlackHoleModule.MIN_DISTANCE;
        distanceLimit[slot] = stats.maxDistance();
        updatePreview(mc, slot, stats, previewBlackHole);
    }

    public static void onHold(Minecraft mc, int slot, int ticks, BlackHoleModule.Stats stats, VoidBlackHoleInstance.Config previewBlackHole) {
        if (mc.player == null || stats == null) {
            return;
        }

        active[slot] = true;
        updateDistance(slot, ticks, stats);
        updatePreview(mc, slot, stats, previewBlackHole);
    }

    public static void onRelease(Minecraft mc, int slot, int ticks, BlackHoleModule.Stats stats) {
        if (mc.player == null) {
            return;
        }
        if (stats == null) {
            clear(slot);
            return;
        }

        updateDistance(slot, ticks, stats);
        Vec3 target = getTarget(mc, slot, stats);
        Vec3 fallbackTarget = getPreviewTarget(slot);
        Vec3 finalTarget = target == null ? fallbackTarget : target;
        clear(slot);
        ClientPacketDistributor.sendToServer(new ReleaseBlackHoleModulePayload(slot, finalTarget.x, finalTarget.y, finalTarget.z));
    }

    public static void onCancel(int slot) {
        if (slot < 0 || slot >= PhaseWatch.WATCH_MODULE_SLOT_COUNT) {
            return;
        }

        clear(slot);
    }

    public static boolean onScroll(Minecraft mc, int slot, int ticks, double scrollY, BlackHoleModule.Stats stats, VoidBlackHoleInstance.Config previewBlackHole) {
        if (mc.player == null || stats == null || !isActive(slot) || scrollY == 0.0D) {
            return false;
        }

        double distance = getDistanceLimit(slot, stats);
        distanceLimit[slot] = Mth.clamp(distance + Math.signum(scrollY) * DISTANCE_STEP, BlackHoleModule.MIN_DISTANCE, stats.maxDistance());
        updateDistance(slot, ticks, stats);
        updatePreview(mc, slot, stats, previewBlackHole);
        return true;
    }

    public static boolean isActive(int slot) {
        return slot >= 0 && slot < PhaseWatch.WATCH_MODULE_SLOT_COUNT && active[slot];
    }

    private static void updatePreview(Minecraft mc, int slot, BlackHoleModule.Stats stats, VoidBlackHoleInstance.Config previewBlackHole) {
        Vec3 target = getTarget(mc, slot, stats);
        if (target == null) {
            return;
        }

        previewTarget[slot] = target;
        VoidBlackHoleManager.updatePersistentBlackHole(indicatorId(slot), target, 1.0F, previewBlackHole);
    }

    private static Vec3 getTarget(Minecraft mc, int slot, BlackHoleModule.Stats stats) {
        if (mc.player == null || stats == null) {
            return null;
        }

        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getLookAngle().normalize();
        return eye.add(look.scale(getDistance(slot, stats)));
    }

    private static void updateDistance(int slot, int ticks, BlackHoleModule.Stats stats) {
        double distance = Math.max(BlackHoleModule.MIN_DISTANCE, ticks * DISTANCE_PER_TICK);
        targetDistance[slot] = Math.min(distance, getDistanceLimit(slot, stats));
    }

    private static double getDistance(int slot, BlackHoleModule.Stats stats) {
        double distance = targetDistance[slot];
        double limit = getDistanceLimit(slot, stats);
        if (distance < BlackHoleModule.MIN_DISTANCE || distance > limit) {
            distance = BlackHoleModule.MIN_DISTANCE;
            targetDistance[slot] = distance;
        }
        return distance;
    }

    private static double getDistanceLimit(int slot, BlackHoleModule.Stats stats) {
        double distance = distanceLimit[slot];
        if (distance < BlackHoleModule.MIN_DISTANCE || distance > stats.maxDistance()) {
            distance = stats.maxDistance();
            distanceLimit[slot] = distance;
        }
        return distance;
    }

    private static Vec3 getPreviewTarget(int slot) {
        if (slot < 0 || slot >= PhaseWatch.WATCH_MODULE_SLOT_COUNT) {
            return Vec3.ZERO;
        }

        Vec3 target = previewTarget[slot];
        return target == null ? Vec3.ZERO : target;
    }

    private static void clear(int slot) {
        active[slot] = false;
        previewTarget[slot] = Vec3.ZERO;
        targetDistance[slot] = 0.0D;
        distanceLimit[slot] = 0.0D;
        VoidBlackHoleManager.removePersistentBlackHole(indicatorId(slot));
    }

    private static String indicatorId(int slot) {
        return "black_hole_aim_" + slot;
    }
}
