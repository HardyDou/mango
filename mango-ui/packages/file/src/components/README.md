# @mango/file Components

本入口说明 `@mango/file` 的公共组件：`MUpload` 和 `FilePreviewPanel`。它们用于业务表单上传文件、详情页预览文件、工作流运行时表单上传附件。

## 1. 概览

这里的能力属于 `business-component`。管理页面注册请使用包根目录 README 中的 `admin-pages` 入口。

## 2. 功能清单

| 能力 | 导出 |
|------|------|
| 上传文件 | `MUpload` |
| 预览文件 | `FilePreviewPanel` |
| 上传组件类型 | `UploadColumn`、`UploadColumnKey`、`UploadDisplay`、`UploadSizeRules`、`UploadValueType` |

## 3. 接入方式

引入组件和样式：

```ts
import '@mango/file/style.css';
import { FilePreviewPanel, MUpload } from '@mango/file';
```

上传并保存文件 ID：

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { MUpload } from '@mango/file';

const fileIds = ref<string[]>([]);
</script>

<template>
  <MUpload v-model="fileIds" value-type="id" :count="5" />
</template>
```

完整上传用法：

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

预览文件：

```vue
<FilePreviewPanel :file-id="fileId" />
```

## 4. 参数与事件

`MUpload` props：

| Prop | 类型 | 默认值 | 含义 |
|------|------|--------|------|
| `modelValue` | `string \| string[] \| FileRecord \| FileRecord[] \| null` | 空 | 表单值。 |
| `fmt` | `string \| string[]` | 空 | 前端扩展名限制，例如 `pdf,docx,png`。 |
| `count` | `number` | `1` | 最大文件数；大于 1 时允许多选。 |
| `size` | `string \| number` | 空 | 前端通用大小限制，支持字节数或 `20MB`。 |
| `sizes` | `UploadSizeRules` | 空 | 按文件类型设置大小限制。 |
| `display` | `list \| thumbnail \| table \| drag` | `list` | 展示样式。 |
| `columns` | `UploadColumn[] \| UploadColumnKey[]` | 默认列 | `display="table"` 时的列配置。 |
| `valueType` | `id \| token \| record` | `token` | 回写值格式。 |
| `auto` | `boolean` | `true` | 是否选择文件后立即上传。 |
| `readonly` | `boolean` | `false` | 只读展示。 |
| `purpose` | `string` | `attachment` | 上传用途。 |
| `accessLevel` | `string` | `PRIVATE` | 文件访问级别。 |
| `bizType` | `string` | 空 | 业务类型。 |
| `bizId` | `string \| number` | 空 | 业务 ID。 |
| `bizMeta` | `object \| string` | 空 | 业务元数据。 |
| `directoryId` | `string \| number` | 空 | 文件中心逻辑目录 ID。 |
| `buttonText` | `string` | `上传文件` | 默认按钮文字。 |

`valueType`：

| 值 | 回写内容 |
|----|----------|
| `id` | 文件记录 ID。 |
| `token` | `mango-file:<id>`。 |
| `record` | 完整 `FileRecord`。 |

`MUpload` events：

| 事件 | 参数 | 含义 |
|------|------|------|
| `update:modelValue` | `value` | v-model 更新。 |
| `change` | `value, records` | 文件列表变化。 |
| `success` | `record` | 单个文件上传成功。 |
| `error` | `error` | 上传失败。 |

`MUpload` slots：

| Slot | 含义 |
|------|------|
| `trigger` | 默认上传触发器。 |
| `thumbnail-trigger` | 缩略图模式触发器。 |
| `drag-trigger` | 拖拽模式触发器。 |

上传策略：

- `auto=true` 时，选择文件后调用 `fileApi.upload()`。
- `auto=false` 时，点击“上传到服务器”后提交。
- 小文件批量上传调用 `fileApi.uploadBatch()`。
- 单个文件或存在大文件时逐个上传，便于走分片链路。
- 前端默认分片阈值是 `20MB`。
- 秒传命中时，后端返回 `instant=true` 和 `fileRecord`，组件直接回写。
- `fmt`、`size`、`sizes` 是前端预检查；后端最终限制来自 `GET /file/settings`。

`FilePreviewPanel` props：

| Prop | 类型 | 默认值 | 含义 |
|------|------|--------|------|
| `fileId` | `ApiId \| mango-file:<id> \| null` | 空 | 文件 ID 或 token。 |
| `file` | `FileReference` | 空 | 文件引用，优先用于解析 ID。 |
| `preview` | `FilePreview \| null` | 空 | 外部已加载预览对象；传入后不再请求 `fileApi.preview()`。 |
| `previewProviderUrl` | `string` | 环境变量或后端返回 | 文档预览服务地址。 |
| `previewExternalExtensions` | `string[]` | 空 | 外部预览扩展名。 |
| `downloadPermission` | `string` | `file:files:download` | 下载按钮权限码。 |
| `showActions` | `boolean` | `true` | 是否显示下载和新窗口预览操作。 |

`FilePreviewPanel` events 和 expose：

| 名称 | 参数 | 含义 |
|------|------|------|
| `actions-change` | `{ canDownload, canOpenInNewWindow }` | 操作可用状态变化。 |
| `openDownload()` | 无 | 暴露方法，触发下载。 |
| `openPreviewInNewWindow()` | 无 | 暴露方法，打开预览。 |

预览规则：

- 图片：`image/*` 或 `jpg`、`jpeg`、`png`、`gif`、`webp`、`bmp`、`svg`、`ico`。
- PDF：`application/pdf` 或 `pdf`。
- 视频：`video/*` 或 `mp4`、`webm`、`ogg`、`mov`、`m4v`。
- 音频：`audio/*` 或 `mp3`、`wav`、`ogg`、`m4a`、`aac`、`flac`。
- 图片、PDF、音视频的内联预览只使用 `directPreviewUrl` 或有效 `previewUrl`；不会使用 `directDownloadUrl`、`downloadUrl` 或 `fileApi.downloadUrl(id)` 兜底。
- 其他文件优先使用有效 `previewUrl`；默认预览入口会请求 `/file-preview/files/preview-link`。
- `/api/file/files/download` 和 `/file/files/download` 这类下载接口不会进入预览区域。
- 没有可用预览地址时，展示下载查看提示，不会自动触发下载。

## 5. 后端依赖

| 能力 | 后端依赖 |
|------|----------|
| 上传、批量上传、下载、预览元数据 | `mango-file`。 |
| 分片上传和秒传 | `/file/files/uploads` 上传会话接口。 |
| 文档预览链接 | `mango-file-preview` 的 `/file-preview/files/preview-link`。 |
| Office/PDF 等文件处理 | `mango-infra-fileproc`。 |

## 6. 权限与数据边界

常用权限：

| 操作 | 权限码 |
|------|--------|
| 上传 | `file:files:upload` |
| 查询预览元数据 | `file:files:query` |
| 下载 | `file:files:download` |

租户、目录、文件状态、访问级别和业务归属由后端校验。组件不会绕过后端权限。

## 7. 快速开始

1. 在业务前端包引入 `@mango/file/style.css`。
2. 上传字段使用 `MUpload v-model="fileIds" value-type="id"`，业务接口保存文件 ID。
3. 详情页使用 `FilePreviewPanel :file-id="fileId"` 回显预览和下载入口。
4. 上传大小、扩展名、秒传和预览地址以后端 `GET /file/settings` 返回值为准。

## 8. 问题排查

**组件限制了大小但接口仍拒绝**

检查后端 `GET /file/settings` 返回的 `maxSize`。

**秒传没有命中**

检查后端是否开启 `instantUploadEnabled`，以及上传会话是否提交了 `fileHash`。

**下载按钮不可见**

检查 `downloadPermission` 和当前账号授权。

**文档只能下载不能预览**

检查 `mango-file-preview` 和 `/file-preview/files/preview-link`。

**点击预览触发下载**

检查传入的 `preview.previewUrl` 是否为文件下载接口。下载接口只应给下载按钮使用；预览组件会在没有有效预览地址时展示下载查看提示。

## 9. 相关文档

- [@mango/file README](../../README.md)
- [Mango File 后端 README](../../../../../mango/mango-platform/mango-file/README.md)
- [Mango File Preview README](../../../../../mango/mango-platform/mango-file-preview/README.md)
- [能力说明维护规范](../../../../../mango-pmo/rules/08-capability-docs.md)
