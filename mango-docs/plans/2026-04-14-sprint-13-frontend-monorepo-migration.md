# Sprint 13: 前端 Monorepo 架构演进 (Frontend Monorepo Migration)

## 1. 任务目的 (Mission)
将现有的单体前端项目 `mango-web` 迁移至基于 **pnpm workspaces** 的 Monorepo 架构（新项目名：`mango-ui`）。
- **解耦业务逻辑**：将 RBAC、组织机构、系统配置等业务模块拆分为独立的 package。
- **对标后端架构**：实现前端版的 API/Core/Remote 隔离，支持开发态（Mock）与部署态（Remote）的无缝切换。
- **提升复用性**：将 M* 基础组件库和通用工具类下沉到基础包中。

## 2. 任务内容 (Tasks)

### 阶段一：规范与环境初始化 (Governance & Init)
1. **PMO 规范制定**：在 `mango-pmo` 中增加 `06-monorepo-architecture.md`。
2. **初始化 Monorepo 骨架**：创建 `mango-ui` 根目录、`pnpm-workspace.yaml` 及根 `package.json`。

### 阶段二：底层包迁移 (Layer 0 & 1 Migration)
1. **mango-common-ui**：迁移 `mango-web` 中的 `utils`, `types`, `hooks`, `assets` 及 `M*` 组件雏形。
2. **mango-api-schema**：定义全局 API 接口协议和 TypeScript 接口。

### 阶段三：业务模块切割 (Module Decoupling)
1. **mango-auth-ui**：迁移登录页及 Token 管理。
2. **mango-rbac-ui**：迁移用户、角色、菜单管理视图。
3. **mango-system-ui**：迁移字典、日志、租户管理视图。

### 阶段四：基座应用组装 (Host Application)
1. **mango-admin-host**：创建基座应用，负责布局 (Layout)、路由聚合 (Router Registry) 和子包集成。

## 3. 验收内容 (Acceptance Criteria)
- [ ] **目录结构合规**：`mango-ui/packages` 和 `mango-ui/apps` 结构符合规范。
- [ ] **模块依赖正确**：业务包禁止相互引用，只能引用 `common` 包。
- [ ] **全量构建通过**：在根目录执行 `pnpm build` 成功。
- [ ] **运行正常**：主应用启动后，登录、权限管理等核心流程功能与原项目一致。
- [ ] **API 隔离验证**：验证可通过配置切换 Mock 实现与 Axios 实现。
