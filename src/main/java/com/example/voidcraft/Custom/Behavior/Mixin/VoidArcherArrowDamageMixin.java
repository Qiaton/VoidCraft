package com.example.voidcraft.Custom.Behavior.Mixin;

import com.example.voidcraft.Custom.Behavior.VoidArcher;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractArrow.class)
public abstract class VoidArcherArrowDamageMixin {

    @ModifyVariable(method = "onHitEntity", at = @At("STORE"), ordinal = 0)
    private float voidcraft$useVanillaEquivalentImpactSpeed(float speed) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        return VoidArcher.getVanillaEquivalentImpactSpeed(arrow, speed);
    }

    @ModifyVariable(method = "onHitEntity", at = @At("STORE"), ordinal = 0)
    private double voidcraft$useVanillaEquivalentBaseDamage(double baseDamage) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        return VoidArcher.getVanillaEquivalentBaseDamage(arrow, baseDamage);
    }

    @Redirect(
            method = "onHitEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;modifyDamage(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;F)F"
            )
    )
    private float voidcraft$modifyDamageWithVanillaEquivalentMotion(
            ServerLevel level,
            ItemStack tool,
            Entity target,
            DamageSource damageSource,
            float damage
    ) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        if (!VoidArcher.shouldNormalizeVanillaHitDamage(arrow)) {
            return EnchantmentHelper.modifyDamage(level, tool, target, damageSource, damage);
        }

        Vec3 currentMovement = arrow.getDeltaMovement();
        arrow.setDeltaMovement(VoidArcher.getVanillaEquivalentMovement(arrow, currentMovement));
        try {
            return EnchantmentHelper.modifyDamage(level, tool, target, damageSource, damage);
        } finally {
            arrow.setDeltaMovement(currentMovement);
        }
    }
}
