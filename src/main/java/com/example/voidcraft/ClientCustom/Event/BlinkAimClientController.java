package com.example.voidcraft.ClientCustom.Event;

import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.Effect.VoidRingManager;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.BlinkVoidModule;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.SafeBlinkVoidModule;
import com.example.voidcraft.Item.custom.PhaseWatch;
import com.example.voidcraft.Network.ReleaseBlinkModulePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import static com.example.voidcraft.Item.custom.ModuleItem.ModuleType.BlinkVoidModule.DISTANCE_PER_TICK;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleType.BlinkVoidModule.MAX_DISTANCE;

public final class BlinkAimClientController {
    private BlinkAimClientController() {}


    private static final double INDICATOR_MIN_DISTANCE = 2.0D;            // 指示器离玩家眼睛的最小距离，避免刚按下时白光贴脸闪出来。
    private static final double DISTANCE_STEP = 1.0D;
    private static final boolean[] active = new boolean[PhaseWatch.WATCH_MODULE_SLOT_COUNT];       // 每个技能槽是否正在客户端瞄准
    private static final Vec3[] previewTarget = new Vec3[PhaseWatch.WATCH_MODULE_SLOT_COUNT];     // 每个技能槽当前预览出来的目标点
    private static final double[] distanceLimit = new double[PhaseWatch.WATCH_MODULE_SLOT_COUNT]; // 滚轮调出来的最远闪现距离。
    private static final VoidRingInstance.Preset preset = VoidRingInstance.Preset.DEFAULT
            .copy()
            .durationTicks(1)
            .centerYOffset(0.0F)
            .startHalfHeight(0.25F)
            .peakHalfHeight(0.35F)
            .endHalfHeight(0.25F)
            .startHalfWidth(0.25F)
            .peakHalfWidth(0.35F)
            .endHalfWidth(0.25F)
            .distortionHeightScale(0F)
            .distortionWidthScale(0F)
            .build();

    public static void onPress(Minecraft mc, int slot) {
        if (mc.player == null ) {
            return;
        }

        active[slot] = true;                                              // 按下时进入瞄准状态
        previewTarget[slot] = mc.player.getEyePosition();                 // 初始目标先放在眼睛位置，后续 HOLD 会刷新
        VoidRingManager.updatePersistentRing(indicatorId(slot), getIndicatorTarget(mc, previewTarget[slot]), 1.0F, preset);


    }
    public static void onHold(Minecraft mc, int slot, int ticks,BlinkVoidModule.Stats stats) {
        if (mc.player == null) {
            return;
        }
        if (stats == null) {
            return;
        }

        active[slot] = true;                                              // 按住期间保持瞄准状态

        previewTarget[slot] = getBlinkTarget(mc, slot, ticks, stats);     // 目标点 = 眼睛位置 + 视线方向 * 距离


        if (mc.level != null) {
            Vec3 target = previewTarget[slot];                            // 这里只负责客户端预览，不真的传送

            VoidRingManager.updatePersistentRing(indicatorId(slot), getIndicatorTarget(mc, target), 1.0F, preset);
        }
    }
    public static void onRelease(Minecraft mc, int slot, int ticks, BlinkVoidModule.Stats stats) {
        if (mc.player == null) {
            return;
        }

        Vec3 target = getBlinkTarget(mc, slot, ticks, stats);             // 松手瞬间重算目标点，避免用到旧缓存导致原地拉丝
        releaseWithTarget(mc, slot, ticks, target);
    }

    public static void onReleaseSafe(Minecraft mc, int slot, int ticks, SafeBlinkVoidModule.Stats stats) {
        if (mc.player == null) {
            return;
        }

        Vec3 target = getSafeBlinkTarget(mc, slot, ticks, stats);         // 安全闪现松手时也重算一次，保证发包坐标和最终小球一致
        releaseWithTarget(mc, slot, ticks, target);
    }

    private static void releaseWithTarget(Minecraft mc, int slot, int ticks, Vec3 target) {
        Vec3 fallbackTarget = getPreviewTarget(slot);
        Vec3 finalTarget = target == null ? fallbackTarget : target;
        active[slot] = false;                                             // 松手时退出瞄准状态

        previewTarget[slot] = Vec3.ZERO;                                  // 清理本地预览缓存
        distanceLimit[slot] = 0.0D;
        VoidRingManager.removePersistentRing(indicatorId(slot));


        PacketDistributor.sendToServer(new ReleaseBlinkModulePayload(slot, ticks, finalTarget.x, finalTarget.y, finalTarget.z)); // 把槽位、蓄力 tick 和目标点交给服务端验算后执行
    }

    public static void onCancel(int slot) {
        if (slot < 0 || slot >= PhaseWatch.WATCH_MODULE_SLOT_COUNT) {
            return;
        }

        active[slot] = false;                                             // Q 取消时退出客户端瞄准状态
        previewTarget[slot] = Vec3.ZERO;                                  // 清掉预览目标点，避免取消后残留指示效果
        distanceLimit[slot] = 0.0D;
        VoidRingManager.removePersistentRing(indicatorId(slot));
    }

    public static boolean onScroll(Minecraft mc, int slot, int ticks, double scrollY, BlinkVoidModule.Stats stats) {
        if (stats == null) {
            return false;
        }

        if (!setDistanceLimit(mc, slot, scrollY, MAX_DISTANCE * stats.maxDistance())) {
            return false;
        }
        onHold(mc, slot, ticks, stats);
        return true;
    }

    public static boolean onScrollSafe(Minecraft mc, int slot, int ticks, double scrollY, SafeBlinkVoidModule.Stats stats) {
        if (stats == null) {
            return false;
        }

        if (!setDistanceLimit(mc, slot, scrollY, SafeBlinkVoidModule.MAX_DISTANCE * stats.maxDistance())) {
            return false;
        }
        onHoldSafe(mc, slot, ticks, stats);
        return true;
    }

    public static boolean isActive(int slot) {
        return slot >= 0 && slot < PhaseWatch.WATCH_MODULE_SLOT_COUNT && active[slot];
    }
    public static Vec3 getPreviewTarget(int slot) {
        if (slot < 0 || slot >= PhaseWatch.WATCH_MODULE_SLOT_COUNT) {
            return Vec3.ZERO;
        }

        Vec3 target = previewTarget[slot];
        return target == null ? Vec3.ZERO : target;
    }

    public static void onHoldSafe(Minecraft mc, int slot, int ticks, SafeBlinkVoidModule.Stats stats) {
            if (mc.player == null || mc.level == null) {
                return;
            }
            if (stats == null) {
                return;
            }

            active[slot] = true;

            Vec3 target = getSafeBlinkTarget(mc, slot, ticks, stats);

            previewTarget[slot] = target;

            VoidRingManager.updatePersistentRing(indicatorId(slot), getIndicatorTarget(mc, target), 1.0F, preset);
    }

    private static Vec3 getBlinkTarget(Minecraft mc, int slot, int ticks, BlinkVoidModule.Stats stats) {
        if (mc.player == null || stats == null) {
            return null;
        }

        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getLookAngle();
        double maxDistance = MAX_DISTANCE * stats.maxDistance();
        double distance = Math.min(ticks * DISTANCE_PER_TICK * stats.Speed(), getDistanceLimit(slot, maxDistance));
        return eye.add(look.scale(distance));
    }

    private static Vec3 getSafeBlinkTarget(Minecraft mc, int slot, int ticks, SafeBlinkVoidModule.Stats stats) {
        if (mc.player == null || mc.level == null || stats == null) {
            return null;
        }

        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getLookAngle().normalize();
        double maxDistance = SafeBlinkVoidModule.MAX_DISTANCE * stats.maxDistance();
        double distance = Math.min(ticks * SafeBlinkVoidModule.DISTANCE_PER_TICK * stats.speed(), getDistanceLimit(slot, maxDistance));
        Vec3 wantedTarget = eye.add(look.scale(distance));
        BlockHitResult hit = mc.level.clip(new ClipContext(
                eye,
                wantedTarget,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mc.player
        ));

        if (hit.getType() == HitResult.Type.BLOCK) {
            return hit.getLocation().subtract(look.scale(0.25D));
        }
        return wantedTarget;
    }

    private static Vec3 getIndicatorTarget(Minecraft mc, Vec3 target) {
        if (mc.player == null || target == null) {
            return target;
        }

        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getLookAngle().normalize();
        double distance = eye.distanceTo(target);
        if (distance >= INDICATOR_MIN_DISTANCE) {
            return target;
        }
        return eye.add(look.scale(INDICATOR_MIN_DISTANCE));
    }

    private static boolean setDistanceLimit(Minecraft mc, int slot, double scrollY, double maxDistance) {
        if (mc.player == null || !isActive(slot) || scrollY == 0.0D) {
            return false;
        }

        double distance = getDistanceLimit(slot, maxDistance);
        distance += Math.signum(scrollY) * DISTANCE_STEP;
        distanceLimit[slot] = Mth.clamp(distance, INDICATOR_MIN_DISTANCE, maxDistance);
        return true;
    }

    private static double getDistanceLimit(int slot, double maxDistance) {
        double distance = distanceLimit[slot];
        if (distance < INDICATOR_MIN_DISTANCE || distance > maxDistance) {
            distance = maxDistance;
            distanceLimit[slot] = distance;
        }
        return distance;
    }

    private static String indicatorId(int slot) {
        return "blink_aim_" + slot;
    }
}
