# 2026-04-18 Phase 3 Realtime 交付记录

## 完成项

- 新建 `mango-infra-realtime` 聚合模块，并拆分为 `api`、`core`、`starter`、`starter-remote`。
- 在 `mango-infra-realtime-api` 定义最小上层契约：
  - `RealtimeApi`
  - `RealtimePublisher` 兼容别名
  - `RealtimePollingService`
  - `RealtimeSession`
  - `RealtimeSubscriptionManager`
  - `RealtimeMessage`
- 将旧 SSE 能力迁入 `io.mango.infra.realtime.core.sse`：
  - 保留 `/realtime/subscribe` 入口。
  - 保留 Authorization header 基础校验。
  - 使用统一 `RealtimeSubscriptionManager` 管理会话。
- 将旧 WebSocket 能力迁入 `io.mango.infra.realtime.core.websocket`：
  - 保留 `/realtime/ws` 入口。
  - 保留 `token`、`tenantId`、`userId` 查询参数握手方式。
  - 使用统一 `RealtimeSubscriptionManager` 管理会话。
- 在 `mango-infra-realtime-starter` 提供条件化自动配置：
  - `mango.infra.realtime.enabled`
  - `mango.infra.realtime.mode`
  - `mango.infra.realtime.sse.*`
  - `mango.infra.realtime.websocket.*`
  - `mango.infra.realtime.polling.*`
  - `mango.infra.realtime.negotiate.*`
  - `mango.infra.realtime.remote.*`
  - `mango.infra.realtime.inbound.*`
- 在 `mango-infra-realtime-starter-remote` 提供远程 `RealtimeApi`，用于不承载前端连接的服务。
- `RealtimeApi` 已作为业务侧推荐发布入口，`RealtimePublisher` 保留为兼容别名。
- 新增 WebSocket 客户端入站消息分发：
  - API 层提供 `RealtimeInboundMessage`、`@RealtimeListener`、`RealtimeSubscriber`、`RealtimeSubscriberApi`、`RealtimeSubscriberRegistration`。
  - core 层提供本地监听器扫描/分发、订阅服务注册表、local/remote/noop inbound transport。
  - starter 层支持 `inbound.mode=none|local|remote`，并暴露 `POST /internal/realtime/subscribers/register`、`POST /internal/realtime/subscribers/unregister`。
  - starter-remote 启动后自动扫描本服务 `@RealtimeListener` / `RealtimeSubscriber`，向 realtime 服务注册，并暴露 `POST /internal/realtime/inbound` 接收入站消息。
  - 当前方案先不依赖 `mango-infra-module`；注册表为 realtime 内存实现，服务重启后由 remote starter 重新注册。
- `mango-biz-notification-core` 已改为依赖 `mango-infra-realtime-api`，通过 realtime 发布契约完成实时投递。
- `mango-biz-notification-starter` 已依赖 `mango-infra-realtime-starter`，负责拉起协议实现。
- `mango-admin-app` 已移除对 `spring-boot-starter-websocket` 的直接依赖，改为依赖 `mango-infra-realtime-starter`。
- `mango-infra-test` 已新增 realtime 测试依赖与测试用例，infra 层测试统一放在 `mango-infra-test`。
- 根 POM dependencyManagement 删除旧 `mango-infra-sse` / `mango-infra-websocket`，新增 `mango-infra-realtime-api/core/starter/starter-remote`。
- `mango-infra` 聚合 POM 删除旧模块，新增 `mango-infra-realtime`。
- 删除旧 `mango-infra-sse` 与 `mango-infra-websocket` 模块目录。

## 不做项

- 未引入外部消息中间件。
- 未把 Feign 与 SSE/WebSocket 合并为一个“通信大包”；仅新增 `starter-remote` 作为远程发布适配。
- 未迁移 `mango-biz-notification` 的业务实体、存储、已读状态或通知规则。
- 未重构 `mango-ai-core` 的原生 `SseEmitter` 流式响应；它是 AI 对话接口的流式返回例外，不依赖旧 infra SSE 模块。
- 未保留旧 `mango-infra-sse` / `mango-infra-websocket` 兼容 shim。

## 被动适配

- `mango-biz-notification-core` 从协议类注入改为 realtime 发布契约注入。
- `mango-biz-notification-starter` 删除旧 `MessageWebSocketConfig`，协议入口由 realtime starter 自动配置。
- `mango-admin-app` 将 websocket 传递依赖收敛到 infra realtime starter。
- `mango-biz-notification-core` 移除对 `mango-infra-web-starter` 的 core -> starter 依赖，改为依赖 `mango-infra-security-api` 与 `jackson-databind`。
- `mango-infra-test` 为 SSE 客户端测试补充 `spring-boot-starter-webflux`，并显式依赖 `mango-infra-realtime-api/core/starter/starter-remote`，避免测试代码只移动但依赖不可解析。
- `mango-infra-test` 的协议集成测试排除 Redis/KV/DB/Flyway 自动配置，避免 infra-test 中其它测试依赖污染 realtime 测试上下文。
- `mango-infra-realtime-starter` 增加 `@EnableWebSocket`，保证自动配置场景下 `/realtime/ws` 端点实际注册。
- `LocalRealtimeInboundDispatcher` 改为延迟扫描监听器，避免自动配置阶段实例化 WebSocket 配置链导致循环依赖。

## 验证结果

- 已执行 `cd mango && mvn -q -DskipTests compile`：
  - 未通过。
  - 阻断点在 `mango-gateway-core`，错误为 `DynamicWhiteListConfig`、`GatewayProperties`、`AuthFilter` 缺少 Spring / SLF4J 相关类型。
  - 该失败点不在 Phase 3 写入范围，本阶段未修改 gateway。
- 已执行 `cd mango && mvn -q -DskipTests -pl mango-infra/mango-infra-realtime,mango-infra/mango-infra-test -am compile`：
  - 通过。
- 已执行 `cd mango && mvn -q -pl mango-infra/mango-infra-test -am -Dtest='io.mango.infra.realtime.**.*Test' -Dsurefire.failIfNoSpecifiedTests=false test`：
  - 通过。
  - 清理旧 surefire realtime 报告后重跑，报告共 20 个 infra realtime 测试类，101 个测试用例，0 failure / 0 error / 0 skipped。
  - 覆盖 `RealtimeHeaders`、`RealtimeMessage` 默认值/headers 防御性复制、内存 polling 队列、订阅索引、发布路由、SSE controller/adapter/session、WebSocket handshake/handler/session、WebSocket 入站消息转发、starter 条件化自动配置、`auto/sse/websocket/polling` 模式、remote publisher、入站监听分发、订阅服务注册/注销、remote starter 自动注册、协议集成和并发投递。
  - `MangoRealtimeProtocolIntegrationTest` 覆盖三种实现：
    - WebSocket：使用真实 `StandardWebSocketClient` 连接 `/realtime/ws?token=test-token&tenantId=tenant-a&userId=1001`，收到 connected 帧后，通过 `RealtimeApi.publishToUser(1001L, "message", "ws-user-message")` 推送，并断言客户端实际收到 `ws-user-message`。
    - SSE：使用真实 `WebClient` 订阅 `GET /realtime/subscribe?userId=2002`，携带 `Authorization` 与 `TENANT-ID` header，收到 connected event 后，通过 `RealtimeApi.publishToUser(2002L, "message", "sse-user-message")` 推送，并断言客户端实际收到 `sse-user-message`。
    - Polling：通过真实 `RealtimePollingService` append 两条消息，断言首次 poll 返回两条且顺序正确，二次 poll 为空，验证队列 drain 语义。
    - HTTP Polling：通过 `GET /realtime/poll` 覆盖短轮询和长轮询；长轮询先 hold 住请求，再发布用户消息，断言客户端响应实际收到消息。
    - Transport 协商：通过 `GET /realtime/negotiate?prefer=polling,sse,websocket` 断言返回 `websocket`、`sse`、`polling` 能力，并按客户端偏好推荐 `polling`。
- 已执行 `cd mango && mvn -q -pl mango-platform/mango-biz-notification/mango-biz-notification-api,mango-platform/mango-biz-notification/mango-biz-notification-core -am test`：
  - 通过。
  - `NotificationServiceImplTest` 共 7 个测试用例，0 failure / 0 error / 0 skipped。
  - 覆盖消息创建、分页/详情、标记已读、批量已读、删除、通过 realtime 发布契约投递用户消息。
- 已执行 `cd mango && mvn -q -DskipTests -pl mango-infra/mango-infra-realtime,mango-infra/mango-infra-test -am checkstyle:check pmd:check`：
  - 通过。
- 已执行 `cd mango && mvn -DskipTests -pl mango-infra/mango-infra-realtime,mango-platform/mango-biz-notification io.mango:mango-maven-plugin:1.0.0-SNAPSHOT:check`：
  - 未通过。
  - 本次新增的 `mango-infra-realtime-starter` module-info 问题已修复。
  - 本次涉及的 `mango-biz-notification-core -> mango-infra-web-starter` core 依赖 starter 问题已修复。
  - 当前阻断在插件内部静态分析委托阶段：`mango-infra-module-starter` 解析 `mango-infra-module-core:1.0.0-SNAPSHOT` 失败，属于 Phase 3 外的本地 reactor/仓库解析问题。
- 已执行 `rg -n "mango-infra-sse|mango-infra-websocket|io\\.mango\\.infra\\.sse|io\\.mango\\.infra\\.websocket" mango --glob '!**/target/**'`：
  - 0 命中。

## 遗留问题

- 当前 `RealtimePollingService` 是本地内存队列，只满足本阶段 polling 抽象落地；分布式持久化队列不属于 Phase 3 范围。
- WebSocket/SSE/Polling/Negotiate 当前只做协议入口的轻量 header 读取和基础校验；认证、鉴权、租户解析、网关路由、限流、跨域与连接超时策略属于后续 `infra-web/security/context` 与 `gateway` Phase，本模块不自行实现。
- WebSocket 客户端入站分发当前只负责路由给业务 listener，不承担可靠消费、ack、重试、死信、顺序保证或跨节点订阅注册持久化。
- Phase 4/5 冻结统一请求上下文、安全上下文和 gateway 可信 header 注入规则后，`mango-infra-realtime` 再被动适配 resolver/provider，替代当前直接读取 header/query 的轻量实现。
- 全量 compile 当前被 `mango-gateway-core` 的既有依赖缺口阻断；进入下一 Phase 前应先恢复全量构建基线，或由对应 Phase 明确接手 gateway 依赖修复。
- `mango:check` 当前仍被 Phase 3 外的既有全局扫描问题阻断；本阶段新增/变更范围内已处理可归属问题。

## 下一 Phase 前置条件

- Phase 4 可在本阶段编译与旧依赖搜索通过后启动。
- Phase 4 需要冻结统一请求上下文 / 安全上下文契约，明确用户 ID、租户 ID、认证状态、请求 ID、trace ID、client IP 的来源和读取方式。
- Phase 5 需要冻结 gateway 对外部请求的认证结果治理、可信 header 注入、外部伪造 header 清洗，以及 WebSocket/SSE/Polling 长连接入口的路由、超时、限流、CORS/Origin 接入约定。
- 后续平台模块如需实时推送，应依赖 `mango-infra-realtime-api`，不要重新引入协议模块或 Spring WebSocket 直连。
