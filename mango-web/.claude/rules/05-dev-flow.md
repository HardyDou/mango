---
paths:
  - "**/*.vue"
  - "**/*.ts"
---

# 前端开发流程 (dev-flow-rules)

## 1. 开发规范

### 1.1 开发命令

```bash
# 安装依赖
npm install

# 开发服务
npm run dev

# 构建
npm run build

# Lint
npm run lint
npm run lint:fix

# 单元测试
npm run test

# E2E 测试
npx playwright test
```

### 1.2 Git 提交规范

```bash
# 提交格式
git commit -m "type(scope): description"

# type 类型
# feat: 新功能
# fix: 修复bug
# docs: 文档
# style: 格式
# refactor: 重构
# test: 测试
# chore: 构建

# 示例
git commit -m "feat(user): add user list page"
git commit -m "fix(auth): fix token refresh issue"
```

---

## 2. 页面开发流程

### 2.1 创建页面步骤

1. **定义类型** - `src/types/xxx.ts`
2. **编写 API** - `src/api/xxx.ts`
3. **创建 Composable** - `src/composables/useXxx.ts`
4. **编写组件** - `src/views/xxx/index.vue`
5. **配置路由** - `src/router/xxx.ts`
6. **编写测试** - `src/views/xxx/index.spec.ts`

### 2.2 目录结构

```
src/views/user/
├── components/           # 页面私有组件
│   ├── UserTable.vue
│   └── UserForm.vue
├── composables/          # 页面私有 hooks
│   └── useUserList.ts
├── types/               # 页面类型定义
│   └── user.ts
├── index.vue            # 页面入口
├── index.spec.ts        # 页面测试
└── router.ts           # 页面路由配置
```

---

## 3. 组件开发流程

### 3.1 创建组件步骤

1. **确定组件类型** - 原子组件 / 业务组件
2. **定义 Props/Emits** - TypeScript 接口
3. **实现组件** - Vue SFC
4. **编写测试** - xxx.spec.ts
5. **导出组件** - `src/components/index.ts`

### 3.2 组件导出

```typescript
// src/components/index.ts
export { default as MButton } from './MButton/index.vue';
export { default as MInput } from './MInput/index.vue';
export { default as MTable } from './MTable/index.vue';
export { default as MForm } from './MForm/index.vue';
export { default as MDialog } from './MDialog/index.vue';
export { default as MDrawer } from './MDrawer/index.vue';
export { default as MTag } from './MTag/index.vue';
export { default as MCard } from './MCard/index.vue';
```

---

## 4. 代码检查

### 4.1 ESLint 配置

```json
{
  "extends": [
    "eslint:recommended",
    "plugin:vue/vue3-recommended",
    "@vue/typescript/recommended"
  ],
  "rules": {
    "vue/no-v-html": "warn",
    "vue/require-default-prop": "off",
    "@typescript-eslint/no-explicit-any": "warn"
  }
}
```

### 4.2 Prettier 配置

```json
{
  "semi": false,
  "singleQuote": true,
  "printWidth": 100,
  "trailingComma": "es5"
}
```

---

## 5. 验收标准

### 5.1 截图验收

| 验收点 | 路径 |
|--------|------|
| 默认状态 | `/golden/页面名-default.png` |
| 加载状态 | `/golden/页面名-loading.png` |
| 空状态 | `/golden/页面名-empty.png` |
| 错误状态 | `/golden/页面名-error.png` |

### 5.2 人类验收清单

```markdown
## [页面名] 验收标准

### 视觉检查
- [ ] 主色调: #409EFF
- [ ] 圆角: 4px
- [ ] 间距: 16px

### 组件使用合规
- [ ] 使用 MButton
- [ ] 使用 MInput
- [ ] 无 <style> 块

### 交互测试
- [ ] 点击按钮显示 loading
- [ ] 提交空表单显示错误
- [ ] 操作成功显示 Toast
```
