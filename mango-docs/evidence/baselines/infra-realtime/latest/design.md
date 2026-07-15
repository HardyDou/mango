# Infra Realtime 历史债务治理设计

## 目标与边界

本次治理覆盖 `mango-infra-realtime-api`、`support`、`core`、`starter` 和
`starter-remote`。保持已发布 DTO、Java API、Feign API、HTTP 路径、合法协议流和配置
默认值兼容；修复越权、索引残留、资源生命周期和测试归属问题。本模块没有独立数据库、
Flyway、初始化数据、演示数据、菜单或页面。

## 已确认问题

1. 登录入口优先读取客户端可控 Header/Query，而不是认证过滤器写入的 request attribute，
   可把当前连接伪装为其他租户或用户。
2. `AuthFilter` 对携带 `rtTicket` 的 probe 路径放行；SSE 校验 ticket，但 WebSocket 和
   Polling probe 未校验，任意非空值即可绕过。
3. 客户端上行消息只要携带 target 就会自动转发；默认 listener 异常不 fail-fast，越权
   target 仍可能投递。
4. 默认租户会话写入 `default` 索引后，查询又和原始空 tenant 比较，导致不可见；同 ID
   会话替换时旧 group 索引和 presence group 会残留。
5. Realtime 自有模块只有 3 条轻量测试；28 条真实协议、并发、Redis、多实例和入口流程
   集中在 `mango-infra-test`，且执行完成后测试 JVM 需 Surefire 等待 30 秒强制结束。

## 方案

- 身份解析采用“认证 request attribute 优先，现有 Header/Query 回退”。这样阻止单体模式
  的客户端覆盖，同时兼容只通过受信网关头传递身份的部署。
- WebSocket probe 使用独立 ticket handshake interceptor；Polling probe 在 Controller
  校验 ticket。SSE 保持既有校验。正常登录入口仍由 `ApiAccess(LOGIN)` 和认证过滤器控制。
- 增加独立 target authorizer：USER 只允许当前 user，CLIENT 只允许当前 client，
  CONNECTION 只允许当前 session，GROUP 只允许当前 session 已加入的组；TENANT 与
  BROADCAST 不允许由客户端直接自动发布。业务 listener 仍可在校验后通过 `RealtimeApi`
  主动发布。
- 会话替换先完整下线旧 presence 和 group，再上线新会话；所有 tenant key 使用统一 trim
  与 default 归一化。Polling 注册保存每个 subscriber 的身份并在重复注册时移除旧索引。
- 价值测试回归各 owning module；保留真实跨模块集成/入口流程，并显式停止 WebSocket client、关闭
  Spring context 和 Redis client，测试进程必须自然退出。

## 验收

- 改前同一组基线：自有模块 3 条、Realtime 集成/E2E 28 条。
- 新增红灯覆盖：身份优先级、三种 probe ticket、六种 target 授权、默认租户、会话替换、
  Polling 重注册/资源释放。
- 改后运行相同基线和新增测试；启动真实随机端口应用，验证 WebSocket、SSE、Polling、
  Redis presence、跨实例下行和跨服务上行。
- 当前 Realtime 源码与 Notice、Admin Starter、Platform App 消费者必须在同一 Maven
  reactor 编译，避免 Issue #522 的旧 JAR 假通过。
- 定向 Checkstyle、PMD、SpotBugs、正式架构、测试质量、Mockito 审计全部通过；不运行
  全仓检查。
