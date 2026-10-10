# 产品文档模板选择

本页只负责选择入口，不替代模板正文和生命周期规则。

## 使用合同

模板不是信息收集表。只填写能被下一阶段、实现、测试、验收或审批使用的内容；没有事实来源或下游用途的字段删除或填 `NONE`，不得用空泛描述占位。

| 填写区域 | 必须回答 | 事实来源 | 下游使用方 | 最小校验 |
|---|---|---|---|---|
| Front Matter | 这是谁的文档、谁负责、来自哪个上游版本 | PMO、上游文档 hash、任务记录 | 生命周期 checker、审批人 | 文档契约 |
| 先读摘要 | 为什么做、输入输出、成功/失败、不做什么 | 正文各章节归纳 | 人类和 Agent 首次阅读 | 人工复述 |
| 主体表格 | 每行是什么事实、决定或可观察结果 | 访谈、源码、规则、命令输出 | 下一阶段文档、开发、QA | ID、引用、枚举检查 |
| 追踪与审批 | 如何证明覆盖和谁批准 | 验收证据、评审记录 | 发布和复盘 | checker、证据路径 |

正文模板中“使用合同与最小填写方式”说明每个区域的填写人、用途和校验；示例复制后必须替换或删除。

## 先回答一个问题

| 你要确认的内容 | 选择模板 | 模板入口 |
|---|---|---|
| 为什么做、谁遇到、业务流程、业务规则、业务验收 | 业务需求说明书（BRD） | [business-requirements.md](./business-requirements.md) |
| 用户能看到什么、系统怎么响应、页面/字段/动作、失败边界、非功能要求 | 系统需求规格说明书（SRS） | [system-requirements.md](./system-requirements.md) |
| 模块、接口、数据、安全、测试和技术取舍 | 技术设计（TDD） | [technical-design.md](./technical-design.md) |
| 把已确认设计拆成任务、依赖和验证命令 | 实施计划（Plan） | [implementation-plan.md](./implementation-plan.md) |

## 选择顺序

```text
业务问题与规则 -> BRD -> SRS -> TDD -> Implementation Plan
```

- 只涉及业务事实、范围或验收：从 BRD 开始。
- 只需明确系统行为，且上游业务事实已有受控来源：从 SRS 开始，并填写上游文档 ID 和摘要。
- 需要同时表达业务和系统内容：分别填写 BRD、SRS；不合并成 PRD。
- TDD 不新增业务规则；Plan 不新增技术设计。
- SIMPLE 不创建四阶段产品文档；STANDARD 使用标准交付记录。适用阶段和风险以 [文档生命周期规则](../rules/product/05-document-lifecycle.md) 为准。

## 相关入口

- [人类阅读入口规则](../rules/product/05-document-lifecycle.md#life-read-050-人类阅读入口)
- [业务需求撰写 Agent](../agents/business-requirements-agent.md)
- [系统需求撰写 Agent](../agents/system-requirements-agent.md)
- [技术设计撰写 Agent](../agents/technical-design-agent.md)
- [实施计划撰写 Agent](../agents/implementation-plan-agent.md)
- [旧 PRD 迁移说明](./prd.md)
