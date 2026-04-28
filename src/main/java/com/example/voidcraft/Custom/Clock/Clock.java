package com.example.voidcraft.Custom.Clock;

import com.example.voidcraft.VoidCraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = VoidCraft.MODID)
public class Clock {
    private static final List<Clock> CLOCKS = new ArrayList<>();

    private int tickLeft;
    private final Runnable event;

    private Clock(int tick, Runnable event) {
        this.tickLeft = tick;
        this.event = event;
    }

    public static void addClock(int tick, Runnable event) {
        CLOCKS.add(new Clock(tick, event));
    }

    private boolean tick() {
        tickLeft--;

        if (tickLeft <= 0) {
            event.run();
            return true;
        }

        return false;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {

        CLOCKS.removeIf(Clock::tick);
    }
}