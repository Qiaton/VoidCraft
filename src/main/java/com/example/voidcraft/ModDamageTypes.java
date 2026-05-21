package com.example.voidcraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

public class ModDamageTypes {
    public static final ResourceKey<DamageType> VOID_ARCHER_PHASE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(VoidCraft.MODID, "void_archer_phase")
    );

    public static final ResourceKey<DamageType> VOID_ARCHER_ENTER_VOID = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(VoidCraft.MODID, "void_archer_enter_void")
    );

    public static final ResourceKey<DamageType> PHASE_TURRET_SHRED = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(VoidCraft.MODID, "phase_turret_shred")
    );

    public static final ResourceKey<DamageType> PHASE_TURRET_DISPERSE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(VoidCraft.MODID, "phase_turret_disperse")
    );
}
