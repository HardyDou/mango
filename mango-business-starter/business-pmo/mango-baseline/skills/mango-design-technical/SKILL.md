---
name: mango-design-technical
description: 创建或评审 L5 Mango 技术设计，追踪技术如何支撑系统需求，记录采用的规范和代码基线、实现决定、统一字典、验证与回滚；不做需求发现和任务执行。
---

# Mango L5 技术设计

## 加载

解析 `PMO_ROOT`，以 `tech-lead/design` 执行 preflight，然后读取：

- `agents/technical-design-agent.md`
- `rules/product/03-technical-design.md`
- `contracts/lean-documents.json`
- `templates/l5-technical-design.md`
- `tools/check-lean-document.mjs`
- preflight 选中的技术规范和代码基线

本 Skill 只用于 `L5`，并要求已有确认的系统需求。

## 执行

1. 重新评估方案风险。每项 `TD -> SR` 写清设计怎样支撑系统需求。
2. 采用的技术规范写精确版本；代码示例/基线写路径和 commit/SHA。
3. 只展开实际适用的模块、契约、事件、持久化、权限/租户、事务/并发、兼容、迁移和回滚决定。
4. 新模块/新系统建立完整公共枚举、状态、字段、错误、权限、配置、事件和映射字典；其他场景只写变更字典，未改项引用代码源。
5. 公共契约、数据、安全、租户、兼容或回滚决定无法推导时，一次集中询问；不得自行决定或复制冲突的历史代码。
6. 使用 `VAL -> SR` 映射可观察断言。运行 `node "$PMO_ROOT/tools/check-lean-document.mjs" --document <path>`，检查通过且 Tech Lead 确认后返回。
