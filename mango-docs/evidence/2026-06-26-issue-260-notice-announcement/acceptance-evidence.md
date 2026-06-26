# Issue #260 通知中心公告能力验收证据

## 1. 验收范围

- 页面：公告管理 `/notice/announcement`、用户公告 `/message-center/announcement`、我的消息 `/message-center/site-message`
- 接口：公告草稿保存、编辑、发布、下线、用户公告列表/详情/确认、系统消息公告跳转已读
- 权限：公告管理、用户公告、系统消息菜单和操作权限通过前端授权菜单 mock 验证
- 数据：全员、组织、角色、指定用户发布对象；组织/角色/用户重复解析后只计一条收件
- 部署形态：mango-admin 本地 Vite + Playwright route mock，后端核心服务通过 notice-core 单测验证；菜单拆分使用真实后端、真实数据库和真实 UI 验证

## 2. 执行环境

- 前端地址：Playwright webServer；真实菜单验收 `http://127.0.0.1:7918`
- 后端地址：E2E 使用 route mock；核心服务单测使用 H2；真实菜单验收 `http://127.0.0.1:18228`
- 数据库或租户：租户 `芒果集团`；后端单测 H2 内存库；真实菜单验收数据库 `mango_dev_d8db9e`
- 测试账号：admin / admin123
- 浏览器：Playwright Chromium

## 3. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| ANN-E2E-001 | 公告管理 | 菜单入口、草稿保存、编辑、组织/角色/指定用户发布、统计 | `系统升级通知`；组织=技术部、角色=系统管理员、用户=管理员 | 草稿保存后展示草稿；发布请求只带 id；发布后状态为已发布；组织/角色/用户重复解析后接收统计为 `2/0/0` | 使用既有 `ParticipantSelector` 选择用户、部门范围、角色；详情弹窗展示接收人数 | E2E 收集 console error 与 notice/api 4xx/5xx，结果为空 | `announcement-admin-mixed-targets.png`；命令 `pnpm test:e2e e2e/specs/notice-announcement.spec.ts --project=chromium` 通过 | PASS |
| ANN-E2E-002 | 公告管理 | 全员发布、发布对象互斥、下线 | `全员安全提醒`；发布对象=全员 | 全员开关后不展示参与人选择器；保存快照为 `ALL/全员`；发布后统计 `3/0/0`；下线后状态为已下线 | 保存并发布、下线确认框按真实文案执行 | E2E 收集 console error 与 notice/api 4xx/5xx，结果为空 | `announcement-admin-mixed-targets.png`；命令 `pnpm test:e2e e2e/specs/notice-announcement.spec.ts --project=chromium` 通过 | PASS |
| ANN-E2E-003 | 用户公告 | 公告列表、详情已读、确认、未读/待确认筛选 | `端午值班安排`；需确认；初始未读待确认 | 打开详情后可见公告内容；点击确认已读后状态为已确认；未读和待确认筛选不再显示已确认公告 | 用户公告页表格、详情弹窗、确认按钮、筛选复选框完成闭环 | E2E 收集 console error 与 notice/api 4xx/5xx，结果为空 | `announcement-user-confirmed.png`；命令 `pnpm test:e2e e2e/specs/notice-announcement.spec.ts --project=chromium` 通过 | PASS |
| ANN-E2E-004 | 我的消息 | 公告系统消息跳转用户公告详情并标记消息已读 | 系统消息 `公告：端午值班安排`；`bizType=notice.announcement.published`；`bizId=ann-100` | 点击系统消息详情后路由为 `/message-center/announcement?id=ann-100`；展示公告详情；原系统消息 readStatus 变为 READ | 系统消息列表不弹普通消息详情，直接进入公告详情 | E2E 收集 console error 与 notice/api 4xx/5xx，结果为空 | `site-message-announcement-jump.png`；命令 `pnpm test:e2e e2e/specs/notice-announcement.spec.ts --project=chromium` 通过 | PASS |
| ANN-REG-001 | 后端核心 | 公告发布、去重、读确认、分页 mapper 回归 | notice-core 单测数据；公告服务 6 个新增用例 | `NoticeAnnouncementServiceTest` 6 个用例通过；notice-core 合计 85 tests 通过 | 后端服务级验证，无页面交互；校验发布落库、收件去重、可见性、确认和分页 mapper | Maven 测试无失败、无错误 | 命令 `mvn -pl mango-platform/mango-notice/mango-notice-core -am test` 通过 | PASS |
| ANN-REG-002 | 前端包 | notice 包构建回归 | `@mango/notice` | Vite build 和类型生成完成 | 前端包级验证，无页面交互；校验公告页面、API、类型和样式可被打包 | 构建命令退出码 0 | 命令 `pnpm --filter @mango/notice build` 通过 | PASS |
| ANN-REG-003 | 菜单资源 | 管理端通知中心和用户侧消息中心拆分 | 真实数据库 `mango_dev_d8db9e`；账号 admin；真实菜单接口 | `通知中心` 可见子菜单为公告管理、消息配置、发送任务、渠道配置、发送记录、失败重试；`消息中心` 可见子菜单为我的消息、公告；接收设置隐藏 | 真实 UI 登录后点击顶部菜单验证通过，侧边菜单显示符合规划 | 后端健康检查 UP；真实菜单接口返回正确树 | `.runtime/issue-260-menu-check/notice-center-admin-menu.png`、`.runtime/issue-260-menu-check/message-center-user-menu.png` | PASS |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| 消息中心 | 我的消息 | 普通消息菜单仍可进入 | 公告消息按 `bizType` 跳转公告页 | 消息页标题、公告详情弹窗可见 | `site-message-announcement-jump.png` | PASS |
| 通知中心 | 公告管理 | 表单必填校验停留在表单提示 | 校验失败不再抛 Vue console error | 标题、内容、发布对象错误提示可见 | Playwright console 监控结果为空 | PASS |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 真实 MySQL + monolith 公告业务写入端到端数据验收 | 本轮完整公告业务 E2E 使用 Playwright route mock，后端核心以单测验证；菜单拆分已使用真实 MySQL/monolith 验证 | 公告发布业务的真实库写入仍主要由 service 单测和 mapper 测试保障 | 合并后如需完整联调，可在当前工作区复用 `18228/7918` 运行真实数据场景 | 用户已确认先满足本质能力 80% |
