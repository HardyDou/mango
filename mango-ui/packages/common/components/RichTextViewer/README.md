# RichTextViewer

富文本只读展示组件。渲染经过安全过滤的 HTML，自动解析其中的托管图片/附件，并把文件点击转换为预览事件。

## 导入

```ts
import { RichTextViewer } from '@mango/common';
import type {
  RichTextAssetResolver,
  RichTextAssetContentResolver,
  RichTextViewerPreviewRequest,
  RichTextViewerResolveError,
} from '@mango/common';
```

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `content` | `string` | `''` | 富文本 HTML 字符串，渲染前会经过安全过滤 |
| `resolveFile` | `RichTextAssetResolver` | `undefined` | 按托管文件 ID 查询附件元数据，未传时使用内置 `getUploadedFileDetail` |
| `resolveFileContent` | `RichTextAssetContentResolver` | `undefined` | 按文件 ID 拉取图片二进制内容，未传时使用内置 `downloadUploadedFile` |
| `emptyText` | `string` | `'-'` | 无有效内容（空文本或仅空白标签）时的占位文本 |

## Events

| 名称 | 参数 | 说明 |
|------|------|------|
| `resolve-error` | `(payload: RichTextViewerResolveError)` | 某个托管文件元数据或内容解析失败，载荷含 `fileId` 与原始 `error` |
| `preview-request` | `(payload: RichTextViewerPreviewRequest)` | 点击或用 Enter/空格激活文中托管文件节点时触发，由宿主决定如何预览 |

## 类型定义

```ts
import type { FileId, UploadResult } from '../../api/upload';

export type RichTextAssetResolver = (id: FileId) => Promise<UploadResult>;
export type RichTextAssetContentResolver = (id: FileId, asset: UploadResult) => Promise<Blob>;

export interface RichTextViewerPreviewRequest {
  fileId: FileId;
  kind: 'image' | 'attachment';
  fileName: string;
  asset?: UploadResult;
}

export interface RichTextViewerResolveError {
  fileId: FileId;
  error: unknown;
}
```

## 使用示例

### 渲染静态富文本

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { RichTextViewer } from '@mango/common';

const html = ref('<p>这是一段<strong>富文本</strong>内容。</p>');
</script>

<template>
  <RichTextViewer :content="html" />
</template>
```

### 处理托管文件预览与解析失败

```vue
<script setup lang="ts">
import { RichTextViewer, type RichTextViewerPreviewRequest, type RichTextViewerResolveError } from '@mango/common';

function handlePreview(payload: RichTextViewerPreviewRequest) {
  if (payload.kind === 'image') {
    // 打开图片预览（如 el-image-viewer 或宿主预览弹层）
  } else {
    // 处理附件下载/预览
  }
}

function handleResolveError(payload: RichTextViewerResolveError) {
  console.error('富文本资源解析失败', payload.fileId, payload.error);
}
</script>

<template>
  <RichTextViewer
    :content="html"
    @preview-request="handlePreview"
    @resolve-error="handleResolveError"
  />
</template>
```

### 自定义资源解析

```vue
<script setup lang="ts">
import type { RichTextAssetResolver, RichTextAssetContentResolver } from '@mango/common';

// 返回附件元数据，需包含 directPreviewUrl / previewUrl / url 等可用地址
const resolveFile: RichTextAssetResolver = async (id) => {
  // 调用宿主的文件详情接口
};

// 自行实现鉴权下载，返回图片 Blob
const resolveFileContent: RichTextAssetContentResolver = async (id, asset) => {
  // 调用宿主的文件下载接口
};
</script>

<template>
  <RichTextViewer
    :content="html"
    :resolve-file="resolveFile"
    :resolve-file-content="resolveFileContent"
    empty-text="暂无正文"
  />
</template>
```

## 渲染与安全行为

- 渲染前执行安全过滤：移除所有 `script` 标签、移除 `on*` 事件属性、剔除 `javascript:` 协议的 `href` / `src` / `xlink:href`，并为 `target="_blank"` 的链接补全 `rel="noopener noreferrer"`
- 内容中的托管文件节点（带 `data-file-id`）会先批量查询元数据：优先使用 `directPreviewUrl`、`previewUrl`、`url` 中非 `javascript:` 且非受保护下载地址的链接直接展示
- 没有可用直链的图片会通过 `resolveFileContent` 拉取 Blob 并创建 object URL 展示；内容变更或组件卸载时自动回收这些 object URL
- 资源加载期间显示「正在加载资源…」，内容区半透明；单个文件解析失败不会阻断其它文件，失败信息通过 `resolve-error` 抛出
- 托管文件节点会被自动加上 `role="button"`、`tabindex="0"`，支持鼠标点击和键盘 Enter/空格触发 `preview-request`
- 判断预览类型时，节点带 `data-file-kind="image"` 或本身为 `img` 时 `kind` 为 `image`，否则为 `attachment`

## 重要约束

- 组件为纯只读渲染，不提供任何插槽和编辑能力，不要用于内容编辑场景
- 安全过滤会移除脚本与事件属性，但仍应只传入可信富文本编辑器产出的 HTML，不要直接渲染未经信任的用户输入
- 组件不会自行弹框预览文件，点击文件只抛出 `preview-request`，预览交互必须由宿主实现
- 自定义解析器返回的预览地址同样会被过滤，`javascript:` 协议地址不会生效
- 受保护下载地址（`/api/file/files/download`、`/file/files/download`、`/api/file/files/preview-content`、`/file/files/preview-content` 及 `/api/file/local-objects/`、`/file/local-objects/` 前缀）的图片必须通过内容解析器以 Blob 方式加载
- 组件内部注册名是 `MangoRichTextViewer`，从 `@mango/common` 导入时名称为 `RichTextViewer`

## 概览

统一富文本内容的只读渲染、资源解析与安全过滤。

## 功能清单

支持 HTML 安全过滤、托管图片/附件元数据解析、受保护图片 Blob 加载、加载状态、空态占位和文件预览事件。

## 接入方式

从 `@mango/common` 导入，将编辑器产出的 HTML 字符串传入 `content`；需要文件预览时监听 `preview-request`。

## 配置说明

通过 `content` 提供内容，通过 `resolveFile`、`resolveFileContent` 替换默认的文件元数据与二进制解析逻辑，通过 `emptyText` 配置空态。

## API 与扩展

提供 `resolve-error`、`preview-request` 两个事件和两个资源解析 Props；无插槽和实例方法。

## 数据与初始化

无数据库或字典初始化要求；托管文件通过上传接口按 ID 实时解析，组件不做本地持久化。

## 管理入口

无独立管理入口，作为详情描述、详情页正文等场景的子组件接入（`MangoDescriptionList` 的富文本预览默认使用该组件）。

## 快速开始

参见渲染静态富文本示例，传入 HTML 即可；文中含托管文件时参考预览事件示例补充宿主预览逻辑。

## 问题排查

文件点击无反应时确认宿主已监听 `preview-request`；图片不显示先通过 `resolve-error` 查看解析失败原因，并检查元数据是否返回了可用预览地址或内容解析器是否返回了有效 Blob；脚本未执行、事件属性丢失属于安全过滤的预期行为；内容区显示空态文本表示 HTML 无任何文本或托管文件节点。

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
