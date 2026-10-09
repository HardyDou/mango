# RightToolbar

列表页右上角工具条，提供刷新、列设置和表格密度三个图标操作。

## 导入

```ts
import { RightToolbar } from '@mango/common';
```

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `size` | `'large' \| 'default' \| 'small'` | `'default'` | 当前密度，对应下拉中的“大 / 中 / 小”，支持 `v-model:size` |
| `showRefresh` | `boolean` | `true` | 是否显示刷新按钮 |
| `showColumnSetting` | `boolean` | `true` | 是否显示列设置按钮 |
| `showDensity` | `boolean` | `true` | 是否显示密度下拉 |

## Events

| 名称 | 参数 | 说明 |
|------|------|------|
| `refresh` | - | 点击刷新图标时触发 |
| `columnSetting` | - | 点击列设置图标时触发，列设置面板由业务侧实现 |
| `sizeChange` | `'large' \| 'default' \| 'small'` | 在密度下拉中选择“大/中/小”时触发 |
| `update:size` | `'large' \| 'default' \| 'small'` | 密度变化时同步触发，用于 `v-model:size` |

## 使用示例

### 基础用法

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { RightToolbar } from '@mango/common';

type TableSize = 'large' | 'default' | 'small';

const tableSize = ref<TableSize>('default');

function handleRefresh() {
  // 重新加载列表数据
}

function handleColumnSetting() {
  // 打开业务侧的列设置面板
}
</script>

<template>
  <RightToolbar
    :size="tableSize"
    @refresh="handleRefresh"
    @column-setting="handleColumnSetting"
    @size-change="tableSize = $event"
  />
</template>
```

### 使用 v-model:size 双向绑定

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { RightToolbar } from '@mango/common';

const size = ref<'large' | 'default' | 'small'>('default');
</script>

<template>
  <RightToolbar v-model:size="size" @refresh="reloadList" />

  <el-table :size="size">
    <!-- 列定义 -->
  </el-table>
</template>
```

### 只保留刷新按钮

```vue
<template>
  <RightToolbar
    :show-column-setting="false"
    :show-density="false"
    @refresh="reloadList"
  />
</template>
```

## 重要约束

- 组件只负责发出操作事件，不实现刷新逻辑、列设置面板和表格尺寸联动，这些都由宿主页面完成。
- 密度下拉固定为三项“大（large）/ 中（default）/ 小（small）”，当前值匹配的一项会高亮。
- 三个图标均带 Element Plus tooltip，提示文案固定为“刷新”“列设置”“密度”，不支持通过 props 修改。
- 无后端依赖、无插槽、无 Expose、无独立 `types.ts`。

## 概览

列表工具条组件，用一行图标统一承载刷新、列设置入口和表格密度切换。

## 功能清单

刷新事件、列设置事件、密度三档下拉、`v-model:size` 双向绑定、按 props 显隐按钮。

## 接入方式

从 `@mango/common` 导入，放置在列表页右上角，并在事件回调中实现各自逻辑。

## 配置说明

按上方 Props 表控制三个操作的显隐和当前密度值。

## API 与扩展

支持 `refresh`、`columnSetting`、`sizeChange`、`update:size` 事件；无 Expose 方法、无插槽。

## 数据与初始化

无后端依赖和初始化要求。

## 管理入口

无独立管理入口。

## 快速开始

参见基础用法示例，监听 `refresh` 和 `column-setting`，并把密度值绑定到表格 `size`。

## 问题排查

点击图标无反应时确认已监听对应事件；密度切换不生效时确认表格的 `size` 已与组件 `size` 绑定（推荐 `v-model:size`）。

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
