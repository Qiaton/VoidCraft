# Validation memory

更新时间：2026-05-29。

## 常用命令

- 编译：`./gradlew --offline compileJava`
- 完整构建：`./gradlew --offline build`
- 资源 JSON 校验：`find src/main/resources -name '*.json' -print0 | xargs -0 jq empty`
- 补丁空白检查：`git diff --check`
- 查看工作区：`git status --short`

## 何时跑什么

- 只改记忆文档时，不需要跑 Gradle。
- 改 Java 逻辑时，至少跑 `./gradlew --offline compileJava` 和 `git diff --check`。
- 改资源 JSON、语言文件、模型、配方、战利品表时，加跑 `jq empty`。
- 改网络包、数据组件、菜单、payload id 时，优先跑完整 `build`，并确认 `NETWORK_VERSION` 是否需要递增。
- 改渲染链路时，除了编译，还要在客户端实际观察或用截图确认后处理和特效没有空白。

## 注意事项

- 当前仓库经常会有用户未提交源码改动；不要回退未确认的改动。
- 记忆文件可以按用户要求修改；源码仍按“开工前不改代码”的约定处理。
- 若看到 `agent.md` 和 `memory/` 变化，这是项目记忆，不是功能源码。
