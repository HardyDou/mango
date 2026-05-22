# 2026-05-21 Realtime PMO Hardening Plan

## 1. 目标

按 `mango-pmo` 研发流程收敛实时通信能力，确保 `/components/realtime` 页面、`RealtimeClient` 前端工具、后端 realtime envelope、点对点、群组通信和集群 presence 路由具备可验证交付状态。

## 2. 范围

- 前端：
  - `@mango/common/utils/realtime` 作为无 UI 实时通信工具能力。
  - `/components/realtime` 作为真实接口示例页，展示 Auto/WebSocket/SSE/Ajax Polling、用户上下文、群组、点对点、指定端投递和心跳。
  - 前端测试使用 v1 envelope 断言，不再以旧 `type/content/headers` 作为主协议断言。
- 后端：
  - `mango-infra-realtime` 使用统一 envelope 模型。
  - 目标投递支持 `USER`、`CLIENT`、`CONNECTION`、`GROUP`、`TENANT`、`BROADCAST`。
  - KV presence 支持集群路由，群组索引随本地在线 session 刷新并在离线时清理。
- 验证：
  - 后端编译和相关测试。
  - 前端单测、构建。
  - 真实浏览器 E2E 覆盖 WebSocket、SSE、Polling。

## 3. 不在本轮

- 不实现离线消息、消息中心、已读未读、审计级消息持久化。
- 不把 Polling 扩展成可靠消息队列。
- 不新增业务通知域能力，业务通知仍归属 `mango-biz-notification`。
- 不在 `mango-docs` 新增长期规范；长期规则如需沉淀，单独进入 `mango-pmo`。

## 4. 验收标准

- `/components/realtime` 可访问，页面基于真实接口联调，不依赖 mock 才能完成主流程。
- Auto 模式先协商再连接；指定 WebSocket/SSE/Polling 时只连接指定协议。
- 客户端上行后服务端返回 `message.accepted`，页面聊天区能直接看到用户消息和服务端回执。
- 服务端模拟投递支持群组、点对点用户、指定客户端。
- 心跳作为系统控制消息处理，不进入业务订阅回调。
- KV presence 的群组索引能刷新 TTL，离线后能清理。
- `pnpm -F mango-admin build`、相关前端测试、后端测试、真实 E2E 通过。

## 5. 风险与处理

- 浏览器 WebSocket/EventSource 不能设置任意 header：认证与普通 HTTP 区分处理，连接身份使用业务上下文和必要 query，不能把全部 header 镜像到 query。
- 集群部署依赖 KV TTL：本轮增加群组索引刷新和离线清理测试，避免长连接在线但群组索引过期。
- 旧协议兼容仍有辅助方法：测试以 v1 envelope 为主，避免继续强化旧字段。
