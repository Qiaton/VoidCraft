# VoidCraft agent memory

更新时间：2026-05-31。

## 协作约定

- 在用户下达开工指令前，不修改源码。
- 修改代码时，方法名尽量用直白、低级词汇表达意思。
- 新代码风格跟随附近旧代码，优先沿用当前包结构、事件写法和注释风格。
- 原项目记忆只记录当前主线代码事实；跨版本工作记录放到对应工作区。
- VoidCraft 代码工作默认不要新增 mixin；优先找公共 API、NeoForge 事件、已有 hook 或小范围本地重构。确实必须用 mixin 时，先说明原因。

## 项目入口

- 工作区：`/Users/a11/Documents/VoidCraft`
- 当前主线提交前基线 HEAD：`4a55bbfa7dea91a5054f6dd953e51e455c05fb56`
- Minecraft / NeoForge：`1.21.11` / `21.11.42`
- Java：21
- mod id：`void_craft`
- mod version：`1.0.4`
- 主入口：`src/main/java/com/example/voidcraft/VoidCraft.java`
- 客户端入口：`src/main/java/com/example/voidcraft/VoidCraftClient.java`
- 网络入口：`src/main/java/com/example/voidcraft/Network/ModNetworking.java`，当前 `NETWORK_VERSION = "37"`

## 记忆索引

- 项目结构和注册入口：`memory/project.md`
- 验证命令和检查习惯：`memory/validation.md`
- 虚空能网络和机器：`memory/modules/void-energy.md`
- 手表、模块数据、强化台和按键：`memory/modules/module-system.md`
- 相位手表容器、能量和打开逻辑：`memory/modules/phase-watch.md`
- 相位炮台、辅助炮台、恢复炮台和形态切换：`memory/modules/turret.md`
- 虚空、冲刺、Blink、传送门、世界模块：`memory/modules/movement-and-world.md`
- 黑洞模块和黑洞事件：`memory/modules/black-hole.md`
- 虚空状态、附魔、战斗事件：`memory/modules/combat-and-enchantments.md`
- 相位维度、投影和穿行：`memory/modules/phase-world.md`
- 客户端特效、后处理和渲染链路：`memory/modules/rendering.md`
- GUI、菜单和网络包：`memory/modules/gui-and-network.md`
- 资源、数据、JEI、掉落和世界生成：`memory/modules/resources-and-data.md`

## 快速路线

- 改功能前先读对应 `memory/modules/*.md`，再读源码。
- 查模块行为先从 `PhaseWatch`、`ModuleSkillClock`、对应 `ModuleType/*Module.java` 看起。
- 查渲染路线先看 `VoidEffect -> VoidPhaseClient -> VoidPhasePostProcessor -> void_phase.json -> void_phase.fsh`。
- 查 GUI/网络路线先看 `Block/Item -> Menu -> Screen -> Payload -> ModNetworking`。
- 查相位世界路线先看 `WorldModule -> GoWorld -> PhaseProjectionSnapshot -> PhaseProjectionClient`。

## 常用验证

- Java 编译：`./gradlew --offline compileJava`
- 完整构建：`./gradlew --offline build`
- 资源 JSON：`find src/main/resources -name '*.json' -print0 | xargs -0 jq empty`
- 空白/补丁检查：`git diff --check`
