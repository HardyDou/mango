# @mango/auth

## 1. 概览

`@mango/auth` 是 Mango 管理端认证前端包，提供登录页、个人中心、修改密码页、用户信息 store、认证 API 封装和登录页运行配置。

集成形态：

| 标识 | 说明 |
|------|------|
| `admin-shell` | 登录页、个人中心、修改密码隐藏页和用户信息上下文，随 Mango Admin 壳使用。 |
| `api-client` | 登录、退出、当前用户、租户选项、企微登录、验证码和修改密码接口封装。 |

它不是官网或 C 端站点的通用登录组件。非管理端站点要复用时，需要单独确认路由、样式、token 保存、租户、验证码和权限菜单依赖。

## 2. 功能清单

| 能力 | 使用入口 | 后端依赖 |
|------|----------|----------|
| 账号密码登录 | `LoginView`、`login()` | `mango-auth` |
| 企微登录 | `wecomLogin()`、`getWecomLoginConfig()` | `mango-auth` 企微渠道配置 |
| 登录租户选择 | `getLoginTenantOptions()`、`getAccountLoginTenantOptions()` | `mango-system`、`mango-auth` |
| 当前用户信息 | `getUserInfo()`、`useUserInfoStore` | `mango-auth`、`mango-identity` |
| 退出登录 | `logout()` | `mango-auth` |
| 修改密码 | `PasswordView`、`updatePassword()` | `mango-identity` |
| 个人中心 | `ProfileView` | `mango-identity` |
| 登录页品牌和默认参数 | `installMangoAuth()` | 无新增后端依赖 |

## 3. 接入方式

开发依赖：

```bash
pnpm add @mango/auth
```

宿主应用还需要提供 `vue`、`vue-router`、`pinia`、`element-plus`，并引入 `@mango/common` 的请求上下文。部署时后端必须启用 auth、identity、authorization、system tenant 和 captcha 相关能力，否则登录后拿不到用户、租户或菜单。

引入页面和样式：

```ts
import '@mango/auth/style.css';
import { LoginView, PasswordView, ProfileView } from '@mango/auth';
```

安装登录页配置：

```ts
import { installMangoAuth } from '@mango/auth';

installMangoAuth(app, {
  login: {
    brand: {
      title: 'Mango Admin',
      subtitle: '管理后台',
      panelTitle: '账号登录',
    },
    defaults: {
      appCode: 'internal-admin',
      tenantCode: 'default',
      redirectPath: '/',
    },
  },
  password: {
    minLength: 8,
  },
});
```

直接调用 API：

```ts
import { getUserInfo, login, logout } from '@mango/auth';

await login({
  username,
  password,
  appCode: 'internal-admin',
  tenantCode: 'default',
  captchaCode,
  captchaKey,
});

const user = await getUserInfo();
await logout();
```

## 4. 配置说明

`installMangoAuth(app, config)` 把配置注入 Vue，同时保存为全局配置。配置只影响前端页面展示和默认登录参数，不会创建后端应用、租户、角色或菜单。

| 配置位置 | 字段 | 含义 |
|----------|------|------|
| `login.brand` | `title` | 登录页品牌标题。 |
| `login.brand` | `subtitle` | 登录页副标题。 |
| `login.brand` | `panelTitle` | 登录表单面板标题。 |
| `login.defaults` | `tenantCode` | 登录参数默认租户编码。 |
| `login.defaults` | `realm` | 登录域。 |
| `login.defaults` | `actorType` | 登录主体类型。 |
| `login.defaults` | `partyType` | 登录参与方类型。 |
| `login.defaults` | `appCode` | 登录应用编码，管理端通常是 `internal-admin`。 |
| `login.defaults` | `redirectPath` | 登录成功后的默认跳转路径。 |
| `login.defaults` | `redirectQueryKey` | 登录页读取的回跳 query 参数名，默认 `redirect`；值必须是站内路径。 |
| `profile` | `avatarUrl` | 个人中心默认头像地址。 |
| `profile` | `roleLabel` | 个人中心角色标签展示值。 |
| `profile` | `fields` | 个人资料展示字段列表。 |
| `password` | `minLength` | 修改密码页新密码最小长度前端校验。 |
| `slots` | `brand`、`formHeader` 等 | 登录页、个人中心、修改密码页插槽组件。 |

请求 base URL、token、refresh token、401 处理和租户头由 `@mango/common` request 负责配置。

## 5. API 与扩展

组件导出：

| 导出 | 用途 |
|------|------|
| `LoginView` | 管理后台登录页。 |
| `ProfileView` | 个人中心页。 |
| `PasswordView` | 修改密码页。 |

配置和状态导出：

| 导出 | 用途 |
|------|------|
| `installMangoAuth` | 安装认证页面配置。 |
| `getMangoAuthConfig` | 读取全局认证配置。 |
| `mergeAuthConfig` | 合并认证配置。 |
| `useUserInfoStore` | 用户信息 store。 |

API 封装：

| 函数 | HTTP 接口 | 说明 |
|------|-----------|------|
| `login(data)` | `POST /auth/login` | 账号密码登录。 |
| `wecomLogin(data)` | `POST /auth/wecom/login` | 企微登录。 |
| `getWecomLoginConfig(tenantId)` | `GET /auth/wecom/login-config` | 读取企微登录配置。 |
| `getAccountLoginTenantOptions(data)` | `POST /auth/login-institutions` | 按账号密码查询可登录租户。 |
| `getLoginTenantOptions()` | `GET /system/tenant/login-options` | 读取登录租户选项。 |
| `getUserInfo()` | `GET /auth/info` | 获取当前登录用户。 |
| `logout()` | `POST /auth/logout` | 退出登录。 |
| `getCaptcha()` | `GET /captcha/arithmetic` | 获取算术验证码。 |
| `updatePassword(data)` | `POST /user/password` | 修改当前用户密码。 |
| `getSystemConfig()` | `GET /system/config/type` | 读取系统配置。 |

常用返回字段：

| 数据 | 字段 |
|------|------|
| 登录租户 | `tenantId`、`tenantCode`、`tenantName` |
| 企微配置 | `channelConfigId`、`corpId`、`agentId`、`redirectUri` |
| 验证码 | `key`、`type`、`image`、`expireTime` |

## 6. 数据与初始化

`@mango/auth` 不带 migration，也不会初始化用户、租户、应用或菜单。接入前要确认这些后端数据已经存在：

| 数据 | 来源 | 前端表现 |
|------|------|----------|
| 用户账号和密码 | `mango-identity` | `POST /auth/login` 可登录。 |
| 租户和登录选项 | `mango-system` | 登录页租户下拉有数据。 |
| 应用编码 | `mango-authorization` | `appCode` 能匹配应用授权边界。 |
| 角色和菜单 | `mango-authorization` | 登录后 Shell 能加载菜单。 |
| 验证码 | `mango-captcha` | 登录页验证码能生成和校验。 |
| 企微渠道配置 | `mango-auth` | 企微登录能拿到 corpId、agentId 和 redirectUri。 |

## 7. 管理入口

认证页通常由 Shell 路由挂载：

| 页面 | 路由或 key | 访问条件 |
|------|------------|----------|
| 登录页 | `/login` | 未登录即可访问。 |
| 个人中心 | `profile/index` | 已登录用户。 |
| 修改密码 | `password/index` | 已登录用户。 |

登录成功只代表拿到了 token 和用户上下文，不代表拥有管理后台菜单。菜单、按钮和接口权限继续由 authorization 后端返回和校验。

## 8. 快速开始

1. 后端启用 auth、identity、authorization、system tenant 和 captcha。
2. 前端安装 `@mango/auth`，引入 `@mango/auth/style.css`。
3. 在 Shell 启动时调用 `installMangoAuth()`，设置 `appCode`、租户默认值和跳转路径。
4. 路由挂载 `LoginView`、`ProfileView`、`PasswordView`。
5. 登录成功后用 `getUserInfo()` 刷新用户信息，再加载菜单。

## 9. 问题排查

| 问题 | 常见原因 | 处理方式 |
|------|----------|----------|
| 登录 401 | 密码、验证码、租户、realm 或 appCode 不匹配 | 对照登录入参和 auth 后端日志。 |
| 登录页租户为空 | `/system/tenant/login-options` 无数据或接口未放行 | 检查租户初始化和公共路径配置。 |
| 登录成功但菜单为空 | 应用、角色、菜单或授权缺失 | 检查 authorization 用户菜单接口。 |
| 企微登录失败 | 企微渠道配置、redirectUri 或 tenantId 不匹配 | 检查 `GET /auth/wecom/login-config` 返回值。 |
| 修改密码失败 | 旧密码错误或后端密码策略不通过 | 看 `POST /user/password` 返回信息。 |
| 个人中心空白 | 路由未挂载或样式未引入 | 检查 Shell 路由和 `@mango/auth/style.css`。 |

## 10. 相关文档

- [后端 Auth](../../../mango/mango-platform/mango-auth/README.md)
- [后端 Identity](../../../mango/mango-platform/mango-identity/README.md)
- [后端 Authorization](../../../mango/mango-platform/mango-authorization/README.md)
- [@mango/common](../common/README.md)
- [认证页面说明](./src/views/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 11. 变更影响记录

- 本次新增登录首次强制改密、密码复杂度提示和弱密码提交拦截；`LoginView` 和 `PasswordView` 都会展示密码规则，并按统一密码策略校验。登录成功后若后端返回 `passwordResetRequired=true` 或 `loginAction=CHANGE_PASSWORD`，前端会切换到改密弹窗而不是直接进入后台。该变更不改变 `login()`、`logout()`、`getUserInfo()`、`getLoginTenantOptions()`、`wecomLogin()` 和 `updatePassword()` 的接口路径。
