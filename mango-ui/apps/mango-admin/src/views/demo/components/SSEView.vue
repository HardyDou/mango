<template>
  <div class="sse-view-container">
    <h1>服务端推送 (SSE)</h1>
    <p class="subtitle">
      基于 Server-Sent Events 的服务端推送组件，支持连接状态管理、自动重连、心跳检测
    </p>

    <el-card class="demo-card">
      <template #header>
        <div class="card-header">
          <span>SSE 消息接收</span>
          <el-button
            v-if="sseStatus !== 'connected'"
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

      <div class="sse-status">
        <el-tag :type="statusType">
          {{ statusText }}
        </el-tag>
        <span class="status-info">当前状态: {{ sseStatus }}</span>
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
          暂无消息，请点击"连接"按钮建立 SSE 连接
        </div>
        <div
          v-for="(msg, index) in messages"
          :key="index"
          class="message-item"
          :class="`message-${msg.type}`"
        >
          <span class="msg-time">{{ msg.time }}</span>
          <el-tag
            v-if="msg.type === 'notification'"
            type="success"
            size="small"
          >
            通知
          </el-tag>
          <el-tag
            v-else-if="msg.type === 'alert'"
            type="danger"
            size="small"
          >
            告警
          </el-tag>
          <el-tag
            v-else-if="msg.type === 'pong'"
            type="info"
            size="small"
          >
            心跳
          </el-tag>
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

    <!-- SSE 组件 (隐藏) -->
    <SSE
      v-if="showSSEComponent"
      ref="sseRef"
      :url="sseUrl"
      @connect="handleSSEConnect"
      @disconnect="handleSSEDisconnect"
      @message="handleSSEMessage"
      @error="handleSSEError"
    />
  </div>
</template>

<script setup lang="ts" name="SSEView">
import { ref, computed, nextTick } from 'vue';
import { SSE } from '@mango/common';

const sseRef = ref<InstanceType<typeof SSE>>();
const showSSEComponent = ref(true);
const sseStatus = ref<'disconnected' | 'connecting' | 'connected' | 'retrying' | 'error'>('disconnected');
const messages = ref<Array<{ type: string; content: string; time: string }>>([]);

const sseUrl = '/bff/admin/sse/connect';

const statusType = computed(() => {
  switch (sseStatus.value) {
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
  switch (sseStatus.value) {
    case 'connected':
      return '已连接';
    case 'connecting':
      return '连接中...';
    case 'retrying':
      return '重连中...';
    case 'error':
      return '连接失败';
    default:
      return '未连接';
  }
});

const propsTableData = [
  { name: 'url', type: 'string', default: '-', description: 'SSE 连接地址' },
  { name: 'enabled', type: 'boolean', default: 'true', description: '是否启用连接' },
  { name: 'reconnectInterval', type: 'number', default: '5000', description: '重连间隔 (ms)' },
  { name: 'maxRetries', type: 'number', default: '6', description: '最大重连次数' },
];

const eventsTableData = [
  { name: 'connect', params: '-', description: '连接成功时触发' },
  { name: 'disconnect', params: '-', description: '连接断开时触发' },
  { name: 'message', params: '{ type, content }', description: '收到消息时触发' },
  { name: 'error', params: 'Error', description: '连接错误时触发' },
];

function handleConnect() {
  sseStatus.value = 'connecting';
  // 模拟连接
  setTimeout(() => {
    sseStatus.value = 'connected';
    addMessage('notification', 'SSE 连接已建立，正在接收服务器推送...');
  }, 500);
}

function handleDisconnect() {
  sseStatus.value = 'disconnected';
  addMessage('notification', 'SSE 连接已断开');
}

function handleSSEConnect() {
  sseStatus.value = 'connected';
  addMessage('notification', 'SSE 连接成功');
}

function handleSSEDisconnect() {
  sseStatus.value = 'disconnected';
  addMessage('notification', 'SSE 连接断开');
}

function handleSSEMessage(data: { type: string; content: string }) {
  addMessage(data.type, data.content);
}

function handleSSEError(err: Error) {
  sseStatus.value = 'error';
  addMessage('alert', `连接错误: ${err.message}`);
}

function addMessage(type: string, content: string) {
  const now = new Date();
  const time = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`;
  messages.value.push({ type, content, time });

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
.sse-view-container {
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

    .sse-status {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 16px;

      .status-info {
        color: #909399;
        font-size: 14px;
      }
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

        .msg-content {
          word-break: break-word;
        }

        &.message-notification {
          .msg-content {
            color: #67c23a;
          }
        }

        &.message-alert {
          .msg-content {
            color: #f56c6c;
          }
        }

        &.message-pong {
          .msg-content {
            color: #909399;
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
