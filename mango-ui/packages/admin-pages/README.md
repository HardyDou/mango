# @mango/admin-pages

`@mango/admin-pages` 是 Mango 管理后台的本地页面注册表。菜单从后端返回 `moduleCode` 和 `component` 后，Admin Shell 通过这个包找到对应 Vue 页面并渲染。

## 1. 概览

这个包属于 `admin-shell` 配套能力，不是业务页面组件库。业务模块要把自己的管理页面接入 Mango Admin 时，通常写一个 `admin-pages` 子入口，并调用这里的 `registerModulePages()`。

## 2. 功能清单

| 能力 | 入口 |
|------|------|
| 注册模块页面 | `registerModulePages()` |
| 注册单个页面 | `registerPage()` |
| 注册 Shell 内置页面 | `registerShellPages()` |
| 按菜单 component 找页面 loader | `getPageLoader()` |
| 从 component 或 path 反查模块 | `resolvePageModuleCode()` |
| 注册隐藏动态路由 | `getRegisteredPageRoutes()` |
| 注册默认系统页面 | `registerDefaultAdminPages()` |
| 控制内置能力集合 | `resolveMangoAdminFeatures()` |
| 注册通知铃铛提供方 | `registerMangoNoticeBellProvider()` |

## 3. 接入方式

业务前端包通常提供一个 `admin-pages` 子入口：

```ts
import { registerModulePages } from '@mango/admin-pages/core';

let registered = false;

export function registerExampleAdminPages() {
  if (registered) {
    return;
  }
  registered = true;
  registerModulePages({
    moduleCode: 'mango-system',
    pages: {
      'system/dict/index': () => import('@mango/system').then(m => m.DictView),
    },
  });
}
```

Admin Shell 启动时调用注册函数：

```ts
import { registerExampleAdminPages } from './admin-pages';

registerExampleAdminPages();
```

后端菜单的 `component` 可以写成 `@/views/system/dict/index.vue`、`views/system/dict/index.vue` 或 `system/dict/index`。注册表会归一化为 `system/dict/index`。

## 4. 配置说明

页面注册配置：

| 字段 | 含义 |
|------|------|
| `moduleCode` | 模块编码，需要和后端菜单的 `moduleCode` 或模块归属一致。 |
| `pages` | 页面 key 到异步 loader 的映射。 |
| `routes` | 可选隐藏路由，适合详情页、弹出式页面和非菜单页。 |

隐藏路由配置：

| 字段 | 默认值 | 含义 |
|------|--------|------|
| `path` | 无 | 路由 path。 |
| `component` | 无 | 页面 key。 |
| `menuName` | 空 | 路由展示名。 |
| `menuCode` | 空 | 权限码或路由编码。 |
| `icon` | 空 | 图标。 |
| `sort` | 空 | 排序。 |
| `visible` | `0` | 是否可见。 |
| `keepAlive` | `0` | 是否缓存。 |

默认能力配置：

| 配置 | 含义 |
|------|------|
| `features: 'core'` | 只注册 authorization、system。 |
| `features: 'full'` | 注册 core 和 workflow、file、template、notice、numgen、calendar、job。 |
| `features: string[]` | core 永远启用，数组内能力额外启用。 |
| `features: Record<string, boolean>` | core 永远启用，值为 `true` 的能力额外启用。 |

## 5. API 与扩展

| API | 作用 |
|-----|------|
| `normalizeComponentPath(componentPath)` | 去掉 `@/`、`src/`、`views/`、`.vue`，得到页面 key。 |
| `registerModulePages(registry)` | 注册一个模块的一组页面和隐藏路由。 |
| `registerPage(moduleCode, component, loader)` | 注册单个页面。 |
| `registerShellPages(loaders)` | 注册首页和 404 等 Shell 内置页面。 |
| `getPageLoader(moduleCode, component)` | 优先按模块查找页面 loader；未传模块时全局查找。 |
| `resolvePageModuleCode(component, path)` | 根据菜单 component 或 path 推断模块编码。 |
| `getRegisteredPageRoutes(moduleCodes)` | 读取已注册的隐藏路由。 |
| `registerDefaultAdminPages(options)` | 注册 Mango 内置 authorization、system 页面和自定义 registries。 |
| `registerMangoNoticeBellProvider(provider)` | 注册通知铃铛组件和提醒配置读取函数。 |
| `getMangoNoticeBellProvider()` | 获取通知铃铛提供方。 |

## 6. 数据与初始化

这个包不访问数据库，也不写菜单数据。菜单、按钮和权限由后端模块 migration 或初始化逻辑写入 `authorization_menu`。

运行时数据来源：

| 数据 | 来源 |
|------|------|
| 菜单树 | Admin Shell 请求 `/authorization/menus/user`。 |
| 页面 loader | 各前端包调用 `registerModulePages()` 写入内存注册表。 |
| 隐藏路由 | 各前端包注册 `routes`。 |
| 默认系统页面 | `registerDefaultAdminPages()` 注册。 |

## 7. 管理入口

这个包本身没有管理页面。它服务于所有 Mango Admin 管理入口：后端菜单 `component` 最终必须能匹配到已注册页面 key。

## 8. 快速开始

1. 在业务前端包中新增 `admin-pages` 子入口。
2. 调用 `registerModulePages()` 注册页面 key。
3. 确保后端菜单 `moduleCode` 和 `component` 能匹配注册项。
4. 在 Shell 启动时调用业务包注册函数。
5. 打开菜单时，Shell 会用 `getPageLoader()` 加载页面。

## 9. 问题排查

**菜单打开后是 404**

检查后端菜单 `component` 归一化后是否等于注册的页面 key，例如 `@/views/system/dict/index.vue` 会变成 `system/dict/index`。

**同一个页面注册多次**

注册函数应使用本地 `registered` 标记保证幂等。重复注册同一个 key 会覆盖旧 loader。

**隐藏详情页无法访问**

确认注册 `routes` 时 `path` 以 `/` 开头或能被自动补齐，`component` 能匹配 `pages` 中的页面 key。

## 10. 相关文档

- [Admin Shell README](../admin-shell/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
