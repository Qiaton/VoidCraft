package com.example.voidcraft.ClientCustom;

import com.example.voidcraft.Effect.VoidRingManager;
import com.example.voidcraft.ModAttachments;
import com.example.voidcraft.VoidCraft;
import com.example.voidcraft.Item.custom.SpatialSword;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

@EventBusSubscriber(modid = VoidCraft.MODID, value = Dist.CLIENT)
public class VoidPhaseClient {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Identifier VOID_PHASE_IDLE_EFFECT =
            Identifier.fromNamespaceAndPath(VoidCraft.MODID, "void_phase");

    private static boolean postEffectApplied = false;               //当前相位视觉效果状态
    private static Identifier activePostEffectId = null;            //当前相位移动状态
    private static boolean lastResolvedInVoid = false;              //上一次判断虚空状态的结果
    private static boolean lastAttachmentInVoid = false;            //上一次判断附件里虚空状态的结果
    private static InVoidLoopSoundInstance activeLoopSound = null;  //初始化循环播放实例

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();         //拿到mc游戏本体的实例
        VoidPhasePostProcessor.ensureTextureRegistered(mc);

        if (mc.isPaused()) {                            //如果当前是暂停状态
            return;
        }

        LocalPlayer player = mc.player;                 //拿到玩家本体
        if (player == null || mc.level == null) {       //如果玩家没生成 或者没有在任何一个世界里（加载状态）
            if (postEffectApplied) {                    //如果之前开启了相位视觉效果
                mc.gameRenderer.clearPostEffect();      //清除相位效果
                postEffectApplied = false;              //关闭相位效果
                activePostEffectId = null;              //清除相位效果
            }
            VoidPhasePostProcessor.resetTexture();
            stopLoopSound(mc);                          //停止播放声音
            lastResolvedInVoid = false;                 //清空缓存的虚空状态
            lastAttachmentInVoid = false;               //同上
            return;
        }

        boolean attachmentInVoid = player.getData(ModAttachments.IN_VOID.get());
        boolean usingSpatialSword = player.isUsingItem() && player.getUseItem().getItem() instanceof SpatialSword;
        boolean inVoid = attachmentInVoid || usingSpatialSword;      //根据条件判断是否在虚空
        boolean hasNearbyTears = VoidRingManager.hasActiveRings();
        boolean shouldApplyPost = inVoid || hasNearbyTears;

        if (attachmentInVoid != lastAttachmentInVoid) {             //如果附件虚空状态发生改变 发送一条改变状态的日志
            LOGGER.info("[VoidPhase] attachment in_void changed -> {}", attachmentInVoid);
            lastAttachmentInVoid = attachmentInVoid;
        }
        if (inVoid != lastResolvedInVoid) {                         //如果虚空状态发生改变 发送一条各组件状态的日志
            LOGGER.info("[VoidPhase] resolved inVoid={} (attachment={}, usingSword={})",
                    inVoid, attachmentInVoid, usingSpatialSword);
            lastResolvedInVoid = inVoid;
        }

        if (inVoid) {
            ensureLoopSound(mc, player);                            //调用下方封装好的播放音频函数
        } else {
            fadeOutLoopSound();                                     //如果不在虚空内 就让循环虚空声淡出
        }

        if (shouldApplyPost) {
            Identifier desiredEffectId = VOID_PHASE_IDLE_EFFECT;    //初始化期望相位效果
            if (!postEffectApplied) {                               //如果当前没有启动相位效果
                try {
                    mc.gameRenderer.setPostEffect(desiredEffectId); //把渲染设置的相位效果设置成期望相位效果
                    postEffectApplied = true;                       //开启相位效果
                    activePostEffectId = desiredEffectId;           //把当前的相位效果设置成期望相位效果
                    LOGGER.info("[VoidPhase] post effect enabled: {}", desiredEffectId);
                } catch (RuntimeException e) {
                    LOGGER.error("[VoidPhase] failed to enable post effect {}", desiredEffectId, e);
                }
            } else if (!desiredEffectId.equals(activePostEffectId)) {       //如果当前相位效果不是期望的相位效果
                try {
                    mc.gameRenderer.setPostEffect(desiredEffectId);         //改成期望的效果
                    activePostEffectId = desiredEffectId;                   //同上
                    LOGGER.info("[VoidPhase] post effect switched: {}", desiredEffectId);
                } catch (RuntimeException e) {
                    LOGGER.error("[VoidPhase] failed to switch post effect {}", desiredEffectId, e);
                }
            }
        } else {
            if (postEffectApplied) {
                mc.gameRenderer.clearPostEffect();
                postEffectApplied = false;                      //清理缓存
                activePostEffectId = null;
                LOGGER.info("[VoidPhase] post effect cleared");
            }
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

}
