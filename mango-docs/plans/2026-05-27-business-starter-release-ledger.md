# Mango Business Starter 发布修复交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| REL-001 | 用户要求 | 发布所有内容前必须确保新特性可用 | `create-mango-app` 发布包内置 `mango-business-starter` | `templates/mango-business-starter` | `npm pack --dry-run` 检查 tarball 文件 | DONE | `mango-ui/packages/create-mango-app/package.json` |
| REL-002 | 发布检查 | 外部项目不能依赖仓内根目录模板 | CLI 默认优先读取包内模板，开发态回退仓内模板 | `src/index.mjs` | `node mango-ui/packages/create-mango-app/scripts/check-cli.mjs` | DONE | `mango-ui/packages/create-mango-app/src/index.mjs` |
| REL-003 | 发布检查 | 发布脚本必须能识别 `create-mango-app` | `create-*` 包不自动转换为 `@mango/*` | `publish-package.mjs` | `pnpm publish:pkg create-mango-app --dry-run` | DONE | `mango-ui/scripts/publish-package.mjs` |
| REL-004 | PMO 要求 | 发布任务必须建立计划和台账 | 新增本轮发布修复计划和台账 | 计划和台账 | `node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/plans/2026-05-27-business-starter-release-plan.md --ledger mango-docs/plans/2026-05-27-business-starter-release-ledger.md --mode verify` | DONE | `mango-docs/plans/2026-05-27-business-starter-release-ledger.md` |
