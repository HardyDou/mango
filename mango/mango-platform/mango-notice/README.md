# 通知 Notice

## 1. 概览
`mango-notice` 提供统一通知中心能力：业务通知类型、发布配置版本、渠道模板、渠道配置、通知任务、发送记录、站内信、接收账户、接收偏好和失败重试。

主要使用者是需要给用户发送站内信、短信、邮件、企业微信、钉钉或微信公众号通知的业务模块。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 业务希望用一个 bizType 发送多渠道通知，并通过模板渲染标题和内容 | Maven 依赖 / HTTP API / Java API |
| 管理员需要维护通知业务类型、渠道模板、渠道账号和通知设置 | Maven 依赖 / HTTP API / Java API |
| 业务需要记录每个接收人、每个渠道的发送结果，并支持失败重试、人工成功、忽略失败 | Maven 依赖 / HTTP API / Java API |
| 用户需要维护自己的短信、邮件、企微等接收账户和接收偏好 | Maven 依赖 / HTTP API / Java API |
| 站内信需要落库，前端需要查询未读数、列表、详情、已读和删除 | Maven 依赖 / HTTP API / Java API |

## 3. 适用场景
- 业务希望用一个 `bizType` 发送多渠道通知，并通过模板渲染标题和内容。
- 管理员需要维护通知业务类型、渠道模板、渠道账号和通知设置。
- 业务需要记录每个接收人、每个渠道的发送结果，并支持失败重试、人工成功、忽略失败。
- 用户需要维护自己的短信、邮件、企微等接收账户和接收偏好。
- 站内信需要落库，前端需要查询未读数、列表、详情、已读和删除。

## 4. 边界说明
- 不产生业务事件；订单创建、审批完成、任务失败等事件由业务模块触发。
- 不保证第三方账号、签名、模板审核一定可用；外部服务错误会记录到发送结果里。
- 不替代 IM 长连接聊天系统；站内信是通知消息，不是实时会话。
- 不负责用户手机号、邮箱、企微 ID 的主数据维护，只读取或保存通知接收账户。

## 5. 模块组成
- `mango-notice-api`：`NoticeApi`、发送命令、配置命令、查询对象、VO 和枚举。
- `mango-notice-core`：通知业务、渠道、任务、发送记录、站内信、接收偏好、设置和 outbox 调度逻辑。
- `mango-notice-support`：`NoticeChannelSender` SPI 和渠道发送命令、结果模型。
- `mango-notice-starter`：`NoticeAutoConfiguration`、`NoticeOutboxAutoConfiguration`、`NoticeProperties`、`NoticeOutboxWorker`。
- `mango-notice-starter-remote`：`NoticeFeignClient`，供微服务远程发送通知。
- 渠道模块：`mango-notice-channel-site`、`email`、`sms`、`wecom`、`dingtalk`、`wechat-official`。

业务模块负责定义通知业务编码、准备模板参数、选择接收人，并处理“通知失败后业务是否回滚”的策略。

## 6. 接入方式
提供通知中心接口和任务执行的服务引入 starter：

```xml
<dependency>
    <groupId>io.mango.platform.notice</groupId>
    <artifactId>mango-notice-starter</artifactId>
</dependency>
```

按需引入渠道模块：

```xml
<dependency>
    <groupId>io.mango.platform.notice</groupId>
    <artifactId>mango-notice-channel-site</artifactId>
</dependency>
<dependency>
    <groupId>io.mango.platform.notice</groupId>
    <artifactId>mango-notice-channel-email</artifactId>
</dependency>
<dependency>
    <groupId>io.mango.platform.notice</groupId>
    <artifactId>mango-notice-channel-sms</artifactId>
</dependency>
```

只做远程发送的服务引入 remote starter：

```xml
<dependency>
    <groupId>io.mango.platform.notice</groupId>
    <artifactId>mango-notice-starter-remote</artifactId>
</dependency>
```

业务代码优先注入 `NoticeApi`，调用 `send(SendNoticeCommand)`。

## 7. 配置说明
配置前缀：`mango.notice`。

| 配置项 | 类型 | 默认值 | 含义 |
|--------|------|--------|------|
| `outbox.enabled` | boolean | `true` | 是否注册通知 outbox 分发器相关 Bean。 |
| `outbox.dispatch-enabled` | boolean | `true` | 是否启动本地 `NoticeOutboxWorker` 后台轮询。关闭后可由外部调度触发 dispatcher。 |
| `outbox.worker-id` | string | `notice-outbox-worker` | outbox claim 使用的 worker 标识，也会用于线程名。 |
| `outbox.batch-size` | int | `50` | 每次从 outbox claim 的消息数；小于等于 0 时不会处理消息。 |
| `outbox.max-attempts` | int | `3` | 单条 outbox 消息最大处理次数，超过后会终止等待重试记录。 |
| `outbox.retry-delay-seconds` | long | `60` | 分发失败或仍有待重试记录时，下次可处理时间延迟秒数。 |
| `outbox.initial-delay-millis` | long | `1000` | worker 启动后第一次调度延迟。 |
| `outbox.fixed-delay-millis` | long | `1000` | worker 两次调度之间的固定延迟。 |

配置示例：

```yaml
mango:
  notice:
    outbox:
      enabled: true
      dispatch-enabled: true
      worker-id: notice-1
      batch-size: 50
      max-attempts: 3
      retry-delay-seconds: 60
```

渠道账号、签名、模板 ID、Webhook、Secret 等不是 YAML 配置，而是通过 `/notice/channels` 保存到 `notice_channel_config.config_json`。不同渠道模块读取自己的 `channelConfigJson`。

## 8. API 与扩展
HTTP 根路径：`/notice`。

| 分类 | 接口 | 权限 | 用途 |
|------|------|------|------|
| 发送 | `POST /notice/send` | `notice:task:create` | 按业务类型发送通知。 |
| 发送 | `POST /notice/site/messages` | `notice:site:create` | 管理端快捷发送站内信。 |
| 业务配置 | `/notice/business-types/**` | `notice:business:*` | 维护业务类型、发布配置版本和渠道模板。 |
| 渠道配置 | `GET /notice/channels` | `notice:channel:view` | 查询渠道配置。 |
| 渠道配置 | `POST /notice/channels` | `notice:channel:create` | 保存渠道配置。 |
| 渠道配置 | `DELETE /notice/channels` | `notice:channel:delete` | 删除渠道配置。 |
| 内部接口 | `GET /notice/internal/wecom-login-config` | INTERNAL | 认证服务读取企微扫码登录配置。 |
| 任务记录 | `GET /notice/tasks` | `notice:task:view` | 查询通知任务。 |
| 发送记录 | `GET /notice/records` | `notice:record:view` | 查询每个接收人、每个渠道的发送记录。 |
| 失败处理 | `/notice/records/**` | `notice:retry:edit` | 重试、批量重试、人工成功、忽略失败。 |
| 设置 | `GET/PUT /notice/settings` | `notice:setting:*` | 读取和保存通知设置。 |
| 接收账户 | `/notice/recipient-accounts/**` | `notice:receive-setting:*` | 维护用户短信、邮件、企微等接收账户。 |
| 企业微信 | `POST /notice/wecom/users/sync` | `system:user:add` | 同步企微用户映射。 |
| 接收偏好 | `GET/PUT /notice/receive-preferences` | `notice:receive-setting:*` | 维护用户或范围级接收偏好。 |
| 站内信 | `/notice/site/my/**` | `notice:site:*` | 查询我的站内信、未读数、已读和删除。 |

Java 契约：

- `NoticeApi.send`：业务发送入口。
- `NoticeApi.sendSiteMessage`：站内信快捷发送。
- `NoticeChannelSender`：扩展新渠道时实现 `channelType()` 和 `send(ChannelSendCommand)`。
- `ChannelSendCommand`：包含任务、发送记录、接收人、手机号、邮箱、企微 ID、标题、内容、附件、业务类型、业务 ID、模板参数、渠道配置 JSON 等。
- `ChannelSendResult`：返回成功、第三方消息 ID、失败码、失败原因、是否可重试和响应快照。

## 9. 数据与初始化
Flyway 路径：`mango-notice-core/src/main/resources/db/migration/notice`。

核心表：

| 表 | 用途 |
|----|------|
| `notice_business_type` | 通知业务类型定义。 |
| `notice_business_config_version` | 业务发送配置版本，支持草稿、生效和历史版本。 |
| `notice_business_channel_template` | 业务在不同渠道下的模板。 |
| `notice_channel_config` | 渠道账号和 provider 配置。 |
| `notice_task` | 通知任务主表。 |
| `notice_recipient` | 任务接收人快照。 |
| `notice_send_record` | 每个接收人、每个渠道的发送记录。 |
| `notice_site_message` | 站内信消息。 |
| `notice_retry_log` | 重试日志。 |
| `notice_callback_log` | 第三方回调日志。 |
| `notice_setting` | 通知设置。 |
| `notice_audit_log` | 操作审计。 |
| `notice_recipient_account` | 用户接收账户。 |
| `notice_receive_preference` | 接收偏好。 |
| `notice_wecom_sync_mapping` | 企业微信同步映射。 |

初始化数据：

- `V5__notice_builtin_site_channel.sql` 和 `V8__notice_builtin_site_channel_default_tenant.sql` 初始化内置站内信渠道。
- `V9__notice_receive_preference.sql` 和 `V10__seed_admin_recipient_account.sql` 初始化接收账户相关数据。
- `V11__seed_email_rich_templates.sql` 初始化邮件富文本模板。
- `V13__notice_business_domain.sql` 接入通知业务域。
- `V14__seed_job_site_message.sql` 初始化定时任务站内信业务类型、配置版本和渠道模板。

发送任务通过 `mango-infra-kv` 的 outbox 存储异步分发；`NoticeOutboxDispatcher` claim 消息后调用 `noticeService.executeTask(taskId)`，并按失败情况 ack 或 nack。

## 10. 管理入口
通知中心接口显式接入 `@ApiAccess` 权限码，管理菜单必须覆盖以下能力组：

- `notice:business:view`
- `notice:business:create`
- `notice:business:edit`
- `notice:business:delete`
- `notice:business:enable`
- `notice:business:publish`
- `notice:channel:view`
- `notice:channel:create`
- `notice:channel:delete`
- `notice:task:create`
- `notice:task:view`
- `notice:record:view`
- `notice:retry:edit`
- `notice:setting:view`
- `notice:setting:edit`
- `notice:receive-setting:view`
- `notice:receive-setting:edit`
- `notice:site:create`
- `notice:site:view`
- `notice:site:edit`
- `notice:site:delete`

企微用户同步接口复用 `system:user:add`。

通知数据按租户和当前用户隔离。站内信用户侧接口通过当前登录用户过滤；outbox 分发时会从消息 header 或任务表恢复 `tenant_id`，确保异步执行仍在正确租户上下文里。

## 11. 快速开始
1. 引入 `mango-notice-starter` 和需要的渠道模块。
2. 在通知中心创建业务类型，例如 `JOB_EXECUTION_FAILED`。
3. 保存业务配置草稿，定义参数 schema、默认优先级和幂等策略，并发布。
4. 为站内信、短信、邮件等渠道保存模板并发布。
5. 保存渠道配置，例如站内信内置渠道、短信 provider 配置、邮件 provider 配置。
6. 业务发生事件时调用 `NoticeApi.send`，传入 `bizType`、`bizId`、接收人和模板参数。
7. 验收 `notice_task`、`notice_send_record`、站内信或第三方发送结果。

## 12. 问题排查
- 任务创建了但没有发送：检查 `mango.notice.outbox.enabled`、`dispatch-enabled`、outbox 表和 worker 日志。
- 第三方渠道失败：先看 `notice_send_record.fail_code`、`fail_reason`、`response_snapshot`，再查渠道配置 JSON。
- 站内信未出现：确认已引入 `mango-notice-channel-site`，业务配置和渠道模板已发布。
- 用户收不到短信或邮件：检查 `notice_recipient_account` 是否有对应手机号或邮箱，接收偏好是否关闭该渠道。
- 重试无效：确认记录状态属于失败、等待重试或最终失败，并且未超过最大重试次数。

## 13. 相关文档
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史资料
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
