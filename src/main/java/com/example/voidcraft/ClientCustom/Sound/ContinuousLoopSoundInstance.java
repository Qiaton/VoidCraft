package com.example.voidcraft.ClientCustom.Sound;

import com.example.voidcraft.Sound.ModSound;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class ContinuousLoopSoundInstance extends AbstractTickableSoundInstance {
    private final int durationTicks;
    private int ticks;

    public ContinuousLoopSoundInstance(Identifier sound, double x, double y, double z, float volume, float pitch, int durationTicks) {
        super(SoundEvent.createVariableRangeEvent(sound), ModSound.VOID_SOUND_SOURCE, SoundInstance.createUnseededRandom());
        this.x = x;
        this.y = y;
        this.z = z;
        this.volume = volume;
        this.pitch = pitch;
        this.durationTicks = Math.max(1, durationTicks);
        this.looping = true;
        this.delay = 0;
        this.relative = false;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
    }

    @Override
    public void tick() {
        this.ticks++;
        if (this.ticks > this.durationTicks + 20) {
            this.stop();
        }
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    public void stopSound() {
        this.stop();
    }
}
