# Business PMO

## 1. 概览

`business-pmo` 是生成业务项目内置的 Mango PMO 工作区。CLI 从精确依赖的 `@mango/pmo` 安装同一份可复现 bundle，并把规则、Agent、模板、文档契约、检查器和项目级 Skill 一起锁定。

长期规则只在 Mango PMO 源仓维护；本目录保存发布快照和项目锁，不维护第二份规则正文。

| 目录 / 文件 | 作用 |
|-------------|------|
| `mango-baseline` | 当前项目锁定的 PMO baseline 快照 |
| `mango-baseline/baseline.json` | bundle 文件、权限、hash、契约版本和 plugin 投影清单 |
| `global-entity-exceptions.json` | 架构门禁显式读取的全局 Entity 例外清单，初始为空 |
| `pmo-lock.json` | 精确锁定 `@mango/pmo` 版本、bundle hash、源码提交和契约版本 |
| 项目根 `.agents/skills` | CLI 从锁定 bundle 同步的项目级交付 Skill |
| 项目根 `.agents/skills/.mango-pmo.json` | bundle-owned Skill 文件和 hash 清单 |
| 项目根 `AGENTS.md` | Agent 路由入口；只引用 baseline，不复制长期规则 |
| 项目根 `.mango` 下的 PMO 备份目录 | upgrade 和 rollback 使用的已校验本地备份 |

项目级 Skill 同步与用户级 Codex plugin 安装是两件事。CLI 只维护当前项目的 `.agents/skills`，不会修改用户级 Codex 配置。

## 2. 文档生命周期资产

正式产品文档按 BRD -> SRS -> TDD -> Plan 逐阶段收敛。每个阶段都从同一 PMO bundle 取得模板、专用 Agent、Skill、规则和 checker。

| 阶段 | 模板 | 专用 Agent | 项目 Skill | Checker | 规范 |
|------|------|------------|------------|---------|------|
| BRD | `templates/business-requirements.md` | `agents/business-requirements-agent.md` | `mango-requirements-business` | `tools/check-business-requirements.mjs` | `rules/product/01-business-requirements.md` |
| SRS | `templates/system-requirements.md` | `agents/system-requirements-agent.md` | `mango-requirements-system` | `tools/check-system-requirements.mjs` | `rules/product/02-system-requirements.md` |
| TDD | `templates/technical-design.md` | `agents/technical-design-agent.md` | `mango-design-technical` | `tools/check-technical-design.mjs` | `rules/product/03-technical-design.md` |
| Plan | `templates/implementation-plan.md` | `agents/implementation-plan-agent.md` | `mango-plan-implementation` | `tools/check-implementation-plan.mjs` | `rules/product/04-implementation-plan.md` |

跨阶段顺序、摘要、追踪和移交由以下资产统一处理：

- 编排 Skill：`mango-pmo-lifecycle`
- 生命周期 checker：`tools/check-lifecycle-handoff.mjs`
- 生命周期规范：`rules/product/05-document-lifecycle.md`
- 机器契约：`contracts/*.json`

旧 `prd.md`、`detailed-design.md` 和 delivery contract 资产只用于存量迁移，不是新产品文档链路入口。

## 3. PMO Bundle 操作

### 3.1 查看与校验

```bash
mango pmo status --project-dir .
mango pmo check --project-dir . --locked
```

`--locked` 以 `pmo-lock.json` 为准，同时校验 baseline、manifest、文件 hash/权限和项目级 Skill。未带 `--locked` 时，命令会对比当前 CLI 可用的 `@mango/pmo` 包，用于判断是否存在可升级版本。

### 3.2 修复当前锁

```bash
mango pmo sync --project-dir . --dry-run
mango pmo sync --project-dir .
```

`sync` 只修复当前项目锁定的 bundle，不隐式升版；它会恢复被修改或缺失的 bundle-owned 文件，并删除清单之外的陈旧 bundle-owned 文件。

### 3.3 显式升级

```bash
mango pmo upgrade --project-dir . --to {{mangoPmoVersion}} --dry-run
mango pmo upgrade --project-dir . --to {{mangoPmoVersion}}
mango pmo check --project-dir . --locked
```

`--to` 必须等于当前项目 CLI 精确依赖并能解析到的 `@mango/pmo` 版本。需要其它版本时，先使用依赖该版本的项目内 CLI。

### 3.4 回滚

```bash
mango pmo rollback --project-dir . --dry-run
mango pmo rollback --project-dir .
mango pmo rollback --project-dir . --to <version>
```

rollback 只使用项目根 `.mango` 下 PMO 运行时目录中的已校验本地备份，并在原子切换后再次执行 locked 校验。

## 4. Preflight

正式任务先校验项目锁，再执行 preflight：

```bash
mango pmo check --project-dir . --locked

node business-pmo/mango-baseline/tools/pmo-preflight.mjs \
  --role <pm|tech-lead|dev|qa|pmo> \
  --phase <requirement|design|develop|verify|release|governance> \
  --task "<任务>" \
  --paths "<影响路径，逗号分隔>"
```

Agent 读取输出中 `Must read` 的每一个文件原文。当前已经位于非 `main` 任务 worktree 时，preflight 会要求复用当前 worktree。

## 5. 文档检查与移交

各阶段文档保存在项目自己的 `business-docs`，不要写入 `mango-baseline`。单文档检查：

CI 和提交前扫描整个业务文档目录，自动识别四类生命周期文档，并检查合同、重复 ID、连续上游和摘要：

```bash
node business-pmo/mango-baseline/tools/check-document-set.mjs \
  --root business-docs
```

需要定位单个文档时执行：

```bash
node business-pmo/mango-baseline/tools/check-business-requirements.mjs \
  --document business-docs/requirements/order-brd.md

node business-pmo/mango-baseline/tools/check-system-requirements.mjs \
  --document business-docs/requirements/order-srs.md

node business-pmo/mango-baseline/tools/check-technical-design.mjs \
  --document business-docs/designs/order-tdd.md

node business-pmo/mango-baseline/tools/check-implementation-plan.mjs \
  --document business-docs/plans/order-plan.md
```

阶段移交只检查截至当前阶段的连续链路：

```bash
node business-pmo/mango-baseline/tools/check-lifecycle-handoff.mjs \
  --brd business-docs/requirements/order-brd.md \
  --srs business-docs/requirements/order-srs.md \
  --through srs \
  --risk L2
```

最终交付检查传入完整四阶段，不使用 `--through`：

```bash
node business-pmo/mango-baseline/tools/check-lifecycle-handoff.mjs \
  --brd business-docs/requirements/order-brd.md \
  --srs business-docs/requirements/order-srs.md \
  --tdd business-docs/designs/order-tdd.md \
  --plan business-docs/plans/order-plan.md \
  --risk L2
```

checker 通过不等于自动审批。阶段状态和 `NEXT` 还需要规范指定的人工审批证据。

## 6. 验收证据

验收证据写入 `business-docs/evidence`，并执行：

```bash
node business-pmo/mango-baseline/tools/acceptance-evidence-check.mjs \
  --evidence business-docs/evidence/order-acceptance.md \
  --min-rows 1
```

测试命令、环境、数据集、账号/租户标识、结果和报告路径保存在证据中，不写密码、token 或密钥。

## 7. 能力边界

- `mango-baseline` 和 bundle-owned `.agents/skills` 只通过 `mango pmo sync`、`mango pmo upgrade` 或 `mango pmo rollback` 更新。
- 业务需求文档、设计、计划、台账和证据保存在 `business-docs`。
- 业务源码、运行时配置、数据库 migration、菜单和权限资源保存在对应业务模块。
- 规则变更回到 Mango PMO 源仓处理；普通业务任务不直接修改发布快照。
- bundle 不替代业务模块 README、测试脚本、发布 runbook 或真实链路验收。

## 8. 问题排查

| 问题 | 原因 | 处理方式 |
|------|------|----------|
| `pmo-lock.json` 缺失 | 项目尚未迁移到锁定 bundle | 使用当前 CLI 执行 `mango pmo upgrade --to <version>` |
| locked check 报 baseline changed | 锁定快照被修改或缺失 | 执行 `mango pmo sync` 修复当前锁 |
| locked check 报 stale files | 旧 bundle 文件残留 | 执行 `mango pmo sync` 按 manifest 清理 |
| locked check 报 project Skill changed | bundle-owned Skill 被修改 | 执行 `mango pmo sync`；业务自有 Skill 使用其它名称 |
| sync 提示锁定版本不可用 | 当前 CLI 的 PMO 依赖与项目锁不一致，且没有本地备份 | 使用锁定版本的项目内 CLI，或显式 upgrade 到当前可用版本 |
| upgrade 拒绝 `--to` | 请求版本不是当前 CLI 可解析的精确 PMO 版本 | 安装匹配的 CLI 后重试 |
| rollback 无可用版本 | 本地没有对应已校验备份 | 使用匹配版本的 CLI 执行 upgrade，不能伪造备份 |
| Codex 中未出现用户级 plugin | 项目 Skill 同步不安装用户级 plugin | 对发布包的 package-root plugin 执行独立安装流程 |
| 文档 checker 失败 | 章节、字段、边界、ID 或证据不满足机器契约 | 按 rule ID 回到对应规范和模板修订 |
| 生命周期 hash 失效 | 上游文档在下游生成后发生变化 | 重新评审上游并重做受影响的下游移交 |

## 9. 相关入口

- Mango Baseline README：`business-pmo/mango-baseline/README.md`
- 产品文档生命周期规范：`business-pmo/mango-baseline/rules/product/05-document-lifecycle.md`
- PMO 总流程：`business-pmo/mango-baseline/rules/00-dev-flow.md`
- 文档资产边界：`business-pmo/mango-baseline/rules/06-document-assets.md`
- 测试用例与自动化流程：`business-pmo/mango-baseline/rules/09-test-case-automation-flow.md`
- 业务需求模板：`business-pmo/mango-baseline/templates/business-requirements.md`
- 系统需求模板：`business-pmo/mango-baseline/templates/system-requirements.md`
- 技术设计模板：`business-pmo/mango-baseline/templates/technical-design.md`
- 实施计划模板：`business-pmo/mango-baseline/templates/implementation-plan.md`
