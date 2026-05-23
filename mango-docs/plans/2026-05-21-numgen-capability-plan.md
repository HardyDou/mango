# 编号生成中心能力支持计划

更新时间：2026-05-21

## 对标结论

参考模块：`~/work/Zhongshu/beer-brace-2/beer-brace-num-generator`。

已识别能力：

- 按 `genKey` 生成单个编号。
- 按 `genKey + count` 批量生成编号。
- 规则变量支持常量、时间、入参、自增值。
- 规则上下文加载与缓存刷新事件。
- 编号生成历史记录。
- 编号池，支持单条、批量、定时填充、变量参数替换。

Mango 目标：建设 `mango-numgen` 编号生成中心，能力不低于参考模块，并补齐企业系统实际需要的并发安全、部署透明、可观测和可验证性能。

## P0 最小闭环用户故事

业务模块配置一个 `ORDER_NO` 编号规则：`SO{yyyyMMdd}{tenantCode}{SEQ:6}`。业务在单体或微服务部署下都只依赖 `mango-numgen-api`，调用 `nextValue` 或 `batchValue` 后获得全局唯一编号；并发 20 线程每线程生成 500 个编号时无重复，批量生成 100 个编号时返回连续序列。

## 模块边界

新增平台能力域：`mango-platform/mango-numgen`。

子模块：

- `mango-numgen-api`：对外服务接口、请求/响应模型、规则模型、枚举。
- `mango-numgen-core`：规则解析、变量处理、自增段分配、本地号段池、repository 接口与实现、历史记录策略。
- `mango-numgen-starter`：本地 facade/adapter 装配、Controller 暴露、自动装配、`module.properties`。
- `mango-numgen-starter-remote`：远程 facade/adapter 装配与 Feign client，只做 `XxxApi` 远程代理。

P0 不设置 `support` 模块；如 P1 后确需共享工具，`support` 只允许放无状态格式化/序列化工具，禁止放规则解析、号段分配、编号池等核心业务。

依赖方向：

```text
业务模块 -> mango-numgen-api
mango-app -> mango-numgen-starter 或 mango-numgen-starter-remote
mango-numgen-core -> mango-numgen-api + mango-infra-kv-api + mango-infra-persistence
mango-numgen-starter -> mango-numgen-api + mango-numgen-core + mango-infra-web-starter
mango-numgen-starter-remote -> mango-numgen-api + mango-infra-feign-starter
```

调用透明性：

- 业务方只注入 `mango-numgen-api` 中的 `NumgenApi`。
- `starter` 提供本地 `NumgenApi` adapter，委托 core service。
- `starter-remote` 提供远程 `NumgenApi` adapter，委托 Feign client。
- core 不感知本地/远程部署形态，不依赖 starter、starter-remote 或 Controller。
- Feign client 放在 `starter-remote`，服务名来自模块信息解析，路径前缀使用 `/{module-path}`，禁止硬编码真实服务名和 contextPath。

DAL 边界：

- core 内部通过 `INumgenRuleRepository`、`INumgenSequenceRepository`、`INumgenHistoryRepository` 访问存储。
- Mapper/ORM 细节只存在于 core 的持久化实现包，不进入 api、starter、starter-remote。
- 缓存和锁必须通过 `ICache`、`ILocker` 抽象，TTL 全部配置化。

## API 能力

P0 API：

- `nextValue(genKey, params)`：生成单个编号。
- `batchValue(genKey, count, params)`：批量生成编号。
- `validateRule(ruleDraft)`：校验规则和变量引用。

P1 管理 API：

- 规则 CRUD、启停、发布。
- 变量 CRUD、排序、默认值、必填校验。
- 编号历史查询。
- 缓存刷新与规则版本回滚。
- `previewRule(ruleDraft, params)`：预览规则结果，不消耗序列。

P2 运维 API：

- 编号池状态查询。
- 编号池预热、暂停、恢复。
- 规则性能统计。
- 编号冲突诊断。

## 数据模型

核心表：

- `mango_numgen_rule`：规则主表，包含 `gen_key`、名称、状态、版本、表达式、池策略。
- `mango_numgen_variable`：变量定义，包含变量名、类型、格式、默认值、排序。
- `mango_numgen_sequence`：序列运行态，按 `gen_key + scope_key + period_key` 唯一。
- `mango_numgen_history`：生成历史，保存输入摘要、规则版本、结果编号、耗时。

P0 不建 `mango_numgen_pool_snapshot`；运维快照进入 P2。

历史记录策略：

- P0 默认只记录规则版本、结果编号、耗时、错误信息和输入摘要，不保存完整入参。
- 历史写入可配置为同步、异步或关闭；高频场景默认异步。
- 生成结果返回不依赖历史写入成功，历史失败进入日志和指标。

数据库变更使用 Flyway migration，不使用裸 SQL 初始化脚本。

## 规则设计

P0 变量类型：

- `CONST`：常量。
- `TIME`：日期时间格式化，支持时区。
- `PARAM`：调用方入参。
- `SEQ`：自增序列，支持补零、步长、周期重置。

P1+ 变量类型：

- `TENANT`：租户上下文变量。
- `RANDOM`：非强序号业务的随机段。

表达式约束：

- 不直接引入通用脚本执行引擎执行任意表达式。
- 使用受限模板段拼接，变量白名单解析。
- 参数变量必须声明类型和默认值策略。

## 性能设计

P0：

- DB 乐观锁或原子 update 分配序列段，保证集群唯一。
- 本地内存号段缓存，按 `genKey + scopeKey + periodKey` 分段。
- 批量生成直接申请连续号段，避免循环单号 DB 往返。

P1：

- 异步预热编号池，低水位触发补货。
- 编号池大小、低水位、预热批次、最大等待时间全部配置化。
- 支持热点规则按租户/周期拆分锁粒度。

P2：

- 提供 JMH 或集成压测基准。
- 单机 8C16G、JDK 17、MySQL 本地网络、规则缓存预热、20 并发、10 万次生成：本地池命中路径 p95 ≤ 5ms，DB 号段补货路径 p95 ≤ 50ms。

## 比 beer-brace 更有用的硬指标

| 类别 | beer-brace 能力 | Mango 必须交付 | 验证方式 |
|---|---|---|---|
| 并发唯一 | 有编号池优化，但未定义多实例验收 | 20 并发 × 500 单号同 key 无重复；批量 100 连续无重复 | IntegrationTest 统计唯一数、最大最小序列 |
| 部署透明 | local/remote provider 分离 | 业务只依赖 `NumgenApi`，本地/远程行为一致 | 契约测试覆盖成功、参数错误、规则不存在 |
| 高可用审计 | 同步历史风险未收敛 | 历史异步/关闭可配置，不阻塞主链路 | 历史写入失败仍返回编号，指标记录失败 |

## P0 计划

| 优先级 | 任务 | 验收标准 |
|---|---|---|
| P0 | `mango-numgen` 模块骨架 | api/core/starter/starter-remote 依赖图无反向依赖，starter-remote 不依赖 core/support |
| P0 | 编号规则最小实现 | `SO{yyyyMMdd}{tenantCode}{SEQ:6}` 输入 `tenantCode=A1` 可生成 `SO20260521A1000001` |
| P0 | 单号与批量生成 API | 同 key 批量 100 个编号连续且不重复；count ≤ 0 返回参数错误 |
| P0 | DB 序列段分配 | 20 并发 × 500 单号同 key 生成 10000 个唯一编号 |
| P0 | Flyway 表结构 | `gen_key + scope_key + period_key` 唯一索引存在，审计字段符合规范 |
| P0 | Platform IntegrationTest | 覆盖单号、批量、参数缺失、规则不存在、周期切换、并发唯一 |

## P1 计划

| 优先级 | 任务 | 验收标准 |
|---|---|---|
| P1 | 编号池低水位预热 | 池命中路径通过 spy/指标证明不访问 DB；低水位只触发一次补货 |
| P1 | 规则发布/回滚 | 草稿修改不影响已发布规则，回滚后生成结果使用旧版本 |
| P1 | 历史记录与审计 | 可按 genKey、结果编号、时间范围查询；历史写入失败不影响生成 |
| P1 | 缓存刷新事件 | 规则发布后本地/集群缓存按 genKey 精准失效 |
| P1 | 远程调用适配 | starter 与 starter-remote 对成功、参数错误、规则不存在行为一致 |

## P2 计划

| 优先级 | 任务 | 验收标准 |
|---|---|---|
| P2 | 编号池运维视图 | 可查看池容量、命中率、补货失败 |
| P2 | 性能基准 | 输出池命中、DB 分配、批量生成 p95/p99 和吞吐 |
| P2 | 多租户变量覆盖 | 支持按租户覆盖规则或变量默认值 |
| P2 | 冲突诊断工具 | 可定位重复来源和规则版本 |

## 测试验收矩阵

| 类型 | P0 必测 |
|---|---|
| 正常场景 | 单号、批量、不同 genKey 隔离 |
| 参数校验 | genKey 空、count ≤ 0、缺少必填 PARAM |
| 边界值 | count=1、count=1000、周期跨天 |
| 异常场景 | 规则不存在、序列行不存在自动创建、历史写入失败 |
| 并发 | 同 key 单号、多 key、批量、多实例模拟、周期切换瞬间 |
| 远程一致性 | 本地/远程成功、失败、非法参数、异常码一致 |
| 性能 | 预热后池命中、未命中补货、批量生成基准 |

测试放置：platform 模块核心链路以 `mango-numgen-core/src/test/java` IntegrationTest 为主；纯规则解析放 UnitTest；starter/starter-remote 行为一致性放契约测试。P0 不要求 app E2E。

## 不做事项

- P0 不做规则草稿发布/回滚。
- P0 不做池运维视图。
- P0 不做任意脚本执行。
- P0 不做租户级规则覆盖，只支持租户参数参与 scope。
- 不把编号规则写死到业务模块。
- 不让业务模块直接访问编号中心表。
- 不在 `api` 放 Entity、Mapper 或 FeignClient。
