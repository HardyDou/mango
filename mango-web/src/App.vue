<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { ElMessage } from 'element-plus';

onMounted(() => {
  // Catch unhandled promise rejections
  window.addEventListener('unhandledrejection', (event) => {
    console.error('Unhandled promise rejection:', event.reason);
    ElMessage.error('系统错误，请刷新页面');
  });
});
</script>

<style>
#app {
  height: 100%;
}
</style>
