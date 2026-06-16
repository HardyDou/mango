# @mango/app-runtime

## 1. 概览
`@mango/app-runtime` 提供 Mango 前端运行时协议：runtime config 解析、本地应用注册、Wujie 微前端挂载、iframe/外链适配、运行日志、事件总线和 Vue 子应用接入工具。

本包是 `admin-shell` 的运行时基础，不是业务页面包。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| Shell 根据 runtime config 决定模块本地渲染或远程加载 | 前端注册 / 组件 / API 封装 |
| 微前端子应用接收 Shell 注入的 token、tenantId、userInfo、permissions、theme、request | 前端注册 / 组件 / API 封装 |
| 子应用需要同时支持独立运行和 Wujie 挂载 | 前端注册 / 组件 / API 封装 |
| 生产环境需要校验远程 entry allowlist、HTTP 限制和 fail closed | 前端注册 / 组件 / API 封装 |
| E2E 或诊断需要读取运行日志和微应用生命周期事件 | 前端注册 / 组件 / API 封装 |

## 3. 适用场景
- Shell 根据 runtime config 决定模块本地渲染或远程加载。
- 微前端子应用接收 Shell 注入的 token、tenantId、userInfo、permissions、theme、request。
- 子应用需要同时支持独立运行和 Wujie 挂载。
- 生产环境需要校验远程 entry allowlist、HTTP 限制和 fail closed。
- E2E 或诊断需要读取运行日志和微应用生命周期事件。

## 4. 边界说明
- 不负责登录页、菜单 UI、布局和 TagsView。
- 不负责业务页面注册；页面 key 由 `@mango/admin-pages` 管理。
- 不负责后端菜单、权限、租户和资源入库。
- 不替代微服务网关、服务发现或后端鉴权。

## 5. 模块组成
核心边界：

- `MangoRuntimeConfig` 描述 profile 和模块加载方式。
- `loadRuntimeConfigWithOptions` 从静态 JSON 合并默认配置并校验。
- `registerLocalApp` 管理本地 app。
- `microAppAdapter` 用 Wujie 加载远程 app。
- `createMangoWujieVueApp` 帮 Vue 子应用兼容独立运行和被 Shell 挂载。
- `MangoAppRuntime` 是 Shell 注入给子应用的上下文。

Shell 决定何时加载 runtime；业务页面只消费注入的 runtime，不应绕过 Shell 自己拉菜单和授权。

## 6. 接入方式
安装：

```bash
pnpm add @mango/app-runtime
```

Shell 侧加载 runtime config：

```ts
import { loadRuntimeConfigWithOptions } from '@mango/app-runtime';

const config = await loadRuntimeConfigWithOptions(
  {
    profile: 'monolith',
    modules: {
      'mango-workflow': { mode: 'local' },
    },
  },
  {
    failClosed: true,
    requireEntryAllowlist: true,
    allowedEntryOrigins: ['https://workflow.example.com'],
  },
);
```

Vue 微前端子应用：

```ts
import { createMangoWujieVueApp, bindMangoRuntimeTheme } from '@mango/app-runtime/vue-micro';

createMangoWujieVueApp({
  standaloneRoot: () => import('./App.vue'),
  standaloneRouter: router,
  runtimeRoot: RuntimeRoot,
  install(app) {
    app.use(pinia);
  },
  onMicroReady(runtime) {
    return bindMangoRuntimeTheme(runtime);
  },
});
```

## 7. 配置说明
### 6.1 Runtime Config

| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 | 源码入口 |
|----------|------------|--------|------|----------|----------|
| `MangoRuntimeConfig` | `profile` | 无 | `monolith`、`hybrid`、`micro` | 描述整体部署形态 | `normalizeRuntimeProfile` |
| `MangoRuntimeConfig` | `modules` | `{}` | moduleCode 到模块配置 | 决定每个模块加载方式 | `normalizeRuntimeConfig` |
| `MangoModuleRuntimeConfig` | `mode` | 无效时 `local` | `local` 或 `micro` | 本地 app 或微应用 | `normalizeRuntimeMode` |
| `MangoModuleRuntimeConfig` | `entry` | 无 | 微应用入口 | `micro` 模式必填 | `validateMicroModule` |
| `MangoModuleRuntimeConfig` | `runtimeCode` | 缺失时警告 | Wujie app name | 区分远程运行单元 | `validateMicroModule` |
| `MangoModuleRuntimeConfig` | `appType` | 按 mode 推导 | `LOCAL` 或 `MICRO_APP` 等 | 选择 adapter | `normalizeRuntimeConfig` |
| `MangoModuleRuntimeConfig` | `framework` | 可选 | 子应用框架标识 | 诊断和部署说明 | 类型定义 |
| `MangoModuleRuntimeConfig` | `timeoutMs` | `15000` | 微应用加载超时 | 超时报 `MangoRuntimeError` | `normalizeTimeout` |
| `MangoModuleRuntimeConfig` | `preload` | 可选 | 是否预加载 | Shell 可调用 preload | `preloadMicroApp` |
| `MangoModuleRuntimeConfig` | `alive` | `false` | Wujie 保活 | 控制 destroy 行为 | `microAppAdapter` |

### 6.2 Load Options

| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 | 源码入口 |
|----------|------------|--------|------|----------|----------|
| `MangoRuntimeConfigLoadOptions` | `configUrl` | `/runtime-config.json` | 静态配置地址 | fetch 目标 | `loadRuntimeConfigWithOptions` |
| `MangoRuntimeConfigLoadOptions` | `failClosed` | `false` | 校验失败是否抛错 | 生产建议 true | `finalizeRuntimeConfig` |
| `MangoRuntimeConfigLoadOptions` | `allowedEntryOrigins` | 空 | 允许的远程 origin | 校验 entry | `isValidRuntimeEntry` |
| `MangoRuntimeConfigLoadOptions` | `allowedEntryHosts` | 空 | 允许的 host | 校验 entry | `isValidRuntimeEntry` |
| `MangoRuntimeConfigLoadOptions` | `requireEntryAllowlist` | `false` | 是否强制白名单 | 无白名单时拒绝远程 entry | `isValidRuntimeEntry` |
| `MangoRuntimeConfigLoadOptions` | `allowRelativeEntries` | 默认允许 | 是否允许相对路径 entry | 支持同域部署 | `isValidRuntimeEntry` |
| `MangoRuntimeConfigLoadOptions` | `allowHttpEntries` | `false` | 是否允许 HTTP | 生产默认应禁止 | `isValidRuntimeEntry` |

### 6.3 App Runtime

| 字段 | 含义 | 子应用使用方式 |
|------|------|----------------|
| `token` | 当前 access token | 只读，不自行持久化 |
| `tenantId` | 当前租户 | 请求和展示上下文 |
| `appCode` | 当前逻辑应用 | 菜单和权限边界 |
| `apiBaseUrl` | API base URL | 子应用请求后端 |
| `menu` | 当前菜单 | 子应用定位当前页面 |
| `userInfo` | 当前用户 | 展示和上下文 |
| `permissions` | 权限码列表 | 前端按钮显示 |
| `theme` | 主题 token | `bindMangoRuntimeTheme` 应用 CSS 变量 |
| `request` | Shell 注入请求对象 | 统一鉴权和错误处理 |
| `eventBus` | runtime 事件总线 | 监听 `unauthorized`、`theme-change`、`runtime-error` |

## 8. API 与扩展
| 导出 | 用途 |
|------|------|
| `registerLocalApp`、`getLocalApp` | 注册和读取本地 app |
| `createRuntimeEventBus` | 创建 runtime 事件总线 |
| `setMangoRuntimeLogger`、`emitMangoRuntimeLog` | 运行日志接入 |
| `loadRuntimeConfig`、`loadRuntimeConfigWithOptions` | 加载 runtime config |
| `mergeRuntimeConfig`、`normalizeRuntimeConfig` | 合并和校验配置 |
| `isValidRuntimeEntry` | 校验远程 entry |
| `localAdapter`、`microAppAdapter`、`iframeAdapter`、`linkAdapter` | 不同 appType 的挂载适配器 |
| `resolveAdapter` | 根据 appType 选择 adapter |
| `preloadMicroApp` | 预加载 Wujie 子应用 |
| `MangoRuntimeError`、`MangoRuntimeConfigError` | 运行时错误类型 |
| `createMangoWujieVueApp` | Vue 子应用双模式启动 |
| `applyMangoRuntimeTheme`、`bindMangoRuntimeTheme` | 主题同步 |

## 9. 数据与初始化
本包不包含数据库 migration。runtime config 是前端静态资产，不是数据库数据。

| 类型 | 位置 | 初始化内容 | 幂等键 / 唯一键 | 生效时机 | 排查入口 |
|------|------|------------|-----------------|----------|----------|
| Runtime config | Shell 静态目录或默认配置 | profile、modules、entry | moduleCode | Shell 启动加载 | runtime config 加载日志 |
| 本地 app | 前端代码注册 | appCode、mount、unmount | appCode | 入口执行注册 | `getLocalApp` |
| 微应用 | 远程 entry | Wujie app bundle | runtimeCode 或 appCode | 菜单打开或预加载 | `__MANGO_MICRO_APP_EVENTS__` |

## 10. 管理入口
Runtime 不初始化菜单权限。它消费 Shell 已经解析出的菜单和上下文。

接入要求：

- `moduleCode` 要能匹配 runtime config 的 module key。
- 微应用必须使用 Shell 注入的 `request` 或至少沿用 `apiBaseUrl`、token 和 tenantId。
- 前端 `permissions` 只用于交互展示，后端接口必须校验。
- 子应用监听 `unauthorized` 时应交给 Shell 统一退出。

## 11. 快速开始
1. Shell 配置 runtime config。
2. 本地模块调用 `registerLocalApp`，远程模块发布 entry。
3. 生产环境配置 allowlist 和 HTTPS。
4. Vue 子应用用 `createMangoWujieVueApp` 接入独立运行和 Wujie 挂载。
5. 子应用使用 Shell 注入的 request、theme 和 eventBus。
6. 执行 runtime 单测、Shell 构建和微前端 E2E。

## 12. 问题排查
| 问题 | 原因 | 处理方式 |
|------|------|----------|
| 远程模块不加载 | `mode=micro` 但缺 `entry` | 补 runtime config |
| 生产环境 entry 被拒绝 | 未配置 allowlist 或使用 HTTP | 配置 HTTPS 和 allowed origins |
| 子应用独立能跑，Shell 挂载失败 | 未实现 Wujie mount/unmount | 使用 `createMangoWujieVueApp` |
| 主题不同步 | 子应用没绑定 runtime theme | 调用 `bindMangoRuntimeTheme` |
| 401 后子应用自己跳转 | 没用 Shell 注入 request / eventBus | 交给 Shell 统一处理 unauthorized |

## 13. 相关文档
- [前端代码规范](../../../mango-pmo/rules/frontend/01-vue-code.md)
- [前端测试规范](../../../mango-pmo/rules/frontend/04-test.md)
- [前端开发流程](../../../mango-pmo/rules/frontend/05-dev-flow.md)
- [Monorepo 架构规范](../../../mango-pmo/rules/frontend/06-monorepo-architecture.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史资料
- [Mango UI README](../../README.md)
- [微前端运行说明](../../docs/micro-frontend-runtime.md)
- [@mango/admin-shell](../admin-shell/README.md)
