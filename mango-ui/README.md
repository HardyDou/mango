# Mango UI

## 1. 概览
`mango-ui` 是 Mango 管理后台前端 Monorepo，承载单体后台、微前端 Shell、子应用和可发布前端能力包。

核心形态：

| 形态 | 入口 | 用途 |
|------|------|------|
| 单体后台 | `apps/mango-admin` | 默认交付形态，所有页面本地打包 |
| 微前端 Shell | `apps/mango-admin-shell` | 登录、布局、菜单、权限、主题、TagsView、运行配置 |
| 子应用 | `apps/mango-admin-rbac-app`、`apps/mango-admin-workflow-app` | 被 Shell 挂载的页面运行单元 |
| 能力包 | `packages/*` | 管理端页面、公共组件、API 类型、runtime、CLI |

前端部署形态不改变后端菜单、权限、租户和业务接口。单体、混合、微前端由前端 runtime config 决定。

## 2. 适用场景
- 开发 Mango 管理后台页面和公共组件。
- 发布 `@mango/admin`、`@mango/admin-shell`、`@mango/admin-pages` 等前端能力包。
- 业务后台通过 `@mango/admin` 或 `@mango/admin-shell` 组装管理端。
- 验证单体、混合、微前端部署组合。
- 调试后端菜单 component key 和前端页面注册关系。

## 3. 边界说明
- 不作为官网、营销站、内容站或 C 端站点模板。
- 不负责后端 resource manifest、菜单、权限和租户数据入库。
- 不替代业务模块 README；业务包仍要说明页面、接口、权限和配置。
- 不在前端绕过后端权限和租户校验。

## 4. 模块组成
前端包分层：

| 层级 | 包 / 应用 | 说明 |
|------|-----------|------|
| 管理入口 | `@mango/admin` | 聚合 Shell、默认页面和样式，业务项目优先使用 |
| Shell | `@mango/admin-shell` | 管理后台壳、菜单、runtime、微前端挂载 |
| 页面注册 | `@mango/admin-pages` | `moduleCode + component` 到页面 loader 的注册表 |
| Runtime | `@mango/app-runtime` | runtime config、Wujie、微前端生命周期协议 |
| 公共基础 | `@mango/common`、`@mango/api-schema` | 请求、组件、hooks、类型 |
| 管理页面包 | `@mango/rbac`、`@mango/system`、`@mango/file`、`@mango/workflow` 等 | 管理端页面和 API 调用 |
| CLI | `@mango/cli` | 项目初始化、模块生成、dev workspace |

依赖方向：

```text
apps/* -> packages/*
admin -> admin-shell + admin-pages + platform page packages
admin-shell -> admin-pages + app-runtime + common + auth
page packages -> common + api-schema + admin-pages
common -> api-schema
```

## 5. 接入方式
安装：

```bash
pnpm -C mango-ui install
```

单体启动：

```bash
pnpm -C mango-ui --filter mango-admin dev -- --host 0.0.0.0 --port 5175
```

指定后端：

```bash
VITE_ADMIN_PROXY_PATH=http://127.0.0.1:5555 pnpm -C mango-ui --filter mango-admin dev -- --host 0.0.0.0 --port 5175
```

微前端启动：

```bash
pnpm -C mango-ui dev:micro
```

默认端口：

| 服务 | 地址 |
|------|------|
| 单体后台 | `http://127.0.0.1:5175` |
| Shell | `http://127.0.0.1:5176` |
| RBAC 子应用 | `http://127.0.0.1:5181` |
| Workflow 子应用 | `http://127.0.0.1:5182` |

业务项目接入前端能力通常使用：

```ts
import { createMangoAdminApp } from '@mango/admin';
import '@mango/admin/style.css';
```

需要定制 Shell 时才直接使用：

```ts
import { createMangoAdminApp } from '@mango/admin-shell';
import '@mango/admin-shell/style.css';
```

## 6. 配置说明
| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 | 源码入口 |
|----------|------------|--------|------|----------|----------|
| 环境变量 | `VITE_ADMIN_PROXY_PATH` | 应用默认值 | 本地后端代理目标 | 开发态 API 请求转发 | app Vite config |
| Shell runtime config | `profile` | `monolith` | 单体、混合或微前端 | 控制模块本地或远程 | `@mango/app-runtime` |
| Shell runtime config | `modules.<module>.mode` | `local` | 模块加载方式 | local 或 micro | `@mango/admin-shell` |
| Shell runtime config | `modules.<module>.entry` | 模块默认值 | 远程子应用入口 | Wujie 加载地址 | `@mango/admin-shell` |
| 环境变量 | `VITE_MANGO_ALLOWED_REMOTE_ORIGINS` | 开发态本地域名 | 远程 entry 白名单 | 生产类环境安全门禁 | `runtimeConfig.ts` |
| 环境变量 | `VITE_MANGO_ALLOW_HTTP_REMOTE_ENTRIES` | `false` | 是否允许 HTTP entry | 生产类环境默认不开放 | `runtimeConfig.ts` |
| 环境变量 | `VITE_MANGO_DEPLOY_ENV` | Vite mode | 部署环境 | 控制开发中心和 fail closed | `runtimeConfig.ts` |
| `package.json` | `scripts.dev:micro` | 已配置 | 启动 Shell 和子应用 | 微前端本地调试 | 根 package scripts |

runtime config 例子：

```json
{
  "profile": "hybrid",
  "modules": {
    "mango-authorization": {
      "mode": "micro",
      "runtimeCode": "mango-admin-rbac-app",
      "entry": "http://127.0.0.1:5181/"
    },
    "mango-system": {
      "mode": "local"
    },
    "mango-workflow": {
      "mode": "micro",
      "runtimeCode": "mango-admin-workflow-app",
      "entry": "http://127.0.0.1:5182/"
    }
  }
}
```

## 7. API 与扩展
| 扩展点 | 包 | 适用对象 | 说明 |
|--------|----|----------|------|
| `createMangoAdminApp` | `@mango/admin` | 业务后台 | 推荐入口，聚合默认管理能力 |
| `createMangoAdminApp` | `@mango/admin-shell` | Shell 定制 | 直接组装管理后台壳 |
| `registerModulePages` | `@mango/admin-pages` | 页面包 | 注册 component key 到 loader |
| `runtime config` | `@mango/app-runtime` | Shell / 子应用 | 控制本地和远程模块加载 |
| `request` | `@mango/common` | 所有页面包 | 统一 API base URL、401、错误处理 |
| `admin-pages` export | 管理页面包 | Shell / admin | 向统一页面注册表贡献页面 |
| `style.css` | 各包 | 宿主应用 | 引入包样式 |

前端组件区分：

| 类型 | 标识 | 适用范围 |
|------|------|----------|
| `admin-shell` 配套 | `@mango/admin-shell`、runtime、menu、stores | Mango 管理后台壳 |
| `admin-pages` 配套 | `@mango/rbac`、`@mango/system`、`@mango/file` 等页面包 | Mango 管理后台页面 |
| 公共基础组件 | `@mango/common` 部分组件 | 可复用，但仍需检查依赖 Element Plus、Pinia、请求封装 |

官网或普通网站不要直接集成 `admin-pages` 页面包。

## 8. 数据与初始化
前端没有数据库 migration。前端依赖后端已经初始化的数据：

| 类型 | 后端来源 | 前端消费方式 | 验证方式 |
|------|----------|--------------|----------|
| 应用 | authorization app | `appCode=internal-admin` | 菜单接口返回 |
| 菜单 | authorization menu | Shell 渲染路由和导航 | 菜单树显示 |
| 权限 | authorization permission | 页面按钮展示、接口后端校验 | 角色授权和接口 403 |
| 租户 | identity / system / context | 请求头、用户上下文、后端过滤 | 不串租 |
| 字典、区域、组织 | system / org | `@mango/common` API 和组件 | 下拉、树、选择器可用 |

## 9. 管理入口
菜单打开链路：

```text
后端 resource manifest / migration -> authorization 菜单接口 -> Shell menuHost -> admin-pages registry -> Vue page
```

需要对齐的字段：

| 字段 | 说明 |
|------|------|
| `appCode` | 管理后台应用，当前为 `internal-admin` |
| `moduleCode` | 模块编码，例如 `mango-authorization`、`mango-system`、业务模块 code |
| `component` | 页面 key，例如 `system/user/index`、`order/sales-order/index` |
| `path` | 前端路由路径 |
| `permissionCode` | 后端接口和按钮权限使用 |

前端可以隐藏按钮，但不能把隐藏按钮当成权限控制。所有写操作、查询范围和租户隔离都要由后端接口校验。

## 10. 质量检查
单体构建：

```bash
pnpm -C mango-ui --filter mango-admin build
```

Shell 构建和测试：

```bash
pnpm -C mango-ui --filter @mango/admin-shell test
pnpm -C mango-ui --filter mango-admin-shell build
```

微前端构建：

```bash
pnpm -C mango-ui build:micro
```

微前端 E2E：

```bash
pnpm -C mango-ui dev:micro
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true pnpm -C mango-ui test:micro --project=chromium
```

README 门禁：

```bash
node mango-pmo/tools/audit-module-readmes.mjs
node mango-pmo/tools/audit-readme-source-facts.mjs
```

发布包文档检查：

```bash
pnpm -C mango-ui --filter @mango/common pack --pack-destination /tmp/mango-pack
tar -tf /tmp/mango-pack/mango-common-*.tgz | grep 'package/README.md'
```

所有 `mango-ui/packages/*/package.json` 的 `files` 都应显式包含 `README.md`。业务只拿到 npm 包时，可从包根目录的 `README.md` 阅读页面注册、接口、样式和依赖说明。

发布防漏检查：

```bash
pnpm -C mango-ui release:impact --base=origin/main --head=HEAD
pnpm -C mango-ui release:verify-npm grid-widgets --version=1.0.5
pnpm -C mango-ui release:verify-npm system --version=1.0.11
```

`release:impact` 用于 PR 和发布前检查。脚本会根据 `packages/*` 的 `src`、`package.json`、`vite.config.ts`、`README.md` 和包内脚本变更计算需要发布的 npm 包，并要求这些包完成 `package.json` 升版、内部固定依赖升版和 `packages/mango-cli/release-versions.json` 同步。命令默认也会检查本地未提交改动；CI 只检查提交范围时可追加 `--committed-only`。

`release:verify-npm` 用于发布后回查公司内网 Nexus tarball。默认 registry 是 `http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`，也可通过 `--registry=<url>` 指定。需要校验关键产物时维护 `release-contracts.json`，例如日历小组件、系统配置面板、样式入口或其它必须进入 npm 包的文件。

正式发布单包仍使用：

```bash
pnpm -C mango-ui publish:pkg <package|short-name> --release-tag=<tag>
```

`publish:pkg` 发布后会同时回查 `npm-hosted` 和 `npm-group`，并复用 `release-contracts.json` 的 tarball 契约。

## 11. 快速开始
1. 后端模块初始化菜单、权限和 API 资源。
2. 前端业务包实现页面并调用 `registerModulePages`。
3. 后台入口引入业务包，执行页面注册函数。
4. Shell 登录后请求 `internal-admin` 菜单。
5. 点击菜单，确认 `moduleCode + component` 能找到页面 loader。
6. 页面调用真实后端 API，验证按钮权限、接口权限和租户数据。
7. 执行构建、E2E 和交付台账登记。

## 12. 问题排查
| 问题 | 原因 | 处理方式 |
|------|------|----------|
| 菜单不显示 | 后端未授权、appCode 不一致或 feature 被关闭 | 查 authorization 菜单接口和 Shell 配置 |
| 菜单空白 | component key 没注册 | 查 `@mango/admin-pages` 注册表 |
| 本地能用，生产远程加载失败 | entry 不在 allowlist 或 HTTP 默认不开放 | 配置 `VITE_MANGO_ALLOWED_REMOTE_ORIGINS` 和 HTTPS |
| 官网想复用后台页面包 | admin-pages 页面强依赖管理后台上下文 | 只抽取真正公共组件，避免引入后台壳 |
| 按钮隐藏但接口仍可调用 | 前端隐藏不是权限控制 | 后端接口补权限校验 |

## 13. 相关文档
- [前端代码规范](../mango-pmo/rules/frontend/01-vue-code.md)
- [Element Plus UI 规范](../mango-pmo/rules/frontend/02-element-plus-ui.md)
- [前端组件规范](../mango-pmo/rules/frontend/03-component-development.md)
- [前端测试规范](../mango-pmo/rules/frontend/04-test.md)
- [前端开发流程](../mango-pmo/rules/frontend/05-dev-flow.md)
- [Monorepo 架构规范](../mango-pmo/rules/frontend/06-monorepo-architecture.md)
- [能力说明维护规范](../mango-pmo/rules/08-capability-docs.md)

## 14. 历史资料
- [微前端运行说明](./docs/micro-frontend-runtime.md)
- [Mango 能力地图](../mango-docs/capabilities/README.md)
- [@mango/admin-pages](./packages/admin-pages/README.md)
- [@mango/admin-shell](./packages/admin-shell/README.md)
- [@mango/admin](./packages/admin/README.md)
