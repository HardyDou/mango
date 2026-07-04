# @mango/home

## 1. 概览

`@mango/home` 提供 Mango 用户多首页工作台的前端 API 契约，负责调用 `mango-home` 后端的个人首页、模板草稿、模板发布、授权和用户最终视图能力。

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
| 后台维护用户首页 | 重命名、保存布局、单条或批量删除租户内用户首页 | `homePageApi.adminRename` / `homePageApi.adminSaveLayout` / `homePageApi.adminDelete` / `homePageApi.adminBatchDelete` |
| 查询模板 | 获取后台首页模板列表 | `homeTemplateApi.list` |
| 编辑模板草稿 | 新建或保存模板草稿布局 | `homeTemplateApi.create` / `homeTemplateApi.updateDraft` |
| 发布模板 | 发布草稿，授权用户生效 | `homeTemplateApi.publish` |
| 模板授权 | 保存个人、部门、角色授权 | `homeTemplateApi.saveAuthorizations` |
| 用户最终视图 | 查询指定用户可见首页集合 | `homeTemplateApi.resolveUserPages` |

## 3. 接入方式

在前端包或应用中引入：

```ts
import { homePageApi, homeTemplateApi } from '@mango/home';
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
await homeTemplateApi.publish(templateId);
```

主要类型：

| 类型 | 说明 |
|------|------|
| `HomePageVO` | 首页列表和解析结果 |
| `CreateHomePageCommand` | 创建首页入参 |
| `RenameHomePageCommand` | 重命名首页入参 |
| `SaveHomePageLayoutCommand` | 保存布局入参 |
| `SortHomePagesCommand` | 首页排序入参 |
| `SetDefaultHomePageCommand` | 默认首页入参，`homeId` 支持个人首页 ID 或 `template:{id}` |
| `ResolveHomePageQuery` | 解析首页入参 |
| `HomeTemplateVO` | 首页模板视图 |
| `HomeTemplateAuthorizationVO` | 首页模板授权视图 |
| `UserHomeViewQuery` | 后台用户最终首页查询入参 |

## 6. 数据与初始化

本包只传输前端布局 JSON 和首页元数据，不在浏览器本地持久化首页列表或默认首页偏好。首页数据由后端 `mango-home-starter` 写入 `sys_user_home_page`、`sys_user_home_preference`、`sys_home_template`、`sys_home_template_version` 和 `sys_home_template_authorization`。

## 7. 管理入口

本包不注册菜单和路由。后台首页入口由 `@mango/admin-shell` 提供，默认首页路由用于解析当前用户默认首页，带 `homeId` 参数的首页路由用于打开当前用户拥有的指定首页。模板管理、首页列表和用户首页菜单由后端资源注册到 `平台能力 / 首页管理`：

| 菜单 | 组件 key | 用途 |
|------|----------|------|
| 首页模板 | `home/templates/index` | 管理模板草稿、复制、发布、启停和授权 |
| 首页列表 | `home/list/index` | 按用户查询、预览、编辑、单条或批量删除用户自定义首页 |
| 用户首页 | `home/user/index` | 输入或选择用户后查看该用户最终可见首页 |

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
