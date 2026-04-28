package com.example.voidcraft.network;

import com.example.voidcraft.Custom.Clock.VoidClock;
import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.Effect.VoidRingManager;
import com.example.voidcraft.Effect.VoidTrailInstance;
import com.example.voidcraft.Effect.VoidTrailManager;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.BlinkVoidModule;
import com.example.voidcraft.Item.custom.PhaseWatch;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private static final String NETWORK_VERSION = "13";

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
        registrar.playToServer(UseWatchModulePayload.TYPE,UseWatchModulePayload.STREAM_CODEC,ModNetworking::handleUseWatchModuleServer);
        registrar.playToServer(ReleaseBlinkModulePayload.TYPE,ReleaseBlinkModulePayload.STREAM_CODEC,ModNetworking::handleReleaseBlinkModuleServer);
    }
    private static void handleReleaseBlinkModuleServer(
            ReleaseBlinkModulePayload payload,
            IPayloadContext context
    ){
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        int slot = payload.slot();
        int ticks = payload.ticks();

        if (slot < 0 || slot >= PhaseWatch.WATCH_MODULE_SLOT_COUNT) {      // 服务端再次检查槽位，不能只信客户端
            return;
        }

        ItemStack watchStack = player.getOffhandItem();                    // 真正执行时，以服务端副手里的手表为准

        if (!(watchStack.getItem() instanceof PhaseWatch)) {               // 副手不是 PhaseWatch，就不能释放模块技能
            return;
        }

        ItemContainerContents contents = watchStack.getOrDefault(
                DataComponents.CONTAINER,
                ItemContainerContents.EMPTY
        );
        NonNullList<ItemStack> items = NonNullList.withSize(
                PhaseWatch.WATCH_MODULE_SLOT_COUNT,
                ItemStack.EMPTY
        );
        contents.copyInto(items);                                          // 把手表容器里的模块复制到固定槽位列表

        ItemStack moduleStack = items.get(slot);                           // 找到客户端说要释放的那个模块槽
        if (!(moduleStack.getItem() instanceof BlinkVoidModule blinkModule)) { // 释放包只服务 Blink 模块
            return;
        }

        blinkModule.releaseBlink(player, watchStack, moduleStack, slot, ticks); // 最终交给 BlinkVoidModule 决定怎么闪


    }
    private static void handleUseWatchModuleServer(
            UseWatchModulePayload payload,
            IPayloadContext context
    ) {
        Player player = context.player();

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        int slot = payload.slot();

        if (slot < 0 || slot >= PhaseWatch.WATCH_MODULE_SLOT_COUNT) {
            return;
        }
        ItemStack watchStack = serverPlayer.getOffhandItem();

        if (!(watchStack.getItem() instanceof PhaseWatch)) {
            return;
        }
        PhaseWatch.useModule(serverPlayer, watchStack, slot);
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
