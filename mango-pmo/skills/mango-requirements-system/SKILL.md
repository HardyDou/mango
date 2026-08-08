---
name: mango-requirements-system
description: 创建或评审 L5 Mango 系统需求文档，覆盖名称术语、业务需求满足方式、适用流程/状态/数据流图、系统边界和可观察验收；不写实现设计。
---

# Mango L5 系统需求

## 加载

解析 `PMO_ROOT`，以 `pm/requirement` 执行 preflight，然后读取：

- `agents/system-requirements-agent.md`
- `rules/product/02-system-requirements.md`
- `contracts/lean-documents.json`
- `templates/l5-system-requirements.md`
- `tools/check-lean-document.mjs`

本 Skill 只用于 `L5`，并要求已有确认的业务输入。

## 执行

1. 统一系统、模块、功能名称、实际简称、业务术语、角色、对象和状态。
2. 每项 `SR -> US/BR` 说明系统如何满足它：入口、前置、角色、系统行为、状态/数据变化、成功及失败/拒绝结果。
3. 只增加实际适用的 Mermaid 流程图、状态图或数据流图；图和正文名称、状态必须一致。
4. 写清系统与外部责任、权限/租户、数据输入输出边界。采用的规范写版本，代码行为写 commit/SHA。
5. 系统责任、名称、状态/数据语义或外部行为无法确定时，一次集中询问；不得发明业务规则或实现。
6. 运行 `node "$PMO_ROOT/tools/check-lean-document.mjs" --document <path>`。检查通过且责任人确认系统行为后返回生命周期。
