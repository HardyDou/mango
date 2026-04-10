# Mango Web 前端

Vue 3 + Element Plus 管理前端。

## 技术栈

| 组件 | 技术 |
|------|------|
| 框架 | Vue 3 + Composition API |
| UI 库 | Element Plus（封装 M* 组件） |
| 构建 | Vite |
| 语言 | TypeScript |
| 状态 | Pinia |
| 路由 | Vue Router 4 |
| 样式 | Tailwind CSS |
| 测试 | Vitest + Playwright |

## 开发命令

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

## 核心规范

| 序号 | 规范 | 说明 |
|------|------|------|
| 01 | `01-vue-code.md` | Vue 代码规范、M* 组件 |
| 02 | `02-naming.md` | 命名规范 |
| 03 | `03-api.md` | API 调用规范 |
| 04 | `04-test.md` | 测试规范 |
| 05 | `05-dev-flow.md` | 开发流程 |

## M* 组件库

必须使用 M* 组件，禁止直接使用 el-*：

| M* 组件 | el-* | 用途 |
|---------|------|------|
| MButton | el-button | 按钮 |
| MInput | el-input | 输入框 |
| MTable | el-table | 表格 |
| MForm | el-form | 表单 |
| MDialog | el-dialog | 弹窗 |
| MDrawer | el-drawer | 抽屉 |
| MTag | el-tag | 标签 |
| MCard | el-card | 卡片 |

## 目录结构

```
src/
├── api/              # API 接口
├── components/       # 公共组件（M*）
├── composables/     # 组合式函数
├── config/          # 配置
├── hooks/           # 钩子函数
├── layout/          # 布局组件
├── router/          # 路由配置
├── stores/          # Pinia Store
├── theme/           # 主题配置
├── types/           # 类型定义
├── utils/           # 工具函数
└── views/          # 页面组件
```
