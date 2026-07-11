# Map Chameleon v2.0 — 开发进度技术备忘录

> 日期：2026-07-11 | 构建状态：✅ BUILD SUCCESSFUL | 目标 MC：1.20.5–1.21.x (Java 21)

---

## 一、项目概况

**Fabric 服务端模组**，包名 `pl.kosma.mapchameleon`，作者 xmmtx，GPL-3.0 协议。
灵感来源于 [MapModCompanion](https://github.com/turikhay/MapModCompanion)（MIT），在其基础上加入了路径点共享、多存储驱动、BlueMap/JourneyMap 集成。

**核心能力：**
- 为多世界服务器伪装世界标识符，防止 VoxelMap / XaeroMap / JourneyMap 各维度地图数据互相覆盖
- 玩家间协作路径点分享（带冷却机制）
- BlueMap 网页地图 POIMarker 同步
- JourneyMap 服务端插件
- 多种存储后端

---

## 二、目录结构

```
src/main/java/pl/kosma/mapchameleon/
├── MapChameleonServer.java          ← 主入口 ModInitializer
├── MapChameleonConfig.java          ← YAML 配置加载/保存
├── integration/
│   ├── BlueMapIntegration.java      ← BlueMap POIMarker 同步
│   └── JourneyMapServerPlugin.java  ← JourneyMap IServerPlugin (反射)
├── mixin/
│   └── MixinPlayerManager.java      ← 挂钩 PlayerManager.sendWorldInfo
├── network/
│   ├── WorldNamePacket.java         ← 底层字节协议解析 (VoxelMap/JourneyMap/XaeroMap)
│   ├── WorldNameHandler.java        ← CustomPayload 注册 + 请求-响应处理
│   ├── WaypointSharePayload.java    ← C→S 分享路径点
│   ├── WaypointDeletePayload.java   ← C→S 删除路径点
│   └── WaypointSyncPayload.java     ← S→C 广播路径点 (新增/移除)
└── storage/
    ├── MapStorageEngine.java        ← 接口 (5 个方法 + close)
    ├── SharedWaypoint.java          ← 数据实体 (POJO)
    ├── StorageFactory.java          ← 工厂 (switch on type)
    ├── FileStorageEngine.java       ← JSON 文件存储 (默认)
    ├── JdbcStorageEngine.java       ← JDBC 抽象基类 (SQLite/H2/MySQL)
    ├── SQLiteStorageEngine.java     ← SQLite (waypoints.db)
    ├── H2StorageEngine.java         ← H2 内嵌 (MySQL 兼容模式)
    └── MySqlStorageEngine.java      ← MySQL/MariaDB (HikariCP 连接池)

src/main/resources/
├── fabric.mod.json                  ← 模组元数据 + entrypoints + mixins
└── map-chameleon.mixins.json        ← Mixin 配置
```

---

## 三、已完成 ✅

### 3.1 世界名称伪装（核心功能）— 完整实现

| 组件 | 状态 | 说明 |
|------|------|------|
| `WorldNamePacket` | ✅ | 解析 5 种 VoxelMap/JourneyMap 请求变体（Forge 1.12.2 / 1.13–1.16.3 / 1.16.4+ / Fabric bug / LiteLoader），生成 XaeroMap 响应 |
| `WorldNameHandler` | ✅ | Fabric CustomPayload 注册 + 请求-响应处理 + XaeroMap 主动推送 |
| `MixinPlayerManager` | ✅ | 挂钩 `PlayerManager.sendWorldInfo()`，世界切换/重生时重新推送 XaeroMap 数据 |

### 3.2 路径点共享 — 完整实现

| 组件 | 状态 | 说明 |
|------|------|------|
| `WaypointSharePayload` | ✅ | C→S 网络包，name/x/y/z/worldId |
| `WaypointDeletePayload` | ✅ | C→S 网络包，仅所有者可删除 |
| `WaypointSyncPayload` | ✅ | S→C 广播包，含 deleted 标志 |
| 分享冷却 | ✅ | `waypointShareCooldownSeconds`，默认 3 秒，`ConcurrentHashMap` 实现 |
| 玩家加入同步 | ✅ | 玩家加入时自动推送当前世界所有已存在的公共路径点 |

### 3.3 存储引擎 — 完整实现

| 引擎 | 状态 | 说明 |
|------|------|------|
| `MapStorageEngine` 接口 | ✅ | 6 个方法，全部返回 `CompletableFuture` |
| `FileStorageEngine` | ✅ | JSON 文件，`synchronized(lock)` 线程安全，默认引擎 |
| `JdbcStorageEngine` 基类 | ✅ | 建表 DDL、CRUD、MERGE 兜底 DELETE+INSERT |
| `SQLiteStorageEngine` | ✅ | 继承 JDBC 基类，`waypoints.db` |
| `H2StorageEngine` | ✅ | 继承 JDBC 基类，`h2_waypoints`，MySQL 兼容模式 |
| `MySqlStorageEngine` | ✅ | 继承 JDBC 基类，HikariCP 连接池 (max=5, timeout=5s) |
| `StorageFactory` | ✅ | 支持 mysql/mariadb/sqlite/h2/file，未知类型回落 File |
| `SharedWaypoint` 实体 | ✅ | POJO，包含 id/serverId/worldId/name/x/y/z/ownerUuid/ownerName/createdAt/updatedAt |

### 3.4 集成 — 完整实现

| 集成 | 状态 | 说明 |
|------|------|------|
| `BlueMapIntegration` | ✅ | POIMarker 双向同步 (add/remove)，`MARKER_SET_ID = "map_chameleon"`，10000 格可见 |
| `JourneyMapServerPlugin` | ✅ | `@JourneyMapPlugin(apiVersion = "2.0.0")`，反射获取 GUID 以避免映射冲突 |

### 3.5 配置 — 完整实现

| 组件 | 状态 | 说明 |
|------|------|------|
| `MapChameleonConfig` | ✅ | YAML 格式 (`config/map-chameleon/config.yml`)，首次运行自动生成，支持 database/server_settings/features/waypoint 四个 section |

### 3.6 CI/CD — 完整

| 文件 | 状态 | 说明 |
|------|------|------|
| `.github/workflows/build.yml` | ✅ | 矩阵构建 MC 1.20–1.20.4 + MC 1.20.5–1.21.x，自动上传 artifact，tag 时创建 Release |

### 3.7 编译修复

| 问题 | 状态 |
|------|------|
| `BlueMapIntegration.java` import 路径 `server.storage` → `storage` | ✅ 已修复 |
| `MixinPlayerManager.java` import 路径 `server.network` → `network` | ✅ 已修复 |
| CI workflow 中 `actions/checkout@v5` 等不存在版本 → `@v4` | ✅ 已修复 |

---

## 四、待完成 / 占位 / 已知问题 ⚠️

### 4.1 功能缺失

| 项目 | 优先级 | 说明 |
|------|--------|------|
| **客户端模组** | 🔴 高 | 当前仅有服务端代码。客户端需要实现：分享路径点 UI、接收 `WaypointSyncPayload` 后在小地图上渲染标记、删除已分享路径点的按钮。服务端已完整实现，客户端模块完全空白 |
| **JourneyMap 集成未连线** | 🟡 中 | `JourneyMapServerPlugin` 已写好，但 `MapChameleonServer.onInitialize()` 中没有调用它。目前 JourneyMap 路径点不会自动创建/同步，仅有占位接口 |
| **功能开关未生效** | 🟡 中 | `features.voxelmap/xaeromap/journeymap/bluemap` 四个开关已定义在配置中，但 `onInitialize()` 中没有用 `if` 判断。VoxelMap/XaeroMap 始终启用 |
| **README 未更新** | 🟢 低 | README 仍是旧版内容，与当前 v2.0 实现不符 |

### 4.2 代码质量

| 项目 | 优先级 | 说明 |
|------|--------|------|
| **broadcastWaypoint 中的 getServerPlayers()** | 🟡 中 | 通过 static `ServerHolder` 获取玩家列表，耦合度高。可改为接收 `MinecraftServer` 参数或使用事件系统 |
| **handleWaypointDelete 匹配逻辑脆弱** | 🟡 中 | 用 name+x+y+z+ownerUuid 匹配，应由客户端发送 `waypointId` 直接删除 |
| **BlueMapIntegration.getOrCreateMarkerSet** | 🟢 低 | 暴力遍历所有地图创建 MarkerSet，可优化为只对目标世界操作 |
| **JdbcStorageEngine 未使用 PreparedStatement try-with-resources 的 Connection** | 🟢 低 | connection 字段直接暴露，线程安全性依赖调用方 |
| **没有单元测试** | 🟡 中 | 所有代码 0 测试 |

### 4.3 架构待定

| 项目 | 说明 |
|------|------|
| **多服务器支持** | `serverId` 字段已在 SharedWaypoint 和配置中预留，路径点查询也都带了 `serverId` 过滤，但尚未在实际多服务器场景下测试 |
| **路径点分页** | 大型服务器可能有数千个路径点，当前全量推送，可能导致首次加入时网络拥塞 |

---

## 五、关键架构决策

1. **异步模型**：所有存储操作通过 `CompletableFuture` + 4 线程 `ExecutorService` ("MapChameleon-Worker") 异步执行
2. **网络层**：使用 Fabric MC 1.20.5+ 原生 `CustomPayload` + `PacketCodec<RegistryByteBuf, T>`（非旧版 PacketByteBuf）
3. **存储可插拔**：`MapStorageEngine` 接口 + `StorageFactory`，支持 JSON/SQLite/H2/MySQL
4. **依赖打包**：SnakeYAML、HikariCP、MySQL Connector、SQLite、H2 均通过 `include` 打包进 JAR
5. **Mixin 策略**：仅 1 个 Mixin（`PlayerManager.sendWorldInfo`），最小侵入原则
6. **配置格式**：YAML（SnakeYAML 2.2），非旧版 `.properties`
7. **协议兼容**：`WorldNamePacket.parseRequest()` 处理 5 种 VoxelMap/JourneyMap 历史变体，向前兼容

---

## 六、构建信息

| 属性 | 值 |
|------|-----|
| Gradle | 8.10.2 |
| Fabric Loom | 1.7.4 |
| Fabric Loader | 0.16.9 |
| Fabric API | 0.110.0+1.21.4 |
| 开发 MC 版本 | 1.21.4 |
| 产物 | `build/libs/map-chameleon-2.0.0+mc1.20.5-1.21.jar` (~18 MB) |
| CI 矩阵 | MC 1.20–1.20.4 (Java 17) + MC 1.20.5–1.21.x (Java 21) |
