# 支付核心单元测试验收证据

## 1. 验收范围

- 台账项：`PAY-TEST-001`
- 后端模块：`mango-payment-core`
- 覆盖要求：状态机、金额、签名、通道路由、返回码映射必须有单元测试且命令通过。

## 2. 执行环境

- 工作区：`/Users/hardy/Work/mango/.claude/worktrees/payment-platform`
- 测试工具：Maven Surefire、JUnit 5、Mockito
- 说明：本证据只关闭单元测试覆盖项，不替代集成测试、E2E、外部通道联调、长周期调度或投产验收。

## 3. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| PAY-TEST-001 | `PaymentOrderStateServiceTest`、支付/退款服务单测 | 状态机单元测试 | 业务订单 `TO_PAY/PAYING/SUCCESS/REFUNDING/PARTIAL_REFUNDED`；支付订单 `CREATED/PAYING/SUCCESS/FAILED/CLOSED/DUPLICATE_*`；退款订单 `CREATED/REFUNDING/PROCESSING/SUCCESS/FAILED` | 状态服务只允许设计内迁移，拒绝终态回滚；收银台支付、主动查单、关单、主动查退款、标准化回调和 OpenAPI 退款相关单测覆盖真实状态推进和副作用边界 | 后端单元测试，不涉及页面布局 | Maven Surefire 执行 Java 单测，结果 `68` 个测试通过，失败 `0`，错误 `0` | `PaymentOrderStateServiceTest`、`PaymentCashierServiceImplTest`、`PaymentChannelOrderQueryServiceTest`、`PaymentChannelOrderCloseServiceTest`、`PaymentChannelRefundQueryServiceTest`、`PaymentChannelCallbackServiceTest`、`PaymentOpenApiServiceTest` | DONE |
| PAY-TEST-001 | `MoneyTest`、金额相关服务单测 | 金额规则单元测试 | `12345` 分乘以 `0.006789`；`0.4900/0.5000` 分正数边界；退款占用金额 `9000` 分；对账溢出边界 | 过程计算保留至少 4 位小数，最终收敛为整数分；负数、0 分付款、倒置金额范围和超过 `Long.MAX_VALUE` 均按业务异常拒绝；退款剩余可退金额和对账差异金额通过 `Money` 统一校验 | 后端单元测试，不涉及页面布局 | Maven Surefire 执行真实 Java 代码，未使用固定返回值替代金额计算 | `MoneyTest`、`PaymentOpenApiServiceTest`、`PaymentReconciliationServiceTest`、`PaymentSettlementSummaryServiceTest` | DONE |
| PAY-TEST-001 | `PaymentOpenApiServiceTest` | OpenAPI 签名单元测试 | `AppId=app_openapi`、`tenantId=1`、`timestamp`、`nonce`、HMAC-SHA256 Base64 签名 | 创建订单、查询订单、发起支付、查询支付、发起退款和查询退款均校验 canonical string 签名；错误签名和 nonce 重放按业务码拒绝；密文应用密钥解密后参与签名链路 | API-only 单元测试，不涉及页面布局 | Maven Surefire 执行服务单测，签名使用真实 `Mac` 算法生成，不使用接口拦截 | `PaymentOpenApiServiceTest` | DONE |
| PAY-TEST-001 | `PaymentMethodRouteServiceImplTest` | 通道路由单元测试 | 应用 `310001`、主体 `320001`、方式 `PERSONAL_WECHAT_QR`、终端 `WEB`、接入场景 `MANGO_PAY`、签约能力 `333001`、金额 `9900` 分 | 路由规则创建必须校验应用、主体、支付方式和签约能力；试算命中启用且金额范围内的候选；金额超限返回过滤原因；环境不匹配拒绝保存；已有支付订单引用的路由拒绝删除并写审计 | 后端单元测试，不涉及页面布局 | Maven Surefire 执行服务单测，断言 mapper 入参、业务异常和审计动作 | `PaymentMethodRouteServiceImplTest` | DONE |
| PAY-TEST-001 | `PaymentMangoPayResultMappingServiceTest` | 通道返回码映射单元测试 | `SUCCESS/FAILED/PARAM_ERROR/SIGN_ERROR/CHANNEL_UNAVAILABLE/TIMEOUT/UNKNOWN` 和退款侧 `PARAMETER_ERROR/SIGNATURE_ERROR` | 芒果支付返回码统一映射为支付状态 `SUCCESS/FAILED/PAYING` 或退款状态 `SUCCESS/FAILED/REFUNDING`；超时和未知码保持处理中等待查单或查退款；第三方码不直接污染业务状态 | 后端单元测试，不涉及页面布局 | Maven Surefire 执行纯服务单测，覆盖支付和退款两类映射 | `PaymentMangoPayResultMappingServiceTest` | DONE |

## 4. 验证命令

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=MoneyTest,PaymentOrderStateServiceTest,PaymentCashierServiceImplTest,PaymentOpenApiServiceTest,PaymentChannelOrderQueryServiceTest,PaymentChannelOrderCloseServiceTest,PaymentChannelRefundQueryServiceTest,PaymentChannelCallbackServiceTest,PaymentMethodRouteServiceImplTest,PaymentMangoPayResultMappingServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
```

## 5. 本次验证结果

- Maven Reactor：`BUILD SUCCESS`
- `mango-payment-core` 测试：`Tests run: 68, Failures: 0, Errors: 0, Skipped: 0`

## 6. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 集成测试全链路 | 本证据只覆盖单元测试，未声明 API、数据库约束、并发、通知、对账、结算的集成测试全量闭环 | `PAY-TEST-002` 仍保持 `IN_PROGRESS` | 继续按集成测试专项补证据或补测试 | 不适用 |
| 交付台账总体验收 | 台账仍存在外部通道、长周期调度、可观测性和用户确认项 | 不能据此声明支付模块整体完成或可投产 | 继续按台账逐项关闭或登记用户确认例外 | 不适用 |
