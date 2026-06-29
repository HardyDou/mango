# Mango File 使用说明

`mango-file` 是 Mango 的文件中心。业务系统用它统一处理上传、文件记录、下载、预览、逻辑目录、存储配置、上传策略、秒传和分片上传。

业务模块只保存 `fileId` 或自己的附件关联关系，不保存 bucket、objectName、对象存储地址、预签名 URL、下载 URL 或预览 URL。访问地址由文件中心按当前配置实时生成。

## 1. 概览

`mango-file` 对业务开发者提供一个黑盒文件能力：

- 前端上传文件，拿到 `fileId`。
- 业务表保存 `fileId` 或附件关系。
- 详情页按 `fileId` 回显文件名、大小、预览和下载入口。
- 后端生成文件时，通过 `FileApi.save()` 写入文件中心。
- 管理员在文件中心配置存储、大小限制、扩展名、秒传、直传、预览和归档策略。

不属于 `mango-file` 的能力：Office/PDF 转换、OCR、在线编辑、模板渲染。文档预览和文件加工分别看 `mango-file-preview`、`mango-infra-fileproc`。

## 2. 功能清单

| 能力 | 用途 | 常用入口 |
|------|------|----------|
| 单文件上传 | 上传附件、图片、合同、凭证等文件 | `POST /file/files` |
| 批量上传 | 一次上传多个小文件 | `POST /file/files/batch` |
| 文件记录 | 查询文件列表、详情、业务标记和状态 | `GET /file/files/page`、`GET /file/files/detail` |
| 下载 | 按 `fileId` 下载当前账号有权访问的文件 | `GET /file/files/download`、`FileApi.download()` |
| 预览 | 获取预览地址、下载地址和直连地址 | `GET /file/files/preview` |
| 后端保存文件 | 后端生成 PDF、Excel、归档包后写入文件中心 | `FileApi.save(SaveFileCommand)` |
| 后端打包文件 | 按目录结构清单把多个已存在文件打成 ZIP，并保存为新文件记录 | `POST /file/files/package`、`FileApi.packageFiles(FilePackageCommand)` |
| 秒传 | 相同文件命中后不再上传文件内容 | `POST /file/files/uploads` |
| 分片上传 | 大文件按会话和分片上传 | `/file/files/uploads/**` |
| 逻辑目录 | 管理文件中心目录树 | `/file/directories/**` |
| 存储配置 | 配置本地、MinIO、S3、OSS、COS、七牛等存储 | `/file/storage-configs/**` |
| 文件配置 | 配置大小、扩展名、MIME、秒传、直传、访问、预览、归档策略 | `GET/PUT /file/settings` |

## 3. 后端接入

### 3.0 权限边界

文件中心把基础文件访问入口和文件管理动作分开处理：

| 场景 | 访问模式 | 是否需要为角色/用户单独配置 |
|------|----------|------------------------------|
| 文件详情、预览元数据、下载入口 | `LOGIN` | 不需要。所有已登录用户默认可调用。 |
| 文件运行时配置读取 | `LOGIN` | 不需要。上传/预览组件可读取当前租户限制和策略。 |
| 文件列表、上传、分片上传、归档、删除 | `PERMISSION` | 需要。按菜单、按钮、套餐和角色授权。 |
| 存储配置、文件配置保存、目录维护 | `PERMISSION` | 需要。只给文件管理员或平台管理员。 |

`LOGIN` 只表示登录用户可以进入文件基础接口，不表示可以读取任意文件。`mango-file` 在 `FileApi.get()`、`preview()`、`download()` 和预览链路中继续按当前 `tenantId` 查询可见文件，归档、删除或跨租户文件不会返回。

业务使用时不需要给每个角色、每个用户配置 `file:files:query`、`file:files:download` 或 `file:settings:query` 才能让详情页预览/下载已保存的附件，或让上传/预览组件读取运行时策略；只需要保证用户已登录，并且业务页面本身有权限展示该 `fileId`。新增、归档、删除文件，以及保存文件中心配置仍然需要对应权限码。

### 3.1 开发依赖

只保存前端返回的 `fileId` 时，业务后端可以不依赖 `mango-file`，业务表保存自己的附件关系即可：

```text
business_id / file_id / purpose / sort
```

业务后端需要查询、下载或保存后端生成文件时，依赖 API 契约：

```xml
<dependency>
    <groupId>io.mango.platform.file</groupId>
    <artifactId>mango-file-api</artifactId>
</dependency>
```

常用 Java API：

| API | 用途 |
|-----|------|
| `FileApi.page(FileRecordPageQuery)` | 分页查询文件记录。 |
| `FileApi.get(Long id)` | 查询文件详情。 |
| `FileApi.preview(Long id)` | 获取预览元数据。 |
| `FileApi.download(Long id)` | 读取文件流。 |
| `FileApi.downloadTo(Long id, Path directory)` | 下载文件到指定目录。 |
| `FileApi.save(SaveFileCommand)` | 保存后端生成的文件。 |
| `FileApi.packageFiles(FilePackageCommand)` | 按目录结构清单生成 ZIP 并保存为新文件记录。 |
| `FileApi.archive(FileArchiveCommand)` | 归档文件记录。 |

保存后端生成文件：

```java
SaveFileCommand command = new SaveFileCommand();
command.setInputStream(inputStream);
command.setFileName("invoice.pdf");
command.setFileSize(fileSize);
command.setContentType("application/pdf");
command.setPurpose("invoice");
command.setAccessLevel("PRIVATE");
command.setBizType("INVOICE");
command.setBizId(invoiceId.toString());

FileRecordVO file = fileApi.save(command).getData();
Long fileId = file.getId();
```

按目录结构打包已存在文件：

```java
FilePackageCommand command = new FilePackageCommand();
command.setFileName("contract-materials.zip");
command.setPurpose("contract-material-package");
command.setAccessLevel("PRIVATE");
command.setBizType("CONTRACT_MATERIAL_PACKAGE");
command.setBizId(contractId.toString());
command.setEntries(List.of(
        new FilePackageEntryCommand(fileId1, "01_签约资料/${fileName}"),
        new FilePackageEntryCommand(fileId2, "01_签约资料/企业资料/${fileName}"),
        new FilePackageEntryCommand(fileId3, "02_资料清单/配置的资料清单.xlsx")
));

FileRecordVO zipFile = fileApi.packageFiles(command).getData();
Long zipFileId = zipFile.getId();
```

打包入口会复用文件中心的可见性、下载和保存规则。`entries.path` 是 ZIP 内部相对路径，禁止空路径、目录项、绝对路径、`..` 路径穿越和重复路径；生成的 ZIP 会写入当前存储层，并返回新的 `FileRecordVO`。

`entries.path` 支持变量，业务不需要为了拼 ZIP 路径额外查询文件名；文件服务在读取源文件记录时会替换变量，然后再做路径安全校验。当前支持的变量：

| 变量 | 含义 | 示例 |
|------|------|------|
| `${fileName}` | 源文件记录里的文件名，也就是文件上传或保存时进入文件中心的 `fileName` | `01_签约资料/${fileName}` |

未在上表登记的变量不会被替换，业务不要传 `${fileId}`、`${fileExt}`、`${bizId}` 等未定义变量。

`entries.path` 支持多级嵌套目录，不需要单独传目录项；ZIP 会按路径自动形成目录结构。例如：

```text
项目名称+被保人名称+保函金额.zip
├── 01_签约资料
│   ├── 保函申请书.pdf
│   └── 企业资料
│       └── 营业执照.pdf
├── 02_资料清单
│   └── 配置的资料清单.xlsx
└── 04_反担保资料
    └── 合同
        └── 反担保合同.pdf
```

下载文件到工作目录：

```java
Path localFile = fileApi.downloadTo(fileId, workDirectory);
```

### 3.2 部署依赖

提供文件服务的应用引入 `mango-file-starter`：

```xml
<dependency>
    <groupId>io.mango.platform.file</groupId>
    <artifactId>mango-file-starter</artifactId>
</dependency>
```

远程调用独立文件服务的应用引入 `mango-file-starter-remote`：

```xml
<dependency>
    <groupId>io.mango.platform.file</groupId>
    <artifactId>mango-file-starter-remote</artifactId>
</dependency>
```

`mango-file-starter-remote` 提供 `FileFeignClient`，服务名 `mango-file`，路径 `/file/files`。它覆盖文件记录、上传、详情、预览、下载和归档能力；存储配置、文件配置、目录管理是文件服务侧管理能力。

## 4. 前端接入

前端包是 `@mango/file`。它有两类入口：

| 入口 | 适用范围 | 说明 |
|------|----------|------|
| `@mango/file/admin-pages` | Admin Pages | Mango 管理后台配套页面：文件管理、存储配置、文件配置。 |
| `MUpload`、`FilePreviewPanel` | 业务可复用组件 | 可放在业务表单、详情页、工作流表单里。 |

### 4.1 注册文件管理页面

后台应用需要文件中心管理页时注册：

```ts
import { registerMangoFileAdminPages } from '@mango/file/admin-pages';

registerMangoFileAdminPages();
```

页面 key：

| 管理页 | 页面 key | 用途 |
|--------|----------|------|
| 文件管理 | `file/files/index` | 文件列表、上传、下载、归档、删除、目录树。 |
| 存储配置 | `file/storage-configs/index` | 存储分页、详情、新增、编辑、测试、设为默认。 |
| 文件配置 | `file/settings/index` | 上传限制、秒传、直传、访问、预览、归档策略。 |

这些页面是 Admin Shell / Admin Pages 配套插件，不是官网、营销站或 C 端页面组件。

### 4.2 业务表单上传

```vue
<script setup lang="ts">
import { ref } from 'vue';
import '@mango/file/style.css';
import { MUpload } from '@mango/file';

const attachmentIds = ref<string[]>([]);
</script>

<template>
  <MUpload
    v-model="attachmentIds"
    value-type="id"
    :count="5"
    fmt="pdf,doc,docx,png,jpg"
    size="20MB"
    purpose="attachment"
    access-level="PRIVATE"
    biz-type="contract"
    :biz-id="contractId"
  />
</template>
```

`value-type` 决定业务表单拿到什么：

| `value-type` | 返回值 | 适用场景 |
|--------------|--------|----------|
| `id` | 文件记录 ID | 业务表保存附件关系，最常用。 |
| `token` | `mango-file:<id>` | 一个字段需要区分文件来源时使用。 |
| `record` | `FileRecord` | 上传后立刻展示文件名、大小、URL 等信息。 |

`fmt`、`size`、`sizes` 是前端提前拦截。最终限制以 `GET /file/settings` 返回的 `maxSize`、扩展名和 MIME 配置为准。

### 4.3 详情页预览

```vue
<script setup lang="ts">
import '@mango/file/style.css';
import { FilePreviewPanel } from '@mango/file';
</script>

<template>
  <FilePreviewPanel :file-id="fileId" />
</template>
```

常用 props：

| Prop | 说明 |
|------|------|
| `fileId` | 文件 ID 或 `mango-file:<id>`。 |
| `file` | 已加载的文件引用。 |
| `preview` | 已加载的预览对象；传入后不再请求 `fileApi.preview()`。 |
| `previewProviderUrl` | 文档预览服务地址。 |
| `downloadPermission` | 下载按钮权限，默认 `file:files:download`。 |
| `showActions` | 是否显示下载和新窗口预览操作。 |

## 5. 快速开始

### 5.1 上传附件并保存业务关系

1. 业务表增加附件关系字段或附件关系表。
2. 前端表单使用 `MUpload value-type="id"`。
3. 保存业务单据时把 `fileId` 一起提交给业务接口。
4. 业务详情页按 `fileId` 使用 `FilePreviewPanel` 或调用文件详情接口回显。

业务表建议保存：

```text
business_id / file_id / purpose / sort
```

不建议保存：

```text
bucket / object_name / preview_url / download_url / presigned_url
```

### 5.2 限制上传大小

默认限制写在应用 YAML：

```yaml
mango:
  file:
    upload:
      max-size: 104857600
```

运行时限制在管理端“文件配置”页面设置 `maxSize`，或调用：

```http
PUT /file/settings
```

`maxSize` 单位是字节。普通上传 `POST /file/files` 和分片初始化 `POST /file/files/uploads` 都会校验它。

### 5.3 开启秒传

默认开关写在应用 YAML：

```yaml
mango:
  file:
    upload:
      instant-upload-enabled: true
```

运行时开关在管理端“文件配置”页面设置：

| 字段 | 含义 |
|------|------|
| `instantUploadEnabled` | 是否开启秒传。 |
| `instantUploadScope` | 秒传匹配范围：`TENANT` 当前租户，`GLOBAL` 全局。 |

客户端初始化分片上传时传 `fileHash` 和 `fileSize`。命中后 `FileUploadInitVO.instant=true`，并直接返回 `fileRecord`，前端不再上传分片。

### 5.4 大文件分片上传

初始化：

```http
POST /file/files/uploads
```

常用入参：

| 字段 | 说明 |
|------|------|
| `fileName` | 原始文件名。 |
| `fileSize` | 文件大小，必须小于等于当前 `maxSize`。 |
| `fileHash` | SHA-256，用于秒传和完成校验。 |
| `contentType` | MIME 类型。 |
| `chunkSize` | 分片大小；不传时服务端按默认值处理。 |
| `totalParts` | 总分片数；最大 10000。 |
| `purpose` / `accessLevel` / `bizType` / `bizId` / `bizMeta` / `directoryId` | 与普通上传一致。 |

返回 `FileUploadInitVO`：

| 字段 | 说明 |
|------|------|
| `instant` | 是否命中秒传。 |
| `fileRecord` | 秒传命中时返回的文件记录。 |
| `sessionId` | 未命中秒传时返回上传会话 ID。 |
| `uploadMode` | `SERVER_CHUNK` 或 `S3_MULTIPART`。 |
| `storageUploadId` | 对象存储原生 multipart uploadId。 |
| `chunkSize` / `totalParts` | 后续上传分片使用。 |
| `expiresAt` | 会话过期时间。 |

上传模式：

| 模式 | 后续步骤 |
|------|----------|
| `SERVER_CHUNK` | `POST /file/files/uploads/{sessionId}/parts` 上传分片，再 `POST /file/files/uploads/{sessionId}/complete` 完成。 |
| `S3_MULTIPART` | `POST /file/files/uploads/{sessionId}/parts/sign` 获取直传地址，浏览器直传对象存储，再 `PUT /file/files/uploads/{sessionId}/parts` 登记分片，最后 complete。 |

取消上传：

```http
DELETE /file/files/uploads/{sessionId}
```

### 5.5 配置预览

默认预览服务地址：

```yaml
mango:
  file:
    preview:
      provider-url: /file-preview/files/preview
      expire-seconds: 600
```

运行时在管理端“文件配置”页面设置：

| 字段 | 含义 |
|------|------|
| `previewProviderUrl` | 文档预览服务地址。 |
| `previewExpireSeconds` | 预览地址有效期，单位秒。 |
| `previewExternalExtensions` | 交给预览服务处理的扩展名。 |

图片、PDF、视频、音频可以由前端组件内联展示。Office、压缩包等文件通常需要接入 `mango-file-preview`。

## 6. 配置说明

配置分两层：

| 配置来源 | 在哪里改 | 什么时候用 |
|----------|----------|------------|
| YAML 默认值 | 应用配置文件 `mango.file.*` | 应用启动默认值；当前租户没有保存运行时配置时使用。 |
| 运行时配置 | 管理端“文件配置”页或 `PUT /file/settings` | 保存到 `file_settings`，当前租户优先使用。 |

`GET /file/settings` 返回 `defaultConfig=true` 表示当前租户还没有保存运行时配置，当前值来自 YAML 默认值和代码默认值。

## 7. YAML 配置字段

配置前缀：`mango.file`。

```yaml
mango:
  file:
    enabled: true
    storage-type: LOCAL
    default-bucket: local
    public-base-url: https://example.com/api
    local:
      root-path: ./data/files
      public-path: /file/local-objects
    upload:
      max-size: 104857600
      allowed-extensions: []
      blocked-extensions: [exe, bat, cmd, sh, jar]
      instant-upload-enabled: true
      direct-upload-enabled: false
      direct-upload-expire-seconds: 900
    access:
      mode: PROXY
      token-enabled: false
      token-expire-seconds: 600
    preview:
      provider-url: /file-preview/files/preview
      expire-seconds: 600
      external-extensions: [doc, docx, xls, xlsx, xlsm, ppt, pptx, odt, ods, odp, ofd, wps, et, dps, csv, txt, zip, rar, 7z, eml, msg]
```

| 配置项 | 默认值 | 含义 |
|--------|--------|------|
| `enabled` | `true` | 是否启用文件服务自动配置。 |
| `storage-type` | `LOCAL` | 默认存储类型。 |
| `default-bucket` | `local` | 默认 bucket。 |
| `public-base-url` | 空 | 对外访问基准地址，例如网关地址。 |
| `local.root-path` | `./data/files` | 本地文件落盘目录。 |
| `local.public-path` | `/file/local-objects` | 本地对象读取路径。 |
| `upload.max-size` | `104857600` | 单文件最大字节数，默认 100MB。 |
| `upload.allowed-extensions` | `[]` | 扩展名白名单，空表示不限制。 |
| `upload.blocked-extensions` | `exe,bat,cmd,sh,jar` | 扩展名黑名单。 |
| `upload.instant-upload-enabled` | `true` | 是否默认开启秒传。 |
| `upload.direct-upload-enabled` | `false` | 是否默认允许浏览器直传对象存储。 |
| `upload.direct-upload-expire-seconds` | `900` | 直传分片签名有效期，单位秒。 |
| `access.mode` | `PROXY` | `PROXY` 由 Java 服务转发，`DIRECT` 返回底层存储直连地址。 |
| `access.token-enabled` | `false` | 是否默认启用访问令牌。 |
| `access.token-expire-seconds` | `600` | 下载/访问令牌有效期。 |
| `preview.provider-url` | `/file-preview/files/preview` | 文档预览服务地址，支持相对地址、绝对地址和占位符。 |
| `preview.expire-seconds` | `600` | 预览地址有效期。 |
| `preview.external-extensions` | 文档和压缩包扩展名 | 交给预览服务处理的扩展名。 |

## 8. 资源注入

文件中心默认存储配置和默认运行时配置通过 `mango-resource` 注入，不在 Flyway 中写业务配置数据。资源文件放在：

```text
mango-file-starter/src/main/resources/META-INF/mango/resources/file-common-storage.yml
```

### 8.1 FILE_STORAGE_CONFIG

`FILE_STORAGE_CONFIG` 落库到 `file_storage_config`，按 `configName` 合并更新。

| 字段 | 类型 | 必填 | 含义 |
|------|------|------|------|
| `id` | `STRING` | 是 | 资源稳定 ID，使用雪花 ID 字符串。 |
| `version` | `INT` | 是 | 资源版本，声明内容升级时递增。 |
| `biz-key` | `STRING` | 是 | 资源业务键，例如 `file.storage.local-default`。 |
| `target-module` | `STRING` | 是 | 固定为 `file`。 |
| `storageConfigId` | `LONG` | 否 | 存储配置稳定 ID，不填时使用资源 ID。 |
| `tenantId` | `LONG` | 否 | 租户 ID，默认 `1`。 |
| `configName` | `STRING` | 是 | 存储配置名称，全局唯一。 |
| `storageType` | `STRING` | 是 | `LOCAL`、`MINIO`、`AWS_S3`、`ALIYUN_OSS`、`TENCENT_COS`、`QINIU_KODO`。 |
| `endpoint` | `STRING` | 否 | 接入地址。 |
| `publicEndpoint` | `STRING` | 否 | 公开访问地址。 |
| `region` | `STRING` | 否 | 区域。 |
| `bucketName` | `STRING` | 是 | 存储桶名称。 |
| `storagePath` | `STRING` | 否 | 存储路径前缀，默认空字符串。 |
| `accessKey` | `STRING` | 否 | 访问密钥 AccessKey。 |
| `secretKey` | `STRING` | 否 | 访问密钥 SecretKey。 |
| `pathStyleAccess` | `INT` | 否 | 是否使用 Path Style 访问，默认 `0`。 |
| `sslEnabled` | `INT` | 否 | 是否启用 HTTPS，默认 `0`。 |
| `active` | `INT` | 否 | 是否默认启用，默认 `0`。 |
| `status` | `INT` | 否 | `1` 启用，`0` 停用，默认 `1`。 |
| `remark` | `STRING` | 否 | 备注。 |

### 8.2 FILE_SETTINGS

`FILE_SETTINGS` 落库到 `file_settings`，按 `tenantId` 合并更新。

| 字段 | 类型 | 必填 | 含义 |
|------|------|------|------|
| `settingsId` | `LONG` | 否 | 文件中心配置稳定 ID，不填时使用资源 ID。 |
| `tenantId` | `LONG` | 否 | 租户 ID，默认 `1`。 |
| `maxSize` | `LONG` | 否 | 单文件最大大小，默认 `104857600`。 |
| `allowedExtensions` | `STRING` | 否 | 允许上传扩展名，逗号分隔；为空表示不限制。 |
| `blockedExtensions` | `STRING` | 否 | 禁止上传扩展名，默认 `exe,bat,cmd,sh,jar`。 |
| `defaultAccessLevel` | `STRING` | 否 | 默认访问级别，默认 `PRIVATE`。 |
| `duplicateNameStrategy` | `STRING` | 否 | 重名处理策略，默认 `REJECT`。 |
| `duplicateCheckDirectoryScoped` | `INT` | 否 | 是否按目录隔离重名，默认 `1`。 |
| `objectNameStrategy` | `STRING` | 否 | 对象命名策略，默认 `DATE_UUID`。 |
| `instantUploadEnabled` | `INT` | 否 | 是否启用秒传，默认 `1`。 |
| `instantUploadScope` | `STRING` | 否 | 秒传匹配范围，默认 `TENANT`。 |
| `contentTypeCheckEnabled` | `INT` | 否 | 是否校验 MIME 类型，默认 `1`。 |
| `allowedContentTypes` | `STRING` | 否 | 允许上传 MIME，逗号分隔；为空表示不限制。 |
| `blockedContentTypes` | `STRING` | 否 | 禁止上传 MIME。 |
| `directUploadEnabled` | `INT` | 否 | 是否启用客户端直传，默认 `0`。 |
| `directUploadExpireSeconds` | `LONG` | 否 | 直传签名有效期，默认 `900`。 |
| `accessTokenEnabled` | `INT` | 否 | 是否启用限时访问令牌，默认 `0`。 |
| `publicReadRequiresToken` | `INT` | 否 | 公开读取是否仍要求签名访问，默认 `0`。 |
| `accessMode` | `STRING` | 否 | 文件访问模式，默认 `PROXY`。 |
| `accessTokenExpireSeconds` | `LONG` | 否 | 访问令牌有效期，默认 `600`。 |
| `previewProviderUrl` | `STRING` | 否 | 外部预览服务地址。 |
| `previewExpireSeconds` | `LONG` | 否 | 预览访问有效期，默认 `600`。 |
| `previewExternalExtensions` | `STRING` | 否 | 外部预览扩展名，逗号分隔。 |
| `archiveRetainEnabled` | `INT` | 否 | 是否保留归档记录，默认 `1`。 |
| `archiveRetainDays` | `INT` | 否 | 归档记录保留天数，默认 `180`。 |
| `archiveRestoreEnabled` | `INT` | 否 | 是否允许恢复归档，默认 `0`。 |
| `physicalDeleteEnabled` | `INT` | 否 | 是否删除物理对象，默认 `0`。 |

## 9. 运行时配置字段

接口：

| 接口 | 权限 | 用途 |
|------|------|------|
| `GET /file/settings` | `file:settings:query` | 读取当前租户文件配置。 |
| `PUT /file/settings` | `file:settings:edit` | 保存当前租户文件配置。 |

字段：

| 字段 | 默认值来源 | 含义 |
|------|------------|------|
| `maxSize` | `mango.file.upload.max-size` | 单文件大小上限，普通上传和分片初始化都会校验。 |
| `allowedExtensions` | `mango.file.upload.allowed-extensions` | 允许扩展名，空表示不限制。 |
| `blockedExtensions` | `mango.file.upload.blocked-extensions` | 禁止扩展名。 |
| `defaultAccessLevel` | `PRIVATE` | 上传未传 `accessLevel` 时使用。 |
| `duplicateNameStrategy` | `REJECT` | 同目录重名处理：`REJECT`、`AUTO_RENAME`、`ALLOW`。 |
| `duplicateCheckDirectoryScoped` | `true` | 是否只在同一逻辑目录内检查重名。 |
| `objectNameStrategy` | `DATE_UUID` | 底层对象命名：`DATE_UUID`、`HASH`、`ORIGINAL`。 |
| `instantUploadEnabled` | `mango.file.upload.instant-upload-enabled` | 是否开启秒传。 |
| `instantUploadScope` | `TENANT` | 秒传范围：`TENANT` 当前租户，`GLOBAL` 全局。 |
| `contentTypeCheckEnabled` | `true` | 是否校验 MIME 类型。 |
| `allowedContentTypes` | `[]` | MIME 白名单，空表示不限制。 |
| `blockedContentTypes` | `application/x-msdownload, application/x-sh` | MIME 黑名单。 |
| `directUploadEnabled` | `mango.file.upload.direct-upload-enabled` | 是否允许对象存储直传。 |
| `directUploadExpireSeconds` | `mango.file.upload.direct-upload-expire-seconds` | 分片直传签名有效期。 |
| `accessTokenEnabled` | `mango.file.access.token-enabled` | 是否启用访问令牌。 |
| `publicReadRequiresToken` | `false` | `PUBLIC_READ` 文件是否仍要求签名访问。 |
| `accessMode` | `mango.file.access.mode` | 文件访问模式：`PROXY` 或 `DIRECT`。 |
| `accessTokenExpireSeconds` | `mango.file.access.token-expire-seconds` | 下载/访问令牌有效期。 |
| `previewProviderUrl` | `mango.file.preview.provider-url` | 文档预览服务地址。 |
| `previewExpireSeconds` | `mango.file.preview.expire-seconds` | 预览地址有效期。 |
| `previewExternalExtensions` | `mango.file.preview.external-extensions` | 交给预览服务处理的扩展名。 |
| `archiveRetainEnabled` | `true` | 是否保留归档记录。 |
| `archiveRetainDays` | `180` | 归档保留天数。 |
| `archiveRestoreEnabled` | `false` | 是否允许恢复归档记录。 |
| `physicalDeleteEnabled` | `false` | 归档/删除且引用数为 0 时是否删除物理对象。 |

## 10. 返回字段

README 只列业务常用字段，完整接口字段以 OpenAPI 为准。

### 10.1 `FileRecordVO`

| 字段 | 含义 | 业务是否应入库 |
|------|------|----------------|
| `id` | 文件记录 ID，也就是业务保存的 `fileId`。 | 是 |
| `bizType` / `bizId` / `purpose` / `bizMeta` | 上传时传入的业务标记。 | 按业务需要 |
| `fileName` / `fileExt` / `fileSize` / `contentType` | 展示用文件信息。 | 可冗余展示 |
| `status` / `archived` | 文件状态。 | 通常不需要 |
| `url` / `previewUrl` / `downloadUrl` | 运行时访问地址。 | 不要入库 |
| `directPreviewUrl` / `directDownloadUrl` | 对象存储直连地址。 | 不要入库 |

### 10.2 `FilePreviewVO`

| 字段 | 含义 |
|------|------|
| `id` | 文件 ID。 |
| `fileName` / `fileExt` / `fileSize` / `contentType` | 展示信息。 |
| `previewable` | 是否可预览。 |
| `previewUrl` | 当前配置下可用的预览地址。 |
| `downloadUrl` | 当前配置下可用的下载地址。 |
| `directAccess` | 是否使用对象存储直连。 |
| `directPreviewUrl` / `directDownloadUrl` | 直连地址。 |
| `directPreviewExpireSeconds` / `directDownloadExpireSeconds` | 直连地址有效期。 |

## 11. 管理入口

| 管理能力 | 菜单 component | 入口权限 |
|----------|----------------|----------|
| 文件管理 | `file/files/index` | `file:files:list` |
| 存储配置 | `file/storage-configs/index` | `file:storage-configs:list` |
| 文件配置 | `file/settings/index` | `file:settings:query` |

主要权限码：

| 能力 | 权限码 |
|------|--------|
| 文件 | `file:files:list`、`file:files:query`、`file:files:upload`、`file:files:download`、`file:files:archive`、`file:files:delete` |
| 目录 | `file:directories:list`、`file:directories:add`、`file:directories:edit`、`file:directories:delete` |
| 存储配置 | `file:storage-configs:list`、`file:storage-configs:query`、`file:storage-configs:add`、`file:storage-configs:edit`、`file:storage-configs:delete`、`file:storage-configs:active`、`file:storage-configs:test` |
| 文件配置 | `file:settings:query`、`file:settings:edit` |

## 12. 数据与初始化

Flyway 路径：`mango-file-core/src/main/resources/db/migration/file`。

| Migration | 初始化内容 | 幂等键 / 唯一键 |
|-----------|------------|-----------------|
| `V1__init_file.sql` | `file_record`、`file_storage_config`、`file_settings`、`file_directory`、`file_object`、`file_hash_mapping`、`file_upload_session`、`file_upload_part` | 表级 `CREATE TABLE IF NOT EXISTS`、`file_storage_config.config_name`、`file_settings.tenant_id` |

默认本地存储、MinIO 本地联调配置和默认文件中心运行时配置通过 `mango-resource` 注入，资源文件是 `file-common-storage.yml`。运行时文件配置写入 `file_settings`，按 `tenant_id` 唯一；当前租户没有配置时，`GET /file/settings` 会返回 YAML 默认值和 `defaultConfig=true`。

文件菜单和权限由 `mango-file-starter/src/main/resources/META-INF/mango/resources/file-common-menu.json` 的 `AUTH_MENU` 资源注入。

## 13. 问题排查

- 上传被拒绝：先查 `GET /file/settings` 返回的 `maxSize`、扩展名和 MIME 黑白名单，再查前端 `MUpload` 的 `fmt`、`size`、`sizes`。
- 秒传没有命中：确认运行时配置 `instantUploadEnabled=true`，客户端初始化分片上传时传了 `fileHash`，并确认 `instantUploadScope` 是否按租户或全局匹配。
- 大文件分片失败：检查 `POST /file/files/uploads` 返回的 `uploadMode`，`S3_MULTIPART` 需要存储配置支持直传签名，`SERVER_CHUNK` 需要 Java 服务能接收分片。
- 预览只能下载不能打开：检查 `previewProviderUrl`、`previewExternalExtensions`，Office 类文件还需要 `mango-file-preview` 和 `mango-infra-fileproc`。
- 页面空白或按钮不可见：检查 authorization 菜单 component 是否是 `file/files/index`、`file/storage-configs/index`、`file/settings/index`，并确认账号拥有对应 `file:*` 权限码。
- 业务表里出现对象存储地址：应改为保存 `fileId` 或业务附件关系；`url`、`previewUrl`、`downloadUrl` 只用于当前页面即时展示。

## 14. 相关文档

- [@mango/file 前端包](../../../mango-ui/packages/file/README.md)
- [@mango/file 组件](../../../mango-ui/packages/file/src/components/README.md)
- [File Preview](../mango-file-preview/README.md)
- [Fileproc](../../mango-infra/mango-infra-fileproc/README.md)
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
