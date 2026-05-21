package com.example.voidcraft.loot;

import com.example.voidcraft.Item.ModItem;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleData;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleMode;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierData;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierItem;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierType;
import com.example.voidcraft.ModDataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.item.component.CustomModelData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ModLootTables {
    private static final Set<ResourceLocation> COMMON_CHESTS = Set.of(
            mc("chests/village/village_weaponsmith"),
            mc("chests/village/village_toolsmith"),
            mc("chests/village/village_armorer"),
            mc("chests/ruined_portal")
    );

    private static final Set<ResourceLocation> MID_CHESTS = Set.of(
            mc("chests/simple_dungeon"),
            mc("chests/abandoned_mineshaft"),
            mc("chests/shipwreck_map"),
            mc("chests/shipwreck_supply"),
            mc("chests/shipwreck_treasure"),
            mc("chests/desert_pyramid"),
            mc("chests/jungle_temple")
    );

    private static final Set<ResourceLocation> HIGH_CHESTS = Set.of(
            mc("chests/ancient_city"),
            mc("chests/end_city_treasure"),
            mc("chests/bastion_treasure")
    );

    private static final ResourceLocation FISHING_TREASURE = mc("gameplay/fishing/treasure");

    private static final WeightedInt[] COMMON_MODIFIER_LEVELS = {
            new WeightedInt(1, 1)
    };

    private static final WeightedInt[] MID_MODULE_LEVELS = {
            new WeightedInt(1, 75),
            new WeightedInt(2, 25),
            new WeightedInt(4, 1)
    };

    private static final WeightedInt[] MID_MODULE_MODIFIER_LEVELS = {
            new WeightedInt(1, 80),
            new WeightedInt(2, 20),
            new WeightedInt(5, 1)
    };

    private static final WeightedInt[] MID_MODIFIER_LEVELS = {
            new WeightedInt(2, 70),
            new WeightedInt(3, 25),
            new WeightedInt(4, 5)
    };

    private static final WeightedInt[] HIGH_MODULE_LEVELS = {
            new WeightedInt(2, 55),
            new WeightedInt(3, 35),
            new WeightedInt(4, 10),
            new WeightedInt(5, 1)
    };

    private static final WeightedInt[] HIGH_MODULE_MODIFIER_LEVELS = {
            new WeightedInt(1, 75),
            new WeightedInt(2, 20),
            new WeightedInt(3, 5)
    };

    private static final WeightedInt[] HIGH_MODIFIER_LEVELS = {
            new WeightedInt(3, 65),
            new WeightedInt(4, 30),
            new WeightedInt(5, 5),
            new WeightedInt(6, 1)
    };

    private static final WeightedInt[] FISHING_MODIFIER_LEVELS = {
            new WeightedInt(1, 550),
            new WeightedInt(2, 250),
            new WeightedInt(3, 150),
            new WeightedInt(4, 35),
            new WeightedInt(5, 5),
            new WeightedInt(6, 4),
            new WeightedInt(7, 3),
            new WeightedInt(8, 2),
            new WeightedInt(9, 1)
    };

    private static final WeightedInt[] FISHING_MODULE_LEVELS = {
            new WeightedInt(1, 55),
            new WeightedInt(2, 35),
            new WeightedInt(3, 5),
            new WeightedInt(4, 4),
            new WeightedInt(5, 1)
    };

    private static final WeightedInt[] FISHING_MODULE_MODIFIER_LEVELS = {
            new WeightedInt(1, 85),
            new WeightedInt(2, 14),
            new WeightedInt(3, 1)
    };

    private static final WeightedModifierType[] STRUCTURE_MODIFIER_TYPES = {
            new WeightedModifierType(ModuleModifierType.COOLDOWN_REDUCTION, 45),
            new WeightedModifierType(ModuleModifierType.SPEED_BOOST, 35),
            new WeightedModifierType(ModuleModifierType.ACTIVE_DURATION, 20)
    };

    private static final WeightedModifierType[] FISHING_MODIFIER_TYPES = {
            new WeightedModifierType(ModuleModifierType.COOLDOWN_REDUCTION, 30),
            new WeightedModifierType(ModuleModifierType.SPEED_BOOST, 30),
            new WeightedModifierType(ModuleModifierType.ACTIVE_DURATION, 30)
    };

    private ModLootTables() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ModLootTables::onLootTableLoad);
    }

    private static void onLootTableLoad(LootTableLoadEvent event) {
        ResourceLocation name = event.getName();
        LootTable table = event.getTable();

        if (COMMON_CHESTS.contains(name)) {
            addPool(table, modifierPool("void_craft_common_modifier", 0.15F, STRUCTURE_MODIFIER_TYPES, COMMON_MODIFIER_LEVELS));
        }

        if (MID_CHESTS.contains(name)) {
            addPool(table, midModulePool());
            addPool(table, modifierPool("void_craft_mid_modifier", 0.30F, STRUCTURE_MODIFIER_TYPES, MID_MODIFIER_LEVELS));
        }

        if (HIGH_CHESTS.contains(name)) {
            addPool(table, highModulePool());
            addPool(table, modifierPool("void_craft_high_modifier", 0.50F, STRUCTURE_MODIFIER_TYPES, HIGH_MODIFIER_LEVELS));
            addPool(table, modifierPool("void_craft_high_modifier_bonus", 0.10F, STRUCTURE_MODIFIER_TYPES, HIGH_MODIFIER_LEVELS));
        }

        if (FISHING_TREASURE.equals(name)) {
            addPool(table, modifierPool("void_craft_fishing_modifier", 0.10F, FISHING_MODIFIER_TYPES, FISHING_MODIFIER_LEVELS));
            addPool(table, fishingModulePool());
        }
    }

    private static LootPool midModulePool() {
        LootTable noModifier = moduleTable(MID_MODULE_LEVELS, List.of(new WeightedModifierSet(List.of(), 1)));
        LootTable oneModifier = moduleTable(
                MID_MODULE_LEVELS,
                oneModifierSets(STRUCTURE_MODIFIER_TYPES, MID_MODULE_MODIFIER_LEVELS)
        );

        return LootPool.lootPool()
                .name("void_craft_mid_void_module")
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemRandomChanceCondition.randomChance(0.25F))
                .add(NestedLootTable.inlineLootTable(noModifier).setWeight(70))
                .add(NestedLootTable.inlineLootTable(oneModifier).setWeight(30))
                .build();
    }

    private static LootPool highModulePool() {
        LootTable oneModifier = moduleTable(
                HIGH_MODULE_LEVELS,
                oneModifierSets(STRUCTURE_MODIFIER_TYPES, HIGH_MODULE_MODIFIER_LEVELS)
        );
        LootTable twoModifiers = moduleTable(
                HIGH_MODULE_LEVELS,
                twoModifierSets(STRUCTURE_MODIFIER_TYPES, HIGH_MODULE_MODIFIER_LEVELS)
        );
        LootTable threeModifiers = moduleTable(
                HIGH_MODULE_LEVELS,
                threeModifierSets(STRUCTURE_MODIFIER_TYPES, HIGH_MODULE_MODIFIER_LEVELS)
        );

        return LootPool.lootPool()
                .name("void_craft_high_void_module")
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemRandomChanceCondition.randomChance(0.40F))
                .add(NestedLootTable.inlineLootTable(oneModifier).setWeight(95))
                .add(NestedLootTable.inlineLootTable(twoModifiers).setWeight(4))
                .add(NestedLootTable.inlineLootTable(threeModifiers).setWeight(1))
                .build();
    }

    private static LootPool fishingModulePool() {
        LootTable noModifier = moduleTable(FISHING_MODULE_LEVELS, List.of(new WeightedModifierSet(List.of(), 1)));
        LootTable oneModifier = moduleTable(
                FISHING_MODULE_LEVELS,
                oneModifierSets(FISHING_MODIFIER_TYPES, FISHING_MODULE_MODIFIER_LEVELS)
        );

        return LootPool.lootPool()
                .name("void_craft_fishing_void_module")
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemRandomChanceCondition.randomChance(0.02F))
                .add(NestedLootTable.inlineLootTable(noModifier).setWeight(85))
                .add(NestedLootTable.inlineLootTable(oneModifier).setWeight(15))
                .build();
    }

    private static LootPool modifierPool(
            String name,
            float chance,
            WeightedModifierType[] modifierTypes,
            WeightedInt[] modifierLevels
    ) {
        LootPool.Builder pool = LootPool.lootPool()
                .name(name)
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemRandomChanceCondition.randomChance(chance));

        for (WeightedModifierType modifierType : modifierTypes) {
            for (WeightedInt level : modifierLevels) {
                pool.add(modifierEntry(modifierType.type(), level.value(), modifierType.weight() * level.weight()));
            }
        }

        return pool.build();
    }

    private static LootTable moduleTable(WeightedInt[] moduleLevels, List<WeightedModifierSet> modifierSets) {
        LootPool.Builder pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F));

        for (ModuleMode mode : ModuleMode.values()) {
            for (WeightedInt moduleLevel : moduleLevels) {
                for (WeightedModifierSet modifierSet : modifierSets) {
                    pool.add(moduleEntry(
                            mode,
                            moduleLevel.value(),
                            modifierSet.modifiers(),
                            moduleLevel.weight() * modifierSet.weight()
                    ));
                }
            }
        }

        return LootTable.lootTable()
                .withPool(pool)
                .build();
    }

    private static List<WeightedModifierSet> oneModifierSets(
            WeightedModifierType[] modifierTypes,
            WeightedInt[] modifierLevels
    ) {
        List<WeightedModifierSet> sets = new ArrayList<>();
        for (WeightedModifierType modifierType : modifierTypes) {
            for (WeightedInt level : modifierLevels) {
                sets.add(new WeightedModifierSet(
                        List.of(new ModuleModifierData(modifierType.type(), level.value())),
                        modifierType.weight() * level.weight()
                ));
            }
        }
        return sets;
    }

    private static List<WeightedModifierSet> twoModifierSets(
            WeightedModifierType[] modifierTypes,
            WeightedInt[] modifierLevels
    ) {
        List<WeightedModifierSet> sets = new ArrayList<>();
        for (int first = 0; first < modifierTypes.length; first++) {
            for (int second = first + 1; second < modifierTypes.length; second++) {
                WeightedModifierType firstType = modifierTypes[first];
                WeightedModifierType secondType = modifierTypes[second];
                for (WeightedInt firstLevel : modifierLevels) {
                    for (WeightedInt secondLevel : modifierLevels) {
                        sets.add(new WeightedModifierSet(
                                List.of(
                                        new ModuleModifierData(firstType.type(), firstLevel.value()),
                                        new ModuleModifierData(secondType.type(), secondLevel.value())
                                ),
                                firstType.weight() * secondType.weight() * firstLevel.weight() * secondLevel.weight()
                        ));
                    }
                }
            }
        }
        return sets;
    }

    private static List<WeightedModifierSet> threeModifierSets(
            WeightedModifierType[] modifierTypes,
            WeightedInt[] modifierLevels
    ) {
        List<WeightedModifierSet> sets = new ArrayList<>();
        for (int first = 0; first < modifierTypes.length; first++) {
            for (int second = first + 1; second < modifierTypes.length; second++) {
                for (int third = second + 1; third < modifierTypes.length; third++) {
                    WeightedModifierType firstType = modifierTypes[first];
                    WeightedModifierType secondType = modifierTypes[second];
                    WeightedModifierType thirdType = modifierTypes[third];
                    for (WeightedInt firstLevel : modifierLevels) {
                        for (WeightedInt secondLevel : modifierLevels) {
                            for (WeightedInt thirdLevel : modifierLevels) {
                                sets.add(new WeightedModifierSet(
                                        List.of(
                                                new ModuleModifierData(firstType.type(), firstLevel.value()),
                                                new ModuleModifierData(secondType.type(), secondLevel.value()),
                                                new ModuleModifierData(thirdType.type(), thirdLevel.value())
                                        ),
                                        firstLevel.weight() * secondLevel.weight() * thirdLevel.weight()
                                ));
                            }
                        }
                    }
                }
            }
        }
        return sets;
    }

    private static LootItem.Builder<?> modifierEntry(ModuleModifierType type, int level, int weight) {
        return LootItem.lootTableItem(ModItem.MODULE_MODIFIER_ITEM.get())
                .setWeight(weight)
                .apply(SetComponentsFunction.setComponent(
                        ModDataComponents.MODULE_MODIFIER_DATA.get(),
                        new ModuleModifierData(type, level)
                ))
                .apply(SetComponentsFunction.setComponent(
                        DataComponents.CUSTOM_MODEL_DATA,
                        new CustomModelData(type.ordinal() + 1)
                ));
    }

    private static LootItem.Builder<?> moduleEntry(
            ModuleMode mode,
            int level,
            List<ModuleModifierData> modifiers,
            int weight
    ) {
        return LootItem.lootTableItem(ModItem.MODULE_ITEM.get())
                .setWeight(weight)
                .apply(SetComponentsFunction.setComponent(
                        ModDataComponents.MODULE_DATA.get(),
                        new ModuleData(mode, level, List.copyOf(modifiers))
                ));
    }

    private static void addPool(LootTable table, LootPool pool) {
        if (table.getPool(pool.getName()) == null) {
            table.addPool(pool);
        }
    }

    private static ResourceLocation mc(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }

    private record WeightedInt(int value, int weight) {
    }

    private record WeightedModifierType(ModuleModifierType type, int weight) {
    }

    private record WeightedModifierSet(List<ModuleModifierData> modifiers, int weight) {
    }
}
