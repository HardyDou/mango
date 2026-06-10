# 支付核心业务表租户隔离验收证据

## 1. 交付项

- 台账项：`PAY-DATA-004`
- 设计来源：`统一支付系统设计说明书.md` 第 12 节“多租户设计”
- 验收目标：支付核心业务表包含 `tenant_id`，后台查询默认带租户上下文；关闭 MyBatis-Plus 租户插件的支付 mapper 必须显式带租户边界；后台受控操作不得跨租户更新资源。

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| PAY-DATA-004 | 数据库结构 | 支付核心表租户字段 | 本地库 `mango_dev_e397cd` 内 27 张 `payment_%` 基础表 | 缺失 `tenant_id` 查询无返回；`payment_table_count=27` | 后端数据结构项，不涉及 UI；验证对象为数据库表结构 | MySQL 查询通过 | 本文件第 2 节命令与输出 | 通过 |
| PAY-DATA-004 | Mapper SQL | 关闭租户插件后的显式租户条件 | `io.mango.payment.core.mapper` 所有 `@InterceptorIgnore(tenantLine = "true")` 方法 | `PaymentTenantIsolationContractTest` 自动扫描方法并断言 XML 语句含 `tenant_id = #{tenantId}`；通知租户枚举入口只返回租户分组 | 后端 Mapper 契约项，不涉及 UI；验证对象为 SQL 租户边界 | Maven 测试通过 | 本文件第 3、5 节 | 通过 |
| PAY-DATA-004 | 后台受控操作 service | 跨租户资源拒绝 | 异常订单、通知记录、对账差异、结算汇总模拟 `tenantId=2`，当前上下文 `tenantId=1` | 返回“资源不存在”；不调用 `updateById`；不写成功审计 | 后端 Service 安全项，不涉及 UI；验证对象为跨租户拒绝行为 | Maven 测试通过 | 本文件第 4、5 节 | 通过 |

## 2. 数据库结构检查

执行命令：

```bash
mysql -uroot -h127.0.0.1 -P3306 mango_dev_e397cd -e "
SELECT t.table_name
FROM information_schema.tables t
LEFT JOIN information_schema.columns c
  ON c.table_schema=t.table_schema
 AND c.table_name=t.table_name
 AND c.column_name='tenant_id'
WHERE t.table_schema='mango_dev_e397cd'
  AND t.table_name LIKE 'payment_%'
  AND t.table_type='BASE TABLE'
  AND c.column_name IS NULL
ORDER BY t.table_name;
SELECT COUNT(*) payment_table_count
FROM information_schema.tables
WHERE table_schema='mango_dev_e397cd'
  AND table_name LIKE 'payment_%'
  AND table_type='BASE TABLE';
"
```

结果：

```text
payment_table_count
27
```

第一段查询未返回缺失 `tenant_id` 的 `payment_%` 基础表；当前库内 27 张支付表均具备租户字段。

## 3. Mapper 隔离契约

新增测试：

- `PaymentTenantIsolationContractTest`

覆盖内容：

- 自动扫描 `io.mango.payment.core.mapper` 下所有 `@InterceptorIgnore(tenantLine = "true")` mapper 方法。
- 要求每个被扫描到的方法都登记在租户隔离契约清单中。
- 要求对应 XML 语句或公共 SQL 片段显式包含 `tenant_id = #{tenantId}`。
- 例外：`PaymentNotificationRecordMapper.selectDueNotificationTenantIds` 是后台通知调度入口，用于枚举待投递租户；该 SQL 不读取业务明细，按 `nr.tenant_id` 分组返回租户 ID，后续明细拉取仍按租户执行。

## 4. Service 跨租户拒绝

新增或补强测试：

- `PaymentReadonlyResourceServiceTest`
  - `handleExceptionOrder_crossTenant_rejectsBeforeUpdate`
  - `retryNotificationRecord_crossTenant_rejectsBeforeUpdate`
  - `handleDifference_crossTenant_rejectsBeforeUpdate`
- `PaymentSettlementSummaryServiceTest`
  - `confirmSettlementSummary_crossTenant_rejectsBeforeUpdate`
  - `voidSettlementSummary_crossTenant_rejectsBeforeUpdate`

覆盖内容：

- 即使 `selectById` 返回了其他租户资源，service 也必须以“资源不存在”拒绝。
- 拒绝后不得调用 `updateById`。
- 拒绝后不得写成功审计。

## 5. 验证命令

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentReadonlyResourceServiceTest,PaymentSettlementSummaryServiceTest,PaymentTenantIsolationContractTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
```

结果：

```text
Tests run: 33, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 6. 结论

`PAY-DATA-004` 当前验收结论为通过：

- 支付库 27 张 `payment_%` 基础表均包含 `tenant_id`。
- 关闭租户插件的支付 mapper 已通过契约测试约束显式租户条件。
- 后台异常订单、通知重推、差异处理、结算确认/作废等受控操作已补跨租户拒绝测试。

全量支付模块仍有其他 `IN_PROGRESS` 台账项，不能据此声明支付模块整体完成。
