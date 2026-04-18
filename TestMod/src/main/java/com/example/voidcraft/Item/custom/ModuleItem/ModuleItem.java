package com.example.voidcraft.Item.custom.ModuleItem;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ModuleItem extends Item {

    public ModuleItem(Properties properties) {
        super(properties
                .stacksTo(1)
        );
    }
    private enum ModuleType{
        CHANNEL,
        BURST,
        BLINK
    }
    public record ModuleData(ModuleType moduleType,
                             int level
                             ){

    }
}
