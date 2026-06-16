# Business PMO

## 1. 概览
`business-pmo` 是生成业务项目内置的 PMO 工作区，用来承载当前业务仓可执行的 Mango baseline 快照、业务交付规则入口、Agent 路由和交付检查工具。

目录定位：

| 目录 / 文件 | 作用 |
|-------------|------|
| `mango-baseline` | Mango PMO baseline 快照，包含规则、Agent、工具和模板 |
| `mango-baseline/rules/index.json` | preflight 路由索引 |
| `mango-baseline/tools/pmo-preflight.mjs` | 按 role、phase、task、paths 输出 Must read |
| `mango-baseline/tools/delivery-contract-check.mjs` | 校验设计和交付台账 |
| `mango-baseline/tools/acceptance-evidence-check.mjs` | 校验验收证据表 |
| 项目根 `AGENTS.md` | Agent 入口，只路由到 baseline，不复制长期规则正文 |

## 2. 功能清单

| 能力 | 使用入口 | 说明 |
|------|----------|------|
| 规则路由 | `mango-baseline/tools/pmo-preflight.mjs` | 按 role、phase、task、paths 输出 Must read。 |
| 交付契约检查 | `delivery-contract-check.mjs` | 校验设计说明和交付台账。 |
| 验收证据检查 | `acceptance-evidence-check.mjs` | 校验验收证据表和弱表达。 |
| baseline 快照 | `mango-baseline/rules`、`agents`、`templates` | 业务仓脱离 Mango 源码后仍能读取规则。 |
| baseline 同步 | `mango pmo sync` | 从 CLI 模板同步 baseline、入口和兼容脚本。 |

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
- 业务需求的设计、台账、验收证据放到 `business-docs`。
- Agent 每次正式交付前读取 preflight 输出的 Must read 文件原文。
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
| PMO baseline | `business-pmo/mango-baseline` | 规则、Agent、工具、模板 | 文件路径 | `mango init` 或 `mango pmo sync` | preflight 能输出 Must read |
| 业务计划示例 | `business-docs/plans` | example contract 和 ledger | 文件路径 | `mango init`；sync 时已有文件不覆盖 | delivery contract check |
| Agent 入口 | 项目根 `AGENTS.md` | 规则路由入口 | 文件路径 | `mango init` 或带参数 sync | 人工检查入口指向本仓 baseline |

## 9. 管理入口
本目录不提供菜单、权限资源或租户数据。涉及菜单、权限和租户时，preflight 会根据任务和路径命中后端模块、数据库、安全或菜单规则；实际资源应在业务模块的 migration、resource manifest、授权配置和测试证据中登记。

## 10. 快速开始
1. 需求开始前执行 preflight，按输出读取 Must read 文件原文。
2. 设计或交付任务创建 design 和 ledger，并用 `delivery-contract-check.mjs --mode plan` 检查列和覆盖项。
3. 开发和验证过程中把证据写入 `business-docs/evidence`。
4. 验证阶段执行 `acceptance-evidence-check.mjs`，避免只写“接口 200”“页面正常”。
5. 交付前执行 `delivery-contract-check.mjs --mode verify`，确认台账状态为 `DONE` 或有明确 `EXCEPTION`。
6. 最终回复列出实际加载的 baseline 文件、验证命令、未验证项和 PMO 例外。

## 11. 问题排查
| 问题 | 原因 | 处理方式 |
|------|------|----------|
| Must read 为空或不符合预期 | `--task` 和 `--paths` 太空泛 | 写清任务关键词和影响路径 |
| preflight 报 Missing PMO file | `rules/index.json` 指向不存在文件 | 通过 baseline 同步修复 |
| verify 模式台账失败 | 台账状态仍是 `TODO` 或 `IN_PROGRESS` | 完成验收后改为 `DONE`，例外写 `EXCEPTION` 和证据 |
| 禁用词扫描失败 | 代码或文档仍有 mock、TODO 等标记 | 删除临时实现或登记明确例外 |
| 验收证据被判弱表达 | 只写了“接口 200”“页面无异常”等泛化句 | 写具体测试数据、关键断言、UI 检查、network/console 结果和截图路径 |
| 普通需求改了 baseline | 把规则当成业务文档改了 | 还原 baseline，业务说明放入 `business-docs` |

## 12. 相关文档
- [开发流程规范](./mango-baseline/rules/00-dev-flow.md)
- [交付契约规范](./mango-baseline/rules/01-delivery-contract.md)
- [AI 编码红线](./mango-baseline/rules/03-ai-coding-redlines.md)
- [交付质量门禁](./mango-baseline/rules/05-ai-delivery-quality.md)
- [文档资产规范](./mango-baseline/rules/06-document-assets.md)

- [Mango Baseline README](./mango-baseline/README.md)
- [交付契约模板](./mango-baseline/templates/delivery-contract.md)
- [验收证据模板](./mango-baseline/templates/acceptance-evidence.md)
