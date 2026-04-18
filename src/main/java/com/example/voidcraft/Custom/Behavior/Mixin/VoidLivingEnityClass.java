package com.example.voidcraft.Custom.Behavior.Mixin;

import com.example.voidcraft.ModAttachments;
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
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void VOID_FLUID_TRAVEL(Vec3 travelVector, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity)(Object)this;

        if (!(entity instanceof Player player)) {
            return;
        }
        // 不是虚空，直接走原版
        if (!player.getData(ModAttachments.IN_VOID.get())) {
            return;
        }

        // 不在水/岩浆里，直接走原版
        if (!player.isInFluidType()) {
            return;
        }

        // 先记住原来的Y速度，尽量别乱碰垂直方向
        Vec3 oldMotion = player.getDeltaMovement();

        // 只处理水平输入，避免流体减速
        Vec3 horizontalInput = new Vec3(travelVector.x, 0.0, travelVector.z);

        // 按玩家当前速度把输入转换成运动
        player.moveRelative(player.getSpeed(), horizontalInput);

        Vec3 newMotion = player.getDeltaMovement();

        // 只给水平一点阻尼，Y保留原值
        player.setDeltaMovement(
                newMotion.x * 0.51,
                oldMotion.y,
                newMotion.z * 0.51
        );

        player.move(MoverType.SELF, player.getDeltaMovement());

        // 不再走原版流体 travel 逻辑
        ci.cancel();
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
