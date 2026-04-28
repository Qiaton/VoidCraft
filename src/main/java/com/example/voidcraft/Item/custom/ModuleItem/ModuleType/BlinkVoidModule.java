package com.example.voidcraft.Item.custom.ModuleItem.ModuleType;

import com.example.voidcraft.ClientCustom.ModuleInputMode;
import com.example.voidcraft.Custom.Clock.ModuleSkillClock;
import com.example.voidcraft.Custom.Clock.VoidClock;
import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.Item.custom.ModuleItem.*;
import com.example.voidcraft.ModDataComponents;
import com.example.voidcraft.Sound.ModSound;
import com.example.voidcraft.network.ModNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.example.voidcraft.Item.custom.ModuleItem.ModuleMode.BURST;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierType.*;

public class BlinkVoidModule extends ModuleItem {
    private static final long ENERGY_COST = 40L;
    private static final long COOLDOWN_TICKS = 0L;
    private static final Integer VOID_TICK = 25;
    public static final float DASH_SPEED = 3F;
    public static final double DISTANCE_PER_TICK = 0.8D;
    public static final double MAX_DISTANCE = 22.0D;

    public BlinkVoidModule(Properties properties) {
        super(properties);
    }
    @Override
    protected void doUseSkill(ServerPlayer player, ItemStack watchStack, ItemStack moduleStack, int slot) {
        releaseBlink(player, watchStack, moduleStack, slot, 40);           // 兼容老的点击入口：没有蓄力 tick 时按满蓄力处理
    }

    public void releaseBlink(ServerPlayer player, ItemStack watchStack, ItemStack moduleStack, int slot, int ticks) {
        if(watchStack.isEmpty()) return;                                   // 手表不存在就不释放
        if(player == null) return;                                         // 服务端玩家不存在就不释放
        useBlink(player, moduleStack, slot, ticks);                         // 真正的 Blink 规则集中放在一个方法里
    }

    private void useBlink(ServerPlayer player, ItemStack moduleStack, int slot, int ticks) {
        Stats stats = getStats(moduleStack);
        Level level = player.level();
        if(stats == null) return;

        ModuleMode mode = stats.mode();
        if(mode == null) return;
        if(mode == BURST){
            if(!ModuleSkillClock.checkCooldown(player,slot)){              // 先检查这个技能槽是否还在冷却
                return;
            }
            setBlink(player,ticks);                                        // 用松手时传来的蓄力 tick 计算闪现距离
            ModSound.playEnterVoid(level, player);
            ModNetworking.sendPhaseTear(player, VoidRingInstance.Preset.DEFAULT); //相位裂缝动画
            ModuleSkillClock.setCooldown(player, slot, COOLDOWN_TICKS);    // 成功释放后，给这个技能槽加冷却

        }
    }
    public static void setBlink(ServerPlayer player,int ticks) {
        int safeTicks = Mth.clamp(ticks, 0, 40);                            // 服务端限制蓄力 tick，避免客户端乱发超远距离

        double distance = Math.min(safeTicks * DISTANCE_PER_TICK, MAX_DISTANCE);                // 0.3 格/ tick，最多 12 格

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        Vec3 eyeTarget = eye.add(look.scale(distance));                    // 先算眼睛应该到的位置
        Vec3 feetTarget = eyeTarget.subtract(0.0D, player.getEyeHeight(), 0.0D); // teleport 要传脚底坐标，所以减掉眼高
        ModNetworking.sendPhaseTear(player,VoidRingInstance.Preset.DEFAULT);
        VoidClock.SET_VOID_TICKS(player, 3);
        player.connection.teleport(
                feetTarget.x,
                feetTarget.y,
                feetTarget.z,
                player.getYRot(),
                player.getXRot()
        );
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
        return ModuleInputMode.HOLD_RELEASE;
    }
    @Override
     public boolean canUseMode(ModuleMode mode) {
        return BURST==mode;
    }
}
