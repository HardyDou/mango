---
title: ISSUE 641、642 Notice 邮件附件与 Resource 路由账号组设计
status: REVIEW
date: 2026-07-24
issues:
  - https://github.com/HardyDou/mango/issues/641
  - https://github.com/HardyDou/mango/issues/642
deliveryMode: FULL
riskLevel: L3
workspaceDecision: CREATE
branch: fix/issues-641-642-notice-delivery
---

# ISSUE 641、642 Notice 邮件附件与 Resource 路由账号组设计

## 1. 目标与现状

本次合并交付修复 Notice EMAIL 已接收 `attachmentFileIds` 但未投递真实附件的问题，并补齐渠道账号的 Resource 声明、Secret 安全补录、稳定路由标签和账号组路由能力。

`main` 基线存在以下事实：

- `NoticeDeliveryService` 已把任务快照中的附件 ID 写入 `NoticeChannelMessage`。
- `EmailNoticeChannelSender` 的内部消息模型没有附件，SMTP DATA 固定为单一 `text/html` MIME。
- `MESSAGE_CHANNEL` Resource 已能创建或更新渠道配置，但按 `tenantId + channelType + providerCode` 定位，并直接覆盖 `configJson`。
- 渠道模板只支持精确 `channelConfigId` 和空值 AUTO 两种路由。
- 渠道管理接口会掩码常见敏感键，但 Resource、环境 Secret、人工补录和运行时解析之间没有独立契约。

完成后应满足：

1. EMAIL 附件经过 Mango File 的服务内读取契约进入标准 MIME 邮件。
2. 附件读取、限制校验或 MIME 组装失败时发送记录失败，不产生假成功。
3. 渠道 Resource 不携带明文 Secret，重复同步不清空环境内 Secret。
4. 业务模板可在精确账号、稳定路由标签和全局 AUTO 三种模式间互斥选择。
5. 标签路由找不到可用账号时明确失败，不回退到全局 AUTO。
6. 现有精确绑定、未绑定 AUTO 和无附件邮件保持兼容。

## 2. 范围

### 2.1 纳入范围

- `mango-file-api` 的现有 `IFileContentProvider.downloadForService` 消费，不新增下载 URL 旁路。
- `mango-notice-channel-email` 的附件读取、限制校验、MIME 组装、SMTP 投递和结果摘要。
- `mango-notice-api/support/core/starter/starter-remote` 的渠道配置、标签、路由模式、错误和审计契约。
- Notice 数据库 fresh-install schema 和增量 Flyway migration。
- `MESSAGE_CHANNEL` Resource schema、同步合并和 Secret 引用解析。
- `@mango/notice` 渠道管理和消息配置页面。
- 后端单元、集成、API 验证，前端组件与定向 UI 验证。
- Notice README、Resource 声明示例、升级和回滚说明。

### 2.2 不处理范围

- 不建设跨 Mango 全平台的通用 Secret 管理中心。
- 不修改业务项目的保函要素或业务侧发件账号字段。
- 不改变 Notice 的任务调度、重试次数和站内信等其它渠道协议。
- 不在本任务执行 Maven/npm 发布；发布必须走独立 `mango-release` 流程并取得用户授权。
- 不保存附件内容、临时下载地址、预签名 URL 或解析后的 Secret 到发送记录、日志或 API 返回。

## 3. 方案选择

评估过三种方案：

| 方案 | 做法 | 优点 | 缺点 | 结论 |
|---|---|---|---|---|
| 最小 JSON 补丁 | 在现有 `configJson` 和模板上追加 JSON 字段 | 改动少 | 缺少关系约束、引用保护和可靠审计 | 不采用 |
| Notice 一等模型 | 建立稳定编码、标签关系、路由模式和 Secret 分层模型 | 契约清晰、可迁移、可审计 | 涉及数据库、后端和前端 | 采用 |
| 通用 Resource Secret 平台 | 先扩展全平台 Secret 类型和管理中心 | 抽象完整 | 显著扩大范围和发布风险 | 不采用 |

采用 Notice 一等模型，通用扩展点只保留最小的 `NoticeChannelSecretResolver` SPI，避免把本次交付扩大为通用凭据平台。

## 4. 总体架构

```text
MESSAGE_CHANNEL Resource
        |
        v
NoticeChannelResourceHandler
  - 校验非敏感声明
  - 合并 Resource 管理字段
  - 保留环境 Secret
  - 同步账号与标签关系
        |
        v
notice_channel_config + route_tag tables
        |
        +----------------------+
        |                      |
        v                      v
Channel Admin UI        Template Routing UI
  - Secret 补录          - EXACT / TAG / AUTO
  - 来源与漂移            - 标签账号预览
        |                      |
        +----------+-----------+
                   v
           NoticeDeliveryService
          - 候选账号选择与切换
          - 运行时 Secret 解析
                   |
                   v
          EmailNoticeChannelSender
          - Mango File 读取
          - 限制校验
          - multipart/mixed
          - SMTP
```

模块边界如下：

- `mango-notice-api`：公开 Command、VO、枚举和错误语义。
- `mango-notice-support`：渠道发送消息、发送结果和 Secret 解析 SPI。
- `mango-notice-core`：持久化、Resource 合并、标签管理、路由选择、Secret 物化和审计。
- `mango-notice-channel-email`：只负责 EMAIL 配置解释、附件读取与 MIME/SMTP，不访问 Notice 数据库。
- `mango-file-api`：提供服务内文件内容，不向 Notice 暴露存储实现和临时 URL。
- `@mango/notice`：只通过 Notice API 管理账号、标签和模板绑定。

## 5. EMAIL 附件设计

### 5.1 文件读取

`EmailNoticeChannelSender` 注入可选的 `IFileContentProvider`。无附件时不要求 File 能力存在；有附件但未装配 File provider 时返回不可重试的渠道能力失败。

每个附件使用当前任务租户上下文调用：

```java
FileDownloadVO download = fileContentProvider.downloadForService(fileId);
```

只消费 `inputStream`、`fileName`、`contentType` 和 `contentLength`。读取完成或失败时关闭流，不创建仓库内或 evidence 目录临时文件。

### 5.2 限制配置

EMAIL 渠道 `configJson` 增加以下非敏感字段：

| 字段 | 默认值 | 语义 |
|---|---:|---|
| `attachmentMaxCount` | 10 | 单封邮件最大附件数 |
| `attachmentMaxFileSizeBytes` | 10485760 | 单文件最大 10 MiB |
| `attachmentMaxTotalSizeBytes` | 26214400 | 原始附件总大小最大 25 MiB |
| `attachmentReadTimeoutMillis` | 15000 | 单个附件读取超时 |
| `attachmentAllowedContentTypes` | PDF、常见图片、文本、ZIP 和 Office MIME | MIME 白名单，支持 `type/*` |

读取器使用受控并发执行器和超时取消，先校验声明长度，再以“上限 + 1”方式读取流，防止错误的 `contentLength` 绕过限制。数量、单文件和总大小任一超限即停止发送。

允许类型基于文件中心返回的 Content-Type 判断；空类型按 `application/octet-stream` 处理，只有管理员显式允许该类型时才可发送。

### 5.3 MIME 格式

无附件时保留现有单一 HTML MIME。存在附件时生成：

```text
Content-Type: multipart/mixed; boundary="..."

--boundary
Content-Type: text/html; charset=UTF-8
Content-Transfer-Encoding: base64

PGh0bWw+Li4u
--boundary
Content-Type: application/pdf; name*=UTF-8''guarantee.pdf
Content-Disposition: attachment; filename*=UTF-8''guarantee.pdf
Content-Transfer-Encoding: base64

JVBERi0xLjcuLi4=
--boundary--
```

标题、地址和文件名拒绝 CR/LF 注入。文件名移除目录分隔符和控制字符，空文件名按文件 ID 生成，例如 `attachment-1001`。正文与附件 Base64 每行不超过 76 字符，并使用 CRLF。

### 5.4 失败与审计

新增或细化以下失败语义：

- `ATTACHMENT_PROVIDER_UNAVAILABLE`
- `ATTACHMENT_NOT_FOUND_OR_FORBIDDEN`
- `ATTACHMENT_LIMIT_EXCEEDED`
- `ATTACHMENT_TYPE_NOT_ALLOWED`
- `ATTACHMENT_READ_TIMEOUT`
- `ATTACHMENT_READ_FAILED`
- `EMAIL_MIME_BUILD_FAILED`

文件不存在、租户/服务访问失败、超限和类型不允许均不可重试；暂时性存储读取失败和超时可重试；MIME 组装失败不可重试。具体映射以 Mango File 异常码为依据，不通过异常文本猜测。

`ChannelSendResult` 支持安全响应摘要。发送记录的摘要只包含：

```json
{
  "status": "SENT",
  "provider": "SMTP",
  "attachments": [
    {"fileId": "1001", "fileName": "guarantee.pdf", "contentType": "application/pdf", "size": 12345, "status": "ATTACHED"}
  ]
}
```

失败摘要使用同一结构记录已经解析的元数据和失败阶段，不包含文件内容、访问 URL、SMTP 响应中的敏感字段或 Secret。

## 6. 渠道配置与 Secret 设计

### 6.1 渠道稳定身份

`notice_channel_config` 新增不可变 `config_code`。新 Resource 必须声明 `configCode`，新人工配置也必须输入稳定编码；创建后不允许修改。

现有记录按 `LEGACY_` 加原主键的规则回填，例如 `LEGACY_1900000000000000001`；保持原 ID 和模板引用不变。Resource 同步改为按 `tenantId + configCode` 定位，兼容旧声明时可按稳定 `channelConfigId` 定位一次并写入派生编码；不再把 `providerCode` 当账号唯一身份，因此同一 SMTP provider 可有多个账号。

### 6.2 配置分层

渠道配置拆成三部分：

- `config_json`：非敏感 provider 与运行参数。
- `secret_refs_json`：Resource 管理的 Secret 引用，不包含解析值。
- `secret_config_json`：部署环境内人工补录的 Secret 值，任何查询 VO 和日志都不得返回。

已知敏感键至少包括 `password`、`smtpPassword`、`secret`、`appSecret`、`token`、`accessKey`、`accessKeySecret` 和 `secretKey`。Resource 的 `configJson` 出现这些键且值不是掩码时，同步直接失败。

`secretRefs` 格式为键到引用的映射，首批内置支持：

- `env:NAME`：操作系统环境变量。
- `property:path.to.secret`：Spring Environment 属性。

`NoticeChannelSecretResolver` 为扩展 SPI；无法识别的引用类型保持未解析，不降级读取任意文件或远程 URL。

运行时配置合并优先级固定为：

1. Resource 声明的 Secret 引用解析值；
2. 未被引用管理的环境人工补录值；
3. 非敏感 `config_json`。

同一键存在 Secret 引用时，管理员不能用人工值覆盖该键。引用未变时，Resource 重复同步不得清空 `secret_config_json`；引用显式删除后，原人工值仍保留但需重新通过完整性计算才可生效。

### 6.3 完整性与来源

渠道配置新增：

- `resource_id`
- `resource_version`
- `resource_module_code`
- `resource_source`
- `managed_fields_json`
- `secret_status`

`config_status` 不再由 Resource 任意声明为 COMPLETE，而是根据渠道类型/provider 的必填非敏感字段和必需 Secret 计算。缺少或解析失败时为 `INCOMPLETE`，禁止参与发送路由。

Resource 只更新 `managed_fields_json` 列出的字段。人工配置来源为 `MANUAL`；Resource 来源为 `RESOURCE`。Resource 管理的稳定字段在页面只读，Secret 补录和明确允许的运行参数仍可由管理员维护。

## 7. 路由标签与账号组

### 7.1 数据模型

新增：

- `notice_channel_route_tag`：稳定 `tag_code`、展示名、渠道类型、来源和审计信息。
- `notice_channel_config_route_tag`：渠道配置与标签的多对多关联。

同一租户和渠道类型下 `tag_code` 唯一。机器编码创建后不可修改；展示名允许修改。Resource 的 `routeTags` 使用 `{code, name}` 列表，账号同步时 upsert 标签并同步该账号由 Resource 管理的关联，不删除其它来源的人工关联。

业务渠道模板新增：

- `route_mode`：`EXACT`、`TAG`、`AUTO`。
- `route_tag_code`：仅 `TAG` 使用。

约束如下：

| 模式 | `channelConfigId` | `routeTagCode` | 行为 |
|---|---|---|---|
| `EXACT` | 必填 | 空 | 只使用指定账号 |
| `TAG` | 空 | 必填 | 只使用同渠道类型、标签匹配账号 |
| `AUTO` | 空 | 空 | 使用该渠道类型所有可用账号 |

后端对互斥关系做最终校验，前端联动不能代替服务端校验。

### 7.2 候选账号顺序

所有模式都要求候选账号：

- 租户一致；
- `channelType` 一致；
- 已启用；
- `configStatus=COMPLETE`。

精确模式还要求指定账号真实存在；标签模式还要求标签匹配。标签模式无候选账号时返回 `CHANNEL_ROUTE_TAG_UNAVAILABLE`，禁止转 AUTO。

候选账号按以下顺序发送：

1. `priority` 数值较小的优先级组先执行；
2. 同优先级组内按 `weight` 和发送记录 ID 做稳定轮换；
3. 同组最近状态为 `SUCCESS/NONE` 的账号先于 `FAILED`；
4. 可重试失败达到单账号尝试上限后切换下一候选账号；
5. 不可重试的账号配置或消息错误立即终止，不误切换成其它身份发送。

每次尝试更新账号健康状态，发送记录最终保存实际使用的 `channelConfigId` 和配置名快照。

### 7.3 引用保护

删除标签、从账号移除标签、停用或删除账号前，服务返回受影响模板数量和名称。存在 `TAG` 或 `EXACT` 引用时默认拒绝破坏性动作；管理员必须先调整模板，不提供静默级联和 AUTO 回退。

## 8. Resource 声明契约

新声明示例：

```yaml
resources:
  MESSAGE_CHANNEL:
    - id: "2000000000000000642"
      version: 1
      biz-key: guarantee.notice.email.primary
      target-module: notice
      fields:
        configCode: { type: STRING, value: GUARANTEE_EMAIL_PRIMARY }
        channelType: { type: STRING, value: EMAIL }
        providerCode: { type: STRING, value: SMTP }
        configName: { type: STRING, value: 保函电子件主账号 }
        enabled: { type: BOOLEAN, value: true }
        priority: { type: INT, value: 10 }
        weight: { type: INT, value: 100 }
        routeTags:
          type: LIST
          value:
            - { code: GUARANTEE_ELECTRONIC_DELIVERY, name: 保函电子件 }
        configJson:
          type: JSON
          value:
            host: smtp.example.com
            port: 465
            username: guarantee@example.com
            from: guarantee@example.com
            ssl: true
        secretRefs:
          type: JSON
          value:
            password: env:GUARANTEE_SMTP_PASSWORD
```

Resource registry 继续承担声明版本、来源文件、变更日志和同步日志；Notice 保存发送时需要的来源快照和受控字段，不复制 Resource registry 的完整审计模型。

## 9. API 契约

### 9.1 渠道配置

`SaveNoticeChannelConfigCommand` 增加：

- `configCode`
- `routeTagCodes`
- `secretValues`，仅写入，不出现在 VO

`NoticeChannelConfigVO` 增加：

- Resource 来源和版本字段；
- `routeTags`；
- `secretStatus`、缺失 Secret 键名；
- 精确引用和标签引用影响摘要。

`configJson` 只返回非敏感配置。原掩码逻辑保留作为旧数据防线，但新模型不再通过 `***` 回传 Secret 占位。

### 9.2 标签

Notice API 增加标签分页/列表、保存展示名、删除、账号关联影响查询。标签编码和渠道类型创建后不可修改。

### 9.3 模板路由

`SaveNoticeChannelTemplateCommand` 和 `NoticeChannelTemplateVO` 增加 `routeMode`、`routeTagCode`。保留 `channelConfigId` 字段。

兼容旧调用：

- 未传 `routeMode` 且 `channelConfigId` 非空，解释为 `EXACT`。
- 未传 `routeMode` 且 `channelConfigId` 为空，解释为 `AUTO`。

新调用显式传入 `routeMode` 后执行严格互斥校验。

## 10. 数据库迁移

新增 `V2__notice_channel_resource_route_and_secret.sql`，并同步更新 fresh-install `V1__init_notice.sql` 最终结构。

迁移步骤：

1. 新增渠道稳定编码、Resource、Secret 与来源字段。
2. 以 `LEGACY_` 加原主键的规则回填已有 `config_code` 并建立租户唯一索引。
3. 新建路由标签及关联表。
4. 新增模板 `route_mode` 和 `route_tag_code`。
5. 将 `channel_config_id IS NOT NULL` 的模板回填为 `EXACT`，其余回填为 `AUTO`。
6. 从已知旧 `config_json` 敏感键迁移到 `secret_config_json`，并从非敏感 JSON 中移除；无法解析的旧 JSON 保持原值并标记 `INCOMPLETE`，由管理员修复，不丢弃数据。

回滚策略不是删除新列，而是应用回退：旧应用继续读取原 `channel_config_id` 和兼容后的运行时配置；发布恢复时先停止创建 TAG 绑定。已产生 TAG 绑定后若要回退旧版本，必须先把模板改为 EXACT 或 AUTO。数据库物理回退需单独备份和人工授权。

## 11. 管理页面

### 11.1 渠道配置页

列表和详情展示：稳定编码、渠道/provider、来源、Resource 版本、路由标签、Secret 状态、配置完整状态、健康状态、优先级和权重。

编辑行为：

- Resource 管理的稳定字段只读并标明来源。
- Secret 使用密码输入框，仅允许新值写入；接口不回显原值。
- 已配置、缺失和引用解析失败使用明确状态，不用掩码字符串推断。
- 停用、删除或移除标签前展示受影响模板；存在引用时阻止提交。

### 11.2 消息配置页

“通道”区域改为路由模式选择：

- 精确账号：选择同渠道类型账号。
- 路由标签：选择同渠道类型标签，并展示当前匹配账号数和名称。
- 全局 AUTO：保留现有语义和说明。

标签无可用账号时显示阻断提示，保存和发布均由后端再次校验。页面保留加载、空、错误和权限不足状态。

新增稳定测试锚点：

- `data-surface="notice.channel.routing"`
- `data-field="notice.channel.route-mode"`
- `data-field="notice.channel.route-tag"`
- `data-field="notice.channel.secret-status"`
- `data-action="notice.channel.secret-supply"`

## 12. 测试与验收

### 12.1 M09 静态验证

- 直接修改 Maven 模块的 compile/verify 和架构检查。
- `@mango/notice` lint、typecheck、test、build。
- 前端边界、admin 样式聚合和模块样式检查。
- migration、Resource schema 和文档 checker。

### 12.2 M10 单元测试

- 无附件兼容、单附件、多附件、中文和特殊文件名 MIME。
- 数量、单文件、总大小、类型和超时限制。
- SMTP 前失败不产生成功结果。
- Resource 明文 Secret 拒绝、重复同步保留人工 Secret、引用变更语义。
- EXACT、TAG、AUTO 候选过滤、优先级、权重和失败切换。
- 前端三种模式互斥、Secret 状态和引用影响交互。

### 12.3 M11 集成测试

- Mango File 本地服务 provider 读取真实测试文件并由 SMTP 测试服务解析 MIME。
- Resource 首次同步、人工补录、再次同步、引用解析失败与恢复。
- Flyway 从 1.0.25 等价 schema 升级并验证数据回填。
- 标签多账号、无候选账号、主账号失败切换备用账号。

### 12.4 M12 API 验证

- 渠道查询不返回 Secret。
- Resource 管理字段更新限制。
- 标签 CRUD、引用保护和影响摘要。
- 模板三种路由模式、互斥校验和兼容旧请求。

### 12.5 M13 UI 验证

- 渠道页来源、标签、Secret 状态与补录。
- 消息配置页三种路由模式和账号预览。
- 标签无账号、接口错误、权限不足和引用阻断反馈。
- 浏览器 console、network 和关键页面截图。

### 12.6 M16 现场验收边界

自动化 SMTP 可以证明 MIME 和路由行为，但不能证明业务生产邮箱的反垃圾、网关和最终收件箱行为。发布后业务项目仍需使用真实 SMTP 和真实收件箱完成一次附件投递验收；未完成前不得把 Baohan #329 的外部送达判定为完成。

## 13. 兼容、发布与恢复

- 无附件 EMAIL 继续发送单正文 MIME。
- 现有精确 `channelConfigId` 继续生效。
- 现有未绑定模板继续为全局 AUTO。
- 已有人工账号无需迁移为 Resource。
- 新 API 字段均为增量；旧前端和旧业务调用可以继续使用原字段。
- 本次公共 Maven 与 npm 能力变化需要版本升级，但版本号和发布批次由后续 `mango-release` 流程决定。
- 应用回退前必须确认没有仅由 TAG 表达的模板绑定；数据库 migration 不做自动降级。

## 14. 风险与控制

| 风险 | 后果 | 控制 |
|---|---|---|
| 大附件导致内存压力 | worker 内存升高或 OOM | 数量/单文件/总大小硬限制，流式上限读取，受控并发 |
| Resource 同步覆盖 Secret | 渠道全部变为不可用 | Secret 独立存储，受控字段合并，重复同步集成测试 |
| 标签无账号却回退 AUTO | 使用错误发件身份 | TAG 无候选明确失败，禁止隐式回退 |
| 旧 JSON Secret 迁移失败 | 配置不可用或泄漏 | 失败标记 INCOMPLETE，不记录原值，管理员修复入口 |
| 健康状态长期排除账号 | 故障恢复后无法重试 | FAILED 只降低同优先级顺序，不永久排除 |
| 旧客户端字段组合不完整 | 升级后保存失败 | 未传 routeMode 时兼容推导，新请求严格校验 |

## 15. 设计完成条件

本设计在以下条件全部满足后进入 FULL 生命周期资产和实施：

1. 用户确认本文件准确承载已批准的合并方案。
2. 文档不存在占位符、互相矛盾的行为或未决产品选择。
3. 后续 BRD、SRS、TDD、实施计划按 PMO 生命周期生成、校验并保留人工审批边界。
4. 未经用户独立授权，不执行 commit、push、PR 或发布。
