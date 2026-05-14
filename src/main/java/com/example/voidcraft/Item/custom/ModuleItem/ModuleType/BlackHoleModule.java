package com.example.voidcraft.Item.custom.ModuleItem.ModuleType;

import com.example.voidcraft.ClientCustom.ModuleInputMode;
import com.example.voidcraft.Custom.Behavior.BlackHole.BlackHoleEventManager;
import com.example.voidcraft.Custom.Clock.ModuleSkillClock;
import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleData;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleItem;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleMode;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierData;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierType;
import com.example.voidcraft.ModDataComponents;
import com.example.voidcraft.Network.ModNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.example.voidcraft.Item.custom.ModuleItem.ModuleMode.BURST;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierType.ACTIVE_DURATION;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierType.COOLDOWN_REDUCTION;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierType.SPEED_BOOST;

public class BlackHoleModule extends ModuleItem {
    public static final double MIN_DISTANCE = 2.0D;
    private static final double BASE_DISTANCE = 12.0D;
    private static final double DISTANCE_PER_LEVEL = 8.0D;
    private static final float BASE_RADIUS = 1.0F;
    private static final float RADIUS_PER_LEVEL = 1.0F;
    private static final float BASE_STRENGTH = 0.1F;
    private static final float STRENGTH_PER_LEVEL = 0.2F;
    private static final int BASE_DURATION_TICKS = 50;
    private static final int DURATION_TICKS_PER_LEVEL = 10;
    private static final long BURST_COOLDOWN_TICKS = 2400L;
    private static final long BURST_ENERGY_COST = 2400L;
    private static final int CORE_COLOR = 0x02030A;
    private static final int COLOR = 0x6D7DFF;
    private static final float BLACK_HOLE_CENTER_Y_OFFSET = 0.75F;
    private static final float ARRIVAL_LIGHT_SIZE = 1.5F;
    private static final VoidRingInstance.Preset ARRIVAL_LIGHT = VoidRingInstance.Preset.builder()
            .durationTicks(10)
            .peakHoldTicks(2)
            .centerYOffset(0.0F)
            .followCameraPitch(true)
            .distortionFollowCameraPitch(true)
            .startHalfHeight(0.48F)
            .peakHalfHeight(1.0F)
            .endHalfHeight(0.18F)
            .startHalfWidth(0.48F)
            .peakHalfWidth(1.0F)
            .endHalfWidth(0.18F)
            .glowAlpha(0.92F)
            .glowWidthScale(1.55F)
            .glowHeightScale(1.55F)
            .shaderGlowWidthScale(2.10F)
            .shaderGlowHeightScale(2.10F)
            .shaderCompatOuterGlowGain(2.20F)
            .shaderCompatCoreGain(1.80F)
            .shaderCompatLineGain(2.00F)
            .shaderCompatBloomGain(2.35F)
            .shaderCompatBloomAlphaScale(0.92F)
            .coreAlpha(1.0F)
            .distortionAlpha(3.40F)
            .lineAlpha(1.0F)
            .color(CORE_COLOR)
            .filledFadeStart(0.40F)
            .swirlStrength(0.20F)
            .suctionStrength(0.40F)
            .distortionThickness(6.20F)
            .distortionAmplitude(15.50F)
            .distortionWidthScale(7.0F)
            .distortionHeightScale(7.0F)
            .noiseFrequency(3.60F)
            .noiseScrollSpeed(5.20F)
            .build();

    public BlackHoleModule(Properties properties) {
        super(properties);
    }

    @Override
    protected void doUseSkill(ServerPlayer player, ItemStack watchStack, ItemStack moduleStack, int slot) {
        Stats stats = getStats(moduleStack);
        if (stats == null || stats.mode() != BURST) {
            return;
        }

        Vec3 target = player.getEyePosition().add(player.getLookAngle().normalize().scale(stats.maxDistance()));
        releaseBlackHole(player, watchStack, moduleStack, slot, target.x, target.y, target.z);
    }

    public void releaseBlackHole(
            ServerPlayer player,
            ItemStack watchStack,
            ItemStack moduleStack,
            int slot,
            double x,
            double y,
            double z
    ) {
        if (watchStack.isEmpty()) return;
        if (player == null) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        Stats stats = getStats(moduleStack);
        if (stats == null) return;
        if (stats.mode() != BURST) return;

        Vec3 center = new Vec3(x, y, z);
        if (!canUseTarget(player, center, stats)) {
            return;
        }

        boolean cooldownReady = ModuleSkillClock.canUseNow(player, slot);
        if (!cooldownReady && !ModuleSkillClock.tryUseEnergy(player, stats.burstEnergyCost())) {
            return;
        }

        sendArrivalLight(level, center, stats);
        BlackHoleEventManager.addBurst(
                player,
                level,
                center,
                stats.radius(),
                stats.strength(),
                stats.durationTicks(),
                CORE_COLOR,
                COLOR
        );

        if (cooldownReady) {
            ModuleSkillClock.setCooldown(player, slot, stats.burstCooldownTicks());
        }
    }

    private static boolean canUseTarget(ServerPlayer player, Vec3 target, Stats stats) {
        if (!Double.isFinite(target.x) || !Double.isFinite(target.y) || !Double.isFinite(target.z)) {
            return false;
        }

        return player.getEyePosition().distanceTo(target) <= stats.maxDistance() + 3.5D;
    }

    private static void sendArrivalLight(ServerLevel level, Vec3 center, Stats stats) {
        Vec3 lightCenter = center.add(0.0D, BLACK_HOLE_CENTER_Y_OFFSET, 0.0D);
        ModNetworking.sendPhaseTearAt(level, lightCenter, getArrivalLightScale(stats), ARRIVAL_LIGHT);
    }

    private static float getArrivalLightScale(Stats stats) {
        float gateSize = (float) Math.max(0.80D, Math.min(2.20D, stats.radius() * 0.36D + 0.20D));
        return gateSize * ARRIVAL_LIGHT_SIZE;
    }

    public static Stats getStats(ItemStack moduleStack) {
        ModuleData data = moduleStack.get(ModDataComponents.MODULE_DATA.get());
        if (data == null) return null;

        int moduleLevel = Math.max(1, data.level());
        int extraLevel = moduleLevel - 1;
        int cooldownLevel = 0;
        int speedLevel = 0;
        int activeDurationLevel = 0;
        List<ModuleModifierData> modifiers = data.modifiers();

        for (ModuleModifierData modifier : modifiers) {
            ModuleModifierType modifierType = modifier.type();
            if (modifierType == null) continue;
            if (modifierType == COOLDOWN_REDUCTION) {
                cooldownLevel += modifier.level();
            }
            if (modifierType == SPEED_BOOST) {
                speedLevel += modifier.level();
            }
            if (modifierType == ACTIVE_DURATION) {
                activeDurationLevel += modifier.level();
            }
        }

        float radius = BASE_RADIUS + RADIUS_PER_LEVEL * extraLevel;
        float strength = BASE_STRENGTH + STRENGTH_PER_LEVEL * extraLevel;
        strength = grow(strength, speedLevel, 0.10F);
        int baseDuration = BASE_DURATION_TICKS + DURATION_TICKS_PER_LEVEL * extraLevel;
        int durationTicks = Math.round(grow(baseDuration, activeDurationLevel, 0.20F));
        long cooldownTicks = Math.round(shrink(BURST_COOLDOWN_TICKS, cooldownLevel, 0.12F));
        long energyCost = Math.round(shrink(BURST_ENERGY_COST, cooldownLevel, 0.10F));
        double maxDistance = BASE_DISTANCE + DISTANCE_PER_LEVEL * extraLevel;

        return new Stats(
                data.moduleMode(),
                radius,
                strength,
                durationTicks,
                cooldownTicks,
                energyCost,
                maxDistance
        );
    }

    private static float grow(float value, int level, float rate) {
        for (int i = 0; i < level; i++) {
            value *= 1.0F + rate;
        }
        return value;
    }

    private static float shrink(float value, int level, float rate) {
        for (int i = 0; i < level; i++) {
            value *= 1.0F - rate;
        }
        return value;
    }

    @Override
    public ModuleInputMode getInputMode() {
        return ModuleInputMode.HOLD_RELEASE;
    }

    @Override
    public boolean canUseMode(ModuleMode mode) {
        return mode == BURST;
    }

    public record Stats(
            ModuleMode mode,
            float radius,
            float strength,
            int durationTicks,
            long burstCooldownTicks,
            long burstEnergyCost,
            double maxDistance
    ) {
    }
}
