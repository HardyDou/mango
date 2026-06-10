# 支付中心剩余交付项复核证据

## 1. 复核范围

- 设计文档：`mango-docs/designs/统一支付系统设计说明书.md`
- 交付台账：`mango-docs/plans/2026-05-25-payment-delivery-ledger.md`
- Sprint 计划：`mango-docs/plans/2026-05-25-payment-sprint-01.md`
- 投产就绪：`mango-docs/plans/2026-05-25-payment-production-readiness.md`
- 阻塞证据：`mango-docs/plans/evidence/payment-production-blockers-acceptance.md`

## 2. 本轮结论

当前支付中心管理页、收银台、芒果支付内置虚拟通道、线下收款通道、对账、结算、审计和完整 E2E 已形成验收证据；交付台账仍有 10 项 `IN_PROGRESS`。这些剩余项不能通过现有 E2E、芒果支付内置虚拟通道、线下收款通道或配置种子数据替代关闭。

## 3. 剩余项复核表

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| PAY-CHANNEL-003 | 通联支付适配层、机构回调入口 | 通联真实通道剩余项复核 | 台账状态 `IN_PROGRESS`；当前仅有芒果支付适配器 | 原因：通联支付必须真实支持支付、回调验签、主动查单、退款、账单和对账；当前没有真实适配器、商户参数、证书、账单样例和联调证据，不能用芒果支付代替 | 后台已有通道、签约、路由和收银台承载能力；未验收通联真实付款页面 | 未发起通联联调环境请求；无机构响应、回调和账单网络证据 | `payment-production-blockers-acceptance.md`、production readiness 阻塞登记 | BLOCKED |
| PAY-CHANNEL-005 | 华夏银行适配层、机构回调入口 | 华夏银行真实通道剩余项复核 | 台账状态 `IN_PROGRESS`；当前缺官方或内部对接资料 | 原因：华夏银行通道必须按官方或内部文档实现；资料不足时停止并登记阻塞，不允许替代交付 | 后台已有通道、签约、路由和收银台承载能力；未验收华夏银行真实付款页面 | 未发起华夏银行联调环境请求；无银行响应、回调和账单网络证据 | `payment-production-blockers-acceptance.md`、Sprint Q-005 | BLOCKED |
| PAY-CHANNEL-006 | 通道清单治理 | 其他已知通道清单复核 | 设计候选：微信、支付宝、连连等；台账状态 `IN_PROGRESS` | 原因：除芒果支付、通联、华夏银行外，其他已知通道清单和优先级仍缺确认；不能自行删减或降级为芒果支付 | 通道管理页面可承载通道产品定义；未按新增候选通道逐项验收 | 无真实外部通道网络请求；无用户确认记录 | 设计文档第 21 节、Sprint Q-002、production readiness 阻塞登记 | BLOCKED |
| PAY-CHANNEL-007 | 微信支付适配层 | 微信支付真实通道复核 | SDK 参考版本已记录；台账状态 `IN_PROGRESS` | 原因：当前没有微信商户号、AppId、API v3 Key、证书、公网回调和账单联调证据；代码边界测试确认未注册微信真实适配器 | 收银台已支持微信标准支付方式聚合展示；未验收微信直连真实付款页面 | 未发起微信支付 API 请求；无微信平台回调和账单网络证据 | `PaymentExternalChannelReadinessContractTest`、阻塞证据 | BLOCKED |
| PAY-CHANNEL-008 | 支付宝适配层 | 支付宝真实通道复核 | SDK 参考版本已记录；台账状态 `IN_PROGRESS` | 原因：当前没有支付宝 AppId、应用私钥、支付宝公钥、网关、回调和账单联调证据；代码边界测试确认未注册支付宝真实适配器 | 收银台已支持支付宝标准支付方式聚合展示；未验收支付宝直连真实付款页面 | 未发起支付宝 API 请求；无支付宝回调和账单网络证据 | `PaymentExternalChannelReadinessContractTest`、阻塞证据 | BLOCKED |
| PAY-TEST-004 | PMO 交付台账 verify | 最终台账检查复核 | `Rows 88`、`DONE 78`、`EXCEPTION 0`、`Incomplete 10` | 原因：当前 `delivery-contract-check --mode verify` 仍返回 10 个未完成项；PMO 规则要求未完成项不为 0 时禁止声明整体完成 | 不涉及页面布局；完整 E2E 和页面证据已在各页面证据文件记录 | 台账检查命令退出码为 1，符合剩余项未关闭现状 | 本轮台账检查输出 | BLOCKED |
| PAY-CONFIRM-001 | 线下收款独立通道 | 线下收款范围复核 | 用户已确认线下收款作为完整支付通道，需新增独立菜单、确认到账、支付凭证、银行流水 Excel 导入、对账匹配和退款处理；具体实现拆分为 `PAY-OFFLINE-001` 至 `PAY-OFFLINE-007` | 原确认项已关闭；线下收款实现项已通过真实后端测试和 Playwright E2E 关闭 | 当前线下收款菜单、收银台、凭证提交、确认到账、Excel 后端导入解析、匹配确认、部分退款、统一退款流水和审计边界已完成；外部真实通道仍独立阻塞 | E2E 真实调用线下收银台支付、凭证提交、确认到账、线下退款和银行流水导入确认接口 | `payment-user-confirmation-request.md`、`payment-offline-collection-design-acceptance.md` | DONE |
| PAY-CONFIRM-002 | 通道清单治理 | 通道优先级确认复核 | 台账状态 `IN_PROGRESS`；与 `PAY-CHANNEL-006` 绑定 | 原因：通道清单和优先级仍缺用户确认；不能由 Agent 自行删除候选通道 | 通道管理和路由页面具备承载能力；未按最终清单验收新增候选通道 | 无候选通道联调网络证据；无用户确认记录 | 设计文档第 21 节、Sprint Q-002 | BLOCKED |
| PAY-CONFIRM-003 | 支付结果通知协议 | 开票触发边界复核 | 当前支付系统只发送支付结果通知 | 原因：是否需要通知业务系统触发开票及是否扩展通知协议仍未确认；未经确认不得把开票业务耦合进支付模块 | 通知记录页面已覆盖支付结果通知；开票相关 UI 不属于当前已确认菜单 | 无业务开票系统联调网络证据；无用户确认记录 | 设计文档第 21 节、Sprint Q-003、通知验收证据 | BLOCKED |
| PAY-CONFIRM-004 | 租户模型、权限和数据隔离 | 租户定义复核 | 当前模型保留通用租户隔离 | 原因：租户代表内部事业部、外部客户还是两者都支持仍未确认，不能把权限和数据模型调整为单一假设 | 后台页面已按租户上下文查询和操作；未按最终租户业务定义执行专项验收 | 无最终租户模型验收记录；无用户确认记录 | 设计文档第 12、21 节、租户隔离验收证据 | BLOCKED |

## 4. 不可替代原则

| 场景 | 本轮判断 |
|---|---|
| 用芒果支付内置虚拟通道替代外部真实通道 | 不允许。芒果支付可用于内部验证支付闭环，但不能证明通联、华夏、微信或支付宝真实机构联调完成 |
| 用通道种子配置替代真实适配器 | 不允许。配置、字典和路由承载数据不等于机构 SDK、签名、验签、查单、退款或账单解析 |
| 用完整 E2E 通过替代台账最终完成 | 不允许。完整 E2E 已证明当前已实现模块可用，但 `PAY-TEST-004` 仍受外部通道、用户确认项和通道账单多获取源阻塞 |
| 未确认时自行调整开票、租户定义或其他外部通道优先级 | 不允许。设计文档第 21 节明确列为待确认问题；线下收款范围已由用户确认并完成独立台账验收 |

## 5. 本轮验证命令

```bash
node mango-pmo/tools/delivery-contract-check.mjs --design "mango-docs/designs/统一支付系统设计说明书.md" --ledger mango-docs/plans/2026-05-25-payment-delivery-ledger.md --mode verify

node mango-pmo/tools/acceptance-evidence-check.mjs --evidence mango-docs/plans/evidence/payment-production-blockers-acceptance.md --min-rows 10

node mango-pmo/tools/acceptance-evidence-check.mjs --evidence mango-docs/plans/evidence/payment-remaining-ledger-review.md --min-rows 10

rg -n "mock|fake|dummy|hardcode|固定成功|固定返回|TODO|FIXME|UnsupportedOperationException|SANDBOX|sandbox|沙箱|模拟|伪代码|未来优化|后续优化" mango/mango-platform/mango-payment/mango-payment-api/src/main mango/mango-platform/mango-payment/mango-payment-core/src/main mango/mango-platform/mango-payment/mango-payment-starter/src/main mango-ui/packages/payment/src mango-ui/apps/mango-admin/src/config/adminFeatureRegistrars.ts mango-ui/apps/mango-admin/src/config/menuLoader.ts mango-ui/apps/mango-admin/src/router/backEnd.ts

rg -n "implements IPaymentChannelAdapter|IPaymentChannelAdapter" mango/mango-platform/mango-payment/mango-payment-core/src/main/java/io/mango/payment/core/service

rg -n "IJPay|wechatpay|weixin-java|alipay-sdk" mango -g 'pom.xml'

mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentExternalChannelReadinessContractTest,PaymentProductionRedlineContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

## 6. 后续动作

| 优先级 | 动作 | 前置条件 |
|---|---|---|
| P0 | 等待或获取外部通道资料：通联、华夏、微信、支付宝 | 用户提供商户、证书、公钥私钥、网关、回调、账单样例和联调账号 |
| P0 | 等待用户确认通道清单、开票触发和租户定义 | 用户明确回复确认口径；线下收款范围已确认并转入 `PAY-OFFLINE-*` 研发项 |
| P1 | 用户确认例外后更新设计文档、台账和证据，把对应项标记为 `EXCEPTION` | 必须有用户确认依据 |
| P1 | 外部资料齐备后逐通道研发和联调，不允许写占位适配器或固定成功 | 按对应通道官方或内部文档实施 |

## 7. 生产红线复查

| 检查项 | 当前结果 | 结论 |
|---|---|---|
| 支付域生产代码和支付前端包红线词扫描 | 对 `mango-payment` 的 `src/main`、`mango-ui/packages/payment/src`、支付菜单注册相关文件执行 mock/fake/dummy/hardcode/TODO/FIXME/sandbox/沙箱/模拟/伪代码/固定成功/固定返回等关键词扫描，命令退出码为 1 且无输出，表示未命中 | 未发现支付域生产交付代码存在私自 mock、固定成功、沙箱残留或伪代码标记 |
| 通道适配器实现扫描 | `implements IPaymentChannelAdapter` 仅命中 `PaymentMangoPayChannelAdapter.java`；其余命中为 SPI、注册表和调用点 | 当前没有通联、华夏、微信、支付宝等外部通道伪适配器，外部通道仍必须保持未完成/阻塞 |
| 外部 SDK 提前接入扫描 | `rg -n "IJPay|wechatpay|weixin-java|alipay-sdk" mango -g 'pom.xml'` 无输出 | 当前没有在真实适配器缺失时提前声明微信/支付宝 SDK 依赖 |
| 生产红线契约测试 | `PaymentProductionRedlineContractTest` 覆盖支付生产源码红线词、历史运行态种子数据清理；`PaymentExternalChannelReadinessContractTest` 覆盖未完成外部通道注册表拒绝、生产适配器只有芒果支付、外部 SDK 未提前声明、外部通道台账保持阻塞 | 可作为本轮“未私自 mock、未伪接外部通道、未把阻塞项伪完成”的自动化证据 |
