# Business PMO

## 1. 概览
`business-pmo` 是业务项目模板中的 PMO 工作区，用来承载业务仓自己的交付流程入口和 Mango baseline 快照。它让生成后的业务仓在脱离 Mango 源码后，仍能执行 preflight、读取规则、校验交付台账。

目录定位：

| 目录 / 文件 | 作用 |
|-------------|------|
| `mango-baseline` | Mango PMO baseline 快照 |
| `mango-baseline/rules/index.json` | preflight 规则路由索引 |
| `mango-baseline/tools/pmo-preflight.mjs` | 输出 Must read 规则文件 |
| `mango-baseline/tools/delivery-contract-check.mjs` | 校验设计和交付台账 |
| 项目根 `AGENTS.md` | Agent 入口，路由到业务仓 baseline |

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 生成业务项目后执行 PMO preflight | CLI / 模板 / 生成产物 |
| 业务需求、设计、开发、验证和发布需要交付契约和台账 | CLI / 模板 / 生成产物 |
| 业务仓需要在本仓保存业务计划、设计、证据和例外记录 | CLI / 模板 / 生成产物 |
| 业务仓升级 Mango baseline 时同步规则、Agent 和工具 | CLI / 模板 / 生成产物 |


## 3. 能力边界
- 不作为 Mango 长期规范源。
- 不随普通业务需求直接修改 `mango-baseline/**`。
- 不保存业务源码、数据库 migration、菜单权限资源或运行时配置。
- 不替代业务模块 README、业务设计文档和测试报告。

## 4. 模块入口
`business-pmo/mango-baseline` 是可同步快照；业务自有文档放在 baseline 外，例如 `business-docs/plans` 和 `business-docs/evidence`。

边界要求：

- baseline 内规则和工具只通过 baseline 升级任务同步。
- 普通业务需求只改业务文档、业务代码和业务测试。
- Agent 每次正式交付前读取 preflight 输出的 Must read 文件原文。
- 规则冲突或例外写入业务交付记录，不在 baseline 里临时改规则绕过。

## 5. 接入方式
preflight：

```bash
node business-pmo/mango-baseline/tools/pmo-preflight.mjs \
  --role dev \
  --phase develop \
  --task "新增订单管理模块" \
  --paths "backend,frontend,business-docs"
```

交付台账 plan 检查：

```bash
node business-pmo/mango-baseline/tools/delivery-contract-check.mjs \
  --design business-docs/plans/order-design.md \
  --ledger business-docs/plans/order-ledger.md \
  --mode plan
```

交付台账 verify 检查：

```bash
node business-pmo/mango-baseline/tools/delivery-contract-check.mjs \
  --design business-docs/plans/order-design.md \
  --ledger business-docs/plans/order-ledger.md \
  --mode verify
```

## 6. 配置说明
本目录没有运行时配置。工具参数和 `rules/index.json` 是配置入口。

| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 | 源码入口 |
|----------|------------|--------|------|----------|----------|
| `rules/index.json` | `always` | dev flow、delivery contract、AI redlines | 全局必读规则 | 每次 preflight 输出 | `pmo-preflight.mjs` |
| `rules/index.json` | `roles` | `pm`、`tech-lead`、`dev`、`qa`、`pmo` | 角色规则 | 根据 `--role` 匹配 | `pmo-preflight.mjs` |
| `rules/index.json` | `phases` | requirement、design、develop、verify、release、governance | 阶段规则 | 根据 `--phase` 匹配 | `pmo-preflight.mjs` |
| `rules/index.json` | `bundles` | backend、frontend、pmo、devEnvironment、product 等 | 关键词和路径规则包 | 根据 `--task` 和 `--paths` 匹配 | `pmo-preflight.mjs` |
| preflight 参数 | `--task` | 空 | 任务描述 | 匹配 bundle keywords | `bundleMatches` |
| preflight 参数 | `--paths` | 空 | 影响路径，逗号分隔 | 匹配 bundle paths | `pathMatches` |
| delivery 参数 | `--mode` | `plan` | `plan` 或 `verify` | verify 要求台账完成 | `delivery-contract-check.mjs` |
| delivery 参数 | `--require` | 空 | 必须覆盖的条目 | 同时检查设计和台账 | `checkRequiredItems` |
| delivery 参数 | `--scan` | 空 | 扫描路径 | 检查禁用词 | `checkForbidden` |

## 7. API 与扩展
| 扩展点 | 可扩展内容 | 约束 |
|--------|------------|------|
| `mango-baseline/rules/index.json` | role、phase、bundle、keyword、path 路由 | baseline 升级任务中维护 |
| `mango-baseline/rules/**` | 后端、前端、测试、文档、产品规则 | 普通业务需求不改 |
| `mango-baseline/agents/**` | Agent 角色职责 | 不复制到业务文档 |
| `mango-baseline/templates/delivery-contract.md` | 交付契约模板 | 复制到 `business-docs` 后填写 |
| `business-docs/**` | 业务设计、计划、台账、证据 | 业务团队维护 |

## 8. 数据与初始化
本目录不包含数据库 migration。初始化内容是模板生成的规则、工具和 README。

| 类型 | 位置 | 初始化内容 | 幂等键 / 唯一键 | 生效时机 | 排查入口 |
|------|------|------------|-----------------|----------|----------|
| PMO baseline | `business-pmo/mango-baseline` | 规则、Agent、工具、模板 | 文件路径 | 生成业务项目或 baseline 同步 | preflight 能输出 Must read |
| 业务计划示例 | `business-docs/plans` | example contract 和 ledger | 文件路径 | 生成业务项目 | delivery check 能解析 |
| Agent 入口 | 项目根 `AGENTS.md` | 规则路由入口 | 文件路径 | 生成业务项目 | 人工检查入口指向本仓 baseline |

## 9. 管理入口
本目录不提供菜单、权限资源或租户数据。涉及菜单、权限和租户时，preflight 会根据任务和路径命中后端模块、数据库、安全或前端规则；实际资源在业务模块的 migration、resource manifest、授权配置和验收证据中登记。

## 10. 快速开始
1. 需求开始前执行 preflight，按输出读取 Must read 文件原文。
2. 创建设计说明和交付台账，并用 `delivery-contract-check.mjs --mode plan` 检查列和覆盖项。
3. 开发过程中把证据和例外写入 `business-docs`。
4. 交付前执行 `delivery-contract-check.mjs --mode verify`，确认状态为 `DONE` 或有明确 `EXCEPTION`。
5. 最终回复列出实际加载的 baseline 文件、验证命令、未验证项和 PMO 例外。

## 11. 问题排查
| 问题 | 原因 | 处理方式 |
|------|------|----------|
| Must read 不符合预期 | `--task` 和 `--paths` 太空泛 | 写清任务关键词和影响路径 |
| preflight 报 Missing PMO file | `rules/index.json` 指向不存在文件 | 通过 baseline 同步修复 |
| verify 模式失败 | 台账状态仍是未开始或进行中 | 完成后改 `DONE`，例外写 `EXCEPTION` 和证据 |
| 禁用词扫描失败 | 代码或文档仍有临时实现标记 | 删除临时实现或登记明确例外 |
| 普通需求改了 baseline | 把规则当成业务文档改了 | 还原 baseline，业务说明放入 `business-docs` |

## 12. 相关文档
- [开发流程规范](./mango-baseline/rules/00-dev-flow.md)
- [交付契约规范](./mango-baseline/rules/01-delivery-contract.md)
- [AI 编码红线](./mango-baseline/rules/03-ai-coding-redlines.md)
- [开发环境规范](./mango-baseline/rules/02-dev-environment.md)

## 13. 补充资料
- [Mango Baseline README](./mango-baseline/README.md)
- [交付契约模板](./mango-baseline/templates/delivery-contract.md)
