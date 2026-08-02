# Issue 692 当前账号登录日志标准交付记录

## 1. 元数据

- 任务 ID：Issue 692 - 当前账号登录日志
- 交付模式：STANDARD
- 需求影响：L2 - 新增个人中心登录日志入口，并开放当前用户只读分页接口，涉及登录审计数据访问边界。
- 方案风险：L2 - 复用系统登录日志表和成熟分页模型，但必须由后端强制限定当前租户与当前用户。
- 最终风险：L2
- 工作区决策：REUSE

## 2. 目标与范围

- 目标：在个人中心“安全设置”分组增加“登录日志”，展示当前账号的登录时间、IP 地址、IP 地区和浏览器 UA。
- 成功条件：普通登录用户无需管理员日志权限即可查看自己的分页记录，且不能通过请求参数读取其他用户或其他租户的记录。
- 处理范围：个人中心扩展入口、当前用户登录日志页面、前端 API、系统登录日志 API/Service、API 资源重新同步恢复、数据隔离测试、浏览器验收。
- 不处理范围：管理员登录日志管理页、日志表结构、登录日志采集逻辑、日志清理和统计能力。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| SR-001 | 个人中心侧栏 | 用户已登录并进入个人中心 | “登录日志”显示在“安全设置”分组内 | 未注册 system 前端模块时不提供该扩展入口 | 菜单位于“修改密码”之后，点击后切换到登录日志页 |
| SR-002 | 登录日志页面 | 当前账号存在登录审计记录 | 分页显示登录时间、IP 地址、IP 地区和浏览器 UA | 无数据时显示标准空状态，请求失败由统一请求层反馈 | 页面字段与真实接口返回一致 |
| SR-003 | `GET /system/log/login/my/list` | 请求具有有效登录态 | 只返回当前租户中当前用户 ID 对应记录，并兼容该账号用户名下未绑定用户 ID 的失败记录 | 缺少当前用户或租户上下文时返回系统参数错误 | 集成测试证明跨用户、跨租户记录不可见 |
| SR-004 | API 资源同步 | 自动管理的公开接口资源曾因旧路径或重复路由被禁用 | 再次扫描到同一资源时恢复为启用，并按声明的 `PUBLIC` 模式允许匿名访问 | 未匹配启用资源时仍按 `LOGIN` 拒绝匿名请求；`MANUAL` 资源继续保留人工状态 | H2 集成测试证明自动资源恢复且手工禁用资源不被覆盖 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | SR-001 | 使用 `registerMangoAuthProfileSections` 注册 system 领域页面，归入既有“安全设置”分组 | `mango-ui/packages/auth`、`mango-ui/packages/system` | 删除扩展注册与页面组件 |
| TD-002 | SR-002 | 复用 `sys_login_log` 已有 `login_time/ip/location/browser` 数据，不新增字段或 migration | `@mango/system` log API 与个人页面 | 删除 `currentUserList` 与个人页面 |
| TD-003 | SR-003 | 新接口使用 `LOGIN` 访问模式；用户 ID、租户 ID和用户名全部从 `MangoContextHolder` 获取，不接受客户端指定 | `mango-system-api/core/starter` | 删除 API、Controller 和 Service 方法 |
| TD-004 | SR-004 | API 注册命令代表当前有效扫描结果，合并已有自动资源时显式恢复 `status=1`；资源处理器继续在 `MANUAL`/`INIT_ONLY` 模式下恢复人工状态 | `mango-authorization-core`、`mango-authorization-resource-sync-starter` | 恢复原合并状态策略 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| TASK-001 | TD-003 | 1 | `mango/mango-platform/mango-system/**` | 当前账号接口、服务过滤与访问模式测试完成 |
| TASK-002 | TD-001、TD-002 | 2 | `mango-ui/packages/auth`、`mango-ui/packages/system` | 菜单、页面、分页和字段展示完成 |
| TASK-003 | SR-001 至 SR-003 | 3 | `mango-ui/apps/mango-admin/e2e`、`mango-docs/evidence` | 浏览器回归、真实接口联调与截图证据完成 |
| TASK-004 | TD-004 | 4 | `mango-authorization-core`、`mango-authorization-resource-sync-starter` | 已禁用公开资源重新同步后恢复，手工禁用资源保持不变 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| SR-001 | M09 + M13 | 构建 `@mango/auth`、`@mango/system`；运行定向 Chromium 用例 | PASS | `notice-message-center-menu.spec.ts`、`notice-login-log.png` |
| SR-002 | M13 | Chromium fixture 返回含时间、IP、地区和 UA 的当前账号记录 | PASS | `notice-login-log.png` |
| SR-003 | M11 | system API/core/starter 定向 Maven 测试 | PASS | `SysLogServiceIntegrationTest`、`SystemApiContractTest`；72 tests passed |
| SR-004 | M11 | 运行授权 core 与资源同步 starter 的 H2 集成测试 | PASS | `ApiResourceServiceImplIntegrationTest` 16 tests；`ApiAccessResourceProviderDatabaseComparisonTest` 3 tests |

## 7. 例外与剩余风险

- 本地隔离数据库中的既有登录日志数量取决于当前工作区内的实际登录次数；非空字段展示由 Chromium 隔离数据覆盖，真实接口与真实数据库在最终验收阶段回读。
