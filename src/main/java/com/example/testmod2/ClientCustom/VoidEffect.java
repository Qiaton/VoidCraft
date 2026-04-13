package com.example.testmod2.ClientCustom;

import com.example.testmod2.ModAttachments;
import com.example.testmod2.TestMod2;
import com.google.common.reflect.TypeToken;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

@EventBusSubscriber(modid = TestMod2.MODID, value = Dist.CLIENT)
public class VoidEffect {

    public static final ContextKey<Boolean> IN_VOID_RENDER =
            new ContextKey<>(Identifier.fromNamespaceAndPath(TestMod2.MODID, "no_invisible"));

    @SubscribeEvent
    public static void NO_INVISIBLE(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(
                new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>() {},
                (entity, state) -> {
                    if (entity instanceof Player player) {      //隐身效果
                        boolean inVoid = player.getData(ModAttachments.IN_VOID.get());

                        state.setRenderData(IN_VOID_RENDER, inVoid);

                        if (inVoid) {               //清除影子
                            state.shadowRadius = 0.0F;
                            state.shadowPieces.clear();
                        }
                    }
                }
        );
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Boolean inVoid = event.getRenderState().getRenderData(IN_VOID_RENDER);

        if (Boolean.TRUE.equals(inVoid)) {
            event.setCanceled(true);
        }
    }
}