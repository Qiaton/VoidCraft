package com.example.voidcraft.Network;

import com.example.voidcraft.Block.Block.ChunkMapperBlock;
import com.example.voidcraft.Block.entity.ChunkMapperBlockEntity;
import com.example.voidcraft.Custom.Behavior.Energy.BoundVoidPosition;
import com.example.voidcraft.Custom.Behavior.Energy.VoidEnergyBinding;
import com.example.voidcraft.Custom.Behavior.Energy.VoidEnergyTransfer;
import com.example.voidcraft.Custom.Behavior.Energy.VoidEnergyTransferBlockEntity;
import com.example.voidcraft.Custom.Clock.ModuleSkill.TeleportVoidModuleClock;
import com.example.voidcraft.ClientCustom.Sound.ContinuousLoopSoundClient;
import com.example.voidcraft.Gui.ChunkMapperStatusScreen;
import com.example.voidcraft.Gui.CoordinateBindingScreen;
import com.example.voidcraft.ClientCustom.Turret.PhaseEmitterClientManager;
import com.example.voidcraft.ClientCustom.Void.PhaseWorldTransitionClient;
import com.example.voidcraft.World.GoWorld;
import com.example.voidcraft.World.projection.PhaseProjectionClient;
import com.example.voidcraft.World.projection.PhaseProjectionSnapshot;
import com.example.voidcraft.Custom.Clock.VoidClock;
import com.example.voidcraft.Gui.EnergyHud;
import com.example.voidcraft.Effect.VoidBeamInstance;
import com.example.voidcraft.Effect.VoidBlackHoleInstance;
import com.example.voidcraft.Effect.VoidBlackHoleManager;
import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.Effect.VoidRingManager;
import com.example.voidcraft.Effect.VoidTrailInstance;
import com.example.voidcraft.Effect.VoidTrailManager;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.AssistPhaseTurretModule;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.BlackHoleModule;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.BlinkVoidModule;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.PhaseTurretModule;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.TeleportVoidModule;
import com.example.voidcraft.Item.custom.PhaseWatch;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ModNetworking {
    private static final String NETWORK_VERSION = "37";
    private static final double PHASE_TEAR_SEND_RANGE = 128.0D;

    private ModNetworking() {
    }

    // 网络注册（调用场景：模组初始化；参数 bus 是 NeoForge 事件总线，用来挂载所有自定义包注册）。
    public static void register(IEventBus bus) {
        bus.addListener(ModNetworking::registerPayloads);
    }

    // 能量 HUD 同步（调用场景：手表能量变化；player 是接收玩家，percent 是能量百分比，visible 控制 HUD 显隐）。
    public static void sendEnergyHud(ServerPlayer player, int percent, boolean visible) {
        PacketDistributor.sendToPlayer(player, new EnergyHudPayload(percent, visible));
    }

    // 坐标绑定面板同步（调用场景：玩家打开/刷新绑定列表；ownerPosition 是当前方块坐标，owner 提供输入输出绑定）。
    public static void sendCoordinateBindings(
            ServerPlayer player,
            BoundVoidPosition ownerPosition,
            VoidEnergyTransferBlockEntity owner
    ) {
        // 把输入列表和输出列表合并成一份界面数据，客户端只负责展示和发删除请求。
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

    // 区块映射器状态同步（调用场景：玩家打开/调整区块映射器面板；mapper 提供档位、能量和输入口状态）。
    public static void sendChunkMapperStatus(ServerPlayer player, ChunkMapperBlockEntity mapper) {
        // 状态面板打开时即时组装一次快照，避免客户端自己推算服务端状态。
        MinecraftServer server = player.level().getServer();
        BoundVoidPosition inputSource = null;
        VoidEnergyTransfer.BindingStatus inputStatus = VoidEnergyTransfer.BindingStatus.NOT_FUNCTIONAL;
        Component inputName = Component.translatable("screen.void_craft.chunk_mapper_status.input_empty");
        if (!mapper.getInputSources().isEmpty()) {
            VoidEnergyBinding binding = mapper.getInputSources().get(0);
            inputSource = binding.target();
            inputStatus = VoidEnergyTransfer.describeInputBinding(server, mapper, binding);
            inputName = getBoundBlockName(server, inputSource);
        }

        PacketDistributor.sendToPlayer(player, new ChunkMapperStatusPayload(
                mapper.getVoidPosition(),
                mapper.getTier(),
                mapper.getChunkRadius(),
                mapper.getCoverageSize(),
                mapper.getEnergyCostPerTick(),
                mapper.getEnergyStored(),
                mapper.getEnergyCapacity(),
                mapper.isRunning(),
                inputSource,
                inputStatus,
                inputName
        ));
    }

    private static Component getBoundBlockName(MinecraftServer server, BoundVoidPosition position) {
        // 目标没加载时仍然显示“未加载”，不要为了名字强行加载区块。
        ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, position.dimension());
        ServerLevel level = server.getLevel(levelKey);
        if (level == null || !level.isLoaded(position.pos())) {
            return Component.translatable("screen.void_craft.coordinate_bindings.target_unloaded");
        }
        return level.getBlockState(position.pos()).getBlock().getName();
    }

    // 相位世界切换触发（调用场景：GoWorld 开始传送；sourceDimension 是来源维度，targetDimension 是目标维度）。
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

    // 相位投影同步（调用场景：进入相位维度时发送原世界投影；snapshot 是客户端用于编译投影方块的快照）。
    public static void sendPhaseProjection(ServerPlayer player, PhaseProjectionSnapshot snapshot) {
        // 投影数据只发给进入相位维度的玩家本人，客户端负责把它画成线框。
        PacketDistributor.sendToPlayer(player, new PhaseProjectionPayload(snapshot));
    }

    // 手动炮台状态同步（调用场景：手动炮台开关；active 控制显示炮台球并让客户端接管左右键）。
    public static void sendTurretState(ServerPlayer player, boolean active) {
        sendTurretState(player, active, true);
    }

    // 手动炮台状态同步（调用场景：带模块数据开关手动炮台；moduleStack 用来取炮台球数量和恢复型颜色）。
    public static void sendTurretState(ServerPlayer player, boolean active, ItemStack moduleStack) {
        sendTurretState(player, active, true, PhaseTurretModule.getEmitterCount(moduleStack), PhaseTurretModule.hasHealthVisual(moduleStack));
    }

    // 辅助炮台状态同步（调用场景：辅助炮台关闭或无模块状态时；active 只控制炮台球，不接管输入）。
    public static void sendAssistTurretState(ServerPlayer player, boolean active) {
        sendTurretState(player, active, false);
    }

    // 辅助炮台状态同步（调用场景：辅助炮台启动；moduleStack 用来取炮台球数量和恢复型颜色）。
    public static void sendAssistTurretState(ServerPlayer player, boolean active, ItemStack moduleStack) {
        sendTurretState(player, active, false, PhaseTurretModule.getEmitterCount(moduleStack), AssistPhaseTurretModule.hasHealthVisual(moduleStack));
    }

    // 炮台状态基础同步（调用场景：无模块数据的默认状态；blocksInput 表示是否吞本地左右键）。
    private static void sendTurretState(ServerPlayer player, boolean active, boolean blocksInput) {
        sendTurretState(player, active, blocksInput, PhaseTurretModule.getEmitterCount(), false);
    }

    // 炮台状态完整同步（调用场景：所有炮台开关；emitterCount 是球数量，healthVisual 决定是否用恢复炮台球颜色）。
    private static void sendTurretState(ServerPlayer player, boolean active, boolean blocksInput, int emitterCount, boolean healthVisual) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player,
                new TurretStatePayload(player.getId(), active, blocksInput, emitterCount, active && healthVisual)
        );
    }

    // 炮台射击特效同步（调用场景：炮台服务端结算后；emitterIndex 是发射球序号，targetPos 是命中点，beamConfig 是光束颜色/宽度配置）。
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
                        UUID.randomUUID(),
                        player.getId(),
                        emitterIndex,
                        targetPos.x,
                        targetPos.y,
                        targetPos.z,
                        beamConfig
                )
        );
    }

    // 黑洞触发（调用场景：无归属实体的世界黑洞效果；center 是中心点，scale 是整体缩放，config 是黑洞视觉参数）。
    public static void sendBlackHoleAt(ServerLevel level, Vec3 center, float scale, VoidBlackHoleInstance.Config config) {
        sendBlackHoleAt(level, center, scale, VoidBlackHolePayload.NO_ENTITY, config);
    }

    // 黑洞触发（调用场景：有归属实体的黑洞效果；ownerEntityId 标记来源，center/scale/config 控制客户端实例）。
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

        sendBlackHoleAt(level, center, scale, UUID.randomUUID(), 0, ownerEntityId, config);
    }

    public static void sendBlackHoleAt(
            ServerLevel level,
            Vec3 center,
            float scale,
            UUID effectId,
            int ageTicks,
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
                effectId,
                ageTicks,
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

    // 持续循环声音开始（调用场景：服务端事件创建；客户端按 id 循环播放直到 stop 包或兜底时长结束）。
    public static void sendLoopSoundStart(
            ServerLevel level,
            UUID id,
            Identifier sound,
            Vec3 center,
            float volume,
            float pitch,
            int durationTicks
    ) {
        if (level == null || id == null || sound == null || center == null) {
            return;
        }

        PacketDistributor.sendToPlayersNear(
                level,
                null,
                center.x,
                center.y,
                center.z,
                128.0D,
                ContinuousLoopSoundPayload.start(id, sound, center, volume, pitch, durationTicks)
        );
    }

    // 持续循环声音停止（调用场景：服务端事件结束；客户端按 id 停止对应循环声音）。
    public static void sendLoopSoundStop(ServerLevel level, UUID id, Identifier sound, Vec3 center) {
        if (level == null || id == null || sound == null || center == null) {
            return;
        }

        PacketDistributor.sendToPlayersNear(
                level,
                null,
                center.x,
                center.y,
                center.z,
                128.0D,
                ContinuousLoopSoundPayload.stop(id, sound, center)
        );
    }

    // 相位裂隙触发（调用场景：玩家当前位置生成不跟随相位环；preset 是环的颜色、尺寸和扭曲配置）。
    public static void sendPhaseTear(Player player, VoidRingInstance.Preset preset) {
        sendPhaseTearDetached(player, preset);
    }

    // 相位裂隙触发（调用场景：任意实体当前位置生成不跟随相位环；entity 提供坐标和高度缩放）。
    public static void sendPhaseTear(Entity entity, VoidRingInstance.Preset preset) {
        sendPhaseTearDetached(entity, preset);
    }

    // 相位裂隙触发（调用场景：实体当前位置固定相位环；发出后不会继续跟随实体）。
    public static void sendPhaseTearDetached(Entity entity, VoidRingInstance.Preset preset) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        VoidRingInstance.Preset actualPreset = preset == null ? VoidRingInstance.Preset.DEFAULT : preset;
        float scale = entity.getBbHeight() / 1.8F;
        sendPhaseTearAt(serverLevel, entity.position(), scale, entity.getId(), PhaseTearPayload.NO_ENTITY, actualPreset);
    }

    // 相位裂隙触发（调用场景：跟随实体移动的相位环；客户端持续读取 trackedEntityId 的位置）。
    public static void sendPhaseTearAttached(Entity entity, VoidRingInstance.Preset preset) {
        sendAttachedPhaseTear(entity, preset);
    }

    // 相位裂隙触发（调用场景：从实体所在世界发指定坐标相位环；center 是未加 centerYOffset 的世界坐标）。
    public static void sendPhaseTearAt(Entity source, Vec3 center, float scale, VoidRingInstance.Preset preset) {
        if (!(source.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        sendPhaseTearAt(serverLevel, center, scale, preset);
    }

    // 相位裂隙触发（调用场景：无来源实体的世界相位环；level/center/scale/preset 直接决定广播范围和样式）。
    public static void sendPhaseTearAt(ServerLevel level, Vec3 center, float scale, VoidRingInstance.Preset preset) {
        sendPhaseTearAt(level, center, scale, 0.0F, PhaseTearPayload.NO_ENTITY, PhaseTearPayload.NO_ENTITY, preset);
    }

    public static void sendPhaseTearAt(ServerLevel level, Vec3 center, float scale, float yaw, VoidRingInstance.Preset preset) {
        sendPhaseTearAt(level, center, scale, yaw, PhaseTearPayload.NO_ENTITY, PhaseTearPayload.NO_ENTITY, preset);
    }

    public static void sendPhaseTearPair(
            ServerPlayer owner,
            ServerLevel level,
            Vec3 firstCenter,
            float firstScale,
            float firstYaw,
            Vec3 secondCenter,
            float secondScale,
            float secondYaw,
            VoidRingInstance.Preset preset
    ) {
        sendPhaseTearPair(
                owner,
                level,
                UUID.randomUUID(),
                0,
                firstCenter,
                firstScale,
                firstYaw,
                UUID.randomUUID(),
                0,
                secondCenter,
                secondScale,
                secondYaw,
                preset
        );
    }

    public static void sendPhaseTearPair(
            ServerPlayer owner,
            ServerLevel level,
            UUID firstId,
            int firstAgeTicks,
            Vec3 firstCenter,
            float firstScale,
            float firstYaw,
            UUID secondId,
            int secondAgeTicks,
            Vec3 secondCenter,
            float secondScale,
            float secondYaw,
            VoidRingInstance.Preset preset
    ) {
        if (level == null || firstCenter == null || secondCenter == null) {
            return;
        }

        VoidRingInstance.Preset actualPreset = preset == null ? VoidRingInstance.Preset.DEFAULT : preset;
        float actualFirstScale = Math.max(0.01F, firstScale);
        float actualSecondScale = Math.max(0.01F, secondScale);
        Vec3 actualFirstCenter = firstCenter.add(0.0D, actualPreset.centerYOffset() * actualFirstScale, 0.0D);
        Vec3 actualSecondCenter = secondCenter.add(0.0D, actualPreset.centerYOffset() * actualSecondScale, 0.0D);
        PhaseTearPayload firstPayload = PhaseTearPayload.fromPreset(
                firstId,
                firstAgeTicks,
                PhaseTearPayload.NO_ENTITY,
                PhaseTearPayload.NO_ENTITY,
                actualFirstCenter.x,
                actualFirstCenter.y,
                actualFirstCenter.z,
                actualFirstScale,
                firstYaw,
                actualPreset
        );
        PhaseTearPayload secondPayload = PhaseTearPayload.fromPreset(
                secondId,
                secondAgeTicks,
                PhaseTearPayload.NO_ENTITY,
                PhaseTearPayload.NO_ENTITY,
                actualSecondCenter.x,
                actualSecondCenter.y,
                actualSecondCenter.z,
                actualSecondScale,
                secondYaw,
                actualPreset
        );

        Set<ServerPlayer> receivers = new HashSet<>();
        double rangeSqr = PHASE_TEAR_SEND_RANGE * PHASE_TEAR_SEND_RANGE;
        for (ServerPlayer player : level.players()) {
            if (isNear(player, actualFirstCenter, rangeSqr) || isNear(player, actualSecondCenter, rangeSqr)) {
                receivers.add(player);
            }
        }
        if (owner != null && owner.level() == level) {
            receivers.add(owner);
        }

        for (ServerPlayer player : receivers) {
            PacketDistributor.sendToPlayer(player, firstPayload);
            PacketDistributor.sendToPlayer(player, secondPayload);
        }
    }

    // 相位裂隙触发（调用场景：相位环完整入口；ownerEntityId 标记来源，trackedEntityId 决定是否跟随实体）。
    public static void sendPhaseTearAt(
            ServerLevel level,
            Vec3 center,
            float scale,
            int ownerEntityId,
            int trackedEntityId,
            VoidRingInstance.Preset preset
    ) {
        sendPhaseTearAt(level, center, scale, 0.0F, ownerEntityId, trackedEntityId, preset);
    }

    public static void sendPhaseTearAt(
            ServerLevel level,
            Vec3 center,
            float scale,
            float yaw,
            int ownerEntityId,
            int trackedEntityId,
            VoidRingInstance.Preset preset
    ) {
        sendPhaseTearAt(level, center, scale, yaw, UUID.randomUUID(), 0, ownerEntityId, trackedEntityId, preset);
    }

    public static void sendPhaseTearAt(
            ServerLevel level,
            Vec3 center,
            float scale,
            float yaw,
            UUID effectId,
            int ageTicks,
            int ownerEntityId,
            int trackedEntityId,
            VoidRingInstance.Preset preset
    ) {
        VoidRingInstance.Preset actualPreset = preset == null ? VoidRingInstance.Preset.DEFAULT : preset;
        float actualScale = Math.max(0.01F, scale);
        Vec3 actualCenter = center.add(0.0D, actualPreset.centerYOffset() * actualScale, 0.0D);
        PhaseTearPayload payload = PhaseTearPayload.fromPreset(
                effectId,
                ageTicks,
                ownerEntityId,
                trackedEntityId,
                actualCenter.x,
                actualCenter.y,
                actualCenter.z,
                actualScale,
                yaw,
                actualPreset
        );

        PacketDistributor.sendToPlayersNear(
                level,
                null,
                actualCenter.x,
                actualCenter.y,
                actualCenter.z,
                PHASE_TEAR_SEND_RANGE,
                payload
        );
    }

    // 实体拖尾触发（调用场景：默认尺寸的实体拖尾；entity 是被追踪实体，preset 是拖尾视觉配置）。
    public static void sendEntityTrail(Entity entity, VoidTrailInstance.Preset preset) {
        sendEntityTrail(entity, preset, 1.0F);
    }

    // 实体拖尾触发（调用场景：指定尺寸的实体拖尾；scale 控制宽高缩放，preset 控制采样、寿命和颜色）。
    public static void sendEntityTrail(Entity entity, VoidTrailInstance.Preset preset, float scale) {
        sendEntityTrail(entity, preset, scale, -1.0D);
    }

    // 实体拖尾触发（调用场景：高速实体拖尾；seedLength 限制初始拖尾段长度，避免速度过快拉太远）。
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
        Vec3 seedStart = getTrailSeedStart(entity, seedEnd, seedLength);
        VoidTrailPayload payload = VoidTrailPayload.fromPreset(UUID.randomUUID(), entity.getId(), actualScale, seedStart, seedEnd, actualPreset);

        Vec3 center = seedEnd;
        PacketDistributor.sendToPlayersNear(serverLevel, null, center.x, center.y, center.z, 128.0D, payload);
    }

    // 一次性拖尾触发（调用场景：Blink 等瞬移效果；from/to 是拖尾段两端，ownerEntityId 标记来源）。
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
        VoidTrailPayload payload = VoidTrailPayload.fromSegment(UUID.randomUUID(), ownerEntityId, actualScale, from, to, actualPreset);

        PacketDistributor.sendToPlayersNear(level, null, to.x, to.y, to.z, 128.0D, payload);
    }

    // 相位裂隙触发（调用场景：内部跟随实体入口；entity 同时作为来源和跟随目标）。
    private static void sendAttachedPhaseTear(Entity entity, VoidRingInstance.Preset preset) {
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

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        // 网络包注册（调用场景：NeoForge payload 注册事件；NETWORK_VERSION 改变时客户端/服务端协议会重新匹配）。
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        // 客户端相位裂隙事件：服务端发位置、缩放、跟随实体和环配置，客户端生成 VoidRing。
        registrar.playToClient(PhaseTearPayload.TYPE, PhaseTearPayload.STREAM_CODEC, ModNetworking::onPhaseTearClient);
        // 客户端拖尾事件：服务端发实体或线段信息，客户端生成持续拖尾或一次性拖尾。
        registrar.playToClient(VoidTrailPayload.TYPE, VoidTrailPayload.STREAM_CODEC, ModNetworking::onTrailClient);
        // 客户端黑洞事件：服务端发中心、缩放和黑洞配置，客户端生成黑洞视觉。
        registrar.playToClient(VoidBlackHolePayload.TYPE, VoidBlackHolePayload.STREAM_CODEC, ModNetworking::onBlackHoleClient);
        // 客户端持续循环声音事件：服务端发 start/stop，客户端按 id 开关循环音。
        registrar.playToClient(ContinuousLoopSoundPayload.TYPE, ContinuousLoopSoundPayload.STREAM_CODEC, ModNetworking::onLoopSoundClient);
        // 客户端能量 HUD 事件：服务端发百分比和显示状态，客户端刷新手表能量条。
        registrar.playToClient(EnergyHudPayload.TYPE, EnergyHudPayload.STREAM_CODEC, ModNetworking::onEnergyHudClient);
        // 客户端相位世界转场事件：服务端发源/目标维度，客户端播放遮罩转场。
        registrar.playToClient(PhaseWorldTransitionPayload.TYPE, PhaseWorldTransitionPayload.STREAM_CODEC, ModNetworking::onWorldMoveClient);
        // 客户端相位投影事件：服务端发投影快照，客户端缓存并触发区块重编译。
        registrar.playToClient(PhaseProjectionPayload.TYPE, PhaseProjectionPayload.STREAM_CODEC, ModNetworking::onPhaseProjectionClient);
        // 客户端炮台状态事件：服务端发玩家、开关、输入拦截、球数量和恢复色，客户端显示炮台球。
        registrar.playToClient(TurretStatePayload.TYPE, TurretStatePayload.STREAM_CODEC, ModNetworking::onTurretStateClient);
        // 客户端炮台射击事件：服务端发命中点和光束配置，客户端播放光束/炮口特效。
        registrar.playToClient(TurretShotFxPayload.TYPE, TurretShotFxPayload.STREAM_CODEC, ModNetworking::onTurretShotClient);
        // 客户端坐标绑定事件：服务端发绑定列表，客户端打开/刷新坐标制定器面板。
        registrar.playToClient(CoordinateBindingsPayload.TYPE, CoordinateBindingsPayload.STREAM_CODEC, ModNetworking::onBindingsClient);
        // 客户端区块映射器事件：服务端发状态快照，客户端打开/刷新区块映射器面板。
        registrar.playToClient(ChunkMapperStatusPayload.TYPE, ChunkMapperStatusPayload.STREAM_CODEC, ModNetworking::onMapperStatusClient);
        // 服务端模块使用事件：客户端发槽位，服务端按副手手表重新取模块并执行。
        registrar.playToServer(UseWatchModulePayload.TYPE, UseWatchModulePayload.STREAM_CODEC, ModNetworking::onUseWatchServer);
        // 服务端 Blink 释放事件：客户端发槽位、蓄力 tick 和目标点，服务端重新校验后闪现。
        registrar.playToServer(ReleaseBlinkModulePayload.TYPE, ReleaseBlinkModulePayload.STREAM_CODEC, ModNetworking::onReleaseBlinkServer);
        // 服务端手动炮台输入事件：客户端发左右键状态，服务端只更新手动炮台开火状态。
        registrar.playToServer(UseTurretShotPayload.TYPE, UseTurretShotPayload.STREAM_CODEC, ModNetworking::onUseTurretShotServer);
        // 服务端相位转场就绪事件：客户端白屏就绪后通知，服务端完成世界切换。
        registrar.playToServer(PhaseWorldTransitionReadyPayload.TYPE, PhaseWorldTransitionReadyPayload.STREAM_CODEC, ModNetworking::onWorldReadyServer);
        // 服务端删除绑定事件：客户端发面板中的绑定目标，服务端校验方块后删除。
        registrar.playToServer(RemoveCoordinateBindingPayload.TYPE, RemoveCoordinateBindingPayload.STREAM_CODEC, ModNetworking::onRemoveBindingServer);
        // 服务端请求绑定事件：客户端请求刷新面板，服务端校验方块后重发列表。
        registrar.playToServer(RequestCoordinateBindingsPayload.TYPE, RequestCoordinateBindingsPayload.STREAM_CODEC, ModNetworking::onRequestBindingsServer);
        // 服务端区块映射器档位事件：客户端发目标档位，服务端校验范围和方块后设置。
        registrar.playToServer(SetChunkMapperTierPayload.TYPE, SetChunkMapperTierPayload.STREAM_CODEC, ModNetworking::onSetMapperTierServer);
        // 服务端黑洞释放事件：客户端发槽位和目标点，服务端重新读取模块后释放黑洞。
        registrar.playToServer(ReleaseBlackHoleModulePayload.TYPE, ReleaseBlackHoleModulePayload.STREAM_CODEC, ModNetworking::onReleaseBlackHoleServer);
        registrar.playToServer(CancelTeleportModulePayload.TYPE, CancelTeleportModulePayload.STREAM_CODEC, ModNetworking::onCancelTeleportServer);
    }

    // 客户端发包入口（调用场景：按键、GUI、转场 ready；payload 是具体 C2S 自定义包）。
    public static void sendToServer(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }

    // Blink 释放接收（调用场景：客户端松开 Blink 蓄力键；slot 是模块槽，ticks 是蓄力时长，x/y/z 是客户端预览目标）。
    private static void onReleaseBlinkServer(
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

    // 模块使用接收（调用场景：客户端点击 F/G 或模块按钮；slot 是手表模块槽，服务端重新读取副手手表）。
    private static void onUseWatchServer(
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

    private static void onCancelTeleportServer(
            CancelTeleportModulePayload payload,
            IPayloadContext context
    ) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        int slot = payload.slot();
        if (slot < 0 || slot >= PhaseWatch.WATCH_MODULE_SLOT_COUNT) {
            return;
        }

        ItemStack watchStack = player.getOffhandItem();
        if (!(watchStack.getItem() instanceof PhaseWatch)) {
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
        contents.copyInto(items);

        ItemStack moduleStack = items.get(slot);
        if (!(moduleStack.getItem() instanceof TeleportVoidModule)) {
            return;
        }

        TeleportVoidModuleClock.cancelDeploy(player, slot);
    }

    // 黑洞释放接收（调用场景：客户端松开黑洞模块蓄力；slot 是模块槽，x/y/z 是释放目标点）。
    private static void onReleaseBlackHoleServer(
            ReleaseBlackHoleModulePayload payload,
            IPayloadContext context
    ) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        int slot = payload.slot();
        if (slot < 0 || slot >= PhaseWatch.WATCH_MODULE_SLOT_COUNT) {
            return;
        }

        ItemStack watchStack = player.getOffhandItem();
        if (!(watchStack.getItem() instanceof PhaseWatch)) {
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
        contents.copyInto(items);

        ItemStack moduleStack = items.get(slot);
        if (!(moduleStack.getItem() instanceof BlackHoleModule blackHoleModule)) {
            return;
        }

        blackHoleModule.releaseBlackHole(
                player,
                watchStack,
                moduleStack,
                slot,
                payload.x(),
                payload.y(),
                payload.z()
        );
    }

    // 拖尾接收（调用场景：服务端同步箭/瞬移拖尾；trackEntity 决定追踪实体还是生成一次性线段）。
    private static void onTrailClient(VoidTrailPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!payload.trackEntity()) {
                VoidTrailManager.addTrailSegment(payload.effectId(), payload.seedStart(), payload.seedEnd(), payload.scale(), payload.toPreset());
                return;
            }

            VoidTrailManager.trackEntity(payload.effectId(), payload.entityId(), payload.scale(), payload.toPreset(), payload.seedStart(), payload.seedEnd());
        });
    }

    // 能量 HUD 接收（调用场景：服务端同步手表能量；percent 是百分比，visible 控制客户端显示）。
    private static void onEnergyHudClient(EnergyHudPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> EnergyHud.update(payload.percent(), payload.visible()));
    }

    // 相位世界转场接收（调用场景：服务端准备换维度；source/target 维度用于客户端显示转场状态）。
    private static void onWorldMoveClient(PhaseWorldTransitionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PhaseWorldTransitionClient.beginLoadingTransition(
                payload.sourceDimension(),
                payload.targetDimension()
        ));
    }

    // 相位投影接收（调用场景：进入相位维度；snapshot 是源世界附近方块快照）。
    private static void onPhaseProjectionClient(PhaseProjectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PhaseProjectionClient.accept(payload.snapshot()));
    }

    // 炮台状态接收（调用场景：服务端开关手动/辅助炮台；playerId 定位玩家，active/blocksInput/emitterCount/healthVisual 控制客户端状态）。
    private static void onTurretStateClient(TurretStatePayload payload, IPayloadContext context) {
        // blocksInput 是手动/辅助炮台的客户端边界，不能只用 active 推断。
        context.enqueueWork(() -> PhaseEmitterClientManager.syncState(
                payload.playerId(),
                payload.active(),
                payload.blocksInput(),
                payload.emitterCount(),
                payload.healthVisual()
        ));
    }

    // 炮台射击接收（调用场景：服务端结算炮台命中后；targetX/Y/Z 是命中点，beamConfig 是客户端光束配置）。
    private static void onTurretShotClient(TurretShotFxPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PhaseEmitterClientManager.playShotFx(
                payload.effectId(),
                payload.playerId(),
                payload.emitterIndex(),
                new Vec3(payload.targetX(), payload.targetY(), payload.targetZ()),
                payload.toBeamConfig()
        ));
    }

    // 坐标绑定面板接收（调用场景：服务端发绑定列表；payload 带 owner 和输入/输出条目）。
    private static void onBindingsClient(CoordinateBindingsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> CoordinateBindingScreen.open(payload));
    }

    // 区块映射器面板接收（调用场景：服务端发映射器状态；payload 带档位、覆盖范围、能量和输入口状态）。
    private static void onMapperStatusClient(ChunkMapperStatusPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ChunkMapperStatusScreen.open(payload));
    }

    // 删除绑定接收（调用场景：客户端在绑定面板点删除；owner 是被操作方块，target 是要删除的另一端，outputList 指定列表方向）。
    private static void onRemoveBindingServer(RemoveCoordinateBindingPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        // 删除绑定必须重新按距离和维度找方块，不能信客户端传来的位置。
        BlockEntity ownerBlockEntity = getUseBlock(player, payload.owner());
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

    // 请求绑定接收（调用场景：客户端面板刷新；owner 是被查看的虚空能端点方块）。
    private static void onRequestBindingsServer(RequestCoordinateBindingsPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        BlockEntity blockEntity = getUseBlock(player, payload.owner());
        if (!(blockEntity instanceof VoidEnergyTransferBlockEntity owner)) {
            return;
        }

        sendCoordinateBindings(player, payload.owner(), owner);
    }

    // 区块映射器档位接收（调用场景：客户端点击档位按钮；owner 是映射器方块，tier 是目标档位）。
    private static void onSetMapperTierServer(SetChunkMapperTierPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        // 服务端再次限制档位范围，客户端按钮不能直接决定合法性。
        if (payload.tier() < 0 || payload.tier() > ChunkMapperBlock.MAX_TIER) {
            return;
        }
        BlockEntity blockEntity = getUseBlock(player, payload.owner());
        if (!(blockEntity instanceof ChunkMapperBlockEntity mapper)) {
            return;
        }

        mapper.setTier(payload.tier());
        sendChunkMapperStatus(player, mapper);
    }

    private static BlockEntity getUseBlock(ServerPlayer player, BoundVoidPosition position) {
        // 只允许操作同维度、近距离、已加载的方块，防止客户端远程乱改。
        if (!position.dimension().equals(player.level().dimension().identifier())) {
            return null;
        }
        if (player.blockPosition().distSqr(position.pos()) > 64.0D) {
            return null;
        }
        if (!player.level().isLoaded(position.pos())) {
            return null;
        }
        return player.level().getBlockEntity(position.pos());
    }

    // 黑洞接收（调用场景：服务端广播黑洞视觉；ownerEntityId 是来源，center/scale/config 控制客户端黑洞实例）。
    private static void onBlackHoleClient(VoidBlackHolePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                return;
            }

            VoidBlackHoleManager.addBlackHole(
                    payload.effectId(),
                    payload.ownerEntityId(),
                    new Vec3(payload.centerX(), payload.centerY(), payload.centerZ()),
                    payload.scale(),
                    payload.toConfig(),
                    payload.ageTicks()
            );
        });
    }

    // 持续循环声音接收（调用场景：服务端事件生命周期 start/stop；客户端负责真正循环和停止）。
    private static void onLoopSoundClient(ContinuousLoopSoundPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ContinuousLoopSoundClient.handle(payload));
    }

    private static boolean isNear(ServerPlayer player, Vec3 center, double rangeSqr) {
        if (player == null || center == null) {
            return false;
        }
        return player.distanceToSqr(center.x, center.y, center.z) <= rangeSqr;
    }

    private static Vec3 getTrailSeedStart(Entity entity, Vec3 seedEnd, double seedLength) {
        Vec3 motion = entity.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-8D) {
            return null;
        }

        if (seedLength > 0.0D) {
            return seedEnd.subtract(motion.normalize().scale(seedLength));
        }

        return seedEnd.subtract(motion);
    }

    // 相位裂隙接收（调用场景：服务端广播相位环；ownerEntityId 标记来源，trackedEntityId 存在时跟随实体）。
    private static void onPhaseTearClient(PhaseTearPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                return;
            }

            Entity trackedEntity = payload.hasTrackedEntity() ? mc.level.getEntity(payload.trackedEntityId()) : null;
            VoidRingInstance.Preset preset = payload.toPreset();
            Vec3 center = new Vec3(payload.centerX(), payload.centerY(), payload.centerZ());
            if (payload.hasTrackedEntity() && trackedEntity != null) {
                VoidRingManager.addTrackedRing(
                        payload.effectId(),
                        payload.ownerEntityId(),
                        payload.trackedEntityId(),
                        center,
                        payload.scale(),
                        preset,
                        payload.yaw(),
                        payload.ageTicks()
                );
            } else {
                VoidRingManager.addRing(
                        payload.effectId(),
                        payload.ownerEntityId(),
                        center,
                        payload.scale(),
                        preset,
                        payload.yaw(),
                        payload.ageTicks()
                );
            }

            if (trackedEntity instanceof LivingEntity livingEntity) {
                VoidClock.flashVoidEntity(livingEntity);
            }
        });
    }

    // 手动炮台输入接收（调用场景：客户端左右键状态变化；shooting 是左键，volleyShooting 是右键齐射）。
    private static void onUseTurretShotServer(
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

    // 相位世界 ready 接收（调用场景：客户端转场白屏已可见；服务端收到后完成 GoWorld 传送）。
    private static void onWorldReadyServer(
            PhaseWorldTransitionReadyPayload payload,
            IPayloadContext context
    ) {
        Player player = context.player();

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        GoWorld.finishMove(serverPlayer);
    }
}
