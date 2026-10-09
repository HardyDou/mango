# TreeSelect

树形下拉选择组件，封装 Element Plus `el-tree-select`，由调用方提供树数据。

## 导入

```ts
import { TreeSelect } from '@mango/common';
```

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `modelValue` | `string \| number \| null` | `null` | 选中节点的值，支持 `v-model` |
| `data` | `any[]` | `[]` | 树形数据，必填，由业务侧提供 |
| `placeholder` | `string` | `'请选择'` | 占位文本 |
| `clearable` | `boolean` | `true` | 是否可清空 |
| `checkStrictly` | `boolean` | `false` | 父子节点是否不互相关联 |
| `showCheckbox` | `boolean` | `false` | 是否显示复选框 |
| `expandOnClickNode` | `boolean` | `true` | 点击节点展开/收起时是否触发选中逻辑 |
| `renderAfterExpand` | `boolean` | `true` | 是否在节点展开后才渲染其内容 |
| `defaultExpandAll` | `boolean` | `false` | 是否默认展开全部节点 |
| `nodeKey` | `string` | `'id'` | 节点唯一标识字段 |
| `filterNodeMethod` | `(value: string, data: any) => boolean` | - | 节点过滤函数，透传给 `el-tree-select` |
| `props` | `TreeNodeProps`（Element Plus） | - | 树字段映射，会与默认映射 `{ label: 'name', children: 'children' }` 合并，传入字段优先 |

## Events

| 名称 | 参数 | 说明 |
|------|------|------|
| `update:modelValue` | `string \| number \| null` | 选中值变化，支持 `v-model` |
| `change` | `string \| number \| null` | 选中值变化 |
| `node-click` | `data: any` | 节点被点击，参数为节点数据 |
| `check-change` | `data: any, checked: boolean` | 节点勾选状态变化 |

## 使用示例

### 基础用法

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { TreeSelect } from '@mango/common';

const deptId = ref<string | number | null>(null);

const treeData = [
  {
    id: '1',
    name: '总公司',
    children: [
      { id: '1-1', name: '技术部' },
      { id: '1-2', name: '产品部' },
    ],
  },
];
</script>

<template>
  <TreeSelect
    v-model="deptId"
    :data="treeData"
    default-expand-all
  />
</template>
```

### 自定义节点字段

```vue
<script setup lang="ts">
const menuData = [
  {
    key: 100,
    title: '系统管理',
    items: [{ key: 101, title: '用户管理' }],
  },
];
</script>

<template>
  <TreeSelect
    v-model="menuId"
    :data="menuData"
    node-key="key"
    :props="{ label: 'title', children: 'items' }"
  />
</template>
```

### 复选框与勾选事件

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { TreeSelect } from '@mango/common';

const checkedIds = ref<string | number | null>(null);

function handleCheckChange(data: any, checked: boolean) {
  console.log('节点勾选变化：', data, checked);
}
</script>

<template>
  <TreeSelect
    v-model="checkedIds"
    :data="treeData"
    show-checkbox
    check-strictly
    @check-change="handleCheckChange"
  />
</template>
```

## 重要约束

- 组件不发起任何请求，树数据必须由业务侧通过 `data` 传入，默认为空数组。
- 默认数据结构为 `{ id, name, children }`；字段不同必须同时传 `node-key` 和 `:props` 映射，否则无法选中和回显。
- 组件根节点宽度为 100%，未定义插槽，节点内容不可自定义。
- 类型仅引用了 Element Plus 的 `TreeNodeProps` 与 `TreeSelectProps`，组件自身没有独立 `types.ts`。

## 概览

受控树形下拉框，透传常用树选择配置，适合菜单、部门等由业务侧加载的层级数据选择。

## 功能清单

单选/复选、可清空、字段映射、默认展开、父子不关联、节点点击与勾选事件、过滤函数透传。

## 接入方式

从 `@mango/common` 导入，准备好树数据后通过 `:data` 传入，并用 `v-model` 绑定节点值。

## 配置说明

按上方 Props 表配置字段映射、展开方式、复选框和父子关联；所有展示数据来自 `data`。

## API 与扩展

支持 `update:modelValue`、`change`、`node-click`、`check-change` 事件；无 Expose 方法、无插槽。

## 数据与初始化

无后端依赖，数据由宿主页面自行请求后传入，组件不做初始化请求。

## 管理入口

无独立管理入口。

## 快速开始

参见基础用法示例，提供 `:data` 树数据即可使用。

## 问题排查

选中不生效或回显异常时，检查 `node-key` 与数据唯一字段是否一致、`:props` 的 `label`/`children` 映射是否正确；无选项显示时确认 `data` 是否为空。

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
