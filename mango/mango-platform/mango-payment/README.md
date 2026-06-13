# mango-payment

## 1. 能力定位

`mango-payment` 是 Mango 支付中心后端模块，提供支付应用、企业主体、支付方式、支付通道、通道签约、收银台、业务订单、支付订单、退款、流水、通知、对账、结算、线下收款和线下退款能力。

## 2. 适用场景

- 平台需要统一接入多支付通道，并通过支付方式、通道能力和签约配置完成路由。
- 业务系统需要创建业务订单、拉起收银台、发起支付、退款、查单、查退款和关闭订单。
- 财务或运营需要查看流水、通知、对账批次、差异处理、结算汇总和操作审计。
- 线下收款、线下退款、银行流水导入和支付异常处理需要纳入同一支付域。

## 3. 不适用场景

- 不直接保存银行卡、证书明文、私钥明文或外部支付账号敏感资料。
- 不替代外部支付机构的商户开户、资质审核和通道开通流程。
- 不把业务域订单生命周期完全迁移到支付中心；业务域仍负责自己的商品、合同、履约和售后语义。
- 不依赖手工改库完成通道、菜单、权限或编号初始化。

## 4. 模块边界

- `mango-payment-api` 提供支付业务码、Command、Query、VO 和模块 API。
- `mango-payment-core` 提供领域模型、Mapper、Service、通道适配、Flyway 迁移、状态流和编号生成适配。
- `mango-payment-starter` 提供自动配置、Web Controller、调度入口和模块声明。
- `mango-payment-starter-remote` 提供远程调用适配。

支付中心依赖 `mango-numgen` 生成平台编号，依赖 `mango-authorization` 初始化菜单和权限，依赖 `mango-workflow` 支撑退款审批，依赖 `mango-file` 保存证书或凭据类文件。

## 5. 接入方式

Maven 依赖：

```xml
<dependency>
  <groupId>io.mango</groupId>
  <artifactId>mango-payment-starter</artifactId>
</dependency>
```

应用侧启用 starter 后，随模块加载 Flyway migration、Controller、通道适配器、通知调度和退款审批初始化能力。微服务场景按应用拓扑接入 `mango-payment-starter-remote`。

## 6. 配置项

支付中心运行配置由 starter 配置、数据库通道配置和签约配置共同决定：

- 通道、通道能力、签约字段模板、路由规则、菜单权限和编号规则通过 Flyway 初始化。
- 商户号、应用号、证书文件、密钥引用、网关地址和回调地址通过支付签约配置维护。
- 证书和文件类配置保存文件中心 ID，敏感字段需要加密存储并脱敏展示。

## 7. 对外接口 / 扩展点

模块 API 覆盖：

- `PaymentApplicationApi`
- `PaymentBusinessOrderApi`
- `PaymentCashierApi`
- `PaymentCashierConfigApi`
- `PaymentChannelApi`
- `PaymentChannelCallbackApi`
- `PaymentChannelContractApi`
- `PaymentMethodApi`
- `PaymentMethodRouteApi`
- `PaymentOrderApi`
- `PaymentRefundOrderApi`
- `PaymentRefundApprovalApi`
- `PaymentReconciliationApi`
- `PaymentDifferenceApi`
- `PaymentSettlementSummaryApi`
- `PaymentOfflineCollectionApi`
- `PaymentOfflineRefundApi`
- `PaymentNotificationRecordApi`
- `PaymentOperationAuditApi`
- `PaymentOpenApi`
- `PaymentSecurityApi`
- `PaymentTaskApi`
- `MangoPayVirtualPaymentApi`

扩展点包括通道支付适配器、通道回调适配器、账单获取或解析能力、支付方式路由规则、退款审批流程定义和通知调度。

## 8. 数据库 / 初始化数据

`mango-payment-core` 包含 `db/migration/payment` Flyway 脚本，用于初始化支付应用、通道、能力、签约、订单、退款、流水、通知、对账、结算、线下收款、线下退款、虚拟支付和相关约束。

关联初始化数据分布在：

- `mango-authorization`：支付中心菜单、页面资源和操作权限。
- `mango-domain`：`PAYMENT` 业务域。
- `mango-numgen`：支付订单、退款、流水、通知、对账和线下单据编号生成器。
- `mango-job`：支付通道账单获取任务。

## 9. 菜单 / 权限 / 租户

支付中心菜单和操作权限由 `mango-authorization` migration 初始化。后端接口按当前登录态、租户上下文、应用、主体、签约通道和权限码控制访问范围；前端页面 key 需要与 `@mango/payment` 注册的页面 key 保持一致。

## 10. 验证方式

后端单元和契约测试：

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-payment -am test
```

相关编号、权限、工作流和文件能力变更时，追加对应模块测试：

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-numgen,mango-platform/mango-authorization,mango-platform/mango-workflow,mango-platform/mango-file -am test
```

真实链路验收至少覆盖：创建业务订单、路由命中签约通道、拉起支付、接收回调、主动查单、发起退款、查退款、获取或导入账单、生成对账批次、识别差异、生成通知和审计记录。

## 11. 业务接入最小闭环

业务方先创建支付应用、企业主体、支付方式、通道和签约配置，再配置收银台和路由规则。业务下单时创建业务订单，前端拉起收银台，支付中心生成支付订单并调用通道，通道回调或主动查单推进支付状态，业务方通过通知记录或开放接口同步结果。

最小验收断言：业务订单可创建，收银台可展示可用支付方式，支付订单可进入通道，支付成功后业务订单和支付订单状态一致，退款可按权限发起并产生退款流水，对账能生成批次和差异结果。

## 12. 常见问题

- 支付方式不显示：检查支付方式状态、收银台配置、企业主体、签约能力和路由规则。
- 通道返回签名错误：检查签约字段、证书文件、回调地址、网关地址和适配器签名算法。
- 回调未推进状态：检查公网回调入口、通道回调路由、验签结果、幂等记录和统一状态流日志。
- 编号不连续：确认对应 `genKey`、日期分组和租户上下文是否符合 `mango-numgen` 规则。
- 对账无结果：检查账单获取任务、导入文件、通道交易号、支付流水和对账批次状态。

## 13. 关联 PMO 规则

- [后端代码规范](../../../mango-pmo/rules/backend/01-code.md)
- [模块菜单规范](../../../mango-pmo/rules/backend/11-module-menu.md)
- [AI 编码红线](../../../mango-pmo/rules/03-ai-coding-redlines.md)
- [AI 交付质量](../../../mango-pmo/rules/05-ai-delivery-quality.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [支付边界与台账](../../../mango-docs/plans/2026-05-25-payment-app-cashier-boundary-ledger.md)
- [支付交付台账](../../../mango-docs/plans/2026-05-25-payment-delivery-ledger.md)
- [支付生产就绪](../../../mango-docs/plans/2026-05-25-payment-production-readiness.md)
- [支付 Sprint 01](../../../mango-docs/plans/2026-05-25-payment-sprint-01.md)
- [支付交付证据汇总](../../../mango-docs/plans/evidence/payment-delivery-evidence-summary.md)
- [支付富友收银台 E2E 证据](../../../mango-docs/evidence/2026-06-12-payment-fuiou-cashier-e2e.md)
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)

## 15. 支付通道接入规约

接入新的外部支付通道时，长期代码和交付约束以 `mango-pmo` 为准，本节只说明支付模块接入步骤和交付物。

接入顺序：

1. 资料确认：确认通道产品、支付方式、终端类型、扫码模式、网银模式、退款、查单、关单、账单、对账、回调方式、测试账号和测试金额。
2. 能力建模：形成通道能力矩阵，明确支付方式、终端、物料类型、退款、查单、关单、账单和对账能力。
3. 签约字段建模：只把商户需要维护的接入资料放入字段模板，例如商户号、机构号、AppId、公钥、私钥、证书、API Key、网关地址和回调地址。
4. 数据固化：通过 Flyway 固化通道、能力、签约字段模板、默认测试签约配置、路由能力、权限和菜单数据。
5. 适配器实现：通道适配器负责发起支付、查单、退款、查退款、关单、账单获取或账单解析。
6. 统一回调：公网回调入口先进入统一通道路由，再由对应通道回调适配器完成验签和解析。
7. 统一结果推进：回调、主动查单和对账补偿只提交通道结果；状态、副作用、通知和审计由统一领域服务推进。
8. 异常可诊断：失败记录需要定位通道、租户、签约、订单、请求摘要、响应摘要和原始异常。
9. 真实验收：开放支付方式需要完成真实下单或官方测试金额验收，覆盖支付物料、回调、主动查单、退款、退款查单、账单和对账。
10. 证据归档：保存订单号、退款单号、通道交易号、对账批次、请求摘要、响应摘要、回调样本、日志摘要和页面截图。

通道能力矩阵模板：

| 支付方式 | 终端 | 物料类型 | 支付 | 查单 | 退款 | 查退款 | 关单 | 账单 | 对账 | 说明 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 例如 PERSONAL_WECHAT_QR | WEB | QR | 是 | 是 | 是 | 是 | 按通道文档 | 是 | 是 | 微信扫码主扫 |

不得声明完成的情况：

- 只有页面配置，没有真实通道接口调用。
- 只有适配器代码，没有 Flyway 固化通道、能力和字段模板。
- 只跑通支付，不验证退款、查单、回调和对账。
- 使用固定成功、mock、临时报文或手工改库替代真实链路。
- 回调、查单、退款或对账失败时无法从日志或记录定位根因。
- 配置表单要求商户理解协议实现细节，例如 XML、JSON、RSA、SM2、接口模式或内部接口路径。

## 16. 编号规范

支付中心所有平台自生成编号统一通过 `mango-numgen` 生成。业务 Service 不自行拼接 `时间戳 + 随机数`，也不在 Controller、Service 或前端写死编号。

通用格式：

```text
业务前缀 + yyyyMMdd + 8 位日内递增序号
```

编号清单：

| 业务对象 | 字段 | genKey | 前缀 | 生成责任 |
| --- | --- | --- | --- | --- |
| 业务订单 | `biz_order_no` | `PAY_BIZ_ORDER_NO` | `BO` | 后台或内部创建时由 payment 生成；开放接口传入时由业务方负责 |
| 支付订单 | `pay_order_no` | `PAY_ORDER_NO` | `PO` | payment 生成 |
| 退款订单 | `refund_order_no` | `PAY_REFUND_ORDER_NO` | `RO` | payment 生成 |
| 业务退款号 | `biz_refund_no` | `PAY_BIZ_REFUND_NO` | `BR` | 后台或审批发起且未传入时由 payment 生成；开放接口传入时由业务方负责 |
| 退款审批 | `approval_no` | `PAY_REFUND_APPROVAL_NO` | `RA` | 后台退款审批生成 |
| 支付流水 | `flow_no` | `PAY_FLOW_NO` | `PF` | payment 支付状态流或资金流水生成 |
| 退款流水 | `flow_no` | `PAY_REFUND_FLOW_NO` | `RF` | payment 退款状态流生成 |
| 通知记录 | `notification_no` | `PAY_NOTIFY_NO` | `NT` | payment 通知生成 |
| 对账批次 | `reconciliation_no` / `batch_no` | `PAY_RECON_BATCH_NO` | `RC` | 对账导入生成 |
| 对账差异 | `difference_no` | `PAY_DIFF_NO` | `DF` | 对账差异生成 |
| 异常订单 | `exception_no` | `PAY_EXCEPTION_NO` | `EX` | 重复支付或异常处理生成 |
| 线下收款单 | `offline_collection_no` | `PAY_OFFLINE_COLLECTION_NO` | `OC` | 线下收款通道生成 |
| 线下退款单 | `offline_refund_no` | `PAY_OFFLINE_REFUND_NO` | `OF` | 线下退款通道生成 |
| 芒果支付虚拟付款单 | `virtual_payment_no` | `PAY_MANGO_VIRTUAL_NO` | `MP` | 芒果支付通道生成 |

`channel_trade_no` 和 `channel_refund_no` 以外部通道返回为准，不由平台编号生成。
