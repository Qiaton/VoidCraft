package com.example.voidcraft.network;

import com.example.voidcraft.Custom.Behavior.Energy.BoundVoidPosition;
import com.example.voidcraft.Custom.Behavior.Energy.VoidEnergyTransfer;
import com.example.voidcraft.VoidCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

// 区块映射器状态面板只需要这一份快照；修改档位后服务端会重新发送。
public record ChunkMapperStatusPayload(
        BoundVoidPosition owner,
        int tier,
        int radius,
        int coverageSize,
        long energyCostPerTick,
        long energyStored,
        long energyCapacity,
        boolean running,
        @Nullable BoundVoidPosition inputSource,
        VoidEnergyTransfer.BindingStatus inputStatus,
        Component inputName
) implements CustomPacketPayload {
    public static final Type<ChunkMapperStatusPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VoidCraft.MODID, "chunk_mapper_status"));

    public static final StreamCodec<ByteBuf, ChunkMapperStatusPayload> STREAM_CODEC = StreamCodec.of(
            ChunkMapperStatusPayload::encode,
            ChunkMapperStatusPayload::decode
    );

    private static void encode(ByteBuf buffer, ChunkMapperStatusPayload payload) {
        // 输入源可能为空，所以先写一个 boolean 标记。
        CoordinateBindingsPayload.writePosition(buffer, payload.owner);
        buffer.writeInt(payload.tier);
        buffer.writeInt(payload.radius);
        buffer.writeInt(payload.coverageSize);
        buffer.writeLong(payload.energyCostPerTick);
        buffer.writeLong(payload.energyStored);
        buffer.writeLong(payload.energyCapacity);
        buffer.writeBoolean(payload.running);
        buffer.writeBoolean(payload.inputSource != null);
        if (payload.inputSource != null) {
            CoordinateBindingsPayload.writePosition(buffer, payload.inputSource);
        }
        buffer.writeInt(payload.inputStatus.ordinal());
        ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.encode(buffer, payload.inputName);
    }

    private static ChunkMapperStatusPayload decode(ByteBuf buffer) {
        BoundVoidPosition owner = CoordinateBindingsPayload.readPosition(buffer);
        int tier = buffer.readInt();
        int radius = buffer.readInt();
        int coverageSize = buffer.readInt();
        long energyCostPerTick = buffer.readLong();
        long energyStored = buffer.readLong();
        long energyCapacity = buffer.readLong();
        boolean running = buffer.readBoolean();
        BoundVoidPosition inputSource = buffer.readBoolean() ? CoordinateBindingsPayload.readPosition(buffer) : null;
        VoidEnergyTransfer.BindingStatus inputStatus = readStatus(buffer.readInt());
        Component inputName = ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.decode(buffer);
        return new ChunkMapperStatusPayload(
                owner,
                tier,
                radius,
                coverageSize,
                energyCostPerTick,
                energyStored,
                energyCapacity,
                running,
                inputSource,
                inputStatus,
                inputName
        );
    }

    private static VoidEnergyTransfer.BindingStatus readStatus(int ordinal) {
        // 防止旧客户端/坏数据把状态枚举读越界。
        VoidEnergyTransfer.BindingStatus[] values = VoidEnergyTransfer.BindingStatus.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : VoidEnergyTransfer.BindingStatus.NOT_FUNCTIONAL;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
