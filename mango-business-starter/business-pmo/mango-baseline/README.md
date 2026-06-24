# Mango PMO Baseline

## 1. 概览
`mango-pmo` 是 Mango 的长期规范源，维护流程规则、角色定义、交付模板和 PMO 工具。业务项目不直接复制本目录，而是通过 `@mango/pmo` 和 `@mango/cli` 消费版本化快照。

生成到业务项目后的目录是 `business-pmo/mango-baseline`。它是可执行快照，不是业务需求、业务设计或验收证据的存放位置。

## 2. 功能清单
| 能力 | 入口 | 说明 |
|------|------|------|
| PMO preflight | `tools/pmo-preflight.mjs` | 根据 role、phase、task、paths 输出 Must read 文件 |
| 交付契约检查 | `tools/delivery-contract-check.mjs` | 校验设计说明和交付台账 |
| 验收证据检查 | `tools/acceptance-evidence-check.mjs` | 校验验收证据表和弱表达 |
| 规则路由 | rules index JSON | 维护规则、角色、阶段和 bundle 映射 |
| 角色定义 | `agents/**` | PM、Tech Lead、Dev、QA、PMO 的职责说明 |
| 模板资产 | `templates/**` | PRD、详细设计、交付契约、验收证据模板 |

## 3. 接入方式
业务项目通过 `@mango/cli` 提供的 `mango pmo ...` 命令管理 baseline。推荐在开发机全局安装 CLI，用于创建项目、历史项目升级和临时诊断：

```bash
npm install -g @mango/cli --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/
```

生成后的业务项目仍以项目内锁定版本为准：先执行 `cd frontend && pnpm install` 安装 `frontend` 中声明的 `@mango/cli`，再通过 `scripts/dev-workspace.sh ...` 执行本地开发命令。脚本会优先使用项目内 CLI；项目依赖未安装时才回退到全局 `mango`。

历史项目如果还没有兼容脚本或项目内 CLI，可以先使用全局 CLI 执行 `mango pmo upgrade --project-dir .`，把 baseline、Agent 入口和兼容脚本升级到当前版本。

常用 baseline 命令：

```bash
mango pmo status --project-dir .
mango pmo check --project-dir .
mango pmo sync --project-dir .
mango pmo upgrade --project-dir .
```

正式任务前执行：

```bash
node business-pmo/mango-baseline/tools/pmo-preflight.mjs \
  --role dev \
  --phase develop \
  --task "新增业务模块" \
  --paths "backend,frontend,business-docs"
```

## 4. 配置说明
| 配置入口 | 字段 | 含义 |
|----------|------|------|
| rules index JSON | `always` | 每次 preflight 固定加载的规则 |
| rules index JSON | `roles` | 按角色加载的规则 |
| rules index JSON | `phases` | 按阶段加载的规则 |
| rules index JSON | `bundles` | 按关键词和路径加载的规则包 |
| `baseline.json` | `packageVersion` | 业务仓当前 baseline 包版本 |
| `baseline.json` | `files[].sha256` | 业务仓 baseline 漂移检查依据 |

## 5. API 与扩展
| API / 扩展点 | 输入 | 输出 |
|--------------|------|------|
| `pmo-preflight.mjs` | role、phase、task、paths | Must read、workspace policy、required checks |
| `delivery-contract-check.mjs` | design、ledger、mode | 台账覆盖和状态检查结果 |
| `acceptance-evidence-check.mjs` | evidence、min rows | 验收证据表检查结果 |
| `@mango/pmo` | `dist/baseline.json`、`dist/baseline/**` | 可发布 PMO baseline 包 |
| `mango pmo check` | business project root | baseline 漂移状态 |
| `mango pmo upgrade` | business project root | 已升级 baseline 快照 |

## 6. 数据与初始化
本目录不包含数据库 migration、菜单、权限、租户或业务初始化数据。

| 类型 | 位置 | 初始化方式 |
|------|------|------------|
| baseline 快照 | `business-pmo/mango-baseline` | `mango init` 或 `mango pmo sync/upgrade` |
| baseline manifest | `business-pmo/mango-baseline/baseline.json` | `@mango/pmo` build 生成 |
| 业务文档 | `business-docs/**` | 业务项目自行维护 |
| 本地端口和 DB | `.mango/dev-workspace.env` | `mango init-dev` 分配 |

## 7. 管理入口
本目录没有页面菜单和后端管理接口。管理入口是 CLI 和 PMO 工具：

| 任务 | 命令 |
|------|------|
| 检查业务仓 baseline | `mango pmo check --project-dir .` |
| 升级历史业务 baseline | `mango pmo upgrade --project-dir .` |
| 输出任务规则 | `node business-pmo/mango-baseline/tools/pmo-preflight.mjs ...` |
| 检查交付台账 | `node business-pmo/mango-baseline/tools/delivery-contract-check.mjs ...` |

## 8. 快速开始
1. 在 Mango 主仓修改 `mango-pmo/**`。
2. 执行 `pnpm -F @mango/pmo build` 生成 package baseline。
3. 执行 `pnpm -F @mango/pmo check` 校验包内工具。
4. 在业务项目执行 `mango pmo upgrade --project-dir . --dry-run` 查看升级计划。
5. 确认后执行 `mango pmo upgrade --project-dir .`。

## 9. 问题排查
| 问题 | 原因 | 处理方式 |
|------|------|----------|
| `mango pmo check` 报 changed | baseline 文件被手改或版本落后 | 确认不是业务需求改动后执行 `mango pmo upgrade --project-dir .` |
| preflight Missing PMO file | rules index JSON 指向不存在文件 | 修复 `mango-pmo` 源并重新发布 baseline |
| 业务路径未命中规则 | rules index JSON bundle paths 不覆盖业务目录 | 在 mango-pmo rules index 补充路径 |
| 历史项目仍引用主仓路径 | 旧 `AGENTS.md` 未升级 | 执行 `mango pmo upgrade --project-dir . --write-agents` |

## 10. 相关文档
- [PMO 总流程](./rules/00-dev-flow.md)
- [开发环境规范](./rules/02-dev-environment.md)
- [AI 编码红线](./rules/03-ai-coding-redlines.md)
- [AI 交付质量门禁](./rules/05-ai-delivery-quality.md)
- [Mango Issue 登记 Runbook](./rules/07-mango-issue-runbook.md)
