# Mango Admin Runtime 产品化 Sprint 2 设计说明

## 1. 目标

Sprint 2 交付 Mango 内置能力的 API SDK 和 Admin UI 分包边界，让新业务项目优先依赖 `@mango/*-api` 与 `@mango/*-admin`，旧混合包只作为兼容出口。

本阶段必须证明：

- `*-api` 包不包含后台页面、菜单、capability 和 Admin Shell 依赖。
- `*-admin` 包包含 capability、页面、菜单、权限、局部样式入口，并依赖对应 `*-api`。
- `@mango/admin` full preset 使用 Admin 能力包后仍能启动完整 Mango Admin。
- 非管理 UI 可以独立消费 `*-api` 包并完成构建。

## 2. 范围

### 2.1 本阶段范围

- 为 `auth`、`calendar`、`file`、`notice`、`numgen`、`rbac`、`system`、`template`、`workflow` 建立对应 `@mango/*-api` 和 `@mango/*-admin` 包。
- 旧 `@mango/*` 混合包保留兼容出口，不作为新开发推荐入口。
- `@mango/admin-pages` 默认能力来源改为 `@mango/*-admin/capability`。
- `package:check` 扩展 API/Admin 边界检查。
- 增加 API SDK 独立消费 E2E，至少覆盖安装、类型导入和构建。
- full preset E2E 继续通过截图、布局、菜单、颜色和接口数据核验。

### 2.2 非本阶段范围

- 不在 Sprint 2 完成 custom preset 自动依赖补齐，该事项仍属于 Sprint 3。
- 不改变后端菜单和权限接口契约。
- 不删除旧混合包，删除或迁移期限属于 Sprint 7。

## 3. 设计决策

### 3.1 包命名

```text
@mango/system-api      系统 API SDK
@mango/system-admin    系统管理 Admin 能力
@mango/rbac-api        权限 API SDK
@mango/rbac-admin      权限 Admin 能力
...
```

旧包 `@mango/system`、`@mango/rbac` 等继续存在，但只作为兼容入口，不能作为新能力边界的验收依据。

### 3.2 API 包边界

API 包只导出 `src/api`、类型、枚举和纯工具。禁止包含：

- `src/views`
- `src/capability.ts`
- 后台页面组件
- `@mango/admin`、`@mango/admin-shell`、`@mango/admin-pages`
- Element Plus 运行时依赖

### 3.3 Admin 包边界

Admin 包导出：

- `./capability`
- 页面组件和后台组件
- 局部样式入口
- 必要的 API 代理路径，代理到对应 `*-api`，避免页面继续维护第二套请求实现

Admin 包必须依赖对应 API 包。

### 3.4 兼容策略

旧混合包保留原有导出，避免已存在 app 和示例立即失效。新默认集成路径改为 `@mango/*-admin`。

## 4. 测试策略

### 4.1 新特性测试

- `pnpm package:check` 验证所有 `*-api` 和 `*-admin` 包边界。
- `pnpm api-sdk:consumer-e2e` 验证非管理 UI 独立消费 API SDK。
- `pnpm package:build -- --filter @mango/system-api` 等至少覆盖代表性 API/Admin 包构建。
- `pnpm admin:full-preset-e2e` 验证 full preset 不因分包破坏。

### 4.2 回归测试

- `pnpm -F @mango/admin-pages test`
- `pnpm -F @mango/admin test`
- `pnpm package:check`
- `pnpm admin:full-preset-e2e`
- starter/template 检查

## 5. 完成标准

- 新 API/Admin 包存在且 package contract 通过。
- `@mango/admin-pages` 默认能力使用 Admin 包。
- 旧混合包仍可构建，作为兼容出口存在。
- API SDK 独立消费 E2E 通过并保留证据。
- full preset 截图和布局报告通过。
- 未完成的 custom 自动依赖解析仍在台账中明确为后续范围。
