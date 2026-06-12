# pre-release-main-cleanup-20260612 Stash 处置记录

## 1. 对象

- Stash：`stash@{0}: On main: pre-release-main-cleanup-20260612`
- 清理分支：`chore/cleanup-pre-release-stash-20260612`
- PR：`#142 chore: cleanup pre-release stash assets`
- 处置日期：2026-06-12

## 2. 结论

本 stash 不再作为待恢复工作保留。可保留内容已提交到 PR #142；已被当前 `main` 新实现覆盖的旧实现不恢复；已与 `main` 一致的文件不重复提交。

早先曾误把 `git diff stash@{0}^3^!` 的结果当作未跟踪文件清单，导致未跟踪内容范围被放大。正确清单来自：

```bash
git ls-tree -r --name-only stash@{0}^3
```

## 3. 已提交到 PR #142

以下内容与 stash 一致，已通过 PR #142 提交：

- `mango-pmo/rules/**`：业务使用 Mango 时的问题上报、接口层 Command、Service 入参、持久化访问、mapper.xml SQL 等规范收口。
- `mango-business-starter/business-pmo/mango-baseline/rules/**`：业务模板同步 PMO 基线。
- `mango-ui/packages/mango-cli/templates/full/business-pmo/mango-baseline/rules/**`：CLI full 模板同步 PMO 基线。
- `mango/mango-tools/mango-maven-plugin/src/main/java/io/mango/plugin/check/CheckMojo.java`：新增业务持久化风格检查。
- `mango/mango-tools/mango-maven-plugin/src/test/java/io/mango/plugin/check/CheckMojoTest.java`：新增对应测试。
- `mango-docs/evidence/2026-06-11-menu-acceptance/**`：菜单缩进、job 菜单归属、业务域页面验收证据。
- `mango-docs/evidence/2026-06-05-issue-module-path-api-prefix.md`：记录 `/api` 代理前缀不应进入后端模块契约的问题。

## 4. 已与 main 一致，不需要恢复

以下文件在当前 `main` 与 stash 内容一致，不需要重复提交：

- `mango/mango-infra/mango-infra-event/mango-infra-event-core/src/main/java/io/mango/infra/event/core/memory/InMemoryDomainEventBus.java`
- `mango/mango-infra/mango-infra-kv/mango-infra-kv-api/pom.xml`
- `mango/mango-infra/mango-infra-kv/mango-infra-kv-core/src/main/java/io/mango/infra/kv/core/outbox/OutboxKeys.java`

## 5. 被 main 新实现覆盖，不恢复

以下事件 outbox 相关文件在 stash 中是旧状态，当前 `main` 已通过 `c15c258e feat: add reliable domain event delivery` 和 `edbfdd76 fix: harden redis stream domain event delivery` 提供更新实现。恢复 stash 会删除 Redis Stream transport、System Event API、系统事件运维入口、失败终态、重新投递和查询能力，因此不恢复：

- `mango-docs/designs/mango-domain-event-transparent-delivery-design.md`
- `mango/mango-infra/mango-infra-event/README.md`
- `mango/mango-infra/mango-infra-event/mango-infra-event-api/pom.xml`
- `mango/mango-infra/mango-infra-event/mango-infra-event-api/src/main/java/io/mango/infra/event/api/SystemEventApi.java`
- `mango/mango-infra/mango-infra-event/mango-infra-event-api/src/main/java/io/mango/infra/event/api/command/ReconsumeSystemEventCommand.java`
- `mango/mango-infra/mango-infra-event/mango-infra-event-api/src/main/java/io/mango/infra/event/api/query/SystemEventPageQuery.java`
- `mango/mango-infra/mango-infra-event/mango-infra-event-api/src/main/java/io/mango/infra/event/api/vo/SystemEventVO.java`
- `mango/mango-infra/mango-infra-event/mango-infra-event-core/pom.xml`
- `mango/mango-infra/mango-infra-event/mango-infra-event-core/src/main/java/io/mango/infra/event/core/outbox/OutboxDomainEventDispatcher.java`
- `mango/mango-infra/mango-infra-event/mango-infra-event-core/src/main/java/io/mango/infra/event/core/redis/RedisStreamDomainEventTransport.java`
- `mango/mango-infra/mango-infra-event/mango-infra-event-core/src/main/java/io/mango/infra/event/core/system/SystemEventService.java`
- `mango/mango-infra/mango-infra-event/mango-infra-event-core/src/main/java/io/mango/infra/event/core/transport/DomainEventTransport.java`
- `mango/mango-infra/mango-infra-event/mango-infra-event-core/src/main/java/io/mango/infra/event/core/transport/TransportDomainEventDispatcher.java`
- `mango/mango-infra/mango-infra-event/mango-infra-event-starter/pom.xml`
- `mango/mango-infra/mango-infra-event/mango-infra-event-starter/src/main/java/io/mango/infra/event/starter/DomainEventAutoConfiguration.java`
- `mango/mango-infra/mango-infra-event/mango-infra-event-starter/src/main/java/io/mango/infra/event/starter/DomainEventProperties.java`
- `mango/mango-infra/mango-infra-event/mango-infra-event-starter/src/main/java/io/mango/infra/event/starter/DomainEventTransportScheduler.java`
- `mango/mango-infra/mango-infra-event/mango-infra-event-starter/src/main/java/io/mango/infra/event/starter/controller/SystemEventController.java`
- `mango/mango-infra/mango-infra-kv/mango-infra-kv-api/src/main/java/io/mango/infra/kv/api/IOutboxStore.java`
- `mango/mango-infra/mango-infra-kv/mango-infra-kv-api/src/main/java/io/mango/infra/kv/api/OutboxMessageQuery.java`
- `mango/mango-infra/mango-infra-kv/mango-infra-kv-core/src/main/java/io/mango/infra/kv/core/outbox/KvOutboxStore.java`

## 6. 验证

已执行：

```bash
mvn -pl mango-tools/mango-maven-plugin -am -Dtest=CheckMojoTest -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：`Tests run: 58, Failures: 0, Errors: 0, Skipped: 0`。

## 7. 后续处理

PR #142 合并后，`pre-release-main-cleanup-20260612` stash 可删除；删除前以本文件作为最终处置依据。
