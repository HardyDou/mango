# RBAC Views

## 1. 概览
本入口说明 `@mango/rbac` 的权限、菜单、角色、用户、组织、岗位、应用和菜单包页面。它们是 RBAC 管理页面入口，不是独立复用组件库。

## 2. 功能清单
来自 `@mango/rbac`：

- `MenuView`
- `MenuPackageView`
- `RoleView`
- `UserView`
- `OrgView`
- `PermissionView`
- `AppView`
- `PostView`
- `appApi`、`menuApi`、`menuPackageApi`、`orgApi`、`postApi`、`roleApi`、`userApi` 相关导出。

`@mango/rbac` 当前没有独立 `admin-pages` 注册入口；页面通常由 admin-shell 或业务应用按菜单 component 映射引用。

- 维护应用、菜单、页面 component key 和权限资源。
- 维护角色授权、用户、组织和岗位。
- 排查登录后菜单、按钮权限和组织数据。
- 业务项目接入 Mango 后配置后台 RBAC 基础数据。

## 3. 页面入口
常用页面 key 映射：

| 页面 key | 组件 |
|----------|------|
| `rbac/menu/index` | `MenuView` |
| `rbac/menu-package/index` | `MenuPackageView` |
| `rbac/role/index` | `RoleView` |
| `rbac/user/index` | `UserView` |
| `rbac/org/index` | `OrgView` |
| `rbac/permission/index` | `PermissionView` |
| `rbac/app/index` | `AppView` |
| `rbac/post/index` | `PostView` |

接入示例：

```ts
import { MenuView, RoleView, UserView } from '@mango/rbac';
import '@mango/rbac/style.css';
```

业务应用按路由或菜单 component 映射页面组件。页面 key 需要和后端 authorization 菜单资源保持一致。

页面组件不对外定义 props 或事件；数据来源是各 `src/api/*.ts` API 封装。

页面之间通过后端资源关系协作：

- 应用和菜单包影响菜单归属。
- 菜单和权限影响按钮显示和访问控制。
- 用户、组织、岗位和角色影响授权结果。

## 4. 后端依赖
- 后端模块：`mango-platform/mango-authorization`、`mango-platform/mango-identity`、`mango-platform/mango-org`、`mango-platform/mango-access`。
- API 前缀覆盖 app、menu、menu-package、org、post、role、user 相关接口，具体路径以 `src/api/*.ts` 为准。
- 登录菜单闭环还依赖 `@mango/auth` 和 admin-shell。

## 5. 管理入口
- 页面中的菜单、角色、用户、组织和岗位数据由后端按租户、数据权限和接口权限校验。
- 前端按钮权限来自菜单资源和用户权限集合。
- 菜单 component key 需要能映射到真实页面组件，否则登录后会出现空页面或找不到组件。

## 6. 问题排查
- 菜单能看到但页面打不开时，检查 component key 与前端注册映射。
- 按钮不显示时，检查后端资源同步、角色授权和用户权限集合。
- 用户或组织为空时，检查 identity/org 后端数据和租户上下文。

菜单打不开排障顺序：

1. `/authorization/menus/user?fmt=tree&appCode=internal-admin` 返回目标菜单。
2. 菜单的 `component` 字段命中上方页面 key 映射。
3. 当前用户角色已绑定菜单和权限资源。
4. 当前租户已绑定目标应用。
5. 前端已引入 `@mango/rbac/style.css`，并在路由或页面注册表中绑定对应组件。
6. 浏览器 console 和 network 没有未解释的模块加载、接口 401/403/404 错误。

## 7. 相关文档
- [@mango/rbac README](../../README.md)
- [Authorization 后端 README](../../../../../mango/mango-platform/mango-authorization/README.md)
- [Identity 后端 README](../../../../../mango/mango-platform/mango-identity/README.md)
- [Access 后端 README](../../../../../mango/mango-platform/mango-access/README.md)
- [能力地图](../../../../../mango-docs/capabilities/README.md)
- [能力说明维护规范](../../../../../mango-pmo/rules/08-capability-docs.md)
