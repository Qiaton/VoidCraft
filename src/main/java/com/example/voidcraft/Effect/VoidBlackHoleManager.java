package com.example.voidcraft.Effect;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class VoidBlackHoleManager {
    private static final List<VoidBlackHoleInstance> BLACK_HOLES = new ArrayList<>();
    private static final Map<String, VoidBlackHoleInstance> PERSISTENT_BLACK_HOLES = new HashMap<>();
    private static final Map<UUID, VoidBlackHoleInstance> BLACK_HOLE_IDS = new HashMap<>();

    private VoidBlackHoleManager() {
    }

    public static VoidBlackHoleInstance addBlackHole(Vec3 center, float scale, VoidBlackHoleInstance.Config config) {
        return addBlackHole(-1, center, scale, config);
    }

    public static VoidBlackHoleInstance addBlackHole(int ownerEntityId, Vec3 center, float scale, VoidBlackHoleInstance.Config config) {
        VoidBlackHoleInstance blackHole = new VoidBlackHoleInstance(center, Math.max(0.01F, scale), config, ownerEntityId);
        BLACK_HOLES.add(blackHole);
        return blackHole;
    }

    public static VoidBlackHoleInstance addBlackHole(UUID effectId, int ownerEntityId, Vec3 center, float scale, VoidBlackHoleInstance.Config config, int ageTicks) {
        if (effectId == null) {
            VoidBlackHoleInstance blackHole = addBlackHole(ownerEntityId, center, scale, config);
            setAge(blackHole, ageTicks);
            return blackHole;
        }

        removeBlackHole(effectId);
        VoidBlackHoleInstance blackHole = new VoidBlackHoleInstance(center, Math.max(0.01F, scale), config, ownerEntityId);
        setAge(blackHole, ageTicks);
        BLACK_HOLE_IDS.put(effectId, blackHole);
        BLACK_HOLES.add(blackHole);
        return blackHole;
    }

    public static VoidBlackHoleInstance addPersistentBlackHole(Vec3 center, float scale, VoidBlackHoleInstance.Config config) {
        return addPersistentBlackHole(-1, center, scale, config);
    }

    public static VoidBlackHoleInstance addPersistentBlackHole(int ownerEntityId, Vec3 center, float scale, VoidBlackHoleInstance.Config config) {
        if (center == null) {
            return null;
        }

        VoidBlackHoleInstance blackHole = new VoidBlackHoleInstance(
                center,
                Math.max(0.01F, scale),
                config,
                ownerEntityId,
                true
        );
        BLACK_HOLES.add(blackHole);
        return blackHole;
    }

    public static void removeBlackHole(VoidBlackHoleInstance blackHole) {
        if (blackHole == null) {
            return;
        }

        PERSISTENT_BLACK_HOLES.values().remove(blackHole);
        BLACK_HOLES.remove(blackHole);
    }

    public static void updatePersistentBlackHole(String id, Vec3 center, float scale, VoidBlackHoleInstance.Config config) {
        updatePersistentBlackHole(id, -1, center, scale, config);
    }

    public static void updatePersistentBlackHole(String id, int ownerEntityId, Vec3 center, float scale, VoidBlackHoleInstance.Config config) {
        if (id == null || center == null) {
            return;
        }

        VoidBlackHoleInstance blackHole = PERSISTENT_BLACK_HOLES.get(id);
        if (blackHole == null) {
            blackHole = addPersistentBlackHole(ownerEntityId, center, scale, config);
            PERSISTENT_BLACK_HOLES.put(id, blackHole);
            return;
        }

        blackHole.setTargetCenter(center);
    }

    public static void removePersistentBlackHole(String id) {
        VoidBlackHoleInstance blackHole = PERSISTENT_BLACK_HOLES.remove(id);
        if (blackHole != null) {
            BLACK_HOLES.remove(blackHole);
        }
    }

    public static void clientTick(Minecraft mc) {
        if (mc.level == null) {
            clear();
            return;
        }

        tickBlackHoles();
    }

    public static List<VoidBlackHoleInstance> getBlackHoles() {
        return BLACK_HOLES;
    }

    public static boolean hasActiveBlackHoles() {
        return !BLACK_HOLES.isEmpty();
    }

    private static void tickBlackHoles() {
        Iterator<VoidBlackHoleInstance> iterator = BLACK_HOLES.iterator();
        while (iterator.hasNext()) {
            VoidBlackHoleInstance instance = iterator.next();
            instance.tickPersistent();
            if (!instance.persistent) {
                instance.age++;
            }
            if (instance.isDead()) {
                PERSISTENT_BLACK_HOLES.values().remove(instance);
                BLACK_HOLE_IDS.values().remove(instance);
                iterator.remove();
            }
        }
    }

    private static void removeBlackHole(UUID effectId) {
        VoidBlackHoleInstance blackHole = BLACK_HOLE_IDS.remove(effectId);
        if (blackHole != null) {
            PERSISTENT_BLACK_HOLES.values().remove(blackHole);
            BLACK_HOLES.remove(blackHole);
        }
    }

    private static void setAge(VoidBlackHoleInstance blackHole, int ageTicks) {
        if (blackHole == null) {
            return;
        }
        blackHole.age = Math.max(0, Math.min(ageTicks, Math.max(0, blackHole.config.durationTicks() - 1)));
    }

    private static void clear() {
        BLACK_HOLES.clear();
        PERSISTENT_BLACK_HOLES.clear();
        BLACK_HOLE_IDS.clear();
    }
}
