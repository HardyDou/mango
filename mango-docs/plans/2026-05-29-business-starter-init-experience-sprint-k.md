# Business Starter 初始化体验产品化 Sprint K

## 1. 背景

Sprint J 已证明 `create-mango-app` 生成项目可以真实 `dev-start`、真实登录、真实 CRUD、真实前端访问，并通过 registry 消费回归。下一步需要把初始化体验的质量门槛前移到模板和 E2E：业务开发默认使用 Mango 持久化基础设施，E2E 必须保留截图和报告，页面布局必须按 Mango 后台规范验收。

## 2. 目标

让业务 starter 初始化后具备可验证的开发基线：后端业务持久化通过 `mango-infra-persistence` 复用 MyBatis-Plus 能力，前端业务页符合 Mango 列表页结构，E2E 证据目录保留截图、布局报告和 summary，便于后续人工复核。

## 3. 范围

- 追加 PMO 后端持久化规范，明确业务模块使用 `mango-infra-persistence-starter` 或 `mango-infra-persistence-web-starter`，禁止业务主路径直接写 JDBC。
- 同步 starter 内置 business PMO baseline 和 `create-mango-app` 模板 baseline。
- 调整 business starter 后端模板，去掉业务模块直接声明 MyBatis-Plus starter 和手写 ID/审计字段，改用 infra-persistence 的基础实体和统一装配。
- 调整 business starter 前端业务列表页，补齐搜索区、功能区、表格区、分页区、错误态和可检查的布局标记。
- 增强 dev-start E2E 和 registry E2E，保留截图、布局 JSON 报告和 summary 索引。

## 4. 不做什么

- 不在本 Sprint 接入真实远程 npm/Nexus 发布。
- 不扩展文件、通知、流程等复杂业务闭环。
- 不声明所有历史页面均已满足 Mango UI 规范。

## 5. 设计说明

### 5.1 影响模块

- `mango-pmo/rules/backend/07-persistence.md`
- `mango-business-starter`
- `mango-ui/packages/create-mango-app/templates/mango-business-starter`
- `mango-ui/scripts/business-starter-dev-start-e2e.mjs`
- `mango-ui/scripts/registry-consumption-e2e.mjs`
- `mango-docs/plans`

### 5.2 接口变化

业务 API 路径不变：

- `POST /{module}/{aggregate}s`
- `GET /{module}/{aggregate}s`
- `GET /{module}/{aggregate}s/detail?id=...`

返回 ID 仍按前端契约使用字符串语义，但数据库主键恢复为 Mango 标准 `BIGINT` 雪花 ID。

### 5.3 数据变化

业务表模板改为：

- `id BIGINT`
- `tenant_id`
- `created_by`
- `created_at`
- `updated_by`
- `updated_at`

实体继承 infra-persistence 的 `TenantEntity`，避免重复声明主键、租户和审计字段。

### 5.4 菜单/页面/权限变化

业务菜单不变。业务列表页补齐 Mango 后台列表页结构，E2E 检查搜索区、功能区、表格区、分页区、业务菜单、真实业务数据和横向溢出。

### 5.5 测试范围

- 新特性测试：模板检查、CLI 检查、脚本语法检查、生成项目 Maven test、真实 dev-start E2E、布局报告、截图证据。
- 回归测试：registry publish/install/build/browser smoke E2E，保留 shell/runtime 布局报告和截图。
- 交付契约：台账全部 `DONE` 或有明确 `EXCEPTION` 后执行 delivery-contract-check。

## 6. 完成标准

- PMO 和 starter baseline 均包含业务持久化规范。
- 模板检查能阻止业务后端模板直接依赖 MyBatis-Plus starter 或直接写 JDBC。
- `create-mango-app` 生成项目通过模板检查、typecheck、build、Maven test。
- dev-start E2E 通过，并产出 `layout-report.json`、截图和 summary。
- registry E2E 通过，并产出 `frontend-smoke-report.json`、截图和 summary。
- 页面布局报告通过 Mango 列表页基础结构检查。

## 7. 风险与限制

- dev-start E2E 仍依赖本机 MySQL、Maven、pnpm 和 Playwright。
- registry E2E 的 mixed 模式只验证发布物料、shell 和微前端路由决策；业务微应用完整挂载由 local/dev-start E2E 验证。
