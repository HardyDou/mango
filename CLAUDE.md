# Mango - AI Native 开发底座

Java Spring Cloud Alibaba + Vue 3 全栈开发底座，让 AI Agent 高效率、高质量地实现业务需求。

---

## 会话初始化

### 1. 确认当前角色

| 角色 | 职责 | Agent 文件 |
|------|------|-----------|
| PM Agent | 需求沟通、PRD、Sprint 规划 | `@mango-pmo/agents/01-pm-agent.md` |
| Tech Lead Agent | 架构、API、数据库设计 | `@mango-pmo/agents/02-tech-lead-agent.md` |
| Dev Agent | 代码实现、UT、E2E | `@mango-pmo/agents/03-dev-agent.md` |
| QA Agent | 测试用例、E2E、报告 | `@mango-pmo/agents/04-qa-agent.md` |
| PMO Agent | 流程优化、规范制定 | `@mango-pmo/agents/05-pmo-agent.md` |


### 2. 初始化流程

1. 阅读 `@mango-pmo/rules/00-dev-flow.md` 了解项目全局研发流程
2. 通过 Ask User 组件让用户选择当前角色
3. 加载对应角色的 Agent 文件

---

## 项目特性

| 特性 | 说明 |
|------|------|
| SPI + Starter | SPI 机制 + Starter 模式，模块可插拔切换 |
| 模块拆分 | 模块划分为 api/core/starter/starter-remote，通过 -app 依赖 starter（内存）或 starter-remote（RPC）切换部署形态 |
| DAL 层抽象 | Redis/DB 必须通过 ICache/ILocker 接口 |
| TTL 配置化 | 缓存超时禁止硬编码 |
| DDL Flyway | 数据库变更必须 migration 文件 |
| M* 组件 | 前端必须使用封装组件 |

---

## 子项目

| 项目 | 技术栈 | 规范文件 |
|------|--------|----------|
| [mango](./mango/) | Java 17 + Spring Boot 3.x | `@mango/CLAUDE.md` |
| [mango-web](./mango-web/) | Vue 3 + TypeScript | `@mango-web/CLAUDE.md` |
| [mango-pmo](./mango-pmo/) | 流程规范 & Agent 定义 | `@mango-pmo/rules/00-dev-flow.md` |
| [mango-docs](./mango-docs/) | 项目文档 | - |

---

