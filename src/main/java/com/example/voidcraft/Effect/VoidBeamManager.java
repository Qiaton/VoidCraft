package com.example.voidcraft.Effect;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class VoidBeamManager {
    private static final List<VoidBeamInstance> BEAMS = new ArrayList<>();
    private static final Map<UUID, VoidBeamInstance> BEAM_IDS = new HashMap<>();

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

    public static VoidBeamInstance addBeam(UUID effectId, Vec3 start, Vec3 end, float scale, VoidBeamInstance.Config config) {
        if (effectId != null && BEAM_IDS.containsKey(effectId)) {
            return null;
        }
        VoidBeamInstance beam = addBeam(start, end, scale, config);
        if (beam != null && effectId != null) {
            BEAM_IDS.put(effectId, beam);
        }
        return beam;
    }

    public static boolean hasBeam(UUID effectId) {
        return effectId != null && BEAM_IDS.containsKey(effectId);
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
                BEAM_IDS.values().remove(beam);
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
        BEAM_IDS.clear();
    }
}
