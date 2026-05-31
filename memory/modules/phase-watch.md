# Phase watch memory

更新时间：2026-05-29。

## 容器和能量

- `PhaseWatch` 有 2 个模块槽和 1 个能量核心槽。
- 槽位 0/1 是模块槽，槽位 2 是能量核心槽。
- 容器存在 `DataComponents.CONTAINER`。
- 手表能量存在 `WATCH_ENERGY`。
- 能量默认满，最低允许到 `-10000`。
- 档位容量：crude 200，attuned 600，stabilized / phase_watch 1000，resonant 2000，void_energy 5000。

## 能量核心

- `EnergyCoreItem` 给手表回充，回充会磨损当前寿命和最大寿命。
- 损坏后变成 `ENERGY_CORE_RESIDUE`。
- 基础寿命 200。
- BASIC / PLUS / PRO / MAX 每 tick 回能 1/2/3/4，寿命倍率 1.0 / 1.5 / 2.0 / 3.0。
- 每恢复 2000 能量扣当前寿命；每恢复 10000 能量扣最大寿命。
- 最大寿命最低按剩余核心生命映射到约 80% 起步。

## 打开逻辑

- `PhaseWatch.openMenu` 打开 `ModuleMenu`，容器是 `WatchModuleContainer(watchStack)`。
- 默认 V 键打开手持相位手表：客户端发 `OpenPhaseWatchPayload(HAND_SLOT)`；服务端先检查主手，主手不是手表时检查副手。
- 在容器界面悬停相位手表按 V：客户端使用公开 `AbstractContainerScreen#getSlotUnderMouse()` 获取槽位；服务端按 `player.containerMenu.slots` 重新取槽内物品。
- `PhaseWatch.shouldCauseReequipAnimation` 只在换槽或换物品时触发重装备动画；单纯能量或核心组件变化不触发。

## Tooltip

- 相位手表 tooltip 显示档位、能量、打开键和已装模块模式。
- 打开键用 `Component.keybind("key.voidcraft.open_phase_watch")`，跟随玩家改键。
- 模块形态切换提示也用 `Component.keybind("key.voidcraft.switch_module_form")`。
