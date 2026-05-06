package com.example.voidcraft.world;

import com.example.voidcraft.VoidCraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = VoidCraft.MODID)
public final class PhasePlayerStateHandler {
    private static final Map<UUID, AbilitySnapshot> PHASE_ABILITY_SNAPSHOTS = new HashMap<>();

    private PhasePlayerStateHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() && PhaseWorldRules.shouldApplyPhaseTraversal(player.level())) {
            applyClientPhaseTraversal(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (PhaseWorldRules.shouldApplyPhaseTraversal(player.level())) {
            applyServerPhaseTraversal(player);
        } else {
            restoreServerAbilities(player, false);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (PhaseWorldRules.shouldApplyPhaseTraversal(player.level())) {
            applyServerPhaseTraversal(player);
        } else {
            restoreServerAbilities(player, PhaseDimensions.isPhaseMirror(event.getFrom()));
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (PhaseWorldRules.shouldApplyPhaseTraversal(player.level())) {
            applyServerPhaseTraversal(player);
        } else {
            restoreServerAbilities(player, false);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && PhaseWorldRules.shouldApplyPhaseTraversal(player.level())) {
            applyServerPhaseTraversal(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            restoreServerAbilities(player, false);
        }
    }

    public static boolean shouldPhaseThrough(Player player) {
        return player != null && PhaseWorldRules.shouldApplyPhaseTraversal(player.level());
    }

    private static void applyServerPhaseTraversal(ServerPlayer player) {
        PHASE_ABILITY_SNAPSHOTS.computeIfAbsent(player.getUUID(), ignored -> AbilitySnapshot.capture(player));
        boolean changed = applyPhaseAbilities(player);
        applyPhasePhysics(player);

        if (changed) {
            player.onUpdateAbilities();
        }
    }

    private static void applyClientPhaseTraversal(Player player) {
        applyPhaseAbilities(player);
        applyPhasePhysics(player);
    }

    private static boolean applyPhaseAbilities(Player player) {
        Abilities abilities = player.getAbilities();
        boolean changed = false;

        if (!abilities.mayfly) {
            abilities.mayfly = true;
            changed = true;
        }

        if (!abilities.flying) {
            abilities.flying = true;
            changed = true;
        }

        return changed;
    }

    private static void applyPhasePhysics(Player player) {
        player.noPhysics = true;
        player.setOnGround(false);
        player.resetFallDistance();
    }

    private static void restoreServerAbilities(ServerPlayer player, boolean forceRemoveSurvivalFlight) {
        AbilitySnapshot snapshot = PHASE_ABILITY_SNAPSHOTS.remove(player.getUUID());
        if (snapshot == null && !forceRemoveSurvivalFlight) {
            return;
        }

        Abilities abilities = player.getAbilities();
        boolean mayfly = snapshot != null ? snapshot.mayfly : abilities.mayfly;
        boolean flying = snapshot != null ? snapshot.flying : abilities.flying;

        if ((forceRemoveSurvivalFlight || (snapshot != null && snapshot.forceRemoveOnExit))
                && shouldForceRemovePhaseFlight(player)) {
            mayfly = false;
            flying = false;
        }

        boolean changed = abilities.mayfly != mayfly || abilities.flying != flying;
        abilities.mayfly = mayfly;
        abilities.flying = flying;
        player.noPhysics = player.isSpectator();

        if (changed) {
            player.onUpdateAbilities();
        }
    }

    private static boolean shouldForceRemovePhaseFlight(ServerPlayer player) {
        GameType gameMode = player.gameMode.getGameModeForPlayer();
        return gameMode != GameType.SPECTATOR && !gameMode.isCreative();
    }

    private record AbilitySnapshot(boolean mayfly, boolean flying, boolean forceRemoveOnExit) {
        private static AbilitySnapshot capture(ServerPlayer player) {
            Abilities abilities = player.getAbilities();
            return new AbilitySnapshot(abilities.mayfly, abilities.flying, shouldForceRemovePhaseFlight(player));
        }
    }
}
