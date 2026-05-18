package com.example.voidcraft.Effect;

import com.example.voidcraft.Item.custom.SpatialSword;
import com.example.voidcraft.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class VoidTrailManager {
    private static final Map<Integer, TrailTracker> TRACKERS = new HashMap<>();
    private static final Map<Integer, TrailTracker> ENTITY_TRACKERS = new HashMap<>();
    private static final Map<UUID, Integer> ENTITY_TRAIL_IDS = new HashMap<>();
    private static final List<VoidTrailInstance> WORLD_TRAILS = new ArrayList<>();
    private static final Map<UUID, VoidTrailInstance> WORLD_TRAIL_IDS = new HashMap<>();
    private static final List<VoidTrailInstance> VISIBLE_TRAILS = new ArrayList<>();
    private static VoidTrailInstance.Preset activePreset = VoidTrailInstance.Preset.DEFAULT;

    private VoidTrailManager() {
    }

    // 设置玩家虚空移动拖尾的全局预设；切换预设时重建现有拖尾，避免新旧参数混在一条带子里。
    public static void setActivePreset(VoidTrailInstance.Preset preset) {
        activePreset = preset == null ? VoidTrailInstance.Preset.DEFAULT : preset;
        TRACKERS.clear();
        ENTITY_TRACKERS.clear();
        ENTITY_TRAIL_IDS.clear();
        WORLD_TRAILS.clear();
        WORLD_TRAIL_IDS.clear();
        VISIBLE_TRAILS.clear();
    }

    // 当前玩家虚空移动拖尾使用的预设。
    public static VoidTrailInstance.Preset getActivePreset() {
        return activePreset;
    }

    // 渲染层每帧读取当前所有可见拖尾：玩家拖尾、实体拖尾、独立世界坐标段都会汇总到这里。
    public static Collection<VoidTrailInstance> getTrails() {
        VISIBLE_TRAILS.clear();
        for (TrailTracker tracker : TRACKERS.values()) {
            if (tracker.trail != null && tracker.trail.hasRenderablePoints()) {
                VISIBLE_TRAILS.add(tracker.trail);
            }
        }
        for (TrailTracker tracker : ENTITY_TRACKERS.values()) {
            if (tracker.trail != null && tracker.trail.hasRenderablePoints()) {
                VISIBLE_TRAILS.add(tracker.trail);
            }
        }
        for (VoidTrailInstance trail : WORLD_TRAILS) {
            if (trail.hasRenderablePoints()) {
                VISIBLE_TRAILS.add(trail);
            }
        }
        return VISIBLE_TRAILS;
    }

    // 快速判断是否还需要启动 trail 渲染路径。
    public static boolean hasActiveTrails() {
        for (TrailTracker tracker : TRACKERS.values()) {
            if (tracker.trail != null && tracker.trail.hasRenderablePoints()) {
                return true;
            }
        }
        for (TrailTracker tracker : ENTITY_TRACKERS.values()) {
            if (tracker.trail != null && tracker.trail.hasRenderablePoints()) {
                return true;
            }
        }
        for (VoidTrailInstance trail : WORLD_TRAILS) {
            if (trail.hasRenderablePoints()) {
                return true;
            }
        }
        return false;
    }

    // 追踪某个实体的位置生成持续拖尾，常用于飞行中的箭或投射物。
    public static void trackEntity(int entityId, float scale, VoidTrailInstance.Preset preset) {
        trackEntity(entityId, scale, preset, null, null);
    }

    // 追踪实体的完整入口；seedStart/seedEnd 可用于注册时先补一段起始轨迹。
    public static void trackEntity(int entityId, float scale, VoidTrailInstance.Preset preset, Vec3 seedStart, Vec3 seedEnd) {
        trackEntity(null, entityId, scale, preset, seedStart, seedEnd);
    }

    public static void trackEntity(UUID effectId, int entityId, float scale, VoidTrailInstance.Preset preset, Vec3 seedStart, Vec3 seedEnd) {
        if (entityId < 0) {
            return;
        }

        if (effectId != null) {
            ENTITY_TRAIL_IDS.put(effectId, entityId);
        }

        VoidTrailInstance.Preset actualPreset = preset == null ? VoidTrailInstance.Preset.DEFAULT : preset;
        float actualScale = Math.max(0.01F, scale);
        TrailTracker tracker = ENTITY_TRACKERS.get(entityId);
        boolean shouldApplySeed = false;
        if (tracker == null) {
            tracker = new TrailTracker(actualScale, actualPreset);
            ENTITY_TRACKERS.put(entityId, tracker);
            shouldApplySeed = true;
        } else if (Math.abs(tracker.scale - actualScale) > 1.0E-3F || !tracker.trail.preset.equals(actualPreset)) {
            tracker.rebuild(actualScale, actualPreset);
            shouldApplySeed = true;
        } else if (!tracker.trail.hasRenderablePoints()) {
            shouldApplySeed = true;
        }
        tracker.missingTicks = 0;
        seedEntityTrail(entityId, tracker, shouldApplySeed ? seedStart : null, shouldApplySeed ? seedEnd : null);
    }

    // 在两个世界坐标之间直接生成一段一次性拖尾，不依赖实体连续移动采样。
    public static void addTrailSegment(Vec3 from, Vec3 to, float scale, VoidTrailInstance.Preset preset) {
        addTrailSegment(null, from, to, scale, preset);
    }

    public static void addTrailSegment(UUID effectId, Vec3 from, Vec3 to, float scale, VoidTrailInstance.Preset preset) {
        if (from == null || to == null || from.distanceToSqr(to) < 1.0E-8D) {
            return;
        }

        if (effectId != null && WORLD_TRAIL_IDS.containsKey(effectId)) {
            return;
        }

        VoidTrailInstance.Preset actualPreset = preset == null ? VoidTrailInstance.Preset.DEFAULT : preset;
        float actualScale = Math.max(0.01F, scale);
        VoidTrailInstance trail = new VoidTrailInstance(actualScale, actualPreset);
        appendInterpolatedPoints(trail, from, to, actualScale, actualPreset);
        WORLD_TRAILS.add(trail);
        if (effectId != null) {
            WORLD_TRAIL_IDS.put(effectId, trail);
        }
    }

    // 给玩家自己的拖尾补一段坐标轨迹，适合 Blink 这种瞬移行为。
    public static void seedPlayerTrail(int playerId, float scale, VoidTrailInstance.Preset preset, Vec3 seedStart, Vec3 seedEnd) {
        if (playerId < 0 || seedStart == null || seedEnd == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getEntity(playerId) instanceof Player player)) {
            return;
        }

        VoidTrailInstance.Preset actualPreset = preset == null ? VoidTrailInstance.Preset.DEFAULT : preset;
        float actualScale = Math.max(0.01F, scale);
        TrailTracker tracker = TRACKERS.computeIfAbsent(
                playerId,
                id -> new TrailTracker(actualScale, actualPreset)
        );

        if (Math.abs(tracker.scale - actualScale) > 1.0E-3F || !tracker.trail.preset.equals(actualPreset)) {
            tracker.rebuild(actualScale, actualPreset);
        }

        applySeedSegment(tracker, seedStart, seedEnd);
        tracker.lastObservedPos = computeAnchor(player, actualPreset, actualScale);
    }

    // 是否已经由网络包注册为实体持续拖尾；箭的原版渲染会用它避免重复显示。
    public static boolean isTrackedEntity(int entityId) {
        return ENTITY_TRACKERS.containsKey(entityId);
    }

    // 客户端 tick 入口：推进生命周期、更新玩家拖尾、更新实体拖尾。
    public static void clientTick(Minecraft mc) {
        if (mc.level == null) {
            clear();
            return;
        }

        tickTrails();
        updateTrackers(mc);
    }

    private static void tickTrails() {
        for (TrailTracker tracker : TRACKERS.values()) {
            tracker.trail.tick();
        }
        for (TrailTracker tracker : ENTITY_TRACKERS.values()) {
            tracker.trail.tick();
        }
        WORLD_TRAILS.removeIf(trail -> {
            trail.tick();
            if (trail.isEmpty()) {
                WORLD_TRAIL_IDS.values().remove(trail);
                return true;
            }
            return false;
        });
    }

    private static void updateTrackers(Minecraft mc) {
        Set<Integer> seenPlayers = new HashSet<>();
        LocalPlayer localPlayer = mc.player;
        for (Player player : mc.level.players()) {
            if (player == null || player.isRemoved() || player.isSpectator()) {
                continue;
            }

            seenPlayers.add(player.getId());
            boolean inVoid = player.getData(ModAttachments.IN_PHASE.get());
            if (player == localPlayer) {
                inVoid |= player.isUsingItem() && player.getUseItem().getItem() instanceof SpatialSword;
            }
            updatePlayerTrail(player, inVoid, activePreset);
        }

        TRACKERS.entrySet().removeIf(entry -> !seenPlayers.contains(entry.getKey()));
        updateEntityTrackers(mc);
    }

    private static void updatePlayerTrail(Player player, boolean inVoid, VoidTrailInstance.Preset preset) {
        TrailTracker tracker = TRACKERS.computeIfAbsent(
                player.getId(),
                id -> new TrailTracker(player.getBbHeight() / 1.8F, preset)
        );

        float scale = player.getBbHeight() / 1.8F;
        if (Math.abs(tracker.scale - scale) > 1.0E-3F || !tracker.trail.preset.equals(preset)) {
            tracker.rebuild(scale, preset);
        }

        Vec3 anchor = computeAnchor(player, preset, scale);
        updateTrail(tracker, anchor, inVoid, scale, preset);
    }

    private static void updateEntityTrackers(Minecraft mc) {
        Iterator<Map.Entry<Integer, TrailTracker>> iterator = ENTITY_TRACKERS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, TrailTracker> entry = iterator.next();
            TrailTracker tracker = entry.getValue();
            Entity entity = mc.level.getEntity(entry.getKey());
            if (entity == null || entity.isRemoved()) {
                tracker.missingTicks++;
                if (tracker.missingTicks > tracker.trail.preset.lifetimeTicks() && tracker.trail.isEmpty()) {
                    ENTITY_TRAIL_IDS.values().remove(entry.getKey());
                    iterator.remove();
                }
                continue;
            }

            tracker.missingTicks = 0;
            updateEntityTrail(tracker, computeAnchor(entity, tracker.trail.preset, tracker.scale), tracker.scale, tracker.trail.preset);
        }
    }

    private static void updateEntityTrail(
            TrailTracker tracker,
            Vec3 anchor,
            float scale,
            VoidTrailInstance.Preset preset
    ) {
        if (tracker.lastObservedPos == null) {
            tracker.lastObservedPos = anchor;
            tracker.lastReleasedPos = anchor;
            tracker.trail.addPoint(anchor);
            return;
        }

        double minMoveDistance = preset.minMoveDistance() * scale;
        if (anchor.distanceToSqr(tracker.lastObservedPos) < minMoveDistance * minMoveDistance) {
            return;
        }

        Vec3 from = tracker.lastReleasedPos == null ? tracker.lastObservedPos : tracker.lastReleasedPos;
        appendInterpolatedPoints(tracker.trail, from, anchor, scale, preset);
        tracker.lastObservedPos = anchor;
        tracker.lastReleasedPos = anchor;
        tracker.delayedPoints.clear();
        tracker.sampleTicks = 0;
    }

    private static void updateTrail(
            TrailTracker tracker,
            Vec3 anchor,
            boolean active,
            float scale,
            VoidTrailInstance.Preset preset
    ) {
        if (tracker.lastObservedPos == null) {
            tracker.lastObservedPos = anchor;
            return;
        }

        double minMoveDistance = preset.minMoveDistance() * scale;
        boolean movedEnough = anchor.distanceToSqr(tracker.lastObservedPos) >= minMoveDistance * minMoveDistance;
        if (!active) {
            tracker.resetMotion(anchor);
            return;
        }
        if (!movedEnough) {
            return;
        }

        if (tracker.delayedPoints.isEmpty()) {
            tracker.delayedPoints.addLast(tracker.lastObservedPos);
        }
        tracker.delayedPoints.addLast(anchor);

        int readyThreshold = Math.max(1, preset.startDelayTicks()) + 1;
        if (tracker.delayedPoints.size() > readyThreshold) {
            Vec3 delayedPoint = tracker.delayedPoints.removeFirst();
            tracker.sampleTicks++;
            if (tracker.lastReleasedPos == null) {
                tracker.trail.addPoint(delayedPoint);
                tracker.lastReleasedPos = delayedPoint;
                tracker.sampleTicks = 0;
            } else if (tracker.sampleTicks >= preset.sampleIntervalTicks()) {
                appendInterpolatedPoints(tracker.trail, tracker.lastReleasedPos, delayedPoint, scale, preset);
                tracker.lastReleasedPos = delayedPoint;
                tracker.sampleTicks = 0;
            }
        }

        tracker.lastObservedPos = anchor;
    }

    private static void appendInterpolatedPoints(
            VoidTrailInstance trail,
            Vec3 from,
            Vec3 to,
            float scale,
            VoidTrailInstance.Preset preset
    ) {
        if (from.distanceToSqr(to) < 1.0E-8D) {
            return;
        }

        trail.addPoint(from);
        double distance = from.distanceTo(to);
        double spacing = Math.max(1.0E-4D, preset.pointSpacing() * scale);
        int steps = Math.max(1, (int) Math.ceil(distance / spacing));
        steps = Math.min(steps, preset.maxInterpolationSteps());
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            trail.addPoint(from.lerp(to, t));
        }
    }

    private static void seedEntityTrail(int entityId, TrailTracker tracker, Vec3 seedStart, Vec3 seedEnd) {
        applySeedSegment(tracker, seedStart, seedEnd);

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        Entity entity = mc.level.getEntity(entityId);
        if (entity == null || entity.isRemoved()) {
            return;
        }

        Vec3 anchor = computeAnchor(entity, tracker.trail.preset, tracker.scale);
        if (tracker.lastObservedPos == null) {
            tracker.lastObservedPos = anchor;
            tracker.lastReleasedPos = anchor;
            tracker.trail.addPoint(anchor);
            return;
        }

        appendInterpolatedPoints(tracker.trail, tracker.lastObservedPos, anchor, tracker.scale, tracker.trail.preset);
        tracker.lastObservedPos = anchor;
        tracker.lastReleasedPos = anchor;
        tracker.delayedPoints.clear();
        tracker.sampleTicks = 0;
    }

    private static void applySeedSegment(TrailTracker tracker, Vec3 seedStart, Vec3 seedEnd) {
        if (seedStart == null || seedEnd == null || seedStart.distanceToSqr(seedEnd) < 1.0E-8D) {
            return;
        }

        if (tracker.lastObservedPos == null) {
            appendInterpolatedPoints(tracker.trail, seedStart, seedEnd, tracker.scale, tracker.trail.preset);
        } else if (tracker.lastObservedPos.distanceToSqr(seedEnd) >= 1.0E-8D) {
            Vec3 from = tracker.lastReleasedPos == null ? tracker.lastObservedPos : tracker.lastReleasedPos;
            appendInterpolatedPoints(tracker.trail, from, seedStart, tracker.scale, tracker.trail.preset);
            appendInterpolatedPoints(tracker.trail, seedStart, seedEnd, tracker.scale, tracker.trail.preset);
        }

        tracker.lastObservedPos = seedEnd;
        tracker.lastReleasedPos = seedEnd;
        tracker.delayedPoints.clear();
        tracker.sampleTicks = 0;
    }

    private static Vec3 computeAnchor(Player player, VoidTrailInstance.Preset preset, float scale) {
        return player.position().add(0.0D, preset.centerYOffset() * scale, 0.0D);
    }

    private static Vec3 computeAnchor(Entity entity, VoidTrailInstance.Preset preset, float scale) {
        return entity.position().add(0.0D, preset.centerYOffset() * scale, 0.0D);
    }

    private static void clear() {
        TRACKERS.clear();
        ENTITY_TRACKERS.clear();
        ENTITY_TRAIL_IDS.clear();
        WORLD_TRAILS.clear();
        WORLD_TRAIL_IDS.clear();
        VISIBLE_TRAILS.clear();
        activePreset = VoidTrailInstance.Preset.DEFAULT;
    }

    private static final class TrailTracker {
        private VoidTrailInstance trail;
        private float scale;
        private Vec3 lastObservedPos;
        private final ArrayDeque<Vec3> delayedPoints = new ArrayDeque<>();
        private Vec3 lastReleasedPos;
        private int sampleTicks;
        private int missingTicks;

        private TrailTracker(float scale, VoidTrailInstance.Preset preset) {
            this.scale = scale;
            this.trail = new VoidTrailInstance(scale, preset);
        }

        private void rebuild(float scale, VoidTrailInstance.Preset preset) {
            this.scale = scale;
            this.trail = new VoidTrailInstance(scale, preset);
            this.lastObservedPos = null;
            this.delayedPoints.clear();
            this.lastReleasedPos = null;
            this.sampleTicks = 0;
            this.missingTicks = 0;
        }

        private void resetMotion(Vec3 anchor) {
            if (this.lastReleasedPos != null || !this.delayedPoints.isEmpty()) {
                this.trail.startNewSegment();
            }
            this.lastObservedPos = anchor;
            this.delayedPoints.clear();
            this.lastReleasedPos = null;
            this.sampleTicks = 0;
        }
    }
}
