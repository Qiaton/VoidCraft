package com.example.voidcraft.Custom.Behavior;

import com.example.voidcraft.ModAttachments;
import com.example.voidcraft.VoidCraft;

import net.minecraft.resources.Identifier;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber
public class VoidEvents {
    public static final Identifier VOID_SPEED_ID = Identifier.fromNamespaceAndPath(VoidCraft.MODID, "void_speed");
    @SubscribeEvent
    //虚空内无法受伤
    public static void IN_VOID_Immunity(EntityInvulnerabilityCheckEvent event){//这一大串相当于 造成伤害时
        if(event.getEntity() instanceof Player player){ //如果获取的实体为玩家类型 player就是event的别名
            if (!player.level().isClientSide()) {
                if (player.getData(ModAttachments.IN_VOID.get())) { //如果player获取IN_VOID的数据为true
                    event.setInvulnerable(true);    //开启无敌效果
                }
            }
        }
    }
    @SubscribeEvent
    public static void VOID_JUMP(net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!player.getData(ModAttachments.IN_VOID.get())) {
            return;
        }

        Vec3 motion = player.getDeltaMovement();

        // 当前水平速度
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);

        double minHorizontalSpeed = 0.55;

        // 只有“起跳后水平速度不够”时才补
        if (horizontal > 1.0E-6 && horizontal < minHorizontalSpeed) {
            double scale = minHorizontalSpeed / horizontal;

            player.setDeltaMovement(
                    motion.x * scale,
                    motion.y,
                    motion.z * scale
            );

            player.hurtMarked = true;
        }
    }
    @SubscribeEvent
    //玩家在虚空内无法交互方块
    public static void NO_TOUCH(PlayerInteractEvent.RightClickBlock event){
        if(event.getEntity() instanceof Player player){
            if(player.getData(ModAttachments.IN_VOID.get())){
                event.setCanceled(true);
            }
        }
    }
    @SubscribeEvent
    //玩家在虚空内无法交互道具
    public static void NO_TOUCH(PlayerInteractEvent.RightClickItem event){
        if(event.getEntity() instanceof Player player){
            if(player.getData(ModAttachments.IN_VOID.get())){
                event.setCanceled(true);
            }
        }
    }
    @SubscribeEvent
    //玩家在虚空内无法交互实体
    public static void NO_TOUCH(PlayerInteractEvent.EntityInteract event){
        if(event.getEntity() instanceof Player player){
            if(player.getData(ModAttachments.IN_VOID.get())){
                event.setCanceled(true);
            }
        }
    }
    @SubscribeEvent
    //怪物无法看见玩家
    public static void NO_TARGET(LivingChangeTargetEvent event){ //生物准备更换目标时
        if(event.getNewAboutToBeSetTarget() instanceof Player player && player.getData(ModAttachments.IN_VOID.get())) {
            if (player.getData(ModAttachments.IN_VOID.get())) {
                event.setNewAboutToBeSetTarget(null);
            }
        }
    }
    @SubscribeEvent
    //让原来锁定玩家的怪物让他们听话别锁定
    public static void NO_TARGET(PlayerTickEvent.Post event){
        Player player = event.getEntity();
        if (player.level().isClientSide() || !player.getData(ModAttachments.IN_VOID.get())) {
            return;
        }
        //getEntitiesOfClass查找实体（实体类型，范围） getBoundingBox().inflate(32)获取碰撞箱32范围内
        for(Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(32))) {
            mob.setTarget(null);
        }
    }
    @SubscribeEvent
    //虚空内无法溺死
    public static void UNLIMITED_OXYGEN(PlayerTickEvent.Post event){
        Player player = event.getEntity();      //从玩家刻中提取玩家
        if(player.getData(ModAttachments.IN_VOID.get())){ //如果玩家在虚空内
            player.setAirSupply(player.getAirSupply());   //将氧气值锁定满值
        }
    }
    @SubscribeEvent
    //禁止虚空内拾取物品
    public static void noPickup(ItemEntityPickupEvent.Pre event) {//拾取物品前
        if (event.getPlayer().getData(ModAttachments.IN_VOID.get())) {//如果玩家在虚空里
            event.setCanPickup(TriState.FALSE);//禁止玩家拾取物品
        }
    }
}
