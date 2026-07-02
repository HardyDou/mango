# @mango/grid-widgets 使用说明

## 1. 概览

`@mango/grid-widgets` 是 Mango 首页小组件的注册、类型与聚合基础包。它不再承载具体业务小组件实现，具体组件跟随业务 UI 包维护：业务包被集成并执行自身 admin registrar 后，小组件自动注册到首页组件库。

本包不负责布局拖拽、个人布局保存、登录态读取、权限接口读取和业务数据查询。布局能力由 `@mango/grid-layout` 提供，业务数据由各业务包自己的小组件实现读取。

## 2. 功能清单

| 能力 | 用途 | 入口 |
|------|------|------|
| 小组件定义类型 | 约束业务包注册的小组件元数据 | `MangoGridWidgetDefinition` |
| 运行时上下文 | 向小组件注入用户、租户、菜单、跳转函数 | `MangoWidgetRuntimeContext` |
| 聚合去重 | 合并系统、业务、临时传入的小组件 | `mergeGridWidgets` |
| 基础类型 | 快捷入口、消息中心、日历、工作流小组件 props 类型 | 包根入口 |

## 3. 快速开始

业务后台通常不需要直接操作 `@mango/grid-widgets`。应用集成 `@mango/admin-shell` 和对应业务 UI 包后，业务包的 admin registrar 会返回小组件定义，Shell 自动把它们聚合到首页组件库。

```ts
import { mergeGridWidgets } from '@mango/grid-widgets';

const widgets = mergeGridWidgets({
  runtime,
  businessWidgets: registeredWidgets,
});
```

独立集成时至少准备：

| 输入 | 来源 | 说明 |
|------|------|------|
| `runtime.user` | 宿主登录态 | 小组件展示当前用户、租户或权限相关内容时使用 |
| `runtime.menus` | 宿主菜单与权限结果 | 快捷入口、网址导航等需要判断可访问菜单 |
| `runtime.navigate` | 宿主路由跳转函数 | 小组件点击后交给宿主完成跳转 |
| `businessWidgets` | 已执行的业务包 registrar | 只聚合当前应用实际集成的业务小组件 |

## 4. 组件归属

| 小组件 | 类型 | 所属业务包 |
|--------|------|------------|
| 用户信息 | `system.user-profile` | `@mango/system` |
| 快捷入口 | `system.quick-entry` | `@mango/system` |
| 网址导航 | `system.link-navigation` | `@mango/link` |
| 日历 | `calendar.calendar` | `@mango/calendar` |
| 消息中心 | `notice.message-center` | `@mango/notice` |
| 我的待办 | `workflow.my-todo` | `@mango/workflow` |
| 我的申请 | `workflow.my-process` | `@mango/workflow` |
| 我的任务 | `workflow.my-task` | `@mango/workflow` |

## 5. 接入方式

业务包通过自身 admin registrar 返回 `widgets`，`@mango/admin-shell` 执行已集成业务包的 registrar 后，会把这些小组件纳入首页组件库。新应用只需要集成对应业务 UI 包和它的 registrar，不需要在首页逐个登记组件。

```ts
import { mergeGridWidgets } from '@mango/grid-widgets';

const widgets = mergeGridWidgets({
  runtime,
  businessWidgets: registeredWidgets,
});
```

如果某个业务包未被集成，它的 registrar 不会执行，对应小组件不会出现在组件库，也不能被新增使用。若历史布局中保存了已移除业务包的小组件类型，布局渲染会按已有失效组件机制兼容处理。

## 6. 配置说明

`@mango/grid-widgets` 自身没有远程配置、环境变量或持久化配置项。它只消费调用方传入的运行时上下文和小组件定义。

| 配置项 | 设置位置 | 默认值 | 行为影响 |
|--------|----------|--------|----------|
| `runtime` | 调用 `mergeGridWidgets` 时传入 | 无 | 决定小组件能读取的用户、菜单、租户和跳转能力 |
| `businessWidgets` | 业务包 admin registrar 返回 | `[]` | 决定组件库出现哪些业务小组件 |
| `extraWidgets` | 调用方额外传入 | `[]` | 允许宿主追加临时或应用私有小组件 |

具体业务小组件的数据接口、权限码和样式入口由所属业务包 README 说明，例如 `@mango/link`、`@mango/system`、`@mango/calendar`、`@mango/notice` 和 `@mango/workflow`。

## 7. API 与扩展

### `mergeGridWidgets`

`mergeGridWidgets` 用于把不同来源的小组件定义合并成首页组件库可消费的列表，并按 `type` 去重。调用方应保证每个小组件 `type` 稳定且全局唯一。

```ts
import type { MangoGridWidgetDefinition } from '@mango/grid-widgets';
import { mergeGridWidgets } from '@mango/grid-widgets';

const widgets: MangoGridWidgetDefinition[] = mergeGridWidgets({
  runtime,
  businessWidgets,
  extraWidgets,
});
```

### `MangoGridWidgetDefinition`

业务包新增小组件时放在自身 UI 包内，例如：

```text
packages/<business>/src/widgets/<widget-code>/
├─ <WidgetName>.vue
├─ <widget-code>.ts
└─ index.ts
```

小组件定义必须声明业务域信息：

```ts
export const exampleWidgets: MangoGridWidgetDefinition[] = [
  {
    type: 'business.example',
    title: '示例组件',
    component: ExampleWidget,
    businessDomainCode: 'BUSINESS',
    businessDomainName: '业务名称',
    groupName: '业务名称',
  },
];
```

业务包的 `admin-pages` registrar 返回组件列表：

```ts
export function registerMangoBusinessAdminPages() {
  return {
    businessDomainCode: 'BUSINESS',
    businessDomainName: '业务名称',
    groupName: '业务名称',
    widgets: exampleWidgets,
  };
}
```

小组件定义应通过业务包根入口或业务包 `admin-pages` registrar 暴露，不能让业务项目依赖仓库内部路径。

## 8. 数据与初始化

`@mango/grid-widgets` 不初始化数据库、不写入菜单、不创建权限、不保存个人布局，也不直接请求业务数据。

| 数据 | 所属模块 | 初始化或读取方式 |
|------|----------|------------------|
| 首页布局 | `@mango/admin-shell` + `@mango/grid-layout` | Shell 读取并保存当前用户布局 |
| 网址导航数据 | `mango-link` / `@mango/link` | Link 后端接口与前端小组件读取 |
| 用户和快捷入口 | `mango-system` / `@mango/system` | 宿主登录态、菜单和系统前端能力提供 |
| 日历数据 | `mango-calendar` / `@mango/calendar` | Calendar 业务包小组件读取 |
| 消息中心 | `mango-notice` / `@mango/notice` | Notice 业务包小组件读取 |
| 工作流待办 | `mango-workflow` / `@mango/workflow` | Workflow 业务包小组件读取 |

如果业务项目缺少对应后端 starter、前端包或菜单权限，对应小组件不应通过 `@mango/grid-widgets` 兜底造数据。

## 9. 管理入口

首页小组件的用户入口在管理后台首页。组件库由 `@mango/admin-shell` 渲染，布局拖拽和卡片尺寸由 `@mango/grid-layout` 承担。

| 入口 | 负责模块 | 说明 |
|------|----------|------|
| 首页组件库 | `@mango/admin-shell` | 展示已集成业务包注册的小组件 |
| 首页布局编辑 | `@mango/grid-layout` | 拖拽、调整尺寸、保存个人布局 |
| 业务小组件内容 | 各业务 UI 包 | 自己读取接口、处理空态和权限不足 |

菜单是否可见、按钮权限和业务数据权限由所属业务模块负责。`@mango/grid-widgets` 只维护聚合契约，不提供后台管理页面。

## 10. 问题排查

| 现象 | 排查方向 |
|------|----------|
| 组件库没有某个业务小组件 | 确认业务 UI 包是否已安装、是否在 admin/full 或 CLI 模块清单中声明、admin registrar 是否被执行 |
| 小组件样式缺失 | 确认所属业务包是否导出 `./style.css`，admin 样式聚合是否包含该业务包 |
| 历史布局出现失效组件 | 确认保存的 widget `type` 是否来自已移除或未集成业务包，进入布局编辑清理 |
| 小组件显示空态 | 查看所属业务包 README，确认后端 starter、菜单权限、租户数据和接口返回 |
| 点击跳转无反应 | 确认宿主传入 `runtime.navigate`，且目标菜单或路由已注册 |

## 11. 相关文档

- [@mango/grid-layout README](../grid-layout/README.md)
- [@mango/admin-shell README](../admin-shell/README.md)
- [@mango/link README](../link/README.md)
- [前端组件开发规范](../../../mango-pmo/rules/frontend/03-component-development.md)
- [前端 Monorepo 架构规范](../../../mango-pmo/rules/frontend/06-monorepo-architecture.md)
