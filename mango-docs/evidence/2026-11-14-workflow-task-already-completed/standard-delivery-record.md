# 标准交付记录

> **结论：** 并发或重复办理同一任务时，平台返回明确的 `TASK_ALREADY_COMPLETED`，不再让失败方落到 Flowable `ACT_FK_IDL_PROCINST` 外键异常；引擎其它异常仍原样抛出。

## 1. 元数据

- 任务 ID：ISSUE-1247-CONCURRENCY
- 交付模式：STANDARD
- 需求影响：L2 - 审批任务并发办理涉及状态和异常边界，失败方会收到错误提示并影响审计排障，影响面限定在任务完成入口
- 方案风险：L2 - 修改共享任务完成路径，增加运行态任务、完成记录和引擎异常三段判定，模块内可回退
- 最终风险：L2
- 工作区决策：CREATE

## 2. 目标与范围

- 目标：完成 Mango Issue #1247 第 2 条，让并发办理同一任务的失败方得到明确业务错误，而不是引擎外键异常。
- 成功条件：任务已被其他审批人完成时返回 `TASK_ALREADY_COMPLETED`；任务从未存在时仍返回 `TASK_NOT_FOUND`；其它引擎异常不被误吞。
- 处理范围：`WorkflowCode`、`completeWithResult` 任务完成路径、集成测试、Workflow README 和业务接入指南。
- 不处理范围：不改变 reject、return、transfer、claim 的并发语义；不引入 Redis/数据库分布式锁；不修改 Flowable 引擎或数据库结构。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| SR-1 | 业务审批完成接口 | 同一任务已被其他审批人完成，运行态任务不存在，`workflow_task_record` 有 COMPLETE 记录 | 返回 `TASK_ALREADY_COMPLETED` | 不返回 `TASK_NOT_FOUND`，不抛 Flowable 外键异常 | 集成测试断言业务码为 3661 |
| SR-2 | 业务审批完成接口 | 运行态任务仍可查询，提交阶段 Flowable 抛并发冲突，且完成记录已落库 | 转换为 `TASK_ALREADY_COMPLETED` | 不把引擎异常直接暴露给业务 | 集成测试断言业务码为 3661 |
| SR-3 | 业务审批完成接口 | 运行态任务仍可查询，提交阶段 Flowable 抛并发冲突，但没有完成记录 | 原样抛出引擎异常 | 不把非“已办理”冲突伪装成 `TASK_ALREADY_COMPLETED` | 集成测试断言原异常抛出 |
| SR-4 | 业务审批完成接口 | 运行态任务不存在且没有完成记录 | 返回 `TASK_NOT_FOUND` | 不返回 `TASK_ALREADY_COMPLETED` | 由 `completableTask` 分支和既有 `TASK_NOT_FOUND` 语义保证 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D-1 | SR-1、SR-4 | `WorkflowCode` 新增 `TASK_ALREADY_COMPLETED(3661)`；运行态任务为空时用 `workflow_task_record` 的 COMPLETE 记录区分已办理与不存在 | `WorkflowCode`、`WorkflowTaskCompletionSupport#requireCompletable` | 删除业务码和分支 |
| D-2 | SR-2、SR-3 | 捕获 Flowable 并发异常和 `DataIntegrityViolationException`，仅在当前读确认 COMPLETE 记录存在时转换业务错误，否则原样抛出 | `WorkflowTaskCompletionSupport#complete`、`WorkflowTaskRuntimeService#completeWithResult` | 恢复直接调用 `taskService.complete` |
| D-3 | SR-2 | 使用 `select ... for update` 当前读确认完成记录，避免 MySQL REPEATABLE READ 快照看不到并发事务刚提交的记录 | `WorkflowTaskCompletionSupport#hasCompletedRecordForUpdate` | 恢复普通查询 |
| D-4 | 兼容性 | 认领动作移入同一异常转换范围，覆盖未指派任务在 claim/complete 阶段的竞态；事务仍由 `@Transactional(rollbackFor = Exception.class)` 统一回滚 | `completeWithResult`、`completeTask` | 恢复独立 claim 调用 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IMPL-1 | D-1 | 1 | `mango/mango-platform/mango-workflow/mango-workflow-api/src/main/java/io/mango/workflow/api/enums/WorkflowCode.java` | 新增 3661 业务码 |
| IMPL-2 | D-1、D-2、D-3、D-4 | 2 | `mango/mango-platform/mango-workflow/mango-workflow-core/src/main/java/io/mango/workflow/core/service/impl/WorkflowTaskRuntimeService.java`、`mango/mango-platform/mango-workflow/mango-workflow-core/src/main/java/io/mango/workflow/core/support/WorkflowTaskCompletionSupport.java` | 完成入口返回明确业务错误 |
| IMPL-3 | SR-1、SR-2、SR-3 | 3 | `mango/mango-platform/mango-workflow/mango-workflow-core/src/test/java/io/mango/workflow/core/service/impl/WorkflowTaskRuntimeServiceImplIntegrationTest.java` | 3 个异常/并发场景测试通过 |
| IMPL-4 | SR-1 | 4 | `mango/mango-platform/mango-workflow/mango-workflow-core/src/test/java/io/mango/workflow/core/service/impl/WorkflowTaskRuntimeServiceIntegrationTest.java` | 真实 Flowable 重复完成测试通过 |
| IMPL-5 | D-1、D-2 | 5 | `mango/mango-platform/mango-workflow/README.md`、`mango-docs/guides/business-integration/workflow-business-approval.md` | 文档说明业务码和失败语义 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| SR-1 | M11 集成测试 | `mvn -pl mango-platform/mango-workflow/mango-workflow-api,mango-platform/mango-workflow/mango-workflow-core -Dtest=WorkflowTaskRuntimeServiceImplIntegrationTest#completeWithResultReturnsAlreadyCompletedWhenRuntimeTaskGoneAndCompletionRecordExists test` | PASS：业务码 3661 | Surefire 报告 |
| SR-2 | M11 集成测试 | 同上，`completeWithResultTranslatesEngineConflictWhenCompletionRecordExists` | PASS：业务码 3661 | Surefire 报告 |
| SR-3 | M11 集成测试 | 同上，`completeWithResultRethrowsEngineConflictWhenCompletionRecordAbsent` | PASS：原异常抛出 | Surefire 报告 |
| SR-1 | M11 真实 Flowable 测试 | `... -Dtest=WorkflowTaskRuntimeServiceIntegrationTest#completeWithResultReturnsAlreadyCompletedForStaleTask test` | PASS：重复完成返回 3661 | Surefire 报告 |
| 全部 | 模块质量门禁 | `mvn -pl mango-platform/mango-workflow/mango-workflow-api,mango-platform/mango-workflow/mango-workflow-core verify` | PASS：182 tests，0 failures，0 errors，4 skipped | Maven `BUILD SUCCESS` |
| 全部 | 架构与 Java 质量门禁 | `mvn -f mango/pom.xml -pl :mango-architecture-verification,mango-platform/mango-workflow/mango-workflow-api,mango-platform/mango-workflow/mango-workflow-core ... verify` | PASS：blocking=0，newIssues=0 | `mango-architecture-report.json`、`mango-static-report.json` |
| 测试资产 | 测试质量与 Mock 审计 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main`、`node mango-pmo/tools/audit-backend-test-mocks.mjs --report-only --changed-only --base origin/main` | PASS：1 file；block=0，warn=0 | 命令输出 |

## 7. 例外与剩余风险

- 未在真实 MySQL 并发双请求下复现原始 `ACT_FK_IDL_PROCINST`；由 Flowable 异常转换测试、完成记录当前读和真实 Flowable 重复完成测试覆盖同一语义。
- reject、return、transfer 的并发冲突不在本次范围，仍按各自现有行为处理。
