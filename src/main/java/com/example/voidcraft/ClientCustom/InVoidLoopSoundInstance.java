package com.example.voidcraft.ClientCustom;

import com.example.voidcraft.Sound.ModSound;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.util.Mth;

public class InVoidLoopSoundInstance extends AbstractTickableSoundInstance {
    private final LocalPlayer player;
    private int activeTicks = 0;
    private boolean fadingOut = false;
    private int fadeOutTicks = 0;
    private float fadeOutStartVolume = 0.0F;

    public InVoidLoopSoundInstance(LocalPlayer player) {
        super(ModSound.IN_VOID_LONG.get(), ModSound.VOID_SOUND_SOURCE, SoundInstance.createUnseededRandom());
        this.player = player;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.pitch = ModSound.LOOP_VOID_PITCH;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.x = 0.0;
        this.y = 0.0;
        this.z = 0.0;
    }

    @Override
    public void tick() {
        if (this.player.isRemoved()) {
            this.stop();
            return;
        }

        if (this.fadingOut) {
            tickFadeOut();
            return;
        }

        this.activeTicks++;
        this.volume = computeFadeInVolume();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    public void beginFadeIn() {
        if (!this.fadingOut) {
            return;
        }

        this.fadingOut = false;
        this.fadeOutTicks = 0;

        float targetVolume = ModSound.LOOP_VOID_VOLUME;
        if (targetVolume <= 0.0F) {
            this.activeTicks = 0;
            this.volume = 0.0F;
            return;
        }

        int fadeInTicks = Math.max(1, ModSound.LOOP_VOID_FADE_IN_TICKS);
        int delayTicks = Math.max(0, ModSound.LOOP_VOID_START_DELAY_TICKS);
        float progress = Mth.clamp(this.volume / targetVolume, 0.0F, 1.0F);
        this.activeTicks = delayTicks + Math.round(progress * fadeInTicks);
    }

    public void beginFadeOut() {
        if (this.fadingOut || this.isStopped()) {
            return;
        }

        int configuredFadeOutTicks = Math.max(0, ModSound.LOOP_VOID_FADE_OUT_TICKS);
        if (configuredFadeOutTicks == 0 || this.volume <= 0.0F) {
            this.volume = 0.0F;
            this.stop();
            return;
        }

        this.fadingOut = true;
        this.fadeOutTicks = 0;
        this.fadeOutStartVolume = this.volume;
    }

    private float computeFadeInVolume() {
        float targetVolume = ModSound.LOOP_VOID_VOLUME;
        int delayTicks = Math.max(0, ModSound.LOOP_VOID_START_DELAY_TICKS);
        if (this.activeTicks <= delayTicks) {
            return 0.0F;
        }

        int fadeInTicks = Math.max(0, ModSound.LOOP_VOID_FADE_IN_TICKS);
        if (fadeInTicks == 0) {
            return targetVolume;
        }

        float progress = (this.activeTicks - delayTicks) / (float) fadeInTicks;
        return targetVolume * Mth.clamp(progress, 0.0F, 1.0F);
    }

    private void tickFadeOut() {
        this.fadeOutTicks++;
        int configuredFadeOutTicks = Math.max(1, ModSound.LOOP_VOID_FADE_OUT_TICKS);
        float progress = this.fadeOutTicks / (float) configuredFadeOutTicks;
        this.volume = this.fadeOutStartVolume * (1.0F - Mth.clamp(progress, 0.0F, 1.0F));

        if (this.fadeOutTicks >= configuredFadeOutTicks) {
            this.volume = 0.0F;
            this.stop();
        }
    }
}
