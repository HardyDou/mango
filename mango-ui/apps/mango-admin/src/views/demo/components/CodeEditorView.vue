<template>
  <DemoDocLayout
    class="code-editor-view"
    title="代码编辑器"
    subtitle="基于 CodeMirror 的代码编辑组件，支持语言模式、主题、只读状态、行号和编辑器方法调用。"
    content-box
    :toc-items="tocItems"
  >
    <section id="basic" class="doc-section">
      <h2>基础用法</h2>
      <p>使用 v-model 绑定代码内容，language 控制语法模式，适合规则脚本、SQL、JSON 配置等输入场景。</p>
      <div class="demo-block" data-testid="code-editor-demo">
        <div class="demo-source">
          <CodeEditor v-model="basicCodeValue" language="javascript" height="260px" @change="handleChange" />
          <div class="result-note">当前代码长度：{{ basicCodeValue.length }} 字符</div>
        </div>
        <div class="op-btns" @click="toggleCode('basic')">
          <el-icon><component :is="codeVisible.basic ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.basic ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.basic" :code="basicUsageCode" />
      </div>
    </section>

    <section id="language" class="doc-section">
      <h2>语言与主题</h2>
      <p>通过 language 和 theme 切换语法高亮与编辑器外观，主题可选 default、material-darker、material-ocean。</p>
      <div class="demo-block">
        <div class="demo-source">
          <div class="demo-controls">
            <el-select v-model="currentLanguage" class="demo-control-narrow" placeholder="选择语言">
              <el-option v-for="item in languageOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-select v-model="currentTheme" class="demo-control-medium" placeholder="选择主题">
              <el-option label="默认主题" value="default" />
              <el-option label="Material Darker" value="material-darker" />
              <el-option label="Material Ocean" value="material-ocean" />
            </el-select>
          </div>
          <CodeEditor
            v-model="languageCodeValue"
            :language="currentLanguage"
            :theme="currentTheme"
            width="100%"
            height="280px"
          />
        </div>
        <div class="op-btns" @click="toggleCode('language')">
          <el-icon><component :is="codeVisible.language ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.language ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.language" :code="languageUsageCode" />
      </div>
    </section>

    <section id="readonly" class="doc-section">
      <h2>只读状态</h2>
      <p>readonly 适合详情页展示脚本、配置或生成后的代码片段。</p>
      <div class="demo-block">
        <div class="demo-source">
          <CodeEditor v-model="readonlyCodeValue" language="json" height="220px" readonly />
        </div>
        <div class="op-btns" @click="toggleCode('readonly')">
          <el-icon><component :is="codeVisible.readonly ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.readonly ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.readonly" :code="readonlyUsageCode" />
      </div>
    </section>

    <section id="methods" class="doc-section">
      <h2>方法调用</h2>
      <p>通过 ref 可以读取、写入、清空、刷新或聚焦编辑器。</p>
      <div class="demo-block">
        <div class="demo-source">
          <CodeEditor ref="editorRef" v-model="methodCodeValue" language="sql" height="220px" />
          <div class="demo-actions">
            <el-button type="primary" @click="showValue">读取内容</el-button>
            <el-button @click="setSql">写入 SQL</el-button>
            <el-button @click="focusEditor">聚焦</el-button>
            <el-button type="warning" @click="clearValue">清空</el-button>
          </div>
        </div>
        <div class="op-btns" @click="toggleCode('methods')">
          <el-icon><component :is="codeVisible.methods ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.methods ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.methods" :code="methodsUsageCode" />
      </div>
    </section>

    <section id="props" class="doc-section api-section">
      <h2>支持属性</h2>
      <el-table :data="propsTable" size="small" border>
        <el-table-column prop="name" label="属性名" width="170" />
        <el-table-column prop="description" label="说明" min-width="260" />
        <el-table-column prop="type" label="类型" min-width="240" />
        <el-table-column prop="defaultValue" label="默认值" width="150" />
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
        <el-table-column prop="name" label="名称" width="170" />
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
import { CodeEditor } from '@mango/common';
import DemoCodeBlock from './DemoCodeBlock.vue';
import DemoDocLayout from './DemoDocLayout.vue';

const tocItems = [
  { id: 'basic', label: '基础用法' },
  { id: 'language', label: '语言与主题' },
  { id: 'readonly', label: '只读状态' },
  { id: 'methods', label: '方法调用' },
  { id: 'props', label: '支持属性' },
  { id: 'slots', label: '支持插槽' },
  { id: 'events', label: '支持方法 / 事件' },
  { id: 'value', label: '返回字段' },
];

const editorRef = ref<InstanceType<typeof CodeEditor>>();
const basicCodeValue = ref(`function hello(name) {
  return \`Hello, \${name}\`;
}

console.log(hello('Mango'));`);
const languageCodeValue = ref(`{
  "name": "mango-admin",
  "component": "CodeEditor",
  "enabled": true
}`);
const readonlyCodeValue = ref(`{
  "status": "readonly",
  "message": "该内容仅用于展示"
}`);
const methodCodeValue = ref('select id, username, created_time from sys_user where status = 1;');
const currentLanguage = ref('json');
const currentTheme = ref<'default' | 'material-darker' | 'material-ocean'>('default');
const codeVisible = ref<Record<string, boolean>>({
  basic: false,
  language: false,
  readonly: false,
  methods: false,
});

const languageOptions = [
  { label: 'JavaScript', value: 'javascript' },
  { label: 'HTML', value: 'html' },
  { label: 'CSS', value: 'css' },
  { label: 'Python', value: 'python' },
  { label: 'Java', value: 'java' },
  { label: 'SQL', value: 'sql' },
  { label: 'JSON', value: 'json' },
  { label: 'Markdown', value: 'markdown' },
  { label: 'Go', value: 'go' },
  { label: 'Rust', value: 'rust' },
];

const basicUsageCode = `<CodeEditor
  v-model="code"
  language="javascript"
  height="260px"
  @change="handleChange"
/>`;

const languageUsageCode = `<CodeEditor
  v-model="code"
  language="json"
  theme="material-ocean"
  width="100%"
  height="280px"
/>`;

const readonlyUsageCode = `<CodeEditor
  v-model="jsonText"
  language="json"
  height="220px"
  readonly
/>`;

const methodsUsageCode = `<template>
  <CodeEditor ref="editorRef" v-model="sql" language="sql" />
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { CodeEditor } from '@mango/common';

const editorRef = ref<InstanceType<typeof CodeEditor>>();
const sql = ref('select * from sys_user;');

function getValue() {
  return editorRef.value?.getValue();
}

function clear() {
  editorRef.value?.clear();
}
<\/script>`;

const propsTable = [
  { name: 'v-model', description: '编辑器文本内容', type: 'string', defaultValue: "''" },
  { name: 'language', description: '语法高亮语言，支持常见脚本、标记语言和配置格式', type: 'string', defaultValue: 'javascript' },
  { name: 'theme', description: '编辑器主题', type: "'default' | 'material-darker' | 'material-ocean'", defaultValue: 'default' },
  { name: 'readonly', description: '是否只读', type: 'boolean', defaultValue: 'false' },
  { name: 'lineNumbers', description: '是否显示行号', type: 'boolean', defaultValue: 'true' },
  { name: 'matchBrackets', description: '是否启用括号匹配', type: 'boolean', defaultValue: 'true' },
  { name: 'autoCloseBrackets', description: '是否自动闭合括号', type: 'boolean', defaultValue: 'true' },
  { name: 'height', description: '编辑器高度，传入 CSS 长度', type: 'string', defaultValue: '300px' },
  { name: 'width', description: '编辑器宽度，默认自适应父容器', type: 'string', defaultValue: '100%' },
];

const slotsTable = [
  { name: '-', description: '当前组件不提供业务插槽，内容通过 v-model 传入和回写', scope: '-' },
];

const eventsTable = [
  { name: 'update:modelValue', description: '代码内容变化时触发，用于 v-model 双向绑定', payload: 'string' },
  { name: 'change', description: '代码内容变化时触发', payload: 'string' },
  { name: 'getEditor', description: '暴露方法，获取 CodeMirror 实例', payload: '() => CodeMirror.Editor' },
  { name: 'getValue', description: '暴露方法，获取当前代码内容', payload: '() => string' },
  { name: 'setValue', description: '暴露方法，写入代码内容', payload: '(value: string) => void' },
  { name: 'clear', description: '暴露方法，清空代码内容', payload: '() => void' },
  { name: 'refresh', description: '暴露方法，刷新编辑器布局', payload: '() => void' },
  { name: 'focus', description: '暴露方法，聚焦编辑器', payload: '() => void' },
];

const valueTable = [
  { field: 'v-model', type: 'string', description: '返回当前编辑器完整文本内容，可直接作为脚本、SQL、JSON 或配置字段提交' },
  { field: 'getValue()', type: 'string', description: '返回当前编辑器完整文本内容，适合按钮触发保存或校验时读取' },
  { field: 'change', type: 'string', description: '每次内容变化回传最新文本，适合实时校验、预览或状态标记' },
];

function toggleCode(key: string) {
  codeVisible.value[key] = !codeVisible.value[key];
}

function handleChange(value: string) {
  basicCodeValue.value = value;
}

function showValue() {
  ElMessage.success(`代码长度：${editorRef.value?.getValue().length ?? 0} 字符`);
}

function setSql() {
  editorRef.value?.setValue('select id, username, nickname from sys_user order by created_time desc;');
}

function focusEditor() {
  editorRef.value?.focus();
}

function clearValue() {
  editorRef.value?.clear();
}
</script>

<style scoped lang="scss">
@use './demo-page.scss';

.demo-controls,
.demo-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}

.demo-actions {
  margin-top: 14px;
  margin-bottom: 0;
}

.result-note {
  margin-top: 12px;
  color: var(--el-text-color-secondary);
}
</style>
