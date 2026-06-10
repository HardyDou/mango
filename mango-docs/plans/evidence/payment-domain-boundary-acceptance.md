# 支付域边界验收证据

## 1. 交付项

- 台账项：`PAY-SCOPE-001`
- 设计来源：`统一支付系统设计说明书.md` 第 1、2 节
- 验收目标：支付系统只提供支付域能力，不承载保函业务模型、钱包账户、财务总账或保证金/担保金业务；业务系统通过开放接口接入支付域。

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| PAY-SCOPE-001 | Maven 依赖 | 支付模块不依赖非支付业务模块 | `mango-payment` 下所有 `pom.xml` | 不出现 `mango-guarantee`、`mango-bond`、`mango-deposit`、`mango-wallet`、`mango-ledger`、`mango-accounting`、`mango-escrow` 等非支付域模块依赖 | 后端架构边界项，不涉及 UI | `PaymentDomainBoundaryContractTest` 通过 | 本文件第 3、5 节 | DONE |
| PAY-SCOPE-001 | Java 源码 | 支付源码不导入非支付业务包 | `mango-payment` 下 Java 源码 | 不导入 `io.mango.guarantee`、`io.mango.deposit`、`io.mango.wallet`、`io.mango.ledger`、`io.mango.accounting` 等非支付域包 | 后端架构边界项，不涉及 UI | `PaymentDomainBoundaryContractTest` 通过 | 本文件第 4、5 节 | DONE |
| PAY-SCOPE-001 | Java/SQL 模型 | 支付模块不声明保函、保证金、钱包账户、总账等业务模型 | `mango-payment` 下 Java 和 SQL | 不声明 `Guarantee`、`Deposit`、`Margin`、`WalletAccount`、`WalletBalance`、`GeneralLedger`、`AccountingVoucher`、`VirtualAccount`、`CustodialAccount` 等模型或表 | 后端架构边界项，不涉及 UI | `PaymentDomainBoundaryContractTest` 通过 | 本文件第 5 节 | DONE |

## 2. 设计边界

设计文档第 1、2 节明确：

- 支付系统负责收款、退款、支付结果通知、查单、对账、资金核对和结算汇总能力。
- 支付系统不承载保函业务模型，不判断保函业务状态，不决定业务是否可退款，不决定出函流程。
- 支付系统不沉淀用户余额，不建设钱包账户，不建设财务总账，不替代财务系统，不自动付款。
- 支付系统不涉及保证金/担保金业务，不提供虚拟户、监管户、保证金缴退、冻结、释放、扣划等能力。

本轮验收仅收口支付模块边界，不新增业务功能、不调整通道能力、不修改页面。

## 3. 依赖检查

当前 `mango-payment` 的模块依赖集中在：

- `mango-payment-api`
- `mango-payment-core`
- `mango-payment-starter`
- `mango-payment-starter-remote`
- Mango 公共/基础设施能力：`mango-common`、`mango-infra-context`、`mango-infra-persistence`、`mango-infra-crypto`、`mango-infra-web`、`mango-infra-feign`
- 管理接口权限注解：`mango-authorization-api`

未发现保函、保证金、钱包账户、总账、会计、监管户等非支付业务模块依赖。

## 4. 关键词扫描说明

人工扫描命令：

```bash
rg -n "保函|担保|保证金|押金|钱包|余额|总账|会计|凭证|出函|拒保|撤单|冻结|释放|扣划|虚拟户|监管户|guarantee|bond|deposit|wallet|balance|ledger|accounting|voucher" mango/mango-platform/mango-payment -g"*.java" -g"*.xml" -g"*.sql" -g"*.yml" -g"pom.xml"
```

扫描结果里保留的允许项：

- `PaymentOpenReceiptVO` 和开放接口“支付凭证”：属于支付域交易凭证，不是财务总账凭证。
- 结算汇总接口描述中的“财务核对汇总，不触发自动付款或会计凭证”：用于明确不替代财务系统。
- 支付方式字典中的 `WALLET` / “钱包快捷”：表达外部钱包类支付方式，不是自建用户余额或钱包账户体系。

未发现保函业务状态、保证金生命周期、钱包账户余额、总账分录、虚拟户或监管户等支付域外模型。

## 5. 契约测试

新增测试：

- `PaymentDomainBoundaryContractTest`

覆盖内容：

- 扫描 `mango-payment` 模块 `pom.xml`，禁止依赖非支付业务模块。
- 扫描 Java import，禁止导入非支付业务包。
- 扫描 Java 类型和字段声明，禁止声明保函、保证金、钱包账户、总账、会计凭证、虚拟户、监管户等模型。
- 扫描 payment migration 创建表语句，禁止创建支付域外业务表。

执行命令：

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentDomainBoundaryContractTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
```

结果：

```text
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 6. 结论

`PAY-SCOPE-001` 当前验收结论为通过：

- 支付模块未依赖保函、保证金、钱包账户、总账、会计等非支付业务模块。
- 支付模块未声明设计禁止的非支付域业务模型或业务表。
- 支付凭证、财务核对、外部钱包支付方式属于设计允许的支付域表达，不构成越界。
- 边界已由 `PaymentDomainBoundaryContractTest` 固化为自动回归检查。

全量支付模块仍有其他 `IN_PROGRESS` 台账项，不能据此声明支付模块整体完成。
