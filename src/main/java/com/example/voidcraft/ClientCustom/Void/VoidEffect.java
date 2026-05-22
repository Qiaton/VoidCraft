package com.example.voidcraft.ClientCustom.Void;

import com.example.voidcraft.Gui.PhaseWorldTransitionOverlay;
import com.example.voidcraft.ClientCustom.Turret.PhaseEmitterClientManager;
import com.example.voidcraft.Custom.Clock.VoidClock;
import com.example.voidcraft.Effect.VoidBlackHoleInstance;
import com.example.voidcraft.Effect.VoidBlackHoleManager;
import com.example.voidcraft.Effect.VoidBlackHoleRenderer;
import com.example.voidcraft.Effect.VoidBeamInstance;
import com.example.voidcraft.Effect.VoidBeamManager;
import com.example.voidcraft.Effect.VoidBeamRenderer;
import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.Effect.VoidRingManager;
import com.example.voidcraft.Effect.VoidRingRenderer;
import com.example.voidcraft.Effect.VoidTrailInstance;
import com.example.voidcraft.Effect.VoidTrailManager;
import com.example.voidcraft.Effect.VoidTrailRenderer;
import com.example.voidcraft.ModAttachments;
import com.example.voidcraft.VoidCraft;
import com.example.voidcraft.World.PhaseDimensions;
import com.google.common.reflect.TypeToken;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
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
            RenderTypes.eyes(VOID_SOFT_GLOW_TEXTURE);
    private static final RenderType VOID_RING_BLOOM_EFFECT_COMPAT =
            RenderTypes.energySwirl(VOID_SOFT_GLOW_TEXTURE, 0.0F, 0.0F);
    private static final RenderType VOID_BLACK_HOLE_WORLD_EFFECT_COMPAT =
            RenderTypes.entityTranslucent(VOID_FLAT_WHITE_TEXTURE);
    private static final RenderType PHASE_EMITTER_ORB_EFFECT_COMPAT =
            RenderTypes.eyes(VOID_FLAT_WHITE_TEXTURE);

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
    public static class VoidLivingEffect<S extends LivingEntityRenderState, M extends EntityModel<? super S>>
            extends RenderLayer<S, M> {
        public VoidLivingEffect(RenderLayerParent<S, M> parent) {
            super(parent);          //沿用当前实体自己的模型，只在外面叠一层虚空材质
        }
        @Override
        public void submit(PoseStack poseStack,
                           SubmitNodeCollector collector,   //获取实体渲染
                           int packedLight,         //获取实体光照信息
                           S state,                  //获取实体当前状态
                           float yRot,              //y轴旋转信息
                           float xRot) {            //x轴旋转信息
            Float alpha = state.getRenderData(VOID_PLAYER_ALPHA);
            if (alpha == null || alpha < 0.01F) {
                return;
            }
            int a = Mth.clamp((int)(alpha*255), 0, 255);        //虚空闪光效果实现
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
    public static void noInvisible(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(
                new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>() {},
                (entity, state) -> {
                    boolean inVoid = entity.getData(ModAttachments.IN_PHASE.get());
                    state.setRenderData(IN_VOID_RENDER, inVoid);
                    if (VoidClock.hasVoidFlash(entity) || inVoid) {               //虚空实体不投普通影子
                        float alpha = VoidClock.getVoidFlashAlpha(entity);
                        state.shadowRadius = 0.0F;
                        state.shadowPieces.clear();
                        state.setRenderData(VOID_PLAYER_ALPHA, alpha);
                    }
                }
        );
    }
    @SubscribeEvent
    public static void addVoidPlayerLayer(EntityRenderersEvent.AddLayers event) {
        for(EntityType<?> entityType : event.getEntityTypes()) {
            EntityRenderer<?, ?> renderer = event.getRenderer(entityType);
            if (renderer instanceof LivingEntityRenderer<?, ?, ?> livingRenderer) {
                addVoidLivingLayer(livingRenderer);
            }
        }

        for(var type : event.getSkins()) {
            LivingEntityRenderer<?, ?, ?> playerRenderer = event.getPlayerRenderer(type);    //玩家模型单独注册在皮肤表里
            if (playerRenderer != null) {
                addVoidLivingLayer(playerRenderer);
            }
        }

    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addVoidLivingLayer(LivingEntityRenderer renderer) {
        renderer.addLayer(new VoidLivingEffect(renderer));
    }
    @SubscribeEvent
    public static void tickClientEffects(ClientTickEvent.Post event) {
          Minecraft mc = Minecraft.getInstance();
          VoidRingManager.clientTick(mc);
          VoidTrailManager.clientTick(mc);
          VoidBeamManager.clientTick(mc);
          VoidBlackHoleManager.clientTick(mc);
          if (mc.level != null && mc.player != null) {
              PhaseWorldTransitionOverlay.prepare(mc);
          }
          PhaseWorldTransitionClient.clientTick();
          VoidInOutEffectClient.tick();
      }
    @SubscribeEvent
    public static void renderVoidEffects(RenderLevelStageEvent.AfterParticles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            VoidPhasePostProcessor.resetFrame();
            return;
        }
        PoseStack poseStack = event.getPoseStack();   //拿一个姿态栈（前这一帧渲染时，专门用来记录“位置、旋转、缩放”的一叠变换）
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position(); //获取相机坐标
        float partialTick = mc.gameRenderer.getMainCamera().getPartialTickTime();//获取相机的帧间隔
        PhaseEmitterClientManager.updateBeforeRender(partialTick);
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        boolean localInVoid = mc.player.getData(ModAttachments.IN_PHASE.get());
        boolean localInPhaseDimension = PhaseDimensions.isPhaseMirror(mc.level);
        boolean phaseTransitionActive = PhaseWorldTransitionClient.isActive();
        boolean voidInOutEffectActive = VoidInOutEffectClient.isActive();
        boolean shaderPackActive = isShaderCompatMode();
          var rings = VoidRingManager.getRings();
          var trails = VoidTrailManager.getTrails();
          var beams = VoidBeamManager.getBeams();
          var blackHoles = VoidBlackHoleManager.getBlackHoles();
          boolean hasVisibleEmitters = PhaseEmitterClientManager.hasVisibleEmitters();
          if (!localInVoid
                  && !localInPhaseDimension
                  && !phaseTransitionActive
                  && !voidInOutEffectActive
                  && rings.isEmpty() && trails.isEmpty() && beams.isEmpty() && blackHoles.isEmpty()
                  && !hasVisibleEmitters) {
              VoidPhasePostProcessor.resetFrame();
              return;
          }

        VoidPhasePostProcessor.beginFrame(mc, partialTick);
        List<PreparedRingRender> preparedRings = prepareVisibleRings(mc, rings, cameraPos, partialTick, firstPerson);
        List<PreparedBlackHoleRender> preparedBlackHoles = prepareVisibleBlackHoles(mc, blackHoles, cameraPos, partialTick, firstPerson);

        int light = 0x00F000F0; //设定光照颜色亮度
          MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
          if (shaderPackActive) {
              renderTrailPass(buffers, VOID_TRAIL_WORLD_EFFECT_COMPAT, trails, poseStack, cameraPos, partialTick, light, TrailRenderPass.SHADER_COMPAT);
              renderBeamPass(buffers, VOID_TRAIL_WORLD_EFFECT_COMPAT, beams, poseStack, cameraPos, partialTick, light, BeamRenderPass.SHADER_COMPAT);
              renderRingPass(buffers, VOID_RING_WORLD_EFFECT_COMPAT, preparedRings, poseStack, light, RingRenderPass.SHADER_COMPAT);
              renderBlackHolePass(buffers, VOID_BLACK_HOLE_WORLD_EFFECT_COMPAT, preparedBlackHoles, poseStack, light, BlackHoleRenderPass.SHADER_COMPAT);
              renderTrailPass(buffers, VOID_TRAIL_GLOW_EFFECT_COMPAT, trails, poseStack, cameraPos, partialTick, light, TrailRenderPass.SHADER_GLOW);
              renderBeamPass(buffers, VOID_TRAIL_GLOW_EFFECT_COMPAT, beams, poseStack, cameraPos, partialTick, light, BeamRenderPass.SHADER_GLOW);
              PhaseEmitterClientManager.renderEmitters(buffers, PHASE_EMITTER_ORB_EFFECT_COMPAT, poseStack, cameraPos, partialTick, light, true);
              // 黑洞核心需要普通透明混合，eyes 管线里的黑色几乎没有可见输出。
          } else {
              renderTrailPass(buffers, VOID_WORLD_EFFECT, trails, poseStack, cameraPos, partialTick, light, TrailRenderPass.NORMAL);
              renderBeamPass(buffers, VOID_WORLD_EFFECT, beams, poseStack, cameraPos, partialTick, light, BeamRenderPass.NORMAL);
              renderRingPass(buffers, VOID_WORLD_EFFECT, preparedRings, poseStack, light, RingRenderPass.NORMAL);
              renderBlackHolePass(buffers, VOID_WORLD_EFFECT, preparedBlackHoles, poseStack, light, BlackHoleRenderPass.NORMAL);
              PhaseEmitterClientManager.renderEmitters(buffers, VOID_WORLD_EFFECT, poseStack, cameraPos, partialTick, light, false);
          }

        writeWorldEffects(mc, buffers, preparedRings, preparedBlackHoles, poseStack, partialTick, shaderPackActive);
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

      private static void renderBeamPass(
              MultiBufferSource.BufferSource buffers,
              RenderType renderType,
              Collection<VoidBeamInstance> beams,
              PoseStack poseStack,
              Vec3 cameraPos,
              float partialTick,
              int light,
              BeamRenderPass pass
      ) {
          if (beams.isEmpty()) {
              return;
          }

          VertexConsumer buffer = buffers.getBuffer(renderType);
          for (VoidBeamInstance beam : beams) {
              switch (pass) {
                  case NORMAL -> VoidBeamRenderer.render(poseStack, buffer, beam, cameraPos, partialTick);
                  case SHADER_COMPAT -> VoidBeamRenderer.renderShaderCompat(poseStack, buffer, beam, cameraPos, partialTick, light);
                  case SHADER_GLOW -> VoidBeamRenderer.renderShaderGlow(poseStack, buffer, beam, cameraPos, partialTick, light);
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

    private static void renderBlackHolePass(
            MultiBufferSource.BufferSource buffers,
            RenderType renderType,
            List<PreparedBlackHoleRender> blackHoles,
            PoseStack poseStack,
            int light,
            BlackHoleRenderPass pass
    ) {
        if (blackHoles.isEmpty()) {
            return;
        }

        VertexConsumer buffer = buffers.getBuffer(renderType);
        for (PreparedBlackHoleRender prepared : blackHoles) {
            poseStack.pushPose();
            poseStack.translate(
                    prepared.renderX(),
                    prepared.renderY(),
                    prepared.renderZ()
            );
            switch (pass) {
                case NORMAL -> VoidBlackHoleRenderer.render(
                        poseStack,
                        buffer,
                        prepared.blackHole(),
                        prepared.partialTick(),
                        prepared.coreFacingData(),
                        prepared.viewFacingData()
                );
                case SHADER_COMPAT -> VoidBlackHoleRenderer.renderShaderCompat(
                        poseStack,
                        buffer,
                        prepared.blackHole(),
                        prepared.partialTick(),
                        light,
                        prepared.coreFacingData(),
                        prepared.viewFacingData()
                );
                case SHADER_GLOW -> VoidBlackHoleRenderer.renderShaderGlow(
                        poseStack,
                        buffer,
                        prepared.blackHole(),
                        prepared.partialTick(),
                        light,
                        prepared.coreFacingData(),
                        prepared.viewFacingData()
                );
            }
            poseStack.popPose();
        }
        buffers.endBatch(renderType);
    }

    private static void writeWorldEffects(
            Minecraft mc,
            MultiBufferSource.BufferSource buffers,
            List<PreparedRingRender> rings,
            List<PreparedBlackHoleRender> blackHoles,
            PoseStack poseStack,
            float partialTick,
            boolean shaderPackActive
    ) {
        if (rings.isEmpty() && blackHoles.isEmpty()) {
            return;
        }

        VoidPhasePostProcessor.beginMaskWrite();
        VertexConsumer maskBuffer = buffers.getBuffer(VOID_MASK_EFFECT);

        int effectIndex = 0;
        for (PreparedRingRender prepared : rings) {
            if (!prepared.ring().preset.renderStyle().writesWorldEffect()) {
                continue;
            }
            if (effectIndex >= VoidPhasePostProcessor.MAX_EFFECTS) {
                break;
            }

            poseStack.pushPose();
            poseStack.translate(
                    prepared.renderX(),
                    prepared.renderY(),
                    prepared.renderZ()
            );
            VoidRingRenderer.applyDistortionFacingRotation(poseStack, prepared.ring(), prepared.distortionFacingData());
            VoidRingRenderer.ScreenMaskData screenMaskData = null;
            if (prepared.ring().preset.occludedByBlocks()) {
                screenMaskData = VoidRingRenderer.computeScreenMaskData(
                        mc,
                        prepared.ring(),
                        prepared.center(),
                        partialTick,
                        prepared.distortionFacingData()
                );
            }
            VoidPhasePostProcessor.writeEffectRow(
                    effectIndex,
                    prepared.ring(),
                    partialTick,
                    screenMaskData == null ? -1.0F : screenMaskData.centerDepth()
            );
            VoidRingRenderer.renderMask(poseStack, maskBuffer, prepared.ring(), partialTick, effectIndex);
            poseStack.popPose();
            effectIndex++;
        }

        for (PreparedBlackHoleRender prepared : blackHoles) {
            if (effectIndex >= VoidPhasePostProcessor.MAX_EFFECTS) {
                break;
            }

            poseStack.pushPose();
            poseStack.translate(
                    prepared.renderX(),
                    prepared.renderY(),
                    prepared.renderZ()
            );
            VoidBlackHoleRenderer.applyDistortionFacingRotation(
                    poseStack,
                    prepared.blackHole().config,
                    prepared.distortionFacingData()
            );
            VoidBlackHoleRenderer.ScreenMaskData screenMaskData = null;
            if (prepared.blackHole().config.occludedByBlocks()) {
                screenMaskData = VoidBlackHoleRenderer.computeScreenMaskData(
                        mc,
                        prepared.blackHole(),
                        prepared.center(),
                        partialTick,
                        prepared.distortionFacingData()
                );
            }
            VoidPhasePostProcessor.writeBlackHoleEffectRow(
                    effectIndex,
                    prepared.blackHole(),
                    partialTick,
                    screenMaskData == null ? -1.0F : screenMaskData.centerDepth()
            );
            VoidBlackHoleRenderer.renderMask(poseStack, maskBuffer, prepared.blackHole(), partialTick, effectIndex);
            poseStack.popPose();
            effectIndex++;
        }

        buffers.endBatch(VOID_MASK_EFFECT);
        VoidPhasePostProcessor.endMaskWrite();
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
                    VoidRingRenderer.computeFacingData(ring, center, cameraPos),
                    VoidRingRenderer.computeDistortionFacingData(ring, center, cameraPos)
            ));
        }
        return prepared;
    }

    private static List<PreparedBlackHoleRender> prepareVisibleBlackHoles(
            Minecraft mc,
            List<VoidBlackHoleInstance> blackHoles,
            Vec3 cameraPos,
            float partialTick,
            boolean firstPerson
    ) {
        if (blackHoles.isEmpty()) {
            return List.of();
        }

        List<PreparedBlackHoleRender> prepared = new ArrayList<>(blackHoles.size());
        for (VoidBlackHoleInstance blackHole : blackHoles) {
            if (!VoidPhasePostProcessor.shouldRenderBlackHole(mc, blackHole, firstPerson)) {
                continue;
            }

            Vec3 center = blackHole.getCenter(partialTick);
            prepared.add(new PreparedBlackHoleRender(
                    blackHole,
                    center,
                    center.x - cameraPos.x,
                    center.y - cameraPos.y,
                    center.z - cameraPos.z,
                    partialTick,
                    VoidBlackHoleRenderer.computeCoreFacingData(blackHole.config, center, cameraPos),
                    VoidBlackHoleRenderer.computeViewFacingData(center, cameraPos),
                    VoidBlackHoleRenderer.computeDistortionFacingData(blackHole.config, center, cameraPos)
            ));
        }
        return prepared;
    }

      private enum TrailRenderPass {
          NORMAL,
          SHADER_COMPAT,
          SHADER_GLOW
      }

      private enum BeamRenderPass {
          NORMAL,
          SHADER_COMPAT,
          SHADER_GLOW
      }

      private enum RingRenderPass {
        NORMAL,
        SHADER_COMPAT,
        SHADER_GLOW
    }

    private enum BlackHoleRenderPass {
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
            VoidRingRenderer.FacingData facingData,
            VoidRingRenderer.FacingData distortionFacingData
    ) {
    }

    private record PreparedBlackHoleRender(
            VoidBlackHoleInstance blackHole,
            Vec3 center,
            double renderX,
            double renderY,
            double renderZ,
            float partialTick,
            VoidBlackHoleRenderer.FacingData coreFacingData,
            VoidBlackHoleRenderer.FacingData viewFacingData,
            VoidBlackHoleRenderer.FacingData distortionFacingData
    ) {
    }

    @SubscribeEvent
    public static void noInvisible(RenderLivingEvent.Pre<?, ?, ?> event) {
        Boolean inVoid = event.getRenderState().getRenderData(IN_VOID_RENDER);
        Float alpha = event.getRenderState().getRenderData(VOID_PLAYER_ALPHA);//隐身效果实现
        if (Boolean.TRUE.equals(inVoid) && alpha != null && alpha <= 0.01F) {
            event.setCanceled(true);
        }
    }
}
