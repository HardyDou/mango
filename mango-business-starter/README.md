# Mango Business Starter

## 1. 能力定位

`mango-business-starter` 是 Mango 源仓中的业务项目模板资产和回退模板，面向业务模块模板、业务 PMO baseline 下发和单体/微服务拓扑说明。主要使用者是 `@mango/cli`、Mango 维护者和业务项目开发者。

代码事实：

- 后端模板目录：`backend/modules/{{moduleKebab}}`。
- 前端应用模板：`frontend/apps/{{projectKebab}}-admin`。
- 前端 API 包模板：`frontend/packages/{{moduleKebab}}-api`。
- 前端页面包模板：`frontend/packages/{{moduleKebab}}`。
- 模板校验脚本：`scripts/check-template.mjs`。
- 业务 PMO baseline：`business-pmo/mango-baseline`。

`mango init --preset full` 当前实际读取 `mango-ui/packages/mango-cli/templates/full`；`mango module add` 在存在 `mango-ui/packages/mango-cli/templates/business-module` 时优先使用 CLI 内置业务模块模板，否则回退到本目录。本文描述的是本目录模板资产本身，不等同于所有 CLI full 初始化产物结构。

## 2. 适用场景

- 维护业务模块模板资产和 business starter 回退模板。
- 校验业务模块 `api/core/starter/starter-remote` 模板结构。
- 校验业务前端 API 包、页面包和后台应用接入模板。
- 将 Mango PMO baseline 带入业务仓库，使业务项目脱离 Mango 源码后仍可执行 preflight 和交付台账检查。

## 3. 不适用场景

- 不作为运行时 starter 依赖使用。
- 不承载 Mango 平台模块源码。
- 不替代业务项目后续的领域建模、数据库设计和验收设计。
- 不自动修复已生成项目中的业务自定义代码。

## 4. 模块边界

本目录提供模板资产、回退模板和静态校验。`mango-ui/packages/mango-cli` 负责选择 CLI 内置模板或本目录回退模板、替换变量和执行命令；生成后的业务项目由业务仓库维护。

## 5. 接入方式

通过 CLI 使用时，先以 CLI README 和 CLI 内置模板为准：

```bash
mango init <project> --preset full --topology monolith
mango add <module>
mango module add <module> --aggregate <name>
```

核心变量包括：

- `{{projectKebab}}`、`{{projectPascal}}`
- `{{moduleKebab}}`、`{{modulePackage}}`、`{{modulePascal}}`、`{{moduleCamel}}`
- `{{aggregateKebab}}`、`{{aggregatePascal}}`、`{{aggregateCamel}}`
- `{{basePackage}}`、`{{basePackagePath}}`

## 6. 配置项

本模板自身未发现运行时配置项。生成后的项目配置由 `mango.dev.json`、`.mango/dev-workspace.env`、各应用 `package.json`、Maven `pom.xml` 和业务配置文件承载。

## 7. 对外接口 / 扩展点

后端模板：

- 聚合模块包含 `{{moduleKebab}}-api`、`{{moduleKebab}}-core`、`{{moduleKebab}}-starter`、`{{moduleKebab}}-starter-remote`。
- API 模板提供 `create`、`update`、`delete`、`page`、`detail`。
- Controller 模板路径为 `/{{moduleKebab}}/{{aggregateKebab}}s`，继承 `BaseCrudController`。

前端模板：

- `@{{projectKebab}}/{{moduleKebab}}-api` 导出 CRUD 请求函数。
- `@{{projectKebab}}/{{moduleKebab}}` 导出 `{{moduleCamel}}PageRegistry` 和 `register{{modulePascal}}Pages()`。
- 后台应用入口调用 `createMangoAdminApp()` 并注册业务页面。

## 8. 数据库 / 初始化数据

模板为业务模块生成 Flyway migration 入口和表名前缀变量。具体表结构由生成后的业务模块维护。

## 9. 菜单 / 权限 / 租户

模板为业务页面注册组件 key `{{moduleKebab}}/{{aggregateKebab}}/index`。菜单、权限码、租户边界应在生成后的业务模块资源清单、migration 和 authorization 配置中维护。

## 10. 验证方式

模板静态校验：

```bash
node mango-business-starter/scripts/check-template.mjs
```

该脚本检查必备文件、业务 PMO、baseline、后端模板、前端模板和拓扑 README。它不运行 Maven、Vite 或浏览器验收。

## 11. 业务接入最小闭环

业务项目生成后，先在项目根目录执行 PMO preflight，确认 baseline 可读；再执行 `mango validate` 或模板自带 workspace 校验，随后按拓扑运行后端测试、前端构建和本地启动。单体模式验收一个后端进程和一个后台应用；微服务模式还要验收调用方 remote 契约、网关路由和真实后端 API。

模板变更验收分两层：Mango 源仓运行 `node mango-business-starter/scripts/check-template.mjs`；生成项目内运行 PMO preflight、后端 Maven、前端构建和菜单页面打开。模板校验通过不等于生成项目业务链路已通过。

## 12. 常见问题

- 新增模板变量后，需要同步后端、前端、CLI 替换逻辑和模板校验。
- business-pmo baseline 变更归入 Mango baseline 升级任务；普通业务需求通常只改 baseline 外的业务文件。
- 生成项目已有业务自定义代码时，升级模板应通过 CLI sync 或人工迁移，不覆盖业务自有文件。

## 13. 关联 PMO 规则

- [开发流程规范](../mango-pmo/rules/00-dev-flow.md)
- [交付质量门禁](../mango-pmo/rules/05-ai-delivery-quality.md)
- [文档资产规范](../mango-pmo/rules/06-document-assets.md)
- [能力说明维护规范](../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [Mango 能力地图](../mango-docs/capabilities/README.md)
- [业务 PMO 说明](./business-pmo/README.md)
- [单体拓扑说明](./topologies/monolith/README.md)
- [微服务拓扑说明](./topologies/microservice/README.md)
