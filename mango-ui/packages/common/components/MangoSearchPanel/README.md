# MangoSearchPanel

列表页搜索面板组件。统一字段栅格、按钮位置、展开收起和查询/重置事件。

## 导入

```ts
import { MangoSearchPanel } from '@mango/common';
```

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `model` | `Record<string, unknown>` | `undefined` | 搜索表单数据对象 |
| `labelWidth` | `string \| number` | `'96px'` | 表单 label 宽度 |
| `labelSuffix` | `string` | `'：'` | 表单 label 后缀 |
| `labelPosition` | `'left' \| 'right' \| 'top'` | `'right'` | 表单 label 位置 |
| `size` | `'large' \| 'default' \| 'small'` | `'default'` | 表单尺寸 |
| `searchText` | `string` | `'查询'` | 查询按钮文案 |
| `resetText` | `string` | `'重置'` | 重置按钮文案 |
| `showReset` | `boolean` | `true` | 是否显示重置按钮 |
| `collapsible` | `boolean` | `false` | 是否启用搜索项展开/收起 |
| `defaultExpanded` | `boolean` | `false` | 初始是否展开全部搜索项 |
| `collapsedRows` | `number` | `2` | 未指定 `collapsedCount` 时，收起态显示几行 |
| `collapsedCount` | `number` | `undefined` | 收起态显示前几个搜索项 |
| `expandText` | `string` | `'展开'` | 展开按钮文案 |
| `collapseText` | `string` | `'收起'` | 收起按钮文案 |
| `columns` | `number \| 'auto'` | `4` | 搜索字段区列数。传入 `auto` 时按字段宽度自适应 |
| `morePlacement` | `'actions' \| 'bottom'` | `'bottom'` | 展开/收起按钮位置 |
| `fieldMinWidth` | `string` | `'280px'` | 自适应列模式下字段最小宽度 |
| `fieldMaxWidth` | `string` | `'320px'` | 自适应列模式下字段最大宽度 |

## Events

| 名称 | 参数 | 说明 |
|------|------|------|
| `search` | - | 点击查询按钮触发 |
| `reset` | - | 点击重置按钮触发 |
| `expandChange` | `expanded: boolean` | 展开状态变化 |

## Slots

| 名称 | 说明 |
|------|------|
| `default` | 搜索字段区域，通常放置 `el-form-item` |
| `actions` | 自定义按钮区域，默认显示查询/重置/展开收起按钮 |

## 折叠规则

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `collapsible` | `false` | 是否启用搜索项展开/收起 |
| `collapsedCount` | 自动按列数和 `collapsedRows` 计算 | 收起态显示前几个搜索项。业务把常用搜索项放在前面 |
| `collapsedRows` | `2` | 未指定 `collapsedCount` 时，收起态显示几行 |
| `columns` | `4` | 搜索字段区列数。传入 `auto` 时按字段宽度自适应；固定列数在窄屏下自动降列 |
| `morePlacement` | `bottom` | 展开/收起按钮位置。`actions` 表示跟随查询、重置按钮；`bottom` 表示放在搜索区底部居中 |
| `fieldMinWidth` | `280px` | 自适应列模式下字段最小宽度 |
| `fieldMaxWidth` | `320px` | 自适应列模式下字段最大宽度 |
| `defaultExpanded` | `false` | 初始是否展开全部搜索项 |
| `expandText` / `collapseText` | `展开` / `收起` | 展开按钮文案 |
| `expandChange` | - | 展开状态变化事件 |

默认操作区顺序为查询、重置、展开或收起，位置固定在搜索面板右下角；传入 `actions` slot 时由业务自行接管按钮区域。

## 使用示例

### 基础用法

```vue
<script setup lang="ts">
import { reactive } from 'vue';
import { MangoSearchPanel } from '@mango/common';

const query = reactive({ keyword: '', status: '' });

function search() {
  // 调用业务 API
}
function reset() {
  query.keyword = '';
  query.status = '';
  search();
}
</script>

<template>
  <MangoSearchPanel :model="query" @search="search" @reset="reset">
    <el-form-item label="关键字">
      <el-input v-model="query.keyword" clearable placeholder="请输入关键字" />
    </el-form-item>
    <el-form-item label="状态">
      <el-input v-model="query.status" clearable placeholder="请选择状态" />
    </el-form-item>
  </MangoSearchPanel>
</template>
```

### 可折叠搜索面板

```vue
<template>
  <MangoSearchPanel
    :model="query"
    collapsible
    :collapsed-count="3"
    @search="search"
    @reset="reset"
  >
    <el-form-item label="关键字">
      <el-input v-model="query.keyword" clearable />
    </el-form-item>
    <el-form-item label="状态">
      <el-input v-model="query.status" clearable />
    </el-form-item>
    <el-form-item label="负责组织">
      <el-input v-model="query.orgName" clearable />
    </el-form-item>
    <el-form-item label="创建时间">
      <el-date-picker v-model="query.createdAt" type="daterange" />
    </el-form-item>
  </MangoSearchPanel>
</template>
```

### 自定义按钮区域

```vue
<template>
  <MangoSearchPanel :model="query" @search="search" @reset="reset">
    <el-form-item label="关键字">
      <el-input v-model="query.keyword" clearable />
    </el-form-item>

    <template #actions>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
      <el-button type="primary" plain>导出</el-button>
    </template>
  </MangoSearchPanel>
</template>
```

### 自定义表单样式

```vue
<template>
  <MangoSearchPanel
    :model="query"
    label-suffix=""
    label-position="left"
    size="small"
    @search="search"
    @reset="reset"
  >
    <el-form-item label="客户名称">
      <el-input v-model="query.customerName" clearable />
    </el-form-item>
  </MangoSearchPanel>
</template>
```

## 响应式

- 在 `max-width: 960px` 时表单变为单列布局
- 在 `max-width: 640px` 时字段区强制单列

## 重要约束

- 组件只负责字段栅格、按钮区和展开收起能力，不自带白底、边框、圆角或阴影
- 字段区默认桌面端一行四列，也可设置 `columns="auto"` 使用字段宽度自适应
- 表单默认使用中等尺寸、label 右对齐并带中文冒号
- 组件不请求接口，查询和重置事件由宿主监听后处理

## 概览

统一管理端列表搜索面板，负责字段栅格、折叠和查询操作布局。

## 功能清单

支持四列宽屏布局、窄屏降列、搜索/重置事件、字段折叠和自定义操作区。

## 接入方式

从 `@mango/common` 导入，作为 `MangoListPage` 的 `search` slot 内容使用。

## 配置说明

通过 `model`、`columns`、`collapsible`、`collapsedCount` 和尺寸/文案 props 配置。

## API 与扩展

支持 `search`、`reset`、`expandChange` 事件及 `default`/`actions` slot。

## 数据与初始化

无数据库、字典或默认数据初始化要求；组件不直接请求接口。

## 管理入口

无独立管理入口，由业务列表页面接入。

## 快速开始

参见上方基础用法和可折叠搜索面板示例。

## 问题排查

确认搜索字段按常用程度排序，并由宿主在事件中重置页码和触发数据查询。

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
