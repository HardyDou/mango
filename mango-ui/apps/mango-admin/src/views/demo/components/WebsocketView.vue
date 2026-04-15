<template>
  <div class="websocket-view-container">
    <h1>WebSocket 客户端</h1>
    <p class="subtitle">
      基于原生 WebSocket 的客户端组件，支持心跳检测、自动重连、消息队列
    </p>

    <el-card class="demo-card">
      <template #header>
        <div class="card-header">
          <span>WebSocket 通信</span>
          <el-button
            v-if="wsStatus !== 'connected'"
            type="primary"
            size="small"
            @click="handleConnect"
          >
            连接
          </el-button>
          <el-button
            v-else
            type="danger"
            size="small"
            @click="handleDisconnect"
          >
            断开
          </el-button>
        </div>
      </template>

      <div class="ws-status">
        <el-tag :type="statusType">
          {{ statusText }}
        </el-tag>
        <span class="status-info">当前状态: {{ wsStatus }}</span>
        <span
          v-if="wsStatus === 'retrying'"
          class="retry-info"
        >
          重试次数: {{ retryCount }}/{{ maxRetries }}
        </span>
      </div>

      <el-divider>发送消息</el-divider>

      <div class="send-form">
        <el-input
          v-model="sendMessage"
          placeholder="请输入发送内容"
          style="width: 300px; margin-right: 10px"
        />
        <el-button
          type="primary"
          :disabled="wsStatus !== 'connected'"
          @click="handleSend"
        >
          发送
        </el-button>
        <el-button
          :disabled="wsStatus !== 'connected'"
          @click="handleSendPing"
        >
          发送 Ping
        </el-button>
      </div>

      <el-divider>消息日志</el-divider>

      <div
        ref="logContainerRef"
        class="message-log"
      >
        <div
          v-if="messages.length === 0"
          class="empty-log"
        >
          暂无消息，请点击"连接"按钮建立 WebSocket 连接
        </div>
        <div
          v-for="(msg, index) in messages"
          :key="index"
          class="message-item"
          :class="`message-${msg.direction}`"
        >
          <el-tag
            :type="msg.direction === 'sent' ? 'primary' : 'success'"
            size="small"
          >
            {{ msg.direction === 'sent' ? '发送' : '接收' }}
          </el-tag>
          <span class="msg-time">{{ msg.time }}</span>
          <span class="msg-type">{{ msg.type }}</span>
          <span class="msg-content">{{ msg.content }}</span>
        </div>
      </div>

      <div class="log-actions">
        <el-button
          size="small"
          @click="handleClearLog"
        >
          清空日志
        </el-button>
      </div>
    </el-card>

    <el-card
      class="demo-card"
      style="margin-top: 20px"
    >
      <template #header>
        <span>组件属性</span>
      </template>
      <el-table :data="propsTableData">
        <el-table-column
          prop="name"
          label="属性名"
          width="150"
        />
        <el-table-column
          prop="type"
          label="类型"
          width="120"
        />
        <el-table-column
          prop="default"
          label="默认值"
          width="100"
        />
        <el-table-column
          prop="description"
          label="说明"
        />
      </el-table>
    </el-card>

    <el-card
      class="demo-card"
      style="margin-top: 20px"
    >
      <template #header>
        <span>组件事件</span>
      </template>
      <el-table :data="eventsTableData">
        <el-table-column
          prop="name"
          label="事件名"
          width="150"
        />
        <el-table-column
          prop="params"
          label="参数"
          width="200"
        />
        <el-table-column
          prop="description"
          label="说明"
        />
      </el-table>
    </el-card>

    <!-- WebSocket 组件 (隐藏) -->
    <Websocket
      v-if="showWSComponent"
      ref="wsRef"
      :url="wsUrl"
      @connect="handleWSConnect"
      @disconnect="handleWSDisconnect"
      @message="handleWSMessage"
      @error="handleWSError"
    />
  </div>
</template>

<script setup lang="ts" name="WebsocketView">
import { ref, computed, nextTick } from 'vue';
import { Websocket } from '@mango/common';

const wsRef = ref<InstanceType<typeof Websocket>>();
const showWSComponent = ref(true);
const wsStatus = ref<'disconnected' | 'connecting' | 'connected' | 'retrying' | 'error'>('disconnected');
const retryCount = ref(0);
const maxRetries = 6;
const messages = ref<Array<{ direction: string; type: string; content: string; time: string }>>([]);
const sendMessage = ref('');

const wsUrl = '/bff/admin/ws/chat';

const statusType = computed(() => {
  switch (wsStatus.value) {
    case 'connected':
      return 'success';
    case 'connecting':
      return 'warning';
    case 'retrying':
      return 'warning';
    case 'error':
      return 'danger';
    default:
      return 'info';
  }
});

const statusText = computed(() => {
  switch (wsStatus.value) {
    case 'connected':
      return '已连接';
    case 'connecting':
      return '连接中...';
    case 'retrying':
      return `重连中 (${retryCount.value}/${maxRetries})`;
    case 'error':
      return '连接失败';
    default:
      return '未连接';
  }
});

const propsTableData = [
  { name: 'url', type: 'string', default: '-', description: 'WebSocket 服务器地址' },
  { name: 'enabled', type: 'boolean', default: 'true', description: '是否启用连接' },
  { name: 'reconnectInterval', type: 'number', default: '5000', description: '重连间隔 (ms)' },
  { name: 'maxRetries', type: 'number', default: '6', description: '最大重连次数' },
  { name: 'heartbeatInterval', type: 'number', default: '30000', description: '心跳间隔 (ms)' },
];

const eventsTableData = [
  { name: 'connect', params: '-', description: '连接成功时触发' },
  { name: 'disconnect', params: '-', description: '连接断开时触发' },
  { name: 'message', params: '{ type, content }', description: '收到消息时触发' },
  { name: 'error', params: 'Error', description: '连接错误时触发' },
];

function handleConnect() {
  wsStatus.value = 'connecting';
  // 模拟连接
  setTimeout(() => {
    wsStatus.value = 'connected';
    retryCount.value = 0;
    addMessage('received', 'message', 'WebSocket 连接已建立');
  }, 500);
}

function handleDisconnect() {
  wsStatus.value = 'disconnected';
  addMessage('received', 'message', 'WebSocket 连接已断开');
}

function handleSend() {
  if (!sendMessage.value.trim()) return;

  addMessage('sent', 'message', sendMessage.value);
  // 模拟发送
  setTimeout(() => {
    addMessage('received', 'message', `服务器收到: ${sendMessage.value}`);
  }, 200);
  sendMessage.value = '';
}

function handleSendPing() {
  addMessage('sent', 'ping', 'ping');
  setTimeout(() => {
    addMessage('received', 'pong', 'pong');
  }, 100);
}

function handleWSConnect() {
  wsStatus.value = 'connected';
  retryCount.value = 0;
  addMessage('received', 'message', 'WebSocket 连接成功');
}

function handleWSDisconnect() {
  wsStatus.value = 'disconnected';
  addMessage('received', 'message', 'WebSocket 连接断开');
}

function handleWSMessage(data: { type: string; content: string }) {
  addMessage('received', data.type, data.content);
}

function handleWSError(err: Error) {
  wsStatus.value = 'error';
  addMessage('received', 'error', `连接错误: ${err.message}`);
}

function addMessage(direction: string, type: string, content: string) {
  const now = new Date();
  const time = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`;
  messages.value.push({ direction, type, content, time });

  nextTick(() => {
    const container = document.querySelector('.message-log');
    if (container) {
      container.scrollTop = container.scrollHeight;
    }
  });
}

function handleClearLog() {
  messages.value = [];
}
</script>

<style scoped lang="scss">
.websocket-view-container {
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
      align-items: center;
      justify-content: space-between;
    }

    .ws-status {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 16px;

      .status-info {
        color: #909399;
        font-size: 14px;
      }

      .retry-info {
        color: #e6a23c;
        font-size: 14px;
      }
    }

    .send-form {
      display: flex;
      align-items: center;
      margin-bottom: 16px;
    }

    .message-log {
      max-height: 300px;
      overflow-y: auto;
      background: #f5f7fa;
      border-radius: 4px;
      padding: 12px;

      .empty-log {
        color: #909399;
        text-align: center;
        padding: 20px;
      }

      .message-item {
        display: flex;
        align-items: flex-start;
        gap: 8px;
        margin-bottom: 8px;
        font-size: 14px;

        .msg-time {
          color: #909399;
          flex-shrink: 0;
        }

        .msg-type {
          color: #409eff;
          flex-shrink: 0;
          min-width: 50px;
        }

        .msg-content {
          word-break: break-word;
          color: #303133;
        }

        &.message-sent {
          .msg-content {
            color: #409eff;
          }
        }
      }
    }

    .log-actions {
      margin-top: 12px;
      text-align: right;
    }
  }
}
</style>
