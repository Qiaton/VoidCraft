package com.example.voidcraft.Custom.Behavior.Turret;

import com.example.voidcraft.Config;

public final class PhaseEmitterSlot {
    private static final int MIN_EMITTER_COUNT = 1;
    private static final int MAX_EMITTER_COUNT = 20;

    private final int fireIndex;
    private final int positionIndex;
    private final int emitterCount;

    private PhaseEmitterSlot(int fireIndex, int positionIndex, int emitterCount) {
        this.fireIndex = fireIndex;
        this.positionIndex = positionIndex;
        this.emitterCount = emitterCount;
    }

    public int fireIndex() {
        return this.fireIndex;
    }

    public double orbitPhase() {
        return (Math.PI * 2.0D * this.positionIndex) / this.emitterCount;
    }

    public static int configuredCount() {
        return normalizeCount(Config.getPhaseTurretEmitterCount());
    }

    public static int normalizeCount(int emitterCount) {
        return Math.max(MIN_EMITTER_COUNT, Math.min(MAX_EMITTER_COUNT, emitterCount));
    }

    public static boolean isValidFireIndex(int index) {
        return isValidFireIndex(index, configuredCount());
    }

    public static boolean isValidFireIndex(int index, int emitterCount) {
        return index >= 0 && index < normalizeCount(emitterCount);
    }

    public static PhaseEmitterSlot byFireIndex(int index) {
        return byFireIndex(index, configuredCount());
    }

    public static PhaseEmitterSlot byFireIndex(int index, int emitterCount) {
        int actualCount = normalizeCount(emitterCount);
        int actualFireIndex = Math.floorMod(index, actualCount);
        return new PhaseEmitterSlot(actualFireIndex, toPositionIndex(actualFireIndex, actualCount), actualCount);
    }

    public static PhaseEmitterSlot[] fireOrder() {
        return fireOrder(configuredCount());
    }

    public static PhaseEmitterSlot[] fireOrder(int emitterCount) {
        int actualCount = normalizeCount(emitterCount);
        PhaseEmitterSlot[] slots = new PhaseEmitterSlot[actualCount];
        for (int index = 0; index < actualCount; index++) {
            slots[index] = byFireIndex(index, actualCount);
        }
        return slots;
    }

    private static int toPositionIndex(int fireIndex, int emitterCount) {
        if (emitterCount == 4) {
            return switch (fireIndex) {
                case 1 -> 2;
                case 2 -> 3;
                case 3 -> 1;
                default -> 0;
            };
        }

        return fireIndex;
    }
}
