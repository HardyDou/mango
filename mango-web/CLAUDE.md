# Mango Web - 前端开发规范

## 技术栈

| 组件 | 技术 |
|------|------|
| 框架 | Vue 3 + Element Plus |
| 构建 | Vite |
| 语言 | TypeScript |
| 状态 | Pinia |
| 路由 | Vue Router 4 |
| 样式 | Tailwind CSS |

---

## ⚠️ 强制规范

### UI 组件规范

**必须使用 M* 组件，禁止直接使用 el-*：**

| M* 组件 | el-* 底层 | 用途 |
|---------|----------|------|
| MButton | el-button | 按钮 |
| MInput | el-input | 输入框 |
| MSelect | el-select | 选择器 |
| MTable | el-table | 表格 |
| MForm | el-form | 表单 |
| MDialog | el-dialog | 弹窗 |
| MDrawer | el-drawer | 抽屉 |
| MTag | el-tag | 标签 |
| MCard | el-card | 卡片 |

### 样式规范

- **必须使用 CSS 变量**，禁止硬编码
- **禁止** `<style>` 块（除 M* 组件）
- **组件命名**：PascalCase（如 `UserCard.vue`）

---

## 规范文件

| 任务 | 规范 |
|------|------|
| UI/组件 | `../mango/rules/09-ui.md` |
| 命名规范 | `../mango/rules/02-naming.md` |
| 测试 | `../mango/rules/08-test.md` |

---

## 目录结构

```
src/
├── api/            # API 接口
├── components/     # 公共组件（M* 组件）
├── composables/    # 组合式函数
├── config/         # 配置文件
├── directive/      # 指令
├── hooks/          # 钩子
├── i18n/           # 国际化
├── layout/         # 布局组件
├── router/         # 路由
├── stores/          # Pinia 状态
├── theme/          # 主题配置
├── types/          # 类型定义
├── utils/          # 工具函数
└── views/          # 页面组件
```

---

## 常用命令

```bash
# 开发
npm run dev

# 构建
npm run build

# Lint
npm run lint

# E2E 测试
npx playwright test
```

---

## 上下文管理

| 条件 | AI 行为 |
|------|---------|
| 超过 60% | 提示用户 |
| 超过 80% | 建议总结 |
| 超过 90% | 强制总结 |
