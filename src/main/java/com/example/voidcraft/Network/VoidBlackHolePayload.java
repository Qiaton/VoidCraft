package com.example.voidcraft.Network;

import com.example.voidcraft.Effect.VoidBlackHoleInstance;
import com.example.voidcraft.VoidCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record VoidBlackHolePayload(
        UUID effectId,
        int ageTicks,
        int ownerEntityId,
        double centerX,
        double centerY,
        double centerZ,
        float scale,
        int durationTicks,
        float centerYOffset,
        boolean coreFollowCameraPitch,
        float coreYaw,
        boolean distortionFollowCameraPitch,
        boolean flatGate,
        float startHalfHeight,
        float peakHalfHeight,
        float endHalfHeight,
        float startHalfWidth,
        float peakHalfWidth,
        float endHalfWidth,
        int peakHoldTicks,
        float coreAlpha,
        float rimAlpha,
        int coreColor,
        int color,
        float coreAlphaScale,
        float rimAlphaScale,
        float shaderRimAlphaScale,
        float horizonAlphaScale,
        float centerShadowScale,
        boolean hideFromOwnerInFirstPerson,
        float distortionAlpha,
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

    public static final Type<VoidBlackHolePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(VoidCraft.MODID, "void_black_hole"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VoidBlackHolePayload> STREAM_CODEC = StreamCodec.of(
            VoidBlackHolePayload::encode,
            VoidBlackHolePayload::decode
    );

    public static VoidBlackHolePayload fromConfig(
            int ownerEntityId,
            double centerX,
            double centerY,
            double centerZ,
            float scale,
            VoidBlackHoleInstance.Config config
    ) {
        return fromConfig(UUID.randomUUID(), 0, ownerEntityId, centerX, centerY, centerZ, scale, config);
    }

    public static VoidBlackHolePayload fromConfig(
            UUID effectId,
            int ageTicks,
            int ownerEntityId,
            double centerX,
            double centerY,
            double centerZ,
            float scale,
            VoidBlackHoleInstance.Config config
    ) {
        VoidBlackHoleInstance.Config actualConfig = config == null ? VoidBlackHoleInstance.Config.DEFAULT : config;
        return new VoidBlackHolePayload(
                effectId == null ? UUID.randomUUID() : effectId,
                Math.max(0, ageTicks),
                ownerEntityId,
                centerX,
                centerY,
                centerZ,
                scale,
                actualConfig.durationTicks(),
                actualConfig.centerYOffset(),
                actualConfig.coreFollowCameraPitch(),
                actualConfig.coreYaw(),
                actualConfig.distortionFollowCameraPitch(),
                actualConfig.flatGate(),
                actualConfig.startHalfHeight(),
                actualConfig.peakHalfHeight(),
                actualConfig.endHalfHeight(),
                actualConfig.startHalfWidth(),
                actualConfig.peakHalfWidth(),
                actualConfig.endHalfWidth(),
                actualConfig.peakHoldTicks(),
                actualConfig.coreAlpha(),
                actualConfig.rimAlpha(),
                actualConfig.coreColor(),
                actualConfig.color(),
                actualConfig.coreAlphaScale(),
                actualConfig.rimAlphaScale(),
                actualConfig.shaderRimAlphaScale(),
                actualConfig.horizonAlphaScale(),
                actualConfig.centerShadowScale(),
                actualConfig.hideFromOwnerInFirstPerson(),
                actualConfig.distortionAlpha(),
                actualConfig.swirlStrength(),
                actualConfig.suctionStrength(),
                actualConfig.occludedByBlocks(),
                actualConfig.distortionThickness(),
                actualConfig.distortionAmplitude(),
                actualConfig.distortionWidthScale(),
                actualConfig.distortionHeightScale(),
                actualConfig.noiseFrequency(),
                actualConfig.noiseScrollSpeed()
        );
    }

    public VoidBlackHoleInstance.Config toConfig() {
        return VoidBlackHoleInstance.Config.builder()
                .durationTicks(this.durationTicks)
                .centerYOffset(this.centerYOffset)
                .coreFollowCameraPitch(this.coreFollowCameraPitch)
                .coreYaw(this.coreYaw)
                .distortionFollowCameraPitch(this.distortionFollowCameraPitch)
                .flatGate(this.flatGate)
                .startHalfHeight(this.startHalfHeight)
                .peakHalfHeight(this.peakHalfHeight)
                .endHalfHeight(this.endHalfHeight)
                .startHalfWidth(this.startHalfWidth)
                .peakHalfWidth(this.peakHalfWidth)
                .endHalfWidth(this.endHalfWidth)
                .peakHoldTicks(this.peakHoldTicks)
                .coreAlpha(this.coreAlpha)
                .rimAlpha(this.rimAlpha)
                .coreColor(this.coreColor)
                .color(this.color)
                .coreAlphaScale(this.coreAlphaScale)
                .rimAlphaScale(this.rimAlphaScale)
                .shaderRimAlphaScale(this.shaderRimAlphaScale)
                .horizonAlphaScale(this.horizonAlphaScale)
                .centerShadowScale(this.centerShadowScale)
                .hideFromOwnerInFirstPerson(this.hideFromOwnerInFirstPerson)
                .distortionAlpha(this.distortionAlpha)
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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, VoidBlackHolePayload payload) {
        writeUuid(buffer, payload.effectId);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.ageTicks);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.ownerEntityId);
        ByteBufCodecs.DOUBLE.encode(buffer, payload.centerX);
        ByteBufCodecs.DOUBLE.encode(buffer, payload.centerY);
        ByteBufCodecs.DOUBLE.encode(buffer, payload.centerZ);
        ByteBufCodecs.FLOAT.encode(buffer, payload.scale);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.durationTicks);
        ByteBufCodecs.FLOAT.encode(buffer, payload.centerYOffset);
        ByteBufCodecs.BOOL.encode(buffer, payload.coreFollowCameraPitch);
        ByteBufCodecs.FLOAT.encode(buffer, payload.coreYaw);
        ByteBufCodecs.BOOL.encode(buffer, payload.distortionFollowCameraPitch);
        ByteBufCodecs.BOOL.encode(buffer, payload.flatGate);
        ByteBufCodecs.FLOAT.encode(buffer, payload.startHalfHeight);
        ByteBufCodecs.FLOAT.encode(buffer, payload.peakHalfHeight);
        ByteBufCodecs.FLOAT.encode(buffer, payload.endHalfHeight);
        ByteBufCodecs.FLOAT.encode(buffer, payload.startHalfWidth);
        ByteBufCodecs.FLOAT.encode(buffer, payload.peakHalfWidth);
        ByteBufCodecs.FLOAT.encode(buffer, payload.endHalfWidth);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.peakHoldTicks);
        ByteBufCodecs.FLOAT.encode(buffer, payload.coreAlpha);
        ByteBufCodecs.FLOAT.encode(buffer, payload.rimAlpha);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.coreColor);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.color);
        ByteBufCodecs.FLOAT.encode(buffer, payload.coreAlphaScale);
        ByteBufCodecs.FLOAT.encode(buffer, payload.rimAlphaScale);
        ByteBufCodecs.FLOAT.encode(buffer, payload.shaderRimAlphaScale);
        ByteBufCodecs.FLOAT.encode(buffer, payload.horizonAlphaScale);
        ByteBufCodecs.FLOAT.encode(buffer, payload.centerShadowScale);
        ByteBufCodecs.BOOL.encode(buffer, payload.hideFromOwnerInFirstPerson);
        ByteBufCodecs.FLOAT.encode(buffer, payload.distortionAlpha);
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

    private static VoidBlackHolePayload decode(RegistryFriendlyByteBuf buffer) {
        UUID effectId = readUuid(buffer);
        int ageTicks = ByteBufCodecs.VAR_INT.decode(buffer);
        int ownerEntityId = ByteBufCodecs.VAR_INT.decode(buffer);
        double centerX = ByteBufCodecs.DOUBLE.decode(buffer);
        double centerY = ByteBufCodecs.DOUBLE.decode(buffer);
        double centerZ = ByteBufCodecs.DOUBLE.decode(buffer);
        float scale = ByteBufCodecs.FLOAT.decode(buffer);
        int durationTicks = ByteBufCodecs.VAR_INT.decode(buffer);
        float centerYOffset = ByteBufCodecs.FLOAT.decode(buffer);
        boolean coreFollowCameraPitch = ByteBufCodecs.BOOL.decode(buffer);
        float coreYaw = ByteBufCodecs.FLOAT.decode(buffer);
        boolean distortionFollowCameraPitch = ByteBufCodecs.BOOL.decode(buffer);
        boolean flatGate = ByteBufCodecs.BOOL.decode(buffer);
        float startHalfHeight = ByteBufCodecs.FLOAT.decode(buffer);
        float peakHalfHeight = ByteBufCodecs.FLOAT.decode(buffer);
        float endHalfHeight = ByteBufCodecs.FLOAT.decode(buffer);
        float startHalfWidth = ByteBufCodecs.FLOAT.decode(buffer);
        float peakHalfWidth = ByteBufCodecs.FLOAT.decode(buffer);
        float endHalfWidth = ByteBufCodecs.FLOAT.decode(buffer);
        int peakHoldTicks = ByteBufCodecs.VAR_INT.decode(buffer);
        float coreAlpha = ByteBufCodecs.FLOAT.decode(buffer);
        float rimAlpha = ByteBufCodecs.FLOAT.decode(buffer);
        int coreColor = ByteBufCodecs.VAR_INT.decode(buffer);
        int color = ByteBufCodecs.VAR_INT.decode(buffer);
        float coreAlphaScale = ByteBufCodecs.FLOAT.decode(buffer);
        float rimAlphaScale = ByteBufCodecs.FLOAT.decode(buffer);
        float shaderRimAlphaScale = ByteBufCodecs.FLOAT.decode(buffer);
        float horizonAlphaScale = ByteBufCodecs.FLOAT.decode(buffer);
        float centerShadowScale = ByteBufCodecs.FLOAT.decode(buffer);
        boolean hideFromOwnerInFirstPerson = ByteBufCodecs.BOOL.decode(buffer);
        float distortionAlpha = ByteBufCodecs.FLOAT.decode(buffer);
        float swirlStrength = ByteBufCodecs.FLOAT.decode(buffer);
        float suctionStrength = ByteBufCodecs.FLOAT.decode(buffer);
        boolean occludedByBlocks = ByteBufCodecs.BOOL.decode(buffer);
        float distortionThickness = ByteBufCodecs.FLOAT.decode(buffer);
        float distortionAmplitude = ByteBufCodecs.FLOAT.decode(buffer);
        float distortionWidthScale = ByteBufCodecs.FLOAT.decode(buffer);
        float distortionHeightScale = ByteBufCodecs.FLOAT.decode(buffer);
        float noiseFrequency = ByteBufCodecs.FLOAT.decode(buffer);
        float noiseScrollSpeed = ByteBufCodecs.FLOAT.decode(buffer);

        return new VoidBlackHolePayload(
                effectId,
                ageTicks,
                ownerEntityId,
                centerX,
                centerY,
                centerZ,
                scale,
                durationTicks,
                centerYOffset,
                coreFollowCameraPitch,
                coreYaw,
                distortionFollowCameraPitch,
                flatGate,
                startHalfHeight,
                peakHalfHeight,
                endHalfHeight,
                startHalfWidth,
                peakHalfWidth,
                endHalfWidth,
                peakHoldTicks,
                coreAlpha,
                rimAlpha,
                coreColor,
                color,
                coreAlphaScale,
                rimAlphaScale,
                shaderRimAlphaScale,
                horizonAlphaScale,
                centerShadowScale,
                hideFromOwnerInFirstPerson,
                distortionAlpha,
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
