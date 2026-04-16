package com.example.testmod2.Effect;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class VoidRingManager {
    private static final List<VoidRingInstance> RINGS=new ArrayList<>();
    public static void addRing(Vec3 vec3){
        RINGS.add(new VoidRingInstance(vec3,5,0.5F,2F,0.3F));
    }
    public static void clientTick(){
        Iterator<VoidRingInstance> ring =RINGS.iterator();
        while(ring.hasNext()){
            VoidRingInstance instance=ring.next();
            instance.age++;
            if (instance.isDead()) ring.remove();
        }
    }
    public static List<VoidRingInstance> getRings(){
        return RINGS;
    }
}
