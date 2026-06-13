# Mango Tools

## 1. 能力定位

提供 Mango 后端构建工具和 Maven 插件。

主要使用者：Mango 维护者、Mango 开发者、业务开发者和 AI Agent。

## 2. 适用场景

需要生成模块、生成 CRUD、执行代码/迁移/权限规则检查时使用。

## 3. 不适用场景

不作为业务运行时依赖。

## 4. 模块边界

当前包含 `mango-maven-plugin`；工具只服务开发、检查和生成。

## 5. 接入方式

在 Maven 命令中调用 `mango:*` goal。

## 6. 配置项

工具配置来自 Maven 插件参数和项目结构。

## 7. 对外接口 / 扩展点

Maven plugin goals，例如 `mango:check`、`mango:gen-module`、`mango:gen-crud`。

## 8. 数据库 / 初始化数据

无生产数据库；生成和检查 migration 时只读取项目文件。

## 9. 菜单 / 权限 / 租户

可检查权限码、路径参数和 schema 规则，但不授予运行时权限。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-tools/mango-maven-plugin -am test
```

## 11. 业务接入最小闭环

开发者用工具生成模块或执行检查后，仍要补齐模块 README、能力地图和真实测试。

## 12. 常见问题

生成结果只是脚手架，不能替代业务实现和验收。

## 13. 关联 PMO 规则

- [后端模块规范](../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史设计 / 交付记录

- [能力地图](../../mango-docs/capabilities/README.md)
