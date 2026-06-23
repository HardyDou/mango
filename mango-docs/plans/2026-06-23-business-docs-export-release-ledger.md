# Business Docs Export Release Ledger

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| BDE-001 | 用户要求 | PRD 文档可输出给业务开发人员 | 在公开文档站白名单、首页和侧栏暴露 PRD 模板与 PRD 模板规范 | `mango-docs/.vitepress/stage-public-docs.mjs`、`mango-docs/README.md` | `npm --prefix mango-docs run docs:stage` 后检查 staged 文件和导航 | DONE | `mango-docs/.vitepress/stage-public-docs.mjs` |
| BDE-002 | 用户要求 | 设计文档可输出给业务开发人员 | 在公开文档站白名单、首页和侧栏暴露详细设计模板与详细设计模板规范 | `mango-docs/.vitepress/stage-public-docs.mjs`、`mango-docs/README.md` | `npm --prefix mango-docs run docs:stage` 后检查 staged 文件和导航 | DONE | `mango-docs/.vitepress/stage-public-docs.mjs` |
| BDE-003 | 用户要求 | 规范可输出给业务开发人员 | 保持长期规范仍在 `mango-pmo`，文档站只提供入口和白名单发布 | `mango-docs/.vitepress/stage-public-docs.mjs`、`mango-docs/README.md` | 人工检查链接目标均指向 `mango-pmo` 规范源 | DONE | `mango-docs/README.md` |
| BDE-004 | PMO 发布门禁 | 发布日志说明发布对象、业务升级步骤和验证 | 在平台级 `CHANGELOG.md` 增加 `v2026.06.23-business-docs-export` 发布段 | `CHANGELOG.md` | 人工检查最新发布段包含 Published Packages、Upgrade Notes、Verification | DONE | `CHANGELOG.md` |
| BDE-005 | 交付契约规则 | 本次发布有计划、台账和完整性验证 | 新增发布计划和交付台账 | 本文档、发布计划 | `node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/plans/2026-06-23-business-docs-export-release-plan.md --ledger mango-docs/plans/2026-06-23-business-docs-export-release-ledger.md --mode verify` | DONE | 本文档 |
