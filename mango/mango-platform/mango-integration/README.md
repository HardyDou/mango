# 集成应用 Integration

## 1. 概览

`mango-integration` 是 Mango 的集成应用模块，负责**定义与声明**，不提供运行时编排。

能力边界：

| 负责 | 不负责 |
|---|---|
| 连接器定义（配置规范、认证规范、能力清单、用途分类、标签） | 按操作调用外部系统 |
| 集成凭证（多字段、加密、掩码、版本、轮换；证书以文本入库并脱敏） | 令牌获取、缓存、刷新策略 |
| 连接实例（配置值、凭证绑定、能力授权、回调路径） | 回调验签、解密、投递、重试 |
| 声明读取（SPI 与认证工具类） | 调用审计、限流、告警 |
| — | 第三方登录（授权码流程、账号绑定、登录态）属 `mango-auth` |

模块结构：

| 模块 | 说明 |
|---|---|
| `mango-integration-api` | 契约：Api、Command、Query、VO、枚举、声明 SPI |
| `mango-integration-support` | 认证工具类：出站认证、令牌缓存、入站验签与解密、统一调用客户端 |
| `mango-integration-core` | 实体、Mapper、Service、声明读取实现、调用记录写入 |
| `mango-integration-starter` | Controller、菜单资源声明、连接器声明处理器、Flyway migration |

## 2. 功能清单

| 能力 | 说明 | 常用入口 |
|---|---|---|
| 连接器定义 | 维护配置规范、认证规范、能力清单、用途分类、标签与文档地址 | `IntegrationConnectorApi`、`/integration/connectors` |
| 定义资源声明 | 按 `INTEGRATION_CONNECTOR` 声明同步连接器，声明缺失时停用不删除 | `IntegrationConnectorResourceHandler` |
| 集成凭证 | 多字段凭证、Secret 加密、证书明文脱敏、版本、轮换记录、到期状态 | `IntegrationCredentialApi`、`/integration/credentials` |
| 连接实例 | 按配置规范校验配置值、绑定凭证、授权能力、生成回调路径 | `IntegrationInstanceApi`、`/integration/instances` |
| 连接测试 | 按实例 `apiBaseUrl` 发起可达性探测，默认拒绝私网目标 | `POST /integration/instances/test-connection` |
| 声明读取 | 业务域读取实例、认证规范与解密凭证，不直接查表 | `IntegrationDeclarationProvider` |
| 认证工具 | 出站 API Key/Bearer/Basic/OAuth2/HMAC/Query Token，入站验签、重放窗口、AES 解密 | `IntegrationAuthConfig`、`IntegrationOutboundAuth`、`IntegrationInboundAuth` |
| 统一调用 | 按声明拼装地址、认证、超时、重试并返回结构化结果 | `IntegrationClient`、`IntegrationCallResult` |
| 记录与清理 | 调用记录、凭证读取审计、轮换记录，支持按保留天数手动清理 | `GET /integration/instances/call-logs`、`POST /integration/maintenance/cleanup` |

凭证类型：

| 类型 | 存储 |
|---|---|
| `STRING` | 明文 |
| `SECRET` | 加密（`enc:` 前缀） |
| `CERT` / `FILE` / `PRIVATE_KEY` | 明文文本入库（不加密），列表与详情脱敏；不使用 fileId 引用 |

能力声明为对象数组，实例授权只保存能力编码，展示时按连接器声明补全名称：

```json
[{ "code": "message.send", "name": "发送应用消息", "description": "向成员发送企业微信应用消息" }]
```

用途分类使用系统字典 `integration_connector_category`：支付、消息、身份、文件、其他。

## 3. 接入方式

业务域只依赖契约与工具类，不依赖 starter：

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

声明读取（SPI，由容器注入）：

```java
IntegrationDeclarationProvider provider = ...;

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
IntegrationOutboundAuth.of(config, tokenCache).apply(requestBuilder, "POST", uri);

IntegrationInboundAuth inbound = IntegrationInboundAuth.of(config);
if (!inbound.verifySignature(params, body) || !inbound.withinReplayWindow(params)) {
    throw new IllegalStateException("回调验签失败");
}
String plain = inbound.decrypt(encryptedPayload);
```

统一调用客户端：

```java
IntegrationClient client = IntegrationClient.of(declarationProvider, "wechat-pay-main", tokenCache, callRecorder);

IntegrationCallResult response = client.post("/v3/pay/transactions/jsapi", payload);
if (!response.isSuccess()) {
    log.warn("调用失败 status={} message={}", response.getStatusCode(), response.getMessage());
}
```

调用约定：

- 接口地址取实例配置的 `apiBaseUrl`，兼容 `baseUrl`、`endpoint`、`gatewayBaseUrl`
- 超时取 `timeoutMs`，重试取 `retryMax`
- 认证按连接器 `authSchema` 自动应用
- 返回状态码、耗时、响应体（截断）与失败原因，不抛业务异常
- 传入 `IntegrationCallRecorder` 时写入 `integration_call_log`，不记录请求体和凭证

## 4. 配置说明

连接测试默认拒绝回环、私网、链路本地和组播地址，避免被用作内网探测。需要访问内网时显式开启：

```yaml
mango:
  integration:
    test:
      allow-private-network: true
```

配置规范支持以下校验规则，由连接器 `configSchema` 声明：

| 规则 | 字段 |
|---|---|
| 必填与未声明字段 | `required`、字段白名单 |
| 枚举取值 | `enum` |
| 正则 | `pattern` |
| 字符串长度 | `minLength` / `maxLength` |
| 数值范围 | `minimum` / `maximum` |
| 系统字典 | `dictCode`（字典不可用时放行） |

字段类型支持单行文本、多行文本、数字、布尔、字典、系统字典、JSON 字符串与文件。

## 5. API 与扩展

| 方法 | 路径 | 权限码 |
|---|---|---|
| GET | `/integration/connectors/page` | `integration:connector:list` |
| GET | `/integration/connectors/detail` | `integration:connector:query` |
| POST | `/integration/connectors` | `integration:connector:edit` |
| GET | `/integration/credentials` | `integration:credential:list` |
| POST | `/integration/credentials` | `integration:credential:add` |
| POST | `/integration/credentials/rotate` | `integration:credential:rotate` |
| GET | `/integration/credentials/rotations` | `integration:credential:list` |
| GET | `/integration/instances/page` | `integration:instance:list` |
| GET | `/integration/instances/detail` | `integration:instance:query` |
| POST | `/integration/instances` | `integration:instance:add` |
| POST | `/integration/instances/change-status` | `integration:instance:edit` |
| POST | `/integration/instances/test-connection` | `integration:instance:edit` |
| GET | `/integration/instances/call-logs` | `integration:instance:list` |
| POST | `/integration/maintenance/cleanup` | `integration:maintenance:cleanup` |

扩展点：

- 新增连接器：优先在声明文件 `integration-connectors.json` 中登记，页面只维护 `source=MANUAL` 的连接器
- 新增认证方式：在连接器 `authSchema` 中声明 `authMode` 与参数，工具类按声明生效
- 业务域运行策略：令牌刷新、回调投递、审计告警由业务域自行实现，模块只提供声明与工具类

凭证到期与轮换：

- 保存与轮换都会写入 `integration_credential_rotation`（版本、更新字段、操作人、时间）
- 过期凭证在查询时返回 `EXPIRED`，且不能用于启用实例
- 业务域读取凭证会写入 `integration_credential_access_log`，只记录凭证编码、字段编码、操作人与来源，不记录原文

历史日志手动清理：

```text
POST /integration/maintenance/cleanup
{"resource": "CALL_LOG|ACCESS_LOG|ROTATION", "retentionDays": 90}
```

- 页面入口：应用定义 → 清理日志
- 早于保留天数的记录会被永久删除，操作不可恢复

## 6. 数据与初始化

Flyway 路径：`mango-integration-starter/src/main/resources/db/migration/integration`，当前为 `V1`–`V5`。

| 版本 | 内容 |
|---|---|
| `V1__init_integration.sql` | 连接器、凭证、实例三张主表 |
| `V2__add_credential_rotation_and_access_log.sql` | 凭证轮换记录与读取审计 |
| `V3__add_integration_call_log.sql` | 调用记录 |
| `V4__add_connector_source.sql` | 连接器来源 `source`（`DECLARED` / `MANUAL`） |
| `V5__add_revision_for_concurrency.sql` | 三张主表乐观锁字段 `revision` |

数据表：

| 表 | 说明 | 租户 |
|---|---|---|
| `integration_connector` | 连接器定义 | 不参与租户过滤 |
| `integration_credential` | 集成凭证 | 按当前租户隔离 |
| `integration_instance` | 连接实例 | 按当前租户隔离 |
| `integration_credential_rotation` | 凭证轮换记录 | 按当前租户隔离 |
| `integration_credential_access_log` | 凭证读取审计 | 按当前租户隔离 |
| `integration_call_log` | 调用记录 | 按当前租户隔离 |

初始化与资源声明：

- 资源清单目录：`mango-integration-starter/src/main/resources/META-INF/mango/resources`
- 连接器声明：`integration-connectors.json`，资源类型 `INTEGRATION_CONNECTOR`，`moduleCode=integration`
- 菜单声明：`integration-common-menu.json`，资源类型 `AUTH_MENU`，`appCode=internal-admin`
- 连接器声明在启动或资源同步时按 `connectorCode` 覆盖保存，保存即生效
- 声明缺失时停用连接器（`status=0`），不删除数据，避免破坏已有实例
- 声明来源的连接器（`source=DECLARED`）禁止页面编辑，接口返回“该连接器由资源声明维护，请在声明文件中修改”
- 页面新建的连接器为 `source=MANUAL`，可自由编辑
- 凭证、实例与日志表不做任何初始化登记，只能由真实业务录入或调用产生

连接器声明字段：

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

## 7. 管理入口

菜单由 `integration-common-menu.json` 声明，挂在“平台能力”下：

```text
平台能力
└── 集成管理  /integration
    ├── 应用定义  /integration/definition
    └── 集成应用  /integration/application
```

页面 key 由 `@mango/admin-pages` 注册：

| 页面 | page key | 组件 |
|---|---|---|
| 应用定义 | `integration/definition/index` | `IntegrationConnectorView` |
| 集成应用 | `integration/application/index` | `IntegrationApplicationView` |
| 微应用总览 | `system/micro-application/index` | `MicroApplicationView` |

权限码：`integration:connector:list|query|edit`、`integration:credential:list|add|rotate`、`integration:instance:list|query|add|edit`、`integration:maintenance:cleanup`。菜单 `apiCodes` 与 Controller 上的 `@ApiAccess` 保持一致。

## 8. 快速开始

```bash
mvn -f mango/pom.xml -pl :mango-integration-core,:mango-integration-support test
mvn -f mango/pom.xml -pl :mango-integration-starter -am -DskipTests compile
```

本地联调：

1. 启动后端：`mango dev start backend`
2. 启动后台：`mango dev start mango-admin`
3. 打开“平台能力 → 集成管理 → 应用定义”，确认连接器清单
4. 在“集成应用”创建实例并绑定凭证，再用“测试连接”验证可达性

## 9. 问题排查

- 连接器无法编辑：检查 `source` 是否为 `DECLARED`，声明来源需修改声明文件后重新同步
- 连接器消失或停用：检查声明文件是否缺少该 `connectorCode`，缺失时只会停用不会删除
- 实例无法保存：检查配置值是否符合 `configSchema`（必填、枚举、正则、长度、数值范围、字典值）
- 实例显示“连接器已停用”：先启用连接器，再启用实例
- 保存提示“已被他人修改”：`revision` 乐观锁冲突，刷新页面后重试
- 凭证不可用：检查凭证是否过期（`EXPIRED`）、是否绑定到实例、Secret 是否已轮换
- 测试连接失败：确认 `apiBaseUrl` 可达；内网目标需开启 `mango.integration.test.allow-private-network`
- 调用记录为空：确认调用方传入了 `IntegrationCallRecorder`
- 清理日志无效：确认保留天数与资源类型，早于保留天数的记录才会删除

## 10. 相关文档

- [业务需求](../../../mango-docs/designs/application-integration-management/business-requirements.md)
- [系统需求](../../../mango-docs/designs/application-integration-management/system-requirements.md)
- [技术设计](../../../mango-docs/designs/application-integration-management/technical-design.md)
- [应用定义规范](../../../mango-docs/designs/application-integration-management/application-definition-spec.md)
- [投产核对清单](../../../mango-docs/guides/operations/integration-production-checklist.md)
- [验收证据](../../../mango-docs/evidence/2026-11-02-application-integration-management/acceptance-evidence.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
