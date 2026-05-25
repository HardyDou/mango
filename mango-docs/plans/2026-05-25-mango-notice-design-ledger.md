| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| NOTICE-DES-001 | 用户要求 | 设计多渠道通知中心 | 主模块命名 `mango-notice` | 设计文档 | 文档审阅 | DONE | mango-docs/designs/mango-notice多渠道通知中心设计说明书.md |
| NOTICE-DES-002 | 用户要求 | 从 `mango-biz-notification` 更名 | 旧模块迁移到 `mango-notice`，不长期双实现 | 迁移设计 | 文档审阅 | DONE | mango-docs/designs/mango-notice多渠道通知中心设计说明书.md |
| NOTICE-DES-003 | 用户要求 | 支持站内信、短信、微信、邮件等渠道 | 渠道统一 `mango-notice-channel-*` | 渠道规划 | 文档审阅 | DONE | mango-docs/designs/mango-notice多渠道通知中心设计说明书.md |
| NOTICE-DES-004 | 用户要求 | 管理平台菜单规划 | 一级菜单“通知中心”，二级菜单按业务入口组织 | 菜单规划 | 文档审阅 | DONE | mango-docs/designs/mango-notice多渠道通知中心设计说明书.md |
| NOTICE-DES-005 | 用户要求 | 明确依赖技术栈 | 按后端、Mango 内部、渠道、前端分层 | 技术栈设计 | 文档审阅 | DONE | mango-docs/designs/mango-notice多渠道通知中心设计说明书.md |
| NOTICE-DES-006 | PMO 设计要求 | 明确模块边界 | `core` 编排，`channel-*` 发送，infra 只做协议 | 边界设计 | 文档审阅 | DONE | mango-docs/designs/mango-notice多渠道通知中心设计说明书.md |
| NOTICE-DES-007 | PMO 设计要求 | 明确接口变化 | 新增 `NoticeApi` 和 `/notice/*` API | API 设计 | 文档审阅 | DONE | mango-docs/designs/mango-notice多渠道通知中心设计说明书.md |
| NOTICE-DES-008 | PMO 设计要求 | 明确数据变化 | 新增 notice 系列表，旧 `sys_notification` 迁移 | 数据库设计 | 文档审阅 | DONE | mango-docs/designs/mango-notice多渠道通知中心设计说明书.md |
| NOTICE-DES-009 | PMO 设计要求 | 明确测试范围 | 按 service、SPI、channel、前端、E2E 分层验证 | 测试策略 | 文档审阅 | DONE | mango-docs/designs/mango-notice多渠道通知中心设计说明书.md |
| NOTICE-DES-010 | 架构设计要求 | 输出 ADR | 记录命名、渠道 SPI、站内信边界、Outbox 调度等决策 | ADR | 文档审阅 | DONE | mango-docs/designs/mango-notice多渠道通知中心设计说明书.md |
| NOTICE-DES-011 | 用户补充 | 异步发送、削峰、定时发送简单高效，支持单机/集群/分布式 | 默认使用 `mango-infra-kv` Outbox + 轻量 worker，不直接绑定 PowerJob | 异步调度设计 | 文档审阅 | DONE | mango-docs/designs/mango-notice多渠道通知中心设计说明书.md |
