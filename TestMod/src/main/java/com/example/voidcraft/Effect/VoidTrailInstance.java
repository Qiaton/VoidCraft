package com.example.voidcraft.Effect;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class VoidTrailInstance {
    private final List<TrailPoint> points = new ArrayList<>();
    public final float scale;
    public final Preset preset;

    public VoidTrailInstance(float scale, Preset preset) {
        this.scale = Math.max(0.01F, scale);
        this.preset = preset;
    }

    public void addPoint(Vec3 position) {
        if (position == null) {
            return;
        }
        if (!this.points.isEmpty() && this.points.get(this.points.size() - 1).position.distanceToSqr(position) < 1.0E-8D) {
            return;
        }
        this.points.add(new TrailPoint(position));
    }

    public void tick() {
        Iterator<TrailPoint> iterator = this.points.iterator();
        while (iterator.hasNext()) {
            TrailPoint point = iterator.next();
            point.age++;
            if (point.age >= this.preset.lifetimeTicks()) {
                iterator.remove();
            }
        }
    }

    public boolean hasRenderablePoints() {
        return this.points.size() >= 2;
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

    public static final class TrailPoint {
        public final Vec3 position;
        public int age;

        private TrailPoint(Vec3 position) {
            this.position = position;
            this.age = 0;
        }

        public float getLife(float partialTick, int lifetimeTicks) {
            float progress = Mth.clamp((this.age + partialTick) / Math.max(1, lifetimeTicks), 0.0F, 1.0F);
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
        private final float width;
        private final float height;
        private final float tailWidthScale;
        private final float tailHeightScale;
        private final float edgeFadeRatio;
        private final float headFadeRatio;
        private final float glowWidthMultiplier;
        private final float glowHeightMultiplier;
        private final float alpha;
        private final float glowAlpha;
        private final int tailColor;
        private final int headColor;

        private Preset(Builder builder) {
            this.startDelayTicks = builder.startDelayTicks;
            this.sampleIntervalTicks = builder.sampleIntervalTicks;
            this.lifetimeTicks = builder.lifetimeTicks;
            this.centerYOffset = builder.centerYOffset;
            this.minMoveDistance = builder.minMoveDistance;
            this.pointSpacing = builder.pointSpacing;
            this.width = builder.width;
            this.height = builder.height;
            this.tailWidthScale = builder.tailWidthScale;
            this.tailHeightScale = builder.tailHeightScale;
            this.edgeFadeRatio = builder.edgeFadeRatio;
            this.headFadeRatio = builder.headFadeRatio;
            this.glowWidthMultiplier = builder.glowWidthMultiplier;
            this.glowHeightMultiplier = builder.glowHeightMultiplier;
            this.alpha = builder.alpha;
            this.glowAlpha = builder.glowAlpha;
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

        public int tailColor() {
            return this.tailColor;
        }

        public int headColor() {
            return this.headColor;
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
            private int lifetimeTicks = 60; // 整条能量带里的轨迹点保留多久。
            private float centerYOffset = 0.86F; // 流光挂在玩家身上的高度偏移。
            private float minMoveDistance = 0.004F; // 单次累计位移达到这个阈值后才释放采样点，越小越容易补点。
            private float pointSpacing = 0.020F; // 连续轨迹带的补点间距，越小越连续，采样密度也越高。
            private float width = 0.31F; // 能量带横向半宽，决定 X/Z 方向的厚度。
            private float height = 0.52F; // 能量带在 Y 轴上的半高，越大越像竖向宽带。
            private float tailWidthScale = 0.72F; // 尾部横向宽度相对头部的比例。
            private float tailHeightScale = 0.84F; // 尾部纵向高度相对头部的比例。
            private float edgeFadeRatio = 0.78F; // 主带保留多少核心区域，其余部分用于边缘渐隐。
            private float headFadeRatio = 0.18F; // 靠玩家最近这一端沿长度的渐隐范围比例。
            private float glowWidthMultiplier = 1.85F; // 外层柔光横向放大倍数。
            private float glowHeightMultiplier = 1.22F; // 外层柔光纵向放大倍数。
            private float alpha = 0.48F; // 主能量带透明度。
            private float glowAlpha = 0.24F; // 外层柔光透明度。
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
                this.width = preset.width;
                this.height = preset.height;
                this.tailWidthScale = preset.tailWidthScale;
                this.tailHeightScale = preset.tailHeightScale;
                this.edgeFadeRatio = preset.edgeFadeRatio;
                this.headFadeRatio = preset.headFadeRatio;
                this.glowWidthMultiplier = preset.glowWidthMultiplier;
                this.glowHeightMultiplier = preset.glowHeightMultiplier;
                this.alpha = preset.alpha;
                this.glowAlpha = preset.glowAlpha;
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
