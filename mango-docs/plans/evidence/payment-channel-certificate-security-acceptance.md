# 通道证书安全验收证据

## 1. 验收范围

- 台账项：`PAY-CHANNEL-004`
- 设计来源：统一支付系统设计说明书 6.3、15、16
- 后端接口：`GET /payment/channel-contracts/certificates/expiring`、`POST /payment/channel-contracts/certificates/rotate`
- 后端服务：`PaymentChannelContractServiceImpl`
- 数据：`payment_channel_contract_capability`、`payment_channel_contract_value`、`payment_channel_certificate_rotation_record`、`payment_operation_audit`
- 权限：`payment:channel-contract:certificate-expiry`、`payment:channel-contract:certificate-rotate`

## 2. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| PAY-CHANNEL-004 | `GET /payment/channel-contracts/certificates/expiring`、`PaymentChannelContractCapabilityMapper.selectExpiringCertificates` | 通道证书到期提醒 | 当前租户 `1`，提醒天数 `7`，签约能力 `331102` | 查询只读取当前租户启用签约、启用通道和启用签约能力；只返回 `certificate_expire_time <= now + warningDays` 的记录；提醒天数超过 `0..365` 范围时抛业务异常 | 后端安全查询能力，不新增页面布局；接口通过独立权限 `payment:channel-contract:certificate-expiry` 受控 | Maven Surefire 覆盖租户参数传递、提醒天数范围校验和结果 VO 返回 | `PaymentChannelContractServiceImplTest.listExpiringCertificates_queriesCurrentTenant`、`PaymentChannelContractServiceImplTest.listExpiringCertificates_rejectsOutOfRangeDays` | DONE |
| PAY-CHANNEL-004 | `POST /payment/channel-contracts/certificates/rotate`、`payment_channel_certificate_rotation_record` | 通道证书轮换记录 | 签约配置 `331002`，签约能力 `331102`，证书字段 `certificateFileId`，旧文件 `900001`，新文件 `900002` | 轮换只允许通道字段模板中的 `fileId` 字段；新证书文件 ID 必须为正数；同步更新签约配置 JSON、`payment_channel_contract_value.file_id` 和签约能力 `certificate_expire_time`；新增轮换记录包含旧文件 ID、新文件 ID、旧有效期、新有效期、原因、操作人和轮换时间 | 后端受控操作能力，不新增页面布局；接口通过独立权限 `payment:channel-contract:certificate-rotate` 受控 | Maven Surefire 覆盖更新文件 ID、更新证书有效期、写轮换记录和返回 VO | `PaymentChannelContractServiceImplTest.rotateCertificate_updatesFileAndRecordsAudit` | DONE |
| PAY-CHANNEL-004 | `PaymentChannelContractServiceImpl.rotateCertificate`、`payment_operation_audit` | 证书轮换操作审计 | 操作 `ROTATE_CHANNEL_CERTIFICATE`，资源 `PAYMENT_CHANNEL_CONTRACT`，资源 ID `331002` | 证书轮换成功后写入 `payment_operation_audit`；审计记录使用当前操作人和当前租户上下文；拒绝非法文件字段时不更新签约配置、不写轮换记录 | 后端审计能力，不新增页面布局；审计可在既有操作审计列表按动作和资源查询 | Maven Surefire 断言 `PaymentOperationAuditService.record` 被调用且结果为 `SUCCESS` | `PaymentChannelContractServiceImplTest.rotateCertificate_updatesFileAndRecordsAudit` | DONE |
| PAY-CHANNEL-004 | `PaymentChannelContractServiceImpl.rotateCertificate` | 拒绝非证书文件字段轮换 | 证书字段编码 `privateKey`，新文件 `900002` | 当字段不是通道模板中的文件 ID 字段时抛业务异常；不会更新 `payment_channel_contract`；不会写入 `payment_channel_certificate_rotation_record` | 后端安全校验能力，不新增页面布局；避免把私钥、API Key 等非文件字段误当证书文件替换 | Maven Surefire 覆盖拒绝非法字段且无副作用 | `PaymentChannelContractServiceImplTest.rotateCertificate_rejectsUnknownFileField` | DONE |

## 3. 验证命令

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core,mango-platform/mango-payment/mango-payment-starter -am -Dtest=PaymentChannelContractServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
git diff --check -- mango/mango-platform/mango-payment/mango-payment-api mango/mango-platform/mango-payment/mango-payment-core mango/mango-platform/mango-payment/mango-payment-starter mango/mango-platform/mango-authorization/mango-authorization-core/src/main/resources/db/migration/authorization/V56__payment_channel_contract_certificate_permissions.sql
node mango-pmo/tools/acceptance-evidence-check.mjs --evidence mango-docs/plans/evidence/payment-channel-certificate-security-acceptance.md
```

## 4. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 外部通道真实证书联调 | 本证据只覆盖支付系统内证书文件 ID、到期提醒和轮换记录能力；通联、华夏、微信、支付宝、连连等真实证书格式、验签和网关联调仍依赖官方资料与联调环境 | 不能据此声明外部通道投产完成 | 外部通道专项台账继续保持 `IN_PROGRESS`，取得资料和联调环境后逐通道验收 | 不适用 |
| 前端证书轮换页面入口 | 本轮未新增页面，只补后台受控接口、权限、数据和审计能力 | 管理员暂不能在页面上直接点按钮轮换证书；接口和权限已具备，可后续按页面设计补入口 | 后续如设计要求页面操作，再在通道签约配置详情中接入，不改变本轮后端能力 | 不适用 |
| appSecret、银行账号、证件号跨模块加密 | 本证据只覆盖通道证书和密钥安全；`PAY-SEC-001` 中跨模块敏感字段仍需独立补齐 | 不能把 `PAY-SEC-001` 标记为完成 | 继续按 `PAY-SEC-001` 推进应用密钥和主体敏感字段安全检查 | 不适用 |
