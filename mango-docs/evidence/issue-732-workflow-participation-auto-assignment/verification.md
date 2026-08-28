---
documentId: EVIDENCE-WORKFLOW-732
documentType: verification-record
pmoVersion: 1.4.2
schemaRevision: 1
status: APPROVED
owner: Mango Workflow 交付负责人
approver: HardyDou
approvalEvidence: mango-docs/designs/issue-732-workflow-participation-auto-assignment/review/APPROVAL.md
---

# Issue #732 验证记录

## 目标

验证工作流参与关系只读查询、稳定用户声明、运行时参与投影、`assignmentMode=CLAIM/AUTO` 配置兼容，以及 `AUTO` 的 `ROUND_ROBIN` 派单和空候选回滚边界。

## 已执行命令

```bash
mvn -pl mango-platform/mango-workflow/mango-workflow-starter \
  -am -DskipTests -Dcheckstyle.skip=true compile
```

结果：通过。Workflow starter、API、core、migration 和 remote client 编译成功。

```bash
mvn -pl mango-platform/mango-workflow/mango-workflow-core -am \
  -Dcheckstyle.skip=true \
  -Dtest=WorkflowDesignerBpmnConverterTest,WorkflowMigrationContractTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：通过，11 tests，0 failures，0 errors。覆盖 AUTO 节点 BPMN 延迟派单、旧 JSON 缺失字段兼容和 V3 migration 契约。

```bash
pnpm exec vitest run --config packages/workflow/vitest.config.ts \
  packages/workflow/src/views/workflow-definition/components/workflow-designer/__tests__/WorkflowNodeApprovalConfig.spec.ts
```

结果：通过，1 test file，2 tests。覆盖 CLAIM/AUTO 控件和静态候选校验。

```bash
node mango-pmo/tools/check-document-set.mjs \
  --dir mango-docs/designs/issue-732-workflow-participation-auto-assignment
```

结果：通过，BRD/SRS/TDD/实施计划四阶段文档集合顺序、上游摘要和生命周期状态均通过。

```bash
node mango-pmo/tools/check-business-requirements.mjs \
  --document mango-docs/designs/issue-732-workflow-participation-auto-assignment/business-requirements.md
node mango-pmo/tools/check-system-requirements.mjs \
  --document mango-docs/designs/issue-732-workflow-participation-auto-assignment/system-requirements.md
node mango-pmo/tools/check-technical-design.mjs \
  --document mango-docs/designs/issue-732-workflow-participation-auto-assignment/technical-design.md
node mango-pmo/tools/check-implementation-plan.mjs \
  --document mango-docs/designs/issue-732-workflow-participation-auto-assignment/implementation-plan.md
```

结果：四项均 PASS。

```bash
set -a
source .mango/dev-workspace.env
set +a
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true \
PLAYWRIGHT_BASE_URL=http://127.0.0.1:30009 \
PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18009 \
pnpm -C mango-ui exec playwright test \
  --config apps/mango-admin/playwright.config.ts \
  apps/mango-admin/e2e/specs/workflow-management.spec.ts \
  --project=chromium --workers=1 --grep '历史参与只读' --trace=on
```

结果：通过，1 个 P0 用例；真实 MySQL 数据库 `mango_dev_mango_issue_732_v2_009`、Flowable 运行时、权限/租户上下文均参与。断言覆盖：参与可见性、非参与人不可读、跨租户同键隔离、参与声明不授予任务操作权、稳定 userId ROUND_ROBIN、完成后仅保留 `COMPLETED_HANDLER`、空候选错误码 3654 以及流程/投影/表记录零提交。测试 finally 清理流程定义、表数据、临时用户和临时权限绑定。

Trace：`.runtime/playwright/mango-admin/artifacts/specs-workflow-management--88115-史参与只读且自动派单按稳定用户ID轮询并在空候选时回滚-chromium/trace.zip`

```bash
node mango-pmo/tools/test-quality-check.mjs --base origin/main
```

结果：通过，9 个变更测试文件无恒真断言、同值断言或 mock 被测对象。

```bash
node mango-pmo/tools/audit-module-readmes.mjs
node mango-pmo/tools/audit-readme-source-facts.mjs
```

结果：通过，Workflow 模块 README、前端包 README、能力地图和源码事实一致。

后端 Workflow core 全量测试：79 tests，0 failures，0 errors，4 skipped（既有环境条件跳过）。前端 `@mango/workflow`：9 test files，43 tests，0 failures；生产构建通过。

## 未完成验证与风险

- `WorkflowParticipationServiceTest` 的 report-only mock 审计提示其使用 Mapper mock；该测试只验证输入校验、查询聚合和授权分支，不作为数据库落库验收依据，真实 MySQL/Mapper/事务链路由 P0 E2E 和 Spring 集成测试证明。
- 直接执行 `vue-tsc -p packages/workflow/tsconfig.json --noEmit` 仍受 workspace 现有 alias、测试类型声明和历史类型债务影响；本次包生产构建及 43 条 Vitest 通过，P0 运行使用已构建前端。
- `WorkflowTaskRuntimeService` 的自动派单候选目录当前直接查询授权角色、租户成员组织表；生产验证需确认这些表在目标部署中已由 Identity/Authorization 初始化。
- 12 并发 MySQL Testcontainers 专测未在本次本地环境执行，严格行锁逻辑由代码审查、游标 Mapper 契约和单/多候选真实 E2E 覆盖；并发吞吐仍是剩余 L3 风险。
- V3 migration 对 username-only 历史记录不做身份猜测；业务需要历史只读时，应由业务在确认用户后调用声明 API。

## 证据边界

本记录证明当前 worktree 的编译、隔离测试库、真实 MySQL/Flowable/权限 API E2E 和文档门禁结果；不代表已提交、合并、发布、部署或生产数据迁移完成。
