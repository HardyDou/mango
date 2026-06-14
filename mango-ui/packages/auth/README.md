# 前端认证包

## 1. 能力定位

提供登录、密码、个人信息页面和认证配置。

主要使用者：前端开发者、业务开发者和 AI Agent。

## 2. 适用场景

业务管理端需要复用 Mango 登录和用户状态时使用。

## 3. 不适用场景

不负责后端 token 签发和权限资源。

## 4. 模块边界

包名：`@mango/auth`。本包只提供前端运行时、页面、组件、API 封装或页面注册能力，不改变后端接口契约。

## 5. 接入方式

安装并引入：

```ts
import '@mango/auth/style.css';
import { LoginView, PasswordView, ProfileView, login, logout, getUserInfo } from '@mango/auth';
```

后端需要接入 [Auth](../../../mango/mango-platform/mango-auth/README.md)、[Identity](../../../mango/mango-platform/mango-identity/README.md) 和 [Authorization](../../../mango/mango-platform/mango-authorization/README.md) 能力。

## 6. 配置项

配置来自业务应用 Vite、Shell runtimeConfig、后端 API baseURL 和包导出的注册入口；本 README 不复制长期前端规则。

## 7. 对外接口 / 扩展点

公开入口：

- `LoginView`、`ProfileView`、`PasswordView`。
- `login`、`logout`、`getUserInfo`、企微登录相关 API。
- `useUserInfoStore` 和认证配置。

主要 API 前缀：`/auth`。

## 8. 数据库 / 初始化数据

无前端数据库。菜单、权限和初始化数据由对应后端模块或 business starter 维护。

## 9. 菜单 / 权限 / 租户

前端只负责页面注册、菜单 component 映射和交互展示；权限、租户和数据归属由后端接口校验。

## 10. 验证方式

```bash
pnpm -F @mango/auth build
```

## 11. 业务接入最小闭环

业务应用引入样式和认证页面，配置后端代理到 Auth 服务，登录后调用 `/auth/info` 回显用户信息，并确认退出调用 `/auth/logout`。

## 12. 常见问题

登录 401 优先检查 Auth 服务、验证码/KV 和账号状态；403 优先检查角色和菜单绑定；页面空白优先检查样式入口、路由挂载和后端 `/auth/info` 返回。

## 13. 关联 PMO 规则

- [前端模块规范](../../../mango-pmo/rules/frontend/01-vue-code.md)
- [前端测试规范](../../../mango-pmo/rules/frontend/04-test.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [能力地图](../../../mango-docs/capabilities/README.md)
