package com.example.voidcraft.Network;

import com.example.voidcraft.Effect.VoidBeamInstance;
import com.example.voidcraft.VoidCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

// 炮台射击只把命中坐标和光束配置同步给客户端，伤害始终在服务端结算。
public record TurretShotFxPayload(
        UUID effectId,
        int playerId,
        int emitterIndex,
        double targetX,
        double targetY,
        double targetZ,
        int lifetimeTicks,
        float coreRadius,
        float glowRadius,
        float startRadiusScale,
        float endRadiusScale,
        float coreAlpha,
        float glowAlpha,
        float crossAlphaScale,
        float fadeInRatio,
        float fadeOutRatio,
        float shaderCompatCoreGain,
        float shaderCompatGlowGain,
        float shaderCompatBloomAlphaScale,
        float shaderCompatBloomWidthScale,
        int coreColor,
        int glowColor
) implements CustomPacketPayload {
    public static final Type<TurretShotFxPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(VoidCraft.MODID, "turret_shot_fx"));

    public static final StreamCodec<ByteBuf, TurretShotFxPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        writeUuid(buf, payload.effectId());
                        buf.writeInt(payload.playerId());
                        buf.writeInt(payload.emitterIndex());
                        buf.writeDouble(payload.targetX());
                        buf.writeDouble(payload.targetY());
                        buf.writeDouble(payload.targetZ());
                        buf.writeInt(payload.lifetimeTicks());
                        buf.writeFloat(payload.coreRadius());
                        buf.writeFloat(payload.glowRadius());
                        buf.writeFloat(payload.startRadiusScale());
                        buf.writeFloat(payload.endRadiusScale());
                        buf.writeFloat(payload.coreAlpha());
                        buf.writeFloat(payload.glowAlpha());
                        buf.writeFloat(payload.crossAlphaScale());
                        buf.writeFloat(payload.fadeInRatio());
                        buf.writeFloat(payload.fadeOutRatio());
                        buf.writeFloat(payload.shaderCompatCoreGain());
                        buf.writeFloat(payload.shaderCompatGlowGain());
                        buf.writeFloat(payload.shaderCompatBloomAlphaScale());
                        buf.writeFloat(payload.shaderCompatBloomWidthScale());
                        buf.writeInt(payload.coreColor());
                        buf.writeInt(payload.glowColor());
                    },
                    buf -> new TurretShotFxPayload(
                            readUuid(buf),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readInt(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readInt(),
                            buf.readInt()
                    )
            );

    public static TurretShotFxPayload fromConfig(
            int playerId,
            int emitterIndex,
            double targetX,
            double targetY,
            double targetZ,
            VoidBeamInstance.Config config
    ) {
        return fromConfig(UUID.randomUUID(), playerId, emitterIndex, targetX, targetY, targetZ, config);
    }

    public static TurretShotFxPayload fromConfig(
            UUID effectId,
            int playerId,
            int emitterIndex,
            double targetX,
            double targetY,
            double targetZ,
            VoidBeamInstance.Config config
    ) {
        VoidBeamInstance.Config actualConfig = config == null ? VoidBeamInstance.Config.DEFAULT : config;
        return new TurretShotFxPayload(
                effectId == null ? UUID.randomUUID() : effectId,
                playerId,
                emitterIndex,
                targetX,
                targetY,
                targetZ,
                actualConfig.lifetimeTicks(),
                actualConfig.coreRadius(),
                actualConfig.glowRadius(),
                actualConfig.startRadiusScale(),
                actualConfig.endRadiusScale(),
                actualConfig.coreAlpha(),
                actualConfig.glowAlpha(),
                actualConfig.crossAlphaScale(),
                actualConfig.fadeInRatio(),
                actualConfig.fadeOutRatio(),
                actualConfig.shaderCompatCoreGain(),
                actualConfig.shaderCompatGlowGain(),
                actualConfig.shaderCompatBloomAlphaScale(),
                actualConfig.shaderCompatBloomWidthScale(),
                actualConfig.coreColor(),
                actualConfig.glowColor()
        );
    }

    public VoidBeamInstance.Config toBeamConfig() {
        return VoidBeamInstance.Config.builder()
                .lifetimeTicks(this.lifetimeTicks)
                .coreRadius(this.coreRadius)
                .glowRadius(this.glowRadius)
                .startRadiusScale(this.startRadiusScale)
                .endRadiusScale(this.endRadiusScale)
                .coreAlpha(this.coreAlpha)
                .glowAlpha(this.glowAlpha)
                .crossAlphaScale(this.crossAlphaScale)
                .fadeInRatio(this.fadeInRatio)
                .fadeOutRatio(this.fadeOutRatio)
                .shaderCompatCoreGain(this.shaderCompatCoreGain)
                .shaderCompatGlowGain(this.shaderCompatGlowGain)
                .shaderCompatBloomAlphaScale(this.shaderCompatBloomAlphaScale)
                .shaderCompatBloomWidthScale(this.shaderCompatBloomWidthScale)
                .coreColor(this.coreColor)
                .glowColor(this.glowColor)
                .build();
    }

    private static void writeUuid(ByteBuf buffer, UUID uuid) {
        UUID actualUuid = uuid == null ? UUID.randomUUID() : uuid;
        buffer.writeLong(actualUuid.getMostSignificantBits());
        buffer.writeLong(actualUuid.getLeastSignificantBits());
    }

    private static UUID readUuid(ByteBuf buffer) {
        return new UUID(buffer.readLong(), buffer.readLong());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
