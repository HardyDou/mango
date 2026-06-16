# Mango Infra Feign

## 1. 概览
`mango-infra-feign` 提供 Mango 微服务间 OpenFeign 基础设施，负责 Feign 重试器、日志级别、Mango 上下文请求头透传、Authorization token 捕获与透传、内部调用 HMAC 签名和模块目标补充。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 微服务模块通过 @FeignClient 调用其他 Mango 服务 | Maven 依赖 / starter / Java API |
| 调用链需要透传 request id、trace id、tenant id、user id 等 Mango 上下文 | Maven 依赖 / starter / Java API |
| 下游内部接口使用 @Inner 保护，需要调用方自动加内部调用签名头 | Maven 依赖 / starter / Java API |
| 入口请求的 Authorization token 需要传到下游服务 | Maven 依赖 / starter / Java API |

## 3. 适用场景
- 微服务模块通过 `@FeignClient` 调用其他 Mango 服务。
- 调用链需要透传 request id、trace id、tenant id、user id 等 Mango 上下文。
- 下游内部接口使用 `@Inner` 保护，需要调用方自动加内部调用签名头。
- 入口请求的 Authorization token 需要传到下游服务。

## 4. 边界说明
- 不定义业务 Feign Client。
- 不负责服务注册发现、网关路由、熔断降级策略和业务 fallback。
- 不替代接口契约测试；写接口重试是否安全由业务模块保证幂等。

## 5. 模块组成
本模块只提供 Feign starter、请求拦截器和 token 捕获 filter。业务模块负责定义 remote API、`@FeignClient`、fallback、超时策略和幂等策略。接收端内部接口校验由 `mango-infra-web` 完成。

## 6. 接入方式
```xml
<dependency>
    <groupId>io.mango.infra.feign</groupId>
    <artifactId>mango-infra-feign-starter</artifactId>
</dependency>
```

典型 remote client：

```java
@FeignClient(name = "mango-payment", path = "/payment")
public interface PaymentRemoteClient extends PaymentApi {
}
```

调用内部接口时，调用方配置 `mango.internal-call.secret`，接收方配置同一个 `mango.web.inner.secret`。

## 7. 配置说明
配置前缀：`mango.feign`。`connect-timeout`、`read-timeout`、`retry`、`logger-level`、`interceptor-enabled`、`module-target-enabled` 绑定到 `FeignProperties`；`enabled`、`internal-call-enabled`、`token-propagation-enabled` 是自动配置条件开关。

| 配置 | 默认值 | 含义 |
|------|--------|------|
| `enabled` | `true` | 是否启用 Feign 自动配置。 |
| `connect-timeout` | `5000` | 传入 Feign `Retryer.Default` 的 period 参数，单位毫秒；不是全局请求连接超时。 |
| `read-timeout` | `10000` | 传入 Feign `Retryer.Default` 的 maxPeriod 参数，单位毫秒；不是全局请求读取超时。 |
| `retry` | `3` | Feign retryer 最大尝试次数。 |
| `logger-level` | `BASIC` | Feign 日志级别。 |
| `interceptor-enabled` | `true` | 是否注册 `FeignRequestInterceptor`，透传 Authorization 和 Mango 上下文请求头。 |
| `module-target-enabled` | `true` | 是否注册 `ModuleTargetFeignInterceptor`，补充模块目标信息。 |
| `internal-call-enabled` | `true` | 是否注册 `InternalCallFeignInterceptor`，在配置 secret 后添加内部调用签名头。 |
| `token-propagation-enabled` | `true` | 是否注册 `FeignTokenFilter`，从入口请求捕获 Authorization。 |

内部调用签名配置使用独立前缀：

| 配置 | 默认值 | 含义 |
|------|--------|------|
| `mango.internal-call.secret` | 空 | 调用方 HMAC-SHA256 共享密钥；为空时不添加内部调用头。 |
| `mango.internal-call.secret-version` | `1` | 调用方写入 `X-Internal-Secret-Version` 的密钥版本。 |

示例：

```yaml
mango:
  feign:
    enabled: true
    retry: 2
    logger-level: BASIC
    interceptor-enabled: true
    internal-call-enabled: true
    token-propagation-enabled: true
  internal-call:
    secret: ${MANGO_INTERNAL_CALL_SECRET}
    secret-version: 1
```

需要真正配置 Feign 连接和读取超时时，还要使用 Spring Cloud OpenFeign 原生配置。

## 8. API 与扩展
- `FeignRequestInterceptor`：透传 Authorization、`X-Mango-Request-Id`、`X-Mango-Trace-Id`、`X-Mango-Tenant-Id`、`X-Mango-User-Id`、`X-Mango-Member-Id`、principal、realm、actor、party、app、client ip。
- `InternalCallFeignInterceptor`：配置 secret 后添加 `X-Internal-Call`、`X-Internal-Timestamp`、`X-Internal-Nonce`、`X-Internal-Secret-Version`、`X-Internal-Signature`。
- `FeignTokenFilter`：从入口 `Authorization` 请求头写入 `TokenContextHolder`，请求结束后清理。
- `ModuleTargetFeignInterceptor`：结合 `ModuleInfoResolver` 补充模块目标上下文。

## 9. 数据与初始化
无数据库 migration、无 Runner、无 Initializer、无初始化数据。

## 10. 管理入口
本模块不创建菜单和权限。租户和用户只通过请求头透传，接收方必须继续执行认证、授权和租户校验。内部调用签名只能证明调用来自持有共享密钥的服务，不等同于业务授权。

## 11. 快速开始
1. remote 模块定义 API 契约和 `@FeignClient`。
2. 调用方接入 `mango-infra-feign-starter`。
3. 接收方接入 `mango-infra-web-starter` 并在内部接口上标记 `@Inner`。
4. 调用方和接收方配置同一组内部调用 secret。
5. 对写接口明确幂等键或关闭不安全重试。

## 12. 问题排查
- 下游拿不到 token：检查 `token-propagation-enabled` 是否开启，入口请求是否带 Authorization。
- 下游拿不到 tenant/user：检查 `mango-infra-context` 是否已有上下文，`interceptor-enabled` 是否开启。
- 内部接口 403：检查调用方 `mango.internal-call.secret` 和接收方 `mango.web.inner.secret` 是否一致，时间戳是否过期，nonce 是否重复。
- 重试导致重复写：写接口必须有幂等约束，或在业务 Feign 配置中调整重试策略。

## 13. 相关文档
- [后端 API 规范](../../../mango-pmo/rules/backend/03-api.md)
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史资料
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
