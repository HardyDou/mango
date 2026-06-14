# 编号生成 Numgen

## 1. 能力定位

提供业务编号规则、号段、序列和生成历史。

主要使用者：Mango 开发者、业务开发者和 AI Agent。

## 2. 适用场景

订单号、单据号、合同号等需要按规则生成唯一编号时使用。

## 3. 不适用场景

不负责业务实体持久化，也不替代数据库唯一约束。

## 4. 模块边界

包含 API/core/starter/remote；负责规则管理、号段消耗和编号生成。

## 5. 接入方式

后端引入 `mango-numgen-starter`；远程调用引入 `mango-numgen-starter-remote`。HTTP 入口 `/numgen`、`/numgen/rules`、`/numgen/sequences`、`/numgen/segments`、`/numgen/histories`、`/numgen/generators`。

## 6. 配置项

`mango.numgen.enabled` 控制自动配置；`mango.numgen.kv` 用于分布式防并发能力。

## 7. 对外接口 / 扩展点

`NumgenApi`、`NumgenFeignClient` 以及规则、序列、号段、历史 Controller。

## 8. 数据库 / 初始化数据

numgen core migration 管理规则、序列、号段和生成历史表。

## 9. 菜单 / 权限 / 租户

管理接口接入编号生成相关权限；业务生成调用需绑定租户和业务编码。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-numgen -am test
```

## 11. 业务接入最小闭环

业务创建编号规则，调用 `NumgenApi` 按业务编码取号，保存业务单据时用数据库唯一约束兜底。

## 12. 常见问题

重复号或取号失败时检查规则启用状态、KV 能力、数据库唯一约束和并发测试结果。

## 13. 关联 PMO 规则

- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史设计 / 交付记录

- [能力地图](../../../mango-docs/capabilities/README.md)
