---
documentId: {{DOCUMENT_ID}}
documentType: business-requirements
pmoVersion: {{PMO_VERSION}}
schemaRevision: 1
riskLevel: {{REQUIREMENT_IMPACT_LEVEL}}
riskAssessmentEvidence: {{REQUIREMENT_IMPACT_EVIDENCE}}
status: DRAFT
action: WRITE
owner: {{OWNER}}
approver: {{APPROVER}}
approvalEvidence: {{APPROVAL_EVIDENCE}}
upstreamDocumentId: NONE
upstreamDocumentHash: NONE
---

# {{REQUIREMENT_NAME}} 业务需求说明书

## 1. 业务背景与问题

| 问题ID | 当前现状 | 业务问题 | 影响对象 | 影响程度 | 事实来源 |
|---|---|---|---|---|---|
| {{BP_ID}} | {{CURRENT_STATE}} | {{BUSINESS_PROBLEM}} | {{AFFECTED_PARTY}} | {{IMPACT}} | {{EVIDENCE}} |

## 2. 业务目标与成功口径

| 目标ID | 来源问题ID | 目标对象 | 当前基线 | 目标值或完成条件 | 统计周期 | 统计口径 |
|---|---|---|---|---|---|---|
| {{BG_ID}} | {{BP_ID}} | {{TARGET_PARTY}} | {{BASELINE}} | {{TARGET}} | {{PERIOD}} | {{MEASUREMENT}} |

## 3. 范围与不处理范围

| 范围ID | 范围类型 | 业务能力或场景 | 适用对象 | 边界说明 | 对目标的影响 |
|---|---|---|---|---|---|
| {{BS_ID}} | 纳入范围 | {{CAPABILITY_OR_SCENARIO}} | {{APPLICABLE_PARTY}} | {{BOUNDARY}} | {{GOAL_IMPACT}} |

## 4. 业务参与者与术语

| 参与者ID | 业务身份 | 业务职责 | 参与场景 | 允许动作 | 禁止动作 | 业务原因 |
|---|---|---|---|---|---|---|
| {{BA_ID}} | {{BUSINESS_ACTOR}} | {{RESPONSIBILITY}} | {{SCENARIO}} | {{ALLOWED_ACTIONS}} | {{FORBIDDEN_ACTIONS}} | {{BUSINESS_REASON}} |

| 术语 | 业务含义 | 主名称 | 必要别名 | 适用范围 |
|---|---|---|---|---|
| {{TERM}} | {{BUSINESS_MEANING}} | {{PRIMARY_NAME}} | {{ALIAS_OR_NONE}} | {{TERM_SCOPE}} |

## 5. 关键业务对象与生命周期

| 对象ID | 对象名称 | 业务含义 | 唯一识别口径 | 归属口径 | 数量金额或有效期边界 |
|---|---|---|---|---|---|
| {{BO_ID}} | {{OBJECT_NAME}} | {{BUSINESS_MEANING}} | {{IDENTITY_RULE}} | {{OWNERSHIP_RULE}} | {{BUSINESS_BOUNDARY}} |

| 对象ID | 业务状态 | 状态含义 | 进入条件 | 退出条件 | 允许动作 | 禁止动作 | 是否可逆 | 是否终态 |
|---|---|---|---|---|---|---|---|---|
| {{BO_ID}} | {{BUSINESS_STATE}} | {{STATE_MEANING}} | {{ENTRY_CONDITION}} | {{EXIT_CONDITION}} | {{ALLOWED_ACTIONS}} | {{FORBIDDEN_ACTIONS}} | {{YES_OR_NO}} | {{YES_OR_NO}} |

## 6. 业务场景与流程

| 流程ID | 父流程ID | 流程名称 | 业务目标 | 参与者ID | 对象ID | 前置条件 | 用户业务动作 | 可观察业务结果 | 异常或终止分支 | 规则ID |
|---|---|---|---|---|---|---|---|---|---|---|
| {{BF_ID}} | NONE | {{FLOW_NAME}} | {{BUSINESS_GOAL}} | {{BA_ID}} | {{BO_ID}} | {{PRECONDITION}} | {{USER_BUSINESS_ACTION}} | {{OBSERVABLE_RESULT}} | {{EXCEPTION_OR_TERMINATION}} | {{BR_ID}} |

## 7. 业务规则

| 规则ID | 规则名称 | 适用参与者ID | 触发条件 | 判断口径 | 允许结果 | 禁止或失败结果 | 业务反馈 | 状态影响 | 优先级 | 例外 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| {{BR_ID}} | {{RULE_NAME}} | {{BA_ID}} | {{TRIGGER}} | {{DECISION_RULE}} | {{ALLOWED_RESULT}} | {{DENIED_RESULT}} | {{BUSINESS_FEEDBACK}} | {{STATE_IMPACT}} | {{PRIORITY}} | {{EXCEPTION_OR_NONE}} |

## 8. 业务约束、风险与待确认问题

| 条目ID | 类型 | 来源 | 影响范围 | 当前处理 | 责任人 | 截止时间 | 状态 | 是否阻断 |
|---|---|---|---|---|---|---|---|---|
| {{BI_ID}} | {{CONSTRAINT_RISK_ASSUMPTION_OR_QUESTION}} | {{SOURCE}} | {{IMPACT_SCOPE}} | {{CURRENT_HANDLING}} | {{OWNER}} | {{DUE_DATE}} | CLOSED | 否 |

## 9. 业务验收标准

| 验收ID | 来源ID | 业务场景 | 前置条件 | 业务动作 | 期望业务结果 | 失败或边界结果 |
|---|---|---|---|---|---|---|
| {{BAC_ID}} | {{BG_BO_BF_OR_BR_ID}} | {{BUSINESS_SCENARIO}} | {{PRECONDITION}} | {{BUSINESS_ACTION}} | {{EXPECTED_BUSINESS_RESULT}} | {{FAILURE_OR_BOUNDARY_RESULT}} |

## 10. 业务追踪矩阵

| 来源ID | 关联ID | 业务验收ID | 覆盖说明 |
|---|---|---|---|
| {{SOURCE_ID}} | {{RELATED_IDS}} | {{BAC_ID}} | {{COVERAGE}} |

## 11. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 业务需求 checker | {{PASS_OR_FAIL}} | {{CHECK_COMMAND_AND_OUTPUT}} |
| 未关闭阻断数量 | {{BLOCKER_COUNT}} | {{BLOCKER_EVIDENCE}} |
| 例外 | {{EXCEPTION_COUNT}} | {{EXCEPTION_EVIDENCE_OR_NONE}} |
| 业务审批 | {{APPROVED_OR_REJECTED}} | {{APPROVAL_EVIDENCE}} |
