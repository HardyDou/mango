# mango-notice Sprint01：通知中心统一消息编排重构计划

文档状态：待审阅
设计输入：`mango-docs/designs/mango-notice多渠道通知中心设计说明书.md`

## 1. 背景

通知中心重新定位为统一消息编排中心，不是短信系统，也不是按系统消息、短信、邮件、微信拆开的渠道菜单。

新版后台菜单为：

```text
通知中心
├── 消息定义
├── 发送消息
├── 通知渠道
├── 接收设置
├── 发送记录
├── 失败重试
└── 系统监控
```

当前实现中的“业务通知配置、通知模板、通知计划、消息中心、通知偏好”等口径需要统一迁移到新版领域模型和菜单。

## 2. 目标

本 Sprint 完成通知中心新版产品口径落地：

- 后台菜单、路由、权限、页面名称按新版六菜单收敛。
- 消息定义成为核心入口，承载业务消息、参数、渠道启停和模板配置。
- 发送消息提供管理端人工触发入口，页面主体展示发送列表，右上角“发送消息”弹窗内按发送给、消息模板、自定义字段的顺序提交发送。
- 通知渠道按渠道类型使用结构化配置表单，底层保存 JSON。
- 接收设置支持用户是否接收某类消息。
- 发送记录展示所有发送历史和详情。
- 失败重试独立管理失败消息。
- 系统监控展示渠道状态、队列状态和发送统计。
- 用户侧系统消息保留在右上角小铃铛和用户消息入口，不作为后台配置一级菜单。

## 3. 范围

### 3.1 本 Sprint 做

| 分类 | 内容 |
|---|---|
| 文档基线 | 重写设计文档、Sprint 计划和交付台账 |
| UI 交互 | 明确六个菜单的列表、筛选、表单、详情、空态、错误态、操作反馈和布局规则 |
| 菜单权限 | 后端入库菜单改为消息定义、通知渠道、接收设置、发送记录、失败重试、系统监控 |
| 前端路由 | `packages/notice` 按新版六个页面重组页面和标题 |
| 消息定义 | 业务消息基础信息、参数定义、渠道模板、渠道启停、版本发布 |
| 发送消息 | 主体展示发送列表；右上角“发送消息”打开弹窗；弹窗先选接收用户，再选消息模板，最后按参数 schema 填自定义字段；实际渠道由消息配置和用户联系方式决定，最低发送系统消息 |
| 通知渠道 | SITE/SMS/EMAIL/WECHAT_OFFICIAL/WECOM/DINGTALK/FEISHU/WEBHOOK 配置表单 |
| 短信接入 | 短信渠道配置参考阿里云、腾讯云对接参数，并接入阿里云、腾讯云 sender |
| 接收设置 | 全局、业务域、单消息维度的用户接收控制 |
| 发送记录 | 列表、详情、渲染内容、发送参数、渠道响应、失败原因、耗时 |
| 失败重试 | 自动重试、手动重试、批量重试、忽略失败、重试历史 |
| 系统监控 | 渠道状态、队列堆积、发送量、成功率、失败率、渠道占比、业务占比 |
| 用户系统消息 | 保留右上角小铃铛、未读数、详情、已读、删除能力 |
| 安全 | 渠道敏感配置和记录快照输出脱敏，内部发送读取原始配置 |
| 测试 | 后端服务测试、前端构建、Playwright E2E、交付台账检查 |

### 3.2 本 Sprint 不做

| 不做项 | 说明 |
|---|---|
| IM 聊天 | 不做双向会话、群聊、在线状态业务模型 |
| 营销旅程 | 不做人群画像、活动编排、A/B 测试 |
| 财务计费 | 不做短信余额、邮件费用、渠道账单结算 |
| 三方账号全生命周期 | 不管理公众号粉丝、短信平台子账号、邮箱账号生命周期 |
| 内容风控平台 | 不做敏感词审核和反垃圾系统 |
| 通用 MQ 平台 | 通知中心只管理业务通知，不替代消息队列 |
| 存储加密 | 本 Sprint 先做输出脱敏，存储加密由后续安全任务单独确认 |

## 4. 影响模块

| 模块 | 影响 |
|---|---|
| `mango/mango-platform/mango-notice` | 后端通知中心主模块 |
| `mango/mango-platform/mango-authorization` | 菜单、权限、初始化数据 |
| `mango/mango-app/**` | 单体和微服务装配验证 |
| `mango-ui/packages/notice` | 新版通知中心业务包 |
| `mango-ui/packages/admin-pages` | notice 页面映射 |
| `mango-ui/apps/mango-admin` | 后台菜单、右上角小铃铛、E2E |
| `mango-infra-sensitive` | 敏感字段输出脱敏 |
| `mango-infra-realtime` | 用户系统消息在线提醒 |

## 5. 接口变化

### 5.1 业务发送 API

| 能力 | 路径 | 方法 |
|---|---|---|
| 发送业务消息 | `/notice/send` | POST |

核心入参：

| 字段 | 说明 |
|---|---|
| `bizCode` | 消息编码 |
| `params` | 业务参数 |
| `userIds` | 接收系统用户 ID 列表 |
| `sendMode` | 立即或定时 |
| `scheduledTime` | 定时发送时间 |

### 5.2 管理后台 API

| 菜单 | 路径 | 方法 |
|---|---|---|
| 消息定义 | `/notice/message-definitions`、`/notice/message-definitions/{id}` | GET/POST/PUT |
| 消息定义版本 | `/notice/message-definitions/{id}/versions`、`/notice/message-definitions/{id}/versions/{versionId}/publish` | GET/POST |
| 发送消息 | `/notice/send` | POST |
| 通知渠道 | `/notice/channels`、`/notice/channels/{id}` | GET/POST/PUT |
| 渠道测试 | `/notice/channels/{id}/test-send` | POST |
| 接收设置 | `/notice/receive-settings` | GET/PUT |
| 发送记录 | `/notice/send-records`、`/notice/send-records/{id}` | GET |
| 失败重试 | `/notice/retry-records`、`/notice/retry-records/{id}/retry`、`/notice/retry-records/ignore` | GET/POST |
| 系统监控 | `/notice/monitor/summary`、`/notice/monitor/channels`、`/notice/monitor/queues` | GET |

### 5.3 用户侧系统消息 API

| 能力 | 路径 | 方法 |
|---|---|---|
| 我的系统消息 | `/notice/site/my/messages` | GET |
| 我的系统消息详情 | `/notice/site/my/messages/{id}` | GET |
| 我的未读数 | `/notice/site/my/unread-count` | GET |
| 标记已读 | `/notice/site/my/messages/{id}/read` | POST |
| 批量已读 | `/notice/site/my/messages/read-batch` | POST |
| 全部已读 | `/notice/site/my/messages/read-all` | POST |
| 删除系统消息 | `/notice/site/my/messages/{id}/delete` | POST |

## 6. 数据变化

实施阶段新增 migration，不修改已执行 migration。

目标表：

| 表 | 说明 |
|---|---|
| `notice_message_definition` | 消息定义主表 |
| `notice_message_definition_version` | 消息定义版本表 |
| `notice_message_channel_template` | 版本内渠道模板 |
| `notice_channel_config` | 通知渠道配置 |
| `notice_receive_setting` | 接收设置 |
| `notice_send_task` | 发送任务，后台不作为一级菜单 |
| `notice_send_record` | 发送记录 |
| `notice_retry_record` | 失败重试记录 |
| `notice_retry_log` | 重试日志 |
| `notice_site_message` | 用户系统消息 |
| `notice_callback_log` | 三方回调日志 |
| `notice_monitor_metric` | 监控指标快照 |
| `notice_audit_log` | 操作审计 |

兼容映射：

| 旧概念 | 新概念 |
|---|---|
| 业务通知配置、通知模板 | 消息定义 |
| 业务类型编码 | 消息编码 |
| 通知计划 | 内部发送任务 |
| 通知偏好 | 接收设置 |
| 消息中心后台菜单 | 用户侧小铃铛和系统消息入口 |

## 7. 前端变化

目标结构：

```text
mango-ui/packages/notice/
├── api
├── components
│   ├── NoticeBell
│   ├── NoticeDetailDialog
│   ├── channel-forms
│   ├── template-editor
│   └── metric-widgets
├── realtime
├── types
└── views
    ├── message-definition
    ├── channel
    ├── receive-setting
    ├── send-record
    ├── retry
    └── monitor
```

页面：

| 页面 | 能力 |
|---|---|
| 消息定义 | 业务域、消息编码、消息名称、参数定义、渠道模板、启停、版本发布 |
| 通知渠道 | 渠道账号配置、供应商配置、权重轮换、启停、限流、超时、失败切换 |
| 接收设置 | 全局、业务域、单消息维度接收控制 |
| 发送记录 | 发送历史、详情、渲染内容、发送参数、渠道响应、失败原因、耗时 |
| 失败重试 | 失败池、自动重试、手动重试、批量重试、忽略失败 |
| 系统监控 | 渠道状态、队列监控、统计分析 |

UI 交互要求：

| 要求 | 说明 |
|---|---|
| 标准布局 | 每个页面必须有清晰标题区、筛选区、内容区和操作区 |
| 边距对齐 | 页面左、上边距与 Mango 后台其他标准页面一致 |
| 表单分组 | 新增和编辑表单按业务含义分组，复杂表单使用步骤条、Tab 或折叠面板 |
| 禁止乱布局 | 不允许字段无序堆叠、按钮散落、卡片套卡片、表格塞大段内容 |
| ASCII 线框 | 消息定义和通知渠道实现必须参考设计文档 ASCII 布局，不允许改成创新式看板或营销式页面 |
| 结构化配置 | 参数、渠道配置、限流、超时、重试等均使用结构化控件，JSON 只作为只读结果查看或高级辅助 |
| 详情承载 | 发送记录、失败重试等复杂详情使用抽屉或分组详情 |
| 状态反馈 | 页面必须有加载态、空态、错误态、保存成功和保存失败反馈 |
| 操作确认 | 发布、批量重试、忽略失败、删除等高风险操作必须二次确认 |
| 中文文案 | 字段标签和按钮使用业务中文，不把数据库字段名暴露给用户 |
| E2E 约束 | Playwright 需要覆盖关键页面入口、表单保存、详情查看和错误反馈 |

## 8. 权限变化

| 菜单 | 权限码 |
|---|---|
| 消息定义 | `notice:message-definition:view/create/edit/publish/enable/delete` |
| 通知渠道 | `notice:channel:view/create/edit/enable` |
| 接收设置 | `notice:receive-setting:view/edit` |
| 发送记录 | `notice:send-record:view/export` |
| 失败重试 | `notice:retry:view/retry/ignore` |
| 系统监控 | `notice:monitor:view` |
| 用户系统消息 | `notice:site:view/read/delete` |

## 9. 验证方式

文档阶段：

```bash
node mango-pmo/tools/delivery-contract-check.mjs \
  --design mango-docs/designs/mango-notice多渠道通知中心设计说明书.md \
  --ledger mango-docs/plans/2026-05-25-mango-notice-sprint-01-ledger.md \
  --mode plan
```

实施阶段后端：

```bash
cd mango
mvn -pl mango-platform/mango-notice -am test
mvn -pl mango-platform/mango-notice -am verify
mvn mango:check -Drule=all
```

实施阶段前端：

```bash
cd mango-ui
pnpm test
pnpm build
cd apps/mango-admin
pnpm playwright test
```

实施阶段交付台账：

```bash
node mango-pmo/tools/delivery-contract-check.mjs \
  --design mango-docs/designs/mango-notice多渠道通知中心设计说明书.md \
  --ledger mango-docs/plans/2026-05-25-mango-notice-sprint-01-ledger.md \
  --mode verify
```

## 10. 完成标准

文档审阅阶段：

1. 设计文档、Sprint 计划、设计台账和实施台账均使用新版菜单。
2. 旧菜单口径不再作为目标方案出现。
3. 交付台账拆分到菜单、页面、API、数据、权限、测试。
4. 设计文档逐项描述消息定义、通知渠道、接收设置、发送记录、失败重试、系统监控和小铃铛的交互过程。
5. 设计文档明确 UI 布局、表单分组、详情承载、空态、错误态和操作反馈要求。
6. `delivery-contract-check --mode plan` 通过。

实施交付阶段：

1. 后台只展示消息定义、通知渠道、接收设置、发送记录、失败重试、系统监控。
2. 消息定义可配置参数和多渠道模板。
3. 通知渠道按类型使用结构化表单。
4. 接收设置影响发送决策。
5. 发送记录和失败重试可追踪、可操作。
6. 系统监控可展示渠道、队列和统计。
7. 用户侧系统消息入口可用。
8. 消息定义列表包含同步状态、生效版本、最后发布和更新时间；同步状态参考工作流流程定义列表展示已同步和待发布。
9. 消息定义操作包含详情、编辑、发布、更多；编辑和配置渠道合并为编辑，不提供测试发送。
10. 消息定义只要修改就保存为草稿，发布后才影响线上发送。
11. 通知渠道去掉默认通道概念，AUTO 模式按启用且配置完整的通道做权重轮换。
12. 消息定义支持指定具体通道，默认使用 AUTO。
13. 通道失败后当前通道最多尝试 3 次，仍失败则切换同类型下一个可用通道。
14. 无可用通道时记录 `CHANNEL_UNAVAILABLE`，指定通道停用时记录 `CHANNEL_DISABLED`。
15. 消息定义和通知渠道页面均采用传统后台布局，边距对齐，标题区、筛选区、表格区、分页区、详情/编辑抽屉完整，并严格参考设计文档 ASCII 线框。
16. 新增和编辑表单均按业务分组，不出现大 JSON 输入框替代结构化表单。
17. 详情、快照、响应等复杂内容使用抽屉或分组只读展示，不破坏列表布局。
18. 每个页面具备加载态、空态、错误态和操作反馈。
19. 后端、前端、E2E 和台账 verify 通过。

## 11. 风险与限制

| 风险 | 缓解 |
|---|---|
| 旧代码和新版领域词不一致 | 以新版文档和台账为基准，实施时做兼容迁移 |
| 菜单已在本地库执行旧 migration | 新增菜单修正 migration，并提供本地数据修正验证 |
| 短信供应商差异大 | 阿里云和腾讯云先作为明确供应商接入，其他供应商走 SPI |
| 接收设置影响发送决策 | 先明确过滤规则和取消记录策略，再写服务测试 |
| 敏感配置泄露 | 使用 `mango-infra-sensitive` 做输出脱敏，内部发送保留原始配置 |
| 系统监控依赖统计口径 | 以发送记录和重试记录聚合，必要时落监控快照表 |

## 12. PMO 加载记录

- `mango-pmo/rules/00-dev-flow.md`
- `mango-pmo/rules/01-delivery-contract.md`
- `mango-pmo/agents/01-pm-agent.md`
- `mango-pmo/rules/product/01-prd-template.md`
- `mango-pmo/rules/product/02-sprint.md`
- `mango-pmo/rules/backend/10-dev-flow.md`
- `mango-pmo/rules/backend/01-code.md`
- `mango-pmo/rules/backend/02-naming.md`
- `mango-pmo/rules/backend/05-module.md`
- `mango-pmo/rules/backend/03-api.md`
- `mango-pmo/rules/backend/04-db.md`
- `mango-pmo/rules/backend/08-test.md`
- `mango-pmo/rules/frontend/05-dev-flow.md`
- `mango-pmo/rules/frontend/01-vue-code.md`
- `mango-pmo/rules/frontend/06-monorepo-architecture.md`
- `mango-pmo/rules/frontend/04-test.md`
