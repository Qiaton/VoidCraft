package com.example.voidcraft.loot;

import com.example.voidcraft.Item.ModItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

public final class MobEnergyDrops {
    private static final float CHAOS_CHANCE = 0.05F;
    private static final float NEUTRAL_CHANCE = 0.10F;
    private static final float PURE_CHANCE = 0.80F;
    private static final int PURE_ONE_WEIGHT = 100;
    private static final int PURE_TWO_WEIGHT = 50;
    private static final int PURE_THREE_WEIGHT = 5;
    private static final int PURE_FOUR_WEIGHT = 1;
    private static final int PURE_TOTAL_WEIGHT = PURE_ONE_WEIGHT + PURE_TWO_WEIGHT + PURE_THREE_WEIGHT + PURE_FOUR_WEIGHT;

    private MobEnergyDrops() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(MobEnergyDrops::onLivingDrops);
    }

    private static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !event.isRecentlyHit()) {
            return;
        }

        Item item = getItem(entity);
        if (item == null) {
            return;
        }

        float chance = getChance(entity);
        if (entity.getRandom().nextFloat() < chance) {
            addDrop(event, entity, item, getCount(entity, item));
        }
    }

    private static Item getItem(LivingEntity entity) {
        if (isMiddleMob(entity)) {
            return ModItem.NEUTRAL_ENERGY.get();
        }

        if (isBadMob(entity)) {
            return ModItem.CHAOS_ENERGY.get();
        }

        if (isGoodMob(entity)) {
            return ModItem.PURE_ENERGY.get();
        }

        return null;
    }

    private static float getChance(LivingEntity entity) {
        if (isMiddleMob(entity)) {
            return NEUTRAL_CHANCE;
        }

        if (isBadMob(entity)) {
            return CHAOS_CHANCE;
        }

        return PURE_CHANCE;
    }

    private static int getCount(LivingEntity entity, Item item) {
        if (item == ModItem.PURE_ENERGY.get() || item == ModItem.NEUTRAL_ENERGY.get()) {
            return getPureCount(entity);
        }

        if (item != ModItem.CHAOS_ENERGY.get()) {
            return 1;
        }

        return 1;
    }

    private static int getPureCount(LivingEntity entity) {
        int roll = entity.getRandom().nextInt(PURE_TOTAL_WEIGHT);
        if (roll < PURE_FOUR_WEIGHT) {
            return 4;
        }

        roll -= PURE_FOUR_WEIGHT;
        if (roll < PURE_THREE_WEIGHT) {
            return 3;
        }

        roll -= PURE_THREE_WEIGHT;
        if (roll < PURE_TWO_WEIGHT) {
            return 2;
        }

        return 1;
    }

    private static boolean isBadMob(LivingEntity entity) {
        return entity instanceof Enemy || entity.getType().getCategory() == MobCategory.MONSTER;
    }

    private static boolean isMiddleMob(LivingEntity entity) {
        return entity instanceof NeutralMob;
    }

    private static boolean isGoodMob(LivingEntity entity) {
        return !(entity instanceof Player) && entity.getType().getCategory().isFriendly();
    }

    private static void addDrop(LivingDropsEvent event, LivingEntity entity, Item item, int count) {
        event.getDrops().add(new ItemEntity(
                entity.level(),
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                new ItemStack(item, count)
        ));
    }
}
