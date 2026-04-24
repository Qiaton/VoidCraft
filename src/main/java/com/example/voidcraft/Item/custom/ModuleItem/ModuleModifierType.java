package com.example.voidcraft.Item.custom.ModuleItem;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;

public enum ModuleModifierType {
    COOLDOWN_REDUCTION("cooldown_reduction"),
    SPEED_BOOST("speed_boost"),
    ACTIVE_DURATION("active_duration");
    private final String id;
    ModuleModifierType(String id) {
        this.id = id;
    }
    public String getId() {
        return id;
    }
    public String getTranslationKey() {
        return "module_modifier_type.void_craft." + id;
    }
    public Component getDisplayName() {
        return Component.translatable(getTranslationKey());
    }
    public static ModuleModifierType byId(String id) {
        for (ModuleModifierType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        throw new IllegalArgumentException(id + " is not a valid module modifier type");
    }
    public final static Codec<ModuleModifierType> MODIFIER_TYPE_CODEC = Codec.STRING.xmap(ModuleModifierType::byId, ModuleModifierType::getId);
}
