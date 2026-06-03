# Mango 业务项目开发说明

## 1. 定位

本文说明业务团队如何基于 Mango 开启和维护一个生产业务项目。长期规范仍以 `mango-pmo` 为唯一来源；本文只做项目启动、资产选择和协作流程说明。

## 2. 命名

| 对象 | 名称 | 说明 |
|---|---|---|
| 业务项目 starter 资产 | `mango-business-starter` | 仓内生产起点，不是 demo |
| 官方脚手架 CLI | `@mango/cli` | 通过已发布 Maven / npm 包生成业务工程；安装后命令为 `mango` / `mango-cli` |
| 历史本地初始化 CLI | `create-mango-app` | 仓内 starter 验证资产，不作为当前业务项目首选入口 |
| Web 生成服务 | Mango Initializr | 对齐 Spring Initializr |
| 生成后的业务项目 | `<business>-platform` | 例如 `guarantee-platform`、`baohan-platform` |
| 后端框架依赖 | `mango-admin-starter` | Maven starter 依赖 |
| 前端管理后台壳 | `@mango/admin-shell` | npm shell 依赖 |

不建议把生成后的业务项目命名为 `mango-starter`，避免和 Maven starter 依赖混淆。

## 3. 推荐启动方式

首选 `@mango/cli`。包名必须使用 scoped package，避免 Nexus `npm-group` 与公共 npm 上同名 `mango-cli` 包冲突；安装后推荐使用 `mango` 命令。

企业环境先配置 npm registry。全局安装 CLI 时使用用户级 `~/.npmrc`；项目内安装依赖时使用企业项目根目录 `.npmrc`。不要依赖父目录 `.npmrc` 配合 `npm --prefix <dir>`，npm 不会按这个方式稳定读取父目录配置。

```ini
registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/
@mango:registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/
```

配置用户级 `~/.npmrc` 后安装 CLI：

```bash
npm install -g @mango/cli@1.0.20

mango init guarantee-platform \
  --preset custom \
  --modules workflow,template,file \
  --package com.example.guarantee \
  --group-id com.example \
  --topology monolith
```

一次性执行也必须指定 scoped package：

```bash
npm exec --package @mango/cli@1.0.20 -- \
  mango init guarantee-platform \
  --preset custom \
  --modules workflow,template,file \
  --package com.example.guarantee \
  --group-id com.example \
  --topology monolith
```

不要安装或执行未 scoped 的 `mango-cli` npm 包。

Mango 包发布到 `npm-hosted`，消费从 `npm-group` 安装。`@mango/cli` 包内 `publishConfig` 已指向 `npm-hosted`，框架维护者发布 CLI 时不需要在命令里重复写 registry。

需要全量 Mango 管理端能力时使用 full preset：

```bash
mango init guarantee-platform \
  --preset full \
  --package com.example.guarantee \
  --group-id com.example \
  --topology monolith
```

full preset 后端会依赖 `mango-admin-starter` 和可选的 `mango-seed-starter`。初始化种子数据默认关闭；首次空库启动如需官方入口数据，必须显式启用并提供管理员初始密码：

```bash
MANGO_SEED_ENABLED=true \
MANGO_SEED_ADMIN_PASSWORD='replace-with-a-strong-password' \
scripts/backend-dev.sh
```

seed 只补齐默认租户、管理员账号、租户成员、管理员角色、成员角色绑定、租户应用绑定和官方菜单套餐授权；不会复制或掩盖缺失的菜单、组件或历史 migration 问题。重复执行保持幂等，已有管理员密码不会被覆盖，`prod`/`production` profile 下禁止弱默认密码。

只需要必选系统能力时使用 custom preset 且不选择可选模块：

```bash
mango init guarantee-platform \
  --preset custom \
  --modules none \
  --package com.example.guarantee \
  --group-id com.example \
  --topology monolith
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
    src/
    public/runtime-config.json
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

前端消费 Mango 已发布 npm 包：

- full preset 使用 `@mango/admin/full` 和 `@mango/admin/style-full.css`。
- custom preset 使用 `@mango/admin`、显式 `features` 和 `featureRegistrars`。
- 业务自有页面和接口在生成项目内维护，不修改 Mango 包源码。

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

前端页面包通过 `registerModulePages` 注册页面，后台壳通过 `@mango/admin-shell` 汇总注册结果。业务项目接入已发布能力包时，按能力包文档显式启用 feature 和 registrar。

组件和能力包使用方式见：

- [Mango 组件与能力使用指南 ForAI](./mango-capability-usage-guide-for-ai.md)

## 9. 验证

仓内 starter 资产验证：

```bash
node mango-business-starter/scripts/check-template.mjs
```

CLI 验证：

```bash
pnpm --dir mango-ui -F mango-cli test
node mango-ui/packages/mango-cli/scripts/check-cli.mjs
```

生成后的业务项目还需要执行自己的 Maven、pnpm 和 E2E 验证，不能只依赖 starter 资产检查。
