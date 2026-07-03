# @mango/home

## 1. 概览

`@mango/home` 提供 Mango 用户多首页工作台的前端 API 契约，负责调用 `mango-home` 后端的首页列表、默认首页解析、创建、重命名、复制、排序、删除和布局保存能力。

本包不渲染首页宿主，不注册工作台小组件。后台 Shell 使用本包配合 `@mango/grid-layout` 和 `@mango/grid-widgets` 完成首页展示与布局编辑。

## 2. 功能清单

| 能力 | 用途 | 对应方法 |
|------|------|----------|
| 查询我的首页 | 获取当前用户首页列表 | `homePageApi.listMyPages` |
| 解析首页 | 获取默认首页或指定首页 | `homePageApi.resolve` |
| 创建首页 | 新建个人首页并可设为默认 | `homePageApi.create` |
| 重命名首页 | 修改首页标题 | `homePageApi.rename` |
| 复制首页 | 复制当前首页布局 | `homePageApi.duplicate` |
| 保存布局 | 持久化布局 JSON | `homePageApi.saveLayout` |
| 首页排序 | 保存首页页签顺序 | `homePageApi.sort` |
| 设置默认首页 | 设置登录后默认首页 | `homePageApi.setDefault` |
| 删除首页 | 删除当前用户首页 | `homePageApi.delete` |

## 3. 接入方式

在前端包或应用中引入：

```ts
import { homePageApi } from '@mango/home';
```

`@mango/admin-shell` 已集成本包。业务应用只需要依赖 `@mango/admin-shell` 首页宿主时，无需直接操作 API；需要自定义首页入口时，可复用本包的类型和请求方法。

## 4. 配置说明

本包不读取运行时配置，也不维护前端环境变量。后端服务地址、鉴权、租户上下文和请求拦截由宿主应用的 HTTP 客户端统一处理。

## 5. API 与扩展

常用调用：

```ts
import { homePageApi } from '@mango/home';

await homePageApi.resolve();
await homePageApi.create({ name: '项目工作台', layoutJson, setDefault: true });
await homePageApi.saveLayout(homeId, { layoutJson });
```

主要类型：

| 类型 | 说明 |
|------|------|
| `HomePageVO` | 首页列表和解析结果 |
| `CreateHomePageCommand` | 创建首页入参 |
| `RenameHomePageCommand` | 重命名首页入参 |
| `SaveHomePageLayoutCommand` | 保存布局入参 |
| `SortHomePagesCommand` | 首页排序入参 |
| `ResolveHomePageQuery` | 解析首页入参 |

## 6. 数据与初始化

本包只传输前端布局 JSON 和首页元数据，不在浏览器本地持久化首页列表或默认首页偏好。首页数据由后端 `mango-home-starter` 写入 `sys_user_home_page` 和 `sys_user_home_preference`。

## 7. 管理入口

本包不注册菜单和路由。后台首页入口由 `@mango/admin-shell` 提供，默认首页路由用于解析当前用户默认首页，带 `homeId` 参数的首页路由用于打开当前用户拥有的指定首页。

## 8. 快速开始

```bash
pnpm -F @mango/home build
```

最小接入步骤：

1. 后端应用引入 `mango-home-starter`。
2. 前端应用依赖 `@mango/home`。
3. 首页宿主在进入页面时调用 `homePageApi.resolve()`。
4. 用户编辑布局后调用 `homePageApi.saveLayout()`。

## 9. 问题排查

| 现象 | 排查项 |
|------|--------|
| 首页列表为空 | 确认当前用户是否已创建个人首页；未创建时首页宿主可使用内置默认布局 |
| 保存布局失败 | 确认后端是否引入 `mango-home-starter`，并检查 `layoutJson` 是否满足后端校验 |
| 指定首页打不开 | 确认 `homeId` 属于当前登录用户且首页未被删除 |

## 10. 相关文档

- [mango-home 后端 README](../../../mango/mango-platform/mango-home/README.md)
- [admin-shell README](../admin-shell/README.md)
- [Mango 能力索引](../../../mango-docs/capabilities/README.md)
