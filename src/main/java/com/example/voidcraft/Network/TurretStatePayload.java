package com.example.voidcraft.Network;

import com.example.voidcraft.Custom.Behavior.Turret.PhaseEmitterSlot;
import com.example.voidcraft.VoidCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// active 控制炮台球视觉，blocksInput 控制本地左/右键是否被手动炮台接管。
public record TurretStatePayload(int playerId, boolean active, boolean blocksInput, int emitterCount) implements CustomPacketPayload {
    public static final Type<TurretStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(VoidCraft.MODID, "turret_state"));

    public TurretStatePayload(int playerId, boolean active) {
        // 老入口默认保持手动炮台行为，避免调用方漏传时意外放行输入。
        this(playerId, active, true);
    }

    public TurretStatePayload(int playerId, boolean active, boolean blocksInput) {
        this(playerId, active, blocksInput, PhaseEmitterSlot.configuredCount());
    }

    public static final StreamCodec<ByteBuf, TurretStatePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeInt(payload.playerId());
                        buf.writeBoolean(payload.active());
                        buf.writeBoolean(payload.blocksInput());
                        buf.writeInt(payload.emitterCount());
                    },
                    buf -> new TurretStatePayload(
                            buf.readInt(),
                            buf.readBoolean(),
                            buf.readBoolean(),
                            buf.readInt()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
