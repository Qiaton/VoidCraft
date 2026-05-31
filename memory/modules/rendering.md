# Rendering memory

更新时间：2026-05-29。

## 主链路

- 客户端渲染教学和排查路线：`VoidPhaseClient -> VoidEffect -> VoidPhasePostProcessor -> void_phase.json -> void_phase.fsh`。
- `VoidEffect.renderVoidEffects` 挂在 `RenderLevelStageEvent.AfterParticles`。
- `VoidCraftClient.renderTurretBlockFlash` 挂在 `AfterTranslucentBlocks`，渲染炮台破坏形态命中方块的白闪。

## VoidEffect

- `tickClientEffects` 推进 `VoidRingManager`、`VoidTrailManager`、`VoidBeamManager`、`VoidBlackHoleManager`、`PhaseWorldTransitionOverlay`、`PhaseWorldTransitionClient` 和 `VoidInOutEffectClient`。
- 普通路径使用自定义 RenderType：`VOID_WORLD_EFFECT` / `VOID_MASK_EFFECT`，RenderPipeline 是 `DEBUG_QUADS`，输出到 `ITEM_ENTITY_TARGET`。
- Iris shaderpack 兼容通过反射调用 Iris API；shaderpack 开启时使用 `RenderTypes.eyes`、`RenderTypes.energySwirl`、`entityTranslucent` 等兼容路径。
- 无玩家、无世界或无可见效果时，调用 `VoidPhasePostProcessor.resetFrame()`。

## VoidPhaseClient

- 每客户端 tick 判断是否需要挂 `void_phase` 后处理。
- 触发条件包括玩家在虚空状态、使用 `SpatialSword`、位于相位维度、附近有相位环、有 active black hole、相位世界转场中、虚空进出效果中。
- 进入虚空时播放 `InVoidLoopSoundInstance` 循环音；离开时淡出。
- 维度切换会清后处理，所以每 tick 按真实 `gameRenderer.currentPostEffect()` 修正。

## VoidPhasePostProcessor

- `MAX_EFFECTS = 16`。
- 数据纹理宽 12，高 `MAX_EFFECTS + 1`，即 12x17。
- 第 0 行是全屏相位和转场头信息；后 16 行是一帧内 ring / black hole 参数。
- 数据纹理 id：`textures/effect/phase_tear_data.png`。
- mask 纹理 id：`textures/effect/phase_tear_mask.png`。
- 普通路径把本地 mask 坐标和 effect id 写进 mask target。
- shader 先尝试读取 raster mask，读不到再按屏幕中心、半宽高和轴向参数算 analytic mask。
- `writePackedU16` 把两个 0-1 浮点打包进 RGBA 字节；shader 用 `decodePair` 还原。

## void_phase.fsh

- 先从数据纹理读全屏相位、转场进度和最多 16 个世界扭曲效果。
- ring 走膜面扭曲。
- black hole 用数据行 shape 标记区分，额外做暗心、薄边、swirl 和 suction。
- 有 occlusion 时，用主场景 depth 和 effectDepth 做可见性过滤。
- 最终用 `texCoord + totalOffset` 采样场景，再混入全屏滤镜、黑洞暗心、边缘高光和膜面提亮。

## 其他客户端视觉

- `EnergyHud` 注册在 hotbar 上层，按服务端百分比选择 0-100 帧贴图，超过 1500ms 未刷新就隐藏。
- `PhaseWorldTransitionOverlay` 用 512x512 动态 mask 纹理画进入白圈、退出窗口和全白 hold。
- `PhaseWorldTransitionClient` 的基础转场 250ms；shader 兼容模式会等全白可见帧后再发 ready。
- `VoidPhenomenonCollectorBlackHoleClient` 每 2 tick 扫 96 格内 active collector，给运行中发电机维护 distortion-only persistent black hole；丢失 4 次扫描后移除。
- `PhaseTurretBlockFlashClient` 给被破坏形态命中的方块绘制 5 tick 白色 debug filled box。
