# Mango Auth

## 1. 概览

`mango-auth` 是 Mango 的认证模块，提供用户名密码登录、登录前机构选择、access token / refresh token 签发、刷新、注销、校验、当前登录用户信息、验证码发送入口和企业微信扫码登录入口。

它只生成和校验认证事实，不保存用户主数据，不维护菜单和权限。账号、密码哈希、成员和外部身份绑定来自 `mango-identity`；角色、菜单、权限和授权快照来自 `mango-authorization`；边界入口拦截通常由 `mango-access` 执行。

## 2. 功能清单

| 能力 | 说明 |
|------|------|
| 用户名密码登录 | 校验账号密码、机构成员、用户状态，签发 access token 和 refresh token |
| 登录前机构选择 | 校验用户名密码后返回当前账号可登录的启用机构列表 |
| token 刷新 | 使用 refresh token 换发新的 access token 和 refresh token，并撤销旧 refresh token |
| 退出登录 | 注销当前 token，清理 `MANGO_TOKEN` Cookie |
| token 校验 | 校验 access token 是否有效、未撤销 |
| 当前用户信息 | 返回用户、机构、角色和权限列表 |
| 企业微信登录 | 用企业微信 code 换取外部用户，按已绑定 Mango 用户签发 token |
| 验证码入口 | 通过 `CaptchaApi` 发送短信或邮件验证码，登录请求可按路径要求验证码头 |
| 防重放能力 | 支持时间戳、nonce、幂等键和可选签名校验 |
| 远程调用契约 | 微服务可通过 `mango-auth-starter-remote` 使用 `AuthApi` Feign Client |

## 3. 后端接入

业务模块只需要使用认证契约时依赖 API 包：

```xml
<dependency>
    <groupId>io.mango.platform.auth</groupId>
    <artifactId>mango-auth-api</artifactId>
</dependency>
```

部署认证服务或单体应用内启用认证 HTTP 接口时依赖 starter：

```xml
<dependency>
    <groupId>io.mango.platform.auth</groupId>
    <artifactId>mango-auth-starter</artifactId>
</dependency>
```

微服务中远程调用认证服务时依赖 remote starter：

```xml
<dependency>
    <groupId>io.mango.platform.auth</groupId>
    <artifactId>mango-auth-starter-remote</artifactId>
</dependency>
```

`mango-auth-starter-remote` 注册的 Feign Client 服务名是 `mango-auth`，路径是 `/auth`。

部署 `mango-auth-starter` 的应用需要具备这些依赖能力：

| 依赖能力 | 用途 |
|----------|------|
| `AuthUserProvider` | 按 `realm + username` 或 userId 读取认证用户、密码哈希、状态和主体信息 |
| `LoginTenantProvider` | 返回用户可登录机构，登录时解析 `memberId`、`tenantId`、`tenantCode`、`tenantName` |
| `IdentityUserApi` | `/auth/info` 和企业微信登录读取用户资料、外部身份绑定 |
| `ITokenProvider` | 生成、刷新、校验 token，读取 token claim |
| `IAuthorizationProvider` | 加载角色编码和权限编码，写入 `LoginVO` |
| `NoticeApi` | 企业微信扫码登录读取通知中心企业微信渠道配置 |
| `CaptchaApi` | 验证码发送和登录验证码校验 |
| `SysLoginLogApi` | 可选，记录登录日志 |
| `IpLocationResolver` | 可选，登录日志解析 IP 位置 |
| `IKvStore` | 可选，用于登录失败计数、refresh token replay、防重放 nonce 和幂等键 |

## 4. 前端接入

管理后台登录页来自 `@mango/auth`，通常由 `@mango/admin-shell` 路由到 `/login`：

```ts
import { LoginView, ProfileView, PasswordView, installMangoAuth } from '@mango/auth';
import '@mango/auth/style.css';
```

前端登录默认值通过 Admin Shell 的 `login` 配置传入，常用字段包括：

| 字段 | 默认行为 |
|------|----------|
| `realm` | 登录页默认使用 `INTERNAL` |
| `actorType` | 登录页默认使用 `INTERNAL_USER` |
| `partyType` | 登录页默认使用 `INTERNAL_ORG` |
| `appCode` | 登录页默认使用 `internal-admin` |
| `tenantCode` | 登录页会优先选中匹配机构，常见默认值是 `default` |
| `redirectPath` | 登录成功后跳转路径，未配置时跳转 `/home` |

登录成功后，前端应使用 `Authorization: Bearer <accessToken>` 调用后端接口。后端登录接口也会写入 `MANGO_TOKEN` HttpOnly Cookie，供同域场景使用。

## 5. 快速开始

1. 准备 identity 用户，确认 `realm + username` 可读取到密码哈希、状态和主体信息。
2. 准备用户机构成员，确认 `LoginTenantProvider` 能返回启用的 `tenantId`、`tenantCode` 和 `memberId`。
3. 准备 authorization 应用、角色、菜单权限和成员角色绑定。
4. 部署认证服务时接入 `mango-auth-starter`，并配置 `mango.security.jwt.secret`。
5. 前端先调 `/auth/login-institutions` 获取可登录机构，再调 `/auth/login`。
6. 登录成功后保存 `accessToken`，后续请求带 `Authorization: Bearer <accessToken>`。
7. 调 `/auth/info` 确认返回的用户、角色和权限与授权配置一致。

## 6. 配置说明

最小 JWT 配置：

```yaml
mango:
  security:
    jwt:
      secret: "replace-with-at-least-32-byte-secret"
      access-token-validity: 7200
      refresh-token-validity: 604800
```

Spring Security 放行路径示例：

```yaml
mango:
  auth:
    security:
      permit-paths:
        - /auth/login
        - /auth/login-institutions
        - /auth/refresh
        - /auth/captcha/send
```

验证码和防重放示例：

```yaml
mango:
  captcha:
    required-paths:
      - /auth/login
    ttl: 300
  auth:
    anti-replay:
      app-secrets:
        admin-web: "replace-with-md5-secret-or-base64-public-key"
      allow-fallback: false
```

## 7. YAML 配置字段

| 配置项 | 默认值 | 含义 |
|--------|--------|------|
| `mango.security.jwt.secret` | 无 | JWT HMAC 密钥，`JjwtTokenServiceImpl` 要求 UTF-8 字节长度至少 32 |
| `mango.jwt.secret` | 无 | 历史兼容密钥，仅在 `mango.security.jwt.secret` 为空时使用 |
| `mango.security.jwt.access-token-validity` | `7200` | access token 有效期，单位秒 |
| `mango.security.jwt.refresh-token-validity` | `604800` | refresh token 有效期，单位秒 |
| `mango.auth.security.permit-paths` | 空列表 | Spring Security 层直接匿名放行的 Ant 路径 |
| `mango.captcha.required-paths` | `/login,/register` | `CaptchaConfigService` 判断哪些路径需要验证码 |
| `mango.captcha.ttl` | `300` | 默认验证码有效期，单位秒 |
| `mango.auth.anti-replay.app-secrets` | 空 map | appKey 到 secret 的映射 |
| `mango.auth.anti-replay.default-secret` | 空 | 兜底签名密钥 |
| `mango.auth.anti-replay.allow-fallback` | `false` | 未找到 appKey 时是否允许使用默认密钥 |

## 8. 接口/API 使用

HTTP 接口前缀是 `/auth`。

| 方法 | 路径 | 访问模式 | 用途 |
|------|------|----------|------|
| POST | `/auth/login` | PUBLIC | 用户名密码登录，成功后返回 `LoginVO` 并写入 `MANGO_TOKEN` Cookie |
| POST | `/auth/login-institutions` | PUBLIC | 查询账号可登录机构 |
| POST | `/auth/wecom/login` | PUBLIC | 企业微信 code 登录 |
| GET | `/auth/wecom/login-config?tenantId=...` | PUBLIC | 查询企业微信扫码登录公开配置 |
| POST | `/auth/refresh` | PUBLIC | 使用 refresh token 换发 token |
| POST | `/auth/logout` | LOGIN | 注销 token，清理 Cookie |
| POST | `/auth/validate` | LOGIN | 校验 access token 是否有效 |
| GET | `/auth/info` | LOGIN | 返回当前用户、角色和权限 |
| POST | `/auth/captcha/send` | PUBLIC | 发送短信或邮件验证码 |

`LoginCommand`：

| 字段 | 必填 | 含义 |
|------|------|------|
| `username` | 是 | 用户名 |
| `password` | 是 | 密码 |
| `tenantId` | 与 `tenantCode` 二选一 | 机构 ID |
| `tenantCode` | 与 `tenantId` 二选一 | 机构编码 |
| `realm` | 否 | 登录域，例如 `INTERNAL`、`CUSTOMER` |
| `actorType` | 否 | 操作者类型，例如 `INTERNAL_USER` |
| `partyType` | 否 | 归属主体类型，例如 `INTERNAL_ORG` |
| `partyId` | 否 | 归属主体 ID |
| `appCode` | 否 | 应用编码，后端默认 `internal-admin` |
| `captchaCode` | 否 | 验证码。当前登录验证码拦截器主要读取请求头 |
| `captchaKey` | 否 | 验证码键。当前登录验证码拦截器主要读取请求头 |

需要验证码时，请求头使用：

| 请求头 | 含义 |
|--------|------|
| `X-Captcha-Key` | 验证码 key |
| `X-Captcha-Code` | 用户输入的验证码 |
| `X-Captcha-Type` | 验证码类型，未传时按 `ARITHMETIC` |

防重放请求头：

| 请求头 | 行为 |
|--------|------|
| `X-Request-Timestamp` | 可选；毫秒时间戳，和服务端时间差不能超过 5 分钟 |
| `X-Replay-Nonce` | 可选；存在时不能重复 |
| `X-Idempotency-Key` | 可选；POST、PUT、DELETE 请求存在时做幂等控制 |
| `X-Sign-Algorithm` | 和 `X-App-Key`、`X-Sign` 同时存在时触发签名校验 |
| `X-App-Key` | 签名应用标识 |
| `X-Sign` | 请求签名 |

Java API 使用 `AuthApi`，本地 starter 会注册 `AuthApiAdapter`，remote starter 会注册 Feign Client：

```java
R<LoginVO> response = authApi.login(command);
```

## 9. 返回字段

`LoginVO`：

| 字段 | 含义 |
|------|------|
| `accessToken` | 访问令牌 |
| `tokenType` | 固定为 `Bearer` |
| `expiresIn` | access token 有效期，单位秒 |
| `refreshToken` | 刷新令牌 |
| `userId` | 用户 ID |
| `memberId` | 当前机构成员 ID |
| `username` | 用户名 |
| `nickname` | 昵称 |
| `realm` | 登录域 |
| `actorType` | 操作者类型 |
| `partyType` | 归属主体类型 |
| `partyId` | 归属主体 ID |
| `tenantId` | 当前机构 ID |
| `tenantCode` | 当前机构编码 |
| `tenantName` | 当前机构名称 |
| `appCode` | 应用编码 |
| `roles` | 角色编码列表 |
| `permissions` | 权限编码列表 |

`LoginTenantVO`：

| 字段 | 含义 |
|------|------|
| `tenantId` | 机构 ID |
| `tenantCode` | 机构编码 |
| `tenantName` | 机构名称 |
| `memberId` | 当前账号在机构下的成员 ID |
| `memberName` | 成员显示名称 |
| `memberType` | 成员类型 |

`WecomLoginConfigVO`：

| 字段 | 含义 |
|------|------|
| `channelConfigId` | 通知渠道配置 ID |
| `corpId` | 企业微信 CorpId |
| `agentId` | 企业微信 AgentId |
| `redirectUri` | 扫码登录回调地址 |

## 10. 管理入口

`mango-auth` 后端不提供独立管理菜单。

相关前端页面来自 `@mango/auth`：

| 页面 | 页面 key 或路由 | 说明 |
|------|-----------------|------|
| 登录页 | `/login`，组件 `LoginView` | 账号密码、机构选择、企业微信登录 |
| 个人中心 | `profile/index`，组件 `ProfileView` | 当前用户资料 |
| 修改密码 | `password/index`，组件 `PasswordView` | 当前用户修改密码 |

登录日志如果启用了 `SysLoginLogApi`，管理入口在 `mango-system` 的登录日志页面。

## 11. 数据与初始化

`mango-auth` 没有独立 Flyway migration。认证链路需要这些数据先存在：

| 数据 | 来源模块 | 用途 |
|------|----------|------|
| 用户、密码哈希、用户状态 | `mango-identity` | 登录校验 |
| 可登录机构、成员 ID | `mango-org` / `mango-identity` 提供的 `LoginTenantProvider` | 解析登录机构上下文 |
| 外部身份绑定 | `mango-identity` | 企业微信账号映射到 Mango 用户 |
| 企业微信渠道配置 | `mango-notice` | 查询 CorpId、AgentId、Secret、RedirectUri |
| 应用、角色、菜单、权限、成员角色绑定 | `mango-authorization` | 登录响应返回 roles 和 permissions |
| 登录日志 | `mango-system` | 可选，记录登录成功或失败 |
| KV 数据 | `mango-infra-kv` | 可选，登录失败计数、token 撤销、防重放和幂等 |

token claim 会写入 `username`、`realm`、`actorType`、`partyType`、`partyId`、`memberId`、`tenantId`、`tenantCode`、`tenantName`、`appCode`。后续 `mango-access` 和业务服务依赖这些 claim 建立请求上下文。

## 12. 问题排查

| 现象 | 排查点 |
|------|--------|
| 启动失败或 token 无法签发 | 检查 `mango.security.jwt.secret` 是否配置，长度是否满足 JJWT 要求 |
| 登录提示机构必选 | `/auth/login` 请求体必须传 `tenantId` 或 `tenantCode` |
| 登录提示机构不可用或无成员 | 检查 `LoginTenantProvider` 是否能返回该用户的启用机构和 `memberId` |
| 登录成功但权限为空 | 检查 authorization 的应用编码、角色、菜单权限和成员角色绑定；默认 `appCode` 是 `internal-admin` |
| `/auth/info` 失败 | 检查 Authorization 请求头是否是 `Bearer <accessToken>`，且 token claim 中有 `memberId` |
| refresh token 重复刷新失败 | 刷新成功后旧 refresh token 会被撤销，客户端必须保存新的 refresh token |
| 企微登录提示未绑定 | 检查 identity 外部身份绑定是否存在 `WECOM + corpId + externalUserId` |
| 企微配置为空 | 检查 notice 中当前机构是否有启用的企业微信登录渠道配置 |
| 登录被验证码拦截返回 428 | 按 `X-Captcha-Key`、`X-Captcha-Code`、`X-Captcha-Type` 补齐请求头，或检查 `mango.captcha.required-paths` |
| 防重放返回 401/409 | 检查客户端时间、nonce 是否重复、幂等键是否复用、签名密钥和算法是否匹配 |

## 13. 相关文档

- [Mango Access](../mango-access/README.md)
- [Mango Authorization](../mango-authorization/README.md)
- [Mango Identity](../mango-identity/README.md)
- [Mango Captcha](../mango-captcha/README.md)
- [@mango/auth](../../../mango-ui/packages/auth/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
