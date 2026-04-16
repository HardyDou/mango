# Mango Frontend Context

为了避免规范在多个文件中重复定义导致冲突，本目录下的 Gemini CLI 规范采用引用方式管理。

在执行前端开发、重构或调试任务前，你必须读取并严格遵守以下核心文档：

1. **前端顶层约束**：读取当前目录下的 `CLAUDE.md`（定义了边界、组件归属和依赖方向规则）。
2. **项目细节与验收**：读取当前目录下的 `README.md`（定义了命令、构建和 Playwright 测试要求）。
3. **前端架构规则**：读取 `../mango-pmo/rules/frontend/` 目录下的 Monorepo 和代码规范文件。
