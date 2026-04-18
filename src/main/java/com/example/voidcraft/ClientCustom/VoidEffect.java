package com.example.voidcraft.ClientCustom;

import com.example.voidcraft.Custom.Clock.VoidClock;
import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.Effect.VoidRingManager;
import com.example.voidcraft.Effect.VoidRingRenderer;
import com.example.voidcraft.Effect.VoidTrailInstance;
import com.example.voidcraft.Effect.VoidTrailManager;
import com.example.voidcraft.Effect.VoidTrailRenderer;
import com.example.voidcraft.ModAttachments;
import com.example.voidcraft.VoidCraft;
import com.google.common.reflect.TypeToken;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;


@EventBusSubscriber(modid = VoidCraft.MODID, value = Dist.CLIENT)
public class VoidEffect {

    public static final ContextKey<Boolean> IN_VOID_RENDER =
            new ContextKey<>(Identifier.fromNamespaceAndPath(VoidCraft.MODID, "no_invisible"));
    public static final ContextKey<Float> VOID_PLAYER_ALPHA =
            new ContextKey<>(Identifier.fromNamespaceAndPath(VoidCraft.MODID,"void_player_alpha"));
    private static final Identifier VOID_PLAYER_TEXTURE =
            Identifier.fromNamespaceAndPath(VoidCraft.MODID, "textures/entity/void_player.png");
    private static final RenderType VOID_WORLD_EFFECT = RenderType.create(
            "void_world_effect",
            RenderSetup.builder(RenderPipelines.DEBUG_QUADS)
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                    .sortOnUpload()
                    .bufferSize(RenderType.SMALL_BUFFER_SIZE)
                    .createRenderSetup()
    );
    private static final RenderType VOID_MASK_EFFECT = RenderType.create(
            "void_mask_effect",
            RenderSetup.builder(RenderPipelines.DEBUG_QUADS)
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                    .bufferSize(RenderType.SMALL_BUFFER_SIZE)
                    .createRenderSetup()
    );
    public static class VoidPlayerEffect extends  RenderLayer<AvatarRenderState, PlayerModel> {
        public VoidPlayerEffect(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
            super(parent);          //玩家渲染类
        }
        @Override
        public void submit(PoseStack poseStack,
                           SubmitNodeCollector collector,   //获取玩家渲染
                           int packedLight,         //获取玩家光照信息
                           AvatarRenderState state, //获取玩家当前状态
                           float yRot,              //y轴旋转信息
                           float xRot) {            //x轴旋转信息
            Float alpha = state.getRenderData(VOID_PLAYER_ALPHA);
            if (alpha == null || alpha < 0.01F) {
                return;
            }
            int a = Mth.clamp((int)(alpha*255), 0, 255);        //玩家闪光效果实现
            int tintColor = (a<<24)|0xFFFFFF;
            collector.order(EntityRenderState.NO_OUTLINE)
                    .submitModel(
                            this.getParentModel(),
                            state,
                            poseStack,
                            RenderTypes.entityTranslucent(VOID_PLAYER_TEXTURE),
                            packedLight,
                            LivingEntityRenderer.getOverlayCoords(state, 0.0F),
                            tintColor,
                            null,
                            state.outlineColor,
                            null
                    );
        }


    }
    @SubscribeEvent
    public static void NO_INVISIBLE(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(
                new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>() {},
                (entity, state) -> {
                    if (entity instanceof Player player) {      //隐身效果
                        boolean inVoid = player.getData(ModAttachments.IN_VOID.get());
                        state.setRenderData(IN_VOID_RENDER, inVoid);
                        if (!(VoidClock.VOID_PLAYER_TICKS.get(player.getUUID())==null) || inVoid) {               //清除影子
                            int left = VoidClock.VOID_PLAYER_TICKS.getOrDefault(player.getUUID(),0);
                            int total = VoidClock.VOID_PLAYER_TOTAL_TICKS.getOrDefault(
                                    player.getUUID(),
                                    VoidClock.DEFAULT_VOID_PLAYER_FLASH_TOTAL
                            );
                            float progress = 1 - (float) left / Math.max(1, total);
                            float alpha = 1.0F - progress * progress;
                            state.shadowRadius = 0.0F;
                            state.shadowPieces.clear();
                            state.setRenderData(VOID_PLAYER_ALPHA, alpha);



                        }
                    }
                }
        );
    }
    @SubscribeEvent
    public static void VOID_PLAYER_EFFECT(EntityRenderersEvent.AddLayers event) {
        for(PlayerModelType type : event.getSkins()) {
            AvatarRenderer<AbstractClientPlayer> playerRenderer = event.getPlayerRenderer(type);    //玩家模型闪光方法
            if (playerRenderer != null) {
                playerRenderer.addLayer(new VoidPlayerEffect(playerRenderer));
            }
        }

    }
    @SubscribeEvent
    public static void CLIENT_TICK(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        VoidRingManager.clientTick(mc);
        VoidTrailManager.clientTick(mc);
    }
    @SubscribeEvent
    public static void VOID_RING(RenderLevelStageEvent.AfterParticles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            VoidPhasePostProcessor.resetFrame();
            return;
        }
        PoseStack poseStack = event.getPoseStack();   //拿一个姿态栈（前这一帧渲染时，专门用来记录“位置、旋转、缩放”的一叠变换）
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position(); //获取相机坐标
        float partialTick = mc.gameRenderer.getMainCamera().getPartialTickTime();//获取相机的帧间隔
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        boolean localInVoid = mc.player.getData(ModAttachments.IN_VOID.get());
        if (!localInVoid && VoidRingManager.getRings().isEmpty() && VoidTrailManager.getTrails().isEmpty()) {
            VoidPhasePostProcessor.resetFrame();
            return;
        }

        VoidPhasePostProcessor.beginFrame(mc, partialTick);
        int light = 0x00F000F0; //设定光照颜色亮度
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        var buffer = buffers.getBuffer(VOID_WORLD_EFFECT);
        for (VoidTrailInstance trail : VoidTrailManager.getTrails()) {
            VoidTrailRenderer.render(poseStack, buffer, trail, cameraPos, partialTick);
        }
        for(VoidRingInstance ring : VoidRingManager.getRings()) {  //遍历所有存在的圆环
            if (firstPerson && mc.player != null && ring.ownerEntityId == mc.player.getId()) {
                continue;
            }
            Vec3 center = ring.getCenter(mc.level, partialTick);
            poseStack.pushPose();      //把当前姿态存档一下
            poseStack.translate(
                    center.x - cameraPos.x,
                    center.y - cameraPos.y,        //吧posestack里的坐标纠正成圆环真正的位置
                    center.z - cameraPos.z);
            float yawToCamera = (float) Math.atan2(cameraPos.x - center.x, cameraPos.z - center.z);
            poseStack.mulPose(Axis.YP.rotation(yawToCamera));
            VoidRingRenderer.render(poseStack, buffer, ring, partialTick, light);
            poseStack.popPose();
        }
        buffers.endBatch(VOID_WORLD_EFFECT);

        if (!VoidRingManager.getRings().isEmpty()) {
            VoidPhasePostProcessor.beginMaskWrite();
            var maskBuffer = buffers.getBuffer(VOID_MASK_EFFECT);
            int effectIndex = 0;
            for (VoidRingInstance ring : VoidRingManager.getRings()) {
                if (!VoidPhasePostProcessor.shouldRenderRing(mc, ring, firstPerson)) {
                    continue;
                }
                if (effectIndex >= VoidPhasePostProcessor.MAX_EFFECTS) {
                    break;
                }

                VoidPhasePostProcessor.writeEffectRow(effectIndex, ring, partialTick);
                Vec3 center = ring.getCenter(mc.level, partialTick);
                poseStack.pushPose();
                poseStack.translate(
                        center.x - cameraPos.x,
                        center.y - cameraPos.y,
                        center.z - cameraPos.z
                );
                float yawToCamera = (float) Math.atan2(cameraPos.x - center.x, cameraPos.z - center.z);
                poseStack.mulPose(Axis.YP.rotation(yawToCamera));
                VoidRingRenderer.renderMask(poseStack, maskBuffer, ring, partialTick, effectIndex);
                poseStack.popPose();
                effectIndex++;
            }
            buffers.endBatch(VOID_MASK_EFFECT);
            VoidPhasePostProcessor.endMaskWrite();
        }

        VoidPhasePostProcessor.finishFrame();
    }
    @SubscribeEvent
    public static void NO_INVISIBLE(RenderPlayerEvent.Pre event) {
        Boolean inVoid = event.getRenderState().getRenderData(IN_VOID_RENDER);
        Float alpha = event.getRenderState().getRenderData(VOID_PLAYER_ALPHA);//隐身效果实现
        if (Boolean.TRUE.equals(inVoid) && alpha != null && alpha <= 0.01F) {
            event.setCanceled(true);
        }
    }
}
