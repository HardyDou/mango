# 2026-04-18 Phase 3 Realtime 交付记录

## 完成项

- 新建并收敛 `mango-infra-realtime` 聚合模块，当前子模块为 `api`、`support`、`core`、`starter`、`starter-remote`。
- `mango-infra-realtime-api` 只保留跨模块契约：
  - `RealtimeApi`
  - `RealtimeInboundApi`
  - `RealtimeInboundReceiverApi`
  - `RealtimeOutboundApi`
  - `RealtimeOutboundMessage`
  - `RealtimeInboundMessage`
  - `RealtimeInboundReceiverRegistration`
  - `RealtimeInboundMessageListener`
- `mango-infra-realtime-support` 承载 core 与 starter-remote 复用的运行时支撑，包括入站监听器扫描、调用器和本地入站分发服务。
- `mango-infra-realtime-core` 承载协议 adapter、本地 session、presence 抽象、入站 receiver registry、出站 publish service、本地 polling 队列；不再放 HTTP Controller。
- `mango-infra-realtime-starter` 负责本地装配和所有 HTTP 入口，Controller 已按职责收敛到 `starter.controller`。
- `mango-infra-realtime-starter-remote` 只依赖 `api`、`support` 和 Feign 装配，不依赖 `core`；远程 `RealtimeApi` 直接由 Feign client 实现。
- URI 规则已统一：
  - `/realtime/transports/**`：客户端连接、polling、negotiate。
  - `/realtime/messages/**`：正向发布和客户端入站消息。
  - `/realtime/receivers/**`：remote starter 入站监听服务注册。
  - `/_realtime/messages/**`：反向服务间调用。
- 当前默认 endpoint：
  - `GET /realtime/transports/sse`
  - `/realtime/transports/websocket`
  - `GET /realtime/transports/polling`
  - `GET /realtime/transports/negotiate`
  - `POST /realtime/messages/inbound/sse`
  - `POST /realtime/messages/inbound/polling`
  - `POST /realtime/messages/publish`
  - `POST /realtime/receivers/register`
  - `POST /realtime/receivers/unregister`
  - `POST /_realtime/messages/inbound`
  - `POST /_realtime/messages/outbound`
- WebSocket、SSE、Polling 都支持客户端入站消息，协议入口独立，底层复用统一入站转发和 listener 分发逻辑。
- `inbound.mode` 支持 `none`、`local`、`remote`、`local_remote`，默认推荐连接承载服务使用 `local_remote`。
- 支持多实例在线出站投递：
  - 本机先本地投递。
  - 再通过 presence 查找远端节点。
  - 远端节点通过 `/_realtime/messages/outbound` 接收后只做本地分发，避免递归转发。
- 支持 KV presence：
  - 存在 `IKvStore` 且实现 `IKvSortedSet` 时，starter 自动启用 `KvRealtimePresenceService`。
  - Redis KV 使用 `IKvStore` 保存 session 路由详情，使用 `IKvSortedSet` 维护 user / tenant / all 到 sessionId 的 TTL 索引。
  - starter 默认 `ObjectMapper` 改为 `JsonMapper.builder().findAndAddModules().build()`，支持 `Instant` 等 Java Time 类型。
- 旧 `mango-infra-sse`、`mango-infra-websocket` 模块已删除，不保留兼容 shim。
- README 与设计文档已同步当前 URI、部署模式、KV presence、多实例边界和测试规划。

## 不做项

- 不引入 MQ、事件总线或外部消息中间件。
- 不把业务通知、消息中心、已读未读、离线消息或审计持久化放进 realtime。
- 不共享真实连接对象；`RealtimeSession`、WebSocket session、SSE emitter、Polling 队列仍只存在本机内存。
- 不提供 ack、重试、死信、可靠投递或跨服务顺序保证。
- 不在 realtime 内实现认证、鉴权、租户规则、token 解析或网关策略。
- 不保留旧 endpoint 兼容路径。

## 被动适配

- `mango-biz-notification-core` 通过 `mango-infra-realtime-api` 的 `RealtimeApi` 完成在线投递。
- `mango-biz-notification-starter` 依赖 realtime starter，由 realtime starter 暴露协议入口。
- `mango-admin-app` 收敛 websocket 传递依赖，运行时通过 realtime starter 装配。
- `mango-infra-test` 统一承载 realtime 的协议、并发、多服务、多实例和 Redis presence 集成测试。
- `mango:check` 已补充并验证通用模块边界、API 契约、remote adapter、module.properties 和 KV key 规则；不加入 realtime 专用路径规则。

## 验证结果

- `mvn -q -pl mango-infra/mango-infra-realtime,mango-infra/mango-infra-test -am compile -DskipTests -Dcheckstyle.skip=true`：通过。
- `mvn -q -pl mango-infra/mango-infra-realtime,mango-infra/mango-infra-test -am test -Dcheckstyle.skip=true -Dtest=RealtimePresenceAutoConfigurationTest,MangoRealtimeOutboundMultiInstanceE2ETest,MangoRealtimeInboundMultiServiceE2ETest,MangoRealtimeProtocolIntegrationTest,MangoRealtimeConcurrencyIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false`：通过。
- `mvn -q -pl mango-infra/mango-infra-realtime,mango-infra/mango-infra-test -am test -Dcheckstyle.skip=true -Dtest=RedisKvRealtimePresenceIntegrationTest,RealtimePresenceAutoConfigurationTest,MangoRealtimeOutboundMultiInstanceE2ETest -Dsurefire.failIfNoSpecifiedTests=false`：通过。
- `mvn -q -pl mango-tools/mango-maven-plugin -am test -Dcheckstyle.skip=true -Dtest=CheckMojoTest -Dsurefire.failIfNoSpecifiedTests=false`：通过。
- Redis presence 真实集成测试覆盖：
  - online / find / offline 生命周期。
  - TTL 续约在连接存活期间保持 presence 不过期。
  - 双节点 publish 只向远端节点转发，不重复回送本机。

## 遗留问题

- 入站 receiver registry 当前仍是内存实现；如果需要多实例稳定支撑 remote listener 服务发现，后续应引入注册中心、KV/DB 心跳或 module 能力发现。
- `RealtimePollingService` 当前是本地内存临时队列，不承担离线消息或可靠队列。
- 认证、鉴权、可信 header、长连接网关路由、超时、限流、CORS/Origin 策略等待 Phase 4/5 冻结统一规则后被动适配。
- 生产环境如果使用服务名转发，需要应用提供支持服务发现/负载均衡的 `RestOperations`。

## 下一 Phase 前置条件

- Phase 4 需要冻结统一请求上下文 / 安全上下文契约，明确 userId、tenantId、认证状态、requestId、traceId、clientIp 的来源和读取方式。
- Phase 5 需要冻结 gateway 对 `/realtime/*` 与 `/_realtime/*` 的路由、内部接口保护、WebSocket Upgrade、SSE/Polling 超时、限流和 Origin 策略。
- 后续平台模块如需实时推送，只依赖 `mango-infra-realtime-api`，不得直接依赖 `core` 或协议实现类。
