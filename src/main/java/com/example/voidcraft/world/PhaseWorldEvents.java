package com.example.voidcraft.world;

import com.example.voidcraft.VoidCraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

@EventBusSubscriber(modid = VoidCraft.MODID)
public final class PhaseWorldEvents {
    private PhaseWorldEvents() {
    }

    @SubscribeEvent
    public static void onSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        if (PhaseWorldRules.shouldBlockMobSpawn(event.getLevel().getLevel())) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
        }
    }

    @SubscribeEvent
    public static void onSpawnPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (PhaseWorldRules.shouldBlockMobSpawn(event.getLevel().getLevel())) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!PhaseWorldRules.shouldClearGeneratedLootContainers(event.getLevel())) {
            return;
        }

        if (event.getEntity() instanceof Mob) {
            event.setCanceled(true);
            return;
        }

        clearGeneratedLootContainer(event.getEntity());
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel) || !PhaseWorldRules.shouldClearGeneratedLootContainers(serverLevel)) {
            return;
        }

        clearGeneratedLootContainers(event.getChunk());
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide() || !PhaseWorldRules.shouldClearGeneratedLootContainers(level)) {
            return;
        }

        clearGeneratedLootContainer(level.getBlockEntity(event.getPos()));
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        clearGeneratedLootContainerOnInteract(event.getLevel(), event.getTarget());
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        clearGeneratedLootContainerOnInteract(event.getLevel(), event.getTarget());
    }

    private static void clearGeneratedLootContainers(LevelChunk chunk) {
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            clearGeneratedLootContainer(blockEntity);
        }
    }

    private static void clearGeneratedLootContainer(BlockEntity blockEntity) {
        if (!(blockEntity instanceof RandomizableContainer container) || container.getLootTable() == null) {
            return;
        }

        container.setLootTable(null);
        container.setLootTableSeed(0L);
        container.clearContent();
        blockEntity.setChanged();
    }

    private static void clearGeneratedLootContainerOnInteract(Level level, Entity entity) {
        if (level.isClientSide() || !PhaseWorldRules.shouldClearGeneratedLootContainers(level)) {
            return;
        }

        clearGeneratedLootContainer(entity);
    }

    private static void clearGeneratedLootContainer(Entity entity) {
        if (!(entity instanceof ContainerEntity container) || container.getContainerLootTable() == null) {
            return;
        }

        container.setContainerLootTable(null);
        container.setContainerLootTableSeed(0L);
        container.clearItemStacks();
        container.setChanged();
    }
}
