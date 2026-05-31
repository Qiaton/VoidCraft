# GUI and network memory

更新时间：2026-05-29。

## 菜单和 Screen

- `ModMenuType` 注册 `ModuleMenu`、`ModuleBoostMenu`、`VoidPhenomenonCollectorMenu`、`VoidChargerMenu`、`VoidEnergyConverterMenu`、`GuideBookMenu`。
- `VoidCraftClient.registerModuleMenu` 绑定 `ModuleScreen`、`ModuleBoostScreen`、`VoidPhenomenonCollectorScreen`、`VoidChargerScreen`、`VoidEnergyConverterScreen`、`GuideBookScreen`。
- 机器 GUI 统一走 `GuiDraw` / `GuiStyle`：暗色背景、面板、分割线、背包区域、槽位和标签页；长文本用 `GuiDraw.clip` 截断。

## 常见 GUI 路线

- 相位手表：`PhaseWatch -> ModuleMenu -> ModuleScreen -> WatchModuleContainer`。
- 模块强化台：`ModuleBoostBlock -> ModuleBoostMenu -> ModuleBoostScreen`。
- 虚空现象收集器：`VoidPhenomenonCollectorBlock -> VoidPhenomenonCollectorMenu -> VoidPhenomenonCollectorScreen`。
- 充能器：`VoidChargerBlock -> VoidChargerMenu -> VoidChargerScreen`。
- 能量转换器：`VoidEnergyConverterBlock -> VoidEnergyConverterMenu -> VoidEnergyConverterScreen`。
- 指南书：`GuideBookItem -> GuideBookMenu -> GuideBookScreen`。

## 网络

- 网络入口：`ModNetworking`。
- 当前 `NETWORK_VERSION = "37"`。
- 修改 payload 类型、字段、注册顺序或语义时，检查是否需要递增网络版本。

## 服务端到客户端

- `PhaseTearPayload`、`VoidTrailPayload`、`VoidBlackHolePayload`、`ContinuousLoopSoundPayload`、`EnergyHudPayload`、`PhaseWorldTransitionPayload`、`PhaseProjectionPayload`、`TurretStatePayload`、`TurretShotFxPayload`、`PhaseTurretBlockFlashPayload`、`CoordinateBindingsPayload`、`ChunkMapperStatusPayload`。

## 客户端到服务端

- `UseWatchModulePayload`、`OpenPhaseWatchPayload`、`ReleaseBlinkModulePayload`、`UseTurretShotPayload`、`PhaseWorldTransitionReadyPayload`、`RemoveCoordinateBindingPayload`、`RequestCoordinateBindingsPayload`、`SetChunkMapperTierPayload`、`ReleaseBlackHoleModulePayload`、`CancelTeleportModulePayload`、`SwitchModuleFormPayload`。

## 安全习惯

- C2S payload 只当请求，不直接信客户端状态。
- 手表模块使用、打开手表、切换模块形态都在服务端重新读取玩家当前副手或当前菜单槽。
- 坐标绑定删除和刷新会按距离、维度和方块实体重新校验。
- Blink、黑洞等目标点由客户端预览，但服务端重新检查距离和坐标有限性。
