# Resources and data memory

更新时间：2026-05-29。

## 资源目录

- assets：`src/main/resources/assets/void_craft`
- data：`src/main/resources/data/void_craft`
- shader：`assets/void_craft/post_effect/void_phase.json`、`assets/void_craft/shaders/post/void_phase.fsh`、`assets/void_craft/shaders/post/void_copy.fsh`
- 语言：`assets/void_craft/lang/en_us.json`、`assets/void_craft/lang/zh_cn.json`
- 音效注册在 `assets/void_craft/sounds.json` 和 `Sound/ModSound.java`。

## 模型和物品状态

- 坐标制定器模型使用 `CustomModelData` 字符串区分 output/input/unbind。
- modifier 物品把 modifier 类型 id 写进 `CustomModelData.strings()`。
- 相位手表和模块 tooltip 的按键提示使用 `Component.keybind(...)`，会跟随玩家实际改键。
- 相位炮台形态名使用语言 key：`phase_turret_form.void_craft.normal`、`phase_turret_form.void_craft.destroy`。
- 虚空模块形态名使用语言 key：`void_module_form.void_craft.phase`、`void_module_form.void_craft.void`。

## JEI

- JEI 插件：`compat/jei/VoidCraftJeiPlugin`。
- 注册两个自定义展示类型：`module_turn` 和 `module_boost`。
- `MODULE_TURN` 展示模块转化配方。
- `MODULE_BOOST` 展示模块强化。
- 辅助炮台切换展示的中心物品包含四种炮台模块：`PHASE_TURRET_MODULE`、`ASSIST_PHASE_TURRET_MODULE`、`HEALTH_PHASE_TURRET_MODULE`、`HEALTH_ASSIST_PHASE_TURRET_MODULE`。
- `registerRecipeCatalysts` 把强化台作为 `MODULE_BOOST` 和原版 crafting 的工作站，也把 crafting table 作为 `MODULE_TURN` 工作站。

## 掉落和战利品

- `MobEnergyDrops` 按生物类别掉能量物品。
- 当前 neutral 掉率 5%，pure 掉率 10%。
- `ModLootTables` 注入结构宝箱和钓鱼宝藏，产出模块和 modifier。

## 世界生成和配置

- `ModWorldGeneration` 注册世界生成相关内容。
- 虚矿和相位维度数据在 `data/void_craft` 下。
- 客户端配置 `energyHud`：位置默认 `BOTTOM_RIGHT`，X/Y 偏移默认 12，范围 0-512。
- 通用配置 `phaseTurret.emitterCount`：默认 4，范围 1-20，读取失败时回退默认值。
