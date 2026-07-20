<template>
  <NoticeClientMessageCenter
    @settings="goReceiveSetting"
    @announcement="goAnnouncement"
    @interaction="handleInteraction"
  />
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import NoticeClientMessageCenter from '../../client/NoticeClientMessageCenter.vue';
import type { NoticeSiteMessage, NoticeSiteMessageAction } from '../../types/notice';

const router = useRouter();

function goReceiveSetting() {
  openNamedTarget('notice:receive-setting');
}

function goAnnouncement(id: string) {
  openNamedTarget('notice:announcement-user', { id });
}

async function handleInteraction(payload: {
  message: NoticeSiteMessage;
  action?: NoticeSiteMessageAction;
  targetKey?: string;
  targetType?: 'ROUTE' | 'FLOW';
  params?: Record<string, unknown>;
}) {
  if (!payload.targetKey) {
    ElMessage.warning('该消息动作未配置目标');
    return;
  }
  await openNamedTarget(payload.targetKey, payload.params);
}

async function openNamedTarget(targetKey: string, params?: Record<string, unknown>) {
  const path = NOTICE_TARGET_PATHS[targetKey];
  if (!path && !router.hasRoute(targetKey)) {
    ElMessage.warning('目标未注册或当前无权访问');
    return;
  }
  try {
    await router.push(path
      ? { path, query: normalizeQuery(params) }
      : { name: targetKey, query: normalizeQuery(params) });
  } catch (error) {
    console.warn('Notice target navigation failed', targetKey, error);
    ElMessage.warning('目标未注册或当前无权访问');
  }
}

const NOTICE_TARGET_PATHS: Record<string, string> = {
  'notice:receive-setting': '/message-center/receive-setting',
  'notice:announcement-user': '/message-center/announcement',
};

function normalizeQuery(params?: Record<string, unknown>) {
  return Object.fromEntries(Object.entries(params || {})
    .filter(([, value]) => value !== undefined && value !== null)
    .map(([key, value]) => [key, String(value)]));
}
</script>
