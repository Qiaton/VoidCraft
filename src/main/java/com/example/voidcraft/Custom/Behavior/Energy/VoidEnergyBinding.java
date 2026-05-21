package com.example.voidcraft.Custom.Behavior.Energy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.Optional;

// 一条绑定只保存另一端的位置和绑定类型；真正能不能传电由两端方块实体再判断。
public record VoidEnergyBinding(BoundVoidPosition target, VoidEnergyBindingType type) {
    // 绑定去重只看目标方块位置，不看显示文字或当前状态。
    public boolean matches(BoundVoidPosition other) {
        return this.target.sameBlock(other);
    }

    // 绑定会跟着方块实体一起存盘，重进世界后还能恢复连接关系。
    public void save(CompoundTag tag) {
        this.target.save(tag);
        tag.putString("Type", this.type.getId());
    }

    // 读不到位置时直接丢掉这条绑定，避免保留半坏的连接。
    public static Optional<VoidEnergyBinding> load(CompoundTag tag) {
        String typeId = tag.contains("Type", Tag.TAG_STRING)
                ? tag.getString("Type")
                : VoidEnergyBindingType.OUTPUT.getId();
        return BoundVoidPosition.load(tag).map(target -> new VoidEnergyBinding(
                target,
                VoidEnergyBindingType.byId(typeId)
        ));
    }
}
