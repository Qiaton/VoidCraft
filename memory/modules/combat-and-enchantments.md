# Combat and enchantments memory

更新时间：2026-05-31。

## 虚空状态

- `IN_VOID` / `IN_PHASE` / `VOID_SPEED` 是 NeoForge attachments。
- `VoidClock` 用 tick 表驱动实体虚空状态和客户端虚空闪光。
- `VoidEvents` 处理虚空实体免伤、免交互、免拾取、怪物不锁定、保氧、清火、相位内禁游泳、跳跃速度和虚空物理状态。
- 相位规则按“带重力、能踩方块的旁观者”分层：保留方块碰撞和重力，只在 `LivingEntity.travel` 的流体移动判断里让 `isAffectedByFluids` 返回 false，跳过水下 travel 减速/浮力/流体影响，但不全局关闭该方法，避免破坏原版跳跃入口。
- `VoidEntity` 对相位实体拦截 `isInWater`、`isInLava`、`isInFluidType`、`isPushedByFluid`、`makeStuckInBlock`、`getBlockSpeedFactor`、`getBlockJumpFactor`；不再依赖 `VoidLivingEntityClass.travel` 的流体内最后速度结算。
- `VoidLivingEntityClass` 只保留相位实体的推挤/选取/固定速度/默认摩擦/流体移动分支总开关，摩擦统一返回普通方块的 `0.6F`，避免冰、黏液等方块材质改变相位移动；相位实体处于真实流体时，`travel` TAIL 会按输入和相位速度统一重算水平速度，不区分踩地和浮空。
- 相位实体处于真实流体且长按跳跃时，`travel` TAIL 会把 Y 速度至少补到 `0.3D`，实现水里持续上浮。
- 相位仍忽略蜘蛛网等 `makeStuckInBlock` 卡住效果、方块速度倍率、方块跳跃倍率、实体推挤和准星选取。
- `IN_VOID` 是完整旁观者层：在相位免疫基础上临时开启玩家 `mayfly/flying`，同时保存并开启实体 `noPhysics` 和 `noGravity`，退出虚空后按进入前状态恢复；相位状态本身不额外给飞行、不穿方块、不清重力。
- 虚空目标 AI：`LivingChangeTargetEvent` 遇到虚空目标时取消目标切换；已有目标在 `clearMobTarget` 里清掉；还会清 `Brain` 的 `MemoryModuleType.ATTACK_TARGET` 和 `LOOK_TARGET`，并在怪物仍看向相位/虚空实体时重置 `LookControl`。

## 现有 mixin

- `VoidEntity`、`VoidLivingEntityClass`、`VoidPlayer` 处理碰撞、推挤、投射物命中、旁观者式环境免疫、拾取选取、速度和摩擦。
- `PhasePlayerTraversalMixin` 防止原版 tick 重置相位穿墙的 `noPhysics`。
- `PhaseProjectionSectionCompilerMixin` 让相位投影参与客户端区块编译。
- `PhaseProjectionRenderSectionRegionMixin`、`PhaseProjectionSectionOcclusionMixin` 辅助相位投影渲染和遮挡。
- `VoidArrowRenderer` 隐藏虚空射手拖尾追踪的原版箭模型。
- `VoidArcherArrowDamageMixin` 修正虚空射手高速箭的原版命中伤害和死亡消息。

## VoidArcher

- 附魔 key：`ModEnchantment.VoidArcher.VOID_ARCHER`。
- 箭进入世界和 tick 时检测附魔等级，隐藏箭模型，锁定初始瞄准方向，按等级加速。
- 加速倍率为每级 +2.0；最大箭寿命 80 tick。
- 为避免高速导致原版直击伤害暴涨，会把基础伤害除回加速倍率，并在命中前还原。
- 命中活体时先等待原版直击伤害结算，再按直击伤害 * 0.9 做 AOE。
- AOE 基础半径 2.5，每级 +1.5，最大 AOE 目标 48。
- 方块或非活体命中会取消原版箭命中并直接结算 AOE。
- 替换原版射箭声为 `void_archer_shoot`；命中发相位环和 `void_archer_hit`；飞行发实体拖尾。

## VoidHunter

- 附魔 key：`ModEnchantment.VoidHunter.VOID_HUNTER`。
- 只读取剑类物品，触发时机是 `LivingDamageEvent.Post`。
- 如果目标生命低于最大生命 * `0.05 * level`，会执行处决链。
- 处决使用 `VOID_HUNTER` 伤害类型，避免递归触发。
- AOE 伤害为原始伤害 * 0.6。
- AOE 搜索盒按目标碰撞盒尺寸 * 3 膨胀。
- 每 tick 最多处理 10 个连锁目标。
- 击杀视觉是短相位环，yaw 固定，turn 带随机偏移，并播放 `void_archer_hit`。
