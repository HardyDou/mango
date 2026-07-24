# @mango/file

`@mango/file` 是 Mango 文件能力的前端接入包，提供文件管理页面、存储配置页面、文件设置页面、上传组件、预览组件和 API 封装。

## 1. 概览

这个包有三种集成形态：

| 标识                 | 内容                               |
| -------------------- | ---------------------------------- |
| `admin-pages`        | 文件管理、存储配置、文件设置页面。 |
| `business-component` | `MUpload`、`FilePreviewPanel`。    |
| `api-client`         | 文件、存储配置、文件设置接口封装。 |

`admin-pages` 面向 Mango 管理后台；官网、营销站或独立前台项目通常不应直接复用这些管理页面。业务页面需要上传或预览文件时，优先使用 `MUpload` 和 `FilePreviewPanel`。

## 2. 功能清单

| 能力                                                 | 使用入口                                |
| ---------------------------------------------------- | --------------------------------------- |
| 文件记录分页、详情、上传、下载、归档、删除           | `FileView`、`fileApi`。                 |
| 存储配置分页、详情、新增、编辑、删除、测试、设为默认 | `FileStorageView`、`fileStorageApi`。   |
| 上传限制、秒传、直传、访问、预览、归档策略           | `FileSettingsView`、`fileSettingsApi`。 |
| 业务表单上传附件或图片                               | `MUpload`。                             |
| 详情页预览图片、PDF、音视频或文档                    | `FilePreviewPanel`。                    |
| 工作流运行时表单上传字段                             | `MUpload`。                             |

## 3. 集成形态

`admin-pages`：

- 从 `@mango/file` 的 `admin-pages` 子入口导入 `registerMangoFileAdminPages()`。
- 注册模块码是 `mango-file`。
- 页面 key 是 `file/files/index`、`file/storage-configs/index`、`file/settings/index`。

`business-component`：

- `MUpload` 负责选择文件、前端预检查、调用文件上传接口并回写文件 ID、token 或完整记录。
- `MUpload` 上传和回显只要求文件 ID、`previewUrl`、`downloadUrl` 这些业务字段；需要图片缩略图时，组件会按文件 ID 获取预览元数据，或通过鉴权下载生成临时 `blob:` 地址，不会把预览地址写入业务表单值。
- `FilePreviewPanel` 负责按文件 ID 或文件记录加载预览元数据，并展示预览、下载和新窗口预览操作；预览区域只使用有效 `previewUrl`、预览元数据中的临时展示地址或文档预览服务地址，`downloadUrl` 和 `fileApi.downloadUrl(id)` 只用于下载动作。

`api-client`：

- `fileApi` 封装文件记录、上传、分片、预览和下载。
- `fileStorageApi` 封装存储配置。

URL 字段职责：

- `FileRecord.previewUrl` 是文件原始内容预览地址，适合图片、PDF、音视频等浏览器可直接内联展示的文件。
- `FileRecord.downloadUrl` 是文件下载地址，只用于下载动作。
- `FileRecord` 是业务可见文件记录，不包含 `storageType`、`bucketName`、`objectName`、`url`、`directPreviewUrl`、`directDownloadUrl` 等存储层或直连细节。
- 后端 `PROXY` 模式下，预览和下载是两个不同接口；后端 `DIRECT` 模式下，两个字段来自存储公开访问地址，可能相同。
- `FilePreviewPanel` 需要 Office 转换或文档预览服务时，会按文件 ID 获取预览元数据，并使用 `documentPreviewUrl`；这条链路不要求业务保存或理解存储公开访问字段。
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

| 配置来源                                                                  | 用途                                                                       |
| ------------------------------------------------------------------------- | -------------------------------------------------------------------------- |
| `registerMangoFileAdminPages()`                                           | 注册文件模块管理页面。                                                     |
| `GET /file/settings`                                                      | 后端文件运行时配置，控制大小限制、分片、秒传、直传、访问、预览和归档策略。 |
| `FilePreviewPanel.previewProviderUrl` 或 `VITE_FILE_PREVIEW_PROVIDER_URL` | 文档预览服务地址兜底。                                                     |

限制上传大小：

- 管理端在“文件配置”页面保存 `maxSize`。
- 后端可配置 `mango.file.upload.max-size`。
- `MUpload.size`、`MUpload.sizes` 只是前端提前拦截，最终以后端 `maxSize` 为准。

配置分片上传：

- 管理端在“文件配置”页面设置 `multipartEnabled` 和 `multipartThreshold`。
- 默认启用分片，默认临界值为 `20 * 1024 * 1024` 字节（20 MiB）。
- `fileApi.upload()` 和 `MUpload` 会读取当前租户运行时配置；关闭分片时改走普通上传。
- HTTP IP 等非安全上下文没有 Web Crypto 时，前端不再因 SHA-256 计算失败中断，而是省略客户端哈希并走 `SERVER_CHUNK`。本次上传不能预先秒传，服务端合并后会补算哈希，后续上传可继续秒传。

开启秒传：

- 管理端在“文件配置”页面打开 `instantUploadEnabled`，并设置 `instantUploadScope`。
- 后端可配置 `mango.file.upload.instant-upload-enabled`。
- 前端上传会话会提交 `fileHash`，后端命中后返回 `instant=true` 和 `fileRecord`。

开启直传：

- 管理端在“文件配置”页面打开 `directUploadEnabled`。
- 对象存储支持 multipart 时，前端会使用分片签名链路；否则走服务端分片接收。

## 7. API 与扩展

页面导出：

| 导出               | 标识          | 页面 key                     |
| ------------------ | ------------- | ---------------------------- |
| `FileView`         | `admin-pages` | `file/files/index`           |
| `FileStorageView`  | `admin-pages` | `file/storage-configs/index` |
| `FileSettingsView` | `admin-pages` | `file/settings/index`        |

组件导出：

| 导出               | 标识                 | 用途                              |
| ------------------ | -------------------- | --------------------------------- |
| `MUpload`          | `business-component` | 上传文件并回写 ID、token 或记录。 |
| `FilePreviewPanel` | `business-component` | 预览、下载和打开文件。            |

`fileApi`：

| 方法                                                       | 接口                                              | 用途                   |
| ---------------------------------------------------------- | ------------------------------------------------- | ---------------------- |
| `page(params)`                                             | `GET /file/files/page`                            | 文件分页。             |
| `detail(id)`                                               | `GET /file/files/detail`                          | 文件详情。             |
| `preview(id)`                                              | `GET /file/files/preview`                         | 文件预览元数据。       |
| `previewLink(id)`                                          | `GET /file-preview/files/preview-link`            | 文档预览链接。         |
| `upload(file, params, options)`                            | `POST /file/files` 或上传会话链路                 | 上传单文件。           |
| `uploadPolicy()`                                           | `GET /file/settings`                              | 读取当前租户分片策略。 |
| `uploadBatch(files, params, options)`                      | `POST /file/files/batch`                          | 批量上传小文件。       |
| `archive(id, reason)`                                      | `DELETE /file/files`                              | 归档。                 |
| `delete(ids)`                                              | `POST /file/files/delete`                         | 删除记录。             |
| `createUploadSession(command)`                             | `POST /file/files/uploads`                        | 初始化分片上传和秒传。 |
| `createUploadPartSign(sessionId, partNumber, partSize)`    | `POST /file/files/uploads/{sessionId}/parts/sign` | 获取直传签名。         |
| `uploadServerPart(sessionId, partNumber, chunk, fileName)` | `POST /file/files/uploads/{sessionId}/parts`      | 服务端接收分片。       |
| `completeUploadPart(sessionId, command)`                   | `PUT /file/files/uploads/{sessionId}/parts`       | 确认直传分片。         |
| `completeUploadSession(sessionId)`                         | `POST /file/files/uploads/{sessionId}/complete`   | 完成上传。             |
| `abortUploadSession(sessionId)`                            | `DELETE /file/files/uploads/{sessionId}`          | 取消上传。             |
| `downloadUrl(id)`                                          | `/api/file/files/download?id=...`                 | 拼接下载地址。         |
| `download(id)`                                             | `GET /file/files/download`                        | 下载二进制。           |

`fileStorageApi`：

| 方法            | 接口                               |
| --------------- | ---------------------------------- |
| `page(params)`  | `GET /file/storage-configs/page`   |
| `detail(id)`    | `GET /file/storage-configs/detail` |
| `create(data)`  | `POST /file/storage-configs`       |
| `update(data)`  | `PUT /file/storage-configs`        |
| `delete(id)`    | `DELETE /file/storage-configs`     |
| `activate(id)`  | `PUT /file/storage-configs/active` |
| `test(command)` | `POST /file/storage-configs/test`  |

`fileSettingsApi`：

| 方法         | 接口                 |
| ------------ | -------------------- |
| `get()`      | `GET /file/settings` |
| `save(data)` | `PUT /file/settings` |

前端分片开关和临界值来自 `GET /file/settings`；接口失败时回退为启用分片、临界值 `20 * 1024 * 1024`。

## 8. 数据与初始化

这个前端包不包含数据库 migration。菜单、权限、存储默认配置和文件设置由后端 `mango-file` 初始化。

| 数据                               | 来源                                       |
| ---------------------------------- | ------------------------------------------ |
| 文件记录、目录、存储配置、文件设置 | 后端 `mango-file`。                        |
| 文档预览链接                       | 后端 `mango-file-preview`。                |
| 菜单和权限                         | 后端 migration 写入 `authorization_menu`。 |

菜单 component 和前端页面 key：

| 后端菜单 component                       | 前端页面 key                 |
| ---------------------------------------- | ---------------------------- |
| `@/views/file/files/index.vue`           | `file/files/index`           |
| `@/views/file/storage-configs/index.vue` | `file/storage-configs/index` |
| `@/views/file/settings/index.vue`        | `file/settings/index`        |

## 9. 管理入口

文件模块访问基线：

| 范围                                                                 | 访问要求                                                                                                                                                                                                      |
| -------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 详情、上传、批量、秒传、分片、预览、下载、打包、合并、运行时设置读取 | 登录即可，不要求角色权限码                                                                                                                                                                                    |
| 跨域预览、下载                                                       | 直接使用查询返回的存储安全签名 URL，默认有效期 24 小时                                                                                                                                                        |
| 文件管理                                                             | `file:files:list`、`file:files:archive`、`file:files:delete`                                                                                                                                                  |
| 目录                                                                 | `file:directories:list`、`file:directories:add`、`file:directories:edit`、`file:directories:delete`                                                                                                           |
| 存储配置                                                             | `file:storage-configs:list`、`file:storage-configs:query`、`file:storage-configs:add`、`file:storage-configs:edit`、`file:storage-configs:delete`、`file:storage-configs:active`、`file:storage-configs:test` |
| 设置管理                                                             | `file:settings:edit`                                                                                                                                                                                          |

前端只负责页面注册、按钮显隐和交互展示。租户、目录、文件状态、访问级别、业务归属和下载权限由后端校验。

## 10. 问题排查

**上传大小被拒**

以后端 `GET /file/settings` 返回的 `maxSize` 为准。组件 `size` 只是提前拦截。

**秒传没有命中**

检查后端是否开启 `instantUploadEnabled`，以及上传会话是否提交了 `fileHash`。

**页面空白或 404**

确认业务 admin app 调用了 `registerMangoFileAdminPages()`，再检查菜单 component 和页面 key 映射。

**上传、预览或下载接口返回 401/403**

确认当前用户已登录，并检查文件状态、访问级别、租户和业务归属。基础文件接口不再依赖 `file:files:upload`、`file:files:query` 或 `file:files:download` 角色权限；跨域预览和下载直接使用查询返回的安全签名 URL，过期后重新查询文件记录。

**文档预览打不开**

检查 `mango-file-preview`、`previewProviderUrl` 和 `/file-preview/files/preview-link`。

**点击预览却触发浏览器下载**

检查业务侧是否把 `/api/file/files/download` 或 `/file/files/download` 这类下载接口写入了 `previewUrl`。`FilePreviewPanel` 会拒绝把下载接口作为预览地址；没有可用预览地址时，页面展示下载查看提示，由用户手动点击下载。

## 11. 相关文档

- [文件组件 README](./src/components/README.md)
- [Mango File 后端 README](../../../mango/mango-platform/mango-file/README.md)
- [Mango File Preview README](../../../mango/mango-platform/mango-file-preview/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 12. 变更影响记录

- `@mango/file@1.0.26` publishes the file preview dialog fix: PDF preview fills the dialog content height and owns its own scrolling; the open-in-new-window action always renders and is disabled only when no usable preview URL exists. File ID persistence, upload, download, preview APIs, page keys, permissions and backend startup stay compatible.

- `@mango/file@1.0.22` 发布文件预览弹框体验修复：文件管理页预览弹框中的 `FilePreviewPanel`
  会按弹框内容区高度铺满，PDF 由自身滚动承载内容；新窗口预览按钮始终渲染，缺少可用预览地址时仅禁用。
  文件 ID 持久化、上传、下载、预览 API、页面 key、权限和后端启动方式不变。
- `@mango/file@1.0.21` 向前发布当前文件访问实现和完整 README，不恢复已移除的
  `FilePreviewPanel.downloadPermission`，也不回退登录级上传、预览和下载访问基线；运行时行为与 `1.0.20`
  一致，并将 `@mango/admin-pages` 精确依赖对齐到本发布批次。
- v2026.07.11-npm-lock-sync-release 将文件管理页的上传、预览和下载操作对齐到登录级文件访问基线，不再用
  `file:files:upload`、`file:files:query` 或 `file:files:download` 控制前端按钮显隐；最终访问仍由后端依据登录态、
  文件状态、访问级别、租户和业务归属校验。列表、归档、删除、目录、存储配置和文件设置继续使用既有细粒度权限。
- `FilePreviewPanel` 移除了 `downloadPermission` prop，详情预览组件不再接受自定义下载按钮权限码。业务页面应删除
  该 prop，并继续通过文件服务返回的下载地址或组件 `openDownload()` 执行下载。
- 文件管理预览弹框调整为响应式宽高，`FilePreviewPanel` 支持通过 CSS 变量适配弹框内容高度；文件 ID 持久化、上传、
  下载、预览 API、页面 key 和后端启动方式不变。
