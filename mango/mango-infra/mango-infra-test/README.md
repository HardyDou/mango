# 基础设施测试 Infra Test

## 1. 能力定位

集中放置 Mango infra 跨模块集成测试。

主要使用者：Mango 开发者、业务开发者和 AI Agent。

## 2. 适用场景

验证 KV、Realtime、Outbox、并发和租户隔离等基础设施组合能力时使用。

## 3. 不适用场景

不作为生产依赖，不承载业务测试用例。

## 4. 模块边界

测试模块，包含 H2/Redis/Testcontainers 等集成测试 fixture。

## 5. 接入方式

只在 Maven test 阶段使用，不被业务模块依赖。

## 6. 配置项

测试配置由测试类和测试资源声明。

## 7. 对外接口 / 扩展点

测试 fixture，例如 KV、Realtime 集成测试支撑类。

## 8. 数据库 / 初始化数据

测试中按需创建 H2 表，例如 `infra_kv_entry`；不提供生产 migration。

## 9. 菜单 / 权限 / 租户

测试覆盖租户隔离和消息隔离断言；不提供运行期权限能力。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-infra/mango-infra-test -am test
```

## 11. 业务接入最小闭环

新增 infra 跨模块能力时把集成测试放入本模块，验证真实 store、并发和租户隔离。

## 12. 常见问题

测试失败时先确认本机 Docker/Redis/Testcontainers 环境和 Maven profile。

## 13. 关联 PMO 规则

- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史设计 / 交付记录

- [能力地图](../../../mango-docs/capabilities/README.md)
