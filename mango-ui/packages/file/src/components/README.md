# @mango/file Components

本目录说明 `@mango/file` 的业务复用组件：`MUpload` 和 `FilePreviewPanel`。这两个组件可以放进 Mango admin 页面、业务表单、工作流运行时表单和详情页。

## 1. 概览
本入口说明 `@mango/file` 对外复用组件：`MUpload` 和 `FilePreviewPanel`。它们用于业务页面接入 Mango 文件上传、文件引用回显、图片/PDF/音视频内联预览和文档预览跳转。

## 2. 功能清单
来自 `@mango/file`：

- `MUpload`：上传组件。
- `FilePreviewPanel`：文件预览面板。
- `UploadColumn`、`UploadColumnKey`、`UploadDisplay`、`UploadSizeRules`、`UploadValueType`：上传组件类型。

页面注册入口在 `@mango/file/admin-pages`。

## 3. 适用场景
- 业务表单上传附件、图片、合同、凭证或审批材料。
- 详情页展示已上传文件并允许下载或预览。
- 工作流运行时表单通过表单渲染器渲染上传字段。
- 文件中心管理页复用上传和预览能力。

## 4. 接入方式
```ts
import '@mango/file/style.css';
import { FilePreviewPanel, MUpload } from '@mango/file';
```

### 4.1 MUpload

`MUpload` 封装 Element Plus `el-upload`，上传到 Mango 文件中心，并把结果按指定格式回写给业务表单。

最小用法：

```vue
<MUpload v-model="fileIds" value-type="id" :count="5" />
```

完整业务用法：

```vue
<MUpload
  v-model="attachments"
  value-type="record"
  display="table"
  :count="10"
  fmt="pdf,doc,docx,png,jpg"
  size="50MB"
  purpose="contract"
  access-level="PRIVATE"
  biz-type="contract"
  :biz-id="contractId"
  :biz-meta="{ source: 'contract-form' }"
  :directory-id="directoryId"
  @success="handleUploadSuccess"
  @error="handleUploadError"
/>
```

## 5. 参数与事件
### 5.1 MUpload Props

| Prop | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `modelValue` | `string \| string[] \| FileRecord \| FileRecord[] \| null` | 空 | 表单值。 |
| `fmt` | `string \| string[]` | 空 | 前端扩展名限制，例如 `pdf,docx,png`。 |
| `count` | `number` | `1` | 最大文件数；大于 1 时允许多选。 |
| `size` | `string \| number` | 空 | 前端通用大小限制，支持字节数或 `20MB` 这类字符串。 |
| `sizes` | `UploadSizeRules` | 空 | 按文件类型设置大小限制，如 `{ image: '5MB', video: '200MB' }`。 |
| `display` | `list \| thumbnail \| table \| drag` | `list` | 展示样式。 |
| `columns` | `UploadColumn[] \| UploadColumnKey[]` | 默认列 | `display="table"` 时的列配置。 |
| `valueType` | `id \| token \| record` | `token` | 回写值格式。 |
| `auto` | `boolean` | `true` | 是否选择文件后立即上传。 |
| `readonly` | `boolean` | `false` | 只读展示。 |
| `purpose` | `string` | `attachment` | 上传用途，传给后端 `purpose`。 |
| `accessLevel` | `string` | `PRIVATE` | 传给后端 `accessLevel`，常用 `PRIVATE`、`PUBLIC_READ`、`INTERNAL`。 |
| `bizType` | `string` | 空 | 业务类型。 |
| `bizId` | `string \| number` | 空 | 业务 ID。 |
| `bizMeta` | `object \| string` | 空 | 业务元数据，最终提交给后端 `bizMeta`。 |
| `directoryId` | `string \| number` | 空 | 文件中心逻辑目录 ID。 |
| `buttonText` | `string` | `上传文件` | 默认按钮文字。 |

`valueType` 行为：

- `id`：返回文件记录 ID，适合业务表单保存附件关系。
- `token`：返回 `mango-file:<id>` 格式，适合需要区分文件来源的字段。
- `record`：返回完整 `FileRecord`，适合详情页或需要立即展示文件名、大小、URL 的页面。

### 5.2 MUpload Events

| 事件 | 参数 | 说明 |
|------|------|------|
| `update:modelValue` | `value` | v-model 更新。 |
| `change` | `value, records` | 文件列表变化。 |
| `success` | `record` | 单个文件上传成功。 |
| `error` | `error` | 上传失败。 |

### 5.3 MUpload Slots

| Slot | 说明 |
|------|------|
| `trigger` | 默认上传触发器。 |
| `thumbnail-trigger` | 缩略图模式触发器。 |
| `drag-trigger` | 拖拽模式触发器。 |

### 5.4 MUpload 上传策略

- `auto=true` 时，选择文件后调用 `fileApi.upload()`。
- `auto=false` 时，组件显示“上传到服务器”，点击后再提交。
- 单个文件或存在大文件时逐个上传，便于走分片链路。
- 小文件批量提交时调用 `fileApi.uploadBatch()`。
- 前端默认分片阈值是 `20MB`。
- 超过阈值后调用 `POST /file/files/uploads` 初始化上传会话。
- 秒传命中时，后端返回 `instant=true` 和 `fileRecord`，组件直接回写结果。
- 对象存储支持 multipart 时走直传签名；否则走 Java 服务接收分片。

`fmt`、`size`、`sizes` 是前端预检查。服务端最终限制来自 `GET /file/settings` 返回的 `maxSize`、扩展名、MIME 黑白名单。

### 5.5 FilePreviewPanel

`FilePreviewPanel` 根据 `fileId` 或 `FileRecord` 加载预览元数据，展示图片、PDF、视频、音频或文档预览服务入口。

最小用法：

```vue
<FilePreviewPanel :file-id="fileId" />
```

传入已加载文件：

```vue
<FilePreviewPanel
  :file="fileRecord"
  download-permission="file:files:download"
  @actions-change="handlePreviewActionsChange"
/>
```

### 5.6 FilePreviewPanel Props

| Prop | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `fileId` | `ApiId \| mango-file:<id> \| null` | 空 | 文件 ID 或 token。 |
| `file` | `FileReference` | 空 | 文件引用，优先用于解析 ID。 |
| `preview` | `FilePreview \| null` | 空 | 外部已加载的预览对象；传入后组件不再请求 `fileApi.preview()`。 |
| `previewProviderUrl` | `string` | `VITE_FILE_PREVIEW_PROVIDER_URL` 或后端返回 | 文档预览服务地址。 |
| `previewExternalExtensions` | `string[]` | 空 | 预览扩展名，当前主要由后端预览元数据决定。 |
| `downloadPermission` | `string` | `file:files:download` | 下载按钮权限码。 |
| `showActions` | `boolean` | `true` | 是否显示下载和新窗口预览操作。 |

### 5.7 FilePreviewPanel Events and Expose

| 名称 | 参数 | 说明 |
|------|------|------|
| `actions-change` | `{ canDownload, canOpenInNewWindow }` | 操作可用状态变化。 |
| `openDownload()` | 无 | 暴露方法，触发下载。 |
| `openPreviewInNewWindow()` | 无 | 暴露方法，打开预览。 |

### 5.8 预览规则

- 图片：`image/*` 或 `jpg`、`jpeg`、`png`、`gif`、`webp`、`bmp`、`svg`、`ico`，使用 `el-image`。
- PDF：`application/pdf` 或 `pdf`，使用 iframe。
- 视频：`video/*` 或 `mp4`、`webm`、`ogg`、`mov`、`m4v`，使用 video。
- 音频：`audio/*` 或 `mp3`、`wav`、`ogg`、`m4a`、`aac`、`flac`，使用 audio。
- 其他文件：优先使用后端 `previewUrl`；默认预览入口会进一步请求 `/file-preview/files/preview-link`。
- 没有可用预览地址时，展示下载查看提示。

## 6. 后端依赖
后端接口：

- `GET /file/files/preview`
- `GET /file/files/download`
- `POST /file/files`
- 分片上传接口 `/file/files/uploads/**`

文档预览需要：

- `GET /file-preview/files/preview-link`
- 后端 `mango-file-preview`
- 文件处理能力 `mango-infra-fileproc`

## 7. 权限与数据边界
权限：

- 上传：`file:files:upload`
- 预览元数据：`file:files:query`
- 下载：`file:files:download`

租户、目录、文件状态、访问级别和业务归属由后端校验。组件不会绕过后端权限。

## 8. 快速开始

1. 在业务前端包引入 `@mango/file/style.css`。
2. 上传字段使用 `MUpload v-model="fileIds" value-type="id"`，业务接口只接收文件 ID。
3. 详情页使用 `FilePreviewPanel :file-id="fileId"` 回显预览和下载入口。
4. 上传大小、扩展名、秒传和预览地址以后端 `GET /file/settings` 返回值为准。

## 9. 问题排查
- 组件限制了大小但接口仍拒绝：检查后端 `GET /file/settings.maxSize`。
- 秒传没有命中：检查后端是否开启 `instantUploadEnabled`，以及大文件链路是否提交 `fileHash`。
- 下载按钮不可见：检查 `downloadPermission` 和当前账号授权。
- 文档只能下载不能预览：检查 `mango-file-preview` 和 `/file-preview/files/preview-link`。

## 10. 相关文档
- [@mango/file README](../../README.md)
- [Mango File 后端 README](../../../../../mango/mango-platform/mango-file/README.md)
- [Mango File Preview README](../../../../../mango/mango-platform/mango-file-preview/README.md)
- [能力说明维护规范](../../../../../mango-pmo/rules/08-capability-docs.md)
