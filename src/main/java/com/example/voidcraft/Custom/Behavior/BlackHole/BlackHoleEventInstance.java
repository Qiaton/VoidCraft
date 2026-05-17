package com.example.voidcraft.Custom.Behavior.BlackHole;

import com.example.voidcraft.Effect.VoidBlackHoleInstance;
import com.example.voidcraft.ModDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import java.util.UUID;

public class BlackHoleEventInstance {
    public final UUID owner;
    public final UUID uuid;
    public final ServerLevel level;
    public final Vec3 center;
    public final float pullRadius;
    public final float pullStrength;
    public final int totalDuration;
    public int duration;
    public final int coreColor;
    public final int color;
    public final float coreDamage;
    public final boolean hurtPlayers;
    public final boolean pullPlayers;
    public final ResourceKey<DamageType> damageType;
    public final float coreYaw;
    public VoidBlackHoleInstance.Config config = VoidBlackHoleInstance.Config.DEFAULT;
    public BlackHoleEventInstance(UUID owner, UUID uuid, ServerLevel level, Vec3 center, float pullRadius, float pullStrength, int duration, int coreColor, int color) {
        this(owner, uuid, level, center, pullRadius, pullStrength, duration, coreColor, color, 0.0F);
    }
    public BlackHoleEventInstance(UUID owner, UUID uuid, ServerLevel level, Vec3 center, float pullRadius, float pullStrength, int duration, int coreColor, int color, float coreYaw) {
        this(owner, uuid, level, center, pullRadius, pullStrength, duration, coreColor, color, 0.0F, false, false, ModDamageTypes.RIFT_TEAR, coreYaw);
    }
    public BlackHoleEventInstance(UUID owner, UUID uuid, ServerLevel level, Vec3 center, float pullRadius, float pullStrength, int duration, int coreColor, int color, float coreDamage, boolean hurtPlayers, boolean pullPlayers, ResourceKey<DamageType> damageType, float coreYaw) {
        this.owner = owner;
        this.uuid = uuid;
        this.level = level;
        this.center = center;
        this.pullRadius = pullRadius;
        this.pullStrength = pullStrength;
        this.totalDuration = Math.max(1, duration);
        this.duration = duration;
        this.coreColor = coreColor;
        this.color = color;
        this.coreDamage = coreDamage;
        this.hurtPlayers = hurtPlayers;
        this.pullPlayers = pullPlayers;
        this.damageType = damageType == null ? ModDamageTypes.RIFT_TEAR : damageType;
        this.coreYaw = coreYaw;
        setBlackHole();
    }
    public BlackHoleEventInstance(UUID owner, UUID uuid, ServerLevel level, Vec3 center, float pullRadius, float pullStrength, int coreColor, int color) {
        this(owner, uuid, level, center, pullRadius, pullStrength, 50, coreColor, color);
    }
    public BlackHoleEventInstance(UUID owner, UUID uuid, ServerLevel level, Vec3 center, float pullRadius, float pullStrength, int coreColor, int color, float coreYaw) {
        this(owner, uuid, level, center, pullRadius, pullStrength, 50, coreColor, color, coreYaw);
    }
    public void setBlackHole(){
        float gateSize = (float) Math.max(0.80D, Math.min(2.20D, this.pullRadius * 0.36D + 0.20D));
        float maskScale = (float) Math.max(2.20D, Math.min(3.40D, this.pullRadius / Math.max(0.01F, gateSize)));
        float pull = Math.max(0.10F, this.pullStrength);
        VoidBlackHoleInstance.Config blackHole = config.copy()
                .durationTicks(this.duration)
                .centerYOffset(0.75F)
                .coreFollowCameraPitch(false)
                .coreYaw(this.coreYaw)
                .distortionFollowCameraPitch(true)
                .startHalfHeight(gateSize * 0.12F)
                .peakHalfHeight(gateSize)
                .endHalfHeight(gateSize * 0.24F)
                .startHalfWidth(gateSize * 0.12F)
                .peakHalfWidth(gateSize)
                .endHalfWidth(gateSize * 0.24F)
                .peakHoldTicks((int) (this.duration * 0.62F))
                .rimAlpha(0.72F)
                .coreAlpha(1.00F)
                .rimAlphaScale(0.76F)
                .shaderRimAlphaScale(0.58F)
                .horizonAlphaScale(0.96F)
                .centerShadowScale(0.0F)
                .distortionAlpha(1.85F)
                .distortionAmplitude(Math.min(12.0F, pull * 10.0F))
                .distortionHeightScale(maskScale)
                .distortionWidthScale(maskScale)
                .distortionThickness(4.60F)
                .noiseScrollSpeed(2.20F + pull * 0.30F)
                .noiseFrequency(3.40F + pull * 0.80F)
                .swirlStrength(0.28F)
                .suctionStrength(0.96F)
                .color(this.color)
                .flatGate(false)
                .coreColor(coreColor)
                .build();
        this.config = blackHole;
    }
    public VoidBlackHoleInstance.Config getConfig() {
        return config;
    }
    public int getAgeTicks() {
        return Math.max(0, this.totalDuration - this.duration);
    }
    public float getCoreRadius() {
        return (float) Math.max(0.80D, Math.min(2.20D, this.pullRadius * 0.36D + 0.20D));
    }
}
