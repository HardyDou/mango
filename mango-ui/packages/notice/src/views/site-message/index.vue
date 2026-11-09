<template>
  <NoticeClientMessageCenter
    :category="messageCategory"
    :unread-only="unreadOnly"
    @announcement="goAnnouncement"
    @interaction="handleInteraction"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import NoticeClientMessageCenter from '../../client/NoticeClientMessageCenter.vue';
import type { NoticeInteractionPayload } from '../../client/interaction';
import type { NoticeSiteMessageCategory } from '../../types/notice';
import { openNoticeTarget } from './targets';

defineOptions({ name: 'NoticeSiteMessageView' });

const router = useRouter();
const route = useRoute();
const messageCategory = computed(() => normalizeCategory(route.query.category));
const unreadOnly = computed(() => route.query.unreadOnly === 'true');

function normalizeCategory(value: unknown): NoticeSiteMessageCategory | undefined {
  return typeof value === 'string' && ['APPROVAL', 'SYSTEM', 'BUSINESS'].includes(value)
    ? (value as NoticeSiteMessageCategory)
    : undefined;
}

function goAnnouncement(id: string) {
  void router.push({ path: '/profile', query: { tab: 'notice-announcement-user', id } });
}

async function handleInteraction(payload: NoticeInteractionPayload) {
  if (!payload.targetKey) {
    ElMessage.warning('该消息动作未配置目标');
    payload.onComplete?.(false);
    return;
  }
  await openNamedTarget(payload.targetKey, payload.params, payload.onComplete);
}

async function openNamedTarget(
  targetKey: string,
  params?: Record<string, unknown>,
  onComplete?: (success: boolean) => void,
) {
  const resolution = await openNoticeTarget(router, targetKey, params);
  onComplete?.(resolution !== 'none');
  if (resolution === 'fallback') {
    ElMessage.warning('原页面不可访问，已打开通用页面');
  } else if (resolution === 'message-center') {
    ElMessage.warning('消息目标未注册，已打开消息中心');
  } else if (resolution === 'none') {
    ElMessage.warning('目标未注册或当前无权访问');
  }
}
</script>
