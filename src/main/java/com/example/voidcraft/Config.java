package com.example.voidcraft;


import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec COMMON_SPEC;
    public static final ModConfigSpec.EnumValue<EnergyHudPosition> ENERGY_HUD_POSITION;
    public static final ModConfigSpec.IntValue ENERGY_HUD_OFFSET_X;
    public static final ModConfigSpec.IntValue ENERGY_HUD_OFFSET_Y;
    public static final ModConfigSpec.IntValue PHASE_TURRET_EMITTER_COUNT;

    private static final int DEFAULT_PHASE_TURRET_EMITTER_COUNT = 4;

    static {
        ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();

        clientBuilder.push("energyHud");
        ENERGY_HUD_POSITION = clientBuilder
                .comment("Energy HUD anchor position.")
                .defineEnum("position", EnergyHudPosition.BOTTOM_RIGHT);
        ENERGY_HUD_OFFSET_X = clientBuilder
                .comment("Horizontal offset from the selected anchor.")
                .defineInRange("offsetX", 12, 0, 512);
        ENERGY_HUD_OFFSET_Y = clientBuilder
                .comment("Vertical offset from the selected anchor.")
                .defineInRange("offsetY", 12, 0, 512);
        clientBuilder.pop();

        CLIENT_SPEC = clientBuilder.build();
        SPEC = CLIENT_SPEC;

        ModConfigSpec.Builder commonBuilder = new ModConfigSpec.Builder();

        commonBuilder.push("phaseTurret");
        PHASE_TURRET_EMITTER_COUNT = commonBuilder
                .comment("How many phase turret emitter orbs should be created around the player.")
                .defineInRange("emitterCount", DEFAULT_PHASE_TURRET_EMITTER_COUNT, 1, 20);
        commonBuilder.pop();

        COMMON_SPEC = commonBuilder.build();
    }

    public static int getPhaseTurretEmitterCount() {
        try {
            return PHASE_TURRET_EMITTER_COUNT.get();
        } catch (IllegalStateException ignored) {
            return DEFAULT_PHASE_TURRET_EMITTER_COUNT;
        }
    }

    public enum EnergyHudPosition {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }
}
