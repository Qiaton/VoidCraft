package com.example.voidcraft.Custom.Behavior.Mixin;

import com.example.voidcraft.Custom.Clock.ModuleSkill.TeleportVoidModuleClock;
import com.example.voidcraft.ModAttachments;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class VoidLivingEntityClass {
    @Inject(method = "isPushable",at = @At("HEAD"),cancellable = true)
    public void noPushable(CallbackInfoReturnable<Boolean> cir){
        LivingEntity entity = (LivingEntity)(Object)this;
        if(entity.getData(ModAttachments.IN_PHASE.get())){   //相位实体不参与活体推挤
            cir.setReturnValue(false);
        }
    }
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void moveInFluid(Vec3 travelVector, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity)(Object)this;

        // 不是虚空，直接走原版
        if (!entity.getData(ModAttachments.IN_PHASE.get())) {
            return;
        }

        // 不在水/岩浆里，直接走原版
        if (!entity.isInFluidType()) {
            return;
        }

        if (TeleportVoidModuleClock.isMoving(entity)) {
            entity.move(MoverType.SELF, entity.getDeltaMovement());
            ci.cancel();
            return;
        }

        // 先记住原来的Y速度，尽量别乱碰垂直方向
        Vec3 oldMotion = entity.getDeltaMovement();

        // 只处理水平输入，避免流体减速
        Vec3 horizontalInput = new Vec3(travelVector.x, 0.0, travelVector.z);

        // 按实体当前速度把输入转换成运动
        entity.moveRelative(entity.getSpeed(), horizontalInput);

        Vec3 newMotion = entity.getDeltaMovement();

        // 只给水平一点阻尼，Y保留原值
        entity.setDeltaMovement(
                newMotion.x * 0.51,
                oldMotion.y,
                newMotion.z * 0.51
        );

        entity.move(MoverType.SELF, entity.getDeltaMovement());

        // 不再走原版流体 travel 逻辑
        ci.cancel();
    }



    @Inject(method = "isPickable",at = @At("HEAD"),cancellable = true)
    public void noPickable(CallbackInfoReturnable<Boolean> cir){
        LivingEntity entity = (LivingEntity)(Object)this;
        if(entity.getData(ModAttachments.IN_PHASE.get())){   //相位实体不响应准星选取
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getSpeed", at = @At("HEAD"), cancellable = true)
    private void setVoidSpeed(CallbackInfoReturnable<Float> cir) {
        LivingEntity entity = (LivingEntity)(Object)this;

        if (entity.getData(ModAttachments.IN_PHASE.get())) {
            cir.setReturnValue(entity.getData(ModAttachments.VOID_SPEED.get()));
        }
    }


}
