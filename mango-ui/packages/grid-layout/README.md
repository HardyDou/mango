# @mango/grid-layout 使用说明

## 1. 概览

`@mango/grid-layout` 是 Mango 前端通用自定义栅格布局组件包，提供 12 栅格查看态和编辑态能力。它适合工作台、看板、配置页等需要用户自行排列小组件的页面。

本包与 `@mango/common`、`@mango/file`、`@mango/calendar` 等前端包平级发布。组件只负责布局展示、拖拽编辑、布局数据转换和当前登录人布局接口封装，不内置具体业务小组件、不读取宿主 store、router、菜单或权限上下文。

## 2. 功能清单

| 能力 | 用途 | 常用入口 |
|------|------|----------|
| 查看态布局 | 按保存后的布局渲染页面卡片 | `MangoGridLayout` |
| 编辑态布局 | 拖拽、排序、调整宽高和删除卡片 | `MangoGridDesigner` |
| 小组件库 | 搜索、分组、点击添加和拖拽添加小组件 | `MangoGridDesigner.widgets` |
| 栅格算法 | 12 栅格碰撞整理、占位、移动和缩放计算 | `useGridEngine` |
| 布局接口 | 查询、保存、恢复当前登录人的页面布局 | `gridLayoutPersonalApi` |
| 数据转换 | 后端 JSON 与前端布局对象互转 | `parseGridLayoutValue`、`stringifyGridLayoutValue` |
| 类型导出 | 业务页面定义小组件和布局数据 | `GridLayoutItem`、`GridWidgetDefinition`、`GridLayoutValue` |
| 样式入口 | 独立消费组件样式 | `@mango/grid-layout/style.css` |

## 3. 接入方式

业务页面安装或声明依赖后，从包入口导入组件、类型和样式：

```ts
import { MangoGridDesigner, MangoGridLayout } from '@mango/grid-layout';
import '@mango/grid-layout/style.css';
import type { GridLayoutItem, GridWidgetDefinition } from '@mango/grid-layout';
```

后台首页已接入本包作为工作台自定义布局能力。其它页面接入时，需要业务侧自行提供：

- 当前页面稳定的 `pageCode`。
- 可用小组件库 `widgets`，权限过滤在传入前完成。
- 默认布局数据，没有个人布局时使用。
- 保存、取消和恢复默认按钮的业务交互。

## 4. 配置说明

组件配置通过 props 传入，不读取宿主全局配置。

| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 | 源码入口 |
|----------|------------|--------|------|----------|----------|
| `MangoGridLayout` | `items` | `[]` | 当前布局项 | 决定查看态渲染哪些卡片 | `src/components/MangoGridLayout.vue` |
| `MangoGridLayout` | `widgets` | `[]` | 可用小组件定义 | 按 `widgetType` 匹配卡片内容 | `src/components/MangoGridLayout.vue` |
| `MangoGridLayout` / `MangoGridDesigner` | `columns` | `12` | 栅格列数 | 决定横向布局边界 | `src/types.ts` |
| `MangoGridLayout` / `MangoGridDesigner` | `rowHeight` | `15` | 单行高度，单位 px | 决定卡片高度换算 | `src/types.ts` |
| `MangoGridLayout` / `MangoGridDesigner` | `gap` | `15` | 横向间距，单位 px | 决定卡片间距和编辑网格间距 | `src/types.ts` |
| `MangoGridLayout` / `MangoGridDesigner` | `minRows` | `30` | 最少展示行数 | 保证空白区域可拖入卡片 | `src/types.ts` |
| `MangoGridDesigner` | `defaultWidth` | `3` | 新增卡片默认宽度 | 从组件库添加时生成默认宽度 | `src/components/MangoGridDesigner.vue` |
| `MangoGridDesigner` | `defaultHeight` | `10` | 新增卡片默认高度行数 | 从组件库添加时生成默认高度 | `src/components/MangoGridDesigner.vue` |

卡片是否展示标题和是否有默认内边距由布局项字段覆盖，也可以由小组件定义提供默认值。具体业务数据、权限和按钮行为由宿主页面负责。查看态遇到已保存但当前 `widgets` 中不存在的组件时不渲染该布局项；编辑态保留失效组件占位并提示用户删除后保存布局。

## 5. API 与扩展

### 组件 API

`MangoGridLayout` 用于查看态渲染，主要 props：

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `items` | 当前布局项 | `[]` |
| `widgets` | 可用小组件定义 | `[]` |
| `columns` | 栅格列数 | `12` |
| `rowHeight` | 行高，单位 px | `15` |
| `gap` | 卡片横向间距，单位 px | `15` |
| `minRows` | 最少展示行数 | `30` |

`MangoGridDesigner` 用于编辑态渲染，主要 props：

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `modelValue` | 当前编辑布局项，支持 `v-model` | `[]` |
| `widgets` | 可用小组件定义 | `[]` |
| `columns` | 栅格列数 | `12` |
| `rowHeight` | 行高，单位 px | `15` |
| `gap` | 卡片横向间距，单位 px | `15` |
| `defaultWidth` | 新增卡片默认宽度 | `3` |
| `defaultHeight` | 新增卡片默认高度行数 | `10` |
| `minRows` | 编辑区域最少行数 | `30` |

事件：

| 事件 | 说明 |
|------|------|
| `update:modelValue` | 布局草稿变化 |
| `change` | 布局草稿变化 |

插槽：

| 插槽 | 说明 |
|------|------|
| `widget` | 自定义小组件渲染，参数为 `{ item, widget }` |

### 布局接口

本包封装当前登录人的布局接口：

| 方法 | 说明 |
|------|------|
| `gridLayoutPersonalApi.getPersonal(pageCode)` | 查询当前登录人在指定页面的个人布局 |
| `gridLayoutPersonalApi.savePersonal({ pageCode, layoutJson })` | 保存当前登录人在指定页面的个人布局 |
| `gridLayoutPersonalApi.resetPersonal(pageCode)` | 删除个人布局，业务页面回到默认布局 |

`pageCode` 由业务页面定义，例如后台首页使用 `admin-home-workbench`。

### 扩展点

业务侧通过 `GridWidgetDefinition` 注册小组件。小组件实现只关注自身页面内容和响应式展示，是否可见、是否可添加、默认业务 props 等由业务页面在传入组件前处理。

## 6. 数据与初始化

前端包不维护数据库 migration、菜单、字典或默认权限数据。布局数据由业务页面传入，保存时序列化为后端 `layoutJson`。

常用布局结构：

```ts
interface GridLayoutItem {
  id: string;
  widgetType: string;
  layout: { x: number; y: number; w: number; h: number };
  title?: string;
  props?: Record<string, unknown>;
  showTitle?: boolean;
  padding?: boolean;
}
```

`showTitle` 和 `padding` 可由布局项覆盖，也可由 `GridWidgetDefinition` 元数据提供默认值。组件库按 `businessDomainName/businessDomainCode/domainName/domainCode/moduleCode` 识别业务域，按 `groupName` 展示可选组名，组件名称来自 `title`；旧的 `category` 仅作为兼容展示字段。

后端持久化由 `mango-grid-layout` 模块负责，详情见 [Grid Layout 后端模块 README](../../../mango/mango-platform/mango-grid-layout/README.md)。

## 7. 管理入口

本包不新增后台菜单、按钮权限码或角色授权数据。需要按角色、菜单或业务权限控制小组件可见性时，业务页面在生成 `widgets` 前完成过滤，再传入 `MangoGridDesigner`。

后台首页工作台的编辑入口由业务页面提供；布局组件只渲染编辑器区域和组件库，不决定入口按钮是否展示。

## 8. 快速开始

基础接入示例：

```vue
<template>
  <MangoGridDesigner
    v-if="editing"
    v-model="items"
    :widgets="widgets"
    :default-width="3"
    :default-height="10"
  />
  <MangoGridLayout
    v-else
    :items="items"
    :widgets="widgets"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { MangoGridDesigner, MangoGridLayout } from '@mango/grid-layout';
import '@mango/grid-layout/style.css';
import type { GridLayoutItem, GridWidgetDefinition } from '@mango/grid-layout';

const widgets: GridWidgetDefinition[] = [];
const items = ref<GridLayoutItem[]>([]);
</script>
```

包构建验证：

```bash
pnpm.cmd -F @mango/grid-layout build
```

后台首页消费场景可继续执行：

```bash
pnpm.cmd -F @mango/admin-shell build
pnpm.cmd admin:styles:check
pnpm.cmd -F @mango/admin build
```

## 9. 返回字段

`gridLayoutPersonalApi.getPersonal(pageCode)` 返回后端个人布局对象，常用字段如下：

| 字段 | 说明 | 是否建议业务入库 |
|------|------|------------------|
| `id` | 个人布局记录 ID | 后端维护 |
| `tenantId` | 当前租户 ID | 后端维护 |
| `userId` | 当前用户 ID | 后端维护 |
| `pageCode` | 页面编码 | 业务页面传入 |
| `layoutJson` | 布局 JSON 字符串 | 后端保存 |
| `createdAt` | 创建时间 | 后端维护 |
| `updatedAt` | 更新时间 | 后端维护 |

前端业务页面通常只需要读取 `layoutJson`，再通过 `parseGridLayoutValue` 转为 `GridLayoutValue`。

## 10. 问题排查

| 问题 | 排查方向 |
|------|----------|
| 卡片内容不显示 | 检查 `items[].widgetType` 是否能在 `widgets` 中匹配到定义；查看态会隐藏失效组件 |
| 编辑布局看到组件已失效 | 当前业务 UI 包未集成或已移除该组件，删除失效组件后保存布局 |
| 编辑器组件库为空 | 检查业务页面是否传入 `widgets`，以及权限过滤后是否还有可用小组件 |
| 保存后刷新仍是默认布局 | 检查 `pageCode` 是否稳定，保存接口是否成功，返回的 `layoutJson` 是否可解析 |
| 样式缺失 | 检查业务入口是否引入 `@mango/grid-layout/style.css` |
| 恢复默认后仍显示旧布局 | 检查 `resetPersonal` 成功后，业务页面是否清空个人布局并回到默认布局 |

## 11. 相关文档

- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [前端组件开发规范](../../../mango-pmo/rules/frontend/03-component-development.md)
- [前端 Monorepo 架构规范](../../../mango-pmo/rules/frontend/06-monorepo-architecture.md)
- [@mango/grid-layout 独立包定位 ADR](../../../mango-docs/designs/mango-grid-layout-package-adr.md)
- [工作台自定义布局设计方案](../../../mango-docs/designs/mango-grid-layout-workbench-design.md)
- [工作台自定义布局交付台账](../../../mango-docs/plans/2026-06-15-grid-layout-workbench-delivery-ledger.md)
- [Grid Layout 后端模块 README](../../../mango/mango-platform/mango-grid-layout/README.md)

## 12. 本次变更影响记录

- 本次 PR 仅收窄编辑态左侧组件库列表宽度；不改变 `@mango/grid-layout` 的公开组件 API、布局数据结构、拖拽算法、个人布局接口、菜单权限、租户隔离、启动方式和校验方式。
