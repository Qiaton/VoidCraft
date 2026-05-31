package com.example.voidcraft.compat.jei;

import com.example.voidcraft.Block.ModBlockItem;
import com.example.voidcraft.Item.ModItem;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleData;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierData;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierItem;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierType;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleMode;
import com.example.voidcraft.ModDataComponents;
import com.example.voidcraft.VoidCraft;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

@JeiPlugin
public class VoidCraftJeiPlugin implements IModPlugin {
    public static final IRecipeType<ModuleTurnJeiRecipe> MODULE_TURN = IRecipeType.create(name("module_turn"), ModuleTurnJeiRecipe.class);
    public static final IRecipeType<ModuleBoostJeiRecipe> MODULE_BOOST = IRecipeType.create(name("module_boost"), ModuleBoostJeiRecipe.class);

    @Override
    public Identifier getPluginUid() {
        return name("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new ModuleTurnJeiCategory(registration.getJeiHelpers().getGuiHelper()),
                new ModuleBoostJeiCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(MODULE_TURN, List.of(
                recipe("phase_turret_module_item", ModItem.PHASE_TURRET_MODULE.get(), 3, 3,
                        item(Items.IRON_INGOT), item(ModItem.CHAOS_ENERGY.get()), item(Items.IRON_INGOT),
                        item(Items.IRON_INGOT), item(ModItem.MODULE_ITEM.get()), item(Items.IRON_INGOT),
                        item(Items.BLAZE_ROD), item(ModItem.CHAOS_ENERGY.get()), item(Items.BLAZE_ROD)),
                recipe("assist_phase_turret_module_item", ModItem.ASSIST_PHASE_TURRET_MODULE.get(), 3, 1,
                        item(Items.DIAMOND), item(ModItem.PHASE_TURRET_MODULE.get(), ModItem.ASSIST_PHASE_TURRET_MODULE.get(), ModItem.HEALTH_PHASE_TURRET_MODULE.get(), ModItem.HEALTH_ASSIST_PHASE_TURRET_MODULE.get()), item(Items.DIAMOND)),
                recipe("health_phase_turret_module_item", ModItem.HEALTH_PHASE_TURRET_MODULE.get(), 3, 3,
                        empty(), item(ModItem.VOID_ENERGY.get()), empty(),
                        empty(), item(ModItem.PHASE_TURRET_MODULE.get(), ModItem.ASSIST_PHASE_TURRET_MODULE.get(), ModItem.HEALTH_PHASE_TURRET_MODULE.get(), ModItem.HEALTH_ASSIST_PHASE_TURRET_MODULE.get()), empty(),
                        item(ModItem.PURE_ENERGY.get()), item(ModItem.PURE_ENERGY.get()), item(ModItem.PURE_ENERGY.get())),
                recipe("health_void_module_item", ModItem.HEALTH_VOID_MODULE.get(), 3, 3,
                        empty(), item(ModItem.VOID_ENERGY.get()), empty(),
                        empty(), item(ModItem.MODULE_ITEM.get()), empty(),
                        item(ModItem.PURE_ENERGY.get()), item(ModItem.PURE_ENERGY.get()), item(ModItem.PURE_ENERGY.get())),
                recipe("dash_void_module_item", ModItem.DASH_VOID_MODULE.get(), 3, 3,
                        item(ModItem.CHAOS_ENERGY.get()), item(ModItem.MODULE_ITEM.get()), item(ModItem.CHAOS_ENERGY.get()),
                        item(ModItem.CHAOS_ENERGY.get()), item(ModItem.NEUTRAL_ENERGY.get()), item(ModItem.CHAOS_ENERGY.get()),
                        empty(), item(ModItem.NEUTRAL_ENERGY.get()), empty()),
                recipe("blink_void_module_item", ModItem.BLINK_VOID_MODULE.get(), 3, 3,
                        empty(), item(ModItem.BASIC_ENERGY_CORE.get()), empty(),
                        item(ModItem.NEUTRAL_ENERGY.get()), item(ModItem.MODULE_ITEM.get()), item(ModItem.NEUTRAL_ENERGY.get()),
                        empty(), item(ModItem.BASIC_ENERGY_CORE.get()), empty()),
                recipe("safe_blink_void_module_item", ModItem.SAFE_BLINK_VOID_MODULE.get(), 3, 1,
                        item(Items.DIAMOND), item(ModItem.BLINK_VOID_MODULE.get(), ModItem.SAFE_BLINK_VOID_MODULE.get()), item(Items.DIAMOND)),
                recipe("teleport_void_module_item", ModItem.TELEPORT_VOID_MODULE.get(), 3, 3,
                        item(ModItem.CHAOS_ENERGY.get()), item(ModItem.CHAOS_ENERGY.get()), item(ModItem.CHAOS_ENERGY.get()),
                        item(ModItem.NEUTRAL_ENERGY.get()), item(ModItem.PURE_VOID_CRYSTAL.get()), item(ModItem.NEUTRAL_ENERGY.get()),
                        item(ModItem.NEUTRAL_ENERGY.get()), item(ModItem.MODULE_ITEM.get()), item(ModItem.NEUTRAL_ENERGY.get())),
                recipe("world_module_item", ModItem.WORLD_MODULE.get(), 3, 3,
                        item(ModItem.VOID_ENERGY.get()), item(ModItem.VOID_ENERGY.get()), item(ModItem.VOID_ENERGY.get()),
                        item(ModItem.VOID_ENERGY.get()), item(ModItem.MODULE_ITEM.get()), item(ModItem.VOID_ENERGY.get()),
                        item(ModItem.VOID_ENERGY.get()), item(ModItem.HIGH_PURITY_VOID_CRYSTAL.get()), item(ModItem.VOID_ENERGY.get())),
                recipe("black_hole_module_item", ModItem.BLACK_HOLE_MODULE.get(), 3, 3,
                        item(ModItem.VOID_ENERGY.get()), item(ModItem.PURE_VOID_CRYSTAL.get()), item(ModItem.VOID_ENERGY.get()),
                        item(ModItem.PURE_VOID_CRYSTAL.get()), item(ModItem.MODULE_ITEM.get()), item(ModItem.PURE_VOID_CRYSTAL.get()),
                        item(ModItem.VOID_ENERGY.get()), item(ModItem.PURE_VOID_CRYSTAL.get()), item(ModItem.VOID_ENERGY.get())),
                recipe("tear_black_hole_module_item", ModItem.TEAR_BLACK_HOLE_MODULE.get(), 3, 3,
                        item(ModItem.CHAOS_ENERGY.get(), ModItem.PURE_ENERGY.get()), item(ModItem.CHAOS_ENERGY.get(), ModItem.PURE_ENERGY.get()), item(ModItem.CHAOS_ENERGY.get(), ModItem.PURE_ENERGY.get()),
                        item(ModItem.CHAOS_ENERGY.get(), ModItem.PURE_ENERGY.get()), item(ModItem.BLACK_HOLE_MODULE.get(), ModItem.TEAR_BLACK_HOLE_MODULE.get()), item(ModItem.CHAOS_ENERGY.get(), ModItem.PURE_ENERGY.get()),
                        item(ModItem.CHAOS_ENERGY.get(), ModItem.PURE_ENERGY.get()), item(ModItem.CHAOS_ENERGY.get(), ModItem.PURE_ENERGY.get()), item(ModItem.CHAOS_ENERGY.get(), ModItem.PURE_ENERGY.get())),
                recipe("annihilation_black_hole_module_item", ModItem.ANNIHILATION_BLACK_HOLE_MODULE.get(), 3, 3,
                        item(ModItem.CHAOS_ENERGY.get()), item(ModItem.VOID_ENERGY.get()), item(ModItem.CHAOS_ENERGY.get()),
                        item(ModItem.CHAOS_ENERGY.get()), item(ModItem.TEAR_BLACK_HOLE_MODULE.get()), item(ModItem.CHAOS_ENERGY.get()),
                        item(ModItem.CHAOS_ENERGY.get()), item(ModItem.VOID_ENERGY.get()), item(ModItem.CHAOS_ENERGY.get()))
        ));

        registration.addRecipes(MODULE_BOOST, boostRecipes());
        registration.addItemStackInfo(moduleStack(List.of()), info("jei.info.void_craft.void_module"));
        for (ModuleModifierType type : ModuleModifierType.values()) {
            registration.addItemStackInfo(modifierStack(type), info("jei.info.void_craft.module_modifier"));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(MODULE_BOOST, ModBlockItem.MODULE_BOOST_TABLE.get());
        registration.addCraftingStation(MODULE_TURN, Items.CRAFTING_TABLE);
        registration.addCraftingStation(RecipeTypes.CRAFTING, ModBlockItem.MODULE_BOOST_TABLE.get());
    }

    private List<ModuleBoostJeiRecipe> boostRecipes() {
        return java.util.Arrays.stream(ModuleModifierType.values())
                .map(type -> new ModuleBoostJeiRecipe(
                        name("module_boost/" + type.getId()),
                        moduleStack(List.of()),
                        modifierStack(type),
                        moduleStack(List.of(new ModuleModifierData(type, 5)))
                ))
                .toList();
    }

    private ModuleTurnJeiRecipe recipe(String id, Item output, int width, int height, Ingredient... input) {
        return new ModuleTurnJeiRecipe(name(id), List.of(input), new ItemStack(output), width, height);
    }

    private static Identifier name(String id) {
        return Identifier.fromNamespaceAndPath(VoidCraft.MODID, id);
    }

    private ItemStack moduleStack(List<ModuleModifierData> modifiers) {
        ItemStack stack = new ItemStack(ModItem.MODULE_ITEM.get());
        stack.set(ModDataComponents.MODULE_DATA.value(), new ModuleData(ModuleMode.BURST, 5, modifiers));
        return stack;
    }

    private ItemStack modifierStack(ModuleModifierType type) {
        ItemStack stack = new ItemStack(ModItem.MODULE_MODIFIER_ITEM.get());
        ModuleModifierItem.setData(stack, new ModuleModifierData(type, 5));
        return stack;
    }

    private Ingredient item(Item... item) {
        return Ingredient.of(item);
    }

    private Ingredient empty() {
        return Ingredient.of();
    }

    private Component[] info(String key) {
        return new Component[]{
                Component.translatable(key + ".1"),
                Component.translatable(key + ".2"),
                Component.translatable(key + ".3"),
                Component.translatable(key + ".4")
        };
    }
}
