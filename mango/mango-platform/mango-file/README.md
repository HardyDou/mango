# mango-file

`mango-file` 是平台统一文件资产模块，负责文件上传、下载、记录查询、预览入口、归档、存储配置、文件中心运行时策略和前端复用组件。它是独立能力域，不属于 `system` 系统管理模块。

本模块只管理“文件资产”：文件在哪里、谁能访问、有哪些版本、有哪些派生文件。Office 转 PDF、PDF 合并压缩、OCR、在线 Office 编辑、业务模板渲染不属于 `mango-file` 核心职责，应由 `mango-document` 或 `mango-template` 承担，处理结果再回写文件中心。

## 模块结构

- `mango-file-api`：文件业务码、枚举、Command、Query、VO、模块 API。
- `mango-file-core`：文件记录持久化、文件服务、本地存储实现、Flyway 迁移。
- `mango-file-starter`：自动配置、Web Controller、Swagger/Knife4j 模块声明。

## P0 能力

- `POST /file/files`：单文件上传。
- `POST /file/files/batch`：多文件上传。
- `GET /file/files/page`：文件记录分页。
- `GET /file/files/detail?id=1`：文件详情。
- `GET /file/files/preview?id=1`：预览元数据。
- `GET /file/files/download?id=1`：文件下载。
- `DELETE /file/files?id=1`：归档文件记录。默认不物理删除对象；开启物理删除策略时，会在没有其他文件记录引用同一对象后删除底层对象。
- `POST /file/files/uploads`：初始化分片上传。命中秒传时直接返回文件记录；未命中时返回上传会话。
- `POST /file/files/uploads/{sessionId}/parts/sign`：为 MinIO/S3 原生分片上传签发分片 PUT 地址。
- `POST /file/files/uploads/{sessionId}/parts`：后端接收分片，用于不支持原生分片的存储类型。
- `PUT /file/files/uploads/{sessionId}/parts`：登记已完成分片。
- `POST /file/files/uploads/{sessionId}/complete`：完成分片上传并创建文件记录。
- `DELETE /file/files/uploads/{sessionId}`：取消分片上传并清理对象存储会话或后端临时分片。
- `GET /file/storage-configs/page`：文件存储配置分页。
- `GET /file/storage-configs/detail?id=1`：文件存储配置详情。
- `POST /file/storage-configs`：新增文件存储配置。
- `PUT /file/storage-configs`：修改文件存储配置。
- `DELETE /file/storage-configs?id=1`：删除文件存储配置。
- `PUT /file/storage-configs/active?id=1`：设为默认文件存储配置。
- `POST /file/storage-configs/test`：测试文件存储配置。
- `GET /file/settings`：查询文件中心运行时策略。
- `PUT /file/settings`：保存文件中心运行时策略。
- `GET /file/directories/tree`：查询文件逻辑目录树。
- `POST /file/directories`：新增文件逻辑目录。
- `PUT /file/directories`：修改文件逻辑目录。
- `DELETE /file/directories?id=1`：删除空文件逻辑目录。
- 默认本地存储，支持 S3 兼容、MinIO、AWS S3、阿里云 OSS、腾讯云 COS、七牛云 Kodo。
- 文件记录按当前登录机构隔离，技术字段为 `tenant_id`，用户可见语义为“机构”。
- 存储配置是平台级基础设施配置，不按机构隔离；文件对象路径和文件记录继续按机构隔离。
- Knife4j/Swagger 分组路径：`/file`。
- 管理端一级菜单为“文件中心”，路由为 `/file/files`、`/file/storage-configs` 和 `/file/settings`，权限码统一使用 `file:*`。

## 机构隔离

文件记录表为 `file_record`，包含 `tenant_id`。上传时从当前登录上下文写入机构 ID，查询、详情、下载、归档均按当前机构范围访问。

文件对象路径包含机构前缀。默认对象命名策略为 `DATE_UUID`：

```text
{storage-path}/tenant-{tenantId}/yyyy/MM/dd/{uuid}.{ext}
```

如果启用 `HASH` 对象命名，路径为：

```text
{storage-path}/tenant-{tenantId}/sha256/{前2位}/{第3-4位}/{sha256}.{ext}
```

如果启用 `ORIGINAL` 对象命名，系统仍会在原始文件名前追加 UUID，避免不同上传覆盖同一底层对象。

当前实现不提供匿名公开下载，也不绕过机构隔离做平台全量查看。对象存储文件可以返回限时预签名 URL，让浏览器直连 MinIO/S3 下载或预览；生成签名前仍会经过后端登录态、机构、归档状态和文件记录校验。

## 内存与大文件约束

文件服务必须按流式处理设计，不允许把完整文件读入内存：

- 上传使用 `MultipartFile.getInputStream()` 或内部 `InputStream/Resource`，不得使用 `readAllBytes()` 作为通用路径。
- 需要计算哈希时按流读取；内部服务上传如果输入流不可重复读取，先流式落临时文件，再从临时文件计算哈希和写入对象存储，最后清理临时文件。
- 下载返回 `InputStreamResource`，不把对象内容转换为 `byte[]`。
- 预览接口只返回预览元数据、登录态下载地址、预签名 URL 或外部预览服务入口，不在 `mango-file` 内做 Office/PDF 转换。
- 浏览器大文件优先使用对象存储直传和分片上传；业务后端不应承担大文件中转流量。

## 配置

配置前缀：`mango.file`。这些配置作为安装默认值和兜底值；生产运行中的上传类型、大小限制、秒传、直传、访问有效期和预览服务地址应在“文件中心 / 文件配置”中维护。

```yaml
mango:
  file:
    enabled: true
    storage-type: LOCAL
    default-bucket: local
    local:
      root-path: ./data/files
    upload:
      max-size: 104857600
      allowed-extensions: []
      blocked-extensions:
        - exe
        - bat
        - cmd
        - sh
        - jar
      instant-upload-enabled: true
      direct-upload-enabled: false
      direct-upload-expire-seconds: 900
    access:
      token-enabled: false
      token-expire-seconds: 600
    preview:
      provider-url:
      expire-seconds: 600
      external-extensions:
        - doc
        - docx
        - xls
        - xlsx
        - xlsm
        - ppt
        - pptx
        - odt
        - ods
        - odp
        - ofd
        - wps
        - et
        - dps
        - csv
        - txt
        - zip
        - rar
        - 7z
        - eml
        - msg
```

存储配置支持 `storage_path` 路径前缀。对象最终路径由“存储桶 + 存储路径前缀 + 机构日期对象名”组成，便于后续按环境、业务域或数据生命周期做迁移和清理。

本地存储路径格式：

```text
{root-path}/{bucket}/{storage-path}/tenant-{tenantId}/yyyy/MM/dd/{uuid}.{ext}
```

## 逻辑目录

文件中心提供业务无关的逻辑目录 `file_directory`。目录只用于文件中心内的组织、筛选和上传落点，不表达业务类型。业务系统仍应只保存 `fileId` 以及自己的业务分类、附件类型、关联关系。

目录规则：

- 根目录 ID 为 `0`。
- 同一机构、同一父目录下目录名称唯一。
- 文件记录通过 `directory_id` 关联逻辑目录。
- 目录非空时不能删除，非空包含子目录或未归档文件。
- 归档文件默认不出现在普通列表，也不能预览下载。

## 文件中心运行时策略

`file_settings` 按机构保存一份文件中心策略，唯一键为 `tenant_id`。服务启动时如果没有运行时策略，会使用 `mango.file` 下的 yml 默认值。保存策略使用事务；首次并发创建由数据库唯一键兜底，冲突后服务会转为更新，保证同一机构只有一份有效配置。

当前策略项按生产文件服务常见能力拆分：

- 上传准入：单文件最大大小、允许扩展名、禁止扩展名、内容类型校验、允许/禁止 Content-Type。
- 默认行为：默认访问级别、重名处理策略、是否按目录隔离重名、对象命名策略。
- 命名与去重：按目录或机构范围处理重名；支持拒绝、自动重命名、允许重复；秒传按 SHA-256 复用底层对象。
- 直传访问：是否启用浏览器直传、直传签名有效期、限时访问令牌、公开读取是否仍强制签名。启用限时访问令牌后，MinIO/S3 文件预览接口会返回预签名 GET URL，浏览器直接访问对象存储，避免大文件流量经过业务系统。
- 预览策略：外部文档预览服务地址、预览有效期、交给外部预览服务的扩展名。
- 归档保留：是否保留归档记录、保留天数、归档时是否物理删除对象。

上传校验以后端策略为准，前端只做提前提示。高频上传路径读取的是按机构缓存的策略，策略保存后会清理对应机构缓存。

## 物理对象、秒传与分片上传

文件服务把业务文件记录和底层物理对象拆开：

- `file_record` 是业务上传记录，保存文件名、业务归属、访问级别、目录、归档状态，并通过 `object_id` 指向物理对象。
- `file_object` 是底层存储对象，保存 `storage_config_id`、`storage_type`、`bucket_name`、`object_name`、哈希、大小和引用数。
- `file_hash_mapping` 是秒传索引，按 `scope_type + tenant_id + storage_config_id + file_hash + file_size` 定位物理对象。

秒传不会再从历史 `file_record` 里按哈希任意复用对象，而是先解析当前生效存储配置，再只复用同一 `storage_config_id` 下的已完成物理对象。这样切换默认存储为 MinIO 后，不会命中旧的 LOCAL 记录，也不会返回本地代理地址。跨存储复制属于后续扩展能力，默认策略是严格复用目标存储内对象。

普通上传、批量上传和分片上传完成后都会写入同一套 `file_object` 与 `file_hash_mapping`。上传响应、详情、预览和下载都以 `object_id` 解析真实存储位置，并保留 `file_record` 上的存储字段作为历史兼容兜底。

分片上传分两种模式：

- `S3_MULTIPART`：MinIO/S3/AWS S3 使用对象存储原生 multipart。后端创建 uploadId、签发每个分片的预签名 PUT URL，前端直传对象存储，再把 ETag 登记给文件服务，最终由文件服务完成 multipart 并创建文件记录。
- `SERVER_CHUNK`：LOCAL、OSS、COS、Kodo 等尚未实现原生分片适配的存储，由后端接收分片到临时目录，完成时按序合并并写入当前存储。

归档语义：归档是逻辑移出正常使用范围。归档后默认不在普通列表展示，也不能通过普通预览和下载接口访问。历史记录仍保留，用于审计和追溯。物理删除是额外策略，默认关闭；开启后会先检查是否还有其他文件记录引用同一 `object_id`，没有引用时才删除底层对象并停用对应秒传映射。

MinIO/S3 直传和直连访问要注意签名域名：服务端内部读写可以使用内网 `endpoint`，但给浏览器使用的预签名 PUT/GET 必须用浏览器可访问的 `public_endpoint` 参与签名，不能签完内网地址后替换 Host。S3 V4 签名包含 Host，替换域名会导致签名失效。

本地模拟真实环境时可这样配置：

- `endpoint`：`http://127.0.0.1:9000`，后端服务内部上传、读取、测试连接使用。
- `public_endpoint`：`http://file.mango.io:9000`，浏览器预览/下载直连使用；本机 hosts 需要把 `file.mango.io` 指到 MinIO 所在机器。
- bucket 保持私有，不需要设置匿名 public policy；浏览器访问依赖预签名 URL 的有效期。

## 云存储配置

第三方存储配置保存在 `file_storage_config`，后台页面为“文件中心 / 存储配置”。配置中最多一个启用为默认，后续上传使用默认配置；文件记录会写入 `storage_config_id`、`storage_type`、`bucket_name`、`object_name`，因此切换默认存储后历史文件仍按原配置读取。

支持类型：

- `LOCAL`：本地磁盘。
- `S3`：S3 兼容对象存储。
- `MINIO`：MinIO，建议开启 Path Style。
- `AWS_S3`：AWS S3。
- `ALIYUN_OSS`：阿里云 OSS。
- `TENCENT_COS`：腾讯云 COS。
- `QINIU_KODO`：七牛云 Kodo。

本地 MinIO 联调可直接使用文件模块内置 Compose：

```bash
cd mango-platform/mango-file
docker compose up -d
```

Compose 会启动 MinIO 并创建 `mango-file` bucket：

- 存储类型：`MINIO`
- 接入地址：`http://127.0.0.1:9000`
- 存储桶：`mango-file`
- AccessKey：`minioadmin`
- SecretKey：`minioadmin`
- Path Style：开启
- 设为默认：启用

迁移脚本会内置一条“MinIO 本地联调”配置。本次文件服务改造后，清理历史文件数据并将本地 MinIO 设为默认存储；上传、详情和预览响应优先返回 MinIO/S3 预签名绝对地址。未启动 MinIO 的环境需要先启动 Compose，或在“文件中心 / 存储配置”中切回本地存储。

SecretKey 不会在详情接口明文返回；编辑时留空表示不修改。

## 能力边界

`mango-file` 保持收窄边界：

- 核心职责：上传、下载、存储适配、文件记录、逻辑目录、访问控制、归档、审计、版本、派生文件关系、预览入口。
- 可触发但不内置的职责：生成预览 PDF、缩略图、压缩文件、OCR 文本等派生文件。
- 不承担的职责：Office 转 PDF、PDF 合并/拆分/压缩/水印、OCR、在线 Office 编辑、模板变量管理、模板渲染出文档。

推荐模块关系：

```text
业务系统 -> mango-file
业务系统 -> mango-template -> mango-document -> mango-file
mango-file -> mango-document 生成预览派生文件
```

示例：Word 预览时，`mango-file` 校验文件权限并查询是否已有 PDF 派生文件；如果没有，调用 `mango-document` 转换；转换结果作为派生文件保存回 `mango-file`，前端最终预览 PDF。

## 文件预览

文件中心前端预览分两层：

- 浏览器内置预览：`image/*`、`video/*`、`audio/*`、`application/pdf`。
- 外部文档预览服务：Office、ODF、OFD、WPS、压缩包、邮件等复杂格式。

复杂文档预览不在文件服务内直接转换。推荐后续由 `mango-document` 提供 Office 转 PDF、缩略图或外部预览适配能力，`mango-file` 只负责权限校验、预览入口和派生文件管理。`ONLYOFFICE Docs` 更适合在线 Office 编辑和协同，应作为 `mango-document` 的可选 provider，而不是上传组件或文件服务核心逻辑。

前端优先使用“文件中心 / 文件配置”中的文档预览服务入口，例如 kkFileView 的 `onlinePreview` 地址；`VITE_FILE_PREVIEW_PROVIDER_URL` 只作为本地兜底。业务组件只依赖 `FilePreviewPanel`，不直接依赖具体预览服务。

注意：外部预览服务通常需要从服务端拉取原文件，不能直接使用需要 `Authorization` 请求头的 `/api/file/files/download`。MinIO/S3 文件会优先把预签名下载 URL 交给 kkFileView 或 ONLYOFFICE；不支持直链的存储再回退到登录态下载接口。

保函、合同、审批场景建议支持的重点格式：

- 图片：`jpg`、`jpeg`、`png`、`gif`、`webp`。
- 视频/音频：`mp4`、`mov`、`mp3`、`wav`，用于现场材料或沟通留痕。
- PDF / OFD：电子保函、发票、签章文件和监管版式文件。
- Office / WPS：`doc`、`docx`、`xls`、`xlsx`、`ppt`、`pptx`、`wps`、`et`、`dps`。
- ODF：`odt`、`ods`、`odp`。
- 文本与数据：`txt`、`csv`。
- 压缩包：`zip`、`rar`、`7z`，用于投标文件包、合同附件包。
- 邮件证据：`eml`、`msg`，用于沟通和送达留痕。

## 前端接入

前端通用上传组件默认接入 `/api/file/files`，统一使用 `@mango/file` 的单一 `Upload` 组件。组件通过配置表达上传场景，不再按 `ImageUpload`、`FileUpload`、`ExcelUpload` 拆分核心上传器。

推荐使用：

```vue
<Upload
  v-model="attachments"
  fmt="image,pdf,word,excel,zip"
  count="20"
  size="100MB"
  display="list"
  :columns="['fileName', 'fileSize', 'bizId', 'createdTime', 'actions']"
/>
```

关键规则：

- `fmt`：允许格式。不配置表示不限格式；支持 `image`、`video`、`audio`、`pdf`、`word`、`excel`、`ppt`、`archive`、`text`、`.ofd` 等分类或扩展名混用。
- `count`：数量限制。大于 1 时自动启用多文件选择，不再单独暴露 `multiple` 作为业务配置。
- `size`：单个文件大小限制。不配置时使用文件中心后台策略。
- `sizes`：按分类覆盖单文件大小限制，例如 `{ image: '10MB', video: '500MB' }`。
- `display`：展示方式，支持 `list`、`thumbnail`、`table`、`drag`。
- `columns`：`list` / `table` 显示列配置，支持文件名、大小、格式、用途、业务类型、业务 ID、上传账号、上传时间、存储方式、状态、操作列等。
- `bizType`、`bizId`、`purpose`、`directoryId`：上传上下文字段，会随文件记录保存。
- `bizMeta`：业务自定义参数，通过文件记录扩展字段保存 JSON。

组件默认返回 `mango-file:{id}` 文件标识，方便已有表单字段继续用字符串保存。需要完整文件记录时，可设置 `value-type="record"`，返回值包含 `id`、`fileName`、`fileSize`、`contentType`、`objectName`、`bizType`、`bizId`、`purpose`、`createdBy`、`createdTime`、`url` 等信息。

文件中心前端包为 `@mango/file`：

- `FileView`：文件上传、管理、预览、下载、归档页面。
- `FileStorageView`：文件存储配置页面。
- `FileSettingsView`：文件上传、访问、直传和预览策略配置页面。
- `FileUploadButton`：业务页面可复用上传按钮。
- `Upload`：业务页面、动态表单和文件中心复用的统一上传组件。
- `FilePreviewPanel`：业务页面可复用预览面板，支持内置预览和外部预览服务。

文件管理页面路径：`packages/file/src/views/files/index.vue`。
文件存储配置页面路径：`packages/file/src/views/storage-configs/index.vue`。
文件配置页面路径：`packages/file/src/views/settings/index.vue`。

文件中心页面按钮级权限：

- 文件管理：`file:files:upload`、`file:files:query`、`file:files:download`、`file:files:archive`。
- 目录管理：`file:directories:list`、`file:directories:add`、`file:directories:edit`、`file:directories:delete`。
- 存储配置：`file:storage-configs:add`、`file:storage-configs:edit`、`file:storage-configs:delete`、`file:storage-configs:test`、`file:storage-configs:active`。
- 文件配置：`file:settings:query`、`file:settings:edit`。

下载必须通过统一 request 发起 blob 请求，不能使用 `window.open('/api/file/files/download?id=1')`。原因是当前认证过滤器读取 `Authorization` 头，浏览器直接打开地址不会自动追加该头。

## 后续计划

P1：

- 旧公共上传组件逐步迁移到 `@mango/file/Upload`。
- 文件记录业务扩展字段继续完善检索和展示。
- 签名 URL 访问令牌落地。
- 云存储直传、分片直传落地。
- `PUBLIC_READ` 匿名读取。
- `INTERNAL` 内部访问边界。
- 文件下载审计。
- 文件版本和派生文件关系模型。

P2：

- 断点续传。
- 批量 ZIP 下载。
- 生命周期清理。
- 内容安全/病毒扫描扩展点。

不纳入 `mango-file` 的后续计划：

- Office 转 PDF、PDF 合并/拆分/压缩/水印、OCR、缩略图生成：规划到 `mango-document`。
- 模板分类、变量定义、模板版本、根据业务数据生成 Word/PDF：规划到 `mango-template`。
- 在线 Office 编辑：由 `mango-document` 以 provider 方式集成 ONLYOFFICE / Collabora / WPS WebOffice 等服务，`mango-file` 只提供文件读取、保存新版本和权限校验。
