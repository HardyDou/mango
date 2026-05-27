# Mango Business Starter 命名收敛交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| BSN-001 | 用户确认 | 业务项目生产起点采用 Mango Business Starter 命名 | 仓内资产从 `mango-business-template` 收敛为 `mango-business-starter` | `mango-business-starter` | `node mango-business-starter/scripts/check-template.mjs` | DONE | `mango-business-starter/README.md` |
| BSN-002 | 用户确认 | CLI 采用类似 Vue 的 init/create 体验 | CLI 包收敛为 `create-mango-app`，bin 提供 `create-mango-app` 和 `mango`，生成项目携带可执行 Mango baseline | `mango-ui/packages/create-mango-app` | `node mango-ui/packages/create-mango-app/scripts/check-cli.mjs` | DONE | `mango-ui/packages/create-mango-app/package.json` |
| BSN-003 | 用户要求 | 给将来业务使用 Mango 作为底座提供说明文档 | 在 docs 中新增开发说明，长期规范仍引用 `mango-pmo` | `business-project-development-guide.md` | 人工阅读并检查 PMO 规范源边界 | DONE | `mango-docs/designs/business-project-development-guide.md` |
| BSN-004 | 用户要求 | 说明真实生成目录结构 | 文档给出 `guarantee-platform` 生产目录结构 | 目录结构说明 | `rg -n "guarantee-platform" mango-docs/designs/business-project-development-guide.md` | DONE | `mango-docs/designs/business-project-development-guide.md` |
| BSN-005 | 用户要求 | 依赖 Mango 做业务开发时必须带出必要规范 | starter 携带 `business-pmo/mango-baseline` 可执行快照，并保留业务规则扩展区 | `business-pmo/mango-baseline` | `node mango-business-starter/business-pmo/mango-baseline/tools/pmo-preflight.mjs --role dev --phase develop --task "新增业务模块" --paths "backend/modules/demo,frontend/packages/demo"` | DONE | `mango-business-starter/business-pmo/mango-baseline/rules/index.json` |
| BSN-006 | PMO 要求 | 实质交付必须建立计划和台账并验证 | 新增本轮计划和交付台账 | 计划和台账 | `node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/plans/2026-05-27-business-starter-naming-plan.md --ledger mango-docs/plans/2026-05-27-business-starter-naming-ledger.md --mode verify` | DONE | `mango-docs/plans/2026-05-27-business-starter-naming-ledger.md` |
