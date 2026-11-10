---
'@mango/notice': patch
'@mango/admin-shell': patch
---

站内信消息目标解析失败时退到消息中心，通知按钮不再成为死路。`@mango/notice` 新增 `openNoticeTarget` 统一入口，按业务目标键解析路由并在失败时回退到消息中心；`@mango/admin-shell` 通知铃铛改用该入口，目标未注册或无权限时仍完成用户可见的导航，不再只提示“目标未注册或当前无权访问”。既有目标键、参数和回调语义不变。
