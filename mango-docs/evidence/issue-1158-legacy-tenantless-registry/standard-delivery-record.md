# 标准交付记录

## 1. 元数据

- 任务 ID：issue-1158-legacy-tenantless-registry
- 交付模式：STANDARD
- 需求影响：L2 - Issue #1158 是 P0 升级阻断项；历史 `resource_registry` 行缺少 `tenant_id` 时曾使 `fail-open=false` 的合法业务应用无法启动。
- 方案风险：L1 - 不改生产代码（兼容契约已在 Maven 1.1.5 发布），只补真实 MySQL 回归用例、供应链路 DDL 可移植性和模块 README 验证说明。
- 最终风险：L2
- 工作区决策：CREATE - `/Users/hardy/Work/mango-issue-1158`，分支 `fix/issue-1158-legacy-tenantless-regression`（`M01=CREATE`，`mango workspace init` slot 17）。

## 2. 目标与范围

- 目标：用真实 MySQL 增量库和真实单体应用核验 1.1.5 的兼容契约，并把该场景固化为可重复回归用例。
- 成功条件：历史无租户 `IDENTITY_USER`/`AUTH_SUBJECT_ROLE` 行只产生 WARN、不调用 Handler、不被修改，Bootstrap 到 `FINALIZED` 且应用健康；同一用例可在真实 MySQL 上执行。
- 处理范围：`mango-resource-core` 集成测试的数据源可配置化与 DDL 可移植化、两条按真实资源类型命名的历史行用例、模块 README 的 MySQL 回归命令、真实 MySQL 与应用启动证据。
- 不处理范围：修改 `ResourceRegistryService` 的兼容逻辑、改 `tenant_id` 数据、改 Flyway history、调整 `fail-open`、发布 Maven 1.1.6、业务仓数据修复。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| SR-001 | 存量业务库升级启动 | `fail-open=false`，Registry 存在 `tenant_id IS NULL` 的 `IDENTITY_USER` 与 `AUTH_SUBJECT_ROLE` 行，执行 `FINALIZE` 同步 | 只输出 WARN 跳过，Bootstrap 完成，应用达到健康 | 抛 `IllegalStateException` 或 `资源缺少租户范围`，启动失败 | 应用 `/actuator/health` 返回 200，日志无上述异常 |
| SR-002 | Registry 数据 | 同上 | 历史行保持 `ACTIVE`、`tenant_id` 仍为 `NULL`，模块 receipt 进入 `FINALIZED` | 行被删除、被改为其它租户或被标记 `REMOVED` | 查询行状态与 receipt 状态符合预期 |
| SR-003 | 租户型 Handler | 同上 | Handler 的 `disable` 不被调用 | Handler 被调用后抛字段缺失异常 | 用例断言 `disable` 调用记录为空 |
| SR-004 | 回归执行者 | 本地有 MySQL 8.x | 可用环境变量把同一套用例切到真实 MySQL 执行，且拒绝非专用库 | 误在业务库建表或无法切换 | 库名不以 `_resource_it` 结尾时用例失败 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | SR-001、SR-002、SR-003 | 不改生产代码：1.1.5 已让租户型 Handler 声明 `executionTenantField()`，Registry 据此跳过无租户历史行 | 无生产代码改动 | 不适用 |
| TD-002 | SR-004 | `@TestPropertySource` 使用 `${MANGO_REGISTRY_IT_DB_*:默认值}` 占位符，默认仍为 H2(MODE=MySQL)，设置环境变量即切真实 MySQL | `ResourceRegistrySyncServiceIntegrationTest` | 删除占位符恢复固定 H2 URL |
| TD-003 | SR-004 | MySQL 运行前校验库名以 `_resource_it` 结尾，防止在业务库建表 | 同上 | 随 TD-002 回滚 |
| TD-004 | SR-004 | 测试建表 DDL 的 `clob` 改为 `text`，使同一 DDL 在 H2 与 MySQL 8.4 都可执行 | 同上 | 恢复 `clob`（仅 H2 可用） |
| TD-005 | SR-001～SR-003 | 新增 `AUTH_SUBJECT_ROLE`、`IDENTITY_USER` 两个按真实资源类型命名的 Handler fixture：`disable` 在缺少 `tenantId` 时抛 `IllegalStateException`，因此"用例通过"等价于"Handler 未被调用" | 同上 | 删除 fixture 与两条用例 |
| TD-006 | SR-004 | 模块 README 增加真实 MySQL 回归命令和环境变量说明（M08） | `mango/mango-platform/mango-resource/README.md` | 删除该小节 |

### 4.1 决策复核

- 事实与用户视角：Issue #1158 首轮为 `IDENTITY_USER` 阻断，1.1.4 复验后 `AUTH_SUBJECT_ROLE` 仍阻断；1.1.5 发布说明声明两类 Handler 均声明 `tenantId` 执行范围。
- 技术与风险视角：`AuthSubjectRoleResourceHandler`、`IdentityUserResourceHandler` 现均实现 `executionTenantField()`，`ResourceRegistryService.prepareTenantScopedDeclaration` 在 `tenant_id` 为空时只告警返回；全仓 43 个 Handler 中不存在"声明 `tenantId` 必填但未声明执行租户字段"的缺口（脚本扫描 0 命中）。
- 验证与交付视角：真实 MySQL 8.4 + 真实单体应用（`mango dev start backend`，库 `mango_dev_mango_issue_1158_017`）按 Issue 描述的 `SYNC_ALWAYS`/`RESOURCE_MANAGED`/`tenant_id NULL` 行形态复验：应用健康 200，两条历史行只产生 WARN，行保持 `ACTIVE`+`NULL`，异常计数 0。同一场景在 `mango-resource-core` 集成测试中用真实 MySQL 执行通过。
- 评审选择：`AGENT_ONLY`（用户未要求外部同行评审；结论保留再次发起外部评审的入口）。

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| TASK-001 | TD-002、TD-003 | 1 | `ResourceRegistrySyncServiceIntegrationTest` 数据源占位符与专用库校验 | 默认 H2 用例全绿；MySQL 环境变量生效 |
| TASK-002 | TD-004 | 2 | 同文件测试 DDL | `clob` 替换为 `text`，H2 用例全绿 |
| TASK-003 | TD-005 | 3 | 同文件 fixture 与两条历史行用例 | 两条用例在 H2 与 MySQL 均通过 |
| TASK-004 | TD-006 | 4 | `mango/mango-platform/mango-resource/README.md` | 记录 MySQL 回归命令与库名约束 |
| TASK-005 | TD-001 | 5 | 真实应用增量启动验证 | 健康 200、warn-only、行未变更 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| SR-001 | M11 真实应用验证 | `mango dev start backend`；库 `mango_dev_mango_issue_1158_017` 预置两条历史无租户行后清空 Bootstrap 记账与模块 receipt，重启执行 `FINALIZE` | PASS | `real-mysql-incremental-evidence.md`：health 200；`Mango resource module coordination complete: mode=FINALIZE`；两条 `tenant scope is unavailable` WARN；`IllegalStateException` 0 次 |
| SR-002 | M11 数据库回读 | 启动后查询 `resource_registry` | PASS | 两行均 `tenant_id=NULL`、`status=ACTIVE`、`last_sync_time=NULL`；模块 receipt `FINALIZED` |
| SR-003 | M11 集成测试 | `ResourceRegistrySyncServiceIntegrationTest#finalizeSkipsLegacyNullTenantAuthSubjectRoleRowWithoutCallingHandler` 与 `#finalizeSkipsLegacyNullTenantIdentityUserRowWithoutCallingHandler` | PASS | fixture `disable` 在缺 `tenantId` 时抛异常；用例断言 `disabledResourceIds()` 为空 |
| SR-004 | M09 静态验证 + M11 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main`；`mvn -o -pl :mango-resource-core verify`；MySQL 定向执行 3 条用例 | PASS | test quality PASS；97 个模块测试通过；MySQL 定向 3 条用例通过并输出两条 WARN |
| 全部 | M08 能力说明 | 模块 README 新增 17.1 节 | PASS | `mango/mango-platform/mango-resource/README.md` |

## 7. 例外与剩余风险

- 未在业务仓 `mango-baohan-risk` 复跑 Maven 1.1.6 升级（`M16` 未启用）；本仓已用同形态历史数据在真实 MySQL 与真实单体应用上验证。
- MySQL 用例依赖本地 MySQL 8.x 且默认不在 CI 执行（仓库无 MySQL service job），为按需回归入口；日常门禁仍由 H2(MODE=MySQL) 全量用例承担。
- 本次不改生产代码，因此不改变已发布的 1.1.5 行为；若后续 Handler 忘记声明 `executionTenantField()`，H2/MySQL 用例与 `AuthSubjectRoleResourceHandlerIntegrationTest#exposesTenantExecutionScopeForLegacyRegistryCleanup` 会阻断。
- 未发布新版本，`CHANGELOG.md` 与版本号由发布流程负责。
