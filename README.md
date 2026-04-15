# Mango

Mango 是一套面向业务系统研发的全栈开发底座，当前工作区包含后端平台、前端 Monorepo、流程规范和项目文档。目标不是只把代码组织起来，而是让需求、设计、开发、测试和交付都有统一约束。

## 仓库组成

```text
sprint-13/
├── mango/              # Java 后端主仓
├── mango-ui/           # Vue 3 + pnpm workspace 前端 Monorepo
├── mango-pmo/          # PMO 规范、流程、Agent 角色定义
├── mango-docs/         # 需求、设计、计划文档
└── README.md
```

## 当前前端状态

`mango-ui` 已经替代旧单体前端的研发入口，当前采用：

- `apps/mango-admin` 作为基座应用
- `packages/common` 作为公共能力层
- `packages/auth`、`packages/rbac`、`packages/system` 作为业务包
- `packages/api-schema` 作为跨包协议层

当前基线已经验证：

- `pnpm dev` 可启动
- `pnpm build` 可通过
- `apps/mango-admin` 下 Playwright E2E 可通过

## 研发原则

### 后端

- 延续 `SPI + Starter`、`DAL 抽象`、`Flyway 迁移`、`TTL 配置化` 等约束
- 业务代码不通过条件分支切换部署形态

### 前端

- 采用 Monorepo 分层，不允许公共包反向依赖基座应用
- 高复用业务组件统一收敛到 `@mango/common`
- 路由动态加载必须可被 Vite 静态分析
- 一个改动至少经过 `dev / build / E2E` 三层验证

## 快速开始

### 后端

```bash
cd mango
mvn mango:check
```

### 前端

```bash
cd mango-ui
pnpm install
pnpm dev
```

常用命令：

```bash
cd mango-ui
pnpm build

cd apps/mango-admin
npx playwright test
```

## 文档入口

- [前端索引](./mango-ui/index.md)
- [前端项目说明](./mango-ui/README.md)
- [协作入口](./CLAUDE.md)
- [研发总流程](./mango-pmo/rules/00-dev-flow.md)
- [前端代码规范](./mango-pmo/rules/frontend/01-vue-code.md)
- [Monorepo 架构规范](./mango-pmo/rules/frontend/06-monorepo-architecture.md)
- [Sprint 与设计文档](./mango-docs/)
