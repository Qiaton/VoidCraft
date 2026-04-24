package com.example.voidcraft.network;

import com.example.voidcraft.Effect.VoidTrailInstance;
import com.example.voidcraft.VoidCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public record VoidTrailPayload(
        int entityId,
        float scale,
        boolean hasSeedSegment,
        double seedStartX,
        double seedStartY,
        double seedStartZ,
        double seedEndX,
        double seedEndY,
        double seedEndZ,
        int startDelayTicks,
        int sampleIntervalTicks,
        int lifetimeTicks,
        float centerYOffset,
        float minMoveDistance,
        float pointSpacing,
        int maxInterpolationSteps,
        float width,
        float height,
        float tailWidthScale,
        float tailHeightScale,
        float edgeFadeRatio,
        int ribbonFadeSegments,
        float headFadeRatio,
        float glowWidthMultiplier,
        float glowHeightMultiplier,
        float alpha,
        float glowAlpha,
        float shaderCompatVerticalGlowGain,
        float shaderCompatVerticalCoreGain,
        float shaderCompatSideGlowGain,
        float shaderCompatSideCoreGain,
        float shaderCompatBloomAlphaScale,
        float shaderCompatBloomWidthScale,
        float shaderCompatBloomHeightScale,
        float shaderCompatBloomTailWhiten,
        float shaderCompatBloomHeadWhiten,
        int tailColor,
        int headColor
) implements CustomPacketPayload {
    public static final Type<VoidTrailPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VoidCraft.MODID, "void_trail"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VoidTrailPayload> STREAM_CODEC = StreamCodec.of(
            VoidTrailPayload::encode,
            VoidTrailPayload::decode
    );

    public static VoidTrailPayload fromPreset(int entityId, float scale, VoidTrailInstance.Preset preset) {
        return fromPreset(entityId, scale, null, null, preset);
    }

    public static VoidTrailPayload fromPreset(int entityId, float scale, Vec3 seedStart, Vec3 seedEnd, VoidTrailInstance.Preset preset) {
        boolean hasSeedSegment = seedStart != null && seedEnd != null && seedStart.distanceToSqr(seedEnd) >= 1.0E-8D;
        return new VoidTrailPayload(
                entityId,
                scale,
                hasSeedSegment,
                hasSeedSegment ? seedStart.x : 0.0D,
                hasSeedSegment ? seedStart.y : 0.0D,
                hasSeedSegment ? seedStart.z : 0.0D,
                hasSeedSegment ? seedEnd.x : 0.0D,
                hasSeedSegment ? seedEnd.y : 0.0D,
                hasSeedSegment ? seedEnd.z : 0.0D,
                preset.startDelayTicks(),
                preset.sampleIntervalTicks(),
                preset.lifetimeTicks(),
                preset.centerYOffset(),
                preset.minMoveDistance(),
                preset.pointSpacing(),
                preset.maxInterpolationSteps(),
                preset.width(),
                preset.height(),
                preset.tailWidthScale(),
                preset.tailHeightScale(),
                preset.edgeFadeRatio(),
                preset.ribbonFadeSegments(),
                preset.headFadeRatio(),
                preset.glowWidthMultiplier(),
                preset.glowHeightMultiplier(),
                preset.alpha(),
                preset.glowAlpha(),
                preset.shaderCompatVerticalGlowGain(),
                preset.shaderCompatVerticalCoreGain(),
                preset.shaderCompatSideGlowGain(),
                preset.shaderCompatSideCoreGain(),
                preset.shaderCompatBloomAlphaScale(),
                preset.shaderCompatBloomWidthScale(),
                preset.shaderCompatBloomHeightScale(),
                preset.shaderCompatBloomTailWhiten(),
                preset.shaderCompatBloomHeadWhiten(),
                preset.tailColor(),
                preset.headColor()
        );
    }

    public VoidTrailInstance.Preset toPreset() {
        return VoidTrailInstance.Preset.builder()
                .startDelayTicks(this.startDelayTicks)
                .sampleIntervalTicks(this.sampleIntervalTicks)
                .lifetimeTicks(this.lifetimeTicks)
                .centerYOffset(this.centerYOffset)
                .minMoveDistance(this.minMoveDistance)
                .pointSpacing(this.pointSpacing)
                .maxInterpolationSteps(this.maxInterpolationSteps)
                .width(this.width)
                .height(this.height)
                .tailWidthScale(this.tailWidthScale)
                .tailHeightScale(this.tailHeightScale)
                .edgeFadeRatio(this.edgeFadeRatio)
                .ribbonFadeSegments(this.ribbonFadeSegments)
                .headFadeRatio(this.headFadeRatio)
                .glowWidthMultiplier(this.glowWidthMultiplier)
                .glowHeightMultiplier(this.glowHeightMultiplier)
                .alpha(this.alpha)
                .glowAlpha(this.glowAlpha)
                .shaderCompatVerticalGlowGain(this.shaderCompatVerticalGlowGain)
                .shaderCompatVerticalCoreGain(this.shaderCompatVerticalCoreGain)
                .shaderCompatSideGlowGain(this.shaderCompatSideGlowGain)
                .shaderCompatSideCoreGain(this.shaderCompatSideCoreGain)
                .shaderCompatBloomAlphaScale(this.shaderCompatBloomAlphaScale)
                .shaderCompatBloomWidthScale(this.shaderCompatBloomWidthScale)
                .shaderCompatBloomHeightScale(this.shaderCompatBloomHeightScale)
                .shaderCompatBloomTailWhiten(this.shaderCompatBloomTailWhiten)
                .shaderCompatBloomHeadWhiten(this.shaderCompatBloomHeadWhiten)
                .tailColor(this.tailColor)
                .headColor(this.headColor)
                .build();
    }

    public Vec3 seedStart() {
        return this.hasSeedSegment ? new Vec3(this.seedStartX, this.seedStartY, this.seedStartZ) : null;
    }

    public Vec3 seedEnd() {
        return this.hasSeedSegment ? new Vec3(this.seedEndX, this.seedEndY, this.seedEndZ) : null;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, VoidTrailPayload payload) {
        ByteBufCodecs.VAR_INT.encode(buffer, payload.entityId);
        ByteBufCodecs.FLOAT.encode(buffer, payload.scale);
        ByteBufCodecs.BOOL.encode(buffer, payload.hasSeedSegment);
        ByteBufCodecs.DOUBLE.encode(buffer, payload.seedStartX);
        ByteBufCodecs.DOUBLE.encode(buffer, payload.seedStartY);
        ByteBufCodecs.DOUBLE.encode(buffer, payload.seedStartZ);
        ByteBufCodecs.DOUBLE.encode(buffer, payload.seedEndX);
        ByteBufCodecs.DOUBLE.encode(buffer, payload.seedEndY);
        ByteBufCodecs.DOUBLE.encode(buffer, payload.seedEndZ);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.startDelayTicks);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.sampleIntervalTicks);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.lifetimeTicks);
        ByteBufCodecs.FLOAT.encode(buffer, payload.centerYOffset);
        ByteBufCodecs.FLOAT.encode(buffer, payload.minMoveDistance);
        ByteBufCodecs.FLOAT.encode(buffer, payload.pointSpacing);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.maxInterpolationSteps);
        ByteBufCodecs.FLOAT.encode(buffer, payload.width);
        ByteBufCodecs.FLOAT.encode(buffer, payload.height);
        ByteBufCodecs.FLOAT.encode(buffer, payload.tailWidthScale);
        ByteBufCodecs.FLOAT.encode(buffer, payload.tailHeightScale);
        ByteBufCodecs.FLOAT.encode(buffer, payload.edgeFadeRatio);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.ribbonFadeSegments);
        ByteBufCodecs.FLOAT.encode(buffer, payload.headFadeRatio);
        ByteBufCodecs.FLOAT.encode(buffer, payload.glowWidthMultiplier);
        ByteBufCodecs.FLOAT.encode(buffer, payload.glowHeightMultiplier);
        ByteBufCodecs.FLOAT.encode(buffer, payload.alpha);
        ByteBufCodecs.FLOAT.encode(buffer, payload.glowAlpha);
        ByteBufCodecs.FLOAT.encode(buffer, payload.shaderCompatVerticalGlowGain);
        ByteBufCodecs.FLOAT.encode(buffer, payload.shaderCompatVerticalCoreGain);
        ByteBufCodecs.FLOAT.encode(buffer, payload.shaderCompatSideGlowGain);
        ByteBufCodecs.FLOAT.encode(buffer, payload.shaderCompatSideCoreGain);
        ByteBufCodecs.FLOAT.encode(buffer, payload.shaderCompatBloomAlphaScale);
        ByteBufCodecs.FLOAT.encode(buffer, payload.shaderCompatBloomWidthScale);
        ByteBufCodecs.FLOAT.encode(buffer, payload.shaderCompatBloomHeightScale);
        ByteBufCodecs.FLOAT.encode(buffer, payload.shaderCompatBloomTailWhiten);
        ByteBufCodecs.FLOAT.encode(buffer, payload.shaderCompatBloomHeadWhiten);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.tailColor);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.headColor);
    }

    private static VoidTrailPayload decode(RegistryFriendlyByteBuf buffer) {
        return new VoidTrailPayload(
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.DOUBLE.decode(buffer),
                ByteBufCodecs.DOUBLE.decode(buffer),
                ByteBufCodecs.DOUBLE.decode(buffer),
                ByteBufCodecs.DOUBLE.decode(buffer),
                ByteBufCodecs.DOUBLE.decode(buffer),
                ByteBufCodecs.DOUBLE.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer)
        );
    }
}
