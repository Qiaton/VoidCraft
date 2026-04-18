package com.example.voidcraft.network;

import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.VoidCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PhaseTearPayload(
        int entityId,
        double centerX,
        double centerY,
        double centerZ,
        float scale,
        int durationTicks,
        float startHalfHeight,
        float peakHalfHeight,
        float endHalfHeight,
        float startHalfWidth,
        float peakHalfWidth,
        float endHalfWidth,
        float glowAlpha,
        float coreAlpha,
        float distortionAlpha,
        float lineAlpha,
        float distortionThickness,
        float distortionAmplitude,
        float noiseFrequency,
        float noiseScrollSpeed,
        int playerFlashTicks
) implements CustomPacketPayload {
    public static final Type<PhaseTearPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VoidCraft.MODID, "phase_tear"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PhaseTearPayload> STREAM_CODEC = StreamCodec.of(
            PhaseTearPayload::encode,
            PhaseTearPayload::decode
    );

    public static PhaseTearPayload fromPreset(int entityId, double centerX, double centerY, double centerZ, float scale, VoidRingInstance.Preset preset) {
        return new PhaseTearPayload(
                entityId,
                centerX,
                centerY,
                centerZ,
                scale,
                preset.durationTicks(),
                preset.startHalfHeight(),
                preset.peakHalfHeight(),
                preset.endHalfHeight(),
                preset.startHalfWidth(),
                preset.peakHalfWidth(),
                preset.endHalfWidth(),
                preset.glowAlpha(),
                preset.coreAlpha(),
                preset.distortionAlpha(),
                preset.lineAlpha(),
                preset.distortionThickness(),
                preset.distortionAmplitude(),
                preset.noiseFrequency(),
                preset.noiseScrollSpeed(),
                preset.playerFlashTicks()
        );
    }

    public VoidRingInstance.Preset toPreset() {
        return VoidRingInstance.Preset.builder()
                .durationTicks(this.durationTicks)
                .startHalfHeight(this.startHalfHeight)
                .peakHalfHeight(this.peakHalfHeight)
                .endHalfHeight(this.endHalfHeight)
                .startHalfWidth(this.startHalfWidth)
                .peakHalfWidth(this.peakHalfWidth)
                .endHalfWidth(this.endHalfWidth)
                .glowAlpha(this.glowAlpha)
                .coreAlpha(this.coreAlpha)
                .distortionAlpha(this.distortionAlpha)
                .lineAlpha(this.lineAlpha)
                .distortionThickness(this.distortionThickness)
                .distortionAmplitude(this.distortionAmplitude)
                .noiseFrequency(this.noiseFrequency)
                .noiseScrollSpeed(this.noiseScrollSpeed)
                .playerFlashTicks(this.playerFlashTicks)
                .minRetriggerTicks(0)
                .build();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, PhaseTearPayload payload) {
        ByteBufCodecs.VAR_INT.encode(buffer, payload.entityId);
        ByteBufCodecs.DOUBLE.encode(buffer, payload.centerX);
        ByteBufCodecs.DOUBLE.encode(buffer, payload.centerY);
        ByteBufCodecs.DOUBLE.encode(buffer, payload.centerZ);
        ByteBufCodecs.FLOAT.encode(buffer, payload.scale);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.durationTicks);
        ByteBufCodecs.FLOAT.encode(buffer, payload.startHalfHeight);
        ByteBufCodecs.FLOAT.encode(buffer, payload.peakHalfHeight);
        ByteBufCodecs.FLOAT.encode(buffer, payload.endHalfHeight);
        ByteBufCodecs.FLOAT.encode(buffer, payload.startHalfWidth);
        ByteBufCodecs.FLOAT.encode(buffer, payload.peakHalfWidth);
        ByteBufCodecs.FLOAT.encode(buffer, payload.endHalfWidth);
        ByteBufCodecs.FLOAT.encode(buffer, payload.glowAlpha);
        ByteBufCodecs.FLOAT.encode(buffer, payload.coreAlpha);
        ByteBufCodecs.FLOAT.encode(buffer, payload.distortionAlpha);
        ByteBufCodecs.FLOAT.encode(buffer, payload.lineAlpha);
        ByteBufCodecs.FLOAT.encode(buffer, payload.distortionThickness);
        ByteBufCodecs.FLOAT.encode(buffer, payload.distortionAmplitude);
        ByteBufCodecs.FLOAT.encode(buffer, payload.noiseFrequency);
        ByteBufCodecs.FLOAT.encode(buffer, payload.noiseScrollSpeed);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.playerFlashTicks);
    }

    private static PhaseTearPayload decode(RegistryFriendlyByteBuf buffer) {
        return new PhaseTearPayload(
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.DOUBLE.decode(buffer),
                ByteBufCodecs.DOUBLE.decode(buffer),
                ByteBufCodecs.DOUBLE.decode(buffer),
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
                ByteBufCodecs.VAR_INT.decode(buffer)
        );
    }
}
