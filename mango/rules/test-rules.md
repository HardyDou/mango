# 测试规范 (test-rules)

## 1. 测试覆盖率要求

### 1.1 覆盖率标准

| 层级 | 覆盖率要求 |
|------|---------|
| Service 层 | ≥ 80% |
| Controller 层 | ≥ 70% |
| Mapper 层 | ≥ 60% |
| 整体 | ≥ 60% |

### 1.2 检测命令

```bash
mvn mango:check -Drule=test-coverage -Dmin=80
```

---

## 2. 单元测试

### 2.1 测试类命名

```java
// ✅ 正确
class UserServiceTest { }
class OrderControllerTest { }

// ❌ 错误
class TestUserService { }
class UserServiceTests { }
```

### 2.2 测试方法命名

```java
// ✅ 正确 - 方法_场景_预期结果
@Test
void findById_existingId_returnsUser() { }

@Test
void save_nullUser_throwsValidationException() { }

@Test
void calculate_discount_withValidAmount_returnsCorrectResult() { }
```

---

## 3. 边界条件测试

### 3.1 边界条件清单

| 边界条件 | 测试点 |
|---------|-------|
| 空值 | null, "" |
| 最大长度 | 0, 1, 最大值 |
| 特殊字符 | `<`, `>`, `&`, `'`, `"` |
| 边界数值 | Integer.MAX_VALUE, Integer.MIN_VALUE |
| 负数 | -1, -100 |
| 零 | 0 |

### 3.2 示例

```java
@Test
void save_emptyUsername_throwsException() {
    User user = new User();
    user.setUsername("");
    assertThrows(ValidationException.class, () -> userService.save(user));
}

@Test
void calculate_maxValue_plusOne_overflow() {
    int max = Integer.MAX_VALUE;
    assertThrows(ArithmeticException.class, () -> calculator.add(max, 1));
}
```

---

## 4. E2E 测试 (Playwright)

### 4.1 组件级截图测试

```typescript
test('OrderTable renders correctly', async ({ mount }) => {
  const component = await mount(OrderTable, {
    props: { orders: mockOrdersData }
  });
  await expect(component).toHaveScreenshot('OrderTable-default.png');
});

test('OrderTable with empty data shows empty state', async ({ mount }) => {
  const component = await mount(OrderTable, {
    props: { orders: [] }
  });
  await expect(component).toHaveScreenshot('OrderTable-empty.png');
});
```

### 4.2 像素差异阈值

```typescript
// playwright.config.ts
export default defineConfig({
  expect: {
    toHaveScreenshot: {
      maxDiffPixels: 150,  // AI 生成代码容忍度
    }
  }
});
```

---

## 5. 测试数据

### 5.1 测试数据来源

| 来源 | 用途 |
|------|------|
| PRD 字段设计 | 字段长度、必填、校验规则 |
| Mock 数据 | 列表、详情数据 |
| 边界值 | 最大、最小、零值 |

### 5.2 Mock 数据目录

```
src/tests/
├── unit/                 # 单元测试
├── integration/          # 集成测试
└── e2e/
    ├── specs/           # Playwright 测试
    └── __snapshots__/   # 截图目录
        ├── golden/      # 验收标准截图
        └── actual/      # 实际截图
```

---

## 6. 测试报告

### 6.1 人类验收标准格式

```markdown
## [页面名] 验收标准

### 视觉检查点
- [ ] 截图: `/golden/页面名-默认状态.png`

### 交互测试
- [ ] 点击主按钮 → Modal 打开
- [ ] 提交空表单 → 显示错误提示

### 组件使用合规
- [ ] 使用 MButton（不是 el-button）
- [ ] 无 <style> 块（除 M* 组件）
```

---

## 7. 测试命令

### 7.1 Mango CLI 测试命令

```bash
# 运行单元测试
mvn test

# 运行集成测试
mvn verify

# 运行 E2E 测试
npx playwright test

# 检查覆盖率
mvn mango:check -Drule=test-coverage
```
