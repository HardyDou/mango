# @mango/admin

`@mango/admin` 是 Mango 管理后台聚合入口。它把 `@mango/admin-shell`、`@mango/admin-pages`、`@mango/auth` 等基础包集中导出，并提供全量管理端样式入口。

## 1. 概览

这个包属于 `admin-shell` 聚合包。业务项目想快速启动 Mango Admin，可以直接依赖它；如果只需要某个底座能力，优先按需依赖 `@mango/admin-shell`、`@mango/admin-pages` 或具体能力包。

## 2. 功能清单

| 能力 | 入口 |
|------|------|
| 创建管理后台 | `createMangoAdminApp()` |
| 导出 Shell 能力 | 来自 `@mango/admin-shell` |
| 导出页面注册能力 | 来自 `@mango/admin-pages` |
| 导出认证页面和配置 | 来自 `@mango/auth` |
| 全量能力注册类型 | `full` 子入口 |
| 聚合样式 | `style.css`、`style-full.css` |

## 3. 接入方式

安装依赖：

```bash
pnpm add @mango/admin
```

最小启动：

```ts
import { createMangoAdminApp } from '@mango/admin';
import '@mango/admin/style.css';

createMangoAdminApp({
  apiBaseUrl: '/api',
}).mount('#app');
```

使用全量样式：

```ts
import '@mango/admin/style-full.css';
```

## 4. 配置说明

`createMangoAdminApp()` 的配置来自 `@mango/admin-shell`：

| 字段 | 默认值 | 含义 |
|------|--------|------|
| `mountTarget` | `#app` | 默认挂载节点。 |
| `apiBaseUrl` | `/api` | 后端 API 基础地址。 |
| `title` | `Mango Admin` | 页面标题。 |
| `contentMode` | `runtime-outlet` | 内容渲染方式。 |
| `login` | 空 | 登录页品牌、默认值和 slot 配置。 |
| `features` | `core` | 内置能力开关。 |
| `featureRegistrars` | 空 | 额外能力注册函数，可注册页面并返回首页小组件。 |
| `widgets` | 空 | 宿主直接传入的首页业务小组件定义。 |
| `modules` | 空 | 模块运行时配置。 |
| `runtimeConfigUrl` | 空 | 运行时配置地址。 |

## 5. API 与扩展

默认入口导出：

| 来源 | 内容 |
|------|------|
| `@mango/admin-shell` | Shell 创建、布局、路由、store、运行时能力。 |
| `@mango/admin-pages` | 页面注册、默认页面、能力开关。 |
| `@mango/auth` | 登录页、个人资料页、密码页、认证配置和用户 store。 |

`full` 子入口导出：

| 导出 | 作用 |
|------|------|
| `registerMangoCalendarAdminPages` | 注册 calendar 页面。 |
| `registerMangoFileAdminPages` | 注册 file 页面。 |
| `registerMangoJobAdminPages` | 注册 job 页面。 |
| `registerMangoNoticeAdminPages` | 注册 notice 页面。 |
| `registerMangoNoticeAdminShell` | 注册 notice Shell 能力。 |
| `registerMangoNumgenAdminPages` | 注册 numgen 页面。 |
| `registerMangoPaymentAdminPages` | 注册 payment 页面。 |
| `registerMangoTemplateAdminPages` | 注册 template 页面。 |
| `registerMangoWorkflowAdminPages` | 注册 workflow 页面。 |
| `registerMangoWorkflowBusinessExampleAdminPages` | 注册 workflow 示例页面。 |
| `mangoFullAdminFeatureRegistrars` | 全量能力注册函数数组。 |

## 6. 数据与初始化

这个包不写数据库、菜单或权限。它依赖各后端模块初始化菜单和权限，依赖各前端能力包注册页面。

样式聚合由 `admin-modules.json` 作为唯一配置源生成，`admin-packages.json`、`style-full.css` 和 `full` 子入口均来自该配置。修改官方 admin 模块、样式入口或 full 注册项时，先更新 `admin-modules.json`，再运行样式生成和检查命令，禁止手工改生成文件。

默认样式入口 `style.css` 只聚合核心管理后台依赖；全量入口 `style-full.css` 会额外聚合官方 full 模块样式。当前默认样式包含：

```text
@mango/common
@mango/admin-shell
@mango/auth
@mango/rbac
@mango/system
@mango/job
@mango/payment
```

## 7. 管理入口

这个包没有自己的菜单。管理入口来自 `@mango/admin-shell` 拉取的后端授权菜单，以及各能力包注册的页面 key。

## 8. 快速开始

1. 安装 `@mango/admin`。
2. 引入 `@mango/admin/style.css` 或 `@mango/admin/style-full.css`。
3. 调用 `createMangoAdminApp()`。
4. 配置 `apiBaseUrl`。
5. 需要可选能力时，从具体能力包或 `full` 子入口注册对应页面。使用 Mango CLI 生成的 full/custom app 时，官方默认模块和已选模块的 `featureRegistrars` 与样式入口会自动生成。

## 9. 问题排查

**样式缺失**

确认引入了 `@mango/admin/style.css`。使用全量能力时引入 `@mango/admin/style-full.css`。

**某个可选能力页面打不开**

`@mango/admin` 不会运行时扫描 `node_modules`。Mango CLI 生成的 app 会根据模块清单生成静态 imports、`featureRegistrars` 和样式 imports；手写宿主需要把对应业务 UI 包的 `registerMangoXxxAdminPages()` 放入 `featureRegistrars`，并确保后端菜单存在。业务 UI 包可以在同一个注册函数中返回 `businessDomainCode`、`businessDomainName`、可选 `groupName` 和首页 `widgets`，Shell 首页会自动把这些小组件加入组件库，并按“业务域 / 组名 / 组件名称”展示；未集成该 UI 包时，对应业务小组件不会注册，也不能被新增使用。

**包体过大**

如果只使用 Shell 基础能力，改为直接依赖 `@mango/admin-shell` 和需要的能力包。

## 10. 相关文档

- [Admin Shell README](../admin-shell/README.md)
- [Admin Pages README](../admin-pages/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
