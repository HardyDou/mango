# FormCreate

配置驱动的动态表单组件，基于 Element Plus 表单控件渲染字段，支持十余种字段类型、规则校验和条件显隐。

## 导入

```ts
import { FormCreate } from '@mango/common';
import type { FormConfig, FormData, FormCreateExpose } from '@mango/common';
```

组件实现直接使用 Element Plus 的 `el-form` 及各表单控件（非 `form-create` 第三方库），随 Common 包的 Element Plus 对等依赖工作。

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `config` | `FormConfig` | - | **必传**。表单配置，包含字段列表与布局参数 |
| `modelValue` | `FormData` | - | 表单数据，支持 `v-model`；初始化时与字段 `defaultValue` 合并 |
| `disabled` | `boolean` | `false` | 全表单禁用开关，透传给 `el-form` 并与字段级 `disabled` 取或 |
| `readonly` | `boolean` | `false` | 只读开关（已声明；当前模板仅读取字段自身的 `readonly`，详见重要约束） |
| `showActions` | `boolean` | `true` | 是否渲染底部提交/重置按钮 |
| `submitText` | `string` | `'提交'` | 提交按钮文案 |
| `resetText` | `string` | `'重置'` | 重置按钮文案 |

`FormData` 类型为 `Record<string, any>`。

## Events

| 名称 | 参数 | 说明 |
|------|------|------|
| `update:modelValue` | `value: FormData` | 任意字段变化时同步最新表单数据 |
| `submit` | `value: FormData` | 点击提交且整体校验通过时触发 |
| `reset` | - | 点击重置按钮并执行 `resetFields()` 后触发 |
| `validate` | `valid: boolean` | 提交校验失败触发 `false`；点击重置后触发 `true` |

## Expose

| 名称 | 参数 | 说明 |
|------|------|------|
| `getValue()` | - | 返回表单数据的浅拷贝 |
| `setValue(value)` | `value: FormData` | 批量赋值，仅写入配置中已存在的 key |
| `reset()` | - | 执行 Element Plus `resetFields()`，不触发事件 |
| `validate()` | - | 手动校验，返回 `Promise<boolean>` |
| `getFieldValue(key)` | `key: string` | 读取指定字段值 |
| `setFieldValue(key, value)` | `key: string, value: any` | 设置指定字段值，key 不存在时忽略 |
| `showField(key)` | `key: string` | 将字段配置的 `show` 置为 `true` |
| `hideField(key)` | `key: string` | 将字段配置的 `show` 置为 `false` |
| `enableField(key)` | `key: string` | 将字段配置的 `disabled` 置为 `false` |
| `disableField(key)` | `key: string` | 将字段配置的 `disabled` 置为 `true` |

## 类型定义

```ts
/** 表单字段类型 */
export type FormFieldType =
  | 'input'
  | 'textarea'
  | 'number'
  | 'password'
  | 'select'
  | 'radio'
  | 'checkbox'
  | 'switch'
  | 'date'
  | 'datetime'
  | 'daterange'
  | 'upload'
  | 'cascader'
  | 'tree-select'
  | 'divider';

/** 字段校验规则（映射为 Element Plus 表单规则） */
export interface FormValidateRule {
  required?: boolean;
  message?: string;
  pattern?: string | RegExp;
  min?: number;
  max?: number;
  validator?: (value: any, callback: (error?: Error) => void) => void;
}

/** 表单字段配置 */
export interface FormField {
  key: string;
  label?: string;
  type: FormFieldType;
  defaultValue?: any;
  placeholder?: string;
  disabled?: boolean;
  readonly?: boolean;
  /** 已声明但当前实现未读取，条件显隐请使用 show */
  visible?: boolean;
  /** false 隐藏；函数形式接收当前表单数据用于条件显隐 */
  show?: boolean | ((values: Record<string, any>) => boolean);
  rules?: FormValidateRule[];
  options?: SelectOption[];
  cascaderOptions?: CascaderOption[];
  treeData?: TreeSelectOption[];
  uploadConfig?: UploadConfig;
  format?: string;
  separator?: string;
  step?: number;
  prefix?: string;
  suffix?: string;
  title?: string;
  props?: Record<string, any>;
}

export interface SelectOption {
  label: string;
  value: any;
  disabled?: boolean;
}

export interface CascaderOption {
  label: string;
  value: any;
  children?: CascaderOption[];
  disabled?: boolean;
}

export interface TreeSelectOption {
  label: string;
  value: any;
  children?: TreeSelectOption[];
  disabled?: boolean;
}

export interface UploadConfig {
  action?: string;
  accept?: string;
  maxSize?: number;
  limit?: number;
  data?: Record<string, any>;
  multiple?: boolean;
}

export interface FormConfig {
  name?: string;
  fields: FormField[];
  labelWidth?: string | number;
  inline?: boolean;
  labelPosition?: 'left' | 'right' | 'top';
  size?: 'large' | 'medium' | 'small';
  showRequiredMark?: boolean;
}

export type FormData = Record<string, any>;
```

## 使用示例

### 基础动态表单

```vue
<script setup lang="ts">
import { reactive, ref } from 'vue';
import { FormCreate } from '@mango/common';
import type { FormConfig, FormData } from '@mango/common';

const config = reactive<FormConfig>({
  labelWidth: '100px',
  fields: [
    { key: 'name', label: '应用名称', type: 'input', placeholder: '请输入名称', rules: [{ required: true }] },
    {
      key: 'type',
      label: '应用类型',
      type: 'select',
      defaultValue: 'web',
      options: [
        { label: 'Web 应用', value: 'web' },
        { label: '移动端', value: 'mobile' },
      ],
    },
    { key: 'remark', label: '备注', type: 'textarea', placeholder: '选填' },
  ],
});

const formData = ref<FormData>({});

function handleSubmit(value: FormData) {
  console.log('提交数据', value);
}
</script>

<template>
  <FormCreate v-model="formData" :config="config" @submit="handleSubmit" />
</template>
```

### 条件显隐与正则校验

```vue
<script setup lang="ts">
import { reactive, ref } from 'vue';
import { FormCreate } from '@mango/common';
import type { FormConfig, FormData } from '@mango/common';

const config = reactive<FormConfig>({
  labelPosition: 'right',
  fields: [
    {
      key: 'enableExpire',
      label: '设置有效期',
      type: 'switch',
      defaultValue: false,
    },
    {
      key: 'expireDate',
      label: '过期日期',
      type: 'date',
      defaultValue: null,
      show: (values) => Boolean(values.enableExpire),
      rules: [{ required: true, message: '请选择过期日期' }],
    },
    {
      key: 'code',
      label: '编码',
      type: 'input',
      rules: [{ pattern: /^[A-Za-z0-9_-]+$/, message: '仅支持字母、数字、下划线和短横线' }],
    },
  ],
});

const formData = ref<FormData>({});
</script>

<template>
  <FormCreate v-model="formData" :config="config" />
</template>
```

### 通过 ref 手动校验与赋值

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { FormCreate } from '@mango/common';
import type { FormConfig, FormCreateExpose, FormData } from '@mango/common';

const formRef = ref<FormCreateExpose>();
const config: FormConfig = {
  inline: true,
  fields: [
    { key: 'keyword', label: '关键词', type: 'input' },
    { key: 'count', label: '数量', type: 'number', step: 5, rules: [{ min: 0, max: 100 }] },
  ],
};

async function handleSearch() {
  const valid = await formRef.value?.validate();
  if (!valid) return;
  console.log(formRef.value?.getValue());
}

function fillDefaults() {
  formRef.value?.setFieldValue('keyword', 'mango');
}
</script>

<template>
  <FormCreate
    ref="formRef"
    :config="config"
    :show-actions="false"
  />
  <el-button @click="fillDefaults">填入默认值</el-button>
  <el-button type="primary" @click="handleSearch">查询</el-button>
</template>
```

## 重要约束

- 组件基于 Element Plus 原生控件渲染，不依赖也未封装 `form-create` 第三方库；`config` 是本组件自定义的 JSON 结构
- 模板实际渲染的类型为：`input`、`textarea`、`number`、`password`、`select`、`radio`、`checkbox`、`switch`、`date`、`datetime`、`daterange`、`cascader`、`tree-select`、`divider`。`FormFieldType` 中声明的 `'upload'` 没有对应渲染分支，配置该类型不会输出任何表单项，`uploadConfig` 当前也不会生效
- 条件显隐读取字段的 `show`（支持布尔值或 `(values) => boolean` 函数，随表单数据实时计算）；类型中声明的 `visible` 字段当前不被读取
- 字段初始值为 `defaultValue ?? null`，再与 `modelValue` 合并；`config` 被深度监听，变化后会按字段定义重建默认数据与校验规则（支撑表单设计器类场景的实时预览）
- 顶层 `readonly` prop 已声明但模板仅透传各字段的 `field.readonly`，需要只读请在字段上配置；`disabled` 同时支持全局与字段级
- `number` 字段的 `min`/`max` 取自该字段 `rules` 中的 `min`/`max`；`date` 默认格式 `YYYY-MM-DD`，`datetime` 默认 `YYYY-MM-DD HH:mm:ss`，`daterange` 分隔符默认「至」
- `labelWidth` 未配置时，`labelPosition` 为 `top` 不设宽度，其余情况默认 `'100px'`；`FormConfig.name`、`showRequiredMark` 已声明但模板未使用
- 每个字段的额外属性通过 `field.props` 透传给对应 Element Plus 控件（如 `clearable`、`multiple` 等）；`setValue` / `setFieldValue` 只写入 `config.fields` 中存在的 key
- 组件无 Slots

## 概览

用一份字段配置生成 Element Plus 表单，覆盖常见录入控件、校验规则、条件显隐和编程式取值赋值。

## 功能清单

支持 14 种已渲染字段类型与分隔线、必填/正则/长度/自定义校验、函数式条件显隐、行内与多种标签布局、提交重置事件和完整的实例方法。

## 接入方式

从 `@mango/common` 导入 `FormCreate`，传入 `FormConfig` 并通过 `v-model` 双向绑定数据；运行环境需已接入 Element Plus。

## 配置说明

通过 `config.fields` 定义字段（类型、默认值、选项、规则、显隐条件），通过 `labelWidth`、`labelPosition`、`inline`、`size` 控制布局，通过 `showActions` 和按钮文案 props 控制操作区。

## API 与扩展

提供 `update:modelValue`、`submit`、`reset`、`validate` 事件和 10 个实例方法；字段级定制使用 `field.props` 透传，当前不提供插槽自定义渲染。

## 数据与初始化

挂载时按 `defaultValue ?? null` 初始化并与 `modelValue` 合并；无独立数据源，选项数据由调用方写入 `config`。

## 管理入口

无独立管理入口，可作为表单设计器或业务查询/编辑页面的渲染内核。

## 快速开始

参见「基础动态表单」，最小配置只需 `config.fields` 并监听 `submit`。

## 问题排查

- 配置了 `upload` 字段但页面空白：当前版本未实现上传渲染分支，请改用其它上传组件组合接入
- 字段不显示：检查 `show` 是否为 `false` 或函数返回了 `false`；`visible` 不参与判断
- 全局只读不生效：在每个字段上配置 `readonly`，或使用全局 `disabled`
- 赋值不生效：`setValue` / `setFieldValue` 仅写入已在 `config.fields` 声明的 key
- 数字框没有范围限制：在该字段 `rules` 中配置 `min`、`max`，并可配置 `step`

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
