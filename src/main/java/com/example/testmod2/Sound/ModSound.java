package com.example.testmod2.Sound;

import com.example.testmod2.TestMod2;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSound {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, TestMod2.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> ENTER_VOID = SOUND_EVENTS.register(
            "enter_void",
            SoundEvent::createVariableRangeEvent
    );
    public static final DeferredHolder<SoundEvent, SoundEvent> OUT_VOID = SOUND_EVENTS.register(
            "out_void",
            SoundEvent::createVariableRangeEvent
    );
    public static final DeferredHolder<SoundEvent, SoundEvent> IN_VOID_LONG = SOUND_EVENTS.register(
            "in_void_long",
            SoundEvent::createVariableRangeEvent
    );

    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }
}
