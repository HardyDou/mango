# 业务开发 PMO 验收加固交付契约

## 1. 目标

补强 Mango 业务项目初始化后携带的开发规范和验收证据口径，让业务开发可以按 PMO 正常开发，并避免 AI 只用接口 200、页面无异常或截图有画面替代功能和 UI 细节验收。

## 2. 范围

- 主 `mango-pmo` QA Agent、前端测试规范和交付模板。
- `mango-cli` full 模板中的 `business-pmo/mango-baseline` 快照。
- `mango-cli` 模板生成测试。

## 3. 不做什么

- 不修改业务服务代码、接口、数据库或前端页面。
- 不发布 npm/Maven 版本。
- 不开放自由版本组合、npm latest 或本地 package source。

## 4. 设计输入

- 用户要求：先处理业务开发配套规范，让业务可以正常开发；做好验收策略确保能够验收。
- PMO 规范：`rules/00-dev-flow.md`、`rules/03-ai-coding-redlines.md`、`rules/01-delivery-contract.md`、`agents/05-pmo-agent.md`。

## 5. 设计说明

### 5.1 影响模块

- `mango-pmo/agents/04-qa-agent.md`
- `mango-pmo/rules/frontend/04-test.md`
- `mango-pmo/templates/delivery-contract.md`
- `mango-pmo/templates/acceptance-evidence.md`
- `mango-ui/packages/mango-cli/templates/full/business-pmo/mango-baseline/**`
- `mango-ui/packages/mango-cli/scripts/check-cli.mjs`

### 5.2 接口变化

无。

### 5.3 数据变化

无。

### 5.4 菜单/页面/权限变化

无。

### 5.5 测试范围

- PMO preflight 能加载业务开发所需前端规则。
- delivery-contract-check 能验证本交付台账。
- `mango-cli` 测试能生成项目并校验 baseline 带出验收模板和 E2E 细则。
- baseline 与主 PMO 的规则、Agent、模板保持同步，`rules/index.json` 保留业务路径特化。

## 6. 风险与限制

- 本次只加固规范和模板，不实际启动业务项目做浏览器 E2E。
- `business-pmo` 是模板快照，已生成的历史业务项目需要升级或手工同步才能获得新规则。

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| TASK-001 | 用户要求 | 业务开发规范覆盖 AI 常见错误，不能只看接口 200 或页面无异常 | 在 QA Agent 和前端测试规范中明确有效 E2E 验收证据 | `mango-pmo/agents/04-qa-agent.md`、`mango-pmo/rules/frontend/04-test.md` | 检查规则文本包含功能点、业务断言、UI 细节、console/network 和截图证据要求 | DONE | `pnpm --dir mango-ui -F mango-cli test` |
| TASK-002 | 用户要求 | 业务项目初始化后也能获得同等规范 | 同步主 PMO 到 `mango-cli` 的业务 baseline 快照 | `mango-ui/packages/mango-cli/templates/full/business-pmo/mango-baseline/**` | 运行 CLI 生成项目并断言 baseline 带出新规则和模板 | DONE | `pnpm --dir mango-ui -F mango-cli test` |
| TASK-003 | 用户要求 | 做好验收策略确保能够验收 | 增加验收证据模板，要求记录页面/接口、功能点、测试数据、断言、UI、console/network、截图/trace | `mango-pmo/templates/acceptance-evidence.md`、baseline 同名模板、`delivery-contract.md` | 检查模板列完整，并通过 CLI 测试验证生成项目包含模板 | DONE | `pnpm --dir mango-ui -F mango-cli test` |
| TASK-004 | PMO 要求 | 本次治理也按交付台账验证 | 新增临时交付契约并运行 delivery-contract-check | `mango-pmo/tmp/2026-06-01-business-pmo-acceptance-hardening.md` | `node mango-pmo/tools/delivery-contract-check.mjs --design ... --ledger ... --mode verify` | DONE | 本文件 |
| TASK-005 | 用户要求 | 加强验收，确保不是只填模板 | 增加验收证据检查脚本，校验证据表必填列、结论、弱验收表述和截图/trace 证据 | `mango-pmo/tools/acceptance-evidence-check.mjs`、baseline 同名脚本、CLI 测试正反样例 | 正样例通过，弱证据样例失败，CLI 生成项目测试通过 | DONE | `node mango-pmo/tools/acceptance-evidence-check.mjs --evidence ...`、`pnpm --dir mango-ui -F mango-cli test` |
| TASK-006 | 用户要求 | 在新业务工作区测试业务开发规范是否可激活、是否全面、是否存在其它问题并记录 | 使用当前 `mango-cli` 在 `/tmp` 生成 custom 业务项目，模拟 dev/qa/pm/design/security/ledger 场景 preflight 和证据检查 | `mango-ui/packages/mango-cli/templates/full/AGENTS.md`、`business-pmo/README.md`、`business-pmo/mango-baseline/README.md`、`mango-cli/src/index.mjs` | 新业务项目 preflight 能激活前后端、权限、UI、E2E、交付契约；弱验收被拒绝；发现并修复 preset 文案固定为 full、baseline commit 固定旧值、入口缺少验收命令说明 | DONE | 临时业务项目 `/tmp/mango-business-pmo-check-*` 执行记录、`pnpm --dir mango-ui -F mango-cli test` |

## 8. 验收证据记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| TASK-001 | PMO 规则 | QA/E2E 规则覆盖 | 规则文本 | 包含“不能只验证接口 200”“业务结果断言”“UI 细节断言”等要求 | 无页面 | 不适用 | `pnpm --dir mango-ui -F mango-cli test` | DONE |
| TASK-002 | CLI 生成项目 | baseline 快照带出 | full preset 生成项目 | 生成项目包含 `frontend/04-test.md`、QA Agent、验收模板 | 无页面 | 不适用 | `pnpm --dir mango-ui -F mango-cli test` | DONE |
| TASK-003 | PMO 模板 | 验收证据模板 | 模板文件 | 模板包含功能点、关键断言、UI/交互、console/network、截图/trace 列 | 无页面 | 不适用 | `pnpm --dir mango-ui -F mango-cli test` | DONE |
| TASK-004 | PMO 检查 | 台账验证 | 本文件 | 台账状态均为 DONE | 无页面 | 不适用 | `delivery-contract-check` | DONE |
| TASK-005 | PMO 工具 | 验收证据检查 | 正反样例 | 正样例通过，包含“接口 200/页面无异常”的弱证据样例被拒绝 | 无页面 | 不适用 | `acceptance-evidence-check`、`pnpm --dir mango-ui -F mango-cli test` | DONE |
| TASK-006 | 新业务工作区 | 规范激活和可用性抽查 | `custom + workflow,notice` 生成项目 | dev/qa/pm/design/security/ledger 场景均能加载相关规则；delivery-contract-check 和 acceptance-evidence-check 可在业务项目内运行 | 入口文档显示 custom preset、验收检查命令；baseline README commit 不再是旧固定值或 unknown | 不适用 | 临时业务项目命令输出、`pnpm --dir mango-ui -F mango-cli test` | DONE |

## 9. 新业务工作区测试记录

测试方式：

```bash
node mango-ui/packages/mango-cli/src/index.mjs init demo-business \
  --preset custom \
  --modules workflow,notice \
  --topology monolith \
  --package com.example.business \
  --group-id com.example \
  --force
```

激活场景：

| 场景 | 结果 |
|---|---|
| `dev/develop` 开发客户管理页面，路径 `frontend,backend` | 激活交付契约、AI 红线、Dev、后端 API/DB/Test、前端 Vue/UI/组件/Monorepo/E2E、安全规则 |
| `qa/verify` 验收客户管理 E2E，路径 `frontend,backend,business-docs` | 激活 QA、交付契约、前后端规则、前端测试/UI 规则、产品文档规则 |
| `pm/requirement` 编写需求，路径 `business-docs` | 激活 PM、PRD、Sprint 和交付契约 |
| `tech-lead/design` 设计接口、数据库、权限、菜单、页面 | 激活 Tech Lead、后端 API/DB/Test、安全、前端 UI/E2E 规则 |
| `dev/develop` 新增权限和租户数据权限 | 激活安全、API、DB、前端和 E2E 规则 |
| `qa/verify` 按交付台账验收 | 激活 QA、交付契约、前后端和前端测试规则 |

工具验证：

- `delivery-contract-check.mjs` 在业务项目内可验证交付台账。
- `acceptance-evidence-check.mjs` 在业务项目内可验证验收证据。
- 弱验收样例包含“接口 200 / 页面无异常 / 无报错 / 截图正常”时被拒绝。

发现并修复：

- `AGENTS.md` 固定写 `mango-cli init --preset full`，custom 项目会误导。已改为模板变量。
- `business-pmo/mango-baseline/README.md` 固定旧 commit。已改为生成时读取当前 Git HEAD，支持普通 clone 和 Git worktree。
- 业务入口文档未直接提示 `acceptance-evidence-check.mjs`。已补充到 `AGENTS.md`、`business-pmo/README.md` 和 baseline README。
