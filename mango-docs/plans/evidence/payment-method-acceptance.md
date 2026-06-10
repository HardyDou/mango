# 支付方式管理验收证据

## 1. 范围

本证据覆盖支付方式管理本轮调整：三级分类字典、标准支付方式维护、分类校验、受控删除、操作审计、前端真实接口联动和 E2E 验证。

## 2. 分级结论

原先把“储蓄卡/信用卡/钱包”作为一级分类不清晰。支付方式分级已调整为：

| 层级 | 含义 | 当前编码 |
|---|---|---|
| 一级 | 账户或资金属性 | `PERSONAL`、`CORPORATE` |
| 二级 | 支付工具或支付网络 | `WECHAT`、`ALIPAY`、`UNIONPAY`、`BANK_CARD`、`WALLET`、`EBANK`、`OFFLINE_TRANSFER` |
| 三级 | 交互或产品形态 | `QR_CODE`、`H5_REDIRECT`、`MINIAPP`、`DEBIT_QUICK`、`CREDIT_QUICK`、`WALLET_QUICK`、`BANK_GATEWAY`、`ACCOUNT_TRANSFER` |

对应标准支付方式示例：

| 标准支付方式 | 一级 | 二级 | 三级 |
|---|---|---|---|
| `PERSONAL_DEBIT_QUICK` | `PERSONAL` | `BANK_CARD` | `DEBIT_QUICK` |
| `PERSONAL_CREDIT_QUICK` | `PERSONAL` | `BANK_CARD` | `CREDIT_QUICK` |
| `PERSONAL_WALLET_QUICK` | `PERSONAL` | `WALLET` | `WALLET_QUICK` |
| `CORPORATE_OFFLINE_ACCOUNT` | `CORPORATE` | `OFFLINE_TRANSFER` | `ACCOUNT_TRANSFER` |

`BANK_CARD`、`WALLET` 是标准支付方式二级分类；储蓄卡、信用卡、钱包快捷等差异由三级 `DEBIT_QUICK`、`CREDIT_QUICK`、`WALLET_QUICK` 承载。收银台展示可以按二级工具分组，再用三级区分具体产品形态。

## 3. 交付物

| 类型 | 交付物 |
|---|---|
| 数据库 | `mango/mango-platform/mango-payment/mango-payment-core/src/main/resources/db/migration/payment/V21__payment_method_category_audit.sql`、`mango/mango-platform/mango-payment/mango-payment-core/src/main/resources/db/migration/payment/V25__payment_method_restore_standard_category_hierarchy.sql` |
| 后端 API | `mango/mango-platform/mango-payment/mango-payment-api/src/main/java/io/mango/payment/api/PaymentMethodApi.java` |
| 后端 Service | `mango/mango-platform/mango-payment/mango-payment-core/src/main/java/io/mango/payment/core/service/impl/PaymentMethodServiceImpl.java` |
| 后端实体 | `PaymentMethod`、`PaymentMethodCategory` 均继承 `AuditableEntity` |
| 前端页面 | `mango-ui/packages/payment/src/views/methods/index.vue` |
| E2E | `mango-ui/apps/mango-admin/e2e/specs/payment-center.spec.ts` |
| 单元测试 | `mango/mango-platform/mango-payment/mango-payment-core/src/test/java/io/mango/payment/core/service/impl/PaymentMethodServiceImplTest.java` |

## 4. 已验证能力

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| PAY-MENU-006 | `/payment/methods/categories` | 三级分类树查询 | 数据库 `payment_method_category` 标准分类 | 返回 `PERSONAL/CORPORATE` 一级，`BANK_CARD/WALLET` 等二级，`DEBIT_QUICK/CREDIT_QUICK/WALLET_QUICK` 等三级 | 页面级联选择来自接口，未使用静态分类数组 | E2E 真实登录态请求成功，未发现支付方式接口 requestfailed | `payment-center.spec.ts` 支付方式用例 | DONE |
| PAY-METHOD-DICT-001 | `payment_method` 种子数据 | 标准支付方式分类纠正 | `PERSONAL_DEBIT_QUICK`、`PERSONAL_CREDIT_QUICK`、`PERSONAL_WALLET_QUICK` | 数据为 `PERSONAL/BANK_CARD/DEBIT_QUICK`、`PERSONAL/BANK_CARD/CREDIT_QUICK`、`PERSONAL/WALLET/WALLET_QUICK` | 列表分类字段作为语义分类展示，普通字段不使用边框样式 | 数据库和 E2E 断言通过 | `PaymentMethodServiceImplTest`、`payment-center.spec.ts` | DONE |
| PAY-MENU-006 | `/payment/methods` | 新增和编辑支付方式 | `PERSONAL_DEBIT_QUICK_TEST` | 后端使用 `Require` 和 `PaymentCode` 校验分类路径、终端、支付物料、金额范围和重复编码 | 表单按业务字段分组，分类级联和支付物料选择可回显 | 接口业务成功，重复编码返回业务错误 | `PaymentMethodServiceImplTest`、`payment-center.spec.ts` | DONE |
| PAY-MENU-006 | `/payment/methods/delete` | 删除保护和审计 | 有通道能力或订单引用的支付方式、无引用测试支付方式 | 有引用返回 `PAYMENT_METHOD_DELETE_HAS_RELATIONS(3759)`；成功删除和拒绝删除均写 `payment_operation_audit` | 前端危险删除使用二次确认，失败原因可见 | 删除接口返回明确业务错误或成功结果 | `PaymentMethodServiceImplTest`、`payment-center.spec.ts` | DONE |
| PAY-MENU-006 | `/payment/methods/page` | 支付方式管理页面真实接口联动 | 真实支付方式列表和分类树 | 列表、查询、新增、详情、删除均通过真实 API 和数据库验证 | 方式编码、名称、终端、支付物料、金额为普通文本；状态和分类字段使用标签 | E2E 检查无 console error、pageerror、支付方式接口 requestfailed | `payment-center.spec.ts` | DONE |

| 能力 | 证据 |
|---|---|
| 三级分类来自数据库 | `/api/payment/methods/categories` 返回 `PERSONAL/CORPORATE` 根节点和下级分类树 |
| 种子数据纠正 | `PERSONAL_DEBIT_QUICK` 为 `PERSONAL / BANK_CARD / DEBIT_QUICK`，`PERSONAL_CREDIT_QUICK` 为 `PERSONAL / BANK_CARD / CREDIT_QUICK`，`PERSONAL_WALLET_QUICK` 为 `PERSONAL / WALLET / WALLET_QUICK`；旧 `DEBIT_CARD / CREDIT_CARD / QUICK_PAY` 分类不再作为标准配置口径 |
| 后端前置校验 | `PaymentMethodServiceImpl` 使用 `Require` 和 `PaymentCode` 校验分类路径、终端范围、支付物料、状态、金额范围和重复编码 |
| 禁止支付方式直接绑定通道 | `PaymentMethodServiceImpl.copy` 强制 `channelId = null`，`PaymentMethodVO` 不返回 `channelId` |
| 删除保护 | 删除前检查通道能力、签约能力、路由规则、收银台配置、支付订单和芒果支付付款记录引用；有引用返回 `PAYMENT_METHOD_DELETE_HAS_RELATIONS(3759)` |
| 持久化规范 | 删除关系统计已下沉到 `PaymentMethodMapper.countDeleteRelations` 和 `PaymentMethodMapper.xml`，Service 不再直接使用 `JdbcTemplate` 或手写 SQL |
| 操作审计 | 创建、更新、删除成功和删除拒绝均记录 `payment_operation_audit` |
| 前端真实接口 | 页面分类级联数据来自 `/payment/methods/categories`，保存时提交标准三级分类编码；图标只提交文件中心 ID |
| UI 展示 | 方式编码、方式名称、终端范围、支付物料和金额为普通文本；分类字段作为语义分类使用 Tag |
| E2E | 覆盖分类树、种子纠正、新增、详情、重复编码、引用删除拒绝、无引用删除成功、审计记录 |

## 5. 验证命令

```bash
mvn -pl mango-platform/mango-payment/mango-payment-core -am -DskipTests compile
mvn -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentMethodServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
mvn -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentEnterpriseSubjectServiceImplTest,PaymentMethodServiceImplTest,PaymentCashierConfigServiceImplTest,PaymentCashierServiceImplTest,MangoPayVirtualPaymentServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
pnpm -F mango-admin build
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1
mvn -pl mango-platform/mango-payment/mango-payment-core,mango-platform/mango-payment/mango-payment-starter -am test -DskipTests=false
```

## 6. 结论

支付方式三级分类调整已按设计文档落到数据库、接口、Service、页面和 E2E。当前完成范围仅限支付方式管理及其分类模型，不代表支付平台所有开放接口、路由试算、芒果支付完整支付链路和对账结算已全部完成。
