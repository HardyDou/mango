# Mango 产品化 Issue #26 Sprint 5 业务模块模板与双模式拓扑

## 1. 背景

Issue #26 要求业务项目可以通过 jar 和 npm 包依赖 Mango 框架资源，并能按模板启动新项目。前置 Sprint 已完成资源同步、后端聚合 starter 和前端 admin shell 的产品化基线，本 Sprint 将业务模块开发规范沉淀为可被后续 Initializr 消费的仓内模板资产。

## 2. 目标

提供 `mango-business-template` 孵化模板，覆盖业务项目单仓结构、后端模块分层、前端业务包、菜单权限资源清单、单体和微服务拓扑、业务交付契约模板和模板完整性检查。该资产作为 Mango business template 的首个落地点，并明确 monolith 与 microservice 两种生成拓扑。

## 3. 范围

- 新增业务项目根模板说明和业务项目 `AGENTS.md` 模板。
- 新增后端 `api/core/starter/starter-remote` 业务模块模板。
- 新增前端 `api-client/types/pages/page registry` 业务模块模板。
- 新增单体和微服务部署拓扑说明。
- 新增业务契约与交付台账模板。
- 新增模板完整性检查脚本。

## 4. 不做什么

- 不实现 Mango Initializr CLI 或在线服务。
- 不生成真实保函模块。
- 不提供真实初始化种子数据。
- 不启动模板项目的真实前后端服务。
- 不把业务模板规则写入 `mango-pmo` 长期规范。

## 5. 设计说明

### 5.1 影响模块

- `mango-business-template`：新增业务项目孵化模板。
- `mango-docs/plans`：新增本 Sprint 设计说明和交付台账。

### 5.2 接口变化

模板后端 API 使用 `R<T>`、`Command`、`Query`、`VO`，并提供 Controller 实现 API、Feign adapter 实现 API 的标准结构。Mango 运行时代码不新增接口。

### 5.3 数据变化

模板提供 Flyway migration 示例路径和表结构占位，Mango 当前数据库不变。

### 5.4 菜单/页面/权限变化

模板提供 `META-INF/mango/resource-manifest.json` 示例，菜单 `component` 与前端 `registerModulePages` 的 page registry key 一致。Mango 当前菜单数据不变。

### 5.5 测试范围

通过 `node mango-business-template/scripts/check-template.mjs` 校验模板必需文件、后端分层、`R<T>` 契约、资源清单、前端 page registry、`@mango/common` 请求封装、无 `workspace:*` 依赖语义。

## 6. 完成标准

- 模板文件完整。
- 单体和微服务拓扑说明明确依赖方式。
- 业务契约和交付台账模板存在。
- 模板完整性检查通过。
- Sprint 交付台账检查通过。

## 7. 遗留问题

- Initializr CLI 和 `npm create mango-business` 由后续 Sprint 交付。
- 真实模板项目启动验证需要 Initializr 完成变量替换后执行。
- 可选初始化数据能力由后续 seed data Sprint 交付。
