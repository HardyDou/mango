---
'@mango/common': minor
'@mango/detail': minor
'@mango/file': minor
'@mango/cli': patch
---

新增独立 `@mango/detail` 折叠详情组件包，以及返回栏、侧边抽屉、详情摘要、描述列表、文件列表和文件预览弹框，并增强富文本预览的安全资源解析、受保护图片加载和文件预览事件。`@mango/detail` 复用 `MangoDataTable`，支持可配置展开状态、加载/错误/空状态、工作流插槽和主题化表头，不再要求消费者安装或替换 `@mango/admin`。
