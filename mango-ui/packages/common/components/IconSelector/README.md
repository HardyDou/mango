# IconSelector

图标选择弹框：内置一份 Element Plus 图标名称清单，支持按名称搜索并选中图标名称字符串。

## 导入

```ts
import { IconSelector } from '@mango/common';
```

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `modelValue` | `string` | - | 当前选中的图标名称，支持 `v-model` |

## Events

| 名称 | 参数 | 说明 |
|------|------|------|
| `update:modelValue` | `string` | 选中图标名称后触发，同时关闭弹框 |
| `change` | `string` | 与 `update:modelValue` 同时触发，参数为图标名称 |

## Expose

| 名称 | 参数 | 说明 |
|------|------|------|
| `open()` | - | 打开图标选择弹框，并重置搜索关键字 |
| `close()` | - | 关闭图标选择弹框 |

## 使用示例

### 配合按钮和 ref 使用

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { IconSelector } from '@mango/common';

const iconSelectorRef = ref<InstanceType<typeof IconSelector> | null>(null);
const iconName = ref('Setting');
</script>

<template>
  <el-button @click="iconSelectorRef?.open()">选择图标</el-button>

  <el-icon v-if="iconName" :size="20">
    <component :is="iconName" />
  </el-icon>
  <span>当前图标：{{ iconName }}</span>

  <IconSelector ref="iconSelectorRef" v-model="iconName" />
</template>
```

### 在表单中监听选择结果

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { IconSelector } from '@mango/common';

const form = ref({ menuName: '', icon: '' });

function handleIconChange(name: string) {
  form.value.icon = name;
}
</script>

<template>
  <el-form :model="form">
    <el-form-item label="菜单名称">
      <el-input v-model="form.menuName" />
    </el-form-item>
    <el-form-item label="菜单图标">
      <el-button @click="iconRef?.open()">
        {{ form.icon || '点击选择图标' }}
      </el-button>
    </el-form-item>
  </el-form>

  <IconSelector ref="iconRef" v-model="form.icon" @change="handleIconChange" />
</template>
```

## 重要约束

- 组件本身只渲染一个 `el-dialog`（标题固定为“选择图标”、宽度固定 800px），不渲染任何触发元素，必须放置组件实例后通过 ref 调用 `open()` 打开。
- 可选范围是组件内部写死的一份 Element Plus 图标名称清单，不支持通过 props 传入自定义图标或扩展清单。
- 弹框内通过 `<component :is="图标名称" />` 渲染图标，宿主应用需已全局注册 `@element-plus/icons-vue` 的同名组件，否则网格中只显示名称、不显示图形；选中后业务侧渲染该名称时同理。
- 选中值是图标名称字符串（如 `'Setting'`），不是组件对象；点击任意图标后立即发出事件并关闭弹框，无确认步骤。
- 无后端依赖、无插槽、无独立 `types.ts`。

## 概览

纯前端图标选择弹框，内置固定图标清单与关键字过滤，选中后向宿主回传图标名称。

## 功能清单

图标网格展示、名称关键字搜索（不区分大小写）、当前选中高亮、Expose 打开/关闭、选中后自动关闭。

## 接入方式

从 `@mango/common` 导入并放置组件，用按钮调用其 `open()` 方法，用 `v-model` 接收图标名称。

## 配置说明

仅需配置 `modelValue`；弹框标题、宽度和图标清单均为内置固定值。

## API 与扩展

支持 `update:modelValue`、`change` 事件和 `open()`、`close()` 方法；无插槽。

## 数据与初始化

无后端依赖和初始化要求，图标清单随组件打包。

## 管理入口

无独立管理入口。

## 快速开始

参见配合按钮和 ref 使用的示例。

## 问题排查

弹框打不开时确认调用了组件实例的 `open()`；图标位置显示空白时确认 Element Plus 图标已全局注册且名称在内置清单中；每次打开搜索词会被清空，属于默认行为。

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
