# 支付收银台支付结果延迟验收证据

## 范围

- 页面：`/#/payment/cashier-configs/:cashierId/cashier`、`/#/payment/cashier-configs` 行内收银台预览。
- 接口：`/api/payment/cashier/session`、`/api/payment/cashier/pay`、`/api/payment/cashier/pay-result`。
- 数据：本地库 `mango_dev_e397cd`，收银台配置 `350001`，芒果支付内置虚拟通道签约 `331001`，E2E 动态业务订单 `PAY-E2E-*`、`PAY-DELAY-E2E-*`。

## 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| PAY-CASHIER-DELAY-001 | 用户要求 | 支付成功后页面要考虑支付结果延迟到达 | 支付提交返回支付订单号和当前状态；非终态按支付订单号轮询真实结果接口 | `PaymentCashierServiceImpl.pay`、`PaymentCashierServiceImpl.payResult`、`PaymentCashierView` | E2E 构造 `PROCESSING` 支付单，轮询后由真实 DB 状态变更为 `SUCCESS` | DONE | `mango-ui/apps/mango-admin/e2e/specs/payment-center.spec.ts` |
| PAY-CASHIER-DELAY-002 | 设计文档 6.6 | 支付结果展示属于收银台流程，不作为后台菜单 | 支付结果区展示 `SUCCESS`、`PROCESSING`、`FAILED`、`CLOSED`，非终态保留重新查询能力 | `mango-ui/packages/payment/src/views/cashier/index.vue` | Playwright 验证收银台非菜单路由展示订单、支付方式、处理中和成功结果 | DONE | `mango-ui/apps/mango-admin/e2e/specs/payment-center.spec.ts` |
| PAY-CASHIER-DELAY-003 | 用户要求 | 不能用前端假数据或临时数据证明投产链路 | E2E 使用真实数据库准备业务订单和通道签约场景，页面调用真实后端接口；不拦截 `/api/payment/cashier/*` 响应 | `payment-center.spec.ts`、本地 Flyway 数据 | `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1` | DONE | 本文件 |
| PAY-CASHIER-DELAY-004 | PMO 验证要求 | 页面必须完成真实 UI 走查 | E2E 覆盖 PC 横向支付分类、支付二维码、处理中提示、终态成功后隐藏二维码和确认支付按钮 | `PaymentCashierView` | Playwright 10 个支付中心用例全部通过；收银台布局未发现控件遮挡或结果区与支付物料混淆 | DONE | 本文件 |

## 验证命令

```bash
cd /Users/hardy/Work/mango/.claude/worktrees/payment-platform/mango/mango-platform/mango-payment
mvn test -DskipTests

cd /Users/hardy/Work/mango/.claude/worktrees/payment-platform/mango-ui
pnpm -F mango-admin build

PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true \
PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 \
PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 \
pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1 --grep "收银台支付结果延迟返回"

PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true \
PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 \
PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 \
pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1

git diff --check
```

## 结果

- 支付中心 E2E：10 个用例全部通过。
- 延迟结果链路：支付提交返回 `PROCESSING`，页面展示“支付处理中，等待结果返回”和二维码；数据库写入成功状态后，页面通过 `/payment/cashier/pay-result` 轮询到 `SUCCESS` 并隐藏二维码。
- UI 走查：收银台 PC 分类横向展示，结果区和支付操作区分离；终态成功后不再展示继续付款物料。

## 风险

- 外部支付通道回调、主动查单适配器仍需在后续通道接入任务中逐通道验证；本次已保证收银台页面和统一结果查询接口能承接延迟状态。
