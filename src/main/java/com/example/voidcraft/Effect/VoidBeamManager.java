package com.example.voidcraft.Effect;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class VoidBeamManager {
    private static final List<VoidBeamInstance> BEAMS = new ArrayList<>();

    private VoidBeamManager() {
    }

    public static VoidBeamInstance addBeam(Vec3 start, Vec3 end, float scale, VoidBeamInstance.Config config) {
        if (start == null || end == null || start.distanceToSqr(end) < 1.0E-8D) {
            return null;
        }

        VoidBeamInstance beam = new VoidBeamInstance(start, end, scale, config);
        BEAMS.add(beam);
        return beam;
    }

    public static void clientTick(Minecraft mc) {
        if (mc.level == null) {
            clear();
            return;
        }

        Iterator<VoidBeamInstance> iterator = BEAMS.iterator();
        while (iterator.hasNext()) {
            VoidBeamInstance beam = iterator.next();
            beam.tick();
            if (beam.isDead()) {
                iterator.remove();
            }
        }
    }

    public static List<VoidBeamInstance> getBeams() {
        return BEAMS;
    }

    public static boolean hasActiveBeams() {
        return !BEAMS.isEmpty();
    }

    private static void clear() {
        BEAMS.clear();
    }
}
