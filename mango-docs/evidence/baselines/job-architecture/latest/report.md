# Job 历史债务治理基线对比报告

## 1. 结论

`mango-job-api/core/support/starter-remote/starter` 已完成定向历史债务治理。初始问题台账记录
263 个规则命中，最终完整定向架构门禁的 dependency、ArchUnit、PMD、blocking 均为 0；未通过
排除注解、预算放宽或保留第二套兼容实现绕过问题。

修改前既有定向测试 55/55 通过；修改后 Job 五模块 57/57、Payment Job handler 消费者 3/3
通过。新增测试用于保护真实 remote starter 装配、动态 Worker HTTP 调用和反向执行端点，不替代
原有业务断言。

## 2. 代码与契约治理

- API 保持传输无关，Controller 负责 HTTP 参数绑定、校验和 `R` 包装。
- 任务定义、告警规则拆分 create/update command，避免可空 ID 混合用例。
- Handler SPI 从 API 移到 support；它是 JVM 内扩展点，不是 HTTP/Feign 契约。
- 查询路由固定为 `GET /job/instances/logs/detail?instanceId=...`，Controller、Feign 和前端一致。
- Worker 反向执行端点由 remote starter 真实装配为 `POST /_job/workers/execute`，使用内部调用 HMAC。
- 运行时动态 Worker 地址由 `RestClient` 调用，不再用动态 URI Feign 方法破坏静态契约。
- Service、内部 Context/Criteria、异常码、构造注入和实现目录按 Mango 规则收敛。

## 3. 同一测试基线

| 范围 | 修改前 | 修改后 | 失败/错误/跳过 |
|---|---:|---:|---:|
| Job 既有定向测试 | 55 | 55 | 0/0/0 |
| Job 新增 remote starter 契约测试 | 0 | 2 | 0/0/0 |
| Job 合计 | 55 | 57 | 0/0/0 |
| Payment Job handler 消费者 | 3 | 3 | 0/0/0 |

修改后执行入口：

```bash
mvn -pl :mango-job-starter -am test
mvn -pl :mango-payment-core -am \
  -Dtest=PaymentChannelBillFetchJobHandlerTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

## 4. 架构门禁

```bash
mvn -pl :mango-job-api,:mango-job-support,:mango-job-core,\
:mango-job-starter-remote,:mango-job-starter \
  io.mango.tools.maven.plugin:mango-maven-plugin:1.0.0-SNAPSHOT:architecture \
  -Dmango.architecture.requireFullReactor=false \
  -Dmango.architecture.mode=full \
  -Dmango.architecture.base=origin/main
```

| 指标 | 修改前问题台账 | 修改后 |
|---|---:|---:|
| Job 五模块规则命中 | 263 | 0 |
| dependency issues | 已纳入台账 | 0 |
| ArchUnit issues | 已纳入台账 | 0 |
| PMD issues | 已纳入台账 | 0 |
| blocking issues | 已纳入台账 | 0 |

## 5. 数据与资源验证

- 隔离全新库：`mango_dev_mango_job_debt_007`。
- Flyway：baseline 与 `V1__init_mango_job.sql` 成功；JAR 中 Job location 仅保留该 V1。
- 最终结构：12 张 `mango_job_%` 表；租户、机构、审计、租约和 Worker 地址快照字段齐全。
- V1 仅包含 DDL，不包含正式数据、演示数据或跨模块数据。
- 正式资源位于 `META-INF/mango/resources/`；演示任务位于 `META-INF/mango/demo/`。
- demo 开启后，Job 两条 Probe 演示任务和 Payment 一条账单拉取演示任务均由各自模块资源成功落库。

## 6. 真实运行验收

| 验证 | 结果 |
|---|---|
| 单体全新库启动 | PASS，health UP，Flyway/Resource Registry 成功 |
| Job 管理端 Chromium E2E | PASS，3/3，2.4 分钟 |
| Cron 稳定性 E2E | PASS，1/1，连续 3 分钟；3 个成功实例，重复窗口 0，失败 0 |
| 独立双 JVM E2E | PASS，JobCenter 18622 + Worker 18623，共用全新库 |
| 远程 Worker 注册 | PASS，`HTTP_INTERNAL`、ONLINE，地址 `http://127.0.0.1:18623` |
| 远程任务执行 | PASS，实例/attempt/log 均成功，Worker 地址快照指向 18623 |
| 内部调用安全 | PASS，未签名直调 Worker 返回 403 |

双 JVM 验收使用真实 capability app 进程，没有在测试上下文手工导入 Controller 或 Executor。
这次发现的关键假绿模式是：旧 Flow 测试补装了生产 starter 并未导出的 Bean，因此单进程测试通过
不能证明发布物可作为 Worker。修复后由 remote starter 自行提供完整执行端点，并通过独立进程验证。

## 7. 验证环境经验

- capability app 必须显式选择可用的 KV 实现；本地验收使用 memory，避免把缺 Redis 误判为 Job 回归。
- 关闭通用 discovery 后还需关闭 Nacos discovery/config/health，最终成功启动的进程配置才算证据。
- 前端源码模式必须让 `VITE_ADMIN_PROXY_PATH` 指向工作区后端端口；Playwright 的 API base URL
  不能替代 Vite 代理配置。
- E2E 必须等待 Resource Registry 完成，不能在端口刚就绪时立即断言演示数据缺失。
