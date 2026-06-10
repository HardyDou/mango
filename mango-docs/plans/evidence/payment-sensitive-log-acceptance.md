# 支付敏感日志验收证据

## 1. 验收范围

- 台账项：`PAY-SEC-002`
- 设计来源：统一支付系统设计说明书 14、15
- 后端模块：`mango-payment-api`、`mango-payment-core`、`mango-payment-starter`
- 检查对象：支付模块 main Java 源码中的日志调用、控制台输出和堆栈直接打印

## 2. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| PAY-SEC-002 | `PaymentSensitiveLogContractTest.paymentLogs_shouldNotPrintSensitiveFields` | 日志不得打印完整敏感字段 | 扫描 `appSecret`、证书、私钥、API Key、银行账号、证件号、签约配置值、通知 payload、请求/响应 payload、`extendInfo` 等敏感字段 token | 支付模块 main Java 源码中，`log` / `LOGGER` 日志调用行不得包含敏感字段 token；当前唯一调度日志只输出通知投递数量和异常摘要，不输出 payload 或密钥 | 后端安全契约测试，不涉及页面布局 | Maven Surefire 执行通过；无敏感日志命中 | `PaymentSensitiveLogContractTest.paymentLogs_shouldNotPrintSensitiveFields` | DONE |
| PAY-SEC-002 | `PaymentSensitiveLogContractTest.paymentSource_shouldNotUseConsoleOutputOrStackTracePrinting` | 禁止控制台输出和直接堆栈打印 | 扫描 `System.out`、`System.err`、`printStackTrace` | 支付模块 main Java 源码中不存在控制台输出和直接堆栈打印，避免绕过日志脱敏策略 | 后端安全契约测试，不涉及页面布局 | Maven Surefire 执行通过；无违规输出命中 | `PaymentSensitiveLogContractTest.paymentSource_shouldNotUseConsoleOutputOrStackTracePrinting` | DONE |

## 3. 验证命令

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentSensitiveLogContractTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
node mango-pmo/tools/acceptance-evidence-check.mjs --evidence mango-docs/plans/evidence/payment-sensitive-log-acceptance.md
```

## 4. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 运行时日志采集平台脱敏策略 | 本轮覆盖支付模块源码日志契约，不接入日志采集平台或生产日志样本 | 不影响支付代码层面“不打印完整敏感信息”的验收；不能替代 `PAY-OBS-001` 可观测性建设 | 日志采集、链路追踪、指标和告警继续按 `PAY-OBS-001` 推进 | 不适用 |
| 数据库 payload 字段内容 | 本轮扫描日志输出，不改变通知 payload、查单 request/response payload、业务 `extendInfo` 的数据库持久化模型 | 不能据此声明所有数据库 payload 都已完成敏感白名单治理 | 涉及 payload 白名单或历史数据治理时按独立安全项推进 | 不适用 |
