package com.example.voidcraft.Block.entity;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Optional;

// 一条绑定只保存另一端的位置和绑定类型；真正能不能传电由两端方块实体再判断。
public record VoidEnergyBinding(BoundVoidPosition target, VoidEnergyBindingType type) {
    // 绑定去重只看目标方块位置，不看显示文字或当前状态。
    public boolean matches(BoundVoidPosition other) {
        return this.target.sameBlock(other);
    }

    // 绑定会跟着方块实体一起存盘，重进世界后还能恢复连接关系。
    public void save(ValueOutput output) {
        this.target.save(output);
        output.putString("Type", this.type.getId());
    }

    // 读不到位置时直接丢掉这条绑定，避免保留半坏的连接。
    public static Optional<VoidEnergyBinding> load(ValueInput input) {
        return BoundVoidPosition.load(input).map(target -> new VoidEnergyBinding(
                target,
                VoidEnergyBindingType.byId(input.getStringOr("Type", VoidEnergyBindingType.OUTPUT.getId()))
        ));
    }
}
