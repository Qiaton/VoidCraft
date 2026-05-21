package com.example.voidcraft.Tags;

import com.example.voidcraft.VoidCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;


public class ItemTag {
    public static final TagKey<Item> BLACK_BLOCK = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(VoidCraft.MODID, "black_block")
    );

}
