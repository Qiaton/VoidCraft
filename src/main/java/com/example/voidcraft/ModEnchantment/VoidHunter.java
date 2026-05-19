package com.example.voidcraft.ModEnchantment;

import com.example.voidcraft.VoidCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

public class VoidHunter {
    public static final ResourceKey<Enchantment> VOID_HUNTER =
            ResourceKey.create(
                    Registries.ENCHANTMENT,
                    Identifier.fromNamespaceAndPath(VoidCraft.MODID, "void_hunter")
            );
}
