# 模块服务 Module

## 1. 能力定位

提供模块元数据、模块资源和模块服务注册基础能力。

主要使用者：Mango 开发者、业务开发者和 AI Agent。

## 2. 适用场景

平台需要发现模块、读取模块资源、同步菜单权限或管理模块服务时使用。

## 3. 不适用场景

不负责具体业务模块功能实现。

## 4. 模块边界

包含 api/core/starter，支撑 platform 模块和 authorization 资源同步。

## 5. 接入方式

后端引入 `mango-infra-module-starter`。

## 6. 配置项

`mango.module.module-service` 控制模块服务配置。

## 7. 对外接口 / 扩展点

模块 API、resource loader 和 `ModuleAutoConfiguration`。

## 8. 数据库 / 初始化数据

模块自身不直接声明业务表；模块资源来自各模块 META-INF 或配置。

## 9. 菜单 / 权限 / 租户

模块资源可被 authorization 消费生成菜单权限；本模块不授予角色。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-infra/mango-infra-module -am test
```

## 11. 业务接入最小闭环

业务或平台模块提供模块资源，启动后由 module service 读取，再由 authorization 同步菜单和权限。

## 12. 常见问题

模块资源未出现时检查资源路径、module-service 配置和授权同步开关。

## 13. 关联 PMO 规则

- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史设计 / 交付记录

- [能力地图](../../../mango-docs/capabilities/README.md)
