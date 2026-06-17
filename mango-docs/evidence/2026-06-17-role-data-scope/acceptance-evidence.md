# 角色数据权限验收证据

## 1. 验收范围

- 页面：`/#/system/role` 角色管理，行操作“数据权限”弹窗。
- 接口：`GET/POST/DELETE /authorization/data-scopes/roles`，角色列表接口，授权运行时应用接口。
- 权限：A 公司管理员登录后配置角色维度数据权限，不配置个人数据权限。
- 数据：临时角色 `E2E_DATA_SCOPE_<timestamp>`，先给角色分配 `authorization:role:list` 功能权限，再在表格新增行的授权树选择数据资源 `角色列表 / authorization:role:list`，范围 `本人部门`。
- 部署形态：本地单体后端 + mango-admin Vite dev server。

## 2. 执行环境

- 前端地址：`http://127.0.0.1:7911`
- 后端地址：`http://127.0.0.1:18221`
- 数据库或租户：`mango_dev_89995b`，A 公司租户。
- 测试账号：A 公司 `admin`。
- 浏览器：Playwright Chromium。

## 3. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| ROLE-DATA-SCOPE-001 | `/#/system/role`；`/authorization/data-scopes/roles` | 角色页在表格内新增、编辑、回显、删除数据权限配置 | A 公司临时角色；先授权 `authorization:role:list`；表格新增行选择 `角色列表 / authorization:role:list`；`scopeMode=SELF_ORG` | 页面表格出现临时角色；弹窗显示角色名；保存响应成功；角色数据权限 API 回显 `authorization:role:list`、`SELF_ORG`、`status=1`；点击编辑后当前行出现行内控件；删除后弹窗表格移除该行 | 数据权限按钮可打开弹窗；新增数据权限按钮可插入编辑行；数据资源树按菜单层级只展示 list 资源；数据范围分段控件支持“本人部门”；行内保存/取消按钮可见；保存后展示“保存成功”；删除有二次确认并展示“删除成功” | E2E 断言 console error 0；pageerror 0；requestfailed 0；HTTP 4xx/5xx 0 | `mango-docs/evidence/2026-06-17-role-data-scope/role-data-scope-dialog.png`；命令 `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true pnpm -F mango-admin exec playwright test --config playwright.config.ts e2e/specs/role-data-scope.spec.ts --project=chromium --reporter=list` | PASS |
| ROLE-DATA-SCOPE-002 | `mango-workflow-core`；`WorkflowDefinitionServiceImplTest` | 工作流流程定义作为业务接入样例使用数据权限应用器 | 资源编码 `workflow:definition:list`；字段映射 `created_by`、`org_id`、`tenant_id` | 工作流核心测试覆盖流程定义查询接入数据权限应用器；目标 Maven Reactor `BUILD SUCCESS`；`WorkflowDefinitionServiceImplTest` 5 个用例通过 | 后端业务服务验证，无页面 UI 项 | Maven Surefire 结果 `Failures: 0, Errors: 0, Skipped: 0` | 命令 `mvn -f mango/pom.xml -pl mango-platform/mango-authorization/mango-authorization-api,mango-platform/mango-authorization/mango-authorization-core,mango-platform/mango-authorization/mango-authorization-starter,mango-platform/mango-workflow/mango-workflow-core -am test` | PASS |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| `@mango/rbac` | 角色管理 | 使用登录级运行时应用接口加载角色表单上下文 | 数据权限弹窗行内新增、编辑、保存和删除 | 弹窗表格、新增按钮、行内控件、按钮和提示均可见 | `role-data-scope-dialog.png` | PASS |
| `@mango/rbac` | 成员管理 | 加入部门默认勾选主部门 | 分配角色弹窗提示部门类数据权限按主部门生效 | 用户只需维护主部门和角色，不配置个人数据权限 | 代码构建和角色 E2E 覆盖 | PASS |
| `@mango/admin-shell` | 顶栏 | 无 `notice:site:view` 权限时不渲染通知铃铛 | 避免无权限用户请求未读数接口 | 顶栏保持可用，角色页 E2E 无 403 | Playwright E2E console/network 断言 | PASS |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 多浏览器 E2E | 本次按验收地址使用 Chromium 做主验收 | Firefox/WebKit 仍需在浏览器兼容性专项中补充 | 进入发布前回归矩阵 | 无需额外确认 |
