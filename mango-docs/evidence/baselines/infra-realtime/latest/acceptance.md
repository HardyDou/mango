# Infra Realtime 历史债务治理验收证据

## 1. 验收范围

- 模块：`mango-infra-realtime-api/support/core/starter/starter-remote`。
- 当前源码消费者：`mango-notice-channel-site/core/starter`、`mango-admin-starter`、
  `mango-platform-app`。
- 产品边界：WebSocket、SSE、Polling、Redis presence、跨实例下行、跨服务上行、
  Spring 自动配置和远程注册生命周期。
- 数据边界：本模块没有数据库、Flyway、初始化数据、演示数据、菜单或浏览器页面；
  Redis 用例使用独立测试 key 并关闭客户端。

## 2. 治理前基线

| 项目 | 结果 |
|---|---|
| 模块自有测试 | 3/3 通过，但只覆盖访问模式和 WebSocket 资源注册 |
| 真实入口/集成测试 | 28/28 断言通过；测试完成后 JVM 等待约 30 秒并由 Surefire 强制终止 |
| 身份边界 | 客户端 Header/Query 可覆盖认证过滤器写入的 request attribute |
| Probe 鉴权 | 携带任意非空 `rtTicket` 可绕过认证，WebSocket/Polling 未验证 ticket |
| 上行目标 | 客户端可自动向 TENANT/BROADCAST 或非本人目标转发 |
| 会话与 presence | 默认租户查询不一致；同 ID 替换会残留旧 group/presence 索引 |
| Polling | subscriber ID 不含租户；重复注册保留旧租户、用户、客户端和群组索引 |
| 静态债务 | Checkstyle：API 38、Support 3、Core 49、Starter 73、Remote 0；SpotBugs：2、1、12、53、11 |

## 3. 修复结果与兼容边界

| 债务类型 | 修复结果 | 兼容边界 |
|---|---|---|
| 可信身份 | 认证 request attribute 优先，既有 Header/Query 仅作兼容回退 | HTTP 路径、参数名和合法登录流不变 |
| Probe ticket | WebSocket 使用独立握手拦截器；SSE/Polling 校验已签发 ticket | 普通 WebSocket/SSE/Polling 入口不改 |
| 目标授权 | 仅允许本人 USER/CLIENT/CONNECTION 和已加入 GROUP；拒绝客户端自动 TENANT/BROADCAST | 服务端 `RealtimeApi` 主动发布能力不变 |
| 会话索引 | tenant 统一 trim/default；替换连接先完整清理旧 group 和 presence | 会话 ID、查询和发布接口不变 |
| Polling 隔离 | subscriber ID 增加租户维度；重复注册清理旧身份和群组索引 | 保留原单参数 subscriber helper 以兼容源码调用 |
| DTO 与配置 | payload/profile/list/map 使用防御性复制；配置 Bean 保持可绑定 getter/setter | JSON 字段、配置键和默认值不变 |
| Listener | 扫描类型时不提前实例化 Bean；注解收敛为实际支持的 METHOD 目标 | 仓内无 TYPE 级消费者；README 示例修正为 `types` |
| 生命周期 | 远程注销失败记录告警但不阻断关闭；测试显式关闭 WebSocket/Redis/Spring 资源 | 运行时注册、注销协议不变 |
| 静态债务 | SpotBugs 全部降为 0；Checkstyle 只保留 SSE 原有魔术数和三元表达式 2 条基线告警 | 未通过改变 SSE/HTTP 返回协议迎合通用 REST 规则 |

## 4. 自动化用例

| 用例 ID | 优先级 | 层级 | 稳定契约 | 执行入口 | 状态 |
|---|---|---|---|---|---|
| TC-RT-001 | P0 | 单元 | 认证身份优先级、非法身份回退 | `RealtimeRequestIdentityResolverTest` | AUTOMATED |
| TC-RT-002 | P0 | 单元/API | WebSocket、SSE、Polling probe ticket 拒绝绕过 | `RealtimeProbeWebSocketHandshakeInterceptorTest`、`RealtimeControllerSecurityTest` | AUTOMATED |
| TC-RT-003 | P0 | 单元 | 六类 target 授权和拒绝边界 | `DefaultRealtimeInboundTargetAuthorizerTest`、`ProtocolRealtimeInboundForwarderTest` | AUTOMATED |
| TC-RT-004 | P0 | 单元 | 默认租户、连接替换、旧 group/presence 清理 | `InMemoryRealtimeSubscriptionManagerTest` | AUTOMATED |
| TC-RT-005 | P0 | 单元 | Polling 跨租户隔离、重复注册和长轮询身份保持 | `InMemoryRealtimePollingServiceTest` | AUTOMATED |
| TC-RT-006 | P1 | 单元 | 消息和 ticket profile 防御性复制、注解目标 | `RealtimeMessageImmutabilityTest`、`RealtimeInboundMessageListenerTest` | AUTOMATED |
| TC-RT-007 | P1 | 组件 | Listener 延迟实例化、Polling-only 自动配置、远程注销失败关闭 | 模块 Support/Starter/Remote 测试 | AUTOMATED |
| TC-RT-008 | P0 | 集成 | WebSocket/SSE/Polling、Redis presence、并发和自动配置 | `mango-infra-test` Realtime IntegrationTest | AUTOMATED |
| TC-RT-009 | P0 | 入口流程 | 多实例下行和跨服务上行，标签 `flow` + `realtime` | `MangoRealtimeOutboundMultiInstanceFlowTest`、`MangoRealtimeInboundMultiServiceFlowTest` | AUTOMATED |

## 5. 验证结果

| 层级 | 命令/入口 | 结果 | 结论 |
|---|---|---|---|
| 治理前模块基线 | Realtime 五子模块定向 `test` | 3/3 | INSUFFICIENT |
| 治理前真实链路 | 原 8 类 Realtime 集成/入口套件 | 28/28，但 JVM 被延迟强制终止 | DEFECT CONFIRMED |
| 治理后模块回归 | Realtime 五子模块同 Reactor `test` | 自有测试 22/22，fail/error/skip 0 | PASS |
| 单用例与标签 | `MangoRealtimeInboundMultiServiceFlowTest` + `groups=flow,realtime` | 1/1 | PASS |
| 真实集成/入口流程 | 8 类 Realtime 定向套件 | 28/28；JVM 自然退出；约 30.4 秒 | PASS |
| 当前源码消费者 | Realtime 五模块与 Notice、Admin Starter、Platform App 同 Reactor 编译 | PASS | PASS |
| 正式架构 | 直接模块 + 消费者 + `mango-architecture-verification` changed partial reactor | dependency/ArchUnit/PMD 7，blocking=0 | PASS |
| 静态质量 | 当前 Realtime 模块 Checkstyle、SpotBugs | Checkstyle 2 条原有 SSE 基线；SpotBugs 0；新增 0 | PASS |
| 测试质量 | `test-quality-check`、Mockito changed-only audit | 17 个测试资产 PASS；block=0、warn=0 | PASS |

## 6. Issue #522 防回归

消费者验证没有读取本地缓存中的旧 Realtime JAR，而是把当前 Realtime 生产者和
Notice、Admin Starter、Platform App 消费者放入同一个 Maven reactor 编译。架构门禁也
以 `origin/main` 为 base，只阻断本次变更，并且没有用 partial 报告改写全仓债务预算。

## 7. 能力说明与数据边界

| 项目 | 结论 |
|---|---|
| 模块 README | 已修正 Listener 注解参数示例；依赖、配置键、HTTP 路径和接入方式不变 |
| 能力地图 | 未新增或删除能力，索引入口不变，不更新 |
| PMO 规则/index | 未改变长期规则，不更新 |
| 数据库/Flyway/init/demo | N/A |
| 浏览器 UI | N/A；该公共能力的真实产品边界是 Java/HTTP/WebSocket/SSE/Redis/跨服务入口 |
| 全仓测试 | 未执行；只验证直接模块、真实入口和当前消费者 |

## 8. 风险分级

- 需求影响：L3。问题涉及跨租户身份、跨实例/跨服务消息投递、并发状态和公共实时能力。
- 方案风险：L3。实现跨 API、Support、Core、Starter、Remote 和真实消费者，但不改变既有
  HTTP 路径、JSON 字段、配置键和合法协议行为，可按单 PR 回退且无数据迁移。
- 最终风险：L3。由旧实现红灯、同一模块回归、Redis/随机端口真实入口、当前源码消费者、
  架构门禁、测试质量和 Mock 审计共同覆盖。
