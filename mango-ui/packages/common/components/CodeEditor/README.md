# CodeEditor

代码编辑组件。基于 CodeMirror 5 封装，支持多语言高亮、主题切换、括号匹配、自动闭合和只读模式，内容通过 `v-model` 双向绑定。

## 导入

```ts
import { CodeEditor } from '@mango/common';
```

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `modelValue` | `string` | `''` | 编辑器文本内容，支持 `v-model` |
| `language` | `string` | `'javascript'` | 语言模式，见下方语言映射表；未命中时回退为 `javascript` |
| `theme` | `'default' \| 'material-darker' \| 'material-ocean'` | `'default'` | 编辑器主题，对应 CodeMirror 同名主题 |
| `readonly` | `boolean` | `false` | 是否只读 |
| `lineNumbers` | `boolean` | `true` | 是否显示行号 |
| `matchBrackets` | `boolean` | `true` | 是否高亮匹配括号 |
| `autoCloseBrackets` | `boolean` | `true` | 是否自动闭合括号 |
| `height` | `string \| number` | `'300px'` | 容器/编辑器高度；数字按像素处理 |
| `width` | `string` | `'100%'` | 容器宽度 |

语言映射（`language` 取值 → CodeMirror mode）：

| 传入值 | mode | 传入值 | mode |
|--------|------|--------|------|
| `js` / `javascript` | `javascript` | `json` | `javascript`（`json: true`） |
| `xml` | `xml` | `html` | `htmlmixed` |
| `css` | `css` | `python` | `python` |
| `java` | `text/x-java` | `sql` | `sql` |
| `markdown` | `markdown` | `c` | `text/x-csrc` |
| `cpp` | `text/x-c++src` | `csharp` | `text/x-csharp` |
| `go` | `text/x-go` | `rust` | `text/x-rust` |

组件初始化时固定开启：当前行高亮（`styleActiveLine`）、自动换行（`lineWrapping`）、`Ctrl-/` 与 `Cmd-/` 切换注释。

## Events

| 名称 | 参数 | 说明 |
|------|------|------|
| `update:modelValue` | `value: string` | 编辑器内容变化时触发 |
| `change` | `value: string` | 与 `update:modelValue` 同载荷触发，供非 `v-model` 场景监听 |

## Expose

| 名称 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `getEditor()` | - | `CodeMirror.Editor \| null` | 获取底层 CodeMirror 编辑器实例 |
| `getValue()` | - | `string` | 获取当前内容，编辑器未初始化时返回 `''` |
| `setValue(value)` | `value: string` | - | 设置编辑器内容 |
| `clear()` | - | - | 清空内容 |
| `refresh()` | - | - | 刷新编辑器布局（尺寸变化后重新测量） |
| `focus()` | - | - | 聚焦编辑器 |

## 使用示例

### 基础用法

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { CodeEditor } from '@mango/common';

const code = ref('const hello = "mango";\nconsole.log(hello);');
</script>

<template>
  <CodeEditor v-model="code" language="javascript" :height="320" />
</template>
```

### JSON 编辑与只读预览

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { CodeEditor } from '@mango/common';

const jsonText = ref(JSON.stringify({ name: 'mango', enabled: true }, null, 2));
const theme = ref<'default' | 'material-darker' | 'material-ocean'>('material-darker');
const readonly = ref(false);
</script>

<template>
  <el-form label-width="80px">
    <el-form-item label="主题">
      <el-select v-model="theme" style="width: 200px">
        <el-option label="默认" value="default" />
        <el-option label="Material Darker" value="material-darker" />
        <el-option label="Material Ocean" value="material-ocean" />
      </el-select>
    </el-form-item>
  </el-form>

  <CodeEditor
    v-model="jsonText"
    language="json"
    :theme="theme"
    :readonly="readonly"
    height="400px"
  />
</template>
```

### 通过 ref 操作编辑器

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { CodeEditor } from '@mango/common';

const editorRef = ref<InstanceType<typeof CodeEditor> | null>(null);

function insertSnippet() {
  editorRef.value?.setValue('// 新内容\n');
  editorRef.value?.focus();
}

function readValue() {
  console.log(editorRef.value?.getValue());
}
</script>

<template>
  <CodeEditor ref="editorRef" language="sql" />
  <el-button @click="insertSnippet">填充模板</el-button>
  <el-button type="primary" @click="readValue">读取内容</el-button>
</template>
```

## 重要约束

- 底层为 CodeMirror 5（`codemirror` 包），通过动态 `import('codemirror')` 异步加载，编辑器在 `onMounted` 后的 `nextTick` 才初始化；立即调用 `getValue()` 可能拿到 `''`，需要实例时用 `getEditor()` 判空。
- 动态加载的 mode 仅包含 javascript、xml、css、htmlmixed、python、sql、markdown、clike（覆盖 java/c/cpp/csharp）；`go`、`rust` 虽然在语言映射表中，但组件未加载对应 mode 文件，开箱时没有语法高亮，需宿主项目自行引入 `codemirror/mode/go/go`、`codemirror/mode/rust/rust`。
- `language`、`theme`、`readonly`、`height`、`width` 变化会实时同步到编辑器；但 `lineNumbers`、`matchBrackets`、`autoCloseBrackets` 只在初始化时生效，运行时修改不会应用。
- 容器尺寸通过 `ResizeObserver` 自动 `refresh()`；在不支持 `ResizeObserver` 的环境或编辑器初始处于 `display: none` 容器中时，显示后需手动调用 `refresh()`。
- 外部写入与内部输入已做判等保护（`newValue !== editor.getValue()` 才 `setValue`），不会因光标事件重复重置内容。
- 组件不发起任何后端请求，内容的保存、校验由业务方处理。

## 概览

供配置、脚本、JSON、SQL 等文本场景使用的嵌入式代码编辑器。

## 功能清单

支持多语言语法高亮、三套主题、行号、括号匹配/自动闭合、当前行高亮、自动换行、注释快捷键、只读模式、外部值同步以及容器尺寸自适应。

## 接入方式

从 `@mango/common` 导入 `CodeEditor`，通过 `v-model` 绑定字符串，在需要代码录入或预览的表单、抽屉、弹框中使用。

## 配置说明

按 Props 表配置语言、主题、只读、行号和尺寸；固定编辑行为（换行、当前行高亮、注释快捷键）不可通过 Props 关闭，需要时可通过 `getEditor()` 操作底层实例。

## API 与扩展

提供 `update:modelValue`、`change` 事件及 `getEditor()`、`getValue()`、`setValue()`、`clear()`、`refresh()`、`focus()` 方法；需要更多 CodeMirror 能力（lint、hint、自定义 keymap）时通过 `getEditor()` 获取原生实例扩展。

## 数据与初始化

无数据库或默认数据初始化。CodeMirror 核心、主题 CSS 与各语言 mode 均在组件挂载后按需动态加载，多个实例共享同一个加载 Promise。

## 管理入口

无独立管理入口，由业务页面接入。

## 快速开始

放置 `<CodeEditor v-model="code" language="json" />` 即可；弹框/标签页等隐藏容器中首次显示异常时调用 ref 的 `refresh()`。

## 问题排查

- 初始内容读不到：编辑器异步初始化，等待挂载完成或监听 `v-model` 变化后再读取。
- Go/Rust 没有高亮：组件未打包这两个 mode，需在应用中额外引入对应 CodeMirror mode 文件。
- 隐藏后再显示编辑器排版错乱：调用 `refresh()` 强制重新测量。
- 修改 `lineNumbers` 等属性不生效：这些 Props 仅初始化时读取，需重建组件（如更换 `key`）。

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
