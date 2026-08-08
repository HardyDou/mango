---
name: mango-plan-implementation
description: 创建或评审 L5 Mango 实施与验证计划，将确认的技术设计转换为文件/符号级任务、依赖、可执行验证、数据切换和回滚；不新增需求、设计或执行结果。
---

# Mango L5 实施与验证计划

## 加载

解析 `PMO_ROOT`，以 `tech-lead/design` 执行 preflight，然后读取：

- `agents/implementation-plan-agent.md`
- `rules/product/04-implementation-plan.md`
- `contracts/lean-documents.json`
- `templates/l5-implementation-plan.md`
- `tools/check-lean-document.mjs`
- preflight 选中的编码/测试规范和代码基线

本 Skill 只用于 `L5`，并要求已有确认的技术设计。

## 执行

1. 定义具体 `DEL`。把工作拆成文件、配置或符号级 `TASK -> TD`，写责任角色、前置和完成标准。
2. 写清顺序、并行条件、外部依赖和失败停止条件。设计缺失时返回技术设计。
3. 编写 `VAL -> TASK/SR`，包含命令/步骤、环境/数据、观察对象、准确断言、证据路径和失败处理。
4. 数据或切换工作写前置检查、顺序、验证、停止、补偿/回滚和责任角色。发布转 `$mango-release`。
5. 责任、环境、账号、租户、数据或不可逆动作授权不明时，一次集中询问；不得臆造事实或把计划写成已完成。
6. 运行 `node "$PMO_ROOT/tools/check-lean-document.mjs" --document <path>`。检查通过、依赖可执行且实施负责人确认后返回。
