<template>
  <NoticeClientMessageCenter @settings="goReceiveSetting" />
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import NoticeClientMessageCenter from '../../client/NoticeClientMessageCenter.vue';

const router = useRouter();

function goReceiveSetting() {
  router.push('/notice/receive-setting');
}
</script>
