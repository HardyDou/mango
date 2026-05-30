# Mango 前端物料依赖与自由组合部署产品化升级计划台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| FMP-001 | 用户要求 | 在新 worktree 制定升级计划 | 使用 `worktree/frontend-productization-plan` 分支和独立 worktree 承载本次文档交付 | `.mango/worktrees/frontend-productization-plan` | `git worktree list` 可见独立 worktree | DONE | 本次执行记录 |
| FMP-002 | 用户要求 | 目标聚焦前端保留 Mango 特性并支持自由组合部署 | 计划明确单体本地模块、微前端模块、混合部署、iframe/外链四类模式 | `mango-docs/plans/2026-05-29-frontend-material-productization-plan.md` | 人工复核计划第 6.2 节 | DONE | `mango-docs/plans/2026-05-29-frontend-material-productization-plan.md` |
| FMP-003 | 用户要求 | 支持通过依赖 Mango 发布物料快速开发 | 计划把 npm 发布契约、Admin Shell API、内置能力包、starter、CLI、外部消费验收拆成 Sprint | `mango-docs/plans/2026-05-29-frontend-material-productization-plan.md` | 人工复核计划第 7 节 | DONE | `mango-docs/plans/2026-05-29-frontend-material-productization-plan.md` |
| FMP-004 | PMO 要求 | 明确影响模块、接口变化、数据变化和测试范围 | 计划写明影响 `mango-ui`、`mango-business-starter`、`create-mango-app`、运行时协议和验收链路 | `mango-docs/plans/2026-05-29-frontend-material-productization-plan.md` | 计划文档覆盖目标架构、Sprint 拆分、验收总口径、风险限制 | DONE | `mango-docs/plans/2026-05-29-frontend-material-productization-plan.md` |
| FMP-005 | PMO 要求 | 建立交付台账并通过台账检查 | 本台账记录本次计划交付项，实施 Sprint 另建台账 | `mango-docs/plans/2026-05-29-frontend-material-productization-ledger.md` | `node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/plans/2026-05-29-frontend-material-productization-plan.md --ledger mango-docs/plans/2026-05-29-frontend-material-productization-ledger.md --mode verify` | DONE | 本次验证命令输出 |
| FMP-006 | 用户要求 | 每个阶段完成后执行回归测试和新特性测试，通过后进入下一阶段 | 计划新增阶段推进门禁，并在 Sprint A-F 验收中分别补充新特性测试和回归测试口径 | `mango-docs/plans/2026-05-29-frontend-material-productization-plan.md` | 交付台账检查通过，人工复核计划第 7.0 节和各 Sprint 验收项 | DONE | `mango-docs/plans/2026-05-29-frontend-material-productization-plan.md` |
