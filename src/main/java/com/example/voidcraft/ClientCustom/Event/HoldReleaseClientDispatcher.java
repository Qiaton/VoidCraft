package com.example.voidcraft.ClientCustom.Event;

import com.example.voidcraft.Custom.Clock.ModuleSkillClock;
import com.example.voidcraft.Custom.ModuleSlotHelper;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.BlinkVoidModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

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

        if (moduleStack.getItem() instanceof BlinkVoidModule) {           // 这里决定：这个 HOLD_RELEASE 模块是不是 Blink
            handleBlink(mc, slot, phase, ticks);                          // 是 Blink，就把阶段交给 Blink 的客户端控制器

        }

        // 以后其他长按模块接在这里
        // if (moduleStack.getItem() instanceof ChargeShotModule) {
        //     handleChargeShot(mc, slot, phase, ticks);
        //     return;
        // }
    }
    private static void handleBlink(
            Minecraft mc,
            int slot,
            HoldReleaseInputState.Phase phase,
            int ticks
    ) {
        LocalPlayer localPlayer = mc.player;
        if (localPlayer != null && ModuleSkillClock.getCooldown(localPlayer, slot) != 0) {
            return;
        }
        switch (phase) {
            case PRESS -> BlinkAimClientController.onPress(mc, slot);     // 刚按下：打开客户端瞄准状态
            case HOLD -> BlinkAimClientController.onHold(mc, slot, ticks); // 按住中：更新预览圈位置
            case RELEASE -> BlinkAimClientController.onRelease(mc, slot, ticks); // 松手：发包通知服务端真正执行
            case NONE -> {
            }
        }
    }
}
