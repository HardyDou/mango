# File：从表单上传到详情预览

## 先说人话

业务表单只需要保存文件中心的 `fileId` 或业务附件关系。上传组件负责把文件交给文件中心，详情页预览组件再根据 `fileId` 获取临时预览内容。

**不要把 `previewUrl`、`downloadUrl`、对象存储地址或预签名 URL 保存成业务数据。**

## 一个具体场景

合同表单需要上传营业执照：

1. 用户选择文件。
2. `MUpload` 上传到 Mango File，回写 `fileId`。
3. 合同接口只保存 `fileId`。
4. 合同详情页把 `fileId` 传给 `FilePreviewPanel`。
5. File Preview 按当前登录用户、租户和文件归属生成可用预览内容。
6. 预览失败时显示失败状态；下载仍是单独动作。

最小流程：

```text
选择文件 -> MUpload -> fileId -> 业务表单保存
                              └─详情页 -> FilePreviewPanel -> 预览/下载
```

## 最小接入

安装并引入样式：

```bash
pnpm add @mango/file
```

表单上传：

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

详情预览：

```vue
<script setup lang="ts">
import { FilePreviewPanel } from '@mango/file';
</script>

<template>
  <FilePreviewPanel :file-id="attachmentId" />
</template>
```

固定高度的弹框或容器可以显式使用 `fit-container`：

```vue
<FilePreviewPanel :file-id="attachmentId" fit-container />
```

## 谁负责什么

| 部分 | 负责什么 |
|---|---|
| `MUpload` | 选择、预检查、上传，并回写文件 ID 或附件关系 |
| 业务表单 | 保存 `fileId` 或业务附件关系 |
| `FilePreviewPanel` | 按文件 ID 加载预览元数据并展示预览/下载动作 |
| `mango-file` 后端 | 文件状态、租户、访问级别、业务归属和接口校验 |
| `mango-file-preview` | 文档转换和文档预览入口 |

## 本次只需要记住的边界

- `previewUrl` 和 `downloadUrl` 是运行时地址，不是业务字段。
- `FilePreviewPanel` 不接受下载地址冒充预览地址。
- 前端组件不绕过后端权限或租户校验。
- 管理后台页面和业务表单组件是两种不同入口；业务页面通常使用 `MUpload` 和 `FilePreviewPanel`。

## 失败时先看哪里

| 现象 | 先检查 |
|---|---|
| 上传成功但提交内容错误 | 表单是否提交了 `fileId`，而不是 URL |
| 详情不显示预览 | 是否传入正确 `fileId`，后端文件状态和租户是否有效 |
| 文档无法预览 | 是否启用 `mango-file-preview`，以及转换服务配置 |
| 页面只有下载没有预览 | 是否把下载接口写入了预览字段 |
| 组件样式异常 | 是否引入 `@mango/file/style.css` |

## 权威来源

- [@mango/file README](../../../mango-ui/packages/file/README.md)
- [文件上传表单接入](../business-integration/file-upload-form.md)
- [File 后端 README](../../../mango/mango-platform/mango-file/README.md)
