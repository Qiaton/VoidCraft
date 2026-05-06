package com.example.voidcraft.Block.entity;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public enum VoidEnergyBindingType {
    OUTPUT("output", "tooltip.void_craft.coordinate_designator.binding_type.output"),
    INPUT("input", "tooltip.void_craft.coordinate_designator.binding_type.input");

    public static final Codec<VoidEnergyBindingType> CODEC = Codec.STRING.xmap(
            VoidEnergyBindingType::byId,
            VoidEnergyBindingType::getId
    );

    private final String id;
    private final String translationKey;

    VoidEnergyBindingType(String id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public String getId() {
        return id;
    }

    public Component getDisplayName() {
        return Component.translatable(this.translationKey);
    }

    public static VoidEnergyBindingType byId(String id) {
        if (id == null) {
            return OUTPUT;
        }

        String normalized = id.toLowerCase(Locale.ROOT);
        for (VoidEnergyBindingType type : values()) {
            if (type.id.equals(normalized)) {
                return type;
            }
        }
        return OUTPUT;
    }
}
