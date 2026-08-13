# Auth Views

## 1. 概览

本入口说明 `@mango/auth` 的登录、个人资料和修改密码页面。它们是认证前端页面入口，不负责后端认证、验证码、租户或用户数据实现。

## 2. 功能清单

来自 `@mango/auth`：

- `LoginView`
- `ProfileView`
- `PasswordView`
- `ProviderCallbackView`
- `ProviderConfigView`
- `useAuthConfig`
- `useUserInfo`
- `useMangoLoginFlow`
- `login`、`logout`、`getInfo`、`updatePassword` 等认证 API 封装。

`ProviderConfigView` 已由默认 `admin-pages` 注册为 `auth/provider-config/index`；登录、个人中心、改密和公开回调路由仍由 Shell 或宿主应用挂载。

## 3. 页面入口

- `/login`：管理端登录，支持租户选项、账号密码、企业微信和钉钉登录入口。
- `/profile`：真实个人资料、实名信息、联系方式验证和第三方授权管理；页面内部使用“个人资料、账号安全、第三方授权”导航，窄屏切换为顶部横向导航。
- `/password`：登录后修改密码。
- `/provider-callback`：公开 OAuth 回调；已绑定直接登录，未绑定时要求输入已有 Mango 账号密码。
- `auth/provider-config/index`：当前租户应用第三方登录配置。
- Shell 或业务应用读取登录品牌、租户选项、验证码和系统配置。

接入示例：

```ts
import { LoginView, ProfileView, PasswordView, ProviderCallbackView } from '@mango/auth';
import '@mango/auth/style.css';
```

路由示例：

```ts
[
  { path: '/login', component: LoginView },
  { path: '/profile', component: ProfileView },
  { path: '/password', component: PasswordView },
  { path: '/provider-callback', component: ProviderCallbackView },
];
```

页面组件不对外定义 props 或事件。业务项目需要完全自定义登录页时，应自行实现 `/login` 页面 UI，并通过 `useMangoLoginFlow()` 复用登录机构、账号密码登录、强制改密、登录态持久化和 redirect 逻辑；第三方登录使用 `startProviderAuthorization()` 跳转后端生成的授权地址，不显示手工 code 输入框，也不自行生成 state。

`ProfileView` 的头像不是 URL 文本字段。选择 JPG、PNG、WebP 图片后只生成本地预览，点击保存时才上传文件中心并把 `mango-file:{id}` 写入个人资料；移除头像同样通过保存资料生效。页面内部导航属于个人中心内容，不修改宿主框架左侧主菜单。

可配置项来自 `useAuthConfig()` 和后端系统配置。

## 4. 后端依赖

- 后端模块：`mango-platform/mango-auth`、`mango-platform/mango-identity`、`mango-platform/mango-captcha`、`mango-platform/mango-system`。
- API 前缀：`/auth/login`、`/auth/providers/**`、`/auth/provider-configs`、`/identity/me/**`、`/user/password`、`/system/tenant/login-options`。
- 验证码由 `@mango/common/api/captcha` 和后端 captcha/kv 能力提供。

## 5. 管理入口

- 登录页在未登录态访问，租户选项来自后端。
- 登录成功后的 token、用户信息、权限列表和租户上下文由后端认证链路返回。
- 修改密码接口由后端校验当前用户和旧密码。
- 前端页面不持久化权限真相，只消费后端返回的登录态和系统配置。

## 6. 问题排查

- 登录页租户为空时，检查 `/system/tenant/login-options`。
- 企业微信扫码后提示授权状态无效时，确认登录入口调用 `/auth/providers/authorize`，授权地址中的 state 由后端生成，而不是前端生成的 `mwc.` state。
- 验证码不显示时，检查 captcha 后端和 kv store。
- 登录成功但菜单为空时，继续检查 authorization/access/admin-shell 闭环。
- 头像 token 未回显时，确认消费位置使用 `@mango/common` 的 `MangoAvatar`，并检查受保护文件下载请求。

## 7. 相关文档

- [@mango/auth README](../../README.md)
- [Auth 后端 README](../../../../../mango/mango-platform/mango-auth/README.md)
- [Identity 后端 README](../../../../../mango/mango-platform/mango-identity/README.md)
- [Captcha 后端 README](../../../../../mango/mango-platform/mango-captcha/README.md)
- [能力地图](../../../../../mango-docs/capabilities/README.md)
- [能力说明维护规范](../../../../../mango-pmo/rules/08-capability-docs.md)
