package com.example.voidcraft.Network;

import com.example.voidcraft.VoidCraft;
import com.example.voidcraft.World.projection.PhaseProjectionSnapshot;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

// 服务端把原维度的轻量投影数据发给客户端；这里不传方块实体和 NBT。
public record PhaseProjectionPayload(PhaseProjectionSnapshot snapshot) implements CustomPacketPayload {
    public static final Type<PhaseProjectionPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VoidCraft.MODID, "phase_projection"));

    public static final StreamCodec<ByteBuf, PhaseProjectionPayload> STREAM_CODEC = StreamCodec.of(
            PhaseProjectionPayload::encode,
            PhaseProjectionPayload::decode
    );

    private static void encode(ByteBuf buffer, PhaseProjectionPayload payload) {
        PhaseProjectionSnapshot snapshot = payload.snapshot();
        Identifier.STREAM_CODEC.encode(buffer, snapshot.sourceDimension());
        BlockPos.STREAM_CODEC.encode(buffer, snapshot.center());
        ByteBufCodecs.VAR_INT.encode(buffer, snapshot.entries().size());
        for (PhaseProjectionSnapshot.Entry entry : snapshot.entries()) {
            BlockPos.STREAM_CODEC.encode(buffer, entry.pos());
            ByteBufCodecs.VAR_INT.encode(buffer, entry.stateId());
        }
    }

    private static PhaseProjectionPayload decode(ByteBuf buffer) {
        Identifier sourceDimension = Identifier.STREAM_CODEC.decode(buffer);
        BlockPos center = BlockPos.STREAM_CODEC.decode(buffer);
        int size = Math.max(0, ByteBufCodecs.VAR_INT.decode(buffer));
        List<PhaseProjectionSnapshot.Entry> entries = new ArrayList<>(size);
        // 每个 entry 只代表一个客户端线框投影点，不会落成真实方块。
        for (int i = 0; i < size; i++) {
            BlockPos pos = BlockPos.STREAM_CODEC.decode(buffer);
            int stateId = ByteBufCodecs.VAR_INT.decode(buffer);
            entries.add(new PhaseProjectionSnapshot.Entry(pos, stateId));
        }
        return new PhaseProjectionPayload(new PhaseProjectionSnapshot(sourceDimension, center, entries));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
