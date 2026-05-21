package com.example.voidcraft.World;

import com.example.voidcraft.Custom.Clock.Clock;
import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.Sound.ModSound;
import com.example.voidcraft.Network.ModNetworking;
import com.example.voidcraft.World.projection.PhaseProjectionSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GoWorld {
    private static final int TELEPORT_FALLBACK_DELAY_TICKS = 40;
    private static final int PROJECTION_CHUNK_DELAY_TICKS = 2;
    private static final Map<UUID, PendingTransition> PENDING_TRANSITIONS = new ConcurrentHashMap<>();

    private GoWorld() {
    }

    public static boolean canGo(ServerPlayer player) {
        // 给模块扣能量前做一次轻量检查，避免明显不能传送时先消耗能量。
        return player != null && canStartMove(player, getTargetWorldLevel(player));
    }

    public static boolean goWorld(ServerPlayer player) {
        if (player == null) {
            return false;
        }

        ServerLevel targetLevel = getTargetWorldLevel(player);
        if (!canStartMove(player, targetLevel)) {
            return false;
        }

        if (PhaseDimensions.isPhaseMirror(targetLevel) && player.level() instanceof ServerLevel sourceLevel) {
            sendPhaseProjectionChunks(player, sourceLevel, player.blockPosition());
        }

        return startMove(player, targetLevel);
    }

    private static void sendPhaseProjectionChunks(ServerPlayer player, ServerLevel sourceLevel, BlockPos center) {
        int centerChunkX = SectionPos.blockToSectionCoord(center.getX());
        int centerChunkZ = SectionPos.blockToSectionCoord(center.getZ());
        int delay = 0;

        // 中心 chunk 先发，玩家进相位后能最快看到脚下环境；外围 chunk 之后分 tick 补齐。
        for (int radius = 0; radius <= PhaseProjectionSnapshot.CHUNK_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }

                    int chunkX = centerChunkX + dx;
                    int chunkZ = centerChunkZ + dz;
                    int sendDelay = delay;
                    Clock.addClock(sendDelay, () -> sendPhaseProjectionChunk(player, sourceLevel, center, chunkX, chunkZ));
                    delay += PROJECTION_CHUNK_DELAY_TICKS;
                }
            }
        }
    }

    private static void sendPhaseProjectionChunk(
            ServerPlayer player,
            ServerLevel sourceLevel,
            BlockPos center,
            int chunkX,
            int chunkZ
    ) {
        if (player.isRemoved() || player.isDeadOrDying()) {
            return;
        }

        ModNetworking.sendPhaseProjection(player, PhaseProjectionSnapshot.makeChunk(sourceLevel, center, chunkX, chunkZ));
    }

    public static void finishMove(ServerPlayer player) {
        PendingTransition pending = PENDING_TRANSITIONS.remove(player.getUUID());
        if (pending == null) {
            return;
        }

        moveWorld(player, pending.sourceDimension());
    }

    private static boolean startMove(ServerPlayer player, ServerLevel targetLevel) {
        if (!canStartMove(player, targetLevel)) {
            return false;
        }

        if (player.isPassenger()) {
            player.stopRiding();
        }
        ResourceKey<Level> sourceDimension = player.level().dimension();
        ResourceKey<Level> targetDimension = targetLevel.dimension();
        PENDING_TRANSITIONS.put(player.getUUID(), new PendingTransition(sourceDimension));
        ModNetworking.sendPhaseWorldTransition(player, sourceDimension, targetDimension);
        Clock.addClock(
                TELEPORT_FALLBACK_DELAY_TICKS,
                () -> finishMove(player)
        );
        ModNetworking.sendPhaseTear(player, VoidRingInstance.Preset.DEFAULT);
        return true;
    }

    private static boolean canStartMove(ServerPlayer player, ServerLevel targetLevel) {
        return targetLevel != null
                && !player.isRemoved()
                && !player.isDeadOrDying()
                && !PENDING_TRANSITIONS.containsKey(player.getUUID());
    }

    private static void moveWorld(ServerPlayer player, ResourceKey<Level> sourceDimension) {
        if (player.isRemoved() || player.isDeadOrDying() || player.level().dimension() != sourceDimension) {
            return;
        }

        ServerLevel targetLevel = getTargetWorldLevel(player);
        if (targetLevel == null) {
            return;
        }

        if (player.isPassenger()) {
            player.stopRiding();
        }

        Vec3 targetPos = PhaseWorldRules.findArrivalPos(targetLevel, player.position());
        Vec3 motion = player.getDeltaMovement();
        boolean teleported = player.teleportTo(
                targetLevel,
                targetPos.x,
                targetPos.y,
                targetPos.z,
                Set.<RelativeMovement>of(),
                player.getYRot(),
                player.getXRot()
        );

        if (!teleported) {
            return;
        }

        player.setDeltaMovement(motion);
        ModSound.playEnterVoid(player.level(), player);
        ModNetworking.sendPhaseTear(player, VoidRingInstance.Preset.DEFAULT);
    }

    private static ServerLevel getTargetWorldLevel(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return null;
        }

        ResourceKey<Level> targetDimension = PhaseWorldRules.getTargetWorld(player.level().dimension());
        if (targetDimension == null) {
            return null;
        }

        return server.getLevel(targetDimension);
    }

    private record PendingTransition(ResourceKey<Level> sourceDimension) {
    }
}
