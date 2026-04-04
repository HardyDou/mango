# Mango Auth

> 认证授权模块 - 用户登录、Token管理

## 模块职责

| 职责 | 说明 |
|------|------|
| 用户认证 | 用户名密码登录、Token发放 |
| Token管理 | Token生成、验证、刷新、失效 |

## 子模块

```
mango-auth/
├── mango-auth-api/          # API 定义（LoginRequest/Response, AuthApi）
├── mango-auth-core/         # 核心业务（IAuthService, AuthServiceImpl）
├── mango-auth-starter/       # 本地调用启动器（含认证控制器）
└── mango-auth-starter-remote/ # 远程调用启动器（Feign Client）
```

## 依赖关系

```
mango-auth-starter
├── mango-auth-core (认证业务逻辑)
├── mango-gateway-core (JwtUtil, GatewayConstant - 共享工具)
└── spring-boot-starter-web

mango-gateway-core
├── mango-gateway-api
└── jjwt (JWT库)
```

**说明**: auth-starter 依赖 gateway-core 是因为认证功能需要使用 gateway 中定义的 JWT 工具类和常量，这是跨域共享基础设施的特例。

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
┌────────┐     ┌─────────┐     ┌──────────┐
│ Client │────▶│  Auth   │────▶│ 业务处理  │
│        │     │ Filter  │     │          │
└────────┘     └─────────┘     └──────────┘
   │              │                 │
   │ 1.带Token   │ 2.验证Token     │
   │   请求       │                 │
   │              │                 │
   │◀────────────│◀────────────────│
   │ 3.通过/401  │                 │
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

认证安全配置，提供 JWT 认证过滤器：

```java
@Configuration
public class AuthSecurityConfig {
    // JWT工具类Bean
    @Bean
    public JwtUtil jwtUtil(...) { ... }

    // 认证过滤器注册
    @Bean
    public FilterRegistrationBean<AuthFilterBean> authFilterRegistration(JwtUtil jwtUtil) {
        // 拦截所有请求，验证Token
    }
}
```

### AuthFilter 逻辑

1. 检查请求路径是否在白名单
2. 获取 `Authorization` 请求头
3. 提取并验证 `Bearer Token`
4. 验证失败返回 401
5. 验证成功将 `userId`/`username` 设置到请求属性

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
