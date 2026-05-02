package com.example.voidcraft.Effect;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class VoidBeamInstance {
    private final Vec3 start;
    private final Vec3 end;
    private final float scale;
    public final Config config;
    private int age;

    public VoidBeamInstance(Vec3 start, Vec3 end, float scale, Config config) {
        this.start = start;
        this.end = end;
        this.scale = Math.max(0.01F, scale);
        this.config = config == null ? Config.DEFAULT : config;
    }

    public Vec3 start() {
        return this.start;
    }

    public Vec3 end() {
        return this.end;
    }

    public float scale() {
        return this.scale;
    }

    public int age() {
        return this.age;
    }

    public void tick() {
        this.age++;
    }

    public boolean isDead() {
        return this.age >= this.config.lifetimeTicks();
    }

    public float fade(float partialTick) {
        float progress = Mth.clamp(
                (this.age + partialTick) / Math.max(1.0F, this.config.lifetimeTicks()),
                0.0F,
                1.0F
        );
        float fadeIn = this.config.fadeInRatio() <= 0.0F
                ? 1.0F
                : smoothstep(0.0F, this.config.fadeInRatio(), progress);
        float fadeOut = this.config.fadeOutRatio() <= 0.0F
                ? 1.0F
                : 1.0F - smoothstep(1.0F - this.config.fadeOutRatio(), 1.0F, progress);
        return fadeIn * fadeOut;
    }

    private static float smoothstep(float start, float end, float value) {
        if (start == end) {
            return value < start ? 0.0F : 1.0F;
        }
        float t = Mth.clamp((value - start) / (end - start), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    public static final class Config {
        public static final Config DEFAULT = builder().build();

        private final int lifetimeTicks;
        private final float coreRadius;
        private final float glowRadius;
        private final float startRadiusScale;
        private final float endRadiusScale;
        private final float coreAlpha;
        private final float glowAlpha;
        private final float crossAlphaScale;
        private final float fadeInRatio;
        private final float fadeOutRatio;
        private final float shaderCompatCoreGain;
        private final float shaderCompatGlowGain;
        private final float shaderCompatBloomAlphaScale;
        private final float shaderCompatBloomWidthScale;
        private final int coreColor;
        private final int glowColor;

        private Config(Builder builder) {
            this.lifetimeTicks = builder.lifetimeTicks;
            this.coreRadius = builder.coreRadius;
            this.glowRadius = builder.glowRadius;
            this.startRadiusScale = builder.startRadiusScale;
            this.endRadiusScale = builder.endRadiusScale;
            this.coreAlpha = builder.coreAlpha;
            this.glowAlpha = builder.glowAlpha;
            this.crossAlphaScale = builder.crossAlphaScale;
            this.fadeInRatio = builder.fadeInRatio;
            this.fadeOutRatio = builder.fadeOutRatio;
            this.shaderCompatCoreGain = builder.shaderCompatCoreGain;
            this.shaderCompatGlowGain = builder.shaderCompatGlowGain;
            this.shaderCompatBloomAlphaScale = builder.shaderCompatBloomAlphaScale;
            this.shaderCompatBloomWidthScale = builder.shaderCompatBloomWidthScale;
            this.coreColor = builder.coreColor;
            this.glowColor = builder.glowColor;
        }

        public int lifetimeTicks() {
            return this.lifetimeTicks;
        }

        public float coreRadius() {
            return this.coreRadius;
        }

        public float glowRadius() {
            return this.glowRadius;
        }

        public float startRadiusScale() {
            return this.startRadiusScale;
        }

        public float endRadiusScale() {
            return this.endRadiusScale;
        }

        public float coreAlpha() {
            return this.coreAlpha;
        }

        public float glowAlpha() {
            return this.glowAlpha;
        }

        public float crossAlphaScale() {
            return this.crossAlphaScale;
        }

        public float fadeInRatio() {
            return this.fadeInRatio;
        }

        public float fadeOutRatio() {
            return this.fadeOutRatio;
        }

        public float shaderCompatCoreGain() {
            return this.shaderCompatCoreGain;
        }

        public float shaderCompatGlowGain() {
            return this.shaderCompatGlowGain;
        }

        public float shaderCompatBloomAlphaScale() {
            return this.shaderCompatBloomAlphaScale;
        }

        public float shaderCompatBloomWidthScale() {
            return this.shaderCompatBloomWidthScale;
        }

        public int coreColor() {
            return this.coreColor;
        }

        public int glowColor() {
            return this.glowColor;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private int lifetimeTicks = 5;
            private float coreRadius = 0.045F;
            private float glowRadius = 0.16F;
            private float startRadiusScale = 1.0F;
            private float endRadiusScale = 0.70F;
            private float coreAlpha = 0.88F;
            private float glowAlpha = 0.36F;
            private float crossAlphaScale = 0.42F;
            private float fadeInRatio = 0.08F;
            private float fadeOutRatio = 0.68F;
            private float shaderCompatCoreGain = 1.18F;
            private float shaderCompatGlowGain = 1.32F;
            private float shaderCompatBloomAlphaScale = 0.74F;
            private float shaderCompatBloomWidthScale = 1.35F;
            private int coreColor = 0xE6FFF4;
            private int glowColor = 0x43FFC8;

            private Builder() {
            }

            public Builder lifetimeTicks(int lifetimeTicks) {
                this.lifetimeTicks = Math.max(1, lifetimeTicks);
                return this;
            }

            public Builder coreRadius(float coreRadius) {
                this.coreRadius = Math.max(0.001F, coreRadius);
                return this;
            }

            public Builder glowRadius(float glowRadius) {
                this.glowRadius = Math.max(0.001F, glowRadius);
                return this;
            }

            public Builder startRadiusScale(float startRadiusScale) {
                this.startRadiusScale = Math.max(0.001F, startRadiusScale);
                return this;
            }

            public Builder endRadiusScale(float endRadiusScale) {
                this.endRadiusScale = Math.max(0.001F, endRadiusScale);
                return this;
            }

            public Builder coreAlpha(float coreAlpha) {
                this.coreAlpha = Mth.clamp(coreAlpha, 0.0F, 1.0F);
                return this;
            }

            public Builder glowAlpha(float glowAlpha) {
                this.glowAlpha = Mth.clamp(glowAlpha, 0.0F, 1.0F);
                return this;
            }

            public Builder crossAlphaScale(float crossAlphaScale) {
                this.crossAlphaScale = Mth.clamp(crossAlphaScale, 0.0F, 1.0F);
                return this;
            }

            public Builder fadeInRatio(float fadeInRatio) {
                this.fadeInRatio = Mth.clamp(fadeInRatio, 0.0F, 1.0F);
                return this;
            }

            public Builder fadeOutRatio(float fadeOutRatio) {
                this.fadeOutRatio = Mth.clamp(fadeOutRatio, 0.0F, 1.0F);
                return this;
            }

            public Builder shaderCompatCoreGain(float shaderCompatCoreGain) {
                this.shaderCompatCoreGain = Math.max(0.0F, shaderCompatCoreGain);
                return this;
            }

            public Builder shaderCompatGlowGain(float shaderCompatGlowGain) {
                this.shaderCompatGlowGain = Math.max(0.0F, shaderCompatGlowGain);
                return this;
            }

            public Builder shaderCompatBloomAlphaScale(float shaderCompatBloomAlphaScale) {
                this.shaderCompatBloomAlphaScale = Math.max(0.0F, shaderCompatBloomAlphaScale);
                return this;
            }

            public Builder shaderCompatBloomWidthScale(float shaderCompatBloomWidthScale) {
                this.shaderCompatBloomWidthScale = Math.max(0.001F, shaderCompatBloomWidthScale);
                return this;
            }

            public Builder coreColor(int coreColor) {
                this.coreColor = coreColor;
                return this;
            }

            public Builder glowColor(int glowColor) {
                this.glowColor = glowColor;
                return this;
            }

            public Config build() {
                return new Config(this);
            }
        }
    }
}
