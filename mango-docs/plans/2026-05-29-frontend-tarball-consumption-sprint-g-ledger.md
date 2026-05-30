# 前端发布物 Tarball 消费 Sprint G 台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| FSG-001 | Sprint G | 发布包不携带源码和测试资产 | 可发布 `@mango/*` 包统一配置 `files: ["dist"]` | `mango-ui/packages/*/package.json` | `pnpm package:check` | DONE | `Package contract check passed: 15 packages checked.` |
| FSG-002 | Sprint G | peer dependency 不因正常小版本升级产生 Mango 警告 | Vue、Element Plus、Pinia、Vue I18n 使用兼容范围 | `mango-ui/packages/*/package.json` | 干净生成项目 `pnpm install --ignore-scripts` 并扫描 `unmet peer` | DONE | `/tmp/mango-sprint-g-e2e-clean/install.out` 无 `unmet peer`，只剩第三方 deprecated 警告 |
| FSG-003 | Sprint G | 15 个 Mango 前端包可生成 tarball | 使用 `pnpm --filter <pkg> pack --pack-destination /tmp/mango-sprint-g-pack` | `/tmp/mango-sprint-g-pack/*.tgz` | 循环执行 15 个包的 `pnpm pack` | DONE | `/tmp/mango-sprint-g-pack` 生成 15 个 `.tgz` |
| FSG-004 | Sprint G | tarball 内容干净 | 扫描 tarball 条目，禁止 `src`、测试文件、`tsconfig`、`vite.config` | `/tmp/mango-sprint-g-pack/*.tgz` | `tar -tzf` 内容扫描脚本 | DONE | `Tarball content check passed: 15 packages.` |
| FSG-005 | Sprint G | 外部生成项目可通过 tarball 安装 Mango 物料 | 在 `/tmp/mango-sprint-g-e2e-clean/tarball-platform` 使用 tarball overrides | 生成项目 `node_modules` 和 lockfile | `node scripts/check-template.mjs && pnpm install --ignore-scripts` | DONE | 模板检查通过；安装成功；无 Mango peer dependency 警告 |
| FSG-006 | Sprint G | tarball 外部项目可类型检查 | 业务 app 只通过发布物公开入口消费类型 | 生成项目 frontend workspace | `pnpm typecheck` | DONE | `/tmp/mango-sprint-g-e2e-clean/tarball-platform` 3 个 workspace 项目类型检查通过 |
| FSG-007 | Sprint G | tarball 外部项目可生产构建 | Admin Shell、内置能力和业务页面从 tarball 进入生产构建 | 生成项目 `dist` | `pnpm build` | DONE | `pnpm build` 成功；保留第三方 PURE 注释和 chunk 体积警告 |
| FSG-008 | Sprint G | tarball 外部项目可浏览器冒烟 | 启动 admin app，验证首页和 mixed 微前端诊断 | 生成项目前端 dev server | `pnpm --filter tarball-platform-admin dev --host 127.0.0.1 --port 5197`，Playwright 访问 | DONE | 首页跳转 `#/home` 且 runtime config load 无错误；`#/guarantee/letters` 显示 `Failed to load Mango micro app` 诊断；截图 `/tmp/mango-sprint-g-e2e-clean/tarball-platform/frontend-smoke.png` |
| FSG-009 | Sprint G | 回归测试通过 | 复跑 starter、CLI、包契约、Admin Pages/Admin Shell 单测、核心包构建 | `mango-ui`、`mango-business-starter` | `check-template`、`check-cli`、`package:check`、单测、核心包构建 | DONE | Sprint F 后续回归已通过；本 Sprint 补跑 `pnpm package:check` 通过 |
| FSG-010 | Sprint G | 真实 registry 发布链路明确状态 | tarball 已证明发布物可消费，但未发布到 Nexus/npm registry | npm/Nexus registry | 直接 registry install 不在本 Sprint 执行 | EXCEPTION | Sprint F 已记录 public npm 404；Sprint G 只验证 tarball，不代表 Nexus 发布与下载 |
| FSG-011 | Sprint G | 后端和真实登录权限链路明确状态 | 本 Sprint 只处理前端发布物消费，不补后端 app | 后端 app、真实认证与权限菜单 | 后端测试、真实登录浏览器验收不在本 Sprint 执行 | EXCEPTION | 沿用 Sprint F 限制：生成项目无根 POM、后端 app、数据库初始化和真实登录权限链路 |
| FSG-012 | PMO | 交付台账通过检查 | 本台账记录 Sprint G 可证明项和例外项 | 本文件 | `node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/plans/2026-05-29-frontend-tarball-consumption-sprint-g.md --ledger mango-docs/plans/2026-05-29-frontend-tarball-consumption-sprint-g-ledger.md --mode verify` | DONE | 交付台账检查通过 |
