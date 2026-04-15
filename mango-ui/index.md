# Mango UI 文档索引

`mango-ui` 是 Mango 的前端 Monorepo，负责统一承载管理后台宿主应用、共享公共能力和业务模块包。

## 你应该先看什么

### 1. 初次进入前端仓库

- [README.md](./README.md)
  了解目录结构、依赖方向、开发命令和验收标准。

### 2. 需要理解架构边界

- [Monorepo 架构规范](../mango-pmo/rules/frontend/06-monorepo-architecture.md)
  说明 `apps`、`packages` 的职责划分、允许依赖方向、API 隔离方式和迁移禁忌。

- [前端代码规范](../mango-pmo/rules/frontend/01-vue-code.md)
  说明组件归属、样式规则、导入规则和开发约束。

### 3. 需要理解本次迁移背景

- [Sprint 13 计划与验收记录](../mango-docs/plans/2026-04-14-sprint-13-frontend-monorepo-migration.md)
  说明为什么从 `mango-web` 迁到 `mango-ui`，以及当前已验证的范围。

## 目录导航

```text
mango-ui/
├── apps/
│   └── mango-admin/      # 管理后台宿主应用
├── packages/
│   ├── api-schema/       # 跨包协议与共享类型
│   ├── auth/             # 登录与认证模块
│   ├── common/           # 公共组件、hooks、utils、theme、公共 API
│   ├── rbac/             # 用户、角色、菜单、组织等权限模块
│   └── system/           # 字典、日志、租户、路由等系统模块
├── package.json
└── pnpm-workspace.yaml
```

## 常用命令

在 `mango-ui/` 根目录：

```bash
pnpm install
pnpm dev
pnpm build
```

在 `mango-ui/apps/mango-admin/`：

```bash
pnpm test:e2e --project=chromium
```

## 当前状态

- 旧单体目录 `mango-web` 已迁移为 `mango-ui`
- 管理后台宿主位于 `apps/mango-admin`
- 公共能力已下沉到 `packages/common`
- 业务模块已拆分为 `auth`、`rbac`、`system`
- 当前主链路已验证 `dev / build / E2E`
