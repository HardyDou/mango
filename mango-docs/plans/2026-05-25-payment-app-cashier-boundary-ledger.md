# 支付应用管理与收银台管理边界交付契约

## 1. 目标

按 `mango-docs/designs/统一支付系统设计说明书.md` 和用户最新确认，完成支付应用管理与收银台管理的职责边界收敛。

## 2. 范围

- 应用管理只维护应用身份、通信配置、接入安全和示例应用标记。
- 收银台管理维护所属应用、默认标记、基础展示配置、允许企业主体、允许支付方式、默认方式、展示顺序、结果跳转和退款规则。
- 数据库移除应用和收银台旧边界字段。
- 后台表单使用结构化控件，不直接让用户填写收银台展示 JSON。
- 收银台 Logo 通过文件中心上传组件保存文件 ID。

## 3. 不做什么

- 不把企业主体、支付方式、退款规则放回应用管理。
- 不在收银台配置中维护终端范围、超时、二维码刷新、轮询间隔、支付结果文案、网银银行列表或线下转账账户。
- 不提供应用密钥重置按钮或接口。
- 不交付全支付模块其它菜单的完成状态。

## 4. 设计输入

- `mango-docs/designs/统一支付系统设计说明书.md`
- 用户确认的应用管理与收银台配置边界。
- `mango-pmo/rules/01-delivery-contract.md`

## 5. 设计说明

### 5.1 影响模块

- 后端：`mango/mango-platform/mango-payment`
- 前端：`mango-ui/packages/payment`
- E2E：`mango-ui/apps/mango-admin/e2e/specs/payment-center.spec.ts`
- 文档：`mango-docs/designs`、`mango-docs/plans`

### 5.2 接口变化

- 应用保存接口不接收企业主体范围、支付方式范围、退款规则、时间戳窗口或 nonce 窗口。
- 收银台保存接口接收多企业主体、支付方式范围、默认方式、展示顺序、结果跳转和退款规则。
- 应用密钥不通过详情接口明文返回。

### 5.3 数据变化

- 新增支付 migration 清理应用旧权限字段和收银台旧终端、超时、结果、网银、线下转账配置字段。
- 报文加密关闭时应用不生成、不保存密钥状态。
- 默认收银台唯一性只约束启用状态的默认收银台。

### 5.4 菜单/页面/权限变化

- 应用管理表单保留基础信息、请求安全、回调与跳转。
- 收银台配置表单保留基础信息、展示主体、支付方式和业务规则。
- 收银台配置列表保留“收银台”行操作，支付页不作为后台菜单。

### 5.5 测试范围

- 后端编译。
- 前端生产构建。
- 支付中心 E2E 中应用管理、收银台配置和收银台入口相关场景。
- 交付台账检查。

## 6. 风险与限制

- 本台账只声明应用管理和收银台管理边界完成，不声明全支付模块完成。
- E2E 依赖本地后端、前端、数据库和菜单 migration 正常启动。

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| PAY-APP-CASHIER-001 | 设计文档 6.1 | 应用管理只维护应用身份、通信配置和接入安全 | 应用不维护企业主体范围、支付方式范围、默认方式、展示顺序、结果跳转或退款规则 | `SavePaymentApplicationCommand`、`PaymentApplicationServiceImpl`、应用管理页面 | 后端编译、前端构建、E2E 应用断言 | DONE | `mango/mango-platform/mango-payment/mango-payment-api/src/main/java/io/mango/payment/api/command/SavePaymentApplicationCommand.java` |
| PAY-APP-CASHIER-002 | 设计文档 6.1 | 报文加密关闭时不生成应用密钥 | 密钥状态与报文加密开关绑定，关闭时清空密钥状态，开启时生成密钥并配置签名算法 | `PaymentApplicationServiceImpl`、E2E 断言 | 后端编译、E2E 接口断言 | DONE | `mango/mango-platform/mango-payment/mango-payment-core/src/main/java/io/mango/payment/core/service/impl/PaymentApplicationServiceImpl.java` |
| PAY-APP-CASHIER-003 | 用户确认 | 管理后台不提供重置密钥按钮或接口 | 删除重置密钥 API 和页面入口，密钥轮换走受控运维流程 | 应用 API、应用页面、E2E 断言 | 前端构建、E2E 按钮不存在断言 | DONE | `mango-ui/apps/mango-admin/e2e/specs/payment-center.spec.ts` |
| PAY-APP-CASHIER-004 | 设计文档 6.6 | 收银台配置管理所属应用、默认标记、展示配置、企业主体、支付方式、跳转和退款规则 | 收银台保存接口和页面承载这些规则；一个应用可有多个收银台，一个收银台只属于一个应用 | `SavePaymentCashierConfigCommand`、`PaymentCashierConfigServiceImpl`、收银台配置页面 | 后端编译、前端构建、E2E 收银台配置断言 | DONE | `mango-ui/packages/payment/src/views/cashier-configs/index.vue` |
| PAY-APP-CASHIER-005 | 用户确认 | 收银台表单不直接编辑 JSON 字符串 | 展示配置拆成 Logo、标题、辅助说明、帮助文案结构化字段，保存时由页面组装协议字段 | 收银台配置页面 | 前端构建、代码审查 | DONE | `mango-ui/packages/payment/src/views/cashier-configs/index.vue` |
| PAY-APP-CASHIER-006 | PMO 文件规则 | Logo 保存文件中心 ID | 使用 `MUpload` 组件上传 Logo，业务字段只保存文件 ID | 收银台配置页面、文件组件依赖 | 前端构建、代码审查 | DONE | `mango-ui/packages/payment/src/views/cashier-configs/index.vue` |
| PAY-APP-CASHIER-007 | 用户确认 | 收银台不配置终端、超时、轮询、二维码刷新和结果文案 | Web/H5 支持由组件代码决定；超时、轮询、结果文案使用平台统一配置 | 收银台配置页面、收银台服务、设计文档 | 后端编译、前端构建、残留扫描 | DONE | `mango-docs/designs/统一支付系统设计说明书.md` |
| PAY-APP-CASHIER-008 | 用户确认 | 网银与线下转账信息不属于收银台配置 | 银行列表、跳转参数、收款账户和认款说明归属支付方式、企业主体和通道签约能力 | 收银台配置页面、设计文档 | 前端构建、残留扫描 | DONE | `mango-docs/designs/统一支付系统设计说明书.md` |
| PAY-APP-CASHIER-009 | 数据库规范 | 数据库移除应用和收银台旧边界字段 | 新增 V9 migration 删除应用权限旧字段和收银台旧配置字段，重建应用索引 | Flyway migration | 后端编译、SQL 审查 | DONE | `mango/mango-platform/mango-payment/mango-payment-core/src/main/resources/db/migration/payment/V9__payment_app_cashier_remove_legacy_columns.sql` |
| PAY-APP-CASHIER-010 | 设计文档 6.6 | 默认收银台只在启用状态下生效 | 默认唯一性只检查启用状态默认收银台，停用配置不阻断新默认配置 | `PaymentCashierConfigServiceImpl` | 后端编译、代码审查 | DONE | `mango/mango-platform/mango-payment/mango-payment-core/src/main/java/io/mango/payment/core/service/impl/PaymentCashierConfigServiceImpl.java` |
| PAY-APP-CASHIER-011 | 用户确认 | 应用不使用业务可自定义编码，改为平台生成 AppId | 创建应用时服务端生成 AppId，表单不提供编码输入，列表和接口展示 AppId；应用名称负责人工可读展示 | 后端 API、实体、服务、迁移、应用管理页面、E2E | 后端编译、前端构建、E2E AppId 断言、残留扫描 | DONE | `mango/mango-platform/mango-payment/mango-payment-core/src/main/resources/db/migration/payment/V10__payment_app_id_cashier_code_alignment.sql` |
| PAY-APP-CASHIER-012 | 用户确认 | 收银台不使用业务可传自定义编码 | 收银台保存接口、VO、实体和页面移除收银台编码；后台预览只使用内部 id 路由；普通业务支付使用应用默认收银台 | 后端 API、实体、服务、迁移、收银台配置页面、E2E | 后端编译、前端构建、E2E 无配置编码断言、残留扫描 | DONE | `mango-ui/packages/payment/src/views/cashier-configs/index.vue` |
