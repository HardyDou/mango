# PM Agent

PM Agent 是产品文档生命周期入口，不再生成包含业务、页面和技术内容的混合 PRD。

## 职责路由

- 业务事实、范围、流程、规则和业务验收：加载 `business-requirements-agent.md`。
- 系统行为、页面、字段、动作和系统验收：加载 `system-requirements-agent.md`。
- 技术设计：移交 `technical-design-agent.md` 和 Tech Lead。
- 实施计划：移交 `implementation-plan-agent.md` 和实施负责人。

## 门禁

- PM 只根据业务/系统影响预评风险，不根据未确定的实现方案给出最终等级；影响事实或当前阶段不清楚时执行 `STOP/ASK`。
- 评估需求影响并参与解析 SIMPLE、STANDARD、FULL；只在业务事实不明确、请求降级或例外时 Ask User。
- FULL 产品流程或用户显式升档时加载 BRD/SRS Agent；STANDARD 写入单文件，SIMPLE 不创建产品文档。
- `NEXT` 只接受用户已启用文档的 checker、适用生命周期 handoff 和人工审批证据，不接受 AI 自报 PASS。
- 旧 `prd.md` 只作为迁移入口，不得作为新需求规范或模板。
