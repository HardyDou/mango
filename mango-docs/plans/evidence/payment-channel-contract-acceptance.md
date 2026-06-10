# 通道签约配置验收证据

## 1. 验收范围

- 页面：`/#/payment/channel-contracts`、`/#/payment/operation-audits`
- 接口：`/api/payment/channel-contracts/page`、`/api/payment/channel-contracts`、`/api/payment/channel-contracts/{id}`、`/api/payment/operation-audits/page`
- 权限：支付中心菜单和登录态接口权限链路
- 数据：本地库 `mango_dev_e397cd`，租户 `1`，种子通道能力 `332001`，E2E 动态签约配置
- 部署形态：单体管理后台，前端 `http://127.0.0.1:7808`，后端 `http://127.0.0.1:18118`

## 2. 执行环境

- 前端地址：`http://127.0.0.1:7808`
- 后端地址：`http://127.0.0.1:18118`
- 数据库或租户：`mango_dev_e397cd` / tenant `1`
- 测试账号：`admin`
- 浏览器：Playwright Chromium

## 3. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| PAY-MENU-004 | `/#/payment/channel-contracts`、`/api/payment/channel-contracts` | 按支付通道字段模板渲染签约表单并真实保存 | 动态签约编码 `E2E_CONTRACT_*`，商户号、私钥、接入场景和状态 | 页面不展示原始 JSON 输入占位；敏感字段 `privateKey` 保存为 `enc:` 密文，详情回显为 `******`；创建、更新、删除均写入支付操作审计 | 动态字段使用 Element Plus 输入控件；普通字段纯文本展示，状态使用语义 tag；列表无额外卡片式外边框 | Playwright Chromium 调用真实后端接口，保存、详情、列表、审计均按业务断言通过；未使用接口拦截或临时前端数据 | `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1 --grep "通道签约配置"` | DONE |
| SECURITY-001A / PAY-CHANNEL-004 | `PaymentChannelContractServiceImpl`、`payment_channel_contract_value` | 通道签约字段安全落库 | 模板字段 `privateKey`、`certificateFileId` | 签约配置创建和更新时同步写入独立值表；敏感或加密字段只写 `encrypted_value`，不写 `value_text`；文件字段只写 `file_id` 且拒绝 `http/https` 文件访问地址；详情继续按模板脱敏返回 `******`；删除签约配置时物理清理签约值行，避免唯一键残留 | 后端安全能力，不新增页面交互；前端继续提交文件中心 ID | 后端定向单测覆盖密文落库、文件 ID 落库、URL 拒绝、删除值表清理；未使用 mock 数据作为交付，只用 Mockito 隔离 Mapper 断言服务行为 | `mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentChannelContractServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false` | DONE |
| PAY-CONTRACT-CAP-001 | `/api/payment/channel-contracts`、`payment_channel_contract_capability` | 签约能力真实保存、更新、删除和关系保护 | 通道能力 `332001`，费率 `0.0060000000`，限额 `10` 到 `880000` 分 | 签约能力保存到真实子表；更新时保留已有 `contract_capability_id`，不删除重建路由目标；删除签约配置时物理删除子能力行；已有路由引用的签约能力禁止移除 | 页面签约能力子表不展示裸 JSON；限额和费率表单可编辑，列表普通字段不使用 tag 装饰 | 后端单测覆盖稳定 ID、路由引用拒删、删除子表清理、负数金额拒绝；E2E 覆盖页面保存、回显和审计 | `mvn -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentChannelContractServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false` | DONE |
| PAY-CONTRACT-CAP-001 | `PaymentChannelContractServiceImpl`、`PaymentChannelContractMapper.xml`、`PaymentChannelContractCapabilityMapper.xml` | 签约配置后端规范化 | E2E 动态签约配置、被路由引用签约能力 | 签约配置删除关系检查通过 `PaymentChannelContractMapper.countDeleteRelations`；签约能力路由引用检查通过 `PaymentChannelContractCapabilityMapper.countRouteRelations`；签约能力物理删除方法下沉到 MyBatis XML；服务层不再使用 `JdbcTemplate` 做关系计数；业务校验继续使用 `Require` 和 `PaymentCode` | 页面无新增交互；接入场景新增数据使用 `MANGO_PAY`，历史 `MANGO_PAY` 仅兼容展示为“芒果支付” | 后端聚焦单测 32 个通过；前端构建通过；支付中心组合 E2E 4 个通过 | `mvn -pl mango-platform/mango-payment/mango-payment-core,mango-platform/mango-payment/mango-payment-starter -am -Dtest=PaymentChannelServiceImplTest,PaymentChannelContractServiceImplTest,PaymentReadonlyResourceServiceTest,PaymentApplicationServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false`；`pnpm -F mango-admin build`；`PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1 --grep "应用管理\|支付通道\|通道签约"` | DONE |
| PAY-CHANNEL-CAP-001 | `payment_channel_capability`、`payment_channel_contract_capability` | 签约能力必须来自当前通道能力并受限额约束 | 当前通道 `330002`，能力 `332001`，E2E 真实签约配置 | 后端校验 `channelCapability.channelId == channel.id` 和租户一致；签约能力限额必须非负且 `minAmount <= maxAmount`；费率最多 10 位小数 | 通道签约页面只选择当前通道下能力，避免把支付方式直接绑定裸通道或裸签约配置 | 单测与 E2E 均通过真实服务校验；数据库迁移 V20 将 `fee_rate` 调整为 `decimal(10,10)` 并已在本地库成功应用 | `mvn -pl mango-platform/mango-payment/mango-payment-core,mango-platform/mango-payment/mango-payment-starter -am test -DskipTests=false`；`pnpm -F mango-admin build` | DONE |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| 支付中心 | 通道签约配置 | 动态字段模板渲染 | 敏感字段加密存储和脱敏回显 | 列表普通字段纯文本，状态 tag，未出现字段框框 | `mango-docs/plans/evidence/payment-list-ui/channel-contracts-list-desktop.png` | DONE |
| 支付中心 | 完整 E2E | 菜单、收银台、应用、签约、主体 | 签约能力和审计链路 | E2E 6 个用例覆盖真实页面、接口和数据库 | `mango-ui/apps/mango-admin/e2e/specs/payment-center.spec.ts` | DONE |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 支付通道管理完整 CRUD 和通道能力后台维护 | 已在 `payment-channel-acceptance.md` 独立补充验收证据 | 本文件不再作为 PAY-MENU-005 的主要证据 | 以 `mango-docs/plans/evidence/payment-channel-acceptance.md` 为准 | 无 |
| 跨通道路由策略完整配置与试算 | 本轮只验证签约能力被路由引用时禁止移除，未完成路由配置页面和试算 | 不能声明 PAY-ROUTE-001、PAY-ROUTE-TEST-001 完成 | 后续按支付方式管理和路由模块开发优先级继续推进 | 无 |
