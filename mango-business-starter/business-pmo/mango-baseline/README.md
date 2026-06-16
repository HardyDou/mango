# Mango PMO Baseline

Source:

- Mango commit: generated from current Mango source
- Mango CLI version: generated from current `@mango/cli`
- Synced at: generated during `mango pmo sync`

## 1. 概览
`business-pmo/mango-baseline` 是业务项目模板携带的 Mango PMO 可执行快照，提供 preflight、交付台账检查、规则索引、Agent 角色说明和交付契约模板。

核心资产：

| 资产 | 作用 |
|------|------|
| `rules/index.json` | preflight 路由索引 |
| `rules/**` | PMO、后端、前端、产品、测试相关规则 |
| `agents/**` | PM、Tech Lead、Dev、QA、PMO 角色职责 |
| `tools/pmo-preflight.mjs` | 输出任务必须阅读的规则文件 |
| `tools/delivery-contract-check.mjs` | 校验设计说明和交付台账 |
| `templates/delivery-contract.md` | 交付契约模板 |

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 业务仓正式开发、验证、提交和发布前执行规则路由 | CLI / 模板 / 生成产物 |
| Agent 交付前确认必须读取的 baseline 文件 | CLI / 模板 / 生成产物 |
| 业务设计和交付台账需要校验固定列、状态和覆盖项 | CLI / 模板 / 生成产物 |
| Mango baseline 升级时整体同步规则和工具 | CLI / 模板 / 生成产物 |

## 3. 适用场景
- 业务仓正式开发、验证、提交和发布前执行规则路由。
- Agent 交付前确认必须读取的 baseline 文件。
- 业务设计和交付台账需要校验固定列、状态和覆盖项。
- Mango baseline 升级时整体同步规则和工具。

## 4. 边界说明
- 不作为业务需求、业务设计、业务证据的存放目录。
- 不随普通业务需求手改规则和工具。
- 不替代 Mango 主仓 `mango-pmo` 的长期规范源。
- 不直接初始化数据库、菜单、权限、租户或业务数据。
- 不代替后端测试、前端构建、E2E、性能基准或发布检查。

## 5. 模块组成
baseline 是快照，不是业务工作区。业务项目可以读取和执行它，但不应在普通业务任务中修改它。

边界：

- 长期规则演进在 Mango 主仓完成。
- 业务仓通过 CLI 或 baseline 升级任务同步。
- 业务自己的计划、设计、台账、证据放在 `business-docs/**`。
- 如果 baseline 和业务现实冲突，先在交付记录中登记例外，再发起 baseline 升级。

## 6. 接入方式
从业务项目根目录执行：

```bash
node business-pmo/mango-baseline/tools/pmo-preflight.mjs \
  --role dev \
  --phase develop \
  --task "新增订单管理模块" \
  --paths "backend,frontend,business-docs"
```

交付契约检查：

```bash
node business-pmo/mango-baseline/tools/delivery-contract-check.mjs \
  --design business-docs/plans/order-design.md \
  --ledger business-docs/plans/order-ledger.md \
  --mode verify
```

## 7. 配置说明
### 6.1 rules/index.json

| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 | 源码入口 |
|----------|------------|--------|------|----------|----------|
| `rules/index.json` | `version` | `1` | 规则索引版本 | 供工具识别索引结构 | `rules/index.json` |
| `rules/index.json` | `always` | dev flow、delivery contract、AI redlines | 全局必读规则 | 每次 preflight 输出 | `buildResult` |
| `rules/index.json` | `rules` | 规则 key 到文件路径映射 | 规则注册表 | role、phase、bundle 引用这些 key | `addRule` |
| `rules/index.json` | `roles` | `pm`、`tech-lead`、`dev`、`qa`、`pmo` | 角色必读规则 | 根据 `--role` 输出 | `buildResult` |
| `rules/index.json` | `phases` | requirement、design、develop、verify、release、governance | 阶段必读规则 | 根据 `--phase` 输出 | `buildResult` |
| `rules/index.json` | `bundles` | backend、frontend、pmo、devEnvironment、product 等 | 关键词和路径规则包 | 根据 `--task` 和 `--paths` 命中 | `bundleMatches` |

### 6.2 preflight 参数

| 参数 | 默认值 | 含义 | 影响行为 | 源码入口 |
|------|--------|------|----------|----------|
| `--role` | `auto` | 当前角色 | 匹配 role 规则 | `parseArgs` |
| `--phase` | `auto` | 当前阶段 | 匹配 phase 规则 | `parseArgs` |
| `--task` | 空 | 任务描述 | 匹配 bundle keywords | `bundleMatches` |
| `--paths` | 空 | 影响路径，逗号分隔 | 支持精确路径和 `/**` 前缀匹配 | `splitPaths`、`pathMatches` |
| `--json` | `false` | JSON 输出 | 输出 `mustRead` 和 `errors` JSON | `parseArgs` |

### 6.3 delivery-contract-check 参数

| 参数 | 默认值 | 含义 | 影响行为 | 源码入口 |
|------|--------|------|----------|----------|
| `--design` | 空 | 设计说明文件 | 缺失或不存在时报错 | `readFile` |
| `--ledger` | 空 | 交付台账文件 | 必须是 Markdown 表格 | `parseLedgerRows` |
| `--mode` | `plan` | `plan` 或 `verify` | verify 要求完成状态 | `checkRows` |
| `--require` | 空 | 必须覆盖的条目 | 同时检查设计和台账 | `checkRequiredItems` |
| `--scan` | 空 | 扫描路径 | 递归扫描禁用词 | `checkForbidden` |
| `--forbidden` | 临时实现、未完成标记等 | 禁用词 | 命中时报错 | `DEFAULT_FORBIDDEN` |
| `--json` | `false` | JSON 输出 | 输出 summary 和 errors | `parseArgs` |

交付台账必须包含列：`ID`、`来源`、`要求`、`设计决策`、`交付物`、`验收方式`、`状态`、`证据文件`。

## 8. API 与扩展
| 接口 / 扩展点 | 输入 | 输出 | 说明 |
|---------------|------|------|------|
| `pmo-preflight.mjs` | role、phase、task、paths | Must read、errors | 正式任务前必须执行 |
| `delivery-contract-check.mjs` | design、ledger、mode、require、scan | summary、errors | plan 和 verify 两种模式 |
| `rules/index.json` | rule key、bundle、path、keyword | preflight 路由结果 | baseline 升级时维护 |
| `templates/delivery-contract.md` | 设计和台账模板 | 业务设计说明起点 | 复制到 `business-docs` 后填写 |

## 9. 数据与初始化
本目录不包含数据库 migration，也不直接初始化菜单、权限或租户。

| 类型 | 位置 | 初始化内容 | 幂等键 / 唯一键 | 生效时机 | 排查入口 |
|------|------|------------|-----------------|----------|----------|
| baseline 快照 | `business-pmo/mango-baseline` | 规则、工具、Agent、模板 | 文件路径 | 生成业务项目或 baseline 同步 | preflight 能运行 |
| 规则索引 | `rules/index.json` | role、phase、bundle 路由 | rule key | baseline 同步时 | JSON 可解析，Must read 文件存在 |
| 模板资产 | `templates/delivery-contract.md` | 交付契约模板 | 文件路径 | baseline 同步时 | delivery check 可识别复制后的表格 |

## 10. 管理入口
本目录不提供菜单、权限资源或租户数据。相关任务通过 preflight 命中规则：

| 任务类型 | 命中方式 | 应读取规则 |
|----------|----------|------------|
| 菜单初始化、resource manifest、默认权限 | backend bundle 的关键词或路径 | 后端模块、数据库、安全规则 |
| 认证、授权、token、租户、数据权限 | security bundle 的关键词 | 后端安全和 API 规则 |
| 数据库表、migration、租户字段 | backend bundle 的路径或关键词 | 数据库和持久化规则 |
| 前端页面、按钮、Element Plus UI | frontend bundle 的路径或关键词 | 前端开发、UI、测试规则 |

实际菜单权限数据必须在业务模块 README、resource manifest、migration、授权配置和验收证据中登记。

## 11. 快速开始
1. Agent 或开发者接到正式任务后，从项目根目录执行 preflight。
2. 逐个读取输出的 Must read 文件原文。
3. 设计任务创建或更新交付契约和台账，并执行 plan 模式检查。
4. 开发任务按规则补齐代码、README、migration、菜单权限和测试。
5. 交付前执行 verify 模式台账检查；未完成项必须是 `EXCEPTION`，并有证据或用户确认。
6. 最终回复列出实际读取的 baseline 文件、验证命令、未验证项和例外。

## 12. 问题排查
| 问题 | 原因 | 处理方式 |
|------|------|----------|
| Must read 没命中后端规则 | `--paths` 没写 backend 路径，或 task 没有关键词 | 补充影响路径或明确任务描述 |
| preflight 输出 Missing PMO file | index 中 path 指向不存在文件 | 通过 baseline 同步修复 |
| verify 模式失败 | 台账还有未开始或进行中状态 | 完成验收后改 `DONE`，例外改 `EXCEPTION` 并写证据 |
| 禁用词扫描报错 | 交付物仍有临时实现、未完成或未来优化等标记 | 删除临时内容或登记明确例外 |
| baseline 和主仓不一致 | 当前业务仓快照未升级 | 使用 baseline 升级任务同步 |

## 13. 相关文档
- [开发流程规范](./rules/00-dev-flow.md)
- [交付契约规范](./rules/01-delivery-contract.md)
- [开发环境规范](./rules/02-dev-environment.md)
- [AI 编码红线](./rules/03-ai-coding-redlines.md)
- [后端模块规范](./rules/backend/05-module.md)
- [前端组件规范](./rules/frontend/03-component-development.md)

## 14. 历史资料
- [Business PMO README](../README.md)
- [交付契约模板](./templates/delivery-contract.md)
