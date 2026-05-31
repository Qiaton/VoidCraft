package com.example.voidcraft.Item.custom.ModuleItem.ModuleType;

import com.example.voidcraft.ClientCustom.ModuleInputMode;
import com.example.voidcraft.Custom.Behavior.BlackHole.BlackHoleEventManager;
import com.example.voidcraft.Custom.Clock.ModuleSkillClock;
import com.example.voidcraft.Effect.VoidBlackHoleInstance;
import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleData;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleItem;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleMode;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierData;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierType;
import com.example.voidcraft.ModDataComponents;
import com.example.voidcraft.ModDamageTypes;
import com.example.voidcraft.Network.ModNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.example.voidcraft.Item.custom.ModuleItem.ModuleMode.BURST;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierType.ACTIVE_DURATION;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierType.COOLDOWN_REDUCTION;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierType.SPEED_BOOST;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleStatHelper.addLess;

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
    private static final VoidRingInstance.Preset ARRIVAL_LIGHT = makeArrivalLight(CORE_COLOR);
    private static final VoidBlackHoleInstance.Config PREVIEW_BLACK_HOLE = makePreviewBlackHole(CORE_COLOR, COLOR);

    protected static VoidRingInstance.Preset makeArrivalLight(int color) {
        return VoidRingInstance.Preset.builder()
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
            .color(color)
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
    }

    protected static VoidBlackHoleInstance.Config makePreviewBlackHole(int coreColor, int color) {
        return VoidBlackHoleInstance.Config.DEFAULT.toBuilder()
                .durationTicks(1)
                .centerYOffset(0.75F)
                .coreFollowCameraPitch(false)
                .distortionFollowCameraPitch(true)
                .startHalfHeight(0.14F)
                .peakHalfHeight(0.52F)
                .endHalfHeight(0.20F)
                .startHalfWidth(0.14F)
                .peakHalfWidth(0.52F)
                .endHalfWidth(0.20F)
                .peakHoldTicks(1)
                .rimAlpha(0.58F)
                .coreAlpha(0.88F)
                .centerShadowScale(0.0F)
                .distortionAlpha(0.0F)
                .distortionAmplitude(0.0F)
                .swirlStrength(0.0F)
                .suctionStrength(0.0F)
                .color(color)
                .coreColor(coreColor)
                .build();
    }

    public BlackHoleModule(Properties properties) {
        super(properties);
    }

    @Override
    protected void doUseSkill(ServerPlayer player, ItemStack watchStack, ItemStack moduleStack, int slot) {
        Stats stats = getBlackHoleStats(moduleStack);
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

        Stats stats = getBlackHoleStats(moduleStack);
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
                getCoreColor(),
                getColor(),
                getCoreDamage(stats),
                canHurtPlayers(),
                canPullPlayers(),
                getCoreDamageType()
        );

        ModuleSkillClock.startRunCooldown(player, slot, stats.durationTicks(), stats.burstCooldownTicks());
    }

    private static boolean canUseTarget(ServerPlayer player, Vec3 target, Stats stats) {
        if (!Double.isFinite(target.x) || !Double.isFinite(target.y) || !Double.isFinite(target.z)) {
            return false;
        }

        return player.getEyePosition().distanceTo(target) <= stats.maxDistance() + 3.5D;
    }

    private void sendArrivalLight(ServerLevel level, Vec3 center, Stats stats) {
        Vec3 lightCenter = center.add(0.0D, BLACK_HOLE_CENTER_Y_OFFSET, 0.0D);
        ModNetworking.sendPhaseTearAt(level, lightCenter, getArrivalLightScale(stats), getArrivalLight());
    }

    private static float getArrivalLightScale(Stats stats) {
        float gateSize = (float) Math.max(0.80D, Math.min(2.20D, stats.radius() * 0.36D + 0.20D));
        return gateSize * ARRIVAL_LIGHT_SIZE;
    }

    public Stats getBlackHoleStats(ItemStack moduleStack) {
        return getStats(moduleStack);
    }

    public VoidBlackHoleInstance.Config getPreviewBlackHole() {
        return PREVIEW_BLACK_HOLE;
    }

    protected VoidRingInstance.Preset getArrivalLight() {
        return ARRIVAL_LIGHT;
    }

    protected int getCoreColor() {
        return CORE_COLOR;
    }

    protected int getColor() {
        return COLOR;
    }

    protected float getCoreDamage(Stats stats) {
        return 0.0F;
    }

    protected boolean canHurtPlayers() {
        return false;
    }

    protected boolean canPullPlayers() {
        return false;
    }

    protected ResourceKey<DamageType> getCoreDamageType() {
        return ModDamageTypes.RIFT_TEAR;
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
        strength += strength * speedLevel * 0.10F;
        int baseDuration = BASE_DURATION_TICKS + DURATION_TICKS_PER_LEVEL * extraLevel;
        int durationTicks = Math.round(baseDuration + baseDuration * activeDurationLevel * 0.20F);
        float cooldownReduction = addLess(1.0F, cooldownLevel, 0.12F);
        float energyReduction = addLess(1.0F, cooldownLevel, 0.10F);
        long cooldownTicks = Math.round(BURST_COOLDOWN_TICKS / cooldownReduction);
        long energyCost = Math.round(BURST_ENERGY_COST / energyReduction);
        double maxDistance = BASE_DISTANCE + DISTANCE_PER_LEVEL * extraLevel;

        return new Stats(
                data.moduleMode(),
                moduleLevel,
                radius,
                strength,
                durationTicks,
                cooldownTicks,
                energyCost,
                maxDistance
        );
    }

    @Override
    public ModuleInputMode getInputMode() {
        return ModuleInputMode.HOLD_RELEASE;
    }

    @Override
    public boolean canUseMode(ModuleMode mode) {
        return mode == BURST;
    }

    @Override
    protected Component getModifierDisplayName(ModuleModifierData modifierData) {
        if (modifierData.type() == SPEED_BOOST) {
            return Component.translatable("module_modifier_type.void_craft.black_hole.gravity_bonus");
        }
        return super.getModifierDisplayName(modifierData);
    }

    public record Stats(
            ModuleMode mode,
            int level,
            float radius,
            float strength,
            int durationTicks,
            long burstCooldownTicks,
            long burstEnergyCost,
            double maxDistance
    ) {
    }
}
