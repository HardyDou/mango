# 文件菜单位置修复交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| FMC-001 | 用户要求 | 找到 `文件中心` 被改成 `文件管理` 且进入 `平台能力` 的原因 | 定位历史迁移 `7411374b`，并按该层级修正授权基线 | 设计说明 | 历史提交和当前 SQL 块可追溯 | DONE | `mango-docs/plans/2026-05-26-file-menu-placement-fix-plan.md` |
| FMC-002 | 用户要求 | `文件管理` 应位于 `平台能力` 下 | 在授权基线中写入 `id=28` 父级为 `2700`，不再作为顶级 `文件中心` | `authorization/V1__init_authorization.sql` | 临时空库 SQL 查询 | DONE | `mango/mango-platform/mango-authorization/mango-authorization-core/src/main/resources/db/migration/authorization/V1__init_authorization.sql` |
| FMC-003 | 用户要求 | 文件子菜单和按钮权限应归属文件模块 | `22/23/24` 归属 `28`，按钮权限使用 `file:*`，模块编码使用 `mango-file` | `authorization/V1__init_authorization.sql` | 临时空库 SQL 查询 | DONE | `mango/mango-platform/mango-authorization/mango-authorization-core/src/main/resources/db/migration/authorization/V1__init_authorization.sql` |
| FMC-004 | 测试要求 | 前端菜单导航测试应反映正确菜单层级 | E2E 断言 `平台能力` 包含 `文件管理`，不存在顶级 `文件中心` | `menu-navigation.spec.ts` | Playwright 菜单导航用例 | DONE | `mango-ui/apps/mango-admin/e2e/specs/menu-navigation.spec.ts` |
| FMC-005 | 测试发现 | 普通机构不应看到 `流程管理` | `workflow` 基线只给平台管理员套餐/角色授权 `流程管理` | `workflow/V1__init_workflow.sql` | 组合回放 authorization/workflow 后查询授权 | DONE | `mango/mango-platform/mango-workflow/mango-workflow-core/src/main/resources/db/migration/workflow/V1__init_workflow.sql` |
| FMC-006 | 规范要求 | 交付前完成台账检查和相关验证 | 使用 PMO 台账检查、临时库 SQL、当前 API 菜单树验证 | 验证命令输出 | 命令执行结果 | DONE | 本文件 |
