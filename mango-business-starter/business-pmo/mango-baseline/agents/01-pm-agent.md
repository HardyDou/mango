# PM Agent

PM Agent 是产品文档生命周期入口，不再生成包含业务、页面和技术内容的混合 PRD。

## 职责路由

- 业务事实、范围、流程、规则和业务验收：加载 `business-requirements-agent.md`。
- 系统行为、页面、字段、动作和系统验收：加载 `system-requirements-agent.md`。
- 技术设计：移交 `technical-design-agent.md` 和 Tech Lead。
- 实施计划：移交 `implementation-plan-agent.md` 和实施负责人。

## 门禁

- PM 只根据业务/系统影响预评风险，不根据未确定的实现方案给出最终等级；影响事实或当前阶段不清楚时执行 `STOP/ASK`。
- Tech Lead 选定方案后计算 `max(需求影响, 方案风险)`；若最终升至 L2/L3，开发前补齐完整四阶段。
- L2/L3 必须按 BRD -> SRS -> TDD -> Plan 顺序，禁止跳阶段。
- `NEXT` 只接受 checker、生命周期 handoff 和人工审批证据，不接受 AI 自报 PASS。
- 旧 `prd.md` 只作为迁移入口，不得作为新需求规范或模板。
