# Frontend Mode Matrix Sprint L

## 1. 背景

Sprint K 已完成 business starter 初始化体验增强，证明生成项目可以通过 `dev-start` 使用真实后端、真实业务 API、真实页面布局和 registry 消费回归。下一阶段需要把 Mango 自由组合部署的前端模式纳入独立验收，避免只验证 `mixed` 而遗漏 `local` 和 `micro`。

## 2. 目标

用同一 `create-mango-app` 初始化入口生成 `local`、`micro`、`mixed` 三种前端模式项目，验证它们都能通过 Mango 发布物料依赖完成安装、类型检查、构建、启动和浏览器冒烟，并保留截图、布局报告和汇总报告。

## 3. 范围

- 新增部署模式矩阵 E2E 脚本，覆盖 `--frontend-mode local`、`--frontend-mode micro`、`--frontend-mode mixed`。
- 每个模式生成独立临时业务项目，复用 Mango package build 产物，不引用 Mango 仓内源码入口。
- 每个模式执行模板检查、依赖安装、类型检查、构建和前端浏览器冒烟。
- `local` 模式验证业务列表页真实渲染，并检查搜索区、功能区、表格区、分页区、未启动后端时的明确错误态和横向溢出。
- `micro`、`mixed` 模式验证业务菜单进入微前端运行时决策，并在远程入口未启动时展示明确诊断态。
- E2E 输出每个模式的截图、布局 JSON 报告和统一 summary。

## 4. 不做什么

- 不新增新的部署模式。
- 不在本 Sprint 启动真实远程微应用。
- 不替代 Sprint K 的真实后端 `dev-start` E2E。
- 不扩大声明为所有历史页面都符合 Mango UI 规范。

## 5. 改动项

- `mango-ui/scripts/frontend-mode-matrix-e2e.mjs`
- `mango-ui/package.json`
- `mango-docs/plans/2026-05-29-frontend-mode-matrix-sprint-l.md`
- `mango-docs/plans/2026-05-29-frontend-mode-matrix-sprint-l-ledger.md`

## 6. 验证方式

- 新特性测试：
  - `node --check mango-ui/scripts/frontend-mode-matrix-e2e.mjs`
  - `pnpm frontend-mode:matrix-e2e -- --evidence-dir /tmp/mango-sprint-l-mode-matrix-evidence`
- 回归测试：
  - `node mango-business-starter/scripts/check-template.mjs`
  - `node mango-ui/packages/create-mango-app/templates/mango-business-starter/scripts/check-template.mjs`
  - `node mango-ui/packages/create-mango-app/scripts/check-cli.mjs`
  - `pnpm package:registry-e2e -- --evidence-dir /tmp/mango-sprint-l-regression-evidence`
- 交付契约：
  - `node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/plans/2026-05-29-frontend-mode-matrix-sprint-l.md --ledger mango-docs/plans/2026-05-29-frontend-mode-matrix-sprint-l-ledger.md --mode verify`

## 7. 完成标准

- 三种前端模式均产出截图和 JSON 报告。
- `local` 模式报告显示业务列表页布局检查通过，并记录未启动后端时的业务 API 错误态。
- `micro` 和 `mixed` 模式报告显示微前端诊断态检查通过。
- 所有生成项目完成 install、typecheck、build。
- registry 回归 E2E 保留截图和报告并通过。
- 台账全部 `DONE`，无未说明例外。

## 8. 风险与限制

- 本 Sprint 的 `micro`、`mixed` 浏览器冒烟只验证 shell 运行时决策和未启动远程应用时的诊断态，真实远程微应用完整挂载需要后续独立 Sprint。
- 前端构建可能继续出现 VueUse `#__PURE__` 和 Vite chunk size 警告，若不影响退出码，本 Sprint 记录为风险。
