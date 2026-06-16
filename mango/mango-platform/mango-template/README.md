# 模板 Template

## 1. 概览

`mango-template` 提供可版本化的业务模板能力，用于合同、函证、通知、单据、导出文档等需要由业务人员维护模板内容的场景。

它对外提供：

- 模板分类和模板管理。
- 草稿、发布版本和当前版本切换。
- 模板变量定义和变量提取。
- TEXT、HTML、DOCX、XLSX 模板渲染。
- TEXT、HTML、DOCX、XLSX、PDF、OFD 输出。
- 同步/异步渲染和渲染记录查询。

模板源文件和文档类渲染结果通过文件能力保存；Office 渲染和格式转换依赖 `mango-infra-fileproc`。

## 2. 功能清单

| 能力 | 说明 | 使用入口 |
|------|------|----------|
| 模板分类 | 维护分类编码、名称、排序和启停状态 | `TemplateCategoryApi` / `/template/categories/**` |
| 模板草稿 | 保存模板基本信息、源格式、文本内容或源文件 ID | `TemplateApi.create`、`TemplateApi.update` |
| 版本发布 | 将草稿发布成版本，并可启用历史版本 | `POST /template/templates/versions`、`PUT /template/templates/versions/current` |
| 变量提取 | 从 TEXT、HTML、DOCX、XLSX 内容或源文件提取变量建议 | `POST /template/templates/variables/extract` |
| 同步渲染 | 立即返回文本内容或输出文件 ID | `POST /template/templates/render` |
| 异步渲染 | 返回渲染记录，后台执行并记录状态 | `POST /template/templates/render/async` |
| 渲染记录 | 按模板编码、业务类型、业务 ID、状态查询记录 | `/template/templates/render-records/**` |

## 3. 后端接入

### 3.1 开发依赖

业务模块只需要面向模板 API 编码时，引入 `mango-template-api`：

```xml
<dependency>
    <groupId>io.mango.platform.template</groupId>
    <artifactId>mango-template-api</artifactId>
</dependency>
```

业务代码优先按 `templateCode` 渲染：

```java
import io.mango.template.api.TemplateApi;
import io.mango.template.api.command.TemplateRenderCommand;
import io.mango.template.api.enums.TemplateOutputFormat;

TemplateRenderCommand command = new TemplateRenderCommand();
command.setTemplateCode("CONTRACT_NOTICE");
command.setOutputFormat(TemplateOutputFormat.PDF);
command.getVariables().put("contractNo", "CT202606160001");
command.setBizType("CONTRACT");
command.setBizId("10001");

Long fileId = templateApi.render(command).getData().getFileId();
```

### 3.2 部署依赖

提供模板能力的应用启用 starter：

```xml
<dependency>
    <groupId>io.mango.platform.template</groupId>
    <artifactId>mango-template-starter</artifactId>
</dependency>
```

微服务中只远程消费模板能力的应用启用 remote starter：

```xml
<dependency>
    <groupId>io.mango.platform.template</groupId>
    <artifactId>mango-template-starter-remote</artifactId>
</dependency>
```

部署模板能力还需要：

| 依赖 | 用途 |
|------|------|
| `mango-file` | 保存模板源文件和文档类输出文件。 |
| `mango-infra-fileproc` | 提供 `RenderApi` 和 `ConvertApi`。 |
| fileproc 转换引擎和 license | 输出 PDF、OFD 等转换格式时需要。 |

## 4. 前端接入

模板前端能力在 `@mango/template`，属于 `admin-pages` 页面插件和 API 封装，不是官网或 C 端通用组件。

```ts
import { registerMangoTemplateAdminPages } from '@mango/template/admin-pages';

registerMangoTemplateAdminPages();
```

文件型模板页面依赖 `@mango/file` 上传模板源文件。业务前端如果只需要渲染模板，可使用 `templateApi.render`，但更推荐由业务后端调用 `TemplateApi`，避免前端直接承担业务幂等、权限和归档逻辑。

## 5. 快速开始

1. 部署应用启用 `mango-template-starter`、`mango-file` 和 `mango-infra-fileproc`。
2. 执行 template migration。
3. 前端注册 `@mango/template/admin-pages`。
4. 创建模板分类。
5. 创建模板，填写 `templateCode`、`templateName`、`domainCode`、分类和备注。
6. TEXT/HTML 模板保存内容；DOCX/XLSX 模板上传源文件并保存 `sourceFileId`。
7. 发布模板版本。
8. 业务后端调用 `TemplateApi.render`，传入 `templateCode`、输出格式、变量、`bizType`、`bizId`。
9. TEXT/HTML 读取返回 `content`；文档类输出读取返回 `fileId`。

## 6. 配置说明

模板模块只有 starter 启停开关。渲染线程池参数当前在自动配置中固定，不通过 YAML 暴露。

```yaml
mango:
  template:
    enabled: true
```

文件存储、Office 渲染和格式转换配置分别在 `mango-file`、`mango-infra-fileproc` 中维护。

## 7. YAML 配置字段

| 配置项 | 默认值 | 含义 |
|--------|--------|------|
| `mango.template.enabled` | `true` | 是否启用模板 starter，启用后注册 Mapper、Service、Controller、渲染管理器和渲染线程池。 |

## 8. 运行时配置字段

### 8.1 模板字段

| 字段 | 含义 |
|------|------|
| `templateCode` | 模板编码，业务渲染主键，租户内唯一。 |
| `templateName` | 模板名称。 |
| `categoryCode` / `categoryName` | 模板分类。 |
| `domainCode` | 业务域编码，必填。 |
| `sourceFormat` | 当前模板源格式：`TEXT`、`HTML`、`DOCX`、`XLSX`。 |
| `draftContent` | 未发布草稿文本或 HTML 内容。 |
| `draftSourceFileId` | 未发布草稿 DOCX 或 XLSX 源文件 ID。 |
| `draftVariables` | 未发布草稿变量定义。 |
| `status` | 模板状态。 |
| `remark` | 备注。 |

`businessGroup`、`businessType`、`businessKey` 是历史兼容字段，新调用统一使用 `templateCode`。

### 8.2 发布版本字段

| 字段 | 含义 |
|------|------|
| `templateId` | 模板 ID。 |
| `sourceFormat` | 内容稿源格式：`TEXT`、`HTML`、`DOCX`、`XLSX`。 |
| `content` | 文本或 HTML 模板内容。 |
| `sourceFileId` | DOCX 或 XLSX 模板源文件 ID。 |
| `versionRemark` | 版本说明。 |
| `variables` | 模板变量定义，支持嵌套结构。 |

### 8.3 渲染字段

| 字段 | 含义 |
|------|------|
| `templateCode` | 模板编码；不传 `versionNo` 时使用当前发布版本。 |
| `versionNo` | 指定模板版本号。 |
| `outputFormat` | 输出格式：`TEXT`、`HTML`、`DOCX`、`XLSX`、`PDF`、`OFD`。 |
| `variables` | 渲染变量 Map。 |
| `async` | 是否异步处理。 |
| `bizType` / `bizId` | 业务关联字段，用于查询渲染记录。 |

## 9. 请求与返回字段

### 9.1 模板分类接口

| 方法 | 路径 | 用途 |
|------|------|------|
| `GET` | `/template/categories/page` | 分页查询分类。 |
| `GET` | `/template/categories/list` | 查询分类列表。 |
| `GET` | `/template/categories/detail` | 查询分类详情。 |
| `POST` | `/template/categories` | 新增分类。 |
| `PUT` | `/template/categories` | 修改分类。 |
| `PUT` | `/template/categories/status` | 启停分类。 |
| `DELETE` | `/template/categories` | 删除分类。 |

### 9.2 模板接口

| 方法 | 路径 | 用途 |
|------|------|------|
| `GET` | `/template/templates/page` | 分页查询模板。 |
| `GET` | `/template/templates/detail` | 查询模板详情、草稿和版本信息。 |
| `POST` | `/template/templates` | 新增模板或保存草稿。 |
| `PUT` | `/template/templates` | 修改模板或草稿。 |
| `DELETE` | `/template/templates` | 删除模板。 |
| `PUT` | `/template/templates/status` | 启停模板。 |
| `POST` | `/template/templates/versions` | 发布模板版本。 |
| `PUT` | `/template/templates/versions/current` | 启用历史版本。 |
| `POST` | `/template/templates/variables/extract` | 提取变量建议。 |
| `POST` | `/template/templates/render` | 同步渲染模板。 |
| `POST` | `/template/templates/render/async` | 异步渲染模板。 |
| `GET` | `/template/templates/render-records/detail` | 查询渲染记录详情。 |
| `GET` | `/template/templates/render-records/page` | 分页查询渲染记录。 |

### 9.3 渲染返回字段

| 字段 | 含义 |
|------|------|
| `recordId` | 渲染记录 ID。 |
| `status` | 渲染状态：`PENDING`、`RUNNING`、`SUCCESS`、`FAILED`。 |
| `content` | 文本类渲染内容。 |
| `fileId` | 文档类渲染产物文件 ID。 |
| `fileName` | 文档类渲染产物文件名。 |
| `contentType` | 文档类渲染产物内容类型。 |
| `errorMessage` | 失败原因。 |

## 10. 管理入口

前端页面由 `@mango/template/admin-pages` 注册：

| 页面 | component key |
|------|---------------|
| 模板管理 | `system/template/index`、`template/templates/index` |
| 模板分类 | `template/categories/index` |
| 渲染记录 | `template/render-records/index` |
| 能力说明 | `debug/capabilities/template` |

当前后端模板接口未在 README 中登记细粒度 `@ApiAccess` 权限码。接入菜单时，需要在 authorization 中为模板分类、模板、发布、渲染记录配置菜单和按钮权限，并确保业务渲染接口受租户和登录态控制。

## 11. 数据与初始化

Flyway 路径：`mango-template-core/src/main/resources/db/migration/template`。

| 脚本 | 内容 |
|------|------|
| `V1__init_template.sql` | 创建 `template`、`template_category`、`template_version`、`template_render_record`，并包含历史结构变更的 squashed 内容。 |
| `V2__template_domain.sql` | 给模板增加 `domain_code`，默认 `TEMPLATE`，并创建 `tenant_id + domain_code` 索引。 |

核心表：

| 表 | 用途 |
|----|------|
| `template_category` | 模板分类，`tenant_id + category_code` 唯一。 |
| `template` | 模板主表，含模板编码、分类、业务域、草稿、当前版本和状态。 |
| `template_version` | 已发布版本，`template_id + version_no` 唯一。 |
| `template_render_record` | 渲染记录，记录版本、输出格式、状态、文件 ID、变量快照和业务关联。 |

## 12. 问题排查

| 问题 | 优先检查 |
|------|----------|
| 找不到模板 | 当前租户、`templateCode`、模板状态、当前发布版本。 |
| 渲染提示格式不支持 | 源格式、输出格式、fileproc 是否支持渲染和转换。 |
| PDF/OFD 输出失败 | fileproc 转换引擎和 license 是否可用。 |
| 文档类输出没有文件 ID | `mango-file` 是否可用，源文件是否存在，输出格式是否为文档类。 |
| 变量缺失 | 先用变量提取接口生成建议，再补齐变量定义和请求参数。 |
| 渲染记录为空 | 是否调用异步渲染或需要查询对应 `bizType`、`bizId`、`templateCode`。 |
| 页面打不开 | 前端是否注册 `@mango/template/admin-pages`，菜单 component 是否映射到页面 key。 |

## 13. 相关文档

- [前端模板包](../../../mango-ui/packages/template/README.md)
- [文件能力](../mango-file/README.md)
- [文件处理基础能力](../../mango-infra/mango-infra-fileproc/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
