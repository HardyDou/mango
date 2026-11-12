---
'@mango/common': patch
'@mango/system': patch
'@mango/admin-shell': patch
'@mango/workflow': patch
'@mango/rbac': patch
---

修复前端质量门禁并统一组件契约：`DictDisplay`、`DictSelect` 改用 `defineOptions` 声明组件名并重排 props 定义，字典 API 与工作流参与人选择器按仓库格式化规则重排，补充 `component-contracts.json` 元数据。组件对外 props、事件、导出和运行时行为不变。
