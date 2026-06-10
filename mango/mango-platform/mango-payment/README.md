# mango-payment

`mango-payment` 是支付中心模块，负责支付应用、收银台、支付方式、支付通道、通道签约、业务订单、支付订单、退款、流水、通知、对账、结算和线下支付通道能力。

## 模块结构

- `mango-payment-api`：支付业务码、枚举、Command、Query、VO、模块 API。
- `mango-payment-core`：支付领域实体、Mapper、Service、通道适配器、Flyway 迁移和业务状态流。
- `mango-payment-starter`：自动配置、Web Controller、Swagger/Knife4j 模块声明。
- `mango-payment-starter-remote`：远程调用适配。

## 编号规范

支付中心所有平台自生成编号统一通过 `mango-numgen` 生成。业务 Service 不再自行拼接 `时间戳 + 随机数`，也不在 Controller、Service 或前端写死编号。

### 通用格式

```text
业务前缀 + yyyyMMdd + 8 位日内递增序号
```

示例：

```text
PO2026060900000001
```

约束：

- `yyyyMMdd` 日期片段参与 `numgen` 流水分组，保证同一租户、同一 `genKey` 每日从 `00000001` 开始递增。
- 租户隔离使用 `numgen` 自身 `tenant_id`。
- 历史编号不回刷；规范生效后新产生的数据使用 `numgen`。
- 平台编号只表达支付中心内部对象身份，不替代外部通道单号。

### 编号清单

| 业务对象 | 字段 | genKey | 前缀 | 生成责任 |
| --- | --- | --- | --- | --- |
| 业务订单 | `biz_order_no` | `PAY_BIZ_ORDER_NO` | `BO` | 后台或内部创建时由 payment 生成；开放接口传入时由业务方负责 |
| 支付订单 | `pay_order_no` | `PAY_ORDER_NO` | `PO` | payment 生成 |
| 退款订单 | `refund_order_no` | `PAY_REFUND_ORDER_NO` | `RO` | payment 生成 |
| 业务退款号 | `biz_refund_no` | `PAY_BIZ_REFUND_NO` | `BR` | 后台或审批发起且未传入时由 payment 生成；开放接口传入时由业务方负责 |
| 退款审批 | `approval_no` | `PAY_REFUND_APPROVAL_NO` | `RA` | 后台退款审批生成 |
| 支付流水 | `flow_no` | `PAY_FLOW_NO` | `PF` | payment 支付状态流或资金流水生成 |
| 退款流水 | `flow_no` | `PAY_REFUND_FLOW_NO` | `RF` | payment 退款状态流生成 |
| 手续费流水 | `flow_no` | `PAY_FEE_FLOW_NO` | `FF` | 对账或手续费处理生成 |
| 调账流水 | `flow_no` | `PAY_ADJUST_FLOW_NO` | `AF` | 后台调账生成 |
| 通知记录 | `notification_no` | `PAY_NOTIFY_NO` | `NT` | payment 通知生成 |
| 对账批次 | `reconciliation_no` / `batch_no` | `PAY_RECON_BATCH_NO` | `RC` | 对账导入生成 |
| 对账差异 | `difference_no` | `PAY_DIFF_NO` | `DF` | 对账差异生成 |
| 支付查单记录 | `query_no` | `PAY_QUERY_NO` | `PQ` | 支付主动查单生成 |
| 退款查单记录 | `query_no` | `PAY_REFUND_QUERY_NO` | `RQ` | 退款主动查单生成 |
| 异常订单 | `exception_no` | `PAY_EXCEPTION_NO` | `EX` | 重复支付或异常处理生成 |
| 线下收款单 | `offline_collection_no` | `PAY_OFFLINE_COLLECTION_NO` | `OC` | 线下收款通道生成 |
| 线下退款单 | `offline_refund_no` | `PAY_OFFLINE_REFUND_NO` | `OF` | 线下退款通道生成 |
| 线下银行流水导入批次 | `batch_no` | `PAY_OFFLINE_BANK_BATCH_NO` | `OB` | 线下收款通道生成 |
| 芒果支付虚拟付款单 | `virtual_payment_no` | `PAY_MANGO_VIRTUAL_NO` | `MP` | 芒果支付通道生成 |
| 芒果支付场景控制单 | `control_no` | `PAY_MANGO_SCENARIO_NO` | `SC` | 芒果支付通道管理生成 |

### 不由平台编号生成的字段

- `channel_trade_no`：外部通道交易号，以通道返回为准。
- `channel_refund_no`：外部通道退款号，以通道返回为准。
- 线下转账备注识别码：用于用户转账备注和批量对账匹配，保持短码语义，不作为订单号。字符集为 `0-9a-zA-Z`，长度按业务配置或通道规则控制。

内置通道例外：

- 芒果支付作为内置虚拟通道时，`virtual_payment_no` 同时可作为该通道侧交易号。
- 线下收款作为内置线下支付通道时，线下收款单号由 `PAY_OFFLINE_COLLECTION_NO` 生成；银行流水号仍以导入流水中的真实银行流水号为准。

### numgen 登记口径

支付中心接入 `mango-numgen` 时，需要登记的是 `numgen_generator` 编号生成器及其生效规则，不新增新的“编码分发器”领域对象。

支付中心编号统一登记到 `numgen_business_domain` 的 `PAYMENT` 支付域下；非支付模块继续使用各自业务域或默认 `GENERAL` 通用域。

每个 `genKey` 应登记一套生效规则：

```text
TEXT(业务前缀) + DATE(yyyyMMdd, sequence_scope=1) + SEQ(width=8, pad=0)
```

支付业务代码只允许通过 payment 内部编号服务按语义方法取号，例如 `nextPayOrderNo()`、`nextRefundOrderNo()`，不允许在业务 Service 中直接散写 `genKey` 或前缀。
