package com.example.voidcraft.Item.custom.ModuleItem.ModuleType;

import com.example.voidcraft.Custom.Clock.ModuleSkillClock;
import com.example.voidcraft.Custom.Clock.VoidClock;
import com.example.voidcraft.Item.custom.ModuleItem.*;
import com.example.voidcraft.ModAttachments;
import com.example.voidcraft.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.List;

import static com.example.voidcraft.Item.custom.ModuleItem.ModuleMode.*;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierType.*;

public class VoidModule extends ModuleItem {

    public VoidModule(Properties properties) {
        super(properties);
    }
    @Override
    protected void doUseSkill(ServerPlayer player, ItemStack gauntletStack, ItemStack moduleStack, int slot) {
        ModuleData data = moduleStack.get(ModDataComponents.MODULE_DATA);
        float cooldownDuration = 1F;
        float speedBoost = 1F;
        float activeDuration = 1F;
        if(data == null) return;
        System.out.println("技能子阶段：空数据检验完毕");
        List<ModuleModifierData> modifiers = data.modifiers();
        for(ModuleModifierData modifier : modifiers){
            ModuleModifierType modifierType = modifier.type();
            if(modifierType == null) continue;
            if(modifierType == COOLDOWN_REDUCTION){
               cooldownDuration += 0.15F*(float)(modifier.level());
            }
            if(modifierType == SPEED_BOOST){
                speedBoost += 0.1F*(float)(modifier.level());
            }
            if(modifierType == ACTIVE_DURATION){
                activeDuration += 0.3F*(float)(modifier.level());
            }
        }
        ModuleMode mode = data.moduleMode();
        if(mode == null) return;
        if(mode == BLINK){
            VoidClock.VOID_TICKS.put(player.getUUID(),(int)(50*activeDuration));
        }
        if(mode == CHANNEL){
            VoidClock.VOID_TICKS.put(player.getUUID(),(int)(50*activeDuration));
        }
        if(mode == BURST){
            System.out.println("进入burst使用环节");
            long cooldown = ModuleSkillClock.getCooldown(player,slot);
            System.out.println("获取槽位冷却");
            if(cooldown > 0){
                return;
            }else {
                ModuleSkillClock.setCooldown(player, slot, 200);
                System.out.println("已设置冷却");
                VoidClock.SET_VOID_TICKS(player,(int)(50*activeDuration));
                System.out.println("已释放效果");
            }
        }
    }
}
