<div align="center">
  <a><img width="144px" alt="logo" src="./icon.svg"/></a>
  <h1 align="center">Fabric Map Chameleon</h1>
  <p><em>为多世界服务器的客户端小地图模组发送可自定义的世界名称。</em></p>
</div>

<div align="center">
  <a href="https://github.com/xmmtx/Fabric-Map-Chameleon/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/xmmtx/Fabric-Map-Chameleon" alt="License" />
  </a>
  <a href="https://www.xm233.cn/sponsor">
    <img src="https://img.shields.io/badge/%24-sponsor-F87171.svg" alt="sponsor" />
  </a>
</div>

[English](./README.md) | 中文

## 简介
向客户端小地图模组（VoxelMap、Xaero's Map）发送可自定义的世界名称。支持三种模式：服务器原始名称、自定义名称、随机数字。

## 环境
**平台：** `Java 版`  
**模组加载器：** `Fabric`  
**端：** `仅服务端`  
**前置：** `Fabric API`  

## 用法
1. 前往 [Releases](https://github.com/xmmtx/Fabric-Map-Chameleon/releases) 下载，选择与 MC 版本匹配的 jar
2. 放入服务器的 `mods` 文件夹
3. 首次启动后自动生成配置文件：`config/map-chameleon.properties`
4. 按需修改。默认配置如下：

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

5. 重启服务器，享受吧！

## 构建
- **默认：** `./gradlew build` → `map-chameleon-<version>+mc1.20.5-26.2.jar`
- **旧版（MC 1.20–1.20.4）：** `./gradlew build -Pminecraft_version=1.20.4 -Pyarn_mappings=1.20.4+build.3 -Pfabric_version=0.96.11+1.20.4 -Pjava_version=17` → `map-chameleon-<version>+mc1.20-1.20.4.jar`

## 许可证
本项目基于 [GPL-3.0 许可证](./LICENSE) 发布。
