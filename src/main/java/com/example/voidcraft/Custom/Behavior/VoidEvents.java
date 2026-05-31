package com.example.voidcraft.Custom.Behavior;

import com.example.voidcraft.ModAttachments;
import com.example.voidcraft.VoidCraft;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@EventBusSubscriber
public class VoidEvents {
    public static final Identifier VOID_SPEED_ID = Identifier.fromNamespaceAndPath(VoidCraft.MODID, "void_speed");
    private static final double LOOK_CHECK_RANGE = 48.0D;
    private static final double LOOK_CHECK_RANGE_SQR = LOOK_CHECK_RANGE * LOOK_CHECK_RANGE;
    private static final double LOOK_POS_RANGE_SQR = 2.25D;
    private static final Map<UUID, AbilitySnapshot> VOID_ABILITY_SNAPSHOTS = new HashMap<>();
    private static final Map<UUID, PhysicsSnapshot> VOID_PHYSICS_SNAPSHOTS = new HashMap<>();
    @SubscribeEvent
    //虚空内无法受伤
    public static void noVoidHurt(EntityInvulnerabilityCheckEvent event){//这一大串相当于 造成伤害时
        if(event.getEntity() instanceof LivingEntity livingEntity){ //虚空状态挂在活体实体上，玩家和生物都走同一条规则
            if (!livingEntity.level().isClientSide()) {
                if (inPhase(livingEntity)) { //如果实体获取IN_PHASE的数据为true
                    event.setInvulnerable(true);    //开启无敌效果
                }
            }
        }
    }
    @SubscribeEvent
    public static void onVoidJump(net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
        LivingEntity entity = event.getEntity();

        if (!inPhase(entity)) {
            return;
        }

        if (!(entity instanceof Player player)) {
            return;
        }

        Vec3 input = new Vec3(player.xxa, 0.0D, player.zza);
        if (input.lengthSqr() <= 1.0E-6D) {
            return;
        }

        Vec3 motion = player.getDeltaMovement();
        player.moveRelative(player.getData(ModAttachments.VOID_SPEED.get()), input);
        Vec3 newMotion = player.getDeltaMovement();
        player.setDeltaMovement(newMotion.x, motion.y, newMotion.z);
        player.hurtMarked = true;
    }
    @SubscribeEvent
    public static void noVoidAttack(AttackEntityEvent event) {
        Player player = event.getEntity();

        if (inPhase(player)) {
            event.setCanceled(true);
        }
    }
    @SubscribeEvent
    public static void noVoidLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();

        if (inPhase(player)) {
            event.setCanceled(true);
        }
    }
    @SubscribeEvent
    //玩家在虚空内无法交互方块
    public static void noVoidTouch(PlayerInteractEvent.RightClickBlock event){
        if(event.getEntity() instanceof Player player){
            if(inPhase(player)){
                event.setCanceled(true);
            }
        }
    }
    @SubscribeEvent
    //玩家在虚空内无法交互道具
    public static void noVoidTouch(PlayerInteractEvent.RightClickItem event){
        if(event.getEntity() instanceof Player player){
            if(inPhase(player)){
                event.setCanceled(true);
            }
        }
    }
    @SubscribeEvent
    //玩家在虚空内无法交互实体
    public static void noVoidTouch(PlayerInteractEvent.EntityInteract event){
        if(event.getEntity() instanceof Player player){
            if(inPhase(player)){
                event.setCanceled(true);
            }
        }
    }
    @SubscribeEvent
    //怪物无法看见进入虚空的活体实体
    public static void noMobTarget(LivingChangeTargetEvent event){ //生物准备更换目标时
        if(event.getNewAboutToBeSetTarget() instanceof LivingEntity target
                && inPhase(target)) {
            event.setCanceled(true);
        }
    }
    @SubscribeEvent
    //让原来锁定虚空实体的怪物让他们听话别锁定
    public static void clearMobTarget(EntityTickEvent.Post event){
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide()) {
            return;
        }

        LivingEntity target = mob.getTarget();
        if (target != null && inPhase(target)) {
            mob.setTarget(null);
        }

        clearBrainTarget(mob);
        clearMobLook(mob);
    }

    private static void clearBrainTarget(Mob mob) {
        Brain<?> brain = mob.getBrain();
        Optional<LivingEntity> target = brain.getMemoryInternal(MemoryModuleType.ATTACK_TARGET);
        if (target != null && target.isPresent() && inPhase(target.get())) {
            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
        }
    }

    private static void clearMobLook(Mob mob) {
        clearBrainLook(mob);

        if (!mob.getLookControl().isLookingAtTarget()) {
            return;
        }

        Vec3 lookPos = new Vec3(
                mob.getLookControl().getWantedX(),
                mob.getLookControl().getWantedY(),
                mob.getLookControl().getWantedZ()
        );

        AABB area = mob.getBoundingBox().inflate(LOOK_CHECK_RANGE);
        for (LivingEntity entity : mob.level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (entity == mob || !inPhase(entity)) {
                continue;
            }

            if (entity.distanceToSqr(mob) > LOOK_CHECK_RANGE_SQR) {
                continue;
            }

            if (entity.getEyePosition().distanceToSqr(lookPos) <= LOOK_POS_RANGE_SQR
                    || entity.position().distanceToSqr(lookPos) <= LOOK_POS_RANGE_SQR) {
                mob.getLookControl().setLookAt(mob.getX(), mob.getEyeY(), mob.getZ());
                return;
            }
        }
    }

    private static void clearBrainLook(Mob mob) {
        Brain<?> brain = mob.getBrain();
        Optional<PositionTracker> target = brain.getMemoryInternal(MemoryModuleType.LOOK_TARGET);
        if (target == null || target.isEmpty()) {
            return;
        }

        PositionTracker tracker = target.get();
        if (tracker instanceof EntityTracker entityTracker
                && entityTracker.getEntity() instanceof LivingEntity livingEntity
                && inPhase(livingEntity)) {
            brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
        }
    }
    @SubscribeEvent
    //虚空内无法溺死
    public static void keepAir(EntityTickEvent.Post event){
        if (!(event.getEntity() instanceof LivingEntity entity) || entity.level().isClientSide()) {
            return;
        }

        if(inPhase(entity)){ //如果实体在相位内
            entity.setAirSupply(entity.getMaxAirSupply());   //将氧气值锁定满值
        }
    }

    @SubscribeEvent
    public static void noPhaseSwim(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }

        if (!inPhase(entity)) {
            return;
        }

        entity.clearFire();
        entity.setSharedFlagOnFire(false);
        if (entity.isSwimming()) {
            entity.setSwimming(false);
        }
    }

    @SubscribeEvent
    public static void tickVoidPhysics(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }

        if(inVoid(entity)){
            setVoidPhysics(entity);
        }
        else{
            restoreVoidPhysics(entity);
        }
    }

    @SubscribeEvent
    public static void tickVoidFly(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if(inVoid(player)){
            setVoidFly(player);
        }
        else{
            restoreVoidFly(player);
        }
    }

    @SubscribeEvent
    public static void clearVoidFly(PlayerEvent.PlayerLoggedOutEvent event) {
        if(event.getEntity() instanceof ServerPlayer player){
            restoreVoidFly(player);
            restoreVoidPhysics(player);
        }
    }

    @SubscribeEvent
    public static void clearVoidPhysics(EntityLeaveLevelEvent event) {
        if(event.getEntity() instanceof LivingEntity entity){
            restoreVoidPhysics(entity);
        }
    }

    private static void setVoidPhysics(LivingEntity entity) {
        VOID_PHYSICS_SNAPSHOTS.computeIfAbsent(entity.getUUID(), ignored -> PhysicsSnapshot.capture(entity));

        entity.noPhysics = true;
        entity.setNoGravity(true);
        entity.setOnGround(false);
        entity.resetFallDistance();
    }

    private static void restoreVoidPhysics(LivingEntity entity) {
        PhysicsSnapshot snapshot = VOID_PHYSICS_SNAPSHOTS.remove(entity.getUUID());
        if(snapshot == null){
            return;
        }

        entity.noPhysics = snapshot.noPhysics;
        entity.setNoGravity(snapshot.noGravity);
    }

    private static void setVoidFly(ServerPlayer player) {
        VOID_ABILITY_SNAPSHOTS.computeIfAbsent(player.getUUID(), ignored -> AbilitySnapshot.capture(player));

        Abilities abilities = player.getAbilities();
        boolean changed = false;
        if(!abilities.mayfly){
            abilities.mayfly = true;
            changed = true;
        }
        if(!abilities.flying){
            abilities.flying = true;
            changed = true;
        }

        player.setOnGround(false);
        player.resetFallDistance();

        if(changed){
            player.onUpdateAbilities();
        }
    }

    private static void restoreVoidFly(ServerPlayer player) {
        AbilitySnapshot snapshot = VOID_ABILITY_SNAPSHOTS.remove(player.getUUID());
        if(snapshot == null){
            return;
        }

        Abilities abilities = player.getAbilities();
        boolean mayfly = snapshot.mayfly;
        boolean flying = snapshot.flying;
        if(snapshot.forceRemoveOnExit && needDropFly(player)){
            mayfly = false;
            flying = false;
        }

        boolean changed = abilities.mayfly != mayfly || abilities.flying != flying;
        abilities.mayfly = mayfly;
        abilities.flying = flying;
        if(changed){
            player.onUpdateAbilities();
        }
    }

    private static boolean needDropFly(ServerPlayer player) {
        GameType gameMode = player.gameMode.getGameModeForPlayer();
        return gameMode != GameType.SPECTATOR && !gameMode.isCreative();
    }

    private record AbilitySnapshot(boolean mayfly, boolean flying, boolean forceRemoveOnExit) {
        private static AbilitySnapshot capture(ServerPlayer player) {
            Abilities abilities = player.getAbilities();
            return new AbilitySnapshot(abilities.mayfly, abilities.flying, needDropFly(player));
        }
    }

    private record PhysicsSnapshot(boolean noPhysics, boolean noGravity) {
        private static PhysicsSnapshot capture(LivingEntity entity) {
            return new PhysicsSnapshot(entity.noPhysics, entity.isNoGravity());
        }
    }

    @SubscribeEvent
    //禁止虚空内拾取物品
    public static void noVoidPickup(ItemEntityPickupEvent.Pre event) {//拾取物品前
        if (inPhase(event.getPlayer())) {//如果玩家在相位里
            event.setCanPickup(TriState.FALSE);//禁止玩家拾取物品
        }
    }

    private static boolean inPhase(LivingEntity entity) {
        return entity.getData(ModAttachments.IN_PHASE.get())
                || entity.getData(ModAttachments.IN_VOID.get());
    }

    private static boolean inVoid(LivingEntity entity) {
        return entity.getData(ModAttachments.IN_VOID.get());
    }
}
