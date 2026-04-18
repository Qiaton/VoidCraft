package com.example.voidcraft.Item.custom;

import com.example.voidcraft.ClientCustom.FlowEffect;
import com.example.voidcraft.Custom.Clock.VoidClock;
import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.ModAttachments;
import com.example.voidcraft.Sound.ModSound;
import com.example.voidcraft.network.ModNetworking;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

import static com.example.voidcraft.Custom.Clock.DashClock.DASH_DIRECTION;
import static com.example.voidcraft.Custom.Clock.DashClock.DASH_TICKS;


public class FlowType extends Item {
    public FlowType(Properties properties) {
        super(properties);
    }
    @Override
    public @NonNull InteractionResult use(Level level, Player player, @NonNull InteractionHand hand){
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide()){
            DASH_TICKS.put(player.getUUID(),15);
            Vec3 vec3 = player.getLookAngle();
            Vec3 direction = new Vec3(vec3.x, 0, vec3.z).normalize();
            DASH_DIRECTION.put(player.getUUID(),direction);
            itemStack.hurtAndBreak(1,player,hand);
            VoidClock.VOID_TICKS.put(player.getUUID(),15);
            ModNetworking.sendPhaseTear(player, VoidRingInstance.Preset.DEFAULT); //相位裂缝动画
            ModSound.playEnterVoid(level, player);

        }
        else{
                FlowEffect.fov_effect=1.5F;
        }
        return InteractionResult.SUCCESS;
    }
}
