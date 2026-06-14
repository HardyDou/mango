# 文件处理 Fileproc

## 1. 能力定位

提供文件渲染、转换和 Aspose license 装载基础能力。

主要使用者：Mango 开发者、业务开发者和 AI Agent。

## 2. 适用场景

模板渲染、Office/PDF 转换、文件预览需要底层处理能力时使用。

## 3. 不适用场景

不负责业务文件存储、上传接口和预览权限。

## 4. 模块边界

包含 api/core/starter，Aspose 子目录 README 说明 license。

## 5. 接入方式

后端引入 `mango-infra-fileproc-starter`。

## 6. 配置项

`mango.fileproc.render` 控制渲染能力，`mango.fileproc.convert` 控制转换能力，`mango.fileproc.aspose` 控制 Aspose license。渲染配置包含 `mango.fileproc.render.enabled` 和 `mango.fileproc.render.pdf-operations-enabled`。

## 7. 对外接口 / 扩展点

`RenderApi`、`ConvertApi`、`AsposeLicenseApi`。

## 8. 数据库 / 初始化数据

无独立数据库和初始化数据。

## 9. 菜单 / 权限 / 租户

本模块不做业务权限；调用方需先完成文件和模板归属校验。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-infra/mango-infra-fileproc -am test
```

## 11. 业务接入最小闭环

业务模块先校验文件或模板归属，再调用 render/convert API，最后验证输出文件类型、渲染开关和 license 状态。

## 12. 常见问题

转换或渲染失败优先检查 `mango.fileproc.render`、Aspose license、LibreOffice/转换插件环境和输入文件格式。

## 13. 关联 PMO 规则

- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史设计 / 交付记录

- [能力地图](../../../mango-docs/capabilities/README.md)
