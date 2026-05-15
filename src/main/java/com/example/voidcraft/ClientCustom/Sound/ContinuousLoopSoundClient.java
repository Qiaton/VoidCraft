package com.example.voidcraft.ClientCustom.Sound;

import com.example.voidcraft.Network.ContinuousLoopSoundPayload;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ContinuousLoopSoundClient {
    private static final Map<UUID, ContinuousLoopSoundInstance> SOUNDS = new HashMap<>();

    private ContinuousLoopSoundClient() {
    }

    public static void handle(ContinuousLoopSoundPayload payload) {
        clearStopped();
        if (!payload.active()) {
            stop(payload.id());
            return;
        }

        start(payload);
    }

    private static void start(ContinuousLoopSoundPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        stop(payload.id());
        ContinuousLoopSoundInstance sound = new ContinuousLoopSoundInstance(
                payload.sound(),
                payload.x(),
                payload.y(),
                payload.z(),
                payload.volume(),
                payload.pitch(),
                payload.durationTicks()
        );
        SOUNDS.put(payload.id(), sound);
        mc.getSoundManager().play(sound);
    }

    private static void stop(UUID id) {
        ContinuousLoopSoundInstance sound = SOUNDS.remove(id);
        if (sound != null) {
            sound.stopSound();
        }
    }

    private static void clearStopped() {
        SOUNDS.entrySet().removeIf(entry -> entry.getValue().isStopped());
    }
}
