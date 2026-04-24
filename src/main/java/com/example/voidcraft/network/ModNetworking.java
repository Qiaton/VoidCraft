package com.example.voidcraft.network;

import com.example.voidcraft.Custom.Clock.VoidClock;
import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.Effect.VoidRingManager;
import com.example.voidcraft.Effect.VoidTrailInstance;
import com.example.voidcraft.Effect.VoidTrailManager;
import com.example.voidcraft.Item.custom.PhaseGauntlet;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private static final String NETWORK_VERSION = "9";

    private ModNetworking() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(ModNetworking::registerPayloadHandlers);
    }

    public static void sendPhaseTear(Player player, VoidRingInstance.Preset preset) {
        sendPhaseTearDetached(player, preset);
    }

    public static void sendPhaseTear(Entity entity, VoidRingInstance.Preset preset) {
        sendPhaseTearDetached(entity, preset);
    }

    public static void sendPhaseTearDetached(Entity entity, VoidRingInstance.Preset preset) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        VoidRingInstance.Preset actualPreset = preset == null ? VoidRingInstance.Preset.DEFAULT : preset;
        float scale = entity.getBbHeight() / 1.8F;
        sendPhaseTearAt(serverLevel, entity.position(), scale, entity.getId(), PhaseTearPayload.NO_ENTITY, actualPreset);
    }

    public static void sendPhaseTearAttached(Entity entity, VoidRingInstance.Preset preset) {
        sendPhaseTearAttachedInternal(entity, preset);
    }

    public static void sendPhaseTearAt(Entity source, Vec3 center, float scale, VoidRingInstance.Preset preset) {
        if (!(source.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        sendPhaseTearAt(serverLevel, center, scale, preset);
    }

    public static void sendPhaseTearAt(ServerLevel level, Vec3 center, float scale, VoidRingInstance.Preset preset) {
        sendPhaseTearAt(level, center, scale, PhaseTearPayload.NO_ENTITY, PhaseTearPayload.NO_ENTITY, preset);
    }

    public static void sendPhaseTearAt(
            ServerLevel level,
            Vec3 center,
            float scale,
            int ownerEntityId,
            int trackedEntityId,
            VoidRingInstance.Preset preset
    ) {
        VoidRingInstance.Preset actualPreset = preset == null ? VoidRingInstance.Preset.DEFAULT : preset;
        float actualScale = Math.max(0.01F, scale);
        Vec3 actualCenter = center.add(0.0D, actualPreset.centerYOffset() * actualScale, 0.0D);
        PhaseTearPayload payload = PhaseTearPayload.fromPreset(
                ownerEntityId,
                trackedEntityId,
                actualCenter.x,
                actualCenter.y,
                actualCenter.z,
                actualScale,
                actualPreset
        );

        PacketDistributor.sendToPlayersNear(
                level,
                null,
                actualCenter.x,
                actualCenter.y,
                actualCenter.z,
                128.0D,
                payload
        );
    }

    public static void sendEntityTrail(Entity entity, VoidTrailInstance.Preset preset) {
        sendEntityTrail(entity, preset, 1.0F);
    }

    public static void sendEntityTrail(Entity entity, VoidTrailInstance.Preset preset, float scale) {
        if (entity.level().isClientSide()) {
            return;
        }

        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        VoidTrailInstance.Preset actualPreset = preset == null ? VoidTrailInstance.Preset.DEFAULT : preset;
        float actualScale = Math.max(0.01F, scale);
        Vec3 seedEnd = entity.position();
        Vec3 seedStart = buildTrailSeedStart(entity, seedEnd);
        VoidTrailPayload payload = VoidTrailPayload.fromPreset(entity.getId(), actualScale, seedStart, seedEnd, actualPreset);

        Vec3 center = seedEnd;
        PacketDistributor.sendToPlayersNear(serverLevel, null, center.x, center.y, center.z, 128.0D, payload);
    }

    private static void sendPhaseTearAttachedInternal(Entity entity, VoidRingInstance.Preset preset) {
        if (entity.level().isClientSide()) {
            return;
        }

        VoidRingInstance.Preset actualPreset = preset == null ? VoidRingInstance.Preset.DEFAULT : preset;
        float scale = entity.getBbHeight() / 1.8F;
        Vec3 center = entity.position().add(0.0D, actualPreset.centerYOffset() * scale, 0.0D);

        PhaseTearPayload payload = PhaseTearPayload.fromPreset(
                entity.getId(),
                entity.getId(),
                center.x,
                center.y,
                center.z,
                scale,
                actualPreset
        );

        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload);
    }

    private static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToClient(PhaseTearPayload.TYPE, PhaseTearPayload.STREAM_CODEC, ModNetworking::handlePhaseTearClient);
        registrar.playToClient(VoidTrailPayload.TYPE, VoidTrailPayload.STREAM_CODEC, ModNetworking::handleVoidTrailClient);
        registrar.playToServer(UseGauntletModulePayload.TYPE,UseGauntletModulePayload.STREAM_CODEC,ModNetworking::handleUseGauntletModuleServer);
        System.out.println("网络主要内容注册完毕");
    }

    private static void handleUseGauntletModuleServer(
            UseGauntletModulePayload payload,
            IPayloadContext context
    ) {
        System.out.println("网络模块执行中");
        Player player = context.player();

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        System.out.println("玩家检验完毕");
        int slot = payload.slot();

        if (slot < 0 || slot >= PhaseGauntlet.GAUNTLET_MODULE_SLOT_COUNT) {
            return;
        }
        System.out.println("槽位检验完毕");
        ItemStack gauntletStack = serverPlayer.getOffhandItem();

        if (!(gauntletStack.getItem() instanceof PhaseGauntlet)) {
            return;
        }
        System.out.println("手套道具检验完毕");
        PhaseGauntlet.useModule(serverPlayer, gauntletStack, slot);
        System.out.println("手套使用中");
    }

    private static void handleVoidTrailClient(VoidTrailPayload payload, IPayloadContext context) {
        VoidTrailManager.trackEntity(payload.entityId(), payload.scale(), payload.toPreset(), payload.seedStart(), payload.seedEnd());
    }

    private static Vec3 buildTrailSeedStart(Entity entity, Vec3 seedEnd) {
        Vec3 motion = entity.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-8D) {
            return null;
        }

        return seedEnd.subtract(motion);
    }

    private static void handlePhaseTearClient(PhaseTearPayload payload, IPayloadContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        Entity trackedEntity = payload.hasTrackedEntity() ? mc.level.getEntity(payload.trackedEntityId()) : null;
        VoidRingInstance.Preset preset = payload.toPreset();
        Vec3 center = new Vec3(payload.centerX(), payload.centerY(), payload.centerZ());
        if (payload.hasTrackedEntity() && trackedEntity != null) {
            VoidRingManager.addTrackedRing(
                    payload.ownerEntityId(),
                    payload.trackedEntityId(),
                    center,
                    payload.scale(),
                    preset
            );
        } else {
            VoidRingManager.addRing(
                    payload.ownerEntityId(),
                    center,
                    payload.scale(),
                    preset
            );
        }

        if (trackedEntity instanceof Player player) {
            VoidClock.VOID_PLAYER_FLASH(player);
        }
    }
}
