# 文件能力基础模块计划

更新时间：2026-05-09

## 任务来源

参考旧项目：

- `/Users/hardy/Work/baohan/baohan/mango-api/mango-file`

为当前 `mango` 增加统一文件能力。旧模块只能作为能力参考，不直接整体搬迁；新实现必须遵循当前 `mango` 的模块分层、权限模型、机构隔离和 Flyway 迁移规范。

## 目标

文件能力作为平台基础能力，向系统管理、组织、授权、后续业务模块统一提供文件上传、下载、元数据管理和存储适配能力。

业务模块只依赖文件 API，不直接关心本地磁盘、MinIO、OSS、COS、S3 等具体存储实现。

## 模块规划

建议新增平台模块：

- `mango-platform/mango-file/mango-file-api`
  - 文件记录 VO、Command、Query。
  - `FileClient` 或领域服务接口。
  - 文件状态、访问级别、存储类型枚举。
- `mango-platform/mango-file/mango-file-core`
  - 文件记录持久化。
  - 文件上传、下载、归档、元数据查询。
  - 存储适配接口与本地存储实现。
  - 机构隔离、公开文件、私有文件访问校验。
- `mango-platform/mango-file/mango-file-starter`
  - Web Controller。
  - Swagger/Knife4j 中文接口文档。
  - 自动配置和默认本地存储配置。

首期先不放到 `mango-infra`，因为文件记录、访问权限、机构边界、审计都属于平台业务基础能力；底层存储适配抽象可以保持干净，后续如需要可下沉到 infra。

## 能力范围

### P0：先完成可替代前端和业务接入的基础能力

- 单文件上传。
- 多文件上传。
- 文件详情查询。
- 文件下载。
- 文件预览元数据。
- 文件归档/删除。
- 文件记录分页查询。
- 默认本地存储。
- 机构内私有文件隔离。
- 平台管理员可查看平台范围文件记录。
- Swagger/Knife4j 可直接调试。
- 前端通用上传组件可接入，并替换原 mock/占位上传地址。
- 文件管理页面可完成上传、查询、预览元数据、下载、归档。

### P1：补正式存储能力

- S3 兼容存储适配，覆盖 MinIO、阿里云 OSS、腾讯云 COS、AWS S3。
- 签名 URL。
- 文件 hash、大小、扩展名、MIME 校验。
- 文件访问级别：
  - `PRIVATE`：仅所属机构或授权主体可访问。
  - `PUBLIC_READ`：公开读取，写入仍需授权。
  - `INTERNAL`：仅系统内部接口使用。
- 文件用途字段，例如头像、附件、合同、资质材料。
- 上传大小、扩展名、MIME 白名单配置。

### P2：补大文件和批处理能力

- 分块上传。
- 断点续传。
- 云存储直传。
- 批量下载 ZIP。
- 文件生命周期清理任务。
- 病毒扫描/内容安全扩展点。

## 旧模块参考结论

旧 `mango-file` 已具备这些可复用设计：

- `FileTemplate` 存储抽象。
- `LocalFileTemplate` 本地存储。
- `S3FileTemplate` S3 兼容存储。
- `sys_file_record` 文件记录表。
- 单文件、多文件、分块、批量下载接口。
- `file.provider.type` 配置模型。

不能直接照搬的点：

- 包名是 `com.mango`，当前项目使用 `io.mango`。
- 旧接口返回体没有统一使用当前 `R<T>` 规范。
- 旧权限边界较弱，下载和详情需要补机构/公开/内部访问控制。
- 旧表字段缺少当前平台需要的 `institution/tenant` 语义说明、访问级别、业务归属、归档语义。
- 旧 controller 中文描述不够完整，需要按当前 API 规范补齐每个接口和参数说明。
- 旧迁移文件在模块根路径，当前必须按模块 Flyway 规范放入 `db/migration/file` 或项目约定路径。

## 推荐接口

### 上传

- `POST /file/files`
  - 单文件上传。
  - `multipart/form-data`。
  - 参数：`file`、`purpose`、`accessLevel`、`bizType`、`bizId`。

- `POST /file/files/batch`
  - 多文件上传。
  - `multipart/form-data`。

### 查询

- `GET /file/files/page`
  - 文件记录分页。
  - 默认按当前机构过滤。

- `GET /file/files/{fileId}`
  - 文件详情。

- `GET /file/files/{fileId}/preview`
  - 预览元数据。
  - 返回文件名、MIME、大小、可预览类型、临时访问地址。

### 访问

- `GET /file/files/{fileId}/download`
  - 文件下载。

- `GET /file/files/{fileId}/url`
  - 获取临时访问地址。

### 删除/归档

- `DELETE /file/files/{fileId}`
  - 默认归档文件记录，不直接物理删除对象。

- `DELETE /file/files/{fileId}/object`
  - 物理删除对象，仅平台管理员或内部维护接口使用。

## 权限边界

- 上传必须登录。
- 私有文件只允许所属机构、创建人、被授权主体访问。
- 公开文件允许匿名读取，但文件元数据维护仍需登录。
- 内部文件不对外开放，只允许内部服务或平台维护能力访问。
- 跨机构访问默认拒绝。
- 下载接口需要记录审计日志。
- 物理删除需要高权限，不作为普通业务页面默认能力。

## 数据表建议

文件记录表建议命名：

- `sys_file_record`

核心字段：

- `id`
- `tenant_id`
- `biz_type`
- `biz_id`
- `purpose`
- `access_level`
- `storage_type`
- `bucket_name`
- `object_name`
- `file_name`
- `file_ext`
- `file_size`
- `content_type`
- `file_hash`
- `status`
- `archived`
- `created_by`
- `created_time`
- `updated_by`
- `updated_time`

## 当前执行记录

### 2026-05-09 P0 已实现

- 新增 `mango-platform/mango-file`，按 `api/core/starter` 拆分。
- 单体服务已接入 `mango-file-starter`。
- 新增 `sys_file_record` Flyway 迁移，迁移路径为 `db/migration/file`。
- 新增文件管理菜单 Flyway 迁移。
- 文件接口已提供上传、批量上传、分页、详情、预览元数据、下载、归档。
- 前端新增文件管理页面。
- 前端通用上传组件默认接入 `/api/file/files`，不再使用旧 `/admin/upload/*` 地址。
- 前端上传组件完成真实文件接口联调：
  - `FileUpload` 默认返回 `mango-file:{id}`，可通过 `valueType=record` 返回完整文件记录。
  - `ImageUpload` 默认返回 `mango-file:{id}`，支持通过统一 request 拉取 blob 预览，避免预览时丢失认证头。
  - `ExcelUpload` 上传原始 Excel 到文件模块，同时保留本地解析预览能力，原始文件记录通过 `upload-success` 事件返回。
  - `Upload` 基础拖拽组件默认走统一文件模块，并把上传结果回填到 `fileList`。
- 文件下载改为统一 request blob 下载，避免 `window.open` 无法携带 `Authorization` 头。
- 新增 `mango-platform/mango-file/README.md`。

### 2026-05-09 P0 边界说明

- 当前用户可见术语是“机构”，底层字段仍为 `tenant_id`。
- 当前实现按机构隔离，不做平台机构绕过查询全部文件。
- `PUBLIC_READ` 作为访问级别先保留，匿名公开下载暂未开放。
- 文件归档只修改记录状态，不物理删除存储对象。

## 验收

### 后端验证

- Flyway 正常执行，不禁用迁移。
- 单体服务 `5555` 可启动。
- Knife4j 显示“文件管理-文件管理接口”中文分组。
- 上传、详情、预览、下载、归档接口返回正常。
- 未登录上传返回 401。
- 无权限跨机构访问私有文件返回 403。
- 公开文件匿名下载可用（P1 开放）。

### 前端 E2E

- 通用上传组件使用真实接口上传。
- 现有 `FileUpload`、`ImageUpload`、`ExcelUpload` 相关 API 使用 `/file/files` 文件能力接口。
- 上传成功后页面展示文件名、大小、状态。
- 点击下载可以获取文件。
- 删除/归档后页面刷新不再显示。
- A 公司无法访问芒果集团私有文件。

## 执行顺序

当前作为插入任务进入系统基础待办。执行时必须先完成当前进行中的机构模型任务验收，再进入文件能力实现，避免机构边界和文件权限边界同时变更导致验证失焦。
