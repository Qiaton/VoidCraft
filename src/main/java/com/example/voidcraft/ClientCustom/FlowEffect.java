package com.example.voidcraft.ClientCustom;

import com.example.voidcraft.VoidCraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

@EventBusSubscriber(modid = VoidCraft.MODID, value = Dist.CLIENT)//申请把下面这个类挂载到游戏进程上，参数为（哪个模组，客户端/服务端）
public class FlowEffect {           //创建公共变量来给其他类使用
    public static float fov_effect=0;       //这是fov的偏移值
    @SubscribeEvent //把下面的方法和EventBusSubscriber一起带走
    public static void EFFECT(ComputeFovModifierEvent e) {
        e.setNewFovModifier(e.getNewFovModifier()+fov_effect);      //基于现有的fov值加一个偏移值达到视角缩放的效果
    }
}

