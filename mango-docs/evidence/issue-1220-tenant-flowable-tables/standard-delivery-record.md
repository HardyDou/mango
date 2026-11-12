# 标准交付记录

## 1. 元数据

- 任务 ID：issue-1220-tenant-flowable-tables
- 关联 Issue：[Mango #1220](https://github.com/HardyDou/mango/issues/1220)（P1，`workflow pageByBusinessKey` 在租户插件下 500）
- 交付模式：STANDARD
- 需求影响：L2 - 影响所有启用默认租户插件的业务应用对 `WorkflowTaskRuntimeApi.pageByBusinessKey` 的调用；查询整体 500，业务侧只能复制平台默认排除清单绕行。
- 方案风险：L2 - 改动共享 `mango-infra-persistence-starter` 的租户拦截器边界，失败后果是跨租户过滤扩大或失效；影响面可限定在第三方表前缀。
- 最终风险：L2
- 工作区决策：CREATE - `/Users/hardy/Work/mango-issue-1220`，分支 `fix/issue-1220-tenant-flowable-tables`，`mango workspace init` slot 44，基线 `origin/main` = `bc804f75b`。

## 2. 目标与范围

- 目标：启用默认租户插件（`mango.persistence.mybatis-plus.tenant.enabled=true`，不覆盖 `excluded-tables`）时，`WorkflowTaskHistoryMapper` 对 Flowable 引擎表的查询不再追加不存在的 `tenant_id` 条件。
- 成功条件：`countTasks`、`selectTasks`、`selectCandidateLinks` 在租户插件开启时正常返回；租户隔离仍由 `workflow_business_apply.tenant_id` 收敛；业务应用不再需要复制平台默认排除清单。
- 处理范围：`mango-infra-persistence-starter` 的第三方表边界与租户拦截器、`mango-workflow-core` 回归测试、相关 README、业务接入指南、历史变更索引、`backend/07-persistence.md` 与 `mango-pmo/rules/index.json`。
- 不处理范围：Flowable 表结构、`WorkflowTaskHistoryMapper.xml` 的既有 SQL 口径、`todo()`/`done()` 的引擎 API 路径、`pageByBusinessKey` 的契约文档补充（`businessKeys` 已是 `@NotEmpty`）、Maven 版本与发布。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| SR-001 | 业务应用调用 `WorkflowTaskRuntimeApi.pageByBusinessKey` | 默认租户配置（租户插件开启、不覆盖 `excluded-tables`），存在业务申请与引擎任务 | 返回按节点展开的任务行 | 500 或 `Unknown column 't.tenant_id'` | 集成测试在租户插件开启时 `countTasks`/`selectTasks` 均返回期望任务 |
| SR-002 | `scope=SELF` 查询 | 引擎存在 `ACT_HI_IDENTITYLINK` 候选人记录 | 候选用户/候选组命中时可返回任务 | `Unknown column 'il.tenant_id'` | 自范围查询返回候选命中的任务 |
| SR-003 | 候选身份增强 | `selectCandidateLinks` 批量传入 taskId | 合并 `ACT_HI_IDENTITYLINK` 与 `ACT_RU_IDENTITYLINK` 结果 | `Unknown column 'tenant_id'` | 返回两张引擎表的候选关联，字段值正确 |
| SR-004 | 跨租户数据 | 同时存在租户 1、租户 2 的申请单 | 当前租户只看到本租户申请的任务 | 注入其它租户数据 | 租户 1 上下文查 `RC1`、`RC2` 只返回 `RC1` 的任务 |
| SR-005 | 租户拦截器行为 | 任意表名传入 `ignoreTable` | `act_*`、`flw_*` 跳过，业务表继续注入 | 业务表被跳过导致跨租户 | 单元测试断言 4 类引擎表忽略、`workflow_business_apply` 不忽略 |
| SR-006 | 业务应用配置负担 | 业务仓已追加 `ACT_*` 到 `excluded-tables` | 平台默认配置即可工作，业务可删除该绕行 | 仍需复制平台默认清单 | 平台默认清单不变即可通过 SR-001 至 SR-004 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | SR-001、SR-002、SR-003、SR-005 | 新增包内私有边界 `ThirdPartyTables.isThirdParty(String)`，以 `act_` / `flw_` 前缀标识 Flowable 引擎表；`TenantLineHandler.ignoreTable` 命中即跳过 | `mango-infra-persistence-starter` | 删除该前缀分支调用 |
| TD-002 | SR-005 | `SchemaValidationRunner` 改用同一 `ThirdPartyTables` 边界，删除本地重复前缀集合，保证“结构校验跳过”和“租户拦截跳过”共用一条事实 | `SchemaValidationRunner` | 恢复本地常量 |
| TD-003 | SR-001、SR-004 | 不修改 `WorkflowTaskHistoryMapper.xml`：租户收敛继续由 `workflow_business_apply.tenant_id` 与 `a.process_instance_id = t.PROC_INST_ID_` 完成；不手写 `t.TENANT_ID_` 条件 | `WorkflowTaskHistoryMapper.xml`（不变） | 无需回滚 |
| TD-004 | SR-006 | 不把 `act_*`、`flw_*` 写入默认 `excluded-tables`：避免业务应用整份覆盖清单时丢失平台新增默认例外；前缀边界不接受配置扩大 | `PersistenceProperties`（不变） | 无需回滚 |
| TD-005 | SR-001 | 新增租户插件开启的集成测试 `WorkflowTaskHistoryTenantIntegrationTest`；原 `WorkflowTaskHistoryMapperIntegrationTest` 保持关闭租户插件，继续覆盖纯 SQL 口径 | `mango-workflow-core` 测试 | 删除新增测试类 |
| TD-006 | SR-006 | 同步 README、业务接入指南、历史变更索引和持久化规范，说明前缀边界与业务侧删除绕行配置的动作 | 文档与 `mango-pmo` | 随代码回滚文档 |

### 4.1 决策复核

- 三视角分析（Agent 角色视角）：
  - 事实与用户视角：Issue #1220 来自 `mango-baohan-risk` 的真实 500；本地 H2 复现的错误 SQL 与 Issue 一致，业务侧绕行代价是必须复制平台默认排除清单。
  - 技术与风险视角：`ACT_HI_TASKINST` 只有 `TENANT_ID_`（`V1__init_workflow.sql:845`），平台租户插件按 `excluded-tables` 无条件追加 `tenant_id`；`SchemaValidationRunner` 早已按 `act_`/`flw_` 前缀跳过同一批表，两处本应共用同一事实。选择前缀硬编码而不是默认排除清单，是因为 `excluded-tables` 是业务可整份覆盖的配置，扩默认清单无法消除 Issue 描述的成本。
  - 验证与交付视角：仅改一行前缀判断无法单独证明效果，必须用租户插件开启的集成测试；已用“移除修复后同一测试 4/4 失败”的反证确认测试确实覆盖缺陷。
- 被拒绝的替代方案：（a）默认清单追加 `act_*`/`flw_*`–业务覆盖清单仍会丢失语义且与 Issue 期望的成本消除不一致；（b）在 XML 里加 `t.TENANT_ID_` 条件–引擎租户列与 Mango 租户上下文来源不同，且 `ACT_HI_IDENTITYLINK` 等表无常量租户需求。
- 评审选择：用户未指定外部同行评审，按 `AGENT_ONLY` 记录，保留后续补外部评审的入口。
- 外部评审/AI 自评均不构成本记录中的“人工审批”。

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| I-001 | TD-001 | 1 | `mango/mango-infra/mango-infra-persistence/mango-infra-persistence-starter/src/main/java/io/mango/infra/persistence/starter/ThirdPartyTables.java` | 新增边界类，含 Javadoc 说明引擎表列语义 |
| I-002 | TD-002 | 2 | `.../SchemaValidationRunner.java` | 删除本地 `THIRD_PARTY_TABLE_PREFIXES`，改用 `ThirdPartyTables` |
| I-003 | TD-001 | 3 | `.../PersistenceMybatisPlusAutoConfiguration.java` | `ignoreTable` 对第三方表返回 `true` |
| I-004 | TD-005 | 4 | `.../PersistenceMybatisPlusAutoConfigurationTest.java` | 新增引擎表忽略断言用例 |
| I-005 | TD-005 | 5 | `mango/mango-platform/mango-workflow/mango-workflow-core/src/test/java/io/mango/workflow/core/service/impl/WorkflowTaskHistoryTenantIntegrationTest.java` | 租户插件开启下 4 个用例覆盖 SR-001 至 SR-004 |
| I-006 | TD-006 | 6 | `mango/mango-infra/mango-infra-persistence/README.md`、`mango/mango-platform/mango-workflow/README.md`、`mango-docs/guides/business-integration/workflow-business-approval.md`、`mango-docs/changelog/business-integration/workflow-business-approval.md`、`mango-pmo/rules/backend/07-persistence.md`、`mango-pmo/rules/index.json` | 文档说明前缀边界、业务侧删除绕行配置；rules index 版本 24 → 25 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| SR-001、SR-002、SR-003、SR-004 | M11 集成测试 | `mvn -o -pl :mango-workflow-core verify`（worktree `/Users/hardy/Work/mango-issue-1220`，H2 2.x，租户插件开启） | PASS - `Tests run: 136, Failures: 0, Errors: 0, Skipped: 4`，其中 `WorkflowTaskHistoryTenantIntegrationTest` 4/4 通过 | `mango/mango-platform/mango-workflow/mango-workflow-core/target/surefire-reports/io.mango.workflow.core.service.impl.WorkflowTaskHistoryTenantIntegrationTest.txt` |
| SR-001 | 缺陷反证 | 临时移除 `ignoreTable` 的第三方前缀分支，重新安装 starter 后运行同一测试 | 4/4 ERROR：`Column "t.tenant_id" not found`、`Column "il.tenant_id" not found`，与 Issue SQL 一致 | 本次执行日志；改动已还原，最终代码含修复 |
| SR-005 | M10 单元测试 | `mvn -o -pl :mango-infra-persistence-starter test -Dtest='PersistenceMybatisPlusAutoConfigurationTest,SchemaValidationRunnerTest,PersistencePropertiesTest,TenantIsolationIntegrationTest'` | PASS - 25/25 | `mango-infra-persistence-starter/target/surefire-reports/` |
| SR-005 | 模块回归 | `mvn -o -pl :mango-infra-persistence-starter test -Dtest='!PersistenceFlywayAutoConfigurationTest,!PersistenceFlywayMigrationIntegrationTest,!PersistenceColdBaselineIntegrationTest,!PersistenceColdBaselinePerformanceIntegrationTest'` | PASS - 60/60（排除的 4 个类为基线环境失败，见第 7 节） | 同上 |
| SR-005 | M09 静态门禁 | `mvn -o -pl :mango-infra-persistence-starter verify -DskipTests` | PASS - BUILD SUCCESS（checkstyle、PMD、SpotBugs、`mango:check`） | 本次执行日志 |
| SR-006 | M08 能力说明与 PMO 门禁 | `node mango-pmo/tools/check-capability-docs.mjs --base origin/main --head HEAD`、`audit-module-readmes.mjs`、`audit-readme-source-facts.mjs`、`check-business-guides.mjs`、`check-governance-intent.mjs`、`check-pmo-preflight.mjs` | PASS - 11 个改动文件通过；5 个业务指南、18 项 preflight 策略通过 | 本次执行日志 |

## 7. 例外与剩余风险

- 基线失败（非本次引入）：`PersistenceFlywayAutoConfigurationTest` 与 `PersistenceFlywayMigrationIntegrationTest` 在本机 main worktree（基线 `7868557f9`）同样失败，报 `Mango Flyway classpath migration modules are not fully declared: missingModules=[cold-missing-snapshot]`（26 用例 6 failures / 14 errors）。本次改动不涉及 Flyway 模块发现，已在本任务验证集中排除并登记为未解决历史问题，需要单独 Issue 跟踪环境或测试隔离缺陷。
- 未执行 M12/M13/M16：本次未改 Controller、API 契约或前端，服务入口行为由 M11 集成测试在真实 Spring 装配下覆盖；未启动完整单体应用做 API 级验收。
- 兼容性风险：任何以 `act_`、`flw_` 开头的自研表会被当作引擎表跳过租户拦截；当前仓库无此类表，已在 README 和持久化规范中写明禁止依赖该前缀。
- 业务侧动作未验证：`mango-baohan-risk` 删除 `ACT_*` 绕行配置并在平台升版后回归，属于业务仓后续任务，本次未执行。
