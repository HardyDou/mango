---
decisionId: {{DECISION_ID}}
documentType: decision-review
status: DRAFT
subject: {{DECISION_SUBJECT}}
decisionOwner: {{DECISION_OWNER}}
reviewDate: {{REVIEW_DATE}}
---

# {{DECISION_SUBJECT}} 决策复核

> **先读：** 先看“决定”和“影响”，再看三视角分析、替代方案、验证和评审选择。先询问并记录是否需要外部同行评审；`AGENT_ONLY` 不称为人工评审或人工审批。

## 1. 决定摘要

- **决定：** {{DECISION}}
- **不决定：** {{OUT_OF_SCOPE}}
- **读者下一步：** {{NEXT_ACTION}}
- **依据来源：** {{SOURCE_PATHS_AND_IDS}}
- **状态：** DRAFT / REVIEWED / BLOCKED / APPROVED

## 2. 第一轮：事实与用户视角

| 参与者/角色 | 当前事实 | 用户场景与成功/失败边界 | 不确定项 | 意见 |
|---|---|---|---|---|
| {{FACT_REVIEWER}} | {{CURRENT_FACTS}} | {{USER_SCENARIO}} | {{FACT_UNCERTAINTY}} | {{FACT_OPINION}} |

## 3. 第二轮：技术与风险视角

| 参与者/角色 | 模块/接口/数据边界 | 权限/租户/兼容性风险 | 回退方式 | 意见 |
|---|---|---|---|---|
| {{TECH_REVIEWER}} | {{TECHNICAL_BOUNDARY}} | {{RISK_BOUNDARY}} | {{ROLLBACK}} | {{TECH_OPINION}} |

## 4. 第三轮：验证与交付视角

| 参与者/角色 | 可观察结果 | 验证命令或步骤 | 证据路径 | 意见 |
|---|---|---|---|---|
| {{DELIVERY_REVIEWER}} | {{OBSERVABLE_RESULT}} | {{VALIDATION}} | {{EVIDENCE}} | {{DELIVERY_OPINION}} |

## 5. 方案整理

| 项目 | 内容 |
|---|---|
| 采用方案 | {{SELECTED_OPTION}} |
| 选择依据 | {{DECISION_BASIS}} |
| 被拒绝方案 | {{REJECTED_OPTIONS_AND_REASONS}} |
| 未解决冲突 | {{OPEN_CONFLICTS}} |
| 影响范围 | {{IMPACT}} |
| 剩余风险 | {{RESIDUAL_RISK}} |
| 评审选择 | AGENT_ONLY / EXTERNAL_PEER_REVIEW |
| 人工确认 | {{HUMAN_CONFIRMATION_OWNER_AND_DEADLINE}} |

## 6. 评审选择与记录

- **评审选择：** AGENT_ONLY / EXTERNAL_PEER_REVIEW
- **询问记录：** {{REVIEW_CHOICE_QUESTION_AND_ANSWER}}
- **用户决定人：** {{REVIEW_CHOICE_OWNER}}

| 评审方式 | 评审人/Agent 角色 | 检查范围 | 阻断问题 | 非阻断建议 | 结论 |
|---|---|---|---|---|---|
| {{REVIEW_MODE}} | {{PEER_OR_AGENT_REVIEWER}} | 事实 / 范围 / 风险 / 验证 / 文本 | {{BLOCKERS_OR_NONE}} | {{NON_BLOCKING_SUGGESTIONS}} | PASS / RECORDED / BLOCKED |

## 7. 审批与后续动作

- **审批人：** {{APPROVER}}
- **审批证据：** {{APPROVAL_EVIDENCE}}
- **下一动作：** {{NEXT_ACTION}}
- **责任人：** {{ACTION_OWNER}}
- **完成条件：** {{DONE_CRITERIA}}
