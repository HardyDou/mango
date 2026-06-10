# Payment 应用管理删除验收证据

## 1. 验收范围

- 页面：`/#/payment/applications`、`/#/payment/cashier-configs`、`/#/payment/operation-audits`、`/#/payment/cashier-configs/:cashierId/cashier`
- 接口：`/api/payment/applications/page`、`/api/payment/applications`、`/api/payment/cashier-configs/page`、`/api/payment/operation-audits/page`、支付中心 16 个列表查询接口
- 权限：后端数据库菜单与 `payment:application:delete` 删除接口权限接入
- 数据：本地库 `mango_dev_e397cd`，租户 `1`，示例应用 `管理后台支付 Demo`，E2E 动态应用 `支付E2E*`
- 部署形态：单体管理后台，前端 `http://127.0.0.1:7808`，后端 `http://127.0.0.1:18118`

## 2. 执行环境

- 前端地址：`http://127.0.0.1:7808`
- 后端地址：`http://127.0.0.1:18118`
- 数据库或租户：`mango_dev_e397cd` / tenant `1`
- 测试账号：`admin`
- 浏览器：Playwright Chromium / Chrome channel

## 3. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| PAY-MENU-001 | `/api/authorization/menus/user`、支付中心菜单 | 后端菜单分组和支付中心入口 | `appCode=internal-admin` | 菜单包含支付中心、应用接入、支付通道、交易订单、对账结算和 16 个支付页面；不包含独立租户收银台 | 从支付中心入口进入应用管理，页面标题为应用管理 | Playwright 未记录 console error、page error 或资源/API 4xx/5xx | `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1` | DONE |
| PAY-MENU-002 | `/#/payment/applications`、`/api/payment/applications` | 应用新增、接入安全、受控删除拒绝 | `支付E2E受控删除应用*`，并创建关联收银台 | 新增应用返回平台生成 AppId；报文加密关闭时不生成密钥；有关联收银台删除返回业务码 `3709`；应用仍可查询 | 删除按钮可见，二次确认提示列明收银台、订单、流水、通知、异常、对账差异等关联数据；拒绝删除后页面提示业务错误 | Playwright 未记录 console error、page error 或资源/API 4xx/5xx；删除接口业务错误码按预期返回 `3709` | `mango-docs/plans/evidence/payment-application-ui/applications-delete-dialog-desktop.png`；`mango-docs/plans/evidence/payment-application-ui/applications-delete-dialog-mobile.png` | DONE |
| PAY-MENU-002 | `/#/payment/applications`、`/api/payment/applications` | 无关联应用删除成功 | `支付E2E可删除应用*` | 删除接口成功；列表不再返回该应用；后端逻辑删除并写成功审计 | 删除二次确认可操作；删除成功后页面显示已删除，当前筛选结果移除该行 | Playwright 未记录 console error、page error 或资源/API 4xx/5xx | `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1` | DONE |
| PAY-MENU-002 | `PaymentApplicationServiceImpl`、`PaymentApplicationMapper.xml`、`V37__payment_application_audit_columns.sql` | 应用管理后端规范化 | 受控删除应用、有关联和无关联两类数据 | 删除关联检查通过 `PaymentApplicationMapper.countDeleteRelations` 和 MyBatis XML 查询真实表；应用实体继承 `AuditableEntity`；新增标准审计字段迁移；业务校验继续使用 `Require` 和 `PaymentCode`；无 `JdbcTemplate` 删除检查残留 | 无前端交互变化；页面通过同一真实接口查询和删除 | 后端单测 2 个用例通过；前端构建通过 | `mvn -pl mango-platform/mango-payment/mango-payment-core,mango-platform/mango-payment/mango-payment-starter -am -Dtest=PaymentApplicationServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false`；`pnpm -F mango-admin build` | DONE |
| PAY-MENU-006A | `/#/payment/cashier-configs`、收银台非菜单页面 | 收银台配置与应用删除保护联动 | `支付E2E删除保护收银台*`、`订单中心 Web 收银台` | 收银台配置保存的 `applicationId` 等于应用 ID；Logo 通过 `/api/file/files` 上传并在 `displayConfig.logoFileId` 保存文件 ID；允许主体、支付方式、默认方式、退款规则按接口回显；行操作可弹窗预览收银台并完成芒果支付付款成功展示 | 收银台配置页面可新增；行内收银台按钮打开当前页弹窗；弹窗展示统一支付收银台、支付方式和确认支付按钮，不跳转登录页 | Playwright 未记录 console error、page error 或资源/API 4xx/5xx | `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1` | DONE |
| PAY-MENU-016 | `/#/payment/operation-audits`、`/api/payment/operation-audits/page` | 删除操作审计 | `DELETE_APPLICATION`、E2E 动态 AppId | 拒绝删除写入 `DELETE_APPLICATION / PAYMENT_APPLICATION / REJECTED`；删除成功写入 `DELETE_APPLICATION / PAYMENT_APPLICATION / SUCCESS`；审计列表可按 `DELETE_APPLICATION` 查询 | 操作审计菜单可进入；查询后列表展示 `DELETE_APPLICATION` 和 `REJECTED` | Playwright 未记录 console error、page error 或资源/API 4xx/5xx | `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1` | DONE |
| PAY-TEST-003 | 支付中心 E2E | 支付中心应用管理相关 E2E 全链路 | 2 个 Playwright 应用管理用例 | 2 个用例全部通过：应用管理配置接入安全，收银台配置支付规则；应用管理删除受控校验和审计记录可用 | 应用管理列表普通字段纯文本展示，状态类字段使用 Tag；操作审计页面结果按中文展示“成功/已拒绝”，E2E 按本次 AppId 定位审计行 | Playwright 未记录 console error、page error 或资源/API 4xx/5xx；浏览器仅输出既有 Node deprecation warning | `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1 --grep "应用管理"` | DONE |
| PAY-TEST-003 | 支付中心 E2E | 应用管理、支付通道、通道签约组合回归 | E2E 动态应用、通道、签约配置 | 4 个用例全部通过：应用管理配置接入安全，收银台配置支付规则；通道签约配置按字段模板和签约能力真实保存、回显和删除审计可用；支付通道管理真实维护字段模板、通道能力和删除审计；应用管理删除受控校验和审计记录可用 | 组合回归中应用/通道/签约列表均可操作，普通列表字段不使用额外边框，语义状态使用 Element Plus Tag | Playwright 调用真实前后端和本地 DB；首次组合运行曾出现 E2E 等待弹窗隐藏时序超时，单用例复跑和组合复跑均通过，未发现业务接口失败 | `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1 --grep "应用管理\|支付通道\|通道签约"` | DONE |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| 支付中心 | 应用管理 | 查询 `管理后台支付 Demo` | 删除确认弹窗展示受控删除影响范围 | 桌面和移动视口均包含标题、搜索、按钮、表格、分页，未检测到关键控件重叠 | `mango-docs/plans/evidence/payment-application-ui/applications-desktop.png`；`mango-docs/plans/evidence/payment-application-ui/applications-mobile.png` | DONE |
| 支付中心 | 操作审计 | 查询 `DELETE_APPLICATION` | 回显拒绝删除和成功删除审计记录 | 列表可见审计动作和结果状态 | `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1` | DONE |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 全 payment 交付台账仍有未完成项 | 本轮范围是应用管理删除受控逻辑及其关联 E2E，不包含全部支付平台模块 | 不能声明统一支付系统整体完成 | 后续继续按 `2026-05-25-payment-delivery-ledger.md` 逐项推进 | 无 |
| 外部真实通道联调 | 当前验证使用芒果支付与本地数据库，未接入通联、华夏银行生产或联调环境 | 不影响应用管理删除逻辑，但不代表外部通道能力完成 | 待通道阶段按通道台账补联调证据 | 无 |
