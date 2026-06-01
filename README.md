# VoidCraft

[![Build](https://github.com/Qiaton/VoidCraft/actions/workflows/build.yml/badge.svg)](https://github.com/Qiaton/VoidCraft/actions/workflows/build.yml)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen)
![NeoForge](https://img.shields.io/badge/NeoForge-21.11.42-orange)
![License](https://img.shields.io/badge/License-MIT-blue)

VoidCraft is a NeoForge mod for Minecraft focused on void energy, modular phase watches, combat modules, and a phase-shifted dimension called the Phase Shallows.

Players gather void materials, build a void energy network, install modules into a Phase Watch, and use those modules to move, fight, recover, enter void states, or travel into the Phase Shallows.

## Features

- Void energy progression built from mob energies, void ore, crystals, storage blocks, chargers, and collectors.
- Phase Watches with module slots and energy cores, designed as the main tool for active abilities.
- Module skills for void state, recovery, dash, blink, teleportation, phase turrets, rifts, and world travel.
- Module boosting through modifier parts that tune cooldown, speed, active duration, and other module-specific behavior.
- Phase Shallows dimension with phase transition visuals, overworld projection, and pure void crystal aggregation.
- Utility machines such as the Coordinate Designator, Chunk Mapper, and Void Energy Converter.
- Combat enchantments including Void Archer and Void Hunter.

## Gameplay Overview

VoidCraft's main loop starts with mining void ore and collecting Chaos, Neutral, and Pure Energy from mobs. These materials craft into Void Energy, crystals, energy cores, and the first Phase Watch.

Void crystals can be placed into Void Phenomenon Collectors to produce void energy. That energy can be stored, moved between machines, used to repair energy cores, converted to Forge Energy, or spent by watch modules.

The Phase Watch is the center of player abilities. It holds two modules and one energy core. Once equipped in the off hand, the watch lets the player trigger module skills with keybinds. Some modules use channel and burst modes, and boosted modules read modifier parts in their own way.

The Phase Shallows is VoidCraft's special dimension. It is entered with the World Module, shows projections from nearby overworld terrain, and is used for pure void crystal aggregation.

## Requirements

- Minecraft `1.21.11`
- NeoForge `21.11.42`
- Java `21`
- Gradle wrapper from this repository

## Installation

Download a built mod jar from the release page when releases are available, then place it in the Minecraft `mods` folder for a matching NeoForge instance.

To build from source:

```bash
./gradlew build
```

The generated jar is written under `build/libs/`.

## Development

Run the client from the development environment:

```bash
./gradlew runClient
```

Run the standard build:

```bash
./gradlew build
```

Validate resource JSON files:

```bash
find src/main/resources -name '*.json' -print0 | xargs -0 jq empty
```

If dependency caches are stale, refresh them explicitly:

```bash
./gradlew --refresh-dependencies
```

## Documentation

- Chinese player guide: [docs/zh_cn.md](docs/zh_cn.md)
- Contributing guide: [CONTRIBUTING.md](CONTRIBUTING.md)
- Security policy: [SECURITY.md](SECURITY.md)
- Changelog: [CHANGELOG.md](CHANGELOG.md)

## Contributing

Issues and pull requests are welcome. Please use the GitHub issue templates when reporting bugs or requesting features, and read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request.

When a change touches user-facing text, update both `en_us.json` and `zh_cn.json` where applicable.

## License

VoidCraft is licensed under the [MIT License](LICENSE).

## 中文简介

VoidCraft 是一个面向 Minecraft `1.21.11` 和 NeoForge `21.11.42` 的模组，核心内容围绕虚空能、相位手表、模块技能和相位浅滩展开。

玩家会收集虚空材料，建立虚空能网络，把模块和能量核心装入相位手表，再通过模块进入虚空状态、移动、战斗、恢复、部署炮台或前往相位浅滩。

主要内容包括：

- 虚空能材料、结晶、收集器、存储单位、充电站和能量转换。
- 相位手表、能量核心、模块槽和模块按键技能。
- 虚空、恢复、冲刺、闪现、传送、相位炮台、相位裂口和世界模块。
- 模块强化组件和不同模块自己的强化读取方式。
- 相位浅滩维度、相位转场、原世界投影和纯粹虚空结晶聚合。
- 区块映射器、坐标制定器、虚空射手和虚空猎手等辅助玩法。

中文玩家手册请看：[docs/zh_cn.md](docs/zh_cn.md)。
