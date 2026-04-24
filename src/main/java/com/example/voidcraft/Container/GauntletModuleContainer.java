package com.example.voidcraft.Container;

import com.example.voidcraft.Item.custom.PhaseGauntlet;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import static com.example.voidcraft.Item.custom.PhaseGauntlet.GAUNTLET_MODULE_SLOT_COUNT;

public class GauntletModuleContainer extends SimpleContainer {
    private final ItemStack gauntletStack;

    public GauntletModuleContainer(ItemStack gauntletStack) {
        super(GAUNTLET_MODULE_SLOT_COUNT);
        this.gauntletStack = gauntletStack;

        ItemContainerContents contents = gauntletStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);

        NonNullList<ItemStack> items =
                NonNullList.withSize(GAUNTLET_MODULE_SLOT_COUNT, ItemStack.EMPTY);
        contents.copyInto(items);

        for (int i = 0; i < GAUNTLET_MODULE_SLOT_COUNT; i++) {
            this.setItem(i, items.get(i));
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();

        NonNullList<ItemStack> items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);

        // 获取当前槽位的数据
        for (int i = 0; i < GAUNTLET_MODULE_SLOT_COUNT; i++) {
            items.set(i, this.getItem(i).copy());
        }

        // 将修改后的内容写回到 ItemStack 中
        gauntletStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }
}