# @mango/admin-shell

`@mango/admin-shell` 是 Mango 管理后台壳。它负责创建 Vue 应用、安装基础插件、加载登录态、拉取菜单、按菜单挂载本地页面或微前端页面。

## 1. 概览

这个包属于 `admin-shell`。业务后台项目如果要使用 Mango 管理端布局、菜单、登录态、主题和运行时页面加载能力，入口通常就是 `createMangoAdminApp()`。

它不是纯组件库，也不适合官网、营销站点直接复用。

## 2. 功能清单

| 能力 | 入口 |
|------|------|
| 创建管理后台应用 | `createMangoAdminApp()` |
| 配置 Shell | `configureMangoAdminShell()` |
| 读取 Shell 配置 | `getMangoAdminShellOptions()` |
| 创建路由 | `createMangoAdminRouter()` |
| 安装 Shell 插件 | `installShellApp()` |
| 菜单运行时 | `useMenuHost()` |
| 页面运行时 | `useRuntimeHost()` |
| 运行时配置 | `loadShellRuntimeConfig()` |
| store 导出 | `stores` 子入口 |
| 开发中心页面 | `dev-pages`、`dev-base-pages` 等子入口 |

## 3. 接入方式

安装依赖：

```bash
pnpm add @mango/admin-shell
```

创建管理后台：

```ts
import { createMangoAdminApp } from '@mango/admin-shell';
import '@mango/admin-shell/style.css';

const admin = createMangoAdminApp({
  apiBaseUrl: '/api',
  title: 'Mango Admin',
  features: 'core',
});

admin.mount('#app');
```

注册额外能力页面：

```ts
import { createMangoAdminApp } from '@mango/admin-shell';
import { registerMangoWorkflowAdminPages } from '@mango/workflow/admin-pages';

const admin = createMangoAdminApp({
  featureRegistrars: [
    registerMangoWorkflowAdminPages,
  ],
  features: ['workflow'],
});

admin.mount();
```

## 4. 配置说明

`MangoAdminShellOptions`：

| 字段 | 默认值 | 含义 |
|------|--------|------|
| `mountTarget` | `#app` | 默认挂载节点。 |
| `apiBaseUrl` | `/api` | `@mango/common` request 基础地址。 |
| `title` | `Mango Admin` | 页面标题。 |
| `contentMode` | `runtime-outlet` | 内容渲染方式，支持 `router-view`、`runtime-outlet`。 |
| `devCenter.visible` | 空 | 是否显示开发中心。 |
| `devCenter.deployEnv` | 空 | 开发中心运行环境标识。 |
| `devCenter.registrars` | 空 | 开发中心页面注册函数。 |
| `devCenter.pages` | 空 | 开发中心页面列表函数。 |
| `login` | 空 | 传给 `@mango/auth` 的登录页配置。 |
| `modules` | 空 | 模块运行时配置，结构来自 `@mango/app-runtime`。 |
| `localApps` | 空 | 本地应用配置。 |
| `features` | `core` | 内置能力开关。 |
| `featureRegistrars` | 空 | 额外能力注册函数。 |
| `runtimeConfigUrl` | 空 | 运行时配置地址。 |
| `runtimeConfigLoadOptions` | 空 | 运行时配置加载选项。 |

`features` 支持：

| 值 | 含义 |
|----|------|
| `core` | authorization、system。 |
| `full` | core 加 workflow、file、template、notice、numgen、calendar、job。 |
| 字符串数组 | core 永远启用，数组中的能力额外启用。 |
| 对象 | core 永远启用，值为 `true` 的能力额外启用。 |

## 5. API 与扩展

主入口导出：

| 导出 | 作用 |
|------|------|
| `createMangoAdminApp(options)` | 创建 Vue app、router，并返回 `mount()`。 |
| `MangoAdminShellApp` | Shell 根组件。 |
| `MangoAdminShellView` | Shell 内容视图。 |
| `MangoAdminLayout` | 管理后台布局组件。 |
| `MangoAdminParentView` | 父级路由占位组件。 |
| `createMangoAdminRouter()` | 创建 Shell 路由。 |
| `getShellPinia()` | 获取 Shell Pinia。 |
| `installShellApp(app, options)` | 安装 Shell 依赖和配置。 |
| `configureMangoAdminShell(options)` | 合并 Shell 配置。 |
| `getMangoAdminShellOptions()` | 读取当前 Shell 配置。 |

子入口：

| 子入口 | 内容 |
|--------|------|
| `runtime` | 运行时页面挂载。 |
| `menu` | 菜单加载和菜单路由转换。 |
| `stores` | 用户、布局、主题、偏好等 store。 |
| `router` | Shell 路由。 |
| `home` | 首页组件。 |
| `dev-pages` | 开发中心页面注册。 |
| `dev-base-pages` | 基础能力开发页注册。 |

### Feature Registrars

`featureRegistrars` 用于把能力包的页面注册到 Shell。推荐由业务入口集中传入，例如 `registerMangoWorkflowAdminPages`、`registerMangoFileAdminPages`。注册函数只声明前端页面，不负责创建后端菜单；菜单仍以授权中心返回的数据为准。

### Runtime Modules

`modules` 用于声明运行时模块加载方式，结构来自 `@mango/app-runtime`。本地页面优先使用已注册的 page loader；微前端页面需要在运行时配置中声明 entry、activeRule 和隔离策略。

### Menu Contract

Shell 只消费后端授权菜单。菜单 `component` 会归一化后匹配 `@mango/admin-pages` 注册的页面 key；匹配失败时显示 404，不会自动推断业务包路径。后端菜单、前端注册 key、能力开关必须保持一致。

### Theme

主题状态由 Shell store 管理，当前包含布局偏好、主题色和侧边栏状态。业务项目应通过 Shell 暴露的 store 或配置入口调整主题，不应直接依赖 Shell 内部组件路径。

### I18n

Shell 安装 `vue-i18n` 并提供基础文案。能力包可以在自身注册流程中补充本地文案；业务应用需要复用同一个 i18n 实例，避免重复安装多个互不相通的实例。

### Directives

Shell 会安装管理端基础指令和权限相关运行时。业务包新增指令时应在自身入口显式安装，避免把业务私有指令写入 Shell。

### Migration From App-Local Shell Code

从应用内 Shell 代码迁移时，先把入口替换为 `createMangoAdminApp()`，再把本地路由迁移为 `@mango/admin-pages` 注册项，把菜单来源切回后端授权菜单。迁移期间不要继续引用 `apps/*` 私有路径。

### Compatibility Matrix

| 依赖 | 兼容版本 |
|------|----------|
| Vue | `3.5.13` |
| Vue Router | `^4.1.6` |
| Pinia | `2.0.32` |
| Element Plus | `2.14.1` |
| `@mango/common` | 与 `@mango/cli` 的 `release-versions.json` 保持一致 |
| `@mango/grid-layout` | `1.0.0` |

运行时菜单请求：

| 接口 | 用途 |
|------|------|
| `GET /authorization/menus/user` | 按当前用户读取 `internal-admin` 菜单树。 |

## 6. 数据与初始化

这个包不写数据库数据。运行时依赖后端已经初始化菜单和权限。

| 数据 | 来源 |
|------|------|
| 菜单树 | `/authorization/menus/user?fmt=tree&appCode=internal-admin`。 |
| 页面 loader | `@mango/admin-pages` 注册表。 |
| runtime config | `@mango/app-runtime` 加载的 `/runtime-config.json` 或默认配置。 |
| 登录态 | `@mango/auth` 和 `@mango/common` session。 |
| 主题和布局 | Shell stores。 |

## 7. 管理入口

Shell 自带首页、登录页、账户页和错误页。业务菜单来自后端授权菜单，页面由 `@mango/admin-pages` 注册。

菜单能否打开取决于三件事：

- 后端返回菜单，且当前用户有权限。
- 菜单 `component` 能匹配已注册页面 key。
- 模块运行时配置允许以本地或微前端方式加载。

## 8. 快速开始

1. 安装 `@mango/admin-shell` 和 peer 依赖。
2. 在入口文件调用 `createMangoAdminApp()`。
3. 设置 `apiBaseUrl` 指向后端网关或后端服务。
4. 注册业务模块的 admin-pages 子入口。
5. 确认后端 `/authorization/menus/user` 能返回当前用户菜单。
6. 调用 `mount()` 挂载应用。

## 9. 问题排查

**登录接口或菜单接口请求错地址**

检查 `apiBaseUrl`。`createMangoAdminApp()` 会把它传给 `@mango/common` 的 request。

**菜单有数据但页面 404**

检查业务包是否执行页面注册函数，菜单 `component` 归一化后是否能匹配注册 key。

**可选能力菜单不显示**

检查 `features` 是否启用对应能力，且对应 `featureRegistrars` 是否注册了页面。

**微前端页面加载失败**

检查 `runtimeConfigUrl`、`modules`、微前端 entry、白名单和 `@mango/app-runtime` 的运行时日志。

**401 后没有回到登录页**

Shell 会注册 unauthorized handler 并清理 session 后跳转 `/login`。检查 request 是否使用 `@mango/common` 的请求工具。

## 10. 相关文档

- [Admin Pages README](../admin-pages/README.md)
- [App Runtime README](../app-runtime/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
