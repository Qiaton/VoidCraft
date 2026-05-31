# Turret memory

更新时间：2026-05-31。

## 主要文件

- 手动炮台：`PhaseTurretModule`
- 恢复手动炮台：`HealthPhaseTurretModule`
- 辅助炮台：`AssistPhaseTurretModule`
- 恢复辅助炮台：`HealthAssistPhaseTurretModule`
- 恢复逻辑 helper：`HealthTurretHelper`
- 客户端炮台球：`PhaseEmitterClientManager`、`PhaseEmitterSet`、`PhaseEmitterOrbRenderer`
- 方块命中闪白：`PhaseTurretBlockFlashPayload`、`PhaseTurretBlockFlashClient`

## 手动炮台

- 基础射程 512，基础伤害 3.5，每级 +0.5，开火间隔 5 tick。
- CHANNEL 每 tick 耗能 10。
- BURST 基础冷却 45 秒，持续 5 秒，耗能 800。
- 左键单发；右键齐射。
- 炮台球数量来自 `PhaseEmitterSlot.normalizeCount`，默认 1，也受模块等级和配置影响。
- 服务端按玩家 UUID 和模块槽保存 `FireState`；客户端只处理视觉和输入请求。
- 同一时间只保留一个手动炮台类 channel，避免多个模块争用同一套炮台球视觉。

## 形态切换

- `PHASE_TURRET_FORM` 是 `Integer` 数据组件。
- `PhaseTurretModule` 的基础类可以切换形态；恢复炮台和辅助炮台不参与。
- 默认形态值 0，破坏形态值 1。
- 悬停可切换模块按 B 发 `SwitchModuleFormPayload(slot)`。
- 服务端只信当前菜单槽位，校验 `PhaseTurretModule.canTurnForm(stack)` 后调用 `turnForm`。
- 破坏形态实体伤害变为普通炮台 0.2 倍。
- 破坏形态命中方块时按模块等级、硬度和工具需求等级累积破坏进度；不满足需求等级时进度乘 0.05，破坏成功不掉落。
- `BREAK_RECORDS` 保存方块进度，5 秒未命中会清掉。
- 每次有效命中发 `PhaseTurretBlockFlashPayload`，客户端 5 tick 白色盒体闪烁。
- 破坏成功播放 `BLOCK_BREAK_LIGHT` 相位环和 `void_archer_hit` 音效。

## 辅助炮台

- 基础射程 32，安全距离 4，基础伤害 1.5，每级 +0.5，开火间隔 5 tick。
- CHANNEL 每 tick 耗能 10。

## 恢复炮台

- 手动恢复炮台和辅助恢复炮台共用 `HealthTurretHelper` 计算治疗量、治疗 beam 颜色和回血检查。
- BURST 基础冷却 45 秒，持续 5 秒，耗能 800。
- `AssistPhaseTurretModule.tickAssistTurret` 由 `PlayerTickEvent.Post` 驱动。
- 自动目标优先级包含安全距离内威胁、当前锁定目标、玩家最近攻击目标、正在攻击玩家的目标和怪物目标。
- 最近主动攻击目标记录在 `RECENT_ATTACK_TARGETS`，保留 100 tick。
- 辅助炮台启动时发 `sendAssistTurretState`，只显示炮台球，不接管本地左右键。

## 恢复炮台

- `HealthPhaseTurretModule`：伤害倍率 0.65；左键命中敌人时给玩家自疗 `等级 * 0.07`；右键治疗友方，治疗量为伤害 * `(0.20 + 等级 * 0.15)`。
- `HealthAssistPhaseTurretModule`：伤害倍率 0.65；自疗 `等级 * 0.10`；友方治疗 `0.20 + 等级 * 0.15`；CHANNEL 额外耗能 +2，BURST 额外耗能 +80。
- 恢复辅助炮台在玩家血量低于最大血量 20% 时，优先锁定玩家治疗 10 tick；仍濒死会续 10 tick。
- 治疗目标包括玩家自己、同盟和玩家驯服动物。

## 客户端视觉

- `PhaseEmitterClientManager` 区分 ACTIVE 和 BLOCKING；ACTIVE 只显示炮台球，BLOCKING 接管本地左右键并隐藏手。
- `PhaseEmitterSet` 第一人称用相机 forward/right/up 计算环绕位置，第三人称用玩家朝向后方椭圆轨道计算位置。
- `PhaseEmitterOrbRenderer` 画面向相机的径向圆盘；shader 兼容路径补反向面，避免背面剔除。
- 炮台射击 S2C 只发命中点和 beam 配置；客户端本地生成 beam、炮口 FLASH ring 和命中 FLASH ring。
