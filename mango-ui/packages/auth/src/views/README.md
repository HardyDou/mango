# Auth Views

## 1. 概览
本入口说明 `@mango/auth` 的登录、个人资料和修改密码页面。它们是认证前端页面入口，不负责后端认证、验证码、租户或用户数据实现。

## 2. 功能清单
来自 `@mango/auth`：

- `LoginView`
- `ProfileView`
- `PasswordView`
- `useAuthConfig`
- `useUserInfoStore`
- `login`、`logout`、`getInfo`、`updatePassword` 等认证 API 封装。

`@mango/auth` 当前没有独立 `admin-pages` 注册入口；通常由 Shell、路由或业务应用直接引用页面组件。

## 3. 页面入口
- `/login`：管理端登录，支持租户选项、验证码、账号密码和企业微信登录入口。
- `/profile`：用户中心个人资料展示。
- `/password`：登录后修改密码。
- Shell 或业务应用读取登录品牌、租户选项、验证码和系统配置。

接入示例：

```ts
import { LoginView, ProfileView, PasswordView } from '@mango/auth';
import '@mango/auth/style.css';
```

路由示例：

```ts
[
  { path: '/login', component: LoginView },
  { path: '/profile', component: ProfileView },
  { path: '/password', component: PasswordView },
]
```

页面组件不对外定义 props 或事件；登录表单、租户选择、验证码、用户信息和密码修改通过 `src/api/sys.ts` 与后端交互。

可配置项来自 `useAuthConfig()` 和后端系统配置。

## 4. 后端依赖
- 后端模块：`mango-platform/mango-auth`、`mango-platform/mango-identity`、`mango-platform/mango-captcha`、`mango-platform/mango-system`。
- API 前缀：`/auth/login`、`/auth/wecom/login`、`/auth/login-institutions`、`/auth/info`、`/auth/logout`、`/user/password`、`/system/tenant/login-options`、`/system/config/type`。
- 验证码由 `@mango/common/api/captcha` 和后端 captcha/kv 能力提供。

## 5. 管理入口
- 登录页在未登录态访问，租户选项来自后端。
- 登录成功后的 token、用户信息、权限列表和租户上下文由后端认证链路返回。
- 修改密码接口由后端校验当前用户和旧密码。
- 前端页面不持久化权限真相，只消费后端返回的登录态和系统配置。

## 6. 问题排查
- 登录页租户为空时，检查 `/system/tenant/login-options`。
- 验证码不显示时，检查 captcha 后端和 kv store。
- 登录成功但菜单为空时，继续检查 authorization/access/admin-shell 闭环。

## 7. 相关文档
- [@mango/auth README](../../README.md)
- [Auth 后端 README](../../../../../mango/mango-platform/mango-auth/README.md)
- [Identity 后端 README](../../../../../mango/mango-platform/mango-identity/README.md)
- [Captcha 后端 README](../../../../../mango/mango-platform/mango-captcha/README.md)
- [能力地图](../../../../../mango-docs/capabilities/README.md)
- [能力说明维护规范](../../../../../mango-pmo/rules/08-capability-docs.md)
