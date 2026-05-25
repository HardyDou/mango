# mango-file 集成 file-preview 方案

更新时间：2026-05-24

## 1. 目标

让业务方只持有并传递 `fileId`，通过 `mango-file` 完成文件权限、机构隔离、归档状态和下载能力校验；通过可配置预览地址把文件交给 `mango-file-preview` 或其它预览服务完成真实在线预览。

本方案解决：

- 文件中心、模板、业务附件等场景可以按指定 `fileId` 打开在线预览。
- `mango-file` 预览地址配置支持绝对地址、相对地址、`fileId`、`fileUrl`、`fileName` 和安全相关基础参数。
- 前端复用 `FilePreviewPanel` 时不再自行拼 kkFileView 或第三方预览服务地址。
- 单体和微服务部署都能按相同语义访问预览能力。
- 业务数据继续只保存文件中心标识，不保存预览地址、下载地址或对象存储直连地址。

## 2. 范围

本次设计覆盖：

- `mango-file` 预览元数据接口语义调整。
- `mango-file` 预览地址模板构建规则。
- `mango-file-preview` 基于 `fileId` 的预览入口、源文件临时访问令牌和预览引擎调用链。
- 单体部署与微服务部署的依赖和调用方式。
- 前端 `@mango/file` 预览组件接入方式。
- 权限、安全、生命周期、测试验收口径。

不覆盖：

- Office 转 PDF、缩略图、OCR、在线编辑等派生产物持久化。
- 将 kkFileView 转换内核拆入 `mango-infra-tools-convert` 的实现。
- 业务模块批量迁移历史 URL 字段。

## 3. 现状判断

### 3.1 mango-file 现状

`mango-file` 已提供统一文件资产能力：

- 上传、分片上传、秒传、下载、详情、分页、归档。
- 文件记录按机构隔离，归档文件不能通过普通详情、预览、下载访问。
- `FileApi.preview(Long id)` 返回 `FilePreviewVO`，当前主要包含文件元数据、登录态下载地址、对象存储直连或预签名地址。
- `FilePreviewVO.previewUrl` 当前默认等于 `/file/files/download?id=...` 或对象存储直连地址，适合图片、PDF、音视频等浏览器内置预览，不足以承载 Office、压缩包、邮件等复杂格式预览。

### 3.2 mango-file-preview 现状

`mango-file-preview` 已是独立平台模块：

- `mango-file-preview-api` 提供 `FilePreviewApi.preview(Long fileId)`。
- `mango-file-preview-core` 依赖 `mango-file-api`，通过 `FileApi.get(fileId)` 校验文件可见性，通过 `FileApi.download(fileId)` 读取源文件。
- `mango-file-preview-starter` 暴露：
  - `GET /file-preview/files/preview-link?fileId=...`
  - `GET /file-preview/files/preview?fileId=...`
  - `GET /file-preview/sources/{token}`
- 预览源文件令牌保存在当前进程内存，令牌携带创建时的 `MangoContextSnapshot`，预览引擎回读源文件时恢复上下文后再调用 `FileApi.download`。
- 单体应用已同时装配 `mango-file-starter` 与 `mango-file-preview-starter`；微服务 `mango-file-preview-app` 已通过 `mango-file-starter-remote` 调用文件服务。

## 4. 核心决策

### 4.1 依赖方向

采用单向依赖：

```text
业务前端/业务服务 -> mango-file
前端预览入口 -> mango-file-preview
mango-file-preview -> mango-file-api
```

`mango-file` 不编译依赖 `mango-file-preview-api`，避免文件资产模块反向依赖预览适配模块。`mango-file` 只负责给出运行时预览入口地址；`mango-file-preview` 负责把 `fileId` 转为预览引擎可消费的源文件 URL。

### 4.2 预览入口归一

`mango-file` 的 `GET /file/files/preview?id=...` 继续作为业务统一查询入口。返回值保持 `FilePreviewVO`，但语义调整为：

- 浏览器原生支持类型：`previewUrl` 优先返回可直接嵌入的源文件访问地址。
- 复杂格式：`previewUrl` 按 `previewProviderUrl` 构建，默认返回 `/file-preview/files/preview?fileId={id}&fileUrl=...&fileName=...&expireSeconds=...`。
- `downloadUrl` 始终表达下载入口。
- `directPreviewUrl`、`directDownloadUrl` 只表达对象存储直连或预签名 URL，不用于复杂格式预览服务入口。

前端只消费 `FilePreviewVO.previewUrl`、`downloadUrl`、`direct*` 字段，不再根据扩展名自行拼 `VITE_FILE_PREVIEW_PROVIDER_URL`。

### 4.3 file-preview 只接收 fileId

`mango-file-preview` 对外只接收 `fileId`，不接收业务 URL、对象存储 URL、下载 URL 或任意外链。源文件访问只由内部令牌接口提供：

```text
GET /file-preview/files/preview?fileId={fileId}
  -> 生成 source token
  -> 拼 /file-preview/sources/{token}?fullfilename=...
  -> base64 后交给 /onlinePreview
```

这样可以保证预览链路复用 `mango-file` 的权限、机构、归档状态和存储适配能力。

## 5. 模块设计

### 5.1 mango-file

保留职责：

- 文件记录、物理对象、逻辑目录、上传下载、归档、机构隔离。
- 判断文件是否可预览。
- 返回运行时预览入口和下载入口。

需要调整：

- 增加 `mango.file.preview.provider-url` 的标准默认值，默认为 `/file-preview/files/preview`。
- `FileServiceImpl.preview(id)` 在复杂格式时，将 `previewUrl` 设置为按 `previewProviderUrl` 构建出的预览地址。
- `previewProviderUrl` 没有占位符时，自动追加 `fileId`、`fileUrl`、`fileName`、`expireSeconds` 参数。
- `previewProviderUrl` 有占位符时，支持 `{fileId}`、`{fileUrl}`、`{fileName}`、`{expireSeconds}` 原位替换。
- `FileRecordVO` 的列表/详情仍可返回下载或直连地址，但复杂格式最终预览以 `FilePreviewVO.previewUrl` 为准，避免列表接口过早生成预览令牌或复杂入口。
- `previewProviderUrl` 支持相对路径和绝对路径：
  - 单体：`/file-preview/files/preview`
  - 网关：`/api/file-preview/files/preview`
  - 独立域名：`https://preview.example.com/file-preview/files/preview`
  - URL 模板：`https://preview.example.com/onlinePreview?url={fileUrl}&name={fileName}`

不新增数据库表。现有 `file_settings.preview_provider_url`、`preview_expire_seconds`、`preview_external_extensions` 可继续承载运行时配置。

### 5.2 mango-file-preview

保留职责：

- 接收 `fileId`。
- 调用 `FileApi.get` 校验文件可见性并获取文件名、类型、大小。
- 生成源文件临时访问令牌。
- 调用预览引擎并返回预览页面。
- 通过 `FileApi.download` 读取源文件流。

需要增强：

- `FilePreviewProperties` 增加外部访问基准地址配置，例如 `publicBaseUrl`，用于微服务或网关场景下生成预览引擎可回调的源文件绝对地址。
- 源文件 token 使用 `mango-infra-kv` 存储，支持多实例部署和重启后令牌失效语义可控。
- `preview-link` 与 `preview` 两个接口保持：
  - `preview-link` 返回给 API 调用方。
  - `preview` 用于 iframe 或新窗口直接打开。
- 令牌只保存 `fileId`、上下文快照、过期时间，不保存下载 URL 或文件内容。

### 5.3 前端 @mango/file

调整方向：

- `FilePreviewPanel` 加载 `fileApi.preview(fileId)`。
- 图片、PDF、音视频仍以内联方式渲染。
- 复杂格式直接 iframe `preview.previewUrl`，不再由前端重新拼预览服务地址。
- `previewProviderUrl` 只作为历史兼容兜底，默认预览地址由后端生成。
- 下载继续调用 `downloadFileRecord`，优先使用 `directDownloadUrl`，否则走 `/file/files/download`。

前端组件只关心“预览地址是否可嵌入”，不关心 kkFileView、LibreOffice、对象存储或源文件令牌细节。

## 6. 调用链

### 6.1 单体部署

```text
Browser
  -> GET /file/files/preview?id=100
  -> mango-file 校验文件并返回 previewUrl=/file-preview/files/preview?fileId=100
  -> iframe GET /file-preview/files/preview?fileId=100
  -> mango-file-preview 本地调用 FileApi.get
  -> 生成 source token
  -> forward /onlinePreview?url=base64(sourceUrl)
  -> 预览引擎 GET /file-preview/sources/{token}
  -> mango-file-preview 本地调用 FileApi.download
```

### 6.2 微服务部署

```text
Browser
  -> Gateway /file/files/preview?id=100
  -> mango-file-app 返回 previewUrl=/file-preview/files/preview?fileId=100
  -> Browser/Gateway 访问 mango-file-preview-app
  -> mango-file-preview-app 通过 mango-file-starter-remote 调 FileApi.get/download
  -> 预览引擎回调 publicBaseUrl + /file-preview/sources/{token}
```

微服务部署必须保证预览引擎能访问 `sourceUrl`。如果预览引擎与服务端同进程，`publicBaseUrl` 可以走本服务外部地址；如果预览引擎独立进程，必须配置它可访问的网关或服务地址。

## 7. 接口变化

### 7.1 保持兼容的接口

```text
GET /file/files/preview?id={id}
GET /file-preview/files/preview-link?fileId={id}
GET /file-preview/files/preview?fileId={id}
GET /file-preview/sources/{token}
```

### 7.2 FilePreviewVO 字段语义

| 字段 | 语义 |
|---|---|
| `id` | 文件 ID |
| `fileName` | 原始文件名 |
| `fileExt` | 扩展名 |
| `fileSize` | 文件大小 |
| `contentType` | 内容类型 |
| `previewable` | 当前文件是否有可用预览路径 |
| `previewUrl` | 前端打开预览的入口。可能是文件下载地址、对象存储直连地址，或由 `previewProviderUrl` 构建出的预览服务地址 |
| `downloadUrl` | 下载入口 |
| `directAccess` | 是否存在对象存储直连能力 |
| `directPreviewUrl` | 对象存储直连预览地址，只用于源文件直连 |
| `directDownloadUrl` | 对象存储直连下载地址 |
| `directPreviewExpireSeconds` | 直连预览地址有效期 |
| `directDownloadExpireSeconds` | 直连下载地址有效期 |

不新增 `previewType` 字段。前端可根据文件类型和 `previewUrl` 渲染方式判断内联或 iframe。

## 8. 数据变化

不新增表，不新增 migration。

复用 `file_settings`：

- `preview_provider_url`：复杂格式预览入口。
- `preview_expire_seconds`：直连预览和预览源文件令牌有效期的默认参考值。
- `preview_external_extensions`：复杂格式扩展名集合。

建议安装默认值：

```text
preview_provider_url=/file-preview/files/preview
preview_external_extensions=doc,docx,xls,xlsx,xlsm,ppt,pptx,odt,ods,odp,ofd,wps,et,dps,csv,txt,zip,rar,7z,eml,msg
```

历史环境如未配置 `preview_provider_url`，复杂格式继续返回下载入口，前端显示“下载查看”。

## 9. 安全与生命周期

- 所有业务文件仍由 `mango-file` 校验机构、归档状态和访问权限。
- `mango-file-preview` 的 `preview`、`preview-link` 使用 `file:files:query` 权限。
- `sources/{token}` 是临时令牌接口，只供预览引擎读取源文件；令牌过期后必须拒绝访问。
- 源文件 token 不允许出现在业务表、业务扩展字段或长期缓存中。
- `FilePreviewVO.previewUrl` 是运行时派生地址，业务模块不能持久化。
- iframe 预览路径需要允许同源嵌入；当前 `FilePreviewFrameOptionsFilter` 保持对 `/file-preview/` 和 `/onlinePreview` 设置 `SAMEORIGIN`。
- 对象存储预签名 URL 只用于浏览器源文件直连，不作为复杂文档预览的跨模块契约。

## 10. 失败策略

| 场景 | 行为 |
|---|---|
| 文件不存在、跨机构、已归档 | `mango-file` 或 `mango-file-preview` 返回业务失败 |
| 未配置 `preview_provider_url` | 复杂格式回退为下载查看 |
| 预览源 token 过期 | `sources/{token}` 返回令牌无效 |
| 预览引擎转换失败 | 保持引擎错误页或错误响应，前端提供下载 |
| `mango-file-preview` 服务不可用 | `previewUrl` 可生成但打开失败，前端保留下载动作 |
| 多实例 token 丢失 | source token 必须使用 KV 存储并设置 TTL，不能继续依赖进程内 Map |

## 11. 实施步骤

本方案一次性交付完整能力，不按阶段交付半成品。实施时按以下步骤推进，每一步完成后继续下一步，最终统一验收。

### 11.1 统一预览入口

- 调整 `mango-file` 的复杂格式 `previewUrl` 生成逻辑。
- 将默认 `preview_provider_url` 配置为 `/file-preview/files/preview`。
- 调整 `FilePreviewPanel`，复杂格式直接使用后端返回的 `previewUrl`。
- 补充 `mango-file-preview-app` E2E，覆盖 `fileId -> preview -> source -> engine` 链路。

### 11.2 完成部署稳态

- `mango-file-preview` 增加 `publicBaseUrl`。
- 将 source token 存储从本地 Map 切换到 `mango-infra-kv`，过期时间使用 TTL。
- 增加多实例部署说明和配置样例。

### 11.3 接入派生产物能力

- 与 `mango-infra-tools-convert` 或文档转换模块对接。
- `mango-file` 负责保存原文件和预览产物关系。
- 对高成本格式支持异步转换、缓存、重试和失败状态。

## 12. 验收标准

- 上传 `txt/docx/pdf/png/mp4` 等文件后，文件中心可以通过 `fileId` 打开预览或下载。
- `GET /file/files/preview?id=...` 对复杂格式返回 `/file-preview/files/preview?fileId=...` 风格的预览入口。
- 前端 `FilePreviewPanel` 不需要配置第三方 provider URL 即可打开复杂格式预览入口。
- `mango-file-preview` 只能通过 `fileId` 获取源文件，不能接收任意外部 URL。
- 已归档文件、跨机构文件不能预览。
- 单体应用和 `mango-file-preview-app` 微服务形态均有验证命令或测试记录。

## 13. 预期验证方式

- 后端单元/集成：
  - `mvn -pl mango/mango-platform/mango-file/mango-file-core test`
  - `mvn -pl mango/mango-app/microservice/mango-file-preview-app test`
- 后端质量：
  - `mvn -pl mango/mango-platform/mango-file-preview,mango/mango-platform/mango-file -am mango:check -Drule=all`
- 前端：
  - `pnpm --dir mango-ui test`
  - `pnpm --dir mango-ui lint`
- 手工验收：
  - 文件中心上传复杂格式文件。
  - 打开详情预览面板。
  - 确认 iframe 访问 `/file-preview/files/preview?fileId=...`。
  - 确认下载按钮仍可用。

## 14. 风险

- 当前 source token 是进程内存，微服务多实例下预览引擎回调如果落到不同实例会失败。
- `FileFeignClient.downloadResponse` 当前以 `byte[]` 接收远程下载，大文件预览会占用内存；微服务预览链路需要改为流式 Feign 或优先使用对象存储后端读取能力。
- `mango-file` README 中存在由 render/convert 负责复杂转换的描述，需要在实现时区分“在线预览入口”和“派生产物转换”。
- 预览引擎依赖 LibreOffice/字体/系统库，不同部署环境的格式支持范围可能不同。
