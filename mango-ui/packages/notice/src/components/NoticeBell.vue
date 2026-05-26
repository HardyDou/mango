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
import { getMySiteMessageDetail, getMySiteMessages, getMyUnreadCount, markAllMySiteMessagesRead, markMySiteMessageRead } from '../api/notice';
import NoticeDetailDialog from './NoticeDetailDialog.vue';
import { createNoticeRealtime, playNoticeSound, requestDesktopPermission, showDesktopNotice } from '../realtime/noticeRealtime';
import type { NoticeSiteMessage } from '../types/notice';

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

function notifyNewMessage(message: NoticeSiteMessage) {
 playNoticeSound();
 ElNotification({ title: '您有新消息了', message: message.title, type: 'info', onClick: () => openDetail(message.id) });
 showDesktopNotice(message, () => openDetail(message.id));
}

onMounted(() => {
 requestDesktopPermission();
 void loadUnreadCount();
 stopRealtime = createNoticeRealtime(async event => {
 await loadUnreadCount();
 const message = event.messageId ? await getMySiteMessageDetail(event.messageId) : undefined;
 if (message) {
 notifyNewMessage(message);
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
