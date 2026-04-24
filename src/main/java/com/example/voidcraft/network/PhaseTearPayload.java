package com.example.voidcraft.network;

import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.VoidCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PhaseTearPayload(
        int ownerEntityId,
        int trackedEntityId,
        double centerX,
        double centerY,
        double centerZ,
        float scale,
        int durationTicks,
        float centerYOffset,
        boolean followCameraPitch,
        float startHalfHeight,
        float peakHalfHeight,
        float endHalfHeight,
        float startHalfWidth,
        float peakHalfWidth,
        float endHalfWidth,
        int peakHoldTicks,
        float glowAlpha,
        float glowWidthScale,
        float glowHeightScale,
        float shaderGlowWidthScale,
        float shaderGlowHeightScale,
        float shaderCompatOuterGlowGain,
        float shaderCompatCoreGain,
        float shaderCompatLineGain,
        float shaderCompatBloomGain,
        float shaderCompatBloomAlphaScale,
        float shaderCompatBloomGlowWeight,
        float shaderCompatBloomCoreWeight,
        float shaderCompatBloomCoreLayerGlowWeight,
        float shaderCompatBloomCoreLayerCoreWeight,
        float coreAlpha,
        float distortionAlpha,
        float lineAlpha,
        float distortionThickness,
        float distortionAmplitude,
        float distortionWidthScale,
        float distortionHeightScale,
        float noiseFrequency,
        float noiseScrollSpeed
) implements CustomPacketPayload {
    public static final int NO_ENTITY = -1;

    public static final Type<PhaseTearPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VoidCraft.MODID, "phase_tear"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PhaseTearPayload> STREAM_CODEC = StreamCodec.of(
            PhaseTearPayload::encode,
            PhaseTearPayload::decode
    );

    public static PhaseTearPayload fromPreset(
            int ownerEntityId,
            int trackedEntityId,
            double centerX,
            double centerY,
            double centerZ,
            float scale,
            VoidRingInstance.Preset preset
    ) {
        return new PhaseTearPayload(
                ownerEntityId,
                trackedEntityId,
                centerX,
                centerY,
                centerZ,
                scale,
                preset.durationTicks(),
                preset.centerYOffset(),
                preset.followCameraPitch(),
                preset.startHalfHeight(),
                preset.peakHalfHeight(),
                preset.endHalfHeight(),
                preset.startHalfWidth(),
                preset.peakHalfWidth(),
                preset.endHalfWidth(),
                preset.peakHoldTicks(),
                preset.glowAlpha(),
                preset.glowWidthScale(),
                preset.glowHeightScale(),
                preset.shaderGlowWidthScale(),
                preset.shaderGlowHeightScale(),
                preset.shaderCompatOuterGlowGain(),
                preset.shaderCompatCoreGain(),
                preset.shaderCompatLineGain(),
                preset.shaderCompatBloomGain(),
                preset.shaderCompatBloomAlphaScale(),
                preset.shaderCompatBloomGlowWeight(),
                preset.shaderCompatBloomCoreWeight(),
                preset.shaderCompatBloomCoreLayerGlowWeight(),
                preset.shaderCompatBloomCoreLayerCoreWeight(),
                preset.coreAlpha(),
                preset.distortionAlpha(),
                preset.lineAlpha(),
                preset.distortionThickness(),
                preset.distortionAmplitude(),
                preset.distortionWidthScale(),
                preset.distortionHeightScale(),
                preset.noiseFrequency(),
                preset.noiseScrollSpeed()
        );
    }

    public VoidRingInstance.Preset toPreset() {
        return VoidRingInstance.Preset.builder()
                .durationTicks(this.durationTicks)
                .centerYOffset(this.centerYOffset)
                .followCameraPitch(this.followCameraPitch)
                .startHalfHeight(this.startHalfHeight)
                .peakHalfHeight(this.peakHalfHeight)
                .endHalfHeight(this.endHalfHeight)
                .startHalfWidth(this.startHalfWidth)
                .peakHalfWidth(this.peakHalfWidth)
                .endHalfWidth(this.endHalfWidth)
                .peakHoldTicks(this.peakHoldTicks)
                .glowAlpha(this.glowAlpha)
                .glowWidthScale(this.glowWidthScale)
                .glowHeightScale(this.glowHeightScale)
                .shaderGlowWidthScale(this.shaderGlowWidthScale)
                .shaderGlowHeightScale(this.shaderGlowHeightScale)
                .shaderCompatOuterGlowGain(this.shaderCompatOuterGlowGain)
                .shaderCompatCoreGain(this.shaderCompatCoreGain)
                .shaderCompatLineGain(this.shaderCompatLineGain)
                .shaderCompatBloomGain(this.shaderCompatBloomGain)
                .shaderCompatBloomAlphaScale(this.shaderCompatBloomAlphaScale)
                .shaderCompatBloomGlowWeight(this.shaderCompatBloomGlowWeight)
                .shaderCompatBloomCoreWeight(this.shaderCompatBloomCoreWeight)
                .shaderCompatBloomCoreLayerGlowWeight(this.shaderCompatBloomCoreLayerGlowWeight)
                .shaderCompatBloomCoreLayerCoreWeight(this.shaderCompatBloomCoreLayerCoreWeight)
                .coreAlpha(this.coreAlpha)
                .distortionAlpha(this.distortionAlpha)
                .lineAlpha(this.lineAlpha)
                .distortionThickness(this.distortionThickness)
                .distortionAmplitude(this.distortionAmplitude)
                .distortionWidthScale(this.distortionWidthScale)
                .distortionHeightScale(this.distortionHeightScale)
                .noiseFrequency(this.noiseFrequency)
                .noiseScrollSpeed(this.noiseScrollSpeed)
                .build();
    }

    public boolean hasOwnerEntity() {
        return this.ownerEntityId != NO_ENTITY;
    }

    public boolean hasTrackedEntity() {
        return this.trackedEntityId != NO_ENTITY;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, PhaseTearPayload payload) {
        ByteBufCodecs.VAR_INT.encode(buffer, payload.ownerEntityId);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.trackedEntityId);
        ByteBufCodecs.DOUBLE.encode(buffer, payload.centerX);
        ByteBufCodecs.DOUBLE.encode(buffer, payload.centerY);
        ByteBufCodecs.DOUBLE.encode(buffer, payload.centerZ);
        ByteBufCodecs.FLOAT.encode(buffer, payload.scale);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.durationTicks);
        ByteBufCodecs.FLOAT.encode(buffer, payload.centerYOffset);
        ByteBufCodecs.BOOL.encode(buffer, payload.followCameraPitch);
        ByteBufCodecs.FLOAT.encode(buffer, payload.startHalfHeight);
        ByteBufCodecs.FLOAT.encode(buffer, payload.peakHalfHeight);
        ByteBufCodecs.FLOAT.encode(buffer, payload.endHalfHeight);
        ByteBufCodecs.FLOAT.encode(buffer, payload.startHalfWidth);
        ByteBufCodecs.FLOAT.encode(buffer, payload.peakHalfWidth);
        ByteBufCodecs.FLOAT.encode(buffer, payload.endHalfWidth);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.peakHoldTicks);
        ByteBufCodecs.FLOAT.encode(buffer, payload.glowAlpha);
        ByteBufCodecs.FLOAT.encode(buffer, payload.glowWidthScale);
        ByteBufCodecs.FLOAT.encode(buffer, payload.glowHeightScale);
        ByteBufCodecs.FLOAT.encode(buffer, payload.shaderGlowWidthScale);
        ByteBufCodecs.FLOAT.encode(buffer, payload.shaderGlowHeightScale);
        ByteBufCodecs.FLOAT.encode(buffer, payload.shaderCompatOuterGlowGain);
        ByteBufCodecs.FLOAT.encode(buffer, payload.shaderCompatCoreGain);
        ByteBufCodecs.FLOAT.encode(buffer, payload.shaderCompatLineGain);
        ByteBufCodecs.FLOAT.encode(buffer, payload.shaderCompatBloomGain);
        ByteBufCodecs.FLOAT.encode(buffer, payload.shaderCompatBloomAlphaScale);
        ByteBufCodecs.FLOAT.encode(buffer, payload.shaderCompatBloomGlowWeight);
        ByteBufCodecs.FLOAT.encode(buffer, payload.shaderCompatBloomCoreWeight);
        ByteBufCodecs.FLOAT.encode(buffer, payload.shaderCompatBloomCoreLayerGlowWeight);
        ByteBufCodecs.FLOAT.encode(buffer, payload.shaderCompatBloomCoreLayerCoreWeight);
        ByteBufCodecs.FLOAT.encode(buffer, payload.coreAlpha);
        ByteBufCodecs.FLOAT.encode(buffer, payload.distortionAlpha);
        ByteBufCodecs.FLOAT.encode(buffer, payload.lineAlpha);
        ByteBufCodecs.FLOAT.encode(buffer, payload.distortionThickness);
        ByteBufCodecs.FLOAT.encode(buffer, payload.distortionAmplitude);
        ByteBufCodecs.FLOAT.encode(buffer, payload.distortionWidthScale);
        ByteBufCodecs.FLOAT.encode(buffer, payload.distortionHeightScale);
        ByteBufCodecs.FLOAT.encode(buffer, payload.noiseFrequency);
        ByteBufCodecs.FLOAT.encode(buffer, payload.noiseScrollSpeed);
    }

    private static PhaseTearPayload decode(RegistryFriendlyByteBuf buffer) {
        return new PhaseTearPayload(
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.DOUBLE.decode(buffer),
                ByteBufCodecs.DOUBLE.decode(buffer),
                ByteBufCodecs.DOUBLE.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
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
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer)
        );
    }
}
