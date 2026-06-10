# 对账管理验收证据

## 1. 验收范围

- 页面：支付中心 / 对账结算 / 对账管理
- 接口：`GET /payment/reconciliations/page`、`GET /payment/reconciliations/detail`、`GET /payment/reconciliations/statuses`、`POST /payment/reconciliations/import`
- 权限：`payment:reconciliation:list`、`payment:reconciliation:query`、`payment:reconciliation:import`
- 数据：`payment_reconciliation`、`payment_channel_bill_detail`、`payment_difference`、`payment_operation_audit`
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
| PAY-MENU-013 | `/payment/reconciliations/page` | 对账批次列表和空态查询 | `RC-BIZ-E2E-*`、`sha256-recon-e2e-*`、`NO-RECON-*` | 按批次号查询返回通道、日期、笔数、金额、手续费、文件摘要、导入人和导入时间；不存在关键字返回空态 | Element Plus 表单、表格、分页布局稳定；普通业务字段为纯文本；状态字段使用 `ElTag`；行操作只有详情，没有删除按钮 | console error、pageerror、对账接口 requestfailed 监听数组为空 | `mango-ui/apps/mango-admin/test-results/payment-reconciliations.png` | DONE |
| PAY-MENU-013 | `/payment/reconciliations/detail` | 对账批次详情和账单明细 | `/payment/reconciliations/page` 返回的真实批次 ID | 返回批次号、状态、通道、账单日期、文件名、文件摘要、导入人、金额、手续费、对账结果和明细；明细包含通道交易号、交易类型、金额、手续费、匹配订单和匹配说明 | 详情抽屉按批次信息和账单明细分区；普通字段纯文本，状态和类型使用 `ElTag`；抽屉内容没有字段重叠 | console error、pageerror、对账接口 requestfailed 监听数组为空 | `PLAYWRIGHT... --grep "对账管理"` | DONE |
| PAY-MENU-013 | `/payment/reconciliations/statuses` | 对账状态契约 | 真实登录态 | 状态包含 `IMPORTED`、`MATCHED`、`DIFFERENCE`；状态筛选下拉从接口加载 | 状态筛选可选择和清空，不使用页面最终静态状态数组 | 接口业务成功，状态数据来自后端契约 | `PLAYWRIGHT... --grep "对账管理"` | DONE |
| PAY-RECON-001 | `/payment/reconciliations/import` | 通道账单导入和批次明细入库 | `MANGO_PAY`、`special-recon-*.csv`、真实支付订单通道交易号 | 导入后生成对账批次，`totalCount=1`，金额和手续费按分单位返回；账单明细入库并匹配真实支付订单；匹配结果为 `MATCHED / 已平账` | 导入弹窗可填写通道编码、账单日期、文件名、文件摘要和明细；日期时间使用后端可反序列化格式 `YYYY-MM-DD HH:mm:ss` | 导入接口业务成功，页面显示“账单已导入”并刷新列表 | `PLAYWRIGHT... --grep "对账管理"` | DONE |
| PAY-RECON-001 | `/payment/reconciliations/import` | 同通道、日期、文件摘要重复导入拒绝 | `sha256-recon-e2e-*` 重复提交 | 第二次导入返回业务码 `3783`；原批次保留，未伪造成功 | 页面导入使用同一接口，不提供跳过重复校验的前端路径 | 接口返回业务错误码，未出现 requestfailed | `PLAYWRIGHT... --grep "对账管理"` | DONE |
| PAY-RECON-001 | `/payment/operation-audits/page` | 对账导入操作审计 | `IMPORT_RECONCILIATION` | 导入成功写入 `IMPORT_RECONCILIATION / PAYMENT_RECONCILIATION / SUCCESS`，资源 ID 为对账批次号 | 操作审计通过真实接口查询 | E2E 使用真实登录态查询通过 | `PLAYWRIGHT... --grep "对账管理"` | DONE |
| PAY-RECON-001 | 权限和数据库迁移 | 菜单、权限、批次表、明细表和审计字段入库 | Flyway `payment/V33`、`authorization/V49` | `payment_reconciliation.file_digest/created_at` 存在；`payment_channel_bill_detail` 存在；`payment_difference.created_at` 存在；权限菜单 `281201/281202` 存在 | 登录后可进入支付中心 / 对账结算 / 对账管理并执行导入、查询和详情 | Flyway 实库执行到 `payment V33`、`authorization V49` | `.mango/dev-workspace.env` 对应数据库 `mango_dev_e397cd` | DONE |
| PAY-RECON-002 | `/payment/reconciliations/import`、`/payment/differences/page` | 通道成功但本地无支付订单差异识别 | `CH-MISSING-*` | 通道账单明细无法匹配本地支付订单时生成 `payment_difference`，差异类型 `CHANNEL_SUCCESS_LOCAL_MISSING`，处理状态 `PENDING`，差异金额按分单位返回 | 对账批次状态显示为 `DIFFERENCE / 存在差异`，差异处理列表可查到差异单 | 接口业务成功，差异单来自真实数据库 | `PLAYWRIGHT... --grep "对账管理"` | DONE |
| PAY-RECON-002 | `PaymentReconciliationService.importReconciliation` | 本地成功但通道账单缺失差异识别 | 本地成功支付 `PO-MISSING`、本地成功退款 `RO-MISSING`，账单仅包含 `CASHIER-PO001` | 导入账单后追加查询本地当天成功支付和成功退款；账单交易号集合未包含的本地成功支付生成 `LOCAL_SUCCESS_CHANNEL_MISSING`，本地成功退款生成 `LOCAL_REFUND_CHANNEL_MISSING`；差异金额分别为支付金额和退款金额，处理状态 `PENDING` | 后端服务测试，不涉及页面布局 | Maven Surefire 通过，差异由服务写入 `payment_difference` mapper | `PaymentReconciliationServiceTest.importReconciliation_localSuccessfulOrdersMissingFromBill_createsDifferences` | DONE |
| PAY-DOMAIN-001 / PAY-DOMAIN-002 / PAY-DOMAIN-003 | `PaymentReconciliationService.importReconciliation` | 对账账单作为通道依据补偿推进状态机 | 本地支付订单 `PO-COMP` 为 `PAYING` 且通道账单 `PAYMENT` 金额一致；本地退款订单 `RO-COMP` 为 `REFUNDING` 且通道账单 `REFUND` 金额一致；金额不一致支付订单 `PO-MISMATCH` | 对账导入匹配到本地 `PAYING` 支付订单且金额一致时，通过 `PaymentOrderMapper.updatePayingQueryResult` CAS 推进 `PAYING -> SUCCESS`，写 `PAY_SUCCESS` 流水，推进业务订单成功并写支付/业务订单状态流；匹配到本地 `REFUNDING/PROCESSING` 退款订单且金额一致时，通过 `PaymentRefundOrderMapper.updateRefundingQueryResult` CAS 推进 `REFUNDING -> SUCCESS`，更新业务订单退款进度，写 `REFUND_SUCCESS` 流水并写退款/业务订单状态流；金额不一致时只生成差异，不执行补偿推进 | 后端服务测试；详情页通过状态流来源显示“对账补偿” | 状态机/对账聚焦测试 45 个通过 | `PaymentReconciliationServiceTest.importReconciliation_payingPaymentBillMatched_compensatesSuccessState`、`PaymentReconciliationServiceTest.importReconciliation_refundingBillMatched_compensatesSuccessState`、`PaymentReconciliationServiceTest.importReconciliation_payingPaymentAmountMismatch_createsDifferenceOnly` | DONE |
| PAY-RECON-002 | `PaymentOrderMapper.selectSuccessfulChannelOrdersMissingInBill`、`PaymentRefundOrderMapper.selectSuccessfulChannelRefundsMissingInBill` | 本地成功缺账单的数据库级查询 | H2 MySQL 模式最小真实表：同日成功且未在账单集合中的支付/退款、同日已匹配、失败、次日、其他通道数据 | MyBatis 加载生产 `PaymentOrderMapper.xml`、`PaymentRefundOrderMapper.xml` 执行真实 XML SQL；仅返回同租户、同通道、同账单日、成功状态且不在账单交易号集合中的支付 `PO-MISSING` 和退款 `RO-MISSING`；不返回失败、其他日期、其他通道和已匹配数据 | 后端 mapper 集成测试，不涉及页面布局 | Maven Surefire 通过，验证生产 mapper XML 可执行；日期上界由 Java 传入 `nextBillDate`，SQL 不依赖数据库方言函数 | `PaymentReconciliationMapperIntegrationTest.selectSuccessfulChannelRecordsMissingInBill_returnsOnlyMissingRows` | DONE |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| 支付中心 | 对账管理 | 页面可通过支付中心 / 对账结算 / 对账管理进入 | 查询、状态筛选、导入弹窗、详情抽屉、空态真实可用 | Element Plus 后台布局稳定；普通列表字段不加边框；状态和类型使用标签；搜索区、表格、分页没有重叠 | `mango-ui/apps/mango-admin/test-results/payment-reconciliations.png` | DONE |

## 5. 验证命令

```bash
mvn -pl mango-platform/mango-payment/mango-payment-core,mango-platform/mango-payment/mango-payment-starter -am -Dtest=PaymentReadonlyResourceServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentReconciliationServiceTest,PaymentReconciliationMapperIntegrationTest,PaymentMangoPayChannelAdapterTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentReconciliationServiceTest,PaymentChannelOrderQueryServiceTest,PaymentChannelRefundQueryServiceTest,PaymentChannelCallbackServiceTest,PaymentDuplicatePaymentServiceTest,PaymentOrderStatusFlowServiceTest,PaymentOrderStateServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
pnpm -F mango-admin build
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1 --grep "对账管理"
node mango-pmo/tools/acceptance-evidence-check.mjs --evidence mango-docs/plans/evidence/payment-reconciliation-acceptance.md
```

## 6. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 外部通道账单获取源和解析 | 本轮覆盖统一手动导入、芒果支付账单生成和差异识别；通联、华夏、微信、支付宝、连连等外部机构账单的 FTP/FTPS 拉取、HTTP 接口按时间区间加分页或游标拉取、文件解析和接口响应解析属于对应外部通道交付项 | 不影响 `PAY-RECON-002` 对账差异识别在统一导入模型内完成；不能据此声明外部通道账单获取、外部通道解析或外部通道接入完成 | 后续按 `PAY-CHANNEL-003/005/007/008` 和 `PAY-RECON-004` 在具备联调资料后逐通道验证真实账单获取源、解析和对账 | 不适用 |
| 差异处理闭环 | 本轮只生成差异单，不覆盖查单、补单、退款、忽略、关闭等受控处理动作 | 不能据此声明 `PAY-MENU-014` 或 `PAY-RECON-003` 完成 | 后续进入差异处理列表页和受控处理接口开发 | 不适用 |
