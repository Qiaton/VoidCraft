package com.example.voidcraft.Item;

import com.example.voidcraft.Item.custom.FlowType;
import com.example.voidcraft.Item.custom.PhaseGauntlet;
import com.example.voidcraft.Item.custom.SpatialSword;
import com.example.voidcraft.ModToolMaterial;
import com.example.voidcraft.VoidCraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItem {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(VoidCraft.MODID);
    public static final DeferredItem<FlowType> FLOW_TYPE = ITEMS.registerItem(
            "flow_type",
            FlowType::new,
            props->props
                    .stacksTo(1)//最大堆叠
                    .durability(100)//耐久值
                    .rarity(Rarity.EPIC)//品质（游戏内物品的颜色）

    );
    public static final DeferredItem<Item> SPATIAL_SWORD = ITEMS.registerItem(
            "spatial_sword",
            SpatialSword::new,
            props-> props
            .stacksTo(1)
            .durability(100)
            .sword(ModToolMaterial.BLACK_BLOCK,5,-2.4F)//这是一把剑（材质，攻击加成，攻击速度 空手=4 所以这里-2.4F就是1.6）
    );
    public static final DeferredItem<Item> PHASE_GAUNTLET = ITEMS.registerItem(
            "phase_gauntlet",
            PhaseGauntlet::new,
            props-> props
            .stacksTo(1)
            .durability(100)
    );

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
