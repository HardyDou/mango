---
retrospectiveId: {{RETRO_ID}}
documentType: delivery-retrospective
status: DRAFT
owner: {{OWNER}}
reviewedCommitOrVersion: {{COMMIT_OR_VERSION}}
sourceDeliveryRecord: {{DELIVERY_RECORD}}
---

# {{DELIVERY_NAME}} 交付复盘

> **写作门禁：** 遵循 [`Agent 文本输出规范`](../rules/13-agent-text-output.md)。只写事实、证据和下一步，不用空泛结论替代观察结果。

> **先读：这次复盘要回答什么**
> - **一句话结论：** {{ONE_SENTENCE_CONCLUSION}}
> - **真实场景：** {{CONCRETE_DELIVERY_SCENARIO}}
> - **关键证据：** {{EVIDENCE_PATHS_AND_COMMANDS}}
> - **成功与失败：** {{WHAT_WORKED_AND_WHAT_FAILED}}
> - **明确不做：** 不重写历史结果、不替代审批或 QA、不把单次现象直接升格为长期规则。
> - **阅读顺序：** 先看结论，再看保留/改变/停止，最后看理解与路由及后续动作。

## 1. 保留

| 项目 | 观察到的事实 | 证据 |
|---|---|---|
| {{KEEP_ID}} | {{KEEP_FACT}} | {{KEEP_EVIDENCE}} |

## 2. 改变

| 项目 | 观察到的事实 | 下次改变 | 证据 |
|---|---|---|---|
| {{CHANGE_ID}} | {{CHANGE_FACT}} | {{NEXT_CHANGE}} | {{CHANGE_EVIDENCE}} |

## 3. 停止

| 项目 | 停止的行为或假设 | 原因 | 证据 |
|---|---|---|---|
| {{STOP_ID}} | {{STOP_BEHAVIOR}} | {{STOP_REASON}} | {{STOP_EVIDENCE}} |

## 4. 理解与路由复盘

| 场景 | 用户或 Agent 原先卡在哪里 | 首个有效来源 | 是否一次命中 | 下次入口 |
|---|---|---|---|---|
| {{UNDERSTANDING_SCENARIO}} | {{COMPREHENSION_GAP}} | {{FIRST_USEFUL_SOURCE}} | {{FIRST_SOURCE_HIT}} | {{NEXT_ENTRY}} |

## 5. 后续动作

| 动作ID | 责任人 | 目标路径或 Skill | 完成条件 | 截止里程碑 | 状态 |
|---|---|---|---|---|---|
| {{FOLLOW_UP_ID}} | {{FOLLOW_UP_OWNER}} | {{FOLLOW_UP_TARGET}} | {{FOLLOW_UP_DONE}} | {{FOLLOW_UP_MILESTONE}} | OPEN |

## 6. 证据索引

| 证据 | 类型 | 用途 |
|---|---|---|
| {{EVIDENCE_PATH}} | {{EVIDENCE_TYPE}} | {{EVIDENCE_PURPOSE}} |
