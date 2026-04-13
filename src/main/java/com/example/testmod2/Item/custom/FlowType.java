package com.example.testmod2.Item.custom;

import com.example.testmod2.ClientCustom.FlowEffect;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

import static com.example.testmod2.Custom.Clock.DashClock.DASH_DIRECTION;
import static com.example.testmod2.Custom.Clock.DashClock.DASH_TICKS;


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

        }
        else{
                FlowEffect.fov_effect=1.5F;
        }
        return InteractionResult.SUCCESS;
    }
}
