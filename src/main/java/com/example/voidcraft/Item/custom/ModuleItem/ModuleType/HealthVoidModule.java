package com.example.voidcraft.Item.custom.ModuleItem.ModuleType;

import com.example.voidcraft.Custom.Clock.ModuleSkillClock;
import com.example.voidcraft.Custom.Clock.VoidClock;
import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.Item.custom.ModuleItem.*;
import com.example.voidcraft.ModAttachments;
import com.example.voidcraft.ModDataComponents;
import com.example.voidcraft.Sound.ModSound;
import com.example.voidcraft.network.ModNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.example.voidcraft.Item.custom.ModuleItem.ModuleMode.*;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierType.*;

public class HealthVoidModule extends ModuleItem {
    private static final long ENERGY_COST = 25L;
    private static final long COOLDOWN_TICKS = 600L;
    private static final Integer VOID_TICK = 50;
    private static final float DEFAULT_MOVE_SPEED_OFFSET = 1F;
    public static final float MOVE_SPEED = 0.15F;
    private static final float HEALTH_DEFAULT = 4F;
    private static final float HEALTH_BOOST = 1F;

    public HealthVoidModule(Properties properties) {
        super(properties);
    }
    @Override
    protected void doUseSkill(ServerPlayer player, ItemStack watchStack, ItemStack moduleStack, int slot) {
        Stats stats = getStats(moduleStack);
        Level level = player.level();
        if(stats == null) return;

        player.setData(ModAttachments.VOID_SPEED.get(), stats.voidSpeed());

        ModuleMode mode = stats.mode();
        if(mode == null) return;
        if(mode == CHANNEL){
            long offEnergy = stats.channelEnergyCost();
            if(ModuleSkillClock.getChannel(player,slot)){
                ModuleSkillClock.stopChannel(player,slot);
                VoidClock.STOP_VOID(player);
            }
            else{
                if(!ModuleSkillClock.tryUseEnergy(player,offEnergy)){
                    return;
                }
                ModSound.playEnterVoid(level, player);
                ModNetworking.sendPhaseTear(player,VoidRingInstance.Preset.DEFAULT);
                ModuleSkillClock.startChannel(player,slot,offEnergy);
            }
        }
        if(mode == BURST){
            if(!ModuleSkillClock.checkCooldown(player,slot)){
                return;
            }
            ModSound.playEnterVoid(level, player);
            player.heal(stats.burstHealAmount());
            ModNetworking.sendPhaseTear(player, VoidRingInstance.Preset.DEFAULT);
            ModuleSkillClock.setCooldown(player, slot, stats.cooldownTicks());
            VoidClock.SET_VOID_TICKS(player, stats.activeTicks());
        }
    }

    public static Stats getStats(ItemStack moduleStack) {
        ModuleData data = moduleStack.get(ModDataComponents.MODULE_DATA);
        if(data == null) return null;

        float cooldownDuration = 1F;
        float activeDuration = 1F;
        float moveSpeedOffset = DEFAULT_MOVE_SPEED_OFFSET;
        List<ModuleModifierData> modifiers = data.modifiers();

        for(ModuleModifierData modifier : modifiers){
            ModuleModifierType modifierType = modifier.type();
            if(modifierType == null) continue;
            if(modifierType == COOLDOWN_REDUCTION){
                cooldownDuration += 0.2F*(float)(modifier.level());
            }
            if(modifierType == SPEED_BOOST){
                moveSpeedOffset = 0.1F*(float)(modifier.level());
            }
            if(modifierType == ACTIVE_DURATION){
                activeDuration += 0.15F*(float)(modifier.level());
            }
        }

        return new Stats(data.moduleMode(), cooldownDuration, activeDuration, moveSpeedOffset);
    }

    public record Stats(ModuleMode mode, float cooldownDuration, float activeDuration, float moveSpeedOffset) {
        public long channelEnergyCost() {
            return (long)(ENERGY_COST / cooldownDuration / activeDuration);
        }

        public long cooldownTicks() {
            return (long)(COOLDOWN_TICKS / cooldownDuration);
        }

        public int activeTicks() {
            return (int)(VOID_TICK * activeDuration);
        }

        public float voidSpeed() {
            return MOVE_SPEED / Math.max(0.001F, moveSpeedOffset);
        }

        public float burstHealAmount() {
            return HEALTH_BOOST * HEALTH_DEFAULT;
        }

        public float channelHealAmount() {
            return burstHealAmount() * 0.1F;
        }
    }
}
