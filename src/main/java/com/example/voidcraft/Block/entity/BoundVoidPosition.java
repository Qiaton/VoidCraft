package com.example.voidcraft.Block.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Optional;

public record BoundVoidPosition(Identifier dimension, BlockPos pos) {
    public static final Codec<BoundVoidPosition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("dimension").forGetter(BoundVoidPosition::dimension),
            BlockPos.CODEC.fieldOf("pos").forGetter(BoundVoidPosition::pos)
    ).apply(instance, BoundVoidPosition::new));

    public static BoundVoidPosition of(Level level, BlockPos pos) {
        return new BoundVoidPosition(level.dimension().identifier(), pos.immutable());
    }

    public boolean sameBlock(BoundVoidPosition other) {
        return other != null && this.dimension.equals(other.dimension) && this.pos.equals(other.pos);
    }

    public void save(ValueOutput output) {
        output.putString("Dimension", this.dimension.toString());
        output.putInt("X", this.pos.getX());
        output.putInt("Y", this.pos.getY());
        output.putInt("Z", this.pos.getZ());
    }

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
