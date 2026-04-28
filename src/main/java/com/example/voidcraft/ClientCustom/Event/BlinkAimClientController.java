package com.example.voidcraft.ClientCustom.Event;

import com.example.voidcraft.Custom.Clock.ModuleSkillClock;
import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.Effect.VoidRingManager;
import com.example.voidcraft.Item.custom.PhaseWatch;
import com.example.voidcraft.network.ReleaseBlinkModulePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import static com.example.voidcraft.Item.custom.ModuleItem.ModuleType.BlinkVoidModule.DISTANCE_PER_TICK;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleType.BlinkVoidModule.MAX_DISTANCE;

public final class BlinkAimClientController {
    private BlinkAimClientController() {}


    private static final boolean[] active = new boolean[PhaseWatch.WATCH_MODULE_SLOT_COUNT];       // 每个技能槽是否正在客户端瞄准
    private static final Vec3[] previewTarget = new Vec3[PhaseWatch.WATCH_MODULE_SLOT_COUNT];     // 每个技能槽当前预览出来的目标点


    public static void onPress(Minecraft mc, int slot) {
        if (mc.player == null ) {
            return;
        }

        active[slot] = true;                                              // 按下时进入瞄准状态
        previewTarget[slot] = mc.player.getEyePosition();                 // 初始目标先放在眼睛位置，后续 HOLD 会刷新


    }
    public static void onHold(Minecraft mc, int slot, int ticks) {
        if (mc.player == null) {
            return;
        }

        active[slot] = true;                                              // 按住期间保持瞄准状态

        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getLookAngle();

        double distance = Math.min(ticks * DISTANCE_PER_TICK, MAX_DISTANCE); // 蓄力越久越远，但最多 12 格

        previewTarget[slot] = eye.add(look.scale(distance));              // 目标点 = 眼睛位置 + 视线方向 * 距离


        if (mc.level != null) {
            Vec3 target = previewTarget[slot];                            // 这里只负责客户端预览，不真的传送

            VoidRingInstance.Preset preset = VoidRingInstance.Preset.DEFAULT
                    .copy()
                    .durationTicks(2)
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

            VoidRingManager.addRing(target, 1, preset);
        }
    }
    public static void onRelease(Minecraft mc, int slot, int ticks) {
        if (mc.player == null) {
            return;
        }

        active[slot] = false;                                             // 松手时退出瞄准状态

        previewTarget[slot] = Vec3.ZERO;                                  // 清理本地预览缓存

        mc.player.displayClientMessage(
                Component.literal("闪现释放 slot " + slot + " ticks: " + ticks),
                true
        );

        ClientPacketDistributor.sendToServer(new ReleaseBlinkModulePayload(slot, ticks)); // 把槽位和蓄力 tick 交给服务端执行
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
}
