package com.example.voidcraft.Item.custom.ModuleItem.ModuleType;

import com.example.voidcraft.Custom.Clock.Clock;
import com.example.voidcraft.Custom.Clock.ModuleSkillClock;
import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleData;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleItem;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleMode;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierData;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierType;
import com.example.voidcraft.ModDataComponents;
import com.example.voidcraft.Sound.ModSound;
import com.example.voidcraft.network.ModNetworking;
import com.example.voidcraft.world.PhaseWorldRules;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.voidcraft.Item.custom.ModuleItem.ModuleMode.BURST;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierType.*;

public class WorldModule extends ModuleItem {
    private static final long ENERGY_COST = 600L;
    private static final long COOLDOWN_TICKS = 0L;
    private static final int TELEPORT_FALLBACK_DELAY_TICKS = 40;
    private static final Map<UUID, PendingTransition> PENDING_TRANSITIONS = new ConcurrentHashMap<>();

    public WorldModule(Properties properties) {
        super(properties);
    }

    @Override
    protected void doUseSkill(ServerPlayer player, ItemStack watchStack, ItemStack moduleStack, int slot) {
        Stats stats = getStats(moduleStack);
        if (stats == null || stats.mode() != BURST) {
            return;
        }

        if (!ModuleSkillClock.checkCooldown(player, slot)) {
            return;
        }

        if (PENDING_TRANSITIONS.containsKey(player.getUUID())) {
            return;
        }

        ServerLevel targetLevel = resolveTargetLevel(player);
        if (targetLevel == null) {
            return;
        }

        long energyCost = stats.offEnergy();
        if(!(ModuleSkillClock.tryUseEnergy(player,energyCost))){
            return;
        }
        if (player.isPassenger()) {
            player.stopRiding();
        }
        ResourceKey<Level> sourceDimension = player.level().dimension();
        ResourceKey<Level> targetDimension = targetLevel.dimension();
        ModuleSkillClock.setCooldown(player, slot, stats.cooldownTicks());
        PENDING_TRANSITIONS.put(player.getUUID(), new PendingTransition(sourceDimension));
        ModNetworking.sendPhaseWorldTransition(player, sourceDimension, targetDimension);
        Clock.addClock(
                TELEPORT_FALLBACK_DELAY_TICKS,
                () -> completePendingTransition(player)
        );
        ModNetworking.sendPhaseTear(player, VoidRingInstance.Preset.DEFAULT);
    }

    public static void completePendingTransition(ServerPlayer player) {
        PendingTransition pending = PENDING_TRANSITIONS.remove(player.getUUID());
        if (pending == null) {
            return;
        }

        executeWorldTransition(player, pending.sourceDimension());
    }

    private static void executeWorldTransition(ServerPlayer player, ResourceKey<Level> sourceDimension) {
        if (player.isRemoved() || player.isDeadOrDying() || player.level().dimension() != sourceDimension) {
            return;
        }

        ServerLevel targetLevel = resolveTargetLevel(player);
        if (targetLevel == null) {
            return;
        }

        if (player.isPassenger()) {
            player.stopRiding();
        }

        Vec3 targetPos = PhaseWorldRules.findArrivalPos(targetLevel, player.position());
        ServerPlayer teleported = player.teleport(new TeleportTransition(
                targetLevel,
                targetPos,
                player.getDeltaMovement(),
                player.getYRot(),
                player.getXRot(),
                Set.<Relative>of(),
                TeleportTransition.DO_NOTHING
        ));

        if (teleported == null) {
            return;
        }

        ModSound.playEnterVoid(teleported.level(), teleported);
        ModNetworking.sendPhaseTear(teleported, VoidRingInstance.Preset.DEFAULT);
    }

    private static ServerLevel resolveTargetLevel(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return null;
        }

        ResourceKey<Level> targetDimension = PhaseWorldRules.resolveTransitionTarget(player.level().dimension());
        if (targetDimension == null) {
            return null;
        }

        return server.getLevel(targetDimension);
    }

    public static Stats getStats(ItemStack moduleStack) {
        ModuleData data = moduleStack.get(ModDataComponents.MODULE_DATA.get());
        if (data == null) {
            return null;
        }

        float cooldownReduction = 1.0F;
        float offEnergy = 1.0F;
        List<ModuleModifierData> modifiers = data.modifiers();
        for (ModuleModifierData modifier : modifiers) {
            ModuleModifierType modifierType = modifier.type();
            if (modifierType == COOLDOWN_REDUCTION) {
                cooldownReduction += 0.15F * modifier.level();
            }
            if(modifierType == ACTIVE_DURATION){
                offEnergy += 0.2F * modifier.level();
            }
            if(modifierType == SPEED_BOOST){
                offEnergy += 0.2F * modifier.level();
            }

        }

        return new Stats(data.moduleMode(), cooldownReduction,(long)(ENERGY_COST/offEnergy));
    }

    @Override
    public boolean canUseMode(ModuleMode mode) {
        return mode == BURST;
    }

    @Override
    protected Component getModifierDisplayName(ModuleModifierData modifierData) {
        ModuleModifierType modifierType = modifierData.type();
        if (modifierType == ACTIVE_DURATION || modifierType == SPEED_BOOST) {
            return Component.translatable("module_modifier_type.void_craft.energy_reduction");
        }

        return super.getModifierDisplayName(modifierData);
    }

    public record Stats(ModuleMode mode, float cooldownReduction,long offEnergy) {

        public long cooldownTicks() {
            return Math.max(0L, (long) (COOLDOWN_TICKS / Math.max(0.001F, cooldownReduction)));
        }
    }

    private record PendingTransition(ResourceKey<Level> sourceDimension) {
    }
}
