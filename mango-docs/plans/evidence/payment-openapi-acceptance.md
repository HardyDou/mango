# 支付开放接口订单、支付、退款、凭证与通知验收证据

## 1. 验收范围

- 接口：`POST /openapi/pay/orders`、`GET /openapi/pay/orders/{bizOrderNo}`、`POST /openapi/pay/orders/{bizOrderNo}/cashier`、`POST /openapi/pay/orders/{bizOrderNo}/pay`、`GET /openapi/pay/payment-orders/{payOrderNo}`、`POST /openapi/pay/refunds`、`GET /openapi/pay/refunds/{bizRefundNo}`、`GET /openapi/pay/receipts/{bizOrderNo}`、业务方支付/退款通知 HTTP 回调
- 安全：`AppId`、`tenantId`、`timestamp`、`nonce`、`signature` 签名认证，SHA-256 请求体摘要，HMAC-SHA256 Base64 签名，nonce 防重放
- 数据：`payment_application`、`payment_cashier_config`、`payment_method_route_rule`、`payment_method_route_rule_item`、`payment_business_order`、`payment_order`、`payment_refund_order`、`payment_transaction_flow`、`payment_notification_record`、`payment_openapi_nonce`
- 部署形态：本地单体后端 + Mango Admin 单体前端代理

## 2. 执行环境

- 前端地址：`http://127.0.0.1:7808`
- 后端地址：`http://127.0.0.1:18118`
- 数据库或租户：`mango_dev_e397cd`，租户 `1`
- 测试工具：Playwright Chromium、Maven、MySQL

## 3. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| PAY-API-001 | 开放接口签名认证 | 请求必须携带 `AppId`、`tenantId`、`timestamp`、`nonce`、`signature` | `app_openapi_e2e`，真实 `payment_application` 记录 | 签名通过后可创建订单；错误签名会返回业务错误；重复 nonce 返回 `3793` | API-only，不涉及页面布局 | HTTP 200，业务码区分成功和失败 | `PLAYWRIGHT... --grep "开放接口"` | DONE |
| PAY-API-001 | nonce 防重放 | 相同 nonce 和 signature 重放同一创建请求 | `replay-*` nonce | 首次请求成功，第二次返回 `PAYMENT_OPENAPI_NONCE_REPLAY`，`payment_openapi_nonce` 持久化防重放记录 | API-only，不涉及页面布局 | 接口业务错误符合预期 | `PLAYWRIGHT... --grep "开放接口"` | DONE |
| PAY-API-002 | `POST /openapi/pay/orders` | 创建业务订单 | `OPENAPI-BO-*`，金额 `128800` 分 | 返回真实业务订单 ID、`appId`、业务单号、金额、`PAYING` 状态；落库 `payment_business_order` | API-only，不涉及页面布局 | 接口业务成功 | `PLAYWRIGHT... --grep "开放接口"` | DONE |
| PAY-API-002 | 创建业务订单幂等 | 同一 `tenantId + AppId + bizOrderNo`，相同字段重复提交 | 同一 `OPENAPI-BO-*` | 返回同一业务订单 ID；金额、业务单号不变 | API-only，不涉及页面布局 | 接口业务成功 | `PLAYWRIGHT... --grep "开放接口"` | DONE |
| PAY-API-002 | 创建业务订单幂等冲突 | 同一幂等键但金额不同 | 金额从 `128800` 改为 `129900` | 返回 `3794`，不覆盖原订单 | API-only，不涉及页面布局 | 接口业务错误符合预期 | `PLAYWRIGHT... --grep "开放接口"` | DONE |
| PAY-API-003 | `GET /openapi/pay/orders/{bizOrderNo}` | 查询业务订单 | 已创建 `OPENAPI-BO-*` | 返回同一业务订单 ID、业务单号和金额 | API-only，不涉及页面布局 | 接口业务成功 | `PLAYWRIGHT... --grep "开放接口"` | DONE |
| PAY-API-004 | `POST /openapi/pay/orders/{bizOrderNo}/cashier` | 获取收银台入口 | 默认收银台 `359901` | 返回 `cashierConfigId`、`businessOrderId`、业务单号和 `/payment/cashier-configs/{cashierId}/cashier?businessOrderId={id}` | API-only，不涉及页面布局；收银台页面另由收银台证据覆盖 | 接口业务成功 | `PLAYWRIGHT... --grep "开放接口"` | DONE |
| PAY-API-005 | `POST /openapi/pay/orders/{bizOrderNo}/pay` | 发起支付 | 已创建 `OPENAPI-BO-*`，`methodCode=PERSONAL_WECHAT_QR`，测试应用路由规则命中签约能力 `333001` | 通过真实 `PaymentCashierService` 生成 `payment_order`；返回支付订单号、业务单号、金额、`SUCCESS` 状态、通道 `MANGO_PAY`、路由规则、通道交易号、交易流水号和 QR 支付物料 | API-only，不涉及页面布局；支付物料渲染由收银台证据覆盖 | 接口业务成功，真实落库支付订单和交易流水 | `PLAYWRIGHT... --grep "开放接口"` | DONE |
| PAY-API-006 | `GET /openapi/pay/payment-orders/{payOrderNo}` | 查询支付订单 | 使用 `/pay` 返回的真实 `payOrderNo` | 返回同一支付订单号、业务单号、`AppId`、金额、状态、支付方式、通道、交易流水号；按签名 `AppId` 隔离查询 | API-only，不涉及页面布局 | 接口业务成功 | `PLAYWRIGHT... --grep "开放接口"` | DONE |
| PAY-API-007 | `POST /openapi/pay/refunds` | 发起退款 | 已成功支付的 `OPENAPI-BO-*`，`bizRefundNo=OPENAPI-RF-*`，退款金额 `38800` 分 | 生成真实 `payment_refund_order`；更新业务订单退款进度；生成 `REFUND_SUCCESS` 交易流水；返回退款单号、原支付单号、退款金额、`SUCCESS`、通道退款号和流水号 | API-only，不涉及页面布局 | 接口业务成功，真实落库退款订单和交易流水 | `PLAYWRIGHT... --grep "开放接口"` | DONE |
| PAY-API-007 | 退款幂等 | 同一 `tenantId + AppId + bizRefundNo`，相同字段重复提交 | 同一 `OPENAPI-RF-*` | 返回同一退款订单 ID 和退款订单号，不重复扣减可退金额 | API-only，不涉及页面布局 | 接口业务成功 | `PLAYWRIGHT... --grep "开放接口"` | DONE |
| PAY-API-007 | 退款幂等冲突 | 同一业务退款单号但退款金额不同 | 金额从 `38800` 改为 `39900` | 返回 `3794`，不覆盖原退款订单 | API-only，不涉及页面布局 | 接口业务错误符合预期 | `PLAYWRIGHT... --grep "开放接口"` | DONE |
| PAY-API-007 | 超额退款拦截 | 新业务退款单号，退款金额超过剩余可退金额 | 退款金额 `200000` 分 | 返回 `3802`，不创建退款订单 | API-only，不涉及页面布局 | 接口业务错误符合预期 | `PLAYWRIGHT... --grep "开放接口"` | DONE |
| PAY-API-008 | `GET /openapi/pay/refunds/{bizRefundNo}` | 查询退款订单 | 使用 `/refunds` 返回的真实 `bizRefundNo` | 返回同一退款订单号、业务退款单号、`SUCCESS` 状态和退款流水号；按签名 `AppId` 隔离查询 | API-only，不涉及页面布局 | 接口业务成功 | `PLAYWRIGHT... --grep "开放接口"` | DONE |
| PAY-API-009 | `GET /openapi/pay/receipts/{bizOrderNo}` | 获取支付凭证 | 已成功支付的 `OPENAPI-BO-*` | 返回稳定支付凭证号、业务订单号、支付订单号、`AppId`、金额、币种、支付方式、通道、通道交易号、交易流水号、付款时间和凭证出具时间；按签名 `AppId` 隔离查询 | API-only，不涉及页面布局；本接口返回支付域凭证数据，不生成会计凭证或下载文件 | 接口业务成功 | `PLAYWRIGHT... --grep "开放接口"` | DONE |
| PAY-API-010 | 业务方通知 HTTP 回调 | 支付成功通知业务方 | 已成功支付的 `OPENAPI-BO-*`，订单级 `notifyUrl` 指向 Playwright 本地真实 HTTP 接收器 | 支付事务提交后创建 `payment_notification_record`，真实 POST `PAYMENT_SUCCESS` 报文；报文包含 `notifyNo`、业务单号、支付单号、金额、状态、支付方式、通道、流水号、`signature`；业务方返回 `SUCCESS` ACK 后通知记录更新为 `SUCCESS` | API-only，不涉及页面布局；后台通知记录页另有证据覆盖 | HTTP 回调真实送达，ACK 成功 | `PLAYWRIGHT... --grep "开放接口"` | DONE |
| PAY-API-010 | 业务方通知 HTTP 回调 | 退款成功通知业务方 | 已成功退款的 `OPENAPI-RF-*`，订单级 `notifyUrl` 指向 Playwright 本地真实 HTTP 接收器 | 退款事务提交后创建 `payment_notification_record`，真实 POST `REFUND_SUCCESS` 报文；报文包含 `notifyNo`、业务单号、支付单号、业务退款单号、退款单号、退款金额、状态、通道退款号、流水号、`signature`；业务方返回 `SUCCESS` ACK 后通知记录更新为 `SUCCESS` | API-only，不涉及页面布局；后台通知记录页另有证据覆盖 | HTTP 回调真实送达，ACK 成功 | `PLAYWRIGHT... --grep "开放接口"` | DONE |
| PAY-API-010 | 异常订单页面、通知记录页面 | 支付失败通知业务方 | 真实收银台支付订单 `PO\\d{20}`，异常单 `EX-NT-F-*`，订单级 `notifyUrl` 指向 Playwright 本地真实 HTTP 接收器，芒果支付查单场景 `FAILED` | 页面处理异常订单选择“主动查单”后，支付订单由 `PAYING` 推进为 `FAILED`；事务提交后创建 `payment_notification_record`，真实 POST `PAYMENT_FAILED` 报文；业务方返回 `SUCCESS` ACK 后通知记录更新为 `SUCCESS`，通知记录页面按通知单号和成功状态可回显 | 异常订单处理弹窗、通知记录列表筛选和状态标签通过 Chromium 验证；截图 `test-results/payment-terminal-notifications.png` | HTTP 回调真实送达，ACK 成功；console/pageerror/payment requestfailed 均无异常 | `PLAYWRIGHT... --grep "支付失败关闭和退款失败通知可由真实状态流触发并在页面回显"` | DONE |
| PAY-API-010 | 异常订单页面、通知记录页面 | 支付关闭通知业务方 | 真实收银台支付订单 `PO\\d{20}`，异常单 `EX-NT-C-*`，订单级 `notifyUrl` 指向 Playwright 本地真实 HTTP 接收器 | 页面处理异常订单选择“关闭支付订单”后，支付订单和业务订单由 `PAYING` 推进为 `CLOSED`；事务提交后创建 `payment_notification_record`，真实 POST `PAYMENT_CLOSED` 报文；业务方返回 `SUCCESS` ACK 后通知记录更新为 `SUCCESS`，通知记录页面按通知单号和成功状态可回显 | 异常订单处理弹窗、通知记录列表筛选和状态标签通过 Chromium 验证；截图 `test-results/payment-terminal-notifications.png` | HTTP 回调真实送达，ACK 成功；console/pageerror/payment requestfailed 均无异常 | `PLAYWRIGHT... --grep "支付失败关闭和退款失败通知可由真实状态流触发并在页面回显"` | DONE |
| PAY-API-010 | 退款订单页面、通知记录页面 | 退款失败通知业务方 | 真实成功支付订单 `PO\\d{20}`、退款中退款单 `RO-NT-F-*`，订单级 `notifyUrl` 指向 Playwright 本地真实 HTTP 接收器，芒果支付查退款场景 `FAILED` | 页面点击“主动查退款”后，退款订单由 `REFUNDING` 推进为 `FAILED`；事务提交后创建 `payment_notification_record`，真实 POST `REFUND_FAILED` 报文；业务方返回 `SUCCESS` ACK 后通知记录更新为 `SUCCESS`，通知记录页面按通知单号和成功状态可回显 | 退款订单状态筛选、行操作、通知记录列表筛选和状态标签通过 Chromium 验证；截图 `test-results/payment-terminal-notifications.png` | HTTP 回调真实送达，ACK 成功；console/pageerror/payment requestfailed 均无异常 | `PLAYWRIGHT... --grep "支付失败关闭和退款失败通知可由真实状态流触发并在页面回显"` | DONE |
| PAY-API-010 | `/payment/notification-records/deliver-due`、通知记录页面 | 通知失败按应用重试策略推进并在耗尽后等待人工补偿 | `E2E_NOTIFY_RETRY_*` 真实 `payment_application.notify_retry_policy=1m,5m,15m`；两条到期 `payment_notification_record` 指向 Playwright 本地真实 HTTP 接收器，业务方返回 `WAIT` | 第一次到期重试失败后 `retry_times=1`，`next_retry_time` 推进到约 5 分钟后；第三次到期重试失败后 `retry_times=3`，`next_retry_time` 清空，响应信息追加“通知重试策略已耗尽，等待人工补偿重推”；成功和耗尽均通过专用 mapper 写库，避免 null 字段不落库 | 通知记录列表按“通知失败”筛选可回显耗尽记录，保留“重推”人工补偿按钮；截图 `test-results/payment-notification-retry-policy.png` | 真实 HTTP 回调送达；console/pageerror/notification requestfailed 均无异常 | `PLAYWRIGHT... --grep "通知失败按应用重试策略推进"`；`PaymentNotificationServiceTest` | DONE |
| PAY-API-010 / PAY-DOMAIN-002 / PAY-DOMAIN-003 | `POST /payment/channel-callbacks`、`PaymentChannelCallbackService` | 通道适配器验签后可提交标准化支付/退款回调并触发业务通知 | `callbackType=PAYMENT/REFUND`，`channelCode=MANGO_PAY`，支付订单 `PO202606060001`，退款订单 `RO202606060001`，通道商户号 `MCH202606060001` | 接口标记为 `@InternalApi`，只作为具体通道适配器验签后的标准化入口；支付回调校验通道、商户号和金额，`PAYING -> SUCCESS/FAILED`，成功时写 `PAY_SUCCESS` 流水和业务订单成功，终态幂等不重复通知；退款回调校验通道、商户号和金额，`REFUNDING/PROCESSING -> SUCCESS/FAILED`，成功时更新退款进度并写 `REFUND_SUCCESS` 流水；支付和退款终态均复用 `PaymentNotificationService` 触发业务方通知 | API-only，不涉及页面布局；通道公网回调验签仍归具体通道适配器 | 单元测试覆盖支付成功、支付失败、支付终态幂等、支付金额不一致、退款成功、退款失败、退款处理中回调拒绝 | `PaymentChannelCallbackServiceTest` | DONE |
| PAY-DOMAIN-001 / PAY-DOMAIN-002 / PAY-DOMAIN-003 | `payment_order_status_flow`、业务/支付/退款订单详情 | 业务订单、支付订单、退款订单状态变化必须记录真实流转记录 | `V49__payment_order_status_flow.sql`；收银台支付、OpenAPI 创建/退款、主动查单、受控关单、主动查退款、标准化通道回调 | 新增 `payment_order_status_flow` 真实表，记录 `order_type/order_id/order_no/from_status/to_status/trigger_source/trigger_no/operator/happen_time/tenant_id`；业务订单详情、支付订单详情、退款订单详情均从状态流表读取，不再根据当前状态临时拼时间线；支付成功业务订单状态流记录真实落库状态 `SUCCESS`，展示层仍按枚举显示为“已支付”；已有历史业务/支付/退款订单由 migration 回填 `HISTORY_BACKFILL` 初始化记录；状态变化服务统一使用 `Require` 校验并写审计字段 | 后台详情抽屉展示数据库状态流；业务订单详情新增状态流转时间线，支付/退款详情沿用时间线 | 定向单元测试 66 个通过；状态真实值修正后关键状态流测试 43 个通过；前端生产构建通过 | `PaymentOrderStatusFlowServiceTest`、`PaymentReadonlyResourceServiceTest`、`PaymentCashierServiceImplTest`、`PaymentOpenApiServiceTest`、`PaymentChannelOrderQueryServiceTest`、`PaymentChannelOrderCloseServiceTest`、`PaymentChannelRefundQueryServiceTest`、`PaymentChannelCallbackServiceTest`、`pnpm --dir mango-ui/apps/mango-admin build` | DONE |
| PAY-DOMAIN-002 | `payment_order` 唯一约束 | 一个业务订单最终只允许一笔支付订单成为有效成功支付 | H2 MySQL 模式真实建表插入 `payment_order`，含 `success_business_order_id` 生成列、`uk_payment_order_channel_trade` 和 `uk_payment_order_success_business` | 第一笔 `success_flag=1` 成功写入；同租户同通道同通道交易号第二笔写入触发数据库唯一冲突；同租户同业务订单第二笔 `success_flag=1` 写入触发数据库唯一冲突；同业务订单非有效成功支付 `success_flag=0` 可继续写入 | API-only，不涉及页面布局 | `PaymentOrderUniqueConstraintMigrationTest` 使用真实 JDBC 插入验证数据库约束，不只做 SQL 文本扫描 | `mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentOrderUniqueConstraintMigrationTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false` | DONE |
| PAY-DOMAIN-002 | `PaymentDuplicatePaymentService`、主动查单、标准化支付回调 | 重复成功支付进入自动退款或异常挂起 | 已有有效成功支付后，第二笔 `PAYING` 支付订单被通道查单或回调确认为成功 | `DuplicateKeyException` 不再只向上抛出；重复支付订单先以 `success_flag=0` 记录为非有效成功支付并写 `PAY_SUCCESS` 流水和状态流；芒果支付自动创建 `DUP-{payOrderNo}` 退款单、写 `REFUND_SUCCESS` 流水并推进 `SUCCESS -> DUPLICATE_REFUNDING -> DUPLICATE_REFUNDED`；未具备自动退款适配器的外部通道创建 `DUPLICATE_PAYMENT` 异常单，`HIGH/PENDING` 等待受控处理 | API-only，不涉及页面布局；异常订单页面已有列表和处理证据 | 单元测试覆盖芒果支付自动退款、外部通道异常挂起、主动查单重复成功、通道回调重复成功 | `mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentDuplicatePaymentServiceTest,PaymentChannelOrderQueryServiceTest,PaymentChannelCallbackServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false` | DONE |
| PAY-DOMAIN-003 | `PaymentOpenApiService`、`PaymentChannelRefundQueryService`、`PaymentChannelCallbackService` | 并发退款与累计可退金额保护 | 成功支付订单 `PO202606060001`，退款金额 `3300` 分，已占用退款金额 `7000/9000` 分，业务退款进度 CAS 失败场景 | OpenAPI 退款在统计占用退款金额前先执行 `lockSuccessfulOpenPaymentOrder(... for update)`；按 `sumOccupyingRefundAmount` 将 `CREATED/REFUNDING/PROCESSING/SUCCESS` 退款金额纳入占用；超出剩余可退金额不创建退款单、不写流水；成功退款落库后若 `payment_business_order.updateRefundProgress` 的金额条件或状态条件失败，则事务抛 `PAYMENT_REFUND_AMOUNT_EXCEEDED`，不写 `REFUND_SUCCESS` 流水、不触发业务通知；主动查退款和标准化退款回调同样覆盖业务进度 CAS 失败无副作用 | API-only，不涉及页面布局；退款订单页面已有查询和详情证据 | 单元测试覆盖行锁顺序、占用金额超限、行锁缺失、业务退款进度 CAS 失败、主动查退款 CAS 失败、退款回调 CAS 失败 | `mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentOpenApiServiceTest,PaymentChannelRefundQueryServiceTest,PaymentChannelCallbackServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false` | DONE |
| PAY-DOMAIN-003 | `PaymentChannelRefundQueryService`、`PaymentChannelCallbackService` | 退款查单和退款回调竞争只允许一次成功副作用 | 同一退款单 `RO202606060001`，两次 `REFUNDING -> SUCCESS` 竞争，第一次 CAS 更新成功，第二次 CAS 更新返回 0 | 主动查退款和标准化退款回调均通过 `updateRefundingQueryResult` 的租户、订单 ID 和退款中状态条件做 CAS 推进；第一笔成功触发业务退款进度、`REFUND_SUCCESS` 流水和退款通知；第二笔竞争失败抛 `PAYMENT_REFUND_ORDER_STATE_INVALID`，不重复写退款流水、不重复通知业务方、不重复写查询记录 | API-only，不涉及页面布局 | `PaymentChannelRefundQueryServiceTest`、`PaymentChannelCallbackServiceTest` 新增竞争场景，16 个定向测试通过 | `mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentChannelRefundQueryServiceTest,PaymentChannelCallbackServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false` | DONE |

## 4. 签名约定

E2E 使用的 canonical string：

```text
METHOD
REQUEST_URI
SHA256_HEX(body or "")
timestamp
nonce
```

签名算法：`HMAC-SHA256`，输出 `Base64`。通过前端代理请求 `/api/openapi/pay/orders` 时，后端实际 `REQUEST_URI` 为 `/openapi/pay/orders`，签名按后端路径计算。

## 5. 验证命令

```bash
mvn -pl :mango-payment-starter -am test -DskipTests
mvn -pl :mango-payment-core -am -Dtest='PaymentOpenApiServiceTest,PaymentNotificationServiceTest,PaymentOrderStateServiceTest,PaymentCashierServiceImplTest' -Dsurefire.failIfNoSpecifiedTests=false test
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core,mango-platform/mango-payment/mango-payment-starter -am -Dtest=PaymentChannelCallbackServiceTest,PaymentNotificationServiceTest,PaymentChannelOrderQueryServiceTest,PaymentChannelRefundQueryServiceTest,PaymentNotificationDispatchSchedulerTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentOrderStatusFlowServiceTest,PaymentReadonlyResourceServiceTest,PaymentChannelCallbackServiceTest,PaymentChannelOrderQueryServiceTest,PaymentChannelOrderCloseServiceTest,PaymentChannelRefundQueryServiceTest,PaymentCashierServiceImplTest,PaymentOpenApiServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentCashierServiceImplTest,PaymentChannelOrderQueryServiceTest,PaymentChannelCallbackServiceTest,PaymentOrderStatusFlowServiceTest,PaymentReadonlyResourceServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentOrderUniqueConstraintMigrationTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentDuplicatePaymentServiceTest,PaymentChannelOrderQueryServiceTest,PaymentChannelCallbackServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentOpenApiServiceTest,PaymentChannelRefundQueryServiceTest,PaymentChannelCallbackServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentChannelRefundQueryServiceTest,PaymentChannelCallbackServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
pnpm -F mango-admin exec playwright test --list e2e/specs/payment-center.spec.ts --grep "开放接口"
scripts/dev-workspace.sh backend
mysql -h127.0.0.1 -P3306 -uroot mango_dev_e397cd -e "SHOW TABLES LIKE 'payment_openapi_nonce'; SELECT version, script, success FROM flyway_schema_history_payment WHERE script='V42__payment_openapi_nonce.sql' ORDER BY installed_rank DESC LIMIT 1;"
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1 --grep "开放接口"
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm --dir mango-ui/apps/mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --grep "支付失败关闭和退款失败通知可由真实状态流触发并在页面回显"
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm --dir mango-ui/apps/mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --grep "通知失败按应用重试策略推进"
pnpm -F mango-admin build
```

## 6. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 外部通道公网回调验签适配 | 已补支付成功、支付失败、支付关闭、退款成功、退款失败的真实 HTTP 通知与 ACK 记录；已补按应用重试策略推进、重试耗尽后停止自动调度并保留人工补偿入口；已补通道适配器验签后调用的标准化回调入口；具体外部通道公网验签、机构回调联调仍未完成 | 不阻塞 `PAY-API-010` 的业务方通知能力关闭；继续阻塞 `PAY-CHANNEL-003/005/007/008` 等外部通道项 | 后续按通联、华夏、微信、支付宝等通道台账补齐公网回调验签、真实机构回调联调和适配器证据 | 不适用 |
| 外部通道联调 | 已补 `PaymentOrderStateService` 迁移校验和 `payment_order_status_flow` 真实流转记录，覆盖业务订单、支付订单、退款订单在收银台支付、OpenAPI 创建/退款、芒果支付主动查单、受控关单、主动查退款、标准化通道回调和对账补偿中的状态记录；已补真实数据库插入验证，证明通道交易号唯一和单业务订单有效成功唯一约束生效；已补重复成功支付进入芒果支付自动退款或外部通道异常挂起；已补退款创建行锁、占用金额统计、业务退款进度 CAS 失败无副作用单测；已补退款查单和标准化退款回调竞争时只有一次成功资金副作用；已补对账账单作为通道依据推进 `PAYING -> SUCCESS`、`REFUNDING -> SUCCESS` 的状态流和资金流水证据；但具体外部通道自动退款适配和外部通道验签回调联调仍未完成 | 不阻塞 `PAY-DOMAIN-001/002/003` 核心状态机关闭；继续阻塞 `PAY-CHANNEL-003/005/007/008` 等外部通道项 | 后续按外部通道台账补退款适配、验签回调联调和机构侧集成证据 | 不适用 |
| 调度平台长周期运行 | 设计文档规定支付模块不自建定时任务；本轮已验证平台调度器或人工任务入口可投递到期记录，但未做跨租户长时间运行和运维告警压测 | 不能声明调度平台投产运维能力完整完成 | 后续接平台统一调度运维验收、告警和批量压力验证 | 不适用 |
| 凭证下载文件 | `PAY-API-009` 当前实现支付域凭证查询，未生成或归档 PDF/图片等文件 | 不能声明支付凭证文件下载已完成 | 如确需下载能力，后续必须接入真实文件中心和归档链路 | 不适用 |
| 完整通道退款 SPI | 当前代码尚未建立正式通道退款适配 SPI，本次只在既有芒果支付本地支付闭环下完成 OpenAPI 退款 DB 闭环 | 不能声明通联、华夏、外部机构退款已接通 | 后续按通道台账补齐退款适配、退款查询和回调 | 不适用 |
| 应用密钥加密存储 | 本次范围是 OpenAPI 调用认证和订单接口；密钥静态加密归属安全专项 | 不影响本地验收签名链路，但投产前仍需安全专项验收 | 后续按安全台账推进 | 不适用 |
