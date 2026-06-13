# 上下文 Context

## 1. 能力定位

提供租户、用户、请求和跨线程上下文快照能力。

主要使用者：Mango 开发者、业务开发者和 AI Agent。

## 2. 适用场景

后端模块需要在 Controller、Feign、异步任务或线程池中传递 Mango 上下文时使用。

## 3. 不适用场景

不负责鉴权决策、权限码校验和数据库租户过滤实现。

## 4. 模块边界

包含 core/starter，提供 `MangoContextHolder`、headers 和传播自动配置。

## 5. 接入方式

后端引入 `mango-infra-context-starter`。

## 6. 配置项

`mango.context.enabled` 控制上下文传播；`mango.context.executor.enabled` 控制 executor 包装。

## 7. 对外接口 / 扩展点

`MangoContextSnapshot`、`MangoContextHolder`、`MangoContextHeaders`。

## 8. 数据库 / 初始化数据

无数据库和初始化数据。

## 9. 菜单 / 权限 / 租户

上下文携带 tenant/user 信息，不直接授予权限；权限和租户过滤由 consuming 模块执行。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-infra/mango-infra-context -am test
```

## 11. 业务接入最小闭环

业务请求进入后写入上下文，异步或远程调用读取同一 tenant/user，断言任务结束后上下文清理。

## 12. 常见问题

上下文丢失时检查 header、executor 包装和 Feign 透传配置。

## 13. 关联 PMO 规则

- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史设计 / 交付记录

- [能力地图](../../../mango-docs/capabilities/README.md)
