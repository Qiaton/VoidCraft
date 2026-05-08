package com.example.voidcraft.Block.entity;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public enum VoidEnergyBindingType {
    // OUTPUT 表示“我把能量送到对方”，INPUT 表示“我从对方收能量”。
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

    // 读到未知 id 时回到 OUTPUT，保证旧数据或手改数据不会直接炸。
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
