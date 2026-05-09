package com.example.voidcraft.World.behavior;

import com.example.voidcraft.Item.ModItem;
import com.example.voidcraft.VoidCraft;
import com.example.voidcraft.World.PhaseDimensions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = VoidCraft.MODID)
public final class PhaseCrystalSynthesis {
    private static final String WORK_TAG = "void_craft_phase_crystal_synthesis";
    private static final int NEED_COUNT = 9;
    private static final int WORK_TICKS = 100;
    private static final int CHECK_INTERVAL = 10;
    private static final double SEARCH_RADIUS = 2.5D;
    private static final double START_RADIUS = 1.25D;
    private static final DustParticleOptions VOID_DUST = new DustParticleOptions(0x24D9FF, 1.15F);
    private static final Map<UUID, Work> WORKS = new HashMap<>();
    private static final Map<UUID, UUID> ITEM_WORKS = new HashMap<>();

    private PhaseCrystalSynthesis() {
    }

    @SubscribeEvent
    public static void tickItem(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity item) || !(item.level() instanceof ServerLevel level)) {
            return;
        }

        if (hasWorkTag(item)) {
            tickWorkItem(level, item);
            return;
        }

        if (!PhaseDimensions.isPhaseMirror(level) || !canUse(item)) {
            return;
        }

        if ((item.tickCount + item.getId()) % CHECK_INTERVAL == 0) {
            tryStart(level, item);
        }
    }

    @SubscribeEvent
    public static void enterWorld(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ItemEntity item)) {
            return;
        }

        if (hasWorkTag(item) && !ITEM_WORKS.containsKey(item.getUUID())) {
            recoverItem(item);
        }
    }

    private static void tickWorkItem(ServerLevel level, ItemEntity item) {
        UUID workId = ITEM_WORKS.get(item.getUUID());
        Work work = workId == null ? null : WORKS.get(workId);
        if (work == null) {
            ITEM_WORKS.remove(item.getUUID());
            recoverItem(item);
            return;
        }

        tickWork(level, work);
    }

    private static void tryStart(ServerLevel level, ItemEntity source) {
        List<ItemEntity> items = findItems(level, source);
        if (getCount(items) < NEED_COUNT) {
            return;
        }

        Vec3 center = getCenter(items);
        Work work = new Work(UUID.randomUUID(), center);
        WORKS.put(work.id, work);
        makeItems(level, work, items);

        if (work.items.size() < NEED_COUNT) {
            stopWork(level, work);
        } else {
            showStart(level, center);
        }
    }

    private static List<ItemEntity> findItems(ServerLevel level, ItemEntity source) {
        return level.getEntities(
                EntityTypeTest.forClass(ItemEntity.class),
                source.getBoundingBox().inflate(SEARCH_RADIUS),
                PhaseCrystalSynthesis::canUse
        );
    }

    private static int getCount(List<ItemEntity> items) {
        int count = 0;
        for (ItemEntity item : items) {
            count += item.getItem().getCount();
            if (count >= NEED_COUNT) {
                return count;
            }
        }
        return count;
    }

    private static Vec3 getCenter(List<ItemEntity> items) {
        int left = NEED_COUNT;
        int count = 0;
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;

        for (ItemEntity item : items) {
            int used = Math.min(left, item.getItem().getCount());
            Vec3 pos = item.position();
            x += pos.x * used;
            y += pos.y * used;
            z += pos.z * used;
            count += used;
            left -= used;
            if (left <= 0) {
                break;
            }
        }

        double scale = 1.0D / Math.max(1, count);
        return new Vec3(x * scale, y * scale + 0.35D, z * scale);
    }

    private static void makeItems(ServerLevel level, Work work, List<ItemEntity> items) {
        int left = NEED_COUNT;
        int made = 0;

        for (ItemEntity item : items) {
            ItemStack stack = item.getItem();
            int used = Math.min(left, stack.getCount());
            if (used <= 0) {
                continue;
            }

            stack.shrink(used);
            if (stack.isEmpty()) {
                item.discard();
            } else {
                item.setItem(stack);
            }

            for (int i = 0; i < used; i++) {
                makeItem(level, work, made);
                made++;
            }

            left -= used;
            if (left <= 0) {
                return;
            }
        }
    }

    private static void makeItem(ServerLevel level, Work work, int index) {
        double angle = Math.PI * 2.0D * index / NEED_COUNT;
        double x = work.center.x + Math.cos(angle) * START_RADIUS;
        double y = work.center.y;
        double z = work.center.z + Math.sin(angle) * START_RADIUS;
        ItemEntity item = new ItemEntity(
                level,
                x,
                y,
                z,
                new ItemStack(ModItem.HIGH_PURITY_VOID_CRYSTAL.get()),
                0.0D,
                0.0D,
                0.0D
        );

        setWorkItem(item);
        work.items.add(item.getUUID());
        ITEM_WORKS.put(item.getUUID(), work.id);
        level.addFreshEntity(item);
    }

    private static void tickWork(ServerLevel level, Work work) {
        long now = level.getGameTime();
        if (work.lastTick == now) {
            return;
        }
        work.lastTick = now;

        if (!PhaseDimensions.isPhaseMirror(level)) {
            stopWork(level, work);
            return;
        }

        List<ItemEntity> items = getItems(level, work);
        if (items.size() < NEED_COUNT) {
            stopWork(level, work);
            return;
        }

        work.ticks++;
        for (ItemEntity item : items) {
            pullItem(item, work.center, work.ticks);
        }
        showWork(level, work.center, items, work.ticks);

        if (work.ticks >= WORK_TICKS) {
            finishWork(level, work, items);
        }
    }

    private static List<ItemEntity> getItems(ServerLevel level, Work work) {
        List<ItemEntity> items = new ArrayList<>();
        for (UUID itemId : work.items) {
            Entity entity = level.getEntity(itemId);
            if (entity instanceof ItemEntity item && item.isAlive() && hasWorkTag(item)) {
                items.add(item);
            }
        }
        return items;
    }

    private static void pullItem(ItemEntity item, Vec3 center, int ticks) {
        setWorkItem(item);
        double lift = 0.18D + Math.sin(ticks * 0.18D) * 0.08D;
        Vec3 target = center.add(0.0D, lift, 0.0D);
        Vec3 move = target.subtract(item.position());
        double power = 0.10D + 0.16D * Math.min(1.0D, ticks / (double) WORK_TICKS);
        item.setDeltaMovement(move.scale(power));
        item.hurtMarked = true;
    }

    private static void showStart(ServerLevel level, Vec3 center) {
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 24, 0.9D, 0.35D, 0.9D, 0.04D);
        level.sendParticles(VOID_DUST, center.x, center.y, center.z, 16, 0.7D, 0.25D, 0.7D, 0.0D);
    }

    private static void showWork(ServerLevel level, Vec3 center, List<ItemEntity> items, int ticks) {
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y + 0.25D, center.z, 3, 0.35D, 0.25D, 0.35D, 0.02D);
        if (ticks % 3 == 0) {
            level.sendParticles(VOID_DUST, center.x, center.y + 0.15D, center.z, 5, 0.45D, 0.25D, 0.45D, 0.0D);
        }
        if (ticks % 5 == 0) {
            for (ItemEntity item : items) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, item.getX(), item.getY() + 0.15D, item.getZ(), 1, 0.05D, 0.05D, 0.05D, 0.01D);
            }
        }
    }

    private static void finishWork(ServerLevel level, Work work, List<ItemEntity> items) {
        clearWork(work);
        for (ItemEntity item : items) {
            item.discard();
        }

        ItemEntity result = new ItemEntity(
                level,
                work.center.x,
                work.center.y + 0.15D,
                work.center.z,
                new ItemStack(ModItem.PURE_VOID_CRYSTAL.get())
        );
        result.setPickUpDelay(20);
        level.addFreshEntity(result);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, work.center.x, work.center.y + 0.2D, work.center.z, 36, 0.65D, 0.35D, 0.65D, 0.05D);
        level.sendParticles(VOID_DUST, work.center.x, work.center.y + 0.2D, work.center.z, 32, 0.7D, 0.35D, 0.7D, 0.0D);
    }

    private static void stopWork(ServerLevel level, Work work) {
        List<ItemEntity> items = getItems(level, work);
        clearWork(work);
        for (ItemEntity item : items) {
            recoverItem(item);
        }
    }

    private static void clearWork(Work work) {
        WORKS.remove(work.id);
        for (UUID itemId : work.items) {
            ITEM_WORKS.remove(itemId);
        }
    }

    private static boolean canUse(ItemEntity item) {
        return item.isAlive() && !hasWorkTag(item) && isHighCrystal(item);
    }

    private static boolean isHighCrystal(ItemEntity item) {
        ItemStack stack = item.getItem();
        return !stack.isEmpty() && stack.is(ModItem.HIGH_PURITY_VOID_CRYSTAL.get());
    }

    private static boolean hasWorkTag(ItemEntity item) {
        return item.getTags().contains(WORK_TAG);
    }

    private static void setWorkItem(ItemEntity item) {
        item.addTag(WORK_TAG);
        item.setNeverPickUp();
        item.setNoGravity(true);
        item.setInvulnerable(true);
        item.setGlowingTag(true);
    }

    private static void recoverItem(ItemEntity item) {
        item.removeTag(WORK_TAG);
        item.setPickUpDelay(40);
        item.setNoGravity(false);
        item.setInvulnerable(false);
        item.setGlowingTag(false);
        item.hurtMarked = true;
    }

    private static final class Work {
        private final UUID id;
        private final Vec3 center;
        private final List<UUID> items = new ArrayList<>();
        private int ticks;
        private long lastTick = -1L;

        private Work(UUID id, Vec3 center) {
            this.id = id;
            this.center = center;
        }
    }
}
