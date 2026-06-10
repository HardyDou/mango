# 支付敏感字段安全验收证据

## 1. 验收范围

- 台账项：`PAY-SEC-001`
- 设计来源：统一支付系统设计说明书 6.1、6.2、15
- 后端服务：`PaymentApplicationServiceImpl`、`PaymentEnterpriseSubjectServiceImpl`、`PaymentOpenApiService`、`PaymentNotificationService`、`PaymentCashierServiceImpl`、`PaymentSensitiveValueService`、`PaymentSensitiveFieldReencryptService`
- 数据：`payment_application.app_secret`、`payment_enterprise_subject.credit_code`、`payment_enterprise_subject.credit_code_hash`、`payment_enterprise_subject.bank_account_no`、`payment_subject_bank_account.account_no`
- 前端页面：企业主体管理、收银台配置、通道签约配置、支付方式路由面板中的企业主体展示字段

## 2. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| PAY-SEC-001 | `POST /payment/applications`、`PaymentApplicationServiceImpl` | 应用密钥加密存储和只展示一次 | 报文加密开启，平台生成应用密钥 | 服务返回本次生成的明文 `appSecret`；落库实体为 `enc:` 密文；列表和详情 VO 不返回 `appSecret`；报文加密关闭时不生成、不保存应用密钥 | 应用管理页面仍只展示密钥已配置、版本和最后生成时间，不提供重置密钥按钮 | 后端单测覆盖密文落库和明文只在保存结果返回 | `PaymentApplicationServiceImplTest.createApplication_payloadEncryptionEnabled_encryptsStoredSecret` | DONE |
| PAY-SEC-001 | OpenAPI 认证、业务通知 | 密文应用密钥解密后参与 HMAC 签名 | `app_openapi`，`app_secret = enc:openapi-secret-ciphertext` | OpenAPI 认证读取密文后解密计算签名；支付和退款通知读取密文后解密签名；请求签名和通知签名链路均不使用密文本身 | 后端链路能力，不新增页面 | 后端单测覆盖 OpenAPI 下单、查询、支付、退款和通知投递 | `PaymentOpenApiServiceTest`、`PaymentNotificationServiceTest` | DONE |
| PAY-SEC-001 | `POST/PUT /payment/enterprise-subjects`、`PaymentEnterpriseSubjectServiceImpl` | 企业主体证件号、主体银行账号和独立主体银行账户表加密存储、脱敏展示 | 统一社会信用代码 `91310000MA1PAY001X`，银行账号 `6222000000000001` | 保存主体时 `credit_code`、`bank_account_no` 写入 `enc:` 密文；`credit_code_hash` 写入规范化 SHA-256 哈希用于唯一约束；默认 `payment_subject_bank_account.account_no` 同步写入同一银行账号密文；详情和列表只返回 `creditCodeMask`、`bankAccountNoMask` | 企业主体列表展示脱敏字段；编辑弹窗以占位提示当前脱敏值，保存时需重新输入完整值 | 后端单测覆盖加密保存、独立账户表密文同步和脱敏返回；前端构建覆盖字段改名 | `PaymentEnterpriseSubjectServiceImplTest.createEnterpriseSubject_encryptsSensitiveValues`、`PaymentEnterpriseSubjectServiceImplTest.updateEnterpriseSubject_syncsEncryptedDefaultBankAccount`、`PaymentEnterpriseSubjectServiceImplTest.detailEnterpriseSubject_masksSensitiveValues` | DONE |
| PAY-SEC-001 | 收银台会话和线下转账物料 | 收银台主体信息脱敏，对公转账物料展示真实收款账号 | 主体银行账号 `enc:account-ciphertext` | 收银台主体信息只返回证件号和银行账号脱敏值；线下转账支付物料按业务需要解密返回真实收款账号 | 收银台页面继续从后端真实数据渲染；本轮未改页面布局 | 后端单测覆盖脱敏会话和线下转账物料解密 | `PaymentCashierServiceImplTest.detailSession_masksEncryptedSubjectValues`、`PaymentCashierServiceImplTest.pay_offlineTransfer_decryptsAccountMaterial` | DONE |
| PAY-SEC-001 | 企业主体选择器 | 不在下拉描述中展示完整证件号 | 企业主体下拉用于收银台配置、通道签约配置、支付方式路由 | 企业主体下拉描述字段从 `creditCode` 改为 `creditCodeMask`；前端类型允许保存入参 `creditCode`，展示使用 `creditCodeMask` | 前端构建通过，覆盖相关支付页面组件 | `pnpm -F mango-admin build` 通过 | `mango-ui/packages/payment/src/views/cashier-configs/index.vue`、`mango-ui/packages/payment/src/views/channel-contracts/index.vue`、`mango-ui/packages/payment/src/views/methods/PaymentMethodRoutePanel.vue` | DONE |
| PAY-SEC-001 | `POST /payment/security/sensitive-fields/reencrypt`、`PaymentSensitiveFieldReencryptService` | 历史明文敏感字段受控重加密 | 本地工作区库 `mango_dev_e397cd`，执行前明文残留：`payment_application.app_secret=36`、`payment_enterprise_subject.credit_code=3`、`payment_enterprise_subject.bank_account_no=3`、`payment_subject_bank_account.account_no=3` | 接口需要 `payment:security:reencrypt-sensitive` 权限；只处理当前租户；`limit` 必须在 `1-1000`；不返回明文；执行后四类字段明文残留均为 `0`；第二次执行返回 `totalCount=0`；写入 `payment_operation_audit` 审计 `REENCRYPT_SENSITIVE_FIELDS/PAYMENT_SENSITIVE_FIELDS/tenant:1,count:45/SUCCESS` | 后端受控运维接口，不新增普通页面入口 | Maven 单测覆盖历史明文加密、已加密跳过、非法批量拒绝和审计计数；真实后端、真实登录态、真实数据库接口调用成功 | `PaymentSensitiveFieldReencryptServiceTest`；`flyway_schema_history_payment` 已应用 V56；`flyway_schema_history_authorization` 已应用 V57；`authorization_api_resource` 和 `authorization_menu` 均存在权限记录 | DONE |

## 3. 验证命令

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentApplicationServiceImplTest,PaymentEnterpriseSubjectServiceImplTest,PaymentOpenApiServiceTest,PaymentNotificationServiceTest,PaymentCashierServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentEnterpriseSubjectServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentSensitiveFieldReencryptServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-starter -am -DskipTests compile
pnpm -F mango-admin build
curl -X POST 'http://127.0.0.1:18118/payment/security/sensitive-fields/reencrypt?limit=100' -H 'Authorization: Bearer <token>' -H 'X-Tenant-Id: 1'
```

## 4. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 生产历史数据执行窗口 | 已补受控重加密接口、独立权限、审计、单测和本地真实库执行证据；生产环境仍需按变更流程选择窗口执行同一接口 | 不影响代码交付完成；生产库在执行前仍以现场数据为准 | 投产前由运维在目标租户逐批执行接口并复核明文残留为 0，保留审计记录 | 不适用 |
| 独立银行账户管理页面/API | 本轮仅在企业主体保存事务内同步维护默认 `payment_subject_bank_account` 并验证 `account_no` 密文落库；设计未要求单独新增银行账户页面/API，本轮未扩展新入口 | 默认账户写入链路已接入加密；若后续新增独立银行账户维护入口，仍需复用同一加密服务和脱敏展示 | 后续只在设计或台账明确要求独立入口时补页面/API，不以当前同步能力替代未来独立入口验收 | 不适用 |
| 运行时日志采集平台脱敏演练 | `PAY-SEC-002` 已补支付模块源码日志契约测试；本证据不接入日志采集平台或生产日志样本 | 不影响新写入敏感字段加密和代码层日志不输出完整敏感信息；不能替代 `PAY-OBS-001` 可观测性建设 | 日志采集、链路追踪、指标和告警继续按 `PAY-OBS-001` 推进 | 不适用 |
