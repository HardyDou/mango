# 支付通道管理验收证据

## 1. 验收范围

- 页面：`/#/payment/channels`、`/#/payment/operation-audits`
- 接口：`/api/payment/channels/page`、`/api/payment/channels/detail`、`/api/payment/channels`、`/api/payment/channel-capabilities/page`、`/api/payment/operation-audits/page`
- 数据：`payment_channel`、`payment_channel_capability`、`payment_operation_audit`
- 部署形态：单体管理后台，前端 `http://127.0.0.1:7808`，后端 `http://127.0.0.1:18118`

## 2. 执行环境

- 数据库或租户：`mango_dev_e397cd` / tenant `1`
- 测试账号：`admin`
- 浏览器：Playwright Chromium
- 说明：E2E 使用真实后端、真实数据库和真实页面操作，未使用接口拦截、mock 数据或前端临时数据。

## 3. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| PAY-MENU-005 | `/#/payment/channels`、`/api/payment/channels` | 支付通道产品定义新增、编辑、详情回读和删除 | E2E 动态通道 `支付E2E通道*`，通道编码优先使用未占用的 `LIANLIAN_PAY/WECHAT_PAY/ALIPAY` | 通道保存后详情包含 `channelCode/channelType/adapterType/gatewayBaseUrl/fieldTemplateJson/status`；接口响应不包含主体专属 `merchantNo/appId`；编辑后写 `UPDATE_CHANNEL` 审计；删除后列表和详情查询不可见 | 列表普通字段为纯文本，只有状态和通道类型使用语义 tag；字段模板为结构化编辑器，不使用原始 JSON 文本框替代 | Playwright Chromium 走真实网络请求，`POST/PUT/DELETE /api/payment/channels` 均通过业务断言 | `mango-ui/apps/mango-admin/e2e/specs/payment-center.spec.ts`；`.mango/logs/backend.log` | DONE |
| PAY-CHANNEL-CAP-001 | `/#/payment/channels`、`/api/payment/channel-capabilities/page` | 通道能力子表维护 | `PERSONAL_WECHAT_QR`、`WEB`、`MANGO_PAY`、限额 `10` 到 `990000` 分，退款/查单/关单/账单/对账均启用 | 保存后 `payment_channel_capability` 只读接口可查到能力；能力包含标准支付方式、终端、接入场景、能力开关、限额和状态；删除通道时物理删除未引用的能力行；被签约或路由引用的能力在服务层拒删 | 能力明细在同一编辑弹窗内维护，支付方式来源于真实支付方式接口，未手写临时选项；金额输入以分为单位 | 后端单测覆盖能力保存、模板校验、被引用能力拒删、删除子能力和审计；完整 payment-center E2E 7 个用例通过 | `PaymentChannelServiceImplTest`；`payment-center.spec.ts` | DONE |
| PAY-MENU-005 | `PaymentChannelServiceImpl`、`PaymentChannelMapper.xml`、`PaymentChannelCapabilityMapper.xml`、`V38__payment_channel_audit_columns.sql` | 支付通道后端规范化 | E2E 动态通道、被引用和未引用通道能力 | `PaymentChannel` 继承 `AuditableEntity`；`payment_channel` 审计列迁移到 `created_by/created_at/updated_by/updated_at`；通道删除关系检查通过 `PaymentChannelMapper.countDeleteRelations`；通道能力删除关系检查通过 `PaymentChannelCapabilityMapper.countDeleteRelations`；只读通道能力分页通过 typed Mapper 返回 `PaymentChannelCapabilityVO`；业务校验继续使用 `Require` 和 `PaymentCode` | 无新增前端交互；通道和通道能力列表普通字段仍为纯文本，状态/类型使用 tag | 后端聚焦单测 32 个通过；前端构建通过；支付中心组合 E2E 4 个通过 | `mvn -pl mango-platform/mango-payment/mango-payment-core,mango-platform/mango-payment/mango-payment-starter -am -Dtest=PaymentChannelServiceImplTest,PaymentChannelContractServiceImplTest,PaymentReadonlyResourceServiceTest,PaymentApplicationServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false`；`pnpm -F mango-admin build`；`PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1 --grep "应用管理\|支付通道\|通道签约"` | DONE |

## 4. 验证命令

```bash
mvn -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=MoneyTest,PaymentChannelServiceImplTest test -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false
mvn -pl mango-platform/mango-payment/mango-payment-core,mango-platform/mango-payment/mango-payment-starter -am -Dtest=PaymentChannelServiceImplTest,PaymentChannelContractServiceImplTest,PaymentReadonlyResourceServiceTest,PaymentApplicationServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
pnpm -F mango-admin build
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1 --grep "支付通道管理真实维护"
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1 --grep "应用管理|支付通道|通道签约"
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1
```

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 外部通道真实联调 | 通联、华夏、微信、支付宝、连连的外部机构联调属于 PAY-CHANNEL-003/后续通道接入范围，本轮只完成通道产品定义和能力声明管理 | 不能声明外部通道支付、退款、查单、账单适配已完成 | 后续按通道接入台账逐通道交付适配器和联调证据 | 无 |
| 支付方式管理和路由配置页面 | 本轮完成通道能力声明，不包含支付方式路由策略页面 | 不能声明 PAY-MENU-006、PAY-ROUTE-001 完成 | 下一步按菜单顺序进入支付方式管理和路由策略 | 无 |
