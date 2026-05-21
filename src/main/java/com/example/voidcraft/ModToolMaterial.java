package com.example.voidcraft;

import com.example.voidcraft.Tags.ItemTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

public class ModToolMaterial {
    public static final Tier BLACK_BLOCK = new SimpleTier(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL,//材质赋予的挖掘等级
            2778,//材质赋予的耐久
            10,//材质挖掘速度 石头=4 铁=6
            11,//材质赋予的攻击加成
            25,//材质的附魔值 越高越容易出货 铜=20 金=22
            () -> Ingredient.of(ItemTag.BLACK_BLOCK)//修复工具用的道具
    );

}
