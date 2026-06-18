# Payment 模块样式规范治理交付台账

## 1. 目标

修复 Payment 在单体后台组合部署时 header 样式丢失的问题，并把官方 admin 模块的页面注册、样式聚合、CLI 模块清单和 PR/preflight 门禁做成通用验收，避免以后其它官方模块重复出现同类问题。

## 2. 范围

- `@mango/payment` 接入 `@mango/admin/full` 和 `@mango/admin/style-full.css`。
- `@mango/admin` package 样式聚合、依赖声明、full 注册入口和构建配置。
- `mango-cli` full/custom 模块清单和生成项目校验。
- PMO 前端规则、交付质量门禁和 preflight 必跑检查提示。
- 通用官方模块样式治理脚本。
- Payment 页面 E2E 作为本次缺陷回归样本。

## 3. 不做范围

- 不修改 Payment 业务接口、数据库或支付流程。
- 不改变微前端隔离策略；微前端仍由子应用显式引入自身样式。
- 不把业务模块 JS 入口改成自动 import 全局 CSS。

## 4. 设计输入

- 用户要求：同时满足微前端部署、单体部署和灵活组合部署；单体时使用极简，配置模块即可，不需要到处手工 import。
- 用户要求：Payment 不符合规范就改 Payment，并加强 PR 质量门禁和 preflight 前置规范。
- 前端规范：`mango-pmo/rules/frontend/03-component-development.md`、`mango-pmo/rules/frontend/06-monorepo-architecture.md`。
- 交付质量规范：`mango-pmo/rules/05-ai-delivery-quality.md`。

## 5. 设计说明

- 单体 full preset：业务项目使用 `@mango/admin/full` 加 `@mango/admin/style-full.css`，官方 full 模块的页面注册和样式由 admin 聚合入口提供。
- 灵活 custom preset：`mango-cli` 按模块清单生成 package 依赖、样式 import 和 registrar import。
- 微前端：子应用继续显式引入自身模块样式，不依赖宿主样式穿透。
- 模块治理：新增 `pnpm admin:module-styles:check`，检查所有官方 admin 模块，不只检查 Payment。
- 页面回归：Payment 是本次线上反复出现样式丢失的缺陷样本，因此 E2E 增加 computed style 断言。

## 6. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| TASK-001 | 用户要求 / 前端规范 | 官方模块接入不能只靠 `apps/*` 私有入口手工 import 样式 | 建立通用官方模块治理脚本，覆盖 full 注册、admin 依赖、样式聚合、CLI 清单和 package style export | `mango-ui/scripts/check-admin-module-style-governance.mjs`、`mango-ui/package.json` | `pnpm admin:module-styles:check` | DONE | 本地命令通过，覆盖 9 个官方模块 |
| TASK-002 | 用户要求 / PR 门禁 | PR 和 preflight 前置必须强化这类规范 | PMO 规则要求 PR 必跑样式门禁；preflight 输出 `Required checks` | `mango-pmo/rules/**`、`mango-pmo/tools/pmo-preflight.mjs`、`check-pmo-preflight.mjs` | `node mango-pmo/tools/check-pmo-preflight.mjs` | DONE | 本地命令通过 |
| TASK-003 | Payment 缺陷回归 | Payment header 样式不能再靠 admin app 私有 import 兜底 | Payment 接入 admin full、style-full、admin style manifest 和 CLI optional module | `mango-ui/packages/admin/**`、`mango-ui/packages/mango-cli/**`、`mango-ui/apps/mango-admin/src/main.ts` | `pnpm admin:styles:check`、`pnpm -F @mango/cli test`、`pnpm -F @mango/admin build` | DONE | 本地命令通过 |
| TASK-004 | 页面验收 | 本次缺陷要有页面级样式回归断言 | 在 Payment 列表布局 E2E 中检查 header computed style；Payment 只作为本次缺陷回归样本，通用模块验收由 TASK-001 覆盖 | `mango-ui/apps/mango-admin/e2e/specs/payment-center.spec.ts` | Payment E2E 目标用例 | EXCEPTION | 已运行目标用例，失败于 `http://127.0.0.1:7834/#/login` 连接拒绝，未进入页面断言；需启动本地 Payment E2E 服务和隔离库后复跑 |
| TASK-005 | 文档说明 | 业务使用必须极简，full preset 不再单独 import Payment 样式 | README 说明 full preset 自动带入 Payment 页面注册和样式 | `mango-ui/packages/admin/README.md`、`mango-ui/packages/payment/README.md` | 文档审阅 | DONE | 本文件 |

## 7. 已执行验证记录

| 命令 | 结果 | 说明 |
| --- | --- | --- |
| `pnpm install --no-frozen-lockfile` | PASS | 同步新增 `@mango/payment` peer 依赖到 lockfile |
| `pnpm admin:styles` | PASS | admin 样式聚合生成通过 |
| `pnpm admin:styles:check` | PASS | admin 样式聚合检查通过 |
| `pnpm admin:module-styles:check` | PASS | 通用官方模块样式治理检查通过，覆盖 9 个官方模块 |
| `node mango-pmo/tools/check-pmo-preflight.mjs` | PASS | preflight 自检通过，包含 Required checks 断言 |
| `pnpm -F @mango/cli test` | PASS | CLI full/custom/add/module/pmo sync 检查通过 |
| `pnpm -F @mango/payment build` | PASS | Payment package 构建通过 |
| `pnpm -F @mango/admin build` | PASS | admin full/style 聚合构建通过 |
| `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PAYMENT_E2E_ALLOW_SHARED_DB_MUTATION=true pnpm -F mango-admin exec playwright test --config playwright.config.ts e2e/specs/payment-center.spec.ts --project=chromium --grep "支付中心搜索区、列表区和分页区样式结构一致" --reporter=list` | EXCEPTION | 本机未启动 `http://127.0.0.1:7834`，`page.goto('/#/login')` 连接拒绝 |

## 8. 风险与限制

- Payment E2E 目标用例需要真实本地前后端服务和隔离测试库；本次代码层已补 computed style 断言，并已确认无服务时会在登录页连接阶段失败，运行通过证据需在 E2E 环境可用后补充。
- `admin:module-styles:check` 当前以官方模块清单为准；新增官方模块时必须同步加入该脚本，否则不能算完成模块集成。
