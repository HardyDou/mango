# Issue #918 RBAC 菜单与企微主体修复验收证据

## 1. 验收范围

- 页面：`/#/system/role` 的“分配角色权限”弹窗
- 页面：`/#/system/admin-branding` 的“网站配置”页面
- 接口：`/authorization/roles`、`/authorization/roles/assignable-menus`、`/authorization/roles/menus`
- 公开接口：`GET /system/admin-branding/public`，匿名访问且由标准 `@PublicAccess` / `API_RESOURCE` 注册 `PUBLIC` 合同
- 权限：芒果集团默认管理员维护 tenant 1 角色；停用的租户管理菜单不进入导航或可授权树
- 数据：部分叶子授权菜单树、Notice 企微同步主体、Identity/Authorization `INTERNAL_ORG` 历史数据
- Demo：仅保留 1 个演示租户“芒果集团”，租户下 2 个公司、每公司 2 个部门
- 产品态：唯一租户自动使用且隐藏选择器；租户管理菜单停用；内部租户隔离能力保留
- 部署形态：本地任务 worktree 的 monolith backend 与 mango-admin

## 2. 执行环境

- 前端地址：`http://127.0.0.1:30005`
- 后端地址：`http://127.0.0.1:18005`，Actuator `UP`
- 数据库或租户：`mango_dev_mango_issue_918_rbac_wecom_party_005`；芒果集团 `tenantId=1`
- 测试账号：默认 `admin`，不记录密码或 token
- 浏览器：Playwright Chromium `1.61.1`

## 3. 功能验收记录

| 台账 ID  | 用例 ID | 页面/接口                         | 功能点                       | 测试数据                                                               | 关键断言                                                                             | UI/交互检查                                               | console/network 结果                          | 截图/trace/日志                                                                                                                        | 结论 |
| -------- | ------- | --------------------------------- | ---------------------------- | ---------------------------------------------------------------------- | ------------------------------------------------------------------------------------ | --------------------------------------------------------- | --------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------- | ---- |
| TASK-001 | TC-001  | `/#/system/role` 分配角色权限     | 部分叶子授权精确回显         | 权限管理 / 角色管理 / 查询角色、删除角色                               | 查询角色选中，删除角色未选，两级祖先均半选                                           | 弹窗、树、按钮布局正常；无空白、404 或 loading 卡死       | 浏览器诊断数组为空；拦截请求无 4xx/5xx        | [role-menu-hydration.png](./role-menu-hydration.png)，trace SHA-256 `37df9b5f615020f7b68ced423e6e61ccf53d5c2a94fb79fda57f1b38219388a7` | PASS |
| TASK-001 | TC-001  | `POST /authorization/roles/menus` | 保存必要祖先且不扩大兄弟权限 | checked=`100`，half-checked=`1,10`                                     | 请求集合精确为 `1,10,100`，无 `101`，页面提示“分配成功”                              | 保存按钮使用稳定 `data-action`，弹窗提交后反馈明确        | console error、page error、失败请求均为 0     | 同上截图与 trace                                                                                                                       | PASS |
| TASK-007 | TC-004  | `/#/system/role` 与真实角色 API   | 默认租户角色维护与隐藏菜单拒绝 | 唯一 `E2E_ROLE_*` 临时角色，测试后删除                                 | 新增、编辑、分配、删除成功；停用的租户管理菜单授权返回 `403`                         | 角色列表与弹窗主路径正常，临时数据已清理                  | 页面无认证错误；真实角色 API 无未解释 4xx/5xx | `.runtime/playwright/mango-admin/artifacts/`                                                                                           | PASS |
| TASK-002 | TC-002  | Notice 同步服务入口               | INTERNAL_ORG 主体归一化      | tenant=1、department=10；新建、更新、禁止资料更新、unchanged、重复同步 | Identity create/update `partyId=1`；组织关系仍使用 `10`；已一致数据不产生多余 update | 非 UI 项：由 22 项 Spring 集成测试观察 gateway 与分支行为 | 非浏览器项：Surefire 0 failures / 0 errors    | [test-results.md](./test-results.md)                                                                                                   | PASS |
| TASK-003 | TC-003  | MySQL / Flyway                    | 历史主体修复与幂等           | Identity V6、Authorization V2，含重复与空 appCode 样本                 | 错误主体 0、重复组 0、最小 ID 保留；重复执行 no-op                                   | 非 UI 项：只读 SQL 回查                                   | 非网络项：MySQL 命令退出码 0                  | `.runtime/issue-918/migration-verification.txt`                                                                                        | PASS |
| TASK-008 | TC-005  | Demo Resource 声明                | 默认单租户精简演示结构       | System、Org、Identity、Authorization Resource                           | 仅 tenant 1；A/B 两公司；每公司 2 部门；第二租户及成员声明已移除；7 项契约测试通过   | Auth build 通过；内部 tenant 字段与拦截器未删除            | 非网络项：Maven/Vite `BUILD SUCCESS`        | [test-results.md](./test-results.md)                                                                                                   | PASS |
| TASK-009 | TC-005  | 登录、菜单、组织、岗位与日历      | 单租户冷库产品态             | 芒果集团 admin；任务专属冷库                                           | 1 个租户、7 个组织；无租户选择器和租户管理；系统页面完整；日历 730 天               | Chromium 10 个关键用例和 13 个系统页面用例通过             | 真实 18005 API 与 30005 UI 全部成功          | `.runtime/playwright/mango-admin/artifacts/`、[test-results.md](./test-results.md)                                                     | PASS |
| TASK-010 | TC-006 | `/#/system/admin-branding`、`GET /system/admin-branding/public` | 网站配置命名与 PUBLIC 合同 | 芒果集团 admin；匿名 HTTP 请求；System Resource generation 3 | 菜单/API/数据库均显示“网站配置”；页面进入 ready；旧名称不存在；公开接口 access mode=`PUBLIC`、permission 为空，匿名请求 HTTP 200；内部菜单码、路由、类型、权限码和配置键不变 | 左侧菜单、标题、基础信息/品牌资源/页脚/启用状态分区正常，无空白、404 或 loading 卡死 | console error、page error、失败请求均为 0 | `.runtime/issue-918/website-config-page.png`、[test-results.md](./test-results.md) | PASS |

## 4. 回归抽查记录

| 模块          | 页面             | 功能点 1                            | 功能点 2                         | UI 细节                            | 截图/trace                                                                                | 结论 |
| ------------- | ---------------- | ----------------------------------- | -------------------------------- | ---------------------------------- | ----------------------------------------------------------------------------------------- | ---- |
| RBAC          | `/#/system/role` | `ROLE_ADMIN` 唯一定位并打开权限弹窗 | 默认租户角色新增、编辑、授权、删除 | 表格、弹窗、树和操作按钮无明显错位 | [role-menu-hydration.png](./role-menu-hydration.png)，Chromium trace 位于 `.runtime`       | PASS |
| Authorization | 角色菜单 API     | tenant 1 菜单授权成功               | 停用的租户管理菜单被 `403` 拒绝  | 页面显示成功反馈且无认证错误       | [test-results.md](./test-results.md)                                                      | PASS |
| System        | 13 个管理页面    | 菜单与真实接口完整                  | 组织/岗位显示精简 demo 数据       | 页面无 401/403/路由加载失败        | `.runtime/playwright/mango-admin/artifacts/`                                              | PASS |
| Calendar      | `/#/data/calendar` | 中国标准工作日历可见              | 2026 年工作日计算可用             | 日历表格和工具抽屉正常             | `.runtime/playwright/mango-admin/artifacts/`                                              | PASS |
| System        | `/#/system/admin-branding` | “网站配置”菜单与页面标题一致 | 匿名公开配置仍可用于登录页和后台框架初始化 | 表单分区、保存区和 ready 状态正常，旧名称不再出现 | `.runtime/issue-918/website-config-page.png` | PASS |

## 5. 未验证项和风险

| 项目                 | 原因                                                            | 影响                                       | 后续处理                                                   | 用户确认             |
| -------------------- | --------------------------------------------------------------- | ------------------------------------------ | ---------------------------------------------------------- | -------------------- |
| 真实企微网络同步     | 本任务不改变企微外部 API，集成测试按设计使用目录与 gateway 替身 | 不证明具体企微企业配置、凭证或网络可用性   | 发布/部署阶段在目标企业执行只读配置检查与一次受控同步验收  | 不适用，本地交付边界 |
| 独立 RBAC 微前端形态 | 本轮浏览器验收使用 `mango-admin` 单体消费 `@mango/rbac`         | 未单独证明 `mango-admin-rbac-app` 宿主装配 | 平台发布前的微前端回归套件覆盖；包构建与架构边界本轮已通过 | 不适用，本地交付边界 |
| 多租户能力 E2E 自建数据 | 9 个既有隔离 spec 硬编码已移除的 `tenantId=2/company_a`       | 这些用例不在本次默认单租户定向套件中，未宣称全量 E2E 通过 | 后续改为测试自行创建第二租户；不得恢复默认 demo 第二租户或改用 tenant 1 | 不适用，本次最小方案边界 |
| 完整导航并发用例稳定性 | 并发执行时“通知管理图标”断言曾出现一次 flaky，单 worker 重跑通过 | 不影响网站配置定向断言，但不能把首次并发结果表述为稳定全量通过 | 保留单 worker 定向入口；后续独立治理通知图标用例的并发稳定性 | 不适用，本次追加项已定向通过 |

## 6. 业务开发交接输出

| 输出对象                             | 交接内容                                                                                    | 材料路径                                              | 执行入口                                                               | 数据/账号边界                                                          | 失败/例外处理                                                    | 状态 |
| ------------------------------------ | ------------------------------------------------------------------------------------------- | ----------------------------------------------------- | ---------------------------------------------------------------------- | ---------------------------------------------------------------------- | ---------------------------------------------------------------- | ---- |
| `@mango/rbac` 消费者                 | 升级后菜单树只以授权叶子恢复级联状态，保存 checked 与 half-checked 去重集合；API 不变       | `mango-ui/packages/rbac/README.md`、本目录            | `pnpm --dir mango-ui --filter @mango/rbac test` 与定向 Playwright 用例 | 使用调用方现有角色、appCode 与租户边界                                 | 回显或 payload 不符时保留 trace 并阻断升级                       | PASS |
| Notice/Identity/Authorization 消费者 | `INTERNAL_ORG.partyId` 表示租户主体；部门 ID 只用于组织关系；随模块 Flyway 自动归一历史数据 | 四个模块 README、[test-results.md](./test-results.md) | Notice 定向 Maven 测试与任务库 Flyway 回查                             | migration 仅修改数值 tenant 的目标主体；Authorization 按完整唯一键去重 | migration 计数、唯一键或主体断言失败即停止升级并从发布前备份恢复 | PASS |
| Mango 默认 Demo 消费者               | 默认只呈现芒果集团 tenant 1；A/B 是组织公司；唯一租户隐藏选择器和租户管理菜单              | System、Identity、Authorization、Org README 与本目录 | 任务专属冷库启动、SQL 回查与 Chromium 定向套件                         | 内部租户表、API、上下文、拦截器不删除；存量库不自动清理历史租户       | 需要恢复多租户展示时重新声明租户 Resource 和启用菜单                 | PASS |
| `@mango/system` 消费者                | 对外名称使用“网站配置”；匿名读取接口按 `@PublicAccess` 注册 `PUBLIC`，内部 `AdminBranding*` 合同保持兼容 | `mango-ui/packages/system/README.md`、`mango/mango-platform/mango-system/README.md`、本目录 | `@mango/system` build、System 定向 Maven 测试、匿名 HTTP 与 Chromium 定向用例 | 公开读取不需要 token；管理读取/保存仍使用原权限码和租户上下文 | API Resource 不是 `PUBLIC`、匿名请求失败或内部标识变化时阻断升级 | PASS |
