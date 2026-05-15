package com.example.voidcraft.Item.custom.ModuleItem.ModuleType;

import com.example.voidcraft.ClientCustom.ModuleInputMode;
import com.example.voidcraft.Custom.Clock.ModuleSkillClock;
import com.example.voidcraft.Custom.Clock.VoidClock;
import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.Effect.VoidTrailInstance;
import com.example.voidcraft.Item.custom.ModuleItem.*;
import com.example.voidcraft.ModDataComponents;
import com.example.voidcraft.Sound.ModSound;
import com.example.voidcraft.Network.ModNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.example.voidcraft.Item.custom.ModuleItem.ModuleMode.BURST;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierType.*;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleStatHelper.addLess;

public class BlinkVoidModule extends ModuleItem {
    private static final long COOLDOWN_TICKS = 50L;
    public static final double DISTANCE_PER_TICK = 1D;
    public static final double MAX_DISTANCE = 5.0D;
    public static final float ENERGY_COST = 200;
    private static final VoidTrailInstance.Preset BLINK_TRAIL = VoidTrailInstance.Preset.DEFAULT.toBuilder()
            .lifetimeTicks(36)
            .pointSpacing(0.04F)
            .maxInterpolationSteps(18)
            .width(0.2F)
            .height(0.56F)
            .tailWidthScale(0.58F)
            .tailHeightScale(0.72F)
            .edgeFadeRatio(0.62F)
            .ribbonFadeSegments(9)
            .headFadeRatio(0.22F)
            .glowWidthMultiplier(1.75F)
            .glowHeightMultiplier(1.18F)
            .alpha(0.62F)
            .glowAlpha(0.36F)
            .shaderCompatBloomAlphaScale(0.62F)
            .shaderCompatBloomWidthScale(0.94F)
            .shaderCompatBloomHeightScale(0.9F)
            .build();

    public BlinkVoidModule(Properties properties) {
        super(properties);
    }
    @Override
    protected void doUseSkill(ServerPlayer player, ItemStack watchStack, ItemStack moduleStack, int slot) {
        Vec3 target = player.getEyePosition().add(player.getLookAngle().scale(MAX_DISTANCE));
        releaseBlink(player, watchStack, moduleStack, slot, 40, target.x, target.y, target.z); // 兼容老的点击入口：没有蓄力 tick 时按满蓄力处理
    }

    public void releaseBlink(ServerPlayer player, ItemStack watchStack, ItemStack moduleStack, int slot, int ticks, double x, double y, double z) {
        if(watchStack.isEmpty()) return;                                   // 手表不存在就不释放
        if(player == null) return;                                         // 服务端玩家不存在就不释放
        useBlink(player, moduleStack, slot, ticks, new Vec3(x, y, z));       // 真正的 Blink 规则集中放在一个方法里
    }

    private void useBlink(ServerPlayer player, ItemStack moduleStack, int slot, int ticks, Vec3 target) {
        Stats stats = getStats(moduleStack);
        Level level = player.level();
        if(stats == null) return;
        if(level.isClientSide()) return;
        ModuleMode mode = stats.mode();
        if(mode == null) return;
        if(mode == BURST){
            if (!canBlinkTo(player, ticks, stats, target)) {
                return;
            }
            if(!ModuleSkillClock.canUseNow(player,slot)){              // 先检查这个技能槽是否还在冷却
                if(ModuleSkillClock.tryUseEnergy(player,(long)stats.energyCost())){

                }
                else{
                    return;
                }
            }
            else{
                ModuleSkillClock.setCooldown(player, slot, (long) (COOLDOWN_TICKS/stats.cooldownDuration));
            }
            ModNetworking.sendPhaseTear(player, VoidRingInstance.Preset.DEFAULT); //相位裂缝动画
            setBlink(player, target, stats);                                      // 用客户端预览目标点传送，距离已经由服务端验算
            ModSound.playEnterVoid(level, player);


        }
    }
    public static boolean canBlinkTo(ServerPlayer player, int ticks, Stats stats, Vec3 target) {
        if (!Double.isFinite(target.x) || !Double.isFinite(target.y) || !Double.isFinite(target.z)) {
            return false;
        }
        int safeTicks = Mth.clamp(ticks, 0, 40);
        double maxDistance = Math.min(safeTicks * DISTANCE_PER_TICK * stats.Speed, MAX_DISTANCE * stats.maxDistance);
        return player.getEyePosition().distanceTo(target) <= maxDistance + 3.5D;
    }
    public static void setBlink(ServerPlayer player, Vec3 target, Stats stats) {
        Vec3 feetTarget = target.subtract(0.0D, player.getEyeHeight(), 0.0D); // teleport 要传脚底坐标，所以减掉眼高
        VoidTrailInstance.Preset trailPreset = BLINK_TRAIL;
        float trailScale = Math.max(0.01F, player.getBbHeight() / 1.8F);
        Vec3 trailStart = player.position().add(0.0D, trailPreset.centerYOffset() * trailScale, 0.0D);
        Vec3 trailEnd = feetTarget.add(0.0D, trailPreset.centerYOffset() * trailScale, 0.0D);
        if (player.level() instanceof ServerLevel serverLevel) {
            ModNetworking.sendTrailSegment(serverLevel, player.getId(), trailStart, trailEnd, trailScale, trailPreset);
        }
        ModNetworking.sendPhaseTear(player,VoidRingInstance.Preset.DEFAULT);
        VoidClock.setVoidTicks(player, 2);
        player.connection.teleport(
                feetTarget.x,
                feetTarget.y,
                feetTarget.z,
                player.getYRot(),
                player.getXRot()
        );
        player.resetFallDistance();
    }
    public static Stats getStats(ItemStack moduleStack) {
        ModuleData data = moduleStack.get(ModDataComponents.MODULE_DATA.get());
        if(data == null) return null;

        float cooldownDuration = 1F;
        float maxDistance = 1F+data.level();
        float Speed = 1F+ data.level();
        List<ModuleModifierData> modifiers = data.modifiers();

        for(ModuleModifierData modifier : modifiers){
            ModuleModifierType modifierType = modifier.type();
            if(modifierType == null) continue;
            if(modifierType == COOLDOWN_REDUCTION){
                cooldownDuration = addLess(cooldownDuration, modifier.level(), 0.15F);
            }
            if(modifierType == SPEED_BOOST){
                Speed += Speed * modifier.level() * 0.15F;
            }
            if(modifierType == ACTIVE_DURATION){
                maxDistance += maxDistance * modifier.level() * 0.3F;
            }
        }


        return new Stats(data.moduleMode(), cooldownDuration, maxDistance, Speed);
    }

    public record Stats(ModuleMode mode, float cooldownDuration, float maxDistance, float Speed) {
    public float energyCost(){
        return ENERGY_COST/cooldownDuration;
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

    @Override
    protected Component getModifierDisplayName(ModuleModifierData modifierData) {
        if (modifierData.type() == SPEED_BOOST) {
            return Component.translatable("module_modifier_type.void_craft.charge_speed");
        }
        if (modifierData.type() == ACTIVE_DURATION) {
            return Component.translatable("module_modifier_type.void_craft.distance_bonus");
        }
        return super.getModifierDisplayName(modifierData);
    }
}
