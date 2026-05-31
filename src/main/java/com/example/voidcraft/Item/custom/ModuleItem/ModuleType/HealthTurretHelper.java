package com.example.voidcraft.Item.custom.ModuleItem.ModuleType;

import com.example.voidcraft.Effect.VoidBeamInstance;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleData;
import com.example.voidcraft.ModDataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

final class HealthTurretHelper {
    private static final int BASE_MODULE_LEVEL = 1;

    private static final int LEFT_BEAM_CORE = 0xF2A4C9;
    private static final int LEFT_BEAM_GLOW = 0xD9327C;
    private static final int LEFT_BEAM_LEVEL_2_CORE = 0xF5AED4;
    private static final int LEFT_BEAM_LEVEL_2_GLOW = 0xE0448E;
    private static final int LEFT_BEAM_LEVEL_3_CORE = 0xFABAE0;
    private static final int LEFT_BEAM_LEVEL_3_GLOW = 0xEA55A0;
    private static final int LEFT_BEAM_LEVEL_4_CORE = 0xFFC6EC;
    private static final int LEFT_BEAM_LEVEL_4_GLOW = 0xF164B1;
    private static final int LEFT_BEAM_LEVEL_5_CORE = 0xFFD2F4;
    private static final int LEFT_BEAM_LEVEL_5_GLOW = 0xFA72C2;
    private static final int LEFT_BEAM_VOID_CORE = 0xF5C4FF;
    private static final int LEFT_BEAM_VOID_GLOW = 0xCB68E8;

    private static final int RIGHT_BEAM_CORE = 0xFFF7FC;
    private static final int RIGHT_BEAM_GLOW = 0xFFB8DC;
    private static final int RIGHT_BEAM_LEVEL_2_CORE = 0xFFF9FD;
    private static final int RIGHT_BEAM_LEVEL_2_GLOW = 0xFFC0E5;
    private static final int RIGHT_BEAM_LEVEL_3_CORE = 0xFFFBFE;
    private static final int RIGHT_BEAM_LEVEL_3_GLOW = 0xFFC8ED;
    private static final int RIGHT_BEAM_LEVEL_4_CORE = 0xFFFFFF;
    private static final int RIGHT_BEAM_LEVEL_4_GLOW = 0xFFD0F4;
    private static final int RIGHT_BEAM_LEVEL_5_CORE = 0xFFFFFF;
    private static final int RIGHT_BEAM_LEVEL_5_GLOW = 0xFFD8FA;
    private static final int RIGHT_BEAM_VOID_CORE = 0xFFFFFF;
    private static final int RIGHT_BEAM_VOID_GLOW = 0xE8C4FF;

    private HealthTurretHelper() {
    }

    static VoidBeamInstance.Config getBeam(ItemStack moduleStack, boolean heal) {
        int level = getLevel(moduleStack);
        return PhaseTurretModule.makeShotBeam(
                getBeamCore(level, heal),
                getBeamGlow(level, heal)
        );
    }

    static float getSelfHeal(ItemStack moduleStack, float healPerLevel) {
        return getLevel(moduleStack) * healPerLevel;
    }

    static float getFriendHeal(ItemStack moduleStack, float baseHeal, float healPerLevel) {
        return baseHeal + getLevel(moduleStack) * healPerLevel;
    }

    static boolean heal(LivingEntity target, float amount) {
        if (target == null || amount <= 0.0F || !needsHeal(target)) {
            return false;
        }

        float oldHealth = target.getHealth();
        target.heal(amount);
        return target.getHealth() > oldHealth;
    }

    static boolean needsHeal(LivingEntity target) {
        return target.getHealth() < target.getMaxHealth();
    }

    private static int getLevel(ItemStack moduleStack) {
        ModuleData data = moduleStack.get(ModDataComponents.MODULE_DATA.get());
        if (data == null) {
            return BASE_MODULE_LEVEL;
        }

        return Math.max(BASE_MODULE_LEVEL, data.level());
    }

    private static int getBeamCore(int level, boolean heal) {
        if (heal) {
            return switch (level) {
                case 2 -> RIGHT_BEAM_LEVEL_2_CORE;
                case 3 -> RIGHT_BEAM_LEVEL_3_CORE;
                case 4 -> RIGHT_BEAM_LEVEL_4_CORE;
                case 5 -> RIGHT_BEAM_LEVEL_5_CORE;
                default -> level >= 6 ? RIGHT_BEAM_VOID_CORE : RIGHT_BEAM_CORE;
            };
        }

        return switch (level) {
            case 2 -> LEFT_BEAM_LEVEL_2_CORE;
            case 3 -> LEFT_BEAM_LEVEL_3_CORE;
            case 4 -> LEFT_BEAM_LEVEL_4_CORE;
            case 5 -> LEFT_BEAM_LEVEL_5_CORE;
            default -> level >= 6 ? LEFT_BEAM_VOID_CORE : LEFT_BEAM_CORE;
        };
    }

    private static int getBeamGlow(int level, boolean heal) {
        if (heal) {
            return switch (level) {
                case 2 -> RIGHT_BEAM_LEVEL_2_GLOW;
                case 3 -> RIGHT_BEAM_LEVEL_3_GLOW;
                case 4 -> RIGHT_BEAM_LEVEL_4_GLOW;
                case 5 -> RIGHT_BEAM_LEVEL_5_GLOW;
                default -> level >= 6 ? RIGHT_BEAM_VOID_GLOW : RIGHT_BEAM_GLOW;
            };
        }

        return switch (level) {
            case 2 -> LEFT_BEAM_LEVEL_2_GLOW;
            case 3 -> LEFT_BEAM_LEVEL_3_GLOW;
            case 4 -> LEFT_BEAM_LEVEL_4_GLOW;
            case 5 -> LEFT_BEAM_LEVEL_5_GLOW;
            default -> level >= 6 ? LEFT_BEAM_VOID_GLOW : LEFT_BEAM_GLOW;
        };
    }
}
