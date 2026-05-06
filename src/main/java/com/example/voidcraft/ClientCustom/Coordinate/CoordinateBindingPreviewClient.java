package com.example.voidcraft.ClientCustom.Coordinate;

import com.example.voidcraft.Block.entity.VoidEnergyBinding;
import com.example.voidcraft.Block.entity.VoidEnergyBindingType;
import com.example.voidcraft.Block.entity.VoidEnergyTransferBlockEntity;
import com.example.voidcraft.Effect.VoidBeamInstance;
import com.example.voidcraft.Effect.VoidBeamManager;
import com.example.voidcraft.Item.custom.CoordinateDesignatorItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

public final class CoordinateBindingPreviewClient {
    private static final double RENDER_RADIUS = 64.0D;
    private static final int REFRESH_INTERVAL_TICKS = 4;
    private static final VoidBeamInstance.Config OUTPUT_BEAM = VoidBeamInstance.Config.builder()
            .lifetimeTicks(8)
            .coreRadius(0.035F)
            .glowRadius(0.12F)
            .coreColor(0xC5FC9A)
            .glowColor(0x5CAB1E)
            .build();
    private static final VoidBeamInstance.Config INPUT_BEAM = VoidBeamInstance.Config.builder()
            .lifetimeTicks(8)
            .coreRadius(0.035F)
            .glowRadius(0.12F)
            .coreColor(0xFF9090)
            .glowColor(0xB51E32)
            .build();

    private static int refreshTicks;

    private CoordinateBindingPreviewClient() {
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !isHoldingDesignator(mc.player)) {
            refreshTicks = 0;
            return;
        }

        if (refreshTicks > 0) {
            refreshTicks--;
            return;
        }
        refreshTicks = REFRESH_INTERVAL_TICKS;

        renderLoadedBindings(mc.level, mc.player);
    }

    private static void renderLoadedBindings(ClientLevel level, Player player) {
        Identifier dimension = level.dimension().identifier();
        BlockPos playerPos = player.blockPosition();
        int minChunkX = (playerPos.getX() - (int) RENDER_RADIUS) >> 4;
        int maxChunkX = (playerPos.getX() + (int) RENDER_RADIUS) >> 4;
        int minChunkZ = (playerPos.getZ() - (int) RENDER_RADIUS) >> 4;
        int maxChunkZ = (playerPos.getZ() + (int) RENDER_RADIUS) >> 4;
        double radiusSqr = RENDER_RADIUS * RENDER_RADIUS;
        Set<String> rendered = new HashSet<>();

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof VoidEnergyTransferBlockEntity endpoint)) {
                        continue;
                    }
                    if (Vec3.atCenterOf(blockEntity.getBlockPos()).distanceToSqr(player.position()) > radiusSqr) {
                        continue;
                    }

                    Vec3 start = Vec3.atCenterOf(blockEntity.getBlockPos());
                    for (VoidEnergyBinding binding : endpoint.getOutputTargets()) {
                        if (!binding.target().dimension().equals(dimension)) {
                            continue;
                        }
                        if (!level.isLoaded(binding.target().pos())) {
                            continue;
                        }
                        if (!(level.getBlockEntity(binding.target().pos()) instanceof VoidEnergyTransferBlockEntity)) {
                            continue;
                        }

                        String key = endpoint.getVoidPosition().shortText() + "->" + binding.target().shortText() + ":" + binding.type().getId();
                        if (!rendered.add(key)) {
                            continue;
                        }

                        VoidBeamManager.addBeam(
                                start,
                                Vec3.atCenterOf(binding.target().pos()),
                                0.85F,
                                binding.type() == VoidEnergyBindingType.OUTPUT ? OUTPUT_BEAM : INPUT_BEAM
                        );
                    }
                }
            }
        }
    }

    private static boolean isHoldingDesignator(Player player) {
        return isDesignator(player.getMainHandItem()) || isDesignator(player.getOffhandItem());
    }

    private static boolean isDesignator(ItemStack stack) {
        return stack.getItem() instanceof CoordinateDesignatorItem;
    }
}
