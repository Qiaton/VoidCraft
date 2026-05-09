package com.example.voidcraft.Custom.Behavior.Energy;

// 方块虚空能端点的统一配置：容量、输入速率、绑定上限和网络传输节奏都放这里。
public record VoidEnergyProfile(
        long capacity,
        long maxInputPerTransfer,
        int maxInputBindings,
        int maxOutputBindings,
        int transferIntervalTicks
) {
    public static final int DEFAULT_TRANSFER_INTERVAL_TICKS = 5;
    public static final long DEFAULT_MAX_INPUT_PER_TRANSFER = 6_000L;

    public VoidEnergyProfile {
        capacity = Math.max(0L, capacity);
        maxInputPerTransfer = Math.max(0L, maxInputPerTransfer);
        maxInputBindings = Math.max(0, maxInputBindings);
        maxOutputBindings = Math.max(0, maxOutputBindings);
        transferIntervalTicks = Math.max(1, transferIntervalTicks);
    }

    public static VoidEnergyProfile inputOnly(long capacity, long maxInputPerTransfer, int maxInputBindings) {
        return inputOnly(capacity, maxInputPerTransfer, maxInputBindings, DEFAULT_TRANSFER_INTERVAL_TICKS);
    }

    public static VoidEnergyProfile inputOnly(
            long capacity,
            long maxInputPerTransfer,
            int maxInputBindings,
            int transferIntervalTicks
    ) {
        return new VoidEnergyProfile(
                capacity,
                maxInputPerTransfer,
                maxInputBindings,
                0,
                transferIntervalTicks
        );
    }

    public static VoidEnergyProfile outputOnly(long capacity, int maxOutputBindings) {
        return outputOnly(capacity, maxOutputBindings, DEFAULT_TRANSFER_INTERVAL_TICKS);
    }

    public static VoidEnergyProfile outputOnly(
            long capacity,
            int maxOutputBindings,
            int transferIntervalTicks
    ) {
        return new VoidEnergyProfile(
                capacity,
                0L,
                0,
                maxOutputBindings,
                transferIntervalTicks
        );
    }

    public static VoidEnergyProfile bidirectional(
            long capacity,
            long maxInputPerTransfer,
            int maxInputBindings,
            int maxOutputBindings
    ) {
        return bidirectional(
                capacity,
                maxInputPerTransfer,
                maxInputBindings,
                maxOutputBindings,
                DEFAULT_TRANSFER_INTERVAL_TICKS
        );
    }

    public static VoidEnergyProfile bidirectional(
            long capacity,
            long maxInputPerTransfer,
            int maxInputBindings,
            int maxOutputBindings,
            int transferIntervalTicks
    ) {
        return new VoidEnergyProfile(
                capacity,
                maxInputPerTransfer,
                maxInputBindings,
                maxOutputBindings,
                transferIntervalTicks
        );
    }

    public boolean canReceive() {
        return this.maxInputPerTransfer > 0L && this.maxInputBindings > 0;
    }

    public boolean canExtract() {
        return this.maxOutputBindings > 0;
    }

    public long clampEnergy(long energy) {
        return Math.max(0L, Math.min(this.capacity, energy));
    }
}
