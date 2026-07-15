# Infra KV 历史债务治理验收证据

## 1. 验收范围

- 模块：`mango-infra-kv-api`、`mango-infra-kv-core`、`mango-infra-kv-starter`，以及集中测试模块 `mango-infra-test`。
- 能力：Memory、Redis、JDBC 三种 KV 实现；固定窗口计数；所有者安全删除；Outbox 并发领取；Redis 自动配置。
- 真实消费入口：资源注册同步使用 JDBC KV 锁，管理接口和 `/#/system/menu` 页面完成 API 与 Chromium 验收。
- 数据：独立新库完成全部 Flyway 迁移；正式资源与 demo 资源按既有开关分离。
- 边界：KV 不提供远程 starter、Feign API 或 Controller，API/Controller/Feign 对称性检查不适用。

## 2. 执行环境

- 前端地址：`http://127.0.0.1:30184`。
- 后端地址：`http://127.0.0.1:18184`，`/actuator/health` 为 `UP`，数据库组件为 MySQL。
- 数据库：独立新库 `mango_dev_mango_infra_kv_debt_184`。
- KV 运行配置：`mango.kv.store.type=jdbc`、`mango.kv.capability.enabled=true`、`mango.kv.capability.locker=true`。
- 外部组件：MySQL 8.4.8、真实 Redis；浏览器为 Playwright Chromium，单 worker。

## 3. 功能验收记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| KV-DEBT-001 | TC-001 | KV 三个 Maven 模块 | 治理前测试基线 | 原有 20 个测试类 | 169/169 通过，failures/errors/skips 均为 0，耗时 39.253s | 后端能力无独立 UI | Maven 构建成功 | Surefire 报告 | PASS |
| KV-DEBT-002 | TC-002 | Redis 固定窗口与 Outbox | 缺陷复现基线 | 固定窗口减计数后再增；24 个并发领取者 | 治理前 5 个定向用例出现 2 个失败：TTL 被续期、单条消息被 24/24 worker 同时领取 | 无独立 UI | 失败符合缺陷预期 | Red 阶段 Surefire 报告 | PASS |
| KV-DEBT-003 | TC-003 | Memory、Redis、JDBC KV | 固定窗口、所有者安全删除和契约一致性 | Memory/H2 JDBC/真实 Redis fixtures | 同步最新 main 后，最终受影响套件 59/59 通过；KV 命名空间完整套件 189/189 通过；failures/errors/skips 均为 0 | 无独立 UI | Maven 构建成功 | `KvApiContractTest`、`KvStoreContractTest`、`RedisRealIntegrationTest`、`JdbcKvStoreIntegrationTest` 等 Surefire 报告 | PASS |
| KV-DEBT-004 | TC-004 | JDBC KV | 真实 MySQL 并发、TTL、固定窗口、写后查询竞态 | 独立 MySQL 测试库 `mango_kv_test_184` | 17/17 通过，failures/errors/skips 均为 0；Outbox 并发领取重复 5 轮，每轮 24 worker；MySQL 驱动已确认 | 无独立 UI | Maven 构建成功 | `JdbcKvStoreIntegrationTest`，测试耗时 3.914s | PASS |
| KV-DEBT-005 | TC-005 | Outbox Store | 并发领取和所有权释放 | 24 个并发 worker、单条待处理消息 | 只有一个 worker 成功领取；仅所有者能够释放 claim；过期 processing 可重领，成功/失败消息不可重领 | 无独立 UI | 定向测试成功 | `RedisRealIntegrationTest` 与 Outbox 测试报告 | PASS |
| KV-DEBT-006 | TC-006 | Redis 自动配置 | 配置项语义 | DAL 配置与兼容属性 | DAL 配置优先；`pool.max-idle` 不再错误映射成毫秒级 idle timeout；兼容属性边界有文档和测试 | 无独立 UI | 定向测试成功 | `KvRedisAutoConfigurationTest` | PASS |
| KV-DEBT-007 | TC-007 | 单体启动与全部 Flyway 模块 | 独立空库形成和真实 JDBC KV provider | `mango_dev_mango_infra_kv_debt_184` | 健康状态 `UP`；形成 222 张表、21 张 Flyway history 表、迁移失败 0；日志显示 `JdbcKvStore` 初始化；最终有 1211 条 ACTIVE 注册资源、1227 条 SUCCESS 同步日志、17 条存活 KV 记录 | 真实前端可登录并进入菜单管理 | 后端数据库健康检查为 MySQL/UP | 启动日志、健康检查、数据库回读 | PASS |
| KV-DEBT-008 | TC-008 | `/resource/registries/page`、同步日志、handler specs；`/#/system/menu` | 资源注册通过真实 KV 锁同步，API 与 Chromium 验收 | `API_RESOURCE` 注册资源 | API 登录、菜单、注册资源、同步日志、handler specs 均为 200；10 条 API_RESOURCE、10 条 SUCCESS 同步记录、53 条 handler specs；新增场景单独 1/1 通过（3.9s） | 菜单管理页面可见；与既有菜单 CRUD 合跑 2/2 通过（10.4s） | console errors、Vue warnings、page errors、>=400 responses、failed requests 五类采集结果均为空数组；未使用路由 mock | `infra-kv-resource-registry.spec.ts`；`chromium-resource-menu.png` | PASS |
| KV-DEBT-009 | TC-009 | 架构与测试资产审计 | 历史债务、低价值测试和 mock 审计 | 提交相对 `origin/main` 的 39 个 changed files | 架构门禁 `dependency=0`、`archunit=0`、`pmd=0`、`blocking=0`；聚合静态门禁 total/new/baseline/toolFailure 均为 0；fresh SpotBugs API/Core/Starter 为 0/0/0；测试质量 8 个改动测试文件 PASS；后端 mock 审计 `block=0`、`warn=0` | 浏览器脚本未使用路由 mock | 所有定向门禁命令退出码均为 0 | 提交后定向 architecture/static、三模块 SpotBugs、`test-quality-check.mjs`、`audit-backend-test-mocks.mjs` | PASS |

## 4. 静态债务处置记录

| 初始分布 | 判定 | 处置 | 最终结果 |
|---|---|---|---|
| API 4：`EI_EXPOSE_REP` 3、`RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT` 1 | 4 个真实问题 | Outbox payload/headers 改为防御性复制，Builder 返回隔离副本；默认 `set` 不再静默忽略写失败 | API SpotBugs 0 |
| Core 21：`EI_EXPOSE_REP2` 17、`NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE` 3、`JLM_JSR166_UTILCONCURRENT_MONITORENTER` 1 | 4 个真实问题、17 个明确工具误报 | JDBC 可空查询改为显式 Optional/空值处理并修复并发删除竞态；Memory 使用 `ConcurrentHashMap.compute`；共享 Spring/线程安全依赖只在具体构造器上使用精确 `EI_EXPOSE_REP2` 规则和逐项 justification | Core SpotBugs 0 |
| Starter 22：`EI_EXPOSE_REP` 11、`EI_EXPOSE_REP2` 11 | 22 个明确工具误报 | 嵌套可变 bean 是 Spring `ConfigurationProperties` 绑定契约；只在对应 11 个字段生成的 getter/setter 上分别标记精确规则并写明绑定理由，无全局、包级或宽泛 suppress | Starter SpotBugs 0 |
| 合计 47 | 8 个真实问题、39 个明确误报 | 真实问题全部改代码；误报限定到具体构造器/具体 getter/setter 且逐项说明原因 | API/Core/Starter 0/0/0 |

## 5. 数据与资源策略验证

| 场景 | 配置 | 结果 | 结论 |
|---|---|---|---|
| 正式资源首次启动 | 默认关闭 demo 资源 | 全量 DDL 和正式资源初始化成功；demo 角色不存在，因此既有菜单 CRUD E2E 按预期返回 403 | Flyway 只负责 DDL，demo 数据没有混入默认初始化 |
| 演示验收启动 | `MANGO_RESOURCE_REGISTRY_DEMO_ENABLED=true` | demo 角色与绑定显式安装，真实菜单 CRUD 和资源注册 E2E 通过 | demo 数据通过独立开关登记，符合资源分离政策 |

## 6. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| `mango-infra-kv` / Resource Registry 消费入口 | `/#/system/menu` | JDBC KV 锁支撑资源注册同步 | 菜单真实 CRUD | 页面渲染、菜单入口和真实管理接口均有业务断言 | `infra-kv-resource-registry.spec.ts`；`chromium-resource-menu.png` | PASS |

## 7. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 非 KV 的全仓模块 | 用户明确要求不重复执行全仓检查，本次只验证 KV 模块和真实消费入口 | 不能用本证据宣称所有 Mango 模块均完成回归 | 各模块按独立历史债务批次验证 | 已确认定向验证 |
| 第三方自定义分布式 `IKvStore` | 默认 `deleteIfValue` 仅为源码兼容，不保证跨进程原子性 | 自定义分布式实现若不覆写，不能用于所有者安全锁释放 | README 和接口 Javadoc 已明确要求原子覆写；内置 Memory、Redis、JDBC 均已覆写并测试 | 已在本模块能力边界内处理 |
| CLI 启动方式 | CLI 会自动触发根 reactor `install`，与本次禁止全仓检查的要求冲突，已立即中止 | 不影响直接使用 Spring Boot plugin 的真实启动与验收结论 | 本批次不再使用 CLI 启动 | 已按用户要求停止全仓动作 |

## 8. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| Mango KV 维护者 | Memory/Redis/JDBC 固定窗口契约、所有者安全删除、Outbox 并发领取、真实 Redis/MySQL 套件和 Resource Registry 浏览器回归 | `mango/mango-infra/mango-infra-kv`；`mango/mango-infra/mango-infra-test/src/test/java/io/mango/infra/kv`；`mango-ui/apps/mango-admin/e2e/specs/infra-kv-resource-registry.spec.ts` | 定向 Maven 测试；外部服务启动后执行 Playwright Chromium 单 worker 用例 | 每次使用独立新库；demo 资源须显式开启；测试 Redis/MySQL 连接由既有测试环境提供 | 任一契约、并发、真实组件、Flyway、资源同步或浏览器业务断言失败均阻断；不得用 mock 替代真实 Redis/MySQL/E2E | DONE |
