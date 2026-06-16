# @mango/rbac

## 1. 概览

`@mango/rbac` 是 Mango 管理后台的授权管理前端包，提供应用、菜单、菜单包、角色、用户、组织、岗位和权限资源页面，并导出对应 API 封装。

集成形态：

| 标识 | 说明 |
|------|------|
| `admin-pages` | 授权、用户、组织和岗位管理页面，通常由 `@mango/admin-pages` 默认注册。 |
| `api-client` | authorization、identity、org、post 相关 API 封装。 |

它不做后端鉴权判定，也不会初始化菜单、角色、用户或组织。前端只展示和提交管理操作，最终权限、租户和数据范围由后端校验。

## 2. 功能清单

| 能力 | 使用入口 | 后端依赖 |
|------|----------|----------|
| 应用管理 | `AppView`、`appApi` | `mango-authorization` |
| 应用模块绑定和运行策略 | `appModuleApi` | `mango-authorization`、`@mango/app-runtime` |
| 菜单和按钮资源 | `MenuView`、`menuApi` | `mango-authorization` |
| 菜单包 | `MenuPackageView`、`menuPackageApi` | `mango-authorization` |
| 角色和菜单授权 | `RoleView`、`roleApi` | `mango-authorization` |
| 用户管理 | `UserView`、`userApi` | `mango-identity` |
| 组织和成员 | `OrgView`、`orgApi` | `mango-org` |
| 岗位管理 | `PostView`、`postApi` | `mango-org` |
| 权限资源查看 | `PermissionView` | `mango-authorization` |

## 3. 接入方式

开发依赖：

```bash
pnpm add @mango/rbac
```

宿主应用需要提供 Vue、Vue Router、Element Plus，并接入 `@mango/common` 请求上下文。部署时需要启用 authorization、identity、org 后端能力。

引入页面和样式：

```ts
import '@mango/rbac/style.css';
import { AppView, MenuView, RoleView, UserView } from '@mango/rbac';
```

Mango Admin 默认页面注册通常由 `@mango/admin-pages` 完成：

```ts
import { registerDefaultAdminPages } from '@mango/admin-pages';

registerDefaultAdminPages();
```

直接调用 API：

```ts
import { menuApi, roleApi, userApi } from '@mango/rbac';

const menus = await menuApi.tree({ appCode: 'internal-admin' });
await roleApi.assignMenus(roleId, menuIds);
const users = await userApi.page({ pageNum: 1, pageSize: 20 });
```

## 4. 配置说明

`@mango/rbac` 没有独立运行时配置文件。主要行为由宿主请求配置、菜单 component key、API 参数和后端数据决定。

| 配置位置 | 字段 | 含义 |
|----------|------|------|
| 宿主应用 | API baseURL / 代理 | 决定 `/authorization/**`、`/identity/**`、`/org/**`、`/post/**` 请求转发目标。 |
| authorization 应用 | `appCode` | 应用授权边界，管理端通常是 `internal-admin`。 |
| 菜单数据 | `component` | 必须匹配前端页面 key，Shell 才能打开页面。 |
| 菜单数据 | `menuType` | 区分目录、菜单、按钮。 |
| 角色数据 | `realm`、`actorType` | 角色作用域。 |
| 用户数据 | `realm`、`actorType`、`partyType`、`partyId` | 登录身份上下文。 |
| 组织查询 | `parentId`、`type`、`includeDisabled` | 组织树过滤条件。 |

## 5. API 与扩展

页面导出：

| 导出 | 默认页面 key | 管理能力 |
|------|--------------|----------|
| `AppView` | `system/app/index` | 应用管理。 |
| `MenuView` | `system/menu/index` | 菜单和按钮资源管理。 |
| `MenuPackageView` | `system/menu-package/index` | 菜单包管理。 |
| `RoleView` | `system/role/index` | 角色和菜单授权。 |
| `UserView` | `system/user/index` | 用户、企微同步、外部身份绑定。 |
| `OrgView` | `system/org/index` | 组织树和组织成员。 |
| `PostView` | `system/post/index` | 岗位管理。 |
| `PermissionView` | `system/permission/index` | 权限资源查看。 |

主要 API：

| API | 主要接口 | 能力 |
|-----|----------|------|
| `appApi` | `/authorization/apps` | 应用列表、详情、创建、更新、删除、运行时应用。 |
| `appModuleApi` | `/authorization/app-modules` | 应用模块绑定、同步菜单、运行策略。 |
| `menuApi` | `/authorization/menus` | 用户菜单、菜单树、详情、创建、更新、删除。 |
| `menuPackageApi` | `/authorization/menu-packages` | 菜单包 CRUD。 |
| `roleApi` | `/authorization/roles` | 角色 CRUD、角色菜单、可分配菜单、主体角色绑定。 |
| `userApi` | `/identity/users/page` | 用户分页、详情、创建、更新、删除、重置密码、企微同步、外部身份绑定。 |
| `orgApi` | `/org/tree` | 组织树、子节点、详情、成员、负责人。 |
| `postApi` | `/post/page` | 岗位分页、详情、创建、更新、删除。 |

常用返回字段：

| 数据 | 字段 |
|------|------|
| 应用 | `appCode`、`appName`、`appType`、`deployMode`、`entryUrl` |
| 菜单 | `id`、`parentId`、`menuType`、`path`、`component`、`perms`、`moduleCode` |
| 角色 | `id`、`roleName`、`roleCode`、`appCode`、`realm`、`actorType` |
| 用户 | `userId`、`username`、`nickname`、`status`、`tenantId` |
| 组织 | `id`、`name`、`parentId`、`sort`、`children` |
| 岗位 | `id`、`postCode`、`postName`、`sort`、`status` |

## 6. 数据与初始化

`@mango/rbac` 不包含 migration。接入前要确认：

| 数据 | 来源 | 前端消费 |
|------|------|----------|
| 应用 | `mango-authorization` | 应用管理、登录授权边界。 |
| 菜单和按钮 | authorization resource manifest 或初始化脚本 | 菜单管理、Shell 菜单、按钮权限。 |
| 菜单包 | `mango-authorization` | 租户授权和菜单套餐。 |
| 角色和授权 | `mango-authorization` | 角色管理、角色菜单、主体角色。 |
| 用户和身份 | `mango-identity` | 用户管理、登录、外部身份绑定。 |
| 组织和岗位 | `mango-org` | 组织管理、岗位管理、用户组织关系。 |

业务模块菜单和权限应该由对应模块的 resource manifest 或后端初始化流程入库，不应该在前端手工补假数据。

## 7. 管理入口

菜单的 component 字段应与上面的默认页面 key 保持一致。访问控制分两层：

| 层级 | 说明 |
|------|------|
| 前端展示 | Shell 根据用户菜单和按钮权限决定显示哪些入口。 |
| 后端校验 | 每个 `/authorization/**`、`/identity/**`、`/org/**`、`/post/**` 接口继续校验登录态、租户、角色和数据范围。 |

## 8. 快速开始

1. 后端启用 authorization、identity、org。
2. 初始化 `internal-admin` 应用、基础菜单、角色、管理员用户和组织。
3. 前端引入 `@mango/rbac/style.css`。
4. 调用 `registerDefaultAdminPages()` 或手工把页面 key 映射到导出组件。
5. 给测试用户分配角色和菜单。
6. 登录后确认菜单、页面、按钮和接口权限一致。

## 9. 问题排查

| 问题 | 常见原因 | 处理方式 |
|------|----------|----------|
| 菜单为空 | 应用、菜单、角色授权或用户角色缺失 | 查 `/authorization/menus/user`。 |
| 页面空白 | 页面未注册或 component key 不一致 | 对照默认页面 key 和注册入口。 |
| 用户列表为空 | identity 没有用户或当前账号无权限 | 查 `/identity/users/page` 和接口权限。 |
| 组织树为空 | org 数据未初始化或租户过滤无数据 | 查 `/org/tree`。 |
| 授权后不生效 | 用户仍使用旧 token 或旧菜单缓存 | 重新登录并刷新菜单。 |
| 按钮隐藏但接口还能调 | 只做了前端隐藏，没有后端权限 | 检查后端 authorization 资源和接口鉴权。 |

## 10. 相关文档

- [后端 Authorization](../../../mango/mango-platform/mango-authorization/README.md)
- [后端 Identity](../../../mango/mango-platform/mango-identity/README.md)
- [后端 Org](../../../mango/mango-platform/mango-org/README.md)
- [RBAC 页面说明](./src/views/README.md)
- [@mango/admin-pages](../admin-pages/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
