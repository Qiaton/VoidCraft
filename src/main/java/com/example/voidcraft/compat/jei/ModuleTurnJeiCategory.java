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

public class ModuleTurnJeiCategory implements IRecipeCategory<ModuleTurnJeiRecipe> {
    private final IDrawable icon;
    private final IDrawable arrow;

    public ModuleTurnJeiCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(ModBlockItem.MODULE_BOOST_TABLE.get());
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public IRecipeType<ModuleTurnJeiRecipe> getRecipeType() {
        return VoidCraftJeiPlugin.MODULE_TURN;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.category.void_craft.module_crafting");
    }

    @Override
    public int getWidth() {
        return 118;
    }

    @Override
    public int getHeight() {
        return 54;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ModuleTurnJeiRecipe recipe, IFocusGroup focuses) {
        int yOffset = recipe.height() == 1 ? 18 : 0;

        for (int y = 0; y < recipe.height(); y++) {
            for (int x = 0; x < recipe.width(); x++) {
                int index = x + y * recipe.width();
                builder.addSlot(RecipeIngredientRole.INPUT, x * 18 + 1, yOffset + y * 18 + 1)
                        .add(recipe.input().get(index));
            }
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, 93, 19)
                .add(recipe.output());
    }

    @Override
    public void draw(ModuleTurnJeiRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        arrow.draw(guiGraphics, 60, 18);
    }
}
