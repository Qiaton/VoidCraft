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

    public EnergyCoreData clamp(long initialMaxLifetime, long currentMaxLifetime) {
        long safeInitialMax = Math.max(0L, initialMaxLifetime);
        long safeMaxLoss = Math.max(0L, Math.min(maxLifetimeLoss, safeInitialMax));
        long safeCurrentMax = Math.max(0L, Math.min(currentMaxLifetime, safeInitialMax));
        long safeCurrentLifetime = Math.max(0L, Math.min(currentLifetime, safeCurrentMax));
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
