package com.example.voidcraft.Sound;

import com.example.voidcraft.VoidCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSound {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, VoidCraft.MODID);

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

    // 虚空音效统一配置入口
    public static final SoundSource VOID_SOUND_SOURCE = SoundSource.PLAYERS;
    public static final float ENTER_VOID_VOLUME = 6.0F;
    public static final float ENTER_VOID_PITCH = 1.0F;
    public static final float OUT_VOID_VOLUME = 6.0F;
    public static final float OUT_VOID_PITCH = 1.0F;
    public static final float LOOP_VOID_VOLUME = 6.0F;
    public static final float LOOP_VOID_PITCH = 1.0F;
    public static final int LOOP_VOID_START_DELAY_TICKS = 4;
    public static final int LOOP_VOID_FADE_IN_TICKS = 8;
    public static final int LOOP_VOID_FADE_OUT_TICKS = -3;

    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }

    public static void playEnterVoid(Level level, Player player) {
        playAtPlayer(level, player, ENTER_VOID.get(), ENTER_VOID_VOLUME, ENTER_VOID_PITCH);
    }

    public static void playOutVoid(Level level, Player player) {
        playAtPlayer(level, player, OUT_VOID.get(), OUT_VOID_VOLUME, OUT_VOID_PITCH);
    }

    private static void playAtPlayer(Level level, Player player, SoundEvent event, float volume, float pitch) {
        if (level.isClientSide()) {
            return;
        }

        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                event,
                VOID_SOUND_SOURCE,
                volume,
                pitch
        );
    }
}
