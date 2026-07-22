# Issues #620/#621 Resource Registry 可靠性交付记录

## 1. 元数据

- 任务 ID：`ISSUES-620-621-RESOURCE-SYNC-RELIABILITY`
- 交付模式：FULL。涉及公共锁契约、Spring 生命周期、租户与权限基线、并发数据一致性和业务 readiness；Issue 正文承载已确认需求，本记录承载技术决定、实施与验证，不伪造 BRD/SRS/TDD 审批链。
- 需求影响：L3。资源初始化可能持续 24～30 分钟、快速重启被锁阻塞、跨租户岗位同步永久失败。
- 方案风险：L3。修改 KV 公共能力、Resource/Org/System 协作与 CLI 停服语义。
- 最终风险：L3。
- 工作区决策：CREATE，worktree `/Users/hardy/Work/mango-issues-620-621`，分支 `fix/issues-620-621-resource-sync`。
- 适用措施：M01、M08、M09、M10、M11、M14、M16。无 UI，M13 不适用。
- 非降级事实：TENANT、PERMISSION、并发一致性、公共锁契约。
- 用户批准：2026-07-22 明确要求“按照建议执行，无需询问，做好真实收口测试”。不包含 commit、push、PR 或发布授权。

## 2. 事实、目标与范围

- Issue：[#620](https://github.com/HardyDou/mango/issues/620)、[#621](https://github.com/HardyDou/mango/issues/621)。
- #620 直接故障：资源同步仍在 main/ApplicationRunner 执行时 Spring shutdown 已关闭 Druid，JDBC KV finally 无法可靠释放。CLI 的 2 秒 grace 是放大器，不是唯一根因。
- #620 结构缺口：`ResourceRegistryLock` 丢弃 owner；`KvStoreLocker` 使用固定值和无条件删除；无续租、失租检查和关闭屏障。
- #621 直接故障：Handler 显式 `tenant_id=2`，TenantLine 又追加 ambient tenant 1，查询为空后 INSERT tenant 2 撞唯一键。
- 性能事实：1964 条声明的稳定失败轮次最低执行 1964 次 bizKey SELECT、1964 次 resourceId SELECT 和 1963 次 SKIP INSERT，共 5891 条 SQL；111 轮最低约 65 万条。Baohan 空库进一步定位出 API_RESOURCE 约 980 次逐条回读、SYSTEM_AREA 约 524 次逐条回读。
- 目标：owner-safe lease、DataSource 销毁前的关闭屏障、声明 tenant 执行作用域、业务键并发收敛、失败分类、批量快照与准确 readiness。
- 范围：`mango-infra-kv`、`mango-resource`、`mango-org`、必要的 `mango-system`、Mango CLI、能力说明与真实业务回归。
- 不处理：生产发布、数据库表结构变更、业务 UI、没有下游实际校验的完整 fencing 声明。

## 3. 业务消费场景与可观察要求

| ID | 场景 | 前置条件 | 预期行为 | 失败语义 |
|---|---|---|---|---|
| SR-001 | 空库首次启动 | Flyway 完成，Resource 与 Tenant Provisioning 启用 | 资源与租户基线在 deadline 内收敛，完成前 readiness 非 UP | 永久错误明确失败且不连续重放 |
| SR-002 | 温库重启 | registry 已登记，声明未变化 | 不逐条写 SKIP，不重放 target Handler，快速 READY | 批量预读异常按瞬态处理 |
| SR-003 | 同步期间快速停启 | `dev stop` 后立即 start | 旧实例在基础设施销毁前释放自己的 lease，新实例不等 300 秒 | 超时明确报停服失败，异常退出由 TTL 接管 |
| SR-004 | 双实例或旧实例暂停恢复 | lease 过期后新实例接管 | 旧 token 不能续租、释放新 lease 或继续后续副作用 | 失租 attempt 中止 |
| SR-005 | ambient tenant 1、声明 tenant 2 | tenant 2 岗位已由 Provisioner/人工创建 | 查询、更新、禁用、引用解析均在 tenant 2；复用数据库真实 ID | target/业务键不匹配明确冲突 |
| SR-006 | Provisioner 与 Resource 不同先后顺序 | 共享 `(tenantId, postCode)` | 两种顺序和两线程首次创建均收敛为一行、同一 ID | 非目标唯一键冲突重抛 |
| SR-007 | 瞬态数据库/锁故障 | 声明合法 | 轻量抢锁、指数退避+jitter、恢复后 delta 继续 | readiness 保持非 UP并暴露 next retry |
| SR-008 | 同一声明快照永久错误 | 校验、版本、依赖、租户或业务键错误 | 完整同步最多执行一次，修改声明或人工触发后再试 | PERMANENT_FAILED，可审计但不刷屏 |
| SR-009 | 未启用远程 Resource 的部署 | 无同步状态 Bean | 保持既有启动与 readiness 行为 | 不因可选能力缺失阻断 |
| SR-010 | Baohan 升级 | 新 Mango 修复已通过框架验证 | 先升级并对照验证，再删除旧 240 次补偿器，完成空库/温库/停启 | 任一真实链路失败则恢复补偿与旧版本 |

## 4. 架构与技术决定

| ID | 对应要求 | 决定 | 理由与边界 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | SR-003/SR-004 | 新增显式 `ILeaseLocker` 与不可变 lease handle；每次 acquisition 使用唯一 token | 保留旧 `ILocker` 和 `@Locker` 兼容；Resource 缺安全 lease capability 时 fail-fast | Resource 恢复旧依赖，内置新接口不破坏旧消费者 |
| TD-002 | SR-003/SR-004 | 复用原子 `deleteIfValue`，新增存储端原子 `renewIfValue`，覆盖 JDBC/Redis/Memory | 禁止 `get -> set/delete`；JDBC 使用数据库时间；owner 只观测，token 才是所有权 | 回退新增方法和实现，无 schema 回滚 |
| TD-003 | SR-003 | Resource 同步协调器使用 `SmartLifecycle`：拒绝新任务、维持在途 lease、有界等待释放后回调 Spring | 仅延长 CLI 或 `@PreDestroy unlock` 不能修复 DataSource 先关 | 回退生命周期协调，保留 lease owner-safe 语义 |
| TD-004 | SR-003 | CLI grace 配置化，观察整个 PGID，SIGKILL 后再次确认，未退出不删除 PID/报告 stopped | 后端正确性不依赖 CLI；CLI 是协同防线 | 恢复旧 CLI stop，但不影响 lease 安全 |
| TD-005 | SR-005/SR-006 | Resource 建立本地/远程共用的可执行 tenant scope；P1 覆盖 Post、Unit、Reference、Binding | scope 位于事务代理外层并 finally 恢复；声明格式不变 | 回退统一 invoker，保留逐 Handler 修复 |
| TD-006 | SR-005/SR-006 | targetId 命中后校验 tenant/business key；插入竞争只在同键回读成功后收敛；更新 0 行重新判定 | 业务键优先，不能强制人工/Provisioner 数据使用声明 ID；禁止吞所有 DuplicateKey | 回退并发恢复，唯一约束仍保护数据 |
| TD-007 | SR-001/SR-002/SR-007/SR-008 | 声明 snapshot、失败分类与单一协调器；永久错误同 snapshot 一次，瞬态退避；批量预读 registry，unchanged 不写逐条日志 | 锁竞争只轻量重抢；保留人工触发和完成事件 | 可分别回退批量读取、分类器或状态持久化 |
| TD-008 | SR-001/SR-007~009 | Resource Sync 与 Tenant Reconciliation 共同驱动 readiness | liveness 对可恢复同步错误保持 UP；无 Resource 状态时兼容 | 回退 HealthContributor，不改变同步正确性 |

完整 fencing 只有在 target Handler/数据库写路径校验单调 fence 后才成立。本任务实现 token、续租、失租检查与持锁边界，不把未消费的 fence 值作为关闭 #620 的虚假证据。

## 5. 状态、流程与数据边界

同步状态为 `BOOTSTRAPPING -> SYNCING -> RECONCILING_TENANTS -> READY`；瞬态进入 `TRANSIENT_WAIT`，确定性错误进入 `PERMANENT_FAILED`，关闭进入 `SHUTTING_DOWN`。

- lease token 每次获取唯一；heartbeat 约在 TTL 三分之一周期续租；批次和副作用前检查未失租；finally 条件释放。
- 目标 Handler 保持单资源短事务，不把 1964 条资源包成大事务。
- Org 稳定键为 tenantId+postCode/orgCode。targetId 仅作为目标引用或新建优选 ID，不能覆盖业务键。
- 无 migration。`infra_kv_entry` 继续存 key/value/expire；`org_post`、`sys_org` 继续使用既有唯一键和 TenantLine。
- readiness 只暴露状态、计数、时间和脱敏错误，不输出完整 lease token 或声明敏感字段。

## 6. 实施清单

| ID | 对应决定 | 改动路径 | 完成条件 | 状态 |
|---|---|---|---|---|
| IMPL-001 | TD-001/TD-002 | KV api/core/test | 三 Store lease contract 覆盖 wrong-owner、late-unlock、renew、loss | DONE |
| IMPL-002 | TD-003 | Resource core/sync starter | shutdown 后拒绝新 attempt，DataSource 销毁前 in-flight 释放 | DONE；默认等待 25 秒，低于 Boot 单 phase 30 秒预算 |
| IMPL-003 | TD-004 | Mango CLI | leader 退出但 PGID 存活时不误报；强杀后二次确认 | DONE |
| IMPL-004 | TD-005/TD-006 | Resource support/core、Org starter/core | ambient 1/tenant 2、target mismatch、双线程、两种先后顺序通过 | DONE；MySQL RR 竞争恢复使用 `FOR UPDATE` 当前读 |
| IMPL-005 | TD-007 | Resource core/sync starter | warm SQL 目标、SKIP=0、永久 snapshot 不重放 | DONE |
| IMPL-006 | TD-008 | Resource/System/Actuator | 完成前 readiness 非 UP，成功后 2 秒内 UP，无 Resource 兼容 | DONE；修复 Boot 在 `ApplicationReadyEvent` 后覆盖 readiness 的事件时序 |
| IMPL-007 | 全部 | README、能力地图、evidence、Baohan | 文档门禁、模块门禁与真实消费验收全部 PASS | DONE（显式 `MANGO_BACKEND_AUTO_INSTALL=false` 的源快照验收）；Baohan cold p95=54.11 秒、warm p95=20.50 秒 |
| IMPL-008 | TD-004/TD-007 | Mango CLI | 后端重复停启可显式跳过未变化依赖 install；默认行为保持兼容，健康轮询可配置 | DONE；`MANGO_BACKEND_AUTO_INSTALL` 默认 `true`，`waitPollIntervalMs` 默认 500ms |
| IMPL-009 | TD-007 | Authorization/System handlers | API_RESOURCE、SYSTEM_AREA cold 目标回读批量化，不改变租户、保护模式和重复 targetId 语义 | DONE；新增定向测试，Baohan 真实 cold 资源同步降至约 27～30 秒 |

顺序：IMPL-001 与 IMPL-004 先建立正确性；IMPL-002/003 完成停服；随后 IMPL-005/006 完成性能和状态；最后 IMPL-007～009 完成 CLI 与高频 Handler 收口。性能优化不得改变租户、业务键或完成语义。

## 7. 测试设计与验收目标

| ID | 对应要求 | 层级与环境 | 稳定断言 |
|---|---|---|---|
| TC-001 | SR-003/SR-004 | JDBC/Redis/Memory contract | A 过期、B 接管后 A 不删除/续租 B；相同 owner 的旧 handle 不能释放新 handle |
| TC-002 | SR-003 | Spring integration | SIGTERM 在 Handler 中时 DataSource 保持到 in-flight release 完成；shutdown 后无新 attempt |
| TC-003 | SR-003 | Node 真实进程组 | Maven leader 先退出时 CLI 不提前 stopped；SIGKILL 后无组内残留 |
| TC-004 | SR-005/SR-006 | 专属 MySQL 8.4 + 真实 MyBatis TenantLine | 一行、双方同 ID、错误 target 不变、上下文成功/异常后恢复；禁止 Mockito/H2-only 替代 |
| TC-005 | SR-001/SR-002/SR-008 | 1964 等价声明，fresh 5 次、warm 10 次 | warm Registry 核心 SQL <=20，新增逐资源 SKIP=0，永久 snapshot 五分钟内不重放 |
| TC-006 | SR-001/SR-007~009 | Spring/Actuator | 同步/对账完成前 100% 非 UP，完成后 2 秒内 UP；未启用 Resource 兼容 |
| TC-007 | SR-010 | Baohan 隔离 worktree/数据库 | 无旧补偿器时空库命令到 READY p95<=60s、温库<=30s、快速停启不等旧 TTL，资源/角色/权限/业务域/工作流基线收敛 |
| TC-008 | 全部 | 直接修改模块 verify、CLI tests、能力文档检查、diff check | 无新增架构、质量、测试资产和文档问题 |

## 8. 能力说明、升级与回滚

- KV README 说明 lease API、安全保证和自定义 Store 原子能力。
- Resource README 说明状态、失败分类、readiness、shutdown、性能和排障。
- Org README 说明声明 tenant scope、稳定业务键、targetId 与并发语义。
- System/CLI README 说明组合 readiness、停服 grace、PGID 和错误反馈。
- 能力地图登记 #620/#621、业务升级顺序与验收入口。
- Baohan 必须先升级并对照验证 Mango 修复，再删除 `MangoStartupReconciliationInitializer`。失败时恢复旧版本和补偿器；无数据库回填。

## 9. 验证结果

验证日期为 2026-07-22，均未记录数据库密码、token 或 lease token。

| 用例 | 结果 | 环境与真实证据 |
|---|---|---|
| TC-001 | PASS | Memory/JDBC/Redis lease contract 12/12；`ResourceRegistryLockTest` 4/4，覆盖续租失效、旧 handle 释放、新 owner 接管与 close 后拒绝获取。 |
| TC-002 | PASS（有剩余边界） | `ResourceRegistrySyncServiceIntegrationTest` 25/25；Handler 被阻塞后 stop 拒绝新任务，Handler 返回后因 shutdown fail-closed，不再写 registry/log，lease 释放。Mango/Baohan 真实快速停启均由 CLI 同秒结束 PGID。尚未构造真实 ApplicationContext 中单个 Handler 超过 25 秒的现场。 |
| TC-003 | PASS | `node --test mango-ui/packages/mango-cli/tests/process-control.test.mjs` 3/3；Maven leader/子进程组、SIGKILL 后复查、非法 PID 零信号调用均覆盖。 |
| TC-004 | PASS | MySQL 8.4.8 数据库 `mango_dev_mango_issues_620_621_022`；`OrgPostResourceHandlerMySqlIntegrationTest` 5/5，包含 RR 旧快照后 1062 + locking read、Resource 双线程、Provision 双线程、targetId 错指和上下文恢复；H2 + 真实 TenantLine 补充 6/6。 |
| TC-005 | PASS（源快照 + 显式启动策略） | H2 1964 warm iteration 固定 3 次 Registry Mapper 查询、Handler=0、sync/change log 零增长；Baohan 专属 MySQL 空库 fresh×5 为 54.11/51.99/51.42/49.41/49.41 秒，p95=54.11 秒；最终 warm×10 为 17.45～20.50 秒，p95=20.50 秒；永久业务键冲突保持 `PERMANENT_FAILED` 约 5 分 38 秒，永久错误日志=1，sync/change log 仍 2136/2136。 |
| TC-006 | PASS | Baohan `18021` 高频回读依次观察 `OUT_OF_SERVICE/BOOTSTRAPPING`、`OUT_OF_SERVICE/SYNCING`、`OUT_OF_SERVICE/READY+RECONCILING_TENANTS`、`UP/READY+READY`；Boot ACCEPTING 覆盖回归测试通过。Health 暴露状态、失败计数、尝试/失败/下次重试时间和脱敏错误类型。 |
| TC-007 | PASS（显式启动策略；默认锁定 CLI 仍需升级） | Baohan 隔离 worktree `/Users/hardy/Work/Yunxin/baohan-system-mango-620-621-acceptance`，端口 18021。删除 240 次补偿器并消费本地 `1.0.0-SNAPSHOT`；使用 Mango CLI 源码快照、`MANGO_BACKEND_AUTO_INSTALL=false` 和批量 Handler 后，专属库 fresh×5 p95=54.11 秒（2136 资源），warm×10 p95=20.50 秒；资源/角色/权限/业务域/工作流基线收敛，快速停启不等旧 TTL。原 Baohan 锁定 `@mango/cli` 1.0.89 且默认 install 的 cold p95=70.69 秒，需随 CLI 版本升级后才能获得该优化；不能把未发布源快照当成已发布消费结果。 |
| TC-008 | PASS | 直接修改 11 个 Maven 模块 verify 已通过；新增阻断修复后定向 Resource/Org/System 测试通过，Baohan app verify 11 项（4 skipped、0 failure/error），CLI、README、source facts、style 与 diff 门禁通过。 |

真实停启命令使用 Mango CLI：Mango `node mango-ui/packages/mango-cli/src/index.mjs dev start|stop backend`；Baohan 最终优化验收使用当前 Mango CLI 源快照 `node /Users/hardy/Work/mango-issues-620-621/mango-ui/packages/mango-cli/src/index.mjs dev start|stop backend`，并记录显式 `MANGO_BACKEND_AUTO_INSTALL=false`。Baohan 验收 worktree 保持未提交，主工作区和既有治理 worktree 未修改。

## 10. 剩余风险

- 本任务不宣称完整 fencing；长暂停后的严格陈旧写拒绝仍需所有 target Handler 消费单调 fence。
- 自定义 KV 若不提供原子 lease 能力，Resource Registry 将 fail-fast，需要消费者升级实现。
- Baohan 业务项目当前锁定的 `@mango/cli` 1.0.89 尚未包含后端自动 install 开关与 500ms 健康轮询；发布/升级 Mango CLI 后，业务项目需在依赖未变化的重复停启场景显式设置 `MANGO_BACKEND_AUTO_INSTALL=false`，依赖变化时恢复 `true` 或手工 install。
- 本任务不执行 commit、push、PR 或发布；实现与验证完成后停在当前 worktree 交付状态。
