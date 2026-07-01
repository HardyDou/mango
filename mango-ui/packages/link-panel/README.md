# @mango/link-panel

## 1. 概览

`@mango/link-panel` 是网址导航页面的兼容包。新项目应优先使用 `@mango/link-page`；本包继续导出 `LinkPanel` 和 `MangoLinkPanel`，它们与 `@mango/link-page` 的 `LinkPage`、`MangoLinkPage` 指向同一个真实页面组件。

## 2. 功能清单

| 能力 | 说明 |
|------|------|
| 兼容导出 | 保留 `LinkPanel`、`MangoLinkPanel` 旧导出名。 |
| 组件复用 | 复用 `@mango/link-page` 的网址导航页面实现。 |
| 样式入口 | 提供 `@mango/link-panel/style.css` 兼容入口。 |
| 后端访问 | 通过 `@mango/link-openapi` 调用 `mango-link` 后端接口。 |

## 3. 接入方式

推荐新项目直接接入：

```ts
import { MangoLinkPage } from '@mango/link-page';
import '@mango/link-page/style.css';
```

历史项目可继续接入：

```ts
import { MangoLinkPanel } from '@mango/link-panel';
import '@mango/link-panel/style.css';
```

## 4. 配置说明

`MangoLinkPanel` 透传 `@mango/link-page` 的组件 props，包括 `baseUrl`、`headers`、`authenticated`、`logoUrl`、`jumpEnabled`、`loginHandler`、`logoutHandler` 和 `searchEngines`。后端跳转统计仍由 `mango.link.open.jump.enabled` 控制。

## 5. API 与扩展

| 导出 | 说明 |
|------|------|
| `LinkPanel` | 兼容旧命名的页面组件。 |
| `MangoLinkPanel` | 兼容旧命名的页面组件别名。 |
| `LinkPage` | 来自 `@mango/link-page` 的真实页面组件。 |
| `MangoLinkPage` | 来自 `@mango/link-page` 的真实页面组件别名。 |

## 6. 数据与初始化

本包不持久化业务数据，也不提供初始化脚本。网址分类、网址列表、收藏、个人网址、菜单、权限和系统配置均由后端 `mango-link` 初始化和提供。

## 7. 管理入口

本包没有菜单和管理页面。管理后台入口由 `@mango/link` 提供，后端 `mango-link` 通过 resource 声明注入菜单、component key、权限码和租户边界。

## 8. 快速开始

历史项目只需要把旧的 `MangoLinkPanel` 继续渲染在原位置，并确认宿主已安装 `@mango/link-panel`、`@mango/link-page`、`@mango/link-openapi`、`vue` 和 `element-plus`。

## 9. 问题排查

| 问题 | 优先检查 |
|------|----------|
| 组件 props 不生效 | 对照 `@mango/link-page` README，确认 props 名称仍然匹配真实页面组件。 |
| 样式缺失 | 是否引入 `@mango/link-panel/style.css` 或 `@mango/link-page/style.css`。 |
| 数据为空 | 后端 `mango-link` 是否启用，`baseUrl` 和登录态请求头是否正确。 |

## 10. 相关文档

- [@mango/link-page](../link-page/README.md)
- [@mango/link-openapi](../link-openapi/README.md)
- [mango-link 后端模块](../../../mango/mango-platform/mango-link/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
