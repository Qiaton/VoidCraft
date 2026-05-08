package com.example.voidcraft.ClientCustom.Generator;

import com.example.voidcraft.Block.VoidPhenomenonCollectorBlock;
import com.example.voidcraft.Block.entity.VoidPhenomenonCollectorBlockEntity;
import com.example.voidcraft.Effect.VoidBlackHoleInstance;
import com.example.voidcraft.Effect.VoidBlackHoleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class VoidPhenomenonCollectorBlackHoleClient {
    private static final double RENDER_RADIUS = 96.0D;
    private static final int REFRESH_INTERVAL_TICKS = 2;
    private static final int MISSING_GRACE_SCANS = 4;
    private static final String BLACK_HOLE_ID_PREFIX = "void_phenomenon_collector:";
    private static final VoidBlackHoleInstance.Config WORKING_BLACK_HOLE = VoidBlackHoleInstance.Config.DEFAULT.toBuilder()
            .durationTicks(40)
            .centerYOffset(0.0F)
            .startHalfHeight(1.0F)
            .peakHalfHeight(1.0F)
            .endHalfHeight(1.0F)
            .startHalfWidth(1.0F)
            .peakHalfWidth(1.0F)
            .endHalfWidth(1.0F)
            .peakHoldTicks(39)
            .coreAlpha(0.0F)
            .rimAlpha(0.0F)
            .diskAlpha(0.0F)
            .coreAlphaScale(0.0F)
            .rimAlphaScale(0.0F)
            .shaderRimAlphaScale(0.0F)
            .horizonAlphaScale(0.0F)
            .centerShadowScale(0.0F)
            .diskInnerRadius(0.66F)
            .diskOuterRadius(1.48F)
            .distortionAlpha(1.80F)
            .occludedByBlocks(false)
            .distortionThickness(5.20F)
            .distortionAmplitude(9.60F)
            .distortionWidthScale(1.0F)
            .distortionHeightScale(1.0F)
            .noiseFrequency(5.20F)
            .noiseScrollSpeed(3.40F)
            .build();

    private static final Set<String> activeBlackHoleIds = new HashSet<>();
    private static final Map<String, Integer> missingScanCounts = new HashMap<>();
    private static int refreshTicks;

    private VoidPhenomenonCollectorBlackHoleClient() {
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            clearActiveBlackHoles();
            refreshTicks = 0;
            return;
        }

        if (refreshTicks > 0) {
            refreshTicks--;
            return;
        }
        refreshTicks = REFRESH_INTERVAL_TICKS;

        refreshLoadedCollectors(mc.level, mc.player);
    }

    private static void refreshLoadedCollectors(ClientLevel level, Player player) {
        BlockPos playerPos = player.blockPosition();
        int minChunkX = (playerPos.getX() - (int) RENDER_RADIUS) >> 4;
        int maxChunkX = (playerPos.getX() + (int) RENDER_RADIUS) >> 4;
        int minChunkZ = (playerPos.getZ() - (int) RENDER_RADIUS) >> 4;
        int maxChunkZ = (playerPos.getZ() + (int) RENDER_RADIUS) >> 4;
        double radiusSqr = RENDER_RADIUS * RENDER_RADIUS;
        Set<String> stillActive = new HashSet<>();

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof VoidPhenomenonCollectorBlockEntity)) {
                        continue;
                    }
                    if (Vec3.atCenterOf(blockEntity.getBlockPos()).distanceToSqr(player.position()) > radiusSqr) {
                        continue;
                    }

                    BlockState state = blockEntity.getBlockState();
                    if (!state.hasProperty(VoidPhenomenonCollectorBlock.ACTIVE)
                            || !state.getValue(VoidPhenomenonCollectorBlock.ACTIVE)) {
                        continue;
                    }

                    String blackHoleId = blackHoleId(level, blockEntity.getBlockPos());
                    stillActive.add(blackHoleId);
                    missingScanCounts.remove(blackHoleId);
                    VoidBlackHoleManager.updatePersistentBlackHole(
                            blackHoleId,
                            Vec3.atCenterOf(blockEntity.getBlockPos()),
                            1.0F,
                            WORKING_BLACK_HOLE
                    );
                }
            }
        }

        for (String blackHoleId : Set.copyOf(activeBlackHoleIds)) {
            if (!stillActive.contains(blackHoleId)) {
                // 客户端区块/方块状态同步可能短暂漏一帧，延迟移除可避免持续黑洞闪断。
                int missedScans = missingScanCounts.getOrDefault(blackHoleId, 0) + 1;
                if (missedScans >= MISSING_GRACE_SCANS) {
                    VoidBlackHoleManager.removePersistentBlackHole(blackHoleId);
                    activeBlackHoleIds.remove(blackHoleId);
                    missingScanCounts.remove(blackHoleId);
                } else {
                    missingScanCounts.put(blackHoleId, missedScans);
                }
            }
        }
        activeBlackHoleIds.addAll(stillActive);
    }

    private static void clearActiveBlackHoles() {
        for (String blackHoleId : activeBlackHoleIds) {
            VoidBlackHoleManager.removePersistentBlackHole(blackHoleId);
        }
        activeBlackHoleIds.clear();
        missingScanCounts.clear();
    }

    private static String blackHoleId(ClientLevel level, BlockPos pos) {
        return BLACK_HOLE_ID_PREFIX + level.dimension().identifier() + ":" + pos.asLong();
    }
}
