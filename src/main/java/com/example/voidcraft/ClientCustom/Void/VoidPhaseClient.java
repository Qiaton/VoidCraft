package com.example.voidcraft.ClientCustom.Void;

import com.example.voidcraft.Effect.VoidRingManager;
import com.example.voidcraft.Effect.VoidBlackHoleManager;
import com.example.voidcraft.ModAttachments;
import com.example.voidcraft.VoidCraft;
import com.example.voidcraft.Item.custom.SpatialSword;
import com.example.voidcraft.World.PhaseDimensions;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

@EventBusSubscriber(modid = VoidCraft.MODID, value = Dist.CLIENT)
public class VoidPhaseClient {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ResourceLocation VOID_PHASE_IDLE_EFFECT =
            ResourceLocation.fromNamespaceAndPath(VoidCraft.MODID, "shaders/post/void_phase.json");

    private static boolean postEffectApplied = false;               //当前相位视觉效果状态
    private static ResourceLocation activePostEffectId = null;            //当前相位移动状态
    private static PostChain activePostEffect = null;                //当前相位后处理对象
    private static boolean lastResolvedInVoid = false;              //上一次判断虚空状态的结果
    private static boolean lastAttachmentInVoid = false;            //上一次判断附件里虚空状态的结果
    private static InVoidLoopSoundInstance activeLoopSound = null;  //初始化循环播放实例

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();         //拿到mc游戏本体的实例
        LocalPlayer player = mc.player;                 //拿到玩家本体
        if (player == null || mc.level == null) {       //如果玩家没生成 或者没有在任何一个世界里（加载状态）
            if (!PhaseWorldTransitionClient.isActive()) {
                clearPostEffect(mc);
                VoidPhasePostProcessor.releaseResources();
            }
            stopLoopSound(mc);                          //停止播放声音
            lastResolvedInVoid = false;                 //清空缓存的虚空状态
            lastAttachmentInVoid = false;               //同上
            return;
        }

        if (mc.isPaused()) {                            //如果当前是暂停状态
            return;
        }

        boolean attachmentInVoid = player.getData(ModAttachments.IN_VOID.get());
        boolean usingSpatialSword = player.isUsingItem() && player.getUseItem().getItem() instanceof SpatialSword;
        boolean inVoid = attachmentInVoid || usingSpatialSword;      //根据条件判断是否在虚空
        boolean inPhaseDimension = PhaseDimensions.isPhaseMirror(mc.level);
        boolean hasNearbyTears = VoidRingManager.hasActiveRings();
        boolean hasActiveBlackHoles = VoidBlackHoleManager.hasActiveBlackHoles();
        boolean hasVoidInOutEffect = VoidInOutEffectClient.isActive();
        boolean shouldApplyPost = inVoid
                || inPhaseDimension
                || hasNearbyTears
                || hasActiveBlackHoles
                || PhaseWorldTransitionClient.isActive()
                || hasVoidInOutEffect;

        if (attachmentInVoid != lastAttachmentInVoid) {             //如果附件虚空状态发生改变 发送一条改变状态的日志
            VoidInOutEffectClient.start();
            LOGGER.debug("[VoidPhase] attachment in_void changed -> {}", attachmentInVoid);
            lastAttachmentInVoid = attachmentInVoid;
        }
        if (inVoid != lastResolvedInVoid) {                         //如果虚空状态发生改变 发送一条各组件状态的日志
            LOGGER.debug("[VoidPhase] resolved inVoid={} (attachment={}, usingSword={})",
                    inVoid, attachmentInVoid, usingSpatialSword);
            lastResolvedInVoid = inVoid;
        }

        if (inVoid) {
            ensureLoopSound(mc, player);                            //调用下方封装好的播放音频函数
        } else {
            fadeOutLoopSound();                                     //如果不在虚空内 就让循环虚空声淡出
        }

        if (shouldApplyPost) {
            VoidPhasePostProcessor.ensureTextureRegistered(mc);
            ResourceLocation desiredEffectId = VOID_PHASE_IDLE_EFFECT;    //初始化期望相位效果
            PostChain currentEffect = mc.gameRenderer.currentEffect();
            if (!postEffectApplied || currentEffect == null || currentEffect != activePostEffect || !desiredEffectId.equals(activePostEffectId)) {         //维度切换会清后处理，这里按真实 renderer 状态重新挂载
                try {
                    mc.gameRenderer.loadEffect(desiredEffectId); //把渲染设置的相位效果设置成期望相位效果
                    postEffectApplied = true;                       //开启相位效果
                    activePostEffectId = desiredEffectId;           //把当前的相位效果设置成期望相位效果
                    activePostEffect = mc.gameRenderer.currentEffect();
                    LOGGER.debug("[VoidPhase] post effect enabled: {}", desiredEffectId);
                } catch (RuntimeException e) {
                    postEffectApplied = false;
                    activePostEffectId = null;
                    activePostEffect = null;
                    LOGGER.error("[VoidPhase] failed to enable post effect {}", desiredEffectId, e);
                }
            } else {
                postEffectApplied = true;
                activePostEffectId = desiredEffectId;
                activePostEffect = currentEffect;
            }
        } else {
            clearPostEffect(mc);
        }
    }

    private static void ensureLoopSound(Minecraft mc, LocalPlayer player) {
        if (activeLoopSound == null || activeLoopSound.isStopped() || !mc.getSoundManager().isActive(activeLoopSound)) {
            activeLoopSound = new InVoidLoopSoundInstance(player);
            mc.getSoundManager().play(activeLoopSound);
        } else {
            activeLoopSound.beginFadeIn();
        }
    }

    private static void fadeOutLoopSound() {
        if (activeLoopSound != null) {
            activeLoopSound.beginFadeOut();
        }
    }

    private static void stopLoopSound(Minecraft mc) {
        if (activeLoopSound != null) {
            mc.getSoundManager().stop(activeLoopSound);
            activeLoopSound = null;
        }
    }

    private static void clearPostEffect(Minecraft mc) {
        if (!postEffectApplied) {
            return;
        }

        if (mc.gameRenderer.currentEffect() == activePostEffect) {
            mc.gameRenderer.shutdownEffect();
        }
        postEffectApplied = false;
        activePostEffectId = null;
        activePostEffect = null;
        LOGGER.debug("[VoidPhase] post effect cleared");
    }

}
