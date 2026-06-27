<template>
  <NoticeClientMessageCenter @settings="goReceiveSetting" @announcement="goAnnouncement" />
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import NoticeClientMessageCenter from '../../client/NoticeClientMessageCenter.vue';

const router = useRouter();

function goReceiveSetting() {
  router.push('/notice/receive-setting');
}

function goAnnouncement(id: string) {
  router.push({ path: '/message-center/announcement', query: { id } });
}
</script>
