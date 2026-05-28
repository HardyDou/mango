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
 <el-empty v-if="messages.length === 0" description="暂无消息" :image-size="60" />
 <div v-for="message in messages" :key="message.id" class="notice-bell__item" @click="openDetail(message.id)">
 <div class="notice-bell__avatar">{{ bizAvatar(message) }}</div>
 <div class="notice-bell__body">
 <div class="notice-bell__title">{{ message.title || '未命名消息' }}</div>
 <div class="notice-bell__summary">{{ message.content || '暂无内容' }}</div>
 <div class="notice-bell__meta">{{ bizDisplayName(message) }} · {{ message.createTime || '-' }}</div>
 </div>
 </div>
 <div class="notice-bell__footer">
 <el-button link type="primary" data-test="view-all-button" @click="viewAllMessages">查看全部</el-button>
 <el-button link type="primary" data-test="settings-button" @click="openReceiveSetting">接收设置</el-button>
 </div>
 </div>
 </el-popover>
 <NoticeDetailDialog v-model="detailVisible" :message="currentMessage" />
</template>

<script setup lang="ts">
import { computed, h, onMounted, onUnmounted, ref } from 'vue';
import { ElNotification } from 'element-plus';
import { iconMap } from '@mango/common/utils/iconConfig';
import type { RealtimeOptions } from '@mango/common';
import { getMySiteMessageDetail, getMySiteMessages, getMyUnreadCount, markAllMySiteMessagesRead, markMySiteMessageRead } from '../api/notice';
import NoticeDetailDialog from '../components/NoticeDetailDialog.vue';
import { createNoticeRealtime, requestDesktopPermission, showDesktopNotice, speakNoticeText } from '../realtime/noticeRealtime';
import type { NoticeSiteMessage } from '../types/notice';
import type { NoticeClientBellRuntimeConfig } from './types';

const props = withDefaults(defineProps<{
 enableRealtime?: boolean;
 enablePolling?: boolean;
 pollingInterval?: number;
 pageSize?: number;
 runtimeConfig?: NoticeClientBellRuntimeConfig;
 loadRuntimeConfig?: () => Promise<NoticeClientBellRuntimeConfig> | NoticeClientBellRuntimeConfig;
 realtimeOptions?: RealtimeOptions;
}>(), {
 enableRealtime: true,
 enablePolling: true,
 pollingInterval: 30000,
 pageSize: 5,
});

const emit = defineEmits<{
 (event: 'view-all'): void;
 (event: 'settings'): void;
 (event: 'unread-change', count: number): void;
 (event: 'message-open', message: NoticeSiteMessage): void;
 (event: 'message-received', message: NoticeSiteMessage): void;
}>();

const unreadCount = ref(0);
const messages = ref<NoticeSiteMessage[]>([]);
const currentMessage = ref<NoticeSiteMessage>();
const detailVisible = ref(false);
const badgeValue = computed(() => unreadCount.value > 99 ? '99+' : unreadCount.value);
const BellIcon = iconMap.Bell;
let stopRealtime: (() => void) | undefined;
let pollingTimer: ReturnType<typeof window.setInterval> | undefined;

async function loadUnreadCount() {
 try {
 const result = await getMyUnreadCount();
 unreadCount.value = result.count || 0;
 } catch {
 unreadCount.value = 0;
 }
 emit('unread-change', unreadCount.value);
}

async function loadMessages() {
 try {
 const result = await getMySiteMessages({ pageNum: 1, pageSize: props.pageSize, unreadOnly: true });
 messages.value = result.list || [];
 } catch {
 messages.value = [];
 }
}

async function openDetail(id: string) {
 currentMessage.value = await getMySiteMessageDetail(id);
 detailVisible.value = true;
 await markMySiteMessageRead(id);
 await loadUnreadCount();
 emit('message-open', currentMessage.value);
}

async function markAllRead() {
 await markAllMySiteMessagesRead();
 await loadUnreadCount();
 await loadMessages();
}

function viewAllMessages() {
 emit('view-all');
}

function openReceiveSetting() {
 emit('settings');
}

function defaultRuntimeConfig(): NoticeClientBellRuntimeConfig {
 return {
  voiceText: '您有新的系统消息，请及时查看',
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

function bizDisplayName(message: NoticeSiteMessage) {
 return message.bizGroup || message.bizName || message.bizType || '通用消息';
}

function bizAvatar(message: NoticeSiteMessage) {
 return bizDisplayName(message).trim().slice(0, 1) || '消';
}

function notificationMessage(message: NoticeSiteMessage) {
 return h('div', { class: 'notice-notification-message' }, [
  h('div', { class: 'notice-notification-message__summary' }, message.content || '暂无内容'),
  h('div', { class: 'notice-notification-message__meta' }, `${bizDisplayName(message)} · ${message.createTime || '-'}`),
 ]);
}

async function notifyNewMessage(message: NoticeSiteMessage) {
 emit('message-received', message);
 const config = await resolveRuntimeConfig();
 if (voiceEnabled(config)) {
  speakNoticeText(voiceText(config, message));
 }
 if (config.popupEnabled !== false) {
  ElNotification({
   title: message.title || '未命名消息',
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

onMounted(() => {
 requestDesktopPermission();
 void loadUnreadCount();
 if (props.enablePolling) {
 pollingTimer = window.setInterval(() => {
 void loadUnreadCount();
 }, props.pollingInterval);
 }
 if (!props.enableRealtime) {
 return;
 }
 stopRealtime = createNoticeRealtime(async event => {
 await loadUnreadCount();
 const message = event.messageId ? await getMySiteMessageDetail(event.messageId) : undefined;
 if (message) {
  await notifyNewMessage(message);
 }
 }, { realtimeOptions: props.realtimeOptions });
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

:global(.notice-notification-message__summary) {
 color: var(--el-text-color-regular);
 font-size: 13px;
 line-height: 20px;
 word-break: break-word;
}

:global(.notice-notification-message__meta) {
 margin-top: 6px;
 color: var(--el-text-color-secondary);
 font-size: 12px;
 line-height: 18px;
}
</style>
