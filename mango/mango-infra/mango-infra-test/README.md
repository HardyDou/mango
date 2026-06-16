# Mango Infra Test

## 1. 概览
`mango-infra-test` 是 Mango infra 跨模块测试模块，集中验证 KV、Outbox、事件、Realtime、日志配置和多实例场景。它只在 Maven test 阶段使用，不是生产依赖。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 新增或修改 infra 公共能力后，需要跨模块集成测试 | Maven 依赖 / starter / Java API |
| 需要验证 Redis、KV sorted set、Outbox、Realtime presence、WebSocket、上行 receiver 等组合行为 | Maven 依赖 / starter / Java API |
| 需要用 H2 表模拟 infra_kv_entry 或用真实 Redis 验证 Redis store | Maven 依赖 / starter / Java API |

## 3. 适用场景
- 新增或修改 infra 公共能力后，需要跨模块集成测试。
- 需要验证 Redis、KV sorted set、Outbox、Realtime presence、WebSocket、上行 receiver 等组合行为。
- 需要用 H2 表模拟 `infra_kv_entry` 或用真实 Redis 验证 Redis store。

## 4. 边界说明
- 不作为业务模块依赖。
- 不放业务功能验收用例。
- 不提供生产 migration、Controller 或运行期能力。

## 5. 模块组成
测试覆盖范围包括：

- KV store contract、Memory/Redis/JDBC fixture、capability、key namespace、serializer/converter。
- KV Outbox 自动配置、message claim、ack、fail、requeue。
- Domain event outbox 和 Redis Stream transport。
- Realtime 多实例下行、上行、presence、receiver register。
- Log properties 绑定。

## 6. 接入方式
业务模块不要依赖本模块。新增 infra 跨模块测试时，在本模块增加测试类或测试 fixture，并通过 Maven test 执行。

```bash
mvn -f mango/pom.xml -pl mango-infra/mango-infra-test -am test
```

按范围执行：

```bash
mvn -f mango/pom.xml -pl mango-infra/mango-infra-test -am test -Dtest='*Kv*'
mvn -f mango/pom.xml -pl mango-infra/mango-infra-test -am test -Dtest='*Realtime*'
mvn -f mango/pom.xml -pl mango-infra/mango-infra-test -am test -Dtest='*Outbox*'
```

## 7. 配置说明
本模块没有生产配置项。测试资源包括：

- `src/test/resources/application-realtime-local-test.yml`
- `src/test/resources/application-realtime-remote-test.yml`

部分真实 Redis 集成测试要求本机 `localhost:6379` 可用且无密码；测试类注释会标明该要求。

## 8. API 与扩展
本模块不导出生产 API。可复用测试 fixture 包括：

- `KvStoreTestFixtures`：H2、Redis、Memory store 测试支撑。
- realtime e2e test apps：模拟 local/remote realtime 应用。
- shared presence 和 receiver listener 测试支撑。

## 9. 数据与初始化
测试中按需创建 H2 表，例如 `infra_kv_entry`。这不是生产 migration；生产表结构仍以 `mango-infra-kv` 的 Flyway migration 为准。

## 10. 管理入口
本模块不提供菜单、权限和租户数据。测试会覆盖 tenant id、group、user 等实时投递隔离断言，但这些只服务测试。

## 11. 快速开始
新增 infra 能力时：

1. 在能力模块写单元测试。
2. 如果能力跨 KV、event、realtime、web、feign 等多个模块，在 `mango-infra-test` 增加集成测试。
3. 测试名称表达能力和场景，例如 `OutboxAutoConfigurationTest`、`MangoRealtimeInboundMultiServiceE2ETest`。
4. 在 README 或能力文档的相关文档、排障入口或交付记录中引用对应测试命令。

## 12. 问题排查
- Redis 测试失败：确认 `localhost:6379` 可连接，密码为空，或者跳过真实 Redis 范围。
- Realtime e2e 失败：检查端口占用、WebSocket 支持和测试应用上下文启动日志。
- H2 表结构失败：先看 `KvStoreTestFixtures` 是否和 KV migration 保持一致。

## 13. 相关文档
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史资料
- [能力地图](../../../mango-docs/capabilities/README.md)
