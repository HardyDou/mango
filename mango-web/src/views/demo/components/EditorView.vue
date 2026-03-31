<template>
  <div class="editor-view-container">
    <h1>富文本编辑器</h1>
    <p class="subtitle">
      基于 WangEditor 的富文本编辑组件，支持富文本编辑、图片上传、视频嵌入等功能
    </p>

    <el-card class="demo-card">
      <template #header>
        <div class="card-header">
          <span>富文本编辑器 (Editor)</span>
          <el-radio-group
            v-model="currentMode"
            size="small"
          >
            <el-radio-button label="default">
              完整模式
            </el-radio-button>
            <el-radio-button label="simple">
              简洁模式
            </el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div
        class="editor-demo"
        data-testid="editor-demo"
      >
        <Editor
          :key="editorModeKey"
          v-model="editorContent"
          :height="editorHeightValue"
          :mode="currentMode"
          :disabled="isDisabled"
        />
      </div>
      <div class="editor-toolbar">
        <span class="label">高度调整：</span>
        <el-slider
          v-model="editorHeight"
          :min="200"
          :max="600"
          :step="50"
          style="width: 200px"
        />
        <span class="height-value">{{ editorHeight }}px</span>
        <el-button
          type="primary"
          size="small"
          @click="getContent"
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
          type="info"
          size="small"
          @click="toggleDisabled"
        >
          {{ isDisabled ? '启用' : '禁用' }}
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
          富文本格式
        </el-tag>
        <el-tag type="success">
          图片上传
        </el-tag>
        <el-tag type="success">
          视频嵌入
        </el-tag>
        <el-tag type="success">
          链接插入
        </el-tag>
        <el-tag type="success">
          代码块
        </el-tag>
        <el-tag type="success">
          全屏编辑
        </el-tag>
        <el-tag type="info">
          v-model 双向绑定
        </el-tag>
        <el-tag type="info">
          高度自适应
        </el-tag>
        <el-tag type="info">
          模式切换
        </el-tag>
      </div>
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
  &lt;Editor v-model="content" /&gt;
&lt;/template&gt;

&lt;script setup&gt;
import { ref } from 'vue';
import Editor from '@/components/Editor/index.vue';

const content = ref('&lt;p&gt;初始内容&lt;/p&gt;');
&lt;/script&gt;</code></pre>
          </div>
        </el-tab-pane>
        <el-tab-pane label="自定义高度">
          <div class="code-block">
            <pre><code>&lt;template&gt;
  &lt;Editor v-model="content" :height="500" /&gt;
&lt;/template&gt;</code></pre>
          </div>
        </el-tab-pane>
        <el-tab-pane label="简洁模式">
          <div class="code-block">
            <pre><code>&lt;template&gt;
  &lt;Editor v-model="content" mode="simple" /&gt;
&lt;/template&gt;</code></pre>
          </div>
        </el-tab-pane>
        <el-tab-pane label="禁用状态">
          <div class="code-block">
            <pre><code>&lt;template&gt;
  &lt;Editor v-model="content" :disabled="true" /&gt;
&lt;/template&gt;</code></pre>
          </div>
        </el-tab-pane>
        <el-tab-pane label="监听变化">
          <div class="code-block">
            <pre><code>&lt;template&gt;
  &lt;Editor v-model="content" @change="handleChange" /&gt;
&lt;/template&gt;

&lt;script setup&gt;
const handleChange = (html) => {
  console.log('内容变更:', html);
};
&lt;/script&gt;</code></pre>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 内容预览 -->
    <el-card
      v-if="editorContent"
      class="demo-card"
      style="margin-top: 20px"
    >
      <template #header>
        <span>内容预览</span>
      </template>
      <div
        class="content-preview"
        v-html="editorContent"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { ElMessage } from 'element-plus';
import Editor from '@/components/Editor/index.vue';

const editorContent = ref('<p>这是一个<b>富文本编辑器</b>示例</p><p>支持<strong>加粗</strong>、<em>斜体</em>、<u>下划线</u>等格式</p>');
const currentMode = ref<'default' | 'simple'>('default');
const editorHeight = ref(300);
const isDisabled = ref(false);

// Computed property to force editor re-creation when mode changes
const editorModeKey = computed(() => `editor-${currentMode.value}`);

// Computed property to ensure height is a string with 'px' suffix
const editorHeightValue = computed(() => `${editorHeight.value}px`);

function getContent() {
  ElMessage({
    message: `内容长度: ${editorContent.value.length} 字符`,
    type: 'success',
  });
}

function clearContent() {
  editorContent.value = '';
}

function toggleDisabled() {
  isDisabled.value = !isDisabled.value;
}
</script>

<style scoped lang="scss">
.editor-view-container {
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

  .content-preview {
    padding: 16px;
    border: 1px solid #eee;
    border-radius: 4px;
    min-height: 100px;
    background: #fafafa;

    :deep(p) {
      margin: 8px 0;
    }

    :deep(b),
    :deep(strong) {
      font-weight: bold;
    }

    :deep(em) {
      font-style: italic;
    }

    :deep(u) {
      text-decoration: underline;
    }
  }
}
</style>
