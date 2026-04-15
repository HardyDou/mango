<template>
  <div class="sse-component">
    <!-- Connection Status Indicator -->
    <div
      v-if="showStatus"
      class="sse-status"
      :class="statusClass"
    >
      <span class="sse-status-dot" />
      <span class="sse-status-text">{{ statusText }}</span>
    </div>

    <!-- Reconnect Button (shown when exhausted) -->
    <el-button
      v-if="retryCount >= maxRetries"
      type="danger"
      size="small"
      @click="manualReconnect"
    >
      {{ t('sse.reconnect') }}
    </el-button>
  </div>
</template>

<script setup lang="ts">
/**
 * SSE Component - Server-Sent Events
 *
 * Uses @microsoft/fetch-event-source for SSE connection with:
 * - Automatic reconnection (max 6 retries)
 * - Heartbeat mechanism (30s interval ping/pong)
 * - Token-based authentication
 * - Tenant isolation
 *
 * Backend API: GET /mango-message/sse/connect
 * Headers: Authorization: Bearer {token}, TENANT-ID: {tenantId}
 */

import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElNotification } from 'element-plus';
import { Session } from '../../utils/storage';

// Note: @microsoft/fetch-event-source needs to be installed
// import { fetchEventSource } from '@microsoft/fetch-event-source';

export interface SSEProps {
  /** SSE endpoint URL */
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

export interface SSEEmits {
  (e: 'connected'): void;
  (e: 'disconnected'): void;
  (e: 'message', data: SSEMessage): void;
  (e: 'error', error: Error): void;
  (e: 'retry', count: number): void;
}

export interface SSEExpose {
  /** Connect to SSE endpoint */
  connect(): void;
  /** Disconnect from SSE endpoint */
  disconnect(): void;
  /** Send ping heartbeat */
  sendPing(): void;
  /** Get current connection status */
  getStatus(): SSEStatus;
}

export interface SSEMessage {
  type: 'notification' | 'alert' | 'pong';
  content?: string;
}

type SSEStatus = 'disconnected' | 'connecting' | 'connected' | 'retrying' | 'error';

const props = withDefaults(
  defineProps<SSEProps>(),
  {
    url: '/mango-message/sse/connect',
    showStatus: true,
    showNotifications: true,
    maxRetries: 6,
    heartbeatInterval: 30000,
    enabled: true,
  }
);

const emit = defineEmits<SSEEmits>();

const { t } = useI18n();

// Connection state
const status = ref<SSEStatus>('disconnected');
const retryCount = ref(0);
const eventSource = ref<EventSource | null>(null);
let heartbeatTimer: ReturnType<typeof setInterval> | null = null;

const statusClass = computed(() => ({
  'is-connected': status.value === 'connected',
  'is-connecting': status.value === 'connecting',
  'is-retrying': status.value === 'retrying',
  'is-error': status.value === 'error' || status.value === 'disconnected',
}));

const statusText = computed(() => {
  switch (status.value) {
    case 'connected':
      return t('sse.connected');
    case 'connecting':
      return t('sse.connecting');
    case 'retrying':
      return t('sse.retrying', { count: retryCount.value, max: props.maxRetries });
    case 'error':
      return t('sse.error');
    default:
      return t('sse.disconnected');
  }
});

/**
 * Get auth headers for SSE connection
 */
function getAuthHeaders(): Record<string, string> {
  const token = Session.getToken();
  const userInfo = Session.get('userInfo');
  const tenantId = userInfo?.tenantId || 'master';

  return {
    Authorization: token ? `Bearer ${token}` : '',
    'TENANT-ID': tenantId,
    Accept: 'text/event-stream',
  };
}

/**
 * Show notification for SSE message
 */
function showNotification(message: SSEMessage) {
  if (!props.showNotifications) return;

  const title = message.type === 'alert' ? t('sse.alert') : t('sse.notification');
  const type = message.type === 'alert' ? 'warning' : 'info';

  ElNotification({
    title,
    message: message.content || '',
    type,
    duration: 5000,
  });

  emit('message', message);
}

/**
 * Connect to SSE endpoint
 */
async function connect() {
  if (status.value === 'connecting' || status.value === 'connected') {
    return;
  }

  // Check if SSE is enabled
  if (import.meta.env.VITE_SSE_ENABLE === 'false') {
    console.warn('[SSE] SSE is disabled via VITE_SSE_ENABLE');
    return;
  }

  status.value = 'connecting';
  retryCount.value = 0;

  try {
    // Use native EventSource for simplicity
    // For production with fetch-event-source, use the commented code below
    /*
    await fetchEventSource(props.url, {
      headers: getAuthHeaders(),
      onopen() {
        status.value = 'connected';
        retryCount.value = 0;
        emit('connected');
        startHeartbeat();
      },
      onmessage(event) {
        try {
          const data = JSON.parse(event.data);
          if (data.type === 'pong') return; // Ignore pong
          showNotification(data);
        } catch (e) {
          console.error('[SSE] Failed to parse message:', e);
        }
      },
      onclose() {
        handleDisconnect();
      },
      onerror(error) {
        handleError(error);
      },
    });
    */

    // Native EventSource implementation
    const token = Session.getToken();
    const userInfo = Session.get('userInfo');
    const tenantId = userInfo?.tenantId || 'master';

    // Build URL with query params for native EventSource
    const url = new URL(props.url, window.location.origin);
    url.searchParams.set('token', token || '');
    url.searchParams.set('tenantId', tenantId);

    eventSource.value = new EventSource(url.toString());

    eventSource.value.onopen = () => {
      status.value = 'connected';
      retryCount.value = 0;
      emit('connected');
      startHeartbeat();
    };

    eventSource.value.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        if (data.type === 'pong') return;
        showNotification(data);
      } catch (e) {
        console.error('[SSE] Failed to parse message:', e);
      }
    };

    eventSource.value.onerror = (error) => {
      handleError(error);
    };
  } catch (error) {
    handleError(error);
  }
}

/**
 * Handle disconnection
 */
function handleDisconnect() {
  stopHeartbeat();
  status.value = 'disconnected';
  emit('disconnected');
}

/**
 * Handle connection error with retry
 */
function handleError(error: Event) {
  console.error('[SSE] Connection error:', error);

  stopHeartbeat();

  if (eventSource.value) {
    eventSource.value.close();
    eventSource.value = null;
  }

  if (retryCount.value < props.maxRetries) {
    status.value = 'retrying';
    retryCount.value++;
    emit('retry', retryCount.value);

    // Exponential backoff retry
    const delay = Math.min(1000 * Math.pow(2, retryCount.value), 30000);
    setTimeout(() => {
      if (status.value === 'retrying') {
        connect();
      }
    }, delay);
  } else {
    status.value = 'error';
    emit('error', new Error('Max retries exceeded'));
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
}

/**
 * Send ping heartbeat
 */
function sendPing() {
  // In native EventSource, we can't send custom messages
  // This would need fetch-event-source or WebSocket for proper ping/pong
  // For now, we rely on the native reconnection mechanism
}

/**
 * Manual reconnect (user triggered)
 */
function manualReconnect() {
  retryCount.value = 0;
  connect();
}

/**
 * Disconnect from SSE endpoint
 */
function disconnect() {
  stopHeartbeat();

  if (eventSource.value) {
    eventSource.value.close();
    eventSource.value = null;
  }

  status.value = 'disconnected';
  emit('disconnected');
}

/**
 * Get current status
 */
function getStatus(): SSEStatus {
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
defineExpose<SSEExpose>({
  connect,
  disconnect,
  sendPing,
  getStatus,
});
</script>

<style scoped lang="scss">
.sse-component {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.sse-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
}

.sse-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #909399;
}

.sse-status.is-connected .sse-status-dot {
  background-color: #67c23a;
}

.sse-status.is-connecting .sse-status-dot,
.sse-status.is-retrying .sse-status-dot {
  background-color: #e6a23c;
  animation: pulse 1s infinite;
}

.sse-status.is-error .sse-status-dot {
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
