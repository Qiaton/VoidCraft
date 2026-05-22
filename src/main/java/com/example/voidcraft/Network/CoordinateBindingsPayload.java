package com.example.voidcraft.Network;

import com.example.voidcraft.Custom.Behavior.Energy.BoundVoidPosition;
import com.example.voidcraft.Custom.Behavior.Energy.VoidEnergyBindingType;
import com.example.voidcraft.Custom.Behavior.Energy.VoidEnergyTransfer;
import com.example.voidcraft.VoidCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

// 服务端把某个虚空能方块的输入/输出绑定列表发给客户端解绑面板。
public record CoordinateBindingsPayload(BoundVoidPosition owner, List<Entry> entries) implements CustomPacketPayload {
    public static final Type<CoordinateBindingsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VoidCraft.MODID, "coordinate_bindings"));

    public static final StreamCodec<ByteBuf, CoordinateBindingsPayload> STREAM_CODEC = StreamCodec.of(
            CoordinateBindingsPayload::encode,
            CoordinateBindingsPayload::decode
    );

    public CoordinateBindingsPayload {
        entries = List.copyOf(entries);
    }

    private static void encode(ByteBuf buffer, CoordinateBindingsPayload payload) {
        // 包里不传方块实体本身，只传位置、方向、状态和显示名。
        writePosition(buffer, payload.owner);
        buffer.writeInt(payload.entries.size());
        for (Entry entry : payload.entries) {
            buffer.writeBoolean(entry.outputList());
            writePosition(buffer, entry.target());
            buffer.writeInt(entry.type().ordinal());
            buffer.writeInt(entry.status().ordinal());
            ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.encode(buffer, entry.targetName());
        }
    }

    private static CoordinateBindingsPayload decode(ByteBuf buffer) {
        BoundVoidPosition owner = readPosition(buffer);
        int size = Math.max(0, buffer.readInt());
        List<Entry> entries = new ArrayList<>(size);
        // 每条 entry 对应界面里的一行连接记录。
        for (int i = 0; i < size; i++) {
            boolean outputList = buffer.readBoolean();
            BoundVoidPosition target = readPosition(buffer);
            VoidEnergyBindingType type = readEnum(buffer.readInt(), VoidEnergyBindingType.values(), VoidEnergyBindingType.OUTPUT);
            VoidEnergyTransfer.BindingStatus status = readEnum(
                    buffer.readInt(),
                    VoidEnergyTransfer.BindingStatus.values(),
                    VoidEnergyTransfer.BindingStatus.NOT_FUNCTIONAL
            );
            Component targetName = ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.decode(buffer);
            entries.add(new Entry(outputList, target, type, status, targetName));
        }
        return new CoordinateBindingsPayload(owner, entries);
    }

    static void writePosition(ByteBuf buffer, BoundVoidPosition position) {
        // 网络包用二进制写位置，比 NBT 字符串更轻。
        Identifier.STREAM_CODEC.encode(buffer, position.dimension());
        BlockPos.STREAM_CODEC.encode(buffer, position.pos());
    }

    static BoundVoidPosition readPosition(ByteBuf buffer) {
        return new BoundVoidPosition(
                Identifier.STREAM_CODEC.decode(buffer),
                BlockPos.STREAM_CODEC.decode(buffer)
        );
    }

    private static <T> T readEnum(int ordinal, T[] values, T fallback) {
        // 客户端收到越界枚举时使用兜底值，避免坏包导致崩溃。
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : fallback;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(
            // true 表示这是 owner 的输出列表，false 表示这是 owner 的输入列表。
            boolean outputList,
            BoundVoidPosition target,
            VoidEnergyBindingType type,
            VoidEnergyTransfer.BindingStatus status,
            Component targetName
    ) {
    }
}
