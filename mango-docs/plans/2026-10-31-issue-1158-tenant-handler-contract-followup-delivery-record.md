# 标准交付记录

## 1. 元数据

- 任务 ID：Issue #1158 后续复验
- 交付模式：STANDARD
- 需求影响：L2 - Maven/docs 1.1.4 的历史 tenantless Resource Registry 行仍可能进入未声明租户执行字段的 Handler，阻断已有业务库 Bootstrap
- 方案风险：L2 - 修正多个租户型 ResourceHandler 的公开执行租户契约，并保持有效租户清理及其它异常的 fail-closed 语义
- 最终风险：L2
- 工作区决策：CREATE，`/Users/hardy/Work/mango-resource-tenant-handlers`，`fix/resource-registry-legacy-tenant-handlers`
- 启用措施：M01、M08、M09、M10、M11、M14
- 同行评审选择：`AGENT_ONLY`；用户未要求外部同行评审，未伪造外部评审结论

## 2. 目标与范围

- 目标：使所有声明租户执行字段的租户型 Handler 在 Resource Registry 调用前参与 tenantless 历史行保护，解决 `AUTH_SUBJECT_ROLE` 仍阻断 Bootstrap 的问题。
- 成功条件：`AUTH_SUBJECT_ROLE`、`AUTH_ROLE`、`AUTH_ROLE_DATA_SCOPE`、日历定义和日历年度 Handler 声明 `tenantId` 为执行租户字段；新的有效租户声明将显式 `tenantId` 保存到 Registry；历史 Registry 行缺少 `tenant_id` 时只告警跳过、不调用 Handler、不修改 Registry，Bootstrap 可以继续；有效租户行仍使用原租户上下文；其它清理失败仍 fail-closed。
- 处理范围：租户型 ResourceHandler 执行租户契约、Resource/Authorization README、租户排障 FAQ、授权 Handler 集成回归测试。
- 不处理范围：不猜测或填充历史租户、不修改业务数据库、不关闭 `fail-open`、不修改 Resource Registry 表结构、不自动迁移历史数据、不执行真实 `baohan_test` 写入。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AC-1158-004 | Resource Bootstrap FINALIZE | `AUTH_SUBJECT_ROLE` 历史行 `tenant_id IS NULL` | Registry 根据 Handler 的执行租户字段在调用前跳过 | 不调用授权 Handler，不修改 Registry，不猜测租户 | 资源行保持 `ACTIVE`，FINALIZE receipt 为 `FINALIZED` |
| AC-1158-005 | Resource Handler 扩展契约 | 租户型 Handler | 实现 `executionTenantField()` 返回 `tenantId` | 未声明租户范围的 Handler 不得被误判为全局 Handler | 授权和日历租户型 Handler 契约测试通过 |
| AC-1158-006 | 有效租户及其它清理路径 | Registry 行有有效 `tenant_id` 或清理发生真实异常 | 保持原租户上下文和 fail-closed 语义 | 不因兼容分支吞掉真实故障 | 既有 Resource Registry 回归和模块质量检查通过 |

## 4. 技术决定

### 4.1 三视角复核

#### 事实与用户视角

- 来源：Issue #1158 复验评论、1.1.4 升级指南、Baohan System 隔离库日志摘要。
- 结论：`IDENTITY_USER` 已跳过，但 `AUTH_SUBJECT_ROLE` 因未声明执行租户字段仍进入 Handler 并因 `tenantId` 缺失失败。
- 反对意见：不能把空租户填成默认租户或修改共享库；不能扩大为全局 fail-open。
- 输入：继续保持历史行原状并补齐 Handler 契约。

#### 技术与风险视角

- 来源：`ResourceRegistryService.prepareTenantScopedDeclaration`、`ResourceHandler.executionTenantField`、授权和日历 Handler 的 `tenantId` 字段契约。
- 结论：通过各租户型 Handler 显式声明 `executionTenantField() = tenantId`，复用 Resource Registry 现有安全跳过逻辑；Registry 只持久化声明中显式提供的租户字段，不在 Registry 中猜测资源类型或租户。
- 反对意见：仅修改 `AuthSubjectRoleResourceHandler` 会留下其它同类 Handler 的相同缺口，因此同步修正已确认的租户型 Handler。
- 输入：为授权和日历 Handler 增加契约声明，并保留 Resource Core 的统一保护分支。

#### 验证与交付视角

- 来源：Resource Registry 集成测试、`AuthSubjectRoleResourceHandlerIntegrationTest`、模块 README 和升级排障文档。
- 结论：需要真实 Spring/MyBatis/H2 Handler 测试和 Resource Registry 集成测试；不执行共享业务库写入。
- 未解决项：尚未连接真实 `baohan_test`；发布 1.1.5 或其它修复版本需要独立发布授权。
- 输入：记录版本、隔离库和未验证边界，避免把本地 Handler 契约测试冒充真实业务库升级验收。

### 4.2 决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| DEC-1158-005 | AC-1158-004、AC-1158-005 | `AUTH_ROLE`、`AUTH_ROLE_DATA_SCOPE`、`AUTH_SUBJECT_ROLE`、日历定义和年度 Handler 显式返回 `tenantId` 执行字段 | 授权与日历 Resource Handler | 恢复缺少执行租户字段的方法，重新发布兼容版本 |
| DEC-1158-006 | AC-1158-004、AC-1158-006 | 复用 Resource Registry 现有缺失租户跳过分支；新声明仅保存显式 `tenantId`，不修改历史 tenantless 行 | `ResourceRegistryService.prepareTenantScopedDeclaration`、`ResourceRegistryRepository` | 回退本次 Handler 契约提交 |
| DEC-1158-007 | 全部 | 文档明确 Handler 契约、`AUTH_SUBJECT_ROLE` 排障和不猜租户边界 | Resource/Authorization README、租户 FAQ | 回退文档提交 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| IMP-1158-005 | DEC-1158-005 | 1 | 授权和日历 ResourceHandler | 租户型 Handler 都声明 `executionTenantField()` |
| IMP-1158-006 | DEC-1158-005、DEC-1158-006 | 2 | `ResourceRegistryRepository`、授权 Handler 集成测试、Resource Registry 集成测试 | 显式租户持久化、契约和 tenantless FINALIZE 回归通过 |
| IMP-1158-007 | DEC-1158-007 | 3 | Resource README、Authorization README、租户 FAQ | 文档说明当前行为和升级排障边界 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AC-1158-004、AC-1158-005 | M11 Handler 集成测试与 Resource Registry 集成测试 | `mvn -f mango/pom.xml -pl mango-platform/mango-resource/mango-resource-core -am -Dtest=ResourceRegistrySyncServiceIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test`；授权模块 `verify` | 通过：Resource Registry 64/64；授权模块 90/90；日历模块 9/9，均无失败 | Surefire 输出与 tenantless FINALIZE、显式租户持久化断言 |
| AC-1158-006 | M09 模块质量和静态验证 | 受影响 Maven 模块 `verify`、`git diff --check` | 通过：Resource Core 定向回归、Authorization Starter、Calendar Starter；diff check 通过 | Maven 输出 |
| 全部 | M08 能力说明 | README、业务指南和能力文档检查 | 通过：模块 README、源码事实、业务指南、能力文档 checker 全部通过 | checker 输出 |
| 全部 | M10 测试质量 | 测试质量与 Mock 审计 | 通过：Test quality 1 file；changed-only Mock audit block=0/warn=0 | checker 输出 |

## 7. 例外与剩余风险

- 未执行真实 `baohan_test` 连接和数据修改；本任务不授权修改共享测试库。
- 1.1.4 已发布制品不能被覆盖；修复需要另行发布新 Maven/docs 版本并由业务项目升级验证。
- 历史 tenantless Registry 行仍保持原状态，后续需要业务 Owner 批准并提供可审计的租户归属迁移方案。
