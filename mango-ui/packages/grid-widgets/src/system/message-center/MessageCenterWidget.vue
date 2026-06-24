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

    <el-alert
      v-if="errorMessage"
      class="mango-grid-widget-message-center__error"
      :title="errorMessage"
      type="error"
      :closable="false"
      show-icon
    />

  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  getMySiteMessages,
  getMyUnreadCount,
  markAllMySiteMessagesRead,
} from '@mango/notice';
import type { NoticeSiteMessage } from '@mango/notice';
import type { MessageCenterCategory, MessageCenterWidgetProps } from '../../types';

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
const errorMessage = ref('');
const categoryCounts = ref<Record<string, number>>({});

const resolvedCategories = computed(() => (
  props.categories?.length ? props.categories : DEFAULT_CATEGORIES
));

const categoryStats = computed(() => resolvedCategories.value.map(item => ({
  ...item,
  count: categoryCounts.value[item.key] || 0,
  color: item.color || '#2f80ff',
})));

const latestTitle = computed(() => latestMessage.value?.title || '暂无未读消息');

const latestContent = computed(() => latestMessage.value?.content || '当前没有需要处理的未读消息。');

const latestMetaText = computed(() => {
  if (!latestMessage.value) {
    return '消息中心 · 暂无更新';
  }
  return `${latestMessage.value.bizName || latestMessage.value.bizGroup || '消息通知'} · ${formatRelativeTime(latestMessage.value.createTime)}`;
});

onMounted(() => {
  void refreshMessages();
});

async function refreshMessages(): Promise<void> {
  loading.value = true;
  errorMessage.value = '';
  try {
    await Promise.all([
      loadUnreadCount(),
      loadLatestMessage(),
      loadCategoryCounts(),
    ]);
  } catch {
    errorMessage.value = '消息加载失败，请稍后重试';
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

function formatRelativeTime(createTime?: string): string {
  if (!createTime) {
    return '-';
  }

  const time = new Date(createTime.replace(/-/g, '/')).getTime();
  if (Number.isNaN(time)) {
    return createTime;
  }

  const diffMinutes = Math.max(0, Math.floor((Date.now() - time) / 60000));
  if (diffMinutes < 1) {
    return '刚刚';
  }
  if (diffMinutes < 60) {
    return `${diffMinutes} 分钟前`;
  }

  const diffHours = Math.floor(diffMinutes / 60);
  if (diffHours < 24) {
    return `${diffHours} 小时前`;
  }

  const diffDays = Math.floor(diffHours / 24);
  if (diffDays < 7) {
    return `${diffDays} 天前`;
  }

  return createTime;
}
</script>
