package com.example.voidcraft.ClientCustom.Event;

import com.example.voidcraft.ClientCustom.Key.ModKeyMappings;
import com.example.voidcraft.ClientCustom.ModuleInputMode;
import com.example.voidcraft.VoidCraft;
import com.example.voidcraft.network.UseWatchModulePayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import static com.example.voidcraft.ClientCustom.ModuleInputMode.getInputTypeFromSlot;

@EventBusSubscriber(modid = VoidCraft.MODID, value = Dist.CLIENT)
public final class ClientKeyEvents {
    private ClientKeyEvents() {}

    @SubscribeEvent
    public static void ON_CLIENT_TICK(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null) {
            HoldReleaseInputState.cancelAll();
            return;
        }

        handleSkillKey(mc, 0, ModKeyMappings.SKILL_KEY_1);
        handleSkillKey(mc, 1, ModKeyMappings.SKILL_KEY_2);
    }

    private static void handleSkillKey(Minecraft mc, int slot, KeyMapping key) {
        ModuleInputMode inputMode = getInputTypeFromSlot(mc, slot);

        if (inputMode == ModuleInputMode.HOLD_RELEASE) {
            handleHoldReleaseKey(mc, slot, key);
            return;
        }

        handleClickKey(slot, key);
    }

    private static void handleClickKey(int slot, KeyMapping key) {
        HoldReleaseInputState.cancel(slot);

        while (key.consumeClick()) {
            ClientPacketDistributor.sendToServer(new UseWatchModulePayload(slot));
        }
    }

    private static void handleHoldReleaseKey(Minecraft mc, int slot, KeyMapping key) {
        // HOLD_RELEASE 不用 consumeClick 触发技能，但要清掉点击缓存
        while (key.consumeClick()) {}

        HoldReleaseInputState.Phase phase = HoldReleaseInputState.update(slot, key.isDown());

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
    }
}
