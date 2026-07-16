# Mango Resource 六模块收敛设计

## 1. 背景与结论

PR #554 新增了 `mango-resource-target-core` 和 `mango-resource-target-starter`，用于承载微服务目标端的资源执行服务和 HTTP 入口。最终合并树中两个模块的 Java 包路径命中 `.gitignore` 的 `target/` 规则，生产源码和测试没有进入 Git；`target-core` 只剩 POM，`target-starter` 只剩 POM、模块信息和指向不存在类的自动配置声明。

本次不修补两个空模块，而是删除它们，将 Resource 收敛为六个职责明确的子模块：

```text
mango-resource-api
mango-resource-support
mango-resource-core
mango-resource-starter
mango-resource-sync-starter
mango-resource-starter-remote
```

HTTP 路径、JSON 字段、权限码、同步模式、租户语义和目标模块数据副作用保持不变。

## 2. 方案比较

### 方案 A：删除两个 target 模块，收敛到 support 与 sync-starter（采用）

- `support` 提供纯 Java 的目标执行端口和默认执行器。
- `sync-starter` 在现有声明扫描/上报职责上承载目标接收 Controller；二者同属应用资源同步入口。
- `starter-remote` 只保留注册上报和动态目标调用客户端。
- capability app 不再额外依赖 target starter。

优点是模块最少、部署依赖清楚、没有空壳发布物，且 remote starter 不混入服务端 Controller；
代价是 `sync-starter` 同时负责声明出站同步和目标入站同步，需要通过包结构和装配测试保持边界。

### 方案 B：保留一个 target-starter

删除 target-core，把执行逻辑放 support，仅保留独立目标端 starter。它适合“只接收下发、不进行上报”的独立部署，但当前所有 capability app 已经具有远程 Resource 运行时，没有足够事实支持额外发布物。

### 方案 C：保留并重命名两个模块

把 target 改为 execution，补齐 core/starter。分层最严格，但继续增加两个 Maven 制品、发布顺序和应用依赖，不符合当前简化目标。

## 3. 模块边界

| 模块 | 职责 | 禁止内容 |
|---|---|---|
| `resource-api` | HTTP `XxxApi`、Command、Query、VO、Enum、错误码 | Handler、Provider、数据库和 Spring 装配 |
| `resource-support` | Resource 专属 common：Provider、Handler、声明模型、Builder、Loader、纯目标执行器 | Controller、Feign、Entity、Mapper、Repository、Flyway、JDBC、自动配置 |
| `resource-core` | 注册中心同步编排、锁、注册表、日志、数据库 | HTTP Controller 和远程适配器 |
| `resource-starter` | 本地注册中心装配和管理 API | 目标模块业务逻辑 |
| `resource-sync-starter` | 声明扫描、本地同步或远程上报 runner、目标端 Controller 和默认 Executor 装配 | 注册中心持久化和目标业务表逻辑 |
| `resource-starter-remote` | 注册上报客户端、动态目标客户端 | Controller、Entity、Mapper、Flyway、目标业务表逻辑 |

非 Resource 模块只有在提供 `ResourceProvider`、实现 `ResourceHandler` 或构造声明时才依赖 `resource-support`。普通模块不依赖 Resource；最终应用根据部署方式依赖 starter。

## 4. 目标端执行设计

`resource-support` 增加纯 Java `ResourceTargetExecutor` 端口及默认实现：

- 按 Spring 顺序从当前应用的 Handler 集合中解析匹配的 `resourceType`。
- `upsertBatch` 根据 `requiresCompleteBatch()` 选择变更集合或完整集合。
- `disable/delete` 强制单声明输入。
- Handler 缺失、输入非法和结果缺失使用统一 Resource 业务错误。
- 不访问数据库；实际数据库写入仅由目标模块自己的 Handler 完成。

`resource-sync-starter` 增加：

- `ResourceTargetController implements ResourceTargetApi`，只负责 HTTP 协议转换并调用 `ResourceTargetExecutor`。
- 自动配置以 `@Bean + @ConditionalOnMissingBean` 注册默认执行器和 Controller 所需依赖。
- 继续使用 `/resource/targets/upsert-batch|disable|delete`，并继承 API 层 Bean Validation，Controller 不重复约束。

`resource-starter-remote` 保留当前动态 URI `ResourceTargetHttpClient`，不恢复写死服务名的旧 Feign Target Client。

架构门禁同步承认 Controller 可以依赖经过限定的纯 `*Executor` 接口，要求该类型为接口、不得属于 Controller/Mapper/Entity/Feign，并补充正反例测试；这不是按类名豁免，也不使用抑制注解。

## 5. 依赖与发布变化

删除：

- Resource 聚合 POM中的两个 target module。
- 根 dependencyManagement 中两个 target artifact。
- capability app 和 gateway 对 `mango-resource-target-starter` 的依赖。
- target starter 的重复 `module.properties` 和失效 `AutoConfiguration.imports`。
- SpotBugs 过滤中不存在 target 类的条目。

保留：

- 19 个直接消费者对 `mango-resource-support` 的依赖。
- 单体本地 Resource 链路。
- 微服务远程注册、动态目标地址解析和反向目标执行能力。

## 6. 数据流

### 6.1 单体

```text
声明文件/Provider
  -> ResourceSyncRunner
  -> ResourceRegistryService
  -> 本 JVM ResourceHandler
  -> 目标模块数据库
```

### 6.2 微服务

```text
来源服务声明
  -> sync-starter
  -> ResourceDeclarationApi
  -> 注册中心 ResourceRegistryService
  -> RemoteResourceTargetDispatcher
  -> 目标服务 ResourceTargetController
  -> ResourceTargetExecutor
  -> 目标服务 ResourceHandler
  -> 目标模块数据库
```

## 7. 异常与一致性

- Handler 缺失返回统一 `RESOURCE_NOT_FOUND`，不得变成裸 `IllegalStateException`。
- 单声明动作收到零条或多条声明时返回 `RESOURCE_INVALID`。
- 远端空响应、HTTP 失败和业务失败保留明确错误信息。
- 注册中心多实例通过共享 `ILocker` 保证同一批同步只由一个实例执行。
- 注册中心未取得锁时必须返回“本次未完成”，来源服务继续重试；不得将跳过处理包装成成功。
- 来源服务对远程失败和未完成结果周期重试，首次完整成功后停止，使跨服务父资源依赖在乱序启动时最终收敛。
- 重复投递由 Resource hash/version/sync-mode 和目标 Handler 幂等共同保证。
- Handler 在声明租户上下文执行，结束后恢复调用方上下文；禁止忽略租户拦截器。
- 任一批次部分失败时记录失败对象和目标服务，不返回固定成功。

## 8. 验证矩阵

### 8.1 静态与制品

- `git ls-files` 验证所有实现、测试和自动配置类真实进入最终 Git 树。
- 自动配置契约测试逐项加载 `AutoConfiguration.imports` 中的类。
- Resource 六模块定向 `mvn verify`，架构、PMD、Checkstyle、SpotBugs、MangoCheck 无新增问题。
- 所有直接消费者编译；最终 JAR 检查不包含 target 空模块和失效自动配置。

### 8.2 单体单节点

- 全新 MySQL 启动。
- 正式与 demo 声明隔离。
- Provider/文件声明采集、Handler upsert/disable/delete、重复同步和强制同步。
- 管理 API 的成功、参数错误、权限和租户行为。
- Resource 当前没有独立产品菜单或页面，专属 UI 验收不适用；通用浏览器 shell/API 用例只作为客户端链路补充证据。

### 8.3 单体多节点

- 两个应用实例共享数据库和真实 KV 锁。
- 同时启动时只有一个实例执行同步，另一个明确跳过。
- 锁释放、TTL、失败恢复和再次同步可验证。
- 目标表、registry 和日志不产生重复数据。

### 8.4 微服务单节点

- 启动来源服务、Resource 注册中心和目标服务三个真实 Spring 应用入口。
- 验证声明远程上报、动态目标解析、目标 Controller、Executor、Handler 和目标数据库完整参与。
- 覆盖 upsertBatch、disable、delete、Handler 缺失、非法参数、远端业务失败。

### 8.5 微服务多节点

- 注册中心至少两个实例共享数据库和 KV 锁。
- 目标服务至少两个实例注册到模块解析/路由能力。
- 验证并发同步、重复投递、实例切换、单实例不可用和恢复后的幂等结果。
- 核对 Nacos 健康实例数、registry 总数、资源 ID/业务键重复数及 CREATE/SKIP 日志；停止一个 Resource 节点后再次启动来源节点验证故障切换。
- 不以 Mock Handler、Mock 数据库或固定 HTTP 成功响应代替主链路。

## 9. 完成标准

- 最终 Git 树只包含六个 Resource 子模块。
- 单体和微服务的单节点、多节点验证全部通过。
- HTTP、权限、租户、同步模式和数据库副作用与治理前保持一致。
- Resource 当前没有独立产品页面，UI/E2E 明确标记不适用；不以 APIRequestContext 冒充页面验收。
- 验收证据来自最终 commit 的干净工作区，不记录密码、token 或密钥。
- 任一拓扑未执行或失败时，模块保持未完成状态。
