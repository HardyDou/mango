<template>
  <DemoDocLayout
    class="websocket-view"
    title="WebSocket 客户端"
    subtitle="基于原生 WebSocket 的客户端组件，封装认证参数、租户标识、心跳、重连和消息通知。"
    content-box
    :toc-items="tocItems"
  >
    <section id="basic" class="doc-section">
      <h2>连接与发送</h2>
      <p>组件挂载后可自动连接，也可以通过 ref 手动 connect、disconnect、send。</p>
      <div class="demo-block">
        <div class="demo-source">
          <div class="network-status">
            <el-tag :type="statusType">{{ statusText }}</el-tag>
            <span>当前状态：{{ wsStatus }}</span>
          </div>
          <div class="send-form">
            <el-input v-model="sendMessage" placeholder="请输入发送内容" class="demo-network-url" />
            <el-button type="primary" @click="handleConnect">连接</el-button>
            <el-button :disabled="wsStatus !== 'connected'" @click="handleSend">发送</el-button>
            <el-button type="danger" @click="handleDisconnect">断开</el-button>
          </div>
          <MessageLog :messages="messages" />
        </div>
        <div class="op-btns" @click="toggleCode('basic')">
          <el-icon><component :is="codeVisible.basic ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.basic ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.basic" :code="basicCode" />
      </div>
    </section>

    <section id="heartbeat" class="doc-section">
      <h2>心跳与重连</h2>
      <p>heartbeatInterval 控制心跳间隔；maxRetries 控制异常后的最大重试次数。</p>
      <div class="demo-block">
        <div class="demo-source">
          <el-form label-width="120px" class="demo-panel-wide">
            <el-form-item label="连接地址">
              <el-input v-model="wsUrl" />
            </el-form-item>
            <el-form-item label="心跳间隔">
              <el-input-number v-model="heartbeatInterval" :min="5000" :step="5000" />
            </el-form-item>
            <el-form-item label="最大重试">
              <el-input-number v-model="maxRetries" :min="0" :max="10" />
            </el-form-item>
          </el-form>
        </div>
        <div class="op-btns" @click="toggleCode('heartbeat')">
          <el-icon><component :is="codeVisible.heartbeat ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.heartbeat ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.heartbeat" :code="heartbeatCode" />
      </div>
    </section>

    <section id="props" class="doc-section api-section">
      <h2>支持属性</h2>
      <el-table :data="propsTable" size="small" border>
        <el-table-column prop="name" label="属性名" width="180" />
        <el-table-column prop="description" label="说明" min-width="260" />
        <el-table-column prop="type" label="类型" min-width="180" />
        <el-table-column prop="defaultValue" label="默认值" width="160" />
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
        <el-table-column prop="type" label="类型" min-width="200" />
        <el-table-column prop="description" label="说明" min-width="280" />
      </el-table>
    </section>

    <Websocket
      v-if="showWSComponent"
      ref="wsRef"
      :url="wsUrl"
      :enabled="false"
      :max-retries="maxRetries"
      :heartbeat-interval="heartbeatInterval"
      :show-notifications="false"
      @connected="handleWSConnected"
      @disconnected="handleWSDisconnected"
      @message="handleWSMessage"
      @error="handleWSError"
      @retry="handleWSRetry"
    />
  </DemoDocLayout>
</template>

<script setup lang="ts" name="WebsocketView">
import { computed, defineComponent, h, nextTick, ref } from 'vue';
import { ArrowDown, ArrowUp } from '@element-plus/icons-vue';
import { Websocket } from '@mango/common';
import DemoCodeBlock from './DemoCodeBlock.vue';
import DemoDocLayout from './DemoDocLayout.vue';

const MessageLog = defineComponent({
  props: { messages: { type: Array, required: true } },
  setup(props) {
    return () => h('div', { class: 'message-log' }, (props.messages as Array<any>).length
      ? (props.messages as Array<any>).map((msg) => h('div', { class: `message-item message-${msg.direction}` }, [
        h('span', { class: 'msg-time' }, msg.time),
        h('span', { class: 'msg-type' }, msg.type),
        h('span', { class: 'msg-content' }, msg.content),
      ]))
      : h('div', { class: 'empty-log' }, '暂无消息'));
  },
});

const tocItems = [
  { id: 'basic', label: '连接与发送' },
  { id: 'heartbeat', label: '心跳与重连' },
  { id: 'props', label: '支持属性' },
  { id: 'slots', label: '支持插槽' },
  { id: 'events', label: '支持方法 / 事件' },
  { id: 'value', label: '返回字段' },
];

const wsRef = ref<InstanceType<typeof Websocket>>();
const showWSComponent = ref(true);
const wsStatus = ref<'disconnected' | 'connecting' | 'connected' | 'retrying' | 'error'>('disconnected');
const messages = ref<Array<{ direction: string; type: string; content: string; time: string }>>([]);
const sendMessage = ref('');
const wsUrl = ref('/mango-message/ws/chat');
const heartbeatInterval = ref(30000);
const maxRetries = ref(6);
const codeVisible = ref<Record<string, boolean>>({ basic: false, heartbeat: false });

const statusType = computed(() => {
  if (wsStatus.value === 'connected') return 'success';
  if (wsStatus.value === 'connecting' || wsStatus.value === 'retrying') return 'warning';
  if (wsStatus.value === 'error') return 'danger';
  return 'info';
});

const statusText = computed(() => {
  const map = { connected: '已连接', connecting: '连接中', retrying: '重连中', error: '连接失败', disconnected: '未连接' };
  return map[wsStatus.value];
});

const basicCode = `<Websocket
  ref="wsRef"
  url="/mango-message/ws/chat"
  @connected="handleConnected"
  @message="handleMessage"
/>

wsRef.value?.send({ type: 'message', content: 'hello' });`;
const heartbeatCode = `<Websocket
  url="/mango-message/ws/chat"
  :heartbeat-interval="30000"
  :max-retries="6"
  :show-notifications="false"
/>`;

const propsTable = [
  { name: 'url', description: 'WebSocket 端点地址，组件会附加 token 和 tenantId 查询参数', type: 'string', defaultValue: '/mango-message/ws/chat' },
  { name: 'showStatus', description: '是否显示组件自带连接状态', type: 'boolean', defaultValue: 'true' },
  { name: 'showNotifications', description: '收到消息时是否弹出通知', type: 'boolean', defaultValue: 'true' },
  { name: 'maxRetries', description: '最大自动重连次数', type: 'number', defaultValue: '6' },
  { name: 'heartbeatInterval', description: '心跳间隔，单位毫秒', type: 'number', defaultValue: '30000' },
  { name: 'enabled', description: '是否在挂载后自动连接', type: 'boolean', defaultValue: 'true' },
];

const slotsTable = [{ name: '-', description: '当前组件不提供业务插槽', scope: '-' }];

const eventsTable = [
  { name: 'connected', description: 'WebSocket 连接成功时触发', payload: 'void' },
  { name: 'disconnected', description: '连接断开时触发', payload: 'void' },
  { name: 'message', description: '收到非 pong 消息时触发', payload: 'WSMessage' },
  { name: 'error', description: '连接错误时触发', payload: 'Event' },
  { name: 'retry', description: '进入自动重连时触发', payload: 'number' },
  { name: 'connect', description: '暴露方法，建立连接', payload: '() => void' },
  { name: 'disconnect', description: '暴露方法，断开连接', payload: '() => void' },
  { name: 'send', description: '暴露方法，发送 JSON 消息', payload: '(data: WSMessage) => void' },
  { name: 'sendPing', description: '暴露方法，发送 ping 心跳', payload: '() => void' },
  { name: 'getStatus', description: '暴露方法，获取当前连接状态', payload: '() => WSStatus' },
];

const valueTable = [
  { field: 'WSMessage.type', type: "'ping' | 'pong' | 'message'", description: '消息类型；组件内部忽略 pong，业务消息通过 message 事件返回' },
  { field: 'WSMessage.content', type: 'string | undefined', description: '消息正文内容' },
  { field: 'getStatus()', type: 'WSStatus', description: '返回 disconnected、connecting、connected、retrying 或 error' },
];

function toggleCode(key: string) {
  codeVisible.value[key] = !codeVisible.value[key];
}

function handleConnect() {
  wsStatus.value = 'connecting';
  wsRef.value?.connect();
  window.setTimeout(() => {
    if (wsStatus.value === 'connecting') {
      wsStatus.value = 'connected';
      addMessage('received', 'message', '示例连接已建立');
    }
  }, 300);
}

function handleDisconnect() {
  wsRef.value?.disconnect();
  wsStatus.value = 'disconnected';
  addMessage('received', 'message', '连接已断开');
}

function handleSend() {
  const content = sendMessage.value.trim();
  if (!content) return;
  wsRef.value?.send({ type: 'message', content });
  addMessage('sent', 'message', content);
  sendMessage.value = '';
}

function handleWSConnected() {
  wsStatus.value = 'connected';
  addMessage('received', 'message', 'WebSocket 连接成功');
}

function handleWSDisconnected() {
  wsStatus.value = 'disconnected';
}

function handleWSMessage(data: { type: string; content?: string }) {
  addMessage('received', data.type, data.content || '');
}

function handleWSError() {
  wsStatus.value = 'error';
  addMessage('received', 'error', '连接错误');
}

function handleWSRetry(count: number) {
  wsStatus.value = 'retrying';
  addMessage('received', 'retry', `第 ${count} 次重连`);
}

function addMessage(direction: string, type: string, content: string) {
  const now = new Date();
  const time = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`;
  messages.value.push({ direction, type, content, time });
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
