package com.example.voidcraft.Custom.Clock.ModuleSkill;

import com.example.voidcraft.Custom.Clock.Clock;
import com.example.voidcraft.Custom.Clock.ModuleSkillClock;
import com.example.voidcraft.Custom.Clock.VoidClock;
import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.Effect.VoidTrailInstance;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.TeleportVoidModule;
import com.example.voidcraft.Item.custom.PhaseWatch;
import com.example.voidcraft.Network.ModNetworking;
import com.example.voidcraft.Sound.ModSound;
import com.example.voidcraft.VoidCraft;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber
public class TeleportVoidModuleClock {
    private static final int RECORD_INTERVAL_TICKS = 2;
    private static final int TOUCH_COOLDOWN_TICKS = 40;
    private static final double MIN_POINT_DISTANCE = 0.05D;
    private static final double MIN_GATE_DISTANCE = 0.25D;
    private static final double GATE_HALF_WIDTH = 0.55D;
    private static final double GATE_HALF_HEIGHT = 1.0D;
    private static final float GATE_SCALE = 1.0F;
    private static final int GATE_SYNC_INTERVAL_TICKS = 20;
    private static final VoidTrailInstance.Preset MOVE_TRAIL = makeMoveTrail();
    private static final VoidRingInstance.Preset MOVE_LIGHT = makeMoveLight();
    private static final Identifier DEPLOY_SPEED_ID = Identifier.fromNamespaceAndPath(VoidCraft.MODID, "teleport_deploy_speed");
    private static final Map<UUID, Map<Integer, DeployData>> DEPLOYS = new HashMap<>();
    private static final Map<UUID, GateData> GATES = new HashMap<>();
    private static final Map<UUID, MoveData> MOVES = new HashMap<>();
    private static final Map<UUID, Boolean> TOUCH_COOLDOWN = new HashMap<>();

    public static boolean hasDeploy(ServerPlayer player, int slot) {
        Map<Integer, DeployData> data = DEPLOYS.get(player.getUUID());
        return data != null && data.containsKey(slot);
    }

    public static boolean startDeploy(ServerPlayer player, int slot, TeleportVoidModule.Stats stats) {
        if (player == null || stats == null || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        if (hasAnyDeploy(player)) {
            return false;
        }

        Vec3 start = player.position();
        List<Vec3> points = new ArrayList<>();
        points.add(start);
        DeployData data = new DeployData(player.getUUID(), level, slot, stats, getViewYaw(player), stats.deployEnergy(), points);
        DEPLOYS.computeIfAbsent(player.getUUID(), uuid -> new HashMap<>()).put(slot, data);
        setDeploySpeed(player, stats.deploySpeed());
        ModSound.playTeleportDeployStart(level, player);
        sendEnergyTip(player, data, true);
        return true;
    }

    public static boolean endDeploy(ServerPlayer player, int slot) {
        DeployData data = getDeploy(player, slot);
        if (data == null || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        if (level != data.level) {
            cancelDeploy(player, slot);
            return false;
        }
        if (!tryAddPoint(data, player.position(), true) || !hasPath(data.points)) {
            sendEnergyTip(player, data, true);
            return false;
        }
        boolean cooldownReady = ModuleSkillClock.canUseNow(player, slot);
        if (!cooldownReady && !ModuleSkillClock.tryUseEnergy(player, data.stats.burstEnergyCost())) {
            return false;
        }

        removeDeploy(player, slot);
        ModuleSkillClock.startRunCooldown(player, slot, data.stats.gateTicks(), data.stats.burstCooldownTicks());
        addGates(data, getViewYaw(player));
        addTouchCooldown(player);
        ModSound.playTeleportDeployEnd(level, player);
        return true;
    }

    public static void cancelDeploy(ServerPlayer player, int slot) {
        removeDeploy(player, slot);
    }

    public static boolean isMoving(Entity entity) {
        return entity != null && MOVES.containsKey(entity.getUUID());
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        tickDeploys();
        tickGates();
        tickMoves();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearPlayer(player);
        }
    }

    @SubscribeEvent
    public static void clearRemovedEntity(EntityLeaveLevelEvent event) {
        UUID entityId = event.getEntity().getUUID();
        stopMove(MOVES.remove(entityId));
        TOUCH_COOLDOWN.remove(entityId);
    }

    public static void clearPlayer(ServerPlayer player) {
        if (player == null) {
            return;
        }
        clearDeploySpeed(player);
        DEPLOYS.remove(player.getUUID());
        stopMove(MOVES.remove(player.getUUID()));
        TOUCH_COOLDOWN.remove(player.getUUID());
    }

    private static DeployData getDeploy(ServerPlayer player, int slot) {
        if (player == null) {
            return null;
        }
        Map<Integer, DeployData> data = DEPLOYS.get(player.getUUID());
        if (data == null) {
            return null;
        }
        return data.get(slot);
    }

    private static void removeDeploy(ServerPlayer player, int slot) {
        if (player == null) {
            return;
        }
        Map<Integer, DeployData> data = DEPLOYS.get(player.getUUID());
        if (data == null) {
            return;
        }

        data.remove(slot);
        if (data.isEmpty()) {
            DEPLOYS.remove(player.getUUID());
            clearDeploySpeed(player);
        }
    }

    private static boolean hasAnyDeploy(ServerPlayer player) {
        Map<Integer, DeployData> data = DEPLOYS.get(player.getUUID());
        return data != null && !data.isEmpty();
    }

    private static boolean hasTeleportModule(ServerPlayer player, int slot) {
        if (slot < 0 || slot >= PhaseWatch.WATCH_MODULE_SLOT_COUNT) {
            return false;
        }
        ItemStack watchStack = player.getOffhandItem();
        if (!(watchStack.getItem() instanceof PhaseWatch)) {
            return false;
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

        return items.get(slot).getItem() instanceof TeleportVoidModule;
    }

    private static void tickDeploys() {
        Iterator<Map.Entry<UUID, Map<Integer, DeployData>>> playerIterator = DEPLOYS.entrySet().iterator();
        while (playerIterator.hasNext()) {
            Map.Entry<UUID, Map<Integer, DeployData>> playerEntry = playerIterator.next();
            ServerPlayer player = findPlayer(playerEntry.getValue());
            if (player == null || player.isRemoved() || player.isDeadOrDying()) {
                clearDeploySpeed(player);
                playerIterator.remove();
                continue;
            }

            Iterator<Map.Entry<Integer, DeployData>> deployIterator = playerEntry.getValue().entrySet().iterator();
            while (deployIterator.hasNext()) {
                DeployData data = deployIterator.next().getValue();
                if (player.level() != data.level || !hasTeleportModule(player, data.slot)) {
                    deployIterator.remove();
                    continue;
                }

                setDeploySpeed(player, data.stats.deploySpeed());
                data.ticks++;
                if (data.ticks % RECORD_INTERVAL_TICKS == 0) {
                    tryAddPoint(data, player.position(), false);
                    sendEnergyTip(player, data, false);
                }
            }

            if (playerEntry.getValue().isEmpty()) {
                clearDeploySpeed(player);
                playerIterator.remove();
            }
        }
    }

    private static void tickGates() {
        Iterator<Map.Entry<UUID, GateData>> iterator = GATES.entrySet().iterator();
        while (iterator.hasNext()) {
            GateData gate = iterator.next().getValue();
            gate.ticksLeft--;
            if (gate.ticksLeft <= 0) {
                iterator.remove();
                continue;
            }
            if (!gate.reverse && gate.ticksLeft % GATE_SYNC_INTERVAL_TICKS == 0) {
                syncGatePair(null, gate);
            }
            touchGate(gate);
        }
    }

    private static void tickMoves() {
        Iterator<Map.Entry<UUID, MoveData>> iterator = MOVES.entrySet().iterator();
        while (iterator.hasNext()) {
            MoveData data = iterator.next().getValue();
            Entity entity = data.entity;
            if (entity == null || entity.isRemoved() || entity.level() != data.level) {
                stopMove(data);
                iterator.remove();
                continue;
            }

            data.distance = Math.min(data.pathLength, data.distance + data.blocksPerTick);
            if (data.distance >= data.pathLength) {
                Vec3 end = data.points.get(data.points.size() - 1);
                sendMoveTrail(data, entity, data.lastTrailPos, entity.position());
                sendMoveTrail(data, entity, entity.position(), end);
                setEntityPos(entity, end);
                entity.setDeltaMovement(Vec3.ZERO);
                entity.hurtMarked = true;
                playMoveLight(entity);
                addTouchCooldown(entity);
                stopMove(data);
                iterator.remove();
                continue;
            }

            Vec3 pos = getPoint(data, data.distance);
            Vec3 old = data.lastTrailPos;
            Vec3 current = entity.position();
            setMoveSpeed(data, pos);
            sendMoveTrail(data, entity, old, current);
            data.lastTrailPos = current;
            keepMoveVoid(entity);
        }
    }

    private static void touchGate(GateData gate) {
        AABB box = new AABB(gate.center, gate.center).inflate(GATE_HALF_WIDTH, GATE_HALF_HEIGHT, GATE_HALF_WIDTH);
        List<Entity> entities = gate.level.getEntities(
                (Entity) null,
                box,
                entity -> canTouch(entity)
        );

        for (Entity entity : entities) {
            startMove(entity, gate);
        }
    }

    private static boolean canTouch(Entity entity) {
        if (entity == null || entity.isRemoved()) {
            return false;
        }
        if (entity instanceof Player player && player.isSpectator()) {
            return false;
        }
        UUID entityId = entity.getUUID();
        return !TOUCH_COOLDOWN.containsKey(entityId) && !MOVES.containsKey(entityId);
    }

    private static void startMove(Entity entity, GateData gate) {
        List<Vec3> points = gate.reverse ? getReversePoints(gate.points) : gate.points;
        if (!hasPath(points)) {
            return;
        }

        ModSound.playTeleportPortalEnter(gate.level, gate.center);
        playMoveLight(entity);
        MoveData data = new MoveData(entity, gate.level, points, gate.blocksPerSecond / 20.0D);
        if (data.pathLength <= MIN_GATE_DISTANCE) {
            return;
        }
        keepMovePhysics(data);
        MOVES.put(entity.getUUID(), data);
    }

    private static void addGates(DeployData data, float endYaw) {
        List<Vec3> points = List.copyOf(data.points);
        Vec3 startCenter = getGateCenter(points.get(0));
        Vec3 endCenter = getGateCenter(points.get(points.size() - 1));
        UUID startId = UUID.randomUUID();
        UUID endId = UUID.randomUUID();
        GateData startGate = addGate(data.level, startId, startCenter, data.startYaw, endId, endCenter, endYaw, points, false, data.stats);
        addGate(data.level, endId, endCenter, endYaw, startId, startCenter, data.startYaw, points, true, data.stats);
        ServerPlayer owner = data.level.getServer().getPlayerList().getPlayer(data.playerId);
        syncGatePair(owner, startGate);
    }

    private static GateData addGate(
            ServerLevel level,
            UUID effectId,
            Vec3 center,
            float yaw,
            UUID otherEffectId,
            Vec3 otherCenter,
            float otherYaw,
            List<Vec3> points,
            boolean reverse,
            TeleportVoidModule.Stats stats
    ) {
        GateData gate = new GateData(level, effectId, center, yaw, otherEffectId, otherCenter, otherYaw, points, reverse, stats.gateTicks(), stats.blocksPerSecond());
        GATES.put(effectId, gate);
        return gate;
    }

    private static void syncGatePair(ServerPlayer owner, GateData gate) {
        if (gate == null) {
            return;
        }

        VoidRingInstance.Preset light = makeGateLight(gate.totalTicks);
        int ageTicks = Math.max(0, gate.totalTicks - gate.ticksLeft);
        ModNetworking.sendPhaseTearPair(
                owner,
                gate.level,
                gate.effectId,
                ageTicks,
                gate.center,
                GATE_SCALE,
                gate.yaw,
                gate.otherEffectId,
                ageTicks,
                gate.otherCenter,
                GATE_SCALE,
                gate.otherYaw,
                light
        );
    }

    private static void addTouchCooldown(Entity entity) {
        UUID entityId = entity.getUUID();
        TOUCH_COOLDOWN.put(entityId, true);
        Clock.addClock(TOUCH_COOLDOWN_TICKS, () -> TOUCH_COOLDOWN.remove(entityId));
    }

    private static void sendEnergyTip(ServerPlayer player, DeployData data, boolean force) {
        if (player == null || data == null || data.stats.deployEnergy() <= 0.0D) {
            return;
        }

        int percent = getEnergyPercent(data);
        if (!force && data.lastEnergyPercent == percent) {
            return;
        }

        data.lastEnergyPercent = percent;
        player.displayClientMessage(
                Component.translatable("message.void_craft.teleport_void_module.deploy_energy", percent),
                true
        );
    }

    private static int getEnergyPercent(DeployData data) {
        return Math.max(0, Math.min(100, (int) Math.round(data.energyLeft * 100.0D / data.stats.deployEnergy())));
    }

    private static void sendMoveTrail(MoveData data, Entity entity, Vec3 from, Vec3 to) {
        if (data == null || entity == null || from == null || to == null) {
            return;
        }

        ModNetworking.sendTrailSegment(data.level, entity.getId(), from, to, getEntityEffectScale(entity), MOVE_TRAIL);
    }

    private static void playMoveLight(Entity entity) {
        if (entity == null || !(entity.level() instanceof ServerLevel level)) {
            return;
        }

        ModNetworking.sendPhaseTearAt(level, entity.getBoundingBox().getCenter(), getEntityEffectScale(entity), MOVE_LIGHT);
    }

    private static void setMoveSpeed(MoveData data, Vec3 pos) {
        Entity entity = data.entity;
        keepMovePhysics(data);
        Vec3 move = pos.subtract(entity.position());
        double maxMove = data.blocksPerTick * 1.5D;
        if (move.lengthSqr() > maxMove * maxMove) {
            move = move.normalize().scale(maxMove);
        }
        entity.setDeltaMovement(move);
        entity.resetFallDistance();
        entity.hurtMarked = true;
    }

    private static void keepMovePhysics(MoveData data) {
        Entity entity = data.entity;
        entity.noPhysics = true;
        entity.setNoGravity(true);
        keepMoveVoid(entity);
    }

    private static void stopMove(MoveData data) {
        if (data == null || data.entity == null) {
            return;
        }

        data.entity.noPhysics = data.oldNoPhysics;
        data.entity.setNoGravity(data.oldNoGravity);
    }

    private static void keepMoveVoid(Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            VoidClock.setVoidTicks(livingEntity, 2);
        }
    }

    private static float getEntityEffectScale(Entity entity) {
        return (float) Math.max(0.25D, Math.min(3.0D, Math.max(entity.getBbHeight() * 0.5D, entity.getBbWidth() * 0.90D)));
    }

    private static boolean tryAddPoint(DeployData data, Vec3 point, boolean endPoint) {
        Vec3 last = data.points.get(data.points.size() - 1);
        double distance = last.distanceTo(point);
        if (distance <= MIN_POINT_DISTANCE) {
            return true;
        }
        if (data.energyLeft <= 0.0D) {
            return false;
        }
        if (distance > data.energyLeft) {
            if (endPoint) {
                return false;
            }
            double progress = data.energyLeft / distance;
            point = last.lerp(point, progress);
            distance = data.energyLeft;
        }

        data.points.add(point);
        data.energyLeft -= distance;
        return true;
    }

    private static boolean hasPath(List<Vec3> points) {
        return points.size() >= 2 && points.get(0).distanceTo(points.get(points.size() - 1)) > MIN_GATE_DISTANCE;
    }

    private static List<Vec3> getReversePoints(List<Vec3> points) {
        List<Vec3> reverse = new ArrayList<>(points);
        Collections.reverse(reverse);
        return reverse;
    }

    private static Vec3 getPoint(MoveData data, double distance) {
        if (distance <= 0.0D) {
            return data.points.get(0);
        }
        if (distance >= data.pathLength) {
            return data.points.get(data.points.size() - 1);
        }

        while (data.segmentIndex + 1 < data.distances.length - 1
                && data.distances[data.segmentIndex + 1] < distance) {
            data.segmentIndex++;
        }

        int startIndex = data.segmentIndex;
        int endIndex = startIndex + 1;
        double startDistance = data.distances[startIndex];
        double endDistance = data.distances[endIndex];
        double segmentLength = Math.max(1.0E-6D, endDistance - startDistance);
        double progress = (distance - startDistance) / segmentLength;
        return data.points.get(startIndex).lerp(data.points.get(endIndex), progress);
    }

    private static void setEntityPos(Entity entity, Vec3 pos) {
        if (entity instanceof ServerPlayer player) {
            player.connection.teleport(pos.x, pos.y, pos.z, player.getYRot(), player.getXRot());
            return;
        }
        entity.teleportTo(pos.x, pos.y, pos.z);
    }

    private static Vec3 getGateCenter(Vec3 pos) {
        return pos.add(0.0D, 1.0D, 0.0D);
    }

    private static float getViewYaw(Entity entity) {
        return entity == null ? 0.0F : (float) Math.toRadians(-entity.getYRot());
    }

    private static ServerPlayer findPlayer(Map<Integer, DeployData> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        DeployData first = data.values().iterator().next();
        return first.level.getServer().getPlayerList().getPlayer(first.playerId);
    }

    private static void setDeploySpeed(ServerPlayer player, float speed) {
        AttributeInstance attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) {
            return;
        }
        attribute.addOrUpdateTransientModifier(new AttributeModifier(
                DEPLOY_SPEED_ID,
                Math.max(0.0F, speed),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));
    }

    private static void clearDeploySpeed(Player player) {
        if (!(player instanceof LivingEntity livingEntity)) {
            return;
        }
        AttributeInstance attribute = livingEntity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute != null) {
            attribute.removeModifier(DEPLOY_SPEED_ID);
        }
    }

    private static VoidRingInstance.Preset makeGateLight(int durationTicks) {
        return VoidRingInstance.Preset.builder()
                .durationTicks(durationTicks)
                .centerYOffset(0.0F)
                .followCameraYaw(false)
                .followCameraPitch(false)
                .distortionFollowCameraYaw(false)
                .distortionFollowCameraPitch(false)
                .startHalfHeight(1.0F)
                .peakHalfHeight(1.0F)
                .endHalfHeight(1.0F)
                .startHalfWidth(0.50F)
                .peakHalfWidth(0.50F)
                .endHalfWidth(0.50F)
                .peakHoldTicks(durationTicks)
                .glowAlpha(0.80F)
                .glowWidthScale(1.15F)
                .glowHeightScale(1.10F)
                .shaderGlowWidthScale(1.35F)
                .shaderGlowHeightScale(1.25F)
                .shaderCompatOuterGlowGain(1.70F)
                .shaderCompatCoreGain(1.35F)
                .shaderCompatLineGain(1.40F)
                .shaderCompatBloomGain(1.60F)
                .shaderCompatBloomAlphaScale(0.80F)
                .coreAlpha(0.92F)
                .distortionAlpha(1.80F)
                .lineAlpha(0.72F)
                .color(0xFFFFFF)
                .filledFadeStart(0.48F)
                .distortionThickness(4.20F)
                .distortionAmplitude(11.70F)
                .distortionWidthScale(1.50F)
                .distortionHeightScale(1.50F)
                .noiseFrequency(8.00F)
                .noiseScrollSpeed(5.80F)
                .build();
    }

    private static VoidTrailInstance.Preset makeMoveTrail() {
        return VoidTrailInstance.Preset.builder()
                .startDelayTicks(0)
                .sampleIntervalTicks(1)
                .lifetimeTicks(18)
                .centerYOffset(0.50F)
                .minMoveDistance(0.01F)
                .pointSpacing(0.05F)
                .maxInterpolationSteps(20)
                .width(0.16F)
                .height(0.52F)
                .tailWidthScale(0.62F)
                .tailHeightScale(0.82F)
                .edgeFadeRatio(0.76F)
                .ribbonFadeSegments(6)
                .headFadeRatio(0.20F)
                .glowWidthMultiplier(1.70F)
                .glowHeightMultiplier(1.18F)
                .alpha(0.46F)
                .glowAlpha(0.44F)
                .shaderCompatBloomAlphaScale(0.85F)
                .shaderCompatBloomWidthScale(1.25F)
                .shaderCompatBloomHeightScale(1.12F)
                .shaderCompatBloomTailWhiten(0.85F)
                .shaderCompatBloomHeadWhiten(0.95F)
                .tailColor(0xBFEFFF)
                .headColor(0xFFFFFF)
                .build();
    }

    private static VoidRingInstance.Preset makeMoveLight() {
        return VoidRingInstance.Preset.builder()
                .durationTicks(10)
                .peakHoldTicks(2)
                .centerYOffset(0.0F)
                .followCameraYaw(true)
                .followCameraPitch(true)
                .distortionFollowCameraYaw(true)
                .distortionFollowCameraPitch(true)
                .startHalfHeight(0.36F)
                .peakHalfHeight(1.0F)
                .endHalfHeight(0.20F)
                .startHalfWidth(0.36F)
                .peakHalfWidth(1.0F)
                .endHalfWidth(0.20F)
                .glowAlpha(0.92F)
                .glowWidthScale(1.55F)
                .glowHeightScale(1.55F)
                .shaderGlowWidthScale(2.10F)
                .shaderGlowHeightScale(2.10F)
                .shaderCompatOuterGlowGain(2.20F)
                .shaderCompatCoreGain(1.80F)
                .shaderCompatLineGain(2.00F)
                .shaderCompatBloomGain(2.35F)
                .shaderCompatBloomAlphaScale(0.92F)
                .coreAlpha(1.0F)
                .distortionAlpha(3.20F)
                .lineAlpha(0.96F)
                .color(0xFFFFFF)
                .filledFadeStart(0.40F)
                .swirlStrength(0.20F)
                .suctionStrength(0.40F)
                .distortionThickness(5.80F)
                .distortionAmplitude(13.50F)
                .distortionWidthScale(4.0F)
                .distortionHeightScale(4.0F)
                .noiseFrequency(4.20F)
                .noiseScrollSpeed(5.40F)
                .build();
    }

    private static class DeployData {
        private final UUID playerId;
        private final ServerLevel level;
        private final int slot;
        private final TeleportVoidModule.Stats stats;
        private final float startYaw;
        private final List<Vec3> points;
        private double energyLeft;
        private int lastEnergyPercent = -1;
        private int ticks;

        private DeployData(
                UUID playerId,
                ServerLevel level,
                int slot,
                TeleportVoidModule.Stats stats,
                float startYaw,
                double energyLeft,
                List<Vec3> points
        ) {
            this.playerId = playerId;
            this.level = level;
            this.slot = slot;
            this.stats = stats;
            this.startYaw = startYaw;
            this.energyLeft = energyLeft;
            this.points = points;
        }
    }

    private static class GateData {
        private final ServerLevel level;
        private final UUID effectId;
        private final Vec3 center;
        private final float yaw;
        private final UUID otherEffectId;
        private final Vec3 otherCenter;
        private final float otherYaw;
        private final List<Vec3> points;
        private final boolean reverse;
        private final float blocksPerSecond;
        private final int totalTicks;
        private int ticksLeft;

        private GateData(
                ServerLevel level,
                UUID effectId,
                Vec3 center,
                float yaw,
                UUID otherEffectId,
                Vec3 otherCenter,
                float otherYaw,
                List<Vec3> points,
                boolean reverse,
                int ticksLeft,
                float blocksPerSecond
        ) {
            this.level = level;
            this.effectId = effectId;
            this.center = center;
            this.yaw = yaw;
            this.otherEffectId = otherEffectId;
            this.otherCenter = otherCenter;
            this.otherYaw = otherYaw;
            this.points = points;
            this.reverse = reverse;
            this.totalTicks = ticksLeft;
            this.ticksLeft = ticksLeft;
            this.blocksPerSecond = blocksPerSecond;
        }
    }

    private static class MoveData {
        private final Entity entity;
        private final ServerLevel level;
        private final List<Vec3> points;
        private final double blocksPerTick;
        private final double[] distances;
        private final double pathLength;
        private final boolean oldNoPhysics;
        private final boolean oldNoGravity;
        private Vec3 lastTrailPos;
        private double distance;
        private int segmentIndex;

        private MoveData(Entity entity, ServerLevel level, List<Vec3> points, double blocksPerTick) {
            this.entity = entity;
            this.level = level;
            this.points = points;
            this.blocksPerTick = Math.max(0.05D, blocksPerTick);
            this.distances = makeDistances(points);
            this.pathLength = this.distances[this.distances.length - 1];
            this.oldNoPhysics = entity.noPhysics;
            this.oldNoGravity = entity.isNoGravity();
            this.lastTrailPos = entity.position();
            this.distance = 0.0D;
        }

        private static double[] makeDistances(List<Vec3> points) {
            double[] distances = new double[points.size()];
            for (int i = 1; i < points.size(); i++) {
                distances[i] = distances[i - 1] + points.get(i - 1).distanceTo(points.get(i));
            }
            return distances;
        }
    }
}
