# @mango/file

`@mango/file` 是 Mango 文件能力的前端接入包，提供文件管理页面、存储配置页面、文件设置页面、上传组件、预览组件和 API 封装。

## 1. 概览

这个包有三种集成形态：

| 标识 | 内容 |
|------|------|
| `admin-pages` | 文件管理、存储配置、文件设置页面。 |
| `business-component` | `MUpload`、`FilePreviewPanel`。 |
| `api-client` | 文件、存储配置、文件设置接口封装。 |

`admin-pages` 面向 Mango 管理后台；官网、营销站或独立前台项目通常不应直接复用这些管理页面。业务页面需要上传或预览文件时，优先使用 `MUpload` 和 `FilePreviewPanel`。

## 2. 功能清单

| 能力 | 使用入口 |
|------|----------|
| 文件记录分页、详情、上传、下载、归档、删除 | `FileView`、`fileApi`。 |
| 存储配置分页、详情、新增、编辑、删除、测试、设为默认 | `FileStorageView`、`fileStorageApi`。 |
| 上传限制、秒传、直传、访问、预览、归档策略 | `FileSettingsView`、`fileSettingsApi`。 |
| 业务表单上传附件或图片 | `MUpload`。 |
| 详情页预览图片、PDF、音视频或文档 | `FilePreviewPanel`。 |
| 工作流运行时表单上传字段 | `MUpload`。 |

## 3. 集成形态

`admin-pages`：

- 从 `@mango/file` 的 `admin-pages` 子入口导入 `registerMangoFileAdminPages()`。
- 注册模块码是 `mango-file`。
- 页面 key 是 `file/files/index`、`file/storage-configs/index`、`file/settings/index`。

`business-component`：

- `MUpload` 负责选择文件、前端预检查、调用文件上传接口并回写文件 ID、token 或完整记录。
- `MUpload` 缩略图优先使用 `directPreviewUrl`、`directDownloadUrl` 或 `url` 等直连地址；没有直连地址时，通过鉴权下载生成临时 `blob:` 地址用于图片回显，不会把预览地址写入业务表单值。
- `FilePreviewPanel` 负责按文件 ID 或文件记录加载预览元数据，并展示预览、下载和新窗口预览操作。

`api-client`：

- `fileApi` 封装文件记录、上传、分片、预览和下载。
- `fileStorageApi` 封装存储配置。
- `fileSettingsApi` 封装文件中心运行时配置。

## 4. 接入方式

安装依赖：

```bash
pnpm add @mango/file
```

注册管理页面：

```ts
import { registerMangoFileAdminPages } from '@mango/file/admin-pages';
import '@mango/file/style.css';

registerMangoFileAdminPages();
```

业务表单使用上传组件：

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

详情页使用预览组件：

```vue
<script setup lang="ts">
import { FilePreviewPanel } from '@mango/file';
</script>

<template>
  <FilePreviewPanel :file-id="attachmentId" />
</template>
```

## 5. 快速开始

1. 后端应用启用 `mango-file`，需要文档预览时同时启用 `mango-file-preview`。
2. 管理后台调用 `registerMangoFileAdminPages()`。
3. 打开“文件配置”页面设置 `maxSize`、扩展名黑白名单、秒传、直传、预览和归档策略。
4. 业务表单使用 `MUpload`，业务接口保存文件 ID 或业务自己的附件关系。
5. 详情页使用 `FilePreviewPanel` 回显文件预览和下载入口。

## 6. 配置说明

前端包没有独立配置文件。配置来自三个地方：

| 配置来源 | 用途 |
|----------|------|
| `registerMangoFileAdminPages()` | 注册文件模块管理页面。 |
| `GET /file/settings` | 后端文件运行时配置，控制大小限制、秒传、直传、访问、预览和归档策略。 |
| `FilePreviewPanel.previewProviderUrl` 或 `VITE_FILE_PREVIEW_PROVIDER_URL` | 文档预览服务地址兜底。 |

限制上传大小：

- 管理端在“文件配置”页面保存 `maxSize`。
- 后端可配置 `mango.file.upload.max-size`。
- `MUpload.size`、`MUpload.sizes` 只是前端提前拦截，最终以后端 `maxSize` 为准。

开启秒传：

- 管理端在“文件配置”页面打开 `instantUploadEnabled`，并设置 `instantUploadScope`。
- 后端可配置 `mango.file.upload.instant-upload-enabled`。
- 前端上传会话会提交 `fileHash`，后端命中后返回 `instant=true` 和 `fileRecord`。

开启直传：

- 管理端在“文件配置”页面打开 `directUploadEnabled`。
- 对象存储支持 multipart 时，前端会使用分片签名链路；否则走服务端分片接收。

## 7. API 与扩展

页面导出：

| 导出 | 标识 | 页面 key |
|------|------|----------|
| `FileView` | `admin-pages` | `file/files/index` |
| `FileStorageView` | `admin-pages` | `file/storage-configs/index` |
| `FileSettingsView` | `admin-pages` | `file/settings/index` |

组件导出：

| 导出 | 标识 | 用途 |
|------|------|------|
| `MUpload` | `business-component` | 上传文件并回写 ID、token 或记录。 |
| `FilePreviewPanel` | `business-component` | 预览、下载和打开文件。 |

`fileApi`：

| 方法 | 接口 | 用途 |
|------|------|------|
| `page(params)` | `GET /file/files/page` | 文件分页。 |
| `detail(id)` | `GET /file/files/detail` | 文件详情。 |
| `preview(id)` | `GET /file/files/preview` | 文件预览元数据。 |
| `previewLink(id)` | `GET /file-preview/files/preview-link` | 文档预览链接。 |
| `upload(file, params, options)` | `POST /file/files` 或上传会话链路 | 上传单文件。 |
| `uploadBatch(files, params, options)` | `POST /file/files/batch` | 批量上传小文件。 |
| `archive(id, reason)` | `DELETE /file/files` | 归档。 |
| `delete(ids)` | `POST /file/files/delete` | 删除记录。 |
| `createUploadSession(command)` | `POST /file/files/uploads` | 初始化分片上传和秒传。 |
| `createUploadPartSign(sessionId, partNumber, partSize)` | `POST /file/files/uploads/{sessionId}/parts/sign` | 获取直传签名。 |
| `uploadServerPart(sessionId, partNumber, chunk, fileName)` | `POST /file/files/uploads/{sessionId}/parts` | 服务端接收分片。 |
| `completeUploadPart(sessionId, command)` | `PUT /file/files/uploads/{sessionId}/parts` | 确认直传分片。 |
| `completeUploadSession(sessionId)` | `POST /file/files/uploads/{sessionId}/complete` | 完成上传。 |
| `abortUploadSession(sessionId)` | `DELETE /file/files/uploads/{sessionId}` | 取消上传。 |
| `downloadUrl(id)` | `/api/file/files/download?id=...` | 拼接下载地址。 |
| `download(id)` | `GET /file/files/download` | 下载二进制。 |

`fileStorageApi`：

| 方法 | 接口 |
|------|------|
| `page(params)` | `GET /file/storage-configs/page` |
| `detail(id)` | `GET /file/storage-configs/detail` |
| `create(data)` | `POST /file/storage-configs` |
| `update(data)` | `PUT /file/storage-configs` |
| `delete(id)` | `DELETE /file/storage-configs` |
| `activate(id)` | `PUT /file/storage-configs/active` |
| `test(command)` | `POST /file/storage-configs/test` |

`fileSettingsApi`：

| 方法 | 接口 |
|------|------|
| `get()` | `GET /file/settings` |
| `save(data)` | `PUT /file/settings` |

前端默认分片阈值 `DEFAULT_MULTIPART_THRESHOLD` 是 `20 * 1024 * 1024`。

## 8. 数据与初始化

这个前端包不包含数据库 migration。菜单、权限、存储默认配置和文件设置由后端 `mango-file` 初始化。

| 数据 | 来源 |
|------|------|
| 文件记录、目录、存储配置、文件设置 | 后端 `mango-file`。 |
| 文档预览链接 | 后端 `mango-file-preview`。 |
| 菜单和权限 | 后端 migration 写入 `authorization_menu`。 |

菜单 component 和前端页面 key：

| 后端菜单 component | 前端页面 key |
|-------------------|--------------|
| `@/views/file/files/index.vue` | `file/files/index` |
| `@/views/file/storage-configs/index.vue` | `file/storage-configs/index` |
| `@/views/file/settings/index.vue` | `file/settings/index` |

## 9. 管理入口

文件模块常用权限码：

| 范围 | 权限码 |
|------|--------|
| 文件 | `file:files:list`、`file:files:query`、`file:files:upload`、`file:files:download`、`file:files:archive`、`file:files:delete` |
| 目录 | `file:directories:list`、`file:directories:add`、`file:directories:edit`、`file:directories:delete` |
| 存储配置 | `file:storage-configs:list`、`file:storage-configs:query`、`file:storage-configs:add`、`file:storage-configs:edit`、`file:storage-configs:delete`、`file:storage-configs:active`、`file:storage-configs:test` |
| 设置 | `file:settings:query`、`file:settings:edit` |

前端只负责页面注册、按钮显隐和交互展示。租户、目录、文件状态、访问级别、业务归属和下载权限由后端校验。

## 10. 问题排查

**上传大小被拒**

以后端 `GET /file/settings` 返回的 `maxSize` 为准。组件 `size` 只是提前拦截。

**秒传没有命中**

检查后端是否开启 `instantUploadEnabled`，以及上传会话是否提交了 `fileHash`。

**页面空白或 404**

确认业务 admin app 调用了 `registerMangoFileAdminPages()`，再检查菜单 component 和页面 key 映射。

**下载按钮不显示或接口返回 403**

检查 `file:files:download`、文件状态、访问级别、租户和业务归属。

**文档预览打不开**

检查 `mango-file-preview`、`previewProviderUrl` 和 `/file-preview/files/preview-link`。

## 11. 相关文档

- [文件组件 README](./src/components/README.md)
- [Mango File 后端 README](../../../mango/mango-platform/mango-file/README.md)
- [Mango File Preview README](../../../mango/mango-platform/mango-file-preview/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
