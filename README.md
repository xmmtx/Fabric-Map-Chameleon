<div align="center">
  <a><img width="144px" alt="logo" src="./icon.svg"/></a>
  <h1 align="center">Fabric Map Chameleon</h1>
  <p><em>Sends customizable world names to client-side map mods on multi-world servers.</em></p>
</div>

<div align="center">
  <a href="https://github.com/xmmtx/Fabric-Map-Chameleon/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/xmmtx/Fabric-Map-Chameleon" alt="License" />
  </a>
  <a href="https://www.xm233.cn/sponsor">
    <img src="https://img.shields.io/badge/%24-sponsor-F87171.svg" alt="sponsor" />
  </a>
</div>

English | [中文](./README_sc.md)

## Introduction
Sends customizable world names to client-side map mods (VoxelMap, Xaero's Map) on multi-world servers. Supports three modes: server level-name, custom name, or random digits.

## Environment
**Platform:** `Java Edition`  
**Mod Loader:** `Fabric`  
**Side:** `Server only`  
**Dependencies:** `Fabric API`  

## Usage
1. Download from [Releases](https://github.com/xmmtx/Fabric-Map-Chameleon/releases) — pick the jar that matches your MC version
2. Place the jar in the server's `mods` folder
3. A config file will be created on first startup: `config/map-chameleon.properties`
4. Edit it as needed. The default config looks like this:

```properties
# Map Chameleon Configuration
# Edit, save, then restart the server. | 修改后保存，重启服务器即可。
# mode: level | custom | random
#   level  = use server.properties level-name | 使用服务器 level-name
#   custom = use the name below            | 使用下方 name 的值
#   random = random digits (see length)    | 随机数字 (参见 length)
mode=level

# name: custom world name (used when mode=custom)
#       自定义世界名称 (mode=custom 时生效)
name=world

# length: number of digits for random name (used when mode=random)
#         随机数字位数 (mode=random 时生效)
length=12
```

5. Restart the server and enjoy!

## Building
- **Default:** `./gradlew build` → `map-chameleon-<version>+mc1.20.5-26.2.jar`
- **Legacy (MC 1.20–1.20.4):** `./gradlew build -Pminecraft_version=1.20.4 -Pyarn_mappings=1.20.4+build.3 -Pfabric_version=0.96.11+1.20.4 -Pjava_version=17` → `map-chameleon-<version>+mc1.20-1.20.4.jar`

## License
This project is licensed under the [GPL-3.0 License](./LICENSE).

