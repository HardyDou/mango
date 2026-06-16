# Mango File Preview

## 1. 概览

`mango-file-preview` 是 Mango 的文件在线预览入口。业务系统已经拿到 `fileId` 后，可以用它生成短期预览链接，让内置 kkFileView 预览引擎在有效期内读取 Mango 文件中心的源文件。

它不负责上传、下载、存储配置和文件业务归属，这些属于 `mango-file`。文件是否存在、当前租户是否可见、文件是否可下载，仍由 `mango-file` 判定。

## 2. 功能清单

| 能力 | 说明 |
|------|------|
| 创建预览链接 | `GET /file-preview/files/preview-link?fileId=...` 返回预览 URL、token 和有效期 |
| 直接跳转预览页 | `GET /file-preview/files/preview?fileId=...` 生成 source token 后 forward 到预览引擎 |
| 短期公开入口 | `GET /file-preview/files/preview-entry?token=...` 使用已签发入口 token 进入预览 |
| 源文件读取 | `GET /file-preview/sources/{token}` 供预览引擎读取源文件流 |
| 上下文保存 | token 中保存签发时的 `MangoContextSnapshot`，读取源文件时恢复上下文 |
| 引擎资源注册 | 启动时注册 `/onlinePreview`、`/pdfjs/**`、`/xlsx/**` 等公开资源 |
| 独立 UI 阻断 | 默认拦截 kkFileView 首页和演示文件管理入口 |

## 3. 后端接入

只使用契约时依赖 API 包：

```xml
<dependency>
    <groupId>io.mango.platform.file.preview</groupId>
    <artifactId>mango-file-preview-api</artifactId>
</dependency>
```

同进程启用文件预览能力时依赖 starter：

```xml
<dependency>
    <groupId>io.mango.platform.file.preview</groupId>
    <artifactId>mango-file-preview-starter</artifactId>
</dependency>
```

预览能力通常还需要文件中心和转换处理能力：

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

运行依赖：

| 依赖 | 用途 |
|------|------|
| `FileApi` | 查询文件元数据，读取源文件流 |
| `ITokenStore` | 保存 `file-preview:entry:*` 和 `file-preview:source:*` 短期 token |
| `ApiResourceApi` | 启动时注册预览引擎公开资源 |
| `mango-file-preview-engine` | 内置 kkFileView 预览引擎和静态资源 |

## 4. 前端接入

业务页面通常不直接拼 kkFileView 地址，而是用文件前端能力里的预览组件。

常见方式：

| 场景 | 做法 |
|------|------|
| 管理后台文件列表 | 使用 `@mango/file` 的文件管理页面和预览入口 |
| 业务详情页预览单个文件 | 使用 `FilePreviewPanel`，传入业务保存的 `fileId` |
| 自定义预览按钮 | 调 `/file-preview/files/preview-link?fileId=...`，打开返回的 `previewUrl` |

前端预览提供方可指向：

| 地址 | 说明 |
|------|------|
| `/file-preview/files/preview-link` | 返回 JSON，适合组件先拿链接再打开 |
| `/file-preview/files/preview` | 直接 forward 到预览页，适合新窗口打开 |

## 5. 快速开始

1. 后端接入 `mango-file-starter` 和 `mango-file-preview-starter`。
2. 如果需要 Office 转换能力，按环境接入 `mango-infra-fileproc-starter` 和对应转换依赖。
3. 业务上传文件后只保存 `fileId`。
4. 详情页用 `FilePreviewPanel` 或调用 `/file-preview/files/preview-link` 创建预览链接。
5. 当前用户必须具备 `file:files:query`，否则不能签发预览链接。
6. 预览引擎通过短期 source token 调 `/file-preview/sources/{token}` 读取源文件。

## 6. 配置说明

配置前缀是 `mango.file-preview`。

```yaml
mango:
  file-preview:
    enabled: true
    engine-path: /onlinePreview
    source-path: /file-preview/sources
    source-token-expire-seconds: 86400
    standalone-ui-enabled: false
```

## 7. YAML 配置字段

| 配置项 | 默认值 | 含义 |
|--------|--------|------|
| `mango.file-preview.enabled` | `true` | 是否启用文件预览自动配置 |
| `mango.file-preview.engine-path` | `/onlinePreview` | 预览引擎入口路径 |
| `mango.file-preview.source-path` | `/file-preview/sources` | 预览引擎读取源文件的短期 URL 前缀 |
| `mango.file-preview.source-token-expire-seconds` | `86400` | 入口 token 和源文件 token 有效期，单位秒 |
| `mango.file-preview.standalone-ui-enabled` | `false` | 是否允许访问 kkFileView 独立首页和演示文件管理入口 |

`mango-file-preview-engine` 也支持独立引擎端口配置：

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `mango.file-preview.engine.port` | `8012` | 预览引擎端口 |
| `MANGO_FILE_PREVIEW_ENGINE_PORT` | 无 | 环境变量形式的引擎端口 |
| `KK_SERVER_PORT` | 无 | kkFileView 兼容环境变量 |

## 8. 接口/API 使用

HTTP 接口前缀是 `/file-preview`。

| 方法 | 路径 | 访问模式 | 用途 |
|------|------|----------|------|
| GET | `/file-preview/files/preview-link?fileId=...` | PERMISSION，`file:files:query` | 创建短期预览入口 |
| GET | `/file-preview/files/preview?fileId=...` | PERMISSION，`file:files:query` | 按文件 ID forward 到预览页 |
| GET | `/file-preview/files/preview-entry?token=...` | PUBLIC | 使用已签发入口 token 进入预览 |
| GET | `/file-preview/sources/{token}` | PUBLIC | 预览引擎读取源文件流 |

Java API：

```java
R<FilePreviewLinkVO> response = filePreviewApi.preview(fileId);
```

token 行为：

| token | key 前缀 | 生成入口 | 用途 |
|-------|----------|----------|------|
| 入口 token | `file-preview:entry:` | `/files/preview-link` | 公开入口换取引擎预览链接 |
| 源文件 token | `file-preview:source:` | `/files/preview` 或 `/files/preview-entry` | 预览引擎读取源文件 |

## 9. 返回字段

`FilePreviewLinkVO`：

| 字段 | 含义 |
|------|------|
| `fileId` | 文件 ID |
| `fileName` | 原始文件名 |
| `previewUrl` | 可打开的预览页面地址 |
| `previewToken` | 预览入口临时令牌，直接引擎预览时为空 |
| `expireSeconds` | token 有效期，单位秒 |

错误码：

| 业务码 | 含义 |
|--------|------|
| `180001` | 文件 ID 不能为空 |
| `180002` | 预览令牌无效或已过期 |
| `180003` | 文件不存在或不可预览 |

## 10. 管理入口

`mango-file-preview` 不提供管理菜单。

权限和访问边界：

| 入口 | 边界 |
|------|------|
| 创建预览链接 | 要求当前用户有 `file:files:query` |
| 公开预览入口 | 只接受短期入口 token，不接受任意 `fileId` |
| 源文件读取 | 只接受短期 source token |
| 源文件权限 | 读取时恢复 token 中的上下文，再调用 `FileApi.downloadForService(fileId)` |
| kkFileView 独立 UI | 默认由 `standalone-ui-enabled=false` 阻断 |

## 11. 数据与初始化

本模块没有独立业务表和 Flyway migration。

启动初始化：

| 入口 | 内容 |
|------|------|
| `FilePreviewEngineResourceRegistrar` | 调用 `ApiResourceApi.registerApiResources()` 注册预览引擎公开资源 |
| `FilePreviewPermitPathBeanPostProcessor` | 给 Web 安全放行预览公开入口和 source 路径 |
| `FilePreviewSecurityCustomizer` | 配置预览相关安全放行 |
| `FilePreviewStandaloneUiBlockFilter` | 默认阻断 kkFileView 首页和演示文件管理入口 |

自动注册为 PUBLIC 的资源包括：

```text
/onlinePreview
/picturesPreview
/getCorsFile
/directory
/compressed-file
/file-preview/files/preview-entry
/pdfjs/**
/js/**
/css/**
/images/**
/bootstrap/**
/highlight/**
/xlsx/**
/static/**
/favicon.ico
```

## 12. 问题排查

| 现象 | 排查点 |
|------|--------|
| 创建预览链接失败 | 检查文件是否存在、当前账号是否有 `file:files:query`、租户上下文是否正确 |
| 预览页能打开但文件加载失败 | 检查 `/file-preview/sources/{token}` 是否被网关放行，token 是否过期 |
| Office 预览失败 | 检查预览引擎、LibreOffice、Aspose license 和 `mango-infra-fileproc` |
| 不希望暴露 kkFileView 首页 | 保持 `mango.file-preview.standalone-ui-enabled=false` |
| 反向代理下源文件 URL 不对 | 检查请求 scheme、host、port、context path，以及代理头是否正确传递 |
| 预览 token 过快失效 | 调整 `mango.file-preview.source-token-expire-seconds` |

## 13. 相关文档

- [Mango File](../mango-file/README.md)
- [Fileproc](../../mango-infra/mango-infra-fileproc/README.md)
- [@mango/file](../../../mango-ui/packages/file/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
