# Phase world memory

更新时间：2026-05-29。

## 维度和规则

- 相位维度 key：`void_craft:phase_mirror`。
- `PhaseDimensions` 决定当前维度和目标维度。
- `PhaseWorldRules` 禁止相位维度刷怪、清理生成战利品容器、允许相位穿行。
- 到达位置保持原 x/z，y 限制在目标世界高度内。

## 世界切换

- `WorldModule` 调 `GoWorld.canGo` 和 `GoWorld.goWorld`。
- `GoWorld` 如果目标是相位维度，先按 chunk 分批发送源世界投影。
- 切换时发 `PhaseWorldTransitionPayload` 让客户端播放遮罩转场，并把玩家 UUID 放入 pending transitions。
- 40 tick 后有兜底完成传送；客户端 ready 后也会调用 `finishMove`。
- 进入相位维度前，中心 chunk 先发；外围 chunk 每 2 tick 补一圈。

## 玩家相位穿行

- `PhasePlayerStateHandler` 进入相位维度时保存玩家原飞行能力。
- 服务端和客户端都设置 `mayfly/flying/noPhysics`。
- 离开相位维度时恢复原能力。
- 生存和非旁观者离开时会强制去掉相位维度给的飞行。

## 相位投影

- `PhaseProjectionSnapshot` 横向半径 2 个 chunk，垂直半径上下 64 格，每次可只做一个 chunk 快照。
- 快照记录非空气、可渲染方块和流体。
- `PhaseProjectionClient` 只更新客户端缓存，不改真实世界方块。
- 在相位维度中把相位方块的 draw state 替换成投影方块或空气。
- 不在相位维度时缓存保留 45 秒，因为投影包会早于真正换维度到达。
- 新 chunk 到达后会把旁边一圈也标脏，帮助相邻面重建。

## 相位生成

- `PhaseShallowsChunkGenerator` 生成低起伏黑色相位浅滩，表面用 `BLACK_BLOCK`。
- `PhaseCrystalSynthesis` 在相位维度把 9 个高纯虚空结晶聚合，100 tick 后生成纯虚空结晶。
