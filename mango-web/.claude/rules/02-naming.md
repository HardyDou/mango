---
paths:
  - "**/*.vue"
  - "**/*.ts"
  - "**/*.tsx"
---

# 前端命名规范 (naming-rules)

## 1. 文件命名

### 1.1 Vue 组件

| 类型 | 规范 | 示例 |
|------|------|------|
| 页面组件 | PascalCase + View | `UserManageView.vue` |
| 业务组件 | PascalCase | `UserCard.vue` |
| 布局组件 | PascalCase + Layout | `MainLayout.vue` |
| 原子组件 | PascalCase + Base | `BaseButton.vue` |

### 1.2 TypeScript 文件

| 类型 | 规范 | 示例 |
|------|------|------|
| 类型定义 | kebab-case | `user-type.ts` |
| API 模块 | kebab-case | `user-api.ts` |
| 工具函数 | kebab-case | `format-date.ts` |
| 常量 | UPPER_SNAKE | `API_CONSTANTS.ts` |

### 1.3 目录命名

```
src/
├── api/              # API 接口
├── components/       # 公共组件
├── composables/     # 组合式函数
├── config/          # 配置
├── hooks/           # 钩子函数
├── layout/          # 布局
├── router/          # 路由
├── stores/          # Pinia Store
├── theme/           # 主题
├── types/           # 类型定义
├── utils/           # 工具函数
└── views/          # 页面组件
```

---

## 2. 变量命名

### 2.1 命名风格

| 类型 | 风格 | 示例 |
|------|------|------|
| 变量/函数 | camelCase | `userList`, `fetchUserInfo` |
| 常量 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| 类/接口 | PascalCase | `UserInfo`, `ApiResponse` |
| 组件 | PascalCase | `UserCard` |

### 2.2 命名规则

```typescript
// ✅ 好的命名
const userList = ref<User[]>([]);
const isLoading = ref(false);
const hasError = ref(false);
const maxRetryCount = 3;

// ❌ 糟糕的命名
const list = ref([]);
const loading = ref(false);
const flag = ref(false);
const num = 3;
```

---

## 3. API 命名

### 3.1 接口命名

```typescript
// ✅ RESTful 风格
export const getUserList = () => api.get('/users');
export const getUserById = (id: number) => api.get(`/users/${id}`);
export const createUser = (data: UserDTO) => api.post('/users', data);
export const updateUser = (id: number, data: UserDTO) => api.put(`/users/${id}`, data);
export const deleteUser = (id: number) => api.delete(`/users/${id}`);

// ✅ 文件命名
// api/user.ts
// api/auth.ts
// api/order.ts
```

---

## 4. CSS 命名

### 4.1 BEM 规范

```css
/* Block */
.user-card { }

/* Element */
.user-card__header { }
.user-card__body { }
.user-card__footer { }

/* Modifier */
.user-card--active { }
.user-card__header--highlighted { }
```

### 4.2 语义化命名

```vue
<template>
  <div class="user-card">
    <div class="user-card__header">用户信息</div>
    <div class="user-card__body">
      <span class="user-card__name">{{ user.name }}</span>
    </div>
  </div>
</template>
```
