# Issue 692 个人中心消息中心验收证据

## 1. 验收范围

- 页面：个人中心的“我的消息”“系统公告”“通知设置”“登录日志”，内容运营九个页面，以及旧路由 `/#/notice/receive-setting`。
- 接口：当前用户消息、当前用户公告、消息接收偏好、个人提醒配置、业务域配置、当前账号登录日志、内容运营页面接口和公开 API 资源同步。
- 权限：使用本地 `admin` 账户登录；消息中心改为个人中心内部入口，不再依赖顶层“消息中心”菜单。
- 数据：真实本地数据库为空消息/公告/业务类型数据；定向 Chromium 用例使用隔离 fixture 覆盖有消息、公告确认和渠道保存。
- 部署形态：`mango-admin` 单体开发态，前端 `30015`，后端 `18015`。

## 2. 执行环境

- 前端地址：`http://127.0.0.1:30015`
- 后端地址：`http://127.0.0.1:18015`
- 数据库或租户：`mango_dev_mango_issue_692_notification_center_015` / 芒果集团
- 测试账号：`admin`（仅本地隔离环境）
- 浏览器：Chromium（Google Chrome channel，1440 × 1000）

## 3. 功能验收记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| ISSUE-692-01 | TC-001 | `/#/profile?tab=notice-site-message` | 个人中心保留入口，增加“消息中心”分组与“我的消息” | 隔离消息：请假申请已通过；真实环境为空列表 | 四个分组入口存在；消息中心位于首组；侧栏顶部不显示“个人中心/账户设置”；顶层无可见“消息中心”；消息查询后仍回显目标记录 | 整个菜单和内容区域居中；使用 `MangoListPage`、`MangoSearchPanel`、`MangoListPanel`、`Pagination`；内容区不显示重复标题和说明 | Chromium 用例 diagnostics 为空；真实登录走查 diagnostics 为空 | 我的消息截图（历史验收图片已清理（可从 Git 历史恢复））；`notice-message-center-menu.spec.ts` | PASS |
| ISSUE-692-02 | TC-002 | `/#/profile?tab=notice-announcement-user` | 系统公告迁入个人中心 | 隔离公告：服务维护公告，待确认；真实环境为空列表 | 打开详情后显示公告标题；确认已读后状态变为“已确认” | 使用 `MangoListPage`、`MangoSearchPanel`、`MangoListPanel`、`Pagination`；详情弹窗和空状态均可见 | Chromium 用例 diagnostics 为空；真实登录走查 diagnostics 为空 | 系统公告截图（历史验收图片已清理（可从 Git 历史恢复））；`notice-message-center-menu.spec.ts` | PASS |
| ISSUE-692-03 | TC-003 | `/#/profile?tab=notice-receive-setting` | 通知设置采用横向复选框，`SITE` 显示“站内信” | 隔离业务类型：请假审批通过；邮件初始未选 | 每行只显示站内信、邮件、短信、企业微信四种接收方式；邮件勾选后向接口保存；不存在“绑定账号”区域；业务域不显示代码 | 左业务域、右消息类型、同一行四个接收方式复选框不换行；提醒设置第一行控件基线对齐，第二行含播报内容和操作按钮，两行左右边界误差不超过 1px | Chromium 用例 diagnostics 为空；真实登录走查 diagnostics 为空 | 通知设置截图（历史验收图片已清理（可从 Git 历史恢复））；`notice-message-center-menu.spec.ts` | PASS |
| ISSUE-692-04 | TC-004 | `/#/notice/receive-setting` | 旧通知设置路径兼容 | 隔离 fixture | 旧路由仍渲染通知设置页面 | 页面主内容非空白 | Chromium 用例 diagnostics 为空 | `notice-message-center-menu.spec.ts` | PASS |
| ISSUE-692-05 | TC-005 | `/#/profile?tab=login-log`；`GET /system/log/login/my/list` | 安全设置新增当前账号登录日志 | 隔离日志与真实 `admin` 登录日志 | 页面显示登录时间、IP、IP 地区、浏览器 UA；接口不接收前端用户 ID，并按当前租户和当前用户过滤 | 使用 `MangoListPage`、`MangoSearchPanel`、`MangoListPanel`、`Pagination`；长 UA 可查看完整内容 | Chromium 用例 diagnostics 为空；真实接口返回成功且包含 5 条当前账号日志 | 登录日志截图（历史验收图片已清理（可从 Git 历史恢复））；`notice-message-center-menu.spec.ts` | PASS |
| ISSUE-692-06 | TC-006 | `/#/cms/*` 内容运营九个页面 | 修复内容运营页面动态组件解析 | 隔离 admin 登录态；覆盖站点、栏目、内容、分类、标签、发布、导航、广告位和广告投放 | 九个路由均显示对应二级标题；无 `Invalid vnode type` 警告；页面不为空白 | 页面均由各自 Vue 组件加载，使用统一内容运营页面骨架 | Chromium diagnostics 无 `Invalid vnode type`；页面标题断言全部通过 | `cms-content-pages.spec.ts` | PASS |
| ISSUE-692-07 | TC-007 | `IDENTITY` 业务域资源声明 | 注册正式身份管理业务域 | Identity starter 资源声明 | `domainCode=IDENTITY`、`domainShortCode=IDN`、名称“身份管理”、`target-module=domain` 均可被契约测试读取 | 业务域注册与模块资源声明保持一致 | `IdentityResourceDeclarationContractTest` 通过（2 tests） | identity resource yml、Identity starter 契约测试 | PASS |
| ISSUE-692-08 | TC-008 | API 资源同步 | 恢复被错误禁用的公开接口资源 | H2 隔离资源表中的 `POST /auth/login-institutions`，初始 `status=0`、`access_mode=PUBLIC` | 重新同步后 `status=1` 且匿名访问决策为 `PUBLIC`；手工禁用资源仍保持 `status=0` | 自动同步与人工状态边界保持明确 | `ApiResourceServiceImplIntegrationTest` 16 tests；`ApiAccessResourceProviderDatabaseComparisonTest` 3 tests | 授权资源 H2 集成测试 | PASS |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| `@mango/auth` | 个人中心 | 查询参数 `tab` 驱动侧栏激活态 | 原“第三方授权”入口仍保留 | “消息中心”分组置于首位；侧栏和内容区删除重复标题说明 | 四张页面截图 | PASS |
| `@mango/notice` | 我的消息、系统公告 | 统一列表与搜索组件 | 空状态与详情/确认主路径 | 表格、工具栏、分页、详情弹窗无明显错位 | Chromium E2E | PASS |
| `@mango/notice` | 通知设置 | 站内信标签、播报内容 | 渠道复选框保存 | 不再出现账号绑定管理区；提醒设置两行左右对齐 | 通知设置截图、Chromium E2E | PASS |
| `@mango/system` | 登录日志 | 当前账号数据隔离 | 时间、IP、地区和浏览器 UA 展示 | 安全设置内新增入口，统一列表样式 | 登录日志截图、Chromium E2E、真实接口回读 | PASS |
| `@mango/cms` | 内容运营九页 | 动态页面组件解析 | 页面标题与运行态 | 九个页面均非空白，无 `Invalid vnode type` | CMS Chromium E2E、包测试 | PASS |
| `mango-identity-starter` | `IDENTITY` 业务域 | 资源声明可发现 | 领域名称与模块目标正确 | 业务域契约测试 | PASS |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 真实环境的非空消息/公告/业务类型数据 | 隔离数据库当前没有这三类业务数据 | 不影响真实空状态、登录和页面装配验证；非空主路径由隔离 Chromium fixture 覆盖 | 后续以业务真实数据复跑同一用例即可 | 不需要 |
| 本机 IP 地区为“未知” | IP 定位数据对本地地址没有地区结果 | 页面与接口字段完整，不影响登录日志查询 | 生产公网 IP 将按现有定位能力返回对应地区 | 不需要 |

## 6. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| Mango 维护者 | 个人中心消息中心回归用例与真实页面截图 | `mango-ui/apps/mango-admin/e2e/specs/notice-message-center-menu.spec.ts`；本目录 | `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:30015 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18015 pnpm exec playwright test apps/mango-admin/e2e/specs/notice-message-center-menu.spec.ts --config apps/mango-admin/playwright.config.ts --project chromium` | 用例 fixture 不写真实后端；真实走查仅使用隔离环境本地 `admin` | 检查 Playwright 报告、截图及浏览器 diagnostics | DONE |
