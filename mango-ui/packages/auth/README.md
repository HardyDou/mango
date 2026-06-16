# @mango/auth

## 1. 概览
`@mango/auth` 是 Mango 管理端认证前端包，提供登录页、个人中心、修改密码页、用户信息 store、认证 API 和登录页配置。

本包属于 `admin-shell` 配套能力，依赖 Mango 后端 auth、identity、authorization 能力。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 管理后台需要复用 Mango 登录页 | 前端注册 / 组件 / API 封装 |
| Shell 需要加载个人中心和修改密码隐藏页面 | 前端注册 / 组件 / API 封装 |
| 业务后台需要账号密码登录、企微登录、租户选择、用户信息回显和退出 | 前端注册 / 组件 / API 封装 |
| 登录页需要配置品牌文案、默认 realm、appCode、tenantCode 或自定义插槽 | 前端注册 / 组件 / API 封装 |

## 3. 适用场景
- 管理后台需要复用 Mango 登录页。
- Shell 需要加载个人中心和修改密码隐藏页面。
- 业务后台需要账号密码登录、企微登录、租户选择、用户信息回显和退出。
- 登录页需要配置品牌文案、默认 realm、appCode、tenantCode 或自定义插槽。

## 4. 边界说明
- 不负责后端 token 签发、刷新和权限校验。
- 不负责验证码实现；验证码由 `@mango/common` 和后端 captcha 能力提供。
- 不负责菜单、角色和租户授权入库。
- 不适合作为非管理端网站的通用登录系统直接套用。

## 5. 模块组成
本包负责认证相关前端页面和 API 封装：

- `LoginView`：登录页。
- `ProfileView`：个人中心。
- `PasswordView`：修改密码。
- `login`、`logout`、`getUserInfo` 等 API。
- `useUserInfoStore`：用户信息 store。
- `installMangoAuth`、`MangoAuthConfig`：认证页面配置。

请求、token 注入、refresh token、401 处理由 `@mango/common` request 负责。

## 6. 接入方式
安装：

```bash
pnpm add @mango/auth
```

引入页面和样式：

```ts
import '@mango/auth/style.css';
import { LoginView, PasswordView, ProfileView } from '@mango/auth';
```

配置登录页：

```ts
import { installMangoAuth } from '@mango/auth';

app.use({
  install(vueApp) {
    installMangoAuth(vueApp, {
      login: {
        brand: {
          title: 'Mango Admin',
          subtitle: '管理后台',
        },
        defaults: {
          appCode: 'internal-admin',
          redirectPath: '/',
        },
      },
      password: {
        minLength: 8,
      },
    });
  },
});
```

API 调用：

```ts
import { login, getUserInfo, logout } from '@mango/auth';

await login({ username, password, appCode: 'internal-admin' });
const user = await getUserInfo();
await logout();
```

## 7. 配置说明
### 6.1 MangoAuthConfig

| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 | 源码入口 |
|----------|------------|--------|------|----------|----------|
| `login.brand` | `title` | 空 | 登录页品牌标题 | 登录页展示 | `config.ts` |
| `login.brand` | `subtitle` | 空 | 副标题 | 登录页展示 | `config.ts` |
| `login.brand` | `panelTitle` | 空 | 表单面板标题 | 登录页展示 | `config.ts` |
| `login.defaults` | `tenantCode` | 空 | 默认租户编码 | 登录参数默认值 | `config.ts` |
| `login.defaults` | `realm` | 空 | 登录域 | 登录参数默认值 | `config.ts` |
| `login.defaults` | `actorType` | 空 | 主体类型 | 登录参数默认值 | `config.ts` |
| `login.defaults` | `partyType` | 空 | 参与方类型 | 登录参数默认值 | `config.ts` |
| `login.defaults` | `appCode` | 空 | 登录应用 | 影响后端授权边界 | `config.ts` |
| `login.defaults` | `redirectPath` | 空 | 登录后跳转 | 登录成功路由 | `config.ts` |
| `profile.avatarUrl` | 空 | 默认头像 | 个人中心展示 | `config.ts` |
| `profile.roleLabel` | 空 | 角色标签 | 个人中心展示 | `config.ts` |
| `profile.fields` | username、nickname 等 | 个人资料字段 | 控制展示字段 | `config.ts` |
| `password.minLength` | 空 | 新密码最小长度 | 前端表单校验 | `config.ts` |
| `slots` | 多个 Component | 空 | 登录、资料、密码页扩展位 | 页面定制 | `config.ts` |

### 6.2 Login 参数

| 字段 | 含义 |
|------|------|
| `username` | 登录账号 |
| `password` | 登录密码 |
| `tenantId`、`tenantCode` | 租户 |
| `realm` | 认证域 |
| `actorType`、`partyType`、`partyId` | 主体上下文 |
| `appCode` | 应用编码，管理端通常为 `internal-admin` |
| `captchaCode`、`captchaKey` | 验证码 |

## 8. API 与扩展
| 导出 | 用途 |
|------|------|
| `LoginView` | 登录页 |
| `ProfileView` | 个人中心 |
| `PasswordView` | 修改密码页 |
| `installMangoAuth` | 安装认证配置 |
| `getMangoAuthConfig` | 读取全局认证配置 |
| `mergeAuthConfig` | 合并认证配置 |
| `useUserInfoStore` | 用户信息 store |
| `login` | 账号密码登录 |
| `wecomLogin` | 企微登录 |
| `getWecomLoginConfig` | 获取企微登录配置 |
| `getAccountLoginTenantOptions` | 按账号密码获取登录租户 |
| `getLoginTenantOptions` | 获取登录租户列表 |
| `getUserInfo` | 获取当前用户 |
| `logout` | 退出登录 |
| `getCaptcha` | 获取算术验证码 |
| `updatePassword` | 修改密码 |
| `getSystemConfig` | 获取系统配置 |

## 9. 数据与初始化
本包不包含数据库 migration。依赖后端初始化：

| 类型 | 后端模块 | 前端消费 | 排查入口 |
|------|----------|----------|----------|
| 用户账号 | identity | 登录和用户信息 | `/auth/login`、`/auth/info` 可用 |
| 租户 | identity / system | 登录租户选择 | 登录租户接口可用 |
| 应用授权 | authorization | appCode 权限边界 | 登录后菜单可加载 |
| 验证码 | captcha / common | 登录验证码 | 验证码接口可用 |

## 10. 管理入口
Shell 内置隐藏路由：

| 菜单 / 页面 | component key | 权限码 | 入库来源 | 默认套餐 / 角色 | 后端校验入口 |
|-------------|---------------|--------|----------|-----------------|--------------|
| 个人中心 | `profile/index` | 登录态 | Shell 隐藏路由 | 当前登录用户 | auth / identity |
| 修改密码 | `password/index` | 登录态 | Shell 隐藏路由 | 当前登录用户 | identity |
| 登录页 | 路由 `/login` | 无 | Shell 路由 | 未登录用户 | auth |

登录成功不代表拥有后台菜单。菜单和按钮权限仍由 authorization 返回，接口仍由后端校验。

## 11. 快速开始
1. 后端启用 auth、identity、authorization 和 captcha 能力。
2. 前端引入 `@mango/auth/style.css`。
3. Shell 或后台入口安装认证配置。
4. 设置 `appCode` 和默认租户参数。
5. 登录后保存 token 和用户信息。
6. 打开个人中心、修改密码，并验证菜单权限和租户上下文。

## 12. 问题排查
| 问题 | 原因 | 处理方式 |
|------|------|----------|
| 登录 401 | 账号密码、验证码、租户或认证域错误 | 查登录参数和后端 auth 日志 |
| 登录成功但无菜单 | appCode、角色授权或菜单初始化缺失 | 查 authorization 菜单接口 |
| 企微登录失败 | 未配置 channelConfigId、corpId、agentId 或 redirectUri | 查企微登录配置接口 |
| 修改密码失败 | 后端密码策略或旧密码不匹配 | 查 identity 接口返回 |
| 个人中心空白 | 未注册隐藏页面或样式未引入 | 查 Shell route 和 `@mango/auth/style.css` |

## 13. 相关文档
- [前端代码规范](../../../mango-pmo/rules/frontend/01-vue-code.md)
- [前端测试规范](../../../mango-pmo/rules/frontend/04-test.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [后端 Auth](../../../mango/mango-platform/mango-auth/README.md)
- [后端 Identity](../../../mango/mango-platform/mango-identity/README.md)
- [后端 Authorization](../../../mango/mango-platform/mango-authorization/README.md)

## 14. 历史资料
- [认证页面说明](./src/views/README.md)
- [Mango UI README](../../README.md)
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
