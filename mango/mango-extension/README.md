# Mango Extension

## 1. 概览

`mango-extension` 承载可选扩展能力。当前 `mango-ai` 基于 Spring AI 1.1.8 提供 DeepSeek 流式对话和通过 Mango Realtime 发布的 AI 通知/告警。

扩展模块不属于核心平台必需能力，业务项目按需引入对应 starter。

## 2. 功能清单

- Spring AI DeepSeek 流式对话。
- Mango KV 会话历史、租户/用户隔离和 TTL。
- Mango `IRateLimiter` 限流与 Resilience4j Reactor 熔断。
- Micrometer 请求和 token 指标。
- 通过 Mango Realtime 发布 AI 通知和告警。

## 3. 模块边界

| 模块 | 职责 |
|------|------|
| `mango-ai-api` | `ChatCommand` 请求命令和 AI 业务错误码。 |
| `mango-ai-core` | 基于 Spring AI `ChatModel` 的对话服务、Mango KV 会话、限流、熔断、指标和 Realtime 推送服务。 |
| `mango-ai-starter` | HTTP/SSE Controller、Spring Boot 自动配置和 Spring AI DeepSeek starter 装配。 |

Controller 和原生 `SseEmitter` 只存在于 starter；core 不依赖 Servlet。AI 对话接口是 `POST /ai/chat`，通知使用 Mango Realtime 统一传输，不提供独立 AI SSE 入口。

## 4. 接入方式

```xml
<dependency>
    <groupId>io.mango.extension.ai</groupId>
    <artifactId>mango-ai-starter</artifactId>
</dependency>
```

在宿主应用的 Maven reactor 中引入该 starter，并按下一节配置 Spring AI 和 Mango KV；不要直接调用 core 实现。

## 5. 配置说明

最低配置：

```yaml
spring:
  ai:
    deepseek:
      api-key: ${DEEPSEEK_API_KEY}
      base-url: https://api.deepseek.com
      chat:
        options:
          model: deepseek-chat

mango:
  kv:
    store:
      type: redis
    capability:
      enabled: true
      cache: true
      rate-limiter: true
  ai:
    chat:
      session-ttl: 30m
      max-history-messages: 20
```

AI starter 启动时要求真实 Redis-backed Mango KV、`ICache`、`IRateLimiter` 和 Spring AI DeepSeek API key；不会静默切换到进程内存储或其它模型实现。

## 6. API 与扩展

请求：

```http
POST /ai/chat
Content-Type: application/json

{"message":"生成一份审批说明","sessionId":"optional-session-id","enableThinking":true}
```

调用方必须经过 Mango 授权链并拥有 `ai:chat:use` 权限。租户和用户只从 `MangoContextHolder` 获取，缺失时请求失败闭合；客户端请求头不会覆盖安全上下文。

| 字段 | 规则 |
|------|------|
| `message` | 必填，最多 2000 个字符。 |
| `sessionId` | 可选，只允许字母、数字、点、下划线和连字符，最多 128 个字符。 |
| `enableThinking` | 必填布尔值；JSON 显式 `null` 按命令默认值启用。 |

响应为标准 `text/event-stream`，每个 `data:` 块承载一个 JSON 事件：`thinking`、`message`、`done` 或 `error`。会话完成后才写入完整 user/assistant 历史；会话 key 同时包含租户、用户和 sessionId，TTL 默认 30 分钟。

## 7. 数据与初始化

AI 会话只保存在 Mango KV 的 cache capability 中，不创建 AI 专用表。key 包含环境前缀、租户、用户和 sessionId，TTL 默认 30 分钟；宿主应用必须先完成 Mango Redis KV capability 配置。

## 8. 管理入口

本阶段没有新增管理页面或菜单。HTTP 对话入口要求 Mango 授权链和 `ai:chat:use` 权限，Realtime 通知沿用宿主应用的统一连接和权限边界。

## 9. 快速开始

1. 引入 `mango-ai-starter`。
2. 配置 Spring AI DeepSeek API key、Redis-backed Mango KV、cache 和 rate-limiter capability。
3. 在 Mango 授权链为调用方授予 `ai:chat:use`。
4. 调用 `POST /ai/chat`，读取 `text/event-stream` 事件。

## 10. 运行时能力

- Spring AI `ChatModel.stream(Prompt)` 是唯一模型调用路径，使用 `spring.ai.deepseek.*` 配置。
- Mango `ICache` 保存完整会话历史，Mango `IRateLimiter` 执行调用限流。
- Resilience4j Reactor circuit breaker 在模型连续失败时快速拒绝；不会调用旧实现或其它 Provider。
- Micrometer 记录 `mango.ai.chat.requests` 和 `mango.ai.chat.tokens`；操作日志只记录租户、用户、会话、模型、结果、错误类型和 token 计数，不记录 prompt 正文或密钥。
- `IAiPushService` 将通知和告警交给 `RealtimeApi`。宿主应用需按 Realtime 模块说明接入统一连接。

## 11. 问题排查

- 启动失败并提示 KV 或限流 Bean 缺失：检查 `mango.kv.store.type=redis` 以及 capability 开关和 Redis 连接。
- 请求因上下文失败闭合：检查请求是否经过 Mango 授权链，以及 `MangoContextHolder` 是否包含租户和用户。
- 请求被限流或熔断：检查 Mango rate-limiter 配置和模型服务可用性；服务不会回退到旧 Provider。
- SSE 没有完成事件：检查模型服务响应和应用日志中的错误类型，不要在日志中记录 prompt 或密钥。

## 12. 验证

```bash
cd mango
mvn -pl :mango-ai-core -am test
mvn -pl :mango-ai-starter -am test
mvn verify
```

AI 变更测试覆盖连续对话历史、租户/用户隔离、历史裁剪、Redis-backed 能力装配契约、限流、熔断、SSE HTTP 入口、上下文失败闭合和指标记录。生产 DeepSeek 密钥不进入仓库；外部模型协议测试必须使用真实本地 HTTP/SSE 服务。

## 13. 相关文档

- [后端模块规范](../../mango-pmo/rules/backend/05-module.md)
- [后端安全规范](../../mango-pmo/rules/backend/06-security.md)
- [能力说明维护规范](../../mango-pmo/rules/08-capability-docs.md)
- [Mango 能力地图](../../mango-docs/capabilities/README.md)
