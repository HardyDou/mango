---
documentId: {{DOCUMENT_ID}}
documentType: system-requirements
pmoVersion: {{PMO_VERSION}}
schemaRevision: 1
riskLevel: {{SYSTEM_IMPACT_LEVEL}}
riskAssessmentEvidence: {{SYSTEM_IMPACT_EVIDENCE}}
status: DRAFT
action: WRITE
owner: {{OWNER}}
approver: {{APPROVER}}
approvalEvidence: {{APPROVAL_EVIDENCE}}
upstreamDocumentId: {{BRD_DOCUMENT_ID_OR_NONE}}
upstreamDocumentHash: {{BRD_SHA256_OR_NONE}}
---

# {{REQUIREMENT_NAME}} 系统需求规格说明书

## 1. 系统范围与上下文

| 上下文ID | 上游ID | 系统责任 | 边界外责任 | 参与方 | 可观察输出 |
|---|---|---|---|---|---|
| {{SC_ID}} | {{BS_BG_OR_BF_ID}} | {{SYSTEM_RESPONSIBILITY}} | {{OUT_OF_BOUNDARY}} | {{PARTICIPANTS}} | {{OBSERVABLE_OUTPUT}} |

## 2. 系统参与者与访问行为

| 系统参与者ID | 上游参与者ID | 使用入口类别 | 可见范围 | 可执行动作 | 禁止动作 | 用户可见原因 |
|---|---|---|---|---|---|---|
| {{SA_ID}} | {{BA_ID}} | {{ENTRY_TYPE}} | {{VISIBLE_SCOPE}} | {{ALLOWED_ACTIONS}} | {{FORBIDDEN_ACTIONS}} | {{VISIBLE_REASON}} |

## 3. 功能需求

| 功能ID | 上游ID | 触发者ID | 前置条件 | 输入信息语义 | 系统行为 | 成功反馈 | 失败或禁止反馈 | 状态影响 |
|---|---|---|---|---|---|---|---|---|
| {{FR_ID}} | {{BG_BF_BR_OR_BAC_ID}} | {{SA_ID}} | {{PRECONDITION}} | {{INPUT_SEMANTICS}} | {{SYSTEM_BEHAVIOR}} | {{SUCCESS_FEEDBACK}} | {{FAILURE_FEEDBACK}} | {{STATE_IMPACT}} |

## 4. 用户场景与交互流程

| 场景ID | 上游流程ID | 功能ID | 参与者ID | 入口 | 前置状态 | 用户动作 | 系统反馈 | 替代或异常路径 | 完成状态 |
|---|---|---|---|---|---|---|---|---|---|
| {{UC_ID}} | {{BF_ID}} | {{FR_ID}} | {{SA_ID}} | {{ENTRY}} | {{PRE_STATE}} | {{USER_ACTION}} | {{SYSTEM_FEEDBACK}} | {{ALTERNATIVE_OR_ERROR}} | {{COMPLETION_STATE}} |

## 5. 页面、信息与动作需求

| 页面ID | 页面名称 | 用途 | 参与者ID | 信息区域 | 页面状态 | 功能ID |
|---|---|---|---|---|---|---|
| {{PG_ID}} | {{PAGE_NAME}} | {{PURPOSE}} | {{SA_ID}} | {{INFORMATION_AREAS}} | {{PAGE_STATES}} | {{FR_ID}} |

| 动作ID | 页面ID | 动作名称 | 显示条件 | 可用条件 | 用户交互 | 成功反馈 | 失败反馈 | 功能ID |
|---|---|---|---|---|---|---|---|---|
| {{BT_ID}} | {{PG_ID}} | {{ACTION_NAME}} | {{VISIBLE_CONDITION}} | {{ENABLED_CONDITION}} | {{INTERACTION}} | {{SUCCESS_FEEDBACK}} | {{FAILURE_FEEDBACK}} | {{FR_ID}} |

| 页面ID | 信息名称 | 业务语义 | 来源类别 | 必填条件 | 输入限制 | 空值含义 | 展示要求 |
|---|---|---|---|---|---|---|---|
| {{PG_ID}} | {{INFORMATION_NAME}} | {{BUSINESS_SEMANTICS}} | {{SOURCE_CATEGORY}} | {{REQUIRED_CONDITION}} | {{INPUT_CONSTRAINT}} | {{EMPTY_MEANING}} | {{DISPLAY_REQUIREMENT}} |

| 页面ID | 状态类型 | 触发场景 | 展示内容 | 可见动作 | 可用动作 | 不可操作原因 |
|---|---|---|---|---|---|---|
| {{PG_ID}} | 正常 | {{TRIGGER_SCENARIO}} | {{DISPLAY_CONTENT}} | {{VISIBLE_ACTIONS}} | {{ENABLED_ACTIONS}} | {{DISABLED_REASON_OR_NONE}} |

## 6. 逻辑数据需求

| 数据需求ID | 上游或功能ID | 业务信息 | 来源 | 使用场景 | 完整性或唯一性口径 | 保留要求 | 敏感级别 | 空值业务语义 |
|---|---|---|---|---|---|---|---|---|
| {{DR_ID}} | {{BO_BR_OR_FR_ID}} | {{BUSINESS_INFORMATION}} | {{SOURCE}} | {{USAGE_SCENARIO}} | {{QUALITY_RULE}} | {{RETENTION}} | {{SENSITIVITY}} | {{EMPTY_SEMANTICS}} |

## 7. 外部交互需求

| 外部交互ID | 上游或功能ID | 外部参与方 | 业务目的 | 触发条件 | 输入业务信息 | 输出业务信息 | 时效要求 | 重复或失败处理 | 责任边界 |
|---|---|---|---|---|---|---|---|---|---|
| {{IR_ID_OR_NONE}} | {{SOURCE_ID}} | {{EXTERNAL_PARTY}} | {{BUSINESS_PURPOSE}} | {{TRIGGER}} | {{INPUT_INFORMATION}} | {{OUTPUT_INFORMATION}} | {{TIMELINESS}} | {{DUPLICATE_OR_FAILURE_HANDLING}} | {{RESPONSIBILITY_BOUNDARY}} |

## 8. 非功能需求

| 非功能ID | 上游ID | 类别 | 适用场景 | 度量指标 | 目标值 | 测量条件 | 失败影响 | 验收方式 |
|---|---|---|---|---|---|---|---|---|
| {{NFR_ID}} | {{SOURCE_ID}} | {{CATEGORY}} | {{SCENARIO}} | {{METRIC}} | {{TARGET}} | {{MEASUREMENT_CONDITION}} | {{FAILURE_IMPACT}} | {{ACCEPTANCE_METHOD}} |

## 9. 系统验收标准

| 系统验收ID | 业务验收ID | 系统需求ID | 前置状态 | 用户或外部动作 | 可观察结果 | 失败或边界结果 | 验收类型 |
|---|---|---|---|---|---|---|---|
| {{SAC_ID}} | {{BAC_ID}} | {{LOCAL_REQUIREMENT_IDS}} | {{PRE_STATE}} | {{ACTOR_ACTION}} | {{OBSERVABLE_RESULT}} | {{FAILURE_OR_BOUNDARY_RESULT}} | {{FUNCTIONAL_OR_NFR}} |

## 10. 系统需求追踪矩阵

| 上游ID | 系统需求ID | 系统验收ID | 覆盖说明 |
|---|---|---|---|
| {{UPSTREAM_ID}} | {{LOCAL_REQUIREMENT_IDS}} | {{SAC_ID}} | {{COVERAGE}} |

## 11. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 系统需求 checker | {{PASS_OR_FAIL}} | {{CHECK_COMMAND_AND_OUTPUT}} |
| 生命周期 handoff | {{PASS_OR_FAIL}} | {{HANDOFF_EVIDENCE}} |
| 未关闭阻断数量 | {{BLOCKER_COUNT}} | {{BLOCKER_EVIDENCE}} |
| 系统需求审批 | {{APPROVED_OR_REJECTED}} | {{APPROVAL_EVIDENCE}} |
