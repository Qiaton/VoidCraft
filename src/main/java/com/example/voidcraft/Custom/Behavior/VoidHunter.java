package com.example.voidcraft.Custom.Behavior;

import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.ModDamageTypes;
import com.example.voidcraft.Network.ModNetworking;
import com.example.voidcraft.Sound.ModSound;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@EventBusSubscriber
public class VoidHunter {
    private static final float KILL_HEALTH_PER_LEVEL = 0.05F;
    private static final float AREA_DAMAGE_MULTIPLIER = 0.6F;
    private static final double AREA_SIZE_MULTIPLIER = 3.0D;
    private static final int MAX_HITS_PER_TICK = 10;
    private static final float LIGHT_YAW_DEGREES = 0.0F;
    private static final float LIGHT_TURN_DEGREES = 90.0F;
    private static final float LIGHT_TURN_RANDOM_DEGREES = 40.0F;
    private static final List<HunterRun> RUNS = new ArrayList<>();
    private static final VoidRingInstance.Preset LIGHT = makeLight();

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        DamageSource source = event.getSource();
        if (source.is(ModDamageTypes.VOID_HUNTER)) {
            return;
        }

        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) {
            return;
        }

        ItemStack stack = getStack(source, livingAttacker);
        int level = getLevel(serverLevel, stack);
        if (level <= 0 || !canKill(target, level)) {
            return;
        }

        startHit(serverLevel, livingAttacker, target, level, event.getOriginalDamage() * AREA_DAMAGE_MULTIPLIER);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            runHits(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            clearLevelHits(serverLevel);
        }
    }

    private static void startHit(
            ServerLevel serverLevel,
            LivingEntity attacker,
            LivingEntity target,
            int level,
            float areaDamage
    ) {
        HunterRun run = new HunterRun(serverLevel, attacker, level, areaDamage, new HashSet<>(), new ArrayDeque<>());
        addHit(run, target);
        if (!run.hits().isEmpty()) {
            RUNS.add(run);
        }
    }

    private static void runHits(ServerLevel serverLevel) {
        int count = 0;
        Iterator<HunterRun> iterator = RUNS.iterator();
        while (iterator.hasNext() && count < MAX_HITS_PER_TICK) {
            HunterRun run = iterator.next();
            if (run.serverLevel() != serverLevel) {
                continue;
            }

            while (!run.hits().isEmpty() && count < MAX_HITS_PER_TICK) {
                hitOne(run, run.hits().poll());
                count++;
            }

            if (run.hits().isEmpty()) {
                iterator.remove();
            }
        }
    }

    private static void hitOne(HunterRun run, LivingEntity target) {
        if (target == null || target.is(run.attacker())) {
            return;
        }

        if (canKill(target, run.level())) {
            sendLight(run.serverLevel(), target);
            if (target.isAlive()) {
                target.hurtServer(run.serverLevel(), makeDamageSource(run.attacker()), Float.MAX_VALUE);
            }
            hitArea(run, target);
        } else if (target.isAlive() && run.areaDamage() > 0.0F) {
            target.hurtServer(run.serverLevel(), makeDamageSource(run.attacker()), run.areaDamage());
        }
    }

    private static void hitArea(HunterRun run, LivingEntity center) {
        AABB box = makeBox(center);
        List<LivingEntity> targets = run.serverLevel().getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive);
        for (LivingEntity target : targets) {
            if (target.is(center)) {
                continue;
            }

            addHit(run, target);
        }
    }

    private static void addHit(HunterRun run, LivingEntity target) {
        if (target == null || target.is(run.attacker()) || run.hitTargets().contains(target.getId())) {
            return;
        }

        run.hitTargets().add(target.getId());
        run.hits().add(target);
    }

    private static void clearLevelHits(ServerLevel serverLevel) {
        RUNS.removeIf(run -> run.serverLevel() == serverLevel);
    }

    private static boolean canKill(LivingEntity target, int level) {
        return target.getHealth() < target.getMaxHealth() * KILL_HEALTH_PER_LEVEL * level;
    }

    private static AABB makeBox(LivingEntity entity) {
        AABB box = entity.getBoundingBox();
        double xSize = Math.max(0.5D, box.getXsize());
        double ySize = Math.max(0.5D, box.getYsize());
        double zSize = Math.max(0.5D, box.getZsize());
        return box.inflate(xSize * AREA_SIZE_MULTIPLIER, ySize * AREA_SIZE_MULTIPLIER, zSize * AREA_SIZE_MULTIPLIER);
    }

    private static ItemStack getStack(DamageSource source, LivingEntity attacker) {
        ItemStack stack = source.getWeaponItem();
        if (stack != null && !stack.isEmpty()) {
            return stack;
        }
        return attacker.getMainHandItem();
    }

    private static int getLevel(ServerLevel level, ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(ItemTags.SWORDS)) {
            return 0;
        }

        HolderLookup<Enchantment> enchantments = level.holderLookup(Registries.ENCHANTMENT);
        Optional<Holder.Reference<Enchantment>> voidHunter = enchantments.get(
                com.example.voidcraft.ModEnchantment.VoidHunter.VOID_HUNTER
        );
        if (voidHunter.isEmpty()) {
            return 0;
        }

        return stack.getEnchantmentLevel(voidHunter.get());
    }

    private static DamageSource makeDamageSource(LivingEntity attacker) {
        return attacker.damageSources().source(ModDamageTypes.VOID_HUNTER, attacker, attacker);
    }

    private static void sendLight(ServerLevel serverLevel, LivingEntity target) {
        Vec3 position = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        float scale = Math.max(0.4F, target.getBbHeight() / 1.8F);
        VoidRingInstance.Preset light = LIGHT.copy()
                .ellipseTurn(getLightTurn(serverLevel))
                .build();
        ModNetworking.sendPhaseTearAt(serverLevel, position, scale, getLightYaw(), light);
        ModSound.playVoidArcherHit(serverLevel, position);
    }

    private static float getLightYaw() {
        return (float) Math.toRadians(LIGHT_YAW_DEGREES);
    }

    private static float getLightTurn(ServerLevel serverLevel) {
        float minDegrees = LIGHT_TURN_DEGREES - LIGHT_TURN_RANDOM_DEGREES;
        float maxDegrees = LIGHT_TURN_DEGREES + LIGHT_TURN_RANDOM_DEGREES;
        return (minDegrees + serverLevel.random.nextFloat() * (maxDegrees - minDegrees)) / 360.0F;
    }

    private static VoidRingInstance.Preset makeLight() {
        return VoidRingInstance.Preset.builder()
                .durationTicks(5)
                .peakHoldTicks(1)
                .centerYOffset(0.0F)
                .followCameraYaw(true)
                .followCameraPitch(true)
                .distortionFollowCameraYaw(false)
                .startHalfHeight(0.5F)
                .peakHalfHeight(2.2F)
                .endHalfHeight(0.52F)
                .startHalfWidth(0.1F)
                .peakHalfWidth(0.7F)
                .endHalfWidth(0.1F)
                .coreAlpha(0.96F)
                .distortionAlpha(3.92F)
                .lineAlpha(0.85F)
                .distortionThickness(2.56F)
                .distortionAmplitude(10.78F)
                .distortionWidthScale(1.06F)
                .distortionHeightScale(1.04F)
                .noiseFrequency(8.6F)
                .noiseScrollSpeed(6.68F)
                .build();
    }

    private record HunterRun(
            ServerLevel serverLevel,
            LivingEntity attacker,
            int level,
            float areaDamage,
            Set<Integer> hitTargets,
            ArrayDeque<LivingEntity> hits
    ) {
    }
}
