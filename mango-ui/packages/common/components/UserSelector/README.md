# UserSelector

人员选择组件，提供下拉远程选择和三栏弹框选择两种模式，支持单选、多选与数量上限。

## 导入

```ts
import { UserSelector } from '@mango/common';
import type { UserSelectorExpose, UserSelectorOption } from '@mango/common';
```

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `modelValue` | `string \| string[]` | `[]` | 选中人员的值，支持 `v-model`；单选为字符串，多选为字符串数组 |
| `mode` | `'select' \| 'dialog'` | `'select'` | 选择模式：下拉选择或弹框选择 |
| `multiple` | `boolean` | `false` | 是否多选 |
| `placeholder` | `string` | `'请选择人员'` | 占位文本 |
| `title` | `string` | `'选择人员'` | 弹框模式的对话框标题 |
| `disabled` | `boolean` | `false` | 是否禁用 |
| `width` | `string \| number` | `'920px'` | 弹框宽度 |
| `max` | `number` | `0` | 最多可选人数，`0` 表示不限制 |

## Events

| 名称 | 参数 | 说明 |
|------|------|------|
| `update:modelValue` | `string \| string[] \| undefined` | 选中值变化；单选为字符串（清空时为 `undefined`），多选为字符串数组 |
| `change` | `string \| string[] \| undefined` | 与 `update:modelValue` 同时触发，参数相同 |

## Expose

| 名称 | 参数 | 说明 |
|------|------|------|
| `open()` | - | 打开选择弹框（仅 `dialog` 模式有可见界面）并加载人员与组织数据；`disabled` 时无效 |
| `close()` | - | 关闭选择弹框 |
| `clear()` | - | 清空选择并触发更新事件 |

## 类型定义

```ts
export interface UserSelectorOption {
  value: string;
  label: string;
  username?: string;
  avatar?: string;
  meta?: string;
}

export interface UserSelectorProps {
  modelValue?: string | string[];
  mode?: 'select' | 'dialog';
  multiple?: boolean;
  placeholder?: string;
  title?: string;
  disabled?: boolean;
  width?: string | number;
  max?: number;
}

export interface UserSelectorEmits {
  (e: 'update:modelValue', value: string | string[] | undefined): void;
  (e: 'change', value: string | string[] | undefined): void;
}

export interface UserSelectorExpose {
  open: () => void;
  close: () => void;
  clear: () => void;
}
```

## 使用示例

### 下拉模式单选

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { UserSelector } from '@mango/common';

const owner = ref('');
</script>

<template>
  <UserSelector v-model="owner" placeholder="选择负责人" />
</template>
```

### 下拉模式多选并限制人数

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { UserSelector } from '@mango/common';

const members = ref<string[]>([]);
</script>

<template>
  <UserSelector v-model="members" multiple :max="10" />
</template>
```

### 弹框模式按组织筛选

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { UserSelector } from '@mango/common';

const userIds = ref<string[]>([]);
</script>

<template>
  <UserSelector
    v-model="userIds"
    mode="dialog"
    multiple
    title="选择审批人"
    width="960px"
  />
</template>
```

### 监听变化并通过 ref 清空

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { UserSelector, type UserSelectorExpose } from '@mango/common';

const selectorRef = ref<UserSelectorExpose | null>(null);
const assignee = ref('');

function handleChange(value: string | string[] | undefined) {
  console.log('当前选中：', value);
}
</script>

<template>
  <UserSelector
    ref="selectorRef"
    v-model="assignee"
    @change="handleChange"
  />
  <el-button @click="selectorRef?.clear()">重置</el-button>
</template>
```

## 两种模式的行为

- `select` 模式：可搜索下拉框，首次聚焦或展开时请求一次人员列表并缓存于组件实例，搜索词在已加载列表上按姓名、用户名、描述做前端过滤；下拉项展示姓名与副标题（`@用户名`，缺省为“用户”）。
- `dialog` 模式：只读输入框显示单人姓名或“已选择 N 人”，下方显示可移除标签；弹框为三栏布局——左侧组织树（含“全部人员”）、中间人员列表加关键字搜索、右侧已选清单，点“确认”后才更新绑定值。
- 超出 `max` 时：下拉模式阻止本次选择并提示“最多选择 N 人”，保持原值；弹框模式在勾选时提示并阻止加入。

## 重要约束

- 依赖后端人员接口：组件通过 `GET /identity/users/page`（参数 `page=1&size=200`）加载人员，读取返回结构中的 `records` 或 `list`，只取前 200 条做本地过滤，没有真正的分页远程搜索。
- 弹框模式额外依赖组织接口 `getOrgTree`：`GET /org/tree?parentId=0`，用于左侧组织树筛选。
- 人员字段映射规则：绑定值取 `username`，缺省时取 `userId`/`id`/`memberId`；显示名取 `nickname`、`memberName`、`username`、ID 的第一个可用值；组织归属取 `primaryOrgId`。后端字段需符合该约定。
- `modelValue` 统一按字符串处理（内部会 `String()` 转换）；已选值不在已加载人员列表中时，标签直接显示值本身。
- 弹框模式点“取消”或关闭弹框不更新值，临时选择只在点“确认”后生效。

## 概览

人员选择器，内置下拉搜索与组织/人员/已选三栏弹框两种交互，适配单人取值和多人取值场景。

## 功能清单

人员列表加载、姓名/用户名搜索、单选/多选、人数上限提示、按组织筛选、已选清单、头像展示、Expose 打开/关闭/清空。

## 接入方式

从 `@mango/common` 导入，默认 `select` 模式直接作为表单项使用；需要按组织筛选时设置 `mode="dialog"`。

## 配置说明

按上方 Props 表配置模式、多选、占位文本、弹框标题与宽度以及人数上限；人员与组织数据均由接口加载。

## API 与扩展

支持 `update:modelValue`、`change` 事件和 `open()`、`close()`、`clear()` 方法；无插槽。

## 数据与初始化

首次交互时自动加载数据：下拉模式请求 `GET /identity/users/page`；弹框模式并行请求人员分页接口与 `GET /org/tree`，无需业务侧初始化。

## 管理入口

人员与组织数据分别在后端身份（identity）用户管理和组织管理中维护，组件无独立管理入口。

## 快速开始

参见下拉模式单选示例，`v-model` 绑定用户名/ID 字符串即可。

## 问题排查

下拉数据为空时检查 `/identity/users/page` 返回的 `records`/`list` 及字段映射；搜索不到更多结果是因为仅在首批 200 条内做前端过滤；弹框组织树为空时检查 `/org/tree` 接口。

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
