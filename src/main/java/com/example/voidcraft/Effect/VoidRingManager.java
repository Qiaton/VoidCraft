package com.example.voidcraft.Effect;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VoidRingManager {
    private static final List<VoidRingInstance> RINGS = new ArrayList<>();
    private static final Map<String, VoidRingInstance> PERSISTENT_RINGS = new HashMap<>();
    private static final Map<UUID, VoidRingInstance> RING_IDS = new HashMap<>();

    // 在固定世界坐标生成一圈相位环，适合瞬时爆点、落点预览和不需要跟随实体的效果。
    public static VoidRingInstance addRing(Vec3 center) {
        return addRing(center, 1.0F, VoidRingInstance.Preset.DEFAULT);
    }

    // 固定坐标相位环的完整入口：scale 控大小，preset 控外观和生命周期。
    public static VoidRingInstance addRing(Vec3 center, float scale, VoidRingInstance.Preset preset) {
        return addRing(-1, center, scale, preset, 0.0F);
    }

    // 带 owner 的固定坐标环，用于渲染层识别来源实体，比如第一人称过滤或后处理归属。
    public static VoidRingInstance addRing(int ownerEntityId, Vec3 center, float scale, VoidRingInstance.Preset preset) {
        return addRing(ownerEntityId, center, scale, preset, 0.0F);
    }

    public static VoidRingInstance addRing(int ownerEntityId, Vec3 center, float scale, VoidRingInstance.Preset preset, float yaw) {
        VoidRingInstance ring = new VoidRingInstance(center, Math.max(0.01F, scale), preset, ownerEntityId, -1, false, yaw);
        RINGS.add(ring);
        return ring;
    }

    public static VoidRingInstance addRing(UUID effectId, int ownerEntityId, Vec3 center, float scale, VoidRingInstance.Preset preset, float yaw, int ageTicks) {
        if (effectId == null) {
            VoidRingInstance ring = addRing(ownerEntityId, center, scale, preset, yaw);
            setAge(ring, ageTicks);
            return ring;
        }

        removeRing(effectId);
        VoidRingInstance ring = new VoidRingInstance(center, Math.max(0.01F, scale), preset, ownerEntityId, -1, false, yaw);
        setAge(ring, ageTicks);
        RING_IDS.put(effectId, ring);
        RINGS.add(ring);
        return ring;
    }

    // 生成跟随实体位置的相位环，中心会在渲染时按 trackedEntityId 更新。
    public static void addTrackedRing(int trackedEntityId, Vec3 center, float scale, VoidRingInstance.Preset preset) {
        addTrackedRing(trackedEntityId, trackedEntityId, center, scale, preset);
    }

    // 跟随实体环的完整入口：owner 表示效果来源，tracked 表示实际跟随谁。
    public static void addTrackedRing(int ownerEntityId, int trackedEntityId, Vec3 center, float scale, VoidRingInstance.Preset preset) {
        addTrackedRing(ownerEntityId, trackedEntityId, center, scale, preset, 0.0F);
    }

    public static void addTrackedRing(int ownerEntityId, int trackedEntityId, Vec3 center, float scale, VoidRingInstance.Preset preset, float yaw) {
        RINGS.add(new VoidRingInstance(center, Math.max(0.01F, scale), preset, ownerEntityId, trackedEntityId, false, yaw));
    }

    public static void addTrackedRing(UUID effectId, int ownerEntityId, int trackedEntityId, Vec3 center, float scale, VoidRingInstance.Preset preset, float yaw, int ageTicks) {
        if (effectId == null) {
            VoidRingInstance ring = new VoidRingInstance(center, Math.max(0.01F, scale), preset, ownerEntityId, trackedEntityId, false, yaw);
            setAge(ring, ageTicks);
            RINGS.add(ring);
            return;
        }

        removeRing(effectId);
        VoidRingInstance ring = new VoidRingInstance(center, Math.max(0.01F, scale), preset, ownerEntityId, trackedEntityId, false, yaw);
        setAge(ring, ageTicks);
        RING_IDS.put(effectId, ring);
        RINGS.add(ring);
    }

    // 创建或更新一个持续存在的固定坐标环：同一个 id 会复用旧 ring，只平滑移动到新坐标，适合技能指示器。
    public static void updatePersistentRing(String id, Vec3 center, float scale, VoidRingInstance.Preset preset) {
        if (id == null || center == null) {
            return;
        }

        VoidRingInstance ring = PERSISTENT_RINGS.get(id);
        if (ring == null) {
            ring = new VoidRingInstance(center, Math.max(0.01F, scale), preset, -1, -1, true);
            PERSISTENT_RINGS.put(id, ring);
            RINGS.add(ring);
            return;
        }

        ring.setTargetCenter(center);
    }

    // 停止指定持续环：松手、取消技能或切换状态时调用，让指示器立刻消失。
    public static void removePersistentRing(String id) {
        VoidRingInstance ring = PERSISTENT_RINGS.remove(id);
        if (ring != null) {
            RINGS.remove(ring);
        }
    }

    // 客户端 tick 入口：推进所有 ring 生命周期，并在离开世界时清空。
    public static void clientTick(Minecraft mc) {
        if (mc.level == null) {
            clear();
            return;
        }

        tickRings();
    }

    // 渲染层每帧读取当前还活着的 ring。
    public static List<VoidRingInstance> getRings() {
        return RINGS;
    }

    // 快速判断是否还需要启动 ring 渲染/后处理路径。
    public static boolean hasActiveRings() {
        return !RINGS.isEmpty();
    }

    private static void tickRings() {
        Iterator<VoidRingInstance> iterator = RINGS.iterator();
        while (iterator.hasNext()) {
            VoidRingInstance instance = iterator.next();
            instance.tickPersistent();
            instance.age++;
            if (instance.isDead()) {
                PERSISTENT_RINGS.values().remove(instance);
                RING_IDS.values().remove(instance);
                iterator.remove();
            }
        }
    }

    private static void removeRing(UUID effectId) {
        VoidRingInstance ring = RING_IDS.remove(effectId);
        if (ring != null) {
            PERSISTENT_RINGS.values().remove(ring);
            RINGS.remove(ring);
        }
    }

    private static void setAge(VoidRingInstance ring, int ageTicks) {
        if (ring == null) {
            return;
        }
        ring.age = Math.max(0, Math.min(ageTicks, Math.max(0, ring.preset.durationTicks() - 1)));
    }

    private static void clear() {
        RINGS.clear();
        PERSISTENT_RINGS.clear();
        RING_IDS.clear();
    }
}
