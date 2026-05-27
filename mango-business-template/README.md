# Mango Business Template

`mango-business-template` 是 Mango 业务项目模板的仓内孵化版本。后续 `npm create mango-business@latest` 或 `mango init` 会基于本目录做变量替换和能力选择。

## 1. 使用方式

当前阶段可以把本目录复制为业务项目模板仓，替换以下占位符后启动：

| 占位符 | 示例 | 说明 |
|---|---|---|
| `{{projectKebab}}` | `guarantee-platform` | 项目名 |
| `{{projectPascal}}` | `GuaranteePlatform` | 项目 PascalCase 名 |
| `{{moduleKebab}}` | `guarantee` | 业务模块名 |
| `{{modulePackage}}` | `guarantee` | Java 包名中的模块段 |
| `{{modulePascal}}` | `Guarantee` | Java/TS 类型模块名 |
| `{{aggregateKebab}}` | `letter` | 聚合名 |
| `{{aggregatePascal}}` | `Letter` | 聚合 PascalCase 名 |
| `{{basePackage}}` | `com.example.business` | Java 基础包名 |
| `{{basePackagePath}}` | `com/example/business` | Java 基础包路径 |
| `{{moduleName}}` | `业务模块` | 菜单展示名 |

## 2. 推荐结构

```text
{{projectKebab}}/
├── AGENTS.md
├── backend/
│   └── modules/{{moduleKebab}}/
├── frontend/
│   ├── apps/{{projectKebab}}-admin/
│   └── packages/
│       ├── {{moduleKebab}}/
│       └── {{moduleKebab}}-api/
├── business-pmo/
├── business-docs/
├── topologies/
└── scripts/
```

业务项目默认使用 product monorepo。人员分工通过 `CODEOWNERS`、任务分支、交付台账和 PR review 控制。

## 3. 后端模板

后端模块按 Mango 分层：

```text
{{moduleKebab}}-api
{{moduleKebab}}-core
{{moduleKebab}}-starter
{{moduleKebab}}-starter-remote
```

- `api` 只放 `Command`、`Query`、`VO` 和 `{{modulePascal}}Api`。
- `core` 放实体、服务接口、服务实现、转换和 Flyway migration。
- `starter` 放 Controller、自动装配、`module.properties` 和资源清单。
- `starter-remote` 放 Feign adapter。

## 4. 前端模板

前端拆成 API 包和页面包：

```text
packages/{{moduleKebab}}-api
packages/{{moduleKebab}}
apps/{{projectKebab}}-admin
```

- `{{moduleKebab}}-api` 导出类型和 API client。
- `{{moduleKebab}}` 导出页面注册函数。
- admin app 通过 `@mango/admin-shell` 启动，不复制 Mango app 源码。

## 5. 菜单与权限

后端资源清单中的 `component` 必须与前端 page registry key 一致：

```text
{{moduleKebab}}/{{aggregateKebab}}/index
```

starter 引入后，由 Mango 资源同步能力把菜单和按钮权限同步到后台。

## 6. 拓扑

- 单体模式：业务 app 依赖 `{{moduleKebab}}-starter`。
- 微服务模式：业务服务依赖 `{{moduleKebab}}-starter`，调用方依赖 `{{moduleKebab}}-starter-remote`。

详见 `topologies/monolith/README.md` 和 `topologies/microservice/README.md`。

## 7. 验证

```bash
node mango-business-template/scripts/check-template.mjs
```

该脚本只校验模板资产，不替代生成后项目的 Maven、pnpm 和浏览器验证。
