package com.example.voidcraft.Custom.Behavior.Energy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Optional;

// 虚空能绑定保存的是“维度 + 方块坐标”，不能只存 BlockPos，否则跨维度会串线。
public record BoundVoidPosition(Identifier dimension, BlockPos pos) {
    public static final Codec<BoundVoidPosition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("dimension").forGetter(BoundVoidPosition::dimension),
            BlockPos.CODEC.fieldOf("pos").forGetter(BoundVoidPosition::pos)
    ).apply(instance, BoundVoidPosition::new));

    public static BoundVoidPosition of(Level level, BlockPos pos) {
        return new BoundVoidPosition(level.dimension().identifier(), pos.immutable());
    }

    // 比较绑定点时只关心是不是同一个维度里的同一个方块。
    public boolean sameBlock(BoundVoidPosition other) {
        return other != null && this.dimension.equals(other.dimension) && this.pos.equals(other.pos);
    }

    // 方块实体存盘用的是 ValueInput/ValueOutput，所以这里手动拆成简单字段。
    public void save(ValueOutput output) {
        output.putString("Dimension", this.dimension.toString());
        output.putInt("X", this.pos.getX());
        output.putInt("Y", this.pos.getY());
        output.putInt("Z", this.pos.getZ());
    }

    // 读取旧存档或坏数据时返回 Optional.empty，避免绑定数据把方块实体读崩。
    public static Optional<BoundVoidPosition> load(ValueInput input) {
        Optional<String> dimension = input.getString("Dimension");
        if (dimension.isEmpty()) {
            return Optional.empty();
        }

        Identifier id = Identifier.tryParse(dimension.get());
        if (id == null) {
            return Optional.empty();
        }

        return Optional.of(new BoundVoidPosition(
                id,
                new BlockPos(
                        input.getIntOr("X", 0),
                        input.getIntOr("Y", 0),
                        input.getIntOr("Z", 0)
                )
        ));
    }

    public String shortText() {
        return this.dimension + " [" + this.pos.getX() + ", " + this.pos.getY() + ", " + this.pos.getZ() + "]";
    }
}
