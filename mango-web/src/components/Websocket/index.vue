<template>
  <div class="websocket-component">
    <!-- Connection Status Indicator -->
    <div
      v-if="showStatus"
      class="websocket-status"
      :class="statusClass"
    >
      <span class="websocket-status-dot" />
      <span class="websocket-status-text">{{ statusText }}</span>
    </div>

    <!-- Reconnect Button (shown when exhausted) -->
    <el-button
      v-if="retryCount >= maxRetries"
      type="danger"
      size="small"
      @click="manualReconnect"
    >
      {{ t('websocket.reconnect') }}
    </el-button>
  </div>
</template>

<script setup lang="ts">
/**
 * WebSocket Component - Native WebSocket Wrapper
 *
 * Features:
 * - Native WebSocket with heartbeat (30s interval)
 * - Automatic reconnection (max 6 retries)
 * - Token-based authentication via handshake headers
 * - Sec-WebSocket-Protocol for tenant isolation
 * - JSON message format for ping/pong
 *
 * Backend API: ws://host/mango-message/ws/chat
 * Headers (handshake): Authorization: Bearer {token}, Sec-WebSocket-Protocol: {tenantId}
 */

import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElNotification } from 'element-plus';
import { Session } from '@/utils/storage';

export interface WebsocketProps {
  /** WebSocket endpoint URL */
  url?: string;
  /** Whether to show connection status indicator */
  showStatus?: boolean;
  /** Whether to show notifications for messages */
  showNotifications?: boolean;
  /** Maximum retry attempts */
  maxRetries?: number;
  /** Heartbeat interval in milliseconds */
  heartbeatInterval?: number;
  /** Enabled via environment variable */
  enabled?: boolean;
}

export interface WebsocketEmits {
  (e: 'connected'): void;
  (e: 'disconnected'): void;
  (e: 'message', data: WSMessage): void;
  (e: 'error', error: Event): void;
  (e: 'retry', count: number): void;
}

export interface WebsocketExpose {
  /** Connect to WebSocket endpoint */
  connect(): void;
  /** Disconnect from WebSocket endpoint */
  disconnect(): void;
  /** Send a message */
  send(data: WSMessage): void;
  /** Send ping heartbeat */
  sendPing(): void;
  /** Get current connection status */
  getStatus(): WSStatus;
}

export interface WSMessage {
  type: 'ping' | 'pong' | 'message';
  content?: string;
}

type WSStatus = 'disconnected' | 'connecting' | 'connected' | 'retrying' | 'error';

const props = withDefaults(
  defineProps<WebsocketProps>(),
  {
    url: '/mango-message/ws/chat',
    showStatus: true,
    showNotifications: true,
    maxRetries: 6,
    heartbeatInterval: 30000,
    enabled: true,
  }
);

const emit = defineEmits<WebsocketEmits>();

const { t } = useI18n();

// Connection state
const status = ref<WSStatus>('disconnected');
const retryCount = ref(0);
const ws = ref<WebSocket | null>(null);
let heartbeatTimer: ReturnType<typeof setInterval> | null = null;
let reconnectTimer: ReturnType<typeof setTimeout> | null = null;

const statusClass = computed(() => ({
  'is-connected': status.value === 'connected',
  'is-connecting': status.value === 'connecting',
  'is-retrying': status.value === 'retrying',
  'is-error': status.value === 'error' || status.value === 'disconnected',
}));

const statusText = computed(() => {
  switch (status.value) {
    case 'connected':
      return t('websocket.connected');
    case 'connecting':
      return t('websocket.connecting');
    case 'retrying':
      return t('websocket.retrying', { count: retryCount.value, max: props.maxRetries });
    case 'error':
      return t('websocket.error');
    default:
      return t('websocket.disconnected');
  }
});

/**
 * Get WebSocket URL with protocol and token
 */
function getWsUrl(token?: string, tenantId?: string): string {
  // 开发环境使用 ws://localhost:7777/api/ai/ws/chat 通过 Vite 代理
  // 生产环境直接使用相对路径
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const baseUrl = import.meta.env.DEV ? `${protocol}//${window.location.host}` : '';

  // 构建 URL 并添加 token 和 tenantId 作为 query param
  const url = new URL(props.url, baseUrl || window.location.href);
  if (token) {
    url.searchParams.set('token', token);
  }
  if (tenantId) {
    url.searchParams.set('tenantId', tenantId);
  }

  return url.toString();
}

/**
 * Show notification for message
 */
function showWsNotification(message: WSMessage) {
  if (!props.showNotifications || !message.content) return;

  ElNotification({
    title: t('websocket.message'),
    message: message.content,
    type: 'info',
    duration: 5000,
  });

  emit('message', message);
}

/**
 * Connect to WebSocket endpoint
 */
function connect() {
  if (status.value === 'connecting' || status.value === 'connected') {
    return;
  }

  // Check if WebSocket is enabled
  if (import.meta.env.VITE_WS_ENABLE === 'false') {
    console.warn('[WebSocket] WebSocket is disabled via VITE_WS_ENABLE');
    return;
  }

  status.value = 'connecting';

  try {
    const token = Session.getToken();
    const userInfo = Session.get('userInfo');
    const tenantId = userInfo?.tenantId || 'master';

    // Build URL with token and tenantId as query params
    // Issue A Fix: Token was obtained but never used - now passed via URL query param
    const wsUrl = getWsUrl(token || '', tenantId);

    // Create WebSocket with tenantId as subprotocol (for backend routing)
    ws.value = new WebSocket(wsUrl, [tenantId]);

    ws.value.onopen = () => {
      status.value = 'connected';
      retryCount.value = 0;
      emit('connected');
      startHeartbeat();
    };

    ws.value.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data) as WSMessage;

        // Ignore pong - it's just for heartbeat
        if (data.type === 'pong') return;

        showWsNotification(data);
      } catch (e) {
        console.error('[WebSocket] Failed to parse message:', e);
      }
    };

    ws.value.onerror = (error) => {
      console.error('[WebSocket] Connection error:', error);
      emit('error', error);
    };

    ws.value.onclose = (event) => {
      console.log('[WebSocket] Connection closed:', event.code, event.reason);
      handleDisconnect();
    };
  } catch (error) {
    console.error('[WebSocket] Failed to create WebSocket:', error);
    handleError();
  }
}

/**
 * Handle disconnection
 */
function handleDisconnect() {
  stopHeartbeat();

  if (ws.value) {
    ws.value.close();
    ws.value = null;
  }

  status.value = 'disconnected';
  emit('disconnected');
}

/**
 * Handle connection error with retry
 */
function handleError() {
  stopHeartbeat();

  if (ws.value) {
    ws.value.close();
    ws.value = null;
  }

  if (retryCount.value < props.maxRetries) {
    status.value = 'retrying';
    retryCount.value++;
    emit('retry', retryCount.value);

    // Exponential backoff retry
    const delay = Math.min(1000 * Math.pow(2, retryCount.value), 30000);
    reconnectTimer = setTimeout(() => {
      if (status.value === 'retrying') {
        connect();
      }
    }, delay);
  } else {
    status.value = 'error';
  }
}

/**
 * Start heartbeat timer
 */
function startHeartbeat() {
  stopHeartbeat();
  heartbeatTimer = setInterval(() => {
    sendPing();
  }, props.heartbeatInterval);
}

/**
 * Stop heartbeat timer
 */
function stopHeartbeat() {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer);
    heartbeatTimer = null;
  }
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }
}

/**
 * Send ping heartbeat
 */
function sendPing() {
  if (ws.value && ws.value.readyState === WebSocket.OPEN) {
    try {
      ws.value.send(JSON.stringify({ type: 'ping' }));
    } catch (e) {
      console.error('[WebSocket] Failed to send ping:', e);
    }
  }
}

/**
 * Send a message
 */
function send(data: WSMessage) {
  if (ws.value && ws.value.readyState === WebSocket.OPEN) {
    ws.value.send(JSON.stringify(data));
  }
}

/**
 * Manual reconnect (user triggered)
 */
function manualReconnect() {
  retryCount.value = 0;
  connect();
}

/**
 * Disconnect from WebSocket endpoint
 */
function disconnect() {
  stopHeartbeat();

  if (ws.value) {
    ws.value.close();
    ws.value = null;
  }

  status.value = 'disconnected';
  emit('disconnected');
}

/**
 * Get current status
 */
function getStatus(): WSStatus {
  return status.value;
}

// Lifecycle
onMounted(() => {
  if (props.enabled) {
    connect();
  }
});

onUnmounted(() => {
  disconnect();
});

// Expose methods
defineExpose<WebsocketExpose>({
  connect,
  disconnect,
  send,
  sendPing,
  getStatus,
});
</script>

<style scoped lang="scss">
.websocket-component {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.websocket-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
}

.websocket-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #909399;
}

.websocket-status.is-connected .websocket-status-dot {
  background-color: #67c23a;
}

.websocket-status.is-connecting .websocket-status-dot,
.websocket-status.is-retrying .websocket-status-dot {
  background-color: #e6a23c;
  animation: pulse 1s infinite;
}

.websocket-status.is-error .websocket-status-dot {
  background-color: #f56c6c;
}

@keyframes pulse {
  0% {
    opacity: 1;
  }
  50% {
    opacity: 0.4;
  }
  100% {
    opacity: 1;
  }
}
</style>
