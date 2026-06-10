# Payment 企业主体管理验收证据

## 1. 验收范围

- 页面：`/#/payment/enterprise-subjects`、`/#/payment/operation-audits`
- 接口：`/api/payment/enterprise-subjects/page`、`/api/payment/enterprise-subjects`、`/api/payment/operation-audits/page`
- 权限：支付中心后端菜单与接口登录态权限链路
- 数据：本地库 `mango_dev_e397cd`，租户 `1`，种子主体 `芒果科技有限公司`，E2E 动态主体 `支付E2E企业主体*`
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
| PAY-MENU-003 | `/#/payment/enterprise-subjects`、`/api/payment/enterprise-subjects` | 企业主体新增和列表回显 | `支付E2E企业主体*`、动态统一社会信用代码、动态银行账户 | 新增返回真实主体 ID；列表接口按主体名称可查到该 ID；`bankAccountNo` 不返回，`bankAccountNoMask` 只返回前 4 后 4 脱敏值；创建写入 `CREATE_ENTERPRISE_SUBJECT / PAYMENT_ENTERPRISE_SUBJECT / SUCCESS` 审计 | 页面通过新增弹窗录入主体名称、统一社会信用代码、银行账户、开户行和状态；列表中主体名称、信用代码是普通文本，不被 `.el-tag` 包裹；状态使用标签展示 | Playwright Chromium 执行真实页面和真实接口；保存、查询、审计接口均返回符合业务断言的数据，未出现资源加载失败或业务断言外异常 | `mango-ui/apps/mango-admin/e2e/specs/payment-center.spec.ts`；`mango-docs/plans/evidence/payment-list-ui/enterprise-subjects-list-desktop.png` | DONE |
| PAY-MENU-003 | `/#/payment/enterprise-subjects`、`/api/payment/enterprise-subjects` | 企业主体编辑保存 | E2E 动态主体改名为 `支付E2E企业主体编辑*`，开户行改为 `招商银行上海E2E支行*` | 编辑接口成功；列表接口按新主体名称可查到同一 ID；开户行更新为新值；更新写入 `UPDATE_ENTERPRISE_SUBJECT / PAYMENT_ENTERPRISE_SUBJECT / SUCCESS` 审计 | 编辑弹窗复用真实表单；保存 payload 只提交 Command 字段，脱敏展示字段 `bankAccountNoMask` 不再提交到后端 | Playwright Chromium 捕获到真实 `PUT /api/payment/enterprise-subjects` 响应并完成业务断言；未再出现 Jackson 未识别字段导致的系统异常 | `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1 --grep "企业主体"` | DONE |
| PAY-MENU-003 | `/#/payment/enterprise-subjects`、`/api/payment/enterprise-subjects` | 有关联数据主体拒绝删除 | 种子主体 `芒果科技有限公司` / `resourceId=320001` | 删除接口返回业务码 `3729`，消息包含“企业主体存在关联数据”；主体仍保留；拒绝删除写入 `DELETE_ENTERPRISE_SUBJECT / PAYMENT_ENTERPRISE_SUBJECT / 320001 / REJECTED` 审计 | 页面行内删除按钮可见；删除二次确认可操作；拒绝后页面展示业务错误提示，不移除该行 | Playwright Chromium 捕获到真实 DELETE 业务错误并按 code/message 断言；错误属于预期业务拒绝，不是系统异常 | `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1 --grep "企业主体"` | DONE |
| PAY-MENU-003 | `/#/payment/enterprise-subjects`、`/api/payment/enterprise-subjects` | 无关联数据主体删除成功 | E2E 动态主体 `支付E2E企业主体编辑*` | 删除接口成功；列表接口按主体名称不再返回该主体；删除成功写入 `DELETE_ENTERPRISE_SUBJECT / PAYMENT_ENTERPRISE_SUBJECT / SUCCESS` 审计 | 页面删除二次确认可操作；删除成功后显示“已删除”，当前筛选结果移除该行 | Playwright Chromium 捕获到真实 DELETE 成功响应并完成列表消失和审计断言 | `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1 --grep "企业主体"` | DONE |
| PAY-MENU-003 | `mango-payment-core`、`payment_enterprise_subject` | 后端审计基础类、删除关联校验和单测 | `PaymentEnterpriseSubjectServiceImplTest` | Entity 继承 `AuditableEntity`；保存使用 `Require` 和 `PaymentCode`；删除前检查收银台、签约、业务订单、支付订单、退款、流水、异常、通知、差异、结算关联；拒绝和成功均记录 `payment_operation_audit` | 后台页面仍按 Element Plus 通用资源页操作；普通字段不使用框框/tag，状态字段使用 tag | Maven 单测覆盖拒删审计和成功删除审计；Flyway V15 已在本地库执行成功，字段为 `created_at/updated_at` | `mvn -pl mango-platform/mango-payment/mango-payment-core -am test -DskipTests=false` | DONE |
| PAY-MENU-003 | `PaymentEnterpriseSubjectMapper.xml` | 删除关联统计持久化规范化 | `PaymentEnterpriseSubjectServiceImplTest` | 删除关系统计下沉到 `PaymentEnterpriseSubjectMapper.countDeleteRelations`；Service 不再注入 `JdbcTemplate`，仍覆盖收银台、签约、业务订单、支付订单、退款、流水、异常、通知、差异、结算汇总 | UI 行为不变 | Maven 专项测试 16 个通过；生产代码扫描无 `JdbcTemplate` | `mvn -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentEnterpriseSubjectServiceImplTest,PaymentMethodServiceImplTest,PaymentCashierConfigServiceImplTest,PaymentCashierServiceImplTest,MangoPayVirtualPaymentServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false` | DONE |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| 支付中心 | 企业主体管理 | 新增、编辑、拒删、成功删除 | 创建、更新、删除三类审计查询 | 主体名称、统一社会信用代码、开户行、银行账户为普通文本；状态为 Element Plus tag；弹窗字段未遮挡 | `mango-docs/plans/evidence/payment-list-ui/enterprise-subjects-list-desktop.png`；专项 E2E 命令见功能验收记录 | DONE |
| 支付中心 | 全 payment E2E | 菜单与 16 个列表接口 | 应用、收银台、支付延迟、应用删除、企业主体 CRUD | 完整支付中心 E2E 5 个用例通过，覆盖菜单、表格、弹窗、收银台和审计列表 | `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1` | DONE |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 外部支付通道联调 | 本轮范围是企业主体管理，不包含通联、华夏银行等外部通道签约与生产联调 | 不影响企业主体 CRUD、脱敏、审计和删除保护结论；不能代表通道模块完成 | 后续按通道签约配置、支付通道管理、支付方式管理台账继续推进 | 无 |
| 企业主体证照文件上传深度验收 | 本轮没有新增证照上传专项用例，页面保留 `MUpload` 文件 ID 字段 | 不影响主体基础信息、银行账户脱敏、删除保护和审计；证照文件链路需在文件/签约资料阶段补完整覆盖 | 后续通道签约和证照资料验收中补上传、回显、权限和文件 ID 证据 | 无 |
