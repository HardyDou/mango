# MangoFormPage

表单页外壳组件。提供返回栏、内容区和底部操作栏。

## 导入

```ts
import { MangoFormPage } from '@mango/common';
```

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `title` | `string` | - | 页面标题 |
| `backText` | `string` | `'返回'` | 返回按钮文案 |
| `dataPage` | `string \| undefined` | `undefined` | 页面标识，用于埋点/定位 |

## Events

| 名称 | 参数 | 说明 |
|------|------|------|
| `back` | - | 点击返回按钮触发 |

## Slots

| 名称 | 说明 |
|------|------|
| `default` | 表单内容区域，通常放置 `el-form` 和 `MangoPageSection` |
| `actions` | 底部操作栏，通常放置保存、取消等按钮 |

## 使用示例

```vue
<script setup lang="ts">
import { MangoFormPage, MangoPageSection } from '@mango/common';

const form = reactive({ name: '' });
const submitting = ref(false);

function handleBack() {
  router.back();
}

async function save() {
  submitting.value = true;
  try {
    // 调用业务 API
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <MangoFormPage title="编辑订单" data-page="orders.edit" @back="handleBack">
    <MangoPageSection title="基本信息">
      <el-form :model="form" label-width="120px">
        <el-form-item label="订单名称">
          <el-input v-model="form.name" />
        </el-form-item>
      </el-form>
    </MangoPageSection>

    <template #actions>
      <el-button @click="handleBack">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="save">保存</el-button>
    </template>
  </MangoFormPage>
</template>
```

## 重要约束

- 组件使用 `<main>` 语义标签，每个页面应只有一个 `MangoFormPage` 实例
- `dataPage` 用于前端埋点和页面定位，建议按 `模块.功能` 格式命名（如 `orders.edit`）
- 底部操作栏使用 `sticky` 定位，始终固定在视口底部
- 组件不处理返回逻辑，`back` 事件由宿主监听后调用 `router.back()` 或自定义逻辑
- 短表单建议使用 `MangoDialog` 而非独立页面
