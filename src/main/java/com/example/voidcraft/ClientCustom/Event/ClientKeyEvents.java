package com.example.voidcraft.ClientCustom.Event;

import com.example.voidcraft.ClientCustom.Key.ModKeyMappings;
import com.example.voidcraft.ClientCustom.ModuleInputMode;
import com.example.voidcraft.ClientCustom.Void.VoidInOutEffectClient;
import com.example.voidcraft.Custom.ModuleSlotHelper;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleItem;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.TeleportVoidModule;
import com.example.voidcraft.VoidCraft;
import com.example.voidcraft.Network.CancelTeleportModulePayload;
import com.example.voidcraft.Network.UseWatchModulePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import static com.example.voidcraft.ClientCustom.ModuleInputMode.getInputTypeFromSlot;

@EventBusSubscriber(modid = VoidCraft.MODID, value = Dist.CLIENT)
public final class ClientKeyEvents {
    private ClientKeyEvents() {}

    // 记录取消键上一 tick 是否已经按下，防止按住 Q 时每 tick 重复触发取消逻辑。
    private static boolean wasCancelDown;
    private static final boolean[] teleportDeploying = new boolean[2];
    private static final int HOLD_RELEASE_HINT_INTERVAL_TICKS = 20;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null) {
            HoldReleaseInputState.cancelAll();
            cancelTeleportDeploying();
            wasCancelDown = false;
            return;
        }

        handleHoldReleaseCancel(mc);
        boolean slot0Usable = isSkillSlotUsable(mc, 0);
        boolean slot0Claimed = handleSkillKey(mc, 0, ModKeyMappings.SKILL_KEY_1, slot0Usable);
        if (slot0Usable
                && ModKeyMappings.SKILL_KEY_1.same(ModKeyMappings.SKILL_KEY_2)
                && (slot0Claimed || ModKeyMappings.SKILL_KEY_1.isDown())) {
            cancelBlockedSkillSlot(mc, 1);
            suppressConflictingKeyMappings(mc, ModKeyMappings.SKILL_KEY_1);
            return;
        }

        handleSkillKey(mc, 1, ModKeyMappings.SKILL_KEY_2, isSkillSlotUsable(mc, 1));
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        // 只在按下瞬间抢占冲突键；REPEAT/RELEASE 不需要重复清理点击缓存。
        if (event.getAction() != InputConstants.PRESS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        // 如果当前确实取消了蓄力，就把这次同键位输入吃掉，避免 Q 同时触发丢弃或其他绑定。
        if (ModKeyMappings.CANCEL_HOLD_RELEASE_KEY.matches(event.getKeyEvent()) && cancelHoldRelease(mc)) {
            suppressMatchingKeyMappings(mc, event);
            return;
        }

        int skillSlot = resolveSkillSlotForKeyEvent(mc, event);
        if (skillSlot >= 0) {
            suppressMatchingKeyMappings(mc, event, getSkillKey(skillSlot));
        }
    }

    private static void handleHoldReleaseCancel(Minecraft mc) {
        // tick 里再兜底检查一次 isDown，覆盖 KeyInput 时序没抢到的情况。
        boolean isCancelDown = ModKeyMappings.CANCEL_HOLD_RELEASE_KEY.isDown();
        if (!isCancelDown) {
            wasCancelDown = false;
            return;
        }

        if (wasCancelDown) {
            return;
        }

        wasCancelDown = true;
        if (cancelHoldRelease(mc)) {
            suppressCancelKeyMappings(mc);
        }
    }

    private static boolean cancelHoldRelease(Minecraft mc) {
        boolean canceled = false;

        // 只取消处于 HOLD_RELEASE 且正在蓄力的槽，普通点击技能不会被 Q 影响。
        for (int slot = 0; slot < 2; slot++) {
            if (getInputTypeFromSlot(mc, slot) != ModuleInputMode.HOLD_RELEASE) {
                continue;
            }

            if (!HoldReleaseInputState.isCharging(slot)) {
                continue;
            }

            HoldReleaseInputState.cancelUntilReleased(slot);
            HoldReleaseClientDispatcher.cancel(mc, slot);
            canceled = true;
        }

        if (cancelTeleportDeploy(mc)) {
            canceled = true;
        }

        return canceled;
    }

    private static void suppressMatchingKeyMappings(Minecraft mc, InputEvent.Key event) {
        suppressMatchingKeyMappings(mc, event, null);
    }

    private static void suppressMatchingKeyMappings(Minecraft mc, InputEvent.Key event, KeyMapping primaryKey) {
        // 清掉所有和这次按键事件匹配的 KeyMapping，覆盖同物理键的原版或其他 mod 功能。
        for (KeyMapping keyMapping : mc.options.keyMappings) {
            if (keyMapping == primaryKey || !keyMapping.matches(event.getKeyEvent())) {
                continue;
            }

            keyMapping.setDown(false);
            while (keyMapping.consumeClick()) {}
        }
    }

    private static void suppressCancelKeyMappings(Minecraft mc) {
        // tick 兜底没有 KeyEvent，只能按和取消键冲突/相同的绑定来清理。
        for (KeyMapping keyMapping : mc.options.keyMappings) {
            if (!keyMapping.same(ModKeyMappings.CANCEL_HOLD_RELEASE_KEY)) {
                continue;
            }

            keyMapping.setDown(false);
            while (keyMapping.consumeClick()) {}
        }
    }

    private static void suppressConflictingKeyMappings(Minecraft mc, KeyMapping primaryKey) {
        // 技能键真正被 VoidCraft 接管时，清掉同物理键的其他绑定，避免 F 同时交换副手等冲突。
        for (KeyMapping keyMapping : mc.options.keyMappings) {
            if (keyMapping == primaryKey || !keyMapping.same(primaryKey)) {
                continue;
            }

            keyMapping.setDown(false);
            while (keyMapping.consumeClick()) {}
        }
    }

    private static boolean handleSkillKey(Minecraft mc, int slot, KeyMapping key, boolean slotUsable) {
        if (!slotUsable) {
            HoldReleaseInputState.cancel(slot);
            teleportDeploying[slot] = false;
            return false;
        }

        ModuleInputMode inputMode = getInputTypeFromSlot(mc, slot);

        if (inputMode == ModuleInputMode.HOLD_RELEASE) {
            return handleHoldReleaseKey(mc, slot, key);
        }

        return handleClickKey(mc, slot, key);
    }

    private static boolean handleClickKey(Minecraft mc, int slot, KeyMapping key) {
        HoldReleaseInputState.cancel(slot);
        ItemStack moduleStack = ModuleSlotHelper.getModuleStackFromSlot(mc, slot);

        boolean clicked = false;
        while (key.consumeClick()) {
            ClientPacketDistributor.sendToServer(new UseWatchModulePayload(slot));
            if (moduleStack.getItem() instanceof TeleportVoidModule) {
                VoidInOutEffectClient.start();
                teleportDeploying[slot] = !teleportDeploying[slot];
            } else {
                teleportDeploying[slot] = false;
            }
            clicked = true;
        }
        if (clicked) {
            suppressConflictingKeyMappings(mc, key);
        }
        return clicked;
    }

    private static boolean cancelTeleportDeploy(Minecraft mc) {
        boolean canceled = false;
        for (int slot = 0; slot < 2; slot++) {
            if (!teleportDeploying[slot]) {
                continue;
            }

            ItemStack moduleStack = ModuleSlotHelper.getModuleStackFromSlot(mc, slot);
            if (!(moduleStack.getItem() instanceof TeleportVoidModule)) {
                teleportDeploying[slot] = false;
                continue;
            }

            ClientPacketDistributor.sendToServer(new CancelTeleportModulePayload(slot));
            teleportDeploying[slot] = false;
            canceled = true;
        }
        return canceled;
    }

    private static void cancelTeleportDeploying() {
        for (int slot = 0; slot < teleportDeploying.length; slot++) {
            teleportDeploying[slot] = false;
        }
    }

    private static boolean handleHoldReleaseKey(Minecraft mc, int slot, KeyMapping key) {
        // HOLD_RELEASE 不用 consumeClick 触发技能，但要清掉点击缓存
        while (key.consumeClick()) {}

        HoldReleaseInputState.Phase phase = HoldReleaseInputState.update(slot, key.isDown());
        boolean claimed = key.isDown() || phase != HoldReleaseInputState.Phase.NONE;
        if (claimed) {
            suppressConflictingKeyMappings(mc, key);
        }

        int ticks = switch (phase) {                                      // 统一算出本阶段要传给模块的蓄力 tick
            case HOLD -> HoldReleaseInputState.getChargeTicks(slot);       // 按住中：用正在累计的 tick
            case RELEASE -> HoldReleaseInputState.getLastReleasedTicks(slot); // 松手时：用刚刚保存的最终 tick
            case PRESS, NONE -> 0;                                         // 刚按下/无变化：还没有蓄力值
        };

        HoldReleaseClientDispatcher.handle(
                mc,
                slot,
                phase,                                                     // PRESS/HOLD/RELEASE 都交给分发器处理
                ticks
        );
        showHoldReleaseCancelHint(mc, phase, ticks);
        return claimed;
    }

    private static void showHoldReleaseCancelHint(Minecraft mc, HoldReleaseInputState.Phase phase, int ticks) {
        if (mc.player == null) {
            return;
        }

        // 长按开始时提示一次，长时间蓄力时每秒刷新一次，让玩家知道可以按取消键中断技能。
        if (phase != HoldReleaseInputState.Phase.PRESS
                && (phase != HoldReleaseInputState.Phase.HOLD || ticks % HOLD_RELEASE_HINT_INTERVAL_TICKS != 0)) {
            return;
        }

        mc.player.displayClientMessage(Component.translatable(
                "message.void_craft.hold_release_cancel_hint",
                ModKeyMappings.CANCEL_HOLD_RELEASE_KEY.getTranslatedKeyMessage()
        ), true);
    }

    private static int resolveSkillSlotForKeyEvent(Minecraft mc, InputEvent.Key event) {
        if (ModKeyMappings.SKILL_KEY_1.matches(event.getKeyEvent()) && isSkillSlotUsable(mc, 0)) {
            return 0;
        }

        if (ModKeyMappings.SKILL_KEY_2.matches(event.getKeyEvent()) && isSkillSlotUsable(mc, 1)) {
            return 1;
        }

        return -1;
    }

    private static KeyMapping getSkillKey(int slot) {
        return slot == 0 ? ModKeyMappings.SKILL_KEY_1 : ModKeyMappings.SKILL_KEY_2;
    }

    private static boolean isSkillSlotUsable(Minecraft mc, int slot) {
        ItemStack moduleStack = ModuleSlotHelper.getModuleStackFromSlot(mc, slot);
        return moduleStack.getItem() instanceof ModuleItem;
    }

    private static void cancelBlockedSkillSlot(Minecraft mc, int slot) {
        HoldReleaseInputState.cancel(slot);
        teleportDeploying[slot] = false;
        HoldReleaseClientDispatcher.cancel(mc, slot);
    }
}
