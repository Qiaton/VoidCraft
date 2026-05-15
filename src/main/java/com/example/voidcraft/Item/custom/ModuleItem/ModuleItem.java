package com.example.voidcraft.Item.custom.ModuleItem;


import com.example.voidcraft.ClientCustom.ModuleInputMode;
import com.example.voidcraft.Item.custom.PhaseWatch;
import com.example.voidcraft.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

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
            List<Component> tooltipAdder,
            TooltipFlag flag
    ) {
        ModuleData data = stack.getOrDefault(
                ModDataComponents.MODULE_DATA.get(),
                new ModuleData(ModuleMode.BURST, 1,List.of())
        );

        tooltipAdder.add(Component.translatable(
                "tooltip.void_craft.module.mode",
                data.moduleMode().getDisplayName()
        ));
        tooltipAdder.add(Component.translatable(
                "tooltip.void_craft.module.level",
                data.level()
        ));
        for(ModuleModifierData modifierData : data.modifiers()){
            tooltipAdder.add(Component.translatable(
                    "tooltip.void_craft.module.modifier_entry",
                    getModifierDisplayName(data, modifierData),
                    modifierData.level()
            ));
        }
    }

    protected Component getModifierDisplayName(ModuleData data, ModuleModifierData modifierData) {
        return getModifierDisplayName(modifierData);
    }

    protected Component getModifierDisplayName(ModuleModifierData modifierData) {
        return modifierData.type().getDisplayName();
    }

    public void useSkill(ServerPlayer player, ItemStack watchStack, ItemStack moduleStack, int slot) {
        if(watchStack.isEmpty()) return;
        if(player == null) return;
        if(!(watchStack.getItem() instanceof PhaseWatch)) return;
        if(!watchStack.has(DataComponents.CONTAINER)) return;
        doUseSkill(player, watchStack, moduleStack, slot);
    }
    protected void doUseSkill(ServerPlayer player, ItemStack watchStack, ItemStack moduleStack, int slot) {

    }
    public ModuleInputMode getInputMode() {
        return ModuleInputMode.CLICK;
    }
    public boolean canUseMode(ModuleMode mode) {
        return true;
    }
}
