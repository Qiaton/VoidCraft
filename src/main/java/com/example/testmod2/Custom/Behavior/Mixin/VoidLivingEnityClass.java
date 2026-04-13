package com.example.testmod2.Custom.Behavior.Mixin;

import com.example.testmod2.ModAttachments;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class VoidLivingEnityClass {
    @Inject(method = "isPushable",at = @At("HEAD"),cancellable = true)
    public void NO_PUSHABLE(CallbackInfoReturnable<Boolean> cir){
        LivingEntity entity = (LivingEntity)(Object)this;
        if(!(entity instanceof Player player)){             //如果这个实体不是玩家就直接跳过
            return;
        }
        if(player.getData(ModAttachments.IN_VOID)){         //如果这个玩家处于虚空 关闭碰撞事件
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isPickable",at = @At("HEAD"),cancellable = true)
    public void NO_PICKABLE(CallbackInfoReturnable<Boolean> cir){
        LivingEntity entity = (LivingEntity)(Object)this;
        if(!(entity instanceof Player player)){             //如果这个实体不是玩家就直接跳过
            return;
        }
        if(player.getData(ModAttachments.IN_VOID)){         //如果这个玩家处于虚空 关闭被选中事件
            cir.setReturnValue(false);
        }
    }


}
