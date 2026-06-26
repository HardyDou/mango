# Issue #260 通知中心公告能力实施计划

文档状态：草案
关联 Issue：<https://github.com/HardyDou/mango/issues/260>
日期：2026-06-26

## 1. 目标

升级现有通知中心，在不新增独立“系统通知”模块的前提下，补齐通知公告能力。

首期交付主链路：

- 管理端在通知中心下新增公告管理。
- 用户端新增公告列表和详情。
- 公告支持全员、组织、角色、指定用户发布。
- 发布范围最终展开为用户级接收记录。
- 用户可阅读、确认公告。
- 管理端可查看阅读和确认统计。
- 公告可同步系统消息提醒，并复用现有系统消息渠道能力。

## 2. 不处理范围

- 不重做消息配置、发送任务、渠道配置、发送记录、失败重试。
- 不新增独立“系统通知”模块。
- 不支持公告发布后追加发布范围。
- 不支持岗位、外部用户、动态人群发布。
- 不支持公告正文版本管理。
- 不支持未读或未确认名单导出。
- 不为公告单独建设短信、邮件、企微、钉钉、飞书等外部渠道推送能力。
- 不做大租户全员公告的懒生成或异步分批优化。

## 3. 设计结论

### 3.1 对象边界

| 对象 | 作用 |
|---|---|
| 公告 | 平台正式发布内容，承载标题、正文、有效期、置顶、确认要求和状态。 |
| 发布范围 | 管理员选择的范围规则，包括全员、组织、角色、指定用户。 |
| 接收记录 | 发布范围解析后的用户级结果，承载阅读、确认和统计。 |
| 系统消息提醒 | 公告发布时生成的提醒消息，只负责提醒和跳转。 |

### 3.2 核心规则

- 发布范围是规则，接收记录是结果。
- 公告发布时保留范围规则，同时解析为用户级接收记录。
- 全员、组织、角色、指定用户最终都落到用户。
- 多个范围命中同一用户时，按用户去重，只生成一条接收记录。
- 全员公告也按当前租户内全部可登录用户生成接收记录。
- 后续组织、角色、用户关系变化，不影响已发布公告覆盖范围和统计。
- 公告同步系统消息提醒时，接收人等于去重后的公告覆盖用户。
- 公告正文、阅读、确认、统计以公告对象为准，系统消息只作为提醒入口。

## 4. 数据改动计划

### 4.1 新增表

| 表 | 用途 |
|---|---|
| `notice_announcement` | 公告主表。 |
| `notice_announcement_target` | 公告发布范围规则表。 |
| `notice_announcement_recipient` | 公告用户级接收、阅读、确认表。 |

### 4.2 复用表

| 表 | 复用方式 |
|---|---|
| `notice_task` | 公告同步系统消息提醒时复用现有发送任务。 |
| `notice_send_record` | 复用现有发送记录和失败治理。 |
| `notice_site_message` | 复用现有站内消息，使用业务上下文关联公告详情。 |

### 4.3 不改表

首期不修改 `notice_task`、`notice_send_record`、`notice_site_message` 表结构；优先复用已有业务上下文字段。

## 5. 公共底座任务

### BASE-000 设计与交付台账门禁

交付物：

- 详细设计文档。
- 交付台账。

完成标准：

- 详细设计写清接口变化、数据变化、菜单/页面/权限变化、测试范围。
- 详细设计必须确定发布范围用户解析方法和可复用公共服务。
- 详细设计必须确定公告提醒如何复用现有系统消息发送入口。
- 详细设计必须确定管理端和用户端公告接口权限边界。
- 交付台账逐项登记后台能力、接口能力、数据能力、菜单能力、页面能力和验收标准。
- 未完成本项前，不进入代码实现。

### BASE-001 数据库与持久化对象

| 层 | 交付物 | 完成标准 |
|---|---|---|
| migration | 新增 notice migration | 文件名使用当前 notice migration 最大版本 + 1；当前基线下一版本为 `V15__notice_announcement.sql`；不修改历史 migration。 |
| entity | `NoticeAnnouncementEntity`、`NoticeAnnouncementTargetEntity`、`NoticeAnnouncementRecipientEntity` | 字段覆盖公告主表、范围规则、用户接收记录；包含租户和审计字段。 |
| mapper | `NoticeAnnouncementMapper`、`NoticeAnnouncementTargetMapper`、`NoticeAnnouncementRecipientMapper` | Mapper 访问公告表；用户端公告分页可在公告表和接收表之间联查，保证分页 total 与过滤条件一致。 |
| convert | `NoticeAnnouncementConvert` | Entity 与 VO 转换集中维护，Controller 不直接组装 Entity。 |

约束：

- `notice_announcement_recipient` 必须有 `announcement_id + user_id` 唯一约束。
- 统计以 `notice_announcement_recipient` 为准。
- 发布范围回显以 `notice_announcement_target` 为准。
- `notice_task`、`notice_send_record`、`notice_site_message` 首期不改表结构。

### BASE-002 API 契约与枚举

| 类型 | 交付物 | 说明 |
|---|---|---|
| enum | `NoticeAnnouncementStatus` | `DRAFT`、`PUBLISHED`、`OFFLINE`；过期由有效期计算。 |
| enum | `NoticeAnnouncementTargetType` | `ALL`、`ORG`、`ROLE`、`USER`。 |
| enum | 复用 `NoticeReadStatus` | `UNREAD`、`READ`。 |
| enum | `NoticeAnnouncementConfirmStatus` | `NOT_REQUIRED`、`PENDING`、`CONFIRMED`。 |
| api | `NoticeAnnouncementApi` | 公告管理、用户公告、阅读确认、统计的对外契约。 |
| command | `SaveNoticeAnnouncementCommand` | 创建和编辑草稿；编辑时必须带公告 ID。 |
| command | `PublishNoticeAnnouncementCommand` | 发布公告，包含范围、有效期、置顶、确认、同步提醒。 |
| query | `NoticeAnnouncementIdQuery` | 公告详情、统计、下线、用户详情和确认使用的 ID 入参。 |
| query | `NoticeAnnouncementPageQuery` | 管理端公告分页。 |
| query | `MyNoticeAnnouncementPageQuery` | 用户端公告分页。 |
| vo | `NoticeAnnouncementVO` | 管理端公告列表和详情。 |
| vo | `NoticeAnnouncementVO` | 管理端和用户端公告列表、详情。 |
| vo | `NoticeAnnouncementStatsVO` | 覆盖、已读、待确认、已确认统计。 |

约束：

- API 模型放在 `mango-notice-api`。
- `NoticeAnnouncementController` 必须实现 `NoticeAnnouncementApi`。
- `NoticeAnnouncementApi` 必须按管理端方法和用户端方法分组声明。
- API 入参使用 Command / Query，不暴露 Entity。
- 字段必须有校验注解和中文文档说明。

### BASE-003 公共服务方法

| 服务能力 | 所属层 | 完成标准 |
|---|---|---|
| 发布范围校验 | service | 校验范围不能为空、全员互斥、组织范围包含下级口径。 |
| 覆盖用户解析 | service | 复用 `NoticeRecipientResolver`，支持全员、组织、角色、指定用户，最终返回去重用户集合。 |
| 接收记录批量创建 | service/mapper | 按用户批量插入接收记录，重复用户不重复生成。 |
| 公告有效性判断 | service | 已发布、未下线、未过期才对用户有效。 |
| 阅读状态变更 | service | 首次阅读只记录一次。 |
| 确认状态变更 | service | 必须已读后确认；已确认不可重复确认。 |
| 公告提醒发送 | service | 通过现有 `send` 能力生成系统消息提醒，使用 `bizType + bizId` 关联公告。 |
| 公告统计聚合 | service/mapper | 基于接收记录统计覆盖、已读、未读、已确认、未确认。 |

跨域约束：

- 公告模块不得直接查询身份、组织、角色所属表。
- 覆盖用户解析通过 `NoticeRecipientResolver` 复用系统消息的目标解析能力。
- `mango-notice-core` 不得依赖其它模块 `core` 或 `starter`。

系统消息提醒约束：

- 公告提醒使用固定业务编码 `notice.announcement.published`。
- 公告提醒通过现有 `send` 能力发送 SITE 系统消息；首期使用直接标题/内容，不新增公告专属外部渠道。
- 提醒内容只包含标题、摘要和公告详情跳转上下文。
- 公告发布成功不依赖提醒所有渠道成功；提醒失败进入现有发送记录和失败重试。

### BASE-004 前端类型、API 与页面内组件

| 类型 | 交付物 | 完成标准 |
|---|---|---|
| types | 公告状态、范围类型、阅读状态、确认状态、公告 VO、Query、Command 类型 | ID 使用字符串语义。 |
| api | 管理端公告 API、用户端公告 API | 路径和后端契约对齐。 |
| page component | `AnnouncementStatusTag` | 页面内组件，展示草稿、已发布、已下线、已过期。 |
| page component | `AnnouncementStatSummary` | 页面内组件，展示覆盖、已读、未读、已确认、未确认。 |

约束：

- 发布范围选择优先复用 `@mango/system` 的 `ParticipantSelector`，沿用发送任务页面已有接收对象选择模式。
- 全员开关作为公告发布表单私有控件处理；选择全员时禁用或清空 `ParticipantSelector` 的组织、角色、指定用户选择。
- 首期不升级 `ParticipantSelector` 公共组件；只有现有组件无法满足公告发布表单时，才在详细设计中登记组件升级项。
- 本次不新增 package 公共导出组件。
- 组件放在公告页面目录。

## 6. 功能模块分层计划

### MOD-001 管理端公告管理

页面：通知中心 / 公告管理

| 层 | 交付物 | 职责 |
|---|---|---|
| page | `views/announcement/index.vue` | 管理端公告列表、创建草稿、编辑草稿、发布、下线、统计入口。 |
| component | 发布表单、`ParticipantSelector` 范围选择适配、统计面板 | 优先复用已有参与人选择组件；页面内处理全员互斥。 |
| api.ts | `createAnnouncement`、`updateAnnouncement`、`publishAnnouncement`、`offlineAnnouncement`、`getAnnouncementPage`、`getAnnouncementDetail`、`getAnnouncementStats` | 前端调用后端公告管理能力。 |
| api | `NoticeAnnouncementApi` 管理端方法 | 管理端公告契约。 |
| controller | `NoticeAnnouncementController` 管理端方法 | 实现 `NoticeAnnouncementApi`，只做协议适配、参数校验、权限入口。 |
| service | `INoticeAnnouncementService` / `NoticeAnnouncementService` | 创建草稿、编辑草稿、发布、下线、统计业务编排。 |
| mapper | 三个公告 Mapper | 公告主表、范围表、接收表读写。 |
| convert | `NoticeAnnouncementConvert` | 列表、详情、统计 VO 转换。 |
| test | service 单测、编译和页面 smoke | 范围校验、发布、下线、统计。 |

完成标准：

- 未选择发布范围不能发布。
- 全员与其他范围互斥。
- 发布时写入范围规则和用户级接收记录。
- 已发布正文不可修改。
- 下线后用户不可见，统计保留。
- 管理端接口必须绑定公告管理权限，不能只依赖前端菜单权限。

### MOD-002 发布范围解析

| 层 | 交付物 | 职责 |
|---|---|---|
| service | `NoticeRecipientResolver` 复用 | 统一解析全员、组织、角色、指定用户。 |
| service | 用户去重方法 | 按 `user_id` 去重。 |
| test | service 单测 | 全员、组织、角色、指定用户、组合重复命中。 |

完成标准：

- 所有范围最终落到用户 ID。
- 全员也生成用户级接收记录。
- 组织、角色、指定用户组合命中同一人只生成一条接收记录。
- 发布后组织、角色变化不影响已生成接收记录。

### MOD-003 用户公告

页面：`views/announcement-user/index.vue`

| 层 | 交付物 | 职责 |
|---|---|---|
| page | `views/announcement-user/index.vue` | 用户公告列表、详情、确认。 |
| component | 公告详情、确认按钮 | 展示正文、阅读状态、确认状态。 |
| api.ts | `getMyAnnouncements`、`getMyAnnouncementDetail`、`readMyAnnouncement`、`confirmMyAnnouncement` | 用户端公告 API。 |
| api | `NoticeAnnouncementApi` 用户端方法 | 用户公告契约。 |
| controller | `NoticeAnnouncementController` 用户端方法 | 实现 `NoticeAnnouncementApi`，当前用户公告列表、详情、阅读、确认。 |
| service | 用户公告查询、阅读、确认方法 | 用户可见判断、阅读确认状态变更。 |
| mapper | recipient 查询和更新 | 只操作当前用户覆盖记录。 |
| convert | 用户公告 VO 转换 | 列表和详情展示。 |
| test | service 单测、页面 E2E | 只看覆盖公告、打开详情已读、确认公告。 |

完成标准：

- 用户只能看到覆盖自己的已发布、未下线、未过期公告。
- 打开详情后记录已读。
- 要求确认的公告必须阅读后确认。
- 已确认不可重复确认。
- 用户端接口只能访问当前用户自己的公告接收记录。

### MOD-004 公告系统消息提醒

| 层 | 交付物 | 职责 |
|---|---|---|
| service | 公告提醒发送方法 | 发布时按覆盖用户调用现有系统消息发送能力。 |
| command | `SendNoticeCommand` 组装 | 设置公告提醒业务编码、公告 ID、标题摘要、接收人。 |
| data link | `bizType + bizId` | 公告提醒使用 `bizType = notice.announcement.published`、`bizId = announcementId` 关联公告和发送记录；不要求记录每个站内消息 ID。 |
| page | 我的消息跳转适配 | 识别公告提醒并跳转公告详情。 |
| test | service 单测、前端跳转测试 | 提醒只做跳转，阅读确认仍归公告。 |

完成标准：

- 不为公告单独建设外部渠道推送。
- 提醒复用现有消息配置、渠道、发送记录、失败重试。
- 公告提醒使用 `notice.announcement.published` 作为固定业务编码，消息正文由公告服务直接传入现有发送入口。
- 公告正文、阅读、确认、统计不落到系统消息。

### MOD-005 菜单资源与页面注册

| 层 | 交付物 | 职责 |
|---|---|---|
| resource | 更新 `notice-common-menu.json` | 管理端通知中心新增公告管理；用户侧新增消息中心，包含我的消息和公告；接收设置保留隐藏辅助入口。 |
| frontend registry | 更新 `admin-pages.ts` / `admin.ts` | 注册公告管理、我的消息、用户公告、接收设置页面 key。 |
| page access | 菜单打开验证 | 菜单不空白、不 404、不缺组件。 |
| test | 菜单资源检查、页面 smoke | 菜单和页面 key 对齐。 |

完成标准：

- 不新增独立“系统通知”一级菜单。
- 管理端左侧通知中心只展示公告管理、消息配置、发送任务、渠道配置、发送记录、失败重试。
- 用户侧消息中心展示我的消息和公告。
- 接收设置不作为管理端通知中心或用户侧消息中心主菜单展示。
- 菜单 component 与前端页面 key 一致。
- 页面权限、按钮权限、后端接口权限一致。

### MOD-006 README 与能力说明

| 文档 | 更新内容 |
|---|---|
| 后端 README | 公告对象、发布范围、用户级接收记录、系统消息提醒复用、菜单权限、migration。 |
| 前端 README | 公告管理页面、用户公告页面、我的消息提醒跳转、页面 key。 |

完成标准：

- 能力说明覆盖新增公开能力。
- 不把 README 写成 PMO 规则源。

## 7. 验证计划

### 后端验证

- `mvn -pl mango-platform/mango-notice/mango-notice-core -am test`
- `mvn -pl mango-platform/mango-notice mango:check -Drule=all`
- 公告服务单测覆盖：
  - 发布范围不能为空。
  - 全员互斥。
  - 多范围去重。
  - 草稿不可见。
  - 用户只看覆盖公告。
  - 阅读和确认状态。
  - 系统消息提醒调用。

### 前端验证

- `pnpm --filter @mango/notice build`
- `pnpm --filter @mango/notice test`
- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`
- 公告管理页面：
  - 创建草稿。
  - 发布公告。
  - 范围校验。
  - 下线公告。
  - 查看统计。
- 用户公告页面：
  - 列表。
  - 详情。
  - 阅读。
  - 确认。
- 我的消息：
  - 公告提醒跳转公告详情。

### 页面验收

至少保留截图：

- 通知中心 / 公告管理列表。
- 公告发布表单。
- 公告统计。
- 用户公告列表。
- 用户公告详情。
- 我的消息公告提醒跳转。

### 文档与资源验证

- `node mango-pmo/tools/audit-module-readmes.mjs`
- `node mango-pmo/tools/audit-readme-source-facts.mjs`
- 菜单资源验证 Resource Registry 同步、菜单入口、页面 key 和按钮权限一致。
- 验收证据必须包含 `/auth/info` 菜单树摘要、页面截图、console/network 检查结果。

## 8. 任务拆分

| 顺序 | 任务 | 交付物 | 依赖 |
|---|---|---|---|
| 0 | BASE-000 设计与交付台账门禁 | 详细设计、交付台账 | 无 |
| 1 | BASE-001 数据库与持久化对象 | migration、entity、mapper、convert | 0 |
| 2 | BASE-002 API 契约与枚举 | API、enum、Command、Query、VO | 1 |
| 3 | BASE-003 公共服务方法 | 范围校验、用户解析、接收记录、统计、提醒发送公共方法 | 1、2 |
| 4 | BASE-004 前端类型、API 与页面内组件 | 类型、API、复用 `ParticipantSelector` 的范围选择适配、状态、统计组件 | 2 |
| 5 | MOD-001 管理端公告管理 | 页面、api、controller、service、mapper、convert、test | 1、2、3、4 |
| 6 | MOD-002 发布范围解析 | `NoticeRecipientResolver` 复用、去重、test | 3 |
| 7 | MOD-003 用户公告 | 页面、api、controller、service、mapper、convert、test | 1、2、3、4、6 |
| 8 | MOD-004 公告系统消息提醒 | service、SendNoticeCommand 组装、bizType/bizId 关联、我的消息跳转、test | 3、4、6 |
| 9 | MOD-005 菜单资源与页面注册 | notice 菜单资源、页面 key 注册、smoke | 5、7 |
| 10 | MOD-006 README 与能力说明 | 后端 README、前端 README | 1-9 |
| 11 | 验证与证据 | 后端测试、前端构建、README 审计、页面截图、交付记录 | 1-10 |

## 9. 风险与处理

| 风险 | 影响 | 处理 |
|---|---|---|
| 全员公告用户量大 | 发布时写入大量接收记录 | 首期同步展开，验收记录覆盖用户数量；超出本次性能范围时登记新 Issue。 |
| 公告提醒依赖系统消息配置 | 未配置时提醒发送失败 | 公告发布仍成功，提醒失败进入现有发送记录和失败重试治理。 |
| 已发布正文不可修改 | 管理员修错公告需要处理路径 | 首期通过下线后重新发布解决。 |
| 用户范围解析依赖身份、组织、角色数据 | 数据异常会影响发布 | 发布前校验可解析用户数量；无法解析时阻止发布并提示。 |

## 10. 完成标准

- 公告管理、用户公告、我的消息跳转主链路完成。
- 公告发布范围最终落到用户级接收记录。
- 阅读、确认、统计全部基于用户级接收记录。
- 系统消息提醒复用现有系统消息能力。
- 不出现独立“系统通知”模块。
- 后端、前端 README 已更新。
- 对应验证命令和页面截图完成。
