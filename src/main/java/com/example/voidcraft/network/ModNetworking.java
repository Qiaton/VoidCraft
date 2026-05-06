package com.example.voidcraft.network;

import com.example.voidcraft.Block.entity.BoundVoidPosition;
import com.example.voidcraft.Block.entity.VoidEnergyBinding;
import com.example.voidcraft.Block.entity.VoidEnergyTransfer;
import com.example.voidcraft.Block.entity.VoidEnergyTransferBlockEntity;
import com.example.voidcraft.ClientCustom.Coordinate.CoordinateBindingScreen;
import com.example.voidcraft.ClientCustom.Turret.PhaseEmitterClientManager;
import com.example.voidcraft.ClientCustom.Void.PhaseWorldTransitionClient;
import com.example.voidcraft.Custom.Clock.VoidClock;
import com.example.voidcraft.ClientCustom.EnergyHud;
import com.example.voidcraft.Effect.VoidBeamInstance;
import com.example.voidcraft.Effect.VoidBlackHoleInstance;
import com.example.voidcraft.Effect.VoidBlackHoleManager;
import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.Effect.VoidRingManager;
import com.example.voidcraft.Effect.VoidTrailInstance;
import com.example.voidcraft.Effect.VoidTrailManager;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.BlinkVoidModule;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.PhaseTurretModule;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.WorldModule;
import com.example.voidcraft.Item.custom.PhaseWatch;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private static final String NETWORK_VERSION = "31";

    private ModNetworking() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(ModNetworking::registerPayloadHandlers);
    }

    public static void sendEnergyHud(ServerPlayer player, int percent, boolean visible) {
        PacketDistributor.sendToPlayer(player, new EnergyHudPayload(percent, visible));
    }

    public static void sendCoordinateBindings(
            ServerPlayer player,
            BoundVoidPosition ownerPosition,
            VoidEnergyTransferBlockEntity owner
    ) {
        java.util.ArrayList<CoordinateBindingsPayload.Entry> entries = new java.util.ArrayList<>();
        MinecraftServer server = player.level().getServer();
        for (VoidEnergyBinding binding : owner.getOutputTargets()) {
            entries.add(new CoordinateBindingsPayload.Entry(
                    true,
                    binding.target(),
                    binding.type(),
                    VoidEnergyTransfer.describeOutputBinding(server, owner, binding),
                    getBoundBlockName(server, binding.target())
            ));
        }
        for (VoidEnergyBinding binding : owner.getInputSources()) {
            entries.add(new CoordinateBindingsPayload.Entry(
                    false,
                    binding.target(),
                    binding.type(),
                    VoidEnergyTransfer.describeInputBinding(server, owner, binding),
                    getBoundBlockName(server, binding.target())
            ));
        }
        PacketDistributor.sendToPlayer(player, new CoordinateBindingsPayload(ownerPosition, entries));
    }

    private static Component getBoundBlockName(MinecraftServer server, BoundVoidPosition position) {
        ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, position.dimension());
        ServerLevel level = server.getLevel(levelKey);
        if (level == null || !level.isLoaded(position.pos())) {
            return Component.translatable("screen.void_craft.coordinate_bindings.target_unloaded");
        }
        return level.getBlockState(position.pos()).getBlock().getName();
    }

    public static void sendPhaseWorldTransition(
            ServerPlayer player,
            ResourceKey<Level> sourceDimension,
            ResourceKey<Level> targetDimension
    ) {
        PacketDistributor.sendToPlayer(
                player,
                new PhaseWorldTransitionPayload(sourceDimension.identifier(), targetDimension.identifier())
        );
    }

    // 手动炮台：显示炮台球，同时让客户端把左键交给炮台射击。
    public static void sendTurretState(ServerPlayer player, boolean active) {
        sendTurretState(player, active, true);
    }

    public static void sendTurretState(ServerPlayer player, boolean active, ItemStack moduleStack) {
        sendTurretState(player, active, true, PhaseTurretModule.getEmitterCount(moduleStack));
    }

    // 辅助炮台：只显示炮台球和光束，不隐藏手、不吞掉左右键。
    public static void sendAssistTurretState(ServerPlayer player, boolean active) {
        sendTurretState(player, active, false);
    }

    public static void sendAssistTurretState(ServerPlayer player, boolean active, ItemStack moduleStack) {
        sendTurretState(player, active, false, PhaseTurretModule.getEmitterCount(moduleStack));
    }

    private static void sendTurretState(ServerPlayer player, boolean active, boolean blocksInput) {
        sendTurretState(player, active, blocksInput, PhaseTurretModule.getEmitterCount());
    }

    private static void sendTurretState(ServerPlayer player, boolean active, boolean blocksInput, int emitterCount) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player,
                new TurretStatePayload(player.getId(), active, blocksInput, emitterCount)
        );
    }

    public static void sendTurretShotFx(
            ServerPlayer player,
            int emitterIndex,
            Vec3 targetPos,
            VoidBeamInstance.Config beamConfig
    ) {
        if (targetPos == null) {
            return;
        }

        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player,
                TurretShotFxPayload.fromConfig(
                        player.getId(),
                        emitterIndex,
                        targetPos.x,
                        targetPos.y,
                        targetPos.z,
                        beamConfig
                )
        );
    }

    public static void sendBlackHoleAt(ServerLevel level, Vec3 center, float scale, VoidBlackHoleInstance.Config config) {
        sendBlackHoleAt(level, center, scale, VoidBlackHolePayload.NO_ENTITY, config);
    }

    public static void sendBlackHoleAt(
            ServerLevel level,
            Vec3 center,
            float scale,
            int ownerEntityId,
            VoidBlackHoleInstance.Config config
    ) {
        if (center == null) {
            return;
        }

        VoidBlackHoleInstance.Config actualConfig = config == null ? VoidBlackHoleInstance.Config.DEFAULT : config;
        float actualScale = Math.max(0.01F, scale);
        Vec3 actualCenter = center.add(0.0D, actualConfig.centerYOffset() * actualScale, 0.0D);
        VoidBlackHolePayload payload = VoidBlackHolePayload.fromConfig(
                ownerEntityId,
                actualCenter.x,
                actualCenter.y,
                actualCenter.z,
                actualScale,
                actualConfig
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

    // 在实体当前位置生成一个不跟随实体的相位环，常用于进入/退出虚空瞬间。
    public static void sendPhaseTear(Player player, VoidRingInstance.Preset preset) {
        sendPhaseTearDetached(player, preset);
    }

    // Entity 版本的 detached ring 入口，适合箭、玩家、怪物等任意实体。
    public static void sendPhaseTear(Entity entity, VoidRingInstance.Preset preset) {
        sendPhaseTearDetached(entity, preset);
    }

    // 发送固定在当前坐标的 ring；发出后不会继续跟随实体。
    public static void sendPhaseTearDetached(Entity entity, VoidRingInstance.Preset preset) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        VoidRingInstance.Preset actualPreset = preset == null ? VoidRingInstance.Preset.DEFAULT : preset;
        float scale = entity.getBbHeight() / 1.8F;
        sendPhaseTearAt(serverLevel, entity.position(), scale, entity.getId(), PhaseTearPayload.NO_ENTITY, actualPreset);
    }

    // 发送跟随实体移动的 ring；客户端渲染时会持续读取 trackedEntityId 的位置。
    public static void sendPhaseTearAttached(Entity entity, VoidRingInstance.Preset preset) {
        sendPhaseTearAttachedInternal(entity, preset);
    }

    // 从实体所在世界发一个指定坐标的 ring，center 是未加 centerYOffset 的世界坐标。
    public static void sendPhaseTearAt(Entity source, Vec3 center, float scale, VoidRingInstance.Preset preset) {
        if (!(source.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        sendPhaseTearAt(serverLevel, center, scale, preset);
    }

    // 直接从 ServerLevel 发一个指定坐标的 ring，适合没有来源实体的世界效果。
    public static void sendPhaseTearAt(ServerLevel level, Vec3 center, float scale, VoidRingInstance.Preset preset) {
        sendPhaseTearAt(level, center, scale, PhaseTearPayload.NO_ENTITY, PhaseTearPayload.NO_ENTITY, preset);
    }

    // ring 发包的完整入口：owner 用于标记来源，tracked 用于决定是否跟随实体。
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

    // 让客户端开始持续追踪一个实体并生成拖尾，常用于飞行中的箭。
    public static void sendEntityTrail(Entity entity, VoidTrailInstance.Preset preset) {
        sendEntityTrail(entity, preset, 1.0F);
    }

    // 实体拖尾完整入口：scale 控制宽高缩放，preset 控制采样、寿命和颜色。
    public static void sendEntityTrail(Entity entity, VoidTrailInstance.Preset preset, float scale) {
        sendEntityTrail(entity, preset, scale, -1.0D);
    }

    // 高速实体可传 seedLength，避免用一整 tick 的速度反推到太远的身后位置。
    public static void sendEntityTrail(Entity entity, VoidTrailInstance.Preset preset, float scale, double seedLength) {
        if (entity.level().isClientSide()) {
            return;
        }

        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        VoidTrailInstance.Preset actualPreset = preset == null ? VoidTrailInstance.Preset.DEFAULT : preset;
        float actualScale = Math.max(0.01F, scale);
        Vec3 seedEnd = entity.position();
        Vec3 seedStart = buildTrailSeedStart(entity, seedEnd, seedLength);
        VoidTrailPayload payload = VoidTrailPayload.fromPreset(entity.getId(), actualScale, seedStart, seedEnd, actualPreset);

        Vec3 center = seedEnd;
        PacketDistributor.sendToPlayersNear(serverLevel, null, center.x, center.y, center.z, 128.0D, payload);
    }

    // 在 from/to 两个世界坐标之间生成一次性拖尾段；Blink 这类瞬移效果优先用这个。
    public static void sendTrailSegment(
            ServerLevel level,
            int ownerEntityId,
            Vec3 from,
            Vec3 to,
            float scale,
            VoidTrailInstance.Preset preset
    ) {
        if (from == null || to == null || from.distanceToSqr(to) < 1.0E-8D) {
            return;
        }

        VoidTrailInstance.Preset actualPreset = preset == null ? VoidTrailInstance.Preset.DEFAULT : preset;
        float actualScale = Math.max(0.01F, scale);
        VoidTrailPayload payload = VoidTrailPayload.fromSegment(ownerEntityId, actualScale, from, to, actualPreset);

        PacketDistributor.sendToPlayersNear(level, null, to.x, to.y, to.z, 128.0D, payload);
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
        registrar.playToClient(VoidBlackHolePayload.TYPE, VoidBlackHolePayload.STREAM_CODEC, ModNetworking::handleVoidBlackHoleClient);
        registrar.playToClient(EnergyHudPayload.TYPE, EnergyHudPayload.STREAM_CODEC, ModNetworking::handleEnergyHudClient);
        registrar.playToClient(PhaseWorldTransitionPayload.TYPE, PhaseWorldTransitionPayload.STREAM_CODEC, ModNetworking::handlePhaseWorldTransitionClient);
        registrar.playToClient(TurretStatePayload.TYPE, TurretStatePayload.STREAM_CODEC, ModNetworking::handleTurretStateClient);
        registrar.playToClient(TurretShotFxPayload.TYPE, TurretShotFxPayload.STREAM_CODEC, ModNetworking::handleTurretShotFxClient);
        registrar.playToClient(CoordinateBindingsPayload.TYPE, CoordinateBindingsPayload.STREAM_CODEC, ModNetworking::handleCoordinateBindingsClient);
        registrar.playToServer(UseWatchModulePayload.TYPE, UseWatchModulePayload.STREAM_CODEC, ModNetworking::handleUseWatchModuleServer);
        registrar.playToServer(ReleaseBlinkModulePayload.TYPE, ReleaseBlinkModulePayload.STREAM_CODEC, ModNetworking::handleReleaseBlinkModuleServer);
        registrar.playToServer(UseTurretShotPayload.TYPE, UseTurretShotPayload.STREAM_CODEC, ModNetworking::handleUseTurretShotServer);
        registrar.playToServer(PhaseWorldTransitionReadyPayload.TYPE, PhaseWorldTransitionReadyPayload.STREAM_CODEC, ModNetworking::handlePhaseWorldTransitionReadyServer);
        registrar.playToServer(RemoveCoordinateBindingPayload.TYPE, RemoveCoordinateBindingPayload.STREAM_CODEC, ModNetworking::handleRemoveCoordinateBindingServer);
    }
    public static void sendToServer(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
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
        double x = payload.x();
        double y = payload.y();
        double z = payload.z();

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

        blinkModule.releaseBlink(player, watchStack, moduleStack, slot, ticks, x, y, z); // 最终交给 BlinkVoidModule 验算目标点后决定怎么闪


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
        if (!payload.trackEntity()) {
            VoidTrailManager.addTrailSegment(payload.seedStart(), payload.seedEnd(), payload.scale(), payload.toPreset());
            return;
        }

        VoidTrailManager.trackEntity(payload.entityId(), payload.scale(), payload.toPreset(), payload.seedStart(), payload.seedEnd());
    }

    private static void handleEnergyHudClient(EnergyHudPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> EnergyHud.update(payload.percent(), payload.visible()));
    }

    private static void handlePhaseWorldTransitionClient(PhaseWorldTransitionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PhaseWorldTransitionClient.beginLoadingTransition(
                payload.sourceDimension(),
                payload.targetDimension()
        ));
    }

    private static void handleTurretStateClient(TurretStatePayload payload, IPayloadContext context) {
        // blocksInput 是手动/辅助炮台的客户端边界，不能只用 active 推断。
        context.enqueueWork(() -> PhaseEmitterClientManager.syncState(
                payload.playerId(),
                payload.active(),
                payload.blocksInput(),
                payload.emitterCount()
        ));
    }

    private static void handleTurretShotFxClient(TurretShotFxPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PhaseEmitterClientManager.playShotFx(
                payload.playerId(),
                payload.emitterIndex(),
                new Vec3(payload.targetX(), payload.targetY(), payload.targetZ()),
                payload.toBeamConfig()
        ));
    }

    private static void handleCoordinateBindingsClient(CoordinateBindingsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> CoordinateBindingScreen.open(payload));
    }

    private static void handleRemoveCoordinateBindingServer(RemoveCoordinateBindingPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!payload.owner().dimension().equals(player.level().dimension().identifier())) {
            return;
        }
        if (player.blockPosition().distSqr(payload.owner().pos()) > 64.0D) {
            return;
        }
        if (!player.level().isLoaded(payload.owner().pos())) {
            return;
        }

        BlockEntity ownerBlockEntity = player.level().getBlockEntity(payload.owner().pos());
        if (!(ownerBlockEntity instanceof VoidEnergyTransferBlockEntity owner)) {
            return;
        }

        if (payload.outputList()) {
            VoidEnergyTransfer.removeOutputBinding(player.level().getServer(), owner, payload.target());
        } else {
            VoidEnergyTransfer.removeInputBinding(player.level().getServer(), owner, payload.target());
        }
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.void_craft.coordinate_designator.binding_removed"), true);
        sendCoordinateBindings(player, payload.owner(), owner);
    }

    private static void handleVoidBlackHoleClient(VoidBlackHolePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                return;
            }

            VoidBlackHoleManager.addBlackHole(
                    payload.ownerEntityId(),
                    new Vec3(payload.centerX(), payload.centerY(), payload.centerZ()),
                    payload.scale(),
                    payload.toConfig()
            );
        });
    }

    private static Vec3 buildTrailSeedStart(Entity entity, Vec3 seedEnd, double seedLength) {
        Vec3 motion = entity.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-8D) {
            return null;
        }

        if (seedLength > 0.0D) {
            return seedEnd.subtract(motion.normalize().scale(seedLength));
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
    private static void handleUseTurretShotServer(
            UseTurretShotPayload payload,
            IPayloadContext context
    ) {
        // 这个包只服务手动炮台；辅助炮台完全由服务端自动 tick，不依赖客户端按键。
        Player player = context.player();

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        PhaseTurretModule.setInputState(serverPlayer, payload.shooting(), payload.volleyShooting());
    }

    private static void handlePhaseWorldTransitionReadyServer(
            PhaseWorldTransitionReadyPayload payload,
            IPayloadContext context
    ) {
        Player player = context.player();

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        WorldModule.completePendingTransition(serverPlayer);
    }
}
