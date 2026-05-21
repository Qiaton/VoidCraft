package com.example.voidcraft.Custom.Behavior.Energy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Optional;

// 虚空能绑定保存的是“维度 + 方块坐标”，不能只存 BlockPos，否则跨维度会串线。
public record BoundVoidPosition(ResourceLocation dimension, BlockPos pos) {
    public static final Codec<BoundVoidPosition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("dimension").forGetter(BoundVoidPosition::dimension),
            BlockPos.CODEC.fieldOf("pos").forGetter(BoundVoidPosition::pos)
    ).apply(instance, BoundVoidPosition::new));

    public static BoundVoidPosition of(Level level, BlockPos pos) {
        return new BoundVoidPosition(level.dimension().location(), pos.immutable());
    }

    // 比较绑定点时只关心是不是同一个维度里的同一个方块。
    public boolean sameBlock(BoundVoidPosition other) {
        return other != null && this.dimension.equals(other.dimension) && this.pos.equals(other.pos);
    }

    // 方块实体存盘用简单 NBT 字段，跨版本迁移时更容易兼容旧数据。
    public void save(CompoundTag tag) {
        tag.putString("Dimension", this.dimension.toString());
        tag.putInt("X", this.pos.getX());
        tag.putInt("Y", this.pos.getY());
        tag.putInt("Z", this.pos.getZ());
    }

    // 读取旧存档或坏数据时返回 Optional.empty，避免绑定数据把方块实体读崩。
    public static Optional<BoundVoidPosition> load(CompoundTag tag) {
        if (!tag.contains("Dimension", Tag.TAG_STRING)) {
            return Optional.empty();
        }

        ResourceLocation id = ResourceLocation.tryParse(tag.getString("Dimension"));
        if (id == null) {
            return Optional.empty();
        }

        return Optional.of(new BoundVoidPosition(
                id,
                new BlockPos(
                        tag.getInt("X"),
                        tag.getInt("Y"),
                        tag.getInt("Z")
                )
        ));
    }

    public String shortText() {
        return this.dimension + " [" + this.pos.getX() + ", " + this.pos.getY() + ", " + this.pos.getZ() + "]";
    }
}
