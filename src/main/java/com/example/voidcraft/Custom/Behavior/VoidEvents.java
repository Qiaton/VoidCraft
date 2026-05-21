package com.example.voidcraft.Custom.Behavior;

import com.example.voidcraft.ModAttachments;
import com.example.voidcraft.VoidCraft;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber
public class VoidEvents {
    public static final ResourceLocation VOID_SPEED_ID = ResourceLocation.fromNamespaceAndPath(VoidCraft.MODID, "void_speed");
    @SubscribeEvent
    //虚空内无法受伤
    public static void noVoidHurt(EntityInvulnerabilityCheckEvent event){//这一大串相当于 造成伤害时
        if(event.getEntity() instanceof LivingEntity livingEntity){ //虚空状态挂在活体实体上，玩家和生物都走同一条规则
            if (!livingEntity.level().isClientSide()) {
                if (livingEntity.getData(ModAttachments.IN_VOID.get())) { //如果实体获取IN_VOID的数据为true
                    event.setInvulnerable(true);    //开启无敌效果
                }
            }
        }
    }
    @SubscribeEvent
    public static void onVoidJump(net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
        LivingEntity entity = event.getEntity();

        if (!entity.getData(ModAttachments.IN_VOID.get())) {
            return;
        }

        Vec3 motion = entity.getDeltaMovement();

        // 当前水平速度
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);

        double minHorizontalSpeed = entity.getData(ModAttachments.VOID_SPEED)+0.2;

        // 只有“起跳后水平速度不够”时才补
        if (horizontal > 1.0E-6 && horizontal < minHorizontalSpeed) {
            double scale = minHorizontalSpeed / horizontal;

            entity.setDeltaMovement(
                    motion.x * scale,
                    motion.y,
                    motion.z * scale
            );

            entity.hurtMarked = true;
        }
    }
    @SubscribeEvent
    public static void noVoidAttack(AttackEntityEvent event) {
        Player player = event.getEntity();

        if (player.getData(ModAttachments.IN_VOID.get())) {
            event.setCanceled(true);
        }
    }
    @SubscribeEvent
    public static void noVoidLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();

        if (player.getData(ModAttachments.IN_VOID.get())) {
            event.setCanceled(true);
        }
    }
    @SubscribeEvent
    //玩家在虚空内无法交互方块
    public static void noVoidTouch(PlayerInteractEvent.RightClickBlock event){
        if(event.getEntity() instanceof Player player){
            if(player.getData(ModAttachments.IN_VOID.get())){
                event.setCanceled(true);
            }
        }
    }
    @SubscribeEvent
    //玩家在虚空内无法交互道具
    public static void noVoidTouch(PlayerInteractEvent.RightClickItem event){
        if(event.getEntity() instanceof Player player){
            if(player.getData(ModAttachments.IN_VOID.get())){
                event.setCanceled(true);
            }
        }
    }
    @SubscribeEvent
    //玩家在虚空内无法交互实体
    public static void noVoidTouch(PlayerInteractEvent.EntityInteract event){
        if(event.getEntity() instanceof Player player){
            if(player.getData(ModAttachments.IN_VOID.get())){
                event.setCanceled(true);
            }
        }
    }
    @SubscribeEvent
    //怪物无法看见进入虚空的活体实体
    public static void noMobTarget(LivingChangeTargetEvent event){ //生物准备更换目标时
        if(event.getNewAboutToBeSetTarget() instanceof LivingEntity target
                && target.getData(ModAttachments.IN_VOID.get())) {
            event.setNewAboutToBeSetTarget(null);
        }
    }
    @SubscribeEvent
    //让原来锁定虚空实体的怪物让他们听话别锁定
    public static void clearMobTarget(EntityTickEvent.Post event){
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide()) {
            return;
        }

        LivingEntity target = mob.getTarget();
        if (target != null && target.getData(ModAttachments.IN_VOID.get())) {
            mob.setTarget(null);
        }
    }
    @SubscribeEvent
    //虚空内无法溺死
    public static void keepAir(EntityTickEvent.Post event){
        if (!(event.getEntity() instanceof LivingEntity entity) || entity.level().isClientSide()) {
            return;
        }

        if(entity.getData(ModAttachments.IN_VOID.get())){ //如果实体在虚空内
            entity.setAirSupply(entity.getMaxAirSupply());   //将氧气值锁定满值
        }
    }
    @SubscribeEvent
    //禁止虚空内拾取物品
    public static void noVoidPickup(ItemEntityPickupEvent.Pre event) {//拾取物品前
        if (event.getPlayer().getData(ModAttachments.IN_VOID.get())) {//如果玩家在虚空里
            event.setCanPickup(TriState.FALSE);//禁止玩家拾取物品
        }
    }
}
