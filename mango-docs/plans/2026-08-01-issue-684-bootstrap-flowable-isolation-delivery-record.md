# 标准交付记录

任务：Issue 684 Bootstrap 生命周期迁移前实例化 Flowable 修复

## 1. 元数据

- 任务 ID：GitHub Issue #684
- 交付模式：STANDARD
- 需求影响：L2 - 空库 bootstrap 被共享 Resource/Workflow 装配顺序阻断，影响所有正式生命周期消费方
- 方案风险：L2 - 调整共享 ResourceHandler 的实例化时机，但不改变资源协议、迁移内容或 runtime 对外契约
- 最终风险：L2
- 工作区决策：REUSE - `/Users/hardy/Work/mango-issue-684-bootstrap-flowable-isolation` 上的 `fix/issue-684-bootstrap-flowable-isolation`

## 2. 目标与范围

- 目标：保证 `MangoApplication.run(..., "bootstrap", "apply", ...)` 创建 Spring context 时不会因 Resource 自动配置提前实例化 Workflow 或业务 ResourceHandler；Persistence migration 完成后，Resource step 执行目标操作时再按需加载处理器。
- 成功条件：空库 bootstrap 先完成 Workflow migration，再执行 Workflow 资源同步和流程定义发布；后续 runtime 使用同一回执正常启动；业务 starter 无需增加 Flowable 开关、手工建表或 dummy runtime service。
- 处理范围：Resource target executor 的处理器解析时机、非 Web bootstrap 的 MVC 条件装配、Spring 自动配置装配测试、真实业务消费生命周期回归。
- 不处理范围：不关闭 Flowable，不改变 Workflow 资源发布语义，不把流程定义发布迁移到 runtime，不修改数据库 migration，不执行 Maven 制品发布。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| SR-001 | Mango 或业务应用 `bootstrap apply` | 空 MySQL 数据库，包含 Workflow starter 和依赖 Workflow runtime 的业务 starter | 非 Web context 可完成创建；context refresh 不实例化 ResourceHandler 及其 Flowable runtime 依赖，Persistence step 先执行 | migration 前不得查询 `ACT_GE_PROPERTY`，不得因缺少 `TaskService` 或 MVC mapping 导致 context 创建失败 | 真实消费项目空库 bootstrap 成功 |
| SR-002 | Resource target executor | Persistence migration 已完成，首次收到资源目标操作 | 按 Spring 顺序解析 ResourceHandler 并执行资源同步 | handler 缺失或执行失败继续返回既有业务错误 | 自动配置集成测试证明首次操作前后实例化边界 |
| SR-003 | 后续 `runtime` | 使用同一 release receipt 和已完成初始化的数据库 | Flowable 正常初始化，业务 Workflow 定义可用 | 回执或 Workflow 初始化失败时 runtime 明确失败 | 真实消费项目 runtime 启动和 Workflow 定义检查通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | SR-001/SR-002 | `ResourceSyncAutoConfiguration` 向 executor 传递惰性 handler supplier，禁止在 bean 工厂方法中调用 `orderedStream().toList()` | `mango-resource-sync-starter` | 恢复自动配置的即时 handler 列表解析 |
| TD-002 | SR-002 | `DefaultResourceTargetExecutor` 首次目标操作时线程安全地解析、复制并缓存有序 handler 列表；保留现有 Collection 构造器兼容调用方 | `mango-resource-support` | 删除 supplier 构造器并恢复固定列表字段 |
| TD-003 | SR-001/SR-003 | 非 Web context 只装配与 HTTP 请求映射无关的 Web 基础设施；不在 Bootstrap 模块硬编码 Flowable 排除项，保持 Persistence → Resource → receipt 和 runtime 校验回执的现有契约 | Web / Resource / Bootstrap / Workflow 消费链路 | 代码回退即可，无数据恢复动作 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IMP-001 | TD-001/TD-002 | 1 | `mango-resource-support`、`mango-resource-sync-starter` | executor 创建不解析 handler，首次目标操作只解析一次 |
| IMP-002 | TD-003 | 2 | `mango-infra-web-starter`、`src/test/java` | 非 Web bootstrap 不创建 MVC-only handler mapping scanner |
| IMP-003 | TD-001/TD-002/TD-003 | 3 | 三个直接修改模块的 `src/test/java` | 单元和 Spring 自动配置测试覆盖惰性及非 Web 条件边界 |
| IMP-004 | TD-003 | 4 | 真实业务消费项目 | 专用空库依次完成 bootstrap apply 与 runtime 验证 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| SR-001/SR-002 | M10 单元测试、M11 Spring 装配测试 | 定向执行 `DefaultResourceTargetExecutorTest`、`ResourceSyncAutoConfigurationTest`、`ApiResourceProviderAutoConfigurationTest` 与 `WebAutoConfigurationTest` | DONE | 四个模块共 76 项测试通过；覆盖惰性 handler、非 Web MVC 条件和 API provider bootstrap 边界 |
| SR-001/SR-002 | M09/M10/M11 直接修改模块质量门禁 | 对 `mango-resource-support`、`mango-resource-sync-starter`、`mango-authorization-resource-sync-starter`、`mango-infra-web-starter` 分别执行 `mvn verify` | DONE | 四个直接修改 Maven 模块 `verify` 全部成功 |
| SR-001/SR-003 | M11/M16 真实业务消费回归 | 在一次性空库执行正式 `bootstrap apply`，再使用同一回执启动 `runtime` 并检查 Workflow 定义 | DONE | 数据库 `mango_dev_mango_issue_684_bootstrap_flowable_isolation_012`：`mango_bootstrap_control.state=FINALIZED`、`generation=1`、`release_id=issue-684`；Workflow Flyway 生成 30 个 `ACT_%` 表、`ACT_GE_PROPERTY` 13 条；runtime 日志出现 `Mango runtime receipt accepted` 与 `ProcessEngine default created`，eventual API resource reconciliation 741 条成功，`/actuator/health` 返回 HTTP 200/`UP` |
| SR-001/SR-003 | 回执一致性 | 对 bootstrap 与 runtime 使用的 release receipt 计算 fingerprint 并比对 | DONE | 两阶段 fingerprint 均为 `37cafed76c48a59a977664494ded14eed46f09c420e511163110430370abbb90`；bootstrap `build_revision=1.0.0-mango-012`，runtime 接受同一回执 |
| 全部 | M09 测试资产质量 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main` 与 `node mango-pmo/tools/audit-backend-test-mocks.mjs --report-only --changed-only --base origin/main` | DONE | 质量检查结果见本记录更新后的命令输出；`git diff --check` 无 whitespace 错误 |

## 7. 例外与剩余风险

- 真实业务回归已完成，未发现阻断性剩余风险。
- 环境例外：runtime 验证使用 `KK_OFFICE_PLUGIN_ENABLED=false`，因为验证机没有可执行 LibreOffice；该变量只关闭可选文件转换插件，不属于提交配置，也不影响 bootstrap/Workflow/Resource 主链路。
- 独立未处理问题：`mango.dev.json` 中 `--server.port` 参数顺序问题未纳入本任务，未改变本次 bootstrap/runtime 验收结果。
- 曾尝试完整 Reactor 诊断，但 `mango-platform-app` 阶段因本机磁盘空间不足失败；按照后端质量规则，本任务以四个直接修改模块 `mvn verify` 和真实业务回归作为有效证据，不声明全 Reactor 通过。
- 本任务不执行 Maven 制品发布；提交、Push、PR、合并和 Issue 状态回读按用户本次明确授权执行。
