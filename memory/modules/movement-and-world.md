# Movement and world modules memory

更新时间：2026-05-31。

## VoidModule

- 支持 CHANNEL 和 BURST。
- 基础能耗 25，基础冷却 600 tick，基础虚空时间 50 tick。
- 继承 `ModuleItem` 的通用形态切换：默认相位形态，切换后为虚空形态；形态存在 `VOID_MODULE_FORM`。
- 虚空形态进入 `IN_VOID`，耗能和冷却都是相位形态的 1.5 倍。
- 相位形态是“能踩方块且有重力的旁观者”：只在 `LivingEntity.travel` 的流体移动分支里跳过流体影响，忽略水下 travel 减速、浮力和流体推动，但不全局关闭 `isAffectedByFluids`；真实流体内会按输入和相位速度统一重算水平速度，不区分踩地和浮空。
- 真实流体内长按跳跃时，Y 速度至少补到 `0.3D` 来持续上浮。
- 相位仍忽略蜘蛛网/粉雪等卡住方块和方块速度倍率，但不穿方块、不飞行。
- 虚空形态是完整旁观者层：继承相位免疫，并额外开启飞行、`noPhysics` 和 `noGravity`，退出时恢复进入前能力和物理标记。
- 使用时写玩家 `VOID_SPEED` attachment。
- CHANNEL 再次使用会停止 channel 和虚空状态；开启时先扣一次持续耗能，播放相位环和进入虚空音效。
- BURST 冷却中再次使用，如果玩家仍在虚空中则提前退出；冷却未好时可以额外扣能量释放。
- BURST 使用 `ModuleSkillClock.startRunCooldown`，再按形态调用 `VoidClock.setPhaseTicks` 或 `VoidClock.setVoidTicks`。

## HealthVoidModule

- 直接继承 `VoidModule`，复用相位/虚空形态、移动速度、持续时间和冷却流程。
- 恢复型耗能和冷却在当前 `VoidModule` 形态倍率上再乘 1.5；恢复虚空形态总倍率是基础的 2.25。
- BURST 使用时立即治疗 `level * 0.08`。
- BURST 期间用 0 消耗 channel 保持状态，到时间后 `Clock` 停止 channel。
- `HealthVoidModuleClock` 按当前形态刷新相位或虚空状态，并每 tick 治疗。

## DashVoidModule

- 支持 CHANNEL 和 BURST。
- 基础能耗 40，基础冷却 300 tick，基础持续 25 tick。
- 基础冲刺速度常量 3。
- CHANNEL 是点击开关，不走长按释放。
- CHANNEL 再次使用会停止 channel、停止虚空、清 DashClock、清速度和 FOV 效果。
- BURST 再次使用时如果仍有 dash power，会提前停止。

## BlinkVoidModule

- 只支持 BURST，输入模式是 HOLD_RELEASE。
- 客户端长按预览，松手发 `ReleaseBlinkModulePayload`；服务端重新检查蓄力 tick、距离和目标坐标。
- 基础冷却 50 tick，基础能耗 200。
- `DISTANCE_PER_TICK = 1`，最大基础距离 5。
- 释放时发送相位环和一次性 trail，再传送玩家脚底坐标。
- 安全检查允许目标距离不超过服务端计算最大距离 + 3.5。

## SafeBlinkVoidModule

- 继承 Blink，但只支持安全落点。
- 基础最大距离 7。
- 目标点按方块坐标贴合到 `x + 0.5, y + 1.0, z + 0.5`。
- 同样走 HOLD_RELEASE 和服务端验算。

## TeleportVoidModule

- 只支持 BURST，输入模式 CLICK。
- 基础冷却 900 tick，基础 burst 耗能 600。
- 部署能量基础 100，每级 +100，每个 active_duration 等级 +50。
- 传送门基础存在 600 tick，每级 +400，每个 active_duration 等级 +300。
- 基础部署速度 0.15，每个 speed 等级 +0.15。
- 基础每秒移动 30 格，每个 speed 等级 +10。
- 再次使用时如果已有部署，会结束部署。
- 真正部署由 `TeleportVoidModuleClock` 管理。

## WorldModule

- 只支持 BURST。
- 基础能耗 900，基础冷却 600 tick。
- 调 `GoWorld.canGo(player)` 后再执行世界切换，避免明显不能传送时先消耗能量。
- `GoWorld.goWorld(player)` 成功后设置冷却。
- `ACTIVE_DURATION` 和 `SPEED_BOOST` 在该模块中显示为能量降低类 modifier。
