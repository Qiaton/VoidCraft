package com.example.testmod2;

import com.example.testmod2.Block.ModBlockItem;
import com.example.testmod2.tags.ItemTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ToolMaterial;

public class ModToolMaterial {
    public static final ToolMaterial BLACK_BLOCK = new ToolMaterial(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL,//材质赋予的挖掘等级
            2778,//材质赋予的耐久
            10,//材质挖掘速度 石头=4 铁=6
            11,//材质赋予的攻击加成
            25,//材质的附魔值 越高越容易出货 铜=20 金=22
            ItemTag.BLACK_BLOCK//修复工具用的道具
    );

}
