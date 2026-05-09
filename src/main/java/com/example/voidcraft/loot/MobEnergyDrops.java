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
    private static final float CHAOS_CHANCE = 0.008F;
    private static final float NEUTRAL_CHANCE = 0.02F;
    private static final float PURE_CHANCE = 0.05F;

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
            addDrop(event, entity, item);
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

    private static boolean isBadMob(LivingEntity entity) {
        return entity instanceof Enemy || entity.getType().getCategory() == MobCategory.MONSTER;
    }

    private static boolean isMiddleMob(LivingEntity entity) {
        return entity instanceof NeutralMob;
    }

    private static boolean isGoodMob(LivingEntity entity) {
        return !(entity instanceof Player) && entity.getType().getCategory().isFriendly();
    }

    private static void addDrop(LivingDropsEvent event, LivingEntity entity, Item item) {
        event.getDrops().add(new ItemEntity(
                entity.level(),
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                new ItemStack(item)
        ));
    }
}
