# @mango/link-page

## 1. 概览

`@mango/link-page` 是面向保函业务人员的快捷导航首页。页面从 `mango-link` 读取内置导航数据，按“业务相关、工具相关、其他”三组展示卡片，并提供关键词搜索和整卡点击打开能力。

当前版本只做公共链接导航，不包含登录、收藏、个人网址、前台新增、前台编辑、重复提示和卡片级权限控制。

## 2. 功能清单

| 能力 | 说明 |
|------|------|
| 分组导航 | 默认展示“业务相关、工具相关、其他”三组卡片。 |
| 关键词搜索 | 回车或点击搜索按钮后，调用公开导航接口按关键词筛选。 |
| 搜索结果 | 搜索结果显示在分组上方，不再按组拆分。 |
| 整卡点击 | 卡片任意区域可点击，当前版本统一新标签页打开。 |
| 卡片字段 | 只展示 logo、名称、地址、一句话介绍。 |
| Logo 兜底 | 优先使用 `iconUrl`，否则尝试站点 favicon，失败后显示名称首字或前几个字母。 |
| 状态反馈 | 支持加载、失败、空数据、空搜索结果和 logo 加载失败兜底。 |

## 3. 接入方式

`@mango/link-page` 作为独立 Vue 组件接入宿主应用，宿主负责提供后端代理前缀、请求头和路由容器。它只消费 `mango-link` Open API 返回的导航数据，不直接读写数据库，也不注册后台菜单。

## 4. 快速开始

```bash
pnpm add @mango/link-page @mango/link-openapi
```

宿主应用需要提供 `vue` 和 `element-plus`，并引入组件样式。

```ts
import { MangoLinkPage } from '@mango/link-page';
import '@mango/link-page/style.css';
```

```vue
<template>
  <MangoLinkPage
    base-url="/api"
    headline="保函业务快捷入口"
    subtitle="集中访问保函查询、风险核验和常用辅助工具"
    search-placeholder="搜索网站、工具或关键词"
  />
</template>
```

## 5. 配置说明

| 名称 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `baseUrl` | `string` | `''` | Open API 前缀，例如 `/api`。 |
| `headers` | `HeadersInit \| () => HeadersInit \| Promise<HeadersInit>` | - | 请求头，宿主可传租户、认证等上下文。 |
| `credentials` | `RequestCredentials` | `same-origin` | fetch credentials。 |
| `title` | `string` | `保函业务导航` | 页面标题兜底值。 |
| `headline` | `string` | `保函业务快捷入口` | 搜索框上方主文案，可由宿主配置。 |
| `subtitle` | `string` | - | 主文案下方辅助说明。 |
| `searchPlaceholder` | `string` | `搜索网站、工具或关键词` | 搜索框占位文案。 |
| `jumpEnabled` | `boolean` | - | 组件侧跳转开关。未传时尊重后端 `redirectUrl`；`false` 强制直连原始 `url`；`true` 在后端未返回 `redirectUrl` 时补 `/link/open/jump?url=...`。 |

`LinkPageProps` 中保留了部分历史字段用于类型兼容，例如登录、用户信息、搜索引擎配置等；本版本页面不会展示或使用这些交互。

## 6. API 与扩展

| 能力 | 后端路径 | 说明 |
|------|----------|------|
| 查询导航数据 | `GET /link/open/public-links/list` | 初始化页面时调用。 |
| 关键词筛选 | `GET /link/open/public-links/list?keyword=...` | 搜索时调用，后端按名称、地址、简介、标签匹配。 |
| 系统跳转与访问统计 | `GET /link/open/jump?url=...` | 后端开启 jump 时可返回 `redirectUrl`。 |

页面打开地址优先级为 `redirectUrl || url`。当前版本点击后统一使用新标签页打开。

组件对外导出 `MangoLinkPage`、`LinkPage` 和 `LinkPageProps`。宿主可以通过 `baseUrl`、`headers`、`credentials` 注入请求上下文，通过 `headline`、`subtitle`、`searchPlaceholder` 调整首页文案，通过 `opened` 事件记录点击行为。

## 7. 数据与初始化

页面只展示属于以下三组的启用卡片：

| 分组 | 用途 |
|------|------|
| `业务相关` | 保函查询、风险核验、公共资源和采购信息等业务入口。 |
| `工具相关` | 保费测算、快递 H5、电子签署、AI 辅助和搜索工具等入口。 |
| `其他` | 法律服务、知识问答等辅助入口。 |

卡片字段口径：

| 字段 | 展示用途 |
|------|----------|
| `name` | 卡片名称和文字 logo 兜底。 |
| `url` | 卡片地址和默认打开地址。 |
| `summary` | 一句话介绍；为空时不展示。 |
| `iconUrl` | logo 图片；为空或加载失败时走兜底。 |
| `categoryName` | 分组归属。 |
| `sortNo` | 同组排序兜底。 |
| `redirectUrl` | 存在时优先作为打开地址。 |

本期预置数据通过 `mango-link` Flyway migration 初始化到 `link_category` 和 `link_item`，统一使用公共可见数据，不做公私区分。

## 8. 管理入口

`@mango/link-page` 本身不提供管理页面，也不注册菜单。卡片数据维护复用后端 `mango-link` 的 `link_category` 和 `link_item` 能力；本期先通过 Flyway migration 内置数据，后续如开放后台维护，可接入 `@mango/link` 已有的网址分类和网址列表管理入口。

## 9. 事件

| 事件 | 参数 | 说明 |
|------|------|------|
| `opened` | `LinkPublicItem` | 用户点击卡片并准备打开目标地址时触发。 |

## 10. 开发预览

```bash
pnpm --filter @mango/link-page dev
```

预览服务会读取 `vite.preview.config.ts`。常用环境变量：

| 变量 | 说明 |
|------|------|
| `VITE_PORT` | 预览服务端口。 |
| `MANGO_LINK_PAGE_PORT` | 未设置 `VITE_PORT` 时的预览端口。 |
| `VITE_HOST` | 预览服务 host。 |
| `MANGO_BACKEND_PORT` | 后端端口，用于默认代理。 |
| `VITE_ADMIN_PROXY_PATH` | 显式指定后端代理地址。 |

## 11. 问题排查

| 问题 | 优先检查 |
|------|----------|
| 页面显示“暂无导航” | 数据库是否已执行 link 模块 migration；三组分类名称是否为 `业务相关`、`工具相关`、`其他`。 |
| 搜索没有结果 | 后端 `keyword` 匹配字段是否包含名称、地址、简介或标签；接口是否返回三组内的卡片。 |
| logo 不显示 | `iconUrl` 或站点 `/favicon.ico` 可能不可用，页面会自动显示文字 logo。 |
| 点击后地址不符合预期 | 检查接口返回项的 `redirectUrl`、`url` 和组件 `jumpEnabled` 配置。 |
| 内置工具打不开 | 检查宿主是否已接入 `/tools/premium-calculator` 和 `/tools/express-h5` 路由。 |

## 12. 相关文档

- [link-page 业务首页详细设计](../../../mango-docs/designs/2026-07-10-link-page-home-design.md)
- [mango-link 后端模块](../../../mango/mango-platform/mango-link/README.md)
- [@mango/link-openapi](../link-openapi/README.md)
