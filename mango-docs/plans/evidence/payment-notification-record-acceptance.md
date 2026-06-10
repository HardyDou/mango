# 通知记录列表页验收证据

## 1. 验收范围

- 页面：支付中心 / 对账结算 / 通知记录
- 接口：`GET /payment/notification-records/page`、`GET /payment/notification-records/detail`、`GET /payment/notification-records/statuses`、`POST /payment/notification-records/retry`、`POST /payment/notification-records/deliver-due`
- 权限：`payment:notification-record:list`、`payment:notification-record:query`、`payment:notification-record:retry`、`payment:notification-record:deliver-due`
- 数据：`payment_notification_record`、`payment_operation_audit`
- 部署形态：本地单体后端 + Mango Admin 单体前端

## 2. 执行环境

- 前端地址：`http://127.0.0.1:7808`
- 后端地址：`http://127.0.0.1:18118`
- 数据库或租户：`mango_dev_e397cd`，租户 `1`
- 测试账号：`admin`
- 浏览器：Playwright Chromium

## 3. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| PAY-MENU-012 | `/payment/notification-records/page` | 按关键字和状态查询通知记录 | `NT-E2E-*`、`NT-UI-E2E-*` | 返回通知单号、关联订单号、通知类型、通知状态、目标地址、重试次数、响应码、响应信息和时间字段 | 普通单号、订单号、目标地址为纯文本；通知类型和通知状态使用 `ElTag`；行操作无删除按钮 | 接口业务成功，无 console error、pageerror、通知记录接口 requestfailed | `test-results/payment-notification-records.png` | DONE |
| PAY-MENU-012 | `/payment/notification-records/detail` | 查询通知记录详情 | `/payment/notification-records/page` 返回的真实通知记录 ID | 返回通知信息、响应与重试、人工补偿、时间信息；不存在时使用 `PaymentCode.PAYMENT_NOTIFICATION_RECORD_NOT_FOUND` | 详情抽屉按通知信息、响应与重试、人工补偿、时间信息分组，无字段重叠 | 接口业务成功，无 console error、pageerror、通知记录接口 requestfailed | `test-results/payment-notification-records.png` | DONE |
| PAY-MENU-012 | `/payment/notification-records/statuses` | 查询通知状态契约 | 真实登录态 | 状态包含 `SUCCESS/RETRYING/FAILED/PENDING` | 状态筛选下拉从接口加载，不使用页面最终静态数据 | 接口业务成功 | `PLAYWRIGHT... --grep "通知记录"` | DONE |
| PAY-MENU-012 | `/payment/notification-records/retry` | 人工补偿重推失败通知 | `NT-E2E-*`、`NT-UI-E2E-*` | `FAILED` 通知重推后变为 `RETRYING`；重试次数加 1；记录重推原因、结果、重推人和响应信息；`SUCCESS` 通知重推返回 `3780` | 重推弹窗必填校验可用，提交后列表刷新为重试中 | 接口业务成功，无 console error、pageerror、通知记录接口 requestfailed | `test-results/payment-notification-records.png` | DONE |
| PAY-MENU-012 | `/payment/operation-audits/page` | 重推操作审计 | `RETRY_NOTIFICATION_RECORD` | 写入 `RETRY_NOTIFICATION_RECORD / PAYMENT_NOTIFICATION_RECORD / SUCCESS`，资源 ID 为通知单号 | 操作审计可通过接口查询 | E2E 使用真实登录态通过 | `PLAYWRIGHT... --grep "通知记录"` | DONE |
| PAY-SCHED-001 / PAY-OPS-001 | `/payment/notification-records/deliver-due` | 人工触发投递到期通知记录 | `NT-DUE-UI-E2E-*`，当前租户 `PENDING/RETRYING/FAILED` 且 `next_retry_time <= now` 的通知记录 | 接口按 `limit=1-100` 调用真实通知投递服务；返回本次投递数；写入 `DELIVER_DUE_NOTIFICATION_RECORDS / PAYMENT_NOTIFICATION_RECORD / SUCCESS` 审计；不改变资金状态 | 通知记录页工具栏提供“投递到期通知”按钮；点击后弹出确认框，明确“不修改资金状态”；确认后刷新列表并显示投递结果 | 单测、Controller 权限反射和页面 E2E 通过，无 console error、pageerror、通知记录接口 requestfailed | `PaymentReadonlyResourceServiceTest.deliverDueNotificationRecords_dispatchesAndRecordsAudit`; `PaymentReadonlyResourceControllerTest.notificationDeliverDueEndpoint_usesIndependentPermission`; `test-results/payment-notification-records.png` | DONE |
| PAY-MENU-012 | 权限迁移 | 查询、重推和到期投递权限入库 | `authorization/V48__payment_notification_record_operation_permissions.sql`、`authorization/V55__payment_notification_deliver_due_permission.sql` | `payment:notification-record:query`、`payment:notification-record:retry`、`payment:notification-record:deliver-due` 绑定 ROLE_ADMIN；列表接口使用 `payment:notification-record:list` | 后台登录后可进入页面、打开详情、人工重推失败通知并人工触发到期投递 | E2E 使用真实登录态通过；到期投递权限本轮用反射测试验证 | `PLAYWRIGHT... --grep "通知记录列表、详情和人工重推真实可用"`；`PaymentReadonlyResourceControllerTest.notificationDeliverDueEndpoint_usesIndependentPermission` | DONE |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| 支付中心 | 通知记录 | 页面可通过支付中心 / 对账结算 / 通知记录进入 | 查询、状态筛选、详情抽屉、重推弹窗、到期投递确认框、空态真实可用 | Element Plus 表单、表格、分页、抽屉、弹窗布局稳定；普通业务字段不加边框；到期投递为工具栏按钮，不挤占行操作 | `test-results/payment-notification-records.png` | DONE |
| 支付中心 | 通知记录 | 通知重试策略耗尽记录可回显 | 按“通知失败”筛选可看到策略耗尽记录 | 耗尽记录保留“重推”人工补偿按钮，普通单号和订单号仍为纯文本 | `test-results/payment-notification-retry-policy.png` | DONE |

## 5. 验证命令

```bash
mvn -pl mango-platform/mango-payment/mango-payment-core,mango-platform/mango-payment/mango-payment-starter -am -Dtest=PaymentReadonlyResourceServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core,mango-platform/mango-payment/mango-payment-starter -am -Dtest=PaymentReadonlyResourceServiceTest,PaymentReadonlyResourceControllerTest,PaymentNotificationServiceTest,PaymentNotificationDispatchSchedulerTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
./scripts/dev-workspace.sh backend
curl -fsS http://127.0.0.1:18118/actuator/health
mysql --protocol=TCP -h127.0.0.1 -P3306 -uroot mango_dev_e397cd -e "SELECT version, script, success FROM flyway_schema_history_authorization WHERE script='V55__payment_notification_deliver_due_permission.sql' ORDER BY installed_rank DESC LIMIT 1; SELECT id, menu_name, menu_code, permissions, del_flag FROM authorization_menu WHERE id=281103 OR permissions='payment:notification-record:deliver-due'; SELECT COUNT(*) AS admin_role_binding_count FROM authorization_role_menu rm JOIN authorization_role r ON r.id=rm.role_id WHERE rm.menu_id=281103 AND r.role_code='ROLE_ADMIN';"
pnpm -F mango-admin build
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1 --grep "通知记录列表、详情和人工重推真实可用"
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1 --grep "通知记录"
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm --dir mango-ui/apps/mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --grep "通知失败按应用重试策略推进"
node mango-pmo/tools/acceptance-evidence-check.mjs --evidence mango-docs/plans/evidence/payment-notification-record-acceptance.md
```

## 6. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 外部通道回调通知全量验收 | 本证据覆盖通知记录后台查询、详情、人工重推登记、权限、审计、ACK 失败按应用策略重试和耗尽后人工补偿入口；外部通道回调通知入口仍随 `PAY-API-010` 继续验收 | 不能据此声明支付和退款通知业务方的所有外部通道入口已完成 | 后续继续按 `PAY-API-010` 补外部通道回调通知验收 | 不适用 |
| 人工重推真实 HTTP 投递 | 当前人工重推在通知记录中登记补偿重推并进入 `RETRYING`，等待通知任务执行；本轮补齐的 `/payment/notification-records/deliver-due` 可受控触发到期记录投递并记录审计，但没有在“重推”接口内直接发起外部 HTTP 投递 | 不能据此声明人工重推已完成外部业务系统 ACK 回收 | 后续随 `PAY-API-010` 通知任务执行器接入真实投递、ACK 和长周期调度验收 | 不适用 |
| 到期投递批量压力和长周期验收 | 本轮已完成页面人工触发、真实投递、审计和列表回显；尚未覆盖跨租户长时间运行、调度频率、告警和批量压力 | 不能据此声明调度平台投产运维能力完整完成 | 后续随 `PAY-SCHED-001` 做跨租户长周期调度、批量压力和告警验收 | 不适用 |
