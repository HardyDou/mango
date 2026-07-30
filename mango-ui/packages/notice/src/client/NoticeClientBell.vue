<template>
  <el-popover placement="bottom-end" width="360" trigger="click" @show="loadMessages">
    <template #reference>
      <el-badge :value="badgeValue" :hidden="unreadCount === 0" class="notice-bell">
        <button type="button" class="notice-bell__trigger" aria-label="消息提醒">
          <el-icon :size="20"><BellIcon /></el-icon>
        </button>
      </el-badge>
    </template>
    <div class="notice-bell__panel">
      <div class="notice-bell__header">
        <span>我的消息</span>
        <el-button link type="primary" @click="markAllRead">全部已读</el-button>
      </div>
      <div v-if="grouped" class="notice-bell__categories">
        <button
          v-for="item in visibleCategoryStats"
          :key="item.category"
          type="button"
          class="notice-bell__category"
          :data-test="`notice-category-${item.category.toLowerCase()}`"
          @click="viewCategory(item.category)"
        >
          <span>{{ categoryLabel(item.category) }}</span>
          <span>（{{ item.count }}条）</span>
        </button>
      </div>
      <el-empty v-else-if="messages.length === 0" description="暂无消息" :image-size="60" />
      <div v-for="message in grouped ? [] : messages" :key="message.id" class="notice-bell__item" @click="openDetail(message.id)">
        <div class="notice-bell__avatar">{{ bizAvatar(message) }}</div>
        <div class="notice-bell__body">
          <!-- 标题和摘要可能包含基础富文本，必须先按通知白名单清洗再渲染。 -->
          <!-- eslint-disable-next-line vue/no-v-html -->
          <div class="notice-bell__title" v-html="sanitizeNoticeHtml(message.title || '未命名消息')" />
          <!-- eslint-disable-next-line vue/no-v-html -->
          <div class="notice-bell__summary" v-html="sanitizeNoticeHtml(message.content || '暂无内容')" />
          <div class="notice-bell__meta">{{ bizDisplayName(message) }} · {{ message.createTime || '-' }}</div>
        </div>
      </div>
      <div class="notice-bell__footer">
        <el-button link type="primary" data-test="view-all-button" @click="viewAllMessages">查看全部</el-button>
        <el-button link type="primary" data-test="settings-button" @click="openReceiveSetting">接收设置</el-button>
      </div>
    </div>
  </el-popover>
  <NoticeDetailDialog v-model="detailVisible" :message="currentMessage" @action="handleDetailAction" />
</template>

<script setup lang="ts">
import { computed, h, onMounted, onUnmounted, ref } from 'vue';
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus';
import { iconMap, type RealtimeOptions } from '@mango/common';
import {
  executeMySiteMessageAction,
  getMySiteMessageDetail,
  getMySiteMessages,
  getMyUnreadCategoryStats,
  getMyUnreadCount,
  markAllMySiteMessagesRead,
  markMySiteMessageRead,
} from '../api/notice';
import NoticeDetailDialog from '../components/NoticeDetailDialog.vue';
import {
  createNoticeRealtime,
  playNoticeSound,
  requestDesktopPermission,
  showDesktopNotice,
  speakNoticeText,
} from '../realtime/noticeRealtime';
import type { NoticeRealtimeEvent } from '../realtime/noticeRealtime';
import type { NoticeSiteMessage, NoticeSiteMessageAction } from '../types/notice';
import type { NoticeSiteMessageCategory, NoticeUnreadCategoryCount } from '../types/notice';
import { noticePlainText, sanitizeNoticeHtml } from './html';
import { buildNoticeActionInput, buildNoticeInteraction } from './interaction';
import { presentNoticeMessage } from './messagePresentation';
import type { NoticeInteractionPayload } from './interaction';
import type { NoticeBellViewAllOptions, NoticeClientBellRuntimeConfig } from './types';

const props = withDefaults(
  defineProps<{
    enableRealtime?: boolean;
    enablePolling?: boolean;
    pollingInterval?: number;
    pageSize?: number;
    runtimeConfig?: NoticeClientBellRuntimeConfig;
    loadRuntimeConfig?: () => Promise<NoticeClientBellRuntimeConfig> | NoticeClientBellRuntimeConfig;
    realtimeOptions?: RealtimeOptions;
  }>(),
  {
    enableRealtime: true,
    enablePolling: true,
    pollingInterval: 30000,
    pageSize: 5,
  },
);

const emit = defineEmits<{
  (event: 'view-all', options?: NoticeBellViewAllOptions): void;
  (event: 'settings'): void;
  (event: 'unread-change', count: number): void;
  (event: 'message-open', message: NoticeSiteMessage): void;
  (event: 'message-received', message: NoticeSiteMessage): void;
  (event: 'interaction', payload: NoticeInteractionPayload): void;
}>();

const unreadCount = ref(0);
const messages = ref<NoticeSiteMessage[]>([]);
const categoryStats = ref<NoticeUnreadCategoryCount[]>([]);
const currentMessage = ref<NoticeSiteMessage>();
const detailVisible = ref(false);
const badgeValue = computed(() => (unreadCount.value > 99 ? '99+' : unreadCount.value));
const grouped = ref(false);
const visibleCategoryStats = computed(() => categoryStats.value.filter((item) => item.count > 0));
const BellIcon = iconMap.Bell;
let stopRealtime: (() => void) | undefined;
let pollingTimer: number | undefined;

function updateUnreadCount(count: number, options: { forceEmit?: boolean } = {}) {
  const next = Math.max(0, Math.trunc(count));
  const changed = unreadCount.value !== next;
  unreadCount.value = next;
  if (changed || options.forceEmit) {
    emit('unread-change', unreadCount.value);
  }
}

async function loadUnreadCount() {
  try {
    const result = await getMyUnreadCount();
    updateUnreadCount(result.count || 0, { forceEmit: true });
  } catch {
    updateUnreadCount(0, { forceEmit: true });
  }
}

async function loadMessages() {
  try {
    const stats = await getMyUnreadCategoryStats();
    categoryStats.value = stats.categories || [];
    grouped.value = Number(stats.total || 0) > 10;
    if (grouped.value) {
      messages.value = [];
      return;
    }
    const result = await getMySiteMessages({ pageNum: 1, pageSize: Math.max(10, props.pageSize), unreadOnly: true });
    messages.value = result.list || [];
  } catch {
    messages.value = [];
    categoryStats.value = [];
    grouped.value = false;
  }
}

async function openDetail(id: string) {
  const message = await getMySiteMessageDetail(id);
  currentMessage.value = message;
  detailVisible.value = true;
  await markMySiteMessageRead(id);
  if (!props.enableRealtime) {
    await loadUnreadCount();
  }
  emit('message-open', message);
}

async function markAllRead() {
  await markAllMySiteMessagesRead();
  if (props.enableRealtime) {
    updateUnreadCount(0);
  } else {
    await loadUnreadCount();
  }
  await loadMessages();
}

function viewAllMessages() {
  emit('view-all');
}

function viewCategory(category: NoticeSiteMessageCategory) {
  emit('view-all', { category, unreadOnly: true });
}

function categoryLabel(category: NoticeSiteMessageCategory) {
  return ({ APPROVAL: '审批类消息', SYSTEM: '系统通知', BUSINESS: '业务通知' } as const)[category];
}

function openReceiveSetting() {
  emit('settings');
}

function defaultRuntimeConfig(): NoticeClientBellRuntimeConfig {
  return {
    reminderMode: 'SOUND',
    voiceText: '您有新的系统消息，请及时查看',
    soundType: 'IM',
    popupEnabled: true,
    popupPlacement: 'top-right',
    desktopNotificationEnabled: true,
  };
}

async function resolveRuntimeConfig(): Promise<NoticeClientBellRuntimeConfig> {
  const defaults = defaultRuntimeConfig();
  try {
    const loaded = props.loadRuntimeConfig ? await props.loadRuntimeConfig() : undefined;
    return { ...defaults, ...(props.runtimeConfig || {}), ...(loaded || {}) };
  } catch {
    return { ...defaults, ...(props.runtimeConfig || {}) };
  }
}

function voiceEnabled(config: NoticeClientBellRuntimeConfig) {
  return config.voiceEnabled ?? config.soundEnabled ?? true;
}

function voiceText(config: NoticeClientBellRuntimeConfig, message: NoticeSiteMessage) {
  return config.voiceText || config.soundText || message.title;
}

function soundType(config: NoticeClientBellRuntimeConfig) {
  return config.soundType || 'IM';
}

function reminderMode(config: NoticeClientBellRuntimeConfig) {
  return config.reminderMode || 'SOUND';
}

function bizDisplayName(message: NoticeSiteMessage) {
  return message.bizGroup || message.bizName || message.bizType || '通用消息';
}

function bizAvatar(message: NoticeSiteMessage) {
  return bizDisplayName(message).trim().slice(0, 1) || '消';
}

function notificationMessage(message: NoticeSiteMessage) {
  const presentation = presentNoticeMessage(message);
  const rows = [
    { key: 'type', label: '消息类型', html: sanitizeNoticeHtml(presentation.typeLabel) },
    { key: 'content', label: '消息内容', html: sanitizeNoticeHtml(message.content) },
    { key: 'time', label: '消息时间', html: sanitizeNoticeHtml(message.createTime) },
  ];
  return h('div', { class: 'notice-notification-message' }, [
    h(
      'div',
      { class: 'notice-notification-message__rows' },
      rows.map((row) =>
        h('div', { class: 'notice-notification-message__row', key: row.key }, [
          h('span', { class: 'notice-notification-message__label' }, `${row.label}：`),
          h('span', { class: 'notice-notification-message__value', innerHTML: row.html }),
        ]),
      ),
    ),
    h('div', { class: 'notice-notification-message__footer' }, [
      h('span', { class: 'notice-notification-message__link' }, '点击查看'),
    ]),
  ]);
}

async function handleDetailAction(action: NoticeSiteMessageAction) {
  if (!currentMessage.value) return;
  const message = currentMessage.value;
  if (action.interactionType === 'ROUTE') {
    emit('interaction', {
      ...buildNoticeInteraction(message, action),
      onComplete: (success) => {
        if (success) detailVisible.value = false;
      },
    });
    return;
  }
  if (action.confirmRequired) {
    await ElMessageBox.confirm(`确认执行“${action.actionLabel}”吗？`, '操作确认', { type: 'warning' });
  }
  await executeMySiteMessageAction(message.id, action.actionCode, buildNoticeActionInput(message, action));
  ElMessage.success('操作已提交');
  detailVisible.value = false;
  await Promise.all([loadMessages(), loadUnreadCount()]);
}

async function notifyNewMessage(message: NoticeSiteMessage) {
  emit('message-received', message);
  const config = await resolveRuntimeConfig();
  if (voiceEnabled(config)) {
    if (reminderMode(config) === 'VOICE') {
      speakNoticeText(voiceText(config, message));
    } else {
      playNoticeSound(soundType(config));
    }
  }
  if (config.popupEnabled !== false) {
    ElNotification({
      title: noticePlainText(presentNoticeMessage(message).typeLabel),
      message: notificationMessage(message),
      type: 'info',
      position: config.popupPlacement || 'top-right',
      onClick: () => openDetail(message.id),
    });
  }
  if (config.desktopNotificationEnabled !== false) {
    showDesktopNotice(message, () => openDetail(message.id));
  }
}

async function handleRealtimeEvent(event: NoticeRealtimeEvent) {
  if (typeof event.unreadCount === 'number') {
    updateUnreadCount(event.unreadCount);
  }
  const message = event.messageId ? await getMySiteMessageDetail(event.messageId) : undefined;
  if (message) {
    await notifyNewMessage(message);
  }
}

onMounted(() => {
  requestDesktopPermission();
  void loadUnreadCount();
  if (props.enablePolling && !props.enableRealtime) {
    pollingTimer = window.setInterval(() => {
      void loadUnreadCount();
    }, props.pollingInterval);
  }
  if (!props.enableRealtime) {
    return;
  }
  stopRealtime = createNoticeRealtime(handleRealtimeEvent, { realtimeOptions: props.realtimeOptions });
});

onUnmounted(() => {
  stopRealtime?.();
  if (pollingTimer) {
    window.clearInterval(pollingTimer);
  }
});
defineExpose({ notifyNewMessage, loadUnreadCount });
</script>

<style scoped>
.notice-bell {
  display: inline-flex;
  align-items: center;
}
.notice-bell__trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  padding: 0;
  border: 0;
  color: inherit;
  background: transparent;
  cursor: pointer;
}
.notice-bell__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.notice-bell__categories {
  display: flex;
  flex-direction: column;
}
.notice-bell__category {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 14px 0;
  border: 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
  color: var(--el-text-color-primary);
  background: transparent;
  font-size: 14px;
  cursor: pointer;
}
.notice-bell__category:hover {
  color: var(--el-color-primary);
}
.notice-bell__item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
  cursor: pointer;
}
.notice-bell__item:hover .notice-bell__title {
  color: var(--el-color-primary);
}
.notice-bell__avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 36px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  font-size: 15px;
  font-weight: 600;
}
.notice-bell__body {
  min-width: 0;
  flex: 1;
}
.notice-bell__title {
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.notice-bell__summary {
  margin-top: 4px;
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 18px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.notice-bell__meta {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.notice-bell__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
}

:global(.notice-notification-message__footer),
:global(.notice-notification-message__row) {
  display: flex;
  align-items: center;
  gap: 4px;
}

:global(.notice-notification-message__row) {
  min-width: 0;
  font-size: 12px;
  line-height: 20px;
}

:global(.notice-notification-message__label) {
  flex: 0 0 auto;
  color: var(--el-text-color-secondary);
}

:global(.notice-notification-message__value) {
  min-width: 0;
  overflow: hidden;
  color: var(--el-text-color-regular);
  text-overflow: ellipsis;
  white-space: nowrap;
}

:global(.notice-notification-message__footer) {
  justify-content: flex-end;
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid var(--el-border-color-lighter);
}

:global(.notice-notification-message__link) {
  font-size: 12px;
  line-height: 18px;
}

:global(.notice-notification-message__link) {
  color: var(--el-color-primary);
}
</style>
