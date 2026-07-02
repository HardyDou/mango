<template>
  <section
    v-loading="loading"
    class="mango-grid-widget-message-center"
    >
    <header class="mango-grid-widget-message-center__header">
      <span>我的消息</span>
      <div class="mango-grid-widget-message-center__header-actions">
        <button
          type="button"
          @click="viewAllMessages"
        >
          查看全部
        </button>
        <button
          type="button"
          :disabled="unreadCount === 0 || markingRead"
          @click="markAllRead"
        >
          {{ markingRead ? '处理中' : '全部已读' }}
        </button>
      </div>
    </header>

    <div class="mango-grid-widget-message-center__summary">
      <div class="mango-grid-widget-message-center__unread-card">
        <div class="mango-grid-widget-message-center__label">未读消息</div>
        <div class="mango-grid-widget-message-center__count">
          {{ unreadCount }}
          <span>条</span>
        </div>
      </div>
      <div class="mango-grid-widget-message-center__latest">
        <span>最新未读</span>
        <strong>{{ latestTitle }}</strong>
        <p>{{ latestContent }}</p>
        <em>{{ latestMetaText }}</em>
      </div>
    </div>

    <div class="mango-grid-widget-message-center__stats">
      <div
        v-for="item in categoryStats"
        :key="item.key"
        class="mango-grid-widget-message-center__stat"
        :style="{ '--message-center-stat-color': item.color }"
      >
        <i />
        <span>{{ item.label }}</span>
        <strong>{{ item.count }}</strong>
      </div>
    </div>

  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  getMySiteMessages,
  getMyUnreadCount,
  markAllMySiteMessagesRead,
} from '../../api/notice';
import type { NoticeSiteMessage } from '../../api/notice';
import type { MessageCenterCategory, MessageCenterWidgetProps } from '../types';

defineOptions({
  name: 'MangoMessageCenterWidget',
});

const DEFAULT_CATEGORIES: MessageCenterCategory[] = [
  { key: 'system', label: '系统通知', bizGroup: '系统', color: '#2f80ff' },
  { key: 'business', label: '业务通知', bizGroup: '业务', color: '#2fd1c9' },
  { key: 'approval', label: '审批通知', bizGroup: '审批', color: '#ffbf2f' },
  { key: 'alert', label: '告警通知', bizGroup: '告警', color: '#ff6b6b' },
];

const props = withDefaults(defineProps<MessageCenterWidgetProps>(), {
  messageCenterPath: '/notice/site-message',
  pageSize: 1,
});

const loading = ref(false);
const markingRead = ref(false);
const unreadCount = ref(0);
const latestMessage = ref<NoticeSiteMessage>();
const categoryCounts = ref<Record<string, number>>({});

const resolvedCategories = computed(() => (
  props.categories?.length ? props.categories : DEFAULT_CATEGORIES
));

const categoryStats = computed(() => resolvedCategories.value.map(item => ({
  ...item,
  count: categoryCounts.value[item.key] || 0,
  color: item.color || '#2f80ff',
})));

const latestTitle = computed(() => formatMessageText(latestMessage.value?.title, '暂无未读消息'));

const latestContent = computed(() => formatMessageText(latestMessage.value?.content, '当前没有需要处理的未读消息。'));

const latestMetaText = computed(() => {
  if (!latestMessage.value) {
    return '我的消息 · 暂无更新';
  }
  return `${latestMessage.value.bizName || latestMessage.value.bizGroup || '消息通知'} · ${formatDateTime(latestMessage.value.createTime)}`;
});

onMounted(() => {
  void refreshMessages();
});

async function refreshMessages(): Promise<void> {
  loading.value = true;
  try {
    await Promise.all([
      loadUnreadCount(),
      loadLatestMessage(),
      loadCategoryCounts(),
    ]);
  } catch {
    ElMessage.error('消息加载失败，请稍后重试');
  } finally {
    loading.value = false;
  }
}

async function loadUnreadCount(): Promise<void> {
  const result = await getMyUnreadCount();
  unreadCount.value = Number(result.count || 0);
}

async function loadLatestMessage(): Promise<void> {
  const result = await getMySiteMessages({
    pageNum: 1,
    pageSize: props.pageSize,
    unreadOnly: true,
  });
  latestMessage.value = result.list?.[0];
}

async function loadCategoryCounts(): Promise<void> {
  const entries = await Promise.all(resolvedCategories.value.map(async (item) => {
    const result = await getMySiteMessages({
      pageNum: 1,
      pageSize: 1,
      unreadOnly: true,
      bizGroup: item.bizGroup,
      bizType: item.bizType,
      priority: item.priority,
    });
    return [item.key, Number(result.total || 0)] as const;
  }));
  categoryCounts.value = Object.fromEntries(entries);
}

async function markAllRead(): Promise<void> {
  markingRead.value = true;
  try {
    await markAllMySiteMessagesRead();
    ElMessage.success('已全部标记为已读');
    await refreshMessages();
  } catch {
    ElMessage.error('全部已读失败，请稍后重试');
  } finally {
    markingRead.value = false;
  }
}

async function viewAllMessages(): Promise<void> {
  await props.runtime?.navigate?.({
    path: props.messageCenterPath,
  });
}

function formatMessageText(value: string | undefined, fallback: string): string {
  const text = String(value || '')
    // 登录类系统消息会把客户端 IP 拼进内容，小组件首页只保留对用户有用的摘要。
    .replace(/客户端\s*IP\s*[:：]?\s*[\da-fA-F:.]+/g, '')
    .replace(/已成功登录/g, '')
    .replace(/[，,；;。]\s*[，,；;。]/g, '，')
    .replace(/^[\s，,；;。]+|[\s，,；;。]+$/g, '');
  return text || fallback;
}

function formatDateTime(createTime?: string): string {
  if (!createTime) {
    return '-';
  }

  const normalized = createTime.replace('T', ' ');
  const time = new Date(normalized.replace(/-/g, '/')).getTime();
  if (Number.isNaN(time)) {
    return normalized;
  }

  const date = new Date(time);
  const year = date.getFullYear();
  const month = padDatePart(date.getMonth() + 1);
  const day = padDatePart(date.getDate());
  const hours = padDatePart(date.getHours());
  const minutes = padDatePart(date.getMinutes());
  const seconds = padDatePart(date.getSeconds());
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}

function padDatePart(value: number): string {
  return String(value).padStart(2, '0');
}
</script>
