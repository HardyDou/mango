# Mango Auth

> 认证模块，负责登录、发放 token，并把认证结果接入 Spring Security。

## 模块职责

| 职责 | 说明 |
|------|------|
| 用户认证 | 用户名密码登录、Token发放 |
| Token管理 | Token生成、刷新、失效 |
| 认证接入 | JWT 解析后构建 `Authentication`，写入 `SecurityContextHolder` |

## 子模块

```
mango-auth/
├── mango-auth-api/          # API 定义（LoginRequest/Response, AuthApi）
├── mango-auth-core/         # 核心业务（IAuthService, AuthServiceImpl）
├── mango-auth-starter/      # 本地调用启动器（认证控制器 + JWT 过滤链）
└── mango-auth-starter-remote/ # 远程调用启动器（Feign Client）
```

## 依赖关系

```
mango-auth-starter
├── mango-auth-core (认证业务逻辑)
├── mango-infra-security-starter (Spring Security + ITokenService)
├── mango-identity-api (认证用户事实)
├── mango-authorization-starter (授权能力接入)
└── spring-boot-starter-web
```

`mango-auth` 不保存账号资料，也不计算授权。账号资料由 `mango-identity` 提供，角色/权限由 `mango-authorization` 提供。`mango-auth` 不再维护独立的 servlet request attribute 认证链，当前基线统一走 Spring Security。

## 核心类

### AuthApi

认证服务接口：
```java
public interface AuthApi {
    LoginResponse login(LoginRequest request);
    void logout(String token);
    LoginResponse refreshToken(String refreshToken);
    boolean validateToken(String token);
}
```

### IAuthService

认证业务接口：
```java
public interface IAuthService {
    LoginResponse login(String username, String password);
    void logout(String token);
    LoginResponse refreshToken(String refreshToken);
    boolean validateToken(String token);
}
```

### LoginRequest / LoginResponse

登录请求和响应：
```java
// 请求
- username: 用户名
- password: 密码（可选，用于密码模式）
- captchaToken: 验证码Token（可选，用于验证码模式）
- loginType: 登录类型

// 响应
- accessToken: 访问令牌
- refreshToken: 刷新令牌
- expiresIn: 过期时间
- tokenType: Bearer
- userId: 用户ID
- username: 用户名
```

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
│  │ MangoGatewayStarter                              │  │
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
│  │  - 验证通过后，传递 X-User-Id, X-Tenant-Id 头   │   │
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
                .requestMatchers(AuthConstant.WHITE_LIST).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(new AuthTokenAuthenticationFilter(tokenService),
                UsernamePasswordAuthenticationFilter.class);
    }
}
```

### AuthTokenAuthenticationFilter 逻辑

1. 检查请求路径是否在白名单
2. 获取 `Authorization` 请求头
3. 提取并验证 `Bearer Token`
4. 验证成功后构建 `SecurityPrincipal`
5. 将 `Authentication` 写入 `SecurityContextHolder`
6. 未认证请求由 Spring Security 返回 401

## 使用方式

### 登录示例

```bash
# 用户名密码登录
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 响应
{
  "code": 200,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 7200,
    "tokenType": "Bearer",
    "userId": 1,
    "username": "admin"
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
  - `AuthSecurityIntegrationTest`，覆盖 `Bearer token -> Spring Security context -> @Perm` 的服务内集成链路，验证 `200 / 401 / 403`。
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
