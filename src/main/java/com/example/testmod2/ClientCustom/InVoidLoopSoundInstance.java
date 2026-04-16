package com.example.testmod2.ClientCustom;

import com.example.testmod2.Sound.ModSound;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

public class InVoidLoopSoundInstance extends AbstractTickableSoundInstance {
    private final LocalPlayer player;

    public InVoidLoopSoundInstance(LocalPlayer player) {
        super(ModSound.IN_VOID_LONG.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.player = player;
        this.looping = true;
        this.delay = 0;
        this.volume = 6.0F;
        this.pitch = 1.0F;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
        if (this.player.isRemoved()) {
            this.stop();
            return;
        }

        this.x = this.player.getX();
        this.y = this.player.getY();
        this.z = this.player.getZ();
    }
}
