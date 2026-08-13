# Issue #765 Notice 入站能力交付证据

## 结论

- 代码与自动化验证已覆盖邮箱 IMAP/POP3 拉取、邮箱 Webhook SPI、企业微信 GET/POST 回调、Inbox 幂等、附件写入 `mango-file`、`notice.message.received` 广播及重试/死信、管理员列表与详情查询。
- 企业微信用户提供的 GET 验证向量已通过正式适配器的签名校验和 AES 解密；敏感配置未写入代码、文档、测试和日志。
- 企业微信官方 API 联调已按姓名唯一匹配到“豆晓雨”（UserID `DouXiaoYu`），只向该成员发送一次测试消息；供应商返回 `errcode=0`、`errmsg=ok` 且存在消息 ID，测试标识为 `6c99ebfa75`。该次不是 Mango Notice 发送机制验收。
- 126 邮箱 SMTP/POP3/IMAP 均做过直接协议联调；SMTP 连接发送通过，POP3 此前含附件邮件回读通过，但本轮新主题未回读到；IMAP 登录通过但打开 INBOX 被 126 的 `Unsafe Login` 拦截。以上不是 Mango Notice 发送机制或完整入站闭环验收。
- Notice 正式 POP3 轮询已真实接收到用户回复 `2222 + 1.pdf`：消息 `2087866808973164545` 为 `BROADCASTED`，正文与 HTML 均保存，附件 `1.pdf` 已写入 Mango File（File ID `2087866809128353794`，`PRIVATE`，`SAVED`）。
- 管理列表与详情 API 已回读该真实消息；列表返回三封管理员入站回复，详情返回 `2222` 正文/HTML 和附件 File ID。旧消息 `2087860225505550338` 的历史失败摘要已在本地验收库清理，当前 `BROADCASTED` 且失败字段为空。
- 邮箱 POP3 游标 `channelConfigId=2087860192278274050` 已真实推进，`last_failure_code` 与 `last_failure_reason` 均为空；这验证了成功轮询会清理旧失败字段。
- 浏览器控制接口在本会话不可用，无法完成带登录态的 UI 截图、搜索/分页/详情/附件下载、console/network 走查；该项为 `BLOCKED`，不是通过。

## 证据索引

- [自动化与接口结果](./automated.md)
- [126 邮箱真实联调边界](./mail-test.md)
- [管理端与权限验收](./acceptance-evidence.md)

## 安全边界

- 不记录邮箱网页密码、客户端授权码、企业微信 Token、EncodingAESKey、登录 Token 或 Cookie。
- 公网回调匿名访问只跳过登录认证，不跳过渠道启用状态、租户绑定、企业微信验签/AES 解密和邮箱 provider 验真。
- 管理查询要求登录态及 `notice:inbound:view`；入站消息属于管理员运营数据，不注册到个人消息中心。
