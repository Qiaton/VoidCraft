# Module system memory

更新时间：2026-05-29。

## 核心数据

- `ModuleItem` 是模块基类。
- `ModuleData` 保存 `moduleMode`、`level`、`modifiers`。
- `ModuleMode` 只有 `CHANNEL` 和 `BURST`。
- `ModuleModifierType` 只有 `cooldown_reduction`、`speed_boost`、`active_duration`。
- `ModuleModifierItem.setData` 写 `MODULE_MODIFIER_DATA`，并把类型 id 写入 `CustomModelData.strings()`。
- `ModuleModifierItem.inventoryTick` 会调用 `syncData`，补齐模型字符串。

## 模块使用链路

- 客户端按键或 GUI 发 `UseWatchModulePayload(slot)`。
- `ModNetworking.onUseWatchServer` 只信服务端副手手表，重新读取 `DataComponents.CONTAINER` 中的模块槽。
- `PhaseWatch.useModule` 取 0/1 模块槽，调用模块 `useSkill`。
- `ModuleItem.useSkill` 校验手表和容器后进入 `doUseSkill`。

## ModuleSkillClock

- `ModuleSkillClock` 管每槽冷却、channel 每 tick 消耗、有持续时间的 burst、手表从能量核心回充、能量 HUD 刷新和玩家登出清理。
- channel 每 tick 先扣所有槽的持续耗能；不够时停止 channel。
- 手动炮台的持续开火挂在 channel tick 后调用 `PhaseTurretModule.tickFire(player)`。
- 没有耗能 channel 时，副手手表会从能量核心自动回充。
- 能量 HUD 每 2 tick 发给客户端；副手没有手表时发隐藏状态。

## 按键

- `ModKeyMappings` 注册在自定义分类 `void_craft:void_craft`。
- 默认按键：F 模块槽 0，G 模块槽 1，Q 取消 HOLD_RELEASE 蓄力或传送门部署，V 打开相位手表，B 切换可切换模块形态。
- `VoidCraftClient` 构造函数显式把 `ModKeyMappings::registerKeys` 挂到 mod event bus。
- `ClientKeyEvents` 会清理同物理键的其他 KeyMapping 点击缓存，避免原版或其他绑定一起触发。

## 强化台和配方转换

- `ModuleBoostMenu`：1 个模块槽、3 个 modifier 槽、1 个结果槽；同类型 modifier 不能重复；结果会替换已有同类型 modifier 或追加新类型。
- `ModuleTurnRecipe` 是多种模块转化配方的统一 custom recipe。
- 辅助炮台切换接受普通手动、普通辅助、恢复手动、恢复辅助四种炮台模块互转。
- 恢复炮台配方也接受这四种炮台作为基础。
- 输出继续复制输入模块的组件数据。
