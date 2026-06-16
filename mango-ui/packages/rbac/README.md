# @mango/rbac

## 1. 概览
`@mango/rbac` 提供 Mango 管理后台的授权、用户、角色、菜单、菜单包、应用、组织、岗位和权限管理页面，以及对应 API 封装。

本包属于 `admin-pages` 配套能力，依赖后端 `mango-authorization`、`mango-identity` 和 `mango-org`。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 管理 internal-admin 等应用的菜单、菜单包和页面资源 | 前端注册 / 组件 / API 封装 |
| 给角色分配菜单和权限 | 前端注册 / 组件 / API 封装 |
| 管理用户、组织、岗位和用户组织关系 | 前端注册 / 组件 / API 封装 |
| 管理前端应用、模块绑定和运行策略 | 前端注册 / 组件 / API 封装 |
| 业务页面需要查询角色、菜单、用户、组织或岗位数据 | 前端注册 / 组件 / API 封装 |

## 3. 适用场景
- 管理 `internal-admin` 等应用的菜单、菜单包和页面资源。
- 给角色分配菜单和权限。
- 管理用户、组织、岗位和用户组织关系。
- 管理前端应用、模块绑定和运行策略。
- 业务页面需要查询角色、菜单、用户、组织或岗位数据。

## 4. 边界说明
- 不负责后端鉴权判定。
- 不初始化菜单和权限资源。
- 不替代业务模块自己的 resource manifest。
- 不负责登录、token 签发和租户上下文创建。

## 5. 模块组成
本包包含管理页面和 API：

| 能力 | 页面 | API |
|------|------|-----|
| 应用 | `AppView` | `appApi`、`appModuleApi` |
| 菜单 | `MenuView` | `menuApi` |
| 菜单包 | `MenuPackageView` | `menuPackageApi` |
| 角色 | `RoleView` | `roleApi` |
| 用户 | `UserView` | `userApi` |
| 组织 | `OrgView` | `orgApi` |
| 岗位 | `PostView` | `postApi` |
| 权限 | `PermissionView` | 权限页面内接口 |

默认页面注册由 `@mango/admin-pages` 的 `registerDefaultAdminPages` 完成，本包自身没有单独 `admin-pages` export。

## 6. 接入方式
安装：

```bash
pnpm add @mango/rbac
```

引入样式和页面：

```ts
import '@mango/rbac/style.css';
import { MenuView, RoleView, UserView, appApi, menuApi, roleApi } from '@mango/rbac';
```

默认页面注册：

```ts
import { registerDefaultAdminPages } from '@mango/admin-pages';

registerDefaultAdminPages();
```

## 7. 配置说明
本包没有运行时配置文件。主要行为由 API 参数、后端资源和 `@mango/admin-pages` 默认页面注册控制。

| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 | 源码入口 |
|----------|------------|--------|------|----------|----------|
| `AuthorizationApp` | `appCode` | 无 | 应用编码 | 菜单和授权边界 | `api/app.ts` |
| `AuthorizationApp` | `appType`、`deployMode`、`entryUrl` | 可选 | 前端运行方式 | 影响 runtime 策略 | `api/app.ts` |
| `AppModuleBinding` | `appCode`、`moduleCode` | 无 | 应用模块绑定 | 决定模块菜单同步 | `appModuleApi` |
| `SysMenuVO` | `menuType` | 无 | 目录、菜单、按钮 | Shell 路由过滤和按钮权限 | `api/menu.ts` |
| `SysMenuVO` | `moduleCode`、`component` | 可选 | 页面归属和页面 key | 菜单打开页面 | `api/menu.ts` |
| `RoleVO` | `appCode`、`realm`、`actorType` | 无 | 角色作用域 | 决定授权对象范围 | `api/role.ts` |
| `IdentityUserVO` | `realm`、`actorType`、`partyType`、`partyId` | 可选 | 登录身份上下文 | 影响用户绑定 | `api/user.ts` |
| `OrgTreeParams` | `parentId`、`type`、`includeDisabled` | 可选 | 组织树过滤 | 影响组织选择 | `api/org.ts` |

## 8. API 与扩展
| 导出 | 用途 |
|------|------|
| `AppView` | 应用管理 |
| `MenuView` | 菜单管理 |
| `MenuPackageView` | 菜单包管理 |
| `RoleView` | 角色管理和菜单授权 |
| `UserView` | 用户管理 |
| `OrgView` | 组织管理 |
| `PostView` | 岗位管理 |
| `PermissionView` | 权限资源查看 |
| `appApi`、`appModuleApi` | 应用和模块绑定 |
| `menuApi` | 菜单 CRUD 和用户菜单 |
| `menuPackageApi` | 菜单包 |
| `roleApi` | 角色、角色菜单、主体角色绑定 |
| `userApi` | 用户、企微同步、外部身份绑定 |
| `orgApi` | 组织树和组织成员 |
| `postApi` | 岗位 |

## 9. 数据与初始化
本包不包含数据库 migration。依赖后端初始化：

| 类型 | 后端来源 | 前端消费 | 排查入口 |
|------|----------|----------|----------|
| 应用 | authorization | 应用管理、菜单接口 | 应用列表有 `internal-admin` |
| 菜单和按钮 | authorization / resource manifest | 菜单管理、Shell 菜单 | 菜单树可加载 |
| 角色和授权 | authorization | 角色管理 | 分配菜单后用户可见 |
| 用户身份 | identity | 用户管理和登录 | 用户列表和登录可用 |
| 组织岗位 | org | 组织、岗位、用户组织关系 | 组织树和岗位列表可用 |

## 10. 管理入口
默认页面 key：

| 菜单 / 页面 | component key | 权限码 | 入库来源 | 默认套餐 / 角色 | 后端校验入口 |
|-------------|---------------|--------|----------|-----------------|--------------|
| 菜单包 | `system/menu-package/index` | authorization 定义 | 后端初始化 | 角色授权 | authorization |
| 菜单管理 | `system/menu/index` | authorization 定义 | 后端初始化 | 角色授权 | authorization |
| 角色管理 | `system/role/index` | authorization 定义 | 后端初始化 | 角色授权 | authorization |
| 用户管理 | `system/user/index` | identity / authorization 定义 | 后端初始化 | 角色授权 | identity |
| 组织管理 | `system/org/index` | org / authorization 定义 | 后端初始化 | 角色授权 | org |
| 岗位管理 | `system/post/index` | org / authorization 定义 | 后端初始化 | 角色授权 | org |
| 应用管理 | `system/app/index` | authorization 定义 | 后端初始化 | 角色授权 | authorization |
| 权限资源 | `system/permission/index` | authorization 定义 | 后端初始化 | 角色授权 | authorization |

前端只展示授权结果；接口权限、租户和组织数据范围由后端校验。

## 11. 快速开始
1. 后端启用 authorization、identity、org。
2. 初始化 `internal-admin` 应用、菜单和角色。
3. 前端引入 `@mango/rbac/style.css`。
4. 调用 `registerDefaultAdminPages`。
5. 给测试用户分配角色和菜单。
6. 登录后台验证菜单、页面、按钮、接口权限和租户隔离。

## 12. 问题排查
| 问题 | 原因 | 处理方式 |
|------|------|----------|
| 菜单为空 | 应用、菜单或角色授权缺失 | 查 authorization 菜单接口 |
| 页面空白 | 默认页面未注册或 component key 不一致 | 查 `registerDefaultAdminPages` |
| 用户列表为空 | identity 没有用户或无权限 | 查 identity 数据和接口权限 |
| 组织树为空 | org 未初始化 | 查组织数据 |
| 授权后不生效 | 用户 session 仍是旧权限 | 重新登录或刷新用户权限 |

## 13. 相关文档
- [前端代码规范](../../../mango-pmo/rules/frontend/01-vue-code.md)
- [前端测试规范](../../../mango-pmo/rules/frontend/04-test.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [后端 Authorization](../../../mango/mango-platform/mango-authorization/README.md)
- [后端 Identity](../../../mango/mango-platform/mango-identity/README.md)
- [后端 Org](../../../mango/mango-platform/mango-org/README.md)

## 14. 历史资料
- [RBAC 页面说明](./src/views/README.md)
- [Mango UI README](../../README.md)
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
