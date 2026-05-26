| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| NOTICE-DES-001 | 用户新版方案 | 通知中心重新定位为统一消息编排中心 | 设计文档第一节明确通知中心不是短信系统，而是业务消息编排、模板渲染、接收控制、发送记录、失败重试和监控中心 | 设计定位 | 文档审阅 | IN_PROGRESS | `mango-docs/designs/mango-notice多渠道通知中心设计说明书.md` |
| NOTICE-DES-002 | 用户新版方案 | 一个业务消息对应一套参数、多渠道、多模板、统一流程 | 使用消息定义和消息定义版本承载业务消息、参数定义、渠道启停、模板内容和运行时策略 | 核心模型设计 | 文档审阅 | IN_PROGRESS | `mango-docs/designs/mango-notice多渠道通知中心设计说明书.md` |
| NOTICE-DES-003 | 用户新版方案 | 菜单改为消息定义、通知渠道、接收设置、发送记录、失败重试、系统监控 | 后台一级菜单只保留六项；站内信、短信、邮件、微信等作为渠道类型，不作为一级菜单 | 菜单设计 | 文档审阅 | IN_PROGRESS | `mango-docs/designs/mango-notice多渠道通知中心设计说明书.md` |
| NOTICE-DES-004 | 用户新版方案 | 消息定义作为核心菜单 | 消息定义负责业务域、消息编码、消息名称、参数定义、渠道启停和渠道模板 | 消息定义设计 | 文档审阅 | IN_PROGRESS | `mango-docs/designs/mango-notice多渠道通知中心设计说明书.md` |
| NOTICE-DES-005 | 用户新版方案 | 通知渠道支持站内信、短信、邮件、微信公众号、企业微信、钉钉、飞书、Webhook | 通知渠道按类型和供应商展示结构化配置表单，底层保存 JSON，支持权重轮换、限流、超时、三次重试和失败切换 | 通知渠道设计 | 文档审阅 | IN_PROGRESS | `mango-docs/designs/mango-notice多渠道通知中心设计说明书.md` |
| NOTICE-DES-006 | 用户新版方案 | 短信参考阿里云、腾讯云对接要求并直接接入 | 短信渠道区分阿里云和腾讯云字段，覆盖 AccessKey/SecretId、Secret、Region、Endpoint、签名、模板标识、AppId 和扩展字段 | 短信接入设计 | 文档审阅 | IN_PROGRESS | `mango-docs/designs/mango-notice多渠道通知中心设计说明书.md` |
| NOTICE-DES-007 | 用户新版方案 | 接收设置控制用户是否接收某类消息 | 接收设置支持全局、业务域、单消息维度，并进入发送前检查链路 | 接收设置设计 | 文档审阅 | IN_PROGRESS | `mango-docs/designs/mango-notice多渠道通知中心设计说明书.md` |
| NOTICE-DES-008 | 用户新版方案 | 发送记录记录所有消息发送历史 | 发送记录保存消息、业务单号、渠道、接收人、状态、渲染内容、发送参数、渠道响应、失败原因和耗时 | 发送记录设计 | 文档审阅 | IN_PROGRESS | `mango-docs/designs/mango-notice多渠道通知中心设计说明书.md` |
| NOTICE-DES-009 | 用户新版方案 | 失败重试独立管理发送失败消息 | 失败重试支持自动重试、手动重试、批量重试、忽略失败和重试历史 | 失败重试设计 | 文档审阅 | IN_PROGRESS | `mango-docs/designs/mango-notice多渠道通知中心设计说明书.md` |
| NOTICE-DES-010 | 用户新版方案 | 系统监控展示渠道、队列和统计 | 系统监控覆盖渠道状态、待发送数量、失败数量、消费速度、消息堆积、发送量、成功率、失败率、渠道占比和业务占比 | 系统监控设计 | 文档审阅 | IN_PROGRESS | `mango-docs/designs/mango-notice多渠道通知中心设计说明书.md` |
| NOTICE-DES-011 | PMO 要求 | 明确接口变化 | 设计文档列出业务发送、管理后台和用户侧站内消息 API | API 设计 | 文档审阅 | IN_PROGRESS | `mango-docs/designs/mango-notice多渠道通知中心设计说明书.md` |
| NOTICE-DES-012 | PMO 要求 | 明确数据变化 | 设计文档列出消息定义、版本、渠道模板、渠道配置、接收设置、发送记录、失败重试、站内消息和监控表 | 数据设计 | 文档审阅 | IN_PROGRESS | `mango-docs/designs/mango-notice多渠道通知中心设计说明书.md` |
| NOTICE-DES-013 | PMO 要求 | 明确前端、权限和测试范围 | 设计文档列出新版路由、前端包结构、权限码、后端测试、前端测试和 E2E 范围 | 前端权限测试设计 | 文档审阅 | IN_PROGRESS | `mango-docs/designs/mango-notice多渠道通知中心设计说明书.md` |
| NOTICE-DES-014 | 用户追加要求 | 完成标准增加 UI 交互要求，布局清晰且符合规范 | 设计文档新增 UI 交互与布局规范，约束标题区、筛选区、内容区、操作区、边距、表单分组、空态、错误态和操作反馈 | UI 布局设计 | 文档审阅 | IN_PROGRESS | `mango-docs/designs/mango-notice多渠道通知中心设计说明书.md` |
| NOTICE-DES-015 | 用户追加要求 | 设计文档描述每个功能的交互过程细节，防止 AI 写代码时胡乱搞 | 消息定义、通知渠道、接收设置、发送记录、失败重试、系统监控和小铃铛均写明列表、筛选、编辑、详情和操作规则 | 功能交互设计 | 文档审阅 | IN_PROGRESS | `mango-docs/designs/mango-notice多渠道通知中心设计说明书.md` |
| NOTICE-DES-016 | 用户确认 | 消息定义列表参考工作流流程定义列表 | 消息定义列表增加同步状态、生效版本、最后发布；同步状态展示已同步和待发布 | 消息定义列表设计 | 文档审阅 | IN_PROGRESS | `mango-docs/designs/mango-notice多渠道通知中心设计说明书.md` |
| NOTICE-DES-017 | 用户确认 | 编辑和配置渠道合并为编辑，增加详情，去掉测试发送，只要修改就需要发布 | 消息定义操作改为详情、编辑、发布、更多；任意修改先保存草稿，发布后生效 | 消息定义操作设计 | 文档审阅 | IN_PROGRESS | `mango-docs/designs/mango-notice多渠道通知中心设计说明书.md` |
| NOTICE-DES-018 | 用户确认 | 通知渠道去掉默认通道，统一采用路由策略，第一版只做权重轮换 | AUTO 模式查询启用且配置完整的通道，按权重轮换；消息定义可高级绑定具体通道 | 通知渠道 AUTO 策略设计 | 文档审阅 | IN_PROGRESS | `mango-docs/designs/mango-notice多渠道通知中心设计说明书.md` |
| NOTICE-DES-019 | 用户确认 | 通道失败先定义失败码，尝试 3 次后切换通道 | 设计文档定义失败码；当前通道失败最多尝试 3 次，仍失败切换同类型下一个可用通道 | 通道失败处理设计 | 文档审阅 | IN_PROGRESS | `mango-docs/designs/mango-notice多渠道通知中心设计说明书.md` |
| NOTICE-DES-020 | 用户确认 | UI 使用传统布局，不创新，布局合理 | 消息定义和通知渠道均采用标题区、筛选区、表格区、分页区、详情/编辑抽屉的传统后台布局，并补充 ASCII 线框约束实现 | UI 布局设计 | 文档审阅 | IN_PROGRESS | `mango-docs/designs/mango-notice多渠道通知中心设计说明书.md` |
| NOTICE-DES-021 | 用户确认 | 文档写完后先让用户审阅 | 本轮只重写文档和台账，不继续实施代码 | 审阅门禁 | 用户确认 | IN_PROGRESS | 用户消息“文档写完后让我审阅” |
