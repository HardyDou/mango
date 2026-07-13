---
documentId: {{DOCUMENT_ID}}
documentType: implementation-plan
pmoVersion: {{PMO_VERSION}}
schemaRevision: 1
riskLevel: {{RISK_LEVEL}}
riskAssessmentEvidence: {{RISK_ASSESSMENT_EVIDENCE}}
status: DRAFT
action: WRITE
owner: {{OWNER}}
approver: {{APPROVER}}
approvalEvidence: {{APPROVAL_EVIDENCE}}
upstreamDocumentId: {{TDD_DOCUMENT_ID}}
upstreamDocumentHash: {{TDD_SHA256}}
---

# {{REQUIREMENT_NAME}} 实施计划

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

## 5. 数据、升级、发布与回滚步骤

| 发布步骤ID | 技术设计ID | 环境 | 前置检查 | 动作 | 顺序 | 数据备份或回填 | 兼容窗口 | 验证 | 失败停止条件 | 回滚或补偿 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| {{REL_ID}} | {{DB_IMP_OR_DEC_ID}} | {{ENVIRONMENT}} | {{PRECHECK}} | {{ACTION}} | {{ORDER}} | {{DATA_HANDLING}} | {{COMPATIBILITY_WINDOW}} | {{VALIDATION}} | {{STOP_CONDITION}} | {{ROLLBACK_OR_COMPENSATION}} | {{OWNER}} |

## 6. 文档与能力同步计划

| 文档项ID | 技术设计或交付物ID | 目标文档 | 变化 | 责任人 | 完成条件 | 检查命令 | 不适用依据 |
|---|---|---|---|---|---|---|---|
| {{DOC_ID}} | {{IMP_DEL_OR_TASK_ID}} | {{TARGET_DOCUMENT}} | {{CHANGE}} | {{OWNER}} | {{DONE_CRITERIA}} | {{CHECK_COMMAND}} | {{NOT_APPLICABLE_REASON_OR_NONE}} |

## 7. 风险、阻塞与例外

| 风险ID | 风险等级 | 类型 | 触发条件 | 影响 | 预防 | 应对 | 责任人 | 截止时间 | 状态 | 例外ruleId | 例外批准与到期 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| {{RISK_ID}} | {{RISK_LEVEL}} | {{RISK_BLOCKER_OR_EXCEPTION}} | {{TRIGGER}} | {{IMPACT}} | {{PREVENTION}} | {{RESPONSE}} | {{OWNER}} | {{DUE_DATE}} | CLOSED | NONE | NONE |

## 8. 实施追踪矩阵

| 上游设计ID | 交付物ID | 任务ID | 验证ID | 里程碑发布文档或风险项ID | 覆盖说明 |
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
