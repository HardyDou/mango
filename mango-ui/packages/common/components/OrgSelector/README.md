# OrgSelector

组织选择组件：只读输入框加弹框树，支持单选/多选组织节点并回填名称标签。

## 导入

```ts
import { OrgSelector } from '@mango/common';
import type { OrgSelectorExpose, OrgNode } from '@mango/common';
```

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `modelValue` | `ApiId \| ApiId[]` | `[]` | 选中的组织 ID，支持 `v-model`；单选时为单个 ID，多选时为数组 |
| `multiple` | `boolean` | `false` | 是否多选 |
| `placeholder` | `string` | `'orgSelector.placeholder'` | 输入框占位文本；包含 `.` 时按 i18n key 翻译，否则按原文展示 |
| `title` | `string` | `'orgSelector.title'` | 弹框标题；包含 `.` 时按 i18n key 翻译 |
| `showTagNames` | `boolean` | `true` | 是否在输入框下方以可关闭标签展示已选组织名称 |
| `max` | `number` | `0` | 最多可选数量，`0` 表示不限制 |
| `disabled` | `boolean` | `false` | 是否禁用 |
| `width` | `string \| number` | `'500px'` | 选择弹框宽度 |

## Events

| 名称 | 参数 | 说明 |
|------|------|------|
| `update:modelValue` | `ApiId \| ApiId[] \| undefined` | 确认选择或清空时触发；单选未选为 `undefined`，多选未选为 `[]` |
| `change` | `ApiId \| ApiId[] \| undefined` | 与 `update:modelValue` 同时触发，参数相同 |

## Expose

| 名称 | 参数 | 说明 |
|------|------|------|
| `open()` | - | 打开选择弹框并加载组织树（`disabled` 时无效） |
| `close()` | - | 关闭选择弹框 |
| `getValue()` | 返回 `ApiId[]` | 获取当前选中的组织 ID 数组（单选也返回数组形式） |
| `clear()` | - | 清空选择并触发 `update:modelValue` / `change` |

## 类型定义

```ts
import type { ApiId } from '@mango/api-schema';

export interface OrgNode {
  id: ApiId;
  name: string;
  parentId: ApiId;
  sort?: number;
  children?: OrgNode[];
}

export interface OrgSelectorProps {
  modelValue?: ApiId | ApiId[];
  multiple?: boolean;
  placeholder?: string;
  title?: string;
  showTagNames?: boolean;
  max?: number;
  disabled?: boolean;
  width?: string | number;
}

export interface OrgSelectorEmits {
  (e: 'update:modelValue', value: ApiId | ApiId[] | undefined): void;
  (e: 'change', value: ApiId | ApiId[] | undefined): void;
}

export interface OrgSelectorExpose {
  open(): void;
  close(): void;
  getValue(): ApiId[];
  clear(): void;
}
```

## 使用示例

### 单选组织

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { OrgSelector } from '@mango/common';

const orgId = ref<string>();
</script>

<template>
  <OrgSelector v-model="orgId" />
</template>
```

### 多选并限制数量

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { OrgSelector } from '@mango/common';

const orgIds = ref<string[]>([]);
</script>

<template>
  <OrgSelector
    v-model="orgIds"
    multiple
    :max="5"
    title="选择归属部门"
    placeholder="请选择归属部门"
  />
</template>
```

### 通过 ref 控制弹框与取值

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { OrgSelector, type OrgSelectorExpose } from '@mango/common';

const selectorRef = ref<OrgSelectorExpose | null>(null);

function handleSubmit() {
  const ids = selectorRef.value?.getValue() ?? [];
  console.log('当前选中组织：', ids);
}
</script>

<template>
  <el-button @click="selectorRef?.open()">选择组织</el-button>
  <OrgSelector ref="selectorRef" multiple />
  <el-button type="primary" @click="handleSubmit">提交</el-button>
</template>
```

## 交互与数据加载行为

- 点击只读输入框打开弹框，每次打开都会重新请求组织树；树默认全部展开、显示复选框，父子节点勾选互不关联（`check-strictly`）。
- 单选模式下勾选多个节点时只保留最后一个；多选模式下超出 `max` 时本次勾选会被回滚，点“确定”时若仍超限则截断到 `max` 个。
- 只有点击弹框“确定”才会对外更新值；点击“取消”、关闭弹框或遮罩不会改变已有选择。
- 输入框下方标签展示已选组织名称（依赖已加载过的树数据），点标签上的关闭按钮可移除该项；输入框自带清空按钮，清空后单选发出 `undefined`、多选发出 `[]`。

## 重要约束

- 依赖组织接口 `getOrgTree`：打开弹框时以 `{ parentId: '0' }` 请求 `GET /org/tree`，由后端一次性返回整棵组织树；开发环境设置 `VITE_USE_MOCK=true` 时使用内置 mock 数据。
- 后端节点字段经前端转换：`id`/`pid`(或 `parentId`) 会转为字符串，名称取 `orgName` 或 `name`，树节点需具备 `id`、`name`、`children` 结构。
- 组件使用 vue-i18n（`useI18n`），默认文案 key 为 `orgSelector.placeholder`、`orgSelector.title` 等，宿主需保证对应语言包存在，或直接传入普通文本。
- `modelValue` 在单选时是单个 ID 而非数组；Expose 的 `getValue()` 统一返回数组。
- 加载失败时弹框内显示“加载组织数据失败”，并在控制台打印错误，不会抛到组件外部。

## 概览

组织架构选择器，由只读输入框、名称标签和树形选择弹框组成，支持单选、多选和数量上限。

## 功能清单

树形勾选、单选/多选、最多可选数量、已选名称标签、手动移除、清空、Expose 打开/关闭/取值/清空。

## 接入方式

从 `@mango/common` 导入，在需要绑定组织 ID 的表单字段中以 `v-model` 接入，弹框由组件内部管理。

## 配置说明

按上方 Props 表配置单选/多选、上限、占位文案、弹框标题与宽度；组织数据由接口提供，无需业务侧传入。

## API 与扩展

支持 `update:modelValue`、`change` 事件和 `open()`、`close()`、`getValue()`、`clear()` 方法；无插槽。

## 数据与初始化

无需预先初始化；每次打开弹框请求 `GET /org/tree`（参数 `parentId=0`）加载整棵树，依赖后端组织接口可用。

## 管理入口

组织数据在后端组织管理中维护，组件无独立管理入口。

## 快速开始

参见单选组织示例，`v-model` 绑定组织 ID 后点击输入框即可在弹框中选择。

## 问题排查

弹框提示加载失败时检查 `/org/tree` 接口权限与返回结构；标签不显示名称通常是尚未打开过弹框、树数据未加载；i18n 直接显示 key 时补全 `orgSelector` 语言包或传入普通中文字符串。

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
