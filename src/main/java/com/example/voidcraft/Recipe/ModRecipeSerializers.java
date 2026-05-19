package com.example.voidcraft.Recipe;

import com.example.voidcraft.VoidCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(
            Registries.RECIPE_SERIALIZER,
            VoidCraft.MODID
    );

    public static final Supplier<RecipeSerializer<ModuleTurnRecipe>> PHASE_TURRET_MODULE = RECIPE_SERIALIZERS.register(
            "phase_turret_module",
            () -> new CustomRecipe.Serializer<>(category -> new ModuleTurnRecipe(category, ModuleTurnRecipe.Kind.PHASE_TURRET))
    );
    public static final Supplier<RecipeSerializer<ModuleTurnRecipe>> ASSIST_PHASE_TURRET_MODULE = RECIPE_SERIALIZERS.register(
            "assist_phase_turret_module",
            () -> new CustomRecipe.Serializer<>(category -> new ModuleTurnRecipe(category, ModuleTurnRecipe.Kind.ASSIST_TOGGLE))
    );
    public static final Supplier<RecipeSerializer<ModuleTurnRecipe>> HEALTH_PHASE_TURRET_MODULE = RECIPE_SERIALIZERS.register(
            "health_phase_turret_module",
            () -> new CustomRecipe.Serializer<>(category -> new ModuleTurnRecipe(category, ModuleTurnRecipe.Kind.HEALTH_TURRET))
    );
    public static final Supplier<RecipeSerializer<ModuleTurnRecipe>> HEALTH_VOID_MODULE = RECIPE_SERIALIZERS.register(
            "health_void_module",
            () -> new CustomRecipe.Serializer<>(category -> new ModuleTurnRecipe(category, ModuleTurnRecipe.Kind.HEALTH_VOID))
    );
    public static final Supplier<RecipeSerializer<ModuleTurnRecipe>> DASH_VOID_MODULE = RECIPE_SERIALIZERS.register(
            "dash_void_module",
            () -> new CustomRecipe.Serializer<>(category -> new ModuleTurnRecipe(category, ModuleTurnRecipe.Kind.DASH))
    );
    public static final Supplier<RecipeSerializer<ModuleTurnRecipe>> BLINK_VOID_MODULE = RECIPE_SERIALIZERS.register(
            "blink_void_module",
            () -> new CustomRecipe.Serializer<>(category -> new ModuleTurnRecipe(category, ModuleTurnRecipe.Kind.BLINK))
    );
    public static final Supplier<RecipeSerializer<ModuleTurnRecipe>> SAFE_BLINK_VOID_MODULE = RECIPE_SERIALIZERS.register(
            "safe_blink_void_module",
            () -> new CustomRecipe.Serializer<>(category -> new ModuleTurnRecipe(category, ModuleTurnRecipe.Kind.SAFE_BLINK))
    );
    public static final Supplier<RecipeSerializer<ModuleTurnRecipe>> TELEPORT_VOID_MODULE = RECIPE_SERIALIZERS.register(
            "teleport_void_module",
            () -> new CustomRecipe.Serializer<>(category -> new ModuleTurnRecipe(category, ModuleTurnRecipe.Kind.TELEPORT))
    );
    public static final Supplier<RecipeSerializer<ModuleTurnRecipe>> WORLD_MODULE = RECIPE_SERIALIZERS.register(
            "world_module",
            () -> new CustomRecipe.Serializer<>(category -> new ModuleTurnRecipe(category, ModuleTurnRecipe.Kind.WORLD))
    );
    public static final Supplier<RecipeSerializer<ModuleTurnRecipe>> BLACK_HOLE_MODULE = RECIPE_SERIALIZERS.register(
            "black_hole_module",
            () -> new CustomRecipe.Serializer<>(category -> new ModuleTurnRecipe(category, ModuleTurnRecipe.Kind.BLACK_HOLE))
    );
    public static final Supplier<RecipeSerializer<ModuleTurnRecipe>> TEAR_BLACK_HOLE_MODULE = RECIPE_SERIALIZERS.register(
            "tear_black_hole_module",
            () -> new CustomRecipe.Serializer<>(category -> new ModuleTurnRecipe(category, ModuleTurnRecipe.Kind.TEAR_BLACK_HOLE))
    );
    public static final Supplier<RecipeSerializer<ModuleTurnRecipe>> ANNIHILATION_BLACK_HOLE_MODULE = RECIPE_SERIALIZERS.register(
            "annihilation_black_hole_module",
            () -> new CustomRecipe.Serializer<>(category -> new ModuleTurnRecipe(category, ModuleTurnRecipe.Kind.ANNIHILATION_BLACK_HOLE))
    );

    public static void register(IEventBus bus) {
        RECIPE_SERIALIZERS.register(bus);
    }
}
