# 通知 Notice

## 1. 概览

`mango-notice` 是 Mango 的统一通知管理能力，用来把业务事件转换成站内信、短信、邮件、企业微信、钉钉、微信公众号等渠道通知。

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
| 个人可用消息类型 | 查询当前租户已启用的消息类型 | `/notice/site/business-types` |
| 我的站内信 | 查询未读数、未读分类统计、分类列表、详情、已读和删除 | `/notice/site/my/**` |

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

### 3.3 服务职责边界

`NoticeService` 只作为 `INoticeService` 的兼容门面，负责入口参数校验和职责转发，不再直接持有 Mapper 或实现业务事务。核心实现按能力拆分：

| 服务 | 职责 |
|------|------|
| `NoticeConfigurationService` | 业务类型、配置版本、渠道模板和渠道配置 |
| `NoticeDeliveryService` | 任务创建、接收人解析、模板渲染、渠道路由和实际投递 |
| `NoticeRecordOperationService` | 任务/记录查询、失败重试、人工成功和忽略失败 |
| `NoticeRecipientSettingService` | 全局设置、接收账户和接收偏好 |
| `NoticeSiteMessageService` | 我的站内信、已读状态和动作请求 |
| `NoticeWecomSyncService` | 企业微信用户同步 |
| `NoticeAnnouncementService` | 公告草稿、发布、下线和接收快照 |

只有完整实现 `MangoTypedCrudService<E,C,U,Q,V,ID>` 标准契约的单实体 CRUD 服务才使用 `MangoCrudServiceImpl`。Notice 的配置、公告、接收设置都包含发布、状态流转或多实体事务，不是标准单实体 CRUD，因此保持领域服务实现，避免为了继承基类而扭曲接口和事务边界。

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

### 4.1 权限边界

通知中心把个人通知体验和后台通知管理分开授权：

| 场景 | 访问模式 | 是否需要为角色/用户单独配置 |
|------|----------|------------------------------|
| 我的站内信、未读数、标记已读、删除我的消息 | `LOGIN` | 不需要。所有已登录用户默认可用通知铃铛和消息中心。 |
| 我的公告、我的接收偏好、个人可用消息类型 | `LOGIN` | 不需要。接口只读取或修改当前登录人的数据。 |
| 接收账户管理 | `PERMISSION` | 需要。接收账号绑定能力不在个人通知设置中提供。 |
| 业务通知配置、渠道配置、任务、记录、重试、后台发送、全局设置 | `PERMISSION` | 需要。按通知管理员、运维或业务运营角色授权。 |

`LOGIN` 只表示当前登录人可以操作自己的通知数据，不表示可以查看全租户通知任务或替其他用户管理配置。后台发送系统消息、维护渠道密钥、查看发送记录仍必须配置 `notice:*` 权限码。

个人站内信、个人公告、接收偏好和个人可用消息类型不校验 `notice:*` 权限码。接收偏好服务始终使用登录上下文中的用户 ID，即使客户端提交其它 `userId` 也不能读取或修改他人偏好。`ROLE_ANONYMOUS` 不包含个人消息或 Realtime 建连权限。

## 5. 快速开始

1. 部署通知中心应用，启用 `mango-notice-starter`、`mango-infra-kv` outbox 和需要的渠道模块。
2. 执行 notice、authorization、system、identity、org 等相关 migration。
3. 在通知管理页创建业务类型，例如 `JOB_EXECUTION_FAILED`。
4. 保存业务配置草稿，定义模板参数、默认优先级、幂等策略，并发布。
5. 保存渠道模板，例如站内信标题、站内信内容、邮件标题、邮件内容。
6. 保存渠道配置，例如站内信内置渠道、邮件账号、短信 provider 配置。
7. 业务后端调用 `NoticeApi.send`，传入 `bizType`、`bizId`、接收人和 `params`。
8. 在任务、发送记录、站内信列表里确认发送结果。

### 5.1 站内信动作协议

站内信动作协议用于让一条站内信携带业务对象、命名目标和操作按钮。业务方发送消息时一次性声明这些结构，通知中心负责保存、展示、校验、幂等提交和事件转发。

适用场景：

| 场景 | 推荐交互 | 说明 |
|------|----------|------|
| 进入业务页面查看或处理 | `ROUTE` | 打开已注册的命名页面目标，允许重复进入。 |
| 进入自定义交互流程 | `FLOW` | 打开业务前端注册的命名流程目标，允许重复进入。 |
| 后台提交一个业务命令 | `EVENT` | 点击后提交动作请求，进入 `PROCESSING`，防重复点击。 |

发送时不要把业务 ID、对象 ID、任务 ID、订单号等隐藏上下文拼到消息正文里。正文只写用户需要阅读的内容；业务上下文放到 `messageSubject`、`messageTarget.params`、`messageData` 和动作 `target.params` 中。站内信列表和详情不会默认展示这些隐藏上下文，点击按钮时会把它们结构化传回业务目标或业务事件。

#### 5.1.1 发送字段

| 字段 | 必填 | 用途 | 展示给用户 |
|------|------|------|------------|
| `bizType` | 是 | 通知业务类型，关联模板、优先级、幂等策略和业务域配置 | 通常不直接展示 |
| `bizId` | 否 | 当前业务记录键，进入事件的 `businessKey` | 不默认展示 |
| `params` | 否 | 模板渲染参数，用于标题和正文模板 | 渲染后的标题/正文会展示 |
| `messageScene` | 否 | 消息场景，例如 `workflow.task.assigned` | 不默认展示 |
| `messageSubject.subjectType` | 否 | 业务对象类型，例如 `WORKFLOW_TASK` | 不默认展示 |
| `messageSubject.subjectId` | 否 | 业务对象 ID，例如任务 ID、订单 ID | 不默认展示 |
| `messageSubject.subjectName` | 否 | 业务对象名称快照，用于必要的用户可读摘要 | 可在流程弹窗显示 |
| `messageTarget.targetType` | 否 | 消息默认目标，取值 `NONE`、`ROUTE`、`FLOW` | 不直接展示 |
| `messageTarget.targetKey` | 否 | 命名目标键或应用内绝对路径，例如 `workflow:task-detail`、`/workflow/task/detail` | 不直接展示 |
| `messageTarget.params` | 否 | 消息级隐藏参数，所有动作默认继承 | 不默认展示 |
| `messageData` | 否 | 业务扩展数据，例如流程实例、任务、订单、锁定记录 | 不默认展示 |
| `messageActions` | 否 | 站内信按钮列表 | 展示按钮名称 |

`targetKey` 支持业务前端提前注册的命名目标，也支持经过前端安全校验且当前用户可访问的应用内绝对路径。协议地址、协议相对地址、反斜杠和控制字符不会用于跳转；目标未注册、路径不安全或当前用户无权访问时，前端会提示“目标未注册或当前无权访问”。

#### 5.1.2 动作字段

| 字段 | 必填 | 用途 |
|------|------|------|
| `actionCode` | 是 | 动作稳定编码，同一条消息内不能重复。 |
| `actionLabel` | 是 | 业务动作名称；详情弹窗会优先按消息场景生成“去审批”“去领取”“查看申请”“查看资料”等用户文案，无法识别场景时回退到该字段。 |
| `interactionType` | 否 | `EVENT` 或 `ROUTE`，默认 `EVENT`。`FLOW` 通过 `interactionType=ROUTE` 加 `target.targetType=FLOW` 表达。 |
| `eventType` | `EVENT` 必填 | 后端点击后发布的领域事件类型。 |
| `target` | `ROUTE/FLOW` 必填 | 动作自己的目标，可覆盖消息默认目标；`ROUTE` 可使用命名目标或应用内绝对路径。 |
| `target.params` | 否 | 动作级隐藏参数，会和消息级参数合并后传回。 |
| `confirmRequired` | 否 | `EVENT` 点击前是否弹确认框。 |
| `inputSchema` | 否 | 动作输入 JSON Schema，预留给需要表单输入的动作。 |
| `sortOrder` | 否 | 按钮排序。 |
| `expireTime` | 否 | 动作过期时间。 |

#### 5.1.3 点击后的参数回传

不管 `ROUTE`、`FLOW` 还是 `EVENT`，前端点击按钮时都会整理同一份隐藏上下文：

```json
{
  "bizType": "WORKFLOW_TASK_ASSIGNED",
  "bizId": "WF-20260704-001",
  "bizGroup": "WORKFLOW",
  "bizName": "工作流待办",
  "messageScene": "workflow.task.assigned",
  "messageId": "1234567890",
  "actionCode": "OPEN_TASK",
  "subject": {
    "subjectType": "WORKFLOW_TASK",
    "subjectId": "TASK-001",
    "subjectName": "费用报销审批"
  },
  "data": {
    "processInstanceId": "PI-001",
    "taskId": "TASK-001"
  },
  "processInstanceId": "PI-001",
  "taskId": "TASK-001"
}
```

合并顺序是：消息级 `messageTarget.params` -> 动作级 `target.params` -> 通知中心补充的标准上下文。业务方放在 `messageData` 中的数据会保留在 `data` 下；业务方放在目标参数中的数据会直接出现在顶层，便于页面、流程或事件订阅方读取。

#### 5.1.4 ROUTE

`ROUTE` 用于进入已注册的命名页面目标或经过校验的应用内绝对路径。它不提交后端业务命令，点击后不会自动改变动作状态，也不会禁用按钮。用户关闭页面后可以再次点击进入。

业务方需要准备：

1. 发送消息时设置 `target.targetType=ROUTE`。
2. 设置 `target.targetKey` 为前端已注册的命名页面目标，或以单个 `/` 开头的应用内绝对路径。
3. 把页面需要的业务参数放入 `target.params` 或 `messageTarget.params`。
4. 前端命名目标读取参数后自行加载业务数据和校验权限。

#### 5.1.5 FLOW

`FLOW` 用于进入自定义交互流程，例如安全处置、支付异常处理、补偿确认、跨模块处理向导。它不是页面地址，也不等同于后端事件。`FLOW` 本身仍是前端交互入口，点击后不会自动禁用按钮。

业务方需要准备：

1. 发送消息时设置 `interactionType=ROUTE`。
2. 设置动作 `target.targetType=FLOW`。
3. 设置动作 `target.targetKey` 为业务前端注册的流程键，例如 `auth:security:resolve`。
4. 把流程所需业务参数放入 `target.params`。
5. 业务前端或微前端注册对应流程处理器；未注册时会进入兜底提示。

跨微前端时，仍使用 `targetKey + params` 的命名协议。宿主或微前端运行时负责把目标键分发给对应子应用，通知消息不携带子应用地址。

#### 5.1.6 EVENT

`EVENT` 用于提交后端业务命令，例如“确认告警”“标记处理”“确认风险”“触发补偿”。点击后通知中心执行以下流程：

1. 校验当前用户能看到该站内信。
2. 校验动作存在、类型为 `EVENT`、`eventType` 已配置、动作未过期且可执行。
3. 生成动作请求，`requestId` 使用 `messageId + actionCode + userId` 幂等。
4. 动作状态从 `AVAILABLE` 改为 `PROCESSING`，前端按钮禁用，防止重复点击。
5. 发布领域事件，`eventType` 使用动作的 `eventType`。
6. 领域事件 payload 带上 `messageId`、`actionCode`、`actorUserId`、`requestId`、`subject`、`input` 和 `data`。
7. 业务订阅方完成处理后调用内部完成接口回写结果。

业务订阅方必须使用 `requestId` 做幂等。处理成功时回写 `SUCCEEDED`，处理失败时回写 `FAILED` 和失败原因。`FAILED` 状态允许用户再次点击重试；`PROCESSING` 和 `SUCCEEDED` 不允许重复点击。

完成回写命令：

```java
CompleteNoticeSiteMessageActionCommand complete = new CompleteNoticeSiteMessageActionCommand();
complete.setRequestId(requestId);
complete.setStatus(NoticeSiteMessageActionRequestStatus.SUCCEEDED);
complete.setResult(Map.of("handledBy", "workflow-service"));
noticeApi.completeSiteMessageAction(complete);
```

#### 5.1.7 发送示例

```java
Map<String, Object> taskParams = new LinkedHashMap<>();
taskParams.put("processInstanceId", "PI-001");
taskParams.put("taskId", "TASK-001");

NoticeSiteMessageTargetCommand target = new NoticeSiteMessageTargetCommand();
target.setTargetType(NoticeSiteMessageTargetType.ROUTE);
target.setTargetKey("workflow:task-detail");
target.setParams(taskParams);

NoticeSiteMessageSubjectCommand subject = new NoticeSiteMessageSubjectCommand();
subject.setSubjectType("WORKFLOW_TASK");
subject.setSubjectId("TASK-001");
subject.setSubjectName("费用报销审批");

NoticeSiteMessageActionCommand openTask = new NoticeSiteMessageActionCommand();
openTask.setActionCode("OPEN_TASK");
openTask.setActionLabel("进入审批");
openTask.setInteractionType(NoticeSiteMessageActionInteractionType.ROUTE);
openTask.setTarget(target);

NoticeSiteMessageActionCommand acknowledge = new NoticeSiteMessageActionCommand();
acknowledge.setActionCode("ACKNOWLEDGE");
acknowledge.setActionLabel("标记跟进");
acknowledge.setInteractionType(NoticeSiteMessageActionInteractionType.EVENT);
acknowledge.setEventType("workflow.notice.task.acknowledge");
acknowledge.setConfirmRequired(true);

SendNoticeCommand command = new SendNoticeCommand();
command.setBizType("WORKFLOW_TASK_ASSIGNED");
command.setBizId("WF-20260704-001");
command.setMessageScene("workflow.task.assigned");
command.setMessageSubject(subject);
command.setMessageTarget(target);
command.setMessageData(taskParams);
command.setMessageActions(List.of(openTask, acknowledge));
command.setUserId(1001L);
command.setParams(Map.of("applyNo", "EXP-001"));

noticeApi.send(command);
```

## 6. 配置说明

YAML 只配置通知 outbox 分发行为。渠道账号的非敏感运行参数保存在 `notice_channel_config.config_json`；Resource Secret 引用和环境人工补录值分别保存在 `secret_refs_json`、`secret_config_json`，查询接口不会返回解析值或人工 Secret。模板 ID、路由模式和标签通过通知管理页面或 API 维护。

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

### 7.1 短信渠道配置

`mango-notice-channel-sms` 支持阿里云短信和腾讯云短信。渠道配置通过 `notice_channel_config.provider_code` 选择供应商，未显式配置 provider 时按阿里云兼容处理。

| providerCode | 供应商 | 必填配置 |
|--------------|--------|----------|
| `ALIYUN` / `ALIYUN_SMS` | 阿里云短信 | `accessKeyId`、`accessKeySecret`、`signName` |
| `TENCENT` / `TENCENT_SMS` | 腾讯云短信 | `secretId`、`secretKey`、`smsSdkAppId`、`signName` |

阿里云配置示例：

```json
{
  "accessKeyId": "example-ak",
  "accessKeySecret": "example-secret",
  "signName": "Mango",
  "endpoint": "dysmsapi.aliyuncs.com"
}
```

腾讯云配置示例：

```json
{
  "secretId": "example-secret-id",
  "secretKey": "example-secret-key",
  "smsSdkAppId": "1400000001",
  "signName": "Mango",
  "region": "ap-guangzhou",
  "endpoint": "sms.tencentcloudapi.com",
  "countryCode": "+86"
}
```

短信模板 Code 保存在渠道模板的 `channelTemplateId`，发送时会作为第三方短信模板 ID 使用。模板变量映射保存在 `variableMapping`，格式为第三方模板变量到通知参数名的 JSON，例如：

```json
{
  "code": "verifyCode",
  "expire": "expireMinutes"
}
```

腾讯云短信的 `{1}`、`{2}` 这类序号变量也使用同一映射方式，例如 `{ "1": "verifyCode" }`。没有配置 `variableMapping` 时，通知参数 `params` 会原样作为短信模板参数传递。

### 7.2 邮件附件

EMAIL 渠道会消费 `attachmentFileIds`，通过 Mango File 服务读取当前租户可访问的文件并生成标准 `multipart/mixed` 邮件。默认限制为最多 10 个附件、单文件 10 MiB、总计 25 MiB、单文件读取 15 秒；渠道 `configJson` 可覆盖限制和 MIME 白名单。任一附件读取、权限、大小或类型校验失败时，整封邮件不会提交 SMTP，也不会记录伪成功。

发送记录只保存文件 ID、文件名、类型、大小和处理阶段等安全摘要，不保存文件内容、临时下载地址或 Secret。无附件邮件继续使用原单正文 MIME。

### 7.3 渠道路由与 Secret

每个渠道账号使用同租户唯一且创建后不可变的 `configCode`。渠道模板支持：

- `EXACT`：只发送到指定 `channelConfigId`；
- `TAG`：只在同渠道类型、匹配 `routeTagCode` 的账号中选择；无候选时返回 `CHANNEL_ROUTE_TAG_UNAVAILABLE`，不回退 AUTO；
- `AUTO`：使用同渠道类型全部可用账号。

候选账号必须启用且 `configStatus=COMPLETE`，按较小 `priority`、健康状态、`weight` 和发送记录 ID 稳定轮换。单账号可重试失败达到上限后切换下一账号；不可重试错误立即停止，避免误用其它发送身份。

Resource 的 `configJson` 禁止明文 Secret，`secretRefs` 首批支持 `env:NAME` 和 `property:path.to.secret`。运行时优先使用引用解析值，其次使用没有被引用管理的人工 Secret，最后合并非敏感配置。Resource 重放不会清空人工 Secret。

### 7.4 Outbox Topic

通知 outbox 写入 `topic=notice`、`eventType=notice.send`，后台 worker 只通过 `claimByTopic(..., OutboxTopics.NOTICE, ...)` 获取通知发送任务。历史无 topic 的 `notice.send` 消息会被 KV outbox 推断为通知消息，升级时不需要额外数据迁移。

## 8. 资源注入

通知中心内置站内信渠道通过 `mango-resource` 注入。通知模块也会声明自己的业务域，业务模块可用同一资源协议向通知中心注入消息模板。资源文件放在：

```text
mango-notice-starter/src/main/resources/META-INF/mango/resources/notice-common-menu.json
mango-notice-starter/src/main/resources/META-INF/mango/resources/notice-common-message.yml
mango-notice-starter/src/main/resources/META-INF/mango/resources/notice-common-domain.yml
```

业务模块声明通知模板时，推荐在业务模块自己的 starter 中实现 `ResourceProvider`，并通过 `NoticeMessageTemplateResourceDeclarations.fourChannels(...)` 生成站内信、邮件、企业微信、短信四类模板声明。业务代码只依赖 `mango-notice-api` 和 `mango-resource-api`，不依赖 `mango-resource` 的 core/starter。

### 8.1 MESSAGE_CHANNEL

`MESSAGE_CHANNEL` 落库到 `notice_channel_config`，按 `tenantId + configCode` 合并更新。同一 provider 可以声明多个账号。

| 字段 | 类型 | 必填 | 含义 |
|------|------|------|------|
| `id` | `STRING` | 是 | 资源稳定 ID，使用雪花 ID 字符串。 |
| 顶层 `version` | `INT` | 是 | 资源版本，声明内容升级时递增。 |
| `biz-key` | `STRING` | 是 | 资源业务键，例如 `notice.channel.site-internal-default`。 |
| `target-module` | `STRING` | 是 | 固定为 `notice`。 |
| `channelConfigId` | `LONG` | 否 | 通知渠道配置稳定 ID，不填时使用资源 ID。 |
| `configCode` | `STRING` | 是 | 同租户唯一的稳定账号编码，创建后不可修改。 |
| `tenantId` | `STRING` | 否 | 租户 ID，默认 `1`。 |
| `channelType` | `STRING` | 是 | `SITE`、`SMS`、`EMAIL`、`WECHAT_OFFICIAL`、`WECOM`、`DINGTALK`。 |
| `providerCode` | `STRING` | 是 | 渠道服务商编码。 |
| `configName` | `STRING` | 是 | 渠道配置名称。 |
| `configJson` | `STRING` | 否 | 非敏感渠道配置 JSON；出现明文 Secret 时同步失败。 |
| `secretRefs` | `STRING` | 否 | Secret 键到 `env:` / `property:` 引用的 JSON 对象。 |
| `routeTagCodes` | `STRING` | 否 | 账号绑定的稳定路由标签编码 JSON 数组；标签需已存在。 |
| `rateLimitConfig` | `STRING` | 否 | 限流配置 JSON。 |
| `enabled` | `BOOLEAN` | 否 | 是否启用，默认 `true`。 |
| `priority` | `INT` | 否 | 优先级，默认 `0`。 |
| `weight` | `INT` | 否 | 权重，默认 `100`。 |
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

业务模块不希望通知失败影响主流程时，可发布 `NoticeSendEvent`。事件命令使用 `tenantId`、`appCode`、`realm` 保存产生事件时的应用上下文；本地 `mango-notice-starter` 和微服务 `mango-notice-starter-remote` 在调用 `NoticeApi` 前恢复这些字段，并在调用完成后恢复原线程上下文。这样 `ROLE` 接收目标会在正确的租户、应用和登录域中解析；旧事件未携带 `appCode` 或 `realm` 时沿用消费线程已有值。事件监听器会记录发送失败日志，但不向上抛出异常阻断业务事务。

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

### 9.4 站内信分类

Maven `1.0.29` 增加站内信未读分类统计和分页筛选，不新增数据库字段。服务根据通知业务类型的 `bizGroup` 计算分类，统计和分页查询复用同一组当前用户、租户、未删除和可见性条件。

| 分类 | 枚举 | `bizGroup` 口径 |
|------|------|-----------------|
| 审批类消息 | `APPROVAL` | `WORKFLOW` |
| 系统通知 | `SYSTEM` | `AUTH`、`IDENTITY`、`JOB` |
| 业务通知 | `BUSINESS` | 其它业务组 |

`NoticeSiteMessagePageQuery.category` 接受上述枚举，`unreadOnly=true` 只返回未读消息。`NoticeApi.unreadCategoryStats()` 和 `GET /notice/site/my/unread-category-stats` 返回总未读数及三个分类的精确数量；数量为 0 的分类仍会出现在 API 结果中，由前端决定是否展示。

## 10. 请求与返回字段

HTTP 根路径：`/notice`。

| 分类 | 接口 | 权限 | 用途 |
|------|------|------|------|
| 发送 | `POST /notice/send` | `notice:task:create` | 按业务类型发送通知。 |
| 站内信 | `POST /notice/site/messages` | `notice:site:create` | 快捷发送站内信。 |
| 业务类型 | `/notice/business-types/**` | `notice:business:*` | 维护业务类型、配置版本和渠道模板。 |
| 个人可用消息类型 | `GET /notice/site/business-types` | LOGIN | 查询当前租户已启用的消息类型。 |
| 渠道配置 | `/notice/channels/**` | `notice:channel:*` | 查询、保存、删除渠道配置。 |
| 内部配置 | `GET /notice/internal/wecom-login-config` | INTERNAL | 认证服务读取企微扫码登录配置。 |
| 任务 | `GET /notice/tasks` | `notice:task:view` | 查询通知任务。 |
| 发送记录 | `GET /notice/records` | `notice:record:view` | 查询发送记录。 |
| 失败处理 | `/notice/records/**` | `notice:retry:edit` | 重试、人工成功、忽略失败。 |
| 设置 | `GET /notice/settings`、`PUT /notice/settings` | `notice:setting:*` | 读取和保存通知设置。 |
| 接收账户 | `/notice/recipient-accounts/**` | `notice:receive-setting:*` | 维护接收账户。 |
| 企业微信 | `POST /notice/wecom/users/sync` | `system:user:add` | 同步企微用户映射。 |
| 接收偏好 | `GET /notice/receive-preferences`、`PUT /notice/receive-preferences` | LOGIN | 维护当前登录人的接收偏好。 |
| 我的站内信和公告 | `/notice/site/my/**` | LOGIN | 当前用户的公告、未读数、未读分类统计、分类列表、详情、已读和删除。 |

常用返回对象：

| 返回对象 | 含义 |
|----------|------|
| `NoticeSendResultVO` | 发送入口返回结果。 |
| `NoticeTaskVO` | 通知任务。 |
| `NoticeSendRecordVO` | 每个接收人、每个渠道的发送记录。 |
| `NoticeSiteMessageVO` | 站内信消息。 |
| `NoticeUnreadCountVO` | 未读数量。 |
| `NoticeUnreadCategoryCountVO` | 单个站内信分类及未读数量。 |
| `NoticeUnreadCategoryStatsVO` | 总未读数及审批、系统、业务分类统计。 |
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
notice:announcement:view
notice:announcement:create
notice:announcement:edit
notice:announcement:publish
notice:announcement:offline
```

企微用户同步接口复用 `system:user:add`。

前端页面由 `@mango/notice/admin-pages` 注册。菜单通过 `notice-common-menu.json` 的 `AUTH_MENU` 资源注入到 `internal-admin`，component 映射到页面 key：

管理侧“通知管理”以 `parentCode=data` 挂载在“平台能力”下；`/notice` 及所有子路由保持不变。“消息中心”仍是独立的用户侧一级入口。

| 菜单 | 路径 | component key | 默认展示 |
|------|------|---------------|----------|
| 我的消息 | `/message-center/site-message` | `notice/site-message/index` | 是 |
| 公告管理 | `/notice/announcement` | `notice/announcement/index` | 是 |
| 系统公告 | `/message-center/announcement` | `notice/announcement-user/index` | 是 |
| 消息配置 | `/notice/message-definition` | `notice/message-definition/index` | 是 |
| 发送任务 | `/notice/send-message` | `notice/send-message/index` | 是 |
| 渠道配置 | `/notice/channel` | `notice/channel/index` | 是 |
| 发送记录 | `/notice/record` | `notice/record/index` | 是 |
| 失败重试 | `/notice/retry` | `notice/retry/index` | 是 |
| 接收配置 | `/message-center/receive-setting` | `notice/receive-setting/index` | 是 |
| 全局设置 | `/notice/setting` | `notice/setting/index` | 否 |

旧路径 `/notice/receive-setting` 由前端页面注册表保留为隐藏兼容入口；菜单资源、站内信设置按钮和 `notice:receive-setting` 命名目标统一使用 `/message-center/receive-setting`。

## 12. 数据与初始化

Flyway 路径为 `mango-notice-core/src/main/resources/db/migration/notice`。`V1__init_notice.sql` 是包含 22 张通知表的当前完整新装 schema；`V2__notice_channel_resource_route_and_secret.sql` 使用 `information_schema` 条件守卫，为已发布的旧 V1 增加稳定账号编码、Secret 分层和路由标签，并在当前 V1 新装库上安全空跑。V1 不包含账号、模板或演示数据；V2 只做兼容回填和 DDL。

正式必需资源由 `mango-notice-starter` 在 `META-INF/mango/resources/` 中按 Notice 模块登记，文件统一使用 `notice-common-` 前缀：

| 资源 | 用途 |
|------|------|
| `notice-common-domain.yml` | 通知业务域 |
| `notice-common-menu.json` | 通知管理菜单与权限入口 |
| `notice-common-message.yml` | 默认站内信内部通道 |

这些资源属于模块运行所需的正式声明，不是演示数据。管理员接收账户、业务模板和第三方渠道账号不再自动初始化。认证、任务、支付、工作流等模块需要的通知模板由各自 starter 在自己的 `META-INF/mango/resources/` 中登记，Notice 不集中代管其他模块数据。

Notice 当前不提供演示数据。以后新增示例业务或示例账号时，必须放入 `mango-notice-starter/src/main/resources/META-INF/mango/demo/`，文件使用 `notice-demo-` 前缀并采用 `INIT_ONLY`；只有显式设置 `mango.resource.registry.demo-enabled=true` 才能加载，禁止写回 Flyway 或正式资源目录。

核心表包括 `notice_announcement`、`notice_announcement_recipient`、`notice_announcement_target`、`notice_audit_log`、`notice_business_channel_template`、`notice_business_config_version`、`notice_business_type`、`notice_callback_log`、`notice_channel_config`、`notice_channel_route_tag`、`notice_channel_config_route_tag`、`notice_receive_preference`、`notice_recipient`、`notice_recipient_account`、`notice_retry_log`、`notice_send_record`、`notice_setting`、`notice_site_message`、`notice_site_message_action`、`notice_site_message_action_request`、`notice_task`、`notice_wecom_sync_mapping`。

通知异步分发依赖 `mango-infra-kv` outbox。部署时要确认 outbox 存储可用，否则任务可能创建成功但不会被后台 worker 分发。

升级到 topic 隔离版本时，业务方正常调用 `NoticeApi.send` 或 `/notice/send` 不需要改代码。需要同步升级 `mango-infra-kv`、`mango-infra-event` 和 `mango-notice` 相关依赖，并重启所有旧实例，避免旧进程继续使用无 topic 的 claim API。

## 13. 问题排查

| 问题 | 优先检查 |
|------|----------|
| 任务创建了但没有发送 | `mango.notice.outbox.enabled`、`dispatch-enabled`、outbox 存储、worker 日志。 |
| 通知 outbox 被 `domain-event-dispatcher` 锁定 | 是否已同步升级 topic 隔离版本并重启旧实例；自定义 worker 是否仍调用旧 claim API。 |
| 站内信未出现 | 是否引入 `mango-notice-channel-site`，业务配置和站内信渠道模板是否已发布。 |
| 第三方渠道失败 | `notice_send_record` 的失败码、失败原因、请求快照、响应快照和渠道配置 JSON。 |
| 用户收不到短信或邮件 | `notice_recipient_account` 是否有手机号或邮箱，接收偏好是否关闭渠道。 |
| 重试无效 | 记录状态是否允许重试，是否超过最大重试次数。 |
| 管理页面 403 | 角色是否有对应 `notice:*` 权限。 |
| 铃铛未显示或未读数不变 | 前端是否注册 `@mango/notice/admin-shell`，站内信接口是否可访问。 |

## 14. 相关文档

- [前端通知包](../../../mango-ui/packages/notice/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
