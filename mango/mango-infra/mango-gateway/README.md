# Mango Gateway

> API 网关模块 - 统一入口、认证鉴权

## 模块职责

| 职责 | 说明 |
|------|------|
| 认证鉴权 | JWT Token 验证、白名单处理、用户身份解析 |
| 统一入口 | 所有外部请求的单一入口，便于监控和治理 |

## 子模块

```
mango-gateway/
├── mango-gateway-api/       # API 定义（常量）
├── mango-gateway-core/      # 核心工具（JWT、过滤器基础）
└── mango-gateway-starter/   # Spring Boot 自动配置
```

## 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        客户端                               │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│              Mango Gateway (AuthFilter)                      │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ AuthFilter (JWT验证)                                │   │
│  │  - 验证 Authorization Header                        │   │
│  │  - 解析 JWT Token                                   │   │
│  │  - 设置 userId/username 到请求属性                   │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ WhiteList (白名单)                                  │   │
│  │  - /auth/login (登录)                              │   │
│  │  - /auth/refresh (刷新Token)                        │   │
│  │  - /captcha/** (验证码)                            │   │
│  │  - /actuator/health (健康检查)                      │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────┬───────────────────────────────────┘
                          │
          ┌───────────────┼───────────────┐
          ▼               ▼               ▼
    ┌──────────┐    ┌──────────┐    ┌──────────┐
    │ BFF      │    │ BFF      │    │ BFF      │
    │ Admin    │    │ Portal   │    │ Open     │
    └──────────┘    └──────────┘    └──────────┘
```

## 依赖关系

```
BFF (mango-admin-app)
├── mango-gateway-starter    # AuthFilter
└── mango-auth-starter       # AuthController (登录接口)

mango-gateway-starter
├── mango-gateway-core (JwtUtil, GatewayConstant, AuthFilter)
├── mango-gateway-api (GatewayConstant)
└── spring-boot-starter-web

mango-auth-starter
├── mango-auth-core (认证业务逻辑)
└── mango-gateway-core (JwtUtil, GatewayConstant - 共享工具)
```

**说明**: auth-starter 依赖 gateway-core 是因为认证功能需要使用 JWT 工具类，这是跨域共享基础设施的特例。

## 单体模式 vs 微服务模式

### 单体模式

```
┌────────────────────────────────────────────────────────┐
│           单体应用 (BFF Admin)                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │ MangoGatewayStarter                              │  │
│  │  - AuthFilter (JWT验证)                          │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │ MangoAuthStarter                                 │  │
│  │  - AuthController (/auth/login)                 │  │
│  │  - AuthSecurityConfig                           │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │ Business Starters (User, Permission, I18n...)   │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

### 微服务模式

```
┌──────────────┐     ┌──────────────────────────────────┐
│   Client     │────▶│  API Gateway (Spring Cloud)      │
└──────────────┘     └──────────────┬───────────────────┘
                                     │ HTTP
                    ┌────────────────┼────────────────┐
                    ▼                ▼                ▼
              ┌──────────┐   ┌──────────┐    ┌──────────┐
              │ BFF      │   │ BFF      │    │ Auth     │
              │ Admin    │   │ Portal   │    │ Service  │
              └──────────┘   └──────────┘    └──────────┘
```

**说明**: 微服务模式下，认证在独立的 Auth Service 中处理，BFF 通过 Feign 调用。

## 核心组件

### GatewayConstant

网关常量定义：
- `TOKEN_HEADER` - Token 请求头名称 (`Authorization`)
- `USER_ID_HEADER` - 用户ID请求头 (`X-User-Id`)
- `TENANT_ID_HEADER` - 租户ID请求头 (`X-Tenant-Id`)
- `WHITE_LIST` - 白名单路径数组

### JwtUtil

JWT 工具类：
- `generateToken(userId, username)` - 生成 Token
- `validateToken(token)` - 验证 Token
- `getUserId(token)` - 获取用户ID
- `getUsername(token)` - 获取用户名

### AuthFilter

认证过滤器：
- 拦截所有非白名单请求
- 验证 Authorization Header
- 解析 JWT Token
- 将 userId/username 设置到请求属性

## 白名单

```java
public static final String[] WHITE_LIST = {
    "/auth/login",        // 登录
    "/auth/refresh",      // 刷新Token
    "/auth/captcha",     // 验证码
    "/captcha/**",       // 验证码接口
    "/actuator/health",   // 健康检查
    "/swagger-ui/**",     // API文档
    "/v3/api-docs/**",
    "/h2-console/**"      // H2控制台（仅开发环境）
};
```

## 配置

```yaml
mango:
  gateway:
    auth-enabled: true
    jwt-secret: ${JWT_SECRET:mango-secret-key-change-in-production-must-be-at-least-256-bits-long}
    token-expire-seconds: 7200
```

## 常见问题

### Q: 微服务模式下，BFF 需要再配置认证吗？

**不需要**。认证在独立的 Auth Service 中完成，网关验证通过后会通过请求头传递用户信息：
- `X-User-Id`: 用户ID
- `X-Tenant-Id`: 租户ID

BFF 直接从请求头读取即可。

### Q: 单体模式如何禁用认证？

```yaml
mango:
  gateway:
    auth-enabled: false  # 禁用认证过滤器
```

### Q: JWT Secret 如何配置更安全？

生产环境务必：
1. 使用至少256位的密钥
2. 通过环境变量或配置中心注入
3. 定期轮换密钥
