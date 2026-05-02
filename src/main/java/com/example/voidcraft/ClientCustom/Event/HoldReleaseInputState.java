package com.example.voidcraft.ClientCustom.Event;

public final class HoldReleaseInputState {
    private HoldReleaseInputState() {}

    private static final int SLOT_COUNT = 2;

    private static final boolean[] wasDown = new boolean[SLOT_COUNT];
    private static final boolean[] charging = new boolean[SLOT_COUNT];
    // Q 取消后进入等待松开状态，防止技能键还按着时下一 tick 又重新开始蓄力。
    private static final boolean[] waitingForRelease = new boolean[SLOT_COUNT];
    private static final int[] chargeTicks = new int[SLOT_COUNT];

    // 松手那一刻保存最终蓄力 tick
    private static final int[] lastReleasedTicks = new int[SLOT_COUNT];

    public enum Phase {
        NONE,
        PRESS,
        HOLD,
        RELEASE
    }

    public static Phase update(int slot, boolean isDown) {
        if (!isValidSlot(slot)) {
            return Phase.NONE;
        }

        if (waitingForRelease[slot]) {                                    // 已取消但技能键未松开时，不产生 PRESS/HOLD/RELEASE 阶段
            if (!isDown) {
                waitingForRelease[slot] = false;
                wasDown[slot] = false;
            }
            return Phase.NONE;
        }

        // 刚按下
        if (isDown && !wasDown[slot]) {
            charging[slot] = true;
            chargeTicks[slot] = 0;
            wasDown[slot] = true;
            return Phase.PRESS;
        }

        // 按住中
        if (isDown && charging[slot]) {
            chargeTicks[slot]++;
            wasDown[slot] = true;
            return Phase.HOLD;
        }

        // 刚松手
        if (!isDown && wasDown[slot] && charging[slot]) {
            lastReleasedTicks[slot] = chargeTicks[slot];

            charging[slot] = false;
            chargeTicks[slot] = 0;
            wasDown[slot] = false;

            return Phase.RELEASE;
        }

        wasDown[slot] = isDown;
        return Phase.NONE;
    }

    public static int getChargeTicks(int slot) {
        if (!isValidSlot(slot)) {
            return 0;
        }

        return chargeTicks[slot];
    }

    public static int getLastReleasedTicks(int slot) {
        if (!isValidSlot(slot)) {
            return 0;
        }

        return lastReleasedTicks[slot];
    }

    public static boolean isCharging(int slot) {
        if (!isValidSlot(slot)) {
            return false;
        }

        return charging[slot];
    }

    public static void cancel(int slot) {
        if (!isValidSlot(slot)) {
            return;
        }

        wasDown[slot] = false;
        charging[slot] = false;
        waitingForRelease[slot] = false;
        chargeTicks[slot] = 0;
        lastReleasedTicks[slot] = 0;
    }

    public static void cancelUntilReleased(int slot) {
        if (!isValidSlot(slot)) {
            return;
        }

        // 取消当前蓄力，并要求玩家先松开技能键，避免取消后立刻重新进入蓄力。
        cancel(slot);
        waitingForRelease[slot] = true;
    }

    public static void cancelAll() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            cancel(i);
        }
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }
}
