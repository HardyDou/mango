# Mango Link 历史债务治理验收报告

## 基线

- 后端 Link 模块原有有效测试数为 0；聚合成功不能证明业务行为。
- 架构基线共 206 条阻断：API 77、Core 48、Starter 81。
- Chromium E2E 7 条中 5 条通过、2 条失败：公开/登录语义混用，以及首页跨分类断言错误。
- Flyway 混有 V1–V5 和业务 seed，DDL、演示数据及历史补丁未分离。

## 治理结果

- API、Core、Starter 分别执行 Mango Architecture full 门禁，dependency、ArchUnit、PMD、blocking 均为 0。
- 后端定向测试 9/9：Core 4 条、Starter 5 条。真实 H2 执行生产 V1 和真实 MyBatis Mapper，仅 Mock 外部 `TenantMemberProvider`。
- 前端 `@mango/link-openapi`、`@mango/link-page`、`@mango/link` 三个包构建通过。
- Chromium Link E2E 7/7，并以默认 5 workers 验证并行隔离；登录可见链接、302 和访问记录关键链路串行重复 3/3。
- 全新 MySQL 启动后 Flyway Link 只有 V1；五张表均含租户、组织和审计列。
- Demo 开启并等待 Resource Registry 稳定后：分类 4、网址 21、收藏 3；E2E 完成后临时分类和网址残留均为 0。
- 匿名公开列表返回 20 条；非法 Controller 请求真实返回 HTTP 400；公开和登录跳转分别登记 PUBLIC/LOGIN 并返回原生 302。
- clean JAR 中 Core 只有 `db/migration/link/V1__init_link.sql`；Starter 的 demo 位于 `META-INF/mango/demo`，Flyway 无 DML。

## 关键修复

- PUBLIC 列表与 LOGIN 可见列表分离；公开 `/link/open/*` 与登录 `/link/visible-links/*` 跳转分路，符合 PUBLIC 清理登录上下文的安全规则。
- 删除伪继承 `BaseLinkService`，以 `LinkServiceSupport` 组合复用；构造注入保持 `@RequiredArgsConstructor + private final`。
- 实体统一继承 `TenantEntity`，补齐 V1 最终态字段并移除 Mapper 注解 SQL。
- 删除无代码、无消费者的 `mango-link-starter-remote`。
- 函数式 302 路由通过 `API_RESOURCE` 显式登记，避免新库默认 401。
- 演示分类、网址、收藏迁移到 Link 自有 Resource Handler；Flyway 只负责 DDL。
- 修正并行 E2E 跨用例临时数据消费和未分组网址清理遗漏。

## 拓扑结论

Link 当前只有本地 Starter 能力，没有 Feign 契约、独立 capability app 或真实跨进程消费者，因此单体真实 E2E 适用且已通过；微服务和多节点 Link 业务 E2E 不适用。删除空 remote starter 后没有虚构跨进程验收。

## 非 Link 观察项

最终单体日志出现两条 Notice 异步发送失败后的 System 操作日志 `error_msg` 超长错误，堆栈位于 `NoticeController`、`NoticeSendEventListener` 和 `SysLogService`，不在 Link 调用链；Link 的 7 条浏览器测试、HTTP 状态、数据库副作用和清理结果均通过。本报告不扩大范围修改 Notice/System。
