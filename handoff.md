# WorldNamePacket → 新 Mod 交接文档

## 项目概况
- **定位**: Fabric 服务端辅助模组，向客户端小地图 mod 发送世界名称
- **当前版本**: 2.0.0，适配 MC 1.21.4，仅 Fabric
- **源码**: `pl.kosma.worldnamepacket`

## 核心文件 & 职责

| 文件 | 作用 | 是否需要改 |
|------|------|-----------|
| `FabricMod.java` | Mod 入口，注册网络通道，处理请求/响应 | **是** — 新功能加这里 |
| `WorldNamePacket.java` | 纯协议逻辑：字节包格式、十六进制工具方法 | **看情况** — 改协议才动 |
| `MixinPlayerManager.java` | Mixin 注入 `PlayerManager.sendWorldInfo()`，在玩家入服/切世界时触发 | **可能** — 新触发点加这里 |
| `build.gradle` | Loom 1.7.4, Java 21, Fabric API | **是** — 改模组名/版本 |
| `fabric.mod.json` | id, entrypoints, depends | **是** — 改 id 和入口类 |

## 网络通信机制（关键！）

两个 CustomPayload 通道：

```
VoxelMapPayload  → worldinfo:world_id  (C2S 请求 + S2C 响应)
XaeroMapPayload  → xaeroworldmap:main   (仅 S2C，入服/切世界时主动推)
```

响应包格式（`WorldNamePacket.formatResponsePacket`）:
- Forge 客户端: `00 42 <len> <world_name>`
- VoxelMap Fabric 客户端: `42 <len> <world_name>`（通过检测请求包前 4 字节区分）

## Mixin 注入点
```
@Mixin(PlayerManager.class)
method = "sendWorldInfo(ServerPlayerEntity, ServerWorld)"
at = @At("HEAD")
```
→ 仅用于触发 XaeroMap 推送。VoxelMap 走请求-响应，不需要 Mixin。

## 构建配置要点
- `gradle.properties`: minecraft_version, yarn_mappings, loader_version, fabric_version
- `gradle-wrapper.properties`: Gradle 8.10.2
- 必须用 JDK 21（Java 25 的 class file v69 不被 Gradle 8.10 Groovy 支持）
- `org.gradle.java.home` 指向 JDK 21 路径
- 无测试目录，command: `./gradlew build`

## 新 Mod 起步清单
1. 修改 `fabric.mod.json` 的 `id`（唯一标识）
2. 修改 `gradle.properties` 的 `maven_group`, `archives_base_name`
3. 重命名/新建 Java 包路径
4. 更新 Mixin JSON 中的 `package`
5. 删除旧 jar，`./gradlew clean build`

## 已知坑
- `MinecraftDedicatedServer.getLevelName()` 强转——在单人/局域网集成服务器上会 ClassCastException
- VoxelMap Fabric 版的请求/响应格式与 Forge 版不同（已通过检测首字节处理）
- 字符串拼接用 `StringBuffer`（老代码风格，可不动）
