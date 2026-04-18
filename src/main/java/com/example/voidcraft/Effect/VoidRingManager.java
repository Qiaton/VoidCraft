package com.example.voidcraft.Effect;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class VoidRingManager {
    private static final List<VoidRingInstance> RINGS = new ArrayList<>();

    public static void addRing(Vec3 center) {
        addRing(center, 1.0F, VoidRingInstance.Preset.DEFAULT);
    }

    public static void addRing(Vec3 center, float scale, VoidRingInstance.Preset preset) {
        addRing(-1, center, scale, preset);
    }

    public static void addRing(int ownerEntityId, Vec3 center, float scale, VoidRingInstance.Preset preset) {
        RINGS.add(new VoidRingInstance(center, Math.max(0.01F, scale), preset, ownerEntityId, -1));
    }

    public static void addTrackedRing(int trackedEntityId, Vec3 center, float scale, VoidRingInstance.Preset preset) {
        RINGS.add(new VoidRingInstance(center, Math.max(0.01F, scale), preset, trackedEntityId, trackedEntityId));
    }

    public static void clientTick(Minecraft mc) {
        if (mc.level == null) {
            clear();
            return;
        }

        tickRings();
    }

    public static List<VoidRingInstance> getRings() {
        return RINGS;
    }

    public static boolean hasActiveRings() {
        return !RINGS.isEmpty();
    }

    private static void tickRings() {
        Iterator<VoidRingInstance> iterator = RINGS.iterator();
        while (iterator.hasNext()) {
            VoidRingInstance instance = iterator.next();
            instance.age++;
            if (instance.isDead()) {
                iterator.remove();
            }
        }
    }

    private static void clear() {
        RINGS.clear();
    }
}
