# 按钮展示规则字段文案与聚合测试交付契约

## 1. 目标

纠正菜单管理中 `menuCode` 字段的页面文案，使其与按钮展示规则接入口径一致；补充授权聚合测试，确认按钮展示规则会随权限快照返回。

## 2. 范围

- 菜单管理页面中 `menuCode` 字段文案、输入提示和校验提示。
- `RolePermissionAuthorityContributorTest` 中按钮展示规则聚合断言。
- 按钮展示规则接入说明的 PMO 检查引用。

## 3. 不做什么

- 不调整按钮展示规则运行逻辑。
- 不调整登录接口字段结构。
- 不调整数据库结构或迁移脚本。
- 不新增业务页面或示例模块。

## 4. 设计输入

- 用户确认 `menuCode` 是页面和按钮权限标识，`permissions` 是接口标识。
- 既有接入说明：`mango-docs/guides/business-integration/permission-button-display-rule.md`。
- 既有后端能力：`RolePermissionAuthorityContributor` 已聚合 `buttonRules`。

## 5. 设计说明

### 5.1 影响模块

- 前端 RBAC 菜单管理：`mango-ui/packages/rbac/src/views/menu/index.vue`。
- 后端授权 starter 测试：`mango/mango-platform/mango-authorization/mango-authorization-starter/src/test/java/io/mango/authorization/starter/RolePermissionAuthorityContributorTest.java`。

### 5.2 接口变化

无接口变化。

### 5.3 数据变化

无数据库或持久化数据变化。

### 5.4 菜单/页面/权限变化

- 菜单管理新增和编辑弹框中，`menuCode` 展示为“权限标识”。
- 菜单管理中 `permissions` 继续展示为“接口标识”。
- 权限判断口径不变。

### 5.5 测试范围

- 前端执行 RBAC 包 lint。
- 后端执行授权 starter 指定单测。
- 执行 PMO 交付契约检查。

## 6. 风险与限制

- 本次只修正文案和测试，不改变按钮展示规则的运行行为。
- 前端未启动页面做人工 UI 验收，文案改动通过代码检查确认。

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| BDR-PR-001 | 用户要求 | 菜单管理字段口径与按钮展示规则一致 | `menuCode` 展示为权限标识，`permissions` 展示为接口标识 | `mango-ui/packages/rbac/src/views/menu/index.vue` | 代码 diff 与 lint 检查 | DONE | `mango-ui/packages/rbac/src/views/menu/index.vue` |
| BDR-PR-002 | 用户要求 | 后端聚合测试覆盖按钮展示规则 | 在授权快照测试中断言 `buttonRules` 透传 | `RolePermissionAuthorityContributorTest.java` | 指定 Maven 单测 | DONE | `mango/mango-platform/mango-authorization/mango-authorization-starter/src/test/java/io/mango/authorization/starter/RolePermissionAuthorityContributorTest.java` |
| BDR-PR-003 | PMO 要求 | PR 前完成文档交付检查 | 使用本文件作为交付契约和台账，引用既有接入说明 | `mango-docs/plans/2026-06-22-button-permission-label-test-delivery.md` | `delivery-contract-check.mjs --mode verify` | DONE | `mango-docs/plans/2026-06-22-button-permission-label-test-delivery.md` |

## 8. 验收证据记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| BDR-PR-001 | 菜单管理 | `menuCode` 字段文案 | 菜单表单源码 | label、placeholder 和校验提示均为权限标识口径 | 未启动页面，代码检查确认 | 不涉及 | lint 输出 | DONE |
| BDR-PR-002 | 授权聚合 | `buttonRules` 返回 | `system:user:edit` 表格按钮规则 | 快照中的 `buttonRules` 与服务返回一致 | 不涉及 | 不涉及 | Maven 单测输出 | DONE |
| BDR-PR-003 | PMO 检查 | 交付台账完整性 | 本文件 | 台账列完整且全部 DONE | 不涉及 | 不涉及 | 文档检查输出 | DONE |
