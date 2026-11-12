---
'@mango/common': patch
---

`theme/index.scss` 改为显式带 `.scss` 扩展名导入 `light`、`dark`、`element`、`responsive`，使 `package-exports:check` 能解析该入口声明的样式依赖。导出的样式入口、变量、主题类和构建产物不变。
