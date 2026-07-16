# Mango Resource 历史债务治理验收证据

## 1. 结论

- 验收日期：2026-07-17
- 工作区：`/Users/hardy/Work/mango-resource-target-removal`
- 分支：`refactor/resource-target-removal`
- 结论：Resource 已从八个发布物收敛为 `api/support/core/starter/sync-starter/starter-remote` 六个子模块。两个没有有效实现的 target 模块已删除，HTTP 路径、JSON 契约、权限、租户和目标 Handler 副作用保持不变。
- 范围：按要求不执行全仓检查；验证 Resource 六模块、必要基础设施和真实单体/微服务组装入口。

## 2. 自动化基线与结果

| 层级 | 改前基线 | 最终结果 |
|---|---:|---:|
| Resource 单元/H2 集成 | 48/48 PASS | 60/60 PASS |
| Resource support | 原有 SPI 测试 | 8/8 PASS，包含纯 Java 目标执行器 |
| Resource core | 原有注册/同步测试 | 40/40 PASS，包含真实 JDBC 锁竞争 |
| Resource sync starter | 无启动重试基线 | 4/4 PASS，覆盖失败、未完成、完成三态 |
| Resource remote starter | 旧远程适配测试 | 8/8 PASS，覆盖服务发现、base path、HMAC |
| 架构规则 | 存在模块边界债务 | 154/154 PASS |
| Internal HMAC/Security | 原始 Header 存在信任边界风险 | Authorization 4/4、Auth 5/5 PASS |

最终定向命令：

```bash
mvn -pl :mango-resource-api,:mango-resource-support,:mango-resource-core,\
:mango-resource-starter,:mango-resource-sync-starter,:mango-resource-starter-remote \
  -am clean verify
```

结果：32 个 Reactor 模块 BUILD SUCCESS，Resource 60 条测试全部通过；Checkstyle、PMD、SpotBugs 和架构门禁通过。未运行全仓检查。

## 3. 单体端到端

### 3.1 单节点

- 使用全新 MySQL 数据库 `mango_resource_six_baseline` 启动真实单体应用。
- demo 显式开启后注册 729 条资源，其中 20 条 `API_RESOURCE`；20 条同步日志均为 `SUCCESS`。
- 发现 57 个 Handler Spec。
- 缺少 `resourceId` 的删除请求返回 HTTP 400；`POST /resource/sync/force` 返回 `data=true`。
- 使用真实 Internal HMAC 调用实际 `SEQUENCE_RULE/Numgen` 目标 Handler 返回 200；空声明请求返回 400，证明 API 接口上的 Bean Validation 被 Controller 正确继承。

### 3.2 多节点

- 两个单体实例共享全新数据库 `mango_resource_multi_baseline` 和真实 KV 锁。
- 并发启动时只有一个实例执行同步，另一个明确跳过；最终 registry 为 1778 条，业务键和资源 ID 重复数均为 0。
- 两个实例随后分别执行强制同步均返回 `true`，数据仍无重复。
- 启动依赖顺序必须以应用健康和资源派生完成为准；端口可访问不代表同步完成。

## 4. 微服务端到端

使用 Nacos、全新 MySQL `mango_resource_micro_final`、真实 JDBC KV/锁和真实 Flyway，依次验证 Resource、Authorization、System 三个应用。

### 4.1 单节点与乱序启动

- Resource 先启动，Authorization 后启动，System 最后启动。
- Authorization 的菜单声明引用 System 所属父资源 `system:permission`。父资源尚未出现时，Authorization 保持健康并周期重试；System 完成声明后，Authorization 自动收敛成功。
- 最终 registry 610 条：System 603、Authorization 7；同步日志 `CREATE/SUCCESS` 610 条。
- Authorization 菜单 27 条：System 22、Authorization 5；跨服务父子关系正确。
- 资源 ID 和 `resourceType + bizKey` 重复数均为 0。

### 4.2 多节点与故障切换

- Resource、Authorization、System 各启动两个健康实例，Nacos 均显示 2 个健康节点。
- 第二批来源节点重放后 registry 仍为 610 条，产生 610 条 `SKIP/SKIPPED`，证明幂等。
- 停止一个 Resource 实例后，再启动第三个 Authorization 实例；请求经服务发现路由到剩余 Resource，新增 7 条 `SKIPPED`，最终 registry 和重复数不变。
- 验证了服务名解析、目标 base path 保留、Internal HMAC、注册中心负载均衡和单节点失效后的继续同步。

## 5. 本次暴露并固定的缺陷

1. target Java 包路径被 `.gitignore` 的 `target/` 规则吞掉，两个 Maven 模块成为空壳。最终删除空模块，并用 `git ls-files` 与自动配置类加载测试确认交付物真实存在。
2. Feign 动态改写目标地址时丢失服务 base path。拦截器现保留原 path。
3. 直接 HTTP 客户端没有参与服务发现且没有 Internal HMAC。现通过 LoadBalancer 解析服务名并添加签名；显式 `host:port` 仍直连。
4. 安全链曾直接信任客户端 Header。现只信任 HMAC Filter 写入的服务端 request attribute，伪造 Header 不能绕过鉴权。
5. 来源服务乱序启动时，一次同步失败会终止启动。现对远程失败和“尚未完成”进行有界周期重试，成功后停止。
6. 注册中心锁竞争曾返回成功，导致来源服务误以为声明已登记。现锁未取得返回 `data=false`，来源服务继续重试，不再静默丢声明。

## 6. UI 与验收边界

- Resource 是后端平台能力，当前没有独立 Resource 产品菜单或页面，因此 Resource 专属浏览器 UI 验收为不适用，不能虚构“Resource 页面通过”。
- 既有 Chromium shell/API 用例 1/1 PASS，只证明真实登录、通用管理界面和 Resource API 调用链可用，不替代上述后端拓扑 E2E。
- `resource-support` 等价于 Resource 域内 common/SPI 包，不包含 Controller、Feign、数据库、Mapper、Repository、Flyway 或自动配置。
- Flyway 只负责 DDL；正式资源与 demo 资源继续由不同目录和开关登记。
