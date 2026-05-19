package com.example.voidcraft.Recipe;

import com.example.voidcraft.Item.ModItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class ModuleTurnRecipe extends CustomRecipe {
    private final Kind kind;
    private PlacementInfo placementInfo;

    public ModuleTurnRecipe(CraftingBookCategory category, Kind kind) {
        super(category);
        this.kind = kind;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return switch (kind) {
            case PHASE_TURRET -> matchPhase(input);
            case ASSIST_TOGGLE -> matchAssist(input);
            case HEALTH_TURRET -> matchHealth(input);
            case HEALTH_VOID -> matchHealthVoid(input);
            case DASH -> matchDash(input);
            case BLINK -> matchBlink(input);
            case SAFE_BLINK -> matchSafeBlink(input);
            case TELEPORT -> matchTeleport(input);
            case WORLD -> matchWorld(input);
            case BLACK_HOLE -> matchBlackHole(input);
            case TEAR_BLACK_HOLE -> matchTearBlackHole(input);
            case ANNIHILATION_BLACK_HOLE -> matchAnnihilationBlackHole(input);
        };
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return switch (kind) {
            case PHASE_TURRET -> makeOutput(get(input, 1, 1), ModItem.PHASE_TURRET_MODULE.get());
            case ASSIST_TOGGLE -> makeAssistOutput(get(input, 1, 0));
            case HEALTH_TURRET -> makeHealthOutput(get(input, 1, 1));
            case HEALTH_VOID -> makeOutput(get(input, 1, 1), ModItem.HEALTH_VOID_MODULE.get());
            case DASH -> makeOutput(get(input, 1, 0), ModItem.DASH_VOID_MODULE.get());
            case BLINK -> makeOutput(get(input, 1, 1), ModItem.BLINK_VOID_MODULE.get());
            case SAFE_BLINK -> makeSafeBlinkOutput(get(input, 1, 0));
            case TELEPORT -> makeOutput(get(input, 1, 2), ModItem.TELEPORT_VOID_MODULE.get());
            case WORLD -> makeOutput(get(input, 1, 1), ModItem.WORLD_MODULE.get());
            case BLACK_HOLE -> makeOutput(get(input, 1, 1), ModItem.BLACK_HOLE_MODULE.get());
            case TEAR_BLACK_HOLE -> makeTearBlackHoleOutput(get(input, 1, 1));
            case ANNIHILATION_BLACK_HOLE -> makeOutput(get(input, 1, 1), ModItem.ANNIHILATION_BLACK_HOLE_MODULE.get());
        };
    }

    @Override
    public RecipeSerializer<ModuleTurnRecipe> getSerializer() {
        return switch (kind) {
            case PHASE_TURRET -> ModRecipeSerializers.PHASE_TURRET_MODULE.get();
            case ASSIST_TOGGLE -> ModRecipeSerializers.ASSIST_PHASE_TURRET_MODULE.get();
            case HEALTH_TURRET -> ModRecipeSerializers.HEALTH_PHASE_TURRET_MODULE.get();
            case HEALTH_VOID -> ModRecipeSerializers.HEALTH_VOID_MODULE.get();
            case DASH -> ModRecipeSerializers.DASH_VOID_MODULE.get();
            case BLINK -> ModRecipeSerializers.BLINK_VOID_MODULE.get();
            case SAFE_BLINK -> ModRecipeSerializers.SAFE_BLINK_VOID_MODULE.get();
            case TELEPORT -> ModRecipeSerializers.TELEPORT_VOID_MODULE.get();
            case WORLD -> ModRecipeSerializers.WORLD_MODULE.get();
            case BLACK_HOLE -> ModRecipeSerializers.BLACK_HOLE_MODULE.get();
            case TEAR_BLACK_HOLE -> ModRecipeSerializers.TEAR_BLACK_HOLE_MODULE.get();
            case ANNIHILATION_BLACK_HOLE -> ModRecipeSerializers.ANNIHILATION_BLACK_HOLE_MODULE.get();
        };
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public PlacementInfo placementInfo() {
        if (this.placementInfo == null) {
            this.placementInfo = PlacementInfo.createFromOptionals(getIngredients());
        }
        return this.placementInfo;
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of(new ShapedCraftingRecipeDisplay(
                getWidth(),
                getHeight(),
                getIngredients().stream().map(input -> input.map(Ingredient::display).orElse(SlotDisplay.Empty.INSTANCE)).toList(),
                new SlotDisplay.ItemStackSlotDisplay(new ItemStack(getResultItem())),
                new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
        ));
    }

    private boolean matchPhase(CraftingInput input) {
        return input.width() == 3
                && input.height() == 3
                && input.ingredientCount() == 9
                && is(get(input, 0, 0), Items.IRON_INGOT)
                && is(get(input, 1, 0), ModItem.CHAOS_ENERGY.get())
                && is(get(input, 2, 0), Items.IRON_INGOT)
                && is(get(input, 0, 1), Items.IRON_INGOT)
                && is(get(input, 1, 1), ModItem.MODULE_ITEM.get())
                && is(get(input, 2, 1), Items.IRON_INGOT)
                && is(get(input, 0, 2), Items.BLAZE_ROD)
                && is(get(input, 1, 2), ModItem.CHAOS_ENERGY.get())
                && is(get(input, 2, 2), Items.BLAZE_ROD);
    }

    private boolean matchAssist(CraftingInput input) {
        return input.width() == 3
                && input.height() == 1
                && input.ingredientCount() == 3
                && is(get(input, 0, 0), Items.DIAMOND)
                && isAssistBase(get(input, 1, 0))
                && is(get(input, 2, 0), Items.DIAMOND);
    }

    private boolean matchHealth(CraftingInput input) {
        return input.width() == 3
                && input.height() == 3
                && input.ingredientCount() == 5
                && empty(get(input, 0, 0))
                && is(get(input, 1, 0), ModItem.VOID_ENERGY.get())
                && empty(get(input, 2, 0))
                && empty(get(input, 0, 1))
                && isHealthBase(get(input, 1, 1))
                && empty(get(input, 2, 1))
                && is(get(input, 0, 2), ModItem.PURE_ENERGY.get())
                && is(get(input, 1, 2), ModItem.PURE_ENERGY.get())
                && is(get(input, 2, 2), ModItem.PURE_ENERGY.get());
    }

    private boolean matchHealthVoid(CraftingInput input) {
        return input.width() == 3
                && input.height() == 3
                && input.ingredientCount() == 5
                && empty(get(input, 0, 0))
                && is(get(input, 1, 0), ModItem.VOID_ENERGY.get())
                && empty(get(input, 2, 0))
                && empty(get(input, 0, 1))
                && is(get(input, 1, 1), ModItem.MODULE_ITEM.get())
                && empty(get(input, 2, 1))
                && is(get(input, 0, 2), ModItem.PURE_ENERGY.get())
                && is(get(input, 1, 2), ModItem.PURE_ENERGY.get())
                && is(get(input, 2, 2), ModItem.PURE_ENERGY.get());
    }

    private boolean matchBlink(CraftingInput input) {
        return input.width() == 3
                && input.height() == 3
                && input.ingredientCount() == 5
                && empty(get(input, 0, 0))
                && is(get(input, 1, 0), ModItem.BASIC_ENERGY_CORE.get())
                && empty(get(input, 2, 0))
                && is(get(input, 0, 1), ModItem.NEUTRAL_ENERGY.get())
                && is(get(input, 1, 1), ModItem.MODULE_ITEM.get())
                && is(get(input, 2, 1), ModItem.NEUTRAL_ENERGY.get())
                && empty(get(input, 0, 2))
                && is(get(input, 1, 2), ModItem.BASIC_ENERGY_CORE.get())
                && empty(get(input, 2, 2));
    }

    private boolean matchDash(CraftingInput input) {
        return input.width() == 3
                && input.height() == 3
                && input.ingredientCount() == 7
                && is(get(input, 0, 0), ModItem.CHAOS_ENERGY.get())
                && is(get(input, 1, 0), ModItem.MODULE_ITEM.get())
                && is(get(input, 2, 0), ModItem.CHAOS_ENERGY.get())
                && is(get(input, 0, 1), ModItem.CHAOS_ENERGY.get())
                && is(get(input, 1, 1), ModItem.NEUTRAL_ENERGY.get())
                && is(get(input, 2, 1), ModItem.CHAOS_ENERGY.get())
                && empty(get(input, 0, 2))
                && is(get(input, 1, 2), ModItem.NEUTRAL_ENERGY.get())
                && empty(get(input, 2, 2));
    }

    private boolean matchSafeBlink(CraftingInput input) {
        return input.width() == 3
                && input.height() == 1
                && input.ingredientCount() == 3
                && is(get(input, 0, 0), Items.DIAMOND)
                && isBlinkBase(get(input, 1, 0))
                && is(get(input, 2, 0), Items.DIAMOND);
    }

    private boolean matchTeleport(CraftingInput input) {
        return input.width() == 3
                && input.height() == 3
                && input.ingredientCount() == 9
                && is(get(input, 0, 0), ModItem.CHAOS_ENERGY.get())
                && is(get(input, 1, 0), ModItem.CHAOS_ENERGY.get())
                && is(get(input, 2, 0), ModItem.CHAOS_ENERGY.get())
                && is(get(input, 0, 1), ModItem.NEUTRAL_ENERGY.get())
                && is(get(input, 1, 1), ModItem.PURE_VOID_CRYSTAL.get())
                && is(get(input, 2, 1), ModItem.NEUTRAL_ENERGY.get())
                && is(get(input, 0, 2), ModItem.NEUTRAL_ENERGY.get())
                && is(get(input, 1, 2), ModItem.MODULE_ITEM.get())
                && is(get(input, 2, 2), ModItem.NEUTRAL_ENERGY.get());
    }

    private boolean matchWorld(CraftingInput input) {
        return input.width() == 3
                && input.height() == 3
                && input.ingredientCount() == 9
                && is(get(input, 0, 0), ModItem.VOID_ENERGY.get())
                && is(get(input, 1, 0), ModItem.VOID_ENERGY.get())
                && is(get(input, 2, 0), ModItem.VOID_ENERGY.get())
                && is(get(input, 0, 1), ModItem.PURE_VOID_CRYSTAL.get())
                && is(get(input, 1, 1), ModItem.MODULE_ITEM.get())
                && is(get(input, 2, 1), ModItem.PURE_VOID_CRYSTAL.get())
                && is(get(input, 0, 2), ModItem.VOID_ENERGY.get())
                && is(get(input, 1, 2), ModItem.VOID_ENERGY.get())
                && is(get(input, 2, 2), ModItem.VOID_ENERGY.get());
    }

    private boolean matchBlackHole(CraftingInput input) {
        return input.width() == 3
                && input.height() == 3
                && input.ingredientCount() == 9
                && is(get(input, 0, 0), ModItem.VOID_ENERGY.get())
                && is(get(input, 1, 0), ModItem.PURE_VOID_CRYSTAL.get())
                && is(get(input, 2, 0), ModItem.VOID_ENERGY.get())
                && is(get(input, 0, 1), ModItem.PURE_VOID_CRYSTAL.get())
                && is(get(input, 1, 1), ModItem.MODULE_ITEM.get())
                && is(get(input, 2, 1), ModItem.PURE_VOID_CRYSTAL.get())
                && is(get(input, 0, 2), ModItem.VOID_ENERGY.get())
                && is(get(input, 1, 2), ModItem.PURE_VOID_CRYSTAL.get())
                && is(get(input, 2, 2), ModItem.VOID_ENERGY.get());
    }

    private boolean matchTearBlackHole(CraftingInput input) {
        return input.width() == 3
                && input.height() == 3
                && input.ingredientCount() == 9
                && (matchTearBlackHole(input, ModItem.BLACK_HOLE_MODULE.get(), ModItem.CHAOS_ENERGY.get())
                || matchTearBlackHole(input, ModItem.TEAR_BLACK_HOLE_MODULE.get(), ModItem.PURE_ENERGY.get()));
    }

    private boolean matchTearBlackHole(CraftingInput input, Item center, Item side) {
        return is(get(input, 0, 0), side)
                && is(get(input, 1, 0), side)
                && is(get(input, 2, 0), side)
                && is(get(input, 0, 1), side)
                && is(get(input, 1, 1), center)
                && is(get(input, 2, 1), side)
                && is(get(input, 0, 2), side)
                && is(get(input, 1, 2), side)
                && is(get(input, 2, 2), side);
    }

    private boolean matchAnnihilationBlackHole(CraftingInput input) {
        return input.width() == 3
                && input.height() == 3
                && input.ingredientCount() == 9
                && is(get(input, 0, 0), ModItem.CHAOS_ENERGY.get())
                && is(get(input, 1, 0), ModItem.VOID_ENERGY.get())
                && is(get(input, 2, 0), ModItem.CHAOS_ENERGY.get())
                && is(get(input, 0, 1), ModItem.CHAOS_ENERGY.get())
                && is(get(input, 1, 1), ModItem.TEAR_BLACK_HOLE_MODULE.get())
                && is(get(input, 2, 1), ModItem.CHAOS_ENERGY.get())
                && is(get(input, 0, 2), ModItem.CHAOS_ENERGY.get())
                && is(get(input, 1, 2), ModItem.VOID_ENERGY.get())
                && is(get(input, 2, 2), ModItem.CHAOS_ENERGY.get());
    }

    private ItemStack makeAssistOutput(ItemStack stack) {
        if (is(stack, ModItem.PHASE_TURRET_MODULE.get())) {
            return makeOutput(stack, ModItem.ASSIST_PHASE_TURRET_MODULE.get());
        }
        if (is(stack, ModItem.ASSIST_PHASE_TURRET_MODULE.get())) {
            return makeOutput(stack, ModItem.PHASE_TURRET_MODULE.get());
        }
        return ItemStack.EMPTY;
    }

    private ItemStack makeHealthOutput(ItemStack stack) {
        if (is(stack, ModItem.PHASE_TURRET_MODULE.get())) {
            return makeOutput(stack, ModItem.HEALTH_PHASE_TURRET_MODULE.get());
        }
        if (is(stack, ModItem.ASSIST_PHASE_TURRET_MODULE.get())) {
            return makeOutput(stack, ModItem.HEALTH_ASSIST_PHASE_TURRET_MODULE.get());
        }
        if (is(stack, ModItem.HEALTH_PHASE_TURRET_MODULE.get())) {
            return makeOutput(stack, ModItem.PHASE_TURRET_MODULE.get());
        }
        if (is(stack, ModItem.HEALTH_ASSIST_PHASE_TURRET_MODULE.get())) {
            return makeOutput(stack, ModItem.ASSIST_PHASE_TURRET_MODULE.get());
        }
        return ItemStack.EMPTY;
    }

    private ItemStack makeTearBlackHoleOutput(ItemStack stack) {
        if (is(stack, ModItem.BLACK_HOLE_MODULE.get())) {
            return makeOutput(stack, ModItem.TEAR_BLACK_HOLE_MODULE.get());
        }
        if (is(stack, ModItem.TEAR_BLACK_HOLE_MODULE.get())) {
            return makeOutput(stack, ModItem.BLACK_HOLE_MODULE.get());
        }
        return ItemStack.EMPTY;
    }

    private ItemStack makeSafeBlinkOutput(ItemStack stack) {
        if (is(stack, ModItem.BLINK_VOID_MODULE.get())) {
            return makeOutput(stack, ModItem.SAFE_BLINK_VOID_MODULE.get());
        }
        if (is(stack, ModItem.SAFE_BLINK_VOID_MODULE.get())) {
            return makeOutput(stack, ModItem.BLINK_VOID_MODULE.get());
        }
        return ItemStack.EMPTY;
    }

    private ItemStack makeOutput(ItemStack stack, Item item) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        return stack.transmuteCopy(item, 1);
    }

    private List<Optional<Ingredient>> getIngredients() {
        return switch (kind) {
            case PHASE_TURRET -> List.of(
                    item(Items.IRON_INGOT), item(ModItem.CHAOS_ENERGY.get()), item(Items.IRON_INGOT),
                    item(Items.IRON_INGOT), item(ModItem.MODULE_ITEM.get()), item(Items.IRON_INGOT),
                    item(Items.BLAZE_ROD), item(ModItem.CHAOS_ENERGY.get()), item(Items.BLAZE_ROD)
            );
            case ASSIST_TOGGLE -> List.of(
                    item(Items.DIAMOND), item(ModItem.PHASE_TURRET_MODULE.get(), ModItem.ASSIST_PHASE_TURRET_MODULE.get()), item(Items.DIAMOND)
            );
            case HEALTH_TURRET -> List.of(
                    emptyItem(), item(ModItem.VOID_ENERGY.get()), emptyItem(),
                    emptyItem(), item(ModItem.PHASE_TURRET_MODULE.get(), ModItem.ASSIST_PHASE_TURRET_MODULE.get(), ModItem.HEALTH_PHASE_TURRET_MODULE.get(), ModItem.HEALTH_ASSIST_PHASE_TURRET_MODULE.get()), emptyItem(),
                    item(ModItem.PURE_ENERGY.get()), item(ModItem.PURE_ENERGY.get()), item(ModItem.PURE_ENERGY.get())
            );
            case HEALTH_VOID -> List.of(
                    emptyItem(), item(ModItem.VOID_ENERGY.get()), emptyItem(),
                    emptyItem(), item(ModItem.MODULE_ITEM.get()), emptyItem(),
                    item(ModItem.PURE_ENERGY.get()), item(ModItem.PURE_ENERGY.get()), item(ModItem.PURE_ENERGY.get())
            );
            case DASH -> List.of(
                    item(ModItem.CHAOS_ENERGY.get()), item(ModItem.MODULE_ITEM.get()), item(ModItem.CHAOS_ENERGY.get()),
                    item(ModItem.CHAOS_ENERGY.get()), item(ModItem.NEUTRAL_ENERGY.get()), item(ModItem.CHAOS_ENERGY.get()),
                    emptyItem(), item(ModItem.NEUTRAL_ENERGY.get()), emptyItem()
            );
            case BLINK -> List.of(
                    emptyItem(), item(ModItem.BASIC_ENERGY_CORE.get()), emptyItem(),
                    item(ModItem.NEUTRAL_ENERGY.get()), item(ModItem.MODULE_ITEM.get()), item(ModItem.NEUTRAL_ENERGY.get()),
                    emptyItem(), item(ModItem.BASIC_ENERGY_CORE.get()), emptyItem()
            );
            case SAFE_BLINK -> List.of(
                    item(Items.DIAMOND), item(ModItem.BLINK_VOID_MODULE.get(), ModItem.SAFE_BLINK_VOID_MODULE.get()), item(Items.DIAMOND)
            );
            case TELEPORT -> List.of(
                    item(ModItem.CHAOS_ENERGY.get()), item(ModItem.CHAOS_ENERGY.get()), item(ModItem.CHAOS_ENERGY.get()),
                    item(ModItem.NEUTRAL_ENERGY.get()), item(ModItem.PURE_VOID_CRYSTAL.get()), item(ModItem.NEUTRAL_ENERGY.get()),
                    item(ModItem.NEUTRAL_ENERGY.get()), item(ModItem.MODULE_ITEM.get()), item(ModItem.NEUTRAL_ENERGY.get())
            );
            case WORLD -> List.of(
                    item(ModItem.VOID_ENERGY.get()), item(ModItem.VOID_ENERGY.get()), item(ModItem.VOID_ENERGY.get()),
                    item(ModItem.PURE_VOID_CRYSTAL.get()), item(ModItem.MODULE_ITEM.get()), item(ModItem.PURE_VOID_CRYSTAL.get()),
                    item(ModItem.VOID_ENERGY.get()), item(ModItem.VOID_ENERGY.get()), item(ModItem.VOID_ENERGY.get())
            );
            case BLACK_HOLE -> List.of(
                    item(ModItem.VOID_ENERGY.get()), item(ModItem.PURE_VOID_CRYSTAL.get()), item(ModItem.VOID_ENERGY.get()),
                    item(ModItem.PURE_VOID_CRYSTAL.get()), item(ModItem.MODULE_ITEM.get()), item(ModItem.PURE_VOID_CRYSTAL.get()),
                    item(ModItem.VOID_ENERGY.get()), item(ModItem.PURE_VOID_CRYSTAL.get()), item(ModItem.VOID_ENERGY.get())
            );
            case TEAR_BLACK_HOLE -> List.of(
                    item(ModItem.CHAOS_ENERGY.get(), ModItem.PURE_ENERGY.get()), item(ModItem.CHAOS_ENERGY.get(), ModItem.PURE_ENERGY.get()), item(ModItem.CHAOS_ENERGY.get(), ModItem.PURE_ENERGY.get()),
                    item(ModItem.CHAOS_ENERGY.get(), ModItem.PURE_ENERGY.get()), item(ModItem.BLACK_HOLE_MODULE.get(), ModItem.TEAR_BLACK_HOLE_MODULE.get()), item(ModItem.CHAOS_ENERGY.get(), ModItem.PURE_ENERGY.get()),
                    item(ModItem.CHAOS_ENERGY.get(), ModItem.PURE_ENERGY.get()), item(ModItem.CHAOS_ENERGY.get(), ModItem.PURE_ENERGY.get()), item(ModItem.CHAOS_ENERGY.get(), ModItem.PURE_ENERGY.get())
            );
            case ANNIHILATION_BLACK_HOLE -> List.of(
                    item(ModItem.CHAOS_ENERGY.get()), item(ModItem.VOID_ENERGY.get()), item(ModItem.CHAOS_ENERGY.get()),
                    item(ModItem.CHAOS_ENERGY.get()), item(ModItem.TEAR_BLACK_HOLE_MODULE.get()), item(ModItem.CHAOS_ENERGY.get()),
                    item(ModItem.CHAOS_ENERGY.get()), item(ModItem.VOID_ENERGY.get()), item(ModItem.CHAOS_ENERGY.get())
            );
        };
    }

    private int getWidth() {
        return 3;
    }

    private int getHeight() {
        return switch (kind) {
            case ASSIST_TOGGLE, SAFE_BLINK -> 1;
            default -> 3;
        };
    }

    private Item getResultItem() {
        return switch (kind) {
            case PHASE_TURRET -> ModItem.PHASE_TURRET_MODULE.get();
            case ASSIST_TOGGLE -> ModItem.ASSIST_PHASE_TURRET_MODULE.get();
            case HEALTH_TURRET -> ModItem.HEALTH_PHASE_TURRET_MODULE.get();
            case HEALTH_VOID -> ModItem.HEALTH_VOID_MODULE.get();
            case DASH -> ModItem.DASH_VOID_MODULE.get();
            case BLINK -> ModItem.BLINK_VOID_MODULE.get();
            case SAFE_BLINK -> ModItem.SAFE_BLINK_VOID_MODULE.get();
            case TELEPORT -> ModItem.TELEPORT_VOID_MODULE.get();
            case WORLD -> ModItem.WORLD_MODULE.get();
            case BLACK_HOLE -> ModItem.BLACK_HOLE_MODULE.get();
            case TEAR_BLACK_HOLE -> ModItem.TEAR_BLACK_HOLE_MODULE.get();
            case ANNIHILATION_BLACK_HOLE -> ModItem.ANNIHILATION_BLACK_HOLE_MODULE.get();
        };
    }

    private Optional<Ingredient> item(Item... item) {
        return Optional.of(Ingredient.of(item));
    }

    private Optional<Ingredient> emptyItem() {
        return Optional.empty();
    }

    private boolean isAssistBase(ItemStack stack) {
        return is(stack, ModItem.PHASE_TURRET_MODULE.get())
                || is(stack, ModItem.ASSIST_PHASE_TURRET_MODULE.get());
    }

    private boolean isHealthBase(ItemStack stack) {
        return is(stack, ModItem.PHASE_TURRET_MODULE.get())
                || is(stack, ModItem.ASSIST_PHASE_TURRET_MODULE.get())
                || is(stack, ModItem.HEALTH_PHASE_TURRET_MODULE.get())
                || is(stack, ModItem.HEALTH_ASSIST_PHASE_TURRET_MODULE.get());
    }

    private boolean isBlinkBase(ItemStack stack) {
        return is(stack, ModItem.BLINK_VOID_MODULE.get())
                || is(stack, ModItem.SAFE_BLINK_VOID_MODULE.get());
    }

    private boolean is(ItemStack stack, Item item) {
        return !stack.isEmpty() && stack.getItem() == item;
    }

    private boolean empty(ItemStack stack) {
        return stack.isEmpty();
    }

    private ItemStack get(CraftingInput input, int x, int y) {
        return input.getItem(x + y * input.width());
    }

    public enum Kind {
        PHASE_TURRET,
        ASSIST_TOGGLE,
        HEALTH_TURRET,
        HEALTH_VOID,
        DASH,
        BLINK,
        SAFE_BLINK,
        TELEPORT,
        WORLD,
        BLACK_HOLE,
        TEAR_BLACK_HOLE,
        ANNIHILATION_BLACK_HOLE
    }
}
