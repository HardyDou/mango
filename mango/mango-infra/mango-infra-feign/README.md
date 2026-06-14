# Mango Infra Feign

## 1. 能力定位

`mango-infra-feign` 提供 OpenFeign RPC 基础设施，负责服务间声明式调用的重试、日志、上下文透传、内部调用标记和模块目标路由。

## 2. 适用场景

- 微服务模块通过 Feign 调用平台或业务服务。
- 需要透传 MangoContext、Authorization token 或内部调用标记。
- 需要按模块目标补充 Feign 调用上下文。

## 3. 不适用场景

- 不定义具体业务 Feign Client。
- 不负责服务注册发现、网关路由或业务鉴权。
- 不替代接口契约测试和超时重试的业务幂等设计。

## 4. 模块边界

本模块只提供 Feign starter 和拦截器。业务模块负责定义自己的 Feign Client、fallback、API 契约和调用幂等。

## 5. 接入方式

```xml
<dependency>
    <groupId>io.mango.infra.feign</groupId>
    <artifactId>mango-infra-feign-starter</artifactId>
</dependency>
```

自动配置入口为 `FeignAutoConfiguration`。

## 6. 配置项

配置前缀：`mango.feign`。

已发现字段包括 `enabled`、`connectTimeout`、`readTimeout`、`retry`、`loggerLevel`、`interceptorEnabled`、`moduleTargetEnabled`、`internal-call-enabled`、`token-propagation-enabled`。

当前代码中 `connectTimeout`、`readTimeout` 用于重试器参数，不等同于全局 Feign `Request.Options` 超时 Bean；需要全局请求超时时应结合 OpenFeign 原生配置确认。

## 7. 对外接口 / 扩展点

- `FeignRequestInterceptor`
- `ModuleTargetFeignInterceptor`
- `InternalCallFeignInterceptor`
- `FeignTokenFilter`

本模块未发现业务 Controller 或业务 Feign Client。

## 8. 数据库 / 初始化数据

未发现数据库 migration 或初始化数据。

## 9. 菜单 / 权限 / 租户

本模块不提供菜单或权限资源。租户、用户和内部调用上下文通过请求头透传，具体解释由接收方和安全模块负责。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-infra/mango-infra-feign -am test
```

代表性测试入口：`ModuleTargetFeignInterceptorTest`。

## 11. 业务接入最小闭环

业务 remote starter 中定义 `@FeignClient(name = "<module-name>", path = "/<module-path>")`，并让接口继承 api 模块契约。启用 `mango.feign.interceptor-enabled` 后会透传 Mango 上下文；启用 `mango.feign.internal-call-enabled` 后会附加内部调用签名 header；启用 `mango.feign.token-propagation-enabled` 后会捕获并透传入口 token。

验收断言覆盖：调用方 Feign 能解析目标服务，接收方可读取用户/租户上下文，内部接口只接受带有效内部调用标记的请求，重试不会破坏非幂等写接口。

## 12. 常见问题

- 调用方拿不到上下文时检查 Feign 拦截器开关和请求头是否被覆盖。
- 重试会放大非幂等接口风险，业务写接口需要明确幂等策略。
- fallback 逻辑应留在业务模块，不放入 infra feign。

## 13. 关联 PMO 规则

- [后端 API 规范](../../../mango-pmo/rules/backend/03-api.md)
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
