# @mango/notice

## 1. 概览

`@mango/notice` 是 Mango 通知中心的前端包，配套后端 `mango-notice` 使用。

它提供三类能力：

- 管理后台页面：业务配置、渠道、发送消息、任务、记录、重试、站内信、设置、接收设置。
- Shell 扩展：顶部通知铃铛和未读提醒。
- 客户端能力：消息中心、接收设置、实时通知、桌面通知、声音和语音提醒。

本包适合 Mango 管理后台，不是官网、C 端页面通用组件库。业务前端如需发送通知，优先让业务后端调用 `NoticeApi`，前端只负责触发业务动作和展示结果。

## 2. 功能清单

| 能力 | 说明 |
|------|------|
| 通知管理页面 | 维护业务类型、配置版本、渠道模板、渠道账号、任务、记录和重试。 |
| 站内信页面 | 查询站内信、未读数、详情、已读和删除。 |
| 顶部铃铛 | 在 admin shell 顶部展示未读提醒和最近消息。 |
| 消息中心 | 展示当前用户站内信列表。 |
| 接收设置 | 用户维护接收账户、渠道偏好和提醒方式。 |
| 实时提醒 | 订阅通知实时事件，触发弹窗、桌面通知、声音或语音。 |
| API 封装 | 导出通知发送、业务配置、渠道、任务、记录、站内信和接收偏好的请求函数。 |

## 3. 集成形态

| 形态 | 是否支持 | 说明 |
|------|----------|------|
| `admin-shell` | 是 | `registerMangoNoticeAdminShell` 注册顶部铃铛 provider。 |
| `admin-pages` | 是 | `registerMangoNoticeAdminPages` 注册通知管理页面。 |
| `business-component` | 部分 | `NoticeClientBell`、`NoticeClientMessageCenter`、`NoticeClientReceiveSetting` 可在 Mango 管理端内复用。 |
| `api-client` | 是 | 导出通知中心 API 封装和类型。 |

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

const stop = createNoticeRealtime(message => {
  console.log(message.id, message.title);
});

stop();
```

## 5. 快速开始

1. 后端启用 `mango-notice`、站内信渠道、authorization、system、identity、org 和 realtime 相关能力。
2. 管理后台安装并注册 `@mango/notice/admin-pages`。
3. Shell 注册 `@mango/notice/admin-shell`。
4. 给角色授予通知业务配置、渠道、任务、记录、站内信和接收设置权限。
5. 创建业务类型，保存并发布配置版本和渠道模板。
6. 保存渠道配置。
7. 发送站内信，确认任务记录、未读数、铃铛和消息中心都正常。

## 6. 配置说明

本包没有独立 YAML 配置。前端行为由页面注册、后端权限、后端业务配置和个人提醒设置共同决定。

| 配置入口 | 字段 / Key | 默认值 | 含义 |
|----------|------------|--------|------|
| `registerMangoNoticeAdminPages` | `moduleCode` | `mango-notice` | 页面归属模块。 |
| 页面注册 | component key | 多个 `notice/*/index` | 菜单打开具体通知页面。 |
| `registerMangoNoticeAdminShell` | provider | `NoticeBell` | Shell 顶部铃铛 provider。 |
| 个人提醒配置 | `groupCode` | `notice` | 保存个人提醒设置的分组。 |
| 个人提醒配置 | `bizType` | `client_reminder` | 保存个人提醒设置的业务类型。 |
| 个人提醒配置 | `configKey` | `reminder_setting` | 保存个人提醒设置的配置 key。 |

提醒设置字段：

| 字段 | 默认值 | 含义 |
|------|--------|------|
| `popupEnabled` | `true` | 是否弹出提醒。 |
| `popupPlacement` | `top-right` | 弹出位置。 |
| `voiceEnabled` | `true` | 是否启用声音或语音。 |
| `reminderMode` | `SOUND` | 提醒模式。 |
| `soundType` | `IM` | 声音类型。 |
| `desktopNotificationEnabled` | `true` | 是否启用浏览器桌面通知。 |

依赖：

| 类型 | 依赖 |
|------|------|
| dependencies | `@mango/admin-pages`、`@mango/common`、`@mango/system`、`@element-plus/icons-vue` |
| peerDependencies | `vue`、`vue-router`、`element-plus` |

## 7. API 与扩展

### 7.1 页面 key

| 页面 | component key |
|------|---------------|
| 业务配置 | `notice/business-config/index` |
| 消息定义 | `notice/message-definition/index` |
| 发送消息 | `notice/send-message/index` |
| 渠道管理 | `notice/channel/index` |
| 通知任务 | `notice/task/index` |
| 发送记录 | `notice/record/index` |
| 站内信 | `notice/site-message/index`、`notice/site/messages/index` |
| 全局设置 | `notice/setting/index` |
| 接收设置 | `notice/receive-setting/index` |
| 重试管理 | `notice/retry/index` |

### 7.2 导出对象

| 导出 | 用途 |
|------|------|
| `registerMangoNoticeAdminPages` | 注册通知管理页面。 |
| `registerMangoNoticeAdminShell` | 注册顶部通知铃铛。 |
| `NoticeBell` | Shell 顶部铃铛组件。 |
| `NoticeDetailDialog` | 通知详情弹窗。 |
| `NoticeClientBell` | 客户端铃铛组件。 |
| `NoticeClientMessageCenter` | 消息中心组件。 |
| `NoticeClientReceiveSetting` | 接收设置组件。 |
| `createNoticeRealtime` | 通知实时订阅。 |
| `requestDesktopPermission` | 请求浏览器桌面通知权限。 |
| `showDesktopNotice` | 浏览器桌面通知。 |
| `playNoticeSound` | 声音提醒。 |
| `speakNoticeText` | 语音播报。 |

### 7.3 常用 API

| 分组 | 函数 |
|------|------|
| 发送 | `sendNotice`、`sendSiteNotice` |
| 接收人辅助 | `getIdentityUsers`、`getNoticeOrgTree`、`getNoticePosts`、`getNoticeRoles` |
| 业务类型 | `getBusinessTypes`、`createBusinessType`、`updateBusinessType`、`deleteBusinessType` |
| 配置版本 | `getBusinessConfigVersions`、`saveBusinessConfigDraft`、`publishBusinessConfigDraft`、`activateBusinessConfigVersion` |
| 渠道模板 | `getChannelTemplates`、`saveChannelTemplate`、`publishChannelTemplate` |
| 渠道配置 | `getChannelConfigs`、`saveChannelConfig`、`deleteChannelConfig` |
| 任务和记录 | `getNoticeTasks`、`getSendRecords`、`retrySendRecord`、`markSendRecordManualSuccess`、`ignoreSendRecord` |
| 站内信 | `getMySiteMessages`、`getMySiteMessageDetail`、`getMyUnreadCount`、`markMySiteMessageRead`、`deleteMySiteMessage` |
| 接收设置 | `getRecipientAccounts`、`saveRecipientAccount`、`getReceivePreferences`、`saveReceivePreference` |
| 个人提醒 | `getNoticeReminderSetting`、`saveNoticeReminderSetting` |

## 8. 数据与初始化

本包不创建数据库表，也不初始化菜单权限。它依赖后端完成以下初始化：

| 数据 | 后端来源 | 前端用途 |
|------|----------|----------|
| 通知业务类型 | `mango-notice` migration 或管理页面 | 业务配置、发送消息、消息中心筛选。 |
| 渠道配置 | `mango-notice` migration 或渠道管理页面 | 渠道管理和实际发送。 |
| 任务和发送记录 | `mango-notice` | 任务、记录、重试页面。 |
| 站内信 | `mango-notice` | 铃铛、未读数、消息中心。 |
| 个人提醒配置 | system personal config | 保存弹窗、声音、桌面通知偏好。 |
| 用户、组织、岗位、角色 | identity、org、authorization | 接收人选择器。 |
| 实时通道 | realtime | 新消息实时提醒。 |
| 菜单权限 | authorization | 页面入口和按钮权限。 |

## 9. 管理入口

| 入口 | 页面 key | 依赖权限 |
|------|----------|----------|
| 通知业务配置 | `notice/business-config/index` | `notice:business:*` |
| 发送消息 | `notice/send-message/index` | `notice:task:create` |
| 渠道管理 | `notice/channel/index` | `notice:channel:*` |
| 通知任务 | `notice/task/index` | `notice:task:view` |
| 发送记录 | `notice/record/index` | `notice:record:view` |
| 重试管理 | `notice/retry/index` | `notice:retry:edit` |
| 站内信 | `notice/site-message/index` | `notice:site:*` |
| 全局设置 | `notice/setting/index` | `notice:setting:*` |
| 接收设置 | `notice/receive-setting/index` | `notice:receive-setting:*` |
| 顶部铃铛 | Shell provider | 登录态和站内信接口权限 |

页面可见但打不开时，先检查 `registerMangoNoticeAdminPages()` 是否执行；铃铛不显示时，检查 `registerMangoNoticeAdminShell()` 是否执行。

## 10. 问题排查

| 问题 | 优先检查 |
|------|----------|
| 菜单可见但页面打不开 | 页面 key 是否注册，菜单 component 是否能映射到 `notice/*/index`。 |
| 铃铛不显示 | 是否注册 `@mango/notice/admin-shell`，Shell 是否加载 notice provider。 |
| 未读数不变 | `/notice/site/my/unread-count` 是否可访问，站内信是否写入。 |
| 实时没有提醒 | realtime 是否启用，`createNoticeRealtime` 是否订阅，后端是否推送 notice 事件。 |
| 桌面通知不弹 | 浏览器是否授权桌面通知。 |
| 接收人选择为空 | identity、org、authorization 相关接口是否有数据和权限。 |
| 页面请求 403 | 当前角色是否有对应 `notice:*` 权限。 |

## 11. 相关文档

- [后端通知模块](../../../mango/mango-platform/mango-notice/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
