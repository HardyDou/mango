# 支付关键唯一约束验收证据

## 1. 验收范围

- 台账项：`PAY-DATA-002`
- 设计来源：`统一支付系统设计说明书` 11.2 关键唯一约束
- 数据库：`payment_business_order`、`payment_order`、`payment_refund_order`、`payment_transaction_flow`、`payment_reconciliation`、`payment_notification_record`、`payment_openapi_nonce`
- 服务：收银台支付、主动查单、OpenAPI 幂等、对账匹配
- 部署形态：单体管理后台，后端 `http://127.0.0.1:18118`，数据库 `mango_dev_e397cd`

## 2. 执行环境

- 后端地址：`http://127.0.0.1:18118`
- 数据库或租户：`mango_dev_e397cd` / tenant `1`
- 测试账号：`admin`
- 验证时间：`2026-06-06`

## 3. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| PAY-DATA-002 | `payment_order` / Flyway `payment/V47` | 同一业务订单只能有一个有效成功支付 | `success_flag=1` 的支付订单 | 新增生成列 `success_business_order_id = CASE WHEN success_flag = 1 THEN business_order_id ELSE NULL END`；唯一索引 `uk_payment_order_success_business(tenant_id, success_business_order_id)` 允许多笔非成功支付尝试，但阻断第二笔有效成功支付 | 后端数据约束，不涉及页面布局 | 本地库 Flyway 执行成功，重复成功支付检查无冲突行 | `SHOW COLUMNS FROM payment_order LIKE 'success_business_order_id'`; `SHOW INDEX FROM payment_order WHERE Key_name='uk_payment_order_success_business'` | DONE |
| PAY-DATA-002 | `payment_order` / Flyway `payment/V47` | 通道交易号按通道唯一 | `channel_code=MANGO_PAY`、`channel_trade_no=CASHIER-*` | 新增 `payment_order.channel_code` 并从 `payment_channel` 回填；字段为 `NOT NULL`；唯一索引 `uk_payment_order_channel_trade(tenant_id, channel_code, channel_trade_no)`；对账匹配改为按 `channelCode + channelTradeNo` 查询支付订单 | 后端数据约束，不涉及页面布局 | 本地库 Flyway 执行成功，重复通道交易号检查无冲突行 | `SHOW COLUMNS FROM payment_order LIKE 'channel_code'`; `SHOW INDEX FROM payment_order WHERE Key_name='uk_payment_order_channel_trade'` | DONE |
| PAY-DATA-002 | `/api/payment/cashier/pay` | 收银台支付成功并发重复保护 | 单测模拟 `paymentOrderMapper.insert` 抛 `DuplicateKeyException` | `PaymentCashierServiceImpl` 捕获数据库唯一冲突并抛 `PaymentCode.PAYMENT_BUSINESS_ORDER_ALREADY_PAID`；不写 `PAY_SUCCESS` 流水；不推进业务订单成功 | 不涉及页面改动；收银台原有页面能力不变 | Maven 单测通过 | `PaymentCashierServiceImplTest.pay_successDuplicateConstraint_throwsAlreadyPaid` | DONE |
| PAY-DATA-002 | `payment_order` / H2 MySQL 模式 | 回调和主动查单并发有效成功保护 | 同一业务订单两笔 `PAYING` 支付订单并发更新为 `SUCCESS/success_flag=1` | 数据库唯一索引 `uk_payment_order_success_business(tenant_id, success_business_order_id)` 保证同一业务订单只有一笔有效成功；并发更新时一个成功、一个被唯一约束拒绝，失败分支保持非有效成功，供业务服务进入重复支付处理 | 后端数据约束，不涉及页面布局 | Maven 单测通过 | `PaymentOrderUniqueConstraintMigrationTest.databaseConstraints_rejectConcurrentEffectiveSuccess` | DONE |
| PAY-DATA-002 | 异常订单主动查单 / `PaymentChannelOrderQueryService` | 查单推进成功并发重复保护 | 单测模拟 `updatePayingQueryResult` 抛 `DuplicateKeyException` | 主动查单将支付单推进为成功时，若同一业务订单已有有效成功支付，进入 `PaymentDuplicatePaymentService`；重复支付订单以 `success_flag=0` 记录为非有效成功支付，不推进业务订单成功，不发业务支付成功通知；芒果支付自动退款，外部通道挂异常单 | 不涉及页面改动；异常订单页面原有受控操作能力不变 | Maven 单测通过 | `PaymentChannelOrderQueryServiceTest.queryMangoPayPayment_duplicateSuccess_handlesDuplicatePayment`; `PaymentDuplicatePaymentServiceTest` | DONE |
| PAY-DATA-002 | `/openapi/pay/orders`、`/openapi/pay/refunds`、通知、流水、对账 | 业务幂等和关键编号唯一约束 | 现有迁移和服务实现 | `payment_business_order` 已有 `uk_payment_business_order_tenant_app_no(tenant_id, app_code, biz_order_no)`；`payment_refund_order` 已有 `uk_payment_refund_tenant_biz(tenant_id, biz_refund_no)` 和 `uk_payment_refund_no(refund_order_no)`；`payment_transaction_flow` 已有 `uk_payment_flow_no(flow_no)`；`payment_notification_record` 已有 `uk_payment_notification_no(notification_no)`；`payment_openapi_nonce` 已有 `uk_payment_openapi_nonce(tenant_id, app_id, nonce, del_flag)`；对账批次已有 `uk_payment_reconciliation_file_digest(tenant_id, channel_code, bill_date, file_digest)` | 后端数据约束，不涉及页面布局 | 代码和 migration 审查通过；OpenAPI nonce 重放已有 `DuplicateKeyException -> PAYMENT_OPENAPI_NONCE_REPLAY` 单测 | `V3__payment_platform_schema.sql`、`V33__payment_reconciliation_audit_and_bill_detail.sql`、`V42__payment_openapi_nonce.sql`、`PaymentOpenApiServiceTest` | DONE |

## 4. 数据库验证记录

```sql
SELECT version, script, success
FROM flyway_schema_history_payment
WHERE script LIKE '%payment_order_unique_constraints%';

SHOW COLUMNS FROM payment_order LIKE 'channel_code';
SHOW COLUMNS FROM payment_order LIKE 'success_business_order_id';
SHOW INDEX FROM payment_order
WHERE Key_name IN ('uk_payment_order_channel_trade','uk_payment_order_success_business');

SELECT tenant_id,business_order_id,COUNT(*) c
FROM payment_order
WHERE success_flag=1
GROUP BY tenant_id,business_order_id
HAVING c>1;

SELECT tenant_id,channel_code,channel_trade_no,COUNT(*) c
FROM payment_order
WHERE channel_trade_no IS NOT NULL AND channel_trade_no<>''
GROUP BY tenant_id,channel_code,channel_trade_no
HAVING c>1;
```

执行结果：`V47__payment_order_unique_constraints.sql` 在 `flyway_schema_history_payment` 中 `success=1`；两个新增字段和两个唯一索引均存在；重复成功支付、重复通道交易号查询无返回行。

## 5. 验证命令

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentCashierServiceImplTest,PaymentChannelOrderQueryServiceTest,PaymentReconciliationServiceTest,PaymentOrderUniqueConstraintMigrationTest,PaymentDuplicatePaymentServiceTest,PaymentChannelCallbackServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -DskipTests compile
scripts/dev-workspace.sh backend
mysql -uroot -h127.0.0.1 -P3306 mango_dev_e397cd -e "SELECT version, script, success FROM flyway_schema_history_payment WHERE script LIKE '%payment_order_unique_constraints%' ORDER BY installed_rank DESC LIMIT 1; SHOW COLUMNS FROM payment_order LIKE 'channel_code'; SHOW COLUMNS FROM payment_order LIKE 'success_business_order_id'; SHOW INDEX FROM payment_order WHERE Key_name IN ('uk_payment_order_channel_trade','uk_payment_order_success_business');"
```

## 6. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 全量支付中心 E2E 回归 | 本轮范围是 `PAY-DATA-002` 数据唯一约束和并发保护收口，没有改页面布局 | 不影响本项数据库和服务保护结论；不能替代 `PAY-TEST-003` 全量 E2E | 后续按台账执行完整支付中心 Playwright 回归 | 不适用 |
| 外部通道真实回调并发 | 本轮覆盖芒果支付收银台支付、主动查单成功路径和标准化回调重复成功分支；通联、华夏等外部通道尚未接入真实回调和自动退款适配 | 不把外部通道回调并发验收声明完成 | 外部通道接入时复用同一 `payment_order` 唯一约束和重复支付异常挂起能力，并补对应回调、退款适配和并发集成测试 | 不适用 |
