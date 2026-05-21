package com.example.voidcraft.ModEnchantment;

import com.example.voidcraft.VoidCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

public class VoidArcher {
    public static final ResourceKey<Enchantment> VOID_ARCHER =
            ResourceKey.create(
                    Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath(VoidCraft.MODID, "void_archer")
            );
}
