# Mango Extension

## 1. 概览
`mango-extension` 承载 Mango 可选扩展能力。当前包含 `mango-ai`，提供 DeepSeek 流式对话和 AI SSE 推送示例。

扩展模块不属于核心平台必需能力。业务项目只有明确需要某个扩展时才引入对应 starter。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 业务后台需要接入可选 AI 对话能力 | Maven 依赖 / HTTP API / Java API |
| 第三方能力不适合放入核心 mango-platform | Maven 依赖 / HTTP API / Java API |
| 扩展能力需要独立版本、独立配置和独立边界 | Maven 依赖 / HTTP API / Java API |

## 3. 适用场景
- 业务后台需要接入可选 AI 对话能力。
- 第三方能力不适合放入核心 `mango-platform`。
- 扩展能力需要独立版本、独立配置和独立边界。

## 4. 边界说明
- 不放认证、授权、组织、系统、文件、工作流、支付等核心平台能力。
- 不放业务项目私有逻辑。
- 不把实验性核心能力放到 extension 来规避平台模块规范。

## 5. 模块组成
当前模块结构：

| 模块 | 职责 |
|------|------|
| `mango-ai-api` | AI DTO 和 API 契约，包含 `ChatRequest`。 |
| `mango-ai-core` | AI Controller、Service、DeepSeek provider。 |
| `mango-ai-starter` | 自动扫描 AI controller/service/provider。 |

AI 模块依赖 `mango-infra-context-starter` 的 TTL executor 装饰器，用于异步对话时保留上下文。

## 6. 接入方式
Maven 依赖：

```xml
<dependency>
    <groupId>io.mango.extension.ai</groupId>
    <artifactId>mango-ai-starter</artifactId>
</dependency>
```

配置 DeepSeek：

```yaml
mango:
  ai:
    deepseek:
      base-url: https://api.deepseek.com
      api-key: ${DEEPSEEK_API_KEY}
      model: deepseek-chat
      connect-timeout: 10000
      read-timeout: 60000
    session:
      ttl: 1800000
```

流式对话请求：

```http
POST /ai/chat
Authorization: Bearer <accessToken>
TENANT-ID: 1
Content-Type: application/json

{
  "message": "生成一份审批说明",
  "sessionId": "optional-session-id",
  "enableThinking": true
}
```

建立 AI SSE 推送连接：

```http
GET /ai/sse
Authorization: Bearer <accessToken>
```

## 7. 配置说明
| 配置 | 默认值 | 含义 |
|------|--------|------|
| `mango.ai.deepseek.base-url` | `https://api.deepseek.com` | DeepSeek API 地址。 |
| `mango.ai.deepseek.api-key` | 空字符串 | DeepSeek API key；真实环境必须配置。 |
| `mango.ai.deepseek.model` | `deepseek-chat` | 使用的模型。 |
| `mango.ai.deepseek.connect-timeout` | `10000` | 连接超时，毫秒。 |
| `mango.ai.deepseek.read-timeout` | `60000` | 读取超时，毫秒。 |
| `mango.ai.session.ttl` | `1800000` | 会话上下文 TTL，毫秒。 |

请求体 `ChatRequest`：

| 字段 | 规则 | 含义 |
|------|------|------|
| `message` | 必填，最大 2000 字符 | 用户输入。 |
| `sessionId` | 可选 | 会话 id；为空时服务端生成。 |
| `enableThinking` | 可选 | 是否启用 thinking；不传时默认启用。 |

## 8. API 与扩展
HTTP 接口：

- `POST /ai/chat`：受保护接口，返回 `text/event-stream`。
- `GET /ai/sse`：建立 AI 模块 SSE 连接。

主要类：

- `ChatRequest`
- `ChatController`
- `SseController`
- `ChatService`
- `DeepSeekProvider`
- `MangoAiAutoConfiguration`

当前 provider 是 `DeepSeekProvider`。如果要接入其他 provider，应新增 provider 和配置，并保持 `ChatService` 的调用边界清晰。

## 9. 数据与初始化
当前 AI 扩展不包含数据库 migration。会话上下文保存在内存 `ConcurrentHashMap` 中，按租户和 session 组合 key 管理，并由定时任务清理过期会话。

因此：

- 服务重启会丢失 AI 会话上下文。
- 多实例不会共享会话上下文。
- 需要生产级会话时，应新增持久化或 KV 存储设计。

## 10. 管理入口
`POST /ai/chat` 要求 `Authorization` 请求头以 `Bearer ` 开头；租户优先读取 `TENANT-ID`，为空时读取 Mango 上下文，仍为空时使用 `default`。

当前扩展不初始化菜单和权限。业务接入时应：

- 在 authorization 中登记 AI 页面和接口权限。
- 给需要使用 AI 的角色授权。
- 确认前端调用带上 token 和租户头。
- 根据数据敏感性决定是否增加审计、限流和内容安全策略。

## 11. 快速开始
1. 确认业务确实需要 AI 扩展，而不是核心平台能力。
2. 宿主 app 引入 `mango-ai-starter`。
3. 配置 DeepSeek API key、模型和超时。
4. 在权限系统登记 AI 菜单或接口权限。
5. 前端通过 SSE 方式调用 `/ai/chat`，并处理 message、thinking、done、error。
6. 验证租户隔离、会话 TTL、缺失 token、provider 异常和超时。

## 12. 问题排查
- 调用立刻返回错误事件：检查 Authorization 头是否是 Bearer 格式。
- DeepSeek 无响应：检查 API key、base URL、网络和 read timeout。
- 多实例上下文丢失：当前会话在内存中，不跨实例共享。
- 不应该把 AI 默认放进管理后台：它是可选扩展，按业务需求引入。

## 13. 相关文档
- [后端模块规范](../../mango-pmo/rules/backend/05-module.md)
- [后端安全规范](../../mango-pmo/rules/backend/06-security.md)
- [能力说明维护规范](../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史资料
- [Mango 后端根 README](../README.md)
- [Mango 能力地图](../../mango-docs/capabilities/README.md)
