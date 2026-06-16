# @mango/grid-layout

`@mango/grid-layout` 提供通用的 12 栅格自定义布局展示与编辑能力，适合工作台、看板、配置页等需要用户自定义组件排列的场景。

## 能力定位

本包是可独立发布的前端组件包，与 `@mango/common`、`@mango/file`、`@mango/calendar` 等包平级。

包内负责：

- 布局查看态。
- 布局编辑态。
- 组件库搜索、点击添加和拖拽添加。
- 已添加卡片拖拽排序、宽度调整、高度调整和删除。
- 碰撞整理和 12 栅格布局算法。
- 当前登录人的布局接口封装。
- 公开类型和 `style.css`。

包内不负责：

- 业务小组件实现。
- 菜单、按钮或数据权限过滤。
- 小组件业务接口请求。
- 登录用户、租户、路由和宿主 store 读取。

## 基础用法

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

## 组件 API

### MangoGridLayout

查看态组件。

| 属性 | 说明 | 默认值 |
| --- | --- | --- |
| `items` | 当前布局项 | `[]` |
| `widgets` | 可用小组件定义 | `[]` |
| `columns` | 栅格列数 | `12` |
| `rowHeight` | 行高，单位 px | `15` |
| `gap` | 卡片横向间距，单位 px；默认也会按 1 行预留 15px 纵向间距 | `15` |
| `minRows` | 最少展示行数 | `30` |

### MangoGridDesigner

编辑态组件。

| 属性 | 说明 | 默认值 |
| --- | --- | --- |
| `modelValue` | 当前编辑布局项，支持 `v-model` | `[]` |
| `widgets` | 可用小组件定义 | `[]` |
| `columns` | 栅格列数 | `12` |
| `rowHeight` | 行高，单位 px | `15` |
| `gap` | 卡片横向间距，单位 px；默认也会按 1 行预留 15px 纵向间距 | `15` |
| `defaultWidth` | 新增卡片默认宽度 | `3` |
| `defaultHeight` | 新增卡片默认高度行数，默认 `10 * 15px` | `10` |
| `minRows` | 编辑区域最少行数 | `30` |

事件：

| 事件 | 说明 |
| --- | --- |
| `update:modelValue` | 布局草稿变化 |
| `change` | 布局草稿变化 |

插槽：

| 插槽 | 说明 |
| --- | --- |
| `widget` | 自定义小组件渲染，参数为 `{ item, widget }` |

## 布局接口

包内封装当前登录用户的布局接口：

- `gridLayoutPersonalApi.getPersonal(pageCode)`
- `gridLayoutPersonalApi.savePersonal({ pageCode, layoutJson })`
- `gridLayoutPersonalApi.resetPersonal(pageCode)`

`pageCode` 由业务页面定义，例如后台首页使用 `admin-home-workbench`。

## 数据结构

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

`showTitle` 和 `padding` 可由布局项覆盖，也可由 `GridWidgetDefinition` 的元数据提供默认值。

`GridWidgetDefinition.category` 可用于编辑器组件库分组展示；未传入时归入“未分组”。

## 验证方式

```bash
pnpm.cmd -F @mango/grid-layout build
```

工作台首个消费场景可通过后台首页验证：

```bash
pnpm.cmd -F @mango/admin-shell build
pnpm.cmd admin:styles:check
pnpm.cmd -F @mango/admin build
```

## 关联文档

- [@mango/grid-layout 独立包定位 ADR](../../../mango-docs/designs/mango-grid-layout-package-adr.md)
- [工作台自定义布局设计方案](../../../mango-docs/designs/mango-grid-layout-workbench-design.md)
- [工作台自定义布局交付台账](../../../mango-docs/plans/2026-06-15-grid-layout-workbench-delivery-ledger.md)
