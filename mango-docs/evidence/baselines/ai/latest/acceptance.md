# Mango AI 历史债务验收证据

## 1. 范围与结论

- 验收日期：2026-07-17
- 模块：`mango-extension/mango-ai`
- 基准：commit `3c37c392c5024b5cfb4bd579fc541fc6d08e8b9c`
- 分支：`refactor/ai-debt`
- 结论：AI 的 API、Core、Starter 分层和自动装配已收敛；`/ai/chat`、`/ai/sse`、请求字段、事件类型、默认 thinking、租户会话隔离和历史租户头保持兼容。空消息返回 HTTP 400、SSE 只输出一层 `data:`、DeepSeek `[DONE]` 不再触发 Reactor 空值异常，属于既有缺陷修复。

## 2. 改前基线

| 项目 | 结果 |
|---|---|
| AI API/Core/Starter 测试 | 三个模块均为 0 条测试，定向 Maven test 虽通过，但不能证明 HTTP、SSE、provider 或会话行为 |
| 架构门禁 | API 2 项阻断、Core 25 项阻断、Starter 0 项；Controller、Servlet/SSE 传输和实现类混在 Core |
| 新增特征测试 | 4 条 HTTP 合同中 3 条通过；空消息预期 400、实际 200，证明只有校验注解而没有校验运行时 |
| SSE 契约 | provider 和 Controller 都拼接 `data:`，实际线格式出现 `data:data:`，前端无法按标准 JSON event 解析 |
| Starter 消费 | Starter 没有完整携带 Web、上下文和 Bean Validation 运行时依赖，README 中的单依赖接入方式不可独立成立 |

## 3. 修复范围

1. 保留已发布 `io.mango.ai.api.dto.ChatRequest` 全限定名，增加字段约束和 `AiCode`，不做破坏性包迁移。
2. Core 仅保留 `IChatService`、`IAiPushService`、`IAiProvider` 及实现；Controller 和 SSE 线协议归位 Starter。
3. Starter 补齐 Web、Context、Validation 运行时依赖和 Boot AutoConfiguration；默认 `IAiProvider` 可由业务 Bean 覆盖。
4. provider 只产生 JSON 事件，HTTP 适配器统一添加一次 SSE `data:` 前缀；心跳保持 SSE comment。
5. 会话继续按租户与 session 组合键隔离、最多保留 20 条历史、默认 30 分钟 TTL；每次对话入口清理过期会话。
6. DeepSeek 流解析改为 Reactor `handle`，`[DONE]` 被过滤且不从 `map` 返回 `null`。

## 4. 自动化验收

| 层级 | 范围 | 结果 |
|---|---|---|
| Core 单元测试 | `ChatServiceTest`、`AiPushServiceTest` | 6/6：历史保留、跨租户隔离、Java 入口校验、注入阻断、provider 异常稳定映射、通知与告警推送 |
| Provider 接口测试 | `DeepSeekProviderTest` | 2/2：使用本地真实 HTTP server 返回 DeepSeek SSE，验证 thinking/message/`[DONE]` 和畸形响应 |
| HTTP 合同测试 | `AiHttpContractFlowTest` | 5/5：缺失认证、空消息 400、注入阻断、显式 null 保持默认 thinking、正常 thinking/message/done/sessionId |
| 服务入口测试 | `AiRuntimeFlowTest` | 2/2：随机端口启动真实 Tomcat，经 TCP 验证 `/ai/chat` 三类事件和 `/ai/sse` 连接事件，均无嵌套 `data:` |
| 测试总数 | AI Core + Starter | 15/15 通过；测试替身只替代外部 AI provider 或外部 HTTP 服务，不 mock 被测 Service/Controller |
| 架构 | API/Core/Starter 同 Reactor、full mode | 三模块 dependency、ArchUnit、PMD7、blocking 均为 0 |
| 架构规则回归 | `mango-architecture-rules` | 163/163 通过；新增 `SseEmitter` 原生异步适配正例，原有 JSON Controller 反例继续生效 |
| 静态质量 | Checkstyle / SpotBugs | 三模块 Checkstyle 0；SpotBugs BugInstance 0、Error 0 |
| 测试质量 | `test-quality-check`、mock audit | 5 个 AI 变更测试文件通过；mock audit block=0、warn=0 |

## 5. 真实入口与环境边界

- 服务入口测试启动真实嵌入式 Tomcat 并使用随机端口，端口只在测试期间存在，不提供伪造的长期验收地址。
- 架构验证在 APFS clone 的临时隔离 Maven 仓库中安装本分支规则和插件，再扫描 AI 三模块；没有覆盖共享 `~/.m2` 中的 SNAPSHOT。
- 模块没有数据库、Flyway、初始化资源、菜单或 demo 数据，因此 Fresh DB 不适用。
- 仓内没有 AI 模块自有的正式管理页面；公共 Chat demo 组件属于独立前端资产，本次后端修复未改变该资产，因此浏览器 UI E2E 不适用。
- 验收未调用真实 DeepSeek 生产服务，因为仓库不保存真实密钥；provider 边界使用真实本地 HTTP SSE 服务验证。该替身只替代外部系统，不替代被测 provider。
- 当前会话存储为进程内内存，多实例不共享。这是既有能力边界，本次未扩展为分布式会话。

## 6. 前后兼容对照

| 契约/逻辑 | 修复前 | 修复后 |
|---|---|---|
| HTTP 路径 | `POST /ai/chat`、`GET /ai/sse` | 不变 |
| 请求 JSON | `message`、`sessionId`、`enableThinking` | 不变；缺省或显式 null 的 thinking 均仍为 true |
| 租户头 | 历史 `TENANT-ID` | 继续兼容，并优先支持规范头 `X-Mango-Tenant-Id` |
| 事件类型 | `thinking`、`message`、`done`、`error` | 不变 |
| SSE 线格式 | 重复 `data:`，不是标准单层事件 | 修复为单层 `data:`；事件 JSON 不变 |
| 空消息 | 注解未执行，HTTP 200 | 修复为 HTTP 400，与声明的请求约束一致 |
| 会话隔离与上限 | tenant + session；最多 20 条 | 不变 |
| provider 异常 | 尝试发送 error，链路不稳定 | 稳定输出 `error` 事件，不泄露上游异常 |

## 7. 本次沉淀的经验

1. 添加 Bean Validation 注解不等于运行时生效；Starter 必须携带 validation provider，并用真实 HTTP 参数绑定测试证明。
2. SSE 分层只能有一处负责线协议；provider/service 返回业务 JSON，HTTP adapter 负责 `data:` 与 comment framing。
3. Reactor `map` 禁止返回 `null`；结束帧和空事件应通过 `handle`、`filter` 或 `flatMap` 显式过滤。
4. Starter README 宣称“一条依赖即可接入”时，必须用真实 Boot 上下文和随机端口入口验证传递依赖及自动配置完整性。
5. 外部 AI 无密钥时不能虚构生产 E2E；应以真实本地 HTTP 服务验证线协议，同时保留“未调用生产 provider”的验收边界。
6. 重构包分层时优先保留已发布的 Java FQN、HTTP 路径和 JSON 字段；内部实现可以归位，公开契约不能顺手改名。
7. 无限 SSE 不能包装为阻塞 `Resource` 只求过门禁；真实客户端必须能及时收到首帧、主动断开，并让服务测试进程正常退出。

## 8. 结论

AI 模块从“零测试但构建为绿”提升为 15 条覆盖 Service、进程内推送、HTTP、真实 Tomcat 和外部 SSE 协议的定向基线。修复后目标三模块架构阻断项和静态问题为 0；缺陷修复与公开契约兼容边界均有自动化证据。
