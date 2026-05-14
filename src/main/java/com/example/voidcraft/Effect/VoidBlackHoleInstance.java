package com.example.voidcraft.Effect;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class VoidBlackHoleInstance {
    public Vec3 center;
    private Vec3 previousCenter;
    private Vec3 targetCenter;
    private double targetFollowStrength;
    public final float scale;
    public final Config config;
    public final int ownerEntityId;
    public final boolean persistent;
    public int age;

    public VoidBlackHoleInstance(Vec3 center, float scale, Config config) {
        this(center, scale, config, -1);
    }

    public VoidBlackHoleInstance(Vec3 center, float scale, Config config, int ownerEntityId) {
        this(center, scale, config, ownerEntityId, false);
    }

    public VoidBlackHoleInstance(Vec3 center, float scale, Config config, int ownerEntityId, boolean persistent) {
        this.center = center;
        this.previousCenter = center;
        this.targetCenter = center;
        this.targetFollowStrength = 0.55D;
        this.scale = scale;
        this.config = config == null ? Config.DEFAULT : config;
        this.ownerEntityId = ownerEntityId;
        this.persistent = persistent;
        this.age = 0;
    }

    public Vec3 getCenter() {
        return this.center;
    }

    public Vec3 getCenter(float partialTick) {
        if (this.persistent && this.previousCenter != null) {
            return this.previousCenter.lerp(this.center, Mth.clamp(partialTick, 0.0F, 1.0F));
        }

        return this.center;
    }

    public float getProgress(float partialTick) {
        if (this.persistent) {
            return 0.35F;
        }
        return Mth.clamp((this.age + partialTick) / this.config.durationTicks(), 0.0F, 1.0F);
    }

    public boolean isDead() {
        if (this.persistent) {
            return false;
        }
        return this.age >= this.config.durationTicks();
    }

    // 设置持续黑洞的目标坐标，实际渲染中心会在 tickPersistent 里平滑追过去。
    public void setTargetCenter(Vec3 center) {
        setTargetCenter(center, 0.55D);
    }

    public void setTargetCenter(Vec3 center, double followStrength) {
        this.targetCenter = center;
        this.targetFollowStrength = Mth.clamp(followStrength, 0.0D, 1.0D);
    }

    // 持续黑洞的每 tick 平滑更新：只移动中心点，不推进死亡逻辑。
    public void tickPersistent() {
        if (!this.persistent || this.targetCenter == null) {
            return;
        }

        if (this.targetFollowStrength >= 0.999D) {
            this.previousCenter = this.targetCenter;
            this.center = this.targetCenter;
            return;
        }

        this.previousCenter = this.center;
        this.center = this.center.lerp(this.targetCenter, this.targetFollowStrength);
    }

    public static final class Config {
        public static final Config DEFAULT = builder()
                .durationTicks(34)
                .centerYOffset(0.82F)
                .coreFollowCameraPitch(false)
                .coreYaw(0.0F)
                .distortionFollowCameraPitch(true)
                .flatGate(false)
                .startHalfHeight(0.18F)
                .peakHalfHeight(1.18F)
                .endHalfHeight(0.24F)
                .startHalfWidth(0.18F)
                .peakHalfWidth(1.18F)
                .endHalfWidth(0.24F)
                .peakHoldTicks(18)
                .rimAlpha(0.72F)
                .coreAlpha(1.00F)
                .rimAlphaScale(0.76F)
                .shaderRimAlphaScale(0.58F)
                .horizonAlphaScale(0.96F)
                .centerShadowScale(1.0F)
                .distortionAlpha(1.70F)
                .color(0x8EA3FF)
                .swirlStrength(0.32F)
                .suctionStrength(0.96F)
                .distortionThickness(4.80F)
                .distortionAmplitude(10.40F)
                .distortionWidthScale(3.20F)
                .distortionHeightScale(3.20F)
                .noiseFrequency(4.20F)
                .noiseScrollSpeed(2.60F)
                .build();

        private final int durationTicks;
        private final float centerYOffset;
        private final boolean coreFollowCameraPitch;
        private final float coreYaw;
        private final boolean distortionFollowCameraPitch;
        private final boolean flatGate;
        private final float startHalfHeight;
        private final float peakHalfHeight;
        private final float endHalfHeight;
        private final float startHalfWidth;
        private final float peakHalfWidth;
        private final float endHalfWidth;
        private final int peakHoldTicks;
        private final float coreAlpha;
        private final float rimAlpha;
        private final int coreColor;
        private final int color;
        private final float coreAlphaScale;
        private final float rimAlphaScale;
        private final float shaderRimAlphaScale;
        private final float horizonAlphaScale;
        private final float centerShadowScale;
        private final boolean hideFromOwnerInFirstPerson;
        private final float distortionAlpha;
        private final float swirlStrength;
        private final float suctionStrength;
        private final boolean occludedByBlocks;
        private final float distortionThickness;
        private final float distortionAmplitude;
        private final float distortionWidthScale;
        private final float distortionHeightScale;
        private final float noiseFrequency;
        private final float noiseScrollSpeed;

        private Config(Builder builder) {
            this.durationTicks = builder.durationTicks;
            this.centerYOffset = builder.centerYOffset;
            this.coreFollowCameraPitch = builder.coreFollowCameraPitch;
            this.coreYaw = builder.coreYaw;
            this.distortionFollowCameraPitch = builder.distortionFollowCameraPitch;
            this.flatGate = builder.flatGate;
            this.startHalfHeight = builder.startHalfHeight;
            this.peakHalfHeight = builder.peakHalfHeight;
            this.endHalfHeight = builder.endHalfHeight;
            this.startHalfWidth = builder.startHalfWidth;
            this.peakHalfWidth = builder.peakHalfWidth;
            this.endHalfWidth = builder.endHalfWidth;
            this.peakHoldTicks = Mth.clamp(builder.peakHoldTicks, 0, Math.max(0, builder.durationTicks - 1));
            this.coreAlpha = builder.coreAlpha;
            this.rimAlpha = builder.rimAlpha;
            this.coreColor = builder.coreColor;
            this.color = builder.color;
            this.coreAlphaScale = builder.coreAlphaScale;
            this.rimAlphaScale = builder.rimAlphaScale;
            this.shaderRimAlphaScale = builder.shaderRimAlphaScale;
            this.horizonAlphaScale = builder.horizonAlphaScale;
            this.centerShadowScale = builder.centerShadowScale;
            this.hideFromOwnerInFirstPerson = builder.hideFromOwnerInFirstPerson;
            this.distortionAlpha = builder.distortionAlpha;
            this.swirlStrength = builder.swirlStrength;
            this.suctionStrength = builder.suctionStrength;
            this.occludedByBlocks = builder.occludedByBlocks;
            this.distortionThickness = builder.distortionThickness;
            this.distortionAmplitude = builder.distortionAmplitude;
            this.distortionWidthScale = builder.distortionWidthScale;
            this.distortionHeightScale = builder.distortionHeightScale;
            this.noiseFrequency = builder.noiseFrequency;
            this.noiseScrollSpeed = builder.noiseScrollSpeed;
        }

        public int durationTicks() { return this.durationTicks; }
        public float centerYOffset() { return this.centerYOffset; }
        public boolean coreFollowCameraPitch() { return this.coreFollowCameraPitch; }
        public float coreYaw() { return this.coreYaw; }
        public boolean distortionFollowCameraPitch() { return this.distortionFollowCameraPitch; }
        public boolean flatGate() { return this.flatGate; }
        public float startHalfHeight() { return this.startHalfHeight; }
        public float peakHalfHeight() { return this.peakHalfHeight; }
        public float endHalfHeight() { return this.endHalfHeight; }
        public float startHalfWidth() { return this.startHalfWidth; }
        public float peakHalfWidth() { return this.peakHalfWidth; }
        public float endHalfWidth() { return this.endHalfWidth; }
        public int peakHoldTicks() { return this.peakHoldTicks; }
        public float coreAlpha() { return this.coreAlpha; }
        public float rimAlpha() { return this.rimAlpha; }
        public int coreColor() { return this.coreColor; }
        public int color() { return this.color; }
        public float coreAlphaScale() { return this.coreAlphaScale; }
        public float rimAlphaScale() { return this.rimAlphaScale; }
        public float shaderRimAlphaScale() { return this.shaderRimAlphaScale; }
        public float horizonAlphaScale() { return this.horizonAlphaScale; }
        public float centerShadowScale() { return this.centerShadowScale; }
        public boolean hideFromOwnerInFirstPerson() { return this.hideFromOwnerInFirstPerson; }
        public float distortionAlpha() { return this.distortionAlpha; }
        public float swirlStrength() { return this.swirlStrength; }
        public float suctionStrength() { return this.suctionStrength; }
        public boolean occludedByBlocks() { return this.occludedByBlocks; }
        public float distortionThickness() { return this.distortionThickness; }
        public float distortionAmplitude() { return this.distortionAmplitude; }
        public float distortionWidthScale() { return this.distortionWidthScale; }
        public float distortionHeightScale() { return this.distortionHeightScale; }
        public float noiseFrequency() { return this.noiseFrequency; }
        public float noiseScrollSpeed() { return this.noiseScrollSpeed; }

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
            private int durationTicks = 100; // 黑洞总持续时间，单位 tick。
            private float centerYOffset = 0.90F; // 黑洞中心相对命中点向上偏移的高度。
            private boolean coreFollowCameraPitch = false; // 门本体是否完整朝向镜头俯仰，默认保持世界竖直感。
            private float coreYaw = 0.0F; // 门本体不跟随镜头时使用的固定水平朝向，单位弧度。
            private boolean distortionFollowCameraPitch = true; // 后处理扭曲遮罩是否完整跟随镜头俯仰。
            private boolean flatGate = false; // 是否用面状切口渲染门本体，打开后像空间里只有一片切面。
            private float startHalfHeight = 0.16F; // 起始阶段黑洞半高。
            private float peakHalfHeight = 0.92F; // 爆发阶段黑洞最大半高。
            private float endHalfHeight = 0.12F; // 消散阶段黑洞收束后的半高。
            private float startHalfWidth = 0.16F; // 起始阶段黑洞半宽。
            private float peakHalfWidth = 0.92F; // 爆发阶段黑洞最大半宽。
            private float endHalfWidth = 0.12F; // 消散阶段黑洞收束后的半宽。
            private int peakHoldTicks = 12; // 到达最大尺寸后保持的时间，单位 tick。
            private float coreAlpha = 0.94F; // 事件视界核心透明度。
            private float rimAlpha = 0.82F; // 外圈蓝紫边缘光透明度。
            private int coreColor = 0x02030A; // 核心颜色，格式 0xRRGGBB。
            private int color = 0x6D7DFF; // 外圈和扭曲高光颜色，格式 0xRRGGBB。
            private float coreAlphaScale = 0.94F; // 核心球体渲染时的透明度倍率。
            private float rimAlphaScale = 0.88F; // 普通渲染路径下外圈透明度倍率。
            private float shaderRimAlphaScale = 0.72F; // 光影兼容发光层的外圈透明度倍率。
            private float horizonAlphaScale = 1.08F; // 事件视界中心暗面透明度倍率。
            private float centerShadowScale = 1.0F; // 后处理中心黑影强度，0 表示关掉黑心。
            private boolean hideFromOwnerInFirstPerson = true; // 第一人称下是否隐藏自己拥有的黑洞。
            private float distortionAlpha = 2.00F; // 后处理空间扭曲可见度/影响权重。
            private float swirlStrength = 0.78F; // 旋涡卷曲强度。
            private float suctionStrength = 0.88F; // 向中心吸入的扭曲强度。
            private boolean occludedByBlocks = true; // 后处理扭曲是否被前方方块/实体深度遮挡。
            private float distortionThickness = 5.80F; // 扭曲膜外沿厚度。
            private float distortionAmplitude = 11.50F; // 空间扭曲强度，越大背景拉弯越明显。
            private float distortionWidthScale = 5.24F; // 扭曲遮罩相对核心的横向放大倍数。
            private float distortionHeightScale = 5.24F; // 扭曲遮罩相对核心的纵向放大倍数。
            private float noiseFrequency = 5.20F; // 扭曲噪声频率，越高细节越碎。
            private float noiseScrollSpeed = 3.40F; // 扭曲噪声滚动速度。

            private Builder() {
            }

            private Builder(Config config) {
                this.durationTicks = config.durationTicks;
                this.centerYOffset = config.centerYOffset;
                this.coreFollowCameraPitch = config.coreFollowCameraPitch;
                this.coreYaw = config.coreYaw;
                this.distortionFollowCameraPitch = config.distortionFollowCameraPitch;
                this.flatGate = config.flatGate;
                this.startHalfHeight = config.startHalfHeight;
                this.peakHalfHeight = config.peakHalfHeight;
                this.endHalfHeight = config.endHalfHeight;
                this.startHalfWidth = config.startHalfWidth;
                this.peakHalfWidth = config.peakHalfWidth;
                this.endHalfWidth = config.endHalfWidth;
                this.peakHoldTicks = config.peakHoldTicks;
                this.coreAlpha = config.coreAlpha;
                this.rimAlpha = config.rimAlpha;
                this.coreColor = config.coreColor;
                this.color = config.color;
                this.coreAlphaScale = config.coreAlphaScale;
                this.rimAlphaScale = config.rimAlphaScale;
                this.shaderRimAlphaScale = config.shaderRimAlphaScale;
                this.horizonAlphaScale = config.horizonAlphaScale;
                this.centerShadowScale = config.centerShadowScale;
                this.hideFromOwnerInFirstPerson = config.hideFromOwnerInFirstPerson;
                this.distortionAlpha = config.distortionAlpha;
                this.swirlStrength = config.swirlStrength;
                this.suctionStrength = config.suctionStrength;
                this.occludedByBlocks = config.occludedByBlocks;
                this.distortionThickness = config.distortionThickness;
                this.distortionAmplitude = config.distortionAmplitude;
                this.distortionWidthScale = config.distortionWidthScale;
                this.distortionHeightScale = config.distortionHeightScale;
                this.noiseFrequency = config.noiseFrequency;
                this.noiseScrollSpeed = config.noiseScrollSpeed;
            }

            public Builder durationTicks(int durationTicks) { this.durationTicks = Math.max(1, durationTicks); return this; }
            public Builder centerYOffset(float centerYOffset) { this.centerYOffset = Math.max(0.0F, centerYOffset); return this; }
            public Builder coreFollowCameraPitch(boolean coreFollowCameraPitch) { this.coreFollowCameraPitch = coreFollowCameraPitch; return this; }
            public Builder coreYaw(float coreYaw) { this.coreYaw = coreYaw; return this; }
            public Builder distortionFollowCameraPitch(boolean distortionFollowCameraPitch) { this.distortionFollowCameraPitch = distortionFollowCameraPitch; return this; }
            public Builder flatGate(boolean flatGate) { this.flatGate = flatGate; return this; }
            public Builder startHalfHeight(float startHalfHeight) { this.startHalfHeight = Math.max(0.01F, startHalfHeight); return this; }
            public Builder peakHalfHeight(float peakHalfHeight) { this.peakHalfHeight = Math.max(0.01F, peakHalfHeight); return this; }
            public Builder endHalfHeight(float endHalfHeight) { this.endHalfHeight = Math.max(0.01F, endHalfHeight); return this; }
            public Builder startHalfWidth(float startHalfWidth) { this.startHalfWidth = Math.max(0.001F, startHalfWidth); return this; }
            public Builder peakHalfWidth(float peakHalfWidth) { this.peakHalfWidth = Math.max(0.001F, peakHalfWidth); return this; }
            public Builder endHalfWidth(float endHalfWidth) { this.endHalfWidth = Math.max(0.001F, endHalfWidth); return this; }
            public Builder peakHoldTicks(int peakHoldTicks) { this.peakHoldTicks = Math.max(0, peakHoldTicks); return this; }
            public Builder coreAlpha(float coreAlpha) { this.coreAlpha = Math.max(0.0F, coreAlpha); return this; }
            public Builder rimAlpha(float rimAlpha) { this.rimAlpha = Math.max(0.0F, rimAlpha); return this; }
            public Builder coreColor(int coreColor) { this.coreColor = coreColor & 0xFFFFFF; return this; }
            public Builder color(int color) { this.color = color & 0xFFFFFF; return this; }
            public Builder coreAlphaScale(float coreAlphaScale) { this.coreAlphaScale = Math.max(0.0F, coreAlphaScale); return this; }
            public Builder rimAlphaScale(float rimAlphaScale) { this.rimAlphaScale = Math.max(0.0F, rimAlphaScale); return this; }
            public Builder shaderRimAlphaScale(float shaderRimAlphaScale) { this.shaderRimAlphaScale = Math.max(0.0F, shaderRimAlphaScale); return this; }
            public Builder horizonAlphaScale(float horizonAlphaScale) { this.horizonAlphaScale = Math.max(0.0F, horizonAlphaScale); return this; }
            public Builder centerShadowScale(float centerShadowScale) { this.centerShadowScale = Mth.clamp(centerShadowScale, 0.0F, 1.0F); return this; }
            public Builder hideFromOwnerInFirstPerson(boolean hideFromOwnerInFirstPerson) { this.hideFromOwnerInFirstPerson = hideFromOwnerInFirstPerson; return this; }
            public Builder distortionAlpha(float distortionAlpha) { this.distortionAlpha = Math.max(0.0F, distortionAlpha); return this; }
            public Builder swirlStrength(float swirlStrength) { this.swirlStrength = Mth.clamp(swirlStrength, 0.0F, 1.0F); return this; }
            public Builder suctionStrength(float suctionStrength) { this.suctionStrength = Mth.clamp(suctionStrength, 0.0F, 1.0F); return this; }
            public Builder occludedByBlocks(boolean occludedByBlocks) { this.occludedByBlocks = occludedByBlocks; return this; }
            public Builder distortionThickness(float distortionThickness) { this.distortionThickness = Math.max(0.0F, distortionThickness); return this; }
            public Builder distortionAmplitude(float distortionAmplitude) { this.distortionAmplitude = Math.max(0.0F, distortionAmplitude); return this; }
            public Builder distortionWidthScale(float distortionWidthScale) { this.distortionWidthScale = Math.max(0.1F, distortionWidthScale); return this; }
            public Builder distortionHeightScale(float distortionHeightScale) { this.distortionHeightScale = Math.max(0.1F, distortionHeightScale); return this; }
            public Builder noiseFrequency(float noiseFrequency) { this.noiseFrequency = Math.max(0.0F, noiseFrequency); return this; }
            public Builder noiseScrollSpeed(float noiseScrollSpeed) { this.noiseScrollSpeed = Math.max(0.0F, noiseScrollSpeed); return this; }
            public Config build() { return new Config(this); }
        }
    }
}
