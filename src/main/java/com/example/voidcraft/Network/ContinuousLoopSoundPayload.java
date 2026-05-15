package com.example.voidcraft.Network;

import com.example.voidcraft.VoidCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

// 持续循环声音包：active=true 开始循环，active=false 按 id 停止循环。
public record ContinuousLoopSoundPayload(
        UUID id,
        boolean active,
        Identifier sound,
        double x,
        double y,
        double z,
        float volume,
        float pitch,
        int durationTicks
) implements CustomPacketPayload {
    public static final Type<ContinuousLoopSoundPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VoidCraft.MODID, "continuous_loop_sound"));

    public static final StreamCodec<ByteBuf, ContinuousLoopSoundPayload> STREAM_CODEC = StreamCodec.of(
            ContinuousLoopSoundPayload::encode,
            ContinuousLoopSoundPayload::decode
    );

    public static ContinuousLoopSoundPayload start(UUID id, Identifier sound, Vec3 center, float volume, float pitch, int durationTicks) {
        return new ContinuousLoopSoundPayload(
                id,
                true,
                sound,
                center.x,
                center.y,
                center.z,
                volume,
                pitch,
                durationTicks
        );
    }

    public static ContinuousLoopSoundPayload stop(UUID id, Identifier sound, Vec3 center) {
        return new ContinuousLoopSoundPayload(
                id,
                false,
                sound,
                center.x,
                center.y,
                center.z,
                0.0F,
                1.0F,
                0
        );
    }

    private static void encode(ByteBuf buffer, ContinuousLoopSoundPayload payload) {
        buffer.writeLong(payload.id.getMostSignificantBits());
        buffer.writeLong(payload.id.getLeastSignificantBits());
        ByteBufCodecs.BOOL.encode(buffer, payload.active);
        Identifier.STREAM_CODEC.encode(buffer, payload.sound);
        ByteBufCodecs.DOUBLE.encode(buffer, payload.x);
        ByteBufCodecs.DOUBLE.encode(buffer, payload.y);
        ByteBufCodecs.DOUBLE.encode(buffer, payload.z);
        ByteBufCodecs.FLOAT.encode(buffer, payload.volume);
        ByteBufCodecs.FLOAT.encode(buffer, payload.pitch);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.durationTicks);
    }

    private static ContinuousLoopSoundPayload decode(ByteBuf buffer) {
        UUID id = new UUID(buffer.readLong(), buffer.readLong());
        return new ContinuousLoopSoundPayload(
                id,
                ByteBufCodecs.BOOL.decode(buffer),
                Identifier.STREAM_CODEC.decode(buffer),
                ByteBufCodecs.DOUBLE.decode(buffer),
                ByteBufCodecs.DOUBLE.decode(buffer),
                ByteBufCodecs.DOUBLE.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer)
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
