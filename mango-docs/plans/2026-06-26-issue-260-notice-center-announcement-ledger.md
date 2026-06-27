# Issue #260 通知中心公告能力交付台账

文档状态：草案
关联 Issue：<https://github.com/HardyDou/mango/issues/260>
日期：2026-06-26

## 1. 目标

在现有通知中心内交付通知公告主链路：管理端公告管理、用户端公告阅读确认、发布范围落到用户级接收记录、系统消息提醒复用。

## 2. 范围

- 公告主数据、范围规则、用户级接收记录。
- 公告 API、Controller、Service、Mapper、Convert。
- 发布范围解析公共方法和 `NoticeRecipientResolver` 复用。
- 管理端公告管理页面。
- 用户端公告页面。
- 我的消息公告提醒跳转。
- 菜单资源和页面注册。
- README 和验证证据。

## 3. 不做什么

- 不新增独立“系统通知”模块。
- 不重做消息配置、发送任务、渠道配置、发送记录、失败重试。
- 不支持追加发布范围。
- 不支持岗位、外部用户、动态人群。
- 不支持公告正文版本管理。
- 不支持名单导出。
- 不为公告单独建设外部渠道推送。

## 4. 设计输入

- `mango-docs/plans/2026-06-26-issue-260-notice-center-announcement-plan.md`
- `mango-docs/designs/2026-06-26-issue-260-notice-center-announcement-design.md`
- GitHub Issue #260
- `mango-pmo/rules/**`

## 5. 设计说明

### 5.1 影响模块

- `mango/mango-platform/mango-notice/mango-notice-api`
- `mango/mango-platform/mango-notice/mango-notice-core`
- `mango/mango-platform/mango-notice/mango-notice-starter`
- `mango-ui/packages/notice`
- 后端和前端 README

### 5.2 接口变化

- 新增 `NoticeAnnouncementApi`。
- 新增公告 Command、Query、VO、枚举。
- 新增管理端公告接口和用户端公告接口。

### 5.3 数据变化

- 新增 `notice_announcement`。
- 新增 `notice_announcement_target`。
- 新增 `notice_announcement_recipient`。
- 不修改 `notice_task`、`notice_send_record`、`notice_site_message` 表结构。

### 5.4 菜单/页面/权限变化

- 通知中心新增公告管理菜单。
- 新增管理端页面 key：`notice/announcement/index`。
- 新增用户端页面 key：`notice/announcement-user/index`。
- 新增公告管理权限资源。
- 我的消息支持公告提醒跳转。

### 5.5 测试范围

- 后端 service/controller 单测和质量检查。
- 前端 package build/test。
- admin 样式和模块样式检查。
- 页面截图和 console/network 验收。
- README 审计。

## 6. 风险与限制

- 全员公告覆盖用户较多：首期同步展开，验收记录覆盖用户数量，超出性能范围登记新 Issue。
- 公告提醒发送失败：首期通过现有系统消息发送入口处理，发送失败按现有通知异常和发送记录治理。
- 目标用户解析依赖身份、组织、角色能力：复用身份服务目标解析能力，公告与系统消息共用 `NoticeRecipientResolver`。

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| ANN-001 | 设计 DEV-001 | 新增公告持久化模型 | 新增三张公告表和 Entity/Mapper/Convert | migration、entity、mapper、convert | 后端编译通过，migration 已登记 | DONE | `mvn -pl mango-platform/mango-notice/mango-notice-api,mango-platform/mango-notice/mango-notice-core,mango-platform/mango-notice/mango-notice-starter -am -DskipTests compile` |
| ANN-002 | 设计 DEV-002 | 新增公告 API 契约 | 新增 `NoticeAnnouncementApi` 和 Command/Query/VO/枚举 | api 模块代码 | 后端编译通过，controller 实现检查 | DONE | 同 ANN-001 |
| ANN-003 | 设计 DEV-003 | 发布范围最终落到用户 | 公告与系统消息共用 `NoticeRecipientResolver`，全员分页取启用用户，发布前按用户去重 | resolver、service 发布逻辑 | 后端编译通过，公告 service 单测覆盖全员去重和范围互斥 | DONE | `NoticeAnnouncementServiceTest`；同 ANN-001 |
| ANN-004 | 设计 DEV-004 | 管理端公告管理 | 实现草稿、发布、下线、统计 | controller、service、mapper、page | 编译/build 通过；service 单测覆盖发布规则；E2E 覆盖管理端页面主链路 | DONE | 后端编译、`NoticeAnnouncementServiceTest`、`pnpm --filter @mango/notice build`、`pnpm test:e2e e2e/specs/notice-announcement.spec.ts --project=chromium` |
| ANN-005 | 设计 DEV-005 | 用户公告阅读确认 | 用户只能看自己的公告，可阅读确认 | controller、service、page | 编译/build 通过；service 单测覆盖可见性、阅读、确认；E2E 覆盖用户公告阅读确认 | DONE | 后端编译、`NoticeAnnouncementServiceTest`、`pnpm --filter @mango/notice build`、`pnpm test:e2e e2e/specs/notice-announcement.spec.ts --project=chromium` |
| ANN-006 | 设计 DEV-006 | 公告提醒复用系统消息 | 使用 `notice.announcement.published` 和 `bizType/bizId` | send 集成、我的消息跳转适配 | 编译/build 通过；service 单测覆盖提醒命令；E2E 覆盖系统消息跳转公告详情 | DONE | 后端编译、`NoticeAnnouncementServiceTest`、`pnpm --filter @mango/notice build`、`pnpm test:e2e e2e/specs/notice-announcement.spec.ts --project=chromium` |
| ANN-007 | 设计 DEV-007 | 菜单资源和页面注册 | 管理端 `通知中心` 与用户侧 `消息中心` 分离；不新增系统通知模块 | `notice-common-menu.json`、admin-pages | build 通过；真实数据库、真实菜单接口和真实 UI 菜单验证通过 | DONE | `pnpm --filter @mango/notice build`、真实接口 `/authorization/menus/user?appCode=internal-admin&fmt=tree`、`.runtime/issue-260-menu-check/*.png` |
| ANN-008 | 设计 DEV-008 | README 更新 | 后端和前端说明公告能力 | README | 文档已更新 | DONE | README diff |
| ANN-009 | 计划验证 | 执行后端验证 | 运行 notice 模块编译和 core 测试 | 测试输出 | 编译通过，notice-core 测试 85 个通过；`api-contract`/`path-param` 中无新增公告接口问题，仍被历史问题阻断 | DONE | 后端编译命令；`mvn -pl mango-platform/mango-notice/mango-notice-core -am test`；`mvn -pl mango-platform/mango-notice mango:check -Drule=api-contract`；`mvn -pl mango-platform/mango-notice mango:check -Drule=path-param` |
| ANN-010 | 计划验证 | 执行前端验证 | notice 包 build 和样式检查 | 测试输出 | build、admin 样式、模块样式检查通过；notice 包无 test 脚本 | DONE | `pnpm --filter @mango/notice build`；`pnpm admin:styles:check`；`pnpm admin:module-styles:check` |
| ANN-011 | 计划验证 | 页面验收证据 | 保存关键页面截图和 console/network 结果 | 截图、验收记录 | 验收证据检查 | DONE | `mango-docs/evidence/2026-06-26-issue-260-notice-announcement/acceptance-evidence.md` |

## 8. 验收证据记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| ANN-004 | 通知中心 / 公告管理 | 发布公告 | `系统升级通知`、`全员安全提醒` | 公告发布后生成用户级接收记录，重复用户只保留一条 | 表格、表单、发布确认、下线确认已验证 | E2E console error 与 notice API 4xx/5xx 为空 | `announcement-admin-mixed-targets.png` | PASS |
| ANN-005 | 消息中心 / 公告 | 阅读确认 | `端午值班安排` | 用户只能操作自己的公告，确认后不再出现在未读/待确认筛选 | 列表、详情弹窗、确认按钮、筛选已验证 | E2E console error 与 notice API 4xx/5xx 为空 | `announcement-user-confirmed.png` | PASS |
| ANN-006 | 消息中心 / 我的消息 | 公告提醒跳转 | `bizType=notice.announcement.published`、`bizId=ann-100` | 从消息进入公告详情，确认仍写公告对象 | 消息列表跳转、公告详情、已读状态已验证 | E2E console error 与 notice API 4xx/5xx 为空 | `site-message-announcement-jump.png` | PASS |
| ANN-007 | 菜单 | 管理端和用户侧菜单拆分 | 真实库 `mango_dev_d8db9e`，账号 `admin` | `通知中心` 只显示管理类菜单；`消息中心` 显示我的消息和公告；接收设置隐藏 | 真实 UI 登录后点击顶部菜单验证通过 | 后端健康检查 UP；真实菜单接口返回正确树 | `.runtime/issue-260-menu-check/notice-center-admin-menu.png`、`.runtime/issue-260-menu-check/message-center-user-menu.png` | PASS |
