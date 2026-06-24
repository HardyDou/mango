# 消息中心系统小组件设计方案

## 1. 背景

工作台已通过 `@mango/grid-layout` 和 `@mango/grid-widgets` 支持系统小组件与业务小组件组合。用户确认继续开发新的系统小组件“消息中心”，用于在工作台中轻量展示当前登录人的未读消息情况，并保留进入完整消息中心页面的入口。

本次视觉方向沿用已确认的消息中心小组件草图，不再调整视觉结构。

## 2. 目标

- 在 `@mango/grid-widgets` 新增 `system.message-center` 系统小组件。
- 小组件展示未读总数、最新未读消息、分类未读统计。
- 小组件提供“查看全部”和“全部已读”操作。
- 工作台默认布局增加消息中心小组件。
- 更新 `@mango/grid-widgets` README 和能力地图。

## 3. 不做范围

- 不修改 `@mango/grid-layout`。
- 不新增消息后端接口、数据库表、菜单或权限。
- 不把通知业务逻辑写入工作台页面。
- 不在本次实现小组件级权限过滤。
- 不把消息中心数据写入个人布局 JSON。

## 4. 影响模块

| 模块 | 改动 |
|------|------|
| `@mango/grid-widgets` | 新增消息中心小组件、注册定义、类型、样式、包导出和 README |
| `@mango/admin-shell` 工作台 | 默认布局新增 `system.message-center` 布局项 |
| `mango-docs` | 同步设计文档、能力地图和交付台账 |

## 5. 文件结构

```text
mango-ui/packages/grid-widgets/src/system/message-center/
├─ MessageCenterWidget.vue
├─ index.ts
└─ message-center.ts
```

系统小组件仍按独立目录组织，组件、注册定义和导出放在同一个目录中，公共类型继续从包级 `types.ts` 导出。

## 6. 小组件设计

消息中心小组件内部渲染完整标题、内容和操作区，因此注册定义设置：

```ts
showTitle: false
padding: false
```

默认布局尺寸为 `3 x 10`，和当前工作台系统小组件保持一致。

展示内容：

| 区域 | 内容 |
|------|------|
| 标题 | 我的消息、更新时间提示 |
| 摘要 | 未读消息总数、最新未读标题 |
| 分类统计 | 系统通知、业务通知、审批通知、告警通知 |
| 操作 | 查看全部、全部已读 |
| 异常 | 接口失败时在小组件内部显示错误提示 |

## 7. 数据与接口

小组件复用 `@mango/notice` 已有前端 API：

| 能力 | API |
|------|-----|
| 未读总数 | `getMyUnreadCount()` |
| 最新未读 | `getMySiteMessages({ pageNum: 1, pageSize, unreadOnly: true })` |
| 分类统计 | `getMySiteMessages({ pageNum: 1, pageSize: 1, unreadOnly: true, bizGroup, bizType, priority })` |
| 全部已读 | `markAllMySiteMessagesRead()` |

默认分类配置：

| key | 文案 | 查询口径 |
|-----|------|----------|
| `system` | 系统通知 | `bizGroup: '系统'` |
| `business` | 业务通知 | `bizGroup: '业务'` |
| `approval` | 审批通知 | `bizGroup: '审批'` |
| `alert` | 告警通知 | `bizGroup: '告警'` |

消费页面可以通过 `categories` 覆盖分类口径。分类配置只影响小组件查询和展示，不写入个人布局。

## 8. 跳转

小组件不直接依赖宿主 router。点击“查看全部”时调用：

```ts
runtime.navigate({ path: messageCenterPath })
```

默认 `messageCenterPath` 为 `/notice/site-message`。宿主可以按单体、微前端宿主、子应用或独立部署场景自行实现 `runtime.navigate`。

## 9. 权限与数据边界

本次不做小组件库可见性过滤，所有系统小组件可进入组件库。消息中心数据权限由通知接口在后端按当前登录人、租户和权限控制。

当前端接口返回空数据时，小组件展示未读数为 `0` 和“暂无未读消息”；接口失败时，小组件内部显示错误态，不影响工作台其它小组件。

## 10. 验证方式

- `pnpm.cmd -F @mango/grid-widgets build`
- `pnpm.cmd -F @mango/admin-shell build`
- `pnpm.cmd admin:styles:check`
- `pnpm.cmd admin:module-styles:check`
- 启动前后端后登录工作台，验证默认布局、组件库、查看全部、全部已读、空状态和接口异常提示。

## 11. 风险

| 风险 | 处理 |
|------|------|
| 通知接口无数据时页面看起来较空 | 小组件展示 `0` 和“暂无未读消息” |
| 消息分类口径和后端业务类型不一致 | 默认按 `bizGroup` 查询，后续可通过 `categories` 覆盖；真实接口验证发现当前登录消息业务分组为 `AUTH`，会出现总未读数大于 0 但“系统/业务/审批/告警”四个默认分类均为 0 的情况，需由产品/领导确认默认分类口径是否调整 |
| 宿主未传 `runtime.navigate` | 查看全部不执行跳转，README 已列入排查项 |
| `@mango/grid-widgets` 新增 `@mango/notice` 依赖 | 包构建和 admin-shell 构建验证依赖边界 |
