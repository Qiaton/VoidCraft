package com.example.voidcraft.Custom.Behavior.Mixin;

import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {

    @Accessor("baseDamage")
    double voidcraft$getBaseDamage();

    @Accessor("baseDamage")
    void voidcraft$setBaseDamage(double value);
}
