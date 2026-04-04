# mango-infra-observability

> 可观测性基础设施 - Micrometer 指标、分布式追踪

## 已实现

- **MeterRegistry 自定义** - 应用级别 tags
- **@Timed 注解支持** - 方法级性能监控
- **`@ConfigurationProperties` 模式** - `mango.observability.*` 前缀

## 依赖

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-infra-observability-starter</artifactId>
</dependency>
```

## 配置属性

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `mango.observability.enabled` | `true` | 是否启用 |
| `mango.observability.metrics.enabled` | `true` | 指标采集 |
| `mango.observability.metrics.exportToOtlp` | `false` | 导出到 OTLP |
| `mango.observability.metrics.otlpEndpoint` | `http://localhost:4317` | OTLP 端点 |
| `mango.observability.tracing.enabled` | `true` | 分布式追踪 |
| `mango.observability.tracing.samplingProbability` | `0.1` | 采样率 (0.0-1.0) |

## 使用示例

```yaml
mango:
  observability:
    enabled: true
    metrics:
      enabled: true
      exportToOtlp: true
      otlpEndpoint: http://otel-collector:4317
    tracing:
      enabled: true
      samplingProbability: 0.1
```

### 方法级耗时监控

```java
@Timed(value = "user.get", description = "获取用户耗时")
public User getUser(Long id) {
    return userService.getUser(id);
}
```

### 访问 Actuator 端点

```
GET /actuator/metrics/http.server.requests
GET /actuator/metrics/jvm.memory.used
GET /actuator/prometheus (if Prometheus exporter enabled)
```

## 待实现

| 功能 | 状态 | 说明 |
|------|------|------|
| OTLP 导出器配置 | 待开发 | 自动配置 OTLP MeterRegistry |
| Tracing ID 传播 | 待开发 | 与 Trace ID 日志集成 |
| 自定义指标 | 待开发 | 业务指标自动暴露 |
| 健康检查增强 | 待开发 | 自定义健康检查项 |

## 设计决策

- 使用 Micrometer 而非原生 Micrometer API，Micrometer 是门面模式，便于切换存储后端
- OTLP 作为主要导出协议，兼容 OpenTelemetry Collector 生态
- Brave 作为追踪桥接，支持 Zipkin 兼容
- @Timed 注解基于 AOP，对业务代码无侵入
