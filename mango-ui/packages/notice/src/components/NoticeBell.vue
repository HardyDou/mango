<template>
  <NoticeClientBell
    v-bind="$attrs"
    :runtime-config="runtimeConfig"
    :load-runtime-config="loadRuntimeConfig"
    :realtime-options="realtimeOptions"
    @view-all="(options) => emit('view-all', options)"
    @settings="() => emit('settings')"
    @unread-change="(count) => emit('unread-change', count)"
    @message-open="(message) => emit('message-open', message)"
    @message-received="(message) => emit('message-received', message)"
    @interaction="(payload) => emit('interaction', payload)"
  />
</template>

<script setup lang="ts">
import NoticeClientBell from '../client/NoticeClientBell.vue';
import type { RealtimeOptions } from '@mango/common';
import type { NoticeSiteMessage } from '../types/notice';
import type { NoticeBellViewAllOptions, NoticeClientBellRuntimeConfig } from '../client/types';
import type { NoticeInteractionPayload } from '../client/interaction';

defineOptions({ inheritAttrs: false });

defineProps<{
  runtimeConfig?: NoticeClientBellRuntimeConfig;
  loadRuntimeConfig?: () => Promise<NoticeClientBellRuntimeConfig> | NoticeClientBellRuntimeConfig;
  realtimeOptions?: RealtimeOptions;
}>();

const emit = defineEmits<{
  (event: 'view-all', options?: NoticeBellViewAllOptions): void;
  (event: 'settings'): void;
  (event: 'unread-change', count: number): void;
  (event: 'message-open', message: NoticeSiteMessage): void;
  (event: 'message-received', message: NoticeSiteMessage): void;
  (event: 'interaction', payload: NoticeInteractionPayload): void;
}>();
</script>
