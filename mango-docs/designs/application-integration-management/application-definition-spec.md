# 集成应用定义规范（连接器 / 凭证 / 实例 / 回调订阅）

本文件定义“应用管理 → 集成应用”的目标模型。模型对齐行业主流做法（n8n / Zapier / Workato 的连接器与凭证分离、Airbyte 的 spec.json、Power Platform 的自定义连接器、Stripe 与企业微信的回调订阅），并复用 Mango 现有资源声明机制。

## 1. 分层

| 层 | 名称 | 载体 | 维护者 | 说明 |
|---|---|---|---|---|
| ① | 连接器定义 `IntegrationConnector` | 资源声明 `INTEGRATION_CONNECTOR` 同步 + 页面维护 | 平台 / 模块所有者 | 声明配置规范、认证规范与能力清单 |
| ② | 凭证 `IntegrationCredential` | 数据库 + 密钥管理引用 | 租户管理员 | 按认证 schema 填写，字段级加密，只写不读 |
| ③ | 连接实例 `IntegrationInstance` | 数据库 | 租户管理员 | 绑定连接器版本 + 配置值 + 凭证引用 + 能力授权 |
| ④ | 回调订阅 `IntegrationSubscription` | 数据库 + 投递日志 | 租户管理员 | 回调地址、事件类型、签名密钥、重试与投递记录 |

业务域职责不变：`mango-auth`、`mango-notice`、`mango-payment` 保留自己的协议语义与业务流程，只从 ②③④ 读取配置。

## 2. 连接器定义

### 2.1 字段

| 字段 | 类型 | 说明 |
|---|---|---|
| `connectorCode` | string | 连接器编码，如 `wecom-message`，全局唯一 |
| `connectorName` | string | 名称 |
| `category` | string | 用途分类，字典 `integration_connector_category`（支付、消息、身份、文件、其他） |
| `provider` | string | 厂商标识，如 `WECOM`、`ALIYUN_SMS`、`WECHAT_PAY` |
| `status` | int | 0-停用，1-启用 |
| `configSchema` | JSON Schema | 非敏感配置的字段、类型、校验、分组、默认值 |
| `authSchema` | object | 认证类型与字段声明 |
| `capabilities` | array | 能力对象：`code` 能力编码、`name` 中文名称、`description` 介绍 |
| `docsUrl` | string | 文档地址 |

### 2.2 authSchema

认证方式与参数统一由平台工具类实现，业务域不重复编写签名、令牌与解密逻辑：

| 工具类 | 职责 |
|---|---|
| `IntegrationAuthConfig` | 解析认证规范与凭证值 |
| `IntegrationOutboundAuth` | 出站认证应用（API Key / Bearer / Basic / OAuth2 / HMAC / Query Token） |
| `IntegrationTokenCache` | OAuth2 令牌获取与缓存 |
| `IntegrationInboundAuth` | 回调验签、重放窗口、AES 解密 |
| `IntegrationDeclarationProvider` | 业务域读取实例声明、认证规范与解密凭证 |
| `IntegrationClient` | 统一调用：读取声明、应用认证、超时与重试，返回状态码与响应体 |
| `integration_credential_rotation` | 轮换历史：版本、更新字段、操作人、时间 |
| `integration_credential_access_log` | 凭证读取记录：凭证编码、字段编码、操作人、来源 |
| `integration_call_log` | 调用记录：实例、方法、路径、状态码、耗时、结果 |

```json
{
  "authMode": "SECRET",
  "fields": [
    { "fieldCode": "corpId", "label": "企业 ID", "type": "STRING", "required": true },
    { "fieldCode": "agentId", "label": "应用 AgentId", "type": "STRING", "required": true },
    { "fieldCode": "corpSecret", "label": "应用 Secret", "type": "SECRET", "required": true, "masked": true },
    { "fieldCode": "callbackToken", "label": "回调 Token", "type": "SECRET", "required": false },
    { "fieldCode": "encodingAesKey", "label": "回调 AES Key", "type": "SECRET", "required": false },
    { "fieldCode": "platformCert", "label": "平台公钥证书", "type": "FILE", "required": false }
  ],
  "testOperation": "getToken"
}
```

`authMode` 取值：`SECRET` / `API_KEY` / `OAUTH2` / `JWT` / `MTLS` / `CALLBACK_SIGNATURE` / `NONE`。
`type` 取值：`STRING` / `SECRET` / `FILE` / `CERT` / `ENUM`。

### 2.3 能力与操作（预留，本期未启用）

```json
[
  {
    "operationCode": "getToken",
    "httpMethod": "GET",
    "path": "/cgi-bin/gettoken",
    "inputSchema": { "type": "object", "properties": {} },
    "outputSchema": { "type": "object", "properties": { "access_token": { "type": "string" } } }
  },
  {
    "operationCode": "sendMessage",
    "httpMethod": "POST",
    "path": "/cgi-bin/message/send",
    "capability": "message.send",
    "inputSchema": {
      "type": "object",
      "required": ["touser", "msgtype"],
      "properties": {
        "touser": { "type": "string" },
        "msgtype": { "type": "string", "enum": ["text", "markdown"] },
        "text": { "type": "object" }
      }
    }
  }
]
```

规则：

- `operationCode` 是业务域唯一调用入口，业务代码只依赖它
- 每个操作声明 `capability`，实例授权后才能调用
- 输入输出用 JSON Schema，可供校验与文档生成

### 2.4 错误码目录（预留，本期未启用）

```json
[
  { "match": { "field": "errcode", "in": [40014, 42001] }, "action": "REFRESH_CREDENTIAL", "maxAttempts": 1 },
  { "match": { "field": "errcode", "in": [45009] }, "action": "RETRY", "maxAttempts": 1 },
  { "match": { "httpStatus": [401, 403] }, "action": "FAIL_FAST" }
]
```

`action` 取值：`RETRY` / `REFRESH_CREDENTIAL` / `FAIL_FAST` / `MANUAL`。

### 2.5 声明示例（企业微信应用消息）

```json
{
  "id": "connector-wecom-message-v1",
  "version": 1,
  "resourceType": "INTEGRATION_CONNECTOR",
  "moduleCode": "mango-notice",
  "bizKey": "notice.wecom.message",
  "name": "企业微信应用消息",
  "targetModule": "notice",
  "fields": {
    "connectorCode": { "type": "STRING", "value": "wecom-message" },
    "connectorName": { "type": "STRING", "value": "企业微信应用消息" },
    "connectorVersion": { "type": "INT", "value": 1 },
    "category": { "type": "STRING", "value": "MESSAGING" },
    "provider": { "type": "STRING", "value": "WECOM" },
    "status": { "type": "STRING", "value": "ACTIVE" },
    "adapterClass": { "type": "STRING", "value": "io.mango.notice.channel.wecom.WecomConnectorAdapter" },
    "capabilities": { "type": "LIST", "value": ["message.send", "token.refresh"] },
    "authSchema": { "type": "JSON", "value": { "...": "见 2.2" } },
    "configSchema": { "type": "JSON", "value": { "...": "见 2.6" } },
    "operations": { "type": "JSON", "value": ["..."] },
    "errorCatalog": { "type": "JSON", "value": ["..."] },
    "docsUrl": { "type": "STRING", "value": "https://developer.work.weixin.qq.com/document" }
  }
}
```

### 2.6 configSchema 示例（JSON Schema）

```json
{
  "type": "object",
  "required": ["apiBaseUrl"],
  "properties": {
    "apiBaseUrl": {
      "type": "string",
      "title": "接口地址",
      "default": "https://qyapi.weixin.qq.com",
      "format": "uri"
    },
    "timeoutMs": { "type": "integer", "title": "超时毫秒", "default": 5000, "minimum": 1000, "maximum": 60000 },
    "retryMax": { "type": "integer", "title": "最大重试", "default": 2, "minimum": 0, "maximum": 5 }
  }
}
```

## 3. 凭证

| 字段 | 说明 |
|---|---|
| `credentialCode` | 凭证编码，租户内唯一 |
| `connectorCode` | 绑定的连接器 |
| `fields` | 按 `authSchema` 存储：明文项、加密项（`SECRET`）、证书文本项（`CERT`/`FILE`，明文但脱敏） |
| `fieldVersion` | 每个敏感字段独立版本 |
| `rotatedAt` / `expiresAt` | 轮换时间与到期时间 |
| `status` | `ACTIVE` / `ROTATING` / `EXPIRED` |

规则：

- 敏感字段使用 Mango Crypto 加密，密钥来自部署配置
- 任何查询接口只返回 `configured`、`mask`、`version`、`expiresAt`
- 轮换保留历史版本，支持回滚
- 文件类凭证存 `fileId`，不落库二进制

## 4. 连接实例

| 字段 | 说明 |
|---|---|
| `instanceCode` | 实例编码，如 `wecom-corp-main` |
| `instanceName` | 名称 |
| `connectorCode` | 绑定的连接器 |
| `environment` | `PROD` / `TEST` |
| `configValues` | 按 `configSchema` 校验后的非敏感配置 |
| `credentialCode` | 引用的凭证 |
| `capabilities` | 授权的操作能力，必须在连接器能力清单内 |
| `status` | `ENABLED` / `DISABLED` / `EXPIRED` |
| `owner` / `remark` | 责任人与备注 |

URL 规则：

```text
回调地址：/integration/callback/{connectorCode}/{instanceCode}
```

## 5. 回调订阅

| 字段 | 说明 |
|---|---|
| `subscriptionCode` | 订阅编码 |
| `instanceCode` | 所属连接实例 |
| `endpointPath` | 平台内回调路径，自动生成 |
| `eventTypes` | 订阅的事件类型 |
| `signatureMode` | `SIGNATURE` / `SIGNATURE_WITH_ENCRYPTION` / `MTLS` / `NONE` |
| `secretRef` | 验签密钥引用（凭证字段） |
| `retryPolicy` | 最大次数、退避、超时 |
| `status` | `ACTIVE` / `SUSPENDED` |

投递日志：`subscriptionCode`、`eventId`、`receivedAt`、`signatureValid`、`processedResult`、`retryCount`、`failReason`（脱敏）。

## 6. 版本与兼容

| 规则 | 说明 |
|---|---|
| 定义直接编辑 | 不做版本概念，保存即生效 |
| 兼容校验 | 保存时校验存量实例：删除字段或新增必填字段时提示受影响的实例 |
| 字段废弃 | 建议保留字段并标注说明，避免存量实例配置丢失 |
| 实例状态 | 与定义状态解耦，定义调整不自动停用实例 |
| 凭证兼容 | 字段 `fieldCode` 不变时轮换不影响实例 |

## 7. 与业务域分工

| 事项 | 连接器/实例/订阅 | 业务域 |
|---|---|---|
| 配置字段与校验 | ✅ | 引用 |
| 凭证存储、加密、轮换 | ✅ | 读取 |
| 签名、加解密、SDK 调用 | 由 adapter 提供 | ✅ 业务语义 |
| 令牌获取与缓存 | ✅ 通用能力 | 决定刷新时机 |
| 操作输入输出契约 | ✅ | ✅ 业务校验 |
| 菜单、模板、业务状态机 | ❌ | ✅ |
| 回调验签与解密 | ✅ 平台 | ✅ 事件处理 |
| 调用审计与失败统计 | ✅ | 查询 |

## 8. 菜单与页面

```text
应用管理
├── 逻辑应用
├── 微应用
├── 集成应用      实例列表：按连接器动态渲染表单
└── 应用定义      只读：连接器、版本、分类、厂商、能力、被引用实例数
```

## 9. 分期

| 阶段 | 内容 | 验收 |
|---|---|---|
| P0 | 连接器定义 + 凭证 + 实例三张表与只读定义清单 | 能声明企业微信连接器并创建实例、填写凭证、看到掩码 |
| P1 | 操作调用入口（业务代码可用）+ 测试连接操作 | notice 用实例发出一条真实企业微信消息 |
| P2 | 回调订阅 + 投递日志 + 验签 | 企业微信回调 URL 验证通过，事件可解密投递 |
| P3 | 轮换与到期提醒、调用审计、限流 | 凭证到期前告警，可按实例查看调用失败分布 |

## 10. 首批定义范围

| 连接器 | 分类 | 关键要求 |
|---|---|---|
| `wecom-message` | MESSAGING | corpId/agentId/corpSecret、令牌缓存、错误码 40014/42001 刷新 |
| `wecom-login` | IDENTITY | 授权码换用户、redirectUri 白名单 |
| `aliyun-sms` | MESSAGING | accessKeyId/Secret、签名与模板、SDK 调用 |
| `dingtalk-message` | MESSAGING | appKey/appSecret、加签 webhook |
| `wechat-pay` | PAYMENT | 商户号、私钥证书、平台证书、回调验签、幂等 |

## 11. 范围与实现状态

本模块只负责**定义与声明**：连接器定义、凭证、连接实例、回调订阅的登记与校验。实际调用、令牌获取、验签、投递、重试、审计全部由使用方业务域实现，本模块不提供运行时执行入口。

业务域消费方式：

- 注入 `IIntegrationCredentialService#resolve(credentialCode)` 读取解密后的凭证字段
- 通过实例查询拿到 `configValues`、`capabilities`、`callbackPath`
- 不得把凭证复制到业务表，也不得在日志打印原文

已实现：

| 能力 | 实现 |
|---|---|
| 连接器定义（只读） | `authorization_integration_connector`，`GET /authorization/integration-connectors/page|detail` |
| 凭证（多字段 + 加密 + 版本 + 轮换） | `authorization_integration_credential`，`GET/POST /authorization/integration-credentials`、`POST /rotate` |
| 连接实例（配置规范校验 + 能力授权 + 回调路径） | `authorization_integration_instance`，`GET/POST /authorization/integration-instances`、`POST /change-status` |
| 页面 | 应用管理 → 应用定义（只读）、集成应用（动态表单） |
| 校验 | 必填字段、未声明字段、能力清单、启用前必须绑定可用凭证 |
| 脱敏 | 敏感字段 `enc:` 密文保存，响应返回掩码与字段版本 |

不在本模块范围（由业务域实现）：

| 能力 | 归属 |
|---|---|
| 按 operationCode 调用外部系统 | 业务域（如 notice、payment） |
| 令牌获取、缓存与刷新 | 业务域 |
| 回调验签、解密与投递 | 业务域 |
| 调用审计、限流、告警 | 业务域或基础设施 |


## 12. 支付模块迁移映射（模拟验证）

不改动 `mango-payment`，仅用集成应用模型表达支付渠道与合约配置。

| 支付现有对象 | 集成应用模型 | 说明 |
|---|---|---|
| `payment_channel`（channelCode/channelName/environment/provider） | 连接器定义（编码/名称/分类=PAYMENT/厂商） | 环境下沉到实例 |
| `payment_channel_field_template`（fieldCode/label/dataType/required/default） | 连接器 `configSchema` | 支持编码、名称、类型、必填、默认值、枚举选项、说明 |
| `payment_channel_contract`（merchantNo/appId/configValuesJson） | 连接实例 `configValues` | 商户号、AppId、网关、账单拉取方式等作为配置项 |
| `payment_channel_contract_value`（encryptedValue） | 凭证 `SECRET` 字段 | 加密保存、只写不读、按字段版本 |
| `payment_channel_contract_value`（fileId 证书） | 凭证 `CERT` / `FILE` 字段 | **当前仅保存文件标识字符串，未接文件服务** |
| `payment_channel_capability`（methodCode） | 能力清单 + 实例授权 | 支持按方法授予 |
| `payment_channel_capability`（金额区间/终端类型） | 无对应 | 缺口，需要扩展能力属性 |
| `billFetchModes` | 配置项枚举（如 `API,FILE`） | 可用枚举表达 |
| `enabledMethodCodes` | 实例 `capabilities` | 一致 |

模拟结果（不改支付模块）：

- 连接器 `wechat-pay`、`alipay` 已用配置规范与认证规范表达商户号、AppId、网关、签名类型、账单拉取方式、APIv3 密钥、商户私钥证书标识
- 实例 `wechat-pay-main` 绑定凭证与能力子集（`pay.jsapi`、`refund.apply`），回调路径自动生成
- 页面可直接查看与编辑，无需改支付代码

迁移前需补齐：

| 缺口 | 影响 |
|---|---|
| 证书文本长度与格式校验 | 超大证书需限制长度，格式由业务域校验 |
| 字段校验（pattern/min/max/组件类型/分组） | 复杂字段无法在前端约束 |
| 能力属性（金额区间、终端类型、环境矩阵） | 能力授权只能到方法级 |
| 渠道级默认值与合约继承 | 多合约重复填写 |

## 13. 不做什么

- 不处理第三方登录：授权码流程、state 校验、账号绑定、登录态签发属于 `mango-auth`
- 不让租户编辑连接器定义
- 不把业务状态机、模板、菜单放进连接器
- 不用通用 HTTP 连接器替代需要签名/证书/解密的协议
- 不为未联调的协议预置“看起来能配”的字段
