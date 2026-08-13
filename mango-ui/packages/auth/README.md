# @mango/auth

## 1. 概览

`@mango/auth` 是 Mango 管理端认证前端包，提供登录页、登录逻辑 hook、个人中心、修改密码页、用户信息 store、认证 API 封装和登录页运行配置。

集成形态：

| 标识          | 说明                                                                      |
| ------------- | ------------------------------------------------------------------------- |
| `admin-shell` | 登录页、个人中心、修改密码隐藏页和用户信息上下文，随 Mango Admin 壳使用。 |
| `api-client`  | 登录、退出、当前用户、租户选项、企微登录、验证码和修改密码接口封装。      |

它不是官网或 C 端站点的通用登录组件。非管理端站点要复用时，需要单独确认路由、样式、token 保存、租户、验证码和权限菜单依赖。

## 2. 功能清单

| 能力                 | 使用入口                                                    | 后端依赖                                      |
| -------------------- | ----------------------------------------------------------- | --------------------------------------------- |
| 账号密码登录         | `LoginView`、`useMangoLoginFlow()`、`login()`               | `mango-auth`                                  |
| 企业微信、钉钉登录   | `LoginView`、Provider API                                   | `mango-auth` Provider 配置和 OAuth 编排       |
| 登录租户选择         | `getLoginTenantOptions()`、`getAccountLoginTenantOptions()` | `mango-system`、`mango-auth`                  |
| 当前用户信息         | `getUserInfo()`、`useUserInfoStore`                         | `mango-auth`、`mango-identity`                |
| 退出登录             | `logout()`                                                  | `mango-auth`                                  |
| 修改密码             | `PasswordView`、`updatePassword()`                          | `mango-identity`                              |
| 个人中心             | `ProfileView`                                               | `mango-identity` 真实资料、联系方式和授权 API |
| 授权回调             | `ProviderCallbackView`                                      | 完成授权、绑定已有账号或直接登录              |
| 第三方登录配置       | `ProviderConfigView`                                        | `auth:provider-config:view/edit`              |
| 登录页品牌和默认参数 | `installMangoAuth()`                                        | 无新增后端依赖                                |

## 3. 接入方式

开发依赖：

```bash
pnpm add @mango/auth
```

宿主应用还需要提供 `vue`、`vue-router`、`pinia`、`element-plus`，并引入 `@mango/common` 的请求上下文。部署时后端必须启用 auth、identity、authorization、system tenant 和 captcha 相关能力，否则登录后拿不到用户、租户或菜单。

引入页面和样式：

```ts
import '@mango/auth/style.css';
import { LoginView, PasswordView, ProfileView, ProviderCallbackView, ProviderConfigView } from '@mango/auth';
```

只需要认证配置和个人中心 section 注册能力时，使用轻量子入口，避免加载认证页面：

```ts
import { registerMangoAuthProfileSections } from '@mango/auth/config';
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

| 配置位置         | 字段                     | 含义                                                                    |
| ---------------- | ------------------------ | ----------------------------------------------------------------------- |
| `login.brand`    | `title`                  | 登录页品牌标题。                                                        |
| `login.brand`    | `subtitle`               | 登录页副标题。                                                          |
| `login.brand`    | `panelTitle`             | 登录表单面板标题。                                                      |
| `login.defaults` | `tenantCode`             | 登录参数默认租户编码。                                                  |
| `login.defaults` | `realm`                  | 登录域。                                                                |
| `login.defaults` | `actorType`              | 登录主体类型。                                                          |
| `login.defaults` | `partyType`              | 登录参与方类型。                                                        |
| `login.defaults` | `appCode`                | 登录应用编码，管理端通常是 `internal-admin`。                           |
| `login.defaults` | `redirectPath`           | 登录成功后的默认跳转路径。                                              |
| `login.defaults` | `redirectQueryKey`       | 登录页读取的回跳 query 参数名，默认 `redirect`；值必须是站内路径。      |
| `profile`        | `avatarUrl`              | 个人中心未设置业务头像时的默认头像地址。                                |
| `profile`        | `roleLabel`              | 个人中心角色标签展示值。                                                |
| `profile`        | `fields`                 | 个人资料展示字段列表。                                                  |
| `profile`        | `sections`               | 个人中心扩展 section；能力包通常通过 Admin feature registrar 自动提供。 |
| `profile.slots`  | `theme`                  | 可选主题设置组件；配置后作为个人中心“主题设置”子页展示。                |
| `password`       | `minLength`              | 修改密码页新密码最小长度前端校验。                                      |
| `slots`          | `brand`、`formHeader` 等 | 登录页、个人中心、修改密码页插槽组件。                                  |

请求 base URL、token、refresh token、401 处理和租户头由 `@mango/common` request 负责配置。

`ProfileView` 在页面内容区提供“个人资料、账号安全、第三方授权、修改密码”内部导航；宿主配置 `profile.slots.theme` 后还会显示“主题设置”。能力包可以通过 Admin feature registrar 返回 `profileSections`，由 Shell 自动合并为个人中心页内导航；业务宿主不需要重复硬编码 Notice/System 的 section。这些入口不会增加 Mango 框架主菜单。个人资料头像通过图片选择器维护，支持 JPG、PNG、WebP，最大 5 MB；选择文件时只在本地预览，保存资料时上传到文件中心，业务资料只保存 `mango-file:{id}` 标识。顶部用户区和其它复用位置可通过 `@mango/common` 的 `MangoAvatar` 回显该标识，同时兼容历史普通图片地址。

## 5. API 与扩展

组件导出：

| 导出                   | 用途                                          |
| ---------------------- | --------------------------------------------- |
| `LoginView`            | 管理后台登录页。                              |
| `ProfileView`          | 个人中心页。                                  |
| `PasswordView`         | 修改密码页。                                  |
| `ProviderCallbackView` | 企业微信、钉钉授权回调和绑定已有账号页。      |
| `ProviderConfigView`   | 当前租户 Provider 配置页；Secret 为只写字段。 |

配置和状态导出：

| 导出                               | 用途                                                 |
| ---------------------------------- | ---------------------------------------------------- |
| `installMangoAuth`                 | 安装认证页面配置。                                   |
| `getMangoAuthConfig`               | 读取全局认证配置。                                   |
| `mergeAuthConfig`                  | 合并认证配置。                                       |
| `registerMangoAuthProfileSections` | 通过 `@mango/auth/config` 注册个人中心扩展 section。 |
| `useUserInfo`                      | 用户信息 store。                                     |
| `useMangoLoginFlow`                | 登录流程 hook，供默认登录页和业务自定义登录页复用。  |

自定义登录页可只消费登录逻辑，UI、布局、表单校验和按钮禁用由业务组件自己处理：

```ts
import { useMangoLoginFlow } from '@mango/auth';

const loginFlow = useMangoLoginFlow();

await loginFlow.loadLoginTenants();
loginFlow.setTenantId(selectedTenantId);
loginFlow.form.username = username;
loginFlow.form.password = password;

const result = await loginFlow.submitPasswordLogin();
if (result.status === 'password-reset-required') {
  // 业务组件自行打开强制改密弹窗，并调用 submitRequiredPasswordChange()
}
```

`useMangoLoginFlow()` 会处理登录机构加载、账号密码登录、强制改密、token/user/tenant 持久化和安全 redirect。它会暴露 `loading`、`tenantLoading`、`passwordResetLoading` 等状态，业务组件自行决定按钮禁用、loading 展示和错误区域。

自定义登录页发起企业微信或钉钉登录时，使用统一 Provider API，并直接跳转后端返回的授权地址：

```ts
import { providerCallbackUri, startProviderAuthorization } from '@mango/auth';

const authorization = await startProviderAuthorization({
  tenantId: selectedTenantId,
  appCode: 'internal-admin',
  provider: 'WECOM',
  intent: 'LOGIN',
  redirectUri: providerCallbackUri(),
});

window.location.assign(authorization.authorizationUrl);
```

`providerCallbackUri()` 返回当前前端入口的 `origin + pathname`。公开的 `ProviderCallbackView` 消费厂商返回的 `code/state` 并调用 `/auth/providers/complete`；业务页面不需要显示手工 code 输入框，也不需要自行生成 state 或拼接企业微信二维码地址。

已有自定义二维码弹窗可以继续调用 `loginFlow.prepareWecomLogin()`，并用只读的 `loginFlow.wecomQrUrl` 渲染二维码。刷新按钮重新调用 `prepareWecomLogin()` 获取新的后端 state；弹窗只保留二维码、刷新和取消，不再绑定 `wecomCode` 或 `submitWecomLogin()`。

API 封装：

| 函数                                                        | HTTP 接口                                 | 说明                               |
| ----------------------------------------------------------- | ----------------------------------------- | ---------------------------------- |
| `login(data)`                                               | `POST /auth/login`                        | 账号密码登录。                     |
| `getAccountLoginTenantOptions(data)`                        | `POST /auth/login-institutions`           | 按账号密码查询可登录租户。         |
| `getLoginTenantOptions()`                                   | `GET /system/tenant/login-options`        | 读取登录租户选项。                 |
| `getUserInfo()`                                             | `GET /auth/info`                          | 获取当前登录用户。                 |
| `logout()`                                                  | `POST /auth/logout`                       | 退出登录。                         |
| `getCaptcha()`                                              | `GET /captcha/arithmetic`                 | 获取算术验证码。                   |
| `updatePassword(data)`                                      | `POST /user/password`                     | 修改当前用户密码。                 |
| `getSystemConfig()`                                         | `GET /system/config/type`                 | 读取系统配置。                     |
| `getCurrentUserProfile()`                                   | `GET /identity/me/profile`                | 读取真实个人资料。                 |
| `updateCurrentUserProfile(data)`                            | `PUT /identity/me/profile`                | 保存基础和实名资料。               |
| `sendCurrentContactCaptcha(data)`                           | `POST /identity/me/contact-captcha`       | 发送新联系方式验证码。             |
| `updateCurrentUserContact(data)`                            | `PUT /identity/me/contact`                | 使用当前密码和验证码修改联系方式。 |
| `listCurrentExternalIdentities()`                           | `GET /identity/me/external-identities`    | 查询本人第三方绑定。               |
| `unbindCurrentExternalIdentity(data)`                       | `DELETE /identity/me/external-identities` | 使用当前密码解绑。                 |
| `listAvailableProviders(tenantId, appCode)`                 | `GET /auth/providers`                     | 查询可用企业微信、钉钉。           |
| `startProviderAuthorization(data)`                          | `POST /auth/providers/authorize`          | 发起登录或当前账号绑定。           |
| `completeProviderAuthorization(data)`                       | `POST /auth/providers/complete`           | 完成一次性回调。                   |
| `bindExistingProviderAccount(data)`                         | `POST /auth/providers/bind-existing`      | 匿名用户绑定已有账号并登录。       |
| `listProviderConfigs(appCode)` / `saveProviderConfig(data)` | `/auth/provider-configs`                  | 管理当前租户应用配置。             |

常用返回字段：

| 数据          | 字段                                                                    |
| ------------- | ----------------------------------------------------------------------- |
| 登录用户      | `tenantId`、`tenantCode`、`tenantName`、`departmentName`、`companyName` |
| Provider 授权 | `authorizationUrl`、`expiresInSeconds`                                  |
| 验证码        | `key`、`type`、`image`、`expireTime`                                    |

## 6. 数据与初始化

`@mango/auth` 不带 migration，也不会初始化用户、租户、应用或菜单。接入前要确认这些后端数据已经存在：

| 数据           | 来源                  | 前端表现                                            |
| -------------- | --------------------- | --------------------------------------------------- |
| 用户账号和密码 | `mango-identity`      | `POST /auth/login` 可登录。                         |
| 租户和登录选项 | `mango-system`        | 登录页租户下拉有数据。                              |
| 应用编码       | `mango-authorization` | `appCode` 能匹配应用授权边界。                      |
| 角色和菜单     | `mango-authorization` | 登录后 Shell 能加载菜单。                           |
| 验证码         | `mango-captcha`       | 登录页验证码能生成和校验。                          |
| Provider 配置  | `mango-auth`          | 按租户应用启用企业微信、钉钉，Secret 加密且不回传。 |

## 7. 管理入口

认证页通常由 Shell 路由挂载：

| 页面           | 路由或 key                   | 访问条件                                                                 |
| -------------- | ---------------------------- | ------------------------------------------------------------------------ |
| 登录页         | `/login`                     | 未登录即可访问。                                                         |
| 个人中心       | `profile/index`              | 已登录用户。                                                             |
| 修改密码       | `password/index`             | 已登录用户。                                                             |
| 第三方授权回调 | `/provider-callback`         | 未登录可访问；消费后会清除 URL 中的 code/state。                         |
| 第三方登录配置 | `auth/provider-config/index` | 需要 `auth:provider-config:view`，编辑需要 `auth:provider-config:edit`。 |

登录成功只代表拿到了 token 和用户上下文，不代表拥有管理后台菜单。菜单、按钮和接口权限继续由 authorization 后端返回和校验。

## 8. 快速开始

1. 后端启用 auth、identity、authorization、system tenant 和 captcha。
2. 前端安装 `@mango/auth`，引入 `@mango/auth/style.css`。
3. 在 Shell 启动时调用 `installMangoAuth()`，设置 `appCode`、租户默认值和跳转路径。
4. 路由挂载 `LoginView`、`ProfileView`、`PasswordView` 和公开的 `ProviderCallbackView`；hash 路由宿主需在创建路由前把入口 URL 上的 `code/authCode + state` 转到 `/provider-callback`。
5. 登录成功后用 `getUserInfo()` 刷新用户信息，再加载菜单。

## 9. 问题排查

| 问题                   | 常见原因                                          | 处理方式                                                               |
| ---------------------- | ------------------------------------------------- | ---------------------------------------------------------------------- |
| 登录 401               | 密码、验证码、租户、realm 或 appCode 不匹配       | 对照登录入参和 auth 后端日志。                                         |
| 登录页租户为空         | `/system/tenant/login-options` 无数据或接口未放行 | 检查租户初始化和公共路径配置。                                         |
| 登录成功但菜单为空     | 应用、角色、菜单或授权缺失                        | 检查 authorization 用户菜单接口。                                      |
| 企业微信或钉钉登录失败 | Provider 配置不完整、未启用或 redirectUri 不匹配  | 检查 `GET /auth/providers` 可用状态及当前租户应用的 Provider 配置。    |
| 修改密码失败           | 旧密码错误或后端密码策略不通过                    | 看 `POST /user/password` 返回信息。                                    |
| 个人中心空白           | 路由未挂载或样式未引入                            | 检查 Shell 路由和 `@mango/auth/style.css`。                            |
| 头像选择后未立即上传   | 头像上传与资料保存使用同一提交动作                | 点击“保存资料”；成功后资料字段应为 `mango-file:{id}`，而不是下载地址。 |

## 10. 相关文档

- [后端 Auth](../../../mango/mango-platform/mango-auth/README.md)
- [后端 Identity](../../../mango/mango-platform/mango-identity/README.md)
- [后端 Authorization](../../../mango/mango-platform/mango-authorization/README.md)
- [@mango/common](../common/README.md)
- [认证页面说明](./src/views/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 11. 变更影响记录

- Issue #764 将管理端企业微信登录收敛到统一 Provider 授权链路。`prepareWecomLogin()` 和 `wecomQrUrl` 改为消费 `/auth/providers/authorize` 返回的授权地址；`@mango/auth` 前端包不再导出 `/auth/wecom/login-config`、`/auth/wecom/login`、`mwc.` state、手工 code 登录或旧回调解析能力。回调统一由 `ProviderCallbackView` 完成，后端 `/auth/wecom/*` 兼容接口不在本次前端改动范围内。
- `@mango/auth@1.0.26` 公开 `@mango/auth/config` 中的 `registerMangoAuthProfileSections()`，供 Admin Shell 在 feature registrar 执行后集中装配个人中心扩展。登录、用户资料、权限、租户和既有 `ProfileView` 路由保持兼容。

- Issue 643 为个人中心增加页面内部设置导航，并把头像地址输入改为图片选择、保存时上传和受保护文件回显。修改密码已经收敛为个人中心内置子页；Admin Shell 通过 `profile.slots.theme` 注入页内主题设置，并保留独立 `PasswordView` 和 `/password` 兼容入口。`ProfileView` 的公开导出和路由 key 不变；新增 `MangoAvatar` 供宿主顶部用户区、System 用户组件及业务消费者统一回显文件头像。该变化不新增框架主菜单，不把对象存储地址或临时下载地址写入身份资料。
- 本次新增登录首次强制改密、密码复杂度提示和弱密码提交拦截；`LoginView` 和 `PasswordView` 都会展示密码规则，并按统一密码策略校验。登录成功后若后端返回 `passwordResetRequired=true` 或 `loginAction=CHANGE_PASSWORD`，前端会切换到改密弹窗而不是直接进入后台。该变更不改变 `login()`、`logout()`、`getUserInfo()`、`getLoginTenantOptions()` 和 `updatePassword()` 的接口路径。
- 本次新增 `useMangoLoginFlow()` 登录流程 hook，默认 `LoginView` 已改为复用该 hook。业务项目如需完全自定义登录页，可在自己的 `/login` 组件中调用 hook；页面 UI、布局和表单校验仍由业务组件负责。
