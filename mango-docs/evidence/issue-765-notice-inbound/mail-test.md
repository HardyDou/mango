# Issue #765 126 邮箱真实联调

## 环境与数据

- 邮箱账号标识：`yunxinbaokeji@126.com`
- SMTP：126 SSL 发送配置
- IMAP：126 SSL 拉取配置
- POP3：126 SSL 拉取配置
- 客户端设备名称：`mango`
- 测试邮件主题前缀：`Mango inbound mailbox test IT_765_`
- 凭据通过临时安全输入使用，未写入仓库、测试、日志、Issue 或本文。

## 结果

| 场景 | 结果 | 证据边界 |
|---|---|---|
| 126 SMTP 协议联调 | PASS | 直接使用 Jakarta/Python SMTP 客户端连接 126，服务器接受并发送测试邮件；不是 Mango Notice 发送机制验收 |
| POP3 回读 | PASS | 真实读取到含附件测试邮件并完成 MIME 解析 |
| IMAP TLS 与授权 | PASS | 真实建立 IMAP SSL 会话并认证 |
| IMAP 客户端 ID | PASS | 提交 name/vendor/version/contact-address 客户端标识 |
| IMAP MIME 解析 | PASS | 客户端解析能力通过真实会话验证 |
| IMAP 新邮件自发自收 | BLOCKED_EXTERNAL_SYNC | 新邮件未在 120 秒观察窗口内出现在 IMAP 视图，不能宣称完整链路通过 |

测试邮件已按主题前缀清理。生产启用 IMAP 前仍需在目标账号持续观察新邮件同步、游标推进和重复拉取幂等。

## 2026-08-13 补充联调

本次使用新的唯一主题执行 SMTP 自发测试，未把凭据写入文件或日志：

| 场景 | 结果 | 证据边界 |
|---|---|---|
| 126 SMTP 协议联调 | PASS | 直接使用 SMTP 客户端连接 126，服务器接受测试邮件；不是 Mango Notice 发送机制验收 |
| POP3 新主题回读 | NOT_FOUND | 最近邮件窗口未找到本次主题，不能据此宣称本次 POP3 闭环通过；此前含附件测试邮件的 POP3 回读证据仍保留在上表 |
| IMAP TLS 登录 | PASS | IMAP SSL 登录成功 |
| IMAP 打开 INBOX | BLOCKED_EXTERNAL_SYNC | 126 返回 `EXAMINE Unsafe Login`，无法进入邮箱文件夹查询新主题 |
| 企业微信官方 API 联调 | PASS | 临时脚本直接调用企业微信官方 `message/send` API，唯一匹配“豆晓雨”（UserID `DouXiaoYu`）并发送成功；不是 Mango Notice 发送机制验收；测试标识 `6c99ebfa75`，未向其它成员发送 |

人工回读：豆晓雨已确认实际收到测试消息（2026-08-13）。
| Notice 邮件入库/File/广播 | BLOCKED_MISSING_CHANNEL_CONFIG | 当前隔离库没有 EMAIL 渠道配置，未执行真实 Notice 发送到入站闭环 |
