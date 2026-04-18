package com.example.voidcraft.Effect;

import com.example.voidcraft.Item.custom.SpatialSword;
import com.example.voidcraft.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class VoidTrailManager {
    private static final Map<Integer, TrailTracker> TRACKERS = new HashMap<>();
    private static VoidTrailInstance.Preset activePreset = VoidTrailInstance.Preset.DEFAULT;

    private VoidTrailManager() {
    }

    public static void setActivePreset(VoidTrailInstance.Preset preset) {
        activePreset = preset == null ? VoidTrailInstance.Preset.DEFAULT : preset;
        TRACKERS.clear();
    }

    public static VoidTrailInstance.Preset getActivePreset() {
        return activePreset;
    }

    public static Collection<VoidTrailInstance> getTrails() {
        List<VoidTrailInstance> visibleTrails = new ArrayList<>();
        for (TrailTracker tracker : TRACKERS.values()) {
            if (tracker.trail != null && tracker.trail.hasRenderablePoints()) {
                visibleTrails.add(tracker.trail);
            }
        }
        return visibleTrails;
    }

    public static boolean hasActiveTrails() {
        for (TrailTracker tracker : TRACKERS.values()) {
            if (tracker.trail != null && tracker.trail.hasRenderablePoints()) {
                return true;
            }
        }
        return false;
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
    }

    private static void updatePlayerTrail(Player player, boolean inVoid, VoidTrailInstance.Preset preset) {
        TrailTracker tracker = TRACKERS.computeIfAbsent(
                player.getId(),
                id -> new TrailTracker(player.getBbHeight() / 1.8F, preset)
        );

        float scale = player.getBbHeight() / 1.8F;
        if (Math.abs(tracker.scale - scale) > 1.0E-3F || tracker.trail.preset != preset) {
            tracker.rebuild(scale, preset);
        }

        Vec3 anchor = computeAnchor(player, preset, scale);
        if (tracker.lastObservedPos == null) {
            tracker.lastObservedPos = anchor;
            return;
        }

        double minMoveDistance = preset.minMoveDistance() * scale;
        boolean movedEnough = anchor.distanceToSqr(tracker.lastObservedPos) >= minMoveDistance * minMoveDistance;
        if (!inVoid) {
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
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            trail.addPoint(from.lerp(to, t));
        }
    }

    private static Vec3 computeAnchor(Player player, VoidTrailInstance.Preset preset, float scale) {
        return player.position().add(0.0D, preset.centerYOffset() * scale, 0.0D);
    }

    private static void clear() {
        TRACKERS.clear();
        activePreset = VoidTrailInstance.Preset.DEFAULT;
    }

    private static final class TrailTracker {
        private VoidTrailInstance trail;
        private float scale;
        private Vec3 lastObservedPos;
        private final ArrayDeque<Vec3> delayedPoints = new ArrayDeque<>();
        private Vec3 lastReleasedPos;
        private int sampleTicks;

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
        }

        private void resetMotion(Vec3 anchor) {
            this.lastObservedPos = anchor;
            this.delayedPoints.clear();
            this.lastReleasedPos = null;
            this.sampleTicks = 0;
        }
    }
}
