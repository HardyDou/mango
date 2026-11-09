# mango-integration

集成应用模块：负责**定义与声明**，不提供运行时调用。

## 能力边界

| 负责 | 不负责 |
|---|---|
| 连接器定义（配置规范、认证规范、能力清单、用途分类、标签） | 按操作调用外部系统 |
| 集成凭证（多字段、加密、掩码、版本、轮换；证书以文本入库并脱敏） | 令牌获取、缓存、刷新 |
| 连接实例（配置值、凭证绑定、能力授权、回调路径） | 回调验签、解密、投递、重试 |
| 声明读取（SPI 与认证工具类） | 调用审计、限流、告警 |
| — | 第三方登录（授权码流程、账号绑定、登录态）属 `mango-auth` |

## 模块结构

| 模块 | 说明 |
|---|---|
| `mango-integration-api` | 契约：Api、Command、Query、VO、枚举 |
| `mango-integration-support` | 认证工具类：出站认证、令牌缓存、回调验签与解密 |
| `mango-integration-core` | 实体、Mapper、Service |
| `mango-integration-starter` | Controller、菜单资源声明、Flyway migration |

## 接口

| 方法 | 路径 | 权限码 |
|---|---|---|
| GET | `/integration/connectors/page` | `integration:connector:list` |
| GET | `/integration/connectors/detail` | `integration:connector:query` |
| POST | `/integration/connectors` | `integration:connector:edit` |
| GET | `/integration/credentials` | `integration:credential:list` |
| POST | `/integration/credentials` | `integration:credential:add` |
| POST | `/integration/credentials/rotate` | `integration:credential:rotate` |
| GET | `/integration/instances/page` | `integration:instance:list` |
| GET | `/integration/instances/detail` | `integration:instance:query` |
| POST | `/integration/instances` | `integration:instance:add` |
| POST | `/integration/instances/change-status` | `integration:instance:edit` |

## 凭证类型

| 类型 | 存储 |
|---|---|
| `STRING` | 明文 |
| `SECRET` | 加密（`enc:` 前缀） |
| `CERT` / `FILE` / `PRIVATE_KEY` | 明文文本入库（不加密），列表与详情脱敏；不使用 fileId 引用 |

## 能力声明

能力为对象数组，包含中文名称、能力编码与介绍：

```json
[{ "code": "message.send", "name": "发送应用消息", "description": "向成员发送企业微信应用消息" }]
```

实例授权只保存能力编码，展示时按连接器声明补全名称。

## 用途分类

使用系统字典 `integration_connector_category`：支付、消息、身份、文件、其他。

## 配置规范校验

- 必填字段与未声明字段校验
- 枚举取值校验
- 正则（`pattern`）
- 字符串长度（`minLength` / `maxLength`）
- 数值范围（`minimum` / `maximum`）

## 数据表

`integration_connector`、`integration_credential`、`integration_instance`。
连接器定义表不参与租户过滤；凭证与实例按当前租户隔离。

## 连接器定义声明

连接器定义可通过资源声明同步，无需在页面手工创建：

- 资源类型：`INTEGRATION_CONNECTOR`
- 声明文件：`mango-integration-starter/src/main/resources/META-INF/mango/resources/integration-connectors.json`
- 启动或资源同步时按 `connectorCode` 覆盖保存，保存即生效
- 声明缺失时停用连接器（`status=0`），不删除数据，避免破坏已有实例
- 声明来源的连接器（`source=DECLARED`）禁止页面编辑，接口返回“该连接器由资源声明维护，请在声明文件中修改”
- 页面新建的连接器为 `source=MANUAL`，可自由编辑

声明字段：

| 字段 | 必填 | 说明 |
|---|---|---|
| `connectorCode` | 是 | 连接器编码 |
| `connectorName` | 是 | 连接器名称 |
| `category` | 否 | 用途分类，取值来自字典 `integration_connector_category` |
| `provider` | 否 | 厂商标识 |
| `status` | 否 | 0-停用，1-启用，默认 1 |
| `configSchema` | 否 | 配置规范 JSON Schema 文本 |
| `authSchema` | 否 | 认证规范 JSON 文本 |
| `capabilitiesJson` | 否 | 能力清单 JSON 数组文本 |
| `tags` | 否 | 标签列表 |
| `docsUrl` | 否 | 文档地址 |

## 菜单

```text
平台能力
└── 集成管理
    ├── 应用定义   /integration/definition
    └── 集成应用   /integration/application
```

## 凭证到期与轮换记录

- 凭证保存与轮换都会写入 `integration_credential_rotation`（版本、更新字段、操作人、时间）
- 过期凭证在查询时返回 `EXPIRED`，且不能用于启用实例
- 业务域读取凭证会写入 `integration_credential_access_log`（凭证编码、字段编码、操作人、来源），不记录原文
- 轮换记录查询：`GET /integration/credentials/rotations?credentialCode=...`

## 历史日志手动清理

```text
POST /integration/maintenance/cleanup
{"resource": "CALL_LOG|ACCESS_LOG|ROTATION", "retentionDays": 90}
```

- 页面入口：应用定义 → 清理日志
- 权限码：`integration:maintenance:cleanup`
- 早于保留天数的记录会被永久删除，操作不可恢复

投产核对清单见 `mango-docs/guides/operations/integration-production-checklist.md`。

## 连接测试边界

连接测试按实例配置的 `apiBaseUrl` 发起请求，默认拒绝回环、私网、链路本地和组播地址，避免被用作内网探测。

需要访问内网时显式开启：

```yaml
mango:
  integration:
    test:
      allow-private-network: true
```

## 统一调用客户端

业务域不需要自己拼 HTTP、加认证或重试：

```java
IntegrationClient client = IntegrationClient.of(declarationProvider, "wechat-pay-main", tokenCache, callRecorder);

IntegrationCallResult response = client.post("/v3/pay/transactions/jsapi", payload);
if (!response.isSuccess()) {
    log.warn("调用失败 status={} message={}", response.getStatusCode(), response.getMessage());
}
```

约定：

- 接口地址取实例配置的 `apiBaseUrl`，兼容 `baseUrl`、`endpoint`、`gatewayBaseUrl`
- 超时取 `timeoutMs`，重试取 `retryMax`
- 认证按连接器 `authSchema` 自动应用
- 返回状态码、耗时、响应体（截断）与失败原因，不抛业务异常
- 传入 `IntegrationCallRecorder` 时写入 `integration_call_log`（实例、方法、路径、状态码、耗时、结果），不记录请求体和凭证
- 调用记录查询：`GET /integration/instances/call-logs?instanceCode=...&limit=50`

## 业务域接入

```xml
<dependency>
  <groupId>io.mango.platform.integration</groupId>
  <artifactId>mango-integration-api</artifactId>
</dependency>
<dependency>
  <groupId>io.mango.platform.integration</groupId>
  <artifactId>mango-integration-support</artifactId>
</dependency>
```

声明读取（SPI）：

```java
IntegrationDeclarationProvider provider = ...; // 由容器注入

IntegrationInstanceDeclarationVO instance = provider.instance("wechat-pay-main");
List<IntegrationCredentialFieldVO> credential = provider.credentials(instance.getCredentialCode());
String authSchema = provider.connectorAuthSchema(instance.getConnectorCode());

IntegrationAuthConfig config = IntegrationAuthConfig.of(
        authSchema,
        credential.stream().collect(Collectors.toMap(
                IntegrationCredentialFieldVO::getFieldCode,
                IntegrationCredentialFieldVO::getFieldValue)));
```

认证工具：

```java
IntegrationAuthConfig config = IntegrationAuthConfig.of(authSchemaJson, credentialMap);

IntegrationOutboundAuth.of(config, tokenCache).apply(requestBuilder, "POST", uri);

IntegrationInboundAuth inbound = IntegrationInboundAuth.of(config);
if (!inbound.verifySignature(params, body) || !inbound.withinReplayWindow(params)) {
    throw new IllegalStateException("回调验签失败");
}
String plain = inbound.decrypt(encryptedPayload);
```

## 验证

```bash
mvn -f mango/pom.xml -pl :mango-integration-core test
mvn -f mango/pom.xml -pl :mango-integration-starter -am -DskipTests compile
```
