# @mango/notice

## 1. 概览
`@mango/notice` 提供通知中心前端能力：通知管理页面、站内信、发送记录、重试、渠道配置、接收偏好、客户端铃铛、消息中心和实时提醒。

本包同时包含 `admin-pages` 配套页面和 `admin-shell` 配套铃铛扩展，依赖后端 `mango-notice`、`mango-system`、identity、org、authorization 和 realtime 能力。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 管理通知业务类型、渠道、模板、任务、发送记录和重试 | 前端注册 / 组件 / API 封装 |
| 管理或查看站内信 | 前端注册 / 组件 / API 封装 |
| 在管理后台顶部接入通知铃铛和消息中心 | 前端注册 / 组件 / API 封装 |
| 用户维护通知接收偏好、收件账号和提醒方式 | 前端注册 / 组件 / API 封装 |
| 通过 SSE/WebSocket 或自定义事件接收实时通知 | 前端注册 / 组件 / API 封装 |

## 3. 适用场景
- 管理通知业务类型、渠道、模板、任务、发送记录和重试。
- 管理或查看站内信。
- 在管理后台顶部接入通知铃铛和消息中心。
- 用户维护通知接收偏好、收件账号和提醒方式。
- 通过 SSE/WebSocket 或自定义事件接收实时通知。

## 4. 边界说明
- 不负责后端通知发送、模板渲染、任务调度和第三方渠道投递。
- 不负责接收人解析、组织角色查询的数据初始化。
- 不替代普通业务消息流或 IM 系统。
- 不在前端保证通知幂等、重试和最终一致性。

## 5. 模块组成
本包包含：

- `registerMangoNoticeAdminPages`：通知管理页面注册。
- `registerMangoNoticeAdminShell`：向 Shell 注册通知铃铛 provider。
- `NoticeBell`、`NoticeClientBell`、`NoticeClientMessageCenter`、`NoticeClientReceiveSetting`。
- `noticeApi`：通知发送、配置、渠道、任务、记录、站内信、接收偏好 API。
- `createNoticeRealtime`：实时通知订阅。
- 桌面通知、声音、语音播报工具。

后端负责通知业务模型、渠道适配、权限和租户隔离。

## 6. 接入方式
安装：

```bash
pnpm add @mango/notice
```

注册管理页面：

```ts
import { registerMangoNoticeAdminPages } from '@mango/notice/admin-pages';

registerMangoNoticeAdminPages();
```

注册 Shell 铃铛：

```ts
import { registerMangoNoticeAdminShell } from '@mango/notice/admin-shell';

registerMangoNoticeAdminShell();
```

发送站内信：

```ts
import { sendSiteNotice } from '@mango/notice';

await sendSiteNotice({
  bizType: 'order_audit',
  title: '订单待审核',
  content: '请处理待审核订单',
  recipients: [],
});
```

实时提醒：

```ts
import { createNoticeRealtime } from '@mango/notice/realtime';

const dispose = createNoticeRealtime((event) => {
  console.log(event.messageId, event.title);
});
```

## 7. 配置说明
| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 | 源码入口 |
|----------|------------|--------|------|----------|----------|
| `registerMangoNoticeAdminPages` | `moduleCode` | `mango-notice` | 页面归属模块 | 和后端菜单匹配 | `admin-pages.ts` |
| 页面注册 | `component` | notice 多个页面 key | 通知页面 key | 菜单打开页面 | `admin-pages.ts` |
| `registerMangoNoticeAdminShell` | provider | `NoticeBell` | Shell 铃铛组件 | 顶部通知入口 | `admin-shell.ts` |
| 个人配置 | `groupCode` | `notice` | 提醒配置分组 | 保存个人提醒设置 | `NOTICE_REMINDER_CONFIG_SCOPE` |
| 个人配置 | `bizType` | `client_reminder` | 提醒配置业务类型 | 保存个人提醒设置 | `NOTICE_REMINDER_CONFIG_SCOPE` |
| 个人配置 | `configKey` | `reminder_setting` | 提醒配置 key | 保存个人提醒设置 | `NOTICE_REMINDER_CONFIG_SCOPE` |
| `NoticeReminderSetting` | `popupEnabled` | `true` | 是否弹出提醒 | 影响铃铛提醒 | `defaultNoticeReminderSetting` |
| `NoticeReminderSetting` | `popupPlacement` | `top-right` | 弹出位置 | top-right 或 bottom-right | `normalizeNoticeReminderSetting` |
| `NoticeReminderSetting` | `voiceEnabled` | `true` | 是否声音/语音 | 影响提醒声音 | `defaultNoticeReminderSetting` |
| `NoticeReminderSetting` | `reminderMode` | `SOUND` | 声音或语音 | 影响播放方式 | `normalizeNoticeReminderSetting` |
| `NoticeReminderSetting` | `soundType` | `IM` | 声音类型 | IM、SOFT、DOUBLE、NONE | `normalizeNoticeReminderSetting` |
| `NoticeReminderSetting` | `desktopNotificationEnabled` | `true` | 浏览器桌面通知 | 需要浏览器授权 | `defaultNoticeReminderSetting` |
| `createNoticeRealtime` | `realtimeOptions` | 空 | 实时连接配置 | 传给 `createRealtimeClient` | `noticeRealtime.ts` |

## 8. API 与扩展
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

### 7.2 API 分组

| 分组 | 主要函数 |
|------|----------|
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

### 7.3 客户端和实时

| 导出 | 用途 |
|------|------|
| `NoticeBell` | Shell 顶部铃铛 |
| `NoticeDetailDialog` | 通知详情弹窗 |
| `NoticeClientBell` | 客户端铃铛 |
| `NoticeClientMessageCenter` | 消息中心 |
| `NoticeClientReceiveSetting` | 接收设置 |
| `createNoticeRealtime` | 实时通知订阅 |
| `requestDesktopPermission` | 请求桌面通知权限 |
| `showDesktopNotice` | 浏览器桌面通知 |
| `playNoticeSound` | 声音提醒 |
| `speakNoticeText` | 语音播报 |

## 9. 数据与初始化
本包不包含数据库 migration。依赖后端初始化：

| 类型 | 后端来源 | 前端消费 | 排查入口 |
|------|----------|----------|----------|
| 通知业务类型 | notice | 业务配置、发送消息 | 业务类型列表有数据 |
| 渠道配置 | notice | 渠道管理、发送 | 渠道可保存并发送 |
| 任务和记录 | notice | 任务、记录、重试页面 | 发送后生成记录 |
| 站内信 | notice | 铃铛和消息中心 | 未读数变化 |
| 个人配置 | system personal-configs | 提醒设置 | 保存后刷新仍生效 |
| 用户、组织、岗位、角色 | identity、org、authorization | 接收人选择 | 选择器可搜索 |
| 实时通道 | realtime | `createNoticeRealtime` | 新消息实时触发 |

## 10. 管理入口
| 菜单 / 页面 | component key | 权限码 | 入库来源 | 默认套餐 / 角色 | 后端校验入口 |
|-------------|---------------|--------|----------|-----------------|--------------|
| 通知管理页面 | `notice/*/index` | 后端 notice 模块定义 | 后端 resource / migration | 角色授权 | notice admin API |
| 通知铃铛 | Shell provider | 登录态和站内信权限 | Shell 注册 | 当前登录用户 | notice site API |
| 接收设置 | `notice/receive-setting/index` | 当前用户或管理权限 | 后端 resource / migration | 角色授权 | notice receive API |

通知数据必须按租户隔离。前端请求通过 `@mango/common` 带租户头，后端仍要校验。

## 11. 快速开始
1. 后端启用 notice、system personal-config、identity、org、authorization 和 realtime。
2. 前端注册 `@mango/notice/admin-pages`。
3. Shell 注册 `@mango/notice/admin-shell`。
4. 后端初始化菜单权限并授权。
5. 配置业务类型、渠道和模板。
6. 发送站内信，验证任务记录、消息中心、未读数和实时提醒。
7. 验证不同租户和不同角色的数据隔离。

## 12. 问题排查
| 问题 | 原因 | 处理方式 |
|------|------|----------|
| 铃铛不显示 | 没注册 `registerMangoNoticeAdminShell` | 在 Shell feature registrar 中注册 |
| 未读数不变 | 站内信接口或权限异常 | 查 `/notice/site/my/unread-count` |
| 实时没提醒 | realtime 未启用或未订阅 notice topic | 查 realtime 连接和后端推送 |
| 桌面通知不弹 | 浏览器权限未授权 | 调用 `requestDesktopPermission` 并检查浏览器设置 |
| 接收人选择为空 | identity/org/role 接口无数据或无权限 | 查辅助接口和角色权限 |

## 13. 相关文档
- [前端代码规范](../../../mango-pmo/rules/frontend/01-vue-code.md)
- [前端测试规范](../../../mango-pmo/rules/frontend/04-test.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [后端 Notice](../../../mango/mango-platform/mango-notice/README.md)

## 14. 历史资料
- [Mango UI README](../../README.md)
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
