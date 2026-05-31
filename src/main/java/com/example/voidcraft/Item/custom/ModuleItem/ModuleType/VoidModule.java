package com.example.voidcraft.Item.custom.ModuleItem.ModuleType;

import com.example.voidcraft.Custom.Clock.ModuleSkillClock;
import com.example.voidcraft.Custom.Clock.VoidClock;
import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.Item.custom.ModuleItem.*;
import com.example.voidcraft.ModAttachments;
import com.example.voidcraft.ModDataComponents;
import com.example.voidcraft.Network.ModNetworking;
import com.example.voidcraft.Sound.ModSound;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

import static com.example.voidcraft.Item.custom.ModuleItem.ModuleMode.*;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierType.*;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleStatHelper.addLess;

public class VoidModule extends ModuleItem {
    private static final long ENERGY_COST = 25L;
    private static final long COOLDOWN_TICKS = 600L;
    private static final Integer PHASE_TICK = 50;
    private static final int BURST_COST_TICKS = 38;
    private static final float DEFAULT_MOVE_SPEED_OFFSET = 1F;
    private static final float VOID_FORM_MULTIPLIER = 1.5F;
    private static final int DEFAULT_FORM = 0;
    private static final int VOID_FORM = 1;
    public static final float MOVE_SPEED = 0.15F;


    public VoidModule(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltipAdder,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flag);
        tooltipAdder.accept(Component.translatable(
                "tooltip.void_craft.void_module.form",
                getFormName(stack)
        ));
        tooltipAdder.accept(Component.translatable(
                "tooltip.void_craft.void_module.switch_form",
                Component.keybind("key.voidcraft.switch_module_form")
        ));
    }

    @Override
    protected boolean canTurnForm() {
        return true;
    }

    @Override
    protected void doTurnForm(ItemStack stack) {
        stack.set(
                ModDataComponents.VOID_MODULE_FORM.value(),
                isVoidForm(stack) ? DEFAULT_FORM : VOID_FORM
        );
    }

    @Override
    protected void doUseSkill(ServerPlayer player, ItemStack watchStack, ItemStack moduleStack, int slot) {
        Stats stats = getStats(moduleStack);
        if(stats == null) return;

        player.setData(ModAttachments.VOID_SPEED.get(), stats.voidSpeed());

        ModuleMode mode = stats.mode();
        if(mode == null) return;
        if(mode == CHANNEL){
            useChannel(player, moduleStack, slot, stats);
        }
        if(mode == BURST){
            useBurst(player, moduleStack, slot, stats);
        }
    }

    private void useChannel(ServerPlayer player, ItemStack moduleStack, int slot, Stats stats) {
        long offEnergy = getChannelEnergyCost(moduleStack, stats);
        if(ModuleSkillClock.hasChannel(player,slot)){
            ModuleSkillClock.stopChannel(player,slot);
            stopForm(player);
        }
        else{
            if(!ModuleSkillClock.tryUseEnergy(player,offEnergy)){
                return;
            }
            startEffect(player);
            ModuleSkillClock.startChannel(player,slot,offEnergy);
            setFormTicks(player, moduleStack, 2);
        }
    }

    private void useBurst(ServerPlayer player, ItemStack moduleStack, int slot, Stats stats) {
        boolean cooldownReady = ModuleSkillClock.canUseNow(player,slot);
        if(!cooldownReady){
            if(hasForm(player, moduleStack)){
                stopForm(player);
                ModuleSkillClock.stopRunCooldown(player, slot);
                onBurstStop(player, moduleStack, slot);
                return;
            }
            if(!ModuleSkillClock.tryUseEnergy(player,getBurstEnergyCost(moduleStack, stats))) {
                return;
            }
        }
        startEffect(player);
        int activeTicks = stats.activeTicks();
        ModuleSkillClock.startRunCooldown(player, slot, activeTicks, getCooldownTicks(moduleStack, stats));
        onBurstStart(player, moduleStack, slot, stats, activeTicks);
        setFormTicks(player, moduleStack, activeTicks);
    }

    protected void onBurstStart(ServerPlayer player, ItemStack moduleStack, int slot, Stats stats, int activeTicks) {
    }

    protected void onBurstStop(ServerPlayer player, ItemStack moduleStack, int slot) {
    }

    protected long getChannelEnergyCost(ItemStack moduleStack, Stats stats) {
        return stats.channelEnergyCost(getCostMultiplier(moduleStack));
    }

    protected long getCooldownTicks(ItemStack moduleStack, Stats stats) {
        return stats.cooldownTicks(getCooldownMultiplier(moduleStack));
    }

    protected long getBurstEnergyCost(ItemStack moduleStack, Stats stats) {
        return stats.burstEnergyCost(getCostMultiplier(moduleStack));
    }

    protected float getCostMultiplier(ItemStack moduleStack) {
        return isVoidForm(moduleStack) ? VOID_FORM_MULTIPLIER : 1F;
    }

    protected float getCooldownMultiplier(ItemStack moduleStack) {
        return isVoidForm(moduleStack) ? VOID_FORM_MULTIPLIER : 1F;
    }

    private static void startEffect(ServerPlayer player) {
        Level level = player.level();
        ModNetworking.sendPhaseTear(player,VoidRingInstance.Preset.DEFAULT);
        ModSound.playEnterVoid(level, player);
    }

    private static boolean hasForm(ServerPlayer player, ItemStack moduleStack) {
        if(isVoidForm(moduleStack)){
            return VoidClock.getInVoid(player);
        }
        return VoidClock.getInPhase(player);
    }

    protected static void stopForm(ServerPlayer player) {
        if(VoidClock.getInVoid(player)){
            VoidClock.stopVoid(player);
            return;
        }
        if(VoidClock.getInPhase(player)){
            VoidClock.stopPhase(player);
        }
    }

    public static void setFormTicks(ServerPlayer player, ItemStack moduleStack, int ticks) {
        if(isVoidForm(moduleStack)){
            VoidClock.setVoidTicks(player, ticks);
            return;
        }
        VoidClock.setPhaseTicks(player, ticks);
    }

    public static boolean isVoidForm(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.VOID_MODULE_FORM.get(), DEFAULT_FORM) == VOID_FORM;
    }

    private static Component getFormName(ItemStack stack) {
        String id = isVoidForm(stack) ? "void" : "phase";
        return Component.translatable("void_module_form.void_craft." + id);
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

        public long channelEnergyCost(float multiplier) {
            if(multiplier == 1F){
                return channelEnergyCost();
            }
            return Math.max(1L, Math.round(ENERGY_COST * multiplier / cooldownDuration / energyEfficiency));
        }

        public long cooldownTicks() {
            return (long)(COOLDOWN_TICKS / cooldownDuration);
        }

        public long cooldownTicks(float multiplier) {
            if(multiplier == 1F){
                return cooldownTicks();
            }
            return Math.max(0L, Math.round(COOLDOWN_TICKS * multiplier / cooldownDuration));
        }

        public int activeTicks() {
            return (int)(PHASE_TICK * activeDuration);
        }

        public long burstEnergyCost(float multiplier) {
            if(multiplier == 1F){
                return channelEnergyCost() * BURST_COST_TICKS;
            }
            return Math.max(1L, Math.round(ENERGY_COST * BURST_COST_TICKS * multiplier / cooldownDuration / energyEfficiency));
        }

        public float voidSpeed() {
            return MOVE_SPEED / Math.max(0.001F, moveSpeedOffset);
        }
    }
}
