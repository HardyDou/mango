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

## V3: 组件使用规范

### V3.1 优先使用 Element Plus 原生组件
项目不鼓励对基础组件（如 Button、Input、Tag 等）进行无意义的二次包装。直接使用 `el-button`、`el-input` 等原生组件，保留 TypeScript 类型推断和完整的 API 体验。

### V3.2 高阶组件封装
仅在以下场景才封装高阶组件，并统一命名与导出：
- 内部包含复杂业务逻辑（如：自带远程数据请求的字典下拉框 `DictTag`）。
- 整合了多个基础组件的业务模板（如：自带分页和高级搜索的表格容器）。

### V3.3 组件归属规则

| 场景 | 归属位置 | 说明 |
|------|----------|------|
| 纯页面组件 | `apps/*/src/views` 或 `packages/*/src/views` | 只承载页面布局与交互 |
| 可跨模块复用的业务组件 | `packages/common/components` | 作为唯一实现来源 |
| 仅某业务域内部使用的组件 | 对应 `packages/<domain>/src/components` | 不进入基座或其他包 |
| 基座级初始化组件/指令 | `apps/mango-admin/src` | 例如主题初始化、权限指令 |

要求：
- 已进入 `packages/common/components` 的组件，禁止在 `apps/mango-admin/src/components` 保留第二份实现。
- Demo 页面、业务页面引用公共组件时，统一从 `@mango/common` 导入，不允许再走 `@/components/*`。
- 组件的 `types.ts` 与测试文件必须和组件实现保持同目录，不允许一部分留在基座、一部分下沉到公共包。

### V3.4 公共组件导出规则

- 下沉到 `packages/common/components` 的组件，必须在 `packages/common/index.ts` 中统一导出。
- 除组件默认导出外，对外暴露的 `Props`、`Emits`、`Expose`、实体类型也必须同时导出。
- 如果组件依赖上传、验证码、树形数据等配套 API，这些 API 也应进入 `packages/common/api`，禁止公共组件反向引用基座 `apps/mango-admin/src/api`。

### V3.5 组件设计约束

- 公共组件优先做成受控组件，状态由 `props` 和 `emits` 驱动。
- 公共组件禁止直接依赖基座 store、router、menu 配置、i18n 初始化逻辑。
- 如确需基座参与，改为回调注入或事件通知，例如 `onLocaleChanged`、`sizeChange`、`getCaptchaConfig`。

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

### V4.3 第三方样式处理

- `CodeMirror`、`WangEditor`、`Element Plus` 等第三方样式必须在组件或应用入口显式引入，禁止依赖隐式副作用。
- 公共组件自带的第三方样式应跟随组件实现放在公共包中，避免基座忘记补样式导致 dev 与 build 表现不一致。

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

### V5.3 Store 使用边界

- `apps/mango-admin/src/stores` 只服务于基座应用，不允许被 `packages/common` 直接引用。
- 业务包若需要状态，优先在包内自行维护；跨包共享状态必须通过参数、事件或契约层传递。
- 将文件下沉到公共包或业务包后，必须重新检查所有相对路径和别名路径，禁止留下 `../storage`、`@/stores/*` 这类已经越层的引用。

---

## V6: 路由与动态加载规范

### V6.1 Vite 动态导入规则

- 禁止使用 `import(\`/src/${path}\`)`、`import(path)` 这类完全变量化的绝对路径导入。
- 后端菜单组件路径必须通过“静态映射 + `import.meta.glob`”解析。
- 分包页面优先走 `componentsMap` 一类静态注册表；基座遗留页面允许通过 `import.meta.glob('../views/**/*.vue')` 收集。

### V6.2 路由组件路径规范

- 菜单配置中的组件路径必须使用规范化相对路径，如 `system/user/index`、`views/error/404`。
- 路径解析层必须做统一清洗：去掉 `@/`、前导 `/`、`src/`、`.vue` 后缀，再查找映射。
- 基座错误页、登录页、首页这类宿主页面应显式注册，避免依赖模糊路径匹配。

---

## V7: 开发与验收流程

### V7.1 开发流程

1. 先判断功能归属：基座、公共包还是业务包。
2. 再确定依赖方向：禁止下层包依赖上层应用。
3. 新增公共组件时，同时补导出、类型、测试、样例页。
4. 修改菜单/动态路由时，同时验证 `pnpm dev` 而不只验证 `pnpm build`。

### V7.2 提交流程

- 提交前必须全局搜索新改动是否残留旧路径引用，例如 `@/components/*`、`@/stores/*`、`@/api/*`。
- 若做了组件下沉，必须删除基座中的重复实现，而不是保留两份代码并行维护。
- 对于构建警告，需要区分：
  - 运行时风险警告：必须修复，例如循环 chunk、模块找不到、动态导入不可解析。
  - 体积提示警告：可以分阶段处理，但要记录原因和后续拆分方案。

### V7.3 最低验收标准

- `pnpm run build` 通过。
- `pnpm run dev` 启动后，浏览器控制台无红色模块解析错误。
- 关键 E2E 用例通过，至少覆盖登录页、首页、核心 demo 页面。
- 如修改了等待逻辑，Playwright 断言优先使用“关键元素可见/URL 到达”，避免滥用 `networkidle`。
