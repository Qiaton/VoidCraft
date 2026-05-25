package com.example.voidcraft.compat.jei;

import com.example.voidcraft.Block.ModBlockItem;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class ModuleBoostJeiCategory implements IRecipeCategory<ModuleBoostJeiRecipe> {
    private final IDrawable icon;
    private final IDrawable arrow;

    public ModuleBoostJeiCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(ModBlockItem.MODULE_BOOST_TABLE.get());
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public IRecipeType<ModuleBoostJeiRecipe> getRecipeType() {
        return VoidCraftJeiPlugin.MODULE_BOOST;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.category.void_craft.module_boost");
    }

    @Override
    public int getWidth() {
        return 118;
    }

    @Override
    public int getHeight() {
        return 58;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ModuleBoostJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 1)
                .add(recipe.part());
        builder.addSlot(RecipeIngredientRole.INPUT, 19, 25)
                .add(recipe.module());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 93, 21)
                .add(recipe.output());
    }

    @Override
    public void draw(ModuleBoostJeiRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        arrow.draw(guiGraphics, 58, 20);
    }
}
