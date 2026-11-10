---
'@mango/notice': patch
---

默认工作流通知改为可开关、可配置：新增 `mango.workflow.notice.enabled=false` 关闭默认通知订阅器，消息模板声明统一为 `SYNC_ONCE`，运营在通知中心修改的标题、正文和启用状态不再被声明升级覆盖；业务申请快照的 `customConfig` 新增 `viewQuery` 与 `viewActionLabel`，终态通知按声明生成站内信目标与按钮文案，待办到达通知行为不变。
