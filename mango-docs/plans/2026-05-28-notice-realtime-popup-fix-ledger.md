# 通知实时提醒闭环修复交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| NOTICE-RT-001 | 用户反馈 | 系统消息收到后应出现右上角弹窗、声音/语音提醒和角标变化 | 通知客户端组件只负责展示和触发，配置由调用方通过 props 传入 | `mango-ui/packages/notice/src/client/NoticeClientBell.vue` | 组件测试覆盖 realtime 消息后刷新角标、弹窗、TTS | DONE | `mango-ui/packages/notice/src/components/__tests__/NoticeBell.spec.ts` |
| NOTICE-RT-002 | 用户反馈 | realtime 注册身份由调用者决定，不由通知组件决定 | `NoticeClientBell` 接收 `realtimeOptions`，管理后台从当前登录会话生成 identity | `mango-ui/packages/notice/src/realtime/noticeRealtime.ts`、`mango-ui/apps/mango-admin/src/layout/navBars/index.vue` | 测试覆盖 `createNoticeRealtime` 将 options 透传给 common realtime | DONE | `mango-ui/packages/notice/src/realtime/__tests__/noticeRealtime.spec.ts` |
| NOTICE-RT-003 | 缺陷定位 | 业务事件 `notice` 不应被 realtime 客户端当系统消息吞掉 | 仅 `system.connection.connected` 等系统事件按系统消息处理，`default.notice` 进入业务订阅 | `mango-ui/packages/common/utils/realtime/envelope.ts` | common realtime 测试覆盖 `notice` 业务事件可被订阅收到 | DONE | `mango-ui/apps/mango-admin/src/__tests__/realtime-client.spec.ts` |
| NOTICE-RT-004 | 用户截图 | 我的消息弹层列表样式优化，左侧领域标识，右侧标题/摘要上下结构 | 使用头像式领域首字和标准列表结构，不显示生硬 key 为主信息 | `mango-ui/packages/notice/src/client/NoticeClientBell.vue` | 组件测试和构建验证 | DONE | `mango-ui/packages/notice/src/components/__tests__/NoticeBell.spec.ts` |
