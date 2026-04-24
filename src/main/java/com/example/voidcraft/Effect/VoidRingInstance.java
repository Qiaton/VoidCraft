package com.example.voidcraft.Effect;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class VoidRingInstance {
    public final Vec3 fallbackCenter;
    public final float scale;
    public final Preset preset;
    public final int ownerEntityId;
    public final int trackedEntityId;
    public int age;

    public VoidRingInstance(Vec3 center, float scale, Preset preset) {
        this(center, scale, preset, -1, -1);
    }

    public VoidRingInstance(Vec3 center, float scale, Preset preset, int trackedEntityId) {
        this(center, scale, preset, -1, trackedEntityId);
    }

    public VoidRingInstance(Vec3 center, float scale, Preset preset, int ownerEntityId, int trackedEntityId) {
        this.fallbackCenter = center;
        this.scale = scale;
        this.preset = preset;
        this.ownerEntityId = ownerEntityId;
        this.trackedEntityId = trackedEntityId;
        this.age = 0;
    }

    public Vec3 getCenter(Level level, float partialTick) {
        if (level != null && this.trackedEntityId >= 0) {
            Entity entity = level.getEntity(this.trackedEntityId);
            if (entity != null) {
                return entity.getPosition(partialTick).add(0.0D, this.preset.centerYOffset() * this.scale, 0.0D);
            }
        }

        return this.fallbackCenter;
    }

    public float getProgress(float partialTick) {
        return Mth.clamp((this.age + partialTick) / this.preset.durationTicks(), 0.0F, 1.0F);
    }

    public boolean isDead() {
        return this.age >= this.preset.durationTicks();
    }

    public static final class Preset {
        public static final Preset DEFAULT = builder().build();

        private final int durationTicks;
        private final float centerYOffset;
        private final boolean followCameraPitch;
        private final float startHalfHeight;
        private final float peakHalfHeight;
        private final float endHalfHeight;
        private final float startHalfWidth;
        private final float peakHalfWidth;
        private final float endHalfWidth;
        private final int peakHoldTicks;
        private final float glowAlpha;
        private final float glowWidthScale;
        private final float glowHeightScale;
        private final float shaderGlowWidthScale;
        private final float shaderGlowHeightScale;
        private final float shaderCompatOuterGlowGain;
        private final float shaderCompatCoreGain;
        private final float shaderCompatLineGain;
        private final float shaderCompatBloomGain;
        private final float shaderCompatBloomAlphaScale;
        private final float shaderCompatBloomGlowWeight;
        private final float shaderCompatBloomCoreWeight;
        private final float shaderCompatBloomCoreLayerGlowWeight;
        private final float shaderCompatBloomCoreLayerCoreWeight;
        private final float coreAlpha;
        private final float distortionAlpha;
        private final float lineAlpha;
        private final float distortionThickness;
        private final float distortionAmplitude;
        private final float distortionWidthScale;
        private final float distortionHeightScale;
        private final float noiseFrequency;
        private final float noiseScrollSpeed;

        private Preset(Builder builder) {
            this.durationTicks = builder.durationTicks;
            this.centerYOffset = builder.centerYOffset;
            this.followCameraPitch = builder.followCameraPitch;
            this.startHalfHeight = builder.startHalfHeight;
            this.peakHalfHeight = builder.peakHalfHeight;
            this.endHalfHeight = builder.endHalfHeight;
            this.startHalfWidth = builder.startHalfWidth;
            this.peakHalfWidth = builder.peakHalfWidth;
            this.endHalfWidth = builder.endHalfWidth;
            this.peakHoldTicks = Mth.clamp(builder.peakHoldTicks, 0, Math.max(0, builder.durationTicks - 1));
            this.glowAlpha = builder.glowAlpha;
            this.glowWidthScale = builder.glowWidthScale;
            this.glowHeightScale = builder.glowHeightScale;
            this.shaderGlowWidthScale = builder.shaderGlowWidthScale;
            this.shaderGlowHeightScale = builder.shaderGlowHeightScale;
            this.shaderCompatOuterGlowGain = builder.shaderCompatOuterGlowGain;
            this.shaderCompatCoreGain = builder.shaderCompatCoreGain;
            this.shaderCompatLineGain = builder.shaderCompatLineGain;
            this.shaderCompatBloomGain = builder.shaderCompatBloomGain;
            this.shaderCompatBloomAlphaScale = builder.shaderCompatBloomAlphaScale;
            this.shaderCompatBloomGlowWeight = builder.shaderCompatBloomGlowWeight;
            this.shaderCompatBloomCoreWeight = builder.shaderCompatBloomCoreWeight;
            this.shaderCompatBloomCoreLayerGlowWeight = builder.shaderCompatBloomCoreLayerGlowWeight;
            this.shaderCompatBloomCoreLayerCoreWeight = builder.shaderCompatBloomCoreLayerCoreWeight;
            this.coreAlpha = builder.coreAlpha;
            this.distortionAlpha = builder.distortionAlpha;
            this.lineAlpha = builder.lineAlpha;
            this.distortionThickness = builder.distortionThickness;
            this.distortionAmplitude = builder.distortionAmplitude;
            this.distortionWidthScale = builder.distortionWidthScale;
            this.distortionHeightScale = builder.distortionHeightScale;
            this.noiseFrequency = builder.noiseFrequency;
            this.noiseScrollSpeed = builder.noiseScrollSpeed;
        }

        public int durationTicks() {
            return this.durationTicks;
        }

        public float centerYOffset() {
            return this.centerYOffset;
        }

        public boolean followCameraPitch() {
            return this.followCameraPitch;
        }

        public float startHalfHeight() {
            return this.startHalfHeight;
        }

        public float peakHalfHeight() {
            return this.peakHalfHeight;
        }

        public float endHalfHeight() {
            return this.endHalfHeight;
        }

        public float startHalfWidth() {
            return this.startHalfWidth;
        }

        public float peakHalfWidth() {
            return this.peakHalfWidth;
        }

        public float endHalfWidth() {
            return this.endHalfWidth;
        }

        public int peakHoldTicks() {
            return this.peakHoldTicks;
        }

        public float glowAlpha() {
            return this.glowAlpha;
        }

        public float glowWidthScale() {
            return this.glowWidthScale;
        }

        public float glowHeightScale() {
            return this.glowHeightScale;
        }

        public float shaderGlowWidthScale() {
            return this.shaderGlowWidthScale;
        }

        public float shaderGlowHeightScale() {
            return this.shaderGlowHeightScale;
        }

        public float shaderCompatOuterGlowGain() {
            return this.shaderCompatOuterGlowGain;
        }

        public float shaderCompatCoreGain() {
            return this.shaderCompatCoreGain;
        }

        public float shaderCompatLineGain() {
            return this.shaderCompatLineGain;
        }

        public float shaderCompatBloomGain() {
            return this.shaderCompatBloomGain;
        }

        public float shaderCompatBloomAlphaScale() {
            return this.shaderCompatBloomAlphaScale;
        }

        public float shaderCompatBloomGlowWeight() {
            return this.shaderCompatBloomGlowWeight;
        }

        public float shaderCompatBloomCoreWeight() {
            return this.shaderCompatBloomCoreWeight;
        }

        public float shaderCompatBloomCoreLayerGlowWeight() {
            return this.shaderCompatBloomCoreLayerGlowWeight;
        }

        public float shaderCompatBloomCoreLayerCoreWeight() {
            return this.shaderCompatBloomCoreLayerCoreWeight;
        }

        public float coreAlpha() {
            return this.coreAlpha;
        }

        public float distortionAlpha() {
            return this.distortionAlpha;
        }

        public float lineAlpha() {
            return this.lineAlpha;
        }

        public float distortionThickness() {
            return this.distortionThickness;
        }

        public float distortionAmplitude() {
            return this.distortionAmplitude;
        }

        public float distortionWidthScale() {
            return this.distortionWidthScale;
        }

        public float distortionHeightScale() {
            return this.distortionHeightScale;
        }

        public float noiseFrequency() {
            return this.noiseFrequency;
        }

        public float noiseScrollSpeed() {
            return this.noiseScrollSpeed;
        }

        public Builder toBuilder() {
            return new Builder(this);
        }

        public Builder copy() {
            return this.toBuilder();
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            // 下面这组默认值就是这套入相位特效的主要调参入口，Builder 的同名方法对应修改这些项。
            private int durationTicks = 5; // 特效总时长，单位 tick。
            private float centerYOffset = 0.98F; // 特效中心相对玩家脚底的上移量，决定白光贴在身体哪一段。
            private boolean followCameraPitch = false; // 是否连 Y 轴俯仰也一起朝向镜头；关掉时只会在 XZ 平面转向镜头。
            private float startHalfHeight = 0.88F; // 起始阶段白光半高。
            private float peakHalfHeight = 2.52F; // 爆发阶段白光半高。
            private float endHalfHeight = 0.92F; // 收束阶段白光半高。
            private float startHalfWidth = 0.42F; // 起始阶段白光半宽，越小越像先从细线亮起。
            private float peakHalfWidth = 1.85F; // 爆发阶段白光半宽，决定白光瞬间张开的最大宽度。
            private float endHalfWidth = 0.028F; // 收线阶段白光半宽，越小最后那条线越细。
            private int peakHoldTicks = 1; // 白光到达最大形态后额外停留的时长，单位 tick。
            private float glowAlpha = 0.56F; // 白光外层柔光透明度，数值越高外沿会更亮、更有包裹感。
            private float glowWidthScale = 1.14F; // 非 shader 路径下，白光外圈相对主体的横向放大量。
            private float glowHeightScale = 1.14F; // 非 shader 路径下，白光外圈相对主体的纵向放大量。
            private float shaderGlowWidthScale = 1.42F; // shader bloom 路径下，最外层柔光相对主体的横向放大量。
            private float shaderGlowHeightScale = 1.42F; // shader bloom 路径下，最外层柔光相对主体的纵向放大量。
            private float shaderCompatOuterGlowGain = 1.95F; // 光影兼容下外层柔光亮度增益。
            private float shaderCompatCoreGain = 2.15F; // 光影兼容下主体亮度增益。
            private float shaderCompatLineGain = 1.90F; // 光影兼容下收线阶段亮度增益。
            private float shaderCompatBloomGain = 2.32F; // 光影 bloom 路径整体亮度增益。
            private float shaderCompatBloomAlphaScale = 1.0F; // 光影 bloom 分层透明度整体缩放。
            private float shaderCompatBloomGlowWeight = 1.08F; // 光影 bloom 中柔光对总亮度的贡献权重。
            private float shaderCompatBloomCoreWeight = 0.72F; // 光影 bloom 中主体对白光总亮度的贡献权重。
            private float shaderCompatBloomCoreLayerGlowWeight = 0.28F; // 光影 bloom 最内层里柔光分量的权重。
            private float shaderCompatBloomCoreLayerCoreWeight = 0.48F; // 光影 bloom 最内层里主体分量的权重。
            private float coreAlpha = 1.00F; // 白光主体透明度。
            private float distortionAlpha = 1.92F; // 空间膜整体可见度/影响权重。
            private float lineAlpha = 0.96F; // 最后收成竖线时的亮度。
            private float distortionThickness = 4.56F; // 空间膜外沿厚度，越大越像一层有体积的膜。
            private float distortionAmplitude = 7.78F; // 空间扭曲强度，越大背景被拉弯得越明显。
            private float distortionWidthScale = 3.06F; // 扭曲层相对白光的横向放大倍数；想和白光更贴就保持接近 1。
            private float distortionHeightScale = 3.04F; // 扭曲层相对白光的纵向放大倍数；数值越大越容易看起来和白光错位。
            private float noiseFrequency = 8.6F; // 扭曲噪声频率，越低越像大块膜面拉扯，越高越碎。
            private float noiseScrollSpeed = 6.68F; // 扭曲噪声滚动速度，决定空间膜流动得有多快。

            private Builder() {
            }

            private Builder(Preset preset) {
                this.durationTicks = preset.durationTicks;
                this.centerYOffset = preset.centerYOffset;
                this.followCameraPitch = preset.followCameraPitch;
                this.startHalfHeight = preset.startHalfHeight;
                this.peakHalfHeight = preset.peakHalfHeight;
                this.endHalfHeight = preset.endHalfHeight;
                this.startHalfWidth = preset.startHalfWidth;
                this.peakHalfWidth = preset.peakHalfWidth;
                this.endHalfWidth = preset.endHalfWidth;
                this.peakHoldTicks = preset.peakHoldTicks;
                this.glowAlpha = preset.glowAlpha;
                this.glowWidthScale = preset.glowWidthScale;
                this.glowHeightScale = preset.glowHeightScale;
                this.shaderGlowWidthScale = preset.shaderGlowWidthScale;
                this.shaderGlowHeightScale = preset.shaderGlowHeightScale;
                this.shaderCompatOuterGlowGain = preset.shaderCompatOuterGlowGain;
                this.shaderCompatCoreGain = preset.shaderCompatCoreGain;
                this.shaderCompatLineGain = preset.shaderCompatLineGain;
                this.shaderCompatBloomGain = preset.shaderCompatBloomGain;
                this.shaderCompatBloomAlphaScale = preset.shaderCompatBloomAlphaScale;
                this.shaderCompatBloomGlowWeight = preset.shaderCompatBloomGlowWeight;
                this.shaderCompatBloomCoreWeight = preset.shaderCompatBloomCoreWeight;
                this.shaderCompatBloomCoreLayerGlowWeight = preset.shaderCompatBloomCoreLayerGlowWeight;
                this.shaderCompatBloomCoreLayerCoreWeight = preset.shaderCompatBloomCoreLayerCoreWeight;
                this.coreAlpha = preset.coreAlpha;
                this.distortionAlpha = preset.distortionAlpha;
                this.lineAlpha = preset.lineAlpha;
                this.distortionThickness = preset.distortionThickness;
                this.distortionAmplitude = preset.distortionAmplitude;
                this.distortionWidthScale = preset.distortionWidthScale;
                this.distortionHeightScale = preset.distortionHeightScale;
                this.noiseFrequency = preset.noiseFrequency;
                this.noiseScrollSpeed = preset.noiseScrollSpeed;
            }

            public Builder durationTicks(int durationTicks) {
                this.durationTicks = Math.max(1, durationTicks);
                return this;
            }

            public Builder centerYOffset(float centerYOffset) {
                this.centerYOffset = Math.max(0.0F, centerYOffset);
                return this;
            }

            public Builder followCameraPitch(boolean followCameraPitch) {
                this.followCameraPitch = followCameraPitch;
                return this;
            }

            public Builder startHalfHeight(float startHalfHeight) {
                this.startHalfHeight = Math.max(0.01F, startHalfHeight);
                return this;
            }

            public Builder peakHalfHeight(float peakHalfHeight) {
                this.peakHalfHeight = Math.max(0.01F, peakHalfHeight);
                return this;
            }

            public Builder endHalfHeight(float endHalfHeight) {
                this.endHalfHeight = Math.max(0.01F, endHalfHeight);
                return this;
            }

            public Builder startHalfWidth(float startHalfWidth) {
                this.startHalfWidth = Math.max(0.001F, startHalfWidth);
                return this;
            }

            public Builder peakHalfWidth(float peakHalfWidth) {
                this.peakHalfWidth = Math.max(0.001F, peakHalfWidth);
                return this;
            }

            public Builder endHalfWidth(float endHalfWidth) {
                this.endHalfWidth = Math.max(0.001F, endHalfWidth);
                return this;
            }

            public Builder peakHoldTicks(int peakHoldTicks) {
                this.peakHoldTicks = Math.max(0, peakHoldTicks);
                return this;
            }

            public Builder glowAlpha(float glowAlpha) {
                this.glowAlpha = Math.max(0.0F, glowAlpha);
                return this;
            }

            public Builder glowWidthScale(float glowWidthScale) {
                this.glowWidthScale = Math.max(1.0F, glowWidthScale);
                return this;
            }

            public Builder glowHeightScale(float glowHeightScale) {
                this.glowHeightScale = Math.max(1.0F, glowHeightScale);
                return this;
            }

            public Builder shaderGlowWidthScale(float shaderGlowWidthScale) {
                this.shaderGlowWidthScale = Math.max(1.0F, shaderGlowWidthScale);
                return this;
            }

            public Builder shaderGlowHeightScale(float shaderGlowHeightScale) {
                this.shaderGlowHeightScale = Math.max(1.0F, shaderGlowHeightScale);
                return this;
            }

            public Builder shaderCompatOuterGlowGain(float shaderCompatOuterGlowGain) {
                this.shaderCompatOuterGlowGain = Math.max(0.0F, shaderCompatOuterGlowGain);
                return this;
            }

            public Builder shaderCompatCoreGain(float shaderCompatCoreGain) {
                this.shaderCompatCoreGain = Math.max(0.0F, shaderCompatCoreGain);
                return this;
            }

            public Builder shaderCompatLineGain(float shaderCompatLineGain) {
                this.shaderCompatLineGain = Math.max(0.0F, shaderCompatLineGain);
                return this;
            }

            public Builder shaderCompatBloomGain(float shaderCompatBloomGain) {
                this.shaderCompatBloomGain = Math.max(0.0F, shaderCompatBloomGain);
                return this;
            }

            public Builder shaderCompatBloomAlphaScale(float shaderCompatBloomAlphaScale) {
                this.shaderCompatBloomAlphaScale = Math.max(0.0F, shaderCompatBloomAlphaScale);
                return this;
            }

            public Builder shaderCompatBloomGlowWeight(float shaderCompatBloomGlowWeight) {
                this.shaderCompatBloomGlowWeight = Math.max(0.0F, shaderCompatBloomGlowWeight);
                return this;
            }

            public Builder shaderCompatBloomCoreWeight(float shaderCompatBloomCoreWeight) {
                this.shaderCompatBloomCoreWeight = Math.max(0.0F, shaderCompatBloomCoreWeight);
                return this;
            }

            public Builder shaderCompatBloomCoreLayerGlowWeight(float shaderCompatBloomCoreLayerGlowWeight) {
                this.shaderCompatBloomCoreLayerGlowWeight = Math.max(0.0F, shaderCompatBloomCoreLayerGlowWeight);
                return this;
            }

            public Builder shaderCompatBloomCoreLayerCoreWeight(float shaderCompatBloomCoreLayerCoreWeight) {
                this.shaderCompatBloomCoreLayerCoreWeight = Math.max(0.0F, shaderCompatBloomCoreLayerCoreWeight);
                return this;
            }

            public Builder coreAlpha(float coreAlpha) {
                this.coreAlpha = Math.max(0.0F, coreAlpha);
                return this;
            }

            public Builder distortionAlpha(float distortionAlpha) {
                this.distortionAlpha = Math.max(0.0F, distortionAlpha);
                return this;
            }

            public Builder lineAlpha(float lineAlpha) {
                this.lineAlpha = Math.max(0.0F, lineAlpha);
                return this;
            }

            public Builder distortionThickness(float distortionThickness) {
                this.distortionThickness = Math.max(0.0F, distortionThickness);
                return this;
            }

            public Builder distortionAmplitude(float distortionAmplitude) {
                this.distortionAmplitude = Math.max(0.0F, distortionAmplitude);
                return this;
            }

            public Builder distortionWidthScale(float distortionWidthScale) {
                this.distortionWidthScale = Math.max(0.1F, distortionWidthScale);
                return this;
            }

            public Builder distortionHeightScale(float distortionHeightScale) {
                this.distortionHeightScale = Math.max(0.1F, distortionHeightScale);
                return this;
            }

            public Builder noiseFrequency(float noiseFrequency) {
                this.noiseFrequency = Math.max(0.0F, noiseFrequency);
                return this;
            }

            public Builder noiseScrollSpeed(float noiseScrollSpeed) {
                this.noiseScrollSpeed = Math.max(0.0F, noiseScrollSpeed);
                return this;
            }

            public Preset build() {
                return new Preset(this);
            }
        }
    }
}
