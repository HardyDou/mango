# 支付列表普通字段展示验收证据

## 1. 范围

本证据只覆盖 `PAY-UI-001`：支付中心列表普通业务字段使用纯文本展示，不使用边框、框框或标签包裹。

不覆盖接口业务能力、数据库模型、支付通道联调、退款、对账、结算和完整投产验收。

## 2. 交付物

| 类型 | 交付物 |
|---|---|
| 前端页面 | `mango-ui/packages/payment/src/views/methods/index.vue`、`channels/index.vue`、`channel-contracts/index.vue`、`transaction-flows/index.vue`、`exception-orders/index.vue`、`notification-records/index.vue`、`differences/index.vue`、`operation-audits/index.vue`、`reconciliations/index.vue`、`methods/PaymentMethodRoutePanel.vue` |
| E2E | `mango-ui/apps/mango-admin/e2e/specs/payment-center.spec.ts` |
| 截图 | `mango-docs/plans/evidence/payment-list-ui/payment-list-ui-no-tag.png` |

## 3. 已验证能力

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| PAY-UI-001 | `/#/payment/methods` | 支付方式列表普通字段纯文本展示 | `PERSONAL_WECHAT_QR` | 一级“对私”、二级“微信”、三级“扫码”不在 `.el-tag` 内；行内仅状态使用 Tag | 搜索、列表、路由策略弹窗均可见 | 无 console error/pageerror/requestfailed | `payment-list-ui-no-tag.png` | DONE |
| PAY-UI-001 | `/#/payment/channels`、`/#/payment/channel-contracts` | 通道和签约普通字段纯文本展示 | `MANGO_PAY`、`MANGO_PAY_MANGO_TECH` | “芒果支付”作为通道类型、接入场景时不在 `.el-tag` 内；状态仍使用 Tag | 列表查询和表格行展示正常 | 无 console error/pageerror/requestfailed | `payment-list-ui-no-tag.png` | DONE |
| PAY-UI-001 | `/#/payment/transaction-flows`、`/#/payment/exception-orders`、`/#/payment/notification-records`、`/#/payment/differences` | 手写列表普通类型字段纯文本展示 | 真实接口返回的首条流水、异常、通知、差异数据 | 流水类型、异常类型、通知类型、差异类型不在 `.el-tag` 内 | 列表查询和行展示正常 | 无 console error/pageerror/requestfailed | `payment-list-ui-no-tag.png` | DONE |
| PAY-UI-001 | `/#/payment/operation-audits`、`/#/payment/reconciliations` | 审计和对账明细普通字段纯文本展示 | 真实操作审计、对账批次及账单明细 | 操作动作、资源类型、账单明细交易类型不在 `.el-tag` 内；操作结果、匹配状态仍可使用 Tag | 对账详情抽屉可打开并展示账单明细 | 无 console error/pageerror/requestfailed | `payment-list-ui-no-tag.png` | DONE |

## 4. 静态扫描结论

`mango-ui/packages/payment/src` 中剩余 `el-tag` 均为状态、级别、处理结果、支付结果等语义字段，或通用组件保留的可选 `variant: 'tag'` 能力。本轮已移除普通分类、类型、动作、资源、接入场景字段上的装饰性 Tag。

## 5. 验证命令

```bash
pnpm -F mango-admin exec playwright test --list e2e/specs/payment-center.spec.ts --grep "支付列表普通字段"
pnpm -F mango-admin build
curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:7808
curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:18118/actuator/health
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1 --grep "支付列表普通字段"
```

## 6. 实际验证结果

| 命令 | 结果 |
|---|---|
| Playwright list | 能识别 `支付列表普通字段纯文本展示，无边框标签包裹` 用例 |
| 前端构建 | build success；存在既有 Vite dynamic import chunk 警告 |
| 前端健康检查 | `200` |
| 后端健康检查 | `200` |
| 定向 Playwright E2E | 1 passed |

## 7. 未验证项和风险

1. 本轮只声明 `PAY-UI-001` 完成，不声明支付平台全量完成。
2. 本轮未改动后端接口和数据库。
3. `PaymentResourcePage` 仍保留 `variant: 'tag'` 能力，供语义字段按规范使用；本轮已清理支付中心普通字段列配置。
