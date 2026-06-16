# @mango/admin-shell

## 1. 概览
`@mango/admin-shell` 是 Mango 管理后台 Shell 包，提供登录态接入、布局、路由、菜单宿主、TagsView、主题、运行时配置和微前端挂载能力。

本包属于 `admin-shell`。它面向管理后台，不适合作为官网、营销页或普通内容站的前端基础。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 创建 Mango 标准管理后台应用 | 前端注册 / 组件 / API 封装 |
| 加载 authorization 菜单树并按 component key 分发页面 | 前端注册 / 组件 / API 封装 |
| 单体模式下渲染本地页面包 | 前端注册 / 组件 / API 封装 |
| 微前端模式下按 runtime config 挂载远程子应用 | 前端注册 / 组件 / API 封装 |
| 接入开发中心、组件调试页、上传页、工作流组件页等管理端开发能力 | 前端注册 / 组件 / API 封装 |

## 3. 适用场景
- 创建 Mango 标准管理后台应用。
- 加载 authorization 菜单树并按 component key 分发页面。
- 单体模式下渲染本地页面包。
- 微前端模式下按 runtime config 挂载远程子应用。
- 接入开发中心、组件调试页、上传页、工作流组件页等管理端开发能力。

## 4. 边界说明
- 不提供业务领域页面本身。
- 不负责后端认证、授权、菜单和租户数据持久化。
- 不替代 `@mango/admin-pages` 的页面注册表。
- 不作为普通 Vue UI 组件库或网站模板使用。

## 5. 模块组成
Shell 负责管理后台“壳”：

- 设置 `@mango/common` request base URL。
- 创建 Vue app 和 router。
- 安装 Pinia、Element Plus、i18n、主题和布局。
- 调用 authorization 菜单接口。
- 追加首页、账号页、隐藏路由和开发中心。
- 加载 runtime config，决定本地页面或远程子应用。

业务包负责：

- 注册自己的页面 key。
- 实现页面交互、API 调用和按钮权限展示。
- 配套后端 resource manifest、菜单、权限和租户校验。

## 6. 接入方式
安装依赖：

```bash
pnpm add @mango/admin-shell
```

入口：

```ts
import { createMangoAdminApp } from '@mango/admin-shell';
import '@mango/admin-shell/style.css';

const { mount } = createMangoAdminApp({
  apiBaseUrl: '/api',
  title: 'Mango Admin',
});

mount('#app');
```

直接使用 Shell 时，业务页面要在 mount 前注册：

```ts
import { registerOrderPages } from '@demo/order';

registerOrderPages();
```

多数业务项目优先使用 `@mango/admin` 聚合包；只有需要定制 Shell 行为时才直接依赖本包。

## 7. 配置说明
`createMangoAdminApp` 接收 `MangoAdminShellOptions`，内部通过 `configureMangoAdminShell` 合并配置。

| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 | 源码入口 |
|----------|------------|--------|------|----------|----------|
| `MangoAdminShellOptions` | `mountTarget` | `#app` | 默认挂载点 | `mount()` 未传参时使用 | `config.ts` |
| `MangoAdminShellOptions` | `apiBaseUrl` | `/api` | 后端 API base URL | `setRequestBaseUrl` 使用 | `createMangoAdminApp` |
| `MangoAdminShellOptions` | `title` | `Mango Admin` | 应用标题 | Shell 展示标题 | `config.ts` |
| `MangoAdminShellOptions` | `contentMode` | `runtime-outlet` | 内容渲染方式 | router-view 或 runtime outlet | `createMangoAdminApp` |
| `MangoAdminShellOptions` | `login` | 空 | 登录页品牌、默认值、插槽 | 传给 `@mango/auth` | `config.ts` |
| `MangoAdminShellOptions` | `modules` | 默认 authorization、system、workflow | runtime 模块覆盖 | 控制本地或远程加载 | `runtimeConfig.ts` |
| `MangoAdminShellOptions` | `localApps` | 空 | 本地 app 定义 | runtime host 可挂载本地能力 | `runtimeHost.ts` |
| `MangoAdminShellOptions` | `features` | 全量启用 | 管理端能力开关 | 过滤菜单和默认页注册 | `menuHost.ts` |
| `MangoAdminShellOptions` | `featureRegistrars` | 空 | 能力注册函数 | Shell 启动时执行 | `featureRegistrars.ts` |
| `MangoAdminShellOptions` | `runtimeConfigUrl` | 默认 runtime config URL | 运行配置地址 | 影响配置加载来源 | `runtimeConfig.ts` |
| `MangoAdminShellOptions` | `runtimeConfigLoadOptions` | 按环境生成 | 远程 entry 安全策略 | 生产 fail closed 和 allowlist | `createShellRuntimeConfigOptions` |
| `MangoAdminShellOptions` | `devCenter.visible` | dev/test 显示，prod 隐藏 | 开发中心可见性 | 追加开发中心菜单 | `shouldShowDevCenter` |
| 环境变量 | `VITE_MANGO_RUNTIME_PROFILE` | `monolith` | 默认运行 profile | 默认 runtime config | `defaultRuntimeConfig` |
| 环境变量 | `VITE_MANGO_RBAC_MODE` | `local` | RBAC 本地或远程 | 影响 authorization 页面加载 | `defaultRuntimeConfig` |
| 环境变量 | `VITE_MANGO_RBAC_ENTRY` | `http://127.0.0.1:5181/` | RBAC 远程入口 | 微前端加载地址 | `defaultRuntimeConfig` |
| 环境变量 | `VITE_MANGO_WORKFLOW_MODE` | `local` | Workflow 本地或远程 | 影响 workflow 页面加载 | `defaultRuntimeConfig` |
| 环境变量 | `VITE_MANGO_WORKFLOW_ENTRY` | `http://127.0.0.1:5182/` | Workflow 远程入口 | 微前端加载地址 | `defaultRuntimeConfig` |
| 环境变量 | `VITE_MANGO_ALLOWED_REMOTE_ORIGINS` | 开发态内置本地域名 | 远程入口 origin 白名单 | 生产类环境必须配置 | `createShellRuntimeConfigOptions` |
| 环境变量 | `VITE_MANGO_ALLOW_HTTP_REMOTE_ENTRIES` | `false` | 是否允许 HTTP entry | 生产类环境默认禁止 | `createShellRuntimeConfigOptions` |
| 环境变量 | `VITE_MANGO_DEPLOY_ENV` | `MODE` | 部署环境 | 控制开发中心和安全策略 | `runtimeConfig.ts`、`menuHost.ts` |

菜单接口固定请求：

```text
GET /authorization/menus/user?fmt=tree&appCode=internal-admin
```

## 8. API 与扩展
| 导出 | 用途 |
|------|------|
| `createMangoAdminApp(options)` | 创建标准管理后台 app |
| `MangoAdminShellApp` | Shell 根组件 |
| `MangoAdminShellView` | Shell 页面视图 |
| `MangoAdminLayout` | 后台布局组件 |
| `MangoAdminParentView` | 父级路由占位组件 |
| `createMangoAdminRouter()` | 创建 Shell 路由 |
| `getShellPinia()` | 获取 Shell Pinia 实例 |
| `installShellApp(app, options)` | 给已有 app 安装 Shell 能力 |
| `configureMangoAdminShell(options)` | 合并 Shell 配置 |
| `getMangoAdminShellOptions()` | 读取当前配置 |
| `useMenuHost()` | 加载和管理菜单树 |
| `loadShellRuntimeConfig()` | 加载运行配置 |
| `runtimeHost` exports | runtime outlet、远程挂载、未授权处理 |

`package.json` 还导出 `./runtime`、`./menu`、`./stores`、`./router`、`./home`、开发页入口和 `./style.css`。

## 9. 数据与初始化
本包不包含数据库 migration。它读取后端初始化好的菜单、权限、应用和租户绑定数据。

| 类型 | 位置 | 初始化内容 | 幂等键 / 唯一键 | 生效时机 | 排查入口 |
|------|------|------------|-----------------|----------|----------|
| 首页路由 | Shell 内置 | `/home`、`home/index` | `shell:home` | 菜单加载时追加 | 登录后默认首页 |
| 账号隐藏路由 | Shell 内置 | `/profile`、`/password` | `account:profile`、`account:password` | 菜单加载时追加 | 个人中心和修改密码可打开 |
| 开发中心 | Shell 内置 | `/develop` 下组件页 | `shell:develop` | dev/test 环境追加 | 开发态菜单可见 |
| 后端菜单 | authorization 接口 | 用户菜单树 | 后端菜单 code | 登录后加载 | 菜单树显示 |
| 远程运行配置 | runtime config | 模块 mode、entry、runtimeCode | module code | Shell 启动时加载 | 远程子应用可挂载 |

## 10. 管理入口
Shell 菜单字段契约：

| 字段 | 作用 |
|------|------|
| `appCode` | 管理后台应用，当前固定使用 `internal-admin` |
| `moduleCode` | 页面归属模块，用来匹配 feature 和 runtime 模块 |
| `menuType` | `DIRECTORY`、`MENU`、`BUTTON` |
| `path` | 前端路由路径 |
| `component` | 页面 component key |
| `pageType` | 本地路由、iframe 或外链 |
| `visible` | 是否显示在菜单 |
| `keepAlive` | 是否缓存页面 |

Shell 会过滤按钮菜单，只把目录和页面转为路由。菜单显示不代表接口有权限；按钮权限和租户数据必须由后端接口校验。

## 11. 快速开始
1. 业务后台安装 `@mango/admin-shell` 或使用 `@mango/admin` 聚合包。
2. 在入口创建 app 前注册业务页面。
3. 设置 `apiBaseUrl` 指向后端或网关。
4. 后端初始化 `internal-admin` 下的菜单、权限和角色授权。
5. 确认 `moduleCode`、`component`、runtime config module code 三者一致。
6. 登录后台，验证菜单、页面、接口权限、租户数据和远程子应用加载。

## 12. 问题排查
| 问题 | 原因 | 处理方式 |
|------|------|----------|
| 菜单不显示 | authorization 接口无数据、appCode 不一致或 feature 被关闭 | 查菜单接口和 `features` 配置 |
| 菜单打开空白 | component 没注册 | 查 `@mango/admin-pages` 注册表 |
| 远程页面加载失败 | entry 不在 allowlist、地址不可达或 HTTP 被禁止 | 查 runtime config 和安全策略 |
| 开发中心出现在生产 | `devCenter.visible` 强制为 true 或部署环境配置错 | 修正 `VITE_MANGO_DEPLOY_ENV` |
| Vue 或 Pinia 实例异常 | peer 依赖重复安装 | 统一宿主依赖版本 |

## 13. 相关文档
- [前端代码规范](../../../mango-pmo/rules/frontend/01-vue-code.md)
- [Element Plus UI 规范](../../../mango-pmo/rules/frontend/02-element-plus-ui.md)
- [前端测试规范](../../../mango-pmo/rules/frontend/04-test.md)
- [Monorepo 架构规范](../../../mango-pmo/rules/frontend/06-monorepo-architecture.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史资料
- [Mango UI README](../../README.md)
- [微前端运行说明](../../docs/micro-frontend-runtime.md)
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
