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

public final class VoidTrailManager {
    private static final Map<Integer, TrailTracker> TRACKERS = new HashMap<>();
    private static final Map<Integer, TrailTracker> ENTITY_TRACKERS = new HashMap<>();
    private static final List<VoidTrailInstance> VISIBLE_TRAILS = new ArrayList<>();
    private static VoidTrailInstance.Preset activePreset = VoidTrailInstance.Preset.DEFAULT;

    private VoidTrailManager() {
    }

    public static void setActivePreset(VoidTrailInstance.Preset preset) {
        activePreset = preset == null ? VoidTrailInstance.Preset.DEFAULT : preset;
        TRACKERS.clear();
        ENTITY_TRACKERS.clear();
        VISIBLE_TRAILS.clear();
    }

    public static VoidTrailInstance.Preset getActivePreset() {
        return activePreset;
    }

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
        return VISIBLE_TRAILS;
    }

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
        return false;
    }

    public static void trackEntity(int entityId, float scale, VoidTrailInstance.Preset preset) {
        trackEntity(entityId, scale, preset, null, null);
    }

    public static void trackEntity(int entityId, float scale, VoidTrailInstance.Preset preset, Vec3 seedStart, Vec3 seedEnd) {
        if (entityId < 0) {
            return;
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

    public static boolean isTrackedEntity(int entityId) {
        return ENTITY_TRACKERS.containsKey(entityId);
    }

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
    }

    private static void updateTrackers(Minecraft mc) {
        Set<Integer> seenPlayers = new HashSet<>();
        LocalPlayer localPlayer = mc.player;
        for (Player player : mc.level.players()) {
            if (player == null || player.isRemoved() || player.isSpectator()) {
                continue;
            }

            seenPlayers.add(player.getId());
            boolean inVoid = player.getData(ModAttachments.IN_VOID.get());
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
            this.lastObservedPos = anchor;
            this.delayedPoints.clear();
            this.lastReleasedPos = null;
            this.sampleTicks = 0;
        }
    }
}
