# Mango PMO Baseline

## 1. 概览

`mango-pmo` 是 Mango 的长期规范源，维护流程规则、角色定义、交付模板和 PMO 工具。业务项目不直接复制本目录，而是通过 `@mango/pmo` 和 `@mango/cli` 消费版本化快照。

生成到业务项目后的目录是 `business-pmo/mango-baseline`。它是可执行快照，不是业务需求、业务设计或验收证据的存放位置。

## 2. 功能清单

| 能力                   | 入口                                                                                                            | 说明                                                                                                                                                |
| ---------------------- | --------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| PMO preflight          | `tools/pmo-preflight.mjs`                                                                                       | 根据 role、phase、task、paths 输出 Must read 文件                                                                                                   |
| 交付契约检查           | `tools/delivery-contract-check.mjs`                                                                             | 校验设计说明和交付台账                                                                                                                              |
| 验收证据检查           | `tools/acceptance-evidence-check.mjs`                                                                           | 校验验收证据表和弱表达                                                                                                                              |
| 规则路由               | rules index JSON                                                                                                | 维护规则、角色、阶段和 bundle 映射                                                                                                                  |
| 角色定义               | `agents/**`                                                                                                     | PM、Tech Lead、Dev、QA、PMO 的职责说明                                                                                                              |
| 模板资产               | `templates/**`                                                                                                  | PRD、详细设计、交付契约、验收证据模板                                                                                                               |
| 交付模式               | `rules/11-delivery-assurance.md`、`contracts/delivery-assurance.json`、`skills/mango-design-delivery-assurance` | 自动隔离工作区，按 L0-L3 路由 SIMPLE、STANDARD、FULL；发布独立                                                                                      |
| 文档生命周期           | `contracts/*.json`、`tools/check-*-requirements.mjs`                                                            | STANDARD 检查单文件，FULL 对适用 BRD、SRS、TDD、实施计划执行结构、追踪和审批门禁                                                                    |
| 文档集合门禁           | `tools/check-document-set.mjs`                                                                                  | 扫描业务文档目录，阻断漏类型、未知类型、重复 ID、断链和失效摘要；仅允许用 `.mango-pmo-legacy-documents.json` 对合同启用前的历史文档做逐文件哈希锁定 |
| 风险与保障基线门禁     | `tools/risk-verification.mjs`                                                                                   | 校验需求影响、方案风险、二者最大值、人工确认的 M01-M16 精确值和已启用措施证据；不补固定套餐                                                         |
| CI 措施选择            | `tools/assurance-ci-scope.mjs`                                                                                  | 从已解析模式基线读取 M01-M16；CI 只执行事实启用且适用于 CI 的能力                                                                                   |
| CI 影响范围分类        | `tools/classify-pmo-check-scope.mjs`                                                                            | 按 Git 改动裁剪已启用检查的 PMO、Java、投影和 README 影响范围；只缩小范围，不替用户选择措施                                                         |
| 业务 PR 风险合同       | `contracts/delivery-assurance.json`、`templates/business-pull-request-template.md`、`tools/risk-verification.mjs` | 同一 schema 定义字段、canonical 模板、PR 正文校验和模板结构校验                                                                                    |
| 模块架构债务预算       | `tools/check-architecture-debt-budget.mjs`                                                                      | 比较完整 Reactor 报告与 Git 基准，阻断新增、替换、跨模块迁移和预算回升，并支持按模块查询、递减                                                      |
| 专项 Agent             | `agents/*-requirements-agent.md`、`agents/technical-design-agent.md`、`agents/implementation-plan-agent.md`     | 一个生命周期模板对应一个撰写 Agent                                                                                                                  |
| 可安装 Skills          | `skills/**`                                                                                                     | 保障方案确认、生命周期协调、按需文档、工程、QA、Issue、模块、发布和 PR review                                                                       |
| 全局实体例外           | `contracts/global-entity-exceptions.json`                                                                       | 按 Entity/table/owner/审批/到期日管理精确例外                                                                                                       |
| Mango 主仓分支保护策略 | `.github/branch-protection-policy.json`、`tools/branch-protection-policy.mjs`                                   | 声明并校验单 Owner 或多人维护模式，同时固定 Required Check 和历史保护项                                                                             |

Skill 按实际能力命名，而不是按发布包命名：只有治理编排使用 `mango-pmo-lifecycle`；需求、设计、工程、QA、Issue、模块、评审和发布分别使用各自领域名称，禁止统一套用含义不清的 `mango-pm-*` 前缀。

## 3. 接入方式

业务项目通过 `@mango/cli` 提供的 `mango pmo ...` 命令管理 baseline。全局 CLI 只用于创建项目、历史项目升级和临时诊断：

```bash
npm view @mango/pmo@1.3.4 version --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/
npm view @mango/cli@1.0.88 version --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/
npm install -g @mango/cli@1.0.88 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/
```

两个 `npm view` 都返回精确版本后再执行安装。返回 404 表示该批次仍未发布，源码仓可见不等于业务项目已经可消费。

生成后的业务项目以项目内锁定版本为准：先执行 `cd frontend && pnpm install` 安装 `frontend` 中声明的 `@mango/cli`，再通过 `pnpm exec mango workspace ...`、`pnpm exec mango dev ...` 和 `pnpm exec mango frontend ...` 执行本地开发命令。系统 `PATH` 上的 `mango` 可能落后，不能作为业务项目版本依据。`scripts/dev-workspace.sh` 只保留为旧命令兼容入口。

历史项目如果还没有兼容脚本或项目内 CLI，可以先使用全局 CLI 执行 `mango pmo upgrade --project-dir . --sync-shell`，把 baseline、Agent 入口和兼容脚本升级到当前版本；升级后回到 `frontend` 安装项目内依赖，并用 `pnpm exec mango ...` 执行日常命令。

常用 baseline 命令：

```bash
mango pmo status --project-dir .
mango pmo check --project-dir .
mango pmo upgrade --project-dir . --to 1.3.4 --dry-run
mango pmo upgrade --project-dir . --to 1.3.4 --sync-shell
mango pmo check --project-dir . --locked
```

升级成功后，规则、合同、模板和 Agent 位于 `business-pmo/mango-baseline`，项目 Skill 位于 `.agents/skills`，版本身份位于 `business-pmo/pmo-lock.json`。项目级 Skill 已足够支持业务仓内的 Agent 路由，不要求同时安装用户级 Codex plugin。

业务仓日常命令使用项目内 CLI：

```bash
cd frontend
pnpm install
pnpm exec mango pmo check --project-dir .. --locked
pnpm exec mango workspace init
pnpm exec mango dev doctor
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

| 配置入口                                           | 字段                                                    | 含义                                                                     |
| -------------------------------------------------- | ------------------------------------------------------- | ------------------------------------------------------------------------ |
| rules index JSON                                   | `always`                                                | 每次 preflight 固定加载的规则                                            |
| rules index JSON                                   | `roles`                                                 | 按角色加载的规则                                                         |
| rules index JSON                                   | `phases`                                                | 按阶段加载的规则                                                         |
| rules index JSON                                   | `bundles`                                               | 按关键词和路径加载的规则包                                               |
| `baseline.json`                                    | `packageVersion`                                        | 业务仓当前 baseline 包版本                                               |
| `baseline.json`                                    | `files[].sha256`                                        | 业务仓 baseline 漂移检查依据                                             |
| 业务仓 `mango.config.json`                         | `paths.backend`、`paths.frontend`、`paths.businessDocs` | 声明后端、前端和业务文档的仓库相对目录，供 GitHub/Gitea 标准门禁统一解析 |
| Mango 主仓 `.github/branch-protection-policy.json` | `governanceMode`、Required Check、review 和历史保护字段 | GitHub `main` 分支保护的受版本控制期望状态                               |

## 5. API 与扩展

| API / 扩展点                         | 输入                                                        | 输出                                                                                          |
| ------------------------------------ | ----------------------------------------------------------- | --------------------------------------------------------------------------------------------- |
| `pmo-preflight.mjs`                  | role、phase、task、paths                                    | Must read、workspace policy、required checks                                                  |
| `delivery-contract-check.mjs`        | design、ledger、mode                                        | 台账覆盖和状态检查结果                                                                        |
| `acceptance-evidence-check.mjs`      | evidence、min rows                                          | 验收证据表检查结果                                                                            |
| `check-document-set.mjs`             | business docs root                                          | 自动发现并检查目录内生命周期文档及其上游关系                                                  |
| `risk-verification.mjs`              | PR Markdown body                                            | 风险最大值、人工确认的措施值、启用证据和停用剩余风险                                          |
| `assurance-ci-scope.mjs`             | 已通过保障基线合同的 PR Markdown body                       | M01-M16 是否由策略、事实或人工例外启用的 CI 输出                                              |
| `mango-design-delivery-assurance`    | 当前目标、范围和事实                                        | 工作区策略、风险定级、三档模式和充分验证基线                                                  |
| `classify-pmo-check-scope.mjs`       | Git base/head                                               | PMO/后端/投影/README 布尔范围、Maven `none`、`partial`、`governance` 模式和 project selectors |
| `check-architecture-debt-budget.mjs` | 完整 Reactor 报告、当前预算、可选 Git base ref 或模块选择器 | 全局或模块债务比较、递减要求和稳定身份差异                                                    |
| `@mango/pmo`                         | `dist/baseline.json`、`dist/baseline/**`                    | 可发布 PMO baseline 包                                                                        |
| `@mango/pmo` plugin projection       | `.codex-plugin/plugin.json`、`skills/**`                    | 与 npm 包同版本的 Codex plugin/Skill 投影                                                     |
| `mango pmo check`                    | business project root                                       | baseline 漂移状态                                                                             |
| `mango pmo upgrade`                  | business project root                                       | 已升级 baseline 快照                                                                          |

## 6. 数据与初始化

本目录不包含数据库 migration、菜单、权限、租户或业务初始化数据。

| 类型              | 位置                                                | 初始化方式                               |
| ----------------- | --------------------------------------------------- | ---------------------------------------- |
| baseline 快照     | `business-pmo/mango-baseline`                       | `mango init` 或 `mango pmo sync/upgrade` |
| baseline manifest | `business-pmo/mango-baseline/baseline.json`         | `@mango/pmo` build 生成                  |
| 业务文档          | `business-docs/**`                                  | 业务项目自行维护                         |
| 本地端口和 DB     | `.mango/workspace.json`、`.mango/dev-workspace.env` | `mango workspace init` 分配              |

## 7. 管理入口

本目录没有页面菜单和后端管理接口。管理入口是 CLI 和 PMO 工具：

| 任务                  | 命令                                                                                       |
| --------------------- | ------------------------------------------------------------------------------------------ |
| 检查业务仓 baseline   | `mango pmo check --project-dir .`                                                          |
| 升级历史业务 baseline | `mango pmo upgrade --project-dir .`                                                        |
| 输出任务规则          | `node business-pmo/mango-baseline/tools/pmo-preflight.mjs ...`                             |
| 检查交付台账          | `node business-pmo/mango-baseline/tools/delivery-contract-check.mjs ...`                   |
| 检查全部业务文档      | `node business-pmo/mango-baseline/tools/check-document-set.mjs --root business-docs`       |
| 检查全局架构债务预算  | `node mango-pmo/tools/check-architecture-debt-budget.mjs --base-ref <base-sha>`            |
| 检查单个模块债务      | `node mango-pmo/tools/check-architecture-debt-budget.mjs --module <moduleKey\|artifactId>` |

## 8. 快速开始

1. 在 Mango 主仓修改 `mango-pmo/**`。
2. 执行 `pnpm -F @mango/pmo build` 生成 package baseline。
3. 执行 `pnpm -F @mango/pmo check` 校验包内工具。
4. 在业务项目执行 `mango pmo upgrade --project-dir . --dry-run` 查看升级计划。
5. 确认后执行 `mango pmo upgrade --project-dir .`。

## 9. 问题排查

| 问题                         | 原因                                         | 处理方式                                                       |
| ---------------------------- | -------------------------------------------- | -------------------------------------------------------------- |
| `mango pmo check` 报 changed | baseline 文件被手改或版本落后                | 确认不是业务需求改动后执行 `mango pmo upgrade --project-dir .` |
| `pmo check --locked` 报 PR template missing / differs | 项目缺少 PR 模板或 Risk 区段与锁定合同漂移 | 使用项目内 CLI 执行 `mango pmo sync --project-dir .`；重复区段先人工合并 |
| preflight Missing PMO file   | rules index JSON 指向不存在文件              | 修复 `mango-pmo` 源并重新发布 baseline                         |
| 业务路径未命中规则           | rules index JSON bundle paths 不覆盖业务目录 | 在 mango-pmo rules index 补充路径                              |
| 历史项目仍引用主仓路径       | 旧 `AGENTS.md` 未升级                        | 执行 `mango pmo upgrade --project-dir . --write-agents`        |
| `npm view` 返回 404          | 目标 PMO/CLI 批次尚未发布                    | 等待发布状态机完成并从 npm-group 回查后再升级业务项目          |

## 10. 相关文档

- [PMO 总流程](./rules/00-dev-flow.md)
- [开发环境规范](./rules/02-dev-environment.md)
- [AI 编码红线](./rules/03-ai-coding-redlines.md)
- [AI 交付质量门禁](./rules/05-ai-delivery-quality.md)
- [Mango Issue 登记 Runbook](./rules/07-mango-issue-runbook.md)
