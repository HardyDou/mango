# 通知中心邮件富文本模板交付契约

## 1. 目标

将通知中心邮件模板设计为富文本 HTML 形式，模板视觉层次比普通文本更完整，并验证通知中心邮件发送链路可以发送富文本内容。

## 2. 范围

- 邮件通道发送器支持按 `text/html` MIME 发送内容。
- 初始化两套基础邮件模板：注册成功邮件、安全提醒邮件。
- 管理后台邮件模板编辑继续使用富文本编辑器。
- 固化 admin 接收邮箱和手机号初始化数据。
- 使用本地独立库 `mango_notice_send_flow` 验证 Flyway migration、登录、发送接口和发送记录。

## 3. 不做什么

- 不把真实 SMTP 授权码写入仓库。
- 不修改已经执行过的历史 migration。
- 不重构模板版本模型和渠道配置 UI。

## 4. 设计输入

- 用户要求：将邮件模板设计成富文本形式，样式要复杂一点。
- 用户要求：admin 邮箱 `1012404303@qq.com`、手机号 `18701445644` 固化到 SQL。
- 已有通知中心发送设计：业务类型 + 渠道模板 + 接收人 + outbox 异步发送。

## 5. 设计说明

### 5.1 影响模块

- `mango-notice-channel-email`：邮件发送器。
- `mango-notice-core`：邮件模板、接收账号 migration。
- `mango-identity-core`：admin 联系方式 migration。
- `mango-monolith-app`：本地初始化数据。
- `mango-ui/packages/notice`：邮件模板编辑器沿用富文本控件。

### 5.2 接口变化

无新增接口。继续使用 `POST /notice/send` 发送通知。

### 5.3 数据变化

- 新增 `identity/V2__update_admin_contact.sql` 固化 admin 邮箱和手机号。
- 新增 `notice/V10__seed_admin_recipient_account.sql` 固化 admin 邮箱、手机号接收账号。
- 新增 `notice/V11__seed_email_rich_templates.sql` 固化两套邮件 HTML 模板。

### 5.4 菜单/页面/权限变化

无新增菜单和权限。邮件模板配置页面继续通过消息配置模块进入，邮件内容使用富文本编辑器。

### 5.5 测试范围

- 邮件发送器单元测试覆盖缺少邮箱、缺少配置、SMTP 成功发送。
- 本地后端启动验证 Flyway migration 执行成功。
- 调用登录和发送接口，验证发送记录为 `SUCCESS`，渲染内容为 HTML。

## 6. 风险与限制

- 当前本地 SMTP 授权码仅保存在本地数据库，用于本机联调，不进入仓库。
- 邮件富文本使用 inline style 和 table 结构提升邮件客户端兼容性，不依赖外部 CSS。

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| NOTICE-EMAIL-001 | 用户要求 | 邮件模板为富文本形式，样式复杂一点 | 注册成功、安全提醒模板使用 HTML table、inline style、摘要卡、步骤区、提示区和 CTA | `V11__seed_email_rich_templates.sql` | 查询模板长度和内容前缀，发送记录内容为 HTML | DONE | `mango/mango-platform/mango-notice/mango-notice-core/src/main/resources/db/migration/notice/V11__seed_email_rich_templates.sql` |
| NOTICE-EMAIL-002 | 用户要求 | 通知中心邮件要真实发送 | 邮件通道通过 SMTP 发送 `text/html` MIME 邮件，支持 SSL 465 和 AUTH LOGIN | `EmailNoticeChannelSender.java` | `mvn -pl mango-platform/mango-notice/mango-notice-channel-email test`，系统发送记录 `SUCCESS` | DONE | `mango/mango-platform/mango-notice/mango-notice-channel-email/src/main/java/io/mango/notice/channel/email/EmailNoticeChannelSender.java` |
| NOTICE-EMAIL-003 | 用户要求 | admin 邮箱和手机号固化到 SQL | 身份模块更新用户联系方式，通知模块 seed 接收账号 | `V2__update_admin_contact.sql`; `V10__seed_admin_recipient_account.sql`; `data.sql` | Flyway history 显示 identity V2、notice V10 成功，DB 查询 admin 联系方式 | DONE | `mango/mango-platform/mango-identity/mango-identity-core/src/main/resources/db/migration/identity/V2__update_admin_contact.sql`; `mango/mango-platform/mango-notice/mango-notice-core/src/main/resources/db/migration/notice/V10__seed_admin_recipient_account.sql` |
| NOTICE-EMAIL-004 | PMO 要求 | 有效验证和交付台账 | 记录设计说明、台账和验证命令 | 本文件 | `delivery-contract-check` verify | DONE | `mango-docs/plans/2026-05-28-notice-email-rich-template-ledger.md` |
