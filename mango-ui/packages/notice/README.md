# @mango/notice

## 1. 概览

`@mango/notice` 是 Mango 通知管理的前端包，配套后端 `mango-notice` 使用。

它提供三类能力：

- 管理后台页面：业务配置、渠道、发送消息、任务、记录、重试、站内信、设置、接收设置。
- Shell 扩展：顶部通知铃铛和未读提醒。
- 客户端能力：消息中心、接收设置、实时通知、桌面通知、声音和语音提醒。

本包适合 Mango 管理后台，不是官网、C 端页面通用组件库。业务前端如需发送通知，优先让业务后端调用 `NoticeApi`，前端只负责触发业务动作和展示结果。

## 2. 功能清单

| 能力         | 说明                                                                      |
| ------------ | ------------------------------------------------------------------------- |
| 通知管理页面 | 维护业务类型、配置版本、EXACT/TAG/AUTO 路由、渠道账号、任务、记录和重试。 |
| 站内信页面   | 查询站内信、未读数、详情、已读和删除。                                    |
| 顶部铃铛     | 未读不超过 10 条时展示最近消息；超过 10 条时按审批、系统、业务分类聚合。  |
| 消息中心     | 展示当前用户站内信列表。                                                  |
| 接收设置     | 用户维护接收账户、渠道偏好和提醒方式。                                    |
| 实时提醒     | 订阅通知实时事件，触发弹窗、桌面通知、声音或语音。                        |
| API 封装     | 导出通知发送、业务配置、渠道、任务、记录、站内信和接收偏好的请求函数。    |

## 3. 集成形态

| 形态                 | 是否支持 | 说明                                                                                                    |
| -------------------- | -------- | ------------------------------------------------------------------------------------------------------- |
| `admin-shell`        | 是       | `registerMangoNoticeAdminShell` 注册顶部铃铛 provider。                                                 |
| `admin-pages`        | 是       | `registerMangoNoticeAdminPages` 注册通知管理页面。                                                      |
| `business-component` | 部分     | `NoticeClientBell`、`NoticeClientMessageCenter`、`NoticeClientReceiveSetting` 可在 Mango 管理端内复用。 |
| `api-client`         | 是       | 导出通知管理 API 封装和类型。                                                                           |

## 4. 接入方式

安装依赖：

```bash
pnpm add @mango/notice
```

注册管理页面：

```ts
import { registerMangoNoticeAdminPages } from '@mango/notice/admin-pages';

registerMangoNoticeAdminPages();
```

注册通知铃铛：

```ts
import { registerMangoNoticeAdminShell } from '@mango/notice/admin-shell';

registerMangoNoticeAdminShell();
```

订阅实时通知：

```ts
import { createNoticeRealtime } from '@mango/notice/realtime';

const stop = createNoticeRealtime((message) => {
  console.log(message.id, message.title);
});

stop();
```

## 5. 快速开始

1. 后端启用 `mango-notice`、站内信渠道、authorization、system、identity、org 和 realtime 相关能力。
2. 管理后台安装并注册 `@mango/notice/admin-pages`。
3. Shell 注册 `@mango/notice/admin-shell`。
4. 个人站内信、公告阅读和接收设置由内置 `ROLE_LOGIN` 自动授权；只需按需给管理角色授予业务配置、渠道、任务、记录和后台发送权限。
5. 创建业务类型，保存并发布配置版本和渠道模板。
6. 保存渠道配置；短信渠道按 provider 选择阿里云或腾讯云并填写密钥、签名和接入地址。
7. 发送站内信，确认任务记录、未读数、铃铛和消息中心都正常。

渠道页会展示稳定 `configCode`、Resource 来源和版本、路由标签、Secret 完整性、优先级、权重及健康状态。Resource 管理账号的非敏感字段只读，管理员只能补录 Secret 和维护标签；Secret 输入只写不回显。删除或停用账号、删除标签前会展示引用影响，存在模板引用时后端拒绝破坏性操作。

消息配置页的渠道路由三选一：指定账号（`EXACT`）、路由标签（`TAG`）或自动轮换（`AUTO`）。TAG 会展示当前候选账号数量和名称；零候选标签不能保存为已启用模板，运行时也不会回退到 AUTO。

## 6. 配置说明

本包没有独立 YAML 配置。前端行为由页面注册、后端权限、后端业务配置和个人提醒设置共同决定。

| 配置入口                        | 字段 / Key        | 默认值                | 含义                                               |
| ------------------------------- | ----------------- | --------------------- | -------------------------------------------------- |
| `registerMangoNoticeAdminPages` | `moduleCode`      | `mango-notice`        | 页面归属模块。                                     |
| `registerMangoNoticeAdminPages` | `profileSections` | 三个消息中心 section  | 自动注册“我的消息、系统公告、通知设置”到个人中心。 |
| 页面注册                        | component key     | 多个 `notice/*/index` | 菜单打开具体通知页面。                             |
| `registerMangoNoticeAdminShell` | provider          | `NoticeBell`          | Shell 顶部铃铛 provider。                          |
| 个人提醒配置                    | `groupCode`       | `notice`              | 保存个人提醒设置的分组。                           |
| 个人提醒配置                    | `bizType`         | `client_reminder`     | 保存个人提醒设置的业务类型。                       |
| 个人提醒配置                    | `configKey`       | `reminder_setting`    | 保存个人提醒设置的配置 key。                       |

提醒设置字段：

| 字段                         | 默认值      | 含义                     |
| ---------------------------- | ----------- | ------------------------ |
| `popupEnabled`               | `true`      | 是否弹出提醒。           |
| `popupPlacement`             | `top-right` | 弹出位置。               |
| `voiceEnabled`               | `true`      | 是否启用声音或语音。     |
| `reminderMode`               | `SOUND`     | 提醒模式。               |
| `soundType`                  | `IM`        | 声音类型。               |
| `desktopNotificationEnabled` | `true`      | 是否启用浏览器桌面通知。 |

依赖：

| 类型             | 依赖                                                                              |
| ---------------- | --------------------------------------------------------------------------------- |
| dependencies     | `@mango/admin-pages`、`@mango/common`、`@mango/system`、`@element-plus/icons-vue` |
| peerDependencies | `vue`、`vue-router`、`element-plus`                                               |

### 6.1 短信配置

渠道管理页在 `channelType = SMS` 时按 provider 展示短信配置字段：

| providerCode              | 页面字段                                                                               |
| ------------------------- | -------------------------------------------------------------------------------------- |
| `ALIYUN` / `ALIYUN_SMS`   | AccessKey、Secret、短信签名、模板平台、接入地址、通知地址                              |
| `TENCENT` / `TENCENT_SMS` | SecretId、SecretKey、短信应用 ID、短信签名、模板平台、地域、接入地址、国家码、通知地址 |

消息配置页启用短信渠道时需要填写短信模板 Code。模板 Code 会保存到渠道模板 `channelTemplateId`，后端发送时作为阿里云 TemplateCode 或腾讯云 TemplateId 使用。

短信变量映射用于把第三方模板变量绑定到 Mango 通知参数：

| 模板写法                       | 变量名示例 | 映射示例             |
| ------------------------------ | ---------- | -------------------- |
| 阿里云 `${code}` 或 `{{code}}` | `code`     | `code -> verifyCode` |
| 腾讯云 `{1}`                   | `1`        | `1 -> verifyCode`    |

点击“按参数生成”会从模板内容或业务参数 schema 生成映射行。保存后映射会写入渠道模板 `variableMapping`，格式为 JSON。

## 7. API 与扩展

### 7.1 页面 key

| 页面     | component key                                             |
| -------- | --------------------------------------------------------- |
| 业务配置 | `notice/business-config/index`                            |
| 消息定义 | `notice/message-definition/index`                         |
| 发送消息 | `notice/send-message/index`                               |
| 公告管理 | `notice/announcement/index`                               |
| 系统公告 | `notice/announcement-user/index`                          |
| 渠道管理 | `notice/channel/index`                                    |
| 通知任务 | `notice/task/index`                                       |
| 发送记录 | `notice/record/index`                                     |
| 站内信   | `notice/site-message/index`、`notice/site/messages/index` |
| 全局设置 | `notice/setting/index`                                    |
| 通知设置 | `notice/receive-setting/index`                            |
| 重试管理 | `notice/retry/index`                                      |

### 7.2 导出对象

| 导出                            | 用途                     |
| ------------------------------- | ------------------------ |
| `registerMangoNoticeAdminPages` | 注册通知管理页面。       |
| `registerMangoNoticeAdminShell` | 注册顶部通知铃铛。       |
| `NoticeBell`                    | Shell 顶部铃铛组件。     |
| `NoticeDetailDialog`            | 通知详情弹窗。           |
| `NoticeClientBell`              | 客户端铃铛组件。         |
| `NoticeClientMessageCenter`     | 消息中心组件。           |
| `NoticeClientReceiveSetting`    | 接收设置组件。           |
| `createNoticeRealtime`          | 通知实时订阅。           |
| `requestDesktopPermission`      | 请求浏览器桌面通知权限。 |
| `showDesktopNotice`             | 浏览器桌面通知。         |
| `playNoticeSound`               | 声音提醒。               |
| `speakNoticeText`               | 语音播报。               |

### 7.3 常用 API

| 分组       | 函数                                                                                                                                          |
| ---------- | --------------------------------------------------------------------------------------------------------------------------------------------- |
| 发送       | `sendNotice`、`sendSiteNotice`                                                                                                                |
| 接收人辅助 | `getIdentityUsers`、`getNoticeOrgTree`、`getNoticePosts`、`getNoticeRoles`                                                                    |
| 业务类型   | `getBusinessTypes`、`createBusinessType`、`updateBusinessType`、`deleteBusinessType`                                                          |
| 配置版本   | `getBusinessConfigVersions`、`saveBusinessConfigDraft`、`publishBusinessConfigDraft`、`activateBusinessConfigVersion`                         |
| 渠道模板   | `getChannelTemplates`、`saveChannelTemplate`、`publishChannelTemplate`                                                                        |
| 渠道配置   | `getChannelConfigs`、`saveChannelConfig`、`deleteChannelConfig`                                                                               |
| 路由标签   | `getNoticeRouteTags`、`saveNoticeRouteTag`、`deleteNoticeRouteTag`、`getNoticeChannelReferenceImpact`                                         |
| 任务和记录 | `getNoticeTasks`、`getSendRecords`、`retrySendRecord`、`markSendRecordManualSuccess`、`ignoreSendRecord`                                      |
| 站内信     | `getMySiteMessages`、`getMySiteMessageDetail`、`getMyUnreadCount`、`getMyUnreadCategoryStats`、`markMySiteMessageRead`、`deleteMySiteMessage` |
| 接收设置   | `getRecipientAccounts`、`saveRecipientAccount`、`getReceivePreferences`、`saveReceivePreference`                                              |
| 个人提醒   | `getNoticeReminderSetting`、`saveNoticeReminderSetting`                                                                                       |

个人消息和接收设置接口使用权限码完成资源校验；接收设置展示业务类型还依赖只读权限 `notice:business:view`。这些最小权限由后端 Notice 菜单资源绑定到内置 `ROLE_LOGIN`，所有已登录用户无需额外业务角色即可使用；`ROLE_ANONYMOUS` 不包含个人消息或 Realtime 建连权限。

工作台消息卡的“查看全部”默认进入 `/message-center/site-message`。消息中心已迁入个人中心，入口分别为 `/profile?tab=notice-site-message`、`/profile?tab=notice-announcement-user` 和 `/profile?tab=notice-receive-setting`；执行 `registerMangoNoticeAdminPages()` 后，Shell 会从 registrar 返回的 `profileSections` 自动装配这三个入口。旧站内信路径 `/notice/site-message` 和旧通知设置路径 `/notice/receive-setting` 仅作为隐藏兼容入口保留。业务消息携带的其它 `targetKey` 可以是宿主已注册的命名 Vue 路由，也可以是经过校验且当前用户可访问的应用内绝对路径；未注册、越权或不安全的目标会显示“目标未注册或当前无权访问”。

### 7.4 站内信动作接入

站内信动作由业务后端发送时定义，前端只负责展示按钮、把点击交给命名目标或后端动作接口处理。

详情弹窗的主按钮会按消息场景生成用户可理解的名称，例如“去审批”“去领取”“查看申请”“查看资料”；没有匹配场景时回退到业务后端的 `messageActions[].actionLabel`。

#### 7.4.1 动作类型

| 业务意图       | 后端声明                                           | 前端行为                                            | 点击后是否禁用      |
| -------------- | -------------------------------------------------- | --------------------------------------------------- | ------------------- |
| 进入业务页面   | `interactionType=ROUTE`，`target.targetType=ROUTE` | 触发 `interaction`，宿主按 `targetKey` 打开命名页面 | 否                  |
| 进入自定义流程 | `interactionType=ROUTE`，`target.targetType=FLOW`  | 打开流程处理入口或触发 `interaction` 给宿主处理     | 否                  |
| 提交后台命令   | `interactionType=EVENT`，`eventType` 必填          | 调用动作接口，提交隐藏 `input`                      | 是，`FAILED` 可重试 |

`targetKey` 支持命名目标键和应用内绝对路径。命名目标必须提前注册或由宿主的 `interaction` 事件处理；绝对路径必须以单个 `/` 开头，通过路由解析且当前用户可访问，协议地址、协议相对地址、反斜杠和控制字符会被拒绝。

#### 7.4.2 隐藏业务参数

点击任何动作时，`NoticeClientMessageCenter` 都会整理同一份隐藏上下文：

```ts
{
  ...message.target?.params,
  ...action.target?.params,
  bizType: message.bizType,
  bizId: message.bizId,
  bizGroup: message.bizGroup,
  bizName: message.bizName,
  messageScene: message.messageScene,
  messageId: message.id,
  actionCode: action.actionCode,
  subject: message.subject,
  data: message.data,
}
```

这些参数用于业务页面、业务流程或后端事件处理，不会默认展示在消息正文、详情弹窗或流程弹窗中。业务需要让用户看到的内容，应写进标题、正文、业务对象名称或业务页面本身。

#### 7.4.3 使用消息中心组件

```vue
<template>
  <NoticeClientMessageCenter
    @settings="openReceiveSetting"
    @announcement="openAnnouncement"
    @interaction="openNoticeTarget"
  />
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { NoticeClientMessageCenter, resolveNoticeTargetLocation } from '@mango/notice/client';
import type { NoticeSiteMessage, NoticeSiteMessageAction } from '@mango/notice';

const router = useRouter();

async function openNoticeTarget(payload: {
  message: NoticeSiteMessage;
  action?: NoticeSiteMessageAction;
  targetKey?: string;
  targetType?: 'ROUTE' | 'FLOW';
  params?: Record<string, unknown>;
}) {
  if (!payload.targetKey) return;

  const location = resolveNoticeTargetLocation(router, payload.targetKey, payload.params);
  if (location) await router.push(location);
}
</script>
```

宿主可以把命名 `targetKey` 映射到本地页面，也可以通过微前端运行时把它分发给对应子应用；应用内绝对路径则由路由解析和权限校验后跳转。

#### 7.4.4 ROUTE 处理

`ROUTE` 适合“查看详情”“进入审批”“查看实例”“查看资料”等入口型动作。

业务前端需要保证：

1. `targetKey` 对应的页面名称已注册，或应用内绝对路径能够解析且当前用户可访问。
2. 页面能从 `params` 读取业务 ID 或对象 ID。
3. 页面进入后用真实接口加载业务数据，并按当前用户权限判断是否可操作。
4. 页面关闭或返回后，站内信按钮仍可再次进入。

#### 7.4.5 FLOW 处理

`FLOW` 适合“安全处理”“支付异常处理”“补偿向导”等自定义交互。它可以是本应用弹窗、抽屉、独立页面，也可以由微前端子应用承接。

接入方式：

1. 后端发送动作时使用 `target.targetType=FLOW`。
2. 前端在宿主或子应用中注册同名 `targetKey` 的流程处理器。
3. 处理器读取 `params` 中的隐藏业务上下文。
4. 流程需要提交后端命令时，业务模块调用自己的业务接口；如果要沿用通知动作状态，则配套一个 `EVENT` 动作或由业务后端回写动作结果。

未注册 FLOW 目标时，组件只显示兜底流程弹窗或提示“目标未注册或当前无权访问”。这通常表示业务前端还没有接入对应 `targetKey`，不是通知发送失败。

#### 7.4.6 EVENT 处理

`EVENT` 适合“确认告警”“标记处理”“确认风险”“触发补偿”等命令型按钮。前端点击后调用：

```ts
executeMySiteMessageAction(messageId, actionCode, input);
```

请求体形态：

```json
{
  "input": {
    "bizType": "WORKFLOW_TASK_ASSIGNED",
    "bizId": "WF-20260704-001",
    "messageId": "1234567890",
    "actionCode": "ACKNOWLEDGE",
    "taskId": "TASK-001",
    "data": {
      "processInstanceId": "PI-001"
    }
  }
}
```

后端受理后会把动作改成 `PROCESSING`，前端按钮禁用。业务订阅方处理失败并回写 `FAILED` 后，按钮允许再次点击重试；成功回写 `SUCCEEDED` 后不再允许点击。

#### 7.4.7 常见问题

| 现象                         | 原因                                                     | 处理方式                                   |
| ---------------------------- | -------------------------------------------------------- | ------------------------------------------ |
| 按钮不显示                   | 后端没有传 `messageActions`，或动作状态为 `DISABLED`     | 检查站内信详情接口返回的 `actions`。       |
| 按钮名称不对                 | 消息场景无法识别，且后端 `actionLabel` 不符合业务语义    | 补齐消息场景，或修正 `actionLabel`。       |
| 提示目标未注册或当前无权访问 | `targetKey` 未注册、路径不安全或当前用户不能进入目标页面 | 注册命名目标，或修正路径、菜单和权限。     |
| 点击 EVENT 后按钮灰掉        | 正常行为，命令已提交并处于 `PROCESSING`                  | 等业务订阅方回写成功或失败。               |
| 业务 ID 没在弹窗里显示       | 正常行为，业务上下文是隐藏参数                           | 在目标业务页面展示需要给用户看的业务信息。 |

### 7.5 消息展示与未读分类

`@mango/notice@1.0.35` 使用同一份展示模型承载消息中心详情、顶部铃铛单条消息和右上角实时提醒：

- 详情标题只显示用户可理解的消息类型。
- 详情正文只保留 `消息类型: value`、`消息内容: value`、`消息时间: value`。
- 底部右侧只保留“关闭”和一个按消息场景生成的主操作，例如“去审批”“去领取”“查看申请”“查看资料”。
- 右上角实时提醒使用同一组字段，并提供“点击查看”进入完整详情。
- 消息类型和内容允许基础富文本；渲染前会保留 `a`、`strong`、`em`、`p`、`br`、列表等文本格式标签，移除脚本、事件属性、内联样式和危险链接协议，再通过 `v-html` 或 `innerHTML` 展示。

顶部铃铛的未读阈值固定为 10：

| 未读数量 | 展示方式                                          | 点击结果                                                                  |
| -------- | ------------------------------------------------- | ------------------------------------------------------------------------- |
| `0-10`   | 按单条消息展示                                    | 打开对应消息详情。                                                        |
| `>10`    | 只展示数量大于 0 的审批类、系统通知、业务通知分组 | 进入 `/message-center/site-message?category=<CATEGORY>&unreadOnly=true`。 |

分类由后端根据业务类型配置的 `bizGroup` 统一计算，统计和分页筛选使用同一口径：

| 分类       | `category` | 典型业务组                |
| ---------- | ---------- | ------------------------- |
| 审批类消息 | `APPROVAL` | `WORKFLOW`                |
| 系统通知   | `SYSTEM`   | `AUTH`、`IDENTITY`、`JOB` |
| 业务通知   | `BUSINESS` | 其它业务组                |

`getMyUnreadCategoryStats()` 调用 `GET /notice/site/my/unread-category-stats`；`getMySiteMessages()` 支持 `category` 和 `unreadOnly` 查询参数。分类项为 0 时铃铛不展示该分组，统计失败时不伪造本地数量。

## 8. 数据与初始化

`@mango/notice@1.0.29` 将个人接收设置并入消息中心入口，并与后端 Notice 公共菜单资源保持一致。通知管理、发送记录、失败重试、站内信 HTTP API 和初始化数据来源保持兼容；业务项目升级后应让资源注册流程重新协调菜单声明，不需要重建业务数据。

本包不创建数据库表，也不初始化菜单权限。它依赖后端完成以下初始化：

| 数据                   | 后端来源                                | 前端用途                           |
| ---------------------- | --------------------------------------- | ---------------------------------- |
| 通知业务类型           | `mango-notice` migration 或管理页面     | 业务配置、发送消息、消息中心筛选。 |
| 渠道配置               | `mango-notice` migration 或渠道管理页面 | 渠道管理和实际发送。               |
| 任务和发送记录         | `mango-notice`                          | 任务、记录、重试页面。             |
| 站内信                 | `mango-notice`                          | 铃铛、未读数、消息中心。           |
| 个人提醒配置           | system personal config                  | 保存弹窗、声音、桌面通知偏好。     |
| 用户、组织、岗位、角色 | identity、org、authorization            | 接收人选择器。                     |
| 实时通道               | realtime                                | 新消息实时提醒。                   |
| 菜单权限               | authorization                           | 页面入口和按钮权限。               |

## 9. 管理入口

| 入口         | 页面 key                         | 依赖权限                                                                                 |
| ------------ | -------------------------------- | ---------------------------------------------------------------------------------------- |
| 通知业务配置 | `notice/business-config/index`   | `notice:business:*`                                                                      |
| 发送消息     | `notice/send-message/index`      | `notice:task:create`                                                                     |
| 公告管理     | `notice/announcement/index`      | `notice:announcement:*`                                                                  |
| 系统公告     | `notice/announcement-user/index` | `notice:site:view`、`notice:site:edit`                                                   |
| 渠道管理     | `notice/channel/index`           | `notice:channel:*`                                                                       |
| 通知任务     | `notice/task/index`              | `notice:task:view`                                                                       |
| 发送记录     | `notice/record/index`            | `notice:record:view`                                                                     |
| 重试管理     | `notice/retry/index`             | `notice:retry:edit`                                                                      |
| 站内信       | `notice/site-message/index`      | `ROLE_LOGIN` 默认提供 `notice:site:view/edit`                                            |
| 全局设置     | `notice/setting/index`           | `notice:setting:*`                                                                       |
| 通知设置     | `notice/receive-setting/index`   | `ROLE_LOGIN` 默认提供 `notice:receive-setting:view/edit` 和只读的 `notice:business:view` |
| 顶部铃铛     | Shell provider                   | 登录态和 `ROLE_LOGIN` 个人消息权限                                                       |

页面可见但打不开时，先检查 `registerMangoNoticeAdminPages()` 是否执行；铃铛不显示时，检查 `registerMangoNoticeAdminShell()` 是否执行。

## 10. 问题排查

| 问题                 | 优先检查                                                                       |
| -------------------- | ------------------------------------------------------------------------------ |
| 菜单可见但页面打不开 | 页面 key 是否注册，菜单 component 是否能映射到 `notice/*/index`。              |
| 铃铛不显示           | 是否注册 `@mango/notice/admin-shell`，Shell 是否加载 notice provider。         |
| 未读数不变           | `/notice/site/my/unread-count` 是否可访问，站内信是否写入。                    |
| 实时没有提醒         | realtime 是否启用，`createNoticeRealtime` 是否订阅，后端是否推送 notice 事件。 |
| 桌面通知不弹         | 浏览器是否授权桌面通知。                                                       |
| 接收人选择为空       | identity、org、authorization 相关接口是否有数据和权限。                        |
| 页面请求 403         | 当前角色是否有对应 `notice:*` 权限。                                           |

## 11. 相关文档

- [后端通知模块](../../../mango/mango-platform/mango-notice/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 12. 变更影响记录

- `@mango/notice@1.0.38` 的 `registerMangoNoticeAdminPages()` 随页面注册结果返回个人中心“我的消息、系统公告、通知设置”三个 `profileSections`。配套 Admin Shell `1.0.58` 会自动装配这些页内入口；旧消息中心路由继续保留兼容，权限和租户边界不变。

- `@mango/notice@1.0.35` 发布结构化消息详情和实时提醒、安全富文本展示、上下文主操作、未读超过 10 条时的审批/系统/业务分类聚合，以及统一的目标导航与安全降级。新增未读分类统计和分页分类参数；既有站内信 ID、页面 key、权限码、租户边界和历史消息数据保持兼容。

- `@mango/notice@1.0.31` 将工作台消息卡默认入口对齐到 `/message-center/site-message`，并把旧路径
  `/notice/site-message` 注册为隐藏兼容入口。站内信 API、页面 key、权限和租户语义保持不变。

- `@mango/notice@1.0.22` 将精确依赖升级到 `@mango/admin-pages@1.0.20` 和 `@mango/system@1.0.19`；通知 API、
  页面 key、实时消息、权限、租户和运行时行为相对 `1.0.21` 不变。
