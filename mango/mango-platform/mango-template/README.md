# 模板 Template

## 1. 能力定位

提供模板分类、模板版本和渲染记录管理。

主要使用者：Mango 开发者、业务开发者和 AI Agent。

## 2. 适用场景

合同、通知、文档或业务单据需要可配置模板渲染时使用。

## 3. 不适用场景

不负责文件上传存储和 Office 转换底层实现。

## 4. 模块边界

包含 api/core/starter/remote，依赖 infra-fileproc render/convert 能力。

## 5. 接入方式

后端引入 `mango-template-starter`；远程调用引入 `mango-template-starter-remote`。HTTP 入口 `/template/categories`、`/template/templates`。

## 6. 配置项

`mango.template.enabled` 控制自动配置；渲染和转换配置来自 infra-fileproc。

## 7. 对外接口 / 扩展点

`TemplateApi`、`TemplateCategoryApi`、`TemplateFeignClient`、`TemplateCategoryFeignClient`。

## 8. 数据库 / 初始化数据

`db/migration/template/V1__init_template.sql` 创建 `template`、`template_category`、`template_version`、`template_render_record`。

## 9. 菜单 / 权限 / 租户

模板分类和模板按租户隔离；接口权限由资源同步和授权模块接管。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-template -am test
```

## 11. 业务接入最小闭环

业务创建模板分类和模板，上传或配置模板内容，调用渲染接口生成结果，断言记录、版本和租户归属。

## 12. 常见问题

渲染失败先检查模板版本、占位数据、fileproc render/convert 能力和 license。

## 13. 关联 PMO 规则

- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史设计 / 交付记录

- [能力地图](../../../mango-docs/capabilities/README.md)
