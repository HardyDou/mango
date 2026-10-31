# 标准交付记录

## 1. 元数据

- 任务 ID：Issue #1158
- 交付模式：STANDARD
- 需求影响：L2 - Maven 1.1.3 升级后，历史 Resource Registry 数据可能阻断应用 Bootstrap 启动
- 方案风险：L2 - 改动 Resource FINALIZE 的租户边界和失败语义，必须保留真实租户校验与其它清理失败的 fail-closed 行为
- 最终风险：L2
- 工作区决策：CREATE，`/Users/hardy/Work/mango-resource-tenant-compat`，`fix/resource-registry-legacy-tenant-compat`
- 启用措施：M01、M08、M09、M10、M11、M14
- 同行评审选择：`AGENT_ONLY`；用户未要求外部同行评审，未伪造外部评审结论

## 2. 目标与范围

- 目标：避免历史租户范围缺失的 Resource Registry 行阻断 Maven 1.1.3 的 Bootstrap FINALIZE。
- 成功条件：租户范围缺失时不猜测租户、不调用目标 Handler、不修改 Registry 行，FINALIZE 可以继续完成；真实租户存在时保持原有租户上下文停用行为；其它清理异常仍按 FINALIZE fail-closed 处理。
- 处理范围：`ResourceRegistryService` 缺失资源清理逻辑、Resource Registry 集成回归测试、Resource README 和租户数据排障 FAQ。
- 不处理范围：直接修改业务数据库、为历史行填充特殊租户、关闭全局租户校验、修改 Resource Registry 表结构、Maven 制品发布和业务库部署。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AC-1158-001 | Resource Bootstrap FINALIZE | 缺失的租户范围资源行 `tenant_id IS NULL` | 记录告警并跳过目标 Handler，Registry 行保持原状态，Bootstrap 继续 | 不猜测租户，不执行停用或删除 | FINALIZE 返回成功，receipt 为 `FINALIZED`，资源行仍为 `ACTIVE` |
| AC-1158-002 | Resource Bootstrap FINALIZE | 缺失资源行有有效 `tenant_id` | 使用 Registry 行租户执行目标停用 | 租户上下文无效时失败 | 目标 Handler 收到正确租户，Registry 进入 `REMOVED` |
| AC-1158-003 | Resource Bootstrap FINALIZE | 非租户范围资源缺失 | 按原有全局资源 Handler 处理 | 真实 Handler 失败仍阻断 FINALIZE | 未改变全局资源清理语义 |

## 4. 技术决定

### 4.1 三视角复核

#### 事实与用户视角

- 来源：`ResourceRegistryService`、Issue #1158 描述、Issue #1084 兼容修复和现有 Resource 文档。
- 结论：历史 `IDENTITY_USER` 行的 `tenant_id` 为空时，Mango 无法安全确认目标租户；阻断启动会使升级失败。
- 反对意见：不能把空值写成 `default`、`0` 或 `1`，否则可能跨租户停用用户。
- 输入：保留原行并告警，等待独立数据迁移或人工确认。

#### 技术与风险视角

- 来源：`ResourceRegistryService.disableMissingRow`、`prepareTenantScopedDeclaration`、commit `fa30badd0` 和 PR #1129 对应 commit `c81a3dd32`。
- 结论：仅对“租户范围缺失”这一已知不可安全执行的历史兼容分支跳过；其它 FINALIZE 清理异常仍抛出，避免错误写入成功结果。
- 反对意见：全局关闭 fail-closed 会掩盖数据库、Handler 或远程调用故障，因此不采用。
- 输入：代码分支、集成测试和升级文档必须明确两类失败边界。

#### 验证与交付视角

- 来源：`ResourceRegistrySyncServiceIntegrationTest`、Maven 定向测试、`git diff --check` 和模块文档。
- 结论：需要真实 Spring、MyBatis、H2 Registry 和 Handler 协作测试，不能只测试固定异常消息。
- 未解决项：尚未连接业务远程测试库，不执行生产或测试库数据修复。
- 输入：通过定向 Maven 测试证明 FINALIZE、Registry 状态、receipt 和租户上下文结果。

### 4.2 决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| DEC-1158-001 | AC-1158-001 | 租户范围 Handler 遇到 `tenant_id` 为空时只告警并返回，不执行 Handler，不更新 Registry | `ResourceRegistryService.disableMissingRow` | 恢复 FINALIZE 对该分支抛出异常 |
| DEC-1158-002 | AC-1158-002 | 保留 `prepareTenantScopedDeclaration` 对有效租户的租户上下文注入 | Resource Handler Invoker | 恢复原调用路径 |
| DEC-1158-003 | AC-1158-003 | 其它清理异常继续由 `failOnCleanupError` 控制；本次不改变全局资源 Handler | Resource FINALIZE error path | 仅回退兼容分支 |
| DEC-1158-004 | 全部 | 不向历史 Registry 行回填特殊租户，不修改表结构 | 数据迁移边界 | 由独立批准的数据迁移任务处理 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| IMP-1158-001 | DEC-1158-001、DEC-1158-003 | 1 | `mango-resource-core/.../ResourceRegistryService.java` | 缺失租户范围不再阻断 FINALIZE；其它异常仍保持 fail-closed |
| IMP-1158-002 | DEC-1158-001、DEC-1158-002 | 2 | `ResourceRegistrySyncServiceIntegrationTest.java` | 新增 FINALIZE 历史空租户回归，验证 ACTIVE、FINALIZED 和未调用 Handler |
| IMP-1158-003 | DEC-1158-004 | 3 | `mango-resource/README.md`、租户排障 FAQ | 写明不猜租户、不修改历史行和启动兼容边界 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AC-1158-001、AC-1158-002、AC-1158-003 | M11 Resource Registry 集成测试 | `mvn -f mango/pom.xml -pl mango-platform/mango-resource/mango-resource-core -Dtest=ResourceRegistrySyncServiceIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test` | PASS：64 tests，0 failures，0 errors，0 skipped | Maven Surefire 输出；新测试 `finalizeSkipsLegacyNullTenantResourceWithoutBlockingBootstrap` |
| 全部 | M09 模块质量和静态验证 | `mvn -f mango/pom.xml -pl mango-platform/mango-resource/mango-resource-core verify`；`git diff --check` | PASS：95 tests，0 failures，0 errors，0 skipped；无空白错误 | Maven Verify 输出 |
| 全部 | M08 能力说明 | `node mango-pmo/tools/audit-module-readmes.mjs`；`node mango-pmo/tools/audit-readme-source-facts.mjs`；`node mango-pmo/tools/check-business-guides.mjs` | PASS：README、源码事实和 5 个业务指南检查通过 | checker 输出 |
| 测试资产 | M10 测试质量和替身边界 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main`；`node mango-pmo/tools/audit-backend-test-mocks.mjs --report-only --changed-only --base origin/main` | PASS：测试质量 1 个变更文件；Mock block/warn 为 0 | checker 输出 |

## 7. 例外与剩余风险

- 未执行真实 `baohan_test` 连接和数据修改；本任务不授权修改共享测试库。
- `tenant_id IS NULL` 的历史租户资源仍保持原 Registry 状态，目标数据不会被自动停用；需要业务 Owner 另行批准并提供可证明租户归属的数据迁移方案。
- 非租户资源的全局范围值仍由目标模块 Handler 和目标表契约决定；本次不引入统一特殊租户。
- 本任务未执行 commit、push、PR、Maven 发布或业务部署。
