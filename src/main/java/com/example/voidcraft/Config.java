package com.example.voidcraft;


import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<EnergyHudPosition> ENERGY_HUD_POSITION;
    public static final ModConfigSpec.IntValue ENERGY_HUD_OFFSET_X;
    public static final ModConfigSpec.IntValue ENERGY_HUD_OFFSET_Y;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("energyHud");
        ENERGY_HUD_POSITION = builder
                .comment("Energy HUD anchor position.")
                .defineEnum("position", EnergyHudPosition.BOTTOM_RIGHT);
        ENERGY_HUD_OFFSET_X = builder
                .comment("Horizontal offset from the selected anchor.")
                .defineInRange("offsetX", 12, 0, 512);
        ENERGY_HUD_OFFSET_Y = builder
                .comment("Vertical offset from the selected anchor.")
                .defineInRange("offsetY", 12, 0, 512);
        builder.pop();

        SPEC = builder.build();
    }

    public enum EnergyHudPosition {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }
}
