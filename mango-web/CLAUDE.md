# Mango 前端规范

Vue 3 + TypeScript 前端管理后台。

---

## 核心原则

| 原则 | 说明 |
|------|------|
| M* 组件 | 必须使用 M* 组件，禁止直接用 el-* |
| CSS 变量 | 必须使用 CSS 变量，禁止硬编码 |
| 禁止 style 块 | 除 M* 组件外禁止 `<style>` 块 |

---

## 规范文件

| 规范 | 文件 |
|------|------|
| Vue 代码规范 | `@mango-pmo/rules/frontend/01-vue-code.md` |
| 命名规范 | `@mango-pmo/rules/frontend/02-naming.md` |

---

## 目录结构

```
mango-web/src/
├── components/    # M* 组件库
├── views/        # 页面
├── api/          # 接口
├── stores/       # Pinia 状态
└── layout/       # 布局
```

---

## 常用命令

```bash
npm run dev       # 开发
npm run build     # 构建
npx playwright test  # E2E
```
