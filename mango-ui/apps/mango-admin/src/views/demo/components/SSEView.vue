<template>
  <DemoDocLayout
    class="sse-view"
    title="服务端推送 (SSE)"
    subtitle="基于 Server-Sent Events 的服务端推送组件，封装认证参数、租户标识、连接状态、自动重连和消息通知。"
    content-box
    :toc-items="tocItems"
  >
    <section id="basic" class="doc-section">
      <h2>消息接收</h2>
      <p>SSE 适合通知、告警、进度推送等服务端单向推送场景。</p>
      <div class="demo-block">
        <div class="demo-source">
          <div class="network-status">
            <el-tag :type="statusType">{{ statusText }}</el-tag>
            <span>当前状态：{{ sseStatus }}</span>
          </div>
          <div class="send-form">
            <el-button type="primary" @click="handleConnect">连接</el-button>
            <el-button @click="pushDemoMessage">模拟推送</el-button>
            <el-button type="danger" @click="handleDisconnect">断开</el-button>
            <el-button @click="messages = []">清空日志</el-button>
          </div>
          <div class="message-log">
            <div v-if="messages.length === 0" class="empty-log">暂无消息</div>
            <div v-for="(msg, index) in messages" :key="index" class="message-item">
              <span class="msg-time">{{ msg.time }}</span>
              <span class="msg-type">{{ msg.type }}</span>
              <span class="msg-content">{{ msg.content }}</span>
            </div>
          </div>
        </div>
        <div class="op-btns" @click="toggleCode('basic')">
          <el-icon><component :is="codeVisible.basic ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.basic ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.basic" :code="basicCode" />
      </div>
    </section>

    <section id="reconnect" class="doc-section">
      <h2>重连配置</h2>
      <p>enabled 控制是否挂载后自动连接；maxRetries 控制异常后的最大重试次数。</p>
      <div class="demo-block">
        <div class="demo-source">
          <el-form label-width="120px" class="demo-panel-wide">
            <el-form-item label="连接地址">
              <el-input v-model="sseUrl" />
            </el-form-item>
            <el-form-item label="心跳间隔">
              <el-input-number v-model="heartbeatInterval" :min="5000" :step="5000" />
            </el-form-item>
            <el-form-item label="最大重试">
              <el-input-number v-model="maxRetries" :min="0" :max="10" />
            </el-form-item>
          </el-form>
        </div>
        <div class="op-btns" @click="toggleCode('reconnect')">
          <el-icon><component :is="codeVisible.reconnect ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.reconnect ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.reconnect" :code="reconnectCode" />
      </div>
    </section>

    <section id="props" class="doc-section api-section">
      <h2>支持属性</h2>
      <el-table :data="propsTable" size="small" border>
        <el-table-column prop="name" label="属性名" width="180" />
        <el-table-column prop="description" label="说明" min-width="260" />
        <el-table-column prop="type" label="类型" min-width="180" />
        <el-table-column prop="defaultValue" label="默认值" width="180" />
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
        <el-table-column prop="field" label="字段" width="170" />
        <el-table-column prop="type" label="类型" min-width="200" />
        <el-table-column prop="description" label="说明" min-width="280" />
      </el-table>
    </section>

    <SSE
      v-if="showSSEComponent"
      ref="sseRef"
      :url="sseUrl"
      :enabled="false"
      :max-retries="maxRetries"
      :heartbeat-interval="heartbeatInterval"
      :show-notifications="false"
      @connected="handleSSEConnected"
      @disconnected="handleSSEDisconnected"
      @message="handleSSEMessage"
      @error="handleSSEError"
      @retry="handleSSERetry"
    />
  </DemoDocLayout>
</template>

<script setup lang="ts" name="SSEView">
import { computed, nextTick, ref } from 'vue';
import { ArrowDown, ArrowUp } from '@element-plus/icons-vue';
import { SSE } from '@mango/common';
import DemoCodeBlock from './DemoCodeBlock.vue';
import DemoDocLayout from './DemoDocLayout.vue';

const tocItems = [
  { id: 'basic', label: '消息接收' },
  { id: 'reconnect', label: '重连配置' },
  { id: 'props', label: '支持属性' },
  { id: 'slots', label: '支持插槽' },
  { id: 'events', label: '支持方法 / 事件' },
  { id: 'value', label: '返回字段' },
];

const sseRef = ref<InstanceType<typeof SSE>>();
const showSSEComponent = ref(true);
const sseStatus = ref<'disconnected' | 'connecting' | 'connected' | 'retrying' | 'error'>('disconnected');
const messages = ref<Array<{ type: string; content: string; time: string }>>([]);
const sseUrl = ref('/mango-message/sse/connect');
const heartbeatInterval = ref(30000);
const maxRetries = ref(6);
const codeVisible = ref<Record<string, boolean>>({ basic: false, reconnect: false });

const statusType = computed(() => {
  if (sseStatus.value === 'connected') return 'success';
  if (sseStatus.value === 'connecting' || sseStatus.value === 'retrying') return 'warning';
  if (sseStatus.value === 'error') return 'danger';
  return 'info';
});

const statusText = computed(() => {
  const map = { connected: '已连接', connecting: '连接中', retrying: '重连中', error: '连接失败', disconnected: '未连接' };
  return map[sseStatus.value];
});

const basicCode = `<SSE
  url="/mango-message/sse/connect"
  @connected="handleConnected"
  @message="handleMessage"
/>`;
const reconnectCode = `<SSE
  url="/mango-message/sse/connect"
  :max-retries="6"
  :heartbeat-interval="30000"
  :show-notifications="false"
/>`;

const propsTable = [
  { name: 'url', description: 'SSE 端点地址，组件会附加 token 和 tenantId 查询参数', type: 'string', defaultValue: '/mango-message/sse/connect' },
  { name: 'showStatus', description: '是否显示组件自带连接状态', type: 'boolean', defaultValue: 'true' },
  { name: 'showNotifications', description: '收到消息时是否弹出通知', type: 'boolean', defaultValue: 'true' },
  { name: 'maxRetries', description: '最大自动重连次数', type: 'number', defaultValue: '6' },
  { name: 'heartbeatInterval', description: '心跳间隔，单位毫秒', type: 'number', defaultValue: '30000' },
  { name: 'enabled', description: '是否在挂载后自动连接', type: 'boolean', defaultValue: 'true' },
];

const slotsTable = [{ name: '-', description: '当前组件不提供业务插槽', scope: '-' }];

const eventsTable = [
  { name: 'connected', description: 'SSE 连接成功时触发', payload: 'void' },
  { name: 'disconnected', description: '连接断开时触发', payload: 'void' },
  { name: 'message', description: '收到非 pong 消息时触发', payload: 'SSEMessage' },
  { name: 'error', description: '连接错误或重试耗尽时触发', payload: 'Error' },
  { name: 'retry', description: '进入自动重连时触发', payload: 'number' },
  { name: 'connect', description: '暴露方法，建立连接', payload: '() => void' },
  { name: 'disconnect', description: '暴露方法，断开连接', payload: '() => void' },
  { name: 'sendPing', description: '暴露方法，当前原生 EventSource 实现下保留为空操作', payload: '() => void' },
  { name: 'getStatus', description: '暴露方法，获取当前连接状态', payload: '() => SSEStatus' },
];

const valueTable = [
  { field: 'SSEMessage.type', type: "'notification' | 'alert' | 'pong'", description: '消息类型；组件内部忽略 pong，业务消息通过 message 事件返回' },
  { field: 'SSEMessage.content', type: 'string | undefined', description: '服务端推送的消息正文' },
  { field: 'getStatus()', type: 'SSEStatus', description: '返回 disconnected、connecting、connected、retrying 或 error' },
];

function toggleCode(key: string) {
  codeVisible.value[key] = !codeVisible.value[key];
}

function handleConnect() {
  sseStatus.value = 'connecting';
  sseRef.value?.connect();
  window.setTimeout(() => {
    if (sseStatus.value === 'connecting') {
      sseStatus.value = 'connected';
      addMessage('notification', '示例连接已建立');
    }
  }, 300);
}

function handleDisconnect() {
  sseRef.value?.disconnect();
  sseStatus.value = 'disconnected';
  addMessage('notification', '连接已断开');
}

function pushDemoMessage() {
  addMessage('notification', `服务端推送示例 ${messages.value.length + 1}`);
}

function handleSSEConnected() {
  sseStatus.value = 'connected';
  addMessage('notification', 'SSE 连接成功');
}

function handleSSEDisconnected() {
  sseStatus.value = 'disconnected';
}

function handleSSEMessage(data: { type: string; content?: string }) {
  addMessage(data.type, data.content || '');
}

function handleSSEError(err: Error) {
  sseStatus.value = 'error';
  addMessage('alert', err.message || '连接错误');
}

function handleSSERetry(count: number) {
  sseStatus.value = 'retrying';
  addMessage('notification', `第 ${count} 次重连`);
}

function addMessage(type: string, content: string) {
  const now = new Date();
  const time = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`;
  messages.value.push({ type, content, time });
  nextTick(() => {
    const container = document.querySelector('.message-log');
    if (container) container.scrollTop = container.scrollHeight;
  });
}
</script>

<style scoped lang="scss">
@use './demo-page.scss';

.network-status,
.send-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.message-log {
  max-height: 260px;
  overflow-y: auto;
  padding: 12px;
  background: var(--el-fill-color-light);
  border-radius: 6px;
}

.empty-log {
  color: var(--el-text-color-secondary);
  text-align: center;
}

.message-item {
  display: flex;
  gap: 8px;
  line-height: 1.7;
}

.msg-time {
  flex: none;
  color: var(--el-text-color-secondary);
}

.msg-type {
  flex: none;
  color: var(--el-color-primary);
}

.msg-content {
  word-break: break-word;
}
</style>
