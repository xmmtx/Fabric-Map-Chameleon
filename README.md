<div align="center">
  <h1 align="center">Fabric Map Chameleon</h1>
  <p><em>Support for client map mods for players using multi-world servers.</em></p>
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
This project can support for client map mods for players using multi-world servers.

## Environment
**Platform:** `Java Edition`  
**Mod Loder:** `Fabric`  
**Side:** `Only server`  
**Dependencies:** `Fabric API`  

## Usage
1. Go to the release to download, choose the right version for Minecraft
2. Put it in `mods` folder
3. config flie will create when first start
4. chage config to appropriate, this is defidoult one
```properties
# Map Chameleon Configuration
# 修改后保存，重启服务器即可
# mode: LEVEL_NAME | CUSTOM | RANDOM
mode=LEVEL_NAME

# name: 自定义世界名称 (mode=CUSTOM 时生效)
name=My World

# length: 随机数字位数 (mode=RANDOM 时生效)
length=12
```
5. enjoy it
