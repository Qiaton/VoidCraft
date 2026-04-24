package com.example.voidcraft.Item.custom.ModuleItem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ModuleModifierData(
    ModuleModifierType type,
    int level) {
    public static final Codec<ModuleModifierData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ModuleModifierType.MODIFIER_TYPE_CODEC.fieldOf("module_modifier_type").forGetter(ModuleModifierData::type),
            Codec.INT.fieldOf("level").forGetter(ModuleModifierData::level)
    ).apply(instance, ModuleModifierData::new));
}
