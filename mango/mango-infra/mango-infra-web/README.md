# Mango Infra Web

## 1. 概览
`mango-infra-web` 提供 Mango HTTP API 基础能力，覆盖内部接口标记、内部调用签名校验、请求上下文读取、MangoContext 写入、MDC trace、CORS、全局异常处理、Jackson 长整型字符串化和日期格式化。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| Web 应用需要统一异常响应、trace id 和日志 MDC | Maven 依赖 / starter / Java API |
| Controller 需要声明内部接口，禁止外部直接访问 | Maven 依赖 / starter / Java API |
| 入口请求头需要转换成 Mango 运行时上下文 | Maven 依赖 / starter / Java API |
| 需要统一 CORS、日期格式、Long 序列化策略 | Maven 依赖 / starter / Java API |
| KV 表达式或 outbox 需要从当前 Web 请求贡献上下文 | Maven 依赖 / starter / Java API |


## 3. 能力边界
- 不负责登录、JWT 解析、token 签发。
- 不负责角色、菜单、权限码判断。
- 不负责网关路由、限流、WAF 或跨服务调用。
- 不替代业务 Controller 参数校验和业务异常建模。

## 4. 模块入口
- `mango-infra-web-api`：提供 `@Inner`、`IRequestContextProvider`、`IInternalPathProvider`、`RequestContextSnapshot` 等轻量契约。
- `mango-infra-web-starter`：提供 Web 自动配置、过滤器、内部路径扫描、异常处理、Jackson 定制和 request context provider。

认证由 auth 相关模块负责，授权由 authorization/access 负责，服务间签名头由 feign 模块生成。

## 5. 接入方式
只声明 API 契约或标记内部接口：

```xml
<dependency>
    <groupId>io.mango.infra.web</groupId>
    <artifactId>mango-infra-web-api</artifactId>
</dependency>
```

Spring Web 应用启用完整基础能力：

```xml
<dependency>
    <groupId>io.mango.infra.web</groupId>
    <artifactId>mango-infra-web-starter</artifactId>
</dependency>
```

标记内部接口：

```java
@Inner
@PostMapping("/internal/rebuild")
public R<Boolean> rebuild(@RequestBody RebuildCommand command) {
    return R.ok(service.rebuild(command));
}
```

读取请求上下文：

```java
RequestContextSnapshot snapshot = requestContextProvider.current();
String traceId = snapshot.traceId();
```

## 6. 配置说明
配置前缀：`mango.web`。属性类为 `MangoWebProperties`，自动配置类为 `WebAutoConfiguration`。

| 配置 | 默认值 | 含义 |
|------|--------|------|
| `cors.enabled` | `true` | 是否启用全局 CORS 映射。 |
| `cors.allowed-origin-patterns` | `*` | 允许的 origin pattern 列表。 |
| `cors.allowed-methods` | `GET,POST,PUT,DELETE,OPTIONS` | 允许的 HTTP 方法。 |
| `cors.allowed-headers` | `*` | 允许的请求头。 |
| `cors.allow-credentials` | `true` | 是否允许携带 credential。 |
| `cors.max-age` | `3600` | 预检请求缓存秒数。 |
| `inner.enabled` | `true` | 是否启用内部接口路径扫描和校验。 |
| `inner.secret` | 空 | 接收端内部调用 HMAC 共享密钥；生产内部接口必须配置。 |
| `inner.timestamp-tolerance-seconds` | `300` | 内部调用时间戳最大容忍秒数，只接受过去时间戳。 |
| `inner.nonce-ttl-seconds` | `300` | nonce 防重放记录 TTL。 |
| `inner.path-refresh-interval-seconds` | `300` | 内部路径刷新间隔秒数。 |
| `mdc.enabled` | `true` | 是否注册 Web MDC filter。 |
| `request-context.enabled` | `true` | 是否注册 `IRequestContextProvider`。 |
| `context.enabled` | `true` | 是否注册 `MangoContextWebFilter`，把 request context 写入 `MangoContextHolder`。 |

示例：

```yaml
mango:
  web:
    cors:
      enabled: true
      allowed-origin-patterns:
        - https://admin.example.com
    inner:
      enabled: true
      secret: ${MANGO_INTERNAL_CALL_SECRET}
      timestamp-tolerance-seconds: 300
      nonce-ttl-seconds: 300
    mdc:
      enabled: true
    request-context:
      enabled: true
    context:
      enabled: true
```

## 7. API 与扩展
- `@Inner`：标记 Controller 类或方法为内部接口。
- `IRequestContextProvider`：读取当前请求上下文，默认实现为 `ServletRequestContextProvider`。
- `IInternalPathProvider`：提供内部路径集合；默认聚合多个 provider。
- `InnerMappingScanner`：扫描 `@Inner` 对应的 Spring MVC mapping。
- `InternalCallFilter`：对内部路径校验内部调用头、HMAC 签名、时间戳和 nonce。
- `MangoContextWebFilter`：把 request context 写入 `MangoContextHolder`，请求结束后清理。
- `WebMdcFilter`：把 trace/request 信息写入 MDC。
- `GlobalExceptionHandler`：统一异常响应。
- `WebKvContextContributor`：把 Web 请求上下文贡献给 infra-kv 表达式上下文。

内部调用校验要求请求头：

- `X-Internal-Call: true`
- `X-Internal-Timestamp`
- `X-Internal-Nonce`
- `X-Internal-Secret-Version`
- `X-Internal-Signature`

签名 payload 为 `timestamp:nonce:method:path:query`，算法为 HMAC-SHA256。调用方通常由 `mango-infra-feign` 自动生成这些请求头。

## 8. 数据与初始化
本模块没有 SQL migration、Runner 或 Initializer。`InternalCallFilter` 需要 `IKvStore` 记录 nonce 防重放；如果宿主应用没有 KV store，则不会注册内部调用过滤器，内部接口保护链路不完整。

## 9. 管理入口
本模块不创建菜单和权限。`@Inner` 只限制调用来源，不授予业务权限。请求上下文可以包含 tenant id 和 user id，但权限判断、租户过滤和资源授权必须由上层模块执行。

## 10. 快速开始
1. Web 应用接入 `mango-infra-web-starter`。
2. 内部接口在 Controller 类或方法上标记 `@Inner`。
3. 应用接入 `mango-infra-kv`，保证 nonce 存储可用。
4. 接收方配置 `mango.web.inner.secret`，调用方配置 `mango.internal-call.secret`。
5. 用无签名请求、错误签名请求、合法签名请求和重复 nonce 请求分别验证内部接口保护。

## 11. 问题排查
- 内部接口没有被保护：检查是否接入 starter、是否存在 `IKvStore` Bean、`inner.enabled` 是否为 true、`@Inner` 是否标在 Spring MVC mapping 上。
- 所有内部接口都 403：检查 `inner.secret` 是否为空，调用方和接收方 secret 是否一致，服务时间是否同步。
- 下游拿不到 MangoContext：检查 `request-context.enabled` 和 `context.enabled` 是否开启。
- 前端 Long 精度问题：本模块会把 Long 序列化为字符串，前端不要再按 number 解析大 ID。

## 12. 相关文档
- [后端 API 规范](../../../mango-pmo/rules/backend/03-api.md)
- [后端安全规范](../../../mango-pmo/rules/backend/06-security.md)
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 13. 补充资料
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
