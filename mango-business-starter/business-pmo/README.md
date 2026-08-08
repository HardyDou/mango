# Business PMO

## 1. 概览
`business-pmo` 是生成业务项目内置的 PMO 工作区，用来承载当前业务仓可执行的 Mango baseline 快照、业务交付规则入口、Agent 路由和交付检查工具。

目录定位：

| 目录 / 文件 | 作用 |
|-------------|------|
| `mango-baseline` | Mango PMO baseline 快照，包含规则、Agent、工具和模板 |
| `mango-baseline/rules/index.json` | preflight 路由索引 |
| `mango-baseline/tools/pmo-preflight.mjs` | 按 role、phase、task、paths 输出 References 与 Code baselines |
| `mango-baseline/tools/delivery-contract-check.mjs` | 校验设计和交付台账 |
| `mango-baseline/tools/acceptance-evidence-check.mjs` | 校验验收证据表 |
| `mango-baseline/tools/check-lean-document.mjs` | 校验当前 L2-L5 精简文档、直接追踪、引用和页数 |
| `mango-baseline/tools/check-document-set.mjs` | 读取哈希锁定的历史生命周期文档 |
| `architecture-debt-budget.json` | 项目自有的 schema v4 架构债务预算和不可变首次纳管审计；初始为空 |
| `global-entity-exceptions.json` | 业务架构门禁显式读取的全局 Entity 例外清单，初始为空 |
| 项目根 `.github/pull_request_template.md` | 业务仓自有 PR 说明；其中 Risk / Verification 区段由锁定 PMO 合同同步和检查 |
| 项目根 `AGENTS.md` | Agent 入口，只路由到 baseline，不复制长期规则正文 |

## 2. 功能清单

| 能力 | 使用入口 | 说明 |
|------|----------|------|
| 规则路由 | `mango-baseline/tools/pmo-preflight.mjs` | 按 role、phase、task、paths 输出按需参考与代码 baseline。 |
| 交付契约检查 | `delivery-contract-check.mjs` | 校验设计说明和交付台账。 |
| 验收证据检查 | `acceptance-evidence-check.mjs` | 校验验收证据表和弱表达。 |
| 精简文档门禁 | `check-lean-document.mjs` | 校验 L2-L4 单文档及 L5 四文档的结构、页数、引用、追踪和空话。 |
| 空白上下文路由 | `resolve-lean-document-policy.mjs` | 根据 L0-L5、强制 L5 事实和关键未知项确定文档形态或集中 ASK。 |
| 历史文档读取 | `check-document-set.mjs` | 只读取和校验已锁定的旧 BRD、SRS、TDD、实施计划。 |
| baseline 快照 | `mango-baseline/rules`、`agents`、`templates` | 业务仓脱离 Mango 源码后仍能读取规则。 |
| baseline 同步 | `mango pmo sync` | 从锁定 PMO bundle 同步 baseline、Risk / Verification 区段、入口和兼容脚本。 |

## 3. 能力边界
- 不作为 Mango 主仓长期规范源；长期规范仍由 Mango PMO 维护。
- 普通业务需求不直接修改 `mango-baseline/**`。
- 不保存业务源码、运行时配置、数据库 migration 或菜单权限资源。
- 不替代业务模块 README、业务设计文档、测试报告和发布 runbook。
- 不用来堆放临时日志、大文件截图或未归档运行产物。

## 4. 模块入口
`business-pmo/mango-baseline` 是可同步快照，业务项目自有文档应放在 baseline 外，例如 `business-docs/plans`、`business-docs/evidence` 或项目自定义 PMO 目录。

边界要求：

- baseline 内文件只通过 baseline 升级任务或 `mango pmo sync` 更新。
- `architecture-debt-budget.json` 位于 baseline 外，由完整 Reactor 报告受控递减；PMO sync、upgrade、rollback 均不得覆盖。
- 旧项目尚无预算时，先独立升级 PMO 与 CLI 托管的 GitHub/Gitea workflow，再用只含预算文件的治理 PR 从完整 Reactor 报告初始化；迁移豁免要求 inventory-only 完整报告、至少一个 workflow 变更且 diff 仅含 PMO 资产，业务源码/POM/配置都会失败。
- workflow 缺失时由 CLI 安装；历史标准 hash 或 Mango 托管文件可安全升级，未知定制默认拒绝覆盖，只有显式 `--adopt-governance` 才会备份并接管。
- 项目 PR 模板区段外内容由业务仓维护；`mango pmo sync/upgrade` 只托管 `## Risk / Verification`，重复区段必须人工合并。
- 业务需求的设计、台账、验收证据放到 `business-docs`。
- Agent 使用 preflight 输出的代码 baseline；References 只在边界不明确时定向查阅。
- 交付异常要写在业务交付记录中，不在 baseline 规则里临时改规则绕过。

## 5. 接入方式
preflight：

```bash
node business-pmo/mango-baseline/tools/pmo-preflight.mjs \
  --role dev \
  --phase develop \
  --task "新增订单管理模块" \
  --paths "backend,frontend,business-docs"
```

JSON 输出：

```bash
node business-pmo/mango-baseline/tools/pmo-preflight.mjs \
  --role qa \
  --phase verify \
  --task "订单模块 E2E 验收" \
  --paths "frontend,backend,business-docs/evidence" \
  --json
```

当前精简文档检查：

```bash
node business-pmo/mango-baseline/tools/check-lean-document.mjs \
  --document business-docs/delivery/task-17.md
```

交付台账检查：

```bash
node business-pmo/mango-baseline/tools/delivery-contract-check.mjs \
  --design business-docs/plans/order-design.md \
  --ledger business-docs/plans/order-ledger.md \
  --mode plan
```

验收证据检查：

```bash
node business-pmo/mango-baseline/tools/acceptance-evidence-check.mjs \
  --evidence business-docs/evidence/order-e2e.md \
  --min-rows 1
```

## 6. 配置说明
`business-pmo` 没有运行时配置。工具参数就是配置入口。

### 6.1 preflight 参数

| 参数 | 默认值 | 含义 | 影响行为 | 源码入口 |
|------|--------|------|----------|----------|
| `--role` | `auto` | 任务角色，支持 `pm`、`tech-lead`、`dev`、`qa`、`pmo` | 匹配 `rules/index.json` 的 `roles` | `pmo-preflight.mjs` |
| `--phase` | `auto` | 任务阶段，支持 `requirement`、`design`、`develop`、`verify`、`release`、`governance` | 匹配 `phases` | `pmo-preflight.mjs` |
| `--task` | 空 | 任务描述 | 与 bundle keywords 匹配 | `bundleMatches` |
| `--paths` | 空 | 影响路径，逗号分隔 | 与 bundle paths 匹配 | `splitPaths`、`pathMatches` |
| `--json` | `false` | 输出 JSON | 便于 Agent 或 CI 解析 | `parseArgs` |

### 6.2 delivery-contract-check 参数

| 参数 | 默认值 | 含义 | 影响行为 | 源码入口 |
|------|--------|------|----------|----------|
| `--design` | 空 | 设计说明文件 | 缺失时报错；用于校验 required item 是否存在 | `readFile` |
| `--ledger` | 空 | 交付台账文件 | 缺失时报错；必须含固定列 | `parseLedgerRows` |
| `--mode` | `plan` | `plan` 或 `verify` | verify 要求状态为 `DONE` 或 `EXCEPTION` | `checkRows` |
| `--require` | 空 | 必须覆盖的条目，逗号分隔 | 设计和台账都要命中 | `checkRequiredItems` |
| `--scan` | 空 | 扫描路径，逗号分隔 | 检查禁用词 | `checkForbidden` |
| `--forbidden` | 默认禁用词列表 | 禁用词 | 命中时报错 | `DEFAULT_FORBIDDEN` |
| `--json` | `false` | 输出 JSON | 便于 CI 解析 | `parseArgs` |

### 6.3 acceptance-evidence-check 参数

| 参数 | 默认值 | 含义 | 影响行为 | 源码入口 |
|------|--------|------|----------|----------|
| `--evidence` | 空 | 验收证据 Markdown 文件 | 缺失时报错 | `acceptance-evidence-check.mjs` |
| `--min-rows` | `1` | 证据表最少行数 | 行数不足时报错 | `parseArgs` |
| `--json` | `false` | 输出 JSON | 便于 CI 解析 | `parseArgs` |

## 7. API 与扩展
| 扩展点 | 可扩展内容 | 约束 |
|--------|------------|------|
| `rules/index.json` | role、phase、bundle、keyword、path 路由 | 只在 baseline 升级任务中改 |
| `rules/**` | PMO、后端、前端、测试、文档规则 | 不在普通业务需求中改 |
| `agents/**` | Agent 角色职责 | 不复制到项目根入口 |
| `templates/**` | 交付契约、验收证据模板 | baseline 升级统一维护 |
| `business-docs/**` | 业务设计、计划、台账、证据 | 业务团队维护 |

## 8. 数据与初始化
本目录不包含数据库 migration。初始化内容是 CLI 生成或同步的 Markdown、JSON 和 Node.js 工具文件。

| 类型 | 位置 | 初始化内容 | 幂等键 / 唯一键 | 生效时机 | 排查入口 |
|------|------|------------|-----------------|----------|----------|
| PMO baseline | `business-pmo/mango-baseline` | 规则、Agent、工具、代码模板 | 文件路径 | `mango init` 或 `mango pmo sync` | preflight 能输出 References 与 Code baselines |
| 业务计划示例 | `business-docs/plans` | example contract 和 ledger | 文件路径 | `mango init`；sync 时已有文件不覆盖 | delivery contract check |
| Agent 入口 | 项目根 `AGENTS.md` | 规则路由入口 | 文件路径 | `mango init` 或带参数 sync | 人工检查入口指向本仓 baseline |
| PR 风险合同 | 项目根 `.github/pull_request_template.md` | Risk / Verification 字段和填写提示 | 二级标题 | `mango init` 或 `mango pmo sync/upgrade` | `mango pmo check --locked` 报缺失或漂移 |
| 架构债务预算 | `business-pmo/architecture-debt-budget.json` | 空 schema v4 项目预算 | 模块 identity | 完整 Reactor governance 检查 | `backend/target/mango-architecture-report.json` 与预算检查器 |

## 9. 管理入口
本目录不提供菜单、权限资源或租户数据。涉及菜单、权限和租户时，preflight 会根据任务和路径命中后端模块、数据库、安全或菜单规则；实际资源应在业务模块的 migration、resource manifest、授权配置和测试证据中登记。

## 10. 快速开始
1. 正式变更前执行 preflight，选择 Code baselines；只在边界不明确时查阅具体 Reference。
2. 按 L0-L5 创建无文档、L2-L4 单文档或 L5 四文档，并用 `check-lean-document.mjs` 检查。
3. 开发和验证过程中把证据写入 `business-docs/evidence`。
4. 验证阶段执行 `acceptance-evidence-check.mjs`，避免只写“接口 200”“页面正常”。
5. 交付前执行 `delivery-contract-check.mjs --mode verify`，确认台账状态为 `DONE` 或有明确 `EXCEPTION`。
6. 最终回复列出实际加载的 baseline 文件、验证命令、未验证项和 PMO 例外。

### 10.1 存量业务仓升级

新 starter 已包含空的 `architecture-debt-budget.json`，不需要初始化迁移。旧业务仓缺少预算时必须依次使用独立 PR：

1. PMO/workflow 升级 PR：`mango pmo upgrade --project-dir . --to <version>`，不加 `--sync-shell`，不带业务文件；未知自定义 workflow 只有确认由 Mango 整文件托管后才加 `--adopt-governance`。
2. 预算初始化 PR：以 PR 的精确 base SHA 运行 `-Dmango.architecture.requireFullReactor=true -Dmango.architecture.inventoryOnly=true` 的完整后端 Reactor，再执行 `check-architecture-debt-budget.mjs --report backend/target/mango-architecture-report.json --baseline business-pmo/architecture-debt-budget.json --write`；diff 只新增预算文件。
3. 模块身份纳管 PR：只添加一个 starter `module.properties`，用同一完整 inventory 执行 `--onboard-module <moduleKey-prefix> --module-properties <path> --base-ref <base-sha> --reason "<已评审原因>" --write`；预算会保存不可变审计记录。
4. 业务开发 PR：模块纳管合并后才修改业务源码、POM、配置或数据库；普通 PR 不得抬高预算。

完整 Maven 参数、可信 CI 复验要求、rollback 边界和故障处理见由 CLI 生成的 `business-pmo/README.md`；PMO sync、upgrade、rollback 永远不覆盖项目预算。

## 11. 问题排查
| 问题 | 原因 | 处理方式 |
|------|------|----------|
| Code baselines 为空或不符合预期 | `--task` 和 `--paths` 太空泛，或尚无对应模板 | 写清任务关键词和影响路径，必要时补充标准模板 |
| preflight 报 Missing PMO file | `rules/index.json` 指向不存在文件 | 通过 baseline 同步修复 |
| verify 模式台账失败 | 台账状态仍是待处理或进行中 | 完成验收后改为 `DONE`，例外写 `EXCEPTION` 和证据 |
| 禁用词扫描失败 | 代码或文档仍有临时实现标记 | 删除临时实现或登记明确例外 |
| 验收证据被判弱表达 | 只写了“接口 200”“页面无异常”等泛化句 | 写具体测试数据、关键断言、UI 检查、network/console 结果和截图路径 |
| 普通需求改了 baseline | 把规则当成业务文档改了 | 还原 baseline，业务说明放入 `business-docs` |
| PR template missing / differs | 模板缺失或 Risk / Verification 仍是旧合同 | 执行项目内 `mango pmo sync --project-dir .`；已创建 PR 直接编辑正文 |

## 12. 相关文档
- [开发流程规范](./mango-baseline/rules/00-dev-flow.md)
- [交付契约规范](./mango-baseline/rules/01-delivery-contract.md)
- [AI 编码红线](./mango-baseline/rules/03-ai-coding-redlines.md)
- [交付质量门禁](./mango-baseline/rules/05-ai-delivery-quality.md)
- [文档资产规范](./mango-baseline/rules/06-document-assets.md)

- [Mango Baseline README](./mango-baseline/README.md)
- [交付契约模板](./mango-baseline/templates/delivery-contract.md)
- [验收证据模板](./mango-baseline/templates/acceptance-evidence.md)
