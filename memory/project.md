# Project memory

更新时间：2026-05-29。

## 项目定位

- `/Users/a11/Documents/VoidCraft` 是 VoidCraft 主线原项目。
- 这是 NeoForge 1.21.11 模组，主包是 `com.example.voidcraft`，mod id 是 `void_craft`。
- 当前源码扫描：`src/main/java` 约 202 个 Java 文件、34172 行。
- `gradle.properties`：Minecraft `1.21.11`，NeoForge `21.11.42`，Parchment `2025.12.20`，mod version `1.0.2`。
- `build.gradle` 使用 ModDevGradle `2.0.141`，Java toolchain 21，JEI 以本地 jar `libs/jei-1.21.11-neoforge-27.4.0.22.jar` 作为 `compileOnly`。

## 入口注册

- `VoidCraft` 构造函数注册：CreativeModeTabs、方块、方块物品、方块实体、capability、物品、配方 serializer、attachments、音效、网络包、数据组件、菜单、世界生成、区块票、实体能量掉落和战利品注入。
- `VoidCraftClient` 负责客户端菜单绑定、能量 HUD、相位世界遮罩、炮台球输入、坐标绑定预览、相位投影 tick、方块闪白渲染等。

## 数据组件和附件

- `ModDataComponents` 使用 NeoForge `DeferredRegister.createDataComponents`。
- 主要组件：`MODULE_DATA`、`MODULE_MODIFIER_DATA`、`WATCH_ENERGY`、`ENERGY_CORE_DATA`、`COORDINATE_DESIGNATOR_DATA`、`PHASE_TURRET_FORM`、`VOID_MODULE_FORM`、`VOID_CRYSTAL_PROGRESS`。
- `ModAttachments` 主要有 `IN_PHASE`、`IN_VOID`、`VOID_SPEED`、`GUIDE_BOOK_GIVEN`。
- 1.21.11 版本使用原版数据组件和 `DataComponents.CONTAINER` 保存物品容器数据，不走手写 NBT 容器。

## 修改原则

- 改源码前先读附近代码；保留当前命名、缩进、事件注册和注释风格。
- 功能补丁尽量小，不做无关重构。
- 新增状态优先放在已有数据组件、附件、payload、manager 或事件类里。
- mixin 只在公共 API 和事件无法覆盖原版行为时使用。
