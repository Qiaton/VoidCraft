# Black hole memory

更新时间：2026-05-29。

## 模块

- `BlackHoleModule` 只支持 BURST，输入模式 HOLD_RELEASE。
- 基础投放距离 12，每级 +8。
- 基础半径 1，每级 +1。
- 基础引力 0.1，每级 +0.2。
- 基础持续 50 tick，每级 +10 tick。
- 基础冷却 2400 tick，基础耗能 2400。
- `SPEED_BOOST` 增强引力，`ACTIVE_DURATION` 增加持续时间，`COOLDOWN_REDUCTION` 同时影响冷却和耗能。
- `TearBlackHoleModule` 改颜色，黑洞核心伤害为 `1.0 + level * 0.6`，不伤害玩家，不拉玩家。
- `AnnihilationBlackHoleModule` 改颜色，黑洞核心伤害为 `1.0 + level * 0.6`，可以伤害玩家，也可以拉玩家。

## 释放链路

- 默认点击入口会把目标放在玩家视线最大距离处。
- 长按释放由客户端发目标点，服务端 `releaseBlackHole` 重新检查手表、服务端玩家、目标坐标、距离、冷却和耗能。
- 目标距离不能超过 `maxDistance + 3.5`。
- 释放时先发到达相位环，再调用 `BlackHoleEventManager.addBurst`。

## BlackHoleEventManager

- 运行时事件存在 `BlackHoleEventManager.EVENTS`。
- `addBurst` 创建 `BlackHoleEventInstance`，立刻发黑洞视觉 payload。
- 黑洞开始时播放 release 音效，并发循环 pull 音效 start 包。
- 每 20 tick 重新发一次视觉状态，避免客户端视觉丢失。
- duration 到 0 后停止循环声音并移除事件。

## BlackHoleEvents

- 每个服务端 tick 遍历 active 黑洞。
- 搜索范围是 `pullRadius * pullStrength * 20`。
- 到核心半径内会尝试造成核心伤害。
- 拉力按距离分段：极近处直接把速度设为朝中心 `0.2`，之后按 0.8、1、0.6、0.2 系数衰减。
- owner 不会被拉或伤害。
- 玩家是否被拉/伤害由事件配置的 `pullPlayers` 和 `hurtPlayers` 控制。
