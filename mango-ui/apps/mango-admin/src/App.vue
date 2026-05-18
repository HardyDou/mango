<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { mangoMessage } from '@mango/common';

onMounted(() => {
  // Catch unhandled promise rejections
  window.addEventListener('unhandledrejection', (event) => {
    console.error('Unhandled promise rejection:', event.reason);
    mangoMessage.error('系统错误，请刷新页面');
  });
});
</script>

<style>
#app {
  height: 100%;
}
</style>
