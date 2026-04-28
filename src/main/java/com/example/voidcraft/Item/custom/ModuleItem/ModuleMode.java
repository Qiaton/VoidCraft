package com.example.voidcraft.Item.custom.ModuleItem;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;

public enum  ModuleMode{
    CHANNEL("channel"),//持续
    BURST("burst");//单次
    private final String id;
    ModuleMode(String id) {
        this.id = id;
    }
    public String getId() {
        return id;
    }
    public String getTranslationKey() {
        return "module_mode.void_craft." + id;
    }
    public Component getDisplayName() {
        return Component.translatable(this.getTranslationKey());
    }
    public static ModuleMode byId(String id) {
        for (ModuleMode mode : ModuleMode.values()) {
            if (mode.id.equals(id)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown module mode id: " + id);
    }
    public static final Codec<ModuleMode> MODE_CODEC = Codec.STRING.xmap(ModuleMode::byId, ModuleMode::getId);

}
