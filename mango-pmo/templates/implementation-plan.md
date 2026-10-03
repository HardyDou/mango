---
documentId: {{DOCUMENT_ID}}
documentType: implementation-plan
pmoVersion: {{PMO_VERSION}}
schemaRevision: 1
riskLevel: {{FINAL_RISK_LEVEL_FROM_TDD}}
riskAssessmentEvidence: {{FINAL_RISK_EVIDENCE_FROM_TDD}}
status: DRAFT
action: WRITE
owner: {{OWNER}}
approver: {{APPROVER}}
approvalEvidence: {{APPROVAL_EVIDENCE}}
upstreamDocumentId: {{TDD_DOCUMENT_ID_OR_NONE}}
upstreamDocumentHash: {{TDD_SHA256_OR_NONE}}
---

# {{REQUIREMENT_NAME}} 实施计划

> **写作与决定门禁：** 遵循 [`Agent 文本输出规范`](../rules/13-agent-text-output.md)。先写顺序、责任和下一步，使用具体交付物和完成标准。实施决定另按 [`决策专家评审规范`](../rules/14-decision-expert-review.md)记录三轮意见、方案整理和同行评审。

> **先读：这份文档要回答什么**
> - **一句话目标：** {{ONE_SENTENCE_GOAL}}
> - **真实场景：** {{CONCRETE_IMPLEMENTATION_SCENARIO}}
> - **关键输入 / 输出：** {{IMPLEMENTATION_INPUT_AND_DELIVERABLES}}
> - **成功与失败：** {{IMPLEMENTATION_SUCCESS_AND_FAILURE_BOUNDARY}}
> - **明确不做：** {{IMPLEMENTATION_OUT_OF_SCOPE}}
> - **阅读顺序：** 先看本段，再看交付物与任务依赖，最后看验证、风险和追踪矩阵。
> - **任务图入口：** `node mango-pmo/tools/render-implementation-plan-graph.mjs --document <path>`

## 0. 使用合同与最小填写方式

填写人是实施负责人或 Dev Lead；输入必须来自已批准的 TDD、系统验收标准和仓库现状。Plan 的下游使用方是开发、QA、Reviewer、发布负责人和交付记录维护人。

| 区域 | 只填写什么 | 来源与填写人 | 下游用途 | 校验方式 |
|---|---|---|---|---|
| Front Matter | 文档身份、最终风险、Owner、上游 TDD ID/hash | PMO 元数据；Plan Owner 填写 | 生命周期 handoff 和审批 | 文档契约 + TDD hash 检查 |
| 先读摘要 | 实施目标、交付物、成功/失败和不做范围 | Plan Owner 从第 1～3 节归纳 | 开发者快速理解本次交付 | 人工复述、范围核对 |
| 第 1～3 节 | 交付物、任务、依赖、顺序和里程碑 | TDD、仓库事实、Owner 估算 | 直接执行开发和并行安排 | 任务图检查；无前置环或孤立任务 |
| 第 4～6 节 | 命令、环境、数据、数据库步骤和文档同步 | QA、DB Owner、文档 Owner | 可重复验证、迁移和收尾 | 命令可执行；证据路径可写入 |
| 第 7～9 节 | 风险、追踪、checker、依赖图和审批 | Plan Owner、Approver | 阻断升级、完成判定和审计 | blocker/exception/覆盖检查 |

**最小合格示例（复制后替换，不要保留示例）：**

- `DEL-001`：完成订单 API 和 migration；完成条件是构建通过、契约测试通过、回滚脚本已演练。
- `TASK-001`：先执行 `TASK-000` 的 migration，再实现 Service；验证入口为 `npm test` 或明确 Maven 命令。
- `VAL-001`：在空库和已有数据两种环境执行；预期无数据丢失，失败时停止发布并保留日志。
- `MS-001`：只有 `VAL-001` 通过后才进入灰度；阻塞超过截止时间升级给 Owner。

## 1. 实施目标、范围与交付物

| 交付物ID | 技术设计ID | 交付物 | 路径或模块 | 完成状态定义 | 验收来源 | 不处理边界 |
|---|---|---|---|---|---|---|
| {{DEL_ID}} | {{TDD_IDS}} | {{DELIVERABLE}} | {{PATH_OR_MODULE}} | {{DEFINITION_OF_DONE}} | {{ACCEPTANCE_SOURCE}} | {{OUT_OF_SCOPE}} |

## 2. 工作分解

| 任务ID | 技术设计ID | 交付物ID | 责任角色 | 路径或模块 | 前置任务 | 具体动作 | 完成标准 | 验证ID | 实施批次 | 状态 |
|---|---|---|---|---|---|---|---|---|---|---|
| {{TASK_ID}} | {{TDD_IDS}} | {{DEL_ID}} | {{RESPONSIBLE_ROLE}} | {{PATH_OR_MODULE}} | NONE | {{ACTION}} | {{DONE_CRITERIA}} | {{VAL_ID}} | {{BATCH}} | PLANNED |

## 3. 顺序、依赖与里程碑

| 里程碑ID | 包含任务ID | 进入条件 | 完成条件 | 依赖 | 可并行任务 | 阻塞升级 | 责任人 |
|---|---|---|---|---|---|---|---|
| {{MS_ID}} | {{TASK_IDS}} | {{ENTRY_CONDITION}} | {{EXIT_CONDITION}} | {{DEPENDENCIES_OR_NONE}} | {{PARALLEL_TASKS_OR_NONE}} | {{ESCALATION}} | {{OWNER}} |

## 4. 验证计划

| 验证ID | 测试或验收ID | 任务ID | 验证层级 | 命令或步骤 | 环境 | 测试数据 | 权限或租户边界 | 预期结果 | 证据路径 | 责任人 | 失败处理 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| {{VAL_ID}} | {{TC_OR_SAC_ID}} | {{TASK_ID}} | {{VALIDATION_LEVEL}} | {{COMMAND_OR_STEPS}} | {{ENVIRONMENT}} | {{TEST_DATA}} | {{SECURITY_BOUNDARY}} | {{EXPECTED_RESULT}} | {{EVIDENCE_PATH}} | {{OWNER}} | {{FAILURE_HANDLING}} |

## 5. 数据库实施步骤

| 数据步骤ID | 技术设计ID | 环境 | 前置检查 | 动作 | 顺序 | 数据备份或回填 | 验证 | 失败停止条件 | 补偿 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|---|

## 6. 已启用说明与资产同步计划

| 文档项ID | 技术设计或交付物ID | 目标文档 | 变化 | 责任人 | 完成条件 | 检查命令 | 不适用依据 |
|---|---|---|---|---|---|---|---|

## 7. 风险、阻塞与例外

| 风险ID | 风险等级 | 类型 | 触发条件 | 影响 | 预防 | 应对 | 责任人 | 截止时间 | 状态 | 例外ruleId | 例外批准与到期 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| {{RISK_ID}} | {{RISK_LEVEL}} | {{RISK_BLOCKER_OR_EXCEPTION}} | {{TRIGGER}} | {{IMPACT}} | {{PREVENTION}} | {{RESPONSE}} | {{OWNER}} | {{DUE_DATE}} | CLOSED | NONE | NONE |

## 8. 实施追踪矩阵

| 上游设计ID | 交付物ID | 任务ID | 验证ID | 里程碑数据文档或风险项ID | 覆盖说明 |
|---|---|---|---|---|---|
| {{TDD_ID}} | {{DEL_ID}} | {{TASK_ID}} | {{VAL_ID}} | {{MS_REL_DOC_OR_RISK_ID}} | {{COVERAGE}} |

## 9. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 实施计划 checker | {{PASS_OR_FAIL}} | {{CHECK_COMMAND_AND_OUTPUT}} |
| 生命周期 handoff | {{PASS_OR_FAIL}} | {{HANDOFF_EVIDENCE}} |
| 依赖图 | {{PASS_OR_FAIL}} | {{DEPENDENCY_EVIDENCE}} |
| 未关闭阻断数量 | {{BLOCKER_COUNT}} | {{BLOCKER_EVIDENCE}} |
| 实施审批 | {{APPROVED_OR_REJECTED}} | {{APPROVAL_EVIDENCE}} |
