<template>
  <div class="code-editor-view-container">
    <h1>代码编辑器</h1>
    <p class="subtitle">
      基于 CodeMirror 的代码编辑组件，支持多语言语法高亮、行号显示、括号匹配等特性
    </p>

    <el-card class="demo-card">
      <template #header>
        <div class="card-header">
          <span>代码编辑器 (CodeEditor)</span>
          <div class="controls">
            <el-select
              v-model="currentLanguage"
              size="small"
              placeholder="选择语言"
              style="width: 140px"
            >
              <el-option
                label="JavaScript"
                value="javascript"
              />
              <el-option
                label="HTML"
                value="html"
              />
              <el-option
                label="CSS"
                value="css"
              />
              <el-option
                label="Python"
                value="python"
              />
              <el-option
                label="Java"
                value="java"
              />
              <el-option
                label="SQL"
                value="sql"
              />
              <el-option
                label="JSON"
                value="json"
              />
              <el-option
                label="Markdown"
                value="markdown"
              />
              <el-option
                label="Go"
                value="go"
              />
              <el-option
                label="Rust"
                value="rust"
              />
            </el-select>
            <el-select
              v-model="currentTheme"
              size="small"
              placeholder="选择主题"
              style="width: 140px"
            >
              <el-option
                label="默认主题"
                value="default"
              />
              <el-option
                label="Material Dark"
                value="material-darker"
              />
              <el-option
                label="Material Ocean"
                value="material-ocean"
              />
            </el-select>
          </div>
        </div>
      </template>
      <div
        class="editor-demo"
        data-testid="code-editor-demo"
      >
        <CodeEditor
          ref="editorRef"
          :key="currentLanguage"
          v-model="codeContent"
          :language="currentLanguage"
          :theme="currentTheme"
          :height="editorHeightValue"
          :readonly="isReadonly"
          @change="onEditorChange"
        />
      </div>
      <div class="editor-toolbar">
        <span class="label">高度：</span>
        <el-slider
          v-model="editorHeight"
          :min="200"
          :max="600"
          :step="50"
          style="width: 150px"
        />
        <span class="height-value">{{ editorHeight }}px</span>
        <el-checkbox
          v-model="isReadonly"
          style="margin-left: 16px"
        >
          只读模式
        </el-checkbox>
        <el-button
          type="primary"
          size="small"
          @click="getValue"
        >
          获取内容
        </el-button>
        <el-button
          type="warning"
          size="small"
          @click="clearContent"
        >
          清空
        </el-button>
        <el-button
          type="success"
          size="small"
          @click="testInput"
        >
          测试输入
        </el-button>
      </div>
    </el-card>

    <!-- 功能特性 -->
    <el-card
      class="demo-card"
      style="margin-top: 20px"
    >
      <template #header>
        <span>功能特性</span>
      </template>
      <div class="feature-list">
        <el-tag type="success">
          多语言支持
        </el-tag>
        <el-tag type="success">
          语法高亮
        </el-tag>
        <el-tag type="success">
          行号显示
        </el-tag>
        <el-tag type="success">
          括号匹配
        </el-tag>
        <el-tag type="success">
          自动闭合
        </el-tag>
        <el-tag type="success">
          主题切换
        </el-tag>
        <el-tag type="info">
          v-model 绑定
        </el-tag>
        <el-tag type="info">
          只读模式
        </el-tag>
        <el-tag type="info">
          高度自适应
        </el-tag>
      </div>
    </el-card>

    <!-- 支持的语言 -->
    <el-card
      class="demo-card"
      style="margin-top: 20px"
    >
      <template #header>
        <span>支持的语言</span>
      </template>
      <el-row :gutter="16">
        <el-col :span="6">
          <div class="lang-item">
            <el-tag>JavaScript</el-tag>
            <span>js / javascript</span>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="lang-item">
            <el-tag>HTML</el-tag>
            <span>html</span>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="lang-item">
            <el-tag>CSS</el-tag>
            <span>css</span>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="lang-item">
            <el-tag>Python</el-tag>
            <span>python</span>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="lang-item">
            <el-tag>Java</el-tag>
            <span>java</span>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="lang-item">
            <el-tag>SQL</el-tag>
            <span>sql</span>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="lang-item">
            <el-tag>JSON</el-tag>
            <span>json</span>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="lang-item">
            <el-tag>Markdown</el-tag>
            <span>markdown</span>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 使用方法 -->
    <el-card
      class="demo-card usage-card"
      style="margin-top: 20px"
    >
      <template #header>
        <span>使用方法</span>
      </template>
      <el-tabs>
        <el-tab-pane label="基础用法">
          <div class="code-block">
            <pre><code>&lt;template&gt;
  &lt;CodeEditor v-model="code" /&gt;
&lt;/template&gt;

&lt;script setup&gt;
import { ref } from 'vue';
import { CodeEditor } from '@mango/common';

const code = ref('console.log("Hello World");');
&lt;/script&gt;</code></pre>
          </div>
        </el-tab-pane>
        <el-tab-pane label="指定语言">
          <div class="code-block">
            <pre><code>&lt;template&gt;
  &lt;CodeEditor v-model="code" language="python" /&gt;
&lt;/template&gt;</code></pre>
          </div>
        </el-tab-pane>
        <el-tab-pane label="自定义主题">
          <div class="code-block">
            <pre><code>&lt;template&gt;
  &lt;CodeEditor
    v-model="code"
    theme="material-darker"
  /&gt;
&lt;/template&gt;

// 可选主题: default | material-darker | material-ocean</code></pre>
          </div>
        </el-tab-pane>
        <el-tab-pane label="只读模式">
          <div class="code-block">
            <pre><code>&lt;template&gt;
  &lt;CodeEditor v-model="code" :readonly="true" /&gt;
&lt;/template&gt;</code></pre>
          </div>
        </el-tab-pane>
        <el-tab-pane label="高度自适应">
          <div class="code-block">
            <pre><code>&lt;template&gt;
  &lt;CodeEditor v-model="code" height="500px" /&gt;
&lt;/template&gt;</code></pre>
          </div>
        </el-tab-pane>
        <el-tab-pane label="监听变化">
          <div class="code-block">
            <pre><code>&lt;template&gt;
  &lt;CodeEditor v-model="code" @change="handleChange" /&gt;
&lt;/template&gt;

&lt;script setup&gt;
const handleChange = (value) => {
  console.log('代码变更:', value);
};
&lt;/script&gt;</code></pre>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { CodeEditor } from '@mango/common';

const editorRef = ref<InstanceType<typeof CodeEditor> | null>(null);

const codeContent = ref(`// 在这里输入代码
console.log('Hello!');`);

const currentLanguage = ref('javascript');
const currentTheme = ref('default');
const editorHeight = ref(400);
const isReadonly = ref(false);

const editorHeightValue = computed(() => `${editorHeight.value}px`);

function getValue() {
  ElMessage({
    message: `代码长度: ${codeContent.value.length} 字符`,
    type: 'success',
  });
}

function clearContent() {
  codeContent.value = '';
}

function onEditorChange(value: string) {
  console.log('Editor changed:', value.length, 'chars');
}

function testInput() {
  // 尝试通过 ref 调用 editor 的 focus 和 setValue 方法
  const editor = editorRef.value;
  if (editor) {
    editor.setValue(`// 测试代码\nconst now = Date.now();\nconsole.log('Test at:', now);\n`);
    editor.focus();
    ElMessage({ message: '已设置测试代码，请检查编辑器', type: 'success' });
  } else {
    ElMessage({ message: 'Editor ref not found', type: 'error' });
  }
}
</script>

<style scoped lang="scss">
.code-editor-view-container {
  padding: 20px;

  h1 {
    margin-bottom: 8px;
    font-size: 24px;
    font-weight: 600;
  }

  .subtitle {
    margin-bottom: 20px;
    color: #909399;
  }

  .demo-card {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .controls {
        display: flex;
        gap: 12px;
      }
    }

    .editor-toolbar {
      display: flex;
      align-items: center;
      gap: 16px;
      margin-top: 16px;
      padding-top: 16px;
      border-top: 1px solid #eee;

      .label {
        color: #606266;
        font-size: 14px;
      }

      .height-value {
        color: #409eff;
        font-size: 14px;
        min-width: 50px;
      }
    }

    .feature-list {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    .lang-item {
      display: flex;
      flex-direction: column;
      gap: 4px;
      padding: 8px;
      border: 1px solid #eee;
      border-radius: 4px;

      span {
        font-size: 12px;
        color: #909399;
      }
    }
  }

  .usage-card {
    :deep(.el-tabs__content) {
      max-height: 400px;
      overflow-y: auto;
    }
  }

  .code-block {
    background: #1e1e1e;
    border-radius: 4px;
    padding: 16px;
    overflow-x: auto;

    pre {
      margin: 0;
    }

    code {
      color: #d4d4d4;
      font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
      font-size: 13px;
      line-height: 1.5;
    }
  }
}
</style>
