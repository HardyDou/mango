---
name: mango-pmo-lifecycle
description: 用户明确调用时协调 Mango L0-L5 需求到验收流程，路由单文档或 L5 四阶段；协调器不代写专项阶段。
---

# Mango PMO 生命周期

## 解析规范源

按顺序选择首个存在的 `PMO_ROOT`：`<repo>/business-pmo/mango-baseline`、`<repo>/mango-pmo`、`<plugin-root>/dist/baseline`。均不存在时 `STOP`。执行 preflight，读取 `contracts/delivery-assurance.json`、`contracts/lean-documents.json`、`tools/resolve-lean-document-policy.mjs`、`rules/11-delivery-assurance.md` 和 `rules/product/05-document-lifecycle.md`。

## 路由

1. 确定目标、成功结果、范围、工作区、需求影响、方案风险及二者最大值 `L0-L5`。
2. `L0/L1` 直接进入工程；`L2-L4` 使用一份对应模板，依次完成撰写、`check-lean-document.mjs`、实施和验证；`L5` 按顺序路由四个专项 Skill。
3. 只选择一个动作：
   - `STOP`：规范源冲突、必要直接上游缺失或请求越过必要边界。
   - `ASK`：关键目标、角色诉求、业务规则、状态/数据语义、系统边界、技术决定、文档位置或确认状态无法确定。
   - `WRITE`：可编写当前文档，但尚不能移交。
   - `NEXT`：精简检查通过、直接追踪有效、关键决定已由责任人确认且无阻断。
4. 问题按业务、系统和技术主题集中提出；能从规范或代码确定的事实不问，不臆造缺失事实。
5. 只维护直接追踪：`US -> BR`、`SR -> US/BR`、`TD -> SR`、`TASK -> TD`、`VAL -> SR/TASK`。
6. M09-M16 按可观察验收事实选择，不按等级选择；发布保持独立。

旧阶段合同、模板和检查器仅用于历史兼容，不得用于创建新文档。
