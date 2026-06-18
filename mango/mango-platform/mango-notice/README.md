# 通知 Notice

## 1. 概览

`mango-notice` 是 Mango 的统一通知中心，用来把业务事件转换成站内信、短信、邮件、企业微信、钉钉、微信公众号等渠道通知。

业务模块使用它时只需要关心：

- 用哪个 `bizType` 表示这类通知。
- 通知发给谁。
- 模板需要哪些参数。
- 通知失败后业务是否需要补偿或人工处理。

`mango-notice` 不产生业务事件，也不替代 IM 聊天系统。订单创建、审批完成、任务失败等事件仍由业务模块触发。

## 2. 功能清单

| 能力 | 说明 | 使用入口 |
|------|------|----------|
| 业务通知发送 | 按 `bizType`、接收人和模板参数创建通知任务 | `NoticeApi.send` / `POST /notice/send` |
| 站内信快捷发送 | 管理端或业务端直接发送站内信 | `NoticeApi.sendSiteMessage` / `POST /notice/site/messages` |
| 业务类型管理 | 定义消息 Key、名称、业务域、模板参数和发送策略 | `/notice/business-types/**` |
| 配置版本发布 | 维护草稿、生效版本、历史版本 | `/notice/business-types/{id}/config-*` |
| 渠道模板 | 为站内信、短信、邮件等渠道配置标题和内容模板 | `/notice/business-types/{id}/channel-templates/**` |
| 渠道配置 | 保存第三方账号、Webhook、Secret、签名等配置 | `/notice/channels/**` |
| 任务和发送记录 | 查询任务、每个接收人每个渠道的发送结果 | `/notice/tasks`、`/notice/records` |
| 失败处理 | 支持单条/批量重试、人工成功、忽略失败 | `/notice/records/**` |
| 接收账户 | 维护用户手机号、邮箱、企业微信 ID 等接收账户 | `/notice/recipient-accounts/**` |
| 接收偏好 | 维护用户或范围级渠道开关 | `/notice/receive-preferences` |
| 我的站内信 | 查询未读数、列表、详情、已读和删除 | `/notice/site/my/**` |

## 3. 后端接入

### 3.1 开发依赖

业务模块只需要面向通知 API 编码时，引入 `mango-notice-api`：

```xml
<dependency>
    <groupId>io.mango.platform.notice</groupId>
    <artifactId>mango-notice-api</artifactId>
</dependency>
```

业务代码优先依赖 `NoticeApi`：

```java
import io.mango.notice.api.NoticeApi;
import io.mango.notice.api.command.SendNoticeCommand;

SendNoticeCommand command = new SendNoticeCommand();
command.setBizType("JOB_EXECUTION_FAILED");
command.setBizId("job-1001");
command.getParams().put("jobName", "daily-settle");
command.setUserId(1001L);

noticeApi.send(command);
```

### 3.2 部署依赖

提供通知中心接口和任务执行能力的应用启用 starter：

```xml
<dependency>
    <groupId>io.mango.platform.notice</groupId>
    <artifactId>mango-notice-starter</artifactId>
</dependency>
```

微服务中只远程调用通知中心的应用启用 remote starter：

```xml
<dependency>
    <groupId>io.mango.platform.notice</groupId>
    <artifactId>mango-notice-starter-remote</artifactId>
</dependency>
```

按需引入渠道模块。没有引入对应渠道模块时，只保存渠道配置不会产生实际投递能力。

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

## 4. 前端接入

通知前端能力在 `@mango/notice`：

- `admin-pages`：通知业务配置、渠道管理、发送消息、任务、记录、重试、站内信、全局设置、接收设置页面。
- `admin-shell`：管理后台顶部通知铃铛。
- `client`：客户端铃铛、消息中心、接收设置组件。
- `realtime`：通知实时订阅、桌面通知、声音和语音提醒工具。

注册管理页面：

```ts
import { registerMangoNoticeAdminPages } from '@mango/notice/admin-pages';

registerMangoNoticeAdminPages();
```

注册 Shell 通知铃铛：

```ts
import { registerMangoNoticeAdminShell } from '@mango/notice/admin-shell';

registerMangoNoticeAdminShell();
```

业务前端如果只是读取站内信或未读数，使用 `@mango/notice` 的 API 封装即可。业务通知发送更推荐由业务后端调用 `NoticeApi`，这样可以和业务事务、幂等键、失败补偿放在同一条链路里处理。

## 5. 快速开始

1. 部署通知中心应用，启用 `mango-notice-starter`、`mango-infra-kv` outbox 和需要的渠道模块。
2. 执行 notice、authorization、system、identity、org 等相关 migration。
3. 在通知管理页创建业务类型，例如 `JOB_EXECUTION_FAILED`。
4. 保存业务配置草稿，定义模板参数、默认优先级、幂等策略，并发布。
5. 保存渠道模板，例如站内信标题、站内信内容、邮件标题、邮件内容。
6. 保存渠道配置，例如站内信内置渠道、邮件账号、短信 provider 配置。
7. 业务后端调用 `NoticeApi.send`，传入 `bizType`、`bizId`、接收人和 `params`。
8. 在任务、发送记录、站内信列表里确认发送结果。

## 6. 配置说明

YAML 只配置通知 outbox 分发行为。渠道账号、签名、模板 ID、Webhook、Secret 等运行时配置保存在 `notice_channel_config.config_json`，通过通知渠道管理页面维护。

```yaml
mango:
  notice:
    outbox:
      enabled: true
      dispatch-enabled: true
      worker-id: notice-outbox-worker
      batch-size: 50
      max-attempts: 3
      retry-delay-seconds: 60
      initial-delay-millis: 1000
      fixed-delay-millis: 1000
```

## 7. YAML 配置字段

| 配置项 | 默认值 | 含义 |
|--------|--------|------|
| `mango.notice.outbox.enabled` | `true` | 是否注册通知 outbox dispatcher。 |
| `mango.notice.outbox.dispatch-enabled` | `true` | 是否启动本地 `NoticeOutboxWorker` 后台轮询。 |
| `mango.notice.outbox.worker-id` | `notice-outbox-worker` | claim outbox 消息的 worker 标识。 |
| `mango.notice.outbox.batch-size` | `50` | 每次 claim 的消息数量，小于等于 0 时不会处理消息。 |
| `mango.notice.outbox.max-attempts` | `3` | 单条 outbox 消息最大处理次数。 |
| `mango.notice.outbox.retry-delay-seconds` | `60` | 分发失败或仍有待重试记录时，下次处理延迟秒数。 |
| `mango.notice.outbox.initial-delay-millis` | `1000` | worker 启动后第一次执行延迟。 |
| `mango.notice.outbox.fixed-delay-millis` | `1000` | worker 两次执行之间的固定延迟。 |

## 8. 资源注入

通知中心内置站内信渠道通过 `mango-resource` 注入。通知模块也会声明自己的业务域，业务模块可用同一资源协议向通知中心注入消息模板。资源文件放在：

```text
mango-notice-starter/src/main/resources/META-INF/mango/resources/notice-common-message.yml
mango-notice-starter/src/main/resources/META-INF/mango/resources/notice-common-domain.yml
```

业务模块声明通知模板时，推荐在业务模块自己的 starter 中实现 `ResourceProvider`，并通过 `NoticeMessageTemplateResourceDeclarations.fourChannels(...)` 生成站内信、邮件、企业微信、短信四类模板声明。业务代码只依赖 `mango-notice-api` 和 `mango-resource-api`，不依赖 `mango-resource` 的 core/starter。

### 8.1 MESSAGE_CHANNEL

`MESSAGE_CHANNEL` 落库到 `notice_channel_config`，按 `tenantId + channelType + providerCode` 合并更新。

| 字段 | 类型 | 必填 | 含义 |
|------|------|------|------|
| `id` | `STRING` | 是 | 资源稳定 ID，使用雪花 ID 字符串。 |
| 顶层 `version` | `INT` | 是 | 资源版本，声明内容升级时递增。 |
| `biz-key` | `STRING` | 是 | 资源业务键，例如 `notice.channel.site-internal-default`。 |
| `target-module` | `STRING` | 是 | 固定为 `notice`。 |
| `channelConfigId` | `LONG` | 否 | 通知渠道配置稳定 ID，不填时使用资源 ID。 |
| `tenantId` | `STRING` | 否 | 租户 ID，默认 `1`。 |
| `channelType` | `STRING` | 是 | `SITE`、`SMS`、`EMAIL`、`WECHAT_OFFICIAL`、`WECOM`、`DINGTALK`。 |
| `providerCode` | `STRING` | 是 | 渠道服务商编码，同一租户同一渠道内唯一。 |
| `configName` | `STRING` | 是 | 渠道配置名称。 |
| `configJson` | `STRING` | 否 | 渠道配置 JSON。 |
| `rateLimitConfig` | `STRING` | 否 | 限流配置 JSON。 |
| `enabled` | `BOOLEAN` | 否 | 是否启用，默认 `true`。 |
| `priority` | `INT` | 否 | 优先级，默认 `0`。 |
| `weight` | `INT` | 否 | 权重，默认 `100`。 |
| `configStatus` | `STRING` | 否 | 配置状态，默认 `COMPLETE`。 |
| `lastSendStatus` | `STRING` | 否 | 最近发送状态，默认 `NONE`。 |

### 8.2 MESSAGE_TEMPLATE

`MESSAGE_TEMPLATE` 落库到 `notice_business_type`、`notice_business_config_version` 和 `notice_business_channel_template`，按 `tenantId + bizType + channelType + version` 合并更新。

| 字段 | 类型 | 必填 | 含义 |
|------|------|------|------|
| `id` | `STRING` | 是 | 资源稳定 ID，使用雪花 ID 字符串。 |
| 顶层 `version` | `INT` | 是 | 资源版本，声明内容升级时递增。 |
| `biz-key` | `STRING` | 是 | 资源业务键，例如 `job.notice.execution-failed.site`。 |
| `target-module` | `STRING` | 是 | 固定为 `notice`。 |
| `businessTypeId` | `LONG` | 否 | 通知业务类型稳定 ID，不填时使用资源 ID。 |
| `configVersionId` | `LONG` | 是 | 通知业务配置版本稳定 ID。 |
| `channelTemplateId` | `LONG` | 是 | 通知业务渠道模板稳定 ID。 |
| `tenantId` | `STRING` | 否 | 租户 ID，默认 `1`。 |
| `bizType` | `STRING` | 是 | 通知业务类型编码，同一租户内唯一。 |
| `bizName` | `STRING` | 是 | 通知业务类型名称。 |
| `bizGroup` | `STRING` | 否 | 通知业务分组。 |
| `domainCode` | `STRING` | 否 | 业务域编码，默认 `COMMON`。 |
| `description` | `STRING` | 否 | 通知业务类型说明。 |
| `paramsSchema` | `STRING` | 否 | 通知参数 JSON Schema。 |
| `defaultPriority` | `STRING` | 否 | `LOW`、`NORMAL`、`HIGH`、`URGENT`，默认 `NORMAL`。 |
| `idempotentStrategy` | `STRING` | 否 | 幂等策略。 |
| 字段 `version` | `INT` | 否 | 模板业务版本号，默认顶层资源 `version`。 |
| `versionStatus` | `STRING` | 否 | `DRAFT`、`ACTIVE`、`HISTORY`，默认 `ACTIVE`。 |
| `channelType` | `STRING` | 是 | 通知渠道类型。 |
| `templateName` | `STRING` | 是 | 渠道模板名称。 |
| `titleTemplate` | `STRING` | 是 | 标题模板。 |
| `contentTemplate` | `STRING` | 是 | 内容模板。 |
| `externalTemplateId` | `STRING` | 否 | 第三方渠道模板 ID。 |
| `variableMapping` | `STRING` | 否 | 变量映射 JSON。 |
| `enabled` | `BOOLEAN` | 否 | 是否启用，默认 `true`。 |
| `channelConfigId` | `LONG` | 否 | 绑定渠道配置 ID，空表示自动选择。 |

已接入的默认模板 Provider：

| 模块 | Provider | 主要 `bizType` |
|------|----------|----------------|
| `mango-auth` | `AuthMessageTemplateResourceProvider` | `auth.login.locked`、`auth.login.success` |
| `mango-identity` | `IdentityMessageTemplateResourceProvider` | `identity.user.created`、`identity.password.reset`、`auth.wecom.login.bound`、`auth.wecom.login.unbound` |
| `mango-workflow` | `WorkflowMessageTemplateResourceProvider` | `workflow.task.assigned`、`workflow.task.claimable`、`workflow.task.cc`、`workflow.task.rejected`、`workflow.process.completed`、`workflow.process.rejected`、`workflow.process.ended`、`workflow.task.empty-assignee` |
| `mango-payment` | `PaymentMessageTemplateResourceProvider` | `payment.order.success`、`payment.order.failed`、`payment.refund.success`、`payment.refund.failed`、`payment.refund.approval.created`、`payment.exception.order.created`、`payment.reconciliation.difference`、`payment.settlement.unresolved` |
| `mango-job` | `JobMessageTemplateResourceProvider` | `job.instance.failed`、`job.worker.offline` |

### 8.3 BUSINESS_DOMAIN

`notice-common-domain.yml` 通过 `BUSINESS_DOMAIN` 向业务域模块声明 `NOTICE` 业务域。字段契约以 `mango-domain` 的资源注入说明为准。

## 9. 运行时配置字段

### 9.1 发送通知字段

| 字段 | 含义 |
|------|------|
| `bizType` | 通知业务类型，必填。 |
| `bizId` | 业务对象 ID，用于追踪和幂等。 |
| `params` | 模板参数 Map。 |
| `channelTypes` | 本次指定发送渠道；为空时按业务类型启用模板发送。 |
| `recipients` | 明确接收人列表，可传用户 ID、手机号、邮箱、企微 ID、钉钉 ID 等。 |
| `recipientTargets` | 接收目标，支持 `USER`、`ORG`、`POST`、`ROLE`。 |
| `userId` / `userIds` | 单用户或批量用户快捷发送字段。 |
| `recipientRuleCode` | 接收人规则编码。 |
| `title` / `content` | 未配置业务模板时用于直接发送。 |
| `attachmentFileIds` | 附件文件 ID 列表，只传文件中心标识。 |
| `priority` | 通知优先级，默认 `NORMAL`。 |
| `sendMode` | 发送模式，默认 `IMMEDIATE`。 |
| `scheduledTime` | 定时发送时间。 |
| `idempotentKey` | 幂等键。 |

业务模块不希望通知失败影响主流程时，可发布 `NoticeSendEvent`。本地通知中心由 `mango-notice-starter` 监听事件并调用 `NoticeApi`；微服务调用方由 `mango-notice-starter-remote` 监听事件并远程调用通知中心。事件监听器会记录发送失败日志，但不向上抛出异常阻断业务事务。

### 9.2 接收人字段

| 字段 | 含义 |
|------|------|
| `userId` | 接收用户 ID。 |
| `recipientName` | 接收人名称。 |
| `mobile` | 手机号。 |
| `email` | 邮箱。 |
| `wechatOpenid` | 微信 openid。 |
| `wecomUserId` | 企业微信用户 ID。 |
| `dingtalkUserId` | 钉钉用户 ID。 |
| `externalId` | 外部联系人标识。 |

### 9.3 渠道与模板

| 配置 | 含义 |
|------|------|
| 业务类型 | 定义通知业务 Key、名称、业务域、参数 schema、默认优先级和幂等策略。 |
| 配置版本 | 业务类型的草稿、生效和历史配置。 |
| 渠道模板 | 每个渠道的标题模板、内容模板和发布状态。 |
| 渠道配置 | provider、账号、Secret、Webhook、签名、模板 ID、限流等 JSON 配置。 |
| 接收账户 | 用户手机号、邮箱、企微 ID、钉钉 ID 等接收地址。 |
| 接收偏好 | 用户或范围级渠道开关。 |

## 10. 请求与返回字段

HTTP 根路径：`/notice`。

| 分类 | 接口 | 权限 | 用途 |
|------|------|------|------|
| 发送 | `POST /notice/send` | `notice:task:create` | 按业务类型发送通知。 |
| 站内信 | `POST /notice/site/messages` | `notice:site:create` | 快捷发送站内信。 |
| 业务类型 | `/notice/business-types/**` | `notice:business:*` | 维护业务类型、配置版本和渠道模板。 |
| 渠道配置 | `/notice/channels/**` | `notice:channel:*` | 查询、保存、删除渠道配置。 |
| 内部配置 | `GET /notice/internal/wecom-login-config` | INTERNAL | 认证服务读取企微扫码登录配置。 |
| 任务 | `GET /notice/tasks` | `notice:task:view` | 查询通知任务。 |
| 发送记录 | `GET /notice/records` | `notice:record:view` | 查询发送记录。 |
| 失败处理 | `/notice/records/**` | `notice:retry:edit` | 重试、人工成功、忽略失败。 |
| 设置 | `GET /notice/settings`、`PUT /notice/settings` | `notice:setting:*` | 读取和保存通知设置。 |
| 接收账户 | `/notice/recipient-accounts/**` | `notice:receive-setting:*` | 维护接收账户。 |
| 企业微信 | `POST /notice/wecom/users/sync` | `system:user:add` | 同步企微用户映射。 |
| 接收偏好 | `GET /notice/receive-preferences`、`PUT /notice/receive-preferences` | `notice:receive-setting:*` | 维护接收偏好。 |
| 我的站内信 | `/notice/site/my/**` | `notice:site:*` | 未读数、列表、详情、已读和删除。 |

常用返回对象：

| 返回对象 | 含义 |
|----------|------|
| `NoticeSendResultVO` | 发送入口返回结果。 |
| `NoticeTaskVO` | 通知任务。 |
| `NoticeSendRecordVO` | 每个接收人、每个渠道的发送记录。 |
| `NoticeSiteMessageVO` | 站内信消息。 |
| `NoticeUnreadCountVO` | 未读数量。 |
| `NoticeBusinessTypeVO` | 通知业务类型。 |
| `NoticeBusinessConfigVersionVO` | 业务配置版本。 |
| `NoticeChannelTemplateVO` | 渠道模板。 |
| `NoticeChannelConfigVO` | 渠道配置。 |
| `NoticeRecipientAccountVO` | 接收账户。 |
| `NoticeReceivePreferenceVO` | 接收偏好。 |

## 11. 管理入口

通知中心接口使用 `@ApiAccess` 绑定权限码，菜单和角色授权至少覆盖以下能力：

```text
notice:business:view
notice:business:create
notice:business:edit
notice:business:delete
notice:business:enable
notice:business:publish
notice:channel:view
notice:channel:create
notice:channel:delete
notice:task:create
notice:task:view
notice:record:view
notice:retry:edit
notice:setting:view
notice:setting:edit
notice:receive-setting:view
notice:receive-setting:edit
notice:site:create
notice:site:view
notice:site:edit
notice:site:delete
```

企微用户同步接口复用 `system:user:add`。

前端页面由 `@mango/notice/admin-pages` 注册。菜单 component 需要映射到对应页面 key，例如 `notice/business-config/index`、`notice/channel/index`、`notice/record/index`、`notice/site-message/index`。

## 12. 数据与初始化

Flyway 路径：`mango-notice-core/src/main/resources/db/migration/notice`。

| 脚本 | 内容 |
|------|------|
| `V1__init_notice_site_message.sql` | 初始化通知业务、任务、记录、站内信等基础表。 |
| `V2__notice_business_config_version.sql` | 增加业务配置版本。 |
| `V3__notice_send_record_biz_context.sql` | 补充发送记录业务上下文字段。 |
| `V4__notice_definition_channel_route.sql` | 增加定义和渠道路由相关字段。 |
| `V5__notice_builtin_site_channel.sql` | 内置站内信渠道已迁移到 `mango-resource`。 |
| `V6__notice_site_channel_sound_text.sql` | 补充站内信声音文本。 |
| `V7__notice_task_recipient_targets_snapshot.sql` | 增加接收目标快照。 |
| `V8__notice_builtin_site_channel_default_tenant.sql` | 默认租户站内信渠道已迁移到 `mango-resource`。 |
| `V9__notice_receive_preference.sql` | 初始化接收偏好表。 |
| `V10__seed_admin_recipient_account.sql` | 初始化管理员接收账户。 |
| `V11__seed_email_rich_templates.sql` | 初始化邮件富文本模板。 |
| `V12__notice_wecom_sync_mapping.sql` | 初始化企微同步映射表。 |
| `V13__notice_business_domain.sql` | 通知业务类型接入业务域。 |
| `V14__seed_job_site_message.sql` | 定时任务站内信模板已迁移到 `mango-job` 的 `job-common-message.yml`。 |

核心表包括 `notice_business_type`、`notice_business_config_version`、`notice_business_channel_template`、`notice_channel_config`、`notice_task`、`notice_recipient`、`notice_send_record`、`notice_site_message`、`notice_retry_log`、`notice_callback_log`、`notice_setting`、`notice_recipient_account`、`notice_receive_preference`、`notice_wecom_sync_mapping`。

通知异步分发依赖 `mango-infra-kv` outbox。部署时要确认 outbox 存储可用，否则任务可能创建成功但不会被后台 worker 分发。

## 13. 问题排查

| 问题 | 优先检查 |
|------|----------|
| 任务创建了但没有发送 | `mango.notice.outbox.enabled`、`dispatch-enabled`、outbox 存储、worker 日志。 |
| 站内信未出现 | 是否引入 `mango-notice-channel-site`，业务配置和站内信渠道模板是否已发布。 |
| 第三方渠道失败 | `notice_send_record` 的失败码、失败原因、请求快照、响应快照和渠道配置 JSON。 |
| 用户收不到短信或邮件 | `notice_recipient_account` 是否有手机号或邮箱，接收偏好是否关闭渠道。 |
| 重试无效 | 记录状态是否允许重试，是否超过最大重试次数。 |
| 管理页面 403 | 角色是否有对应 `notice:*` 权限。 |
| 铃铛未显示或未读数不变 | 前端是否注册 `@mango/notice/admin-shell`，站内信接口是否可访问。 |

## 14. 相关文档

- [前端通知包](../../../mango-ui/packages/notice/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
