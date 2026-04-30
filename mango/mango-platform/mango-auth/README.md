# Mango Auth

> 认证模块，负责登录、发放 token，并把认证结果接入 Spring Security。

## 模块职责

| 职责 | 说明 |
|------|------|
| 用户认证 | 用户名密码登录、Token发放 |
| Token管理 | Token生成、刷新、失效 |
| 认证接入 | JWT 解析后构建 `Authentication`，写入 `SecurityContextHolder` |

`mango-auth` 不负责接口资源扫描、权限码注册或权限数据库写入。这些能力归属 `mango-authorization`，由 `mango-authorization-resource-sync-starter` 在每个 App 内扫描 Spring MVC 接口并注册接口资源；接口访问策略统一使用 `@ApiAccess` 声明。运行时如存在 `apiResourceAuthorizationManager`，`mango-auth-starter` 会把 `anyRequest` 委托给该 URL 级授权管理器，否则保持默认的已登录校验。

## 子模块

```
mango-auth/
├── mango-auth-api/           # API 契约（Command、VO、AuthApi）
├── mango-auth-core/          # 认证业务（IAuthService、AuthServiceImpl、token 注销）
├── mango-auth-starter/       # 本地启动器（Controller、JWT 过滤链、Web 拦截器）
└── mango-auth-starter-remote/ # 远程启动器（Feign Client）
```

## 依赖关系

```
mango-auth-starter
├── mango-auth-core (认证业务逻辑)
├── mango-infra-web-starter (Spring Web + request context)
├── mango-infra-security-starter (Spring Security + ITokenProvider)
├── mango-identity-api (认证用户事实)
├── mango-authorization-starter (授权能力接入)
└── mango-captcha-api / mango-infra-kv-api
```

`mango-auth` 不保存账号资料，也不计算授权。账号资料由 `mango-identity` 提供，角色/权限由 `mango-authorization` 提供。`mango-auth` 不再维护独立的 servlet request attribute 认证链，当前基线统一走 Spring Security。

## 核心类

### AuthApi

认证服务接口：
```java
public interface AuthApi {
    R<LoginVO> login(LoginCommand command);
    R<Void> logout(LogoutCommand command);
    R<LoginVO> refreshToken(RefreshTokenCommand command);
    R<Boolean> validateToken(ValidateTokenCommand command);
}
```

### IAuthService

认证业务接口：
```java
public interface IAuthService {
    LoginVO login(LoginCommand command);
    void logout(String token);
    LoginVO refreshToken(String refreshToken);
    boolean validateToken(String token);
}
```

### Command / LoginVO

登录请求和响应：
```java
// 请求
- username: 用户名
- password: 密码
- realm: 登录域，可选；未传时默认按 INTERNAL 查询
- actorType: 操作者类型，可选；优先使用请求值，其次使用 identity 用户资料
- partyType / partyId: 归属主体，可选；用于客户、机构、公司等业务主体隔离
- appCode: 应用入口编码，可选；用于登录后加载当前应用角色和权限
- captchaCode / captchaKey: 验证码字段，可选

// 响应
- accessToken: 访问令牌
- refreshToken: 刷新令牌
- expiresIn: 过期时间
- tokenType: Bearer
- userId: 用户ID
- username: 用户名
- realm / actorType / partyType / partyId / appCode: 本次登录身份上下文
- roles / permissions: 当前 appCode 下的授权快照
```

认证接口统一使用 `LoginCommand`、`RefreshTokenCommand`、`LogoutCommand`、`ValidateTokenCommand` 和 `LoginVO`，不保留兼容别名类型。

## 认证流程

### 登录流程

```
┌────────┐     ┌─────────┐     ┌──────────┐     ┌─────────┐
│ Client │────▶│  BFF    │────▶│  Auth    │────▶│  User   │
│        │     │         │     │ Service  │     │ Service │
└────────┘     └─────────┘     └──────────┘     └─────────┘
   │              │                │                 │
   │ 1.登录请求   │ 2.调用登录     │ 3.验证用户       │
   │ (用户名/密码) │                │                 │
   │              │                │                 │
   │◀─────────────│◀──────────────│◀──────────────│
   │ 4.返回Token  │                │                 │
```

### Token验证流程（请求拦截）

```
┌────────┐     ┌─────────────────────┐     ┌────────────────────────┐
│ Client │────▶│ AuthTokenAuthFilter │────▶│ Spring Security Context │
│        │     │                     │     │ + Controller / Service  │
└────────┘     └─────────────────────┘     └────────────────────────┘
   │                  │                                │
   │ 1.带Token请求    │ 2.验证 access token            │
   │                  │ 3.构建 Authentication          │
   │                  │ 4.写入 SecurityContextHolder   │
   │◀─────────────────│◀───────────────────────────────│
   │ 5.通过 / 401     │                                │
```

## 单体模式 vs 微服务模式

### 单体模式

```
┌────────────────────────────────────────────────────────┐
│           单体应用 (BFF Admin)                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │ MangoAuthStarter                                 │  │
│  │  - AuthController (/auth/login)                 │  │
│  │  - AuthSecurityConfig                           │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │ MangoAuthorizationWebStarter                    │  │
│  │  - AuthFilter (JWT验证)                          │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

### 微服务模式

```
┌─────────────────────────────────────────────────────────┐
│                    API Gateway                          │
│  ┌─────────────────────────────────────────────────┐   │
│  │ AuthFilter (JWT验证)                             │   │
│  │  - 验证通过后，透传 X-Mango-* 上下文头           │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────┬───────────────────────────────────┘
                      │ HTTP (X-User-Id Header)
          ┌───────────┼───────────────────────┐
          ▼           ▼                       ▼
┌───────────────┐ ┌───────────────┐      ┌───────────────┐
│ BFF Admin     │ │ BFF Portal    │      │ Auth          │
│               │ │               │      │ Service       │
│               │ │               │      │ (独立部署)     │
└───────────────┘ └───────────────┘      └───────────────┘
```

## AuthSecurityConfig

认证安全配置，提供 Spring Security `SecurityFilterChain` 和 JWT 认证过滤器：

```java
@Configuration
public class AuthSecurityConfig {
    @Bean
    public SecurityFilterChain authSecurityFilterChain(HttpSecurity http, ...) {
        http
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().access(apiResourceAuthorizationManager)
            )
            .addFilterBefore(new AuthTokenAuthenticationFilter(tokenService),
                UsernamePasswordAuthenticationFilter.class);
    }
}
```

### AuthTokenAuthenticationFilter 逻辑

1. 获取 `Authorization` 请求头
2. 提取并验证 `Bearer Token`，并检查 token 是否已注销
3. 验证成功后构建 `SecurityPrincipal`，从 request context 透传 `tenantId`
4. 将 `Authentication` 写入 `SecurityContextHolder`
5. 请求访问模式由 `@ApiAccess` 同步后的 API 资源策略决定
6. 未认证请求由 Spring Security 返回 401

## Token 注销与登录保护

- `TokenRevocationService` 使用 `mango-infra-kv-api` 保存注销 token 的 SHA-256 摘要，`logout` 和 `refreshToken` 会写入注销记录。
- `validateToken` 与 JWT 过滤链都会拒绝已注销 token。
- `LoginAttemptTracker` 优先使用 `IKvStore` 做分布式登录失败计数，没有 KV 实现时降级为本地内存计数。
- 防重放和验证码 Web 拦截器位于 `mango-auth-starter`，`mango-auth-core` 不再持有 MVC / Servlet 边界代码。
- 请求签名密钥通过 `AppSecretProvider` 获取，默认实现读取 `mango.auth.anti-replay.app-secrets` 配置；未知 `appKey` 默认拒绝，只能显式开启 `allow-fallback` 后使用默认密钥兜底。

```yaml
mango:
  auth:
    anti-replay:
      app-secrets:
        demo-app: demo-secret
      allow-fallback: false
```

## 使用方式

### 登录示例

```bash
# 用户名密码登录
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123","realm":"INTERNAL","appCode":"internal-admin"}'

# 响应
{
  "code": 200,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 7200,
    "tokenType": "Bearer",
    "userId": 1,
    "username": "admin",
    "realm": "INTERNAL",
    "actorType": "INTERNAL_USER",
    "appCode": "internal-admin"
  }
}
```

### 使用Token访问

```bash
curl -X GET http://localhost:8080/permission/menu \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."
```

### 刷新Token

```bash
curl -X POST http://localhost:8080/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"eyJhbGciOiJIUzI1NiIs..."}'
```

## 测试基线

- 集成测试：
  - `AuthSecurityIntegrationTest`，覆盖 `Bearer token -> Spring Security context -> URL 级访问策略` 的服务内集成链路，验证 `200 / 401 / 403`。
- E2E 测试：
  - `AuthSecurityE2ETest`，覆盖 `/auth/login -> accessToken -> 受保护接口` 的完整 HTTP 链路。
- 测试上下文已排除 Flyway、数据源、MyBatis、Authorization 自动配置等无关装配，避免把认证链路测试污染成数据库集成测试。

## 验证命令

```bash
cd mango
mvn -q -DskipTests compile -pl mango-platform/mango-auth -am
mvn -q test -pl mango-platform/mango-auth/mango-auth-starter -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AuthSecurityConfigTest
mvn -q test -pl mango-platform/mango-auth/mango-auth-starter -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AuthSecurityIntegrationTest,AuthSecurityE2ETest
```

## 常见问题

### Q: 登录失败返回什么？

```json
{
  "code": 401,
  "message": "用户名或密码错误"
}
```

### Q: Token过期后怎么办？

使用 refreshToken 调用 `/auth/refresh` 获取新的 accessToken。

### Q: 如何自定义登录逻辑？

实现 `IAuthService` 接口，并在 `AuthAutoConfiguration` 中注册自定义实现。
