# 通知 Notice

## 1. 能力定位

提供站内信、通知任务、通知渠道和接收设置能力。

主要使用者：Mango 开发者、业务开发者和 AI Agent。

## 2. 适用场景

业务需要统一发送站内、邮件、短信、企微、钉钉或公众号通知时使用。

## 3. 不适用场景

不负责具体业务事件生成，也不保证第三方渠道账号可用。

## 4. 模块边界

包含 api/core/support/starter/remote 以及 dingtalk/email/site/sms/wechat-official/wecom channel 模块。

## 5. 接入方式

后端引入 `mango-notice-starter`，按需引入渠道模块；远程调用引入 `mango-notice-starter-remote`。HTTP 入口 `/notice`。

## 6. 配置项

渠道配置由各 channel 模块读取；发送任务和接收设置由 notice core 管理。

## 7. 对外接口 / 扩展点

`NoticeApi`、`NoticeController` 和各渠道 SPI。权限码以 `notice:*`、部分用户接收设置接口复用 `system:user:add`。

## 8. 数据库 / 初始化数据

notice core migration 管理通知业务、渠道、任务、记录和接收设置表。

## 9. 菜单 / 权限 / 租户

通知业务按租户和接收人归属隔离；管理接口接入 `notice:*` 权限。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-notice -am test
```

## 11. 业务接入最小闭环

业务先配置通知业务编码和渠道，再提交通知任务，校验任务记录、站内信接收和失败重试状态。

## 12. 常见问题

第三方通知失败时区分 Mango 任务状态、渠道配置和外部服务返回码。

## 13. 关联 PMO 规则

- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史设计 / 交付记录

- [能力地图](../../../mango-docs/capabilities/README.md)
