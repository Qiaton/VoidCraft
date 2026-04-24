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
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

import java.util.ArrayList;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;


@EventBusSubscriber(modid = VoidCraft.MODID, value = Dist.CLIENT)
public class VoidEffect {

    public static final ContextKey<Boolean> IN_VOID_RENDER =
            new ContextKey<>(Identifier.fromNamespaceAndPath(VoidCraft.MODID, "no_invisible"));
    public static final ContextKey<Float> VOID_PLAYER_ALPHA =
            new ContextKey<>(Identifier.fromNamespaceAndPath(VoidCraft.MODID,"void_player_alpha"));
    private static final Identifier VOID_PLAYER_TEXTURE =
            Identifier.fromNamespaceAndPath(VoidCraft.MODID, "textures/entity/void_player.png");
    private static final Identifier VOID_FLAT_WHITE_TEXTURE =
            Identifier.fromNamespaceAndPath(VoidCraft.MODID, "textures/effect/void_flat_white.png");
    private static final Identifier VOID_SOFT_GLOW_TEXTURE =
            Identifier.fromNamespaceAndPath(VoidCraft.MODID, "textures/effect/void_soft_glow.png");
    private static final boolean IRIS_LOADED = ModList.get().isLoaded("iris");
    private static final Method IRIS_GET_INSTANCE_METHOD = findIrisMethod("getInstance");
    private static final Method IRIS_IS_SHADERPACK_IN_USE_METHOD = findIrisMethod("isShaderPackInUse");
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
    private static final RenderType VOID_TRAIL_WORLD_EFFECT_COMPAT =
            RenderTypes.eyes(VOID_FLAT_WHITE_TEXTURE);
    private static final RenderType VOID_TRAIL_GLOW_EFFECT_COMPAT =
            RenderTypes.energySwirl(VOID_SOFT_GLOW_TEXTURE, 0.0F, 0.0F);
    private static final RenderType VOID_RING_WORLD_EFFECT_COMPAT =
            RenderTypes.entityNoOutline(VOID_SOFT_GLOW_TEXTURE);
    private static final RenderType VOID_RING_BLOOM_EFFECT_COMPAT =
            RenderTypes.energySwirl(VOID_SOFT_GLOW_TEXTURE, 0.0F, 0.0F);

    public static boolean isShaderCompatMode() {
        if (!IRIS_LOADED || IRIS_GET_INSTANCE_METHOD == null || IRIS_IS_SHADERPACK_IN_USE_METHOD == null) {
            return false;
        }

        try {
            Object irisApi = IRIS_GET_INSTANCE_METHOD.invoke(null);
            return irisApi != null && Boolean.TRUE.equals(IRIS_IS_SHADERPACK_IN_USE_METHOD.invoke(irisApi));
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static Method findIrisMethod(String name) {
        if (!IRIS_LOADED) {
            return null;
        }

        try {
            Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            return irisApiClass.getMethod(name);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
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
        var rings = VoidRingManager.getRings();
        var trails = VoidTrailManager.getTrails();
        if (!localInVoid && rings.isEmpty() && trails.isEmpty()) {
            VoidPhasePostProcessor.resetFrame();
            return;
        }

        boolean shaderPackActive = isShaderCompatMode();
        VoidPhasePostProcessor.beginFrame(mc, partialTick);
        List<PreparedRingRender> preparedRings = prepareVisibleRings(mc, rings, cameraPos, partialTick, firstPerson);

        int light = 0x00F000F0; //设定光照颜色亮度
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        if (shaderPackActive) {
            renderTrailPass(buffers, VOID_TRAIL_WORLD_EFFECT_COMPAT, trails, poseStack, cameraPos, partialTick, light, TrailRenderPass.SHADER_COMPAT);
            renderRingPass(buffers, VOID_RING_WORLD_EFFECT_COMPAT, preparedRings, poseStack, light, RingRenderPass.SHADER_COMPAT);
            renderTrailPass(buffers, VOID_TRAIL_GLOW_EFFECT_COMPAT, trails, poseStack, cameraPos, partialTick, light, TrailRenderPass.SHADER_GLOW);
            renderRingPass(buffers, VOID_RING_BLOOM_EFFECT_COMPAT, preparedRings, poseStack, light, RingRenderPass.SHADER_GLOW);
        } else {
            renderTrailPass(buffers, VOID_WORLD_EFFECT, trails, poseStack, cameraPos, partialTick, light, TrailRenderPass.NORMAL);
            renderRingPass(buffers, VOID_WORLD_EFFECT, preparedRings, poseStack, light, RingRenderPass.NORMAL);
        }

        writeRingEffects(mc, buffers, preparedRings, poseStack, partialTick, shaderPackActive);
        VoidPhasePostProcessor.finishFrame();
    }

    private static void renderTrailPass(
            MultiBufferSource.BufferSource buffers,
            RenderType renderType,
            Collection<VoidTrailInstance> trails,
            PoseStack poseStack,
            Vec3 cameraPos,
            float partialTick,
            int light,
            TrailRenderPass pass
    ) {
        if (trails.isEmpty()) {
            return;
        }

        VertexConsumer buffer = buffers.getBuffer(renderType);
        for (VoidTrailInstance trail : trails) {
            switch (pass) {
                case NORMAL -> VoidTrailRenderer.render(poseStack, buffer, trail, cameraPos, partialTick);
                case SHADER_COMPAT -> VoidTrailRenderer.renderShaderCompat(poseStack, buffer, trail, cameraPos, partialTick, light);
                case SHADER_GLOW -> VoidTrailRenderer.renderShaderGlow(poseStack, buffer, trail, cameraPos, partialTick, light);
            }
        }
        buffers.endBatch(renderType);
    }

    private static void renderRingPass(
            MultiBufferSource.BufferSource buffers,
            RenderType renderType,
            List<PreparedRingRender> rings,
            PoseStack poseStack,
            int light,
            RingRenderPass pass
    ) {
        if (rings.isEmpty()) {
            return;
        }

        VertexConsumer buffer = buffers.getBuffer(renderType);
        for (PreparedRingRender prepared : rings) {
            poseStack.pushPose();
            poseStack.translate(
                    prepared.renderX(),
                    prepared.renderY(),
                    prepared.renderZ()
            );
            VoidRingRenderer.applyCameraFacingRotation(poseStack, prepared.ring(), prepared.facingData());
            switch (pass) {
                case NORMAL -> VoidRingRenderer.render(poseStack, buffer, prepared.ring(), prepared.partialTick());
                case SHADER_COMPAT -> VoidRingRenderer.renderShaderCompat(poseStack, buffer, prepared.ring(), prepared.partialTick(), light);
                case SHADER_GLOW -> VoidRingRenderer.renderShaderGlow(poseStack, buffer, prepared.ring(), prepared.partialTick(), light);
            }
            poseStack.popPose();
        }
        buffers.endBatch(renderType);
    }

    private static void writeRingEffects(
            Minecraft mc,
            MultiBufferSource.BufferSource buffers,
            List<PreparedRingRender> rings,
            PoseStack poseStack,
            float partialTick,
            boolean shaderPackActive
    ) {
        if (rings.isEmpty()) {
            return;
        }

        VertexConsumer maskBuffer = null;
        if (!shaderPackActive) {
            VoidPhasePostProcessor.beginMaskWrite();
            maskBuffer = buffers.getBuffer(VOID_MASK_EFFECT);
        }

        int effectIndex = 0;
        for (PreparedRingRender prepared : rings) {
            if (effectIndex >= VoidPhasePostProcessor.MAX_EFFECTS) {
                break;
            }

            poseStack.pushPose();
            poseStack.translate(
                    prepared.renderX(),
                    prepared.renderY(),
                    prepared.renderZ()
            );
            VoidRingRenderer.applyCameraFacingRotation(poseStack, prepared.ring(), prepared.facingData());
            if (shaderPackActive) {
                VoidRingRenderer.ScreenMaskData screenMaskData =
                        VoidRingRenderer.computeScreenMaskData(
                                mc,
                                prepared.ring(),
                                prepared.center(),
                                partialTick,
                                prepared.facingData()
                        );
                if (screenMaskData != null) {
                    VoidPhasePostProcessor.writeEffectRow(
                            effectIndex,
                            prepared.ring(),
                            partialTick,
                            screenMaskData.centerU(),
                            screenMaskData.centerV(),
                            screenMaskData.halfWidthU(),
                            screenMaskData.halfHeightV()
                    );
                } else {
                    VoidPhasePostProcessor.writeEffectRow(effectIndex, prepared.ring(), partialTick);
                }
            } else {
                VoidPhasePostProcessor.writeEffectRow(effectIndex, prepared.ring(), partialTick);
                VoidRingRenderer.renderMask(poseStack, maskBuffer, prepared.ring(), partialTick, effectIndex);
            }
            poseStack.popPose();
            effectIndex++;
        }

        if (!shaderPackActive) {
            buffers.endBatch(VOID_MASK_EFFECT);
            VoidPhasePostProcessor.endMaskWrite();
        }
    }

    private static List<PreparedRingRender> prepareVisibleRings(
            Minecraft mc,
            List<VoidRingInstance> rings,
            Vec3 cameraPos,
            float partialTick,
            boolean firstPerson
    ) {
        if (rings.isEmpty()) {
            return List.of();
        }

        List<PreparedRingRender> prepared = new ArrayList<>(rings.size());
        for (VoidRingInstance ring : rings) {
            if (!VoidPhasePostProcessor.shouldRenderRing(mc, ring, firstPerson)) {
                continue;
            }

            Vec3 center = ring.getCenter(mc.level, partialTick);
            prepared.add(new PreparedRingRender(
                    ring,
                    center,
                    center.x - cameraPos.x,
                    center.y - cameraPos.y,
                    center.z - cameraPos.z,
                    partialTick,
                    VoidRingRenderer.computeFacingData(ring, center, cameraPos)
            ));
        }
        return prepared;
    }

    private enum TrailRenderPass {
        NORMAL,
        SHADER_COMPAT,
        SHADER_GLOW
    }

    private enum RingRenderPass {
        NORMAL,
        SHADER_COMPAT,
        SHADER_GLOW
    }

    private record PreparedRingRender(
            VoidRingInstance ring,
            Vec3 center,
            double renderX,
            double renderY,
            double renderZ,
            float partialTick,
            VoidRingRenderer.FacingData facingData
    ) {
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
