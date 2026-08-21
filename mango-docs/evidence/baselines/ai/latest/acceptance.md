# Mango AI Spring AI 第一阶段验收证据

## 1. 范围与结论

- 验收日期：2026-08-21
- 模块：`mango-extension/mango-ai`
- 基线：Mango 当前 PMO baseline，代码规范和门禁以 baseline 为唯一标准
- 结论：AI 对话已收敛为 Spring AI 1.1.8 `ChatModel` 单一路径；旧 Provider、旧配置、进程内会话和兼容 fallback 已删除。Spring Boot 保持 3.5.14。

## 2. 当前能力

| 能力 | 当前实现 |
|------|----------|
| 模型调用 | Spring AI DeepSeek starter，使用 `spring.ai.deepseek.*`；生产必须配置 API key。 |
| 流式协议 | `POST /ai/chat` 由 starter 原生 `SseEmitter` 输出标准 `text/event-stream`。 |
| 权限边界 | Controller 声明 `ai:chat:use` 的 `PERMISSION` 访问要求；服务层不绕过 Mango 授权链。 |
| 租户/用户边界 | 只读取 `MangoContextHolder` 的租户和用户；缺失时失败闭合，不接受客户端租户覆盖或默认租户。 |
| 会话 | Mango `ICache` 保存完整 user/assistant 历史；key 包含租户、用户和 sessionId；默认 TTL 30 分钟。 |
| 限流 | Mango `IRateLimiter`，每次对话消耗一个许可，拒绝时不调用模型。 |
| 容错 | Resilience4j Reactor circuit breaker；模型持续失败后快速拒绝。 |
| 可观测性 | Micrometer 请求/token 指标和不含 prompt 正文的结构化操作日志。 |
| Realtime | `IAiPushService` 将通知和告警委托给 Mango `RealtimeApi`。 |

## 3. 关键契约

- 请求字段：`message`（必填，最多 2000 字符）、`sessionId`（可选，安全字符集）、`enableThinking`（布尔值，命令默认启用）。
- SSE JSON 事件：`thinking`、`message`、`done`、`error`。
- 会话只在模型成功产生有效 assistant 内容后保存完整问答；KV 读取、序列化或写入失败返回稳定错误事件。
- 日志不记录 prompt 正文、完整模型响应、访问令牌或 API key。

## 4. 自动化验证

```bash
cd mango
mvn -pl :mango-ai-core -am \
  -Dtest=ChatServiceTest,AiPushServiceTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl :mango-ai-starter -am test
```

当前结果：

- Core 定向测试：9/9 通过，覆盖连续对话历史、租户/用户隔离、历史裁剪、限流、熔断、token 指标、缺失上下文和非法 sessionId。
- Starter 定向/入口测试：7/7 通过，覆盖 Bean Validation、旧 `/ai/sse` 入口不存在、MockMvc SSE 契约、随机端口 Tomcat TCP 入口，以及本机 Redis 的 TTL/限流真实集成。
- HTTP 合同测试上下文只提供显式 `ChatModel`、`ICache`、`IRateLimiter` 和 `MeterRegistry`；不注入旧 Provider，不使用内存 KV 作为生产能力证明。Redis 集成测试直接使用 `RedisKvStore`、`KvStoreCache` 和 `KvStoreRateLimiter`。

完整交付门禁：

```bash
mvn verify
node mango-pmo/tools/audit-module-readmes.mjs
node mango-pmo/tools/audit-readme-source-facts.mjs
```

AI 直接模块 `mvn -pl :mango-ai-starter -am verify` 已通过（core 9/9，starter 7/7，含本机 Redis 真实集成）。根目录 `mvn verify` 在既有 `io.mango.identity.core.IdentityMigrationContractTest` 失败：测试只允许 V1/V2，但仓库已有 V3/V4 migration；排除该测试后的全仓 verify 又在既有 `mango-infra-fileproc-core` `OfficeToPdfQualityIT` 处发生 300 秒 LibreOffice 转换超时。两项均与 AI 改动无关。

## 5. 旧实现清理

以下标识在 AI 生产代码、测试和 Extension README 中应为零结果：

```bash
rg -n \
  "IAiProvider|DeepSeekProvider|MangoAiProperties|mango\\.ai\\.deepseek|ChatRequest|ConcurrentHashMap|hasBearerToken|TENANT-ID|X-Mango-Tenant-Id|ConditionalOnMissingBean.*IAiProvider" \
  mango/mango-extension/mango-ai mango/mango-extension/README.md
```

旧 `/ai/sse` 连接、旧 Provider 抽象、旧 `mango.ai.deepseek.*` 配置、进程内会话和默认租户 fallback 不再保留调用或配置入口。

## 6. 外部环境边界

- 生产 DeepSeek 密钥不进入仓库；本阶段不调用生产模型，也未执行真实 DeepSeek-compatible HTTP/SSE 协议联调。当前入口测试使用 Spring AI `ChatModel` 测试替身和真实 Tomcat HTTP。
- Redis-backed Mango KV 是 starter 的运行前置条件，必须显式设置 `mango.kv.store.type=redis` 并启用真实 `ICache`/`IRateLimiter`。
- Spring Boot 4 不在本阶段范围内。当前 Spring AI 1.1.8 与 Boot 3.5.14 配套；Boot 4 应单独作为平台迁移任务评估。
