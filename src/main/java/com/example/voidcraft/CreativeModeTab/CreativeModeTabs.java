package com.example.voidcraft.CreativeModeTab;

import com.example.voidcraft.Block.Block.BatteryBlock;
import com.example.voidcraft.Block.ModBlockEntities;
import com.example.voidcraft.Block.ModBlockItem;
import com.example.voidcraft.Item.ModItem;
import com.example.voidcraft.Item.custom.ModuleItem.*;
import com.example.voidcraft.ModDataComponents;
import com.example.voidcraft.VoidCraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public class CreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            VoidCraft.MODID
    );
    public static final DeferredHolder<CreativeModeTab,CreativeModeTab> VOID_CRAFT_TAB = TABS.register(
            "void_craft",()-> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.void_craft.create_tab"))
                    .icon(()-> new ItemStack(Items.IRON_AXE))
                    .displayItems(((parameters, output) -> {
                        output.accept(ModBlockItem.BLACK_BLOCK.get());
                        output.accept(ModBlockItem.VOID_ORE_BLOCK.get());
                        output.accept(ModBlockItem.BATTERY_BLOCK.get());
                        output.accept(getEmptyBatteryBlockItem());
                        output.accept(ModBlockItem.CHUNK_MAPPER_BLOCK.get());
                        output.accept(ModBlockItem.VOID_ENERGY_CONVERTER.get());
                        output.accept(ModBlockItem.VOID_PHENOMENON_COLLECTOR.get());
                        output.accept(ModBlockItem.IMPROVED_VOID_PHENOMENON_COLLECTOR.get());
                        output.accept(ModBlockItem.ADVANCED_VOID_PHENOMENON_COLLECTOR.get());
                        output.accept(ModBlockItem.VOID_ATTUNER.get());
                        output.accept(ModBlockItem.MODULE_BOOST_TABLE.get());
                        output.accept(ModBlockItem.LOW_VOID_CHARGER.get());
                        output.accept(ModBlockItem.MID_VOID_CHARGER.get());
                        output.accept(ModBlockItem.HIGH_VOID_CHARGER.get());
                        output.accept(ModItem.VOID_ORE.get());
                        output.accept(ModItem.FLOW_TYPE.get());
                        output.accept(ModItem.SPATIAL_SWORD);
                        output.accept(ModItem.CRUDE_PHASE_WATCH);
                        output.accept(ModItem.ATTUNED_PHASE_WATCH);
                        output.accept(ModItem.PHASE_WATCH);
                        output.accept(ModItem.RESONANT_PHASE_WATCH);
                        output.accept(ModItem.VOID_ENERGY_PHASE_WATCH);
                        output.accept(ModItem.BASIC_ENERGY_CORE);
                        output.accept(ModItem.ADVANCED_ENERGY_CORE);
                        output.accept(ModItem.ELITE_ENERGY_CORE);
                        output.accept(ModItem.MAX_ENERGY_CORE);
                        output.accept(ModItem.ENERGY_CORE_RESIDUE);
                        output.accept(ModItem.LOW_PURITY_VOID_CRYSTAL);
                        output.accept(ModItem.HIGH_PURITY_VOID_CRYSTAL);
                        output.accept(ModItem.PURE_VOID_CRYSTAL);
                        output.accept(ModItem.VOID_CRYSTAL_RESIDUE);
                        output.accept(ModItem.CHAOS_ENERGY);
                        output.accept(ModItem.NEUTRAL_ENERGY);
                        output.accept(ModItem.PURE_ENERGY);
                        output.accept(ModItem.VOID_ENERGY);
                        output.accept(ModItem.COORDINATE_DESIGNATOR);
                        output.accept(getModuleItem());
                        output.accept(getModuleModifierItem(ModuleModifierType.COOLDOWN_REDUCTION));
                        output.accept(getModuleModifierItem(ModuleModifierType.SPEED_BOOST));
                        output.accept(getModuleModifierItem(ModuleModifierType.ACTIVE_DURATION));
                        output.accept(getHealthModuleItem());
                        output.accept(getDashVoidModuleItem());
                        output.accept(getBlinkVoidModuleItem());
                        output.accept(getSafeBlinkVoidModuleItem());
                        output.accept(getPhaseTurretModuleItem());
                        output.accept(getAssistPhaseTurretModuleItem());
                        output.accept(getBlackHoleModuleItem());
                        output.accept(getWorldModuleItem());
                    }))
                    .build()

    );
    public static ItemStack getEmptyBatteryBlockItem(){
        ItemStack stack = new ItemStack(ModBlockItem.BATTERY_BLOCK.get());
        stack.set(
                DataComponents.BLOCK_STATE,
                BlockItemStateProperties.EMPTY.with(BatteryBlock.ENERGY_STAGE, 0)
        );
        CompoundTag tag = new CompoundTag();
        tag.putLong("VoidEnergy", 0L);
        BlockItem.setBlockEntityData(stack, ModBlockEntities.BATTERY_BLOCK_ENTITY.get(), tag);
        return stack;
    }
    public static ItemStack getModuleItem(){
        ItemStack stack = new ItemStack(ModItem.MODULE_ITEM.get());
        stack.set(ModDataComponents.MODULE_DATA.value(),new ModuleData(ModuleMode.BURST,5, List.of(new ModuleModifierData(ModuleModifierType.COOLDOWN_REDUCTION,5),new ModuleModifierData(ModuleModifierType.SPEED_BOOST,5),new ModuleModifierData(ModuleModifierType.ACTIVE_DURATION,5))));
        return stack;
    }
    public static ItemStack getHealthModuleItem(){
        ItemStack stack = new ItemStack(ModItem.HEALTH_VOID_MODULE.get());
        stack.set(ModDataComponents.MODULE_DATA.value(),new ModuleData(ModuleMode.CHANNEL,5, List.of(new ModuleModifierData(ModuleModifierType.COOLDOWN_REDUCTION,5),new ModuleModifierData(ModuleModifierType.SPEED_BOOST,5),new ModuleModifierData(ModuleModifierType.ACTIVE_DURATION,5))));
        return stack;
    }
    public static ItemStack getBlinkVoidModuleItem(){
        ItemStack stack = new ItemStack(ModItem.BLINK_VOID_MODULE.get());
        stack.set(ModDataComponents.MODULE_DATA.value(),new ModuleData(ModuleMode.BURST,5, List.of(new ModuleModifierData(ModuleModifierType.COOLDOWN_REDUCTION,5),new ModuleModifierData(ModuleModifierType.SPEED_BOOST,5),new ModuleModifierData(ModuleModifierType.ACTIVE_DURATION,5)))); // Blink 只支持 BURST，创造栏样品也要给 BURST
        return stack;
    }
    public static ItemStack getSafeBlinkVoidModuleItem(){
        ItemStack stack = new ItemStack(ModItem.SAFE_BLINK_VOID_MODULE.get());
        stack.set(ModDataComponents.MODULE_DATA.value(),new ModuleData(ModuleMode.BURST,5, List.of(new ModuleModifierData(ModuleModifierType.COOLDOWN_REDUCTION,5),new ModuleModifierData(ModuleModifierType.SPEED_BOOST,5),new ModuleModifierData(ModuleModifierType.ACTIVE_DURATION,5))));
        return stack;
    }
    public static ItemStack getPhaseTurretModuleItem(){
        ItemStack stack = new ItemStack(ModItem.PHASE_TURRET_MODULE.get());
        stack.set(ModDataComponents.MODULE_DATA.value(),new ModuleData(ModuleMode.BURST,5, List.of(new ModuleModifierData(ModuleModifierType.COOLDOWN_REDUCTION,5),new ModuleModifierData(ModuleModifierType.SPEED_BOOST,5),new ModuleModifierData(ModuleModifierType.ACTIVE_DURATION,5))));
        return stack;
    }
    public static ItemStack getAssistPhaseTurretModuleItem(){
        ItemStack stack = new ItemStack(ModItem.ASSIST_PHASE_TURRET_MODULE.get());
        stack.set(ModDataComponents.MODULE_DATA.value(),new ModuleData(ModuleMode.BURST,5, List.of(new ModuleModifierData(ModuleModifierType.COOLDOWN_REDUCTION,5),new ModuleModifierData(ModuleModifierType.SPEED_BOOST,5),new ModuleModifierData(ModuleModifierType.ACTIVE_DURATION,5))));
        return stack;
    }
    public static ItemStack getBlackHoleModuleItem(){
        ItemStack stack = new ItemStack(ModItem.BLACK_HOLE_MODULE.get());
        stack.set(ModDataComponents.MODULE_DATA.value(),new ModuleData(ModuleMode.BURST,5, List.of(new ModuleModifierData(ModuleModifierType.COOLDOWN_REDUCTION,5),new ModuleModifierData(ModuleModifierType.SPEED_BOOST,5),new ModuleModifierData(ModuleModifierType.ACTIVE_DURATION,5))));
        return stack;
    }
    public static ItemStack getWorldModuleItem(){
        ItemStack stack = new ItemStack(ModItem.WORLD_MODULE.get());
        stack.set(ModDataComponents.MODULE_DATA.value(),new ModuleData(ModuleMode.BURST,5, List.of(new ModuleModifierData(ModuleModifierType.COOLDOWN_REDUCTION,5),new ModuleModifierData(ModuleModifierType.SPEED_BOOST,5),new ModuleModifierData(ModuleModifierType.ACTIVE_DURATION,5))));
        return stack;
    }
    public static ItemStack getModuleModifierItem(ModuleModifierType type){
        ItemStack stack = new ItemStack(ModItem.MODULE_MODIFIER_ITEM.get());
        ModuleModifierItem.setData(stack, new ModuleModifierData(type,5));
        return stack;
    }
    public static ItemStack getDashVoidModuleItem(){
        ItemStack stack = new ItemStack(ModItem.DASH_VOID_MODULE.get());
        stack.set(ModDataComponents.MODULE_DATA.value(),new ModuleData(ModuleMode.CHANNEL,5, List.of(new ModuleModifierData(ModuleModifierType.SPEED_BOOST,5),new ModuleModifierData(ModuleModifierType.COOLDOWN_REDUCTION,5),new ModuleModifierData(ModuleModifierType.ACTIVE_DURATION,5))));
        return stack;
    }
    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
