package com.example.voidcraft.network;

import com.example.voidcraft.Custom.Clock.VoidClock;
import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.Effect.VoidRingManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private static final String NETWORK_VERSION = "1";

    private ModNetworking() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(ModNetworking::registerPayloadHandlers);
    }

    public static void sendPhaseTear(Player player, VoidRingInstance.Preset preset) {
        if (player.level().isClientSide()) {
            return;
        }

        float scale = player.getBbHeight() / 1.8F;
        Vec3 center = player.position().add(0.0D, preset.centerYOffset() * scale, 0.0D);
        PhaseTearPayload payload = PhaseTearPayload.fromPreset(
                player.getId(),
                center.x,
                center.y,
                center.z,
                scale,
                preset
        );
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, payload);
    }

    private static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToClient(PhaseTearPayload.TYPE, PhaseTearPayload.STREAM_CODEC, ModNetworking::handlePhaseTearClient);
    }

    private static void handlePhaseTearClient(PhaseTearPayload payload, IPayloadContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        Entity entity = mc.level.getEntity(payload.entityId());
        VoidRingInstance.Preset preset = payload.toPreset();
        Vec3 center = new Vec3(payload.centerX(), payload.centerY(), payload.centerZ());
        if (mc.player != null && payload.entityId() != mc.player.getId() && entity != null) {
            VoidRingManager.addTrackedRing(
                    payload.entityId(),
                    center,
                    payload.scale(),
                    preset
            );
        } else {
            VoidRingManager.addRing(
                    payload.entityId(),
                    center,
                    payload.scale(),
                    preset
            );
        }

        if (entity instanceof Player player) {
            VoidClock.VOID_PLAYER_FLASH(player);
        }
    }
}
