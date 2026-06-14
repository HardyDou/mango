# RBAC 前端 API 包

## 1. 能力定位

提供用户、角色、菜单、组织、岗位等 RBAC API 封装。

主要使用者：前端开发者、业务开发者和 AI Agent。

## 2. 适用场景

业务前端需要调用授权和组织管理接口时使用。

## 3. 不适用场景

不负责页面壳层和后端授权判定。

## 4. 模块边界

包名：`@mango/rbac`。本包只提供前端运行时、页面、组件、API 封装或页面注册能力，不改变后端接口契约。

## 5. 接入方式

安装并引入：

```ts
import '@mango/rbac/style.css';
import { AppView, MenuView, OrgView, PermissionView, RoleView, UserView } from '@mango/rbac';
import { menuApi, roleApi, userApi } from '@mango/rbac';
```

后端需要接入 [Authorization](../../../mango/mango-platform/mango-authorization/README.md)、[Identity](../../../mango/mango-platform/mango-identity/README.md) 和 [Org](../../../mango/mango-platform/mango-org/README.md) 能力。

## 6. 配置项

配置来自业务应用 Vite、Shell runtimeConfig、后端 API baseURL 和包导出的注册入口；本 README 不复制长期前端规则。

## 7. 对外接口 / 扩展点

公开入口：

- 页面：`AppView`、`MenuView`、`MenuPackageView`、`RoleView`、`UserView`、`OrgView`、`PermissionView`、`PostView`。
- API：`appApi`、`menuApi`、`roleApi`、`userApi`、`orgApi`、`postApi`。

主要 API 前缀：`/authorization`、`/identity`、`/org`。

## 8. 数据库 / 初始化数据

无前端数据库。菜单、权限和初始化数据由对应后端模块或 business starter 维护。

## 9. 菜单 / 权限 / 租户

前端只负责页面注册、菜单 component 映射和交互展示；权限、租户和数据归属由后端接口校验。

## 10. 验证方式

```bash
pnpm -F @mango/rbac build
```

## 11. 业务接入最小闭环

业务页面通过 rbac API 查询菜单、角色、用户和组织，确认菜单树能返回当前应用资源，角色授权后受保护页面可见。

## 12. 常见问题

401 优先检查登录态；403 优先检查角色、菜单和 API 权限码；菜单空白优先检查 authorization 资源同步和应用绑定；组织树为空优先检查 Org 数据。

## 13. 关联 PMO 规则

- [前端模块规范](../../../mango-pmo/rules/frontend/01-vue-code.md)
- [前端测试规范](../../../mango-pmo/rules/frontend/04-test.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [能力地图](../../../mango-docs/capabilities/README.md)
