# Mango 业务项目开发说明

## 1. 定位

本文说明业务团队如何基于 Mango 开启和维护一个生产业务项目。长期规范仍以 `mango-pmo` 为唯一来源；本文只做项目启动、资产选择和协作流程说明。

## 2. 命名

| 对象 | 名称 | 说明 |
|---|---|---|
| 业务项目 starter 资产 | `mango-business-starter` | 仓内生产起点，不是 demo |
| 本地初始化 CLI | `create-mango-app` | 对齐 `create-vue`、`create-react-app` 的命名习惯 |
| Web 生成服务 | Mango Initializr | 对齐 Spring Initializr |
| 生成后的业务项目 | `<business>-platform` | 例如 `guarantee-platform`、`baohan-platform` |
| 后端框架依赖 | `mango-admin-starter` | Maven starter 依赖 |
| 前端管理后台壳 | `@mango/admin-shell` | npm shell 依赖 |

不建议把生成后的业务项目命名为 `mango-starter`，避免和 Maven starter 依赖混淆。

## 3. 推荐启动方式

首选 CLI：

```bash
npm create mango-app@latest guarantee-platform -- \
  --module guarantee \
  --aggregate letter \
  --package com.example.guarantee \
  --group-id com.example \
  --topology monolith
```

本地仓内验证可直接执行：

```bash
node mango-ui/packages/create-mango-app/src/index.mjs init guarantee-platform \
  --module guarantee \
  --aggregate letter \
  --package com.example.guarantee \
  --group-id com.example \
  --topology monolith \
  --template mango-business-starter
```

## 4. 生成项目结构

```text
guarantee-platform/
  AGENTS.md
  README.md
  mango.config.json
  backend/
    modules/
      guarantee/
        guarantee-api/
        guarantee-core/
        guarantee-starter/
        guarantee-starter-remote/
  frontend/
    apps/
      guarantee-platform-admin/
    packages/
      guarantee-api/
      guarantee/
  business-docs/
    plans/
  business-pmo/
  topologies/
    monolith/
    microservice/
  scripts/
```

## 5. 依赖边界

后端业务模块按 `api/core/starter/starter-remote` 分层：

- `api` 定义 `Command`、`Query`、`VO` 和业务 API。
- `core` 承载实体、服务、转换和 Flyway migration。
- `starter` 暴露 Controller、自动装配、资源清单和模块元数据。
- `starter-remote` 提供远程调用 adapter。

前端按 app 和 package 分层：

- `frontend/apps/<project>-admin` 依赖 `@mango/admin-shell` 启动后台。
- `frontend/packages/<module>-api` 维护类型和 API client。
- `frontend/packages/<module>` 维护页面和 page registry。

## 6. PMO 继承

业务项目根目录保留 `AGENTS.md`，入口指向业务自己的 `business-pmo`，同时加载随项目带出的 `business-pmo/mango-baseline`。

`business-pmo/mango-baseline` 是当前 Mango PMO 的可执行快照，用来让业务项目脱离 Mango 源码后仍能执行 preflight 和交付台账检查。业务团队可以在 `business-pmo/rules` 写本业务的交付口径，但不能在普通业务需求中修改 Mango baseline。需要调整框架规范时，应回到 Mango 仓库治理，并通过升级 Mango 版本同步新的 baseline。

业务交付文档放在 `business-docs/plans`，模板已提供：

- `example-contract.md`
- `example-ledger.md`

业务仓内推荐 preflight：

```bash
node business-pmo/mango-baseline/tools/pmo-preflight.mjs \
  --role dev \
  --phase develop \
  --task "<用户任务>" \
  --paths "backend/modules/<module>,frontend/packages/<module>"
```

## 7. 前后端协作

推荐使用同一个业务 product monorepo。前后端人员通过目录、CODEOWNERS、任务分支和 PR review 分工，不通过拆仓隔离协作。

当前端需要后端新增或调整接口时：

1. 前端在任务文档中提出页面目标、字段、交互和期望接口。
2. 后端评审接口边界、权限、数据和错误码，形成最终接口方案。
3. 双方把接口、菜单、权限和 E2E 验收项写入交付台账。
4. 后端优先提交 API 契约或最小可联调实现。
5. 前端基于契约开发；接口未就绪时只能使用与契约一致的临时联调数据，并在提交前移除。
6. PR 必须包含双方影响说明和验证结果。

对于 AI 辅助开发，推荐让 AI 在同一任务分支内同时读取前后端上下文，但具体代码归属仍通过 CODEOWNERS 和 PR review 控制。

## 8. 菜单、权限和资源同步

业务 starter 使用后端资源清单作为菜单和权限事实源：

```text
backend/modules/<module>/<module>-starter/src/main/resources/META-INF/mango/resource-manifest.json
```

资源清单中的 `component` 必须匹配前端 page registry key：

```text
<module>/<aggregate>/index
```

前端页面包通过 `registerModulePages` 注册页面，后台壳通过 `@mango/admin-shell` 汇总注册结果。

## 9. 验证

仓内 starter 资产验证：

```bash
node mango-business-starter/scripts/check-template.mjs
```

CLI 验证：

```bash
node mango-ui/packages/create-mango-app/scripts/check-cli.mjs
```

生成后的业务项目还需要执行自己的 Maven、pnpm 和 E2E 验证，不能只依赖 starter 资产检查。
