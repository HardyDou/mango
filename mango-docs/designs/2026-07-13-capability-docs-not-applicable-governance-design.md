# Capability Docs 独立 Not Applicable 原因治理设计

## 背景

PR #454 的能力文档门禁允许通过独立的 `Not applicable reason` 字段解释为什么未更新受影响模块的 README 或能力地图。该字段把多个必填文档判断合并成一个宽泛说明，既不能准确对应缺失项，也可能绕过本应完成的能力文档更新。

## 目标与边界

- 禁止 PR body 登记独立的 `Not applicable reason` 字段。
- 受影响能力缺少门禁判定为必需的 README、能力地图或业务指南时直接失败。
- 保留各 Capability Docs 字段内的 `not applicable` 状态，但必须在该字段内写明具体事实；它只说明该分类确实不相关，不能替代必需文档。
- 不改变业务代码、公开 API、配置、菜单、权限、租户、启动方式或运行时行为。

长期规则仍以 [能力说明维护规范](../../mango-pmo/rules/08-capability-docs.md) 为唯一来源；本文只记录本次治理变更的方案和取舍。

## 方案

1. 在 PR 模板中删除独立 `Not applicable reason` 输入项。
2. 在 `check-capability-docs.mjs` 中拒绝出现该独立字段，并删除它对缺失 README 或能力地图的豁免路径。
3. 保留现有逐字段状态校验，使 `not applicable` 只能与对应字段的具体说明一起使用。
4. 更新能力文档规则及规则索引，明确机器门禁行为。
5. 为 business starter 的 PMO 投影变更补充模块 README 事实说明，并重新同步 starter 基线。
6. 从 PR #454 body 删除独立字段，逐项记录文档更新或不适用事实。

## 失败处理

- PR body 出现独立字段：门禁报告该字段已被禁止。
- 能力影响文件缺少所需 README、能力地图或业务指南：门禁列出缺失文件，不接受聚合豁免理由。
- 逐字段内容缺少状态或具体说明：继续使用现有字段校验失败信息。

## 验证

- 自测覆盖独立字段被拒绝、独立字段不能豁免缺失文档、合法逐字段说明通过。
- 执行 capability docs checker、README 审计、规则索引检查和 workspace layout 检查。
- 同步并验证 business starter PMO 基线及 CLI 投影。
- 使用更新后的 PR #454 body 对 `origin/main..HEAD` 执行真实门禁。

## 取舍

只删除模板无法阻止手工填写；全面禁止所有 `not applicable` 又会迫使无关分类产生无意义文档变更。因此采用“禁止独立聚合字段、保留逐字段事实说明、必需文档不可豁免”的方案。
