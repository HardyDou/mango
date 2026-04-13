---
paths:
  - "**/api/*.ts"
  - "**/services/*.ts"
---

# 前端 API 规范 (api-rules)

## 1. API 模块结构

### 1.1 文件组织

```
src/api/
├── user.ts        # 用户相关 API
├── auth.ts        # 认证相关 API
├── order.ts       # 订单相关 API
└── index.ts      # API 导出入口
```

### 1.2 API 函数定义

```typescript
// src/api/user.ts
import request from '@/utils/request';
import type { ApiResult, PageResult } from '@/types/api';

// ✅ 统一返回类型
export interface UserDTO {
  id: number;
  username: string;
  email?: string;
}

export interface UserQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
}

// 获取用户列表
export const getUserList = (params: UserQuery): Promise<ApiResult<PageResult<UserDTO>>> => {
  return request.get('/users', { params });
};

// 获取用户详情
export const getUserById = (id: number): Promise<ApiResult<UserDTO>> => {
  return request.get(`/users/${id}`);
};

// 创建用户
export const createUser = (data: UserDTO): Promise<ApiResult<void>> => {
  return request.post('/users', data);
};

// 更新用户
export const updateUser = (id: number, data: UserDTO): Promise<ApiResult<void>> => {
  return request.put(`/users/${id}`, data);
};

// 删除用户
export const deleteUser = (id: number): Promise<ApiResult<void>> => {
  return request.delete(`/users/${id}`);
};
```

---

## 2. 请求封装

### 2.1 统一请求配置

```typescript
// src/utils/request.ts
import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';
import { ElMessage } from 'element-plus';

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000,
});

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 添加 Token
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse<ApiResult<any>>) => {
    const { code, data, message } = response.data;

    if (code === 200) {
      return data;
    }

    ElMessage.error(message || '请求失败');
    return Promise.reject(new Error(message));
  },
  (error) => {
    ElMessage.error(error.message || '网络错误');
    return Promise.reject(error);
  }
);

export default service;
```

---

## 3. API 调用规范

### 3.1 在 Composable 中调用

```typescript
// src/composables/useUser.ts
import { ref } from 'vue';
import { getUserList, deleteUser } from '@/api/user';
import type { UserDTO, UserQuery } from '@/api/user';

export const useUser = () => {
  const users = ref<UserDTO[]>([]);
  const loading = ref(false);
  const total = ref(0);

  const fetchUsers = async (params: UserQuery) => {
    loading.value = true;
    try {
      const result = await getUserList(params);
      users.value = result.list;
      total.value = result.total;
    } finally {
      loading.value = false;
    }
  };

  const removeUser = async (id: number) => {
    await deleteUser(id);
    await fetchUsers({});
  };

  return { users, loading, total, fetchUsers, removeUser };
};
```

### 3.2 在组件中调用

```vue
<script setup lang="ts">
import { onMounted } from 'vue';
import { useUser } from '@/composables/useUser';
import { MButton, MTable, MTag } from '@/components';

const { users, loading, total, fetchUsers, removeUser } = useUser();

onMounted(() => {
  fetchUsers({ page: 1, pageSize: 10 });
});

const handleDelete = async (id: number) => {
  await removeUser(id);
};
</script>
```
