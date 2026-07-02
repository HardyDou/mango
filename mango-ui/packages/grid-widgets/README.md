# @mango/grid-widgets 使用说明

## 1. 概览

`@mango/grid-widgets` 是 Mango 首页小组件的注册、类型与聚合基础包。它不再承载具体业务小组件实现，具体组件跟随业务 UI 包维护：业务包被集成并执行自身 admin registrar 后，小组件自动注册到首页组件库。

本包不负责布局拖拽、个人布局保存、登录态读取、权限接口读取和业务数据查询。布局能力由 `@mango/grid-layout` 提供，业务数据由各业务包自己的小组件实现读取。

## 2. 能力清单

| 能力 | 用途 | 入口 |
|------|------|------|
| 小组件定义类型 | 约束业务包注册的小组件元数据 | `MangoGridWidgetDefinition` |
| 运行时上下文 | 向小组件注入用户、租户、菜单、跳转函数 | `MangoWidgetRuntimeContext` |
| 聚合去重 | 合并系统、业务、临时传入的小组件 | `mergeGridWidgets` |
| 基础类型 | 快捷入口、消息中心、日历、工作流小组件 props 类型 | 包根入口 |

## 3. 组件归属

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

## 4. 接入方式

业务包通过自身 admin registrar 返回 `widgets`，`@mango/admin-shell` 执行已集成业务包的 registrar 后，会把这些小组件纳入首页组件库。新应用只需要集成对应业务 UI 包和它的 registrar，不需要在首页逐个登记组件。

```ts
import { mergeGridWidgets } from '@mango/grid-widgets';

const widgets = mergeGridWidgets({
  runtime,
  businessWidgets: registeredWidgets,
});
```

如果某个业务包未被集成，它的 registrar 不会执行，对应小组件不会出现在组件库，也不能被新增使用。若历史布局中保存了已移除业务包的小组件类型，布局渲染会按已有失效组件机制兼容处理。

## 5. 业务包新增小组件

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

## 6. 相关文档

- [@mango/grid-layout README](../grid-layout/README.md)
- [前端组件开发规范](../../../mango-pmo/rules/frontend/03-component-development.md)
- [前端 Monorepo 架构规范](../../../mango-pmo/rules/frontend/06-monorepo-architecture.md)
