package com.example.voidcraft.compat.jei;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public record ModuleTurnJeiRecipe(Identifier id, List<Ingredient> input, ItemStack output, int width, int height) {
}
