# 业务域 Domain

## 1. 能力定位

提供业务域树和业务归属编码管理。

主要使用者：Mango 开发者、业务开发者和 AI Agent。

## 2. 适用场景

需要把菜单、任务、流程或业务配置按业务域归类时使用。

## 3. 不适用场景

不负责业务数据权限计算和组织机构管理。

## 4. 模块边界

包含 API/core/starter/remote，负责 `biz_domain` 生命周期和远程查询。

## 5. 接入方式

后端引入 `mango-domain-starter`；远程调用引入 `mango-domain-starter-remote`。HTTP 入口 `/domain/domains`。

## 6. 配置项

`mango.domain.enabled` 控制自动配置，默认随 starter 开启。

## 7. 对外接口 / 扩展点

`DomainApi`、`DomainFeignClient`、`DomainController`。权限码包括 `domain:list`、`domain:add`、`domain:edit`、`domain:status`、`domain:delete`。

## 8. 数据库 / 初始化数据

`db/migration/domain/V1__init_domain.sql` 创建 `biz_domain`，`V2__seed_job_domain.sql` 补充 job 业务域种子。

## 9. 菜单 / 权限 / 租户

`DomainEntity` 继承租户实体；接口按权限码接入授权。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-domain -am test
```

## 11. 业务接入最小闭环

业务模块先确认业务域编码存在，再在菜单、任务或流程配置中引用该编码，并用当前租户查询校验隔离。

## 12. 常见问题

业务域树异常时检查租户、父级编码和 domain migration。

## 13. 关联 PMO 规则

- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史设计 / 交付记录

- [能力地图](../../../mango-docs/capabilities/README.md)
