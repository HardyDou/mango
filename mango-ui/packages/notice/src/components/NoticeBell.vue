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
 <div class="notice-bell__title">{{ message.title }}</div>
 <div class="notice-bell__meta">{{ message.bizType || 'GENERAL' }} · {{ message.createTime || '-' }}</div>
 </div>
 </div>
 </el-popover>
 <NoticeDetailDialog v-model="detailVisible" :message="currentMessage" />
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { ElNotification } from 'element-plus';
import { iconMap } from '@mango/common/utils/iconConfig';
import { getChannelConfigs, getMySiteMessageDetail, getMySiteMessages, getMyUnreadCount, markAllMySiteMessagesRead, markMySiteMessageRead } from '../api/notice';
import NoticeDetailDialog from './NoticeDetailDialog.vue';
import { createNoticeRealtime, requestDesktopPermission, showDesktopNotice, speakNoticeText } from '../realtime/noticeRealtime';
import type { NoticeSiteMessage } from '../types/notice';

interface SiteChannelRuntimeConfig {
 soundEnabled?: boolean;
 soundText?: string;
 popupEnabled?: boolean;
 desktopNotificationEnabled?: boolean;
}

const unreadCount = ref(0);
const messages = ref<NoticeSiteMessage[]>([]);
const currentMessage = ref<NoticeSiteMessage>();
const detailVisible = ref(false);
const badgeValue = computed(() => unreadCount.value > 99 ? '99+' : unreadCount.value);
const BellIcon = iconMap.Bell;
let stopRealtime: (() => void) | undefined;

async function loadUnreadCount() {
 try {
 const result = await getMyUnreadCount();
 unreadCount.value = result.count || 0;
 } catch {
 unreadCount.value = 0;
 }
}

async function loadMessages() {
 try {
 const result = await getMySiteMessages({ pageNum: 1, pageSize: 5, unreadOnly: true });
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
}

async function markAllRead() {
 await markAllMySiteMessagesRead();
 await loadUnreadCount();
 await loadMessages();
}

async function loadSiteRuntimeConfig(): Promise<SiteChannelRuntimeConfig> {
 try {
 const result = await getChannelConfigs({ channelType: 'SITE', enabled: true, pageSize: 20 }, { silentError: true });
 const siteChannel = (result.list || []).find(item => item.providerCode === 'INTERNAL') || result.list?.[0];
 return parseSiteRuntimeConfig(siteChannel?.configJson);
 } catch {
 return parseSiteRuntimeConfig();
 }
}

function parseSiteRuntimeConfig(configJson?: string): SiteChannelRuntimeConfig {
 const defaults: SiteChannelRuntimeConfig = {
  soundEnabled: true,
  soundText: '您有新的系统消息，请及时查看',
  popupEnabled: true,
  desktopNotificationEnabled: true,
 };
 if (!configJson) {
  return defaults;
 }
 try {
  const parsed = JSON.parse(configJson);
  return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? { ...defaults, ...parsed } : defaults;
 } catch {
  return defaults;
 }
}

async function notifyNewMessage(message: NoticeSiteMessage) {
 const config = await loadSiteRuntimeConfig();
 if (config.soundEnabled !== false) {
  speakNoticeText(config.soundText || message.title);
 }
 if (config.popupEnabled !== false) {
  ElNotification({ title: '您有新消息了', message: message.title, type: 'info', onClick: () => openDetail(message.id) });
 }
 if (config.desktopNotificationEnabled !== false) {
  showDesktopNotice(message, () => openDetail(message.id));
 }
}

onMounted(() => {
 requestDesktopPermission();
 void loadUnreadCount();
 stopRealtime = createNoticeRealtime(async event => {
 await loadUnreadCount();
 const message = event.messageId ? await getMySiteMessageDetail(event.messageId) : undefined;
 if (message) {
 await notifyNewMessage(message);
 }
 });
});

onUnmounted(() => stopRealtime?.());
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
.notice-bell__header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.notice-bell__item { padding: 8px 0; border-bottom: 1px solid var(--el-border-color-lighter); cursor: pointer; }
.notice-bell__title { font-weight: 600; }
.notice-bell__meta { margin-top: 4px; color: var(--el-text-color-secondary); font-size: 12px; }
</style>
