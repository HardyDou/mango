# @mango/grid-widgets 使用说明

## 1. 概览

`@mango/grid-widgets` 是 Mango 前端系统小组件与小组件注册聚合工具包，定位为 `@mango/grid-layout` 的上层配套能力。它提供 Mango 预制小组件、系统小组件与业务小组件的合并去重能力，并把最终 `widgets` 交给自定义栅格布局组件渲染。

本包不负责布局拖拽、个人布局保存、登录态读取、权限接口读取和业务数据查询。布局能力由 `@mango/grid-layout` 提供，业务页面只需要按自身场景传入运行时上下文和业务小组件。

## 2. 功能清单

| 能力 | 用途 | 常用入口 |
|------|------|----------|
| 小组件聚合 | 合并系统小组件、业务小组件和直接传入的小组件 | `mergeGridWidgets` |
| 去重策略 | 同一 `type` 只保留先注册的小组件，避免隐式覆盖 | `MergeGridWidgetsOptions.onDuplicate` |
| 排序策略 | 按 `order`、`category`、`title` 排序，保证组件库展示稳定 | `mergeGridWidgets` |
| 运行时注入 | 把宿主传入的用户、租户、菜单、跳转函数注入小组件 | `MangoWidgetRuntimeContext` |
| 系统小组件集合 | 汇总 Mango 预制系统小组件，便于工作台一次性接入 | `systemGridWidgets` |
| 用户信息小组件 | 展示当前登录人信息，并跳转到个人中心和修改密码 | `systemUserProfileWidgets` |
| 快捷入口小组件 | 选择可见菜单并保存到浏览器本地，点击后跳转到对应模块 | `systemQuickEntryWidgets` |
| 消息中心小组件 | 展示当前登录人的未读消息、最新未读摘要和消息分类统计 | `systemMessageCenterWidgets` |
| 样式入口 | 独立消费系统小组件样式 | `@mango/grid-widgets/style.css` |
| 类型导出 | 业务侧声明小组件、运行时上下文、快捷入口菜单和消息中心分类 | `MangoGridWidgetDefinition`、`QuickEntryWidgetProps`、`MessageCenterWidgetProps` |

## 3. 接入方式

业务页面需要先接入 `@mango/grid-layout`，再从本包导入系统小组件和聚合工具：

```ts
import { mergeGridWidgets, systemGridWidgets } from '@mango/grid-widgets';
import '@mango/grid-widgets/style.css';
```

后台首页当前接入方式是：工作台页面负责准备当前登录人、租户、菜单树、跳转能力和业务侧默认布局，再把聚合后的 `widgets` 传入 `MangoGridDesigner` 与 `MangoGridLayout`。

```ts
const widgets = mergeGridWidgets({
  runtime,
  systemWidgets: systemGridWidgets,
  businessWidgets: [],
});
```

本包依赖的 Mango 能力如下：

| Mango 能力 | 本模块使用位置 | 文档入口 |
|------------|----------------|----------|
| 自定义栅格布局前端 | 小组件定义最终传给 `MangoGridDesigner` 和 `MangoGridLayout` | [@mango/grid-layout README](../grid-layout/README.md) |
| 公共组件 | 快捷入口配置弹框使用 `MangoDialog`，图标解析使用 `iconMap` | [@mango/common README](../common/README.md) |
| 通知中心前端 | 消息中心小组件读取未读消息、最新未读和消息分类统计 | [@mango/notice README](../notice/README.md) |

## 4. 配置说明

本包不读取全局配置，所有配置通过函数参数、组件 props 或运行时上下文传入。

| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 | 源码入口 |
|----------|------------|--------|------|----------|----------|
| `mergeGridWidgets` | `widgets` | `[]` | 直接传入的小组件列表 | 最先参与合并，同 `type` 时优先保留 | `src/registry.ts` |
| `mergeGridWidgets` | `systemWidgets` | `[]` | Mango 系统小组件列表 | 排在 `widgets` 之后参与合并 | `src/registry.ts` |
| `mergeGridWidgets` | `businessWidgets` | `[]` | 业务系统小组件列表 | 排在系统小组件之后参与合并 | `src/registry.ts` |
| `mergeGridWidgets` | `runtime` | `undefined` | 小组件运行时上下文 | 注入到小组件 props，不进入个人布局 JSON | `src/registry.ts` |
| `mergeGridWidgets` | `onDuplicate` | `undefined` | 重复 `type` 回调 | 发现重复注册时可记录或提示 | `src/registry.ts` |
| `QuickEntryWidget` | `menus` | `[]` | 快捷入口可选菜单来源 | 优先于 `runtime.menus` 使用 | `src/system/quick-entry/QuickEntryWidget.vue` |
| `QuickEntryWidget` | `resolveMenus` | `undefined` | 自定义菜单解析函数 | 覆盖默认菜单过滤和映射规则 | `src/system/quick-entry/QuickEntryWidget.vue` |
| `QuickEntryWidget` | `storageKey` | 按页面、租户、用户生成 | 本地保存 key | 决定快捷入口选择保存位置 | `src/system/quick-entry/QuickEntryWidget.vue` |
| `QuickEntryWidget` | `maxDefaultItems` | `6` | 未保存配置时默认展示数量 | 决定首次进入工作台的快捷入口数量 | `src/system/quick-entry/QuickEntryWidget.vue` |
| `QuickEntryWidget` | `navigate` | `undefined` | 小组件级跳转函数 | 优先于 `runtime.navigate` 调用 | `src/system/quick-entry/QuickEntryWidget.vue` |
| `UserProfileWidget` | `runtime` | `undefined` | 当前登录人和租户上下文 | 决定用户信息展示和跳转能力 | `src/system/user-profile/UserProfileWidget.vue` |
| `UserProfileWidget` | `profilePath` | `/profile` | 个人中心页面路径 | 点击个人中心按钮时传给 `runtime.navigate` | `src/system/user-profile/UserProfileWidget.vue` |
| `UserProfileWidget` | `passwordPath` | `/password` | 修改密码页面路径 | 点击修改密码按钮时传给 `runtime.navigate` | `src/system/user-profile/UserProfileWidget.vue` |
| `MessageCenterWidget` | `runtime` | `undefined` | 当前页面跳转上下文 | 点击查看全部时通过 `runtime.navigate` 跳转 | `src/system/message-center/MessageCenterWidget.vue` |
| `MessageCenterWidget` | `messageCenterPath` | `/notice/site-message` | 消息中心页面路径 | 点击查看全部时传给 `runtime.navigate` | `src/system/message-center/MessageCenterWidget.vue` |
| `MessageCenterWidget` | `pageSize` | `1` | 最新未读查询条数 | 决定最新未读摘要读取数量 | `src/system/message-center/MessageCenterWidget.vue` |
| `MessageCenterWidget` | `categories` | 系统、业务、审批、告警 | 消息分类统计配置 | 按 `bizGroup`、`bizType` 或 `priority` 查询未读统计 | `src/system/message-center/MessageCenterWidget.vue` |

## 5. API 与扩展

### 聚合 API

`mergeGridWidgets(options)` 返回可直接传给 `@mango/grid-layout` 的小组件定义列表。

| 参数 | 说明 |
|------|------|
| `widgets` | 直接传入的小组件定义，适合消费页面临时补充 |
| `systemWidgets` | Mango 系统预制小组件，当前包含用户信息、快捷入口和消息中心小组件 |
| `businessWidgets` | 业务系统自定义小组件 |
| `runtime` | 当前页面运行时上下文，会通过包装组件注入到每个小组件 |
| `onDuplicate` | 重复 `type` 处理回调，当前策略保留先注册项、忽略后注册项 |

### 运行时上下文

```ts
interface MangoWidgetRuntimeContext {
  pageCode: string;
  mode?: 'host' | 'sub-app' | 'standalone';
  user?: { userId?: string | number; username?: string; nickname?: string; avatar?: string; roles?: string[]; appCode?: string };
  tenant?: { tenantId?: string | number; tenantCode?: string; tenantName?: string };
  menus?: unknown[];
  navigate?: (target: MangoWidgetNavigateTarget) => void | Promise<void>;
}
```

`runtime` 只用于小组件运行期，不会写入个人布局 JSON，避免把菜单树、用户上下文或跳转函数持久化到布局配置。

### 系统小组件目录

系统小组件按独立目录组织。新增系统小组件时，组件、注册定义和私有类型优先放在同一个目录中，公共类型仍从包级 `types.ts` 导出。

```text
src/system/
├─ quick-entry/
│  ├─ QuickEntryWidget.vue
│  ├─ index.ts
│  └─ quick-entry.ts
├─ message-center/
│  ├─ MessageCenterWidget.vue
│  ├─ index.ts
│  └─ message-center.ts
├─ user-profile/
│  ├─ UserProfileWidget.vue
│  ├─ index.ts
│  └─ user-profile.ts
└─ index.ts
```

### 用户信息小组件

用户信息小组件展示当前登录人的头像、昵称、账号、租户、角色和应用标识。组件不直接读取宿主 store 或 router，只消费 `runtime.user`、`runtime.tenant` 和 `runtime.navigate`。

默认按钮行为：

| 按钮 | 默认路径 | 行为 |
|------|----------|------|
| 个人中心 | `/profile` | 调用 `runtime.navigate({ path: '/profile' })` |
| 修改密码 | `/password` | 调用 `runtime.navigate({ path: '/password' })` |

### 快捷入口小组件

快捷入口小组件负责菜单适配、过滤、展示、选择、本地保存和触发跳转。默认只保留可见的菜单页面，过滤目录、按钮、隐藏菜单和不可跳转项。宿主系统和微前端子系统可以通过 `runtime.navigate` 统一处理路由跳转。

### 消息中心小组件

消息中心小组件展示当前登录人的未读消息总数、最新未读标题和分类未读统计，并提供“查看全部”和“全部已读”两个操作。组件不直接读取宿主 store 或 router，只消费 `@mango/notice` 的通知接口和宿主注入的 `runtime.navigate`。

默认接口行为：

| 能力 | 接口 | 说明 |
|------|------|------|
| 未读总数 | `getMyUnreadCount()` | 读取当前登录人的未读站内消息数量 |
| 最新未读 | `getMySiteMessages({ pageNum: 1, pageSize, unreadOnly: true })` | 读取最新未读消息标题 |
| 分类统计 | `getMySiteMessages({ pageNum: 1, pageSize: 1, unreadOnly: true, bizGroup, bizType, priority })` | 按分类配置读取未读总数 |
| 全部已读 | `markAllMySiteMessagesRead()` | 把当前登录人的站内消息全部标记为已读 |
| 查看全部 | `runtime.navigate({ path: messageCenterPath })` | 默认跳转到 `/notice/site-message` |

默认分类为系统通知、业务通知、审批通知和告警通知。消费页面可以通过 `categories` 覆盖分类口径，但分类配置只影响查询和展示，不会写入个人布局 JSON。

```ts
import { mergeGridWidgets, systemGridWidgets } from '@mango/grid-widgets';

const runtime = {
  pageCode: 'admin-home-workbench',
  mode: 'host',
  user: {
    userId: userInfo.userInfos.userId,
    username: userInfo.userInfos.username,
    nickname: userInfo.userInfos.nickname,
    avatar: userInfo.userInfos.photo,
    roles: userInfo.userInfos.roles,
    appCode: userInfo.userInfos.appCode,
  },
  tenant: {
    tenantId: userInfo.userInfos.tenantId,
    tenantCode: userInfo.userInfos.tenantCode,
    tenantName: userInfo.userInfos.tenantName,
  },
  menus: routesList.value,
  navigate: target => router.push(target.path),
};

const widgets = mergeGridWidgets({
  runtime,
  systemWidgets: systemGridWidgets,
});
```

## 6. 数据与初始化

本包不维护数据库 migration、后端接口、菜单资源清单、字典或默认权限数据。

快捷入口小组件第一版把用户选择保存在浏览器 `localStorage`，默认 key 由 `pageCode`、`tenantId` 和 `username` 或 `userId` 组成：

```ts
`mango:${pageCode}:quick-entry:${tenantId}:${username}`
```

保存内容是菜单 ID 字符串数组。工作台布局仍然由 `@mango/grid-layout` 的个人布局能力保存，快捷入口选择不和个人布局 JSON 混在一起。

消息中心小组件不新增本地存储，也不修改个人布局 JSON。消息数据、未读数和已读状态全部来自 `@mango/notice` 对应真实接口，接口权限、租户和数据范围由通知后端控制。

## 7. 管理入口

本包不新增后台菜单、按钮权限码、角色授权数据或系统参数页面。第一版不做小组件权限过滤，所有注册小组件都进入组件库，数据权限由各自业务接口控制。

如果后续需要按角色、菜单或业务权限控制小组件可见性，应在消费页面或业务侧小组件注册工具中生成可用小组件列表后，再传给 `mergeGridWidgets`，不要把业务权限接口耦合进布局组件或本包核心聚合逻辑。

## 8. 快速开始

最小接入示例：

```vue
<template>
  <MangoGridDesigner
    v-if="editing"
    v-model="items"
    :widgets="widgets"
  />
  <MangoGridLayout
    v-else
    :items="items"
    :widgets="widgets"
  />
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { MangoGridDesigner, MangoGridLayout } from '@mango/grid-layout';
import { mergeGridWidgets, systemGridWidgets } from '@mango/grid-widgets';
import '@mango/grid-layout/style.css';
import '@mango/grid-widgets/style.css';

const editing = ref(false);
const items = ref([]);

const runtime = computed(() => ({
  pageCode: 'admin-home-workbench',
  menus: [],
  navigate: target => {
    if (target.path) {
      router.push(target.path);
    }
  },
}));

const widgets = computed(() => mergeGridWidgets({
  runtime: runtime.value,
  systemWidgets: systemGridWidgets,
}));
</script>
```

包构建验证：

```bash
pnpm.cmd -F @mango/grid-widgets build
```

后台首页消费场景可继续执行：

```bash
pnpm.cmd -F @mango/admin-shell build
pnpm.cmd admin:styles:check
pnpm.cmd admin:module-styles:check
```

## 9. 返回字段

本包没有 HTTP API 返回对象。对外暴露的主要前端数据结构如下：

| 字段 | 说明 | 是否建议业务入库 |
|------|------|------------------|
| `MangoGridWidgetDefinition.type` | 小组件唯一类型，布局项通过 `widgetType` 匹配 | 可作为布局 JSON 的引用值 |
| `MangoGridWidgetDefinition.title` | 小组件展示名称 | 不建议单独入库，通常随小组件定义维护 |
| `MangoGridWidgetDefinition.category` | 组件库分组名称 | 不建议单独入库，通常随小组件定义维护 |
| `MangoGridWidgetDefinition.defaultLayout` | 小组件默认宽高和约束 | 不建议单独入库，布局保存后以后端布局项为准 |
| `UserProfileWidgetProps.profilePath` | 个人中心跳转路径 | 不建议单独入库，可由消费页面 props 覆盖 |
| `UserProfileWidgetProps.passwordPath` | 修改密码跳转路径 | 不建议单独入库，可由消费页面 props 覆盖 |
| `MessageCenterCategory.key` | 消息中心分类唯一 key | 不建议单独入库，通常随小组件配置维护 |
| `MessageCenterCategory.bizGroup` | 消息分类所属业务分组 | 不建议单独入库，可由消费页面 props 覆盖 |
| `MessageCenterCategory.bizType` | 消息分类所属业务类型 | 不建议单独入库，可由消费页面 props 覆盖 |
| `MessageCenterCategory.priority` | 消息分类优先级 | 不建议单独入库，可由消费页面 props 覆盖 |
| `MessageCenterWidgetProps.messageCenterPath` | 消息中心页面跳转路径 | 不建议单独入库，可由消费页面 props 覆盖 |
| `QuickEntryMenuItem.id` | 快捷入口菜单唯一 ID | 本版仅保存到 `localStorage` |
| `QuickEntryMenuItem.title` | 快捷入口展示名称 | 跟随菜单数据，不单独保存 |
| `QuickEntryMenuItem.path` | 快捷入口路由路径 | 跟随菜单数据，不单独保存 |
| `QuickEntryMenuItem.url` | 外链菜单地址 | 跟随菜单数据，不单独保存 |

## 10. 问题排查

| 问题 | 排查方向 |
|------|----------|
| 组件库里没有系统小组件 | 检查是否把 `systemGridWidgets` 传给 `mergeGridWidgets` |
| 卡片内容不显示 | 检查布局项 `widgetType` 是否等于 `system.user-profile`、`system.quick-entry`、`system.message-center` 或业务小组件 `type` |
| 用户信息显示为空 | 检查 `runtime.user`、`runtime.tenant` 是否传入 |
| 用户信息按钮不跳转 | 检查 `runtime.navigate` 是否传入，并确认 `/profile`、`/password` 已注册 |
| 消息中心没有显示 | 检查 `@mango/notice` 是否已安装，`systemMessageCenterWidgets` 或 `systemGridWidgets` 是否传入 |
| 消息中心数量一直为 0 | 检查 `/notice/site/my/unread-count` 和 `/notice/site/my/messages` 是否返回当前登录人未读数据 |
| 消息分类统计不符合预期 | 检查 `categories` 中的 `bizGroup`、`bizType`、`priority` 是否和通知业务类型配置一致 |
| 查看全部不跳转 | 检查 `runtime.navigate` 是否传入，并确认 `messageCenterPath` 对应页面已注册 |
| 全部已读失败 | 检查 `/notice/site/my/messages/read-all` 接口权限、登录态和后端错误日志 |
| 快捷入口没有菜单 | 检查 `runtime.menus` 或 `menus` 是否传入可见菜单页面 |
| 搜索不到菜单 | 检查菜单是否为目录、按钮、隐藏菜单或不可跳转项，默认解析会过滤这些数据 |
| 点击快捷入口不跳转 | 检查 `runtime.navigate` 或 `navigate` 是否传入，并确认微前端场景的跳转适配 |
| 刷新后选择丢失 | 检查 `storageKey` 是否稳定，以及浏览器是否允许访问 `localStorage` |
| 样式缺失 | 检查业务入口是否引入 `@mango/grid-widgets/style.css` |

## 11. 相关文档

- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [前端组件开发规范](../../../mango-pmo/rules/frontend/03-component-development.md)
- [前端 Monorepo 架构规范](../../../mango-pmo/rules/frontend/06-monorepo-architecture.md)
- [@mango/grid-layout README](../grid-layout/README.md)
- [@mango/common README](../common/README.md)
- [@mango/notice README](../notice/README.md)
- [Grid Widgets 注册聚合设计方案](../../../mango-docs/designs/mango-grid-widgets-registry-design.md)
- [Grid Widgets 注册聚合交付台账](../../../mango-docs/plans/2026-06-22-grid-widgets-registry-delivery-ledger.md)
