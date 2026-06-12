# Issue 137 系统事件验收记录

## 环境

- 时间：2026-06-12
- Worktree：`/Users/hardy/Work/mango/.mango/worktrees/issue-137-domain-event-reliable`
- 后端：`http://127.0.0.1:18952`
- 前端：`http://127.0.0.1:8642`
- 数据库：`127.0.0.1:3306/mango_dev_8b0c47`
- 登录账号：`admin`，租户 `default / 芒果集团`
- 证据截图：`system-event-list.png`、`system-event-detail.png`

## 回归测试报告

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| EVT-001 | `/system/events`、`GET /system/events` | 系统事件列表真实接口加载 | `admin/admin123` 登录；E2E 在 JDBC KV Outbox 写入专用 `FAILED` 事件 `e2e-system-event-issue-137` | 后端健康检查 `UP`；登录后 `GET /system/events` 返回 `success=true`；列表包含 `E2E-ISSUE-137-SYSTEM-EVENT`；菜单树包含“系统维护 / 系统事件” | 搜索区、刷新、表格、分页、异常范围开关、失败事件行可见 | 浏览器请求命中真实后端；无 mock 数据；无 401/403/404 错误 | `system-event-list.png` | PASS |
| EVT-002 | `GET /system/events/detail`、`POST /system/events/reconsume` | 系统事件详情与补偿入口 | 同一条真实 JDBC KV Outbox `FAILED` 事件 | 详情 API 返回 `messageId=e2e-system-event-issue-137`；详情弹框展示事件头、载荷、错误信息；点击“重新投递”后 `POST /system/events/reconsume` 返回成功 | 弹框使用 Element Plus `el-dialog`、`el-descriptions`，长 JSON 可滚动；确认框和成功提示可见 | 真实 API 完成详情查询和重投；E2E 结束清理测试 Outbox 键 | `system-event-detail.png` | PASS |
| EVT-003 | 后端 Outbox / Redis Stream 配置 | 可靠投递、失败转终态、补偿回放、Redis Stream transport bean | `DomainEventOutboxAutoConfigurationTest`、`OutboxAutoConfigurationTest` | 10 个目标测试全部通过；失败消费进入 `FAILED`，`reconsume` 回到 `PENDING`；不存在 messageId 重新投递返回 `false`；Redis Stream transport 可装配 | 不涉及 UI | Maven 输出 `BUILD SUCCESS` | 命令输出记录于本轮执行结果 | PASS |
| EVT-004 | 前端包构建 | 系统事件页面和默认页面注册 | `@mango/system`、`@mango/admin-pages` | 两个包构建成功，页面组件可被 `system/event/index` 加载 | Element Plus 表单、表格、弹框结构通过人工走查 | Vite build 成功 | 命令输出记录于本轮执行结果 | PASS |
| EVT-005 | Redis Stream transport | 真实 Redis Stream 端到端投递、服务组广播、同组竞争、pending 恢复 | 本机真实 Redis `127.0.0.1:6379`；测试 stream `mango:test:domain-event:*`；真实 `RedisKvStore` + `KvOutboxStore` + `OutboxDomainEventPublisher` + `TransportDomainEventDispatcher` | `redis-cli -h 127.0.0.1 -p 6379 ping` 返回 `PONG`；Outbox 发布后 dispatcher 写入真实 Redis Stream 并 ack 本地 Outbox；`payment-service` 与 `notice-service` 两个不同 group 都收到 `workflow.process.completed`；同一 `payment-service` group 两个 consumer 只消费一次；消费者 ACK 前异常后 pending 留存，超过 idle 阈值后另一 consumer auto-claim 并 ACK 清空 pending | 不涉及 UI | Redisson 日志显示连接 `localhost/127.0.0.1:6379`；无 mock、无 Mockito；失败消费者日志为 pending 恢复用例的预期异常 | Maven 输出 `Tests run: 4, Failures: 0, Errors: 0` | PASS |

## 未验证项和风险

- Redis Stream 已连接本机真实 Redis 单实例完成 Outbox 端到端、不同服务 group 广播、同 group 多实例竞争和 pending auto-claim 恢复验证；未做 Redis 集群、网络分区和长时间压测。
- KV Outbox 的全量索引用于系统事件查询，只覆盖新写入或状态变更后的消息；历史无索引消息不会自动出现在系统事件列表。
- E2E 为了覆盖详情和补偿，向本地工作区 JDBC KV 写入专用测试事件，并在结束后清理；没有引入测试后门接口。
