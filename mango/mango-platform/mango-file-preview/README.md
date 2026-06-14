# 文件预览 File Preview

## 1. 能力定位

提供文件预览 URL、预览转换调度和本地预览引擎封装。

主要使用者：Mango 开发者、业务开发者和 AI Agent。

## 2. 适用场景

业务文件上传后需要 PDF、Office 或图片在线预览时使用。

## 3. 不适用场景

不负责文件存储、上传权限和业务附件归属。

## 4. 模块边界

包含 api/core/engine/starter；依赖 mango-file 获取文件信息，依赖 fileproc 执行转换。

## 5. 接入方式

后端引入 `mango-file-preview-starter`。HTTP 入口 `/file-preview`。

## 6. 配置项

`mango.file-preview` 控制预览能力；Office 转换仍受 fileproc/Aspose/LibreOffice 配置影响。

## 7. 对外接口 / 扩展点

`FilePreviewApi`、`FilePreviewController`；接口权限复用 `file:files:query`。

## 8. 数据库 / 初始化数据

未发现本模块独立 migration；预览记录和文件元数据由依赖模块承担。

## 9. 菜单 / 权限 / 租户

预览入口复用文件查询权限和租户上下文，业务侧仍需校验文件归属。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-file-preview -am test
```

## 11. 业务接入最小闭环

业务上传文件后保存 fileId，前端请求预览 URL，后端校验 `file:files:query` 和租户归属，再返回可访问预览地址。

## 12. 常见问题

预览失败优先检查文件是否存在、转换组件是否启用、Office/Aspose license 是否有效。

## 13. 关联 PMO 规则

- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史设计 / 交付记录

- [能力地图](../../../mango-docs/capabilities/README.md)
