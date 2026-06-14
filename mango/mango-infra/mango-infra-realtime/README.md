# Mango Infra Realtime

## 1. 能力定位

`mango-infra-realtime` 提供服务端实时消息基础设施，覆盖 WebSocket、SSE、HTTP Polling、协议协商、在线会话、订阅、上行分发、下行投递和跨实例转发。

## 2. 适用场景

- 浏览器或客户端需要实时接收业务消息。
- 需要按 USER、CLIENT、CONNECTION、GROUP、TENANT、BROADCAST 投递消息。
- 需要 SSE/WebSocket/Polling 自动协商或降级。
- 需要服务端发布、客户端上行和远程 receiver 注册。

## 3. 不适用场景

- 不理解业务消息语义。
- 不替代业务订阅者的权限校验和幂等处理。
- 不替代完整消息队列或离线消息系统。
- 不保存独立业务消息库，可靠 outbox 依赖 `mango-infra-kv`。

## 4. 模块边界

`api` 定义消息和投递契约，`support/core` 提供会话、presence、polling、outbound/inbound 服务，`starter` 暴露本地传输接口，`starter-remote` 提供远程 Feign 调用。

## 5. 接入方式

```xml
<dependency>
    <groupId>io.mango.infra.realtime</groupId>
    <artifactId>mango-infra-realtime-starter</artifactId>
</dependency>
```

远程调用接入 `mango-infra-realtime-starter-remote`，只使用契约时依赖 `mango-infra-realtime-api`。

## 6. 配置项

配置前缀：`mango.infra.realtime`。

已发现配置分组包括 `enabled`、`mode`、`sse`、`websocket`、`polling`、`negotiate`、`remote`、`node`、`outbound`、`presence`、`outbox`、`inbound`。

presence 能力依赖支持 `IKvSortedSet` 的 infra-kv store；缺失时相关服务会启动失败。远程服务名是 module-name 路由语义，业务不应硬编码真实部署服务名和 contextPath。

## 7. 对外接口 / 扩展点

- API：`RealtimeApi`、`RealtimeInboundApi`、`RealtimeOutboundApi`、`RealtimeInboundReceiverApi`。
- 注解：`@RealtimeInboundMessageListener`。
- Controller：`/realtime/messages/publish`、`/realtime/receivers/register`、`/realtime/receivers/unregister`、`/realtime/transports/websocket`、`/realtime/transports/sse`、`/realtime/transports/polling`、`/realtime/transports/probe/sse`、`/realtime/transports/probe/polling`、`/realtime/transports/negotiate`、`/realtime/messages/inbound/sse`、`/realtime/messages/inbound/polling`、`/_realtime/messages/outbound`、`/_realtime/messages/inbound`。
- Feign：`RealtimeFeignClient`、`RealtimeInboundReceiverFeignClient`，服务名 `mango-infra-realtime`。

## 8. 数据库 / 初始化数据

未发现独立 SQL migration。可靠投递和 outbox 依赖 `mango-infra-kv`。

## 9. 菜单 / 权限 / 租户

本模块不提供管理菜单。消息 target 支持租户级投递，业务订阅者仍需基于当前用户、租户和业务规则校验消息权限。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-infra/mango-infra-realtime -am test
```

当前未发现该模块独立 `src/test` 测试类；验收应覆盖 negotiate、SSE、Polling、publish、receiver register 和跨实例转发。

## 11. 业务接入最小闭环

服务端发布消息时通过 `RealtimeApi` 指定 target 类型和业务 payload；客户端先调用 negotiate 获取可用传输，再选择 WebSocket、SSE 或 Polling 建连。客户端上行消息通过对应 inbound endpoint 进入服务端 listener 或 receiver。

跨实例投递需要配置 node、outbound、remote 和 infra-kv/outbox。验收断言覆盖：negotiate 返回可用协议，指定 USER/TENANT/GROUP target 能收到消息，断线后 presence 清理，outbox 失败重试可恢复，业务 listener 对消息权限和租户做二次校验。

## 12. 常见问题

- 客户端连接失败先检查协议协商接口和跨域配置。
- 上行消息未处理时检查 `@RealtimeInboundMessageListener` 注册和 receiver 路由。
- 可靠投递失败时检查 `mango.infra.realtime.outbox` 与 `mango-infra-kv` 配置。

## 13. 关联 PMO 规则

- [后端 API 规范](../../../mango-pmo/rules/backend/03-api.md)
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
