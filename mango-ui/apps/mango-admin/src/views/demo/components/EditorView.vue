<template>
  <DemoDocLayout
    class="editor-view"
    title="富文本编辑器"
    subtitle="基于 WangEditor 的富文本编辑组件，支持完整工具栏、简洁工具栏、只读状态和内容方法调用。"
    content-box
    :toc-items="tocItems"
  >
    <section id="basic" class="doc-section">
      <h2>基础用法</h2>
      <p>使用 v-model 绑定 HTML 内容，适合公告、协议、文章正文等富文本录入场景。</p>
      <div class="demo-block" data-testid="editor-demo">
        <div class="demo-source">
          <Editor v-model="basicContent" height="260px" />
          <div class="result-note">当前 HTML 长度：{{ basicContent.length }} 字符</div>
        </div>
        <div class="op-btns" @click="toggleCode('basic')">
          <el-icon><component :is="codeVisible.basic ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.basic ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.basic" :code="basicCode" />
      </div>
    </section>

    <section id="simple" class="doc-section">
      <h2>简洁模式</h2>
      <p>mode="simple" 保留常用编辑能力，适合评论、备注、审批意见等轻量输入。</p>
      <div class="demo-block">
        <div class="demo-source">
          <Editor v-model="simpleContent" mode="simple" height="220px" placeholder="请输入备注内容" />
        </div>
        <div class="op-btns" @click="toggleCode('simple')">
          <el-icon><component :is="codeVisible.simple ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.simple ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.simple" :code="simpleCode" />
      </div>
    </section>

    <section id="readonly" class="doc-section">
      <h2>只读状态</h2>
      <p>disabled 用于详情页或审批查看场景，保留内容展示但禁止编辑。</p>
      <div class="demo-block">
        <div class="demo-source">
          <Editor v-model="readonlyContent" height="200px" disabled />
        </div>
        <div class="op-btns" @click="toggleCode('readonly')">
          <el-icon><component :is="codeVisible.readonly ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.readonly ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.readonly" :code="readonlyCode" />
      </div>
    </section>

    <section id="methods" class="doc-section">
      <h2>方法调用</h2>
      <p>通过 ref 可以读取纯文本、读取 HTML、设置内容或清空编辑器。</p>
      <div class="demo-block">
        <div class="demo-source">
          <Editor ref="editorRef" v-model="methodContent" mode="simple" height="220px" />
          <div class="demo-actions">
            <el-button type="primary" @click="showHtml">读取 HTML</el-button>
            <el-button @click="showText">读取纯文本</el-button>
            <el-button @click="setSampleContent">写入示例</el-button>
            <el-button type="warning" @click="clearContent">清空</el-button>
          </div>
        </div>
        <div class="op-btns" @click="toggleCode('methods')">
          <el-icon><component :is="codeVisible.methods ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.methods ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.methods" :code="methodsCode" />
      </div>
    </section>

    <section id="props" class="doc-section api-section">
      <h2>支持属性</h2>
      <el-table :data="propsTable" size="small" border>
        <el-table-column prop="name" label="属性名" width="150" />
        <el-table-column prop="description" label="说明" min-width="260" />
        <el-table-column prop="type" label="类型" min-width="220" />
        <el-table-column prop="defaultValue" label="默认值" width="130" />
      </el-table>
    </section>

    <section id="slots" class="doc-section api-section">
      <h2>支持插槽</h2>
      <el-table :data="slotsTable" size="small" border>
        <el-table-column prop="name" label="插槽名" width="150" />
        <el-table-column prop="description" label="说明" min-width="260" />
        <el-table-column prop="scope" label="作用域参数" min-width="180" />
      </el-table>
    </section>

    <section id="events" class="doc-section api-section">
      <h2>支持方法 / 事件</h2>
      <el-table :data="eventsTable" size="small" border>
        <el-table-column prop="name" label="名称" width="160" />
        <el-table-column prop="description" label="说明" min-width="260" />
        <el-table-column prop="payload" label="参数 / 返回" min-width="240" />
      </el-table>
    </section>

    <section id="value" class="doc-section api-section">
      <h2>返回字段</h2>
      <el-table :data="valueTable" size="small" border>
        <el-table-column prop="field" label="字段" width="160" />
        <el-table-column prop="type" label="类型" min-width="180" />
        <el-table-column prop="description" label="说明" min-width="280" />
      </el-table>
    </section>
  </DemoDocLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { ArrowDown, ArrowUp } from '@element-plus/icons-vue';
import { Editor } from '@mango/common';
import DemoCodeBlock from './DemoCodeBlock.vue';
import DemoDocLayout from './DemoDocLayout.vue';

const tocItems = [
  { id: 'basic', label: '基础用法' },
  { id: 'simple', label: '简洁模式' },
  { id: 'readonly', label: '只读状态' },
  { id: 'methods', label: '方法调用' },
  { id: 'props', label: '支持属性' },
  { id: 'slots', label: '支持插槽' },
  { id: 'events', label: '支持方法 / 事件' },
  { id: 'value', label: '返回字段' },
];

const editorRef = ref<InstanceType<typeof Editor>>();
const basicContent = ref('<p>这是一个 <strong>富文本编辑器</strong> 示例。</p><p>支持加粗、斜体、链接、图片等常用编辑能力。</p>');
const simpleContent = ref('<p>简洁模式适合备注、评论等轻量内容。</p>');
const readonlyContent = ref('<p><strong>审批说明：</strong>当前内容为只读展示，不能修改。</p>');
const methodContent = ref('<p>通过 ref 调用组件暴露的方法。</p>');
const codeVisible = ref<Record<string, boolean>>({
  basic: false,
  simple: false,
  readonly: false,
  methods: false,
});

const basicCode = `<Editor
  v-model="content"
  height="260px"
/>`;

const simpleCode = `<Editor
  v-model="remark"
  mode="simple"
  height="220px"
  placeholder="请输入备注内容"
/>`;

const readonlyCode = `<Editor
  v-model="detail"
  height="200px"
  disabled
/>`;

const methodsCode = `<template>
  <Editor ref="editorRef" v-model="content" mode="simple" />
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { Editor } from '@mango/common';

const editorRef = ref<InstanceType<typeof Editor>>();
const content = ref('<p>示例内容</p>');

function readText() {
  return editorRef.value?.getText();
}

function clear() {
  editorRef.value?.clear();
}
<\/script>`;

const propsTable = [
  { name: 'v-model', description: '编辑器 HTML 内容，表单提交时通常直接提交该字段', type: 'string', defaultValue: "''" },
  { name: 'placeholder', description: '输入占位文本', type: 'string', defaultValue: '请输入内容...' },
  { name: 'height', description: '编辑区域高度，支持数字或 CSS 长度', type: 'number | string', defaultValue: '300' },
  { name: 'disabled', description: '是否禁用编辑器', type: 'boolean', defaultValue: 'false' },
  { name: 'mode', description: '工具栏模式，default 为完整模式，simple 为简洁模式', type: "'default' | 'simple'", defaultValue: 'default' },
];

const slotsTable = [
  { name: '-', description: '当前组件不提供业务插槽，内容通过 v-model 传入和回写', scope: '-' },
];

const eventsTable = [
  { name: 'update:modelValue', description: 'HTML 内容变化时触发，用于 v-model 双向绑定', payload: 'string' },
  { name: 'change', description: 'HTML 内容变化时触发', payload: 'string' },
  { name: 'getEditor', description: '暴露方法，获取 WangEditor 实例', payload: '() => EditorInstance' },
  { name: 'getText', description: '暴露方法，获取纯文本内容', payload: '() => string' },
  { name: 'getHtml', description: '暴露方法，获取 HTML 内容', payload: '() => string' },
  { name: 'setContent', description: '暴露方法，写入 HTML 内容', payload: '(content: string) => void' },
  { name: 'clear', description: '暴露方法，清空编辑器内容', payload: '() => void' },
];

const valueTable = [
  { field: 'v-model', type: 'string', description: '返回完整 HTML 字符串，例如 <p>正文</p>，适合直接保存为富文本字段' },
  { field: 'getText()', type: 'string', description: '返回去除 HTML 标签后的纯文本，适合摘要、字数统计或检索字段' },
  { field: 'getHtml()', type: 'string', description: '返回当前 HTML 内容，和 v-model 当前值保持一致' },
];

function toggleCode(key: string) {
  codeVisible.value[key] = !codeVisible.value[key];
}

function showHtml() {
  ElMessage.success(`HTML 长度：${editorRef.value?.getHtml().length ?? 0} 字符`);
}

function showText() {
  ElMessage.info(editorRef.value?.getText() || '暂无纯文本内容');
}

function setSampleContent() {
  editorRef.value?.setContent('<p><strong>已写入：</strong>这是一段通过 setContent 设置的内容。</p>');
}

function clearContent() {
  editorRef.value?.clear();
}
</script>

<style scoped lang="scss">
@use './demo-page.scss';

.result-note {
  margin-top: 12px;
  color: var(--el-text-color-secondary);
}

.demo-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}
</style>
