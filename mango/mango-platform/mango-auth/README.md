# Mango Auth

## 1. 能力定位

`mango-auth` 提供认证能力，负责登录、刷新 token、注销、token 校验、企微登录入口和 Spring Security 认证接入。主要使用者是平台认证服务、业务应用 starter 和需要远程调用认证服务的业务模块。

代码事实：

- 聚合模块 `io.mango.platform.auth:mango-auth`。
- 子模块包括 `mango-auth-api`、`mango-auth-core`、`mango-auth-starter`、`mango-auth-starter-remote`。
- 本地 HTTP Controller 路径为 `/auth`。
- 远程 Feign Client 服务名为 `mango-auth`，路径为 `/auth`。

## 2. 适用场景

- 用户名密码登录并签发 access token / refresh token。
- access token 校验、refresh token 换发、logout 注销。
- 通过 `AuthUserProvider` 读取 `mango-identity` 的认证用户事实。
- 接入验证码、登录租户选择、反重放、签名和幂等保护。
- 微服务模块通过 `mango-auth-starter-remote` 调用认证服务。

## 3. 不适用场景

- 不保存账号主数据，账号资料归属 `mango-identity`。
- 不维护角色、菜单、权限码和接口资源，授权归属 `mango-authorization`。
- 不替代边界入口访问控制，外部入口统一由 `mango-access` 承担。
- 不在 README 中复制 token、密码、权限等长期安全规则。

## 4. 模块边界

`mango-auth` 的边界是“认证事实生成和验证”。登录成功后的授权快照来自 `mango-authorization`，用户密码和状态来自 `mango-identity`，验证码能力来自 `mango-captcha`，短期状态和幂等能力可依赖 `mango-infra-kv`。

## 5. 接入方式

认证服务本地接入：

```xml
<dependency>
    <groupId>io.mango.platform.auth</groupId>
    <artifactId>mango-auth-starter</artifactId>
</dependency>
```

远程调用接入：

```xml
<dependency>
    <groupId>io.mango.platform.auth</groupId>
    <artifactId>mango-auth-starter-remote</artifactId>
</dependency>
```

只使用契约模型时依赖：

```xml
<dependency>
    <groupId>io.mango.platform.auth</groupId>
    <artifactId>mango-auth-api</artifactId>
</dependency>
```

## 6. 配置项

已发现配置前缀：

- `mango.auth.security`：认证安全配置，来源 `AuthSecurityProperties`。
- `mango.auth.anti-replay`：反重放配置，来源 `AntiReplayProperties`。

配置字段以对应 `@ConfigurationProperties` 类为准。

## 7. 对外接口 / 扩展点

- `AuthApi`：认证 Java API。
- `CaptchaConfigService`：验证码配置 SPI。
- `LoginTenantProvider`：登录租户选择 SPI。
- `AuthController` 运行时 HTTP 路径 `/auth`，包含 `/login`、`/login-institutions`、`/wecom/login`、`/wecom/login-config`、`/refresh`、`/logout`、`/validate`、`/info`、`/captcha/send`。
- `AuthFeignClient` 是 remote starter 适配层，业务代码优先依赖 `AuthApi` 契约。
- 主要命令对象包括 `LoginCommand`、`LoginTenantOptionsCommand`、`LogoutCommand`、`RefreshTokenCommand`、`ValidateTokenCommand`、`WecomLoginCommand`、`CaptchaSendRequest`。

## 8. 数据库 / 初始化数据

本模块未发现独立 Flyway migration。账号数据由 `mango-identity` 初始化，权限和菜单数据由 `mango-authorization` 初始化。

## 9. 菜单 / 权限 / 租户

本模块不提供独立管理菜单。登录时可携带 `realm`、`actorType`、`partyType`、`partyId`、`appCode` 等身份上下文；可选租户选择能力由 `LoginTenantProvider` 扩展。

## 10. 验证方式

最小验证命令：

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-auth -am test
```

该命令只验证 auth 模块及依赖的测试覆盖；完整登录闭环还需要准备 `mango-identity` 的真实账号、`mango-authorization` 的应用入口和授权关系，并按需接入 `mango-captcha` / `mango-infra-kv` 验证验证码、反重放和登录失败策略。

代表性接口验收：

- `POST /auth/login` 返回 access token 和 refresh token。
- `GET /auth/info` 可按 Authorization header 或 cookie 中的 token 返回当前登录信息。
- `POST /auth/login-institutions` 可返回登录前可选机构。
- `POST /auth/captcha/send` 可触发验证码发送链路。
- `GET /auth/wecom/login-config` 和 `POST /auth/wecom/login` 覆盖企微登录配置和登录入口。
- `POST /auth/refresh` 可换发 token。
- `POST /auth/validate` 能识别有效和无效 token。
- `POST /auth/logout` 后旧 token 不应继续通过校验。
- 反重放和登录失败锁定需要结合 `mango.auth.anti-replay`、`mango.auth.security` 配置做集成验收。

## 11. 业务接入最小闭环

业务应用接入 auth 时，先接入 `mango-auth-starter` 或 remote starter，并确认 identity 已存在可登录用户、authorization 已存在应用入口和授权关系。最小登录链路是调用 `POST /auth/login`，请求中提供登录域、用户名、密码和需要的 `appCode`；后续请求通过 Authorization header 或受信 cookie 传递 access token。

需要验证码时先调用 `/auth/captcha/send` 或 captcha 图形/行为接口，再把验证码 key 和答案放入登录命令。验收断言覆盖：正确账号可登录并刷新 token，错误密码触发失败策略，logout 后旧 token 失效，缺失或错误 `appCode` 不应得到错误授权快照。

## 12. 常见问题

- 登录失败先确认 `mango-identity` 中账号状态、密码哈希和登录域是否匹配。
- 权限为空先检查 `mango-authorization` 的角色、权限和应用入口绑定，而不是修改 auth。
- 验证码链路异常时检查 `CaptchaConfigService` 和 `mango-captcha` 接入。

## 13. 关联 PMO 规则

- [后端 API 规范](../../../mango-pmo/rules/backend/03-api.md)
- [后端安全规范](../../../mango-pmo/rules/backend/06-security.md)
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
