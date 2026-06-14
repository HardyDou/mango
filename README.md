# Mango

Mango 是一套面向业务系统研发的全栈开发底座，当前工作区包含后端平台、前端 Monorepo、PMO 规范和项目文档。

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

- `cd mango-ui && pnpm dev` 可启动前端研发服务
- `cd mango-ui && pnpm build` 可执行前端 workspace 构建
- `cd mango-ui/apps/mango-admin && npx playwright test` 是管理端 E2E 入口

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
- [Mango 能力地图](./mango-docs/capabilities/README.md)
- [任务管理使用说明](./mango/mango-platform/mango-job/README.md)
- [任务管理部署说明](./deploy/job/README.md)
- [协作入口](./CLAUDE.md)
- [研发总流程](./mango-pmo/rules/00-dev-flow.md)
- [后端模块规范](./mango-pmo/rules/backend/05-module.md)
- [前端代码规范](./mango-pmo/rules/frontend/01-vue-code.md)
- [Monorepo 架构规范](./mango-pmo/rules/frontend/06-monorepo-architecture.md)
- [能力说明维护规范](./mango-pmo/rules/08-capability-docs.md)
- [Sprint 与设计文档](./mango-docs/)
