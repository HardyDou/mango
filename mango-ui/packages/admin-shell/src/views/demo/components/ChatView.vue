<template>
  <DemoDocLayout
    class="chat-view"
    title="AI 对话组件"
    subtitle="基于流式响应的 AI 对话组件，支持浮动窗口、会话 ID、推荐问题、思维链展示和错误重试。"
    content-box
    :toc-items="tocItems"
  >
    <section id="basic" class="doc-section">
      <h2>基础用法</h2>
      <p>组件默认渲染为右下角浮动入口，通过 ref 可以主动打开对话窗口。</p>
      <div class="demo-block">
        <div class="demo-source">
          <div class="chat-preview">
            <el-icon><ChatDotRound /></el-icon>
            <span>点击按钮打开浮动对话窗口</span>
            <el-button type="primary" @click="handleOpenChat">打开对话</el-button>
          </div>
        </div>
        <div class="op-btns" @click="toggleCode('basic')">
          <el-icon><component :is="codeVisible.basic ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.basic ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.basic" :code="basicCode" />
      </div>
    </section>

    <section id="config" class="doc-section">
      <h2>配置推荐问题与会话</h2>
      <p>推荐问题用于空会话引导；session-id 可用于恢复指定会话。</p>
      <div class="demo-block">
        <div class="demo-source">
          <el-form label-width="112px" class="demo-panel-wide">
            <el-form-item label="欢迎消息">
              <el-input v-model="welcomeMessage" />
            </el-form-item>
            <el-form-item label="会话 ID">
              <el-input v-model="sessionId" clearable placeholder="不传则由后端生成" />
            </el-form-item>
            <el-form-item label="启用思维链">
              <el-switch v-model="enableThinking" />
            </el-form-item>
            <el-form-item label="最大长度">
              <el-input-number v-model="maxLength" :min="100" :max="5000" :step="100" />
            </el-form-item>
          </el-form>
        </div>
        <div class="op-btns" @click="toggleCode('config')">
          <el-icon><component :is="codeVisible.config ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.config ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.config" :code="configCode" />
      </div>
    </section>

    <section id="props" class="doc-section api-section">
      <h2>支持属性</h2>
      <el-table :data="propsTable" size="small" border>
        <el-table-column prop="name" label="属性名" width="180" />
        <el-table-column prop="description" label="说明" min-width="260" />
        <el-table-column prop="type" label="类型" min-width="200" />
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
        <el-table-column prop="field" label="字段" width="180" />
        <el-table-column prop="type" label="类型" min-width="200" />
        <el-table-column prop="description" label="说明" min-width="280" />
      </el-table>
    </section>

    <Chat
      ref="chatRef"
      :welcome-message="welcomeMessage"
      :enable-thinking="enableThinking"
      :max-length="maxLength"
      :session-id="sessionId"
      :recommended-questions="recommendedQuestions"
      @message-send="handleMessageSend"
      @session-change="handleSessionChange"
      @error="handleChatError"
    />
  </DemoDocLayout>
</template>

<script setup lang="ts" name="ChatView">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { ArrowDown, ArrowUp, ChatDotRound } from '@element-plus/icons-vue';
import { Chat } from '@mango/common';
import DemoCodeBlock from './DemoCodeBlock.vue';
import DemoDocLayout from './DemoDocLayout.vue';

const tocItems = [
  { id: 'basic', label: '基础用法' },
  { id: 'config', label: '配置会话' },
  { id: 'props', label: '支持属性' },
  { id: 'slots', label: '支持插槽' },
  { id: 'events', label: '支持方法 / 事件' },
  { id: 'value', label: '返回字段' },
];

const chatRef = ref<InstanceType<typeof Chat>>();
const welcomeMessage = ref('chat.welcome');
const enableThinking = ref(true);
const maxLength = ref(2000);
const sessionId = ref('');
const recommendedQuestions = ['你好', '你能做什么？', '介绍一下自己'];
const codeVisible = ref<Record<string, boolean>>({ basic: false, config: false });

const basicCode = `<template>
  <Chat ref="chatRef" @message-send="handleMessageSend" />
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { Chat } from '@mango/common';

const chatRef = ref<InstanceType<typeof Chat>>();

function openChat() {
  chatRef.value?.open();
}
<\/script>`;

const configCode = `<Chat
  :welcome-message="welcomeMessage"
  :session-id="sessionId"
  :recommended-questions="recommendedQuestions"
  :enable-thinking="true"
  :max-length="2000"
/>`;

const propsTable = [
  { name: 'sessionId', description: '默认会话 ID；不传时由后端或组件内部流程生成', type: 'string', defaultValue: "''" },
  { name: 'welcomeMessage', description: '欢迎消息文本或 i18n key', type: 'string', defaultValue: 'chat.welcome' },
  { name: 'recommendedQuestions', description: '空会话时展示的推荐问题', type: 'string[]', defaultValue: '[]' },
  { name: 'enableThinking', description: '是否展示思维链消息', type: 'boolean', defaultValue: 'true' },
  { name: 'maxLength', description: '输入框最大字符数', type: 'number', defaultValue: '2000' },
  { name: 'placeholder', description: '输入框占位文本或 i18n key', type: 'string', defaultValue: 'chat.placeholder' },
];

const slotsTable = [
  { name: '-', description: '当前组件不提供业务插槽；推荐问题、欢迎文案通过 props 配置', scope: '-' },
];

const eventsTable = [
  { name: 'message-send', description: '用户发送消息时触发', payload: 'string' },
  { name: 'session-change', description: '后端返回或组件切换会话 ID 时触发', payload: 'string' },
  { name: 'error', description: '对话请求或流式解析异常时触发', payload: 'Error' },
  { name: 'open', description: '暴露方法，打开对话窗口', payload: '() => void' },
  { name: 'close', description: '暴露方法，关闭对话窗口', payload: '() => void' },
  { name: 'toggle', description: '暴露方法，切换对话窗口', payload: '() => void' },
  { name: 'clearSession', description: '暴露方法，清空当前会话消息和会话 ID', payload: '() => void' },
  { name: 'getSessionId', description: '暴露方法，获取当前会话 ID', payload: '() => string | null' },
];

const valueTable = [
  { field: 'message-send', type: 'string', description: '用户发送的原始文本内容' },
  { field: 'session-change', type: 'string', description: 'AI 流式响应 done 事件中的 sessionId' },
  { field: 'AIEvent', type: 'thinking | message | done | error', description: '后端 SSE 返回事件类型，组件内部根据类型更新思维链、回答内容、会话 ID 或错误状态' },
];

function toggleCode(key: string) {
  codeVisible.value[key] = !codeVisible.value[key];
}

function handleOpenChat() {
  chatRef.value?.open();
}

function handleMessageSend(message: string) {
  ElMessage.info(`已发送 ${message.length} 个字符`);
}

function handleSessionChange(newSessionId: string) {
  sessionId.value = newSessionId;
}

function handleChatError(err: Error) {
  ElMessage.error(err.message || '对话组件发生异常');
}
</script>

<style scoped lang="scss">
@use './demo-page.scss';

.chat-preview {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: var(--el-fill-color-light);
  border-radius: 6px;
}
</style>
