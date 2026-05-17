# 文件服务收敛与后续任务计划

更新时间：2026-05-17

## 当前结论

`mango-file` 定位为文件资产服务，只负责文件记录、存储、访问、目录、权限、归档、版本和派生关系。文档处理和模板渲染不放在文件服务内：

- Office 转 PDF、PDF 合并/拆分/压缩/水印、OCR、缩略图：归到 `mango-document`。
- 模板分类、变量定义、模板版本、按业务数据生成 Word/PDF：归到 `mango-template`。
- 在线 Office 编辑：由 `mango-document` 作为 provider 适配，`mango-file` 只提供文件读写、权限校验、新版本保存。

## 已完成基础能力

- 后端模块已按 `mango-file-api / mango-file-core / mango-file-starter` 拆分。
- 管理端已作为一级菜单“文件中心”，不再挂在 system 下。
- 已支持文件上传、批量上传、分页、详情、预览元数据、下载、归档。
- 已支持逻辑目录、文件配置、存储配置。
- 已支持本地存储、S3 兼容、MinIO、AWS S3、阿里云 OSS、腾讯云 COS、七牛云 Kodo。
- MinIO 本地联调已支持内部 endpoint 与浏览器 public endpoint 分离，浏览器地址使用 `http://file.mango.io:9000`。
- 文件表名已从 `sys_file_*` 收敛为 `file_*`。
- 前端 Long ID 统一按字符串处理。
- README 已明确文件服务边界，不再把文档处理和模板能力塞入文件服务。
- 上传、下载、预览入口按流式处理设计，禁止把完整文件一次性加载到内存。

## P0：当前收尾任务

| 任务 | 内容 | 验收 |
|---|---|---|
| 表名规范收尾 | 确认实体、迁移、README、运行库全部使用 `file_*`；保留兼容迁移将旧 `sys_file_*` rename 到新表 | 后端编译通过；文件接口使用新表；E2E 上传/预览/下载通过 |
| Maven 启动问题修复 | 修正根项目 `spring-boot:run` 执行到 parent POM 的问题，保证指定 `:mango-monolith-app` 可直接启动 | `mvn -pl :mango-monolith-app -am spring-boot:run` 不再寻找 parent mainClass |
| 文件配置 YML/DB 优先级确认 | 明确数据库租户配置优先，YML 作为默认兜底 | README 已说明；接口返回 `defaultConfig` 表意清楚 |
| 测试数据清理 | E2E 产生的文件记录、目录、MinIO 对象清理 | `file_record` / `file_directory` 无 `mango-file-e2e%` 残留 |

## P1：文件服务核心增强

| 任务 | 内容 | 验收 |
|---|---|---|
| 统一前端 Upload 组件 | `@mango/file` 提供单一 `Upload.vue`，不按 Image/File/Excel 拆核心上传器 | 文件中心页面、工作流动态表单、业务页面可复用同一个组件 |
| Upload 简化配置 | 支持 `fmt`、`count`、`size`、`sizes`、`display`、`columns` | 不配置 `fmt` 表示不限格式；`count>1` 自动多文件；list/table 可配置显示列 |
| 上传业务上下文 | 上传支持 `bizType`、`bizId`、`purpose`、`directoryId`、`meta` | 上传记录可展示业务 ID、用途、上传时间、账号、自定义参数 |
| 文件记录扩展字段 | 增加 `biz_meta`，保存业务自定义参数 JSON；保留标准字段独立查询 | 详情和分页返回 `bizMeta`；业务自定义参数不污染固定列 |
| 前端旧组件兼容 | 旧 `ImageUpload/FileUpload/ExcelUpload` 暂时转发到新 Upload，避免一次性破坏旧页面 | 构建通过；旧引用可用；新增业务推荐使用 `Upload` |
| Browser Upload API | 面向浏览器，保留 multipart 上传、目录、业务上下文、权限和策略校验 | 登录态、按钮权限、租户隔离、格式大小校验均生效 |
| Internal File Save API | 面向后端服务上传，支持 `InputStream/Resource` 保存系统生成文件 | `mango-document` / `mango-template` 可通过 Java API 保存生成文件，不模拟前端 multipart |
| 统一底层 FileAssetService | Browser API 和 Internal API 共用对象命名、存储写入、hash、记录创建、权限字段处理 | 不复制两套存储逻辑 |
| 大文件内存约束 | 上传、哈希、下载、预览代理必须流式处理；内部上传使用临时文件或分片中转，不使用 `byte[]` 承载完整文件 | 代码审查无 `readAllBytes` / 全量 `byte[]` 保存大文件；大文件上传内存平稳 |
| 文件下载审计 | 记录预览、下载、归档等访问行为 | 审计记录可按文件 ID、用户、时间查询 |

## P2：大文件与对象存储增强

| 任务 | 内容 | 验收 |
|---|---|---|
| 浏览器直传 | 服务端生成 MinIO/S3 预签名 PUT URL，浏览器直传对象存储 | 上传大文件不经过业务后端流量 |
| 分片上传 | 支持 init、part sign、complete、abort、list parts | 大于阈值文件自动分片；失败可恢复 |
| 断点续传 | 基于 uploadId、part 列表、hash 校验恢复上传 | 刷新页面后可继续上传未完成文件 |
| 秒传增强 | 基于 SHA-256 支持租户内或全局复用底层对象 | 相同文件无需重复上传对象 |
| 文件版本模型 | 支持同一业务附件多版本、系统生成新版本 | 版本可追溯，默认下载最新或指定版本 |
| 派生文件关系 | 支持原文件、预览 PDF、压缩版、缩略图等关系 | `mango-document` 产物能挂回原文件 |
| 生命周期清理 | 清理过期临时上传、过期预签名任务、按策略归档/删除 | 定时任务可观测、可重试 |
| 内容安全扩展点 | 预留病毒扫描、敏感内容检测、文件类型真实识别 | 可插拔，不阻塞基础上传链路 |

## 双入口策略

### Browser Upload API

面向浏览器用户：

- 使用登录态、租户/机构上下文和按钮权限。
- 必须执行格式、大小、Content-Type、目录、归档策略校验。
- 支持 `bizType`、`bizId`、`purpose`、`directoryId`、`meta`。
- 支持后续直传、分片、断点续传。
- 审计记录为用户上传。

### Internal File Save API

面向内部后端服务：

- 不模拟 multipart 前端请求。
- 提供 Java API，必要时再补内部 REST/Feign。
- 输入为 `InputStream` 或 `Resource`；小文件可提供便捷重载，但底层不得依赖全量 `byte[]`。
- 权限使用服务身份和调用方授权，不走按钮权限。
- 仍必须写入同一套文件记录、存储配置、对象命名、hash、审计和租户字段。
- 支持 `source=USER_UPLOAD / SYSTEM_GENERATED / DOCUMENT_DERIVED`、`sourceFileId`、`bizType`、`bizId`、`purpose`、`meta`。

### 内存与大文件规则

- 禁止使用 `readAllBytes()`、`ByteArrayOutputStream` 或 `FileReader` 作为通用文件处理路径。
- 后端浏览器上传使用 `MultipartFile.getInputStream()`，哈希计算和对象写入均为流式读取。
- 内部服务上传如果输入流不可重复读取，先流式写入临时文件，再从临时文件计算哈希和上传对象；临时文件必须在事务结束后清理。
- 下载接口返回 `InputStreamResource`，不把对象内容转成 `byte[]`。
- 预览入口只返回元数据、登录态下载地址、预签名 URL 或外部预览服务地址；文件转换不在 `mango-file` 内完成。
- 前端通用 `Upload` 不做 Excel/Word/PDF 内容解析，避免大文件在浏览器内存中展开；业务确需解析时应由专用业务组件明确限制文件大小。

## 前端 Upload 目标 API

```vue
<Upload
  v-model="attachments"
  fmt="image,pdf,word,excel,zip"
  count="20"
  size="100MB"
  display="list"
  biz-type="expense"
  :biz-id="expenseId"
  :biz-meta="{ stage: 'apply', category: 'invoice' }"
  :columns="['fileName', 'fileSize', 'bizId', 'createdBy', 'createdTime', 'actions']"
/>
```

规则：

- `fmt` 不传表示允许所有格式；支持分类和扩展名混用。
- `count` 控制数量，大于 1 自动多选。
- `size` 是单文件大小限制，默认使用文件中心后台策略。
- `sizes` 支持按分类覆盖单文件大小，例如 `{ image: '10MB', video: '500MB' }`。
- `display` 支持 `list / thumbnail / table / drag`。
- `columns` 控制列表列，支持文件名、大小、格式、用途、业务类型、业务 ID、上传账号、上传时间、存储方式、状态、操作。

## 不做事项

- 不在文件服务内实现 Word 转 PDF。
- 不在文件服务内实现 PDF 合并、拆分、压缩、水印。
- 不在文件服务内实现 OCR。
- 不在文件服务内实现模板变量和模板渲染。
- 不在上传组件内集成 ONLYOFFICE / WPS / Collabora。

这些能力已拆到 [文档处理与模板中心能力规划](./document-template-capability-plan.md)。
