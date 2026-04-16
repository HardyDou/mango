# Mango Project Context (Gemini CLI Entry)

为了避免信息重复和维护成本，本项目的核心规范、架构约束以及 AI Agent 工作流均采用统一的中心化管理。
在处理当前工作区的任何任务时，你必须（MUST）主动读取并严格遵守以下文件中的指导：

## 1. 全局研发流程与角色
- **项目顶层约束**：读取根目录的 `CLAUDE.md` 和 `README.md`。
- **Agent 工作流**：读取 `mango-pmo/rules/00-dev-flow.md` 了解全局研发流程，以及 `mango-pmo/agents/` 下对应的角色定义。
- **架构总览**：读取 `mango-docs/mango-architecture-design.md`。

## 2. 子系统专属规范
- **Java 后端 (mango/)**：进入该目录前，必须读取 `mango/CLAUDE.md` 及 `mango/.claude/rules/` 下的详细规范。
- **Vue 前端 (mango-ui/)**：进入该目录前，必须读取 `mango-ui/CLAUDE.md` 和 `mango-ui/README.md`，并遵守 `mango-pmo/rules/frontend/` 的 Monorepo 约束。
