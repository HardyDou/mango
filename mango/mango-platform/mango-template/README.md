# 模板 Template

## 1. 概览
`mango-template` 提供可版本化的业务模板能力：模板分类、模板草稿、模板发布版本、变量定义、变量提取、同步渲染、异步渲染和渲染记录。

主要使用者是合同、函证、通知、单据、导出文档等需要由业务人员维护模板内容的模块。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 业务需要用模板编码渲染文本、HTML、DOCX、XLSX，并输出 TEXT、HTML、DOCX、XLSX、PDF 或 OFD | Maven 依赖 / HTTP API / Java API |
| 模板需要草稿和发布版本，运行时只使用当前发布版本 | Maven 依赖 / HTTP API / Java API |
| 模板源文件和渲染结果需要通过 mango-file 保存 | Maven 依赖 / HTTP API / Java API |
| 模板变量需要结构化定义，便于前端表单和业务校验 | Maven 依赖 / HTTP API / Java API |
| 业务需要按 bizType、bizId 查询渲染记录 | Maven 依赖 / HTTP API / Java API |

## 3. 适用场景
- 业务需要用模板编码渲染文本、HTML、DOCX、XLSX，并输出 TEXT、HTML、DOCX、XLSX、PDF 或 OFD。
- 模板需要草稿和发布版本，运行时只使用当前发布版本。
- 模板源文件和渲染结果需要通过 `mango-file` 保存。
- 模板变量需要结构化定义，便于前端表单和业务校验。
- 业务需要按 `bizType`、`bizId` 查询渲染记录。

## 4. 边界说明
- 不负责底层 Office 渲染和格式转换算法；这些来自 `mango-infra-fileproc`。
- 不负责文件上传存储本身；模板源文件和输出文件通过 `mango-file` 适配。
- 不负责电子签章、合同审批和业务归档。
- 不适合渲染不受信任的任意脚本模板。

## 5. 模块组成
- `mango-template-api`：`TemplateApi`、`TemplateCategoryApi`、命令、查询、VO、源格式和输出格式枚举。
- `mango-template-core`：模板分类、模板、模板版本、渲染记录、变量校验、渲染调度。
- `mango-template-starter`：`TemplateAutoConfiguration`、Controller、`MangoFileTemplateFileStore`。
- `mango-template-starter-remote`：`TemplateFeignClient`、`TemplateCategoryFeignClient`。

核心渲染链路：读取当前发布版本或草稿内容 -> 调用 `RenderApi` 渲染 -> 必要时调用 `ConvertApi` 转 PDF/OFD -> 通过 `FileApi` 保存输出文件 -> 写入渲染记录。

## 6. 接入方式
提供模板能力的服务引入 starter：

```xml
<dependency>
    <groupId>io.mango.platform.template</groupId>
    <artifactId>mango-template-starter</artifactId>
</dependency>
```

只做远程消费的服务引入 remote starter：

```xml
<dependency>
    <groupId>io.mango.platform.template</groupId>
    <artifactId>mango-template-starter-remote</artifactId>
</dependency>
```

同时必须具备：

- `mango-file`：保存模板源文件和渲染结果文件。
- `mango-infra-fileproc`：提供 `RenderApi`、`ConvertApi`。
- 如果输出 PDF 或 OFD，fileproc 转换引擎和 license 必须可用。

## 7. 配置说明
当前模块没有专属 `@ConfigurationProperties`。模板运行依赖下游能力配置：

| 依赖 | 配置位置 | 用途 |
|------|----------|------|
| `mango-file` | 文件模块配置和文件存储配置 | 保存 `source_file_id` 和 `output_file_id` 对应文件。 |
| `mango-infra-fileproc` | 文件处理模块配置 | 渲染 TEXT/HTML/DOCX/XLSX，并转换 PDF/OFD。 |
| Aspose license | `mango-infra-fileproc` 资源配置 | DOCX/XLSX 到 PDF/OFD 等转换能力。 |

不要配置不存在的 `mango.template.enabled` 作为模块开关；当前 starter 随自动配置加载。

## 8. API 与扩展
模板分类根路径：`/template/categories`。

| 方法 | 路径 | 用途 |
|------|------|------|
| GET | `/template/categories/page` | 分页查询分类。 |
| GET | `/template/categories/list` | 查询分类列表。 |
| GET | `/template/categories/detail` | 查询分类详情。 |
| POST | `/template/categories` | 新增分类。 |
| PUT | `/template/categories` | 修改分类。 |
| PUT | `/template/categories/status` | 启停分类。 |
| DELETE | `/template/categories` | 删除分类。 |

模板根路径：`/template/templates`。

| 方法 | 路径 | 用途 |
|------|------|------|
| GET | `/template/templates/page` | 分页查询模板。 |
| GET | `/template/templates/detail` | 查询模板详情、草稿和版本信息。 |
| POST | `/template/templates` | 新增模板或保存草稿。 |
| PUT | `/template/templates` | 修改模板或草稿。 |
| DELETE | `/template/templates` | 删除模板。 |
| PUT | `/template/templates/status` | 启停模板。 |
| POST | `/template/templates/versions` | 发布模板版本。 |
| PUT | `/template/templates/versions/current` | 启用历史版本。 |
| POST | `/template/templates/variables/extract` | 从模板内容或文件中提取变量建议。 |
| POST | `/template/templates/render` | 同步渲染模板。 |
| POST | `/template/templates/render/async` | 异步渲染模板。 |
| GET | `/template/templates/render-records/detail` | 查询渲染记录详情。 |
| GET | `/template/templates/render-records/page` | 分页查询渲染记录。 |

扩展点：

- `ITemplateFileStore`：模板文件读写适配。默认实现是 `MangoFileTemplateFileStore`，通过 `FileApi` 保存和下载文件。
- `TemplateRenderManager`：调度 `RenderApi` 和 `ConvertApi`。

## 9. 数据与初始化
Flyway 路径：`mango-template-core/src/main/resources/db/migration/template`。

核心表：

| 表 | 用途 |
|----|------|
| `template_category` | 模板分类，`tenant_id + category_code` 唯一。 |
| `template` | 模板主表，含模板编码、分类、业务范围、草稿、当前版本和业务域。 |
| `template_version` | 已发布版本，`template_id + version_no` 唯一。 |
| `template_render_record` | 渲染记录，记录版本、输出格式、状态、文件 ID、变量快照和业务关联。 |

关键字段：

- `template.template_code`：运行时查找模板的稳定编码。
- `template.business_group`、`business_type`、`business_key`：业务范围字段，其中 `business_key` 在租户内唯一。
- `template.draft_*`：未发布草稿内容。
- `template_version.current_published`：当前发布版本标记。
- `template_render_record.output_file_id`：二进制输出文件 ID。
- `template_render_record.output_content`：TEXT 或 HTML 输出内容。

`V2__template_domain.sql` 给模板增加 `domain_code`，默认 `TEMPLATE`，并按分类编码回填。

## 10. 管理入口
模板分类、模板、版本和渲染记录都按 `tenant_id` 隔离。接口当前使用 `LOGIN` 访问模式，没有细粒度权限码；接入管理菜单时需要在 authorization 中为模板分类、模板、发布、渲染记录配置菜单和按钮权限。

业务调用渲染接口时必须确保模板属于当前租户，并且模板状态启用、存在当前发布版本。

## 11. 快速开始
1. 引入 `mango-template-starter`、`mango-file` 和 `mango-infra-fileproc`。
2. 创建模板分类。
3. 创建模板并设置 `templateCode`、业务范围、源格式和变量定义。
4. 文本模板保存 `content`，文档模板上传源文件并保存 `source_file_id`。
5. 发布模板版本。
6. 业务调用 `TemplateApi.render`，传入模板编码、输出格式、变量、`bizType`、`bizId`。
7. 读取返回文本或文件 ID，并检查渲染记录。

## 12. 问题排查
- 渲染提示格式不支持：检查源格式、输出格式和 fileproc `canRender`、`canConvert`。
- PDF/OFD 失败：检查 fileproc 转换引擎和 Aspose license。
- 找不到模板：确认租户、模板编码、状态和当前发布版本。
- 输出文件为空：检查模板内容、源文件 ID、变量是否完整。
- 变量缺失：先用变量提取接口生成建议，再补齐变量定义和请求参数。

## 13. 相关文档
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史资料
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
