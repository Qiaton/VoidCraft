package com.example.voidcraft.Item.custom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record EnergyCoreData(
        long currentLifetime,
        long maxLifetimeLoss,
        long lifetimeWearProgress,
        long maxLifetimeWearProgress
) {
    public static final Codec<EnergyCoreData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("currentLifetime").forGetter(EnergyCoreData::currentLifetime),
            Codec.LONG.fieldOf("maxLifetimeLoss").forGetter(EnergyCoreData::maxLifetimeLoss),
            Codec.LONG.fieldOf("lifetimeWearProgress").forGetter(EnergyCoreData::lifetimeWearProgress),
            Codec.LONG.fieldOf("maxLifetimeWearProgress").forGetter(EnergyCoreData::maxLifetimeWearProgress)
    ).apply(instance, EnergyCoreData::new));

    public EnergyCoreData clamp(long initialMaxLifetime) {
        long safeInitialMax = Math.max(0L, initialMaxLifetime);
        long safeMaxLoss = Math.max(0L, maxLifetimeLoss);
        long currentMaxLifetime = Math.max(0L, safeInitialMax - safeMaxLoss);
        long safeCurrentLifetime = Math.max(0L, Math.min(currentLifetime, currentMaxLifetime));
        long safeLifetimeProgress = Math.max(0L, lifetimeWearProgress);
        long safeMaxProgress = Math.max(0L, maxLifetimeWearProgress);

        return new EnergyCoreData(
                safeCurrentLifetime,
                safeMaxLoss,
                safeLifetimeProgress,
                safeMaxProgress
        );
    }
}
