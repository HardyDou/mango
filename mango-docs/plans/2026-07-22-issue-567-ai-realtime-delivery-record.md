# 标准交付记录

任务：GitHub Issue #567 AI 通知迁移到统一 Realtime 能力。

## 1. 元数据

- 任务 ID：GitHub Issue #567
- 交付模式：STANDARD
- 需求影响：L2 - 删除 AI 独立 `/ai/sse` 公开入口，并把通知和告警迁移到共享 Realtime 协议与连接。
- 方案风险：L2 - 调整 AI core/starter 边界、自动配置顺序和测试依赖；不涉及数据库、权限或数据迁移。
- 最终风险：L2
- 工作区决策：CREATE（`/Users/hardy/Work/mango-issue-567`，`fix/issue-567-ai-realtime`）
- 启用能力：M01、M08、M09、M10、M11、M12、M15

## 2. 目标与范围

- 目标：移除 AI 模块进程内 `Sinks.Many` 和独立 `/ai/sse` 连接，让 AI 通知与告警通过 `RealtimeApi` 发布到统一实时通道。
- 成功条件：
  - `IAiPushService` 不再暴露连接流，`AiPushService` 只委托 `RealtimeApi.broadcast`。
  - `/ai/sse` 不再注册并返回 404。
  - 宿主提供本地或远程 `RealtimeApi` 后，AI starter 自动注册 `IAiPushService`。
  - 真实 Realtime starter、HTTP SSE 连接能够收到 AI 发布的 `notification` 和 `alert` Envelope。
  - `/ai/chat` 的流式 SSE 请求、校验和完成事件保持不变。
- 处理范围：`mango-ai-core`、`mango-ai-starter`、Extension README、能力清单和相关自动化测试。
- 不处理范围：Realtime 协议实现、前端 Realtime 客户端、Redis/多实例部署、AI provider 行为、制品发布。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| REQ-001 | `IAiPushService` 调用方 | 宿主存在 `RealtimeApi` | 通知以 `notification`、告警以 `alert` 通过统一 Realtime 广播 | Realtime 发布异常沿调用链返回，不回退到进程内队列 | 单元测试验证统一 Envelope 类型与内容 |
| REQ-002 | `GET /ai/sse` | 启动 AI starter | 旧独立入口不存在 | 返回 404，不建立第二条 AI SSE 连接 | HTTP 合同测试固定 404 |
| REQ-003 | Realtime SSE 客户端 | 已连接 `/realtime/transports/sse` | 收到 AI 通知和告警的 Realtime Envelope | 连接或消息超时使 E2E 失败 | 真实随机端口 HTTP/SSE E2E 通过 |
| REQ-004 | `POST /ai/chat` 客户端 | 合法或非法 AI 请求 | 继续返回既有流式事件和校验结果 | 不得因通知迁移改变对话 SSE | MockMvc 和随机端口 HTTP 回归通过 |
| REQ-005 | Spring Boot 宿主 | 引入本地或远程 Realtime starter | Realtime 自动配置完成后注册 AI 推送 Bean | 缺少 `RealtimeApi` 时不创建不可用 Bean | 自动配置顺序显式声明，完整 Spring Context E2E 启动成功 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| DEC-001 | REQ-001 | `AiPushService` 依赖最小 `mango-infra-realtime-api`，分别调用 `broadcast("notification", content)` 和 `broadcast("alert", content)` | `mango-ai-core` | 恢复旧进程内实现和 core 依赖 |
| DEC-002 | REQ-002 | 删除 `SseController`、`IAiPushService.connect()`、推送 emitter 与心跳配置；这是 #567 要求的显式旧入口移除 | AI core/starter、README | 恢复删除的 Controller、接口和配置 |
| DEC-003 | REQ-004 | 保留 `ChatController` 与 `AiSseEmitterFactory.createChat`，只移除通知专用 emitter | `mango-ai-starter` | 恢复任务前代码 |
| DEC-004 | REQ-005 | `AiPushService` 由 starter 使用 `@Bean`、`@ConditionalOnMissingBean`、`@ConditionalOnBean(RealtimeApi.class)` 注册，并显式排在本地/远程 Realtime 自动配置之后 | `MangoAiAutoConfiguration` | 删除 Bean 定义并恢复组件扫描注册 |
| DEC-005 | REQ-003 | E2E 使用实际 Realtime starter、MemoryKvStore、随机端口 Tomcat 和真实 HTTP SSE 客户端，不以 mock 替代传输链路 | starter 测试与 test-scope 依赖 | 删除 E2E 和测试依赖 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| IMP-001 | DEC-001 | 1 | `mango-ai-core` POM、接口、实现与单测 | 进程内 Sink 消失，统一 Realtime 发布通过 |
| IMP-002 | DEC-002、DEC-003 | 2 | `mango-ai-starter` Controller、emitter、HTTP/运行时测试 | 旧入口 404，聊天 SSE 回归通过 |
| IMP-003 | DEC-004 | 3 | `MangoAiAutoConfiguration` | 本地/远程 Realtime 条件装配顺序明确 |
| IMP-004 | DEC-005 | 4 | `AiRealtimeE2eTest`、starter test-scope 依赖 | 真实 HTTP/SSE 收到通知和告警 |
| IMP-005 | 全部 | 5 | Extension README、能力清单、本记录 | 接入方式、兼容边界和验证证据一致 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| REQ-001 | M10 单元测试 | `mvn -q -f mango/pom.xml -pl :mango-ai-starter -am -Dtest=AiPushServiceTest,ChatServiceTest,DeepSeekProviderTest,AiHttpContractFlowTest,AiRuntimeFlowTest,AiRealtimeE2eTest -Dsurefire.failIfNoSpecifiedTests=false test` | PASS | AI core 8 项通过；`AiPushServiceTest` 验证 notification/alert Envelope |
| REQ-002 | M12 HTTP 合同 | 同上；`AiHttpContractFlowTest.legacyAiSseEndpoint_已移除` | PASS | `GET /ai/sse` 返回 404 |
| REQ-003 | M11 真实集成 | 同上；`AiRealtimeE2eTest` 启动真实 Realtime starter 和随机端口 Tomcat，连接 `/realtime/transports/sse` 后调用 `IAiPushService` | PASS | 客户端收到 `notification` 与 `alert`，无 mock RealtimeApi |
| REQ-004 | M11/M12 回归 | 同上；`AiHttpContractFlowTest`、`AiRuntimeFlowTest` | PASS | starter 既有 7 项通过，聊天 SSE 类型、内容和会话完成事件不变 |
| REQ-005 | M09/M11 装配 | 同上；Spring Boot Context 同时加载 AI 与 Realtime 自动配置 | PASS | `IAiPushService` 注入成功，真实 SSE E2E 完成 |
| 全部 | M09 静态门禁 | `mvn -q -f mango/pom.xml -pl :mango-ai-core,:mango-ai-starter -DskipTests checkstyle:check`；`git diff --check` | PASS | 受影响模块 Checkstyle 0 violations；diff 格式通过 |
| 全部 | M08 文档事实审计 | `node mango-pmo/tools/audit-module-readmes.mjs`；`node mango-pmo/tools/audit-readme-source-facts.mjs` | PASS | Extension README 与能力清单均为 OK |

## 7. 例外与剩余风险

- 任务专用 E2E 覆盖真实单节点 Realtime HTTP/SSE，KV 使用内存实现；Redis 和多实例转发由 Realtime 模块既有测试负责，本任务未重复执行部署级联调。
- `/ai/sse` 移除是有意兼容性变化；接入方必须迁移到 Mango Realtime 统一连接。
- 不涉及数据库、租户权限、资金、安全边界或不可逆发布；未发布任何制品。
