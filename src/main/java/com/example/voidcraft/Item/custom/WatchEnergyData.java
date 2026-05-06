package com.example.voidcraft.Item.custom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record WatchEnergyData(long energy) {
    public static final Codec<WatchEnergyData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("energy").forGetter(WatchEnergyData::energy)
    ).apply(instance, WatchEnergyData::new));
}
