# 收银台配置与 Web/H5 收银台验收证据

## 范围

- 页面：`/#/payment/cashier-configs`、`/#/payment/cashier-configs/:cashierId/cashier`。
- 接口：`/api/payment/cashier-configs/*`、`/api/payment/cashier/session`、`/api/payment/cashier/pay`、`/api/payment/cashier/pay-result`。
- 数据库：`payment_cashier_config`、`payment_order.cashier_config_id`、`payment_virtual_channel_payment.cashier_config_id`、`payment_operation_audit`。
- 前端组件：`PaymentCashierConfigView`、`PaymentCashierView`。

## 验收结论

| ID | 要求 | 证据 | 状态 |
|---|---|---|---|
| PAY-MENU-006A | 收银台配置列表页管理所属应用、默认标记、基础展示配置、允许企业主体、支付方式、默认方式、展示顺序、结果跳转和退款规则；Logo 通过文件中心保存 ID；行操作按配置渲染收银台 | E2E 覆盖列表查询、新增配置、Logo 上传保存、配置回显、行内“收银台”弹窗、真实支付成功；后端接口使用权限、`Require`、`PaymentCode`、审计和删除保护 | DONE |
| PAY-MENU-017 | 不设置独立“租户收银台”菜单 | E2E 查询后端菜单树，断言不存在“租户收银台” | DONE |
| PAY-CASHIER-001 | 收银台封装为一套 Web/H5 可复用组件 | 后台弹窗预览和非菜单路由均复用 `PaymentCashierView`；E2E 同时覆盖两种入口 | DONE |
| PAY-CASHIER-002 | 收银台行操作按配置渲染 Web/H5 收银台页面 | E2E 点击列表行“收银台”，断言接口返回配置 `350001`、订单、支付方式排序和页面标题 | DONE |
| PAY-CASHIER-003 | Web/PC 收银台支持扫码、网银/银联、线下转账物料 | E2E 在桌面视口选择扫码支付、企业网银、对公转账；真实 `/pay` 返回 `QR`、`HTML_FORM`、`TRANSFER_ACCOUNT` 物料 | DONE |
| PAY-CASHIER-004 | H5 收银台支持 H5 跳转支付和移动 Web 展示 | E2E 切换 390x844 视口，选择快捷账户，真实 `/pay` 返回 `H5_PARAM` 和跳转地址 | DONE |
| PAY-CASHIER-005 | 本期不交付 App 收银台页面 | 设计文档已明确非本期；E2E 菜单树断言不存在“App 收银台” | DONE |
| PAY-CASHIER-006 | 本期不交付小程序收银台页面 | 设计文档已明确非本期；E2E 菜单树断言不存在“小程序收银台” | DONE |
| PAY-CASHIER-007 | 支付结果页属于收银台流程，不作为后台菜单 | `PaymentCashierView` 内部展示成功/处理中/失败/关闭结果；E2E 菜单树断言不存在“支付结果页” | DONE |

## 真实实现检查

| 层面 | 证据 |
|---|---|
| 实体 | `PaymentCashierConfig` 继承 `AuditableEntity`，使用 `created_at/updated_at/created_by/updated_by` 审计字段 |
| 数据库 | `V22__payment_cashier_config_audit_delete_guard.sql` 已应用；`payment_order` 增加 `cashier_config_id` 并建立索引 |
| 权限 | `PaymentCashierConfigController` 使用 `payment:cashier-config:list/query/add/edit/delete` 权限；`V42__payment_cashier_config_operation_permissions.sql` 已应用 |
| Service | `PaymentCashierConfigServiceImpl` 使用 `Require` 和 `PaymentCode` 校验应用、企业主体、可见支付方式、默认方式、展示顺序、状态、退款开关和默认收银台唯一性 |
| 删除保护 | 删除前检查 `payment_order`、`payment_virtual_channel_payment` 是否引用当前收银台配置；有历史支付数据返回 `3764` 并记录拒绝审计 |
| 审计 | 新增、编辑、删除成功、删除拒绝均写入 `payment_operation_audit`，资源类型 `PAYMENT_CASHIER_CONFIG` |
| 前端 | 收银台配置页面调用真实接口；普通业务字段纯文本展示，状态使用语义 tag；Logo 使用文件中心上传组件保存文件 ID |
| 收银台 | 页面按真实配置展示标题、订单、金额、主体、分类、支付方式、默认项和支付结果；终态成功后隐藏二维码和确认支付按钮 |
| 持久化规范 | `PaymentCashierConfigServiceImpl`、`PaymentCashierServiceImpl`、`MangoPayVirtualPaymentService` 不再直接使用 `JdbcTemplate`；删除关系、收银台订单读取、支付结果、路由匹配、支付订单/流水/内置虚拟通道记录写入均通过 MyBatis-Plus Mapper 和 XML |
| 内置虚拟通道记录 | `payment_virtual_channel_payment` 通过 `V39__payment_virtual_channel_payment_audit_columns.sql` 对齐 `created_at/updated_at/created_by/updated_by`，实体 `PaymentVirtualChannelPayment` 继承 `AuditableEntity` |

## 验证命令

```bash
mvn -pl mango-platform/mango-payment/mango-payment-core,mango-platform/mango-payment/mango-payment-starter -am -Dtest=PaymentCashierConfigServiceImplTest,PaymentMethodServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
mvn -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentEnterpriseSubjectServiceImplTest,PaymentMethodServiceImplTest,PaymentCashierConfigServiceImplTest,PaymentCashierServiceImplTest,MangoPayVirtualPaymentServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
mvn -pl mango-platform/mango-payment/mango-payment-core,mango-platform/mango-payment/mango-payment-starter -am test -DskipTests=false
pnpm -F mango-admin build
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1
git diff --check
```

## 本次验证结果

- 后端专项测试：11 个测试通过。
- 2026-06-06 持久化规范化专项测试：16 个测试通过，覆盖收银台配置删除保护、收银台支付订单/流水写入、内置虚拟通道记录写入、支付方式删除保护、企业主体删除保护。
- payment-core/starter 全量 Maven 测试：通过。
- 前端构建：通过。
- 支付中心 E2E：10 个用例全部通过。
- Flyway：`flyway_schema_history_payment` 已到 `V22`，`flyway_schema_history_authorization` 已到 `V42`。

## 未覆盖风险

- 本证据只证明后台收银台配置和 Web/H5 收银台交互可用，不代表开放接口签名、防重放、全通道路由试算、外部真实支付通道、退款、对账、差异处理和结算汇总已完成。
- 芒果支付通道返回的网银表单、H5 参数和线下转账信息已走真实后端生成；外部通道适配器的真实字段仍需在后续通道接入任务中逐通道验证。
