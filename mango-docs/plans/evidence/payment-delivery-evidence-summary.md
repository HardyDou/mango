# 支付平台交付证据汇总

## 1. 汇总范围

本文件汇总 PR #149 支付平台分支中原 `mango-docs/plans/evidence/*.md` 的分散验收记录，避免在 PR 中提交大量碎片化证据文件和截图资产。详细可复核入口以正式测试、E2E、设计文档和交付台账为准。

## 2. 正式验证入口

- 后端支付模块测试：`mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am test`
- 支付 starter 编译/测试：`mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-starter -am test`
- 前端支付中心 E2E：`mango-ui/apps/mango-admin/e2e/specs/payment-center.spec.ts`
- 支付设计文档：`mango-docs/designs/统一支付系统设计说明书.md`
- 支付交付台账：`mango-docs/plans/2026-05-25-payment-delivery-ledger.md`
- 富友/收银台最终验收摘要：`mango-docs/evidence/2026-06-12-payment-fuiou-cashier-e2e.md`

## 3. 已合并的分散证据文件

- `mango-docs/plans/evidence/2026-06-09-payment-ui-normalization.md`：支付中心页面 UI 精细化调整验收证据
- `mango-docs/plans/evidence/payment-application-delete-acceptance.md`：Payment 应用管理删除验收证据
- `mango-docs/plans/evidence/payment-business-order-acceptance.md`：业务订单列表页验收证据
- `mango-docs/plans/evidence/payment-cashier-config-acceptance.md`：收银台配置与 Web/H5 收银台验收证据
- `mango-docs/plans/evidence/payment-cashier-delay-acceptance.md`：支付收银台支付结果延迟验收证据
- `mango-docs/plans/evidence/payment-channel-acceptance.md`：支付通道管理验收证据
- `mango-docs/plans/evidence/payment-channel-certificate-security-acceptance.md`：通道证书安全验收证据
- `mango-docs/plans/evidence/payment-channel-contract-acceptance.md`：通道签约配置验收证据
- `mango-docs/plans/evidence/payment-core-data-model-acceptance.md`：支付核心数据模型验收证据
- `mango-docs/plans/evidence/payment-data-unique-constraints-acceptance.md`：支付关键唯一约束验收证据
- `mango-docs/plans/evidence/payment-difference-acceptance.md`：差异处理验收证据
- `mango-docs/plans/evidence/payment-domain-boundary-acceptance.md`：支付域边界验收证据
- `mango-docs/plans/evidence/payment-e2e-acceptance-2026-06-09.md`：支付中心 E2E 验收证据
- `mango-docs/plans/evidence/payment-enterprise-subject-acceptance.md`：Payment 企业主体管理验收证据
- `mango-docs/plans/evidence/payment-exception-order-acceptance.md`：异常订单列表页验收证据
- `mango-docs/plans/evidence/payment-full-e2e-acceptance.md`：支付中心全量 E2E 验收证据
- `mango-docs/plans/evidence/payment-integration-test-acceptance.md`：支付全链路集成测试验收证据
- `mango-docs/plans/evidence/payment-list-ui-acceptance.md`：支付列表普通字段展示验收证据
- `mango-docs/plans/evidence/payment-mango-pay-acceptance.md`：芒果支付内置虚拟通道验收证据
- `mango-docs/plans/evidence/payment-mango-pay-normalization-acceptance.md`：芒果支付运行态归一化验收记录
- `mango-docs/plans/evidence/payment-method-acceptance.md`：支付方式管理验收证据
- `mango-docs/plans/evidence/payment-method-route-acceptance.md`：支付方式路由策略验收证据
- `mango-docs/plans/evidence/payment-money-rule-acceptance.md`：支付金额规则验收证据
- `mango-docs/plans/evidence/payment-notification-record-acceptance.md`：通知记录列表页验收证据
- `mango-docs/plans/evidence/payment-observability-acceptance.md`：支付可观测性验收证据
- `mango-docs/plans/evidence/payment-offline-collection-design-acceptance.md`：线下收款独立通道设计确认与交付证据
- `mango-docs/plans/evidence/payment-openapi-acceptance.md`：支付开放接口订单、支付、退款、凭证与通知验收证据
- `mango-docs/plans/evidence/payment-operation-audit-acceptance.md`：操作审计验收证据
- `mango-docs/plans/evidence/payment-ops-acceptance.md`：支付后台人工操作验收证据
- `mango-docs/plans/evidence/payment-ops-control-acceptance.md`：支付后台人工操作受控验收证据
- `mango-docs/plans/evidence/payment-order-acceptance.md`：支付订单列表页验收证据
- `mango-docs/plans/evidence/payment-production-blockers-acceptance.md`：支付投产阻塞与用户确认清单
- `mango-docs/plans/evidence/payment-reconciliation-acceptance.md`：对账管理验收证据
- `mango-docs/plans/evidence/payment-refund-order-acceptance.md`：退款订单列表页验收证据
- `mango-docs/plans/evidence/payment-remaining-ledger-review.md`：支付中心剩余交付项复核证据
- `mango-docs/plans/evidence/payment-scheduler-acceptance.md`：支付任务触发验收证据
- `mango-docs/plans/evidence/payment-sensitive-field-acceptance.md`：支付敏感字段安全验收证据
- `mango-docs/plans/evidence/payment-sensitive-log-acceptance.md`：支付敏感日志验收证据
- `mango-docs/plans/evidence/payment-settlement-summary-acceptance.md`：结算汇总验收证据
- `mango-docs/plans/evidence/payment-tenant-isolation-acceptance.md`：支付核心业务表租户隔离验收证据
- `mango-docs/plans/evidence/payment-transaction-flow-acceptance.md`：交易流水列表页验收证据
- `mango-docs/plans/evidence/payment-unit-test-acceptance.md`：支付核心单元测试验收证据
- `mango-docs/plans/evidence/payment-user-confirmation-request.md`：支付中心用户确认单
- `mango-docs/plans/evidence/payment-user-stories-acceptance.md`：支付方式与业务接入用户故事验收证据

## 4. 资产收敛说明

- 不随 PR 提交 `plans/evidence` 下的截图、trace、临时报告等大体积资产。
- 不随 PR 提交富友原始 PDF/XLS、demo 工程、测试密钥或第三方原始资料。
- 不随 PR 提交一次性本地归一化脚本；稳定后应进入正式系统能力或正式 migration。
- 需要复核具体页面和链路时，以正式 E2E 用例、后端测试和交付台账为准。

## 5. 风险

- 本汇总文件只收敛证据索引，不替代真实测试执行结果。
- 外部真实通道联调仍以交付台账中的 IN_PROGRESS/BLOCKED 状态为准。

## 6. 外部通道阻塞项

| 台账 ID | 状态 | 说明 |
|---|---|---|
| PAY-CHANNEL-003 | BLOCKED | 通联等外部通道真实适配、验签、查单、退款、账单解析和联调证据未在本 PR 完整关闭。 |
| PAY-CHANNEL-005 | BLOCKED | 华夏银行等外部银行通道真实适配、网银支付、退款和回调联调证据未在本 PR 完整关闭。 |
| PAY-CHANNEL-007 | BLOCKED | 微信/支付宝等外部扫码真实路由和商户能力仍需机构确认并复测。 |
| PAY-CHANNEL-008 | BLOCKED | 连连及其它外部通道真实联调未纳入本 PR 完整关闭。 |
