# Mango

Mango 是一套面向业务系统研发的全栈开发底座，覆盖 Java 后端平台能力、Vue 管理端、业务项目脚手架、PMO 规则和面向业务开发者的公开文档入口。

业务开发者优先从 [Mango 文档](./mango-docs/README.md) 和 [能力地图](./mango-docs/capabilities/README.md) 开始；需要接入具体能力时，直接进入对应模块 README。

## 1. 仓库组成

| 路径 | 内容 | 主要读者 |
|------|------|----------|
| `mango/` | Java 后端主仓，包含 app、starter、common、infra、platform、tools | 后端开发者、平台维护者 |
| `mango-ui/` | Vue 3 + pnpm workspace 前端 Monorepo | 前端开发者、后台业务开发者 |
| `mango-business-starter/` | 业务项目模板和 PMO baseline | 业务项目初始化人员 |
| `mango-pmo/` | PMO 规范、流程、模板、审计工具 | PM、Tech Lead、Dev、QA、Agent |
| `mango-docs/` | Pages 文档、能力地图、业务接入手册、设计和交付记录 | 业务开发者、架构和交付人员 |
| `deploy/` | 部署样例和运行参数说明 | 运维、后端开发者 |

## 2. 快速开始

后端检查：

```bash
cd mango
mvn mango:check
```

前端开发：

```bash
cd mango-ui
pnpm install
pnpm dev
```

前端构建和 E2E：

```bash
cd mango-ui
pnpm build

cd apps/mango-admin
npx playwright test
```

## 3. 文档入口

| 目标 | 入口 |
|------|------|
| 面向业务开发者的 Mango 介绍和导航 | [mango-docs](./mango-docs/README.md) |
| 能力模块索引 | [Mango 能力地图](./mango-docs/capabilities/README.md) |
| 业务接入场景 | [业务接入场景手册](./mango-docs/guides/business-integration/README.md) |
| 后端聚合说明 | [mango/README.md](./mango/README.md) |
| 前端 workspace 说明 | [mango-ui/README.md](./mango-ui/README.md) |
| 业务项目模板 | [mango-business-starter](./mango-business-starter/README.md) |
| Job 部署 | [deploy/job](./deploy/job/README.md) |
| PMO 总流程 | [00-dev-flow](./mango-pmo/rules/00-dev-flow.md) |
| 能力说明维护规则 | [08-capability-docs](./mango-pmo/rules/08-capability-docs.md) |

## 4. 接入原则

- 后端能力先读模块 README，确认依赖、配置、数据库、菜单权限和排障入口。
- 前端包先确认集成形态。标记为 Admin Shell 或 Admin Pages 的包主要服务管理后台，不适合直接用于官网或 C 端站点。
- 新增或修改能力时，同步 README、能力地图和必要的业务接入手册。
- 正式交付、验证、发布、提交、PR 或规范治理前，按 [AGENTS.md](./AGENTS.md) 和 PMO preflight 判断需读取的规则。
