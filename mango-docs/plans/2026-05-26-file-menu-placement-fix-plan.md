# 文件菜单位置修复设计说明

## 目标

修复文件菜单在初始化基线中的层级：文件能力应作为 `平台能力` 下的 `文件管理`，不再作为顶级 `文件中心`。

## 范围

- 修正 `authorization` 模块初始化基线中的文件菜单、按钮权限、套餐授权和管理员角色授权。
- 修正 `workflow` 模块初始化基线中重新给普通机构套餐/角色授权 `流程管理` 的问题，避免菜单验收被跨模块迁移污染。
- 修正管理端菜单导航 E2E 对文件菜单层级的预期。
- 保持当前模块独立 migration 目录和模块独立 Flyway history table 方案不变。

## 不做什么

- 不调整业务接口和 Mapper 数据源设计。
- 不调整当前本地数据库中的非菜单业务数据。
- 不改动无关模块的迁移文件和页面实现。

## 设计输入

- 用户要求：`文件中心` 改为 `文件管理` 并放到 `平台能力` 下面。
- 历史定位：`7411374b` 的 `V18__platform_capability_menu_restructure.sql` 已将 `file` 菜单改名为 `文件管理` 并移动到 `平台能力(2700)` 下，后续修正时曾误恢复为顶级菜单。
- 当前基线：菜单层级必须直接在 `authorization/V1__init_authorization.sql` 中体现。

## 设计决策

- `authorization` 基线自身必须能从空库初始化出正确菜单，不依赖 `file` 模块后续补正。
- `平台能力` 保持顶级目录 `id=2700`。
- `文件管理` 使用目录 `id=28`，父级为 `2700`，路径 `/file`，模块编码 `mango-file`。
- `文件管理`、`存储配置`、`文件配置` 作为 `id=28` 下的文件功能页面，按钮权限统一使用 `file:*`。
- 管理端 E2E 按后端菜单树验证 `平台能力` 下包含 `文件管理`。
- `workflow` 基线只给平台管理员套餐/角色授权 `流程管理`，普通机构只保留 `流程办理` 和业务示例。

## 交付物

- `mango/mango-platform/mango-authorization/mango-authorization-core/src/main/resources/db/migration/authorization/V1__init_authorization.sql`
- `mango/mango-platform/mango-workflow/mango-workflow-core/src/main/resources/db/migration/workflow/V1__init_workflow.sql`
- `mango-ui/apps/mango-admin/e2e/specs/menu-navigation.spec.ts`
- `mango-docs/plans/2026-05-26-file-menu-placement-fix-ledger.md`

## 验收方式

- 临时空库单独执行 `authorization/V1__init_authorization.sql`，查询菜单树确认 `文件管理(id=28)` 位于 `平台能力(id=2700)` 下。
- 查询确认不存在顶级 `文件中心`。
- 查询确认文件菜单按钮权限为 `file:*` 且模块编码为 `mango-file`。
- 组合执行 `authorization` 和 `workflow` 基线后，查询确认普通机构套餐/角色没有 `流程管理` 授权。
- 执行管理端菜单导航 E2E 或记录未执行原因。
- 执行 PMO 台账检查。

## 风险与限制

- 当前工作区包含大量既有未提交改动，本次只验证菜单修复相关路径。
- 若用户浏览器仍显示旧菜单，需要刷新登录态或确认连接的后端数据库。
