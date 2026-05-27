# AI 编码红线规范治理交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| REDLINE-001 | 用户要求 | 新增 AI 编码红线，禁止伪代码、TODO/FIXME、mock/fake/dummy/hardcode、空实现、吞异常、固定成功、绕过权限/租户/数据权限、未接真实数据却伪装完成 | 新增通用规范 `rules/03-ai-coding-redlines.md` | `mango-pmo/rules/03-ai-coding-redlines.md` | 人工检查规范内容覆盖用户原子要求 | DONE | `mango-pmo/rules/03-ai-coding-redlines.md` |
| REDLINE-002 | 用户要求 | AI 编码红线必须同时适用于 Mango 开发者和业务开发 | 规范适用范围明确写入 Mango 框架开发者和基于 Mango 的业务项目开发者 | `mango-pmo/rules/03-ai-coding-redlines.md` | `node mango-pmo/tools/pmo-preflight.mjs --role dev --phase develop --task "新增业务模块" --paths "mango,mango-ui"` 输出红线规则 | DONE | `mango-pmo/rules/index.json` |
| REDLINE-003 | PMO 规范源规则 | 长期规范必须收口到 `mango-pmo`，不能只写在业务模板或文档中 | `mango-pmo` 为规范源，业务 starter 只携带 Mango baseline 快照 | `mango-pmo/rules/index.json`、业务 baseline | 检查 `mango-pmo` 与两个业务 baseline 均包含红线文件 | DONE | `mango-business-starter/business-pmo/mango-baseline/rules/03-ai-coding-redlines.md` |
| REDLINE-004 | 业务开发要求 | 新业务项目通过模板启动后也必须默认遵守红线 | 同步到 `create-mango-app` 内置模板，并更新模板检查 | `mango-ui/packages/create-mango-app/templates/mango-business-starter/...` | `node mango-ui/packages/create-mango-app/scripts/check-cli.mjs` | DONE | `mango-ui/packages/create-mango-app/templates/mango-business-starter/business-pmo/mango-baseline/rules/03-ai-coding-redlines.md` |
| REDLINE-005 | PMO 要求 | 本次规范治理必须有计划、台账和验证记录 | 新增治理计划和交付台账 | 计划和台账 | `node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/plans/2026-05-27-ai-coding-redlines-governance-plan.md --ledger mango-docs/plans/2026-05-27-ai-coding-redlines-governance-ledger.md --mode verify` | DONE | `mango-docs/plans/2026-05-27-ai-coding-redlines-governance-ledger.md` |
