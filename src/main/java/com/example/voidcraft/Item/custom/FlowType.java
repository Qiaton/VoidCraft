package com.example.voidcraft.Item.custom;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;


public class FlowType extends Item {
    public FlowType(Properties properties) {
        super(properties);
    }
    @Override
    public @NonNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NonNull InteractionHand hand){
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide()){
//            VoidClock.VOID_TICKS.put(player.getUUID(),30);
//            DashClock.setDash(player,30,8);
//            itemStack.hurtAndBreak(1,player,hand);
//            ModNetworking.sendPhaseTear(player, VoidRingInstance.Preset.DEFAULT); //相位裂缝动画
//            ModSound.playEnterVoid(level, player);

        }
        else{
//            Clock.addClock(30,()->{
//                FlowEffect.fov_effect=0.1f;
//            });
//                FlowEffect.fov_effect=1.5F;
        }
        return InteractionResultHolder.success(itemStack);
    }
}
