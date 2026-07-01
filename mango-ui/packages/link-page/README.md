# @mango/link-page

## 1. 概览

`@mango/link-page` 是可嵌入普通页面的网址导航页面。它依赖 `@mango/link-openapi` 调用后端 `mango-link`，按分组标签展示网址图标，并支持登录用户新增个人分组、添加个人网址、收藏和取消收藏。

它适合门户首页、工作台、小组件区域或单页导航站，不依赖 Mango 管理后台菜单运行时。

## 2. 功能清单

| 能力 | 说明 |
|------|------|
| 导航展示 | 按我的收藏、企业导航、个人分组展示网址。 |
| 外部搜索 | 支持默认搜索引擎和可配置搜索引擎列表。 |
| 登录/退出 | 可使用内置 Mango 登录接口，也可由宿主通过 handler 接管。 |
| 收藏网址 | 登录用户走后端收藏接口，匿名用户写入本地 `localStorage`。 |
| 个人网址 | 登录用户可新增个人分组和个人网址。 |
| Logo 展示 | 支持图片 Logo、文字 Logo 和可配置 alt 文案。 |

## 3. 接入方式

```bash
pnpm add @mango/link-page @mango/link-openapi
```

宿主应用需要提供 `vue` 和 `element-plus`。

## 4. 配置说明

| 配置入口 | 字段 / Key | 默认值 | 含义 |
|----------|------------|--------|------|
| 组件 props | `baseUrl` | `''` | 后端 API 前缀，例如 `/api`。 |
| 组件 props | `authenticated` | `false` | 宿主已知的登录状态。 |
| 组件 props | `headers` | - | 登录态请求头。 |
| 组件 props | `jumpEnabled` | - | 控制组件侧是否补出 `/link/open/jump` 跳转地址。 |
| 后端配置 | `mango.link.open.jump.enabled` | `false` | 控制 Open API 是否返回 `redirectUrl`。 |

## 5. 快速开始

在宿主页面引入组件和样式，传入后端 API 前缀、登录态、Logo 和搜索引擎配置后即可渲染网址导航页。

```ts
import { MangoLinkPage } from '@mango/link-page';
import '@mango/link-page/style.css';
```

```vue
<template>
  <MangoLinkPage
    base-url="/api"
    :authenticated="loggedIn"
    :headers="authHeaders"
    logo-url="/brand/logo.png"
    default-search-engine="baidu"
    user-name="Hardy"
    user-account="hardy"
    :login-handler="login"
    :logout-handler="logout"
  />
</template>
```

## 6. 数据与初始化

| 状态 | 数据来源 | 页面能力 |
|------|----------|----------|
| 未登录 | `GET /link/open/public-links/list` + `localStorage` | 展示公开网址；收藏写入本地 `localStorage`。 |
| 已登录 | 同一个接口，带登录态 | 展示当前用户可见的企业导航、我的收藏、我的网址；收藏和个人网址操作走后端接口。 |

打开网址时组件优先使用接口返回的 `redirectUrl`。后端 `/link/open/jump?url=...` 会写入访问记录。只有接口没有返回 `redirectUrl` 且组件无法按 `url` 拼出跳转地址时，组件才回退到原始 `url`。

`mango-link` 后端配置 `mango.link.open.jump.enabled=false` 时，公开导航接口不返回 `redirectUrl`，组件会直接打开原始 `url`。

匿名收藏使用的 key：

```text
mango-link-page:favorites
```

## 7. 管理入口

`@mango/link-page` 没有后台菜单和权限资源。公司网址、公开网址、个人网址和收藏数据由后端 `mango-link` 提供；需要在管理后台维护网址分类和网址列表时，使用 `@mango/link` 注册的管理页面及后端 resource 注入的菜单、component key 和权限码。

## 8. API 与扩展

| 区域 | 说明 |
|------|------|
| 搜索区 | 输入关键字不影响下方网址展示；按回车使用默认搜索引擎打开外部搜索。输入为空时打开搜索引擎首页。 |
| 搜索引擎 | 默认显示百度和 Google 两个按钮；点击按钮立即使用对应搜索引擎搜索，输入为空时打开对应首页。 |
| 分组区 | 固定顺序：`我的收藏` 第一，`企业导航` 第二，个人分组排在后面。 |
| 网址图标 | 优先显示 `iconUrl`；没有时尝试使用网址 origin 的 `/favicon.ico`；仍不可用时显示网址名称首字。 |
| 登录操作 | 未登录时右上角显示“登录”，点击弹出登录窗。默认调用 Mango `/auth/login`，保存 `accessToken`，后续请求自动带 Bearer；也可以通过 `loginHandler` 接管。 |
| 用户入口 | 已登录时右上角显示头像；没有头像时显示账号/用户名首字。点击后显示个人信息卡片和退出按钮。 |
| 退出操作 | 默认调用 Mango `/auth/logout` 并清理本地 token、用户信息和个人分组状态；也可以通过 `logoutHandler` 接管。 |
| 收藏操作 | 网址卡片默认不显示收藏按钮，悬浮到卡片后显示；收藏成功后状态更新，已收藏图标为高亮星标。 |

## 9. Props

| 名称 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `baseUrl` | `string` | `''` | API 前缀，例如 `/api` |
| `headers` | `HeadersInit \| () => HeadersInit \| Promise<HeadersInit>` | - | 登录态请求头 |
| `credentials` | `RequestCredentials` | `same-origin` | fetch credentials |
| `loginUrl` | `string` | - | 未登录时点击登录跳转地址 |
| `title` | `string` | `网址导航` | 页面标题 |
| `logoUrl` | `string` | - | 公司 Logo 地址 |
| `logoText` | `string` | `Mango` | 未配置 `logoUrl` 时显示的文字 Logo |
| `logoAlt` | `string` | `公司 Logo` | Logo 图片 alt 文案 |
| `authenticated` | `boolean` | `false` | 宿主应用已知登录态 |
| `userAvatarUrl` | `string` | - | 当前用户头像地址 |
| `userName` | `string` | - | 当前用户显示名称 |
| `userAccount` | `string` | - | 当前用户账号 |
| `userEmail` | `string` | - | 当前用户邮箱 |
| `userPhone` | `string` | - | 当前用户手机号 |
| `userDepartment` | `string` | - | 当前用户部门 |
| `userRole` | `string` | - | 当前用户角色 |
| `loginHandler` | `(input) => void \| Promise<void>` | - | 登录窗提交时调用，宿主在这里完成真实登录 |
| `logoutHandler` | `() => void \| Promise<void>` | - | 个人信息卡片点击退出时调用，宿主在这里完成真实退出 |
| `jumpEnabled` | `boolean` | - | 组件侧跳转开关。未传时完全尊重后端 `redirectUrl`；`false` 强制直连原始 `url`；`true` 在后端未返回 `redirectUrl` 时按 `/link/open/jump?url=...` 补跳转地址。 |
| `searchEngines` | `LinkPageSearchEngine[]` | 百度、Google | 搜索引擎列表 |
| `defaultSearchEngine` | `string` | `baidu` | 默认搜索引擎 code |

`LinkPageSearchEngine`：

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | `string` | 搜索引擎唯一编码。 |
| `label` | `string` | 下拉显示名称。 |
| `searchUrl` | `string` | 搜索地址，使用 `{keyword}` 作为关键词占位。 |

示例：

```ts
const searchEngines = [
  { code: 'baidu', label: '百度', searchUrl: 'https://www.baidu.com/s?wd={keyword}' },
  { code: 'google', label: 'Google', searchUrl: 'https://www.google.com/search?q={keyword}' },
];
```

## 10. 事件

| 事件 | 参数 | 说明 |
|------|------|------|
| `login` | `{ username, password }` | 登录窗提交时触发。 |
| `logout` | - | 用户点击退出登录时触发。 |
| `created` | - | 新增分组或新增网址成功后触发。 |
| `opened` | `LinkPublicItem` | 打开网址后触发。 |

## 11. 后端依赖

| 能力 | 后端路径 |
|------|----------|
| 查询导航数据 | `GET /link/open/public-links/list` |
| 系统跳转与访问统计 | `GET /link/open/jump?url=...` |
| 登录 | `POST /auth/login` |
| 当前用户信息 | `GET /auth/info` |
| 退出登录 | `POST /auth/logout` |
| 查询个人分组 | `GET /link/personal-categories/list` |
| 新增个人分组 | `POST /link/personal-categories/create` |
| 新增个人网址 | `POST /link/personal-links/create` |
| 收藏网址 | `POST /link/favorites/create` |
| 取消收藏 | `DELETE /link/favorites/delete` |

## 12. 问题排查

| 问题 | 优先检查 |
|------|----------|
| 未登录只看到少量网址 | 这是匿名边界；只有 `PUBLIC` 网址会返回。 |
| 登录后仍只看到公开网址 | `/auth/login` 是否返回 `accessToken`，后续请求是否带 `Authorization: Bearer <token>`。 |
| 点击打开没有访问记录 | 系统配置 `mango.link.open.jump.enabled` 是否为 `true`，接口返回项是否有 `redirectUrl`。 |
| 新增分组失败 | 当前用户是否已登录，后端是否启用 `/link/personal-categories/create`。 |
| 添加网址失败 | URL、名称、分组和登录态是否满足后端校验。 |

## 13. 相关文档

- [mango-link 后端模块](../../../mango/mango-platform/mango-link/README.md)
- [@mango/link-openapi](../link-openapi/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
