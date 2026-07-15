# Infra Event 历史债务治理验收证据

## 1. 验收范围

- 模块：`mango-infra-event-api`、`mango-infra-event-core`、`mango-infra-event-starter`，以及集中测试模块 `mango-infra-test`。
- 能力：进程内事件总线、KV Outbox、Redis Stream 跨进程传输、pending 消息接管、系统事件查询/详情/重投。
- 真实入口：`GET /system/events`、`GET /system/events/detail`、`POST /system/events/reconsume` 和 `/#/system/events`。
- 真实消费者：Workflow、Payment、Notice 使用领域事件 API；本批次保持事件发布/订阅协议与公开 JSON 字段不变。
- 边界：Event 没有独立数据库迁移、Feign 或 `starter-remote`；持久化由 KV provider 提供，接口/远程对称性不适用。

## 2. 治理前基线

| 项目 | 结果 |
|---|---|
| 定向测试 | 原有 13 例中 12 通过、1 失败；失败原因为真实 Redis 测试只清理 Stream，未隔离残留 KV Outbox key |
| 架构债务 | 25：ArchUnit 6、PMD 19 |
| SpotBugs | 16：API 6、Core 6、Starter 4 |
| 功能缺陷 | 系统事件页面、详情和重投未限定 `domain-event` topic；成功事件和其它 topic 可被重投；API 与 Controller 重复定义校验 |
| 契约风险 | payload/headers 暴露可变引用；Redis pending reclaim 的至少一次语义和消费者幂等要求缺少明确测试与文档 |

## 3. 修复结果

| 债务类型 | 修复结果 | 兼容边界 |
|---|---|---|
| API 与模块角色 | 领域事件声明本地能力契约；系统事件服务抽取接口；持久化转换器按职责命名 | `IDomainEventBus`、`IDomainEventPublisher`、`DomainEvent` 和系统事件 HTTP 路径不变 |
| 可变对象 | `DomainEvent` 与 `SystemEventVO` 对 payload/headers 做防御性复制；任意 JSON 由类型化协议对象承载 | 对外 JSON 仍是原有 `payload`、`headers` 对象，不增加包装字段 |
| API/Controller 校验 | 查询参数约束归 API DTO；Controller 不再重复声明同一约束 | 非法请求仍返回相同业务错误语义 |
| 系统事件隔离 | page/detail/reconsume 均只处理 `domain-event`；SUCCESS、其它 topic 和不存在消息拒绝重投 | 失败事件重投后仍回到 PENDING，既有运维入口不变 |
| Redis 语义 | 测试使用唯一命名空间；覆盖同组双消费者、ACK 前崩溃、pending 超时接管和客户端重启 | 交付语义明确为 at-least-once；租约超时后允许重复投递，消费者必须幂等 |
| 静态债务 | 架构、PMD、Checkstyle、SpotBugs 全部定向清零；Spring/线程安全依赖只做精确 SpotBugs 说明 | 无全局或包级 suppress |

## 4. 自动化验证

| 层级 | 入口 | 关键结果 | 结论 |
|---|---|---|---|
| 单元/组件/真实 Redis | `DomainEventContractTest`、`SystemEventServiceIntegrationTest`、`DomainEventOutboxAutoConfigurationTest`、`RedisStreamDomainEventTransportIntegrationTest` | 同步最新 main 后 20/20；failure/error/skip 均为 0；真实 Redis 7 例耗时约 30 秒 | PASS |
| 事件总线契约 | `DomainEventContractTest` | handler 失败聚合但不阻断其它 handler；unsubscribe 和 wildcard；payload/headers 隔离 | PASS |
| 系统事件服务 | `SystemEventServiceIntegrationTest` | 真实 Memory KV + KV Outbox，无 mock；topic 隔离、重投状态、JSON 往返、API-owned executable validation | PASS |
| 自动配置 | `DomainEventOutboxAutoConfigurationTest` | Memory/Redis provider、publisher/dispatcher、失败终态和系统事件暴露 | PASS |
| Redis 并发/恢复 | `RedisStreamDomainEventTransportIntegrationTest` | 同组双消费者租约内不并发重复；ACK 前崩溃后接管；客户端重启后继续投递 | PASS |
| 架构 | Event 三模块 + `mango-architecture-verification` partial full mode | dependency=0、ArchUnit=0、PMD=0、blocking=0 | PASS |
| 静态 | 三模块 Checkstyle/PMD/SpotBugs | Checkstyle 0；API/Core/Starter SpotBugs 0/0/0；tool failure 0 | PASS |
| 测试质量 | `test-quality-check.mjs --base origin/main` | 4 个新增/修改后端测试文件 | PASS |
| Mock 审计 | `audit-backend-test-mocks.mjs --report-only --changed-only --base origin/main` | block=0、warn=0 | PASS |
| 前端构建 | `pnpm --dir mango-ui --filter @mango/system build` | Vite 与类型生成成功 | PASS |

## 5. Fresh MySQL 与真实端到端验收

| 项目 | 结果 |
|---|---|
| 工作区 | slot 196；后端 `18196`；前端 `30196`；独立数据库 `mango_dev_mango_infra_event_debt_196` |
| 启动产物 | 定向安装 Event 与 monolith 依赖后生成 executable JAR；BOOT-INF 中 Event API/Core/Starter JAR SHA-256 与工作区仓库完全一致；运行进程未加载公共 `~/.m2` Event JAR |
| 空库迁移 | 222 张表、21 张 Flyway history 表、失败迁移 0；`infra_kv_entry`、`/system/events` 菜单存在 |
| 演示授权 | 只显式加载 Authorization demo role 声明；管理员得到 `system:event:list/detail/reconsume`，未加载其它模块 demo 数据 |
| 健康检查 | `/actuator/health` HTTP 200，整体与 MySQL 为 `UP` |
| API/UI | Playwright Chromium 单 worker，真实登录、真实接口、真实 MySQL fixture，不使用 route mock；1/1 通过（5.4 秒） |
| 错误采集 | 页面错误面、console error、pageerror、requestfailed、API `>=400` 五类均为空 |
| 数据清理 | fixture message、all index 和 topic pending index全部清理；数据库回读 fixture key 为 0 |
| 截图 | `system-event-list.png`、`system-event-detail.png` 已人工检查，列表、详情、失败原因和重投入口显示正常 |

## 6. 事务与交付语义

Event 不拥有调用方业务事务，也不承诺将任意业务库提交与事件发布自动纳入同一数据库事务。启用 Outbox 后，
`IDomainEventPublisher` 把消息持久化到配置的 KV provider，再由 dispatcher/transport 投递；业务侧仍需在自身事务边界内
选择正确的发布时机。Redis Stream 消费为至少一次，ACK 前进程退出或 pending 租约超时会发生再次投递，订阅者必须按
`eventId` 或业务幂等键实现幂等。

## 7. 未验证项和边界

| 项目 | 原因 | 结论 |
|---|---|---|
| 非 Event 的全仓模块 | 用户明确要求不重复执行全仓检查 | 仅声明 Event 与直接依赖链通过，不用本证据宣称全仓回归 |
| Redis 集群故障切换 | 当前验收使用单机真实 Redis | 已验证消费者重启与 pending 接管；集群故障切换留给部署环境演练 |
| 第三方 Workflow 表告警 | fresh monolith schema validator 输出既有 Flowable 表结构告警 | 未阻断启动、健康、Event API/UI；不归入 Event 修复范围 |

## 8. 业务开发交接

标准回归入口是上述 20 个定向测试、Event 三模块架构/静态门禁、fresh monolith 与系统事件 Chromium 用例。
修改事件 payload/headers、topic、Outbox claim/ACK 或 Redis pending 逻辑时，必须同时通过 JSON 协议、防御性复制、
同组并发、崩溃恢复和真实页面五类断言；不得用 mock KV/Redis 或路由 mock 替代真实集成与端到端验收。
