---
paths:
  - "**/*.spec.ts"
  - "**/*.test.ts"
---

# 前端测试规范 (test-rules)

## 1. 测试框架

| 类型 | 工具 |
|------|------|
| 单元测试 | Vitest |
| 组件测试 | Vue Test Utils |
| E2E 测试 | Playwright |
| 覆盖率 | c8 |

---

## 2. 单元测试规范

### 2.1 测试文件命名

```
src/
├── utils/
│   ├── format-date.spec.ts    # 工具函数测试
│   └── validate.spec.ts       # 校验函数测试
├── composables/
│   ├── useUser.spec.ts         # Composable 测试
│   └── useAuth.spec.ts         # Composable 测试
└── components/
    └── UserCard.spec.ts        # 组件测试
```

### 2.2 测试结构

```typescript
// src/utils/format-date.spec.ts
import { describe, it, expect } from 'vitest';
import { formatDate, formatDateTime } from './format-date';

describe('formatDate', () => {
  // ✅ 使用 describe/it清晰组织
  it('should format date correctly', () => {
    const date = new Date('2024-01-15');
    expect(formatDate(date)).toBe('2024-01-15');
  });

  it('should handle invalid date', () => {
    expect(formatDate(null)).toBe('-');
  });
});
```

### 2.3 Composable 测试

```typescript
// src/composables/useUser.spec.ts
import { describe, it, expect, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import { useUser } from './useUser';
import { getUserList } from '@/api/user';

vi.mock('@/api/user');

describe('useUser', () => {
  it('should fetch users on mount', async () => {
    const mockUsers = [{ id: 1, username: 'test' }];
    (getUserList as any).mockResolvedValue({ list: mockUsers, total: 1 });

    const { users, fetchUsers } = useUser();
    await fetchUsers();

    expect(users.value).toEqual(mockUsers);
  });
});
```

---

## 3. 组件测试

### 3.1 测试结构

```typescript
// src/components/UserCard.spec.ts
import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import UserCard from './UserCard.vue';

describe('UserCard', () => {
  it('should render user info', () => {
    const wrapper = mount(UserCard, {
      props: { user: { id: 1, username: 'test', email: 'test@example.com' } }
    });

    expect(wrapper.find('.user-card__name').text()).toBe('test');
    expect(wrapper.find('.user-card__email').text()).toBe('test@example.com');
  });

  it('should emit edit event', () => {
    const wrapper = mount(UserCard, {
      props: { user: { id: 1, username: 'test' } }
    });

    wrapper.find('.edit-btn').trigger('click');
    expect(wrapper.emitted('edit')).toBeTruthy();
  });
});
```

---

## 4. E2E 测试

### 4.1 测试文件

```typescript
// e2e/user.spec.ts
import { test, expect } from '@playwright/test';

test.describe('用户管理', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/user/list');
  });

  test('should display user list', async ({ page }) => {
    await expect(page.locator('.user-table')).toBeVisible();
  });

  test('should create user', async ({ page }) => {
    await page.click('.add-btn');
    await page.fill('.user-form input[name="username"]', 'test');
    await page.click('.submit-btn');
    await expect(page.locator('.el-message')).toContainText('创建成功');
  });
});
```

---

## 5. 测试覆盖率

### 5.1 覆盖率阈值

| 指标 | 阈值 |
|------|------|
| 语句覆盖率 | ≥ 70% |
| 分支覆盖率 | ≥ 60% |
| 函数覆盖率 | ≥ 80% |
| 行覆盖率 | ≥ 70% |

### 5.2 运行测试

```bash
# 运行所有测试
npm run test

# 运行覆盖率
npm run test:coverage

# 运行 E2E
npx playwright test
```
