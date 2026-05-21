package com.example.voidcraft.Item;

import com.example.voidcraft.Item.custom.FlowType;
import com.example.voidcraft.Item.custom.CoordinateDesignatorItem;
import com.example.voidcraft.Item.custom.EnergyCoreItem;
import com.example.voidcraft.Item.custom.PhaseWatchTier;
import com.example.voidcraft.Item.custom.TieredPhaseWatch;
import com.example.voidcraft.Item.custom.VoidCrystalItem;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierItem;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.AssistPhaseTurretModule;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.BlinkVoidModule;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.DashVoidModule;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.HealthVoidModule;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.PhaseTurretModule;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.SafeBlinkVoidModule;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.VoidModule;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.WorldModule;
import com.example.voidcraft.Item.custom.SpatialSword;
import com.example.voidcraft.ModToolMaterial;
import com.example.voidcraft.VoidCraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItem {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(VoidCraft.MODID);
    public static final DeferredItem<FlowType> FLOW_TYPE = ITEMS.registerItem(
            "flow_type",
            FlowType::new,
            new Item.Properties()
                    .stacksTo(1)//最大堆叠
                    .durability(100)//耐久值
                    .rarity(Rarity.EPIC)//品质（游戏内物品的颜色）

    );
    public static final DeferredItem<Item> SPATIAL_SWORD = ITEMS.registerItem(
            "spatial_sword",
            SpatialSword::new,
            new Item.Properties()
            .stacksTo(1)
            .durability(100)
            .attributes(SwordItem.createAttributes(ModToolMaterial.BLACK_BLOCK,5,-2.4F))//这是一把剑（材质，攻击加成，攻击速度 空手=4 所以这里-2.4F就是1.6）
            .component(DataComponents.TOOL, SwordItem.createToolProperties())
    );
    public static final DeferredItem<Item> CRUDE_PHASE_WATCH = ITEMS.registerItem(
            "crude_phase_watch",
            props -> new TieredPhaseWatch(props, PhaseWatchTier.CRUDE),
            new Item.Properties()
                    .stacksTo(1)
                    .durability(100)
    );
    public static final DeferredItem<Item> ATTUNED_PHASE_WATCH = ITEMS.registerItem(
            "attuned_phase_watch",
            props -> new TieredPhaseWatch(props, PhaseWatchTier.ATTUNED),
            new Item.Properties()
                    .stacksTo(1)
                    .durability(100)
    );
    public static final DeferredItem<Item> PHASE_WATCH = ITEMS.registerItem(
            "phase_watch",
            props -> new TieredPhaseWatch(props, PhaseWatchTier.STABILIZED),
            new Item.Properties()
                    .stacksTo(1)
                    .durability(100)
    );
    public static final DeferredItem<Item> RESONANT_PHASE_WATCH = ITEMS.registerItem(
            "resonant_phase_watch",
            props -> new TieredPhaseWatch(props, PhaseWatchTier.RESONANT),
            new Item.Properties()
                    .stacksTo(1)
                    .durability(100)
    );
    public static final DeferredItem<Item> VOID_ENERGY_PHASE_WATCH = ITEMS.registerItem(
            "void_energy_phase_watch",
            props -> new TieredPhaseWatch(props, PhaseWatchTier.VOID_ENERGY),
            new Item.Properties()
                    .stacksTo(1)
                    .durability(100)
    );
    public static final DeferredItem<EnergyCoreItem> BASIC_ENERGY_CORE = ITEMS.registerItem(
            "basic_energy_core",
            props -> new EnergyCoreItem(props, EnergyCoreItem.CoreTier.BASIC),
            new Item.Properties().stacksTo(1)
    );
    public static final DeferredItem<EnergyCoreItem> ADVANCED_ENERGY_CORE = ITEMS.registerItem(
            "advanced_energy_core",
            props -> new EnergyCoreItem(props, EnergyCoreItem.CoreTier.PLUS),
            new Item.Properties().stacksTo(1)
    );
    public static final DeferredItem<EnergyCoreItem> ELITE_ENERGY_CORE = ITEMS.registerItem(
            "elite_energy_core",
            props -> new EnergyCoreItem(props, EnergyCoreItem.CoreTier.PRO),
            new Item.Properties().stacksTo(1)
    );
    public static final DeferredItem<EnergyCoreItem> MAX_ENERGY_CORE = ITEMS.registerItem(
            "max_energy_core",
            props -> new EnergyCoreItem(props, EnergyCoreItem.CoreTier.MAX),
            new Item.Properties().stacksTo(1)
    );
    public static final DeferredItem<Item> ENERGY_CORE_RESIDUE = ITEMS.registerItem(
            "energy_core_residue",
            Item::new,
            new Item.Properties()
    );
    public static final DeferredItem<VoidCrystalItem> LOW_PURITY_VOID_CRYSTAL = ITEMS.registerItem(
            "low_purity_void_crystal",
            props -> new VoidCrystalItem(props, VoidCrystalItem.CrystalTier.LOW_PURITY),
            new Item.Properties()
                    .stacksTo(1)
                    .durability(VoidCrystalItem.BASE_DURABILITY * VoidCrystalItem.CrystalTier.LOW_PURITY.durabilityMultiplier())
    );
    public static final DeferredItem<VoidCrystalItem> HIGH_PURITY_VOID_CRYSTAL = ITEMS.registerItem(
            "high_purity_void_crystal",
            props -> new VoidCrystalItem(props, VoidCrystalItem.CrystalTier.HIGH_PURITY),
            new Item.Properties()
                    .stacksTo(1)
                    .durability(VoidCrystalItem.BASE_DURABILITY * VoidCrystalItem.CrystalTier.HIGH_PURITY.durabilityMultiplier())
    );
    public static final DeferredItem<VoidCrystalItem> PURE_VOID_CRYSTAL = ITEMS.registerItem(
            "pure_void_crystal",
            props -> new VoidCrystalItem(props, VoidCrystalItem.CrystalTier.PURE),
            new Item.Properties()
                    .stacksTo(1)
                    .durability(VoidCrystalItem.BASE_DURABILITY * VoidCrystalItem.CrystalTier.PURE.durabilityMultiplier())
    );
    public static final DeferredItem<Item> VOID_CRYSTAL_RESIDUE = ITEMS.registerItem(
            "void_crystal_residue",
            // 虚空结晶耗尽后留下的废渣，目前只是普通物品。
            Item::new,
            new Item.Properties()
    );
    public static final DeferredItem<Item> CHAOS_ENERGY = ITEMS.registerItem(
            "chaos_energy",
            Item::new,
            new Item.Properties()
    );
    public static final DeferredItem<Item> NEUTRAL_ENERGY = ITEMS.registerItem(
            "neutral_energy",
            Item::new,
            new Item.Properties()
    );
    public static final DeferredItem<Item> PURE_ENERGY = ITEMS.registerItem(
            "pure_energy",
            Item::new,
            new Item.Properties()
    );
    public static final DeferredItem<Item> VOID_ENERGY = ITEMS.registerItem(
            "void_energy",
            Item::new,
            new Item.Properties()
    );
    public static final DeferredItem<CoordinateDesignatorItem> COORDINATE_DESIGNATOR = ITEMS.registerItem(
            "coordinate_designator",
            CoordinateDesignatorItem::new,
            new Item.Properties().stacksTo(1)
    );
    public static final DeferredItem<Item> VOID_ORE = ITEMS.registerItem(
            "void_ore",
            Item::new,
            new Item.Properties()
    );
    public static final DeferredItem<Item> MODULE_ITEM = ITEMS.registerItem(
            "void_module_item",
            VoidModule::new,
            new Item.Properties()

    );
    public static final DeferredItem<Item> MODULE_MODIFIER_ITEM = ITEMS.registerItem(
            "module_modifier_item",
            ModuleModifierItem::new,
            new Item.Properties()

    );
    public static final DeferredItem<Item> HEALTH_VOID_MODULE = ITEMS.registerItem(
            "health_void_module_item",
            HealthVoidModule::new,
            new Item.Properties()

    );
    public static final DeferredItem<Item> DASH_VOID_MODULE = ITEMS.registerItem(
            "dash_void_module_item",
            DashVoidModule::new,
            new Item.Properties()

    );
    public static final DeferredItem<Item> BLINK_VOID_MODULE = ITEMS.registerItem(
            "blink_void_module_item",
            BlinkVoidModule::new,
            new Item.Properties()

    );
    public static final DeferredItem<Item> SAFE_BLINK_VOID_MODULE = ITEMS.registerItem(
            "safe_blink_void_module_item",
            SafeBlinkVoidModule::new,
            new Item.Properties()

    );
    public static final DeferredItem<Item> PHASE_TURRET_MODULE = ITEMS.registerItem(
            "phase_turret_module_item",
            PhaseTurretModule::new,
            new Item.Properties()

    );
    public static final DeferredItem<Item> ASSIST_PHASE_TURRET_MODULE = ITEMS.registerItem(
            "assist_phase_turret_module_item",
            AssistPhaseTurretModule::new,
            new Item.Properties()

    );
    public static final DeferredItem<Item> WORLD_MODULE = ITEMS.registerItem(
            "world_module_item",
            WorldModule::new,
            new Item.Properties()

    );


    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
