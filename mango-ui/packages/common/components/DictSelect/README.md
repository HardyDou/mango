# DictSelect

基于字典编码自动加载选项的下拉选择组件，封装 Element Plus `el-select`。

## 导入

```ts
import { DictSelect } from '@mango/common';
```

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `modelValue` | `string \| number \| Array<string \| number>` | `undefined` | 选中值，支持 `v-model`；多选时为数组 |
| `dictType` | `string` | - | 字典类型编码（`typeCode`），必填，组件挂载后自动加载对应字典选项 |
| `placeholder` | `string` | `'请选择'` | 占位文本 |
| `clearable` | `boolean` | `true` | 是否可清空 |
| `disabled` | `boolean` | `false` | 是否禁用 |
| `multiple` | `boolean` | `false` | 是否多选 |
| `filterable` | `boolean` | `false` | 是否可搜索 |
| `numberValue` | `boolean` | `false` | 是否将选项值转为 `number` 后再渲染和选中 |
| `showAnyOption` | `boolean` | `false` | 单选时是否在选项顶部插入“不限”选项 |
| `anyOptionLabel` | `string` | `'不限'` | “不限”选项的显示文本 |
| `anyOptionValue` | `string \| number` | `'__ALL__'` | “不限”选项的值 |
| `collapseTags` | `boolean` | `true` | 多选时是否折叠 Tag |
| `collapseTagsTooltip` | `boolean` | `true` | 折叠 Tag 时是否悬浮显示全部 |
| `multipleLimit` | `number` | `0` | 多选最大数量，`0` 表示不限制 |
| `reserveKeyword` | `boolean` | `false` | 多选且可搜索时，是否在选中后保留当前搜索关键词 |

## Events

| 名称 | 参数 | 说明 |
|------|------|------|
| `update:modelValue` | `string \| number \| Array<string \| number> \| undefined` | 选中值变化，支持 `v-model` |
| `change` | `string \| number \| Array<string \| number> \| undefined` | 选中值变化，参数与 `update:modelValue` 一致 |

## 使用示例

### 基础单选

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { DictSelect } from '@mango/common';

const status = ref('');
</script>

<template>
  <DictSelect v-model="status" dict-type="sys_normal_disable" />
</template>
```

### 多选与搜索

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { DictSelect } from '@mango/common';

const roles = ref<Array<string | number>>([]);
</script>

<template>
  <DictSelect
    v-model="roles"
    dict-type="sys_role_type"
    multiple
    filterable
    :multiple-limit="3"
  />
</template>
```

### 使用数字值

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { DictSelect } from '@mango/common';

const level = ref<number>();
</script>

<template>
  <DictSelect v-model="level" dict-type="sys_level" number-value />
</template>
```

### 带“不限”选项的查询条件

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { DictSelect } from '@mango/common';

// 未选择或选中“不限”时，值都会归一化为 undefined
const status = ref<string | number | undefined>(undefined);

function handleSearch() {
  // status 为 undefined 时后端按“不限”处理
}
</script>

<template>
  <DictSelect
    v-model="status"
    dict-type="sys_normal_disable"
    show-any-option
    @change="handleSearch"
  />
</template>
```

## 重要约束

- 依赖 system 服务字典接口：内部通过 `useDict(dictType)` 调用 `listDictOptions`，发起 `GET /system/dict/data/options?typeCode={dictType}` 请求；同一 `dictType` 的选项在前端会话内共享缓存，组件自身不提供刷新缓存的入口。
- 接口返回的选项值统一为字符串；只有显式开启 `numberValue` 时才会转成 `number`，请保证字典值可被 `Number()` 正确转换。
- “不限”选项仅在 `showAnyOption` 且非多选时生效：值为空时下拉框显示 `anyOptionValue`，用户选中“不限”后对外发出的值会被归一化为 `undefined`，不会发出 `'__ALL__'`。
- 组件根节点为 `el-select`，默认最小宽度 180px，未暴露插槽，选项内容不可自定义。

## 概览

字典下拉选择器，传入字典编码即可自动拉取并渲染字典选项，支持单选、多选、搜索和“不限”占位项。

## 功能清单

自动字典加载与缓存、单选/多选、可清空、可搜索、数字值转换、多选数量限制、“不限”选项归一化。

## 接入方式

从 `@mango/common` 导入，在查询表单、编辑表单中以 `dict-type` 指定字典编码，通过 `v-model` 绑定值。

## 配置说明

按上方 Props 表配置字典编码、单选/多选、搜索、清空和“不限”项；选项数据由接口决定，无需业务侧传入。

## API 与扩展

支持 `update:modelValue`、`change` 事件；无 Expose 方法、无插槽。

## 数据与初始化

挂载后自动请求 `GET /system/dict/data/options`（参数 `typeCode`），无需业务侧初始化数据；依赖后端 system 服务字典接口可用。

## 管理入口

字典数据在后端字典管理中维护，组件无独立管理入口。

## 快速开始

参见基础单选示例，传入有效的 `dict-type` 并用 `v-model` 接收选中值即可。

## 问题排查

下拉无选项时先确认 `dictType` 与后端字典 `typeCode` 一致且接口返回数据；值类型不符时检查是否需要开启 `numberValue`；查询条件需要“不限”语义时使用 `show-any-option` 而不是自行传入 `'__ALL__'`。

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
