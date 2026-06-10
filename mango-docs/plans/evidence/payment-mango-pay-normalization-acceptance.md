# 芒果支付运行态归一化验收记录

## 范围

- 仅审计和归一化当前工作区本地数据库 `mango_dev_e397cd` 中支付模块运行态数据。
- 不回改已执行历史 Flyway migration。
- 不用芒果支付内置虚拟通道替代通联、华夏、微信、支付宝、连连等外部机构测试环境和真实联调。

## 已处理内容

- 使用 `scripts/payment_normalize_mango_pay.py` 归一化本地支付种子和 E2E 残留数据。
- 支付通道运行态保留 `MANGO_PAY / 芒果支付 / BUILTIN_VIRTUAL / MANGO_PAY`。
- 签约配置 JSON 旧场景 key 已迁移为 `mangoPayScenario` 和 `mangoPayRefundScenario`。
- 签约字段值表旧场景字段和旧商户号已迁移为芒果支付字段和值。

## 验证命令

```bash
python3 scripts/payment_normalize_mango_pay.py --apply
python3 -m py_compile scripts/payment_normalize_mango_pay.py
cd mango && mvn -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentMangoPayResultMappingServiceTest,PaymentMangoPayChannelAdapterTest,PaymentReadonlyResourceServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/designs/统一支付系统设计说明书.md --ledger mango-docs/plans/2026-05-25-payment-delivery-ledger.md --mode verify
```

## 验证结果

- 归一化脚本执行成功，脚本内 residual 全部为 0。
- 额外 SQL 审计确认 `payment_channel`、`payment_channel_contract`、`payment_channel_contract_value`、`payment_method_route_rule`、`payment_cashier_config`、`payment_notification_record` 启用数据旧概念残留均为 0。
- 后端相关测试 44 个通过。
- 交付契约检查仍有 13 个 `IN_PROGRESS` 项，属于外部通道、确认项、可观测性、集成测试和投产检查未完成，不属于本次归一化脚本失败。
