# Mango Infra Fileproc

## 1. 概览
`mango-infra-fileproc` 是 Mango 的文件渲染、格式转换、PDF 处理和 Aspose license 装载基础能力。它只处理输入流或本地路径，不保存文件、不判断业务权限、不管理租户。

核心能力：

- 模板渲染：TEXT、HTML、DOCX、XLSX 等输入渲染为目标格式。
- 格式转换：HTML/TEXT、Office/PDF、PDF/图片、TIFF/PDF、同格式透传。
- PDF 操作：合并、加水印、压缩、压缩到目标大小。
- Aspose license 装载：Words、Cells、Slides、PDF、Imaging。
- LibreOffice/JODConverter Office 转 PDF。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 模板模块需要把模板和变量渲染为文档 | Maven 依赖 / starter / Java API |
| 文件预览需要把 Office、PDF、图片等转换成预览引擎可处理的格式 | Maven 依赖 / starter / Java API |
| 业务后台需要压缩 PDF、合并 PDF、加水印或导出文档 | Maven 依赖 / starter / Java API |
| 平台模块需要统一 Aspose license 装载方式 | Maven 依赖 / starter / Java API |


## 3. 能力边界
- 不提供 HTTP 上传、下载、预览入口。
- 不保存转换结果到 `mango-file`。
- 不校验文件归属、租户、菜单、按钮或接口权限。
- 不替业务模块决定转换后的文件生命周期和清理策略。

## 4. 模块入口
子模块：

- `mango-infra-fileproc-api`：`RenderApi`、`ConvertApi`、`AsposeLicenseApi`、命令、枚举和 VO。
- `mango-infra-fileproc-core`：渲染器、转换器、注册表、Aspose 和 LibreOffice 适配。
- `mango-infra-fileproc-starter`：自动配置和配置属性。

命令边界：

- `ConvertCommand` 只描述 `sourceFormat`、`targetFormat`、输入流或源路径、目标路径、文件名和 options。
- `RenderCommand` 只描述渲染输入、变量、变量定义、目标格式和目标路径。
- 两个命令都不包含文件中心 ID、存储位置、权限或租户信息。调用方必须先完成业务鉴权，再把输入交给 fileproc。

## 5. 接入方式
启用渲染、转换和 Aspose：

```xml
<dependency>
    <groupId>io.mango.infra.fileproc</groupId>
    <artifactId>mango-infra-fileproc-starter</artifactId>
</dependency>
```

只使用契约：

```xml
<dependency>
    <groupId>io.mango.infra.fileproc</groupId>
    <artifactId>mango-infra-fileproc-api</artifactId>
</dependency>
```

调用示例：

```java
ConvertResultVO result = convertApi.convert(ConvertCommand.builder()
        .sourceFormat(ConvertFormat.DOCX)
        .targetFormat(ConvertFormat.PDF)
        .sourcePath("/tmp/input.docx")
        .fileName("input.docx")
        .build());
```

```java
RenderResultVO result = renderApi.render(RenderCommand.builder()
        .sourceFormat(RenderFormat.HTML)
        .targetFormat(RenderFormat.PDF)
        .inputStream(templateStream)
        .fileName("contract.html")
        .variable("customerName", "Acme")
        .build());
```

## 6. 配置说明
### 6.1 渲染

前缀：`mango.fileproc.render`。

| 配置项 | 默认值 | 含义 |
|--------|--------|------|
| `enabled` | `true` | 是否启用渲染自动配置。 |
| `pdf-operations-enabled` | `true` | 是否启用 PDF 合并、水印、压缩等操作；关闭或缺少 `AsposeLicenseApi` 时使用 `UnsupportedRenderService`。 |

自动注册的渲染能力：

- `SameFormatRenderProvider`
- `HtmlToTextRenderProvider`
- `TextRenderProvider`
- `HtmlRenderProvider`
- `DocxRenderProvider`
- `OoxmlRenderProvider` for XLSX
- `AsposePdfRenderApi` for PDF 操作

### 6.2 转换

前缀：`mango.fileproc.convert`。

```yaml
mango:
  fileproc:
    convert:
      enabled: true
      html-to-text-enabled: true
      office-to-pdf-enabled: true
      aspose-word-to-pdf-enabled: true
      aspose-excel-to-pdf-enabled: true
      aspose-slide-to-pdf-enabled: true
      aspose-pdf-to-image-enabled: true
      aspose-imaging-enabled: true
      pdf-to-image-enabled: true
      tiff-to-pdf-enabled: true
      office-home:
      office-ports: [2001, 2002]
      office-task-execution-timeout: 300000
```

| 配置项 | 默认值 | 含义 |
|--------|--------|------|
| `enabled` | `true` | 是否启用转换自动配置。 |
| `html-to-text-enabled` | `true` | 注册 HTML 转文本转换器。 |
| `office-to-pdf-enabled` | `true` | 注册 LibreOffice/JODConverter Office 转 PDF。 |
| `aspose-word-to-pdf-enabled` | `true` | 注册 Aspose Word 转 PDF。 |
| `aspose-excel-to-pdf-enabled` | `true` | 注册 Aspose Excel 转 PDF。 |
| `aspose-slide-to-pdf-enabled` | `true` | 注册 Aspose PPT 转 PDF。 |
| `aspose-pdf-to-image-enabled` | `true` | 注册 Aspose PDF 转图片。 |
| `aspose-imaging-enabled` | `true` | 注册 Aspose 图片格式转换。 |
| `pdf-to-image-enabled` | `true` | 注册 PDF 转图片转换器。 |
| `tiff-to-pdf-enabled` | `true` | 注册 TIFF 转 PDF 转换器。 |
| `office-home` | 空 | Office 安装目录；为空时由 `LocalOfficeHomeResolver` 和 JODConverter 查找。 |
| `office-ports` | `[2001, 2002]` | LibreOffice 服务端口。 |
| `office-task-execution-timeout` | `300000` | Office 单任务超时时间，单位毫秒。 |

`LocalOfficeHomeResolver` 会尝试常见安装目录，包括项目目录下的 `LibreOfficePortable`、Linux 的 LibreOffice 目录和 OpenOffice 目录。

### 6.3 Aspose

前缀：`mango.fileproc.aspose`。

| 配置项 | 默认值 | 含义 |
|--------|--------|------|
| `enabled` | `true` | 是否启用 Aspose 自动配置。 |
| `license-location` | `classpath:/aspose/license.xml` | 通用 license 位置，支持 classpath、文件路径或目录。 |
| `words-license-location` | 空 | Aspose.Words license，空时回退到通用位置。 |
| `cells-license-location` | 空 | Aspose.Cells license，空时回退到通用位置。 |
| `slides-license-location` | 空 | Aspose.Slides license，空时回退到通用位置。 |
| `pdf-license-location` | 空 | Aspose.PDF license，空时回退到通用位置。 |
| `imaging-license-location` | 空 | Aspose.Imaging license，空时回退到通用位置。 |

默认 license 文件位置是 `mango-infra-fileproc-core/src/main/resources/aspose/license.xml`。资源目录下还有单独 README 说明 license 资产。

## 7. API 与扩展
公开 API：

- `RenderApi`：`render()`、`supportedFormats()`、`mergePdf()`、`addPdfWatermark()`、`compressPdf()`、`compressPdfToTarget()`。
- `ConvertApi`：`convert()`、`supportedFormats()`。
- `AsposeLicenseApi`：按 Aspose 产品装载 license。

格式枚举：

- `ConvertFormat`：TEXT、HTML、DOC、DOCX、XLS、XLSX、PPT、PPTX、PDF、OFD、PNG、JPEG、TIFF、ZIP。
- `RenderFormat`：TEXT、HTML、DOCX、XLSX、PDF、OFD、PNG、JPEG、ZIP。

扩展方式：

- 新增转换器实现 `IConvertProvider`，注册为 Spring Bean 后进入 `ConvertRegistry`。
- 新增渲染器实现 `IRenderProvider`，注册为 Spring Bean 后进入 `RenderRegistry`。
- 替换默认能力时提供同名或同类型 Bean，自动配置使用 `@ConditionalOnMissingBean`。

## 8. 数据与初始化
本模块无独立数据库、migration、菜单和默认业务数据。

初始化来自 Spring Boot 自动配置：

- `AsposeAutoConfiguration`
- `ConvertAutoConfiguration`
- `RenderAutoConfiguration`

模块元数据位于 `mango-infra-fileproc-starter/src/main/resources/META-INF/mango/module.properties`。

## 9. 管理入口
本模块没有菜单和权限码。

调用方必须在调用前完成：

- 文件或模板归属校验。
- 租户隔离校验。
- 下载、预览、导出或生成文件的业务权限校验。
- 输出文件是否写回 `mango-file` 的生命周期决策。

## 10. 快速开始
1. 业务模块先按自己的权限和租户规则读取源文件或模板。
2. 根据文件类型选择 `RenderApi` 或 `ConvertApi`。
3. 传入 `InputStream` 或本地 `sourcePath`，并设置 `sourceFormat`、`targetFormat`、`fileName`。
4. 验证返回的 `contentType`、文件名、输出流或目标路径。
5. 如果需要进入文件中心，再调用 `mango-file` 保存转换结果并建立业务关系。

## 11. 问题排查
- Office 转 PDF 失败：检查 `office-home`、`office-ports`、LibreOffice 安装路径和进程权限。
- Aspose 输出有水印或限制：检查 `mango.fileproc.aspose.*-license-location` 和 license 是否匹配产品。
- PDF 操作提示不支持：检查 `mango.fileproc.render.pdf-operations-enabled` 和 `AsposeLicenseApi` 是否存在。
- 转换结果未写入文件中心：这是调用方职责，需要用 `mango-file` 另行保存。
- 多租户文件被串用：fileproc 不识别租户，调用前必须由业务模块校验。

## 12. 相关文档
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 编码红线](../../../mango-pmo/rules/03-ai-coding-redlines.md)

## 13. 补充资料
- [能力地图](../../../mango-docs/capabilities/README.md)
- [Aspose license 说明](./mango-infra-fileproc-core/src/main/resources/aspose/README.md)
- [Mango File Preview](../../mango-platform/mango-file-preview/README.md)
