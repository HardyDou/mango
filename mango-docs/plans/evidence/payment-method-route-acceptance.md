# 支付方式路由策略验收证据

## 1. 范围

本证据只覆盖本轮收敛范围：

1. 支付方式路由策略配置。
2. 路由试算。
3. 收银台可用支付方式与真实路由匹配的一致性。

不覆盖全量支付中心、开放支付接口、外部通道联调、退款、对账、结算和完整投产验收。

## 2. 交付物

| 类型 | 交付物 |
|---|---|
| 数据库 | `mango/mango-platform/mango-payment/mango-payment-core/src/main/resources/db/migration/payment/V41__payment_channel_capability_delete_flag.sql` |
| 后端 Service | `PaymentMethodRouteServiceImpl`、`PaymentCashierServiceImpl` |
| 后端 Mapper | `PaymentMethodRouteRuleMapper.xml`、`PaymentChannelContractCapabilityMapper.xml`、`PaymentMethodRouteRuleItemMapper.xml` |
| 前端页面 | `mango-ui/packages/payment/src/views/methods/PaymentMethodRoutePanel.vue` |
| E2E | `mango-ui/apps/mango-admin/e2e/specs/payment-center.spec.ts` |
| 截图 | `mango-docs/plans/evidence/payment-route-ui/payment-method-route.png` |

## 3. 已验证能力

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| PAY-ROUTE-001 | `/api/payment/method-routes` | 路由规则创建、环境隔离、删除清理 | `PERSONAL_WECHAT_QR`、应用 `310001`、主体 `320001`、签约能力 `MANGO_PAY_MANGO_TECH` Web 能力 | 创建 Web 芒果支付路由成功；`PROD` 场景复用芒果支付签约能力被拒绝，返回业务码 `3753`；E2E 测试规则最终 `del_flag = 1` | 后台路由策略弹窗能显示新建规则和“芒果支付” | Playwright 未发现 method-routes requestfailed；创建、删除接口业务成功 | `payment-center.spec.ts`、数据库查询、`payment_operation_audit` | DONE |
| PAY-ROUTE-TEST-001 | `/api/payment/method-routes/trial` | 路由试算命中规则和签约能力 | 应用 `310001`、主体 `320001`、金额 `9900` 分、终端 `WEB`、接入场景 `MANGO_PAY` | API 返回 `matched = true`，命中本次 E2E 创建的路由规则和签约能力 | 后台“路由试算”弹窗显示“已命中路由” | Playwright 等待 trial 接口响应成功；无 console error/pageerror | `mango-docs/plans/evidence/payment-route-ui/payment-method-route.png` | DONE |
| PAY-E2E-ROUTE-001 | `/#/payment/methods` | 支付方式管理页面进入路由策略并执行试算 | 登录用户 `admin`，微信扫码支付方式 `PERSONAL_WECHAT_QR` | 页面可搜索支付方式、打开“路由策略”、发起试算、截图、删除测试规则；审计存在 `CREATE_METHOD_ROUTE/TRIAL_METHOD_ROUTE/DELETE_METHOD_ROUTE` 成功记录 | 菜单路径为“支付中心 -> 支付通道 -> 支付方式管理”；UI 展示“芒果支付” | 定向 Playwright E2E `1 passed` | `payment-center.spec.ts`、`payment-route-ui/payment-method-route.png`、审计查询输出 | DONE |

## 4. 关键实现结论

1. 路由规则项指向签约能力，不直接指向裸通道或裸签约配置。
2. 签约能力校验同时匹配通道能力、签约配置、标准支付方式、终端和接入场景。
3. 收银台可用支付方式使用同一套路由匹配条件：应用、企业主体、标准支付方式、终端、芒果支付接入场景和金额。
4. UI 展示“芒果支付”；数据库和接口均使用 `MANGO_PAY`。
5. E2E 测试数据在 finally 中通过真实删除接口清理，清理后 `payment_method_route_rule.del_flag = 1`。

## 5. 验证命令

```bash
mvn -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentMethodRouteServiceImplTest,PaymentCashierServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
pnpm -F mango-admin build
scripts/dev-workspace.sh start
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1 --grep "支付方式路由策略"
```

## 6. 实际验证结果

| 命令 | 结果 |
|---|---|
| 后端目标单测 | 6 tests，0 failures，BUILD SUCCESS |
| 前端构建 | build success；存在既有 Vite dynamic import chunk 警告 |
| 定向 Playwright E2E | 1 passed |

## 7. 未验证项和风险

1. 本轮未声明全量支付平台完成；开放接口、外部通道、退款、对账、结算等仍按总台账状态处理。
2. 路由试算本轮覆盖优先级、环境隔离、真实页面交互和审计；禁用、超限、失败降级细分场景已有单测/SQL过滤支撑，但未在本次定向 E2E 中逐个展开。
3. 支付方式列表截图显示一级/二级/三级分类仍使用 Tag 边框样式；该问题归属 `PAY-UI-001` 支付列表普通字段展示规范，未纳入本轮职责边界。
