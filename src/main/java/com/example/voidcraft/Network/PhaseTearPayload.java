package com.example.voidcraft.Network;

import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.VoidCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record PhaseTearPayload(
        UUID effectId,
        int ageTicks,
        int ownerEntityId,
        int trackedEntityId,
        double centerX,
        double centerY,
        double centerZ,
        float scale,
        float yaw,
        VoidRingInstance.Preset.RenderStyle renderStyle,
        int durationTicks,
        float centerYOffset,
        boolean followCameraYaw,
        boolean followCameraPitch,
        boolean distortionFollowCameraYaw,
        boolean distortionFollowCameraPitch,
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
        int color,
        float filledFadeStart,
        float swirlStrength,
        float suctionStrength,
        boolean occludedByBlocks,
        float distortionThickness,
        float distortionAmplitude,
        float distortionWidthScale,
        float distortionHeightScale,
        float noiseFrequency,
        float noiseScrollSpeed
) implements CustomPacketPayload {
    public static final int NO_ENTITY = -1;

    public static final Type<PhaseTearPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(VoidCraft.MODID, "phase_tear"));

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
        return fromPreset(UUID.randomUUID(), 0, ownerEntityId, trackedEntityId, centerX, centerY, centerZ, scale, 0.0F, preset);
    }

    public static PhaseTearPayload fromPreset(
            int ownerEntityId,
            int trackedEntityId,
            double centerX,
            double centerY,
            double centerZ,
            float scale,
            float yaw,
            VoidRingInstance.Preset preset
    ) {
        return fromPreset(UUID.randomUUID(), 0, ownerEntityId, trackedEntityId, centerX, centerY, centerZ, scale, yaw, preset);
    }

    public static PhaseTearPayload fromPreset(
            UUID effectId,
            int ageTicks,
            int ownerEntityId,
            int trackedEntityId,
            double centerX,
            double centerY,
            double centerZ,
            float scale,
            float yaw,
            VoidRingInstance.Preset preset
    ) {
        return new PhaseTearPayload(
                effectId == null ? UUID.randomUUID() : effectId,
                Math.max(0, ageTicks),
                ownerEntityId,
                trackedEntityId,
                centerX,
                centerY,
                centerZ,
                scale,
                yaw,
                preset.renderStyle(),
                preset.durationTicks(),
                preset.centerYOffset(),
                preset.followCameraYaw(),
                preset.followCameraPitch(),
                preset.distortionFollowCameraYaw(),
                preset.distortionFollowCameraPitch(),
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
                preset.color(),
                preset.filledFadeStart(),
                preset.swirlStrength(),
                preset.suctionStrength(),
                preset.occludedByBlocks(),
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
                .renderStyle(this.renderStyle)
                .durationTicks(this.durationTicks)
                .centerYOffset(this.centerYOffset)
                .followCameraYaw(this.followCameraYaw)
                .followCameraPitch(this.followCameraPitch)
                .distortionFollowCameraYaw(this.distortionFollowCameraYaw)
                .distortionFollowCameraPitch(this.distortionFollowCameraPitch)
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
                .color(this.color)
                .filledFadeStart(this.filledFadeStart)
                .swirlStrength(this.swirlStrength)
                .suctionStrength(this.suctionStrength)
                .occludedByBlocks(this.occludedByBlocks)
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
        writeUuid(buffer, payload.effectId);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.ageTicks);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.ownerEntityId);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.trackedEntityId);
        ByteBufCodecs.DOUBLE.encode(buffer, payload.centerX);
        ByteBufCodecs.DOUBLE.encode(buffer, payload.centerY);
        ByteBufCodecs.DOUBLE.encode(buffer, payload.centerZ);
        ByteBufCodecs.FLOAT.encode(buffer, payload.scale);
        ByteBufCodecs.FLOAT.encode(buffer, payload.yaw);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.renderStyle.id());
        ByteBufCodecs.VAR_INT.encode(buffer, payload.durationTicks);
        ByteBufCodecs.FLOAT.encode(buffer, payload.centerYOffset);
        ByteBufCodecs.BOOL.encode(buffer, payload.followCameraYaw);
        ByteBufCodecs.BOOL.encode(buffer, payload.followCameraPitch);
        ByteBufCodecs.BOOL.encode(buffer, payload.distortionFollowCameraYaw);
        ByteBufCodecs.BOOL.encode(buffer, payload.distortionFollowCameraPitch);
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
        ByteBufCodecs.VAR_INT.encode(buffer, payload.color);
        ByteBufCodecs.FLOAT.encode(buffer, payload.filledFadeStart);
        ByteBufCodecs.FLOAT.encode(buffer, payload.swirlStrength);
        ByteBufCodecs.FLOAT.encode(buffer, payload.suctionStrength);
        ByteBufCodecs.BOOL.encode(buffer, payload.occludedByBlocks);
        ByteBufCodecs.FLOAT.encode(buffer, payload.distortionThickness);
        ByteBufCodecs.FLOAT.encode(buffer, payload.distortionAmplitude);
        ByteBufCodecs.FLOAT.encode(buffer, payload.distortionWidthScale);
        ByteBufCodecs.FLOAT.encode(buffer, payload.distortionHeightScale);
        ByteBufCodecs.FLOAT.encode(buffer, payload.noiseFrequency);
        ByteBufCodecs.FLOAT.encode(buffer, payload.noiseScrollSpeed);
    }

    private static PhaseTearPayload decode(RegistryFriendlyByteBuf buffer) {
        UUID effectId = readUuid(buffer);
        int ageTicks = ByteBufCodecs.VAR_INT.decode(buffer);
        int ownerEntityId = ByteBufCodecs.VAR_INT.decode(buffer);
        int trackedEntityId = ByteBufCodecs.VAR_INT.decode(buffer);
        double centerX = ByteBufCodecs.DOUBLE.decode(buffer);
        double centerY = ByteBufCodecs.DOUBLE.decode(buffer);
        double centerZ = ByteBufCodecs.DOUBLE.decode(buffer);
        float scale = ByteBufCodecs.FLOAT.decode(buffer);
        float yaw = ByteBufCodecs.FLOAT.decode(buffer);
        VoidRingInstance.Preset.RenderStyle renderStyle = VoidRingInstance.Preset.RenderStyle.byId(ByteBufCodecs.VAR_INT.decode(buffer));
        int durationTicks = ByteBufCodecs.VAR_INT.decode(buffer);
        float centerYOffset = ByteBufCodecs.FLOAT.decode(buffer);
        boolean followCameraYaw = ByteBufCodecs.BOOL.decode(buffer);
        boolean followCameraPitch = ByteBufCodecs.BOOL.decode(buffer);
        boolean distortionFollowCameraYaw = ByteBufCodecs.BOOL.decode(buffer);
        boolean distortionFollowCameraPitch = ByteBufCodecs.BOOL.decode(buffer);
        float startHalfHeight = ByteBufCodecs.FLOAT.decode(buffer);
        float peakHalfHeight = ByteBufCodecs.FLOAT.decode(buffer);
        float endHalfHeight = ByteBufCodecs.FLOAT.decode(buffer);
        float startHalfWidth = ByteBufCodecs.FLOAT.decode(buffer);
        float peakHalfWidth = ByteBufCodecs.FLOAT.decode(buffer);
        float endHalfWidth = ByteBufCodecs.FLOAT.decode(buffer);
        int peakHoldTicks = ByteBufCodecs.VAR_INT.decode(buffer);
        float glowAlpha = ByteBufCodecs.FLOAT.decode(buffer);
        float glowWidthScale = ByteBufCodecs.FLOAT.decode(buffer);
        float glowHeightScale = ByteBufCodecs.FLOAT.decode(buffer);
        float shaderGlowWidthScale = ByteBufCodecs.FLOAT.decode(buffer);
        float shaderGlowHeightScale = ByteBufCodecs.FLOAT.decode(buffer);
        float shaderCompatOuterGlowGain = ByteBufCodecs.FLOAT.decode(buffer);
        float shaderCompatCoreGain = ByteBufCodecs.FLOAT.decode(buffer);
        float shaderCompatLineGain = ByteBufCodecs.FLOAT.decode(buffer);
        float shaderCompatBloomGain = ByteBufCodecs.FLOAT.decode(buffer);
        float shaderCompatBloomAlphaScale = ByteBufCodecs.FLOAT.decode(buffer);
        float shaderCompatBloomGlowWeight = ByteBufCodecs.FLOAT.decode(buffer);
        float shaderCompatBloomCoreWeight = ByteBufCodecs.FLOAT.decode(buffer);
        float shaderCompatBloomCoreLayerGlowWeight = ByteBufCodecs.FLOAT.decode(buffer);
        float shaderCompatBloomCoreLayerCoreWeight = ByteBufCodecs.FLOAT.decode(buffer);
        float coreAlpha = ByteBufCodecs.FLOAT.decode(buffer);
        float distortionAlpha = ByteBufCodecs.FLOAT.decode(buffer);
        float lineAlpha = ByteBufCodecs.FLOAT.decode(buffer);
        int color = ByteBufCodecs.VAR_INT.decode(buffer);
        float filledFadeStart = ByteBufCodecs.FLOAT.decode(buffer);
        float swirlStrength = ByteBufCodecs.FLOAT.decode(buffer);
        float suctionStrength = ByteBufCodecs.FLOAT.decode(buffer);
        boolean occludedByBlocks = ByteBufCodecs.BOOL.decode(buffer);
        float distortionThickness = ByteBufCodecs.FLOAT.decode(buffer);
        float distortionAmplitude = ByteBufCodecs.FLOAT.decode(buffer);
        float distortionWidthScale = ByteBufCodecs.FLOAT.decode(buffer);
        float distortionHeightScale = ByteBufCodecs.FLOAT.decode(buffer);
        float noiseFrequency = ByteBufCodecs.FLOAT.decode(buffer);
        float noiseScrollSpeed = ByteBufCodecs.FLOAT.decode(buffer);

        return new PhaseTearPayload(
                effectId,
                ageTicks,
                ownerEntityId,
                trackedEntityId,
                centerX,
                centerY,
                centerZ,
                scale,
                yaw,
                renderStyle,
                durationTicks,
                centerYOffset,
                followCameraYaw,
                followCameraPitch,
                distortionFollowCameraYaw,
                distortionFollowCameraPitch,
                startHalfHeight,
                peakHalfHeight,
                endHalfHeight,
                startHalfWidth,
                peakHalfWidth,
                endHalfWidth,
                peakHoldTicks,
                glowAlpha,
                glowWidthScale,
                glowHeightScale,
                shaderGlowWidthScale,
                shaderGlowHeightScale,
                shaderCompatOuterGlowGain,
                shaderCompatCoreGain,
                shaderCompatLineGain,
                shaderCompatBloomGain,
                shaderCompatBloomAlphaScale,
                shaderCompatBloomGlowWeight,
                shaderCompatBloomCoreWeight,
                shaderCompatBloomCoreLayerGlowWeight,
                shaderCompatBloomCoreLayerCoreWeight,
                coreAlpha,
                distortionAlpha,
                lineAlpha,
                color,
                filledFadeStart,
                swirlStrength,
                suctionStrength,
                occludedByBlocks,
                distortionThickness,
                distortionAmplitude,
                distortionWidthScale,
                distortionHeightScale,
                noiseFrequency,
                noiseScrollSpeed
        );
    }

    private static void writeUuid(RegistryFriendlyByteBuf buffer, UUID uuid) {
        UUID actualUuid = uuid == null ? UUID.randomUUID() : uuid;
        buffer.writeLong(actualUuid.getMostSignificantBits());
        buffer.writeLong(actualUuid.getLeastSignificantBits());
    }

    private static UUID readUuid(RegistryFriendlyByteBuf buffer) {
        return new UUID(buffer.readLong(), buffer.readLong());
    }
}
