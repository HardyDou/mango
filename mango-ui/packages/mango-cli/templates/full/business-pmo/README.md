# Business PMO

## 1. 概览

`business-pmo` 是生成业务项目内置的 Mango PMO 工作区。CLI 从精确依赖的 `@mango/pmo` 安装同一份可复现 bundle，并把规则、Agent、模板、文档契约、检查器和项目级 Skill 一起锁定。

长期规则只在 Mango PMO 源仓维护；本目录保存发布快照和项目锁，不维护第二份规则正文。

| 目录 / 文件 | 作用 |
|-------------|------|
| `mango-baseline` | 当前项目锁定的 PMO baseline 快照 |
| `mango-baseline/baseline.json` | bundle 文件、权限、hash、契约版本和 plugin 投影清单 |
| `architecture-debt-budget.json` | 项目自有的 schema v4 架构债务预算与首次纳管审计记录；初始为空，不属于可同步 baseline |
| `global-entity-exceptions.json` | 架构门禁显式读取的全局 Entity 例外清单，初始为空 |
| `pmo-lock.json` | 精确锁定 `@mango/pmo` 版本、bundle hash、源码提交和契约版本 |
| 项目根 `.agents/skills` | CLI 从锁定 bundle 同步的项目级交付 Skill |
| 项目根 `.agents/skills/.mango-pmo.json` | bundle-owned Skill 文件和 hash 清单 |
| 项目根 `AGENTS.md` | Agent 路由入口；只引用 baseline，不复制长期规则 |
| 项目根 `.github/pull_request_template.md` | 业务仓自有 PR 说明；其中 Risk / Verification 区段由锁定 PMO 合同同步和检查 |
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

`--locked` 以 `pmo-lock.json` 为准，同时校验 baseline、manifest、文件 hash/权限、项目级 Skill 和 PR 风险合同区段。未带 `--locked` 时，命令会对比当前 CLI 可用的 `@mango/pmo` 包，用于判断是否存在可升级版本。

### 3.2 修复当前锁

```bash
mango pmo sync --project-dir . --dry-run
mango pmo sync --project-dir .
```

`sync` 只修复当前项目锁定的 bundle，不隐式升版；它会恢复被修改或缺失的 bundle-owned 文件，并删除清单之外的陈旧 bundle-owned 文件。GitHub/Gitea 的 `pmo-doc-check.yml` 是整文件托管资产：缺失时创建，带 Mango 托管标识或与已知历史标准版本 hash 完全一致时升级；无法识别的业务自定义 workflow 默认拒绝覆盖。确认要把自定义文件完整交给 Mango 管理时才使用 `--adopt-governance`，原文件会进入项目 `.mango` 下的 PMO 备份目录。delivery-assurance schema revision 5 起，PR 模板缺失时创建，存在时只同步 `## Risk / Verification` 区段；重复区段必须人工合并后重跑。

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
回滚会恢复当次备份中的 GitHub/Gitea workflow 和 PR 模板，但不会修改项目自有的 `architecture-debt-budget.json`。

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

`References` 只在边界不明确时定向查阅；代码生成优先使用 `Code baselines`。当前已经位于非 `main` 任务 worktree 时，preflight 会要求复用当前 worktree。

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

### 5.1 风险与验证

PR 模板分别记录需求影响和解决方案风险，最终等级取二者最大值，并记录交付模式、工作区决策、不可降级事实和适用 M01-M16 的选择、理由、证据与剩余风险。BRD/SRS 记录影响预评，TDD 固化最终等级，Plan 原样继承；L0/L1 不生成空的四阶段文档，方案升到 L2/L3 时在实施前切换适用流程。

M09-M16 只按真实观察面启用，不要求为未触发措施填写跳过理由。例如只移动按钮位置且行为不变时可以选择 M09 静态验证与 M13 UI 验证；后端租户/事务结果由真实集成或 API 入口证明，没有浏览器入口时不添加 M13。

```bash
node business-pmo/mango-baseline/tools/risk-verification.mjs \
  --body .pr-body.md
```

生成项目的 `pmo-doc-check` 始终产生 required check 结果，但只在 `mango.config.json` 的 `paths.backend` 指向的后端路径受影响时启动 Java。普通后端质量门禁由 `classify-pmo-check-scope.mjs` 选择直接修改的 Maven 模块，不使用 `-am` 或 `-amd` 扩大 Reactor；依赖构建和消费者兼容性作为独立验证，根 POM、架构验证模块和全局架构输入才使用完整 Reactor。GitHub 与 Gitea 模板共用这套范围判定；Gitea 对已关闭或已合并 PR 的 `edited` 事件只执行正文合同检查，不再构造无效 diff。

### 5.2 存量模块首次纳管

`architecture-debt-budget.json` 是业务仓自己的递减预算，不随 `mango pmo sync/upgrade` 覆盖。门禁启用前已经存在、但缺少 `module.properties` 的模块必须先建立独立纳管 PR；该 PR 只允许目标 starter 的身份文件和此预算文件。先用 PR 的精确 base SHA 生成完整 inventory，再写入审计记录：

旧业务仓尚无项目预算时，先用 `mango pmo upgrade --dry-run` 检查并升级 PMO bundle 和 GitHub/Gitea workflow。迁移门禁只在预算确实缺失、变更至少包含一个托管 workflow、且 diff 仅含 PMO/Skill/PR 模板时允许这一 PR；夹带任何业务文件都会失败。合并后立即建立一个只新增 `business-pmo/architecture-debt-budget.json` 的治理 PR，以未修改业务源码的完整报告执行 `--write` 初始化。初始化 CI 同样要求完整报告，`--baseline-only` 或夹带其它文件都会失败。新生成的干净项目已经携带空预算，不需要此迁移 PR；已有非空预算永远不会被 sync、upgrade 或 rollback 覆盖。

旧业务仓按以下四个独立 PR 顺序执行，前一个合并后再从目标分支创建下一个：

1. PMO/workflow 升级 PR：执行 `mango pmo upgrade --project-dir . --to {{mangoPmoVersion}}`，不加 `--sync-shell`，不带业务文件。未知自定义 workflow 只有确认整文件交给 Mango 管理后才加 `--adopt-governance`。
2. 预算初始化 PR：现场运行下方完整 Reactor inventory 命令，再执行 checker 的 `--report ... --baseline ... --write`；diff 只新增预算文件。
3. 模块身份纳管 PR：只添加一个 starter `module.properties` 并用 `--onboard-module` 更新预算；每个 PR 只纳管一个明确模块范围。
4. 业务开发 PR：纳管合并后再修改业务源码、POM、配置或数据库；普通 PR 只校验预算不可抬高。

预算初始化命令：

```bash
BASE_SHA="$(git rev-parse origin/main)"

mvn -B -ntp -f backend/pom.xml \
  -DskipTests \
  -Dmango.architecture.mode=changed \
  -Dmango.architecture.base="$BASE_SHA" \
  -Dmango.architecture.skip=false \
  -Dmango.architecture.requireFullReactor=true \
  -Dmango.architecture.inventoryOnly=true \
  -Dmango.check.rule=all \
  -Dmango.check.gate=no-new-violations \
  -Dmango.check.changedOnly=true \
  -Dmango.check.baseRef="$BASE_SHA" \
  -Dmango.check.requireFullScope=true \
  -Dmango.check.staticFailurePolicy=block \
  verify

node business-pmo/mango-baseline/tools/check-architecture-debt-budget.mjs \
  --report backend/target/mango-architecture-report.json \
  --baseline business-pmo/architecture-debt-budget.json \
  --write
```

提交前用 `git diff --name-only "$BASE_SHA"...HEAD` 检查允许的文件集合。required check 必须在可信 CI 用 PR 的真实 base SHA 重建报告，不能复用开发者上传产物。

模块身份纳管使用同一条完整 Reactor 命令，然后执行：

```bash
node business-pmo/mango-baseline/tools/check-architecture-debt-budget.mjs \
  --report backend/target/mango-architecture-report.json \
  --baseline business-pmo/architecture-debt-budget.json \
  --onboard-module <moduleKey-prefix> \
  --module-properties <starter-module-properties-path> \
  --base-ref "$BASE_SHA" \
  --reason "<reviewed reason>" \
  --write
```

GitHub/Gitea required check 会在 governance 模式现场重建完整报告并复验预算；普通业务 PR 只做 base 预算不可变检查和受影响模块门禁。纳管 PR 合并后才能在第二个 PR 开发业务代码。

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
| locked check 报 PR template missing / differs | 项目模板缺失或 Risk / Verification 与锁定合同漂移 | 执行项目内 `mango pmo sync --project-dir .`；重复区段先人工合并，已创建 PR 直接编辑正文 |
| rollback 无可用版本 | 本地没有对应已校验备份 | 使用匹配版本的 CLI 执行 upgrade，不能伪造备份 |
| Codex 中未出现用户级 plugin | 项目 Skill 同步不安装用户级 plugin | 对发布包的 package-root plugin 执行独立安装流程 |
| 文档 checker 失败 | 章节、字段、边界、ID 或证据不满足机器契约 | 按 rule ID 回到对应规范和模板修订 |
| 生命周期 hash 失效 | 上游文档在下游生成后发生变化 | 重新评审上游并重做受影响的下游移交 |
| 首次纳管提示 `onboarding-report-required` | CI 只执行了 baseline-only，或项目工作流仍是旧版本 | 升级 GitHub/Gitea PMO workflow，并用完整 Reactor inventory 执行普通 `--base-ref` 复验 |
| 初始化项目预算提示 `initial-budget-report-required` | 旧项目第一次提交预算时没有可信完整报告 | 在独立预算初始化 PR 现场生成完整 Reactor 报告；不得用 baseline-only 或夹带业务代码 |

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
