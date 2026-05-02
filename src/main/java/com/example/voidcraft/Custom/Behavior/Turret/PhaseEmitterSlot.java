package com.example.voidcraft.Custom.Behavior.Turret;

public enum PhaseEmitterSlot {
    LEFT_TOP(-1, 1),
    RIGHT_BOTTOM(1, -1),
    LEFT_BOTTOM(-1, -1),
    RIGHT_TOP(1, 1);

    public final int x;
    public final int y;

    PhaseEmitterSlot(int x, int y) {
        this.x = x;
        this.y = y;
    }

    // 所有炮台模式共用同一套发射顺序，视觉和服务端伤害能保持一致。
    public static final PhaseEmitterSlot[] FIRE_ORDER = {
            LEFT_TOP,
            RIGHT_BOTTOM,
            LEFT_BOTTOM,
            RIGHT_TOP
    };

    public static boolean isValidFireIndex(int index) {
        return index >= 0 && index < FIRE_ORDER.length;
    }

    public static PhaseEmitterSlot byFireIndex(int index) {
        if (!isValidFireIndex(index)) {
            return LEFT_TOP;
        }

        return FIRE_ORDER[index];
    }
}
