# 前端发布源 Registry 消费 Sprint H 台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| FSH-001 | Sprint H | 提供可重复 registry 外部消费验收入口 | 新增 `pnpm package:registry-e2e`，封装 registry 发布、安装、构建和浏览器冒烟 | `mango-ui/package.json`、`mango-ui/scripts/registry-consumption-e2e.mjs` | `pnpm package:registry-e2e -- --evidence-dir /tmp/mango-sprint-h-evidence` | DONE | `/tmp/mango-sprint-h-evidence/summary.md` |
| FSH-002 | Sprint H | 发布源验收不能依赖源码 link 或 tarball overrides | 使用临时 Verdaccio registry，生成项目通过 `.npmrc` 从 registry 安装 `@mango/*` | `registry-consumption-e2e.mjs` | 查看生成项目 install 输出和 lockfile 来源 | DONE | `/tmp/mango-sprint-h-evidence/install.out` |
| FSH-003 | Sprint H | 自动发布 15 个 Mango 前端包 | 脚本按 Mango 包内部依赖拓扑排序，并用 staging package 去除源码目录依赖 | 临时 registry storage | `npm publish` 与 registry metadata 校验 | DONE | `/tmp/mango-sprint-h-evidence/summary.md` |
| FSH-004 | Sprint H | registry 安装无 Mango peer 警告 | 安装后扫描 `install.out` 中 `unmet peer` | `/tmp/mango-sprint-h-evidence/install.out` | 脚本内置扫描 | DONE | `/tmp/mango-sprint-h-evidence/install.out` |
| FSH-005 | Sprint H | registry 外部项目可类型检查和生产构建 | 生成 mixed 模式业务项目并执行 `pnpm typecheck`、`pnpm build` | `/tmp` 生成项目 | `pnpm package:registry-e2e` | DONE | `/tmp/mango-sprint-h-evidence/summary.md` |
| FSH-006 | Sprint H | registry 外部项目可浏览器冒烟 | 启动生成项目 admin app，验证 `#/home` 和微前端缺失诊断 | `/tmp/mango-sprint-h-evidence/frontend-smoke.png` | Playwright 浏览器冒烟 | DONE | `/tmp/mango-sprint-h-evidence/frontend-smoke.png` |
| FSH-007 | Sprint H | 回归测试通过后才能收尾 | 复跑 starter、CLI、包契约、Admin Pages/Admin Shell 单测、核心包构建 | `mango-ui`、`mango-business-starter` | `check-template`、`check-cli`、`package:check`、单测、核心包构建 | DONE | `pnpm package:registry-e2e -- --evidence-dir /tmp/mango-sprint-h-evidence` |
| FSH-008 | Sprint H | 真实 Nexus 状态明确 | 本 Sprint 用临时 registry 证明协议链路，真实 Nexus 需有凭证和版本策略后执行 | Nexus/npm registry | 不在本 Sprint 发布真实版本 | EXCEPTION | 真实 Nexus 凭证、仓库权限和版本号发布策略未提供 |
| FSH-009 | Sprint H | 后端和真实登录权限链路明确状态 | 本 Sprint 只处理前端发布源消费，不补后端 app | 后端 app、真实认证与权限菜单 | 后端测试、真实登录浏览器验收不在本 Sprint 执行 | EXCEPTION | 沿用 Sprint F/G 限制：生成项目无根 POM、后端 app、数据库初始化和真实登录权限链路 |
| FSH-010 | PMO | 交付台账通过检查 | 本台账记录 Sprint H 可证明项和例外项 | 本文件 | `node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/plans/2026-05-29-registry-consumption-sprint-h.md --ledger mango-docs/plans/2026-05-29-registry-consumption-sprint-h-ledger.md --mode verify` | DONE | `node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/plans/2026-05-29-registry-consumption-sprint-h.md --ledger mango-docs/plans/2026-05-29-registry-consumption-sprint-h-ledger.md --mode verify` |
