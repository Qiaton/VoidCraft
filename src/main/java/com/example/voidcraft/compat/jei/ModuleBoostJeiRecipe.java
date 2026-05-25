package com.example.voidcraft.compat.jei;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record ModuleBoostJeiRecipe(Identifier id, ItemStack module, ItemStack part, ItemStack output) {
}
