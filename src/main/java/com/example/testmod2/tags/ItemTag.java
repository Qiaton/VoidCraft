package com.example.testmod2.tags;

import com.example.testmod2.TestMod2;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;


public class ItemTag {
    public static final TagKey<Item> BLACK_BLOCK = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(TestMod2.MODID, "black_block")
    );

}
