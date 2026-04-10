---
paths:
  - "**/*.vue"
  - "**/*.ts"
---

# Vue 代码规范 (vue-code-rules)

## V1: 组件规范

### V1.1 组件命名

| 类型 | 规范 | 示例 |
|------|------|------|
| 页面组件 | PascalCase | `UserManage.vue` |
| 业务组件 | PascalCase | `UserCard.vue` |
| 布局组件 | PascalCase | `MainLayout.vue` |
| 原子组件 | PascalCase | `BaseButton.vue` |

### V1.2 组件结构

```vue
<template>
  <!-- 模板 -->
</template>

<script setup lang="ts">
// 逻辑
</script>

<style scoped>
/* 样式 */
</style>
```

### V1.3 script setup 规范

```vue
<script setup lang="ts">
// ✅ 按以下顺序组织
import { ref, computed, watch } from 'vue';  // 1. Vue API
import { useUserStore } from '@/stores/user'; // 2. Store
import { fetchUserList } from '@/api/user';  // 3. API
import UserCard from './UserCard.vue';       // 4. 组件

// Props & Emits
const props = defineProps<{ userId: number }>();
const emit = defineEmits<{ (e: 'update', id: number): void }>();

// Ref & Reactive
const loading = ref(false);
const userList = ref<User[]>([]);

// Computed
const activeUsers = computed(() => userList.value.filter(u => u.status === 1));

// Watch
watch(() => props.userId, async (newId) => {
  await loadUser(newId);
});

// Methods
const loadUser = async (id: number) => {
  loading.value = true;
  try {
    userList.value = await fetchUserList(id);
  } finally {
    loading.value = false;
  }
};
</script>
```

---

## V2: TypeScript 规范

### V2.1 类型定义

```typescript
// ✅ 使用 interface 定义对象类型
interface User {
  id: number;
  username: string;
  email?: string;
  roles: Role[];
}

// ✅ 使用 type 定义联合类型/别名
type UserStatus = 'active' | 'inactive' | 'banned';
type ApiResponse<T> = { code: number; data: T; message: string };

// ❌ 禁止使用 any
const data: any = response;  // 禁止
```

### V2.2 API 返回类型

```typescript
// ✅ 统一 API 响应结构
interface ApiResult<T> {
  code: number;
  data: T;
  message: string;
}

interface PageResult<T> {
  list: T[];
  total: number;
  page: number;
  pageSize: number;
}
```

---

## V3: M* 组件使用

### V3.1 强制使用 M* 组件

| 必须使用 | 禁止使用 |
|---------|---------|
| MButton | el-button |
| MInput | el-input |
| MTable | el-table |
| MForm | el-form |
| MDialog | el-dialog |

### V3.2 正确示例

```vue
<template>
  <!-- ✅ 正确 -->
  <MButton type="primary" @click="handleSubmit">提交</MButton>
  <MInput v-model="form.username" placeholder="请输入" />
  <MTable :data="tableData" :columns="columns" />
  <MForm :model="form" :rules="rules" ref="formRef" />

  <!-- ❌ 错误 -->
  <el-button type="primary">提交</el-button>
  <el-input v-model="form.username" />
</template>
```

---

## V4: 样式规范

### V4.1 CSS 变量使用

```vue
<style scoped>
.page-container {
  /* ✅ 使用 CSS 变量 */
  padding: var(--spacing-base);
  background: var(--color-primary);

  /* ❌ 禁止硬编码 */
  /* padding: 16px; */
  /* background: #409EFF; */
}
</style>
```

### V4.2 禁止自定义样式

```vue
<!-- ❌ 禁止 -->
<style>
.el-button {
  background-color: #409EFF !important;
}
</style>
```

---

## V5: Store 规范

### V5.1 Store 命名

| 类型 | 规范 | 示例 |
|------|------|------|
| State | 名词 | `userInfo` |
| Getter | 名词/形容词 | `activeUsers` |
| Action | 动词 | `fetchUserList` |

### V5.2 Store 结构

```typescript
// stores/user.ts
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { fetchUserList, updateUser } from '@/api/user';

export const useUserStore = defineStore('user', () => {
  // State
  const userList = ref<User[]>([]);
  const loading = ref(false);

  // Getters
  const activeUsers = computed(() => userList.value.filter(u => u.status === 1));

  // Actions
  const loadUsers = async () => {
    loading.value = true;
    try {
      userList.value = await fetchUserList();
    } finally {
      loading.value = false;
    }
  };

  return { userList, loading, activeUsers, loadUsers };
});
```
