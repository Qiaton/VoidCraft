# Void energy memory

更新时间：2026-05-29。

## 代码位置

- 虚空能行为：`src/main/java/com/example/voidcraft/Custom/Behavior/Energy`
- 方块实体：`src/main/java/com/example/voidcraft/Block/entity`
- 方块：`src/main/java/com/example/voidcraft/Block/Block`
- GUI 菜单：`src/main/java/com/example/voidcraft/Gui`

## 传输模型

- `BoundVoidPosition` 保存维度和方块坐标。
- `VoidEnergyBinding` 保存另一端位置和绑定类型。
- `VoidEnergyTransferBlockEntity` 是虚空能端点接口。
- `VoidEnergyTransfer` 负责解析端点、建立两端绑定、删除绑定、推送输出和描述绑定状态。
- 模型是输出端主动推给输出列表；输入端不主动拉能量。
- 输出分配会轮换起点，先收集目标需求，再按需求拆分可输出能量。
- 失效目标会从输出列表移除；目标不能输入时，也会移除对方的输入记录。

## 机器

- `BatteryBlockEntity`：双向缓存，容量 40000，默认满电，按 0-9 档刷新模型。
- `VoidPhenomenonCollectorBlockEntity`：只输出虚空能，档位决定产能、缓存和结晶槽数。I 为 1/t、50000、1 槽；II 为 2/t、70000、3 槽；III 为 3/t、150000、6 槽；IV / `void_attuner` 为 5/t、500000、9 槽。
- `VoidChargerBlockEntity`：只输入虚空能，用能量修复 `EnergyCoreItem`。低级 1 槽/5000/每 tick 修 1；中级 3 槽/50000/每 tick 修 1；高级 9 槽/200000/每 tick 修 10；每点修复消耗 20 虚空能。
- `ChunkMapperBlockEntity`：只输入虚空能，档位半径 `{0,1,2,3}`，每 tick 耗能 `{1,16,32,128}`，缓存 10000，只允许 1 个输入绑定。
- `VoidEnergyConverterBlockEntity`：双向虚空能缓存 10000，FE 每 tick 输入/输出上限各 1000，FE 转虚空能倍率 0.3，虚空能转 FE 倍率 2.0。
- `VoidEnergyConverterBlockEntity` 每服务端 tick 会主动处理相邻 FE 方块；`INPUT` 面抽 FE，`OUTPUT` 面推 FE。
- 转换方块切面模式时使用 `Block.UPDATE_ALL`、`level.invalidateCapabilities(pos)` 和 `updateNeighborsAt(...)`，让管道/机器重新扫描 capability。

## 坐标制定器

- `CoordinateDesignatorItem`：
  - OUTPUT：先点输出端，再点输入端。
  - INPUT：先点输入端，再点输出端。
  - UNBIND：打开连接列表面板。
  - 区块映射器单输入覆盖需要二次确认。
  - 潜行右键切换模式，并清空第一次记录。
  - 模式写到 `CustomModelData` 字符串，用于物品模型切换。
  - 区块映射器输入覆盖确认窗口是 100 tick，同一玩家、模式、来源和目标才算确认。

## 客户端辅助

- `CoordinateBindingPreviewClient` 手持坐标制定器时，每 4 tick 扫 64 格内已加载方块实体。
- 预览用绿色/红色短 beam 显示输出/输入连接。
- 坐标绑定面板和区块映射器状态面板都是轻量 payload，不让客户端推算完整方块实体状态。
