# @mango/file

`@mango/file` 是文件能力的前端包，面向 admin 应用提供三类入口：

- `admin-pages` 配套页面：文件管理、存储配置、文件配置。
- 业务可复用组件：`MUpload`、`FilePreviewPanel`。
- API 封装：文件、存储配置、文件设置接口。

这个包不是通用官网组件库。管理页面依赖 Mango admin-shell、权限指令、Element Plus 和后端 `mango-file` 接口；官网、营销站或独立前台项目通常只应复用 API 或自行封装上传和预览 UI。

## 1. 概览
`@mango/file` 是 Mango 文件能力的前端接入包，提供 admin 管理页面、业务上传组件、预览组件和接口封装。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| Mango admin 后台需要文件管理、存储配置、文件配置页面 | 前端注册 / 组件 / API 封装 |
| 业务表单需要上传附件、图片、合同、凭证或导入文件 | 前端注册 / 组件 / API 封装 |
| 详情页需要预览或下载 Mango 文件中心的文件 | 前端注册 / 组件 / API 封装 |
| 工作流运行时表单需要复用文件上传字段 | 前端注册 / 组件 / API 封装 |

## 3. 适用场景
- Mango admin 后台需要文件管理、存储配置、文件配置页面。
- 业务表单需要上传附件、图片、合同、凭证或导入文件。
- 详情页需要预览或下载 Mango 文件中心的文件。
- 工作流运行时表单需要复用文件上传字段。

## 4. 边界说明
- 不适合作为官网、营销站、内容站的通用视觉组件库。
- 不负责后端存储策略、租户隔离、文件访问级别和下载鉴权。
- 不替代业务模块自己的附件关系表。

## 5. 模块组成
包名是 `@mango/file`。本包提供前端页面、组件和 API 封装；后端数据、权限、租户和文件实际访问由 `mango-file` 校验。

admin-pages 属于 `admin-shell` 配套页面；`MUpload` 和 `FilePreviewPanel` 是业务可复用组件。

## 6. 接入方式
```ts
import '@mango/file/style.css';
import { FilePreviewPanel, MUpload, fileApi, fileSettingsApi, fileStorageApi } from '@mango/file';
import { registerMangoFileAdminPages } from '@mango/file/admin-pages';
```

注册管理页面：

```ts
import { registerMangoFileAdminPages } from '@mango/file/admin-pages';

registerMangoFileAdminPages();
```

`registerMangoFileAdminPages()` 内部调用 `@mango/admin-pages/core` 的 `registerModulePages()`，模块码是 `mango-file`。函数有幂等保护，多次调用只注册一次。

## 7. 配置说明
前端包自身没有独立配置文件，实际配置来自三个入口：

- 页面注册：调用 `registerMangoFileAdminPages()` 注册 admin 页面 key。
- 后端运行时配置：`GET /file/settings` 返回 `maxSize`、秒传、直传、访问、预览和归档策略。
- 文档预览地址：`FilePreviewPanel.previewProviderUrl`、环境变量 `VITE_FILE_PREVIEW_PROVIDER_URL` 或后端 `previewUrl`。

### 6.1 页面和能力边界

| 入口 | 类型 | 页面 key | 后端接口 | 说明 |
|------|------|----------|----------|------|
| `FileView` | admin-pages | `file/files/index` | `/file/files`、`/file/directories` | 文件记录、上传、下载、归档、删除、目录树。 |
| `FileStorageView` | admin-pages | `file/storage-configs/index` | `/file/storage-configs` | 存储配置分页、详情、新增、编辑、删除、测试、设为默认。 |
| `FileSettingsView` | admin-pages | `file/settings/index` | `/file/settings` | 上传限制、秒传、直传、访问、预览、归档策略。 |
| `MUpload` | public component | 无 | `/file/files`、分片上传接口 | 业务表单上传组件。 |
| `FilePreviewPanel` | public component | 无 | `/file/files/preview`、`/file-preview/files/preview-link` | 文件预览和下载面板。 |

admin-pages 的菜单 `component` 由后端 migration 初始化为 `@/views/file/...`，admin-shell 解析后匹配这里的页面 key。后端 README 的菜单表列出了映射关系。

### 6.2 后端依赖

必须接入：

- `mango-platform/mango-file`：上传、下载、文件记录、存储配置、文件设置。

按预览场景接入：

- `mango-platform/mango-file-preview`：文档预览入口。
- `mango-infra/mango-infra-fileproc`：Office/PDF 等文件处理。

主要接口前缀：

- `/file/files`
- `/file/directories`
- `/file/storage-configs`
- `/file/settings`
- `/file-preview/files`

## 8. API 与扩展
`fileApi`：

| 方法 | 后端接口 | 用途 |
|------|----------|------|
| `upload(file, params, options)` | `POST /file/files` 或分片上传链路 | 上传单文件；大于默认阈值时走分片上传。 |
| `uploadBatch(files, params, options)` | `POST /file/files/batch` | 批量上传小文件。 |
| `page(params)` | `GET /file/files/page` | 文件分页。 |
| `detail(id)` | `GET /file/files/detail` | 文件详情。 |
| `preview(id)` | `GET /file/files/preview` | 文件预览元数据。 |
| `previewLink(id)` | `GET /file-preview/files/preview-link` | 文档预览链接。 |
| `downloadUrl(id)` | `/api/file/files/download?id=...` | 拼接下载地址。 |
| `archive(id)` | `PUT /file/files/archive` | 归档。 |
| `delete(ids)` | `POST /file/files/delete` | 删除记录。 |
| `createUploadSession(command)` | `POST /file/files/uploads` | 初始化分片上传和秒传。 |
| `createUploadPartSign(sessionId, partNumber, partSize)` | `POST /file/files/uploads/{sessionId}/parts/sign` | 获取直传签名。 |
| `uploadServerPart(sessionId, partNumber, chunk, fileName)` | `POST /file/files/uploads/{sessionId}/parts` | 服务端接收分片。 |
| `completeUploadPart(sessionId, command)` | `PUT /file/files/uploads/{sessionId}/parts` | 确认直传分片。 |
| `completeUploadSession(sessionId)` | `POST /file/files/uploads/{sessionId}/complete` | 完成上传。 |
| `abortUploadSession(sessionId)` | `DELETE /file/files/uploads/{sessionId}` | 取消上传。 |

`DEFAULT_MULTIPART_THRESHOLD` 是 `20 * 1024 * 1024`。超过该阈值时前端优先走上传会话链路；服务端最终是否 `S3_MULTIPART` 取决于当前默认存储配置是否支持 multipart。

`fileStorageApi`：

- `page`、`detail`、`create`、`update`、`delete`、`activate`、`test`。
- 存储类型：`LOCAL`、`S3`、`MINIO`、`AWS_S3`、`ALIYUN_OSS`、`TENCENT_COS`、`QINIU_KODO`。

`fileSettingsApi`：

- `get()` -> `GET /file/settings`
- `save(data)` -> `PUT /file/settings`

`defaultFileSettings` 与后端默认值保持一致，页面首次加载时用于补齐缺省字段。

### 7.1 文件设置字段

前端设置页直接映射后端 `FileSettings`：

| 字段 | 默认值 | 页面含义 |
|------|--------|----------|
| `maxSize` | `100MB` | 服务端单文件上限。 |
| `allowedExtensions` | `[]` | 允许扩展名，空表示不限制。 |
| `blockedExtensions` | `exe,bat,cmd,sh,jar` | 禁止扩展名。 |
| `defaultAccessLevel` | `PRIVATE` | 默认访问级别。 |
| `duplicateNameStrategy` | `REJECT` | 重名处理策略。 |
| `duplicateCheckDirectoryScoped` | `true` | 是否按目录检查重名。 |
| `objectNameStrategy` | `DATE_UUID` | 对象命名策略。 |
| `instantUploadEnabled` | `true` | 是否开启秒传。 |
| `instantUploadScope` | `TENANT` | 秒传范围。 |
| `contentTypeCheckEnabled` | `true` | 是否校验 MIME。 |
| `allowedContentTypes` | `[]` | MIME 白名单。 |
| `blockedContentTypes` | `application/x-msdownload, application/x-sh` | MIME 黑名单。 |
| `directUploadEnabled` | `false` | 是否启用浏览器直传。 |
| `directUploadExpireSeconds` | `900` | 直传 URL 有效期。 |
| `accessTokenEnabled` | `false` | 是否启用访问令牌。 |
| `publicReadRequiresToken` | `false` | 公开文件是否仍签名。 |
| `accessMode` | `PROXY` | 代理访问或直连存储。 |
| `accessTokenExpireSeconds` | `600` | 访问令牌有效期。 |
| `previewProviderUrl` | `/file-preview/files/preview` | 文档预览服务地址。 |
| `previewExpireSeconds` | `600` | 预览有效期。 |
| `previewExternalExtensions` | 文档和压缩包扩展名 | 外部预览扩展名。 |
| `archiveRetainEnabled` | `true` | 是否保留归档记录。 |
| `archiveRetainDays` | `180` | 归档保留天数。 |
| `archiveRestoreEnabled` | `false` | 当前保存时固定为 false。 |
| `physicalDeleteEnabled` | `false` | 是否删除物理对象。 |

要限制文件大小，在“文件配置”页面保存 `maxSize`，或后端配置 `mango.file.upload.max-size`。`MUpload.size` 只是前端提前拦截。

要开启秒传，在“文件配置”页面打开 `instantUploadEnabled` 并选择 `instantUploadScope`，或后端配置 `mango.file.upload.instant-upload-enabled`。前端分片上传链路会计算并提交 `fileHash`，命中后直接使用返回的 `fileRecord`。

## 9. 数据与初始化
前端包没有数据库。菜单、权限和默认数据由后端 `mango-file`、`mango-authorization` migration 或业务 starter 初始化。

需要核对的初始化数据：

- 菜单 component：`@/views/file/files/index.vue`、`@/views/file/storage-configs/index.vue`、`@/views/file/settings/index.vue`。
- 前端页面 key：`file/files/index`、`file/storage-configs/index`、`file/settings/index`。
- 权限码：`file:files:*`、`file:directories:*`、`file:storage-configs:*`、`file:settings:*`。

## 10. 管理入口
页面按钮和接口依赖后端权限码：

- 文件：`file:files:list`、`file:files:query`、`file:files:upload`、`file:files:download`、`file:files:archive`、`file:files:delete`。
- 目录：`file:directories:list`、`file:directories:add`、`file:directories:edit`、`file:directories:delete`。
- 存储配置：`file:storage-configs:list`、`file:storage-configs:query`、`file:storage-configs:add`、`file:storage-configs:edit`、`file:storage-configs:delete`、`file:storage-configs:active`、`file:storage-configs:test`。
- 设置：`file:settings:query`、`file:settings:edit`。

前端只做菜单注册、按钮显隐和交互展示。租户、文件归属、访问级别和下载权限由后端接口校验。

## 11. 快速开始
表单上传多个附件并保存 `fileId`：

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { MUpload } from '@mango/file';
import '@mango/file/style.css';

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

详情页预览：

```vue
<script setup lang="ts">
import { FilePreviewPanel } from '@mango/file';
</script>

<template>
  <FilePreviewPanel :file-id="attachmentIds[0]" />
</template>
```

## 12. 问题排查
- 上传大小被拒：以后端 `GET /file/settings.maxSize` 为准，组件 `size` 只是提前拦截。
- 页面空白：先确认业务 admin app 调用了 `registerMangoFileAdminPages()`，再查菜单 component 和页面 key 映射。
- 下载按钮不显示或 403：检查 `file:files:download`、文件状态、访问级别和租户。
- 文档预览打不开：检查 `mango-file-preview`、`previewProviderUrl` 和 `/file-preview/files/preview-link`。

## 13. 相关文档
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [前端模块规范](../../../mango-pmo/rules/frontend/01-vue-code.md)

## 14. 历史资料
- [Mango File 后端模块](../../../mango/mango-platform/mango-file/README.md)
- [文件组件 README](./src/components/README.md)
- [File Preview 后端模块](../../../mango/mango-platform/mango-file-preview/README.md)
