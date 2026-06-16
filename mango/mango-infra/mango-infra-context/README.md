# Mango Infra Context

## 1. 概览
`mango-infra-context` 是 Mango 的运行时上下文传播模块。它把一次请求中的 request id、trace id、tenant id、user id、member id、principal、realm、actor、party、app code 和 client ip 固化为 `MangoContextSnapshot`，并通过 `MangoContextHolder` 在线程内和受托管异步线程池中传递。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| Controller、Filter、Feign、Job、领域事件订阅者需要读取当前请求上下文 | Maven 依赖 / starter / Java API |
| 业务代码需要把租户、用户、trace 信息带入异步任务 | Maven 依赖 / starter / Java API |
| 业务模块需要按 Mango 统一请求头透传上下文 | Maven 依赖 / starter / Java API |


## 3. 能力边界
- 不负责认证、登录态解析、JWT 签发或 token 校验。
- 不负责权限判断、菜单授权、API 资源策略。
- 不直接实现数据库租户过滤；持久化层只可把这里的 tenant id 作为输入事实。

## 4. 模块入口
- `mango-infra-context-core`：提供 `MangoContextSnapshot`、`MangoContextHolder`、`MangoContextHeaders`。
- `mango-infra-context-starter`：提供自动配置、`TaskDecorator`、`mangoContextExecutor` 和上下文包装工具。
- 请求头读取由 `mango-infra-web` 完成；Feign 透传由 `mango-infra-feign` 完成。

## 5. 接入方式
只使用上下文 API：

```xml
<dependency>
    <groupId>io.mango.infra.context</groupId>
    <artifactId>mango-infra-context-core</artifactId>
</dependency>
```

Spring Boot 应用需要异步传播能力：

```xml
<dependency>
    <groupId>io.mango.infra.context</groupId>
    <artifactId>mango-infra-context-starter</artifactId>
</dependency>
```

典型读取方式：

```java
String tenantId = MangoContextHolder.tenantId();
Long userId = MangoContextHolder.userId();
String traceId = MangoContextHolder.traceId();
```

需要手工设置上下文时：

```java
MangoContextHolder.set(MangoContextSnapshot.empty()
        .withRequest(requestId, traceId, tenantId, appCode, clientIp)
        .withSecurity(userId, memberId, tenantId, principalName, realm, actorType, partyType, partyId, appCode));
try {
    // business logic
} finally {
    MangoContextHolder.clear();
}
```

## 6. 配置说明
配置前缀：`mango.context`。自动配置类为 `ContextPropagationAutoConfiguration`。

| 配置 | 默认值 | 含义 |
|------|--------|------|
| `enabled` | `true` | 是否启用上下文传播自动配置。关闭后不注册默认 `TaskDecorator` 和 `mangoContextExecutor`。 |
| `executor.enabled` | `true` | 是否注册名为 `mangoContextExecutor` 的平台默认异步线程池。 |
| `executor.core-pool-size` | `max(2, CPU 核数)` | 默认线程池核心线程数。 |
| `executor.max-pool-size` | `max(16, CPU 核数 * 4)` | 默认线程池最大线程数。 |
| `executor.queue-capacity` | `1024` | 默认线程池等待队列容量。 |
| `executor.keep-alive-seconds` | `60` | 空闲线程保留秒数。 |
| `executor.thread-name-prefix` | `mango-context-async-` | 默认线程名前缀。 |
| `executor.wait-for-tasks-to-complete-on-shutdown` | `true` | 应用停机时是否等待已提交任务完成。 |
| `executor.await-termination-seconds` | `30` | 停机等待任务完成的秒数。 |

示例：

```yaml
mango:
  context:
    enabled: true
    executor:
      enabled: true
      core-pool-size: 8
      max-pool-size: 32
      queue-capacity: 2000
```

## 7. API 与扩展
- `MangoContextSnapshot`：不可变上下文快照，字段包括 request、trace、tenant、user、member、principal、realm、actor、party、app 和 client ip。
- `MangoContextHolder`：基于 `TransmittableThreadLocal` 的上下文持有器，提供 `get`、`set`、`update`、`clear` 和常用字段读取方法。
- `MangoContextHeaders`：统一请求头常量，包含 `X-Mango-Request-Id`、`X-Mango-Trace-Id`、`X-Mango-Tenant-Id`、`X-Mango-User-Id`、`X-Mango-Member-Id` 等。
- `MangoContextTaskDecorator`：包装异步任务，提交时捕获上下文，执行后恢复原上下文。
- `TtlExecutorDecorator`、`MangoContextExecutors`：用于包装自定义 `Executor`，让非默认线程池也能传播上下文。

## 8. 数据与初始化
无数据库 migration、无 Runner、无 Initializer、无初始化数据。

## 9. 管理入口
本模块不创建菜单和权限。tenant id 只是运行时上下文字段，不等同于租户隔离策略；业务模块、授权模块和持久化模块必须分别做权限判断和租户过滤。

## 10. 快速开始
1. Web 应用接入 `mango-infra-web-starter`，入口请求头转为 `MangoContextSnapshot`。
2. 业务代码通过 `MangoContextHolder` 获取 tenant id、user id、trace id。
3. 有异步逻辑时使用 `mangoContextExecutor`，或用 `TtlExecutorDecorator` 包装自有线程池。
4. Feign 调用接入 `mango-infra-feign-starter`，把 `MangoContextHeaders` 对应请求头透传给下游。

## 11. 问题排查
- 异步任务拿不到上下文：先确认任务是否提交到受托管线程池；自建线程池需要包装。
- 下游服务拿不到 tenant/user：检查入口 Web filter 是否写入上下文，Feign 拦截器是否启用，请求头是否被网关清理。
- 串请求污染：所有手工 `set` 场景必须在 `finally` 中 `clear` 或恢复旧快照。

## 12. 相关文档
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 13. 补充资料
- [能力地图](../../../mango-docs/capabilities/README.md)
