---
'@mango/admin': patch
'@mango/admin-pages': patch
'@mango/admin-shell': patch
'@mango/app-runtime': patch
'@mango/calendar': patch
'@mango/cms': patch
'@mango/common': patch
'@mango/detail': patch
'@mango/file': patch
'@mango/grid-layout': patch
'@mango/grid-widgets': patch
'@mango/home': patch
'@mango/job': patch
'@mango/link': patch
'@mango/link-openapi': patch
'@mango/link-page': patch
'@mango/cli': patch
'@mango/notice': patch
'@mango/numgen': patch
'@mango/payment': patch
'@mango/rbac': patch
'@mango/site-shell': patch
'@mango/system': patch
'@mango/template': patch
'@mango/workflow': patch
'@mango/workflow-business-example': patch
---

按仓库 Prettier 与 stylelint 基线清结前端源码格式：统一导入折行、属性顺序、单行长度与模板排版，移除 `.eslintrc.js` 中失效的 `eslint-env` 注释，并同步组件契约与格式化结果。导出的组件、props、事件、API 路径和运行时行为均不变，仅源码排版与静态检查基线发生变化。
