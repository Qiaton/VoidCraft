package com.example.voidcraft.Item.custom.ModuleItem;


import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static com.example.voidcraft.Item.custom.ModuleItem.ModuleMode.MODE_CODEC;

public record ModuleData(ModuleMode moduleMode,
                         int level,
                         List<ModuleModifierData> modifiers
) {
    public static final Codec<ModuleData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MODE_CODEC.fieldOf("moduleMode").forGetter(ModuleData::moduleMode),
            Codec.INT.fieldOf("level").forGetter(ModuleData::level),
            ModuleModifierData.CODEC.listOf().fieldOf("modifiers").forGetter(ModuleData::modifiers)
    ).apply(instance, ModuleData::new));
}
