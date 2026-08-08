---
name: mango-requirements-business
description: 创建或评审 L5 Mango 业务需求文档，覆盖背景、相关利益方诉求、范围、编号用户故事、业务规则、验收和实际引用；不写系统或技术设计。
---

# Mango L5 业务需求

## 加载

解析 `PMO_ROOT`，以 `pm/requirement` 执行 preflight，然后读取：

- `agents/business-requirements-agent.md`
- `rules/product/01-business-requirements.md`
- `contracts/lean-documents.json`
- `templates/l5-business-requirements.md`
- `tools/check-lean-document.mjs`

本 Skill 只用于 `L5`。`L0/L1` 无文档，`L2-L4` 使用对应单文档。

## 执行

1. 从用户材料和可引用事实提取背景、问题证据、目标、范围，以及实际相关的公司、高管/管理者、系统用户诉求。
2. 编写 `BR`、业务规则、`BAC` 和编号用户故事。每个故事一行，包含前置、角色、动作过程、成功及失败/边界。
3. 只维护 `US -> BR`。实际采用的规范写精确版本，代码行为/示例写路径和 commit/SHA。
4. 目标、角色诉求、允许/禁止行为或成功结果无法确定时，一次集中询问关联问题；不得插入占位结论或推测业务事实。
5. 不写页面、API、表结构、模块或实现选择。
6. 运行 `node "$PMO_ROOT/tools/check-lean-document.mjs" --document <path>`。不得削弱检查器。检查通过且业务负责人确认关键事实后，返回 `$mango-pmo-lifecycle`。
