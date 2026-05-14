package com.example.voidcraft.ClientCustom.Event;

import com.example.voidcraft.Custom.ModuleSlotHelper;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.BlackHoleModule;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.BlinkVoidModule;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.SafeBlinkVoidModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public final class HoldReleaseClientDispatcher {
    private HoldReleaseClientDispatcher() {}

    public static void handle(
            Minecraft mc,
            int slot,
            HoldReleaseInputState.Phase phase,
            int ticks
    ) {
        if (phase == HoldReleaseInputState.Phase.NONE) {                  // 这一 tick 没有按下/按住/松手事件
            return;
        }

        if (mc.player == null) {                                          // 客户端还没进世界，不能读玩家背包
            return;
        }

        ItemStack moduleStack = ModuleSlotHelper.getModuleStackFromSlot(mc, slot); // 从副手手表里取出这个技能槽的模块

        if (moduleStack.getItem() instanceof BlackHoleModule) {
            handleBlackHole(mc, slot, phase);
            return;
        }
        if (moduleStack.getItem() instanceof SafeBlinkVoidModule) {
            handleSafeBlink(mc, slot, phase, ticks);
            return;
        }
        if (moduleStack.getItem() instanceof BlinkVoidModule) {           // 这里决定：这个 HOLD_RELEASE 模块是不是 Blink
            handleBlink(mc, slot, phase, ticks);                          // 是 Blink，就把阶段交给 Blink 的客户端控制器
            return;
        }
        // 以后其他长按模块接在这里
        // if (moduleStack.getItem() instanceof ChargeShotModule) {
        //     handleChargeShot(mc, slot, phase, ticks);
        //     return;
        // }
    }

    public static void cancel(Minecraft mc, int slot) {
        if (mc.player == null) {
            return;
        }

        // 根据当前槽位里的模块类型，把“取消蓄力”分发给对应模块的客户端控制器。
        ItemStack moduleStack = ModuleSlotHelper.getModuleStackFromSlot(mc, slot);
        if (moduleStack.getItem() instanceof BlackHoleModule) {
            BlackHoleAimClientController.onCancel(slot);
            return;
        }
        if (moduleStack.getItem() instanceof BlinkVoidModule || moduleStack.getItem() instanceof SafeBlinkVoidModule) {
            BlinkAimClientController.onCancel(slot);                      // Blink/SafeBlink 取消时只清本地瞄准/预览，不给服务端发释放包
        }
    }

    public static boolean handleScroll(Minecraft mc, double scrollY) {
        if (mc.player == null) {
            return false;
        }

        for (int slot = 0; slot < 2; slot++) {
            ItemStack moduleStack = ModuleSlotHelper.getModuleStackFromSlot(mc, slot);
            if (moduleStack.getItem() instanceof BlackHoleModule && BlackHoleAimClientController.isActive(slot)) {
                return BlackHoleAimClientController.onScroll(mc, slot, HoldReleaseInputState.getChargeTicks(slot), scrollY, BlackHoleModule.getStats(moduleStack));
            }
            if (moduleStack.getItem() instanceof SafeBlinkVoidModule && BlinkAimClientController.isActive(slot)) {
                return BlinkAimClientController.onScrollSafe(mc, slot, HoldReleaseInputState.getChargeTicks(slot), scrollY, SafeBlinkVoidModule.getSafeStats(moduleStack));
            }
            if (moduleStack.getItem() instanceof BlinkVoidModule && BlinkAimClientController.isActive(slot)) {
                return BlinkAimClientController.onScroll(mc, slot, HoldReleaseInputState.getChargeTicks(slot), scrollY, BlinkVoidModule.getStats(moduleStack));
            }
        }

        return false;
    }

    private static void handleBlackHole(
            Minecraft mc,
            int slot,
            HoldReleaseInputState.Phase phase
    ) {
        ItemStack moduleStack = ModuleSlotHelper.getModuleStackFromSlot(mc, slot);
        BlackHoleModule.Stats stats = BlackHoleModule.getStats(moduleStack);
        switch (phase) {
            case PRESS -> BlackHoleAimClientController.onPress(mc, slot, stats);
            case HOLD -> BlackHoleAimClientController.onHold(mc, slot, HoldReleaseInputState.getChargeTicks(slot), stats);
            case RELEASE -> BlackHoleAimClientController.onRelease(mc, slot, HoldReleaseInputState.getLastReleasedTicks(slot), stats);
            case NONE -> {
            }
        }
    }

    private static void handleBlink(
            Minecraft mc,
            int slot,
            HoldReleaseInputState.Phase phase,
            int ticks
    ) {
        ItemStack moduleStack = ModuleSlotHelper.getModuleStackFromSlot(mc, slot);
        BlinkVoidModule.Stats stats = BlinkVoidModule.getStats(moduleStack);
        switch (phase) {
            case PRESS -> BlinkAimClientController.onPress(mc, slot);     // 刚按下：打开客户端瞄准状态
            case HOLD -> BlinkAimClientController.onHold(mc, slot, ticks,stats); // 按住中：更新预览圈位置
            case RELEASE -> BlinkAimClientController.onRelease(mc, slot, ticks, stats); // 松手：重算目标点并发包通知服务端真正执行
            case NONE -> {
            }
        }
    }

    private static void handleSafeBlink(
            Minecraft mc,
            int slot,
            HoldReleaseInputState.Phase phase,
            int ticks
    ) {
        ItemStack moduleStack = ModuleSlotHelper.getModuleStackFromSlot(mc, slot);
        SafeBlinkVoidModule.Stats stats = SafeBlinkVoidModule.getSafeStats(moduleStack);
        switch (phase) {
            case PRESS -> BlinkAimClientController.onPress(mc, slot);
            case HOLD -> BlinkAimClientController.onHoldSafe(mc, slot, ticks, stats);
            case RELEASE -> BlinkAimClientController.onReleaseSafe(mc, slot, ticks, stats);
            case NONE -> {
            }
        }
    }
}
