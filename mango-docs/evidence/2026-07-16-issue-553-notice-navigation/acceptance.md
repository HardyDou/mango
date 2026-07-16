# Issue 553 通知入口与登录角色权限验收证据

## 1. 验收范围

- 页面：Admin Shell 顶部通知铃铛、消息中心“我的消息”、接收设置、统一文件预览页。
- 接口：个人系统消息、接收设置、用户菜单、Realtime negotiate/SSE/WebSocket/Polling，以及文件上传、详情、预览内容、统一预览链接、下载和打包。
- 权限：内置 `ROLE_LOGIN` 的个人消息与文件基础访问；`ROLE_ANONYMOUS` 不获得个人消息、文件上传或预览链接创建权限；文件管理写操作仍需业务权限。
- 数据：隔离工作区数据库 `mango_dev_mango_issue_553_notice_route_003`，只使用 Resource Registry 正式资源和真实 API。
- 部署形态：Mango 单体后端与管理端 source 模式。

## 2. 执行环境

- 前端地址：`http://127.0.0.1:30003`
- 后端地址：`http://127.0.0.1:18003`
- 数据库或租户：`mango_dev_mango_issue_553_notice_route_003`；平台租户。
- 测试账号：通知/Realtime 使用隔离库普通成员 `issue553user`；文件 E2E 由 admin 仅负责创建/清理临时普通成员，所有文件允许/拒绝断言均使用该普通成员 token，且建号流程未分配任何显式角色（验收记录不保存密码或 token）。
- 浏览器：Chromium。

## 3. 功能验收记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| NOTICE-553-001 | TC-001 | Admin Shell 菜单解析 | 按 `menuCode` 解析嵌套消息菜单 | `notice:site-message` | 返回 `/message-center/site-message` | Vitest 纯函数用例覆盖嵌套菜单 | 单元测试不发网络请求；8 个测试文件、42 条用例全部通过 | `pnpm --filter @mango/admin-shell test`：42 条通过 | PASS |
| NOTICE-553-002 | TC-002 | Admin Shell 菜单解析 | 接收设置隐藏页面可显式导航；兼容旧路由名；缺失/不可运行目标返回空 | `notice:receive-setting`、缺失编码 | 隐藏目标返回 `/notice/receive-setting`；异常目标返回空 | Vitest 覆盖隐藏、兼容和失败分支 | 单元测试不发网络请求；8 个测试文件、42 条用例全部通过 | `pnpm --filter @mango/admin-shell test`：42 条通过 | PASS |
| NOTICE-553-003 | TC-003 | Notice 资源声明 | 登录角色获得个人消息菜单与最小权限 | `ROLE_LOGIN` | “我的消息/公告”绑定 `ROLE_LOGIN`；基础权限含 site、receive-setting view/edit 和 business view；无业务配置写权限；无 `ROLE_ANONYMOUS` | 直接解析 classpath Resource JSON，非浏览器用例 | 不发网络请求；契约断言角色、权限白名单和匿名排除均通过 | `NoticeResourceDeclarationContractTest`：2 条通过 | PASS |
| NOTICE-553-004 | TC-004 | Realtime 访问资源 | 客户端实时传输入口全部为登录访问 | negotiate、SSE、WebSocket、Polling、probe、inbound | Controller 注解与 WebSocket 动态资源均为 `LOGIN` | 反射 Controller 注解并检查动态资源注册器，非浏览器用例 | 不发网络请求；Realtime 两个测试类共 3 条用例全部通过 | `RealtimeControllerAccessModeTest`、`RealtimeWebSocketResourceRegistrarTest`：3 条通过 | PASS |
| NOTICE-553-005 | TC-005 | 顶部通知铃铛 / 我的消息 | 无显式角色的普通用户真实登录后点击“查看全部” | `issue553user`，仅自动 `ROLE_LOGIN` | URL 进入 `/message-center/site-message` 且页面加载完成 | 页面标题和消息内容正常 | 用户菜单、未读数、列表、详情均 200；无失败请求/console error | [my-messages.png](./my-messages.png) | PASS |
| NOTICE-553-006 | TC-006 | 顶部通知铃铛 / 接收设置 | 普通用户分别从消息页和铃铛点击“接收设置” | `issue553user`，仅自动 `ROLE_LOGIN` | URL 进入 `/notice/receive-setting` 且页面加载完成 | 提醒设置、接收账户、业务类型正常显示 | 后端 `GET /notice/business-types`、接收账户、接收偏好均 200；无 401/403 | [receive-setting.png](./receive-setting.png) | PASS |
| NOTICE-553-007 | TC-007 | 普通用户真实登录会话 / Realtime | 协商并建立实际传输链路 | `issue553user`，无显式角色绑定 | negotiate 与实际 WebSocket 成功，无 401/403 | 页面持续接收实时消息 | negotiate 200；probe WebSocket 收 1 帧，正式 WebSocket 收 2 帧；资源库中的 negotiate/SSE/WS/Polling/probe 均为 `LOGIN` | `.mango/run/logs/mango-backend.log` | PASS |
| FILE-553-008 | TC-008 | `/file/files`、detail、preview-content、download、package | 普通登录用户的附件基础能力 | E2E 临时普通成员，无显式角色 | 上传、详情、预览元数据、预览原文、下载和打包均 200，预览原文与上传内容一致 | 使用真实后端存储与文件响应，无 mock | 所有允许断言均使用普通用户 token | `file-access-baseline-permissions.spec.ts`：Chromium 1 条通过 | PASS |
| FILE-553-009 | TC-009 | `/file-preview/files/preview-link`、前端 `/api/.../preview-entry` | 普通登录用户创建并访问统一预览链接 | 同 TC-008 | 后端预览链接请求状态为 HTTP 200；实际访问前端 `/api` 代理链接返回 HTML 响应，状态为 HTTP 200 | 预览入口可访问；Office 插件关闭不影响 txt 验收 | 后端实际路径为不带 `/api` 的 `/file-preview/...`，`/api` 仅为前端代理标识 | 同 TC-008 | PASS |
| FILE-553-010 | TC-010 | 文件拒绝矩阵 | 匿名和普通用户管理边界 | 匿名会话、普通成员会话 | 匿名上传/创建预览链接被拒绝；普通用户列表、归档、删除、保存设置被拒绝 | 无管理功能越权 | 拒绝响应为 401/403 或对应业务失败码 | 同 TC-008 | PASS |
| AUTH-553-011 | TC-011 | 干净库启动对账 | 恢复平台 admin 的管理员角色 | admin 机构成员 `1001` | 真实库 `authorization_subject_role` 绑定 `ROLE_ADMIN`；admin 可创建/清理 E2E 普通成员 | admin 不用于文件权限结论 | 启动资源同步和租户对账均完成 | `IdentityUserServiceIntegrationTest`：9 条通过；真实库 SQL 核对 | PASS |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| Admin Shell / Notice | 我的消息、接收设置 | 通知铃铛查看全部 | 消息页入口与通知铃铛设置 | 页面主内容、框架区域、提示、URL 与菜单激活均正常 | [my-messages.png](./my-messages.png)、[receive-setting.png](./receive-setting.png) | PASS |
| File / File Preview | 附件基础访问、统一预览 | 普通用户上传/预览/下载 | 匿名与文件管理写操作拒绝 | 前端预览代理页返回 HTML 200 | Playwright API/browser-context 访问记录 | PASS |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| npm/Maven 发布与业务项目升级 | 本任务只修复源码并创建 PR，不执行 release | 合并后已发布版本仍不包含修复 | 独立发布流程升版并回查 Nexus | 不适用 |

## 6. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| Mango 业务开发者 | 升级后不再为普通业务角色重复配置个人消息或 Realtime 建连权限；通知管理写权限仍单独授权 | Notice、Admin Shell、Realtime README | 注册 Notice admin-pages/admin-shell，真实登录验证铃铛 | 登录主体使用 `ROLE_LOGIN`；匿名主体无个人消息权限 | 菜单或页面缺失时 Shell 明确提示；检查资源同步版本与页面注册 | READY |
