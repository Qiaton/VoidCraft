package com.example.voidcraft.tags;

import com.example.voidcraft.VoidCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;


public class ItemTag {
    public static final TagKey<Item> BLACK_BLOCK = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(VoidCraft.MODID, "black_block")
    );

}
