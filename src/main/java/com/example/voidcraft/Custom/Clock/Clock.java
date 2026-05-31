package com.example.voidcraft.Custom.Clock;

import com.example.voidcraft.VoidCraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = VoidCraft.MODID)
public class Clock {
    private static final List<Clock> CLOCKS = new ArrayList<>();
    private final UUID uuid = UUID.randomUUID();
    private int tickLeft;
    private final Runnable event;

    private Clock(int tick, Runnable event) {
        this.tickLeft = tick;
        this.event = event;
    }
    public static UUID addClock(int tick, Runnable event) {
       Clock clock = new Clock(tick,event);
       CLOCKS.add(clock);
       return clock.uuid;
    }
    public static void  removeClock(UUID uuid){
        if(uuid == null){
            return;
        }
        CLOCKS.removeIf(clock -> clock.uuid.equals(uuid));
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
