# Mango File Preview

## 1. 概览
`mango-file-preview` 是 Mango 文件在线预览入口。它基于 `fileId` 创建短期预览链接，让 kkFileView 预览引擎在有效期内读取 Mango 文件中心的源文件。

核心能力：

- 按 `fileId` 创建预览入口：`GET /file-preview/files/preview-link`。
- 按 `fileId` 直接 forward 到预览页：`GET /file-preview/files/preview`。
- 公开短期入口：`GET /file-preview/files/preview-entry?token=...`。
- 预览引擎读取源文件：`GET /file-preview/sources/{token}`。
- 启动时注册预览引擎需要的公开资源，例如 `/onlinePreview`、`/pdfjs/**`、`/xlsx/**`。
- 通过 filter 阻断 kkFileView 独立首页和演示文件管理入口，除非显式开启。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 文件中心上传后的 Office、PDF、图片、文本、压缩包等在线预览 | Maven 依赖 / HTTP API / Java API |
| 前端 FilePreviewPanel 需要把 fileId 转成可打开的预览页 URL | Maven 依赖 / HTTP API / Java API |
| admin 后台或业务详情页需要在保留 Mango 文件权限校验的前提下嵌入预览引擎 | Maven 依赖 / HTTP API / Java API |

## 3. 适用场景
- 文件中心上传后的 Office、PDF、图片、文本、压缩包等在线预览。
- 前端 `FilePreviewPanel` 需要把 `fileId` 转成可打开的预览页 URL。
- admin 后台或业务详情页需要在保留 Mango 文件权限校验的前提下嵌入预览引擎。

## 4. 边界说明
- 不负责文件上传、下载、存储配置和文件业务归属。
- 不替代 `mango-infra-fileproc` 的转换、渲染、PDF 操作和 Aspose license 装载能力。
- 不提供独立文件管理站点；`standalone-ui-enabled=false` 时会拦截 kkFileView 的首页和演示管理入口。

## 5. 模块组成
子模块：

- `mango-file-preview-api`：`FilePreviewApi`、`FilePreviewLinkVO`。
- `mango-file-preview-core`：预览 token、源文件读取、预览 URL 组装。
- `mango-file-preview-engine`：内置 kkFileView 预览引擎、静态资源和模板。
- `mango-file-preview-starter`：Controller、自动配置、安全公开路径注册。

依赖关系：

- 通过 `FileApi` 读取 `file_record` 元数据和源文件流。
- 通过 `ITokenStore` 保存 `file-preview:entry:*` 和 `file-preview:source:*` 短期 token。
- 通过 `MangoContextSnapshot` 保存签发 token 时的租户、用户和上下文。
- 文档转换和渲染能力由预览引擎及 `mango-infra-fileproc` 相关依赖承担。

## 6. 接入方式
同进程启用文件预览：

```xml
<dependency>
    <groupId>io.mango.platform.file</groupId>
    <artifactId>mango-file-preview-starter</artifactId>
</dependency>
```

通常同时需要：

```xml
<dependency>
    <groupId>io.mango.platform.file</groupId>
    <artifactId>mango-file-starter</artifactId>
</dependency>
<dependency>
    <groupId>io.mango.infra.fileproc</groupId>
    <artifactId>mango-infra-fileproc-starter</artifactId>
</dependency>
```

只使用契约时依赖 `mango-file-preview-api`。

## 7. 配置说明
配置类：`FilePreviewProperties`，前缀 `mango.file-preview`。

```yaml
mango:
  file-preview:
    enabled: true
    engine-path: /onlinePreview
    source-path: /file-preview/sources
    source-token-expire-seconds: 86400
    standalone-ui-enabled: false
```

| 配置项 | 默认值 | 含义 |
|--------|--------|------|
| `enabled` | `true` | 是否启用文件预览自动配置。 |
| `engine-path` | `/onlinePreview` | 预览引擎入口路径，`createEnginePreview()` 会把源文件 URL base64 后放到 `url` 参数。 |
| `source-path` | `/file-preview/sources` | 预览引擎读取源文件的短期 URL 前缀。 |
| `source-token-expire-seconds` | `86400` | 入口 token 和源文件 token 有效期，单位秒。 |
| `standalone-ui-enabled` | `false` | 是否允许访问 kkFileView 独立首页、演示文件管理等入口。 |

引擎独立启动配置：

- `mango.file-preview.engine.port` 默认 `8012`。
- 环境变量优先级来自 `mango-file-preview-engine.properties`：`MANGO_FILE_PREVIEW_ENGINE_PORT`、`KK_SERVER_PORT`。

前端 `FilePreviewPanel` 可通过 `previewProviderUrl` 或 `VITE_FILE_PREVIEW_PROVIDER_URL` 指向 `/file-preview/files/preview` 或 `/file-preview/files/preview-entry`。

## 8. API 与扩展
| 接口 | 权限 | 说明 |
|------|------|------|
| `GET /file-preview/files/preview-link?fileId=...` | `file:files:query` | 创建短期预览入口，返回 `previewUrl`、`previewToken`、`expireSeconds`。 |
| `GET /file-preview/files/preview?fileId=...` | `file:files:query` | 按文件 ID 创建源文件 token 并 forward 到 `engine-path`。 |
| `GET /file-preview/files/preview-entry?token=...` | PUBLIC | 使用已鉴权接口签发的入口 token 创建引擎预览。 |
| `GET /file-preview/sources/{token}` | PUBLIC | 预览引擎读取源文件流。 |

`FilePreviewLinkVO` 字段：

- `fileId`
- `fileName`
- `previewToken`
- `previewUrl`
- `expireSeconds`

`FilePreviewServiceImpl` 的 token 行为：

- `preview-link` 生成 `file-preview:entry:<token>`，公开入口再换成 source token。
- `preview` 直接生成 `file-preview:source:<token>`。
- token 存入 `ITokenStore`，内容包含 `fileId`、Mango 上下文快照和过期时间。
- 读取源文件时恢复 token 中的 Mango 上下文，再调用 `FileApi.downloadForService(fileId)`。

## 9. 数据与初始化
本模块没有独立业务表和 migration。

启动初始化：

- `FilePreviewEngineResourceRegistrar` 是 `ApplicationRunner`，会调用 `ApiResourceApi.registerApiResources()` 注册预览引擎公开资源。
- 注册的公开资源包括 `/onlinePreview`、`/picturesPreview`、`/getCorsFile`、`/directory`、`/compressed-file`、`/file-preview/files/preview-entry`、`/pdfjs/**`、`/js/**`、`/css/**`、`/images/**`、`/xlsx/**`、`/static/**`、`/favicon.ico`。
- `FilePreviewPermitPathBeanPostProcessor` 和 `FilePreviewSecurityCustomizer` 会放行 `/file-preview/files/preview-entry` 与 `/file-preview/sources/**`。

## 10. 管理入口
本模块不提供管理菜单。

权限和租户边界：

- 创建预览链接和直接预览都要求 `file:files:query`。
- 公开入口和源文件接口只接受短期 token，不接受任意 `fileId`。
- token 内保存签发时的 `MangoContextSnapshot`，源文件读取时恢复上下文后调用 `FileApi.downloadForService()`。
- 文件是否存在、租户是否可见、文件状态是否可下载，仍由 `mango-file` 判定。

## 11. 快速开始
1. 后端接入 `mango-file-starter`、`mango-file-preview-starter`，按需接入 `mango-infra-fileproc-starter`。
2. 文件配置中把 `previewProviderUrl` 设置为 `/file-preview/files/preview` 或让前端 `FilePreviewPanel` 请求 `/file-preview/files/preview-link`。
3. 业务上传文件后只保存 `fileId`。
4. 详情页用 `FilePreviewPanel :file-id="fileId"` 打开预览。
5. 后端用 `file:files:query` 校验预览链接签发，预览引擎通过短期 source token 读取源文件。

## 12. 问题排查
- 预览链接创建失败：检查文件是否存在、当前账号是否有 `file:files:query`、租户上下文是否正确。
- 预览页能打开但文件加载失败：检查 `/file-preview/sources/{token}` 是否被网关放行，以及 token 是否过期。
- Office 预览失败：检查预览引擎、LibreOffice、Aspose license 和 `mango-infra-fileproc`。
- 不希望暴露 kkFileView 首页：保持 `mango.file-preview.standalone-ui-enabled=false`。
- 反向代理下源文件 URL 不对：检查请求头、网关 context path，以及 `sourceUrl()` 组装出的 scheme、host、port。

## 13. 相关文档
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史资料
- [能力地图](../../../mango-docs/capabilities/README.md)
- [Mango File](../mango-file/README.md)
- [Fileproc](../../mango-infra/mango-infra-fileproc/README.md)
