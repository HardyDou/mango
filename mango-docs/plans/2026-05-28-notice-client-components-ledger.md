# 客户端系统消息组件封装交付台账

## 设计说明

- 目标：将系统消息接收入口封装为可被 Mango 框架下其它业务复用的客户端组件。
- 范围：小铃铛、消息中心、接收设置、详情弹窗、realtime 接收闭环和管理后台适配。
- 不做：重构通知管理后台配置页、变更后端接口、变更数据库结构。
- 模块边界：`packages/notice/src/client` 承载客户端组件；`views/*` 仅作为管理后台路由适配；`apps/mango-admin` 只负责宿主路由跳转。
- 接口变化：无新增后端接口；复用系统消息、未读数、接收账号、接收规则和渠道配置接口。
- 数据变化：无。
- 菜单/页面变化：管理后台菜单不变；消息页面和接收设置页面改为引用客户端组件。
- 测试范围：NoticeClientBell 组件测试覆盖未读数、realtime、声音/TTS、桌面通知、右上角通知、点击详情、路由解耦；mango-admin 构建验证宿主适配。

## 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| NOTICE-CLIENT-A01 | 用户确认 | 封装客户端小铃铛组件 | 下沉到 `client`，组件只暴露事件，不依赖后台路由 | NoticeClientBell | 组件测试、构建 | DONE | mango-ui/packages/notice/src/client/NoticeClientBell.vue |
| NOTICE-CLIENT-A02 | 用户确认 | 封装我的消息页面能力 | 下沉消息列表、详情、批量已读、删除能力，后台页面只做适配 | NoticeClientMessageCenter | 构建 | DONE | mango-ui/packages/notice/src/client/NoticeClientMessageCenter.vue |
| NOTICE-CLIENT-A03 | 用户确认 | 封装接收设置能力 | 下沉账号和接收规则配置，后台页面只做适配 | NoticeClientReceiveSetting | 构建 | DONE | mango-ui/packages/notice/src/client/NoticeClientReceiveSetting.vue |
| NOTICE-CLIENT-A04 | 用户确认 | 系统消息自主集成 realtime | 组件内部订阅 realtime，并保留轮询兜底 | createNoticeRealtime + NoticeClientBell | 组件测试 | DONE | mango-ui/packages/notice/src/client/NoticeClientBell.vue |
| NOTICE-CLIENT-A05 | 用户确认 | 收到消息形成提示闭环 | realtime 后刷新未读数、TTS、桌面通知、右上角通知、点击详情并标记已读 | NoticeClientBell | 组件测试 | DONE | mango-ui/packages/notice/src/components/__tests__/NoticeBell.spec.ts |
| NOTICE-CLIENT-A06 | 用户确认 | 管理后台与客户端组件分离 | 后台导航栏监听组件事件后自行 router.push | mango-admin navBars adapter | 构建 | DONE | mango-ui/apps/mango-admin/src/layout/navBars/index.vue |
