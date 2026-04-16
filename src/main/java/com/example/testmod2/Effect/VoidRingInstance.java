package com.example.testmod2.Effect;

import net.minecraft.world.phys.Vec3;

public class VoidRingInstance {
    public Vec3 center;
    public int age;
    public int duration;
    public float startRadius;
    public float maxRadius;
    public float thickness;
    public VoidRingInstance(Vec3 center, int duration, float startRadius, float maxRadius, float thickness) {
        this.center = center;
        this.age = 0;
        this.duration = duration;
        this.maxRadius = maxRadius;
        this.thickness = thickness;
        this.startRadius = startRadius;
    }
    public boolean isDead(){
            return this.age>=this.duration;

    }
}
