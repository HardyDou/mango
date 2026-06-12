# 支付中心富友与收银台回归验收证据

## 1. 验收范围

- 页面：支付中心 Web 收银台、网银支付选择、线下转账通知单。
- 接口：`/api/payment/cashier/pay`、`/api/payment/channel-callbacks/fuiou_pay`、线下收款凭证提交、确认到账、退款、银行流水导入确认。
- 权限：富友公网回调入口无需登录态，后台管理接口使用 E2E 登录态。
- 数据：数据库 `mango_dev_e397cd`，租户 `1`，测试业务订单前缀 `PAY-EBANK-E2E`、`PAY-TRANSFER-E2E`、`PAY-OFFLINE-BANK`。
- 部署形态：本地单体后端 `18118`，前端 source mode `7775`。

## 2. 执行环境

- 前端地址：`http://127.0.0.1:7775/`、`http://10.6.0.2:7775/`。
- 后端地址：`http://127.0.0.1:18118`。
- 数据库或租户：`mango_dev_e397cd` / tenant `1`。
- 测试账号：E2E 登录工具内置后台账号。
- 浏览器：Playwright Chromium。

## 3. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| PAY-FUIOU-001 | Web 收银台 `/api/payment/cashier/pay` | 网银支付不再采集账号/户名，选择个人/企业网银和银行后生成富友 HTML_FORM | `PAY-EBANK-E2E-*`，收银台 `350901`，金额 `128800` 分 | 返回 `materialType=HTML_FORM`，`payOrderNo` 非空，HTML 表单包含支付订单号；后端不再报“请输入付款账号或卡号” | 默认选中个人网银和中国工商银行；切换企业网银后仍默认选中首个银行；页面不存在账号、户名、企业授权输入 | Playwright 等待 `/api/payment/cashier/pay` 响应并执行业务成功断言；后端单测 `PaymentCashierServiceImplTest` 通过 | `mango-ui/apps/mango-admin/playwright-report/index.html`；`.mango/logs/backend.log`；命令 `PLAYWRIGHT_BASE_URL=http://127.0.0.1:7775 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts -g "Web 收银台网银和线下转账物料真实返回" --project=chromium --reporter=list` | PASS |
| PAY-FUIOU-002 | 富友通道适配器 | 富友网关表单字段不暴露内部租户 ID，页面回跳和后台通知地址分离 | `PO2026061300000008`、富友签约 `FUIOU_PAY_MANGO_TECH` | `page_notify_url=https://douxy.inner.yunxinbaokeji.com:1443/#/payment/gateway-result`，`back_notify_url=https://douxy.inner.yunxinbaokeji.com:1443/api/payment/channel-callbacks/fuiou_pay`，`rem` 为空 | 收银台点击“打开网银支付”使用新 tab 目标名，支付页回跳通过 gateway result 页面通知原窗口同步状态 | 后端单测 `PaymentFuiouPayChannelAdapterTest` 覆盖字段断言并通过；数据库签约配置查询确认 URL 生效 | `.mango/logs/backend.log`；命令 `mvn -pl mango-platform/mango-payment/mango-payment-core -Dtest=PaymentFuiouPayChannelAdapterTest,PaymentFuiouPayConfigParserTest test` | PASS |
| PAY-CALLBACK-001 | `/api/payment/channel-callbacks/fuiou_pay` | 富友公网回调入口 POST/GET 放行，不依赖后台登录 token | 无 token POST 空请求 | 返回业务错误“富友网关回调缺少商户订单号”，HTTP 为 `200`，不是 `401`；说明已进入支付回调处理链路 | 无页面交互；这是公网回调接口验收 | `curl -i -sS -X POST http://127.0.0.1:18118/api/payment/channel-callbacks/fuiou_pay` 返回业务 JSON；后端日志有回调入口日志 | `.mango/logs/backend.log`；命令 `curl -i -sS -X POST 'http://127.0.0.1:18118/api/payment/channel-callbacks/fuiou_pay'` | PASS |
| PAY-OFFLINE-001 | Web 收银台、线下收款接口 | 线下转账通知单、凭证回传、财务确认、支付成功、流水生成闭环 | `PAY-TRANSFER-E2E-*`，收银台 `350902`，金额 `128800` 分 | 通道为 `OFFLINE_COLLECTION`，物料为 `TRANSFER_ACCOUNT`，转账备注匹配 `^[0-9A-Za-z]{4,6}$`；确认后支付订单 `SUCCESS`、业务订单 `PAID`、流水 `PAY_SUCCESS` | 只有一个线下方式时不展示多余层级；显示目标户主、目标账号、目标开户行、转账备注；按钮为“已完成转账，回传转账凭证” | Playwright 调用真实上传和确认接口；响应均执行业务断言；未使用支付成功 mock | `mango-ui/apps/mango-admin/playwright-report/index.html`；命令同 PAY-FUIOU-001 | PASS |
| PAY-OFFLINE-002 | 线下退款接口 | 线下部分退款、退款账户、退款凭证、统一退款流水 | `refundAmount=38800` 分，退款凭证文件 `offline-refund-voucher-*.txt` | 线下退款单状态 `REFUNDED`，退款订单号非空，退款流水 `REFUND_SUCCESS`，业务订单 refundedAmount 增加 `38800` 分 | 退款凭证通过文件中心上传，业务表只保存文件 ID | Playwright 通过后台接口提交真实文件和退款数据；响应按退款单、流水、业务订单金额断言 | `mango-ui/apps/mango-admin/playwright-report/index.html`；命令同 PAY-FUIOU-001 | PASS |
| PAY-OFFLINE-003 | 银行流水导入与批量确认 | 后端解析 Excel，按 4-6 位转账备注匹配线下收款，批量确认到账 | `PAY-OFFLINE-BANK-*`，Excel `offline-bank-statement-*.xlsx`，金额 `1288` 元 | 导入批次 `MATCHED`、`matchedCount=1`；确认后批次 `CONFIRMED`，线下收款 `RECONCILED`，支付订单 `SUCCESS`，业务订单 `PAID` | 该用例通过接口验证后台解析与匹配，不依赖前端本地解析 Excel | Playwright multipart 上传真实 Excel buffer 到后端导入接口；后端完成匹配与确认 | `mango-ui/apps/mango-admin/playwright-report/index.html`；命令同 PAY-FUIOU-001 | PASS |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| 支付中心 | Web 收银台 | 网银默认选择首个类型、首个银行 | 线下转账单据展示收款资料和 4-6 位备注 | 不显示多余支付方式层级，不显示账号/户名输入 | `mango-ui/apps/mango-admin/playwright-report/index.html` | PASS |
| 支付中心 | 富友回调入口 | POST/GET 映射进入统一回调路由 | 无 token 请求不被登录拦截 | 返回支付业务错误，不返回登录页或 401 | `.mango/logs/backend.log` | PASS |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 富友微信/支付宝扫码真实付款闭环 | 原因：当前真实返回存在“未找到路由”，数据库失败记录显示富友侧未给该商户路由到对应扫码能力 | 1 分钱微信/支付宝扫码付款仍需富友确认商户能力/路由后复测 | 工作日向富友确认商户号 `0002900F0370542`、机构号 `08A9999999` 的微信/支付宝扫码路由配置 | 用户已要求登记并工作日咨询富友工作人员 |
| 富友网银真实付款完成与后台回调 | 原因：已验证表单生成和 PAYING 状态，但未在富友网银页面真实完成付款 | 网银最终成功回调和查单推进仍需真实银行测试动作 | 使用 1 分钱订单真实跳转富友网银完成付款后，检查 `payment_order_status_flow.trigger_source` 是否为回调或主动查单 | 未最终人工付款 |
