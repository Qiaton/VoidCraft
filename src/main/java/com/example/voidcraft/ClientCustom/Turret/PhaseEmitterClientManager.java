package com.example.voidcraft.ClientCustom.Turret;

import com.example.voidcraft.Custom.Behavior.Turret.PhaseEmitterSlot;
import com.example.voidcraft.Effect.VoidBeamInstance;
import com.example.voidcraft.Effect.VoidBeamManager;
import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.Effect.VoidRingManager;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.PhaseTurretModule;
import com.example.voidcraft.network.ModNetworking;
import com.example.voidcraft.network.UseTurretShotPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PhaseEmitterClientManager {
    private static boolean localShootingSynced = false;
    private static boolean localVolleyShootingSynced = false;
    private static final Map<Integer, PhaseEmitterSet> ACTIVE_EMITTERS = new HashMap<>();
    private static final Map<Integer, Integer> ACTIVE_EMITTER_COUNTS = new HashMap<>();

    // ACTIVE 只表示“这个玩家身边要显示炮台球”。
    private static final Set<Integer> ACTIVE_PLAYER_IDS = new HashSet<>();

    // BLOCKING 表示“本地输入需要交给手动炮台”，辅助炮台不会进入这个集合。
    private static final Set<Integer> BLOCKING_PLAYER_IDS = new HashSet<>();
    private static final List<FollowedMuzzleFlash> ACTIVE_MUZZLE_FLASHES = new ArrayList<>();

    public static void syncState(int playerId, boolean active) {
        syncState(playerId, active, true);
    }

    public static void syncState(int playerId, boolean active, boolean blocksInput) {
        syncState(playerId, active, blocksInput, PhaseEmitterSlot.configuredCount());
    }

    public static void syncState(int playerId, boolean active, boolean blocksInput, int emitterCount) {
        // 同一个 S2C 状态包同时驱动视觉和输入策略：手动 blocksInput=true，辅助 blocksInput=false。
        int actualEmitterCount = PhaseEmitterSlot.normalizeCount(emitterCount);
        if (active) {
            ACTIVE_PLAYER_IDS.add(playerId);
            ACTIVE_EMITTER_COUNTS.put(playerId, actualEmitterCount);
            if (blocksInput) {
                BLOCKING_PLAYER_IDS.add(playerId);
            } else {
                BLOCKING_PLAYER_IDS.remove(playerId);
            }
            ensureStarted(playerId, blocksInput, actualEmitterCount);
            return;
        }

        ACTIVE_PLAYER_IDS.remove(playerId);
        ACTIVE_EMITTER_COUNTS.remove(playerId);
        stop(playerId);
    }

    public static void start(Player player) {
        start(player, true);
    }

    private static void start(Player player, boolean blocksInput) {
        start(player, blocksInput, PhaseEmitterSlot.configuredCount());
    }

    private static void start(Player player, boolean blocksInput, int emitterCount) {
        // start 也接收 blocksInput，避免辅助炮台通过 ensureStarted 误加入输入拦截集合。
        int playerId = player.getId();
        int actualEmitterCount = PhaseEmitterSlot.normalizeCount(emitterCount);
        int orbColorLevel = actualEmitterCount;
        ACTIVE_PLAYER_IDS.add(playerId);
        ACTIVE_EMITTER_COUNTS.put(playerId, actualEmitterCount);
        if (blocksInput) {
            BLOCKING_PLAYER_IDS.add(playerId);
        } else {
            BLOCKING_PLAYER_IDS.remove(playerId);
        }

        PhaseEmitterSet existingSet = ACTIVE_EMITTERS.get(playerId);
        if (existingSet != null) {
            if (existingSet.getEmitterCount() == actualEmitterCount
                    && existingSet.getOrbColorLevel() == orbColorLevel) {
                return;
            }

            existingSet.remove();
            ACTIVE_EMITTERS.remove(playerId);
        }

        PhaseEmitterSet set = new PhaseEmitterSet(actualEmitterCount, orbColorLevel);
        set.create(player);
        set.playToggleFlash(player);

        ACTIVE_EMITTERS.put(playerId, set);
    }

    public static void stop(int playerId) {
        boolean wasBlocking = BLOCKING_PLAYER_IDS.remove(playerId);
        ACTIVE_EMITTER_COUNTS.remove(playerId);
        PhaseEmitterSet set = ACTIVE_EMITTERS.remove(playerId);
        Minecraft mc = Minecraft.getInstance();

        if (set != null) {
            if (mc.level != null && mc.level.getEntity(playerId) instanceof Player player) {
                set.playToggleFlash(player);
            }
            set.remove();
        }

        if (mc.player != null && mc.player.getId() == playerId) {
            syncLocalTurretInput(false, false);
            if (wasBlocking) {
                suppressTurretBlockedControls(mc);
            }
        }
    }

    public static boolean isActive(int playerId) {
        return ACTIVE_PLAYER_IDS.contains(playerId);
    }

    private static boolean blocksInput(int playerId) {
        return BLOCKING_PLAYER_IDS.contains(playerId);
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null) {
            ACTIVE_PLAYER_IDS.clear();
            BLOCKING_PLAYER_IDS.clear();
            ACTIVE_EMITTER_COUNTS.clear();
            ACTIVE_EMITTERS.clear();
            ACTIVE_MUZZLE_FLASHES.clear();
            localShootingSynced = false;
            localVolleyShootingSynced = false;
            return;
        }

        for (Integer playerId : ACTIVE_PLAYER_IDS) {
            ensureStarted(playerId, blocksInput(playerId), getEmitterCount(playerId));
        }

        ACTIVE_EMITTERS.entrySet().removeIf(entry -> {
            Entity entity = mc.level.getEntity(entry.getKey());

            if (!(entity instanceof Player player)) {
                entry.getValue().remove();
                return true;
            }

            entry.getValue().update(player);
            return false;
        });
        updateMuzzleFlashes(1.0F, false);
    }

    public static void updateBeforeRender(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        if (mc.options.getCameraType().isFirstPerson()) {
            PhaseEmitterSet set = ACTIVE_EMITTERS.get(mc.player.getId());
            if (set != null) {
                set.updateFirstPersonNow(mc.player);
            }
        }
        updateMuzzleFlashes(partialTick, true);
    }

    public static void tickLocalAttackInput() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null) {
            localShootingSynced = false;
            localVolleyShootingSynced = false;
            return;
        }

        if (!blocksInput(mc.player.getId())) {
            // 辅助炮台不限制原版左/右键，也不主动发送手动炮台的 shooting 状态。
            localShootingSynced = false;
            localVolleyShootingSynced = false;
            return;
        }

        if (mc.screen != null) {
            syncLocalTurretInput(false, false);
            return;
        }

        syncLocalTurretInput(
                isMouseDown(mc, GLFW.GLFW_MOUSE_BUTTON_LEFT),
                isMouseDown(mc, GLFW.GLFW_MOUSE_BUTTON_RIGHT)
        );
        suppressTurretBlockedControls(mc);
    }

    public static boolean handleMouseButton(int button, int action) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || !blocksInput(mc.player.getId())) {
            // 非 blocking 状态直接放行，保证辅助炮台不影响交互和使用物品。
            return false;
        }

        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT && button != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return false;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (action == GLFW.GLFW_PRESS) {
                syncLocalShooting(true);
            } else if (action == GLFW.GLFW_RELEASE) {
                syncLocalTurretInput(false, localVolleyShootingSynced);
            }
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (action == GLFW.GLFW_PRESS) {
                syncLocalTurretInput(localShootingSynced, true);
            } else if (action == GLFW.GLFW_RELEASE) {
                syncLocalTurretInput(localShootingSynced, false);
            }
        }

        suppressTurretBlockedControls(mc);
        return true;
    }

    public static boolean shouldHideLocalHands() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null && blocksInput(mc.player.getId());
    }

    public static boolean hasVisibleEmitters() {
        return !ACTIVE_PLAYER_IDS.isEmpty() || !ACTIVE_EMITTERS.isEmpty();
    }

    public static void renderEmitters(
            MultiBufferSource.BufferSource buffers,
            RenderType renderType,
            PoseStack poseStack,
            Vec3 cameraPos,
            float partialTick,
            int light,
            boolean shaderCompat
    ) {
        if (ACTIVE_EMITTERS.isEmpty()) {
            return;
        }

        VertexConsumer buffer = buffers.getBuffer(renderType);
        for (PhaseEmitterSet set : ACTIVE_EMITTERS.values()) {
            set.render(poseStack, buffer, cameraPos, partialTick, light, shaderCompat);
        }
        buffers.endBatch(renderType);
    }

    private static void syncLocalShooting(boolean shooting) {
        syncLocalTurretInput(shooting, localVolleyShootingSynced);
    }

    private static void syncLocalTurretInput(boolean shooting, boolean volleyShooting) {
        if (localShootingSynced == shooting && localVolleyShootingSynced == volleyShooting) {
            return;
        }

        localShootingSynced = shooting;
        localVolleyShootingSynced = volleyShooting;
        ModNetworking.sendToServer(new UseTurretShotPayload(shooting, volleyShooting));
    }

    private static boolean isMouseDown(Minecraft mc, int button) {
        return GLFW.glfwGetMouseButton(mc.getWindow().handle(), button) == GLFW.GLFW_PRESS;
    }

    private static void suppressTurretBlockedControls(Minecraft mc) {
        suppressKeyMapping(mc.options.keyAttack);
        suppressKeyMapping(mc.options.keyUse);
    }

    private static void suppressKeyMapping(KeyMapping keyMapping) {
        keyMapping.setDown(false);
        while (keyMapping.consumeClick()) {
        }
    }

    public static void playShotFx(
            int playerId,
            int emitterIndex,
            Vec3 targetPos,
            VoidBeamInstance.Config beamConfig
    ) {
        int emitterCount = getEmitterCount(playerId);
        if (!PhaseEmitterSlot.isValidFireIndex(emitterIndex, emitterCount) || targetPos == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        Entity entity = mc.level.getEntity(playerId);
        if (!(entity instanceof Player player)) {
            return;
        }

        PhaseEmitterSlot slot = PhaseEmitterSlot.byFireIndex(emitterIndex, emitterCount);
        Vec3 origin = getEmitterPos(player, slot, 1.0F);

        VoidBeamInstance.Config actualConfig = beamConfig == null ? VoidBeamInstance.Config.DEFAULT : beamConfig;
        playShotFlash(playerId, slot, origin, targetPos, actualConfig);
        VoidBeamManager.addBeam(origin, targetPos, 1.0F, actualConfig);
    }

    private static void playShotFlash(
            int playerId,
            PhaseEmitterSlot slot,
            Vec3 origin,
            Vec3 targetPos,
            VoidBeamInstance.Config beamConfig
    ) {
        // 炮台白光跟随现有射击包本地生成，所有收到 S2C 的客户端都能看到。
        VoidRingInstance muzzleFlash = VoidRingManager.addRing(origin, 1.0F, PhaseTurretModule.getMuzzleFlashPreset());
        ACTIVE_MUZZLE_FLASHES.add(new FollowedMuzzleFlash(playerId, slot, muzzleFlash));
        VoidRingManager.addRing(targetPos, 1.0F, PhaseTurretModule.getHitFlashPreset(beamConfig.glowColor()));
    }

    private static void updateMuzzleFlashes(float partialTick, boolean snapToRenderPosition) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            ACTIVE_MUZZLE_FLASHES.clear();
            return;
        }

        ACTIVE_MUZZLE_FLASHES.removeIf(flash -> !updateMuzzleFlash(mc, flash, partialTick, snapToRenderPosition));
    }

    private static boolean updateMuzzleFlash(
            Minecraft mc,
            FollowedMuzzleFlash flash,
            float partialTick,
            boolean snapToRenderPosition
    ) {
        if (flash.ring().isDead()) {
            return false;
        }

        Entity entity = mc.level.getEntity(flash.playerId());
        if (!(entity instanceof Player player)) {
            return false;
        }

        Vec3 center = getEmitterPos(player, flash.slot(), partialTick);
        if (snapToRenderPosition) {
            flash.ring().snapCenter(center);
        } else {
            flash.ring().moveCenter(center);
        }
        return true;
    }

    private static Vec3 getEmitterPos(Player player, PhaseEmitterSlot slot, float partialTick) {
        PhaseEmitterSet set = ACTIVE_EMITTERS.get(player.getId());
        return set == null
                ? PhaseEmitterSet.computeEmitterPos(player, slot)
                : set.getCurrentEmitterPos(player, slot, partialTick);
    }

    private static int getEmitterCount(int playerId) {
        PhaseEmitterSet set = ACTIVE_EMITTERS.get(playerId);
        if (set != null) {
            return set.getEmitterCount();
        }

        return ACTIVE_EMITTER_COUNTS.getOrDefault(playerId, PhaseEmitterSlot.configuredCount());
    }

    private static void ensureStarted(int playerId, boolean blocksInput, int emitterCount) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        PhaseEmitterSet existingSet = ACTIVE_EMITTERS.get(playerId);
        int actualEmitterCount = PhaseEmitterSlot.normalizeCount(emitterCount);
        int orbColorLevel = actualEmitterCount;
        if (existingSet != null
                && existingSet.getEmitterCount() == actualEmitterCount
                && existingSet.getOrbColorLevel() == orbColorLevel) {
            return;
        }

        Entity entity = mc.level.getEntity(playerId);
        if (entity instanceof Player player) {
            start(player, blocksInput, emitterCount);
        }
    }

    private record FollowedMuzzleFlash(int playerId, PhaseEmitterSlot slot, VoidRingInstance ring) {
    }

}
