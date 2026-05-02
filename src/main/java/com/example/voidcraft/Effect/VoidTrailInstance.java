package com.example.voidcraft.Effect;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class VoidTrailInstance {
    private final List<TrailPoint> points = new ArrayList<>();
    public final float scale;
    public final Preset preset;
    private int tickCount;
    private int segmentId;

    public VoidTrailInstance(float scale, Preset preset) {
        this.scale = Math.max(0.01F, scale);
        this.preset = preset;
        this.tickCount = 0;
        this.segmentId = 0;
    }

    public void addPoint(Vec3 position) {
        if (position == null) {
            return;
        }
        if (!this.points.isEmpty() && this.points.get(this.points.size() - 1).position.distanceToSqr(position) < 1.0E-8D) {
            return;
        }
        this.points.add(new TrailPoint(position, this.tickCount, this.segmentId));
    }

    public void startNewSegment() {
        this.segmentId++;
    }

    public void tick() {
        this.tickCount++;
        int expiredCount = 0;
        int lifetimeTicks = this.preset.lifetimeTicks();
        while (expiredCount < this.points.size()
                && this.tickCount - this.points.get(expiredCount).birthTick >= lifetimeTicks) {
            expiredCount++;
        }
        if (expiredCount > 0) {
            this.points.subList(0, expiredCount).clear();
        }
    }

    public boolean hasRenderablePoints() {
        for (int i = 0; i < this.points.size() - 1; i++) {
            if (this.points.get(i).segmentId == this.points.get(i + 1).segmentId) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return this.points.isEmpty();
    }

    public Vec3 getLastPointPosition() {
        return this.points.isEmpty() ? null : this.points.get(this.points.size() - 1).position;
    }

    public List<TrailPoint> getPoints() {
        return Collections.unmodifiableList(this.points);
    }

    List<TrailPoint> rawPoints() {
        return this.points;
    }

    int currentTick() {
        return this.tickCount;
    }

    public static final class TrailPoint {
        public final Vec3 position;
        private final int birthTick;
        private final int segmentId;

        private TrailPoint(Vec3 position, int birthTick, int segmentId) {
            this.position = position;
            this.birthTick = birthTick;
            this.segmentId = segmentId;
        }

        public int segmentId() {
            return this.segmentId;
        }

        public float getLife(float partialTick, int lifetimeTicks, int currentTick) {
            float progress = Mth.clamp((currentTick - this.birthTick + partialTick) / Math.max(1, lifetimeTicks), 0.0F, 1.0F);
            return 1.0F - progress;
        }
    }

    public static final class Preset {
        public static final Preset DEFAULT = builder().build();

        private final int startDelayTicks;
        private final int sampleIntervalTicks;
        private final int lifetimeTicks;
        private final float centerYOffset;
        private final float minMoveDistance;
        private final float pointSpacing;
        private final int maxInterpolationSteps;
        private final float width;
        private final float height;
        private final float tailWidthScale;
        private final float tailHeightScale;
        private final float edgeFadeRatio;
        private final int ribbonFadeSegments;
        private final float headFadeRatio;
        private final float glowWidthMultiplier;
        private final float glowHeightMultiplier;
        private final float alpha;
        private final float glowAlpha;
        private final float shaderCompatVerticalGlowGain;
        private final float shaderCompatVerticalCoreGain;
        private final float shaderCompatSideGlowGain;
        private final float shaderCompatSideCoreGain;
        private final float shaderCompatBloomAlphaScale;
        private final float shaderCompatBloomWidthScale;
        private final float shaderCompatBloomHeightScale;
        private final float shaderCompatBloomTailWhiten;
        private final float shaderCompatBloomHeadWhiten;
        private final int tailColor;
        private final int headColor;

        private Preset(Builder builder) {
            this.startDelayTicks = builder.startDelayTicks;
            this.sampleIntervalTicks = builder.sampleIntervalTicks;
            this.lifetimeTicks = builder.lifetimeTicks;
            this.centerYOffset = builder.centerYOffset;
            this.minMoveDistance = builder.minMoveDistance;
            this.pointSpacing = builder.pointSpacing;
            this.maxInterpolationSteps = builder.maxInterpolationSteps;
            this.width = builder.width;
            this.height = builder.height;
            this.tailWidthScale = builder.tailWidthScale;
            this.tailHeightScale = builder.tailHeightScale;
            this.edgeFadeRatio = builder.edgeFadeRatio;
            this.ribbonFadeSegments = builder.ribbonFadeSegments;
            this.headFadeRatio = builder.headFadeRatio;
            this.glowWidthMultiplier = builder.glowWidthMultiplier;
            this.glowHeightMultiplier = builder.glowHeightMultiplier;
            this.alpha = builder.alpha;
            this.glowAlpha = builder.glowAlpha;
            this.shaderCompatVerticalGlowGain = builder.shaderCompatVerticalGlowGain;
            this.shaderCompatVerticalCoreGain = builder.shaderCompatVerticalCoreGain;
            this.shaderCompatSideGlowGain = builder.shaderCompatSideGlowGain;
            this.shaderCompatSideCoreGain = builder.shaderCompatSideCoreGain;
            this.shaderCompatBloomAlphaScale = builder.shaderCompatBloomAlphaScale;
            this.shaderCompatBloomWidthScale = builder.shaderCompatBloomWidthScale;
            this.shaderCompatBloomHeightScale = builder.shaderCompatBloomHeightScale;
            this.shaderCompatBloomTailWhiten = builder.shaderCompatBloomTailWhiten;
            this.shaderCompatBloomHeadWhiten = builder.shaderCompatBloomHeadWhiten;
            this.tailColor = builder.tailColor;
            this.headColor = builder.headColor;
        }

        public int startDelayTicks() {
            return this.startDelayTicks;
        }

        public int sampleIntervalTicks() {
            return this.sampleIntervalTicks;
        }

        public int lifetimeTicks() {
            return this.lifetimeTicks;
        }

        public float centerYOffset() {
            return this.centerYOffset;
        }

        public float minMoveDistance() {
            return this.minMoveDistance;
        }

        public float pointSpacing() {
            return this.pointSpacing;
        }

        public int maxInterpolationSteps() {
            return this.maxInterpolationSteps;
        }

        public float width() {
            return this.width;
        }

        public float height() {
            return this.height;
        }

        public float tailWidthScale() {
            return this.tailWidthScale;
        }

        public float tailHeightScale() {
            return this.tailHeightScale;
        }

        public float edgeFadeRatio() {
            return this.edgeFadeRatio;
        }

        public int ribbonFadeSegments() {
            return this.ribbonFadeSegments;
        }

        public float headFadeRatio() {
            return this.headFadeRatio;
        }

        public float glowWidthMultiplier() {
            return this.glowWidthMultiplier;
        }

        public float glowHeightMultiplier() {
            return this.glowHeightMultiplier;
        }

        public float alpha() {
            return this.alpha;
        }

        public float glowAlpha() {
            return this.glowAlpha;
        }

        public float shaderCompatVerticalGlowGain() {
            return this.shaderCompatVerticalGlowGain;
        }

        public float shaderCompatVerticalCoreGain() {
            return this.shaderCompatVerticalCoreGain;
        }

        public float shaderCompatSideGlowGain() {
            return this.shaderCompatSideGlowGain;
        }

        public float shaderCompatSideCoreGain() {
            return this.shaderCompatSideCoreGain;
        }

        public float shaderCompatBloomAlphaScale() {
            return this.shaderCompatBloomAlphaScale;
        }

        public float shaderCompatBloomWidthScale() {
            return this.shaderCompatBloomWidthScale;
        }

        public float shaderCompatBloomHeightScale() {
            return this.shaderCompatBloomHeightScale;
        }

        public float shaderCompatBloomTailWhiten() {
            return this.shaderCompatBloomTailWhiten;
        }

        public float shaderCompatBloomHeadWhiten() {
            return this.shaderCompatBloomHeadWhiten;
        }

        public int tailColor() {
            return this.tailColor;
        }

        public int headColor() {
            return this.headColor;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Preset other)) {
                return false;
            }
            return this.startDelayTicks == other.startDelayTicks
                    && this.sampleIntervalTicks == other.sampleIntervalTicks
                    && this.lifetimeTicks == other.lifetimeTicks
                    && Float.compare(this.centerYOffset, other.centerYOffset) == 0
                    && Float.compare(this.minMoveDistance, other.minMoveDistance) == 0
                    && Float.compare(this.pointSpacing, other.pointSpacing) == 0
                    && this.maxInterpolationSteps == other.maxInterpolationSteps
                    && Float.compare(this.width, other.width) == 0
                    && Float.compare(this.height, other.height) == 0
                    && Float.compare(this.tailWidthScale, other.tailWidthScale) == 0
                    && Float.compare(this.tailHeightScale, other.tailHeightScale) == 0
                    && Float.compare(this.edgeFadeRatio, other.edgeFadeRatio) == 0
                    && this.ribbonFadeSegments == other.ribbonFadeSegments
                    && Float.compare(this.headFadeRatio, other.headFadeRatio) == 0
                    && Float.compare(this.glowWidthMultiplier, other.glowWidthMultiplier) == 0
                    && Float.compare(this.glowHeightMultiplier, other.glowHeightMultiplier) == 0
                    && Float.compare(this.alpha, other.alpha) == 0
                    && Float.compare(this.glowAlpha, other.glowAlpha) == 0
                    && Float.compare(this.shaderCompatVerticalGlowGain, other.shaderCompatVerticalGlowGain) == 0
                    && Float.compare(this.shaderCompatVerticalCoreGain, other.shaderCompatVerticalCoreGain) == 0
                    && Float.compare(this.shaderCompatSideGlowGain, other.shaderCompatSideGlowGain) == 0
                    && Float.compare(this.shaderCompatSideCoreGain, other.shaderCompatSideCoreGain) == 0
                    && Float.compare(this.shaderCompatBloomAlphaScale, other.shaderCompatBloomAlphaScale) == 0
                    && Float.compare(this.shaderCompatBloomWidthScale, other.shaderCompatBloomWidthScale) == 0
                    && Float.compare(this.shaderCompatBloomHeightScale, other.shaderCompatBloomHeightScale) == 0
                    && Float.compare(this.shaderCompatBloomTailWhiten, other.shaderCompatBloomTailWhiten) == 0
                    && Float.compare(this.shaderCompatBloomHeadWhiten, other.shaderCompatBloomHeadWhiten) == 0
                    && this.tailColor == other.tailColor
                    && this.headColor == other.headColor;
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    this.startDelayTicks,
                    this.sampleIntervalTicks,
                    this.lifetimeTicks,
                    this.centerYOffset,
                    this.minMoveDistance,
                    this.pointSpacing,
                    this.maxInterpolationSteps,
                    this.width,
                    this.height,
                    this.tailWidthScale,
                    this.tailHeightScale,
                    this.edgeFadeRatio,
                    this.ribbonFadeSegments,
                    this.headFadeRatio,
                    this.glowWidthMultiplier,
                    this.glowHeightMultiplier,
                    this.alpha,
                    this.glowAlpha,
                    this.shaderCompatVerticalGlowGain,
                    this.shaderCompatVerticalCoreGain,
                    this.shaderCompatSideGlowGain,
                    this.shaderCompatSideCoreGain,
                    this.shaderCompatBloomAlphaScale,
                    this.shaderCompatBloomWidthScale,
                    this.shaderCompatBloomHeightScale,
                    this.shaderCompatBloomTailWhiten,
                    this.shaderCompatBloomHeadWhiten,
                    this.tailColor,
                    this.headColor
            );
        }

        public Builder toBuilder() {
            return new Builder(this);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            // 下面这组默认值是虚空移动流光的主要调参入口。
            private int startDelayTicks = 0; // 玩家连续移动多少 tick 后，流光才开始显示。
            private int sampleIntervalTicks = 1; // 轨迹采样间隔，越大轨迹点越稀。
            private int lifetimeTicks = 44; // 整条能量带里的轨迹点保留多久。
            private float centerYOffset = 0.86F; // 流光挂在玩家身上的高度偏移。
            private float minMoveDistance = 0.008F; // 单次累计位移达到这个阈值后才释放采样点，越小越容易补点。
            private float pointSpacing = 0.0367F; // 连续轨迹带的补点间距，越小越连续，采样密度也越高。
            private int maxInterpolationSteps = 12; // 单次补点最多插多少段，避免高速实体一次补出过多点。
            private float width = 0.21F; // 能量带横向半宽，决定 X/Z 方向的厚度。
            private float height = 0.62F; // 能量带在 Y 轴上的半高，越大越像竖向宽带。
            private float tailWidthScale = 0.72F; // 尾部横向宽度相对头部的比例。
            private float tailHeightScale = 0.84F; // 尾部纵向高度相对头部的比例。
            private float edgeFadeRatio = 0.78F; // 主带保留多少核心区域，其余部分用于边缘渐隐。
            private int ribbonFadeSegments = 7; // 边缘渐隐细分段数，越高越平滑，也越吃透明填充性能。
            private float headFadeRatio = 0.18F; // 靠玩家最近这一端沿长度的渐隐范围比例。
            private float glowWidthMultiplier = 1.85F; // 外层柔光横向放大倍数。
            private float glowHeightMultiplier = 1.22F; // 外层柔光纵向放大倍数。
            private float alpha = 0.42F; // 主能量带透明度。
            private float glowAlpha = 0.36F; // 外层柔光透明度。
            private float shaderCompatVerticalGlowGain = 1.26F; // 光影兼容下竖向柔光亮度增益。
            private float shaderCompatVerticalCoreGain = 1.004F; // 光影兼容下竖向主体亮度增益。
            private float shaderCompatSideGlowGain = 1.354F; // 光影兼容下侧向柔光亮度增益。
            private float shaderCompatSideCoreGain = 1.135F; // 光影兼容下侧向主体亮度增益。
            private float shaderCompatBloomAlphaScale = 0.70F; // 光影 bloom 路径整体亮度缩放。
            private float shaderCompatBloomWidthScale = 1.0F; // 光影 bloom 路径横向扩散缩放。
            private float shaderCompatBloomHeightScale = 1.0F; // 光影 bloom 路径纵向扩散缩放。
            private float shaderCompatBloomTailWhiten = 0.72F; // 光影 bloom 对尾部颜色向白色推进的比例。
            private float shaderCompatBloomHeadWhiten = 0.76F; // 光影 bloom 对头部颜色向白色推进的比例。
            private int tailColor = 0x365A86; // 能量带尾部颜色，偏深蓝。
            private int headColor = 0xA5CBE8; // 能量带头部颜色，偏亮蓝。

            private Builder() {
            }

            private Builder(Preset preset) {
                this.startDelayTicks = preset.startDelayTicks;
                this.sampleIntervalTicks = preset.sampleIntervalTicks;
                this.lifetimeTicks = preset.lifetimeTicks;
                this.centerYOffset = preset.centerYOffset;
                this.minMoveDistance = preset.minMoveDistance;
                this.pointSpacing = preset.pointSpacing;
                this.maxInterpolationSteps = preset.maxInterpolationSteps;
                this.width = preset.width;
                this.height = preset.height;
                this.tailWidthScale = preset.tailWidthScale;
                this.tailHeightScale = preset.tailHeightScale;
                this.edgeFadeRatio = preset.edgeFadeRatio;
                this.ribbonFadeSegments = preset.ribbonFadeSegments;
                this.headFadeRatio = preset.headFadeRatio;
                this.glowWidthMultiplier = preset.glowWidthMultiplier;
                this.glowHeightMultiplier = preset.glowHeightMultiplier;
                this.alpha = preset.alpha;
                this.glowAlpha = preset.glowAlpha;
                this.shaderCompatVerticalGlowGain = preset.shaderCompatVerticalGlowGain;
                this.shaderCompatVerticalCoreGain = preset.shaderCompatVerticalCoreGain;
                this.shaderCompatSideGlowGain = preset.shaderCompatSideGlowGain;
                this.shaderCompatSideCoreGain = preset.shaderCompatSideCoreGain;
                this.shaderCompatBloomAlphaScale = preset.shaderCompatBloomAlphaScale;
                this.shaderCompatBloomWidthScale = preset.shaderCompatBloomWidthScale;
                this.shaderCompatBloomHeightScale = preset.shaderCompatBloomHeightScale;
                this.shaderCompatBloomTailWhiten = preset.shaderCompatBloomTailWhiten;
                this.shaderCompatBloomHeadWhiten = preset.shaderCompatBloomHeadWhiten;
                this.tailColor = preset.tailColor;
                this.headColor = preset.headColor;
            }

            public Builder startDelayTicks(int startDelayTicks) {
                this.startDelayTicks = Math.max(0, startDelayTicks);
                return this;
            }

            public Builder sampleIntervalTicks(int sampleIntervalTicks) {
                this.sampleIntervalTicks = Math.max(1, sampleIntervalTicks);
                return this;
            }

            public Builder lifetimeTicks(int lifetimeTicks) {
                this.lifetimeTicks = Math.max(1, lifetimeTicks);
                return this;
            }

            public Builder centerYOffset(float centerYOffset) {
                this.centerYOffset = Math.max(0.0F, centerYOffset);
                return this;
            }

            public Builder minMoveDistance(float minMoveDistance) {
                this.minMoveDistance = Math.max(0.0F, minMoveDistance);
                return this;
            }

            public Builder pointSpacing(float pointSpacing) {
                this.pointSpacing = Math.max(0.001F, pointSpacing);
                return this;
            }

            public Builder maxInterpolationSteps(int maxInterpolationSteps) {
                this.maxInterpolationSteps = Math.max(1, maxInterpolationSteps);
                return this;
            }

            public Builder width(float width) {
                this.width = Math.max(0.001F, width);
                return this;
            }

            public Builder height(float height) {
                this.height = Math.max(0.001F, height);
                return this;
            }

            public Builder tailWidthScale(float tailWidthScale) {
                this.tailWidthScale = Math.max(0.05F, tailWidthScale);
                return this;
            }

            public Builder tailHeightScale(float tailHeightScale) {
                this.tailHeightScale = Math.max(0.05F, tailHeightScale);
                return this;
            }

            public Builder edgeFadeRatio(float edgeFadeRatio) {
                this.edgeFadeRatio = Mth.clamp(edgeFadeRatio, 0.05F, 0.98F);
                return this;
            }

            public Builder ribbonFadeSegments(int ribbonFadeSegments) {
                this.ribbonFadeSegments = Math.max(1, ribbonFadeSegments);
                return this;
            }

            public Builder headFadeRatio(float headFadeRatio) {
                this.headFadeRatio = Mth.clamp(headFadeRatio, 0.0F, 0.95F);
                return this;
            }

            public Builder glowWidthMultiplier(float glowWidthMultiplier) {
                this.glowWidthMultiplier = Math.max(1.0F, glowWidthMultiplier);
                return this;
            }

            public Builder glowHeightMultiplier(float glowHeightMultiplier) {
                this.glowHeightMultiplier = Math.max(1.0F, glowHeightMultiplier);
                return this;
            }

            public Builder alpha(float alpha) {
                this.alpha = Math.max(0.0F, alpha);
                return this;
            }

            public Builder glowAlpha(float glowAlpha) {
                this.glowAlpha = Math.max(0.0F, glowAlpha);
                return this;
            }

            public Builder shaderCompatVerticalGlowGain(float shaderCompatVerticalGlowGain) {
                this.shaderCompatVerticalGlowGain = Math.max(0.0F, shaderCompatVerticalGlowGain);
                return this;
            }

            public Builder shaderCompatVerticalCoreGain(float shaderCompatVerticalCoreGain) {
                this.shaderCompatVerticalCoreGain = Math.max(0.0F, shaderCompatVerticalCoreGain);
                return this;
            }

            public Builder shaderCompatSideGlowGain(float shaderCompatSideGlowGain) {
                this.shaderCompatSideGlowGain = Math.max(0.0F, shaderCompatSideGlowGain);
                return this;
            }

            public Builder shaderCompatSideCoreGain(float shaderCompatSideCoreGain) {
                this.shaderCompatSideCoreGain = Math.max(0.0F, shaderCompatSideCoreGain);
                return this;
            }

            public Builder shaderCompatBloomAlphaScale(float shaderCompatBloomAlphaScale) {
                this.shaderCompatBloomAlphaScale = Math.max(0.0F, shaderCompatBloomAlphaScale);
                return this;
            }

            public Builder shaderCompatBloomWidthScale(float shaderCompatBloomWidthScale) {
                this.shaderCompatBloomWidthScale = Math.max(0.0F, shaderCompatBloomWidthScale);
                return this;
            }

            public Builder shaderCompatBloomHeightScale(float shaderCompatBloomHeightScale) {
                this.shaderCompatBloomHeightScale = Math.max(0.0F, shaderCompatBloomHeightScale);
                return this;
            }

            public Builder shaderCompatBloomTailWhiten(float shaderCompatBloomTailWhiten) {
                this.shaderCompatBloomTailWhiten = Mth.clamp(shaderCompatBloomTailWhiten, 0.0F, 1.0F);
                return this;
            }

            public Builder shaderCompatBloomHeadWhiten(float shaderCompatBloomHeadWhiten) {
                this.shaderCompatBloomHeadWhiten = Mth.clamp(shaderCompatBloomHeadWhiten, 0.0F, 1.0F);
                return this;
            }

            public Builder tailColor(int tailColor) {
                this.tailColor = tailColor & 0xFFFFFF;
                return this;
            }

            public Builder headColor(int headColor) {
                this.headColor = headColor & 0xFFFFFF;
                return this;
            }

            public Preset build() {
                return new Preset(this);
            }
        }
    }
}
