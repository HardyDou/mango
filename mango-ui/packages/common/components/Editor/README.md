# Editor

富文本编辑器组件，基于 wangEditor 封装，支持工具栏定制、图片/附件上传、粘贴与拖拽图片托管，默认以 `mango-file:` 文件令牌形式保存图片和附件。

## 导入

```ts
import { Editor } from '@mango/common';
import type { EditorAssetError, EditorImageError } from '@mango/common';
```

组件内部注册名（`defineOptions.name`）为 `MangoEditor`，但从 `@mango/common` 的导出名是 `Editor`。

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `modelValue` | `string` | `''` | HTML 内容，支持 `v-model`；`token` 模式下输出的是令牌化后的规范化 HTML |
| `placeholder` | `string` | `'请输入内容...'` | 空内容占位提示 |
| `height` | `number \| string` | `300` | 编辑区高度，数字按像素处理，字符串原样使用（通过 CSS 变量 `--editor-height` 生效） |
| `disabled` | `boolean` | `false` | 是否禁用；禁用后工具栏整行（含 `toolbar-actions` 插槽）不渲染 |
| `mode` | `'default' \| 'simple'` | `'default'` | 工具栏模式；`simple` 且未自定义 `toolbarKeys` 时使用 wangEditor 精简工具栏，切换模式会销毁并重建编辑器实例 |
| `toolbarKeys` | `EditorToolbarKey[]` | `[]` | 自定义工具栏；为空数组使用内置工具栏。元素可为字符串，或 `{ key, title?, iconSvg?, menuKeys? }` 对象 |
| `imageValueType` | `'url' \| 'id' \| 'token'` | `'token'` | 图片保存策略：`token` 保存 `mango-file:<文件ID>` 令牌；`id` 保存文件 ID；`url` 保存直链 |
| `pasteImageMode` | `'default' \| 'upload'` | `'upload'` | 粘贴图片策略；仅在 `token` 模式且为 `upload` 时拦截粘贴并上传图片/文件 |
| `attachmentAccept` | `string` | `''` | 工具栏「上传附件」选择框的 `accept` 限制，为空不限制类型 |

## Events

| 名称 | 参数 | 说明 |
|------|------|------|
| `update:modelValue` | `value: string` | 内容变化时触发（令牌模式下为序列化后的规范化 HTML） |
| `change` | `value: string` | 与 `update:modelValue` 同步触发 |
| `image-error` | `error: EditorImageError` | 图片相关失败（上传、预览、序列化）时触发，是 `asset-error` 的子集 |
| `asset-error` | `error: EditorAssetError` | 任意资源失败时触发，含图片与附件 |
| `uploading-change` | `uploading: boolean` | 是否存在进行中的上传任务（并发任务合并计数，全部结束才回到 `false`） |

## Expose

| 名称 | 参数 | 说明 |
|------|------|------|
| `getEditor()` | - | 获取底层 wangEditor `IDomEditor` 实例，可能为 `null` |
| `getText()` | - | 获取纯文本内容 |
| `getHtml()` | - | 获取规范 HTML；`token` 模式返回实时序列化后的令牌 HTML |
| `setContent(content)` | `content: string` | 设置内容：令牌模式会先规范化并补预览，随后同步 emit 新值 |
| `clear()` | - | 等价于 `setContent('')` |
| `blur()` | - | 让编辑器失焦并清除浏览器选区 |

## Slots

| 名称 | 说明 |
|------|------|
| `toolbar-actions` | 工具栏右侧的自定义操作区（如保存按钮）；非禁用且编辑器实例创建后才渲染 |

## 类型定义

```ts
import type { FileId } from '../../api/upload';

export type EditorAssetErrorSource = 'upload' | 'paste' | 'drop' | 'preview' | 'serialize';
export type EditorAssetKind = 'image' | 'attachment';

export interface EditorAssetError {
  code: string;
  message: string;
  source: EditorAssetErrorSource;
  fileId?: FileId;
  fileName?: string;
  kind?: EditorAssetKind;
}

/** 图片错误与资源错误结构一致 */
export type EditorImageError = EditorAssetError;
```

`imageValueType`、`pasteImageMode`、`mode` 的联合类型以及 `EditorToolbarKey` 在 `index.vue` 内部定义；`FileId` 来自 Common 包的文件上传 API。

可能出现的错误码（`code`）：

| code | 触发场景 |
|------|----------|
| `EDITOR_IMAGE_UPLOAD_FAILED` | 工具栏图片上传失败 |
| `EDITOR_ASSET_ID_MISSING` | 上传结果未返回文件 ID（令牌模式必需） |
| `EDITOR_INVALID_MANAGED_ASSET` | 保存内容中存在无法解析的托管资源，已被移除 |
| `EDITOR_ASSET_PREVIEW_UNAVAILABLE` / `EDITOR_IMAGE_PREVIEW_UNAVAILABLE` | 回显时资源详情或预览地址不可用 |
| `EDITOR_ASSET_UPLOAD_FAILED` | 粘贴、拖拽或附件上传存在失败文件 |
| `EDITOR_PASTE_ASSET_LIMIT_EXCEEDED` | 单次粘贴/拖拽超过 10 个文件 |

## 使用示例

### 基础用法

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { Editor } from '@mango/common';

const content = ref('');
</script>

<template>
  <Editor v-model="content" :height="360" placeholder="请输入通知正文" />
</template>
```

### 令牌模式下监听上传状态和错误（默认策略）

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { Editor } from '@mango/common';
import type { EditorAssetError } from '@mango/common';
import { ElMessage } from 'element-plus';

const content = ref('');
const uploading = ref(false);

function handleAssetError(error: EditorAssetError) {
  ElMessage.warning(`${error.code}：${error.message}`);
}

function handleSave() {
  // content 中的图片/附件已是 mango-file:<id> 令牌，可直接提交后端
  console.log(content.value);
}
</script>

<template>
  <Editor
    v-model="content"
    @uploading-change="uploading = $event"
    @asset-error="handleAssetError"
  >
    <template #toolbar-actions>
      <el-button type="primary" :loading="uploading" @click="handleSave">
        保存
      </el-button>
    </template>
  </Editor>
</template>
```

### 精简模式只读回显与实例方法

```vue
<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { Editor } from '@mango/common';

const editorRef = ref<InstanceType<typeof Editor>>();
const content = ref('');

onMounted(() => {
  editorRef.value?.setContent('<p>初始化内容</p>');
});

function inspect() {
  console.log(editorRef.value?.getHtml());
  console.log(editorRef.value?.getText());
}
</script>

<template>
  <Editor
    ref="editorRef"
    v-model="content"
    mode="simple"
    :toolbar-keys="['bold', 'italic', '|', 'insertLink']"
    attachment-accept=".pdf,.doc,.docx"
  />
  <el-button @click="inspect">查看内容</el-button>
</template>
```

## 重要约束

- `imageValueType` 默认 `token`：编辑回显和 `v-model` 输出中的图片 `src`、附件 `href` 均为 `mango-file:<正整数文件ID>` 令牌，并带有 `data-file-id`、`data-file-kind` 属性；后端需按令牌关联文件，不要把临时预览 URL 当作持久值保存
- 令牌模式下回显会按令牌收集文件 ID，逐个调用文件详情接口换取预览地址；取不到预览地址的资源标记为 `data-managed-state="failed"` 并显示「N 个资源暂不可预览」提示
- 上传结果必须返回文件 ID；令牌模式缺失 ID 会报 `EDITOR_ASSET_ID_MISSING` 且内容不会插入。`id` 模式缺 ID 时回退为 URL 并输出 console 警告，`url` 模式直接使用返回的 URL
- 粘贴托管仅在 `imageValueType="token"` 且 `pasteImageMode="upload"` 且非禁用时生效：`data:image/`、`blob:` 剪贴板文件上传，`http(s)://` 图片走远程图片导入，剪贴板中的非图片文件作为附件链接追加；单次最多处理 10 个文件、单张内联图片最大 10 MB、上传并发 3
- 拖拽文件在 `token` 模式下即被拦截上传（与 `pasteImageMode` 无关）；工具栏额外提供「上传附件」菜单，自定义 `toolbarKeys` 时该菜单会被自动插入到 `uploadImage` 之后（已显式配置则不重复插入）
- 工具栏附件菜单和拖放插件通过 wangEditor 的 `Boot.registerMenu` / `Boot.registerPlugin` 全局注册，带幂等保护，属于模块级副作用
- 切换 `mode` 会销毁当前编辑器实例；组件卸载时也会销毁实例并中止上传态通知，宿主应避免长期持有旧的 `getEditor()` 返回值
- wangEditor 全屏容器层级固定为 `z-index: 3000`

## 概览

面向管理端的富文本录入组件，封装 wangEditor 工具栏、图片/附件上传、粘贴拖拽托管和令牌化存取。

## 功能清单

支持 v-model、默认/精简/自定义工具栏、图片与附件上传、远程图片导入、粘贴与拖拽处理、上传状态提示、资源错误事件、令牌化序列化与回显预览，以及编辑器实例方法透出。

## 接入方式

从 `@mango/common` 导入 `Editor`；依赖 `@wangeditor/editor`、`@wangeditor/editor-for-vue`（Common 包已声明）和 Common 包的文件上传/详情接口与登录态。

## 配置说明

通过 `height`、`mode`、`toolbarKeys`、`placeholder`、`disabled` 调整外观与能力，通过 `imageValueType`、`pasteImageMode`、`attachmentAccept` 决定资源存取策略。

## API 与扩展

提供 `update:modelValue`、`change`、`image-error`、`asset-error`、`uploading-change` 事件，暴露 `getEditor()`、`getText()`、`getHtml()`、`setContent()`、`clear()`、`blur()`，并提供 `toolbar-actions` 插槽扩展工具栏。

## 数据与初始化

无本地数据初始化；初始 HTML 通过 `modelValue` 传入，令牌模式下挂载与值变更时会自动规范化并请求文件详情补预览。

## 管理入口

无独立管理入口，由业务表单页面接入。

## 快速开始

参见「基础用法」；需要保存按钮和失败提示参见「令牌模式下监听上传状态和错误」。

## 问题排查

- 保存后图片无法打开：确认后端保存的是 `mango-file:` 令牌而非编辑期的临时预览 URL
- 粘贴图片没有上传：仅 `token` + `pasteImageMode="upload"` 生效，且编辑器不能处于禁用状态
- 提示资源暂不可预览：文件详情接口未返回可用的 `previewUrl`/`url`，错误事件中带 `fileId`，可据此排查文件状态
- 自定义工具栏找不到上传附件：附件菜单会自动插在 `uploadImage` 之后，若工具栏不含 `uploadImage` 则追加到末尾
- 上传中按钮无Loading：监听 `uploading-change`，多个并发上传会合并为一次 `true → false`

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
