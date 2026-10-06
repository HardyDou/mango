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

> **写作与决定门禁：** 遵循 [`Agent 文本输出规范`](../rules/13-agent-text-output.md)。先写重点，使用具体主语和稳定术语，保留成功/失败边界。需求决定另按 [`决策复核规范`](../rules/14-decision-expert-review.md)记录三视角分析，并询问、记录是否需要外部同行评审。

> **先读：这份文档要回答什么**
> - **一句话目标：** {{ONE_SENTENCE_GOAL}}
> - **真实场景：** {{CONCRETE_BUSINESS_SCENARIO}}
> - **关键输入 / 输出：** {{BUSINESS_INPUT_AND_OBSERVABLE_OUTCOME}}
> - **成功与失败：** {{BUSINESS_SUCCESS_AND_FAILURE_BOUNDARY}}
> - **明确不做：** {{BUSINESS_OUT_OF_SCOPE}}
> - **阅读顺序：** 先看本段，再看业务对象与流程，最后看规则、验收和追踪矩阵。

## 0. 使用合同与最小填写方式

填写人是业务负责人或产品经理；事实来源必须来自访谈纪要、现行流程、数据报表、用户反馈或已批准决定。BRD 的下游使用方是 SRS 编写人、验收设计人和审批人。

| 区域 | 只填写什么 | 来源与填写人 | 下游用途 | 校验方式 |
|---|---|---|---|---|
| Front Matter | 文档身份、风险、责任和审批证据 | PMO 元数据；Owner/Approver 填写 | 生命周期 handoff、审批和 checker | 文档契约 checker |
| 先读摘要 | 一句话目标、真实场景、输入输出、成功/失败和不做范围 | BRD Owner 根据第 1～3 节归纳 | 空白上下文 Agent 先定位重点 | 人工复述：能否说出目标和边界 |
| 第 1～3 节 | 问题、可量化目标、纳入/排除范围 | 访谈、数据、会议决定；PM/业务 Owner | 判断是否值得做、限定 SRS 范围 | BP/BG/BS 引用检查 |
| 第 4～5 节 | 参与者、术语、业务对象和状态 | 现行业务规则、领域 Owner | 统一术语并定义状态边界 | BA/BO 前缀与状态闭环检查 |
| 第 6～7 节 | 可执行流程和业务规则 | 业务 Owner；每行必须有观察结果或失败结果 | 生成 SRS 功能、场景和验收 | BF/BR 引用检查 |
| 第 8～11 节 | 风险、验收、追踪和审批 | Owner、QA、Approver | 形成系统验收入口和审批依据 | checker + 未关闭阻断检查 |

**最小合格示例（复制后替换，不要保留示例）：**

- `BP-001`：客服无法看到订单处理进度；事实来源：用户访谈记录 `INT-12`。
- `BG-001`：处理人可在 3 次点击内看到当前状态；统计口径：验收场景 `BAC-001`。
- `BS-001`：纳入“状态查询”，不处理“历史数据迁移”；边界原因：本期无迁移窗口。
- `BF-001`：处理人打开订单 → 查看状态 → 获得成功或明确失败反馈。
- `BAC-001`：给定已存在订单，执行查看动作，页面显示状态；权限不足时返回禁止原因。

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
