# VoidCraft agent memory

更新时间：2026-05-10。

## 协作约定

- 在用户下达开工指令前，不修改源码。
- 修改代码时，方法名尽量用直白、低级词汇表达意思。
- 新代码风格跟随附近旧代码，优先沿用当前包结构、事件写法和注释风格。

## 构建与运行

- Gradle/NeoForge 项目使用 Java 21，ModDevGradle `2.0.141`。
- `gradle.properties`：Minecraft `1.21.11`，NeoForge `21.11.42`，Parchment `2025.12.20`，mod id `void_craft`，mod version `1.0.1`。
- Codex app 的 `.codex/environments/environment.toml` 里“运行”动作是 `./gradlew clean runClient`。
- 当前工作区有未跟踪的 `agent.md` 作为项目记忆文件；不要把它误当作源码改动。

## 项目概览

- 这是一个 NeoForge 模组，主包是 `com.example.voidcraft`，模组 id 是 `void_craft`。
- 主入口 `VoidCraft` 注册 CreativeTab、方块、方块物品、方块实体、能力、物品、附件、音效、网络包、数据组件、菜单、世界生成、区块票、掉落和战利品注入。
- 客户端入口 `VoidCraftClient` 注册菜单 Screen、能量 HUD、相位世界过渡 Overlay，并 tick 炮台客户端、坐标绑定预览、发电机黑洞、相位投影等客户端状态。
- 代码大约 134 个 Java 文件，2.67 万行。大的模块集中在特效渲染、炮台模块、虚空射手、网络分发和方块实体。

## 核心系统

- 虚空能网络在 `Custom/Behavior/Energy`。
  - `BoundVoidPosition` 保存维度和方块坐标。
  - `VoidEnergyBinding` 保存另一端位置和绑定类型。
  - `VoidEnergyTransferBlockEntity` 是虚空能端点接口。
  - `VoidEnergyTransfer` 负责解析端点、两端绑定、删除绑定、周期推送输出。
  - 传输模型是输出端主动推给输出列表，输入端不主动拉取。
  - 分配逻辑按传输轮次轮换起点，先收集目标需求，再把可输出能量按需求公平拆分。
  - 失效目标会从输出列表移除；目标不能输入时也会同步移除对方输入记录。
- 方块实体：
  - `BatteryBlockEntity`：双向缓存，容量 40000，默认满电，按 0-9 档同步模型。
  - `VoidPhenomenonCollectorBlockEntity`：结晶发电，只输出虚空能，多档方块决定产能、缓存和结晶槽数。
  - `VoidChargerBlockEntity`：只输入虚空能，用能量修复 `EnergyCoreItem`。
  - `ChunkMapperBlockEntity`：只输入虚空能，按档位消耗能量维持区块强加载。
  - `VoidEnergyConverterBlockEntity`：双向虚空能缓存，并用 NeoForge FE capability 进行 FE/虚空能转换。
  - `VoidEnergyConverterBlockEntity`：虚空能缓存 10000，FE 每 tick 输入/输出上限各 1000；FE 转虚空能倍率 0.3，虚空能转 FE 倍率 2.0；六面模式用潜行右键在 none/input/output 间轮换。
  - 2026-05-21 已增强虚空能转换方块的 FE 兼容：它不再只被动暴露 `EnergyHandler`，每服务端 tick 会主动处理相邻 FE 方块；`INPUT` 面从相邻对应面抽 FE 转虚空能，`OUTPUT` 面把虚空能转 FE 推给相邻对应面。
  - 转换方块切换面模式时现在用 `Block.UPDATE_ALL`、`level.invalidateCapabilities(pos)` 和 `updateNeighborsAt(...)`，帮助缓存 capability 的管道/机器重新扫描。
  - `VoidPhenomenonCollectorBlock` 档位：I 产 1/t、缓存 50000、1 槽；II 产 2/t、缓存 70000、3 槽；III 产 3/t、缓存 150000、6 槽；IV/void_attuner 产 5/t、缓存 500000、9 槽。
  - `VoidChargerBlock` 档位：低级 1 槽、缓存 5000、每 tick 修 1；中级 3 槽、缓存 50000、每 tick 修 1；高级 9 槽、缓存 200000、每 tick 修 10；每点修复消耗 20 虚空能。
  - `ChunkMapperBlockEntity` 档位半径 `{0,1,2,3}`，每 tick 耗能 `{1,16,32,128}`，缓存 10000，只允许 1 个输入绑定。
- 坐标制定器 `CoordinateDesignatorItem`：
  - OUTPUT：先点输出端，再点输入端。
  - INPUT：先点输入端，再点输出端。
  - UNBIND：打开连接列表面板。
  - 区块映射器单输入覆盖需要二次确认。
  - 潜行右键切换模式，同时清空第一次记录；模式还会同步到 `CustomModelData` 字符串用于物品模型切换。
  - 区块映射器输入覆盖确认窗口是 100 tick，同一玩家、同一模式、同一来源/目标才算确认。

## 手表与模块

- `PhaseWatch` 有 2 个模块槽和 1 个能量核心槽，容器存进 `DataComponents.CONTAINER`。
- 手表能量存 `WATCH_ENERGY`，默认满，允许短暂降到 `-10000`。
- 手表档位容量：crude 200，attuned 600，stabilized/phase_watch 1000，resonant 2000，void_energy 5000。
- `EnergyCoreItem` 给手表充能，会磨损当前寿命和最大寿命；损坏后变 `ENERGY_CORE_RESIDUE`。
- 能量核心基础寿命 200；BASIC/PLUS/PRO/MAX 每 tick 回能 1/2/3/4，寿命倍率 1.0/1.5/2.0/3.0；每恢复 2000 能量扣当前寿命，每恢复 10000 能量扣最大寿命，最大寿命最低按剩余核心生命映射到约 80% 起步。
- `ModuleSkillClock` 统一处理模块冷却、channel 持续扣能、手表从核心回充、能量 HUD 同步和登出清理。
- 客户端按键在 `ClientCustom/Event`：
  - F/G 分别触发 0/1 模块槽。
  - 点击模块直接发 `UseWatchModulePayload`。
  - Blink 类 HOLD_RELEASE 模块客户端预览目标，松手发 `ReleaseBlinkModulePayload`，服务端重新验算。
  - Q 取消长按释放。
  - 按键处理会清理同物理键的其他 KeyMapping 点击缓存，避免 F/G/Q 同时触发原版或其他绑定。
- 模块：
  - `VoidModule`：进入虚空状态，支持 CHANNEL/BURST。
  - `HealthVoidModule`：虚空状态加治疗，支持 CHANNEL/BURST。
  - `DashVoidModule`：虚空冲刺，CHANNEL 是开关型点按。
  - `BlinkVoidModule`：长按闪现到视线点。
  - `SafeBlinkVoidModule`：长按安全闪现，目标贴合方块碰撞点。
  - `PhaseTurretModule`：手动相位炮台，接管输入，左键射击、右键齐射。
  - `AssistPhaseTurretModule`：自动辅助炮台，按威胁/攻击者/锁定/最近攻击/怪物优先级选目标。
  - `WorldModule`：切换主世界和相位镜像维度。
- 模块强化台 `ModuleBoostMenu`：1 个模块槽 + 3 个 modifier 槽 + 1 个结果槽；同类型 modifier 不能重复，结果会替换已有同类型或追加新类型。
- modifier 类型只有 `cooldown_reduction`、`speed_boost`、`active_duration`；modifier 物品会把类型 id 同步到 `CustomModelData` 字符串。
- 虚空结晶 `VoidCrystalItem` 基础耐久 1500，发电进度阈值 500；low/high/pure 的耐久倍率为 1/10/45，产能倍率为 1/8/32。

## 世界与相位

- 相位维度 key：`void_craft:phase_mirror`。
- `GoWorld` 负责世界切换：发过渡包、发送源世界投影、等待客户端 ready 包，超时后兜底传送。
- `GoWorld` 过渡兜底延迟 40 tick；进入相位维度前按 chunk 分批发送投影，外围 chunk 每 2 tick 补一圈。
- `PhaseWorldRules` 控制相位世界规则：禁怪、清生成战利品容器、允许相位穿行。
- `PhasePlayerStateHandler` 在相位世界给玩家 mayfly/flying/noPhysics，并离开后恢复原能力。
- `PhaseShallowsChunkGenerator` 生成低起伏黑色相位浅滩，表面用 `BLACK_BLOCK`。
- `PhaseProjectionSnapshot` 从源世界按玩家附近 chunk 和上下 64 格采样方块；`PhaseProjectionClient` 在相位维度编译区块时用投影方块替换相位方块。
- 相位投影横向半径是 2 个 chunk，垂直半径是上下 64 格；客户端离开相位维度后投影缓存保留 45 秒。
- `PhaseCrystalSynthesis`：在相位维度把 9 个高纯虚空结晶聚合，100 tick 后生成纯虚空结晶。

## 战斗与虚空状态

- `IN_VOID` 和 `VOID_SPEED` 是 NeoForge attachments，同步到客户端。
- `VoidClock` 用 tick 表驱动实体虚空状态和客户端虚空闪光。
- `VoidEvents` 让虚空实体免伤、免交互、免拾取、怪物不锁定、保氧，并处理跳跃速度。
- Mixin：
  - `VoidEntity` / `VoidLivingEntityClass` / `VoidPlayer` 处理碰撞、推挤、投射物命中、流体推力、拾取选取和速度。
  - `PhasePlayerTraversalMixin` 防止原版 tick 重置相位穿墙的 noPhysics。
  - `PhaseProjectionSectionCompilerMixin` 让相位投影参与客户端区块编译。
  - `VoidArcherArrowDamageMixin` 修正虚空射手高速箭的原版命中伤害和死亡消息。
- `VoidArcher` 附魔会隐藏箭、锁定初始视线方向、加速箭、发拖尾，命中时发黑洞效果并分批处理 AOE。

## 特效与客户端渲染

- `VoidRingManager` / `VoidTrailManager` / `VoidBeamManager` / `VoidBlackHoleManager` 管理客户端实例生命周期。
- `VoidEffect` 在客户端 tick 推进特效，并在 `RenderLevelStageEvent.AfterParticles` 渲染 trail、beam、ring、black hole 和炮台球。
- 有 Iris shaderpack 兼容分支，普通模式使用自定义 RenderType 和后处理，光影兼容模式用 eyes/energySwirl 类管线。
- `VoidPhaseClient` 根据虚空状态、相位维度、附近 ring/black hole、过渡状态决定是否挂 `void_phase` 后处理和循环音效。
- `VoidPhasePostProcessor` 用数据纹理和 mask texture 把世界相位效果参数喂给 post effect shader。
- 渲染主入口在 `ClientCustom/Void/VoidEffect`：
  - `tickClientEffects` 推进 ring/trail/beam/black hole manager，并 tick 相位世界过渡。
  - `renderVoidEffects` 是 `AfterParticles` 事件入口；无玩家、无世界、无可见效果时会 `VoidPhasePostProcessor.resetFrame()`。
  - 正常路径用 `VOID_WORLD_EFFECT` / `VOID_MASK_EFFECT`，RenderPipeline 是 `DEBUG_QUADS`，输出到 `ITEM_ENTITY_TARGET`。
  - 光影兼容路径用 `RenderTypes.eyes` 和 `RenderTypes.energySwirl`；trail/beam 有主 pass 和 glow pass，炮台球也走兼容 pass。
  - `renderRingPass` / `renderBlackHolePass` 都先用预计算的中心、partialTick 和 facingData，避免同帧重复算镜头朝向。
  - `writeWorldEffects` 最多写入 `VoidPhasePostProcessor.MAX_EFFECTS` 个 ring/black hole 后处理效果；普通路径写 raster mask，光影路径写屏幕中心和半宽高给 shader 走 analytic mask。
- `VoidPhasePostProcessor` 的数据纹理是 10x17：第 0 行是全屏相位和转场头信息，后 16 行是一帧内的 ring/black hole 参数。
  - 第 0 行写入：全屏相位强度、time、enterProgress、exitProgress、holdWhite、stageCode。
  - ring/black hole 行写入：progress、distortionAmplitude、distortionThickness、distortionAlpha、noiseFrequency、noiseScrollSpeed、屏幕中心/半宽高、swirl/suction、遮挡开关/深度、shape 类型、黑洞中心暗面强度、核心 mask 缩放和 flatGate。
  - 普通路径会把本地 mask 坐标和 effect id 写进 `phase_tear_mask`；shader 先尝试读取 raster mask，读不到再按屏幕中心/半宽高算 analytic mask。
  - `writePackedU16` 把两个 0-1 浮点拆进 RGBA 字节，shader 里用 `decodePair` 还原。
- `void_phase.fsh` 的渲染逻辑：
  - 先从数据纹理读全屏相位、转场进度和最多 16 个世界扭曲效果。
  - ring 走普通膜面扭曲；black hole 由数据行的 `shape.x > 0.5` 区分，额外做虚空门暗心、薄边、swirl 和 suction。
  - 有 occlusion 时用主场景 depth 和 effectDepth 做可见性过滤。
  - 最终用 `texCoord + totalOffset` 采样场景，再混入全屏相位滤镜、黑洞暗心、边缘高光和膜面提亮。
- ring 实例和渲染：
  - `VoidRingInstance.Preset.RenderStyle.FULL` 会写后处理；`FLASH` 是轻量小闪光，不写世界扭曲。
  - ring 的中心可固定、跟随实体或 persistent id 平滑追目标；persistent 的 progress 固定约 0.35。
  - `VoidRingRenderer.computeMetrics` 用 0-0.18 展开、peakHold、再 collapse 的时间线算半宽半高、fade、lineAlpha。
  - FULL 普通渲染是两层柔光、17 层 volume shell、收线层；FLASH 只画两层平面柔光。
  - facing 分普通白光和 distortion 两套，可分别决定是否跟随镜头 pitch。
- black hole 实例和渲染：
  - `VoidBlackHoleInstance.Config` 同时控制虚空门本体、边缘、flatGate 切面模式和后处理扭曲参数。
  - `VoidBlackHoleRenderer` 分 coreFacing 和 distortionFacing；门本体默认保持世界竖直感，扭曲默认跟随镜头 pitch。
  - 可见层由虚空门中心暗面和边缘切口组成；`flatGate(true)` 会把边缘压成更薄的一片空间切口。
  - `centerShadowScale` 只给后处理黑心用，collector 的持续黑洞把可见核心和 rim 关掉，只保留扭曲。
- trail 实例和渲染：
  - `VoidTrailManager` 汇总玩家虚空拖尾、实体拖尾和一次性世界坐标段；玩家拖尾只在 `IN_VOID` 或本地持有 SpatialSword 使用中采样。
  - trail 采样有 startDelay、sampleInterval、minMoveDistance、pointSpacing、maxInterpolationSteps；停止移动会 `startNewSegment`，防止不连续段连起来。
  - `VoidTrailRenderer` 用 scratch 数组构建当前点，按 segmentId 分段；每点根据前后方向算 tangent，再算侧向轴，避免折返时翻面。
  - 普通 trail 每段画竖向柔光/主体和侧向柔光/主体，边缘用 `ribbonFadeSegments` 分段渐隐；shader 兼容路径增加 bloom pass 和向白色推进的颜色。
- beam 实例和渲染：
  - `VoidBeamManager` 只管理短生命周期 beam；坐标制定器预览和炮台射击都复用它。
  - `VoidBeamRenderer` 从 start/end 算方向，再用相机位置算 billboardAxis 和 crossAxis；退化时用世界 up/right 兜底。
  - 每条 beam 由主朝向和交叉朝向两组 ribbon 组成，7 段边缘渐隐；shader 兼容有主 pass 和 glow pass。
- 炮台球渲染：
  - `PhaseEmitterClientManager` 区分 ACTIVE 和 BLOCKING：ACTIVE 只显示炮台球，BLOCKING 才接管本地左右键并隐藏手。
  - `PhaseEmitterSet` 第一人称以相机 forward/right/up 计算环绕位置，第三人称以玩家朝向后方椭圆轨道计算位置；本地第一人称 followStrength 接近 1。
  - `PhaseEmitterOrbRenderer` 画面向相机的径向圆盘；非 shader 路径额外画 halo，shader 兼容路径补反向面防止背面剔除。
  - 炮台射击 S2C 只同步命中坐标和 beam 配置；客户端本地生成 beam、炮口 FLASH ring 和命中 FLASH ring。
- 其他渲染相关入口：
  - `CoordinateBindingPreviewClient` 在手持坐标制定器时每 4 tick 扫 64 格内已加载方块实体，用绿色/红色短 beam 显示输出/输入连接。
  - `VoidPhenomenonCollectorBlackHoleClient` 每 2 tick 扫 96 格内 active collector，给每个运行中发电机维护一个 distortion-only persistent black hole；丢失 4 次扫描后移除，避免短暂闪断。
  - `VoidArrowRenderer` client mixin 会隐藏被虚空射手拖尾追踪的原版箭模型，防止箭模型和拉丝重复显示。
  - `PhaseProjectionSectionCompilerMixin` 在区块编译时把相位方块的 draw state 换成投影方块或空气；`PhaseProjectionClient` 收到快照后标脏相位维度范围，让原版 chunk mesh 重建。
- GUI 和转场渲染：
  - `EnergyHud` 是 `RegisterGuiLayersEvent` 注册的 hotbar 上层 HUD，按服务端同步百分比选择 0-100 帧贴图；超过 1500ms 没更新就隐藏。
  - `PhaseWorldTransitionOverlay` 用 512x512 动态 mask 纹理画进入白圈、退出窗口和全白 hold，加载屏也复用这个 overlay。
  - `PhaseWorldTransitionClient` 的关键计时由 overlay 可见帧推进，基础转场 250ms；光影兼容模式会等全白可见帧后再向服务端发 ready，避免后处理丢帧露底。
  - 机器 GUI 统一走 `GuiDraw` / `GuiStyle`：先暗色背景，再面板、分割线、背包区域、槽位和标签页；文本长时用 `GuiDraw.clip` 截断。

## GUI 和网络

- 网络入口 `ModNetworking`，网络版本当前是 `33`。
- 坐标绑定面板、区块映射器状态面板都是轻量状态包，不直接同步完整方块实体。
- 服务端到客户端：相位环、拖尾、黑洞、能量 HUD、世界过渡、相位投影、炮台状态/射击、绑定面板、区块映射器状态面板。
- 客户端到服务端：使用手表模块、Blink 释放、炮台输入、世界过渡 ready、删除绑定、设置区块映射器档位。
- 机器 GUI 用 `GuiStyle` 和 `GuiDraw` 统一暗色面板风格。
- `ModuleBoostMenu` 用模块和最多 3 个 modifier 生成带新 `ModuleData` 的模块结果，禁止重复 modifier 类型。
- `VoidPhenomenonCollectorMenu` 有概览/连接页，槽位在连接页隐藏不可交互。
- `VoidChargerMenu` 按充能器档位计算槽位布局。

## 资源与数据

- 资源位于 `src/main/resources/assets/void_craft`，包括方块/物品模型、贴图、GUI 能量帧、音效、语言和 post effect shader。
- 数据位于 `src/main/resources/data/void_craft`，包括配方、战利品表、伤害类型、附魔、维度和世界生成配置。
- `MobEnergyDrops` 按生物类别掉 chaos/neutral/pure energy；当前 neutral 掉率 5%，pure 掉率 10%。
- `ModLootTables` 注入结构宝箱和钓鱼宝藏，产出模块和 modifier。

## 配置项

- 客户端配置 `energyHud`：位置默认 `BOTTOM_RIGHT`，X/Y 偏移默认 12，范围 0-512。
- 通用配置 `phaseTurret.emitterCount`：默认 4，范围 1-20；读取失败时回退默认值。
