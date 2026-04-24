package com.example.voidcraft.Item.custom.ModuleItem;


import com.example.voidcraft.Item.custom.PhaseGauntlet;
import com.example.voidcraft.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.neoforge.common.world.BiomeModifier;

import java.util.List;
import java.util.function.Consumer;

public class ModuleItem extends Item {

    public ModuleItem(Properties properties) {
        super(properties
                .stacksTo(1)
 
        );

    }
    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltipAdder,
            TooltipFlag flag
    ) {
        ModuleData data = stack.getOrDefault(
                ModDataComponents.MODULE_DATA.get(),
                new ModuleData(ModuleMode.BURST, 1,List.of())
        );

        tooltipAdder.accept(Component.translatable(
                "tooltip.void_craft.module.mode",
                data.moduleMode().getDisplayName()
        ));
        tooltipAdder.accept(Component.translatable(
                "tooltip.void_craft.module.level",
                data.level()
        ));
        for(ModuleModifierData modifierData : data.modifiers()){
            tooltipAdder.accept(Component.translatable(
                    "tooltip.void_craft.module.modifier_entry",
                    modifierData.type().getDisplayName(),
                    modifierData.level()
            ));
        }
    }
    public void useSkill(ServerPlayer player, ItemStack gauntletStack, ItemStack moduleStack, int slot) {
        if(gauntletStack.isEmpty()) return;
        System.out.println("技能阶段：空槽位检验完毕");
        if(player == null) return;
        System.out.println("技能阶段：空玩家检验完毕");
        if(!(gauntletStack.getItem() instanceof PhaseGauntlet)) return;
        System.out.println("技能阶段：手套类型检验完毕");
        if(!gauntletStack.has(DataComponents.CONTAINER)) return;
        System.out.println("技能阶段：是否有模块检验完毕");
        doUseSkill(player, gauntletStack, moduleStack, slot);
        System.out.println("技能阶段：子模块施放技能完成");
    }
    protected void doUseSkill(ServerPlayer player, ItemStack gauntletStack, ItemStack moduleStack, int slot) {

    }
}
