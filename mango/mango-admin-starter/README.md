# Mango Admin Starter

## 1. 能力定位

聚合 Mango 后端管理端 starter，提供业务宿主应用的一站式依赖入口。

主要使用者：Mango 维护者、Mango 开发者、业务开发者和 AI Agent。

## 2. 适用场景

业务项目希望一次性接入 Mango 平台后端能力时使用。

## 3. 不适用场景

不提供 Controller、领域服务、数据库 migration、seed 数据或应用配置文件。

## 4. 模块边界

只聚合公开 starter，不直接依赖 `*-core` 模块；宿主应用仍负责 Spring Boot runtime、datasource、profiles 和部署配置。

## 5. 接入方式

业务后端在 `pom.xml` 引入 `mango-admin-starter`，版本使用 `${mango.version}`。

## 6. 配置项

无独立配置项；读取被聚合 starter 的配置。

## 7. 对外接口 / 扩展点

Maven 依赖聚合入口，无 HTTP API。

## 8. 数据库 / 初始化数据

无独立数据库和初始化数据。

## 9. 菜单 / 权限 / 租户

菜单、权限、租户来自被聚合的平台模块。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-admin-starter -am test
```

## 11. 业务接入最小闭环

业务宿主引入 starter，配置 datasource 和 Mango 模块配置，启动后验证登录、菜单、字典和核心平台接口。

## 12. 常见问题

如果启动失败，先看缺失的被聚合 starter 配置、datasource、Flyway migration 和 bean 冲突。

## 13. 关联 PMO 规则

- [后端模块规范](../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史设计 / 交付记录

- [能力地图](../../mango-docs/capabilities/README.md)
