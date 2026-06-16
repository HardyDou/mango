# @mango/app-runtime

`@mango/app-runtime` 提供 Mango 前端运行时协议。Admin Shell 用它加载本地模块、微前端模块、iframe 页面和外链页面；微前端子应用也用它接收登录态、主题、菜单和请求能力。

## 1. 概览

这个包属于 `admin-shell` 和 `api-client` 基础能力。它不提供业务页面，也不写菜单数据。

主要能力：

- 定义菜单、运行时配置、子应用配置和运行时上下文类型。
- 加载并校验 `/runtime-config.json`。
- 按模块配置决定本地页面或微前端加载方式。
- 封装 wujie 微前端挂载、卸载、预加载和错误日志。
- 给 Vue 微前端子应用提供独立运行和 wujie 运行两种启动方式。

## 2. 功能清单

| 能力 | 入口 |
|------|------|
| 注册本地应用 | `registerLocalApp()` |
| 读取本地应用 | `getLocalApp()` |
| 创建运行时事件总线 | `createRuntimeEventBus()` |
| 设置运行时日志输出 | `setMangoRuntimeLogger()` |
| 发送运行时日志 | `emitMangoRuntimeLog()` |
| 加载运行时配置 | `loadRuntimeConfig()`、`loadRuntimeConfigWithOptions()` |
| 合并运行时配置 | `mergeRuntimeConfig()` |
| 解析应用适配器 | `resolveAdapter()` |
| 预加载微前端 | `preloadMicroApp()` |
| 创建 wujie Vue 子应用 | `createMangoWujieVueApp()` |
| 应用运行时主题 | `applyMangoRuntimeTheme()`、`bindMangoRuntimeTheme()` |

## 3. 接入方式

安装依赖：

```bash
pnpm add @mango/app-runtime
```

Shell 加载运行时配置：

```ts
import { loadRuntimeConfig } from '@mango/app-runtime';

const config = await loadRuntimeConfig({
  profile: 'monolith',
  modules: {},
});
```

微前端 Vue 子应用接入：

```ts
import { createMangoWujieVueApp, bindMangoRuntimeTheme } from '@mango/app-runtime/vue-micro';

createMangoWujieVueApp({
  standaloneRoot: () => import('./StandaloneApp.vue'),
  standaloneRouter: router,
  runtimeRoot: RuntimeApp,
  install(app) {
    app.use(pinia);
  },
  onMicroReady(runtime) {
    return bindMangoRuntimeTheme(runtime);
  },
});
```

## 4. 配置说明

默认从 `/runtime-config.json` 读取远程配置。读取失败时，如果 `failClosed` 不是 `true`，会回退到传入的默认配置。

配置结构：

```json
{
  "profile": "hybrid",
  "modules": {
    "mango-workflow": {
      "mode": "local"
    },
    "custom-order": {
      "mode": "micro",
      "runtimeCode": "custom-order"
    }
  }
}
```

`MangoRuntimeConfigLoadOptions`：

| 字段 | 默认值 | 含义 |
|------|--------|------|
| `configUrl` | `/runtime-config.json` | 运行时配置地址。 |
| `failClosed` | `false` | 配置读取或校验失败时是否直接抛错。 |
| `allowedEntryOrigins` | 空 | 允许的微前端入口 origin。 |
| `allowedEntryHosts` | 空 | 允许的微前端入口 host。 |
| `requireEntryAllowlist` | `false` | 是否强制要求 entry 命中白名单。 |
| `allowRelativeEntries` | `false` | 是否允许相对路径 entry。 |
| `allowHttpEntries` | `false` | 是否允许 http entry。 |

`MangoRuntimeConfig`：

| 字段 | 含义 |
|------|------|
| `profile` | 运行模式，支持 `monolith`、`hybrid`、`micro`。 |
| `modules` | 模块运行方式映射。 |
| `diagnostics` | 配置诊断结果，由加载器补充。 |

`MangoModuleRuntimeConfig`：

| 字段 | 含义 |
|------|------|
| `mode` | 模块模式，支持 `local`、`micro`。 |
| `entry` | 微前端入口地址。 |
| `style` | 样式地址。 |
| `runtimeCode` | 对应运行时应用编码。 |
| `appType` | 应用类型，支持 `LOCAL`、`MICRO_APP`、`IFRAME`、`EXTERNAL_LINK`。 |
| `framework` | 子应用框架标识。 |
| `timeoutMs` | 加载超时时间。 |
| `preload` | 是否预加载。 |
| `alive` | wujie 是否保活。 |

## 5. API 与扩展

核心类型：

| 类型 | 用途 |
|------|------|
| `MangoMenu` | Shell 菜单节点。 |
| `MangoAppRuntime` | 传给子应用的运行时上下文。 |
| `MangoRuntimeTheme` | 主题变量。 |
| `MangoRuntimeRequest` | 子应用可用请求函数。 |
| `MangoRuntimeEventBus` | 子应用事件总线。 |
| `MangoFrontendApp` | 本地应用注册结构。 |
| `MangoRuntimeAppConfig` | 运行时应用配置。 |
| `MangoRuntimeConfig` | Shell 运行时配置。 |
| `MangoRuntimeConfigDiagnostic` | 配置诊断项。 |
| `MangoRuntimeError` | 微前端运行错误。 |
| `MangoRuntimeConfigError` | 运行时配置错误。 |

`MangoAppRuntime` 字段：

| 字段 | 含义 |
|------|------|
| `token` | 当前登录 token。 |
| `tenantId` | 当前租户 ID。 |
| `appCode` | 当前应用编码。 |
| `apiBaseUrl` | API 基础地址。 |
| `menu` | 当前菜单。 |
| `userInfo` | 当前用户信息。 |
| `permissions` | 当前权限码。 |
| `theme` | 当前主题。 |
| `request` | `get`、`post`、`put`、`delete` 请求能力。 |
| `eventBus` | `unauthorized`、`theme-change`、`runtime-error` 事件总线。 |

适配器：

| 适配器 | 作用 |
|--------|------|
| `localAdapter` | 挂载已注册的本地应用。 |
| `microAppAdapter` | 通过 wujie 挂载微前端应用。 |
| `iframeAdapter` | 挂载 iframe 页面。 |
| `externalLinkAdapter` | 打开外链。 |

## 6. 数据与初始化

这个包不包含数据库、菜单或权限初始化。

运行期数据来源：

| 数据 | 来源 |
|------|------|
| runtime config | `/runtime-config.json` 或调用方传入的默认配置。 |
| 登录态、租户、用户、权限 | Admin Shell 组装后传入子应用。 |
| 菜单 | Admin Shell 从后端授权菜单接口取得。 |
| 主题 | Admin Shell 主题 store 传入，子应用可通过事件同步。 |

## 7. 管理入口

这个包没有管理页面。它被 `@mango/admin-shell` 用来决定菜单对应的页面如何加载。

## 8. 快速开始

1. Shell 项目使用 `loadRuntimeConfig()` 读取运行时配置。
2. 本地模块用 `registerLocalApp()` 或 `@mango/admin-pages` 注册本地页面。
3. 微前端模块在 runtime config 中配置 `mode: "micro"` 和 `runtimeCode`。
4. Vue 子应用用 `createMangoWujieVueApp()` 同时支持独立运行和 wujie 运行。
5. 子应用通过 `mangoRuntime` 获取请求、主题、用户和权限。

## 9. 问题排查

**runtime-config.json 读取失败但页面仍加载**

默认 `failClosed=false`，读取失败会回退到默认配置。需要失败即阻断时设置 `failClosed: true`。

**微前端入口被拒绝**

检查 `allowedEntryOrigins`、`allowedEntryHosts`、`requireEntryAllowlist`、`allowRelativeEntries`、`allowHttpEntries`。

**子应用拿不到主题**

Vue 子应用需要调用 `bindMangoRuntimeTheme(runtime)`，并确认 Shell 传入了 `mangoRuntime.theme`。

**微前端加载超时**

检查 `timeoutMs`、`entryUrl`、网络访问和 wujie 加载日志。运行时日志事件包括 `micro-app-mount`、`micro-app-error`、`micro-app-timeout`。

## 10. 相关文档

- [Admin Shell README](../admin-shell/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
