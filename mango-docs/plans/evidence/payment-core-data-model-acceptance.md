# 支付核心数据模型验收证据

## 1. 交付项

- 台账项：`PAY-DATA-001`
- 设计来源：`统一支付系统设计说明书.md` 第 11.1 节“核心表”
- 验收目标：设计文档列出的支付核心数据模型均具备物理表、审计字段、租户字段、实体和 MyBatis-Plus Mapper；历史 JSON 或主表内联模型补齐为独立表。

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| PAY-DATA-001 | 数据库迁移 | 补齐支付核心独立模型表 | `V48__payment_core_model_completion.sql` | 新增 `payment_tenant`、`payment_subject_bank_account`、`payment_channel_field_template`、`payment_channel_contract_value`、`payment_channel_bill_batch`、`payment_risk_rule`；均包含 `tenant_id`、`created_by`、`created_at`、`updated_by`、`updated_at`、`del_flag` | 后端数据模型项，不涉及 UI | MySQL 临时库全量迁移和工作区库迁移执行通过 | 本文件第 3 节 | DONE |
| PAY-DATA-001 | 实体/Mapper | 设计核心表映射到实体和 MyBatis-Plus Mapper | `PaymentCoreDataModelContractTest` | 自动读取设计文档 11.1 核心表清单；断言每张设计表都有物理表映射、实体继承 `AuditableEntity`、实体 `@TableName` 正确、Mapper 直接继承 `BaseMapper<实体>` | 后端契约项，不涉及 UI | Maven 测试通过 | 本文件第 4 节 | DONE |
| PAY-DATA-001 | 历史数据回填 | 内联数据迁移到独立模型 | 本地库 `mango_dev_e397cd` | 主体银行账户、通道字段模板、签约配置值、账单批次均从现有真实表回填；不是 mock 或固定页面数据 | 后端数据模型项，不涉及 UI | MySQL 查询返回新表和行数 | 本文件第 3 节 | DONE |
| PAY-DATA-001 | 数据库迁移 | 清理历史初始化样例运行数据 | `V64__payment_remove_seeded_business_runtime_data.sql` | 删除旧种子写入的业务订单、支付订单、退款、流水、异常、通知、对账、差异、结算和审计样例记录；保留应用、通道等可运营配置 | 后端数据模型项，不涉及 UI | H2 MySQL 模式执行清理迁移，断言样例运行数据被删除、配置数据仍保留 | `PaymentSeededRuntimeDataCleanupMigrationTest` | DONE |
| PAY-DATA-001 | 生产红线契约 | 防止支付生产源码和迁移再次出现交付红线或旧样例运行数据清理缺口 | `PaymentProductionRedlineContractTest` | 扫描支付后端生产源码和支付前端源码，断言不存在 mock/fake/sandbox/TODO/FIXME/固定成功/伪代码等红线词；同时断言 V4 历史样例运行单号均被 V64 清理迁移覆盖 | 后端契约项，不涉及 UI | Maven 测试通过 | 本文件第 4 节 | DONE |

## 2. 设计表映射

| 设计表 | 当前物理表 | 实体 | Mapper |
|---|---|---|---|
| `pay_tenant` | `payment_tenant` | `PaymentTenantEntity` | `PaymentTenantMapper` |
| `pay_app` | `payment_application` | `PaymentApplication` | `PaymentApplicationMapper` |
| `pay_subject` | `payment_enterprise_subject` | `PaymentEnterpriseSubject` | `PaymentEnterpriseSubjectMapper` |
| `pay_subject_bank_account` | `payment_subject_bank_account` | `PaymentSubjectBankAccountEntity` | `PaymentSubjectBankAccountMapper` |
| `pay_channel` | `payment_channel` | `PaymentChannel` | `PaymentChannelMapper` |
| `pay_channel_capability` | `payment_channel_capability` | `PaymentChannelCapability` | `PaymentChannelCapabilityMapper` |
| `pay_channel_field_template` | `payment_channel_field_template` | `PaymentChannelFieldTemplateEntity` | `PaymentChannelFieldTemplateMapper` |
| `pay_channel_contract` | `payment_channel_contract` | `PaymentChannelContract` | `PaymentChannelContractMapper` |
| `pay_channel_contract_value` | `payment_channel_contract_value` | `PaymentChannelContractValueEntity` | `PaymentChannelContractValueMapper` |
| `pay_channel_contract_capability` | `payment_channel_contract_capability` | `PaymentChannelContractCapability` | `PaymentChannelContractCapabilityMapper` |
| `pay_method_category` | `payment_method_category` | `PaymentMethodCategory` | `PaymentMethodCategoryMapper` |
| `pay_method` | `payment_method` | `PaymentMethod` | `PaymentMethodMapper` |
| `pay_method_route_rule` | `payment_method_route_rule` | `PaymentMethodRouteRule` | `PaymentMethodRouteRuleMapper` |
| `pay_method_route_rule_item` | `payment_method_route_rule_item` | `PaymentMethodRouteRuleItem` | `PaymentMethodRouteRuleItemMapper` |
| `pay_cashier` | `payment_cashier_config` | `PaymentCashierConfig` | `PaymentCashierConfigMapper` |
| `pay_biz_order` | `payment_business_order` | `PaymentBusinessOrderEntity` | `PaymentBusinessOrderMapper` |
| `pay_payment_order` | `payment_order` | `PaymentOrderEntity` | `PaymentOrderMapper` |
| `pay_refund_order` | `payment_refund_order` | `PaymentRefundOrderEntity` | `PaymentRefundOrderMapper` |
| `pay_transaction_flow` | `payment_transaction_flow` | `PaymentTransactionFlowEntity` | `PaymentTransactionFlowMapper` |
| `pay_channel_bill_batch` | `payment_channel_bill_batch` | `PaymentChannelBillBatchEntity` | `PaymentChannelBillBatchMapper` |
| `pay_channel_bill_detail` | `payment_channel_bill_detail` | `PaymentChannelBillDetailEntity` | `PaymentChannelBillDetailMapper` |
| `pay_reconcile_diff` | `payment_difference` | `PaymentDifferenceEntity` | `PaymentDifferenceMapper` |
| `pay_settlement_summary` | `payment_settlement_summary` | `PaymentSettlementSummaryEntity` | `PaymentSettlementSummaryMapper` |
| `pay_notify_record` | `payment_notification_record` | `PaymentNotificationRecordEntity` | `PaymentNotificationRecordMapper` |
| `pay_operation_audit` | `payment_operation_audit` | `PaymentOperationAudit` | `PaymentOperationAuditMapper` |
| `pay_risk_rule` | `payment_risk_rule` | `PaymentRiskRuleEntity` | `PaymentRiskRuleMapper` |

## 3. 数据库迁移执行

临时库全量演练：

```bash
mysql -uroot -h127.0.0.1 -P3306 -e "DROP DATABASE IF EXISTS mango_dev_e397cd_v48test; CREATE DATABASE mango_dev_e397cd_v48test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
for f in $(rg --files mango/mango-platform/mango-payment/mango-payment-core/src/main/resources/db/migration/payment | sort -V); do mysql -uroot -h127.0.0.1 -P3306 mango_dev_e397cd_v48test < "$f" || exit 1; done
```

结果：全量 payment migration 执行通过。

工作区库执行：

```bash
mysql -uroot -h127.0.0.1 -P3306 mango_dev_e397cd < mango/mango-platform/mango-payment/mango-payment-core/src/main/resources/db/migration/payment/V48__payment_core_model_completion.sql
mysql -uroot -h127.0.0.1 -P3306 mango_dev_e397cd -e "
SELECT table_name
FROM information_schema.tables
WHERE table_schema='mango_dev_e397cd'
  AND table_name IN ('payment_tenant','payment_subject_bank_account','payment_channel_field_template','payment_channel_contract_value','payment_channel_bill_batch','payment_risk_rule')
ORDER BY table_name;
SELECT 'payment_tenant' table_name, COUNT(*) row_count FROM payment_tenant
UNION ALL SELECT 'payment_subject_bank_account', COUNT(*) FROM payment_subject_bank_account
UNION ALL SELECT 'payment_channel_field_template', COUNT(*) FROM payment_channel_field_template
UNION ALL SELECT 'payment_channel_contract_value', COUNT(*) FROM payment_channel_contract_value
UNION ALL SELECT 'payment_channel_bill_batch', COUNT(*) FROM payment_channel_bill_batch
UNION ALL SELECT 'payment_risk_rule', COUNT(*) FROM payment_risk_rule;
"
```

结果：

```text
TABLE_NAME
payment_channel_bill_batch
payment_channel_contract_value
payment_channel_field_template
payment_risk_rule
payment_subject_bank_account
payment_tenant

table_name                         row_count
payment_tenant                     1
payment_subject_bank_account       3
payment_channel_field_template     4
payment_channel_contract_value     18
payment_channel_bill_batch         17
payment_risk_rule                  1
```

## 4. 契约测试

新增测试：

- `PaymentCoreDataModelContractTest`
- `PaymentSeededRuntimeDataCleanupMigrationTest`
- `PaymentProductionRedlineContractTest`

执行命令：

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentCoreDataModelContractTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentSeededRuntimeDataCleanupMigrationTest,PaymentMangoPayMigrationConceptContractTest,PaymentMangoPayRuntimeResidualMigrationTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentProductionRedlineContractTest,PaymentSeededRuntimeDataCleanupMigrationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 5. 结论

`PAY-DATA-001` 当前验收结论为通过：

- 设计文档 11.1 的核心表均有当前物理表映射。
- 本轮补齐了过去以内联字段或 JSON 承载的独立模型。
- 新增实体均继承 `AuditableEntity`，新增 Mapper 均使用 MyBatis-Plus `BaseMapper`。
- 已新增 V64 清理旧种子样例运行数据，避免生产初始化结果带业务流水、订单、通知、对账和结算样例记录。
- 已通过临时库全量迁移、工作区库迁移和静态契约测试。

全量支付模块仍有其他 `IN_PROGRESS` 台账项，不能据此声明支付模块整体完成。
