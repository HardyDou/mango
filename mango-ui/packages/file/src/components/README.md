# File Components

## 1. 入口定位

本入口说明 `@mango/file` 对外复用组件：`MUpload` 和 `FilePreviewPanel`。它们用于业务页面接入 Mango 文件上传、文件引用回显、图片/PDF/音视频内联预览和文档预览跳转。

## 2. 公开导出

来自 `@mango/file`：

- `MUpload`：上传组件。
- `FilePreviewPanel`：文件预览面板。
- `UploadColumn`、`UploadColumnKey`、`UploadDisplay`、`UploadSizeRules`、`UploadValueType`：上传组件类型。

页面注册入口仍在 `@mango/file/admin-pages`，组件 key 见 package README。

## 3. 使用场景

- 业务表单上传附件、图片、合同、凭证或审批材料。
- 详情页展示已上传文件并允许下载或预览。
- 工作流运行时表单通过 `RuntimeFormRenderer` 渲染上传字段。
- 文件中心管理页复用上传和预览能力。

## 4. 接入方式

```ts
import { MUpload, FilePreviewPanel } from '@mango/file';
import '@mango/file/style.css';
```

`MUpload` 最小用法：

```vue
<MUpload v-model="fileIds" :count="5" value-type="id" biz-type="contract" :biz-id="contractId" />
```

`FilePreviewPanel` 最小用法：

```vue
<FilePreviewPanel :file-id="fileId" />
```

## 5. Props / 参数 / 事件

`MUpload` 关键 props：

- `modelValue`：支持 id、token、record 或数组。
- `fmt`：限制扩展名或类型集合。
- `count`：最大文件数。
- `size` / `sizes`：文件大小限制。
- `display`：`list`、`thumbnail`、`table`、`drag`。
- `valueType`：`id`、`token`、`record`。
- `auto`：是否自动上传。
- `readonly`：只读展示。
- `purpose`、`accessLevel`、`bizType`、`bizId`、`bizMeta`、`directoryId`：上传业务元数据。

`MUpload` 事件：

- `update:modelValue`
- `change`
- `success`
- `error`

`FilePreviewPanel` 关键 props：

- `fileId` 或 `file`：文件引用。
- `preview`：已加载的预览对象。
- `previewProviderUrl`：外部文档预览服务地址。
- `previewExternalExtensions`：交给外部预览的扩展名。
- `downloadPermission`：下载按钮权限码，默认 `file:files:download`。
- `showActions`：是否展示下载和新窗口预览操作。

`FilePreviewPanel` 事件：

- `actions-change`

## 6. 后端依赖

- 后端模块：`mango-platform/mango-file`。
- 文档预览：`mango-platform/mango-file-preview` 和 `mango-infra/mango-infra-fileproc`。
- API 前缀：`/file/files`、`/file/storage-configs`、`/file/settings`、`/file/directories`、`/file-preview/files`。
- 上传、下载、预览 URL 来自 `src/api/file.ts` 的 `fileApi`。

## 7. 权限 / 租户 / 数据边界

- 下载按钮默认依赖 `file:files:download`。
- 文件访问级别通过 `accessLevel` 传入，常见值包括 `PRIVATE`、`PUBLIC_READ`、`INTERNAL`。
- 租户、目录、业务归属和下载权限由后端文件模块校验。
- 组件只负责采集上传元数据和展示结果，不绕过后端文件授权。

## 8. 验证方式

```bash
pnpm -F @mango/file build
```

页面验收入口：

- 文件管理：`file/files/index`
- 存储配置：`file/storage-configs/index`
- 文件设置：`file/settings/index`

最小断言：

- 上传成功后 `modelValue` 按 `valueType` 返回。
- 超出 `fmt` 或 `size` 时前端阻止上传并提示。
- `FilePreviewPanel` 对图片、PDF、音视频使用内联预览，对文档走预览服务或下载。

## 9. 常见问题

- 页面能上传但不能下载时，先检查 `file:files:download` 和后端文件访问级别。
- 预览为空时，先检查 `fileId` 是否能通过 `fileApi.preview` 返回预览对象。
- 文档预览失败时，检查 `VITE_FILE_PREVIEW_PROVIDER_URL` 或后端 `file-preview` 服务。

## 10. 关联文档

- [@mango/file README](../../README.md)
- [Mango File 后端 README](../../../../../mango/mango-platform/mango-file/README.md)
- [File Preview 后端 README](../../../../../mango/mango-platform/mango-file-preview/README.md)
- [能力地图](../../../../../mango-docs/capabilities/README.md)
- [能力说明维护规范](../../../../../mango-pmo/rules/08-capability-docs.md)
