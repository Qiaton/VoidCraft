package com.example.voidcraft.Item.custom.ModuleItem;

public final class ModuleStatHelper {
    private ModuleStatHelper() {
    }

    public static float addLess(float value, int level, float rate) {
        return value + getLessBonus(level, rate);
    }

    public static float shrink(float value, int level, float rate) {
        for (int i = 0; i < level; i++) {
            value *= 1.0F - rate;
        }
        return value;
    }

    private static float getLessBonus(int level, float rate) {
        float bonus = 0.0F;
        float actualRate = Math.max(0.0F, Math.min(1.0F, rate));
        for (int i = 0; i < level; i++) {
            bonus += (1.0F - bonus) * actualRate;
        }
        return bonus;
    }
}
