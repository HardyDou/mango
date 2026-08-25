# Mango AI Spring AI 基础能力标准交付记录

## 1. 元数据

- 任务 ID：AI-SPRING-AI-FOUNDATION-PHASE-1
- 交付模式：STANDARD
- 需求影响：L2 - 改变 AI 对话公共入口的模型调用、权限、租户、会话持久化与错误边界
- 方案风险：L2 - 涉及 API、core、starter、外部模型、KV、限流和流式响应协作
- 最终风险：L2
- 工作区决策：REUSE - `feat/mango-ai-spring-ai-foundation`

## 2. 目标与范围

- 目标：将 Mango AI 对话重写为 Spring AI 单一路径，并建立可用于生产接入的安全、会话、限流、容错和可观测基础。
- 成功条件：Spring AI DeepSeek 流式调用可用；完整用户与助手上下文写入 Mango KV；权限和租户/用户上下文失败闭合；限流和熔断生效；token 指标可观测；旧 Provider、旧配置、内存会话和 fallback 全部删除。
- 处理范围：`mango-extension/mango-ai` 的 API、core、starter、测试、模块说明、能力地图与 AI 验收基线。
- 不处理范围：Spring Boot 4 平台迁移、RAG、工具调用、知识库、AI 管理页面、模型供应商动态切换、Realtime 通知能力改造。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AC-001 | 具备 `ai:chat:use` 的登录用户调用 `POST /ai/chat` | Mango 上下文包含租户和用户，Spring AI DeepSeek 与 Mango KV 已配置 | 返回标准 SSE 消息、思考与完成事件 | 模型失败返回稳定错误事件，不进入其它模型或旧实现 | Spring AI `ChatModel` 入口替身、MockMvc 和随机端口 Tomcat 应用入口测试通过；真实 DeepSeek HTTP/SSE 协议留待后续验证 |
| AC-002 | 同一租户、用户和会话连续对话 | 首轮模型成功完成 | 第二轮模型收到用户和助手的完整历史 | KV 读取、反序列化或写入失败时本次调用失败闭合 | KV 集成测试证明完整历史、TTL 和租户/用户隔离 |
| AC-003 | 未登录、无权限或缺少租户/用户上下文的调用方 | 访问 AI 对话入口 | Mango 授权链拒绝未授权请求；服务层拒绝缺失上下文 | 不接受客户端伪造租户，不使用默认租户 | 安全契约与服务测试通过 |
| AC-004 | 同一租户用户高频调用 | 超过 Mango KV 固定窗口额度 | 后续调用被拒绝且不调用模型 | 返回限流错误事件，不降级绕过限流 | 限流测试通过 |
| AC-005 | 外部模型持续失败 | 熔断失败阈值达到 | 熔断打开并快速拒绝后续调用 | 返回模型不可用错误事件，不调用旧 Provider | 熔断状态和调用次数测试通过 |
| AC-006 | 运维监控 | 模型调用成功或失败 | 记录不含提示词正文的结构化审计日志和调用/token 指标 | 指标缺失不改变业务结果 | Micrometer 指标测试通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | AC-001 | 使用 Spring AI 1.1.8 `ChatModel` 与 DeepSeek starter；删除自定义 `IAiProvider` 和手写 HTTP 客户端 | `mango-ai-core`、`mango-ai-starter` | 回滚任务提交；不保留运行时 fallback |
| TD-002 | AC-002 | 使用 Mango `ICache` 保存 JSON 会话，业务 key 包含租户、用户和会话；只在模型成功完成后保存完整问答 | `ChatService` | 回滚任务提交；KV 数据按 TTL 自然过期 |
| TD-003 | AC-003 | Controller 声明 `@ApiAccess(PERMISSION)`；服务只读取 `MangoContextHolder` 并失败闭合 | `ChatController`、`ChatService` | 回滚任务提交 |
| TD-004 | AC-004 | 使用 Mango `IRateLimiter`，每次对话消耗一个许可 | `ChatService` | 回滚任务提交 |
| TD-005 | AC-005 | 使用 Resilience4j Reactor circuit breaker 包裹 Spring AI stream；错误统一映射为 SSE 错误事件 | `ChatService` | 回滚任务提交 |
| TD-006 | AC-006 | 使用 Micrometer 记录调用和 token；使用 SLF4J 结构化字段记录租户、用户、会话、结果，不记录提示词内容 | `ChatService` | 回滚任务提交 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| TASK-001 | TD-001 | 1 | `mango/mango-extension/mango-ai/**/pom.xml` | Spring AI 依赖可解析且 Boot 3.5 兼容 |
| TASK-002 | TD-001, TD-003 | 2 | `mango-ai-api`、`mango-ai-starter` | API 命名、SSE Controller 和授权符合当前 baseline |
| TASK-003 | TD-002, TD-004, TD-005, TD-006 | 3 | `mango-ai-core` | KV、限流、熔断、指标和完整历史成为唯一实现 |
| TASK-004 | TD-001 | 4 | 旧源码、配置和测试 | 旧标识反向搜索为零 |
| TASK-005 | AC-001 至 AC-006 | 5 | 模块测试目录 | M09-M12 对应测试通过 |
| TASK-006 | TD-001 至 TD-006 | 6 | Extension README、能力地图、AI acceptance | 对外接入和验收事实同步 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AC-001, AC-003 | M12 API 验证 | `mvn -pl :mango-ai-starter -am verify`；MockMvc 与随机端口 Tomcat 验证 `/ai/chat`、SSE、权限声明和 Mango 上下文失败闭合 | PASS，starter 7/7（含 Redis 能力集成） | `AiHttpContractFlowTest`、`AiRuntimeFlowTest`、`RedisKvCapabilityIntegrationTest` |
| AC-002, AC-004 | M11 集成测试 | `mvn -pl :mango-ai-core -am -Dtest=ChatServiceTest,AiPushServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`；验证 Mango `ICache`/`IRateLimiter` 接口、完整历史、TTL、隔离和限流 | PASS，core 9/9 | `ChatServiceTest`、`AiPushServiceTest` |
| AC-005, AC-006 | M10 单元测试 | 同上定向 core 测试；熔断状态、模型调用次数、请求/token 指标和结构化审计字段均有断言 | PASS | `ChatServiceTest` |
| AC-001 至 AC-006 | M09 静态验证 | `mvn -pl :mango-ai-starter -am verify`；`node mango-pmo/tools/audit-module-readmes.mjs`；`node mango-pmo/tools/audit-readme-source-facts.mjs`；反向搜索旧 Provider、旧配置和 fallback 标识 | AI 直接模块 PASS；根目录 `mvn verify` 被既有 `io.mango.identity.core.IdentityMigrationContractTest` 阻断（V3/V4 migration 已存在但测试只允许 V1/V2）；排除该测试的全仓 verify 又被既有 `OfficeToPdfQualityIT` 的 LibreOffice 转换超时阻断，均不属于本次改动 | Maven AI Reactor、README 审计、AI acceptance |

## 7. 例外与剩余风险

- 外部 DeepSeek-compatible HTTP/SSE 服务尚未纳入本阶段自动化测试；当前验证使用 Spring AI `ChatModel` 测试替身和真实 Tomcat HTTP 入口，不能替代供应商协议联调。
- 根目录完整 `mvn verify` 仍受既有 Identity migration 契约测试失败影响：`IdentityMigrationContractTest` 期望 V1/V2，但源码已有 V3/V4；AI 直接模块 verify 已通过。
- 排除 Identity 契约测试的全仓 verify 继续执行到 `mango-infra-fileproc-core`，但既有 `OfficeToPdfQualityIT` 的 LibreOffice 转换在 300 秒超时；该环境阻塞与 AI 改动无关。
