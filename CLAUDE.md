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
| Monorepo 前端 | `mango-ui` 采用基座 + 业务包 + 公共包结构 |

---

## 子项目

| 项目 | 技术栈 | 规范文件 |
|------|--------|----------|
| [mango](./mango/) | Java 17 + Spring Boot 3.x | `@mango/CLAUDE.md` |
| [mango-ui](./mango-ui/) | Vue 3 + Vite + pnpm workspace | `@mango-ui/CLAUDE.md` |
| [mango-pmo](./mango-pmo/) | 流程规范 & Agent 定义 | `@mango-pmo/rules/00-dev-flow.md` |
| [mango-docs](./mango-docs/) | 项目文档 | - |

---

## 前端协作约束

处理 `mango-ui` 时，先遵守下面几条，再进入具体编码：

1. 阅读 `@mango-ui/README.md` 了解当前包结构、命令和验收方式。
2. 阅读 `@mango-pmo/rules/frontend/01-vue-code.md` 和 `@mango-pmo/rules/frontend/06-monorepo-architecture.md`。
3. 明确依赖方向：`apps/mango-admin -> packages/*`，禁止 `packages/common -> apps/mango-admin`。
4. 高复用业务组件统一从 `@mango/common` 导出，不保留双份实现。
5. 涉及菜单或路由组件加载时，禁止写无法被 Vite 静态分析的完全变量化 `import()`。
6. 交付前至少验证：
   - `cd mango-ui && pnpm dev`
   - `cd mango-ui && pnpm build`
   - `cd mango-ui/apps/mango-admin && npx playwright test`

## 推荐工作顺序

1. 先判断改动属于基座、业务包还是公共包。
2. 再判断是否需要同步公共出口、类型或 API 协议。
3. 代码完成后做 `dev / build / E2E` 三层验收。
4. 如果改动形成了新的约束，回写到 `mango-pmo` 规则文档。
