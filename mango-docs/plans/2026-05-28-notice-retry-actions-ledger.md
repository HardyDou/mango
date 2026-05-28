# 失败重试操作能力交付台账

## 设计说明

- 目标：失败重试列表提供可闭环的人工处理能力。
- 范围：发送记录失败池的详情、单条重试、批量重试、单条/批量标记成功、单条/批量忽略失败；前端列表操作入口；后端操作接口和状态流转。
- 不做：复杂通道切换重试、重新设计路由策略、重做发送记录列表。
- 接口变化：新增发送记录重试、批量重试、单条/批量人工成功、单条/批量忽略接口。
- 数据变化：发送状态枚举增加 `MANUAL_SUCCESS`、`IGNORED`；表字段为 varchar，无 DDL。
- 页面变化：失败重试页增加多选、操作列、处理弹窗和详情弹窗。
- 验收方式：后端 NoticeService 单元测试、前端 mango-admin 构建。

## 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| NOTICE-RETRY-A01 | 用户确认 | 失败重试列表支持详情 | 复用发送记录 VO，弹窗分区展示 | retry 页面详情弹窗 | 前端构建 | DONE | mango-ui/packages/notice/src/views/retry/index.vue |
| NOTICE-RETRY-A02 | 用户确认 | 支持单条重试 | 后端按指定记录立即重试，不扩大到整个任务 | retry API/service/button | 单元测试 | DONE | mango/mango-platform/mango-notice |
| NOTICE-RETRY-A03 | 用户确认 | 支持批量重试 | 多选记录逐条重试 | batch retry API/service/button | 单元测试 | DONE | mango/mango-platform/mango-notice |
| NOTICE-RETRY-A04 | 用户确认 | 支持标记成功 | 新增人工成功状态，保留真实发送状态语义 | manual success API/service/dialog | 单元测试 | DONE | mango/mango-platform/mango-notice |
| NOTICE-RETRY-A05 | 用户确认 | 支持忽略失败 | 新增忽略状态，退出失败池 | ignore API/service/dialog | 单元测试 | DONE | mango/mango-platform/mango-notice |
| NOTICE-RETRY-A06 | 用户确认 | 支持批量成功按钮 | 选中失败记录后统一填写原因，调用批量人工成功接口 | 批量成功按钮/API/service/test | 单元测试、前端构建 | DONE | mango-ui/packages/notice/src/views/retry/index.vue |
| NOTICE-RETRY-A07 | 用户确认 | 支持批量忽略按钮 | 选中失败记录后统一填写原因，调用批量忽略接口 | 批量忽略按钮/API/service/test | 单元测试、前端构建 | DONE | mango-ui/packages/notice/src/views/retry/index.vue |
