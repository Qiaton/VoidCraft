package com.example.voidcraft.Item.custom.ModuleItem.ModuleType;

import com.example.voidcraft.Custom.Clock.ModuleSkillClock;
import com.example.voidcraft.Custom.Clock.VoidClock;
import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.Item.custom.ModuleItem.*;
import com.example.voidcraft.ModAttachments;
import com.example.voidcraft.ModDataComponents;
import com.example.voidcraft.Sound.ModSound;
import com.example.voidcraft.Network.ModNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.example.voidcraft.Item.custom.ModuleItem.ModuleMode.*;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierType.*;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleStatHelper.addLess;

public class VoidModule extends ModuleItem {
    private static final long ENERGY_COST = 25L;
    private static final long COOLDOWN_TICKS = 600L;
    private static final Integer VOID_TICK = 50;
    private static final float DEFAULT_MOVE_SPEED_OFFSET = 1F;
    public static final float MOVE_SPEED = 0.15F;


    public VoidModule(Properties properties) {
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
            if(ModuleSkillClock.hasChannel(player,slot)){
                ModuleSkillClock.stopChannel(player,slot);
                VoidClock.stopVoid(player);
            }
            else{
                if(!ModuleSkillClock.tryUseEnergy(player,offEnergy)){
                    return;
                }
                ModNetworking.sendPhaseTear(player,VoidRingInstance.Preset.DEFAULT);
                ModSound.playEnterVoid(level, player);
                ModuleSkillClock.startChannel(player,slot,offEnergy);
            }
        }
        if(mode == BURST){
            boolean cooldownReady = ModuleSkillClock.canUseNow(player,slot);
            if(!cooldownReady){
                if(VoidClock.getInVoid(player)){
                    VoidClock.stopVoid(player);
                    return;
                }
                if(!ModuleSkillClock.tryUseEnergy(player,stats.channelEnergyCost()*38)) {
                    return;
                }
            }
            ModNetworking.sendPhaseTear(player, VoidRingInstance.Preset.DEFAULT);
            ModSound.playEnterVoid(level, player);
            if(cooldownReady){
                ModuleSkillClock.setCooldown(player, slot, stats.cooldownTicks());
            }
            VoidClock.setVoidTicks(player, stats.activeTicks());
        }
    }

    public static Stats getStats(ItemStack moduleStack) {
        ModuleData data = moduleStack.get(ModDataComponents.MODULE_DATA);
        if(data == null) return null;

        float cooldownDuration = 1F;
        float energyEfficiency = 1F;
        float activeDuration = 1F;
        float moveSpeedOffset = DEFAULT_MOVE_SPEED_OFFSET;
        List<ModuleModifierData> modifiers = data.modifiers();
        moveSpeedOffset -= moveSpeedOffset * data.level() * 0.1F;

        for(ModuleModifierData modifier : modifiers){
            ModuleModifierType modifierType = modifier.type();
            if(modifierType == null) continue;
            if(modifierType == COOLDOWN_REDUCTION){
                cooldownDuration += data.level()*0.1F;
                cooldownDuration = addLess(cooldownDuration, modifier.level(), 0.15F);
            }
            if(modifierType == SPEED_BOOST){
                moveSpeedOffset -= moveSpeedOffset * modifier.level() * 0.1F;
            }
            if(modifierType == ACTIVE_DURATION){
                activeDuration += modifier.level()*0.15F+data.level()*0.1F;
                energyEfficiency = addLess(addLess(energyEfficiency, data.level(), 0.15F), modifier.level(), 0.3F);
                activeDuration += activeDuration * modifier.level() * 0.3F;
            }
        }

        return new Stats(data.moduleMode(), cooldownDuration, energyEfficiency, activeDuration, moveSpeedOffset);
    }

    public record Stats(ModuleMode mode, float cooldownDuration, float energyEfficiency, float activeDuration, float moveSpeedOffset) {
        public long channelEnergyCost() {
            return (long)(ENERGY_COST / cooldownDuration / energyEfficiency);
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
    }
}
