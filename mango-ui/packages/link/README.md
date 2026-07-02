# @mango/link

## 1. 概览

`@mango/link` 是网址导航的管理后台前端包，配套后端 `mango-link` 使用。

它提供：

- 用户侧页面：我的分类、我的收藏、我的网址。
- 后台管理页面：网址分类、网址列表。
- 前端 API 封装：`linkApi`。
- admin-pages 页面注册入口：`registerMangoLinkAdminPages`。
- 首页网址导航小组件：`@mango/link/widgets/link-navigation`。
  小组件由 `registerMangoLinkAdminPages()` 返回注册信息，业务域为 `mango-link / 链接`，分组为 `工作台`。

它不是独立门户组件。非管理后台页面应使用 `@mango/link-page` 或 `@mango/link-openapi`。

## 2. 功能清单

| 能力 | 入口 | 说明 |
|------|------|------|
| 我的分类 | `link/company/index` | 展示当前用户可见的分类，支持查看可见的网址分组。 |
| 我的收藏 | `link/favorites/index` | 展示、打开和取消收藏当前用户收藏的网址。 |
| 我的网址 | `link/my-links/index` | 维护当前用户的个人网址。 |
| 网址分类管理 | `link/categories/index` | 管理公司网址分类。 |
| 网址列表管理 | `link/items/index` | 管理公司、部门、指定人可见的网址。 |
| 前端 API | `linkApi` | 封装后台管理和用户侧网址接口。 |
| 首页网址导航小组件 | `@mango/link/widgets/link-navigation` | 工作台首页搜索与网址入口小组件，组件实现和样式归属 link 包。 |

## 3. 接入方式

```bash
pnpm add @mango/link @mango/link-openapi
```

宿主应用需要提供 `vue`、`element-plus`、`@mango/common` 和 `@mango/admin-pages`。

## 4. 配置说明

| 配置入口 | 字段 / Key | 默认值 | 含义 |
|----------|------------|--------|------|
| 页面注册 | `registerMangoLinkAdminPages()` | - | 把 `link/*/index` 页面 key 注册到 admin-pages，并返回首页组件注册信息（`system.link-navigation`）。 |
| 小组件子入口 | `@mango/link/widgets/link-navigation` | - | 导出 `LinkNavigationWidget`、`linkNavigationWidgets` 和小组件 props 类型。 |
| 样式入口 | `@mango/link/style.css` | - | 加载网址导航管理页样式。 |
| 后端配置 | `mango.link.open.jump.enabled` | `false` | 控制打开网址时是否经过后端跳转并记录访问。 |

## 5. 快速开始

```ts
import { registerMangoLinkAdminPages } from '@mango/link/admin-pages';
import '@mango/link/style.css';

registerMangoLinkAdminPages();
```

在 admin-shell 中，推荐把 `registerMangoLinkAdminPages` 放入 `featureRegistrars`。集成 `@mango/link` 后，首页组件面板会自动出现 `链接 / 工作台 / 网址导航`；未集成该包时不会注册该小组件，历史布局中保存的 `system.link-navigation` 会按布局组件的失效组件逻辑处理。

独立消费小组件类型或定义时使用子入口：

```ts
import { LinkNavigationWidget, linkNavigationWidgets } from '@mango/link/widgets/link-navigation';
```

页面 key：

| key | 页面 |
|-----|------|
| `link/company/index` | 我的分类 |
| `link/favorites/index` | 我的收藏 |
| `link/my-links/index` | 我的网址 |
| `link/categories/index` | 网址分类 |
| `link/items/index` | 网址列表 |

## 6. 管理入口

| 菜单 | 路由 | 页面 key |
|------|------|----------|
| 网址导航 / 我的分类 | `/link/company` | `link/company/index` |
| 网址导航 / 我的收藏 | `/link/favorites` | `link/favorites/index` |
| 网址导航 / 我的网址 | `/link/my-links` | `link/my-links/index` |
| 平台能力 / 网址管理 / 网址分类 | `/data/link/categories` | `link/categories/index` |
| 平台能力 / 网址管理 / 网址列表 | `/data/link/items` | `link/items/index` |

菜单和权限由后端 `mango-link` 的 resource 文件注入。页面注册只解决 component key 到 Vue 页面之间的映射。

## 7. 数据与初始化

| 数据 | 来源 | 说明 |
|------|------|------|
| 菜单和权限 | `mango-link-starter` resource 声明 | 注入 `网址导航` 和 `网址管理` 菜单。 |
| 页面 key | `@mango/link/admin-pages` | 注册 `link/company/index`、`link/items/index` 等 Vue 页面。 |
| 首页小组件 | `@mango/link/widgets/link-navigation` | 通过 `registerMangoLinkAdminPages()` 返回给 admin-shell，按 `mango-link / 链接` 业务域展示。 |
| 网址数据 | `mango-link` 后端 | 页面不持久化业务数据，所有列表、收藏和个人网址操作来自后端接口。 |

## 8. API 与扩展

| 页面 | 查询条件 | 主要字段 | 操作 |
|------|----------|----------|------|
| 我的分类 | 关键字 | 分类名称/分类说明 | 查看分类列表 |
| 我的收藏 | 关键字 | 名称、URL、分类、标签、收藏时间 | 打开、取消收藏 |
| 我的网址 | 关键字 | 名称、URL、说明、标签、更新时间 | 新增、编辑、删除、打开 |
| 网址分类 | 关键字、状态 | 分类名称、说明、排序、状态、更新时间 | 新增、编辑、启停、删除 |
| 网址列表 | 关键字、分类、可见范围、状态 | 名称、URL、分类、可见范围、标签、状态、更新时间 | 新增、编辑、启停、删除、打开 |

页面里的“打开”会使用 `/api/link/open/redirect/{id}`，由后端统一跳转并记录访问。后台网址列表里如果当前账号不满足该网址的可见范围，跳转接口会返回不可见。

首页网址导航小组件默认复用菜单页同一组用户侧接口读取数据：企业网址来自 `/link/company-links/list`，我的网址来自 `/link/personal-links/page`，我的收藏来自 `/link/favorites/list`，个人分类来自 `/link/personal-categories/list`。不要为小组件单独维护另一套数据口径。

## 9. 前端 API

`linkApi` 封装的后端路径：

| 方法 | 后端路径 |
|------|----------|
| `pageCategories` | `/link/categories/page` |
| `listCategories` | `/link/categories/list` |
| `createCategory` | `/link/categories/create` |
| `updateCategory` | `/link/categories/update` |
| `enableCategory` / `disableCategory` | `/link/categories/enable`、`/link/categories/disable` |
| `deleteCategory` | `/link/categories/delete` |
| `pageItems` | `/link/items/page` |
| `createItem` | `/link/items/create` |
| `updateItem` | `/link/items/update` |
| `enableItem` / `disableItem` | `/link/items/enable`、`/link/items/disable` |
| `deleteItem` | `/link/items/delete` |
| `listCompanyLinks` | `/link/company-links/list` |
| `listFavorites` | `/link/favorites/list` |
| `createFavorite` | `/link/favorites/create` |
| `deleteFavorite` | `/link/favorites/delete` |
| `pagePersonalItems` | `/link/personal-links/page` |
| `createPersonalItem` | `/link/personal-links/create` |
| `updatePersonalItem` | `/link/personal-links/update` |
| `deletePersonalItem` | `/link/personal-links/delete` |

辅助方法：

| 方法 | 说明 |
|------|------|
| `linkRedirectUrl(id, source)` | 生成 `/api/link/open/redirect/{id}?source=...`。 |
| `openLinkWithRedirect(item, source)` | 新窗口打开系统跳转地址。 |
| `navigationSourceOf(scope)` | 根据可见范围推导跳转来源。 |

## 10. 后端依赖

| 依赖 | 说明 |
|------|------|
| `mango-link-starter` | 提供 `/link/**` 接口、数据表和菜单 resource。 |
| `mango-authorization` | 提供菜单、权限和角色授权。 |
| `mango-identity` | 提供登录用户、租户和成员关系。 |

## 11. 问题排查

| 问题 | 优先检查 |
|------|----------|
| 菜单看不到 | 后端 resource 是否同步，角色是否获得菜单套餐或权限。 |
| 页面打不开 | 是否调用 `registerMangoLinkAdminPages()`，菜单 component 是否匹配页面 key。 |
| 首页组件面板没有网址导航 | 检查宿主是否集成 `@mango/link`，并把 `registerMangoLinkAdminPages` 传入 admin-shell `featureRegistrars`。 |
| 页面请求 404 | 后端是否启用 `mango-link-starter`，网关是否代理 `/link/**`。 |
| 页面请求 401/403 | 登录态、角色权限、网址可见范围是否满足。 |
| 打开网址没有访问记录 | 页面是否走 `openLinkWithRedirect`，请求路径是否为 `/api/link/open/redirect/{id}`。 |

## 12. 相关文档

- [mango-link 后端模块](../../../mango/mango-platform/mango-link/README.md)
- [@mango/link-openapi](../link-openapi/README.md)
- [@mango/link-page](../link-page/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
