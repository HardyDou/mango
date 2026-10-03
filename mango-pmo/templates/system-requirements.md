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

> **写作与决定门禁：** 遵循 [`Agent 文本输出规范`](../rules/13-agent-text-output.md)。先写重点，使用具体主语和稳定术语，保留成功/失败边界。需求决定另按 [`决策专家评审规范`](../rules/14-decision-expert-review.md)记录三轮意见、方案整理和同行评审。

> **先读：这份文档要回答什么**
> - **一句话目标：** {{ONE_SENTENCE_GOAL}}
> - **真实场景：** {{CONCRETE_SYSTEM_SCENARIO}}
> - **关键输入 / 输出：** {{SYSTEM_INPUT_AND_OBSERVABLE_OUTPUT}}
> - **成功与失败：** {{SYSTEM_SUCCESS_AND_FAILURE_BOUNDARY}}
> - **明确不做：** {{SYSTEM_OUT_OF_SCOPE}}
> - **阅读顺序：** 先看本段，再看参与者、功能和场景，最后看页面、非功能、验收和追踪矩阵。

## 0. 使用合同与最小填写方式

填写人是系统分析师或产品经理；输入必须来自 BRD、已批准的业务决定和可验证的技术/平台事实。SRS 的下游使用方是 TDD、前后端开发、QA 和验收人员。

| 区域 | 只填写什么 | 来源与填写人 | 下游用途 | 校验方式 |
|---|---|---|---|---|
| Front Matter | 文档身份、风险、Owner、上游 BRD ID/hash | PMO 元数据；SRS Owner 填写 | 证明需求来源和 handoff | 文档契约 + 上游 hash 检查 |
| 先读摘要 | 系统目标、输入输出、成功/失败和不做范围 | SRS Owner 从第 1～4 节归纳 | 开发/QA 快速判断系统责任 | 人工复述和范围核对 |
| 第 1～4 节 | 系统边界、参与者、功能、场景和异常路径 | BRD、接口事实、页面走查 | TDD 模块/API 设计和测试场景 | FR/UC 与上游 ID 引用检查 |
| 第 5～7 节 | 页面动作、字段语义、数据和外部交互 | 业务验收、现有 API、外部协议 | 前端实现、数据模型和集成契约 | 字段/动作/失败反馈逐项检查 |
| 第 8～11 节 | 可测非功能、系统验收、追踪和审批 | QA、运维、安全和 Approver | 测试计划、发布门禁和审批 | NFR/SAC 具备指标、条件和证据 |

**最小合格示例（复制后替换，不要保留示例）：**

- `SC-001`：系统负责展示订单状态；业务系统负责决定订单是否允许关闭。
- `FR-001`：登录用户请求订单详情；成功返回状态，越权返回禁止反馈，不泄露对象是否存在。
- `UC-001`：从订单列表进入详情；加载失败显示可重试原因，不显示假成功状态。
- `NFR-001`：在 50 并发、已登录和数据已准备条件下，详情接口 p95 ≤ 500ms；用集成测试测量。
- `SAC-001`：验证 `FR-001` 的成功、越权和对象不存在三条路径，并保存响应和日志证据。

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
