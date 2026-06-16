# Mango Auth

## 1. 概览
`mango-auth` 是 Mango 的认证模块，负责用户名密码登录、登录前机构选择、access token / refresh token 签发、刷新、注销、校验、当前用户信息和企业微信扫码登录入口。它生成认证事实，但不保存账号主数据，也不维护菜单和权限。

代码事实：

- Maven 聚合模块：`io.mango.platform.auth:mango-auth`。
- 子模块：`mango-auth-api`、`mango-auth-core`、`mango-auth-starter`、`mango-auth-starter-remote`。
- 本地 HTTP 路径：`/auth`。
- Remote starter Feign 服务名：`mango-auth`，路径 `/auth`。
- 登录默认应用编码：`internal-admin`。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 管理后台或业务应用使用用户名、密码、机构和应用入口登录 | Maven 依赖 / HTTP API / Java API |
| 登录页先校验用户名密码，再列出可登录机构 | Maven 依赖 / HTTP API / Java API |
| 使用 refresh token 换发新的 access token / refresh token | Maven 依赖 / HTTP API / Java API |
| 用户退出后让旧 token 进入撤销状态 | Maven 依赖 / HTTP API / Java API |
| 通过企业微信 code 换取已绑定 Mango 用户并签发 token | Maven 依赖 / HTTP API / Java API |
| 需要对请求时间戳、nonce、幂等键或签名做统一防重放校验 | Maven 依赖 / HTTP API / Java API |
| 微服务模块通过 remote starter 调用认证服务 | Maven 依赖 / HTTP API / Java API |

## 3. 适用场景
- 管理后台或业务应用使用用户名、密码、机构和应用入口登录。
- 登录页先校验用户名密码，再列出可登录机构。
- 使用 refresh token 换发新的 access token / refresh token。
- 用户退出后让旧 token 进入撤销状态。
- 通过企业微信 code 换取已绑定 Mango 用户并签发 token。
- 需要对请求时间戳、nonce、幂等键或签名做统一防重放校验。
- 微服务模块通过 remote starter 调用认证服务。

## 4. 边界说明
- 不保存账号、密码哈希、成员和外部身份绑定；这些属于 `mango-identity`。
- 不维护角色、菜单、权限码和接口访问策略；这些属于 `mango-authorization`。
- 不承担边界入口拦截；外部入口由 `mango-access` 或 Spring Security filter chain 消费 token 和授权策略。
- 不创建租户、组织、角色或菜单默认数据。

## 5. 模块组成
登录链路依赖四类外部事实：

- `AuthUserProvider` 从 `mango-identity` 读取 `realm + username` 对应用户、密码哈希和状态。
- `LoginTenantProvider` 返回用户可登录机构，并提供 `memberId`、`tenantId`、`tenantCode`、`tenantName`。
- `ITokenProvider` 由 `mango-authorization-support` 提供 JJWT 实现，负责签发和校验 token。
- `IAuthorizationProvider` 从 `mango-authorization` 加载角色和权限快照，写入登录响应。

登录成功后 token claim 会携带：

- `username`
- `realm`
- `actorType`
- `partyType`
- `partyId`
- `memberId`
- `tenantId`
- `tenantCode`
- `tenantName`
- `appCode`

后续 `mango-access` 和业务服务依赖这些 claim 建立请求上下文。

## 6. 接入方式
认证服务本地接入：

```xml
<dependency>
    <groupId>io.mango.platform.auth</groupId>
    <artifactId>mango-auth-starter</artifactId>
</dependency>
```

微服务远程调用接入：

```xml
<dependency>
    <groupId>io.mango.platform.auth</groupId>
    <artifactId>mango-auth-starter-remote</artifactId>
</dependency>
```

只使用契约对象和 Java API：

```xml
<dependency>
    <groupId>io.mango.platform.auth</groupId>
    <artifactId>mango-auth-api</artifactId>
</dependency>
```

本地认证服务还需要装配：

- `AuthUserProvider` 或 `IdentityUserApi`。
- `LoginTenantProvider`。
- `IAuthorizationProvider`。
- `ITokenProvider`。
- `PasswordEncoder`。
- 可选 `CaptchaApi`、`NoticeApi`、`SysLoginLogApi`、`IpLocationResolver`、`IKvStore`。

## 7. 配置说明
### 6.1 Spring Security 放行路径

配置前缀：`mango.auth.security`。

| 配置项 | 默认值 | 作用 |
|--------|--------|------|
| `mango.auth.security.permit-paths` | 空列表 | Spring Security 层直接匿名放行的 Ant 路径 |

示例：

```yaml
mango:
  auth:
    security:
      permit-paths:
        - /auth/login
        - /auth/refresh
        - /auth/captcha/send
```

说明：`AuthController` 已通过 `@ApiAccess` 声明访问模式；这里控制的是 Spring Security filter chain 的放行匹配。不要用它替代 authorization 资源策略验收。

### 6.2 JWT

JJWT 实现在 `JjwtTokenServiceImpl`，配置前缀来自 `mango.security.jwt`。

| 配置项 | 默认值 | 作用 |
|--------|--------|------|
| `mango.security.jwt.secret` | 无，必填 | JWT HMAC 密钥，UTF-8 字节长度必须至少 32 |
| `mango.jwt.secret` | 无 | 历史兼容密钥；仅当新配置为空时使用 |
| `mango.security.jwt.access-token-validity` | `7200` | access token 有效期，单位秒 |
| `mango.security.jwt.refresh-token-validity` | `604800` | refresh token 有效期，单位秒 |

示例：

```yaml
mango:
  security:
    jwt:
      secret: "replace-with-at-least-32-byte-secret"
      access-token-validity: 7200
      refresh-token-validity: 604800
```

如果存在 `IKvStore`，refresh token 刷新时会记录 `jwt:refresh:jti:<jti>`，防止同一个 refresh token 被重复使用；如果没有 `IKvStore`，启动日志会提示 refresh token replay protection disabled。

### 6.3 反重放、幂等和签名

配置前缀：`mango.auth.anti-replay`。

| 配置项 | 默认值 | 作用 |
|--------|--------|------|
| `mango.auth.anti-replay.app-secrets` | 空 map | appKey 到 secret 的映射；SM2/RSA 使用 Base64 公钥，MD5 使用密钥 |
| `mango.auth.anti-replay.default-secret` | 空 | 默认签名密钥 |
| `mango.auth.anti-replay.allow-fallback` | `false` | 未找到 appKey 时是否允许使用默认密钥 |

请求头：

| 请求头 | 行为 |
|--------|------|
| `X-Request-Timestamp` | 可选；存在时必须是毫秒时间戳，和服务端时间差不能超过 5 分钟 |
| `X-Replay-Nonce` | 可选；存在时通过 `ReplayGuard` 占用 `replay:<nonce>`，TTL 10 分钟 |
| `X-Idempotency-Key` | 可选；POST/PUT/DELETE 存在时通过 `IdempotencyGuard` 占用 `idem:<key>`，TTL 24 小时 |
| `X-Sign-Algorithm` | 和 `X-App-Key`、`X-Sign` 同时存在时触发签名校验，支持 `SM2`、`RSA`、`MD5` |
| `X-App-Key` | 签名应用标识，用于查 `app-secrets` |
| `X-Sign` | 请求签名 |

示例：

```yaml
mango:
  auth:
    anti-replay:
      app-secrets:
        admin-web: "replace-with-md5-secret-or-base64-public-key"
      allow-fallback: false
```

签名待签字符串由 `appKey`、`secret`、`timestamp`、`body` 按 key 字典序拼接。只有三个签名请求头都存在时才校验签名；仅配置密钥不会自动要求所有请求必须签名。

## 8. API 与扩展
HTTP 接口：

| 方法 | 路径 | 访问模式 | 说明 |
|------|------|----------|------|
| POST | `/auth/login` | PUBLIC | 用户名密码登录，成功后返回 token 并写入 `MANGO_TOKEN` HttpOnly Cookie |
| POST | `/auth/login-institutions` | PUBLIC | 登录前查询账号可进入的启用机构 |
| POST | `/auth/wecom/login` | PUBLIC | 企业微信扫码或工作台 code 登录 |
| GET | `/auth/wecom/login-config` | PUBLIC | 按 `tenantId` 查询企微扫码登录公开配置 |
| POST | `/auth/refresh` | PUBLIC | 使用 refresh token 换发 token |
| POST | `/auth/logout` | LOGIN | 注销当前 token，并清理浏览器 Cookie |
| POST | `/auth/validate` | LOGIN | 校验 access token 是否有效且未撤销 |
| GET | `/auth/info` | LOGIN | 根据 access token 返回当前用户、角色和权限 |
| POST | `/auth/captcha/send` | PUBLIC | 调用 `CaptchaApi` 发送短信或邮件验证码 |

主要入参：

| 对象 | 必填字段 | 关键可选字段 |
|------|----------|--------------|
| `LoginCommand` | `username`、`password` | `tenantId` 或 `tenantCode` 至少传一个；`realm`、`actorType`、`partyType`、`partyId`、`appCode`、`captchaCode`、`captchaKey` |
| `LoginTenantOptionsCommand` | `username`、`password` | `realm`、`appCode` |
| `RefreshTokenCommand` | `refreshToken` | 可带 `Bearer ` 前缀 |
| `LogoutCommand` | `token` | 也可用 `Authorization` 请求头传入 |
| `ValidateTokenCommand` | `token` | 可带 `Bearer ` 前缀 |
| `WecomLoginCommand` | `code` | `channelConfigId`、`tenantId`、`tenantCode`、`appCode` |

扩展点：

- `AuthApi`：认证 Java API。
- `CaptchaConfigService`：验证码配置 SPI。
- `LoginTenantProvider`：登录机构选择 SPI，登录时必须能按用户和机构返回启用成员。
- `AppSecretProvider`：签名密钥提供者，默认使用 `mango.auth.anti-replay` 配置。
- `TokenRevocationService`：注销和刷新后撤销旧 token 的扩展点。

## 9. 数据与初始化
本模块没有独立 Flyway migration。

认证链路依赖的数据来源：

- 账号、密码哈希、状态、登录域来自 `mango-identity` 的 `identity_user`。
- 可登录机构和成员来自 `tenant_member`。
- 外部身份绑定来自 `identity_external_binding`。
- 应用入口、角色、菜单、权限和成员角色绑定来自 `mango-authorization`。
- refresh replay 和反重放使用 `IKvStore`，key 分别包含 `jwt:refresh:jti:`、`replay:`、`idem:`。

## 10. 管理入口
`mango-auth` 不提供管理菜单。

登录时必须明确机构上下文：

- `tenantId` 和 `tenantCode` 至少传一个。
- 后端通过 `LoginTenantProvider` 校验当前用户是否有该机构启用成员。
- `memberId` 是后续权限快照主体，缺失会导致登录失败或权限为空。
- `appCode` 为空时默认 `internal-admin`。

登录成功后 `LoginVO.roles` 和 `LoginVO.permissions` 来自 `IAuthorizationProvider.load`，不是 auth 自己计算。权限为空时应优先检查角色菜单和授权绑定，而不是修改 auth。

## 11. 快速开始
业务应用做登录接入时，最小顺序是：

1. 在 identity 中创建用户，确认 `realm + username` 唯一、密码哈希正确、状态为 1。
2. 为用户建立 `tenant_member`，并能通过 `LoginTenantProvider` 查询到启用机构。
3. 在 authorization 中建立应用入口、角色和成员角色绑定。
4. 配置 `mango.security.jwt.secret`。
5. 调用 `/auth/login-institutions` 让用户选择机构。
6. 调用 `/auth/login`，后续请求使用 `Authorization: Bearer <accessToken>`。
7. 使用 `/auth/info` 校验角色和权限是否符合预期。

## 12. 问题排查
- 启动失败提示 JWT secret：配置 `mango.security.jwt.secret`，长度至少 32 字节。
- 登录提示机构必选：`LoginCommand` 中传 `tenantId` 或 `tenantCode`。
- 登录后权限为空：检查 authorization 的角色、菜单、权限和成员角色绑定。
- refresh token 重复刷新失败：这是 replay protection 行为，应使用最新 refresh token。
- 企微登录提示未绑定：先通过 identity 外部身份接口绑定 `WECOM + corpId + externalUserId`。
- 防重放请求被拒：检查客户端时间、nonce 是否重复、签名密钥和算法是否匹配。

## 13. 相关文档
- [后端 API 规范](../../../mango-pmo/rules/backend/03-api.md)
- [后端安全规范](../../../mango-pmo/rules/backend/06-security.md)
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史资料
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
