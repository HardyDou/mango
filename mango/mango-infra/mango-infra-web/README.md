# mango-infra-web

> Mango Web 基础设施，分为轻量契约 `api` 和 Spring Web 实现 `starter`。

## 模块结构

```text
mango-infra-web/
├── mango-infra-web-api       # @Inner、IInternalPathProvider、IRequestContextProvider、RequestContextSnapshot
└── mango-infra-web-starter   # 过滤器、扫描器、Servlet 提供器、MDC、异常处理、CORS 配置
```

## 职责边界

- `api`：只放 Web 入口边界契约，不依赖 Spring Boot starter。
- `starter`：封装 Spring Web 入口横切能力，兼容 Spring Boot 原生配置。
- 不负责：登录、JWT 解析、权限判断、租户业务规则、网关路由决策。

## 依赖方式

业务 API 如需声明内部接口：

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-infra-web-api</artifactId>
</dependency>
```

暴露 HTTP Controller 的 starter：

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-infra-web-starter</artifactId>
</dependency>
```

依赖 `mango-infra-web-starter` 后不要重复声明 `spring-boot-starter-web`。

## Spring Boot 配置兼容

`mango-infra-web` 不重新包装 Spring Boot 原生 Web 配置。以下配置继续按 Spring Boot 原生方式使用：

- `server.*`
- `spring.mvc.*`
- `spring.web.*`
- `spring.servlet.multipart.*`
- `spring.jackson.*`
- `spring.messages.*`
- `server.error.*`
- `management.endpoints.web.*`

Mango 只新增项目横切能力配置：

```yaml
mango:
  web:
    request-context:
      enabled: true
    mdc:
      enabled: true
    inner:
      enabled: true
      secret: ""
      timestamp-tolerance-seconds: 300
      nonce-ttl-seconds: 300
      path-refresh-interval-seconds: 300
    cors:
      enabled: true
      allowed-origin-patterns:
        - "*"
      allowed-methods:
        - GET
        - POST
        - PUT
        - DELETE
        - OPTIONS
      allowed-headers:
        - "*"
      allow-credentials: true
      max-age: 3600
```

内部调用相关配置只使用 `mango.web.inner.*`。

## 请求上下文

`IRequestContextProvider` 统一提供：

| 字段 | 来源 |
|------|------|
| `requestId` | `X-Mango-Request-Id`，缺省使用 Servlet request id |
| `traceId` | SkyWalking TraceContext、W3C `traceparent`、`X-Mango-Trace-Id`、`X-Mango-Request-Id` |
| `clientIp` | `X-Forwarded-For` 首个 IP，其次 `X-Real-IP`，最后 `remoteAddr` |
| `headers` | 当前 HTTP headers |
| `cookies` | 当前 HTTP cookies |
| `request` | 当前 Servlet request，历史兼容变量 |

`MangoContextWebFilter` 会把 HTTP 请求事实初始化到 `MangoContextHolder`，后续认证过滤器补齐主体字段，Feign 再统一透传 `X-Mango-*` 请求头。

## @Inner 内部接口

`@Inner` 放在 `mango-infra-web-api`：

```java
@Inner
@PostMapping("/rbac/user/sync")
R<Void> syncUser(@RequestBody SyncUserCommand command);
```

支持标注位置：

- `XxxApi` 类
- `XxxApi` 方法
- Controller 类
- Controller 方法

启动后 `InnerMappingScanner` 会扫描 Spring MVC handler，将最终路径写入内部路径 provider。

## 内部路径聚合

`InternalCallFilter` 只消费聚合后的 `IInternalPathProvider`：

- `@Inner` 扫描结果
- 模块自定义 `IInternalPathProvider`
- 数据库配置 provider

命中内部路径时才要求内部调用 Header 与签名：

- `X-Internal-Call: true`
- `X-Internal-Timestamp`
- `X-Internal-Nonce`
- `X-Internal-Signature`
- 可选 `X-Internal-Secret-Version`

## MDC / Trace

`WebMdcFilter` 在请求生命周期内写入：

- `requestId`
- `traceId`
- `clientIp`

请求结束后自动清理 MDC，避免线程复用污染。

## 禁止事项

- 禁止 `api` 依赖 `mango-infra-web-starter`。
- 禁止依赖 `mango-infra-web-starter` 的模块重复声明 `spring-boot-starter-web`。
- 禁止在 `infra-web` 实现认证、权限、租户业务规则。

## 验证命令

```bash
cd mango
mvn -q test -pl mango-infra/mango-infra-web -am
mvn -q mango:check -Drule=web-boundary
```
