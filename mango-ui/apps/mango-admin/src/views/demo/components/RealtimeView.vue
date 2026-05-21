<template>
  <div class="component-doc-page realtime-view">
    <div class="doc-layout">
      <div class="doc-content is-boxed">
        <header class="page-header">
          <h1>RealtimeClient 实时通信组件</h1>
          <p>以 createRealtimeClient / useRealtime 建立稳定实时链路，业务页面只订阅消息和发送消息，不直接处理底层传输差异。</p>
        </header>

        <main class="examples">
          <section id="chat" class="doc-section">
      <h2>聊天室业务用法</h2>
      <p>登记当前用户后连接；Auto 会先协商协议，失败时按 WebSocket、SSE、Ajax Polling 自动降级。右侧只显示业务消息，点击消息查看原始 JSON。</p>
      <div class="demo-block" data-testid="realtime-chat-panel">
        <div class="demo-source">
          <div class="realtime-shell">
            <aside class="realtime-sidebar">
              <div class="sidebar-title">
                <strong>配置信息</strong>
                <span>连接前可修改用户上下文</span>
              </div>

              <el-tabs v-model="activeConfigTab" class="config-tabs" stretch>
                <el-tab-pane label="链接配置" name="connection">
                  <el-form label-position="top" class="runtime-form">
                    <el-form-item label="链接模式">
                      <el-radio-group v-model="connectionMode" size="small" :disabled="connected">
                        <el-radio-button label="auto">Auto</el-radio-button>
                        <el-radio-button label="websocket">WS</el-radio-button>
                        <el-radio-button label="sse">SSE</el-radio-button>
                        <el-radio-button label="polling">Ajax</el-radio-button>
                      </el-radio-group>
                    </el-form-item>

                    <div class="runtime-grid">
                      <el-form-item label="心跳(ms)">
                        <el-input-number v-model="runtimeConfig.heartbeatInterval" :min="500" :step="500" controls-position="right" :disabled="connected" />
                      </el-form-item>
                      <el-form-item label="超时(ms)">
                        <el-input-number v-model="runtimeConfig.heartbeatTimeout" :min="1000" :step="1000" controls-position="right" :disabled="connected" />
                      </el-form-item>
                      <el-form-item label="最大重连">
                        <el-input-number v-model="runtimeConfig.maxRetries" :min="0" :max="99" controls-position="right" :disabled="connected" />
                      </el-form-item>
                      <el-form-item label="Polling 超时">
                        <el-input-number v-model="runtimeConfig.pollingTimeoutMillis" :min="0" :step="1000" controls-position="right" :disabled="connected" />
                      </el-form-item>
                    </div>
                  </el-form>
                </el-tab-pane>

                <el-tab-pane label="用户信息" name="user">
                  <el-form label-position="top" class="profile-form">
                    <el-form-item label="投递目标">
                      <el-radio-group v-model="profile.deliveryMode" size="small" class="delivery-mode-group">
                        <el-radio-button label="GROUP">群组</el-radio-button>
                        <el-radio-button label="USER">点对点</el-radio-button>
                        <el-radio-button label="CLIENT">指定端</el-radio-button>
                      </el-radio-group>
                    </el-form-item>
                    <div class="profile-grid">
                      <el-form-item label="用户姓名">
                        <el-input v-model="profile.name" placeholder="用户姓名" :disabled="connected" />
                      </el-form-item>
                      <el-form-item label="用户 ID">
                        <el-input-number v-model="profile.userId" :min="1" :disabled="connected" controls-position="right" />
                      </el-form-item>
                    </div>
                    <div v-if="profile.deliveryMode === 'GROUP'" class="profile-grid">
                      <el-form-item label="群组 ID">
                        <el-input v-model="profile.groupId" placeholder="groupId" :disabled="connected" />
                      </el-form-item>
                      <el-form-item label="群组名称">
                        <el-input v-model="profile.groupName" placeholder="群组名称" :disabled="connected" />
                      </el-form-item>
                    </div>
                    <div v-else-if="profile.deliveryMode === 'USER'" class="profile-grid">
                      <el-form-item label="目标用户 ID">
                        <el-input-number v-model="profile.targetUserId" :min="1" :disabled="connected" controls-position="right" />
                      </el-form-item>
                      <el-form-item label="目标用户">
                        <el-input v-model="profile.targetUserName" placeholder="目标用户姓名" :disabled="connected" />
                      </el-form-item>
                    </div>
                    <el-form-item v-else label="目标端 clientId">
                      <el-input v-model="profile.targetClientId" placeholder="对方浏览器 Tab / 设备的 clientId" :disabled="connected" />
                    </el-form-item>
                    <div class="profile-context">
                      <div>
                        <span>租户</span>
                        <code>{{ profile.tenantId }}</code>
                      </div>
                      <div>
                        <span>部门</span>
                        <code>{{ profile.department }}</code>
                      </div>
                      <div>
                        <span>客户端标识</span>
                        <code>{{ profile.clientId }}</code>
                      </div>
                    </div>
                  </el-form>
                </el-tab-pane>

                <el-tab-pane label="链接状态" name="status">
                  <div class="connection-card">
                    <div class="connection-head">
                      <span class="status-dot" :class="`is-${status}`" />
                      <strong>{{ statusText }}</strong>
                      <el-tag size="small" effect="plain">{{ protocol || '未连接' }}</el-tag>
                    </div>
                    <dl>
                      <div v-for="item in statusRows" :key="item.label">
                        <dt>{{ item.label }}</dt>
                        <dd>{{ item.value }}</dd>
                      </div>
                    </dl>
                  </div>

                  <div class="heartbeat-box" data-testid="heartbeat-panel">
                    <div class="heartbeat-title">
                      <span>心跳记录</span>
                      <small>不进入业务消息</small>
                    </div>
                    <div v-if="heartbeatLogs.length" class="heartbeat-timeline">
                      <div
                        v-for="item in heartbeatLogs"
                        :key="item.id"
                        class="heartbeat-row"
                        :class="[`is-${item.direction}`, item.ok ? 'is-ok' : 'is-error']"
                      >
                        <span class="heartbeat-side">{{ item.direction === 'client' ? '客户端' : '服务端' }}</span>
                        <span class="heartbeat-arrow">{{ item.direction === 'client' ? '→' : '←' }}</span>
                        <span class="heartbeat-frame">{{ item.frame.toUpperCase() }}</span>
                        <span class="heartbeat-time">{{ item.time }}</span>
                      </div>
                    </div>
                    <el-empty v-else description="暂无心跳" :image-size="48" />
                  </div>
                </el-tab-pane>
              </el-tabs>

              <div class="action-row">
                <button
                  type="button"
                  class="el-button el-button--primary action-button"
                  :class="{ 'is-loading': status === 'connecting' || status === 'reconnecting', 'is-disabled': connected }"
                  :disabled="connected"
                  @click="connectRoom"
                >
                  <span>{{ status === 'connecting' || status === 'reconnecting' ? '连接中' : '连接' }}</span>
                </button>
                <button
                  type="button"
                  class="el-button action-button"
                  :class="{ 'is-disabled': !connected }"
                  :disabled="!connected"
                  @click="disconnectRoom"
                >
                  <span>断开</span>
                </button>
              </div>
            </aside>

            <div class="room-workspace">
              <aside class="room-members" data-testid="realtime-room-members">
                <header class="members-header">
                  <div>
                    <strong>房间成员</strong>
                    <span>{{ onlineMemberCount }} 在线 / {{ roomMembers.length }} 进入过</span>
                  </div>
                </header>
                <div class="member-list">
                  <div
                    v-for="member in roomMembers"
                    :key="member.clientId"
                    class="member-row"
                    :class="{ 'is-offline': !member.online }"
                  >
                    <div class="member-avatar" :class="member.avatarTone">{{ member.avatar }}</div>
                    <div class="member-info">
                      <div class="member-name">
                        <strong>{{ member.name }}</strong>
                        <span class="member-status" :class="{ 'is-online': member.online }">{{ member.online ? '在线' : '离线' }}</span>
                      </div>
                      <span>{{ member.department || '未填写部门' }}</span>
                    </div>
                  </div>
                  <el-empty v-if="!roomMembers.length" description="连接后显示成员" :image-size="54" />
                </div>
              </aside>

              <main class="chat-panel">
                <header class="chat-header">
                  <div>
                    <strong>业务消息</strong>
                    <span>{{ profile.groupName }} · {{ profile.deliveryMode }}</span>
                  </div>
                  <el-button text size="small" @click="clearMessages">清空</el-button>
                </header>

                <div ref="messageListRef" class="message-list" data-testid="realtime-message-list">
                  <div
                    v-for="message in chatMessages"
                    :key="message.localId"
                    class="message-row"
                    :class="message.direction === 'out' ? 'is-out' : 'is-in'"
                  >
                    <div class="message-avatar" :class="message.avatarTone">{{ message.avatar }}</div>
                    <button class="message-bubble" type="button" @click="openRawMessage(message.raw)">
                      <span class="message-meta">{{ message.sender }} · {{ message.time }}</span>
                      <span class="message-content">{{ message.content }}</span>
                    </button>
                  </div>
                  <el-empty v-if="!chatMessages.length" description="连接后发送一条消息" :image-size="72" />
                </div>

                <footer class="composer">
                  <el-input
                    v-model="draft"
                    type="textarea"
                    :rows="3"
                    resize="none"
                    placeholder="输入业务消息内容"
                    @keydown.enter.exact.prevent="sendRoomMessage"
                  />
                  <div class="composer-actions">
                    <span>{{ sendHint }}</span>
                    <button
                      type="button"
                      class="el-button el-button--primary action-button"
                      :class="{ 'is-disabled': !connected || !draft.trim() }"
                      :disabled="!connected || !draft.trim()"
                      @click="sendRoomMessage"
                    >
                      <span>发送</span>
                    </button>
                    <button
                      type="button"
                      class="el-button action-button"
                      :class="{ 'is-disabled': !connected }"
                      :disabled="!connected"
                      @click="publishServerMessage"
                    >
                      <span>模拟服务端消息</span>
                    </button>
                  </div>
                </footer>
              </main>
            </div>
          </div>
        </div>
        <div class="op-btns" @click="toggleCode('chat')">
          <el-icon><component :is="codeVisible.chat ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.chat ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <CodeBlock v-show="codeVisible.chat" :code="chatCode" />
      </div>
          </section>

          <section id="modes" class="doc-section api-section">
      <h2>支持模式</h2>
      <p>RealtimeClient 支持自动协商和固定协议两种接入方式。业务侧通常使用 Auto；只有在调试、灰度或网络策略明确时才固定为某一种协议。</p>
      <el-table :data="modeTable" size="small" border>
        <el-table-column prop="mode" label="模式" width="140" />
        <el-table-column prop="endpoint" label="相关地址" min-width="300" />
        <el-table-column prop="feature" label="特性" min-width="300" />
      </el-table>
          </section>

          <section id="utility" class="doc-section">
      <h2>提供方法</h2>
      <p>RealtimeClient 是正式业务通信客户端，负责协议协商、连接维护、心跳、重连、降级和统一消息收发；页面 UI 由业务模块自行组织。</p>
      <div class="demo-block" data-testid="realtime-utility-panel">
        <div class="demo-source compact-source">
          <el-table :data="methodTable" size="small" border>
            <el-table-column prop="name" label="方法" width="150" />
            <el-table-column prop="description" label="说明" min-width="260" />
            <el-table-column prop="usage" label="典型用法" min-width="220" />
          </el-table>
        </div>
        <div class="op-btns" @click="toggleCode('utility')">
          <el-icon><component :is="codeVisible.utility ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.utility ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <CodeBlock v-show="codeVisible.utility" :code="utilityCode" />
      </div>
          </section>

          <section id="options" class="doc-section api-section">
      <h2>配置项</h2>
      <el-table :data="optionsTable" size="small" border>
        <el-table-column prop="name" label="参数" width="180" />
        <el-table-column prop="description" label="说明" min-width="280" />
        <el-table-column prop="defaultValue" label="默认值" width="180" />
      </el-table>

      <h3 class="subsection-title">服务端可靠投递</h3>
      <p>业务侧调用发布接口后，服务端先写入 infra-kv Outbox，再由 dispatcher 投递到在线连接。Presence 负责找用户、端、群组在哪个节点；Outbox 负责失败重试。</p>
      <el-table :data="serverDeliveryTable" size="small" border>
        <el-table-column prop="stage" label="阶段" width="160" />
        <el-table-column prop="owner" label="组件" width="180" />
        <el-table-column prop="description" label="说明" min-width="360" />
      </el-table>

      <h3 class="subsection-title">默认接入点</h3>
      <p>业务通常不需要在页面里填写地址；如网关路径不同，可在创建 client 时通过 endpoints 覆盖。</p>
      <el-table :data="endpointTable" size="small" border>
        <el-table-column prop="name" label="用途" width="150" />
        <el-table-column prop="path" label="默认地址" min-width="280" />
        <el-table-column prop="description" label="说明" min-width="220" />
      </el-table>
          </section>

          <section id="message-format" class="doc-section api-section">
      <h2>消息协议</h2>
      <p>业务消息使用统一 envelope。ping/pong 是轻量控制帧，只用于链路保活，不套完整业务消息体，也不会交给业务 subscribe 处理。</p>
      <el-table :data="messageKindTable" size="small" border>
        <el-table-column prop="kind" label="类型" width="140" />
        <el-table-column prop="owner" label="处理方" width="150" />
        <el-table-column prop="description" label="说明" min-width="360" />
      </el-table>
      <CodeBlock :code="messageCode" language="json" />
          </section>
        </main>
      </div>

      <aside class="article-toc" aria-label="页面导航">
        <div class="article-toc-title">页面导航</div>
        <button
          v-for="item in tocItems"
          :key="item.id"
          type="button"
          :class="{ active: activeToc === item.id }"
          @click="scrollToSection(item.id)"
        >
          {{ item.label }}
        </button>
      </aside>
    </div>

    <el-dialog v-model="rawDialogVisible" title="原始消息 JSON" width="640px" append-to-body>
      <pre class="record-json">{{ selectedRawJson }}</pre>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="RealtimeView">
import { computed, defineComponent, h, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { ArrowDown, ArrowUp } from '@element-plus/icons-vue';
import { createRealtimeClient, getPayloadText, Session, type RealtimeClient, type RealtimeMessage, type RealtimeMetrics, type RealtimeProtocol, type RealtimeStatus } from '@mango/common';

type ChatDirection = 'in' | 'out';
type DeliveryMode = 'GROUP' | 'USER' | 'CLIENT';

type ChatMessage = {
  localId: string;
  direction: ChatDirection;
  sender: string;
  avatar: string;
  avatarTone: string;
  content: string;
  time: string;
  raw: RealtimeMessage;
};

type RoomMember = {
  clientId: string;
  userId?: string | number | null;
  name: string;
  department: string;
  avatar: string;
  avatarTone: string;
  online: boolean;
  lastSeenAt: number;
};

const runtimeConfig = reactive({
  endpoints: {
    negotiate: '/api/realtime/transports/negotiate',
    websocket: '/api/realtime/transports/websocket',
    sse: '/api/realtime/transports/sse',
    polling: '/api/realtime/transports/polling',
    probeWebsocket: '/api/realtime/transports/probe/websocket',
    probeSse: '/api/realtime/transports/probe/sse',
    probePolling: '/api/realtime/transports/probe/polling',
    inboundSse: '/api/realtime/messages/inbound/sse',
    inboundPolling: '/api/realtime/messages/inbound/polling',
  },
  heartbeatInterval: 30000,
  heartbeatTimeout: 5000,
  maxRetries: 6,
  pollingMaxSize: 20,
  pollingTimeoutMillis: 25000,
  pollingInterval: 1000,
});
const profile = reactive({
  tenantId: 'default',
  userId: 1001,
  name: '张三',
  department: '产品研发部',
  clientId: `browser-${Math.random().toString(16).slice(2, 8)}`,
  groupId: 'room-001',
  groupName: '订单协作群',
  targetUserId: 1001,
  targetUserName: '张三',
  targetClientId: '',
  deliveryMode: 'GROUP' as DeliveryMode,
});
const status = ref<RealtimeStatus>('idle');
const protocol = ref<RealtimeProtocol | null>(null);
const metrics = ref<RealtimeMetrics>(emptyMetrics());
const clientRef = ref<RealtimeClient | null>(null);
const chatMessages = ref<ChatMessage[]>([]);
const roomMembers = ref<RoomMember[]>([]);
const heartbeatLogs = ref<Array<{
  id: string;
  time: string;
  protocol: RealtimeProtocol;
  direction: 'client' | 'server';
  frame: 'ping' | 'pong';
  ok: boolean;
}>>([]);
const draft = ref('订单 A-1024 已完成资料补充');
const rawDialogVisible = ref(false);
const selectedRawJson = ref('');
const codeVisible = ref<Record<string, boolean>>({ chat: false, utility: false });
const activeConfigTab = ref('connection');
const activeToc = ref('chat');
const connectionMode = ref<'auto' | RealtimeProtocol>('auto');
const messageListRef = ref<HTMLElement | null>(null);
const unsubscribers: Array<() => void> = [];
const roomMemberMessageIds = new Set<string>();

const tocItems = [
  { id: 'chat', label: '聊天室用法' },
  { id: 'modes', label: '支持模式' },
  { id: 'utility', label: '提供方法' },
  { id: 'options', label: '配置项' },
  { id: 'message-format', label: '消息协议' },
];

const connected = computed(() => status.value === 'connected');
const statusText = computed(() => {
  const labels: Record<RealtimeStatus, string> = {
    idle: '未连接',
    connecting: '连接中',
    connected: '连接成功',
    reconnecting: '重连中',
    degraded: '协议降级中',
    disconnected: '已断开',
    error: '连接异常',
  };
  return labels[status.value];
});
const sendHint = computed(() => {
  if (!connected.value) return '连接后可发送';
  const targetName = profile.deliveryMode === 'GROUP'
    ? `群组 ${profile.groupId}`
    : profile.deliveryMode === 'USER'
      ? `用户 ${profile.targetUserId}`
      : `端 ${profile.targetClientId || '未填写'}`;
  if (protocol.value === 'websocket') return `WebSocket 双向发送 · ${targetName}`;
  if (protocol.value === 'sse') return `SSE 下行，HTTP inbound 上行 · ${targetName}`;
  return `Polling 下行，HTTP inbound 上行 · ${targetName}`;
});
const statusRows = computed(() => [
  { label: '当前协议', value: protocol.value || '-' },
  { label: '连接 ID', value: clientRef.value?.getSessionId() || '-' },
  { label: '心跳间隔', value: `${runtimeConfig.heartbeatInterval / 1000}s` },
  { label: '心跳次数', value: String(metrics.value.heartbeatSentCount) },
  { label: '心跳丢失', value: String(metrics.value.heartbeatMissCount) },
  { label: '重连次数', value: String(metrics.value.reconnectCount) },
  { label: '收 / 发', value: `${metrics.value.receivedCount} / ${metrics.value.sentCount}` },
]);
const onlineMemberCount = computed(() => roomMembers.value.filter(member => member.online).length);

async function connectRoom() {
  disposeClient();
  roomMembers.value = [];
  const client = createRealtimeClient({
    mode: connectionMode.value,
    endpoints: { ...runtimeConfig.endpoints },
    identity: {
      tenantId: profile.tenantId,
      userId: profile.userId,
      clientId: profile.clientId,
    },
    heartbeat: {
      interval: runtimeConfig.heartbeatInterval,
      minInterval: 1000,
      timeout: runtimeConfig.heartbeatTimeout,
      suppressEvents: true,
    },
    reconnect: {
      enabled: true,
      maxRetries: runtimeConfig.maxRetries,
      minDelay: 1000,
      maxDelay: 30000,
      factor: 2,
      jitter: true,
    },
    transportPolicy: {
      adaptive: true,
      fallbackOrder: ['websocket', 'sse', 'polling'],
      downgrade: {
        enabled: true,
        onConnectFailure: true,
        onHeartbeatTimeout: true,
        consecutiveErrors: 1,
      },
      upgrade: {
        enabled: false,
      },
    },
    polling: {
      maxSize: runtimeConfig.pollingMaxSize,
      timeoutMillis: runtimeConfig.pollingTimeoutMillis,
      interval: runtimeConfig.pollingInterval,
    },
  });
  clientRef.value = client;
  bindClient(client);
  try {
    await client.connect();
    if (client.getProtocol() === 'polling') {
      await subscribeCurrentGroup();
    }
    refreshMetrics();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : String(error));
  }
}

function bindClient(client: RealtimeClient) {
  unsubscribers.push(
    client.on('status', value => {
      status.value = value;
      protocol.value = client.getProtocol();
      refreshMetrics();
    }),
    client.on('transport-change', value => {
      protocol.value = value.to;
      refreshMetrics();
    }),
    client.on('heartbeat', value => {
      heartbeatLogs.value = [
        {
          id: `${value.at}-${value.protocol}-${value.direction}-${value.frame}`,
          time: formatTime(value.at),
          protocol: value.protocol,
          direction: value.direction,
          frame: value.frame,
          ok: value.ok,
        },
        ...heartbeatLogs.value,
      ].slice(0, 8);
      refreshMetrics();
    }),
    client.on('message', message => {
      if (isRoomMemberAck(message)) {
        refreshMetrics();
        return;
      }
      handleRoomMemberMessage(message);
      if (isRoomMemberEvent(message)) {
        refreshMetrics();
        return;
      }
      rememberMemberFromMessage(message, true);
      chatMessages.value.push(toChatMessage(message, 'in'));
      scrollMessagesToBottom();
      refreshMetrics();
    }),
    client.on('sent', message => {
      if (message.event?.name === 'subscription.subscribe' || message.event?.name === 'subscription.unsubscribe') return;
      if (isRoomMemberEvent(message)) {
        handleRoomMemberMessage(message);
        refreshMetrics();
        return;
      }
      rememberMemberFromMessage(message, true);
      chatMessages.value.push(toChatMessage(message, 'out'));
      scrollMessagesToBottom();
      refreshMetrics();
    }),
    client.on('system-message', message => {
      if (message.event?.name === 'connection.connected') {
        void subscribeCurrentGroup().catch(error => ElMessage.error(error instanceof Error ? error.message : String(error)));
      }
      refreshMetrics();
    }),
    client.on('error', error => {
      ElMessage.error(error.message);
      refreshMetrics();
    }),
  );
}

async function subscribeCurrentGroup() {
  if (!clientRef.value || !profile.groupId.trim()) return;
  await clientRef.value.send({
    event: { domain: 'system', name: 'subscription.subscribe' },
    target: { type: 'GROUP', id: profile.groupId.trim() },
    metadata: {
      groupName: profile.groupName,
    },
    payload: { type: 'text', text: profile.groupName },
    ack: { required: true },
  });
  upsertRoomMember(currentRoomMember(true));
  await announceRoomMember('member.joined');
}

function currentTarget() {
  if (profile.deliveryMode === 'USER') {
    return { type: 'USER' as const, id: String(profile.targetUserId) };
  }
  if (profile.deliveryMode === 'CLIENT') {
    return { type: 'CLIENT' as const, id: profile.targetClientId.trim() || profile.clientId };
  }
  return { type: 'GROUP' as const, id: profile.groupId.trim() };
}

function disconnectRoom() {
  if (connected.value) {
    void announceRoomMember('member.left').finally(() => {
      markCurrentMemberOffline();
      clientRef.value?.disconnect('manual');
      refreshMetrics();
    });
    return;
  }
  clientRef.value?.disconnect('manual');
  refreshMetrics();
}

async function sendRoomMessage() {
  const content = draft.value.trim();
  if (!content || !clientRef.value) return;
  await clientRef.value.send({
    event: { domain: 'chat', name: 'message.send' },
    target: currentTarget(),
    metadata: {
      roomId: profile.groupId,
      roomName: profile.groupName,
      senderName: profile.name,
      department: profile.department,
      senderClientId: profile.clientId,
      senderUserId: profile.userId,
    },
    payload: { type: 'text', text: content },
    ack: { required: true },
  });
  draft.value = '';
  refreshMetrics();
}

async function publishServerMessage() {
  if (!connected.value) return;
  const payload = {
    version: '1.0',
    event: { domain: 'chat', name: 'message.delivered' },
    source: { platform: 'server' },
    context: {
      tenantId: profile.tenantId,
      userId: profile.userId,
    },
    target: currentTarget(),
    metadata: {
      roomId: profile.groupId,
      roomName: profile.groupName,
      senderName: '系统通知',
      department: profile.department,
    },
    payload: { type: 'text', text: `服务端消息：${profile.name} 已加入 ${profile.groupName}` },
    timestamp: new Date().toISOString(),
  };
  const response = await fetch('/api/realtime/messages/publish', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  if (!response.ok) throw new Error(`发布失败：${response.status}`);
  scrollMessagesToBottom();
}

function toChatMessage(message: RealtimeMessage, direction: ChatDirection): ChatMessage {
  const metadata = message.metadata || {};
  const sender = String(metadata.senderName || (direction === 'out' ? profile.name : '服务器'));
  const avatarSeed = String(metadata.senderClientId || message.source?.clientId || metadata.senderUserId || message.context?.userId || sender);
  return {
    localId: message.id || `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    direction,
    sender,
    avatar: firstDisplayChar(sender, direction === 'out' ? '我' : '服'),
    avatarTone: avatarToneClass(avatarSeed),
    content: getPayloadText(message),
    time: formatTime(Date.now()),
    raw: message,
  };
}

function firstDisplayChar(value: unknown, fallback: string) {
  const text = String(value || '').trim();
  return Array.from(text)[0] || fallback;
}

function openRawMessage(message: RealtimeMessage) {
  selectedRawJson.value = JSON.stringify(message, null, 2);
  rawDialogVisible.value = true;
}

function clearMessages() {
  chatMessages.value = [];
}

async function announceRoomMember(eventName: 'member.joined' | 'member.present' | 'member.left') {
  if (!clientRef.value || !profile.groupId.trim()) return;
  const online = eventName !== 'member.left';
  const id = createLocalMessageId();
  roomMemberMessageIds.add(id);
  await clientRef.value.send({
    id,
    event: { domain: 'chat', name: eventName },
    target: { type: 'GROUP', id: profile.groupId.trim() },
    metadata: {
      roomId: profile.groupId,
      roomName: profile.groupName,
      member: currentRoomMember(online),
      senderName: profile.name,
      department: profile.department,
      senderClientId: profile.clientId,
      senderUserId: profile.userId,
    },
    payload: { type: 'text', text: profile.name },
    ack: { required: false },
  });
}

function handleRoomMemberMessage(message: RealtimeMessage) {
  if (!isRoomMemberEvent(message)) return;
  const member = memberFromMessage(message);
  if (!member) return;
  upsertRoomMember({
    ...member,
    online: message.event?.name !== 'member.left',
    lastSeenAt: Date.now(),
  });
  if (message.event?.name === 'member.joined' && member.clientId !== profile.clientId) {
    void announceRoomMember('member.present').catch(error => ElMessage.error(error instanceof Error ? error.message : String(error)));
  }
}

function rememberMemberFromMessage(message: RealtimeMessage, online: boolean) {
  const metadata = message.metadata || {};
  const clientId = String(metadata.senderClientId || message.source?.clientId || '');
  if (!clientId) return;
  upsertRoomMember({
    clientId,
    userId: metadata.senderUserId || message.context?.userId,
    name: String(metadata.senderName || '成员'),
    department: String(metadata.department || ''),
    avatar: firstDisplayChar(metadata.senderName || '成员', '员'),
    avatarTone: avatarToneClass(clientId),
    online,
    lastSeenAt: Date.now(),
  });
}

function isRoomMemberEvent(message: RealtimeMessage) {
  return message.event?.domain === 'chat'
    && ['member.joined', 'member.present', 'member.left'].includes(message.event?.name || '');
}

function isRoomMemberAck(message: RealtimeMessage) {
  const messageId = message.ack?.messageId;
  if (!messageId || !roomMemberMessageIds.has(messageId)) return false;
  roomMemberMessageIds.delete(messageId);
  return true;
}

function memberFromMessage(message: RealtimeMessage): RoomMember | null {
  const rawMember = message.metadata?.member;
  if (!rawMember || typeof rawMember !== 'object') {
    return null;
  }
  const member = rawMember as Partial<RoomMember>;
  const clientId = String(member.clientId || '');
  if (!clientId) return null;
  const name = String(member.name || '成员');
  return {
    clientId,
    userId: member.userId,
    name,
    department: String(member.department || ''),
    avatar: firstDisplayChar(member.avatar || name, '员'),
    avatarTone: avatarToneClass(clientId || member.userId || name),
    online: member.online !== false,
    lastSeenAt: Number(member.lastSeenAt || Date.now()),
  };
}

function currentRoomMember(online: boolean): RoomMember {
  return {
    clientId: profile.clientId,
    userId: profile.userId,
    name: profile.name || '我',
    department: profile.department,
    avatar: firstDisplayChar(profile.name || '我', '我'),
    avatarTone: avatarToneClass(profile.clientId || profile.userId || profile.name),
    online,
    lastSeenAt: Date.now(),
  };
}

function avatarToneClass(seed: unknown) {
  const text = String(seed || 'default');
  let hash = 0;
  for (const char of text) {
    hash = (hash * 31 + char.charCodeAt(0)) >>> 0;
  }
  return `avatar-tone-${hash % 8}`;
}

function upsertRoomMember(member: RoomMember) {
  const index = roomMembers.value.findIndex(item => item.clientId === member.clientId);
  if (index >= 0) {
    roomMembers.value[index] = { ...roomMembers.value[index], ...member };
  } else {
    roomMembers.value.push(member);
  }
  roomMembers.value = [...roomMembers.value].sort((a, b) => Number(b.online) - Number(a.online) || b.lastSeenAt - a.lastSeenAt);
}

function markCurrentMemberOffline() {
  const current = roomMembers.value.find(member => member.clientId === profile.clientId);
  if (current) {
    upsertRoomMember({ ...current, online: false, lastSeenAt: Date.now() });
  }
}

function createLocalMessageId() {
  return `room-member-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
}

function scrollMessagesToBottom() {
  void nextTick(() => {
    const element = messageListRef.value;
    if (!element) return;
    element.scrollTop = element.scrollHeight;
  });
}

function refreshMetrics() {
  if (!clientRef.value) return;
  metrics.value = clientRef.value.getMetrics();
  protocol.value = clientRef.value.getProtocol();
}

function disposeClient() {
  while (unsubscribers.length) unsubscribers.pop()?.();
  if (connected.value) {
    void announceRoomMember('member.left');
    markCurrentMemberOffline();
  }
  clientRef.value?.disconnect('dispose');
  clientRef.value = null;
  status.value = 'idle';
  protocol.value = null;
  metrics.value = emptyMetrics();
  heartbeatLogs.value = [];
  roomMemberMessageIds.clear();
}

function toggleCode(key: string) {
  codeVisible.value[key] = !codeVisible.value[key];
}

function scrollToSection(id: string) {
  activeToc.value = id;
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function emptyMetrics(): RealtimeMetrics {
  return {
    protocol: null,
    status: 'idle',
    reconnectCount: 0,
    receivedCount: 0,
    sentCount: 0,
    heartbeatSentCount: 0,
    heartbeatMissCount: 0,
    transportSwitchCount: 0,
  };
}

function formatTime(value: number) {
  return new Date(value).toLocaleTimeString('zh-CN', { hour12: false });
}

function syncProfileFromSession() {
  const userInfo = Session.get('userInfo') || {};
  const sessionTenantId = userInfo.tenantId || Session.get('tenantId');
  const sessionUserId = userInfo.userId || userInfo.id;
  if (sessionTenantId) profile.tenantId = String(sessionTenantId);
  if (sessionUserId != null) profile.userId = Number(sessionUserId);
  if (userInfo.nickname || userInfo.username) profile.name = String(userInfo.nickname || userInfo.username);
  profile.targetUserId = profile.userId;
  profile.targetUserName = profile.name;
  if (userInfo.department || userInfo.deptName || userInfo.orgName) {
    profile.department = String(userInfo.department || userInfo.deptName || userInfo.orgName);
  }
}

onMounted(syncProfileFromSession);
onBeforeUnmount(disposeClient);

const chatCode = `<script setup lang="ts">
import { useRealtime } from '@mango/common';

const realtime = useRealtime({
  mode: 'auto',
  identity: { tenantId: 'default', userId: 1001, clientId: 'order-page' },
  heartbeat: { interval: 30000, minInterval: 1000, suppressEvents: true },
  reconnect: { enabled: true, maxRetries: 6, minDelay: 1000, maxDelay: 30000 },
  transportPolicy: {
    adaptive: true,
    fallbackOrder: ['websocket', 'sse', 'polling'],
    downgrade: { enabled: true, onConnectFailure: true, onHeartbeatTimeout: true },
    upgrade: { enabled: false }
  }
});

realtime.subscribe('chat.message', message => {
  // 渲染业务消息
});
realtime.subscribe('message.accepted', message => {
  // 渲染服务端接收回执
});

await realtime.connect();
await realtime.send({
  event: { domain: 'system', name: 'subscription.subscribe' },
  target: { type: 'GROUP', id: 'room-001' },
  payload: { type: 'text', text: '订单协作群' },
  ack: { required: true }
});
await realtime.send({
  event: { domain: 'chat', name: 'message.send' },
  target: { type: 'GROUP', id: 'room-001' },
  metadata: { roomId: 'room-001', roomName: '订单协作群', senderName: '张三' },
  payload: { type: 'text', text: 'hello' },
  ack: { required: true }
});
<\/script>`;

const utilityCode = `import { createRealtimeClient } from '@mango/common';

const client = createRealtimeClient({
  mode: 'auto',
  identity: { tenantId, userId, clientId },
  endpoints: {
    negotiate: '/api/realtime/transports/negotiate',
    websocket: '/api/realtime/transports/websocket',
    sse: '/api/realtime/transports/sse',
    polling: '/api/realtime/transports/polling'
  },
  heartbeat: { interval: 30000, minInterval: 1000 },
  reconnect: { enabled: true },
  transportPolicy: { adaptive: true }
});

const off = client.subscribe('order.updated', refreshOrder);
client.on('reconnecting', ({ retryCount }) => showReconnecting(retryCount));
client.on('transport-change', ({ to }) => updateProtocolBadge(to));

await client.connect();
await client.send({
  event: { domain: 'workflow', name: 'order.updated' },
  metadata: { orderId },
  payload: { type: 'text', text: orderId },
  ack: { required: true }
});

// 页面卸载
 off();
client.disconnect();`;

const messageCode = `{
  "id": "01JV8A4EJ5N6P7Q8R9S0T1U2V3",
  "version": "1.0",
  "event": {
    "domain": "chat",
    "name": "message.send"
  },
  "source": {
    "platform": "web",
    "clientId": "browser-xxxxxx",
    "sessionId": "websocket-browser-xxxxxx"
  },
  "context": {
    "tenantId": "default",
    "userId": 1001,
    "traceId": "trace-001",
    "requestId": "req-001"
  },
  "target": {
    "type": "GROUP",
    "id": "room-001"
  },
  "metadata": {
    "roomId": "room-001",
    "roomName": "订单协作群",
    "senderName": "张三"
  },
  "payload": {
    "type": "text",
    "text": "订单 A-1024 已完成资料补充"
  },
  "ack": {
    "required": true
  },
  "sequence": 10001,
  "timestamp": "2026-05-21T00:31:58.875Z"
}`;

const messageKindTable = [
  { kind: 'heartbeat.ping', owner: 'RealtimeClient', description: '内部心跳语义。WebSocket 实际发送轻量帧 {"type":"ping"}，不发送完整业务 envelope；SSE/Polling 依赖流或轮询请求判断活性。' },
  { kind: 'heartbeat.pong', owner: 'RealtimeClient', description: '内部心跳响应语义。WebSocket 收到 {"type":"pong"} 后清除心跳超时；不会进入业务消息列表和 subscribe 回调。' },
  { kind: 'connection.connected', owner: 'RealtimeClient / 系统 UI', description: '连接建立通知，默认作为系统事件处理，不进入业务 subscribe。' },
  { kind: 'message.accepted', owner: '业务模块', description: '服务端接收确认，payload.message 可用于展示“我收到你发送的消息”。' },
  { kind: 'message.error', owner: 'RealtimeClient / 系统 UI', description: '协议层错误消息，如鉴权失败、消息格式错误、会话失效；默认走系统事件和错误处理。' },
  { kind: 'message.send', owner: '业务模块', description: '业务上行消息，使用 event.domain + event.name 表达业务语义，例如 chat/message.send、workflow/order.updated。' },
];

const methodTable = [
  { name: 'connect()', description: '协商协议并建立连接；mode=auto 时先请求 negotiate', usage: '页面进入或用户点击连接' },
  { name: 'disconnect()', description: '主动断开连接并停止心跳、轮询、升级探测', usage: '页面卸载或退出业务房间' },
  { name: 'send(envelope)', description: '发送 v1 envelope；WebSocket 直发，SSE/Polling 自动走 inbound HTTP', usage: '发送聊天、审批、协作事件' },
  { name: 'target', description: '消息 envelope 的投递目标，支持 USER、CLIENT、CONNECTION、GROUP、TENANT、BROADCAST；CLIENT 表示某个具体浏览器 Tab / 设备端', usage: '{ type: "GROUP", id: roomId }' },
  { name: 'subscribe(eventName)', description: '按 event.name 订阅业务消息；心跳、连接通知、错误由客户端内部处理', usage: 'subscribe("order.updated", handler)' },
  { name: 'on(event)', description: '监听状态、错误、重连、协议切换、心跳等生命周期事件', usage: 'on("reconnecting", handler)' },
  { name: 'getMetrics()', description: '读取连接、收发、心跳、协议切换计数', usage: '状态栏或埋点' },
];

const modeTable = [
  {
    mode: 'Auto',
    endpoint: '/api/realtime/transports/negotiate',
    feature: '先协商推荐协议，再按 WebSocket、SSE、Ajax Polling 顺序连接；连接失败或心跳异常时可自动降级。',
  },
  {
    mode: 'WebSocket',
    endpoint: '/api/realtime/transports/websocket',
    feature: '双向长连接，适合高频互动；业务消息通过同一连接上下行，心跳用于检测连接可用性。',
  },
  {
    mode: 'SSE',
    endpoint: '/api/realtime/transports/sse；上行：/api/realtime/messages/inbound/sse',
    feature: '服务端单向推送，浏览器兼容性好；客户端发送消息时自动转为 HTTP inbound。',
  },
  {
    mode: 'Ajax Polling',
    endpoint: '/api/realtime/transports/polling；上行：/api/realtime/messages/inbound/polling',
    feature: '作为兜底协议，适合代理或网络限制场景；支持长轮询超时、批量大小和轮询间隔配置。',
  },
];

const optionsTable = [
  { name: 'mode', description: 'auto 自动协商，或固定 websocket / sse / polling', defaultValue: 'auto' },
  { name: 'identity', description: 'tenantId、userId、clientId、sessionId；连接和上行消息都会携带', defaultValue: '当前会话 + browser id' },
  { name: 'target', description: '投递目标：点对点 USER、指定端 CLIENT、连接 CONNECTION、群组 GROUP、租户 TENANT、广播 BROADCAST', defaultValue: '由业务指定' },
  { name: 'endpoints', description: '覆盖 negotiate、websocket、sse、polling、inboundSse、inboundPolling 地址', defaultValue: '/api/realtime/...' },
  { name: 'heartbeat', description: '心跳开关、间隔、超时和最小间隔；普通模式最小 1000ms，aggressive 可到 500ms', defaultValue: '30s' },
  { name: 'reconnect', description: '断线后指数退避重连，支持最大次数、最小/最大延迟和 jitter', defaultValue: '启用' },
  { name: 'transportPolicy', description: '开启动态降级；可选开启升级探测。默认降级开、升级关', defaultValue: 'adaptive=true' },
  { name: 'polling', description: '长轮询单次最大条数、超时和短轮询间隔', defaultValue: '20 / 25000ms' },
  { name: 'performanceMode', description: 'normal 或 aggressive；aggressive 允许 500ms 心跳下限', defaultValue: 'normal' },
  { name: 'serverDelivery', description: '服务端发布默认走 infra-kv Outbox：入箱、claim、投递、ack/nack、失败重试', defaultValue: 'enabled' },
];

const serverDeliveryTable = [
  { stage: '在线路由', owner: 'Presence', description: '连接建立后写入 infra-kv，按 tenant/user/client/group 建立 SortedSet 索引，支持集群查找目标连接。' },
  { stage: '可靠入箱', owner: 'IOutboxPublisher', description: '/realtime/messages/publish 先写 Outbox，不直接依赖当前请求完成最终投递。' },
  { stage: '投递执行', owner: 'RealtimeOutboxDispatcher', description: '后台 worker claim 待投递消息，调用 RealtimePublishService 下发到本节点和远端节点。' },
  { stage: '失败处理', owner: 'IOutboxStore', description: '成功 ack；异常 nack 并设置 nextAttemptAt。语义为 at-least-once，业务可用 message id 做幂等。' },
];

const endpointTable = [
  { name: '协议协商', path: '/api/realtime/transports/negotiate', description: 'Auto 模式使用，返回推荐协议和可用传输能力' },
  { name: 'WebSocket', path: '/api/realtime/transports/websocket', description: '双向连接，消息可直接通过连接上行' },
  { name: 'SSE', path: '/api/realtime/transports/sse', description: '服务端下行通道，上行走 inbound HTTP' },
  { name: 'Ajax Polling', path: '/api/realtime/transports/polling', description: '长轮询下行通道，上行走 inbound HTTP' },
  { name: 'SSE 上行', path: '/api/realtime/messages/inbound/sse', description: 'SSE 模式发送业务消息' },
  { name: 'Polling 上行', path: '/api/realtime/messages/inbound/polling', description: 'Polling 模式发送业务消息' },
  { name: '服务端发布', path: '/api/realtime/messages/publish', description: '服务端业务发布入口；默认先入 infra-kv Outbox，再由 dispatcher 投递' },
];

const CodeBlock = defineComponent({
  name: 'CodeBlock',
  props: {
    code: {
      type: String,
      required: true,
    },
    language: {
      type: String,
      default: 'ts',
    },
  },
  setup(props) {
    return () => h('pre', { class: 'demo-code' }, [h('code', props.code)]);
  },
});
</script>

<style scoped lang="scss">
@use './demo-page.scss';

.demo-block {
  width: 100%;
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 3px;
  background: var(--el-bg-color);
  transition: box-shadow 0.2s ease;

  &:hover {
    box-shadow: 0 0 8px 0 rgb(232 237 250 / 60%), 0 2px 4px 0 rgb(232 237 250 / 50%);
  }
}

.demo-source {
  padding: 24px;
}

.compact-source {
  padding: 20px 24px 24px;
}

.realtime-shell {
  display: grid;
  grid-template-columns: minmax(280px, 360px) minmax(0, 1fr);
  align-items: start;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  overflow: hidden;
  background: var(--el-fill-color-blank);
}

.realtime-sidebar {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  padding: 18px;
  border-right: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-lighter);
}

.sidebar-title {
  display: flex;
  flex-direction: column;
  gap: 4px;

  strong {
    color: var(--el-text-color-primary);
    font-size: 16px;
  }

  span {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }
}

.config-tabs {
  min-height: 0;
  flex: 1;

  :deep(.el-tabs__header) {
    margin-bottom: 12px;
  }

  :deep(.el-tabs__content) {
    height: calc(100% - 52px);
    overflow: auto;
  }
}

.profile-form,
.runtime-form {
  display: flex;
  flex-direction: column;
  gap: 8px;

  :deep(.el-form-item) {
    margin-bottom: 0;
  }

  :deep(.el-form-item__label) {
    margin-bottom: 4px;
    line-height: 18px;
    font-size: 12px;
  }

  :deep(.el-input__wrapper),
  :deep(.el-input-number .el-input__wrapper) {
    min-height: 30px;
  }

  :deep(.el-input__inner) {
    height: 30px;
    line-height: 30px;
  }

  :deep(.el-input-number) {
    width: 100%;
  }
}

.profile-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 10px;
}

.delivery-mode-group {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  width: 100%;

  :deep(.el-radio-button__inner) {
    width: 100%;
    padding-inline: 0;
  }
}

.profile-context {
  display: grid;
  gap: 6px;
  margin-top: 4px;

  div {
    display: grid;
    grid-template-columns: 68px minmax(0, 1fr);
    gap: 8px;
    align-items: center;
    min-height: 28px;
    padding: 5px 8px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 4px;
    background: var(--el-fill-color-blank);
  }

  span {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  code {
    min-width: 0;
    color: var(--el-text-color-primary);
    font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
    font-size: 12px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.runtime-form {
  :deep(.el-radio-group) {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    width: 100%;
  }

  :deep(.el-radio-button__inner) {
    width: 100%;
    padding-inline: 0;
  }
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 8px;

  &.compact {
    margin-top: 4px;
  }
}

.info-row {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 10px;
  align-items: start;
  padding: 9px 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  background: var(--el-bg-color);

  span {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  code {
    min-width: 0;
    color: var(--el-text-color-primary);
    font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
    font-size: 12px;
    line-height: 1.5;
    overflow-wrap: anywhere;
  }
}

.runtime-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 10px;
}

.connection-card {
  padding: 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  background: var(--el-bg-color);

  dl {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
    margin: 14px 0 0;
  }

  dt {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  dd {
    margin: 4px 0 0;
    color: var(--el-text-color-primary);
    font-size: 14px;
    font-weight: 600;
  }
}

.connection-head {
  display: flex;
  align-items: center;
  gap: 8px;

  strong {
    flex: 1;
    min-width: 0;
  }
}

.status-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--el-color-info);

  &.is-connected {
    background: var(--el-color-success);
  }

  &.is-connecting,
  &.is-reconnecting,
  &.is-degraded {
    background: var(--el-color-warning);
  }

  &.is-error {
    background: var(--el-color-danger);
  }
}

.action-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.action-button {
  justify-content: center;
  min-height: 32px;
  margin-left: 0;

  &.is-disabled {
    cursor: not-allowed;
  }
}

.heartbeat-box {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  height: 184px;
  margin-top: 12px;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  background: var(--el-bg-color);
  overflow: hidden;
}

.heartbeat-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 24px;
  margin-bottom: 8px;

  span {
    font-weight: 600;
  }

  small {
    color: var(--el-text-color-secondary);
  }
}

.heartbeat-timeline {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 0;
  padding-right: 4px;
  color: var(--el-text-color-regular);
  font-size: 12px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: var(--el-border-color) transparent;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-thumb {
    border-radius: 999px;
    background: var(--el-border-color);
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }
}

.heartbeat-row {
  display: grid;
  grid-template-columns: 54px 26px minmax(46px, 1fr) auto;
  gap: 6px;
  align-items: center;
  min-height: 30px;
  padding: 6px 8px 6px 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  background: var(--el-fill-color-lighter);

  &.is-server {
    background: var(--el-fill-color-blank);
  }

  &.is-ok .heartbeat-arrow {
    color: var(--el-color-success);
    background: var(--el-color-success-light-9);
  }

  &.is-error .heartbeat-arrow {
    color: var(--el-color-danger);
    background: var(--el-color-danger-light-9);
  }
}

.heartbeat-side,
.heartbeat-time {
  color: var(--el-text-color-secondary);
}

.heartbeat-arrow {
  display: inline-grid;
  place-items: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  font-size: 16px;
  font-weight: 700;
  text-align: center;
}

.heartbeat-frame {
  color: var(--el-text-color-primary);
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 12px;
  font-weight: 700;
}

.chat-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  height: 520px;
  min-width: 0;
  background: var(--el-bg-color);
}

.room-workspace {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  min-width: 0;
  height: 520px;
  background: var(--el-bg-color);
}

.room-members {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-width: 0;
  border-right: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-light);
}

.members-header {
  padding: 16px 14px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);

  div {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  strong {
    color: var(--el-text-color-primary);
    font-size: 15px;
  }

  span {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }
}

.member-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
  padding: 12px;
  overflow-y: auto;
}

.member-row {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  min-height: 48px;
  padding: 8px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  background: var(--el-bg-color);

  &.is-offline {
    opacity: 0.62;
  }
}

.member-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #e8f3ff;
  color: #155bd4;
  font-size: 13px;
  font-weight: 700;
}

.member-info {
  min-width: 0;

  > span {
    display: block;
    margin-top: 3px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.member-name {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;

  strong {
    min-width: 0;
    color: var(--el-text-color-primary);
    font-size: 13px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.member-status {
  flex: 0 0 auto;
  padding: 1px 6px;
  border-radius: 999px;
  background: var(--el-fill-color);
  color: var(--el-text-color-secondary);
  font-size: 11px;
  line-height: 18px;

  &.is-online {
    background: var(--el-color-success-light-9);
    color: var(--el-color-success);
  }
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border-bottom: 1px solid var(--el-border-color-lighter);

  div {
    display: flex;
    flex-direction: column;
    gap: 3px;
  }

  strong {
    font-size: 16px;
  }

  span {
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  padding: 16px 18px;
  overflow: auto;
  background: linear-gradient(180deg, var(--el-fill-color-lighter), var(--el-bg-color));
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  max-width: min(76%, 560px);

  &.is-out {
    align-self: flex-end;
    flex-direction: row-reverse;
  }

  &.is-in {
    align-self: flex-start;
  }
}

.message-avatar {
  display: inline-flex;
  flex: 0 0 32px;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e8f3ff;
  color: #155bd4;
  font-size: 12px;
  font-weight: 700;
}

.member-avatar,
.message-avatar {
  &.avatar-tone-0 {
    background: #e8f3ff;
    color: #155bd4;
  }

  &.avatar-tone-1 {
    background: #e8fbf2;
    color: #0d7a52;
  }

  &.avatar-tone-2 {
    background: #fff3db;
    color: #a65f00;
  }

  &.avatar-tone-3 {
    background: #f1e8ff;
    color: #6d3fc8;
  }

  &.avatar-tone-4 {
    background: #ffe9ef;
    color: #b4234b;
  }

  &.avatar-tone-5 {
    background: #e7f8fb;
    color: #087184;
  }

  &.avatar-tone-6 {
    background: #f4f0e8;
    color: #70512c;
  }

  &.avatar-tone-7 {
    background: #eef2ff;
    color: #3d4fb5;
  }
}

.message-bubble {
  display: inline-flex;
  flex-direction: column;
  min-width: 0;
  gap: 6px;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
  text-align: left;
  cursor: pointer;

  .message-row.is-out & {
    border-color: var(--el-color-primary-light-7);
    background: var(--el-color-primary-light-9);
  }
}

.message-meta {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.message-content {
  word-break: break-word;
  line-height: 1.6;
}

.composer {
  padding: 12px 18px 14px;
  border-top: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);

  :deep(.el-textarea__inner) {
    min-height: 64px !important;
  }
}

.composer-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;

  span {
    margin-right: auto;
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }
}

.op-btns {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 44px;
  border-top: 1px solid var(--el-border-color-lighter);
  color: var(--el-text-color-regular);
  font-size: 14px;
  font-weight: 500;
  background: var(--el-bg-color);
  cursor: pointer;
  transition: color 0.2s ease, background-color 0.2s ease;

  &:hover {
    color: var(--el-color-primary);
    background-color: var(--el-fill-color-light);
  }
}

.demo-code,
.record-json {
  margin: 0;
  padding: 18px 24px;
  overflow: auto;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-primary);
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  line-height: 1.8;
  white-space: pre;
}

.demo-code {
  border-top: 1px solid var(--el-border-color-lighter);
}

.api-section {
  :deep(.el-table) {
    margin-top: 12px;
  }
}

.subsection-title {
  margin: 24px 0 8px;
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 600;
}

@media (max-width: 900px) {
  .realtime-shell {
    grid-template-columns: 1fr;
  }

  .realtime-sidebar {
    border-right: 0;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }

  .config-tabs {
    flex: none;

    :deep(.el-tabs__content) {
      max-height: 360px;
    }
  }

  .room-workspace {
    grid-template-columns: 1fr;
    height: 480px;
  }

  .room-members {
    grid-template-rows: auto auto;
    border-right: 0;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }

  .member-list {
    flex-direction: row;
    overflow-x: auto;
    overflow-y: hidden;
  }

  .member-row {
    min-width: 180px;
  }

  .chat-panel {
    height: auto;
    min-height: 0;
  }
}

@media (max-width: 760px) {
  .demo-source,
  .compact-source,
  .demo-code {
    padding: 16px;
  }

  .message-row {
    max-width: 92%;
  }

  .composer-actions {
    align-items: stretch;
    flex-direction: column;

    span {
      margin-right: 0;
    }
  }
}
</style>
