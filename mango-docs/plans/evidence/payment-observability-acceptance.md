# 支付可观测性验收证据

## 1. 验收范围

- 台账项：`PAY-OBS-001`
- 设计来源：统一支付系统设计说明书 16
- 后端模块：`mango-payment-api`、`mango-payment-core`、`mango-payment-starter`
- 覆盖能力：摘要日志、链路追踪证据、最小监控指标、告警规则、只读权限、敏感日志约束

## 2. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| PAY-OBS-001 | `PaymentObservabilityService.currentSnapshot` | 最小监控指标 | Mapper 返回支付总数 10、成功 8、失败 2、退款总数 4、成功 3、回调失败 1、通知失败 2、对账差异 1、未处理异常 2、证书即将过期 1 | 指标来自当前租户真实 Mapper 统计；支付成功率为 `0.8000`、通道失败率为 `0.3000`、退款成功率为 `0.7500`；不使用固定成功或临时数据 | 后端服务验收，不涉及页面布局 | Maven Surefire 执行通过，`PaymentObservabilityServiceTest.currentSnapshot_calculatesMinimumMetricsFromMapperCounts` 断言聚合结果 | `PaymentObservabilityService`、`PaymentOrderMapper.xml`、`PaymentRefundOrderMapper.xml`、`PaymentNotificationRecordMapper.xml`、`PaymentExceptionOrderMapper.xml` | DONE |
| PAY-OBS-001 | `PaymentObservabilityService.currentSnapshot` | 告警规则 | 阈值配置：订单积压 2、回调失败 1、通知失败 1、退款失败 1、对账差异 1、未处理异常 1、证书即将过期 1、通道失败率 0.2000 | 生成 `PAYMENT_SUCCESS_RATE`、`REFUND_SUCCESS_RATE`、`ORDER_BACKLOG`、`CALLBACK_FAILURE`、`NOTIFICATION_FAILURE`、`REFUND_FAILURE`、`RECONCILIATION_DIFFERENCE`、`UNHANDLED_EXCEPTION`、`CERTIFICATE_EXPIRING`、`CHANNEL_FAILURE_RATE` 告警 | 后端服务验收，不涉及页面布局 | Maven Surefire 执行通过，阈值命中和无分母不误告警均有断言 | `PaymentObservabilityProperties`、`PaymentObservabilityServiceTest` | DONE |
| PAY-OBS-001 | `GET /payment/readonly-resources/observability/snapshot` | 运维只读接口和独立权限 | 反射检查接口映射和授权 migration | 接口使用 `@GetMapping("/observability/snapshot")`，权限码为 `payment:observability:query`；授权初始化脚本包含该权限 | 后台菜单不新增独立可观测性页面；本次只提供后端只读快照能力 | Maven Surefire 执行通过，`PaymentReadonlyResourceControllerTest.observabilityEndpoint_usesIndependentQueryPermission` 和 migration 静态检查通过 | `PaymentReadonlyResourceController`、`V60__payment_observability_permission.sql` | DONE |
| PAY-OBS-001 | `PaymentChannelCallbackService`、`PaymentChannelOrderQueryService`、`PaymentChannelRefundQueryService`、`PaymentNotificationService`、`PaymentReconciliationService` | 摘要日志覆盖支付、退款、回调、通知、对账 | 支付回调成功、支付主动查单成功、退款主动查单成功、业务通知 ACK 成功、芒果支付账单对账生成成功 | 摘要日志统一输出 `payment.summary event/orderNo/status/amount/channel/durationMs/result`；只在业务结果确定后记录，不改状态机、不吞异常、不输出 payload、密钥或证书 | 后端服务验收，不涉及页面布局 | Maven Surefire 执行通过，相关测试断言 `PaymentObservabilityService.logSummary` 收到真实订单号、状态、金额、通道和结果 | `PaymentChannelCallbackServiceTest`、`PaymentChannelOrderQueryServiceTest`、`PaymentChannelRefundQueryServiceTest`、`PaymentNotificationServiceTest`、`PaymentReconciliationServiceTest` | DONE |
| PAY-OBS-001 | `mango-infra-web` Trace/MDC | 链路追踪证据 | 支付链路沿用平台运行时上下文；测试上下文设置 tenant/user，生产链路通过 Web 入口 MDC/trace 传播 | 支付模块不自建 trace 框架；OpenAPI、支付订单、通道调用、回调、通知、对账均在同一后端调用上下文内输出摘要日志，日志可携带平台 traceId | 后端链路验收，不涉及页面布局 | Maven Surefire 执行通过；源码核对 `MangoContextSnapshot` 支持 `traceId`，支付服务通过 `PaymentContextSupport` 读取租户和用户上下文 | `MangoContextSnapshot`、`PaymentContextSupport`、摘要日志服务调用点 | DONE |
| PAY-OBS-001 | `PaymentSensitiveLogContractTest` | 可观测日志敏感信息约束 | 扫描 main Java 源码日志、控制台输出和直接堆栈打印 | 新增摘要日志仍通过敏感日志契约；日志不包含 appSecret、证书、私钥、API Key、银行账号、证件号、payload、`extendInfo` 等完整敏感字段 | 后端安全验收，不涉及页面布局 | Maven Surefire 执行通过，敏感日志契约无命中 | `payment-sensitive-log-acceptance.md`、`PaymentSensitiveLogContractTest` | DONE |

## 3. 验证命令

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core,mango-platform/mango-payment/mango-payment-starter -am -Dtest=PaymentObservabilityServiceTest,PaymentSensitiveLogContractTest,PaymentReadonlyResourceControllerTest,PaymentChannelCallbackServiceTest,PaymentChannelOrderQueryServiceTest,PaymentChannelRefundQueryServiceTest,PaymentNotificationServiceTest,PaymentReconciliationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
node mango-pmo/tools/acceptance-evidence-check.mjs --evidence mango-docs/plans/evidence/payment-observability-acceptance.md --min-rows 6
```

## 4. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 外部机构生产监控数据 | 通联、华夏、微信、支付宝等真实通道尚未完成联调 | 当前指标和摘要日志可覆盖通道 SPI 链路，但不能证明外部机构生产成功率和失败率 | 随 `PAY-CHANNEL-003/005/007/008` 联调补充通道侧监控样本 | 不适用 |
| 生产告警平台接入 | 本次交付支付模块告警快照和阈值规则，不接入短信、邮件或统一告警平台 | 不影响支付模块告警规则计算；不能替代企业统一告警编排 | 统一告警投递由运维平台或通知中心另行接入 | 不适用 |
| 压测观察 | 本轮执行聚焦 Maven 测试，未运行压测 | 不能给出吞吐量、P95 或容量结论 | 投产前按生产压测计划补性能观测证据 | 不适用 |
