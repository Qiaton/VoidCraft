package com.example.testmod2.Item.custom;

import com.example.testmod2.ClientCustom.SpatialSwordEffect;
import com.example.testmod2.Custom.Behavior.VoidEvents;
import com.example.testmod2.Custom.Clock.VoidClock;
import com.example.testmod2.ModAttachments;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;


public class SpatialSword extends Item{
    public SpatialSword(Properties properties) {
        super(properties);//一键继承item的构造方法进行简单构造
    }
    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity){    //设置物品使用时长
        return 99999;
    }
    @Override
//    ItemStack stack：当前这把剑。
//    Level level：当前世界。
//    LivingEntity entity：当前正在使用这把物品的生物，玩家也属于这个类型。
//    int timeLeft：松开时还剩多少使用时间。这个方法签名和 getUseDuration(ItemStack, LivingEntity) 是配套的
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft){    //玩家取消使用物品时
        if(entity instanceof Player player){
            player.setData(ModAttachments.IN_VOID.get(), false);
            VoidClock.VOID_TICKS.put(player.getUUID(),0);
//            VoidEvents.removeVoidSpeed(player);
            SpatialSwordEffect.IN_VOID=false;
            if (!player.level().isClientSide()) {
                level.playSound(
                        null,                                       //谁听不见
                        player.getX(), player.getY(), player.getZ(),      //声音播放的坐标
                        SoundEvents.ENDERMAN_TELEPORT,                    //播放的声音事件
                        SoundSource.PLAYERS,                              //声音属于哪类（敌对生物 友好生物这种）
                        1.0F,                                             //音量
                        1.0F                                              //音高
                );
            }
        }

        return true;
    }
    @Override
    public @NonNull InteractionResult use(Level level, Player player, @NonNull InteractionHand hand){
        if (!player.level().isClientSide()) {
        player.setData(ModAttachments.IN_VOID.get(),true);//开启玩家虚空状态
        player.startUsingItem(hand);//玩家开始使用主手上的道具
        VoidClock.VOID_TICKS.put(player.getUUID(),100000);
//        VoidEvents.addVoidSpeed(player);
        SpatialSwordEffect.IN_VOID=true;

            level.playSound(
                    null,                                       //谁听不见
                    player.getX(), player.getY(), player.getZ(),      //声音播放的坐标
                    SoundEvents.ENDERMAN_TELEPORT,                    //播放的声音事件
                    SoundSource.PLAYERS,                              //声音属于哪类（敌对生物 友好生物这种）
                    1.0F,                                             //音量
                    1.0F                                              //音高
            );
        }
        return InteractionResult.SUCCESS;
    }

}
