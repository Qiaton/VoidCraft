package com.example.voidcraft.Item.custom;

import com.example.voidcraft.Custom.Clock.VoidClock;
import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.ModAttachments;
import com.example.voidcraft.network.ModNetworking;
import com.example.voidcraft.Sound.ModSound;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;


public class SpatialSword extends Item{
    public SpatialSword(Properties properties) {
        super(properties);//一键继承item的构造方法进行简单构造
    }

//    @Override
//    public boolean isFoil(ItemStack stack) {
//        return true;
//    }

//    @Override
//    public int getUseDuration(ItemStack stack, LivingEntity entity){    //设置物品使用时长
//        return 99999;
//    }
//    @Override
////    ItemStack stack：当前这把剑。
////    Level level：当前世界。
////    LivingEntity entity：当前正在使用这把物品的生物，玩家也属于这个类型。
////    int timeLeft：松开时还剩多少使用时间。这个方法签名和 getUseDuration(ItemStack, LivingEntity) 是配套的
//    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft){    //玩家取消使用物品时
//        if(entity instanceof Player player){
//            player.setData(ModAttachments.IN_VOID.get(), false);
//            VoidClock.VOID_TICKS.put(player.getUUID(),0);
//            ModNetworking.sendPhaseTear(player, VoidRingInstance.Preset.DEFAULT); //相位裂缝动画
//            ModSound.playOutVoid(level, player);
//        }
//
//        return true;
//    }
//    @Override
//    public @NonNull InteractionResult use(Level level, Player player, @NonNull InteractionHand hand){
//        if (!player.level().isClientSide()) {
//        player.setData(ModAttachments.IN_VOID.get(),true);//开启玩家虚空状态
//        player.startUsingItem(hand);//玩家开始使用主手上的道具
//        VoidClock.VOID_TICKS.put(player.getUUID(),100000);
//        ModNetworking.sendPhaseTear(player, VoidRingInstance.Preset.DEFAULT); //相位裂缝动画
//
//            ModSound.playEnterVoid(level, player);
//        }
//        return InteractionResult.SUCCESS;
//    }

}
