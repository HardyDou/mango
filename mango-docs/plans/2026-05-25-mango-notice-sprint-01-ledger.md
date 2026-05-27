| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| NOTICE-S01-001 | 用户新版方案 | 重写通知中心设计文档 | 以统一消息编排中心为新基线，替换旧通知模板、通知计划、消息中心、通知偏好口径 | 设计文档 | 文档审阅 | IN_PROGRESS | `mango-docs/designs/mango-notice多渠道通知中心设计说明书.md` |
| NOTICE-S01-002 | 用户新版方案 | 重写 Sprint 计划 | Sprint 目标调整为新版六菜单和统一消息编排重构 | Sprint 计划 | 文档审阅 | IN_PROGRESS | `mango-docs/plans/2026-05-25-mango-notice-sprint-01.md` |
| NOTICE-S01-003 | 用户新版方案 | 重写设计台账和实施台账 | 台账从新版设计方案抽取原子验收项，状态回到待审阅和待实施 | 设计台账、实施台账 | `delivery-contract-check --mode plan` | IN_PROGRESS | `mango-docs/plans/2026-05-25-mango-notice-design-ledger.md`；`mango-docs/plans/2026-05-25-mango-notice-sprint-01-ledger.md` |
| NOTICE-S01-004 | 用户追加要求 | 完成标准增加 UI 交互要求，布局清晰且符合规范 | Sprint 计划和设计文档明确标题区、筛选区、内容区、操作区、边距、表单分组、详情抽屉、空态、错误态和操作反馈 | UI 交互完成标准 | 文档审阅 + E2E 检查 | IN_PROGRESS | `mango-docs/designs/mango-notice多渠道通知中心设计说明书.md`；`mango-docs/plans/2026-05-25-mango-notice-sprint-01.md` |
| NOTICE-S01-005 | 用户新版方案 | 后台菜单改为消息定义、通知渠道、接收设置、发送记录、失败重试、系统监控 | 新增授权菜单 migration 修正旧菜单；前端页面映射同步新版路由 | 菜单 migration、权限、页面映射 | 数据库菜单检查 + E2E 导航 | TODO | 待实施 |
| NOTICE-S01-006 | 用户新版方案 | 消息定义页面 | 原业务通知配置和通知模板收敛为消息定义，支持业务域、消息编码、消息名称、描述、启用状态、是否允许用户关闭 | 后端 API、前端消息定义页 | 服务测试 + 前端构建 + E2E | TODO | 待实施 |
| NOTICE-S01-007 | 用户新版方案 | 消息定义参数使用结构化表单 | 参数定义维护参数名、参数说明、示例值、是否必填，不以单个 JSON 输入框作为主入口 | 参数编辑组件、保存 API | 组件测试 + E2E 表单保存 | TODO | 待实施 |
| NOTICE-S01-008 | 用户确认 | 消息定义列表参考工作流流程定义列表 | 列表增加同步状态、生效版本、最后发布；同步状态展示已同步和待发布，并提示未发布变更原因 | 消息定义列表字段和交互 | 前端构建 + E2E | TODO | 待实施 |
| NOTICE-S01-009 | 用户新版方案 | 消息定义渠道配置使用渠道卡片 | 每个渠道一张卡片，维护启用状态、模板内容、第三方模板 ID、变量映射，底层保存结构化配置 | 渠道模板卡片、版本草稿 | 前端构建 + E2E | TODO | 待实施 |
| NOTICE-S01-010 | 用户确认 | 消息定义编辑和配置渠道合并为编辑，增加详情，去掉测试发送 | 行操作为详情、编辑、发布、更多；编辑入口承载基础信息、参数定义和渠道配置；详情只读展示 | 消息定义行操作 | 前端构建 + E2E | TODO | 待实施 |
| NOTICE-S01-011 | 用户确认 | 消息定义只要修改就需要发布 | 任意修改先保存草稿，发布后成为新生效版本；列表显示待发布状态 | 版本表、发布服务、前端版本操作 | Maven 测试 + E2E 发布 | TODO | 待实施 |
| NOTICE-S01-011A | 用户追加要求 | 增加发送消息菜单并完善发送功能 | 新增发送消息菜单；选择已启用业务消息后按参数 schema 生成结构化表单；填写接收对象并调用 `/notice/send`；发送记录按渠道和接收人记录明细 | 菜单 migration、发送消息页面、统一发送接口联调、E2E | 前端构建 + E2E 发送请求断言 | DONE | `mango-ui/packages/notice/src/views/send-message/index.vue`；`mango/mango-platform/mango-authorization/mango-authorization-core/src/main/resources/db/migration/authorization/V33__notice_send_message_menu.sql`；`mango-ui/apps/mango-admin/e2e/specs/notice-site-message.spec.ts` |
| NOTICE-S01-012 | 用户新版方案 | 通知渠道支持系统消息、短信、邮件、微信公众号、企业微信、钉钉、飞书、Webhook | 渠道配置按类型和供应商展示不同表单，底层仍保存 JSON；系统消息内置默认通道并维护投递运行参数；页面使用传统后台布局并参考 ASCII 线框 | 通知渠道 API、channel forms、默认系统消息通道 migration、传统列表页 | Maven 测试 + 前端构建 | TODO | 待实施 |
| NOTICE-S01-013 | 用户确认 | 通知渠道去掉默认通道，统一采用 AUTO 权重轮换 | 每个渠道类型允许多个通道；AUTO 查询启用且配置完整通道并按权重轮换；消息定义可指定具体通道 | 通道选择策略 | Maven 测试 + E2E | TODO | 待实施 |
| NOTICE-S01-014 | 用户确认 | 通道失败先尝试 3 次，仍失败切换通道 | 当前通道失败最多尝试 3 次；仍失败切换同类型下一个可用通道；全部失败进入失败重试 | 通道失败切换服务 | Maven 测试 | TODO | 待实施 |
| NOTICE-S01-015 | 用户确认 | 定义通道失败码 | 覆盖 CHANNEL_UNAVAILABLE、CHANNEL_DISABLED、CHANNEL_CONFIG_INVALID、TEMPLATE_INVALID、RECIPIENT_INVALID、PROVIDER_REJECTED、PROVIDER_TIMEOUT、PROVIDER_ERROR、RATE_LIMITED、SEND_EXCEPTION | 失败码枚举和记录 | Maven 测试 | TODO | 待实施 |
| NOTICE-S01-016 | 用户新版方案 | 短信渠道直接接入阿里云和腾讯云 | 新增或调整短信 sender，阿里云字段包含 AccessKey ID、AccessKey Secret、Region、Endpoint、签名、模板 Code、上行扩展码；腾讯云字段包含 SecretId、SecretKey、Region、Endpoint、SmsSdkAppId、签名、模板 ID、扩展码、SessionContext | 阿里云短信 sender、腾讯云短信 sender、配置表单 | sender 测试 + 配置 E2E | TODO | 待实施 |
| NOTICE-S01-017 | 用户新版方案 | 邮件渠道结构化配置 | 供应商不是 SMTP；第一版只开放 CUSTOM_SMTP 和 ALIYUN_DM；SMTP 主机、端口、安全协议、账号、密码、发件人为 CUSTOM_SMTP 配置项；暂未明确接入方式的平台不展示，后续需要时再补设计、字段和 sender 适配 | 邮件配置表单、sender 适配 | sender 测试 + 前端构建 | TODO | 待实施 |
| NOTICE-S01-018 | 用户新版方案 | 微信公众号结构化配置 | AppId、Secret、模板配置、Token、EncodingAESKey 使用表单维护 | 微信配置表单、sender 适配 | sender 测试 + 前端构建 | TODO | 待实施 |
| NOTICE-S01-019 | 用户新版方案 | 企业微信、钉钉、飞书、Webhook 结构化配置 | 按官方接入参数建立表单，渠道 sender 通过 SPI 隔离 | 渠道表单、sender 适配 | sender 测试 + 前端构建 | TODO | 待实施 |
| NOTICE-S01-020 | 用户新版方案 | 接收设置支持全局关闭 | 支持关闭所有短信、关闭所有邮件等全局设置 | 接收设置 API、页面 | 服务测试 + E2E | TODO | 待实施 |
| NOTICE-S01-021 | 用户新版方案 | 接收设置支持业务域关闭 | 支持关闭保函短信、关闭基础邮件等业务域设置 | 接收设置模型、页面筛选 | 服务测试 + E2E | TODO | 待实施 |
| NOTICE-S01-022 | 用户新版方案 | 接收设置支持单消息关闭 | 支持关闭某个消息定义下的某渠道通知，并受“是否允许用户关闭”限制 | 接收设置模型、发送前过滤 | 服务测试 + E2E | TODO | 待实施 |
| NOTICE-S01-023 | 用户新版方案 | 发送记录列表 | 展示消息名称、消息编码、业务单号、渠道、接收人、发送状态、发送时间 | 发送记录页面、查询 API | 服务测试 + E2E | TODO | 待实施 |
| NOTICE-S01-024 | 用户新版方案 | 发送记录详情 | 支持查看渲染内容、发送参数、渠道响应、失败原因、耗时、模板版本、重试历史 | 发送记录详情页、详情 API | 服务测试 + E2E | TODO | 待实施 |
| NOTICE-S01-025 | 用户新版方案 | 失败重试独立菜单 | 独立展示失败消息，支持自动重试、手动重试、批量重试、忽略失败 | 失败重试页面、重试 API | 服务测试 + E2E | TODO | 待实施 |
| NOTICE-S01-026 | 用户新版方案 | 失败重试状态机 | 支持等待重试、重试中、重试成功、重试失败、已忽略、达到上限 | 重试记录表、状态机服务 | Maven 测试 | TODO | 待实施 |
| NOTICE-S01-027 | 用户新版方案 | 系统监控渠道状态 | 展示短信、邮件、公众号、企业微信、钉钉、飞书、Webhook 是否正常 | 监控 API、监控页面 | 服务测试 + E2E | TODO | 待实施 |
| NOTICE-S01-028 | 用户新版方案 | 系统监控队列状态 | 展示待发送数量、失败数量、消费速度、消息堆积、最老待处理时间 | 队列监控 API、页面 | 服务测试 + E2E | TODO | 待实施 |
| NOTICE-S01-029 | 用户新版方案 | 系统监控统计分析 | 展示发送量、成功率、失败率、渠道占比、业务占比、平均耗时、P95 耗时 | 统计 API、页面组件 | 服务测试 + E2E | TODO | 待实施 |
| NOTICE-S01-030 | 用户新版方案 | 右上角小铃铛保留用户系统消息入口 | 系统消息是用户入口，不作为后台配置一级菜单；小铃铛保持无背景同风格 | NoticeBell、用户系统消息 API | 组件测试 + E2E | TODO | 待实施 |
| NOTICE-S01-031 | 安全要求 | 渠道敏感配置输出脱敏 | 使用 `mango-infra-sensitive` 注解或统一输出层脱敏，内部发送读取原始配置 | VO 注解、序列化测试 | 安全测试 + Maven 测试 | TODO | 待实施 |
| NOTICE-S01-032 | PMO 要求 | 后端验证 | 按通知中心改动范围执行 Maven test、verify 和 mango:check | 验证记录 | 命令通过 | TODO | 待实施 |
| NOTICE-S01-033 | PMO 要求 | 前端验证 | 执行 notice 包测试、mango-admin 构建和 Playwright E2E | 验证记录 | 命令通过 | TODO | 待实施 |
| NOTICE-S01-034 | PMO 要求 | 交付台账验证 | 实施完成后执行 delivery-contract-check verify，所有本 Sprint 项为 DONE 或 EXCEPTION | 台账检查记录 | `delivery-contract-check --mode verify` | TODO | 待实施 |
