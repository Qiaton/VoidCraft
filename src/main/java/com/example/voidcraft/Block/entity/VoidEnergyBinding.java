package com.example.voidcraft.Block.entity;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Optional;

public record VoidEnergyBinding(BoundVoidPosition target, VoidEnergyBindingType type) {
    public boolean matches(BoundVoidPosition other) {
        return this.target.sameBlock(other);
    }

    public void save(ValueOutput output) {
        this.target.save(output);
        output.putString("Type", this.type.getId());
    }

    public static Optional<VoidEnergyBinding> load(ValueInput input) {
        return BoundVoidPosition.load(input).map(target -> new VoidEnergyBinding(
                target,
                VoidEnergyBindingType.byId(input.getStringOr("Type", VoidEnergyBindingType.OUTPUT.getId()))
        ));
    }
}
