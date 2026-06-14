# Mango App

## 1. 能力定位

定义 Mango 可部署 Spring Boot 启动入口和单体/微服务拓扑。

主要使用者：Mango 维护者、Mango 开发者、业务开发者和 AI Agent。

## 2. 适用场景

本仓内启动单体、网关、平台服务和业务服务示例时使用。

## 3. 不适用场景

不承载业务规则、平台能力实现和公共组件实现。

## 4. 模块边界

包含 `monolith/mango-monolith-app` 与 `microservice/*-app`；能力实现仍归属 `mango-platform`、`mango-infra` 或业务模块。

## 5. 接入方式

从对应 app 模块启动；业务项目优先通过 CLI/starter 生成自己的宿主应用。

## 6. 配置项

应用配置由各 app resources 和本地 `.mango/dev-workspace.env` 注入。

## 7. 对外接口 / 扩展点

Spring Boot main class 和部署拓扑，无复用 API。

## 8. 数据库 / 初始化数据

不直接拥有业务表；依赖各 starter migration。

## 9. 菜单 / 权限 / 租户

权限、菜单和租户由装配进来的平台模块提供。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-app/monolith/mango-monolith-app -am test
```

## 11. 业务接入最小闭环

选择单体或微服务拓扑，配置数据库和端口，启动后验证 health、登录、菜单和核心业务接口。

## 12. 常见问题

不要把业务逻辑写进 app；启动失败先检查 worktree 环境、端口、数据库和 profile。

## 13. 关联 PMO 规则

- [后端模块规范](../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史设计 / 交付记录

- [能力地图](../../mango-docs/capabilities/README.md)
