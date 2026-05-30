# Mango Admin Runtime 产品化 Sprint 0：基准冻结和防线加固

## 1. 目标

冻结原 Mango Admin 基准，建立后续 `@mango/admin` 抽取和能力物料拆分前的硬门禁，防止再次把简化 Shell、静态菜单或仿写页面当成完整复用。

## 2. 范围

- 定义 capability manifest 2.0 类型骨架。
- 扩展包契约检查，校验 v2 字段、菜单、权限、后端能力、运行时和 E2E 清单。
- 扩展业务 starter 模板检查，禁止 starter 声明 Mango 内置菜单。
- 新增原 Mango Admin baseline E2E 脚本，保存截图、布局报告和菜单/样式/接口检查证据。

## 3. 不做什么

- 本 Sprint 不实现 `@mango/admin`。
- 本 Sprint 不拆分 `*-api` 和 `*-admin` 包。
- 本 Sprint 不实现完整依赖解析，只建立类型和检查器骨架。
- 本 Sprint 不用静态菜单、假数据或截图脚本占位冒充完整验收。

## 4. 设计输入

- `mango-docs/plans/2026-05-29-mango-admin-runtime-full-productization-plan.md`
- `mango-docs/plans/2026-05-29-mango-admin-runtime-full-productization-peer-review.md`
- `mango-pmo/rules/frontend/07-material-productization.md`
- `mango-pmo/rules/frontend/06-monorepo-architecture.md`
- `mango-pmo/rules/frontend/04-test.md`

## 5. 设计说明

### 5.1 影响模块

- `mango-ui/packages/admin-pages/src/core.ts`
- `mango-ui/packages/*/src/capability.ts`
- `mango-ui/scripts/check-package-contracts.mjs`
- `mango-ui/scripts/admin-baseline-e2e.mjs`
- `mango-ui/package.json`
- `mango-business-starter/scripts/check-template.mjs`
- `mango-ui/packages/create-mango-app/templates/mango-business-starter/scripts/check-template.mjs`

### 5.2 接口变化

无后端 HTTP 接口变化。前端 capability 类型增加 `requires`、`optional`、`backend`、`menus`、`permissions`、`styles`、`runtime`、`e2e` 字段。

### 5.3 数据变化

无数据库变化。baseline E2E 读取真实后端菜单接口 `/api/authorization/menus/user?fmt=tree`，只用于验收证据。

### 5.4 菜单/页面/权限变化

不改变运行时菜单。新增检查要求：能力包菜单页面必须在 capability `menus` 中声明，页面权限必须进入菜单权限和顶层权限清单；starter 不得声明 Mango 内置菜单。

### 5.5 测试范围

- `package:check` 校验包契约和 capability v2 骨架。
- `check-template` 校验 starter 模板不引用 Mango app 源码、不声明内置菜单、不直接写 JDBC 逻辑。
- `admin:baseline-e2e` 登录原 Mango Admin，采集首页、用户区、设置抽屉截图和布局报告。

## 6. 风险与限制

- baseline E2E 依赖真实后端和本地登录数据；环境不可用时脚本必须失败并留下错误，不允许标记完成。
- 现阶段 capability v2 是类型和检查器骨架，完整依赖解析在 Sprint 3 实现。
- 当前包仍有过渡期混合包；检查器用于防止继续扩散旧模式，不代表拆包目标已完成。

## 7. 验收命令

```bash
cd mango-ui
node --check scripts/admin-baseline-e2e.mjs
node --check scripts/check-package-contracts.mjs
pnpm package:check
pnpm admin:baseline-e2e -- --evidence-dir ../mango-docs/evidence/2026-05-29-admin-baseline

cd ../mango-business-starter
node --check scripts/check-template.mjs
node scripts/check-template.mjs

cd ../mango-ui/packages/create-mango-app/templates/mango-business-starter
node --check scripts/check-template.mjs
node scripts/check-template.mjs
```
