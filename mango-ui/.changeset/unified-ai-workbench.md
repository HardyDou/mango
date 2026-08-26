---
'@mango/admin-shell': patch
'@mango/admin': minor
'@mango/ai-api': minor
'@mango/ai': minor
'@mango/common': major
'@mango/http-client': patch
'@mango/cli': minor
---

交付统一 AI 服务工作台、模型与配置管理、会话流式交互、附件和结构化结果，并把 AI 模块接入 Admin 聚合入口与 CLI 模块投影。`@mango/common` 的 Chat 组件现在要求调用方显式提供 `stream` provider；HTTP client 同步修正浏览器流响应在消费结束或取消前的生命周期管理。
