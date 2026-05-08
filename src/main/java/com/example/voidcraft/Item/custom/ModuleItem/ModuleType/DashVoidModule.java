package com.example.voidcraft.Item.custom.ModuleItem.ModuleType;


import com.example.voidcraft.ClientCustom.FlowEffect;
import com.example.voidcraft.ClientCustom.ModuleInputMode;
import com.example.voidcraft.Custom.Clock.DashClock;
import com.example.voidcraft.Custom.Clock.ModuleSkillClock;
import com.example.voidcraft.Custom.Clock.VoidClock;
import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.Item.custom.ModuleItem.*;
import com.example.voidcraft.ModDataComponents;
import com.example.voidcraft.Sound.ModSound;
import com.example.voidcraft.network.ModNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.example.voidcraft.Item.custom.ModuleItem.ModuleMode.*;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierType.*;

public class DashVoidModule extends ModuleItem {
    private static final long ENERGY_COST = 40L;
    private static final long COOLDOWN_TICKS = 300L;
    private static final Integer VOID_TICK = 25;
    public static final float DASH_SPEED = 3F;


    public DashVoidModule(Properties properties) {
        super(properties);
    }
    @Override
    protected void doUseSkill(ServerPlayer player, ItemStack watchStack, ItemStack moduleStack, int slot) {
        Stats stats = getStats(moduleStack);
        Level level = player.level();
        if(stats == null) return;

        ModuleMode mode = stats.mode();
        if(mode == null) return;
        if(mode == CHANNEL){
            long offEnergy = stats.channelEnergyCost();
            if(ModuleSkillClock.hasChannel(player,slot)){
                ModuleSkillClock.stopChannel(player,slot);
                VoidClock.stopVoid(player);
                DashClock.clearDash(player);
                player.setDeltaMovement(0,0,0);
                FlowEffect.fov_effect=0;
            }
            else{
                if(!ModuleSkillClock.tryUseEnergy(player,offEnergy)){
                    return;
                }
                ModSound.playEnterVoid(level, player);
                ModNetworking.sendPhaseTear(player, VoidRingInstance.Preset.DEFAULT); //相位裂缝动画
                ModuleSkillClock.startChannel(player,slot,offEnergy);
                DashClock.setDash(player,2, stats.strength());

            }
        }
        if(mode == BURST){
            if(!ModuleSkillClock.canUseNow(player,slot)){
                return;
            }
            ModSound.playEnterVoid(level, player);
            ModNetworking.sendPhaseTear(player, VoidRingInstance.Preset.DEFAULT); //相位裂缝动画
            int activeTicks = stats.activeTicks();
            DashClock.setDash(player,activeTicks, stats.strength());
            ModuleSkillClock.setCooldown(player, slot, COOLDOWN_TICKS);
            VoidClock.setVoidTicks(player,activeTicks);
        }
    }

    public static Stats getStats(ItemStack moduleStack) {
        ModuleData data = moduleStack.get(ModDataComponents.MODULE_DATA);
        if(data == null) return null;

        float cooldownDuration = 1F;
        float activeDuration = 1F;
        float dashSpeed = 1F;
        List<ModuleModifierData> modifiers = data.modifiers();

        for(ModuleModifierData modifier : modifiers){
            ModuleModifierType modifierType = modifier.type();
            if(modifierType == null) continue;
            if(modifierType == COOLDOWN_REDUCTION){
                cooldownDuration += 0.15F*(float)(modifier.level());
            }
            if(modifierType == SPEED_BOOST){
                dashSpeed += (float) (0.15F*(modifier.level())+0.4*(data.level()));
            }
            if(modifierType == ACTIVE_DURATION){
                activeDuration += 0.3F*(float)(modifier.level());
            }
        }

        return new Stats(data.moduleMode(), cooldownDuration, activeDuration, dashSpeed);
    }

    public record Stats(ModuleMode mode, float cooldownDuration, float activeDuration, float dashSpeed) {
        public long channelEnergyCost() {
            return (long)(ENERGY_COST / cooldownDuration / activeDuration);
        }

        public int activeTicks() {
            return (int)(VOID_TICK * activeDuration);
        }

        public float strength() {
            return dashSpeed * DASH_SPEED;
        }
    }
    @Override
    public ModuleInputMode getInputMode(){
        return ModuleInputMode.CLICK;                                      // Dash 的 CHANNEL 是点按开关，不走 Blink 的长按释放链
    }
}
