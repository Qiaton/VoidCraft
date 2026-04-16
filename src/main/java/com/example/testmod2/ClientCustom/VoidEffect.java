package com.example.testmod2.ClientCustom;

import com.example.testmod2.Custom.Clock.VoidClock;
import com.example.testmod2.Effect.VoidRingInstance;
import com.example.testmod2.Effect.VoidRingManager;
import com.example.testmod2.Effect.VoidRingRenderer;
import com.example.testmod2.ModAttachments;
import com.example.testmod2.TestMod2;
import com.google.common.reflect.TypeToken;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
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
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;


@EventBusSubscriber(modid = TestMod2.MODID, value = Dist.CLIENT)
public class VoidEffect {

    public static final ContextKey<Boolean> IN_VOID_RENDER =
            new ContextKey<>(Identifier.fromNamespaceAndPath(TestMod2.MODID, "no_invisible"));
    public static final ContextKey<Float> VOID_PLAYER_ALPHA =
            new ContextKey<>(Identifier.fromNamespaceAndPath(TestMod2.MODID,"void_player_alpha"));
    private static final Identifier VOID_PLAYER_TEXTURE =
            Identifier.fromNamespaceAndPath(TestMod2.MODID, "textures/entity/void_player.png");
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
                            float progress = (1- (float) left / VoidClock.VOID_PLAYER_FLASH_TOTAL);
                            float alpha = 1-progress;
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
        if(mc.level == null) return;
        VoidRingManager.clientTick();
    }
    @SubscribeEvent
    public static void VOID_RING(RenderLevelStageEvent.AfterParticles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;   //检测是否在世界内
        if(VoidRingManager.getRings().isEmpty()) return;  //检测有没有圆环实例
        PoseStack poseStack = event.getPoseStack();   //拿一个姿态栈（前这一帧渲染时，专门用来记录“位置、旋转、缩放”的一叠变换）
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position(); //获取相机坐标
        float partialTick = mc.gameRenderer.getMainCamera().getPartialTickTime();//获取相机的帧间隔
        int light = 0x00F000F0; //设定光照颜色亮度
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        for(VoidRingInstance ring : VoidRingManager.getRings()) {  //遍历所有存在的圆环
            poseStack.pushPose();      //把当前姿态存档一下
            poseStack.translate(
                    ring.center.x - cameraPos.x,
                    ring.center.y - cameraPos.y,        //吧posestack里的坐标纠正成圆环真正的位置
                    ring.center.z - cameraPos.z);
            var buffer = buffers.getBuffer(RenderTypes.debugQuads());
            VoidRingRenderer.render(poseStack, buffer, ring, partialTick, light);
            poseStack.popPose();
        }
        buffers.endBatch(RenderTypes.debugQuads());
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
